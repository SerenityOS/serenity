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
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FinalizationRegistry.h>
#include <LibJS/Runtime/FunctionEnvironmentRecord.h>
#include <LibJS/Runtime/GlobalEnvironmentRecord.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/PromiseReaction.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/TemporaryClearException.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

NonnullRefPtr<VM> VM::create()
{
    return adopt_ref(*new VM);
}

VM::VM()
    : m_heap(*this)
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
    }

#define __JS_ENUMERATE(SymbolName, snake_name) \
    roots.set(well_known_symbol_##snake_name());
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE

    for (auto& symbol : m_global_symbol_map)
        roots.set(symbol.value);

    for (auto* job : m_promise_jobs)
        roots.set(job);
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

void VM::set_variable(const FlyString& name, Value value, GlobalObject& global_object, bool first_assignment, EnvironmentRecord* specific_scope)
{
    Optional<Variable> possible_match;
    if (!specific_scope && m_execution_context_stack.size()) {
        for (auto* environment_record = lexical_environment(); environment_record; environment_record = environment_record->outer_environment()) {
            possible_match = environment_record->get_from_environment_record(name);
            if (possible_match.has_value()) {
                specific_scope = environment_record;
                break;
            }
        }
    }

    if (specific_scope && possible_match.has_value()) {
        if (!first_assignment && possible_match.value().declaration_kind == DeclarationKind::Const) {
            throw_exception<TypeError>(global_object, ErrorType::InvalidAssignToConst);
            return;
        }

        specific_scope->put_into_environment_record(name, { value, possible_match.value().declaration_kind });
        return;
    }

    if (specific_scope) {
        specific_scope->put_into_environment_record(name, { value, DeclarationKind::Var });
        return;
    }

    global_object.put(name, value);
}

bool VM::delete_variable(FlyString const& name)
{
    EnvironmentRecord* specific_scope = nullptr;
    Optional<Variable> possible_match;
    if (!m_execution_context_stack.is_empty()) {
        for (auto* environment_record = lexical_environment(); environment_record; environment_record = environment_record->outer_environment()) {
            possible_match = environment_record->get_from_environment_record(name);
            if (possible_match.has_value()) {
                specific_scope = environment_record;
                break;
            }
        }
    }

    if (!possible_match.has_value())
        return false;
    if (possible_match.value().declaration_kind == DeclarationKind::Const)
        return false;

    VERIFY(specific_scope);
    return specific_scope->delete_from_environment_record(name);
}

void VM::assign(const FlyString& target, Value value, GlobalObject& global_object, bool first_assignment, EnvironmentRecord* specific_scope)
{
    set_variable(target, move(value), global_object, first_assignment, specific_scope);
}

void VM::assign(const Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>>& target, Value value, GlobalObject& global_object, bool first_assignment, EnvironmentRecord* specific_scope)
{
    if (auto id_ptr = target.get_pointer<NonnullRefPtr<Identifier>>())
        return assign((*id_ptr)->string(), move(value), global_object, first_assignment, specific_scope);

    assign(target.get<NonnullRefPtr<BindingPattern>>(), move(value), global_object, first_assignment, specific_scope);
}

void VM::assign(const NonnullRefPtr<BindingPattern>& target, Value value, GlobalObject& global_object, bool first_assignment, EnvironmentRecord* specific_scope)
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

                auto* array = Array::create(global_object);
                for (;;) {
                    auto next_object = iterator_next(*iterator);
                    if (!next_object)
                        return;

                    auto done_property = next_object->get(names.done);
                    if (exception())
                        return;

                    if (!done_property.is_empty() && done_property.to_boolean())
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

                if (!done_property.is_empty() && done_property.to_boolean()) {
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
                    if (!object_property.value.attributes.has_enumerable())
                        continue;
                    if (seen_names.contains(object_property.key.to_display_string()))
                        continue;
                    rest_object->put(object_property.key, object->get(object_property.key));
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
            auto possible_match = lexical_environment()->get_from_environment_record(name);
            if (possible_match.has_value())
                return possible_match.value().value;
            if (!context.arguments_object) {
                context.arguments_object = Array::create(global_object);
                context.arguments_object->put(names.callee, context.function);
                for (auto argument : context.arguments) {
                    context.arguments_object->indexed_properties().append(argument);
                }
            }
            return context.arguments_object;
        }

        for (auto* environment_record = lexical_environment(); environment_record; environment_record = environment_record->outer_environment()) {
            auto possible_match = environment_record->get_from_environment_record(name);
            if (exception())
                return {};
            if (possible_match.has_value())
                return possible_match.value().value;
        }
    }
    auto value = global_object.get(name);
    if (m_underscore_is_last_value && name == "_" && value.is_empty())
        return m_last_value;
    return value;
}

