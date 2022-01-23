/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021-2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FinalizationRegistry.h>
#include <LibJS/Runtime/FunctionEnvironment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PromiseReaction.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/TemporaryClearException.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/SourceTextModule.h>

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

    host_resolve_imported_module = [&](ScriptOrModule referencing_script_or_module, ModuleRequest const& specifier) {
        return resolve_imported_module(move(referencing_script_or_module), specifier);
    };

    host_import_module_dynamically = [&](ScriptOrModule, ModuleRequest const&, PromiseCapability promise_capability) {
        // By default, we throw on dynamic imports this is to prevent arbitrary file access by scripts.
        VERIFY(current_realm());
        auto& global_object = current_realm()->global_object();
        auto* promise = Promise::create(global_object);

        // If you are here because you want to enable dynamic module importing make sure it won't be a security problem
        // by checking the default implementation of HostImportModuleDynamically and creating your own hook or calling
        // vm.enable_default_host_import_module_dynamically_hook().
        promise->reject(Error::create(global_object, ErrorType::DynamicImportNotAllowed.message()));

        promise->perform_then(
            NativeFunction::create(global_object, "", [](auto&, auto&) -> ThrowCompletionOr<Value> {
                VERIFY_NOT_REACHED();
            }),
            NativeFunction::create(global_object, "", [reject = make_handle(promise_capability.reject)](auto& vm, auto& global_object) -> ThrowCompletionOr<Value> {
                auto error = vm.argument(0);

                // a. Perform ! Call(promiseCapability.[[Reject]], undefined, « error »).
                MUST(call(global_object, reject.cell(), js_undefined(), error));

                // b. Return undefined.
                return js_undefined();
            }),
            {});
    };

    host_finish_dynamic_import = [&](ScriptOrModule referencing_script_or_module, ModuleRequest const& specifier, PromiseCapability promise_capability, Promise* promise) {
        return finish_dynamic_import(move(referencing_script_or_module), specifier, promise_capability, promise);
    };

    host_get_import_meta_properties = [&](SourceTextModule const&) -> HashMap<PropertyKey, Value> {
        return {};
    };

    host_finalize_import_meta = [&](Object*, SourceTextModule const&) {
    };

#define __JS_ENUMERATE(SymbolName, snake_name) \
    m_well_known_symbol_##snake_name = js_symbol(*this, "Symbol." #SymbolName, false);
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE
}

VM::~VM()
{
}

