/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FinalizationRegistry.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GlobalEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PromiseReaction.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/TemporaryClearException.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

NonnullRefPtr<VM> VM::create(OwnPtr<CustomData> custom_data)
{
    return adopt_ref(*new VM(move(custom_data)));
}

VM::VM(OwnPtr<CustomData> custom_data)
    : m_heap(*this)
    , m_custom_data(move(custom_data))
{
    m_empty_string = m_heap.allocate_without_global_object<PrimitiveString>(String::empty());
    for (size_t i = 0; i < 128; ++i) {
        m_single_ascii_character_strings[i] = m_heap.allocate_without_global_object<PrimitiveString>(String::formatted("{:c}", i));
    }

#define __JS_ENUMERATE(SymbolName, snake_name) \
    m_well_known_symbol_##snake_name = js_symbol(*this, "Symbol." #SymbolName, false);
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE
}

VM::~VM()
{
}

Interpreter& VM::interpreter()
{
    VERIFY(!m_interpreters.is_empty());
    return *m_interpreters.last();
}

Interpreter* VM::interpreter_if_exists()
{
    if (m_interpreters.is_empty())
        return nullptr;
    return m_interpreters.last();
}

void VM::push_interpreter(Interpreter& interpreter)
{
    m_interpreters.append(&interpreter);
}

void VM::pop_interpreter(Interpreter& interpreter)
{
    VERIFY(!m_interpreters.is_empty());
    auto* popped_interpreter = m_interpreters.take_last();
    VERIFY(popped_interpreter == &interpreter);
}

VM::InterpreterExecutionScope::InterpreterExecutionScope(Interpreter& interpreter)
    : m_interpreter(interpreter)
{
    m_interpreter.vm().push_interpreter(m_interpreter);
}

VM::InterpreterExecutionScope::~InterpreterExecutionScope()
{
    m_interpreter.vm().pop_interpreter(m_interpreter);
}

