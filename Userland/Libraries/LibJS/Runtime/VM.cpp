/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
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

    auto value = expression.execute(interpreter(), global_object);
    if (auto* thrown_exception = exception())
        return JS::throw_completion(thrown_exception->value());
    return value;
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
    if (environment) {
        MUST(environment->initialize_binding(global_object, target, value));
        return {};
    }
    auto reference = resolve_binding(target);
    return reference.put_value(global_object, value);
}

// 8.5.2 Runtime Semantics: BindingInitialization, https://tc39.es/ecma262/#sec-runtime-semantics-bindinginitialization
ThrowCompletionOr<void> VM::binding_initialization(NonnullRefPtr<BindingPattern> const& target, Value value, Environment* environment, GlobalObject& global_object)
{
    if (target->kind == BindingPattern::Kind::Object) {
        TRY(require_object_coercible(global_object, value));
        TRY(property_binding_initialization(*target, value, environment, global_object));
        return {};
    } else {
        auto* iterator = TRY(get_iterator(global_object, value));
        auto iterator_done = false;

        auto result = iterator_binding_initialization(*target, iterator, iterator_done, environment, global_object);

        if (!iterator_done) {
            // iterator_close() always returns a Completion, which ThrowCompletionOr will interpret as a throw
            // completion. So only return the result of iterator_close() if it is indeed a throw completion.
            auto completion = result.is_throw_completion() ? result.release_error() : normal_completion({});
            if (completion = iterator_close(*iterator, move(completion)); completion.is_error())
                return completion.release_error();
        }

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
                assignment_target = resolve_binding((*identifier_ptr)->string(), environment);
            } else if (auto member_ptr = property.alias.get_pointer<NonnullRefPtr<MemberExpression>>()) {
                assignment_target = (*member_ptr)->to_reference(interpreter(), global_object);
            } else {
                VERIFY_NOT_REACHED();
            }

            if (auto* thrown_exception = exception())
                return JS::throw_completion(thrown_exception->value());

            auto* rest_object = Object::create(global_object, global_object.object_prototype());
            VERIFY(rest_object);

            TRY(rest_object->copy_data_properties(object, seen_names, global_object));
            if (!environment)
                return assignment_target.put_value(global_object, rest_object);
            else
                return assignment_target.initialize_referenced_binding(global_object, rest_object);
        }

        PropertyKey name;

        property.name.visit(
            [&](Empty) { VERIFY_NOT_REACHED(); },
            [&](NonnullRefPtr<Identifier> const& identifier) {
                name = identifier->string();
            },
            [&](NonnullRefPtr<Expression> const& expression) {
                auto result = expression->execute(interpreter(), global_object);
                if (exception())
                    return;
                auto name_or_error = result.to_property_key(global_object);
                if (name_or_error.is_error())
                    return;
                name = name_or_error.release_value();
            });

        if (auto* thrown_exception = exception())
            return JS::throw_completion(thrown_exception->value());

        seen_names.set(name);

        if (property.name.has<NonnullRefPtr<Identifier>>() && property.alias.has<Empty>()) {
            // FIXME: this branch and not taking this have a lot in common we might want to unify it more (like it was before).
            auto& identifier = *property.name.get<NonnullRefPtr<Identifier>>();
            auto reference = resolve_binding(identifier.string(), environment);
            if (auto* thrown_exception = exception())
                return JS::throw_completion(thrown_exception->value());

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

        Optional<Reference> reference_to_assign_to;

        property.alias.visit(
            [&](Empty) {},
            [&](NonnullRefPtr<Identifier> const& identifier) {
                reference_to_assign_to = resolve_binding(identifier->string(), environment);
            },
            [&](NonnullRefPtr<BindingPattern> const&) {},
            [&](NonnullRefPtr<MemberExpression> const& member_expression) {
                reference_to_assign_to = member_expression->to_reference(interpreter(), global_object);
            });

        if (auto* thrown_exception = exception())
            return JS::throw_completion(thrown_exception->value());

        auto value_to_assign = TRY(object->get(name));
        if (property.initializer && value_to_assign.is_undefined()) {
            if (auto* identifier_ptr = property.alias.get_pointer<NonnullRefPtr<Identifier>>())
                value_to_assign = TRY(named_evaluation_if_anonymous_function(global_object, *property.initializer, (*identifier_ptr)->string()));
            else
                value_to_assign = property.initializer->execute(interpreter(), global_object);

            if (auto* thrown_exception = exception())
                return JS::throw_completion(thrown_exception->value());
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
ThrowCompletionOr<void> VM::iterator_binding_initialization(BindingPattern const& binding, Object* iterator, bool& iterator_done, Environment* environment, GlobalObject& global_object)
{
    // FIXME: this method is nearly identical to destructuring assignment!
    for (size_t i = 0; i < binding.entries.size(); i++) {
        auto& entry = binding.entries[i];
        Value value;

        Optional<Reference> assignment_target;
        entry.alias.visit(
            [&](Empty) {},
            [&](NonnullRefPtr<Identifier> const& identifier) {
                assignment_target = resolve_binding(identifier->string(), environment);
            },
            [&](NonnullRefPtr<BindingPattern> const&) {},
            [&](NonnullRefPtr<MemberExpression> const& member_expression) {
                assignment_target = member_expression->to_reference(interpreter(), global_object);
            });

        if (auto* thrown_exception = exception())
            return JS::throw_completion(thrown_exception->value());

        if (entry.is_rest) {
            VERIFY(i == binding.entries.size() - 1);

            auto* array = MUST(Array::create(global_object, 0));
            while (!iterator_done) {
                auto next_object_or_error = iterator_next(*iterator);
                if (next_object_or_error.is_throw_completion()) {
                    iterator_done = true;
                    return JS::throw_completion(next_object_or_error.release_error().value());
                }
                auto* next_object = next_object_or_error.release_value();

                auto done_property = TRY(next_object->get(names.done));
                if (done_property.to_boolean()) {
                    iterator_done = true;
                    break;
                }

                auto next_value = TRY(next_object->get(names.value));
                array->indexed_properties().append(next_value);
            }
            value = array;

        } else if (!iterator_done) {
            auto next_object_or_error = iterator_next(*iterator);
            if (next_object_or_error.is_throw_completion()) {
                iterator_done = true;
                return JS::throw_completion(next_object_or_error.release_error().value());
            }
            auto* next_object = next_object_or_error.release_value();

            auto done_property = TRY(next_object->get(names.done));
            if (done_property.to_boolean()) {
                iterator_done = true;
                value = js_undefined();
            } else {
                auto value_or_error = next_object->get(names.value);
                if (value_or_error.is_throw_completion()) {
                    iterator_done = true;
                    return JS::throw_completion(value_or_error.release_error().value());
                }
                value = value_or_error.release_value();
            }
        } else {
            value = js_undefined();
        }

        if (value.is_undefined() && entry.initializer) {
            VERIFY(!entry.is_rest);
            if (auto* identifier_ptr = entry.alias.get_pointer<NonnullRefPtr<Identifier>>())
                value = TRY(named_evaluation_if_anonymous_function(global_object, *entry.initializer, (*identifier_ptr)->string()));
            else
                value = entry.initializer->execute(interpreter(), global_object);

            if (auto* thrown_exception = exception())
                return JS::throw_completion(thrown_exception->value());
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
Reference VM::get_identifier_reference(Environment* environment, FlyString name, bool strict, size_t hops)
{
    // 1. If env is the value null, then
    if (!environment) {
        // a. Return the Reference Record { [[Base]]: unresolvable, [[ReferencedName]]: name, [[Strict]]: strict, [[ThisValue]]: empty }.
        return Reference { Reference::BaseType::Unresolvable, move(name), strict };
    }

    Optional<size_t> index;
    auto exists = TRY_OR_DISCARD(environment->has_binding(name, &index));

    Optional<EnvironmentCoordinate> environment_coordinate;
    if (index.has_value())
        environment_coordinate = EnvironmentCoordinate { .hops = hops, .index = index.value() };

    if (exists)
        return Reference { *environment, move(name), strict, environment_coordinate };
    else
        return get_identifier_reference(environment->outer_environment(), move(name), strict, hops + 1);
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

// 7.3.32 InitializeInstanceElements ( O, constructor ), https://tc39.es/ecma262/#sec-initializeinstanceelements
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
    unwind(ScopeType::Try);
}

// 9.4.4 ResolveThisBinding ( ), https://tc39.es/ecma262/#sec-resolvethisbinding
Value VM::resolve_this_binding(GlobalObject& global_object)
{
    auto& environment = get_this_environment(*this);
    return TRY_OR_DISCARD(environment.get_this_binding(global_object));
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

        [[maybe_unused]] auto result = call(*job, js_undefined());

        // This doesn't match the spec, it actually defines that Job Abstract Closures must return
        // a normal completion. In reality that's not the case however, and all major engines clear
        // exceptions when running Promise jobs. See the commit where these two lines were initially
        // added for a much more detailed explanation.
        clear_exception();
        stop_unwind();

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

}