void VM::enable_default_host_import_module_dynamically_hook()
{
    host_import_module_dynamically = [&](ScriptOrModule referencing_script_or_module, ModuleRequest const& specifier, PromiseCapability promise_capability) {
        return import_module_dynamically(move(referencing_script_or_module), specifier, promise_capability);
    };
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

    auto gather_roots_from_execution_context_stack = [&roots](Vector<ExecutionContext*> const& stack) {
        for (auto& execution_context : stack) {
            if (execution_context->this_value.is_cell())
                roots.set(&execution_context->this_value.as_cell());
            for (auto& argument : execution_context->arguments) {
                if (argument.is_cell())
                    roots.set(&argument.as_cell());
            }
            roots.set(execution_context->lexical_environment);
            roots.set(execution_context->variable_environment);
            roots.set(execution_context->private_environment);
        }
    };

    gather_roots_from_execution_context_stack(m_execution_context_stack);
    for (auto& saved_stack : m_saved_execution_context_stacks)
        gather_roots_from_execution_context_stack(saved_stack);

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

ThrowCompletionOr<Value> VM::named_evaluation_if_anonymous_function(GlobalObject& global_object, ASTNode const& expression, FlyString const& name)
{
    // 8.3.3 Static Semantics: IsAnonymousFunctionDefinition ( expr ), https://tc39.es/ecma262/#sec-isanonymousfunctiondefinition
    // And 8.3.5 Runtime Semantics: NamedEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-namedevaluation
    if (is<FunctionExpression>(expression)) {
        auto& function = static_cast<FunctionExpression const&>(expression);
        if (!function.has_name()) {
            return function.instantiate_ordinary_function_expression(interpreter(), global_object, name);
        }
    } else if (is<ClassExpression>(expression)) {
        auto& class_expression = static_cast<ClassExpression const&>(expression);
        if (!class_expression.has_name()) {
            return TRY(class_expression.class_definition_evaluation(interpreter(), global_object, {}, name));
        }
    }

    return TRY(expression.execute(interpreter(), global_object)).release_value();
}

// 13.15.5.2 Runtime Semantics: DestructuringAssignmentEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-destructuringassignmentevaluation
ThrowCompletionOr<void> VM::destructuring_assignment_evaluation(NonnullRefPtr<BindingPattern> const& target, Value value, GlobalObject& global_object)
{
    // Note: DestructuringAssignmentEvaluation is just like BindingInitialization without an environment
    //       And it allows member expressions. We thus trust the parser to disallow member expressions
    //       in any non assignment binding and just call BindingInitialization with a nullptr environment
    return binding_initialization(target, value, nullptr, global_object);
}

// 8.5.2 Runtime Semantics: BindingInitialization, https://tc39.es/ecma262/#sec-runtime-semantics-bindinginitialization
ThrowCompletionOr<void> VM::binding_initialization(FlyString const& target, Value value, Environment* environment, GlobalObject& global_object)
{
    // 1. Let name be StringValue of Identifier.
    // 2. Return ? InitializeBoundName(name, value, environment).
    return initialize_bound_name(global_object, target, value, environment);
}

// 8.5.2 Runtime Semantics: BindingInitialization, https://tc39.es/ecma262/#sec-runtime-semantics-bindinginitialization
ThrowCompletionOr<void> VM::binding_initialization(NonnullRefPtr<BindingPattern> const& target, Value value, Environment* environment, GlobalObject& global_object)
{
    // BindingPattern : ObjectBindingPattern
    if (target->kind == BindingPattern::Kind::Object) {
        // 1. Perform ? RequireObjectCoercible(value).
        TRY(require_object_coercible(global_object, value));

        // 2. Return the result of performing BindingInitialization of ObjectBindingPattern using value and environment as arguments.

        // BindingInitialization of ObjectBindingPattern
        // 1. Perform ? PropertyBindingInitialization of BindingPropertyList using value and environment as the arguments.
        TRY(property_binding_initialization(*target, value, environment, global_object));

        // 2. Return NormalCompletion(empty).
        return {};
    }
    // BindingPattern : ArrayBindingPattern
    else {
        // 1. Let iteratorRecord be ? GetIterator(value).
        auto iterator_record = TRY(get_iterator(global_object, value));

        // 2. Let result be IteratorBindingInitialization of ArrayBindingPattern with arguments iteratorRecord and environment.
        auto result = iterator_binding_initialization(*target, iterator_record, environment, global_object);

        // 3. If iteratorRecord.[[Done]] is false, return ? IteratorClose(iteratorRecord, result).
        if (!iterator_record.done) {
            // iterator_close() always returns a Completion, which ThrowCompletionOr will interpret as a throw
            // completion. So only return the result of iterator_close() if it is indeed a throw completion.
            auto completion = result.is_throw_completion() ? result.release_error() : normal_completion({});
            if (completion = iterator_close(global_object, iterator_record, move(completion)); completion.is_error())
                return completion.release_error();
        }

        // 4. Return result.
        return result;
    }
}

// 13.15.5.3 Runtime Semantics: PropertyDestructuringAssignmentEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-propertydestructuringassignmentevaluation
// 14.3.3.1 Runtime Semantics: PropertyBindingInitialization, https://tc39.es/ecma262/#sec-destructuring-binding-patterns-runtime-semantics-propertybindinginitialization
ThrowCompletionOr<void> VM::property_binding_initialization(BindingPattern const& binding, Value value, Environment* environment, GlobalObject& global_object)
{
    auto* object = TRY(value.to_object(global_object));

    HashTable<PropertyKey> seen_names;
    for (auto& property : binding.entries) {

        VERIFY(!property.is_elision());

        if (property.is_rest) {
            Reference assignment_target;
            if (auto identifier_ptr = property.name.get_pointer<NonnullRefPtr<Identifier>>()) {
                assignment_target = TRY(resolve_binding((*identifier_ptr)->string(), environment));
            } else if (auto member_ptr = property.alias.get_pointer<NonnullRefPtr<MemberExpression>>()) {
                assignment_target = TRY((*member_ptr)->to_reference(interpreter(), global_object));
            } else {
                VERIFY_NOT_REACHED();
            }

            auto* rest_object = Object::create(global_object, global_object.object_prototype());
            VERIFY(rest_object);

            TRY(rest_object->copy_data_properties(object, seen_names, global_object));
            if (!environment)
                return assignment_target.put_value(global_object, rest_object);
            else
                return assignment_target.initialize_referenced_binding(global_object, rest_object);
        }

        auto name = TRY(property.name.visit(
            [&](Empty) -> ThrowCompletionOr<PropertyKey> { VERIFY_NOT_REACHED(); },
            [&](NonnullRefPtr<Identifier> const& identifier) -> ThrowCompletionOr<PropertyKey> {
                return identifier->string();
            },
            [&](NonnullRefPtr<Expression> const& expression) -> ThrowCompletionOr<PropertyKey> {
                auto result = TRY(expression->execute(interpreter(), global_object)).release_value();
                return result.to_property_key(global_object);
            }));

        seen_names.set(name);

        if (property.name.has<NonnullRefPtr<Identifier>>() && property.alias.has<Empty>()) {
            // FIXME: this branch and not taking this have a lot in common we might want to unify it more (like it was before).
            auto& identifier = *property.name.get<NonnullRefPtr<Identifier>>();
            auto reference = TRY(resolve_binding(identifier.string(), environment));

            auto value_to_assign = TRY(object->get(name));
            if (property.initializer && value_to_assign.is_undefined()) {
                value_to_assign = TRY(named_evaluation_if_anonymous_function(global_object, *property.initializer, identifier.string()));
            }

            if (!environment)
                TRY(reference.put_value(global_object, value_to_assign));
            else
                TRY(reference.initialize_referenced_binding(global_object, value_to_assign));
            continue;
        }

        auto reference_to_assign_to = TRY(property.alias.visit(
            [&](Empty) -> ThrowCompletionOr<Optional<Reference>> { return Optional<Reference> {}; },
            [&](NonnullRefPtr<Identifier> const& identifier) -> ThrowCompletionOr<Optional<Reference>> {
                return TRY(resolve_binding(identifier->string(), environment));
            },
            [&](NonnullRefPtr<BindingPattern> const&) -> ThrowCompletionOr<Optional<Reference>> { return Optional<Reference> {}; },
            [&](NonnullRefPtr<MemberExpression> const& member_expression) -> ThrowCompletionOr<Optional<Reference>> {
                return TRY(member_expression->to_reference(interpreter(), global_object));
            }));

        auto value_to_assign = TRY(object->get(name));
        if (property.initializer && value_to_assign.is_undefined()) {
            if (auto* identifier_ptr = property.alias.get_pointer<NonnullRefPtr<Identifier>>())
                value_to_assign = TRY(named_evaluation_if_anonymous_function(global_object, *property.initializer, (*identifier_ptr)->string()));
            else
                value_to_assign = TRY(property.initializer->execute(interpreter(), global_object)).release_value();
        }

        if (auto* binding_ptr = property.alias.get_pointer<NonnullRefPtr<BindingPattern>>()) {
            TRY(binding_initialization(*binding_ptr, value_to_assign, environment, global_object));
        } else {
            VERIFY(reference_to_assign_to.has_value());
            if (!environment)
                TRY(reference_to_assign_to->put_value(global_object, value_to_assign));
            else
                TRY(reference_to_assign_to->initialize_referenced_binding(global_object, value_to_assign));
        }
    }

    return {};
}

// 13.15.5.5 Runtime Semantics: IteratorDestructuringAssignmentEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-iteratordestructuringassignmentevaluation
// 8.5.3 Runtime Semantics: IteratorBindingInitialization, https://tc39.es/ecma262/#sec-runtime-semantics-iteratorbindinginitialization
ThrowCompletionOr<void> VM::iterator_binding_initialization(BindingPattern const& binding, Iterator& iterator_record, Environment* environment, GlobalObject& global_object)
{
    // FIXME: this method is nearly identical to destructuring assignment!
    for (size_t i = 0; i < binding.entries.size(); i++) {
        auto& entry = binding.entries[i];
        Value value;

        auto assignment_target = TRY(entry.alias.visit(
            [&](Empty) -> ThrowCompletionOr<Optional<Reference>> { return Optional<Reference> {}; },
            [&](NonnullRefPtr<Identifier> const& identifier) -> ThrowCompletionOr<Optional<Reference>> {
                return TRY(resolve_binding(identifier->string(), environment));
            },
            [&](NonnullRefPtr<BindingPattern> const&) -> ThrowCompletionOr<Optional<Reference>> { return Optional<Reference> {}; },
            [&](NonnullRefPtr<MemberExpression> const& member_expression) -> ThrowCompletionOr<Optional<Reference>> {
                return TRY(member_expression->to_reference(interpreter(), global_object));
            }));

        // BindingRestElement : ... BindingIdentifier
        // BindingRestElement : ... BindingPattern
        if (entry.is_rest) {
            VERIFY(i == binding.entries.size() - 1);

            // 2. Let A be ! ArrayCreate(0).
            auto* array = MUST(Array::create(global_object, 0));

            // 3. Let n be 0.
            // 4. Repeat,
            while (true) {
                ThrowCompletionOr<Object*> next { nullptr };

                // a. If iteratorRecord.[[Done]] is false, then
                if (!iterator_record.done) {
                    // i. Let next be IteratorStep(iteratorRecord).
                    next = iterator_step(global_object, iterator_record);

                    // ii. If next is an abrupt completion, set iteratorRecord.[[Done]] to true.
                    // iii. ReturnIfAbrupt(next).
                    if (next.is_error()) {
                        iterator_record.done = true;
                        return next.release_error();
                    }

                    // iv. If next is false, set iteratorRecord.[[Done]] to true.
                    if (!next.value())
                        iterator_record.done = true;
                }

                // b. If iteratorRecord.[[Done]] is true, then
                if (iterator_record.done) {
                    // NOTE: Step i. and ii. are handled below.
                    break;
                }

                // c. Let nextValue be IteratorValue(next).
                auto next_value = iterator_value(global_object, *next.value());

                // d. If nextValue is an abrupt completion, set iteratorRecord.[[Done]] to true.
                // e. ReturnIfAbrupt(nextValue).
                if (next_value.is_error()) {
                    iterator_record.done = true;
                    return next_value.release_error();
                }

                // f. Perform ! CreateDataPropertyOrThrow(A, ! ToString(𝔽(n)), nextValue).
                array->indexed_properties().append(next_value.value());

                // g. Set n to n + 1.
            }
            value = array;
        }
        // SingleNameBinding : BindingIdentifier Initializer[opt]
        // BindingElement : BindingPattern Initializer[opt]
        else {
            // 1. Let v be undefined.
            value = js_undefined();

            // 2. If iteratorRecord.[[Done]] is false, then
            if (!iterator_record.done) {
                // a. Let next be IteratorStep(iteratorRecord).
                auto next = iterator_step(global_object, iterator_record);

                // b. If next is an abrupt completion, set iteratorRecord.[[Done]] to true.
                // c. ReturnIfAbrupt(next).
                if (next.is_error()) {
                    iterator_record.done = true;
                    return next.release_error();
                }

                // d. If next is false, set iteratorRecord.[[Done]] to true.
                if (!next.value()) {
                    iterator_record.done = true;
                }
                // e. Else,
                else {
                    // i. Set v to IteratorValue(next).
                    auto value_or_error = iterator_value(global_object, *next.value());

                    // ii. If v is an abrupt completion, set iteratorRecord.[[Done]] to true.
                    // iii. ReturnIfAbrupt(v).
                    if (value_or_error.is_throw_completion()) {
                        iterator_record.done = true;
                        return value_or_error.release_error();
                    }
                    value = value_or_error.release_value();
                }
            }

            // NOTE: Step 3. and 4. are handled below.
        }

        if (value.is_undefined() && entry.initializer) {
            VERIFY(!entry.is_rest);
            if (auto* identifier_ptr = entry.alias.get_pointer<NonnullRefPtr<Identifier>>())
                value = TRY(named_evaluation_if_anonymous_function(global_object, *entry.initializer, (*identifier_ptr)->string()));
            else
                value = TRY(entry.initializer->execute(interpreter(), global_object)).release_value();
        }

        if (auto* binding_ptr = entry.alias.get_pointer<NonnullRefPtr<BindingPattern>>()) {
            TRY(binding_initialization(*binding_ptr, value, environment, global_object));
        } else if (!entry.alias.has<Empty>()) {
            VERIFY(assignment_target.has_value());
            if (!environment)
                TRY(assignment_target->put_value(global_object, value));
            else
                TRY(assignment_target->initialize_referenced_binding(global_object, value));
        }
    }

    return {};
}

// 9.1.2.1 GetIdentifierReference ( env, name, strict ), https://tc39.es/ecma262/#sec-getidentifierreference
ThrowCompletionOr<Reference> VM::get_identifier_reference(Environment* environment, FlyString name, bool strict, size_t hops)
{
    // 1. If env is the value null, then
    if (!environment) {
        // a. Return the Reference Record { [[Base]]: unresolvable, [[ReferencedName]]: name, [[Strict]]: strict, [[ThisValue]]: empty }.
        return Reference { Reference::BaseType::Unresolvable, move(name), strict };
    }

    // 2. Let exists be ? env.HasBinding(name).
    Optional<size_t> index;
    auto exists = TRY(environment->has_binding(name, &index));

    // Note: This is an optimization for looking up the same reference.
    Optional<EnvironmentCoordinate> environment_coordinate;
    if (index.has_value())
        environment_coordinate = EnvironmentCoordinate { .hops = hops, .index = index.value() };

    // 3. If exists is true, then
    if (exists) {
        // a. Return the Reference Record { [[Base]]: env, [[ReferencedName]]: name, [[Strict]]: strict, [[ThisValue]]: empty }.
        return Reference { *environment, move(name), strict, environment_coordinate };
    }
    // 4. Else,
    else {
        // a. Let outer be env.[[OuterEnv]].
        // b. Return ? GetIdentifierReference(outer, name, strict).
        return get_identifier_reference(environment->outer_environment(), move(name), strict, hops + 1);
    }
}

// 9.4.2 ResolveBinding ( name [ , env ] ), https://tc39.es/ecma262/#sec-resolvebinding
ThrowCompletionOr<Reference> VM::resolve_binding(FlyString const& name, Environment* environment)
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

    // NOTE: The spec says:
    //       Note: The result of ResolveBinding is always a Reference Record whose [[ReferencedName]] field is name.
    //       But this is not actually correct as GetIdentifierReference (or really the methods it calls) can throw.
}