void VM::gather_roots(HashTable<Cell*>& roots)
{
    roots.set(m_empty_string);
    for (auto* string : m_single_ascii_character_strings)
        roots.set(string);

    roots.set(m_exception);

    if (m_last_value.is_cell())
        roots.set(&m_last_value.as_cell());

    for (auto& execution_context : m_execution_context_stack) {
        if (execution_context->this_value.is_cell())
            roots.set(&execution_context->this_value.as_cell());
        roots.set(execution_context->arguments_object);
        for (auto& argument : execution_context->arguments) {
            if (argument.is_cell())
                roots.set(&argument.as_cell());
        }
        roots.set(execution_context->lexical_environment);
        roots.set(execution_context->variable_environment);
    }

#define __JS_ENUMERATE(SymbolName, snake_name) \
    roots.set(well_known_symbol_##snake_name());
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE

    for (auto& symbol : m_global_symbol_map)
        roots.set(symbol.value);

    for (auto* job : m_promise_jobs)
        roots.set(job);

    for (auto* finalization_registry : m_finalization_registry_cleanup_jobs)
        roots.set(finalization_registry);
}

Symbol* VM::get_global_symbol(const String& description)
{
    auto result = m_global_symbol_map.get(description);
    if (result.has_value())
        return result.value();

    auto new_global_symbol = js_symbol(*this, description, true);
    m_global_symbol_map.set(description, new_global_symbol);
    return new_global_symbol;
}

void VM::set_variable(const FlyString& name, Value value, GlobalObject& global_object, bool first_assignment, Environment* specific_scope)
{
    Optional<Variable> possible_match;
    if (!specific_scope && m_execution_context_stack.size()) {
        for (auto* environment = lexical_environment(); environment; environment = environment->outer_environment()) {
            possible_match = environment->get_from_environment(name);
            if (possible_match.has_value()) {
                specific_scope = environment;
                break;
            }
        }
    }

    if (specific_scope && possible_match.has_value()) {
        if (!first_assignment && possible_match.value().declaration_kind == DeclarationKind::Const) {
            throw_exception<TypeError>(global_object, ErrorType::InvalidAssignToConst);
            return;
        }

        specific_scope->put_into_environment(name, { value, possible_match.value().declaration_kind });
        return;
    }

    if (specific_scope) {
        specific_scope->put_into_environment(name, { value, DeclarationKind::Var });
        return;
    }

    global_object.set(name, value, Object::ShouldThrowExceptions::Yes);
}

bool VM::delete_variable(FlyString const& name)
{
    Environment* specific_scope = nullptr;
    Optional<Variable> possible_match;
    if (!m_execution_context_stack.is_empty()) {
        for (auto* environment = lexical_environment(); environment; environment = environment->outer_environment()) {
            possible_match = environment->get_from_environment(name);
            if (possible_match.has_value()) {
                specific_scope = environment;
                break;
            }
        }
    }

    if (!possible_match.has_value())
        return false;
    if (possible_match.value().declaration_kind == DeclarationKind::Const)
        return false;

    VERIFY(specific_scope);
    return specific_scope->delete_from_environment(name);
}

void VM::assign(const FlyString& target, Value value, GlobalObject& global_object, bool first_assignment, Environment* specific_scope)
{
    set_variable(target, move(value), global_object, first_assignment, specific_scope);
}

void VM::assign(const Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>>& target, Value value, GlobalObject& global_object, bool first_assignment, Environment* specific_scope)
{
    if (auto id_ptr = target.get_pointer<NonnullRefPtr<Identifier>>())
        return assign((*id_ptr)->string(), move(value), global_object, first_assignment, specific_scope);

    assign(target.get<NonnullRefPtr<BindingPattern>>(), move(value), global_object, first_assignment, specific_scope);
}

void VM::assign(const NonnullRefPtr<BindingPattern>& target, Value value, GlobalObject& global_object, bool first_assignment, Environment* specific_scope)
{
    auto& binding = *target;

    switch (binding.kind) {
    case BindingPattern::Kind::Array: {
        auto iterator = get_iterator(global_object, value);
        if (!iterator)
            return;

        for (size_t i = 0; i < binding.entries.size(); i++) {
            if (exception())
                return;

            auto& entry = binding.entries[i];

            if (entry.is_rest) {
                VERIFY(i == binding.entries.size() - 1);

                auto* array = Array::create(global_object, 0);
                for (;;) {
                    auto next_object = iterator_next(*iterator);
                    if (!next_object)
                        return;

                    auto done_property = next_object->get(names.done);
                    if (exception())
                        return;

                    if (done_property.to_boolean())
                        break;

                    auto next_value = next_object->get(names.value);
                    if (exception())
                        return;

                    array->indexed_properties().append(next_value);
                }
                value = array;
            } else if (iterator) {
                auto next_object = iterator_next(*iterator);
                if (!next_object)
                    return;

                auto done_property = next_object->get(names.done);
                if (exception())
                    return;

                if (done_property.to_boolean()) {
                    iterator = nullptr;
                    value = js_undefined();
                } else {
                    value = next_object->get(names.value);
                    if (exception())
                        return;
                }
            } else {
                value = js_undefined();
            }

            if (value.is_undefined() && entry.initializer) {
                value = entry.initializer->execute(interpreter(), global_object);
                if (exception())
                    return;
            }

            entry.alias.visit(
                [&](Empty) {},
                [&](NonnullRefPtr<Identifier> const& identifier) {
                    set_variable(identifier->string(), value, global_object, first_assignment, specific_scope);
                },
                [&](NonnullRefPtr<BindingPattern> const& pattern) {
                    assign(pattern, value, global_object, first_assignment, specific_scope);
                });

            if (entry.is_rest)
                break;
        }

        break;
    }
    case BindingPattern::Kind::Object: {
        auto object = value.to_object(global_object);
        HashTable<PropertyName, PropertyNameTraits> seen_names;
        for (auto& property : binding.entries) {
            VERIFY(!property.is_elision());

            PropertyName assignment_name;
            JS::Value value_to_assign;
            if (property.is_rest) {
                VERIFY(property.name.has<NonnullRefPtr<Identifier>>());
                assignment_name = property.name.get<NonnullRefPtr<Identifier>>()->string();

                auto* rest_object = Object::create(global_object, global_object.object_prototype());
                for (auto& object_property : object->shape().property_table()) {
                    if (!object_property.value.attributes.is_enumerable())
                        continue;
                    if (seen_names.contains(object_property.key.to_display_string()))
                        continue;
                    rest_object->set(object_property.key, object->get(object_property.key), Object::ShouldThrowExceptions::Yes);
                    if (exception())
                        return;
                }

                value_to_assign = rest_object;
            } else {
                property.name.visit(
                    [&](Empty) { VERIFY_NOT_REACHED(); },
                    [&](NonnullRefPtr<Identifier> const& identifier) {
                        assignment_name = identifier->string();
                    },
                    [&](NonnullRefPtr<Expression> const& expression) {
                        auto result = expression->execute(interpreter(), global_object);
                        if (exception())
                            return;
                        assignment_name = result.to_property_key(global_object);
                    });

                if (exception())
                    break;

                value_to_assign = object->get(assignment_name);
            }

            seen_names.set(assignment_name);

            if (value_to_assign.is_empty())
                value_to_assign = js_undefined();

            if (value_to_assign.is_undefined() && property.initializer)
                value_to_assign = property.initializer->execute(interpreter(), global_object);

            if (exception())
                break;

            property.alias.visit(
                [&](Empty) {
                    set_variable(assignment_name.to_string(), value_to_assign, global_object, first_assignment, specific_scope);
                },
                [&](NonnullRefPtr<Identifier> const& identifier) {
                    VERIFY(!property.is_rest);
                    set_variable(identifier->string(), value_to_assign, global_object, first_assignment, specific_scope);
                },
                [&](NonnullRefPtr<BindingPattern> const& pattern) {
                    VERIFY(!property.is_rest);
                    assign(pattern, value_to_assign, global_object, first_assignment, specific_scope);
                });

            if (property.is_rest)
                break;
        }
        break;
    }
    }
}

Value VM::get_variable(const FlyString& name, GlobalObject& global_object)
{
    if (!m_execution_context_stack.is_empty()) {
        auto& context = running_execution_context();
        if (name == names.arguments.as_string() && context.function) {
            // HACK: Special handling for the name "arguments":
            //       If the name "arguments" is defined in the current scope, for example via
            //       a function parameter, or by a local var declaration, we use that.
            //       Otherwise, we return a lazily constructed Array with all the argument values.
            // FIXME: Do something much more spec-compliant.
            auto possible_match = lexical_environment()->get_from_environment(name);
            if (possible_match.has_value())
                return possible_match.value().value;
            if (!context.arguments_object) {
                if (context.function->is_strict_mode() || (is<ECMAScriptFunctionObject>(context.function) && !static_cast<ECMAScriptFunctionObject*>(context.function)->has_simple_parameter_list())) {
                    context.arguments_object = create_unmapped_arguments_object(global_object, context.arguments.span());
                } else {
                    context.arguments_object = create_mapped_arguments_object(global_object, *context.function, verify_cast<ECMAScriptFunctionObject>(context.function)->formal_parameters(), context.arguments.span(), *lexical_environment());
                }
            }
            return context.arguments_object;
        }

        for (auto* environment = lexical_environment(); environment; environment = environment->outer_environment()) {
            auto possible_match = environment->get_from_environment(name);
            if (exception())
                return {};
            if (possible_match.has_value())
                return possible_match.value().value;
            if (environment->has_binding(name))
                return environment->get_binding_value(global_object, name, false);
        }
    }

    if (!global_object.storage_has(name)) {
        if (m_underscore_is_last_value && name == "_")
            return m_last_value;
        return {};
    }
    return global_object.get(name);
}

// 9.1.2.1 GetIdentifierReference ( env, name, strict ), https://tc39.es/ecma262/#sec-getidentifierreference
Reference VM::get_identifier_reference(Environment* environment, FlyString name, bool strict)
{
    // 1. If env is the value null, then
    if (!environment) {
        // a. Return the Reference Record { [[Base]]: unresolvable, [[ReferencedName]]: name, [[Strict]]: strict, [[ThisValue]]: empty }.
        return Reference { Reference::BaseType::Unresolvable, move(name), strict };
    }

    // FIXME: The remainder of this function is non-conforming.

    for (; environment && environment->outer_environment(); environment = environment->outer_environment()) {
        auto possible_match = environment->get_from_environment(name);
        if (possible_match.has_value())
            return Reference { *environment, move(name), strict };
        if (environment->has_binding(name))
            return Reference { *environment, move(name), strict };
    }

    auto& global_environment = interpreter().realm().global_environment();
    if (global_environment.has_binding(name) || !in_strict_mode()) {
        return Reference { global_environment, move(name), strict };
    }

    return Reference { Reference::BaseType::Unresolvable, move(name), strict };
}

// 9.4.2 ResolveBinding ( name [ , env ] ), https://tc39.es/ecma262/#sec-resolvebinding
Reference VM::resolve_binding(FlyString const& name, Environment* environment)
{
    // 1. If env is not present or if env is undefined, then
    if (!environment) {
        // a. Set env to the running execution context's LexicalEnvironment.
        environment = running_execution_context().lexical_environment;
    }

    // 2. Assert: env is an Environment Record.
    VERIFY(environment);

    // 3. If the code matching the syntactic production that is being evaluated is contained in strict mode code, let strict be true; else let strict be false.
    bool strict = in_strict_mode();

    // 4. Return ? GetIdentifierReference(env, name, strict).
    return get_identifier_reference(environment, name, strict);
}

static void append_bound_and_passed_arguments(MarkedValueList& arguments, Vector<Value> bound_arguments, Optional<MarkedValueList> passed_arguments)
{
    arguments.ensure_capacity(bound_arguments.size());
    arguments.extend(move(bound_arguments));

    if (passed_arguments.has_value()) {
        auto arguments_list = move(passed_arguments.release_value().values());
        arguments.grow_capacity(arguments_list.size());
        arguments.extend(move(arguments_list));
    }
}

// 7.3.32 InitializeInstanceElements ( O, constructor ), https://tc39.es/ecma262/#sec-initializeinstanceelements
void VM::initialize_instance_elements(Object& object, ECMAScriptFunctionObject& constructor)
{
    for (auto& field : constructor.fields()) {
        field.define_field(*this, object);
        if (exception())
            return;
    }
}

// FIXME: This function should not exist as-is, most of it should be moved to the individual
//        [[Construct]] implementations so that this becomes the Construct() AO (3 steps).
Value VM::construct(FunctionObject& function, FunctionObject& new_target, Optional<MarkedValueList> arguments)
{
    auto& global_object = function.global_object();

    Value this_argument;
    if (!is<ECMAScriptFunctionObject>(function) || static_cast<ECMAScriptFunctionObject&>(function).constructor_kind() == ECMAScriptFunctionObject::ConstructorKind::Base)
        this_argument = TRY_OR_DISCARD(ordinary_create_from_constructor<Object>(global_object, new_target, &GlobalObject::object_prototype));

    // FIXME: prepare_for_ordinary_call() is not supposed to receive a BoundFunction, ProxyObject, etc. - ever.
    //        This needs to be moved to NativeFunction/ECMAScriptFunctionObject's construct() (10.2.2 [[Construct]])
    ExecutionContext callee_context(heap());
    prepare_for_ordinary_call(function, callee_context, &new_target);
    if (exception())
        return {};

    ArmedScopeGuard pop_guard = [&] {
        pop_execution_context();
    };

    if (auto* interpreter = interpreter_if_exists())
        callee_context.current_node = interpreter->current_node();

    append_bound_and_passed_arguments(callee_context.arguments, function.bound_arguments(), move(arguments));

    if (auto* environment = callee_context.lexical_environment) {
        auto& function_environment = verify_cast<FunctionEnvironment>(*environment);
        function_environment.set_new_target(&new_target);
        if (!this_argument.is_empty() && function_environment.this_binding_status() != FunctionEnvironment::ThisBindingStatus::Lexical) {
            function_environment.bind_this_value(global_object, this_argument);
            if (exception())
                return {};
        }
    }

    // If we are a Derived constructor, |this| has not been constructed before super is called.
    callee_context.this_value = this_argument;

    if (is<ECMAScriptFunctionObject>(function) && static_cast<ECMAScriptFunctionObject&>(function).constructor_kind() == ECMAScriptFunctionObject::ConstructorKind::Base) {
        VERIFY(this_argument.is_object());
        initialize_instance_elements(this_argument.as_object(), static_cast<ECMAScriptFunctionObject&>(function));
        if (exception())
            return {};
    }

    auto result = function.construct(new_target);

    pop_execution_context();
    pop_guard.disarm();

    // If we are constructing an instance of a derived class,
    // set the prototype on objects created by constructors that return an object (i.e. NativeFunction subclasses).

    if ((!is<ECMAScriptFunctionObject>(function) || static_cast<ECMAScriptFunctionObject&>(function).constructor_kind() == ECMAScriptFunctionObject::ConstructorKind::Base)
        && is<ECMAScriptFunctionObject>(new_target) && static_cast<ECMAScriptFunctionObject&>(new_target).constructor_kind() == ECMAScriptFunctionObject::ConstructorKind::Derived
        && result.is_object()) {
        if (auto* environment = callee_context.lexical_environment)
            verify_cast<FunctionEnvironment>(environment)->replace_this_binding(result);
        auto prototype = new_target.get(names.prototype);
        if (exception())
            return {};
        if (prototype.is_object()) {
            result.as_object().internal_set_prototype_of(&prototype.as_object());
            if (exception())
                return {};
        }
        return result;
    }

    if (exception())
        return {};

    if (result.is_object())
        return result;

    if (auto* environment = callee_context.lexical_environment)
        return environment->get_this_binding(global_object);
    return this_argument;
}

void VM::throw_exception(Exception& exception)
{
    set_exception(exception);
    unwind(ScopeType::Try);
}

// 9.4.4 ResolveThisBinding ( ), https://tc39.es/ecma262/#sec-resolvethisbinding
Value VM::resolve_this_binding(GlobalObject& global_object)
{
    auto& environment = get_this_environment(*this);
    return environment.get_this_binding(global_object);
}

String VM::join_arguments(size_t start_index) const
{
    StringBuilder joined_arguments;
    for (size_t i = start_index; i < argument_count(); ++i) {
        joined_arguments.append(argument(i).to_string_without_side_effects().characters());
        if (i != argument_count() - 1)
            joined_arguments.append(' ');
    }
    return joined_arguments.build();
}

Value VM::get_new_target()
{
    auto& env = get_this_environment(*this);
    return verify_cast<FunctionEnvironment>(env).new_target();
}

// 10.2.1.1 PrepareForOrdinaryCall ( F, newTarget ), https://tc39.es/ecma262/#sec-prepareforordinarycall
void VM::prepare_for_ordinary_call(FunctionObject& function, ExecutionContext& callee_context, [[maybe_unused]] Object* new_target)
{
    // NOTE: This is a LibJS specific hack for NativeFunction to inherit the strictness of its caller.
    // FIXME: I feel like we should be able to get rid of this.
    if (is<NativeFunction>(function))
        callee_context.is_strict_mode = in_strict_mode();
    else
        callee_context.is_strict_mode = function.is_strict_mode();

    // 1. Let callerContext be the running execution context.
    // 2. Let calleeContext be a new ECMAScript code execution context.

    // NOTE: In the specification, PrepareForOrdinaryCall "returns" a new callee execution context.
    // To avoid heap allocations, we put our ExecutionContext objects on the C++ stack instead.
    // Whoever calls us should put an ExecutionContext on their stack and pass that as the `callee_context`.

    // 3. Set the Function of calleeContext to F.
    callee_context.function = &function;
    callee_context.function_name = function.name();

    // 4. Let calleeRealm be F.[[Realm]].
    auto* callee_realm = function.realm();
    // FIXME: See FIXME in VM::call_internal() / VM::construct().
    if (!callee_realm)
        callee_realm = current_realm();
    VERIFY(callee_realm);

    // 5. Set the Realm of calleeContext to calleeRealm.
    callee_context.realm = callee_realm;

    // 6. Set the ScriptOrModule of calleeContext to F.[[ScriptOrModule]].
    // FIXME: Our execution context struct currently does not track this item.

    // 7. Let localEnv be NewFunctionEnvironment(F, newTarget).
    // FIXME: This should call NewFunctionEnvironment instead of the ad-hoc FunctionObject::create_environment()
    auto* local_environment = function.create_environment(function);

    // 8. Set the LexicalEnvironment of calleeContext to localEnv.
    callee_context.lexical_environment = local_environment;

    // 9. Set the VariableEnvironment of calleeContext to localEnv.
    callee_context.variable_environment = local_environment;

    // 10. Set the PrivateEnvironment of calleeContext to F.[[PrivateEnvironment]].
    // FIXME: We currently don't support private environments.

    // 11. If callerContext is not already suspended, suspend callerContext.
    // FIXME: We don't have this concept yet.

    // 12. Push calleeContext onto the execution context stack; calleeContext is now the running execution context.
    push_execution_context(callee_context, function.global_object());

    // 13. NOTE: Any exception objects produced after this point are associated with calleeRealm.
    // 14. Return calleeContext. (See NOTE above about how contexts are allocated on the C++ stack.)
}

// 10.2.1.2 OrdinaryCallBindThis ( F, calleeContext, thisArgument ), https://tc39.es/ecma262/#sec-ordinarycallbindthis
void VM::ordinary_call_bind_this(FunctionObject& function, ExecutionContext& callee_context, Value this_argument)
{
    auto* callee_realm = function.realm();

    auto* local_environment = callee_context.lexical_environment;
    auto& function_environment = verify_cast<FunctionEnvironment>(*local_environment);

    // This almost as the spec describes it however we sometimes don't have callee_realm when dealing
    // with proxies and arrow functions however this does seemingly achieve spec like behavior.
    if (!callee_realm || (is<ECMAScriptFunctionObject>(function) && static_cast<ECMAScriptFunctionObject&>(function).this_mode() == ECMAScriptFunctionObject::ThisMode::Lexical)) {
        return;
    }

    Value this_value;
    if (function.is_strict_mode()) {
        this_value = this_argument;
    } else if (this_argument.is_nullish()) {
        auto& global_environment = callee_realm->global_environment();
        this_value = &global_environment.global_this_value();
    } else {
        this_value = this_argument.to_object(function.global_object());
    }

    function_environment.bind_this_value(function.global_object(), this_value);
    callee_context.this_value = this_value;
}

ThrowCompletionOr<Value> VM::call_internal(FunctionObject& function, Value this_value, Optional<MarkedValueList> arguments)
{
    VERIFY(!exception());
    VERIFY(!this_value.is_empty());

    if (is<BoundFunction>(function)) {
        auto& bound_function = static_cast<BoundFunction&>(function);

        MarkedValueList with_bound_arguments { heap() };
        append_bound_and_passed_arguments(with_bound_arguments, bound_function.bound_arguments(), move(arguments));

        return call_internal(bound_function.target_function(), bound_function.bound_this(), move(with_bound_arguments));
    }

    // FIXME: prepare_for_ordinary_call() is not supposed to receive a BoundFunction, ProxyObject, etc. - ever.
    //        This needs to be moved to NativeFunction/ECMAScriptFunctionObject's construct() (10.2.2 [[Construct]])
    ExecutionContext callee_context(heap());
    prepare_for_ordinary_call(function, callee_context, nullptr);
    if (auto* exception = this->exception())
        return JS::throw_completion(exception->value());

    ScopeGuard pop_guard = [&] {
        pop_execution_context();
    };

    if (auto* interpreter = interpreter_if_exists())
        callee_context.current_node = interpreter->current_node();

    callee_context.this_value = function.bound_this().value_or(this_value);
    append_bound_and_passed_arguments(callee_context.arguments, function.bound_arguments(), move(arguments));

    if (callee_context.lexical_environment)
        ordinary_call_bind_this(function, callee_context, this_value);

    if (auto* exception = this->exception())
        return JS::throw_completion(exception->value());

    auto result = function.call();
    if (auto* exception = this->exception())
        return JS::throw_completion(exception->value());
    return result;
}

bool VM::in_strict_mode() const
{
    if (execution_context_stack().is_empty())
        return false;
    return running_execution_context().is_strict_mode;
}

void VM::run_queued_promise_jobs()
{
    dbgln_if(PROMISE_DEBUG, "Running queued promise jobs");
    // Temporarily get rid of the exception, if any - job functions must be called
    // either way, and that can't happen if we already have an exception stored.
    TemporaryClearException clear_exception(*this);
    while (!m_promise_jobs.is_empty()) {
        auto* job = m_promise_jobs.take_first();
        dbgln_if(PROMISE_DEBUG, "Calling promise job function @ {}", job);
        [[maybe_unused]] auto result = call(*job, js_undefined());
    }
    // Ensure no job has created a new exception, they must clean up after themselves.
    VERIFY(!m_exception);
}

// 9.5.4 HostEnqueuePromiseJob ( job, realm ), https://tc39.es/ecma262/#sec-hostenqueuepromisejob
void VM::enqueue_promise_job(NativeFunction& job)
{
    m_promise_jobs.append(&job);
}

void VM::run_queued_finalization_registry_cleanup_jobs()
{
    while (!m_finalization_registry_cleanup_jobs.is_empty()) {
        auto* registry = m_finalization_registry_cleanup_jobs.take_first();
        registry->cleanup();
    }
}

// 9.10.4.1 HostEnqueueFinalizationRegistryCleanupJob ( finalizationRegistry ), https://tc39.es/ecma262/#sec-host-cleanup-finalization-registry
void VM::enqueue_finalization_registry_cleanup_job(FinalizationRegistry& registry)
{
    m_finalization_registry_cleanup_jobs.append(&registry);
}

// 27.2.1.9 HostPromiseRejectionTracker ( promise, operation ), https://tc39.es/ecma262/#sec-host-promise-rejection-tracker
void VM::promise_rejection_tracker(const Promise& promise, Promise::RejectionOperation operation) const
{
    switch (operation) {
    case Promise::RejectionOperation::Reject:
        // A promise was rejected without any handlers
        if (on_promise_unhandled_rejection)
            on_promise_unhandled_rejection(promise);
        break;
    case Promise::RejectionOperation::Handle:
        // A handler was added to an already rejected promise
        if (on_promise_rejection_handled)
            on_promise_rejection_handled(promise);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void VM::dump_backtrace() const
{
    for (ssize_t i = m_execution_context_stack.size() - 1; i >= 0; --i) {
        auto& frame = m_execution_context_stack[i];
        if (frame->current_node) {
            auto& source_range = frame->current_node->source_range();
            dbgln("-> {} @ {}:{},{}", frame->function_name, source_range.filename, source_range.start.line, source_range.start.column);
        } else {
            dbgln("-> {}", frame->function_name);
        }
    }
}

void VM::dump_environment_chain() const
{
    for (auto* environment = lexical_environment(); environment; environment = environment->outer_environment()) {
        dbgln("+> {} ({:p})", environment->class_name(), environment);
        if (is<DeclarativeEnvironment>(*environment)) {
            auto& declarative_environment = static_cast<DeclarativeEnvironment const&>(*environment);
            for (auto& variable : declarative_environment.variables()) {
                dbgln("    {}", variable.key);
            }
        }
    }
}

VM::CustomData::~CustomData()
{
}

}