Reference VM::get_reference(const FlyString& name)
{
    for (auto* environment_record = lexical_environment(); environment_record && environment_record->outer_environment(); environment_record = environment_record->outer_environment()) {
        auto possible_match = environment_record->get_from_environment_record(name);
        if (possible_match.has_value())
            return { Reference::LocalVariable, name };
    }
    return { Reference::GlobalVariable, name };
}

Value VM::construct(Function& function, Function& new_target, Optional<MarkedValueList> arguments)
{
    auto& global_object = function.global_object();
    ExecutionContext execution_context;
    execution_context.function = &function;
    if (auto* interpreter = interpreter_if_exists())
        execution_context.current_node = interpreter->current_node();
    execution_context.is_strict_mode = function.is_strict_mode();

    push_execution_context(execution_context, global_object);
    if (exception())
        return {};
    ArmedScopeGuard pop_guard = [&] {
        pop_execution_context();
    };

    execution_context.function_name = function.name();
    execution_context.arguments = function.bound_arguments();
    if (arguments.has_value())
        execution_context.arguments.extend(arguments.value().values());
    auto* environment = function.create_environment_record();
    execution_context.lexical_environment = environment;
    execution_context.variable_environment = environment;
    if (environment)
        environment->set_new_target(&new_target);

    Object* new_object = nullptr;
    if (function.constructor_kind() == Function::ConstructorKind::Base) {
        new_object = Object::create(global_object, nullptr);
        if (environment)
            environment->bind_this_value(global_object, new_object);
        if (exception())
            return {};
        auto prototype = new_target.get(names.prototype);
        if (exception())
            return {};
        if (prototype.is_object()) {
            new_object->set_prototype(&prototype.as_object());
            if (exception())
                return {};
        }
    }

    // If we are a Derived constructor, |this| has not been constructed before super is called.
    Value this_value = function.constructor_kind() == Function::ConstructorKind::Base ? new_object : Value {};
    execution_context.this_value = this_value;
    auto result = function.construct(new_target);

    if (environment)
        this_value = environment->get_this_binding(global_object);
    pop_execution_context();
    pop_guard.disarm();

    // If we are constructing an instance of a derived class,
    // set the prototype on objects created by constructors that return an object (i.e. NativeFunction subclasses).
    if (function.constructor_kind() == Function::ConstructorKind::Base && new_target.constructor_kind() == Function::ConstructorKind::Derived && result.is_object()) {
        if (environment) {
            VERIFY(is<FunctionEnvironmentRecord>(lexical_environment()));
            static_cast<FunctionEnvironmentRecord*>(lexical_environment())->replace_this_binding(result);
        }
        auto prototype = new_target.get(names.prototype);
        if (exception())
            return {};
        if (prototype.is_object()) {
            result.as_object().set_prototype(&prototype.as_object());
            if (exception())
                return {};
        }
        return result;
    }

    if (exception())
        return {};

    if (result.is_object())
        return result;

    return this_value;
}

void VM::throw_exception(Exception& exception)
{
    set_exception(exception);
    unwind(ScopeType::Try);
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
    VERIFY(is<FunctionEnvironmentRecord>(env));
    return static_cast<FunctionEnvironmentRecord&>(env).new_target();
}

Value VM::call_internal(Function& function, Value this_value, Optional<MarkedValueList> arguments)
{
    VERIFY(!exception());
    VERIFY(!this_value.is_empty());

    ExecutionContext execution_context;
    execution_context.function = &function;
    if (auto* interpreter = interpreter_if_exists())
        execution_context.current_node = interpreter->current_node();
    execution_context.is_strict_mode = function.is_strict_mode();
    execution_context.function_name = function.name();
    execution_context.this_value = function.bound_this().value_or(this_value);
    execution_context.arguments = function.bound_arguments();
    if (arguments.has_value())
        execution_context.arguments.extend(arguments.value().values());
    auto* environment = function.create_environment_record();
    execution_context.lexical_environment = environment;
    execution_context.variable_environment = environment;

    if (environment) {
        VERIFY(environment->this_binding_status() == FunctionEnvironmentRecord::ThisBindingStatus::Uninitialized);
        environment->bind_this_value(function.global_object(), execution_context.this_value);
    }

    if (exception())
        return {};

    push_execution_context(execution_context, function.global_object());
    if (exception())
        return {};
    auto result = function.call();
    pop_execution_context();
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
    for (ssize_t i = m_execution_context_stack.size() - 1; i >= 0; --i)
        dbgln("-> {}", m_execution_context_stack[i]->function_name);
}

void VM::dump_environment_record_chain() const
{
    for (auto* environment_record = lexical_environment(); environment_record; environment_record = environment_record->outer_environment()) {
        dbgln("+> {} ({:p})", environment_record->class_name(), environment_record);
        if (is<DeclarativeEnvironmentRecord>(*environment_record)) {
            auto& declarative_environment_record = static_cast<DeclarativeEnvironmentRecord const&>(*environment_record);
            for (auto& variable : declarative_environment_record.variables()) {
                dbgln("    {}", variable.key);
            }
        }
    }
}

}