// 7.3.33 InitializeInstanceElements ( O, constructor ), https://tc39.es/ecma262/#sec-initializeinstanceelements
ThrowCompletionOr<void> VM::initialize_instance_elements(Object& object, ECMAScriptFunctionObject& constructor)
{
    for (auto& method : constructor.private_methods())
        TRY(object.private_method_or_accessor_add(method));

    for (auto& field : constructor.fields())
        TRY(object.define_field(field.name, field.initializer));
    return {};
}

void VM::throw_exception(Exception& exception)
{
    set_exception(exception);
}

// 9.4.4 ResolveThisBinding ( ), https://tc39.es/ecma262/#sec-resolvethisbinding
ThrowCompletionOr<Value> VM::resolve_this_binding(GlobalObject& global_object)
{
    // 1. Let envRec be GetThisEnvironment().
    auto& environment = get_this_environment(*this);
    // 2. Return ? envRec.GetThisBinding().
    return TRY(environment.get_this_binding(global_object));
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

// 9.4.5 GetNewTarget ( ), https://tc39.es/ecma262/#sec-getnewtarget
Value VM::get_new_target()
{
    // 1. Let envRec be GetThisEnvironment().
    auto& env = get_this_environment(*this);

    // 2. Assert: envRec has a [[NewTarget]] field.
    // 3. Return envRec.[[NewTarget]].
    return verify_cast<FunctionEnvironment>(env).new_target();
}

// NOTE: This is only here because there's a million invocations of vm.call() - it used to be tied to the VM in weird ways.
// We should update all of those and then remove this, along with the call() template functions in VM.h, and use the standalone call() AO.
ThrowCompletionOr<Value> VM::call_internal(FunctionObject& function, Value this_value, Optional<MarkedValueList> arguments)
{
    VERIFY(!exception());
    VERIFY(!this_value.is_empty());

    return JS::call_impl(function.global_object(), &function, this_value, move(arguments));
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
    TemporaryClearException temporary_clear_exception(*this);
    while (!m_promise_jobs.is_empty()) {
        auto* job = m_promise_jobs.take_first();
        dbgln_if(PROMISE_DEBUG, "Calling promise job function @ {}", job);

        // NOTE: If the execution context stack is empty, we make and push a temporary context.
        ExecutionContext execution_context(heap());
        bool pushed_execution_context = false;
        if (m_execution_context_stack.is_empty()) {
            static FlyString promise_execution_context_name = "(promise execution context)";
            execution_context.function_name = promise_execution_context_name;
            // FIXME: Propagate potential failure
            MUST(push_execution_context(execution_context, job->global_object()));
            pushed_execution_context = true;
        }

        [[maybe_unused]] auto result = call(job->global_object(), *job, js_undefined());

        // This doesn't match the spec, it actually defines that Job Abstract Closures must return
        // a normal completion. In reality that's not the case however, and all major engines clear
        // exceptions when running Promise jobs. See the commit where these two lines were initially
        // added for a much more detailed explanation.
        clear_exception();

        if (pushed_execution_context)
            pop_execution_context();
    }
    // Ensure no job has created a new exception, they must clean up after themselves.
    // If they don't, we help a little (see above) so that this assumption remains valid.
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

VM::CustomData::~CustomData()
{
}

void VM::save_execution_context_stack()
{
    m_saved_execution_context_stacks.append(move(m_execution_context_stack));
}

void VM::restore_execution_context_stack()
{
    m_execution_context_stack = m_saved_execution_context_stacks.take_last();
}

// 9.4.1 GetActiveScriptOrModule ( ), https://tc39.es/ecma262/#sec-getactivescriptormodule
ScriptOrModule VM::get_active_script_or_module() const
{
    // 1. If the execution context stack is empty, return null.
    if (m_execution_context_stack.is_empty())
        return Empty {};

    // 2. Let ec be the topmost execution context on the execution context stack whose ScriptOrModule component is not null.
    for (auto i = m_execution_context_stack.size() - 1; i > 0; i--) {
        if (!m_execution_context_stack[i]->script_or_module.has<Empty>())
            return m_execution_context_stack[i]->script_or_module;
    }

    // 3. If no such execution context exists, return null. Otherwise, return ec's ScriptOrModule.
    // Note: Since it is not empty we have 0 and since we got here all the
    //       above contexts don't have a non-null ScriptOrModule
    return m_execution_context_stack[0]->script_or_module;
}

VM::StoredModule* VM::get_stored_module(ScriptOrModule const&, String const& filepath)
{
    // Note the spec says:
    // Each time this operation is called with a specific referencingScriptOrModule, specifier pair as arguments
    // it must return the same Module Record instance if it completes normally.
    // Currently, we ignore the referencing script or module but this might not be correct in all cases.
    auto end_or_module = m_loaded_modules.find_if([&](StoredModule const& stored_module) {
        return stored_module.filepath == filepath;
    });
    if (end_or_module.is_end())
        return nullptr;
    return &(*end_or_module);
}

ThrowCompletionOr<void> VM::link_and_eval_module(Badge<Interpreter>, SourceTextModule& module)
{
    return link_and_eval_module(module);
}

ThrowCompletionOr<void> VM::link_and_eval_module(SourceTextModule& module)
{
    auto filepath = module.filename();

    auto module_or_end = m_loaded_modules.find_if([&](StoredModule const& stored_module) {
        return stored_module.module.ptr() == &module;
    });

    StoredModule* stored_module;

    if (module_or_end.is_end()) {
        dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] Warning introducing module via link_and_eval_module {}", module.filename());
        if (m_loaded_modules.size() > 0) {
            dbgln("Using link_and_eval module as entry point is not allowed if it is not the first module!");
            VERIFY_NOT_REACHED();
        }
        m_loaded_modules.empend(
            &module,
            module.filename(),
            module,
            true);
        stored_module = &m_loaded_modules.last();
    } else {
        stored_module = module_or_end.operator->();
        if (stored_module->has_once_started_linking) {
            dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] Module already has started linking once {}", module.filename());
            return {};
        }
        stored_module->has_once_started_linking = true;
    }

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] Linking module {}", filepath);
    auto linked_or_error = module.link(*this);
    if (linked_or_error.is_error())
        return linked_or_error.throw_completion();

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] Linking passed, now evaluating module {}", filepath);
    auto evaluated_or_error = module.evaluate(*this);

    VERIFY(!exception());

    if (evaluated_or_error.is_error())
        return evaluated_or_error.throw_completion();

    auto* evaluated_value = evaluated_or_error.value();

    run_queued_promise_jobs();
    VERIFY(m_promise_jobs.is_empty());

    // FIXME: This will break if we start doing promises actually asynchronously.
    VERIFY(evaluated_value->state() != Promise::State::Pending);

    if (evaluated_value->state() == Promise::State::Rejected) {
        VERIFY(!exception());
        return JS::throw_completion(evaluated_value->result());
    }

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] Evaluating passed for module {}", module.filename());
    return {};
}

// 16.2.1.7 HostResolveImportedModule ( referencingScriptOrModule, specifier ), https://tc39.es/ecma262/#sec-hostresolveimportedmodule
ThrowCompletionOr<NonnullRefPtr<Module>> VM::resolve_imported_module(ScriptOrModule referencing_script_or_module, ModuleRequest const& specifier)
{
    if (!specifier.assertions.is_empty())
        return throw_completion<InternalError>(current_realm()->global_object(), ErrorType::NotImplemented, "HostResolveImportedModule with assertions");

    // An implementation of HostResolveImportedModule must conform to the following requirements:
    //  - If it completes normally, the [[Value]] slot of the completion must contain an instance of a concrete subclass of Module Record.
    //  - If a Module Record corresponding to the pair referencingScriptOrModule, specifier does not exist or cannot be created, an exception must be thrown.
    //  - Each time this operation is called with a specific referencingScriptOrModule, specifier pair as arguments it must return the same Module Record instance if it completes normally.

    StringView base_filename = referencing_script_or_module.visit(
        [&](Empty) {
            return "."sv;
        },
        [&](auto* script_or_module) {
            return script_or_module->filename();
        });

    LexicalPath base_path { base_filename };
    auto filepath = LexicalPath::absolute_path(base_path.dirname(), specifier.module_specifier);

#if JS_MODULE_DEBUG
    String referencing_module_string = referencing_script_or_module.visit(
        [&](Empty) -> String {
            return ".";
        },
        [&](auto* script_or_module) {
            if constexpr (IsSame<Script*, decltype(script_or_module)>) {
                return String::formatted("Script @ {}", script_or_module);
            }
            return String::formatted("Module @ {}", script_or_module);
        });

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] resolve_imported_module({}, {})", referencing_module_string, filepath);
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE]     resolved {} + {} -> {}", base_path, specifier.module_specifier, filepath);
#endif

    auto* loaded_module_or_end = get_stored_module(referencing_script_or_module, filepath);
    if (loaded_module_or_end != nullptr) {
        dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] resolve_imported_module({}) already loaded at {}", filepath, loaded_module_or_end->module.ptr());
        return loaded_module_or_end->module;
    }

    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] reading and parsing module {}", filepath);

    auto& global_object = current_realm()->global_object();

    auto file_or_error = Core::File::open(filepath, Core::OpenMode::ReadOnly);

    if (file_or_error.is_error()) {
        return throw_completion<SyntaxError>(global_object, ErrorType::ModuleNotFound, specifier.module_specifier);
    }

    // FIXME: Don't read the file in one go.
    auto file_content = file_or_error.value()->read_all();
    StringView content_view { file_content.data(), file_content.size() };

    // Note: We treat all files as module, so if a script does not have exports it just runs it.
    auto module_or_errors = SourceTextModule::parse(content_view, *current_realm(), filepath);

    if (module_or_errors.is_error()) {
        VERIFY(module_or_errors.error().size() > 0);
        return throw_completion<SyntaxError>(global_object, module_or_errors.error().first().to_string());
    }

    auto module = module_or_errors.release_value();
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] resolve_imported_module(...) parsed {} to {}", filepath, module.ptr());

    // We have to set it here already in case it references itself.
    m_loaded_modules.empend(
        referencing_script_or_module,
        filepath,
        module,
        false);

    return module;
}

// 16.2.1.8 HostImportModuleDynamically ( referencingScriptOrModule, specifier, promiseCapability ), https://tc39.es/ecma262/#sec-hostimportmoduledynamically
void VM::import_module_dynamically(ScriptOrModule referencing_script_or_module, ModuleRequest const& specifier, PromiseCapability promise_capability)
{
    auto& global_object = current_realm()->global_object();

    // Success path:
    //  - At some future time, the host environment must perform FinishDynamicImport(referencingScriptOrModule, specifier, promiseCapability, promise),
    //    where promise is a Promise resolved with undefined.
    //  - Any subsequent call to HostResolveImportedModule after FinishDynamicImport has completed,
    //    given the arguments referencingScriptOrModule and specifier, must complete normally.
    //  - The completion value of any subsequent call to HostResolveImportedModule after FinishDynamicImport has completed,
    //    given the arguments referencingScriptOrModule and specifier, must be a module which has already been evaluated,
    //    i.e. whose Evaluate concrete method has already been called and returned a normal completion.
    // Failure path:
    //  - At some future time, the host environment must perform
    //    FinishDynamicImport(referencingScriptOrModule, specifier, promiseCapability, promise),
    //    where promise is a Promise rejected with an error representing the cause of failure.

    auto* promise = Promise::create(global_object);

    ScopeGuard finish_dynamic_import = [&] {
        host_finish_dynamic_import(referencing_script_or_module, specifier, promise_capability, promise);
    };

    // Generally within ECMA262 we always get a referencing_script_or_moulde. However, ShadowRealm gives an explicit null.
    // To get around this is we attempt to get the active script_or_module otherwise we might start loading "random" files from the working directory.
    if (referencing_script_or_module.has<Empty>()) {
        referencing_script_or_module = get_active_script_or_module();

        // If there is no ScriptOrModule in any of the execution contexts
        if (referencing_script_or_module.has<Empty>()) {
            // Throw an error for now
            promise->reject(InternalError::create(global_object, String::formatted(ErrorType::ModuleNotFoundNoReferencingScript.message(), specifier.module_specifier)));
            return;
        }
    }

    VERIFY(!exception());
    // Note: If host_resolve_imported_module returns a module it has been loaded successfully and the next call in finish_dynamic_import will retrieve it again.
    auto module_or_error = host_resolve_imported_module(referencing_script_or_module, specifier);
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] HostImportModuleDynamically(..., {}) -> {}", specifier.module_specifier, module_or_error.is_error() ? "failed" : "passed");
    if (module_or_error.is_throw_completion()) {
        // Note: We should not leak the exception thrown in host_resolve_imported_module.
        clear_exception();
        promise->reject(*module_or_error.throw_completion().value());
    } else {
        // Note: If you are here because this VERIFY is failing overwrite host_import_module_dynamically
        //       because this is LibJS internal logic which won't always work
        auto module = module_or_error.release_value();
        VERIFY(is<SourceTextModule>(*module));
        auto& source_text_module = static_cast<SourceTextModule&>(*module);

        auto evaluated_or_error = link_and_eval_module(source_text_module);

        if (evaluated_or_error.is_throw_completion()) {
            // Note: Again we don't want to leak the exception from link_and_eval_module.
            clear_exception();
            promise->reject(*evaluated_or_error.throw_completion().value());
        } else {
            VERIFY(!exception());
            promise->fulfill(js_undefined());
        }
    }

    // It must return NormalCompletion(undefined).
    // Note: Just return void always since the resulting value cannot be accessed by user code.
}

// 16.2.1.9 FinishDynamicImport ( referencingScriptOrModule, specifier, promiseCapability, innerPromise ), https://tc39.es/ecma262/#sec-finishdynamicimport
void VM::finish_dynamic_import(ScriptOrModule referencing_script_or_module, ModuleRequest const& specifier, PromiseCapability promise_capability, Promise* inner_promise)
{
    dbgln_if(JS_MODULE_DEBUG, "[JS MODULE] finish_dynamic_import on {}", specifier.module_specifier);

    // 1. Let fulfilledClosure be a new Abstract Closure with parameters (result) that captures referencingScriptOrModule, specifier, and promiseCapability and performs the following steps when called:
    auto fulfilled_closure = [referencing_script_or_module, specifier, promise_capability](VM& vm, GlobalObject& global_object) -> ThrowCompletionOr<Value> {
        auto result = vm.argument(0);
        // a. Assert: result is undefined.
        VERIFY(result.is_undefined());
        // b. Let moduleRecord be ! HostResolveImportedModule(referencingScriptOrModule, specifier).
        auto module_record = MUST(vm.host_resolve_imported_module(referencing_script_or_module, specifier));

        // c. Assert: Evaluate has already been invoked on moduleRecord and successfully completed.
        // Note: If HostResolveImportedModule returns a module evaluate will have been called on it.

        // d. Let namespace be GetModuleNamespace(moduleRecord).
        auto namespace_ = module_record->get_module_namespace(vm);

        VERIFY(!vm.exception());
        // e. If namespace is an abrupt completion, then
        if (namespace_.is_throw_completion()) {
            // i. Perform ! Call(promiseCapability.[[Reject]], undefined, « namespace.[[Value]] »).
            MUST(call(global_object, promise_capability.reject, js_undefined(), *namespace_.throw_completion().value()));
        }
        // f. Else,
        else {
            // i. Perform ! Call(promiseCapability.[[Resolve]], undefined, « namespace.[[Value]] »).
            MUST(call(global_object, promise_capability.resolve, js_undefined(), namespace_.release_value()));
        }
        // g. Return undefined.
        return js_undefined();
    };

    // 2. Let onFulfilled be ! CreateBuiltinFunction(fulfilledClosure, 0, "", « »).
    auto* on_fulfilled = NativeFunction::create(current_realm()->global_object(), "", move(fulfilled_closure));

    // 3. Let rejectedClosure be a new Abstract Closure with parameters (error) that captures promiseCapability and performs the following steps when called:
    auto rejected_closure = [promise_capability](VM& vm, GlobalObject& global_object) -> ThrowCompletionOr<Value> {
        auto error = vm.argument(0);
        // a. Perform ! Call(promiseCapability.[[Reject]], undefined, « error »).
        MUST(call(global_object, promise_capability.reject, js_undefined(), error));
        // b. Return undefined.
        return js_undefined();
    };

    // 4. Let onRejected be ! CreateBuiltinFunction(rejectedClosure, 0, "", « »).
    auto* on_rejected = NativeFunction::create(current_realm()->global_object(), "", move(rejected_closure));

    // 5. Perform ! PerformPromiseThen(innerPromise, onFulfilled, onRejected).
    inner_promise->perform_then(on_fulfilled, on_rejected, {});

    VERIFY(!exception());
}

}
