/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
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

    m_scope_object_shape = m_heap.allocate_without_global_object<Shape>(Shape::ShapeWithoutGlobalObjectTag::Tag);

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

    roots.set(m_scope_object_shape);
    roots.set(m_exception);

    if (m_last_value.is_cell())
        roots.set(&m_last_value.as_cell());

    for (auto& call_frame : m_call_stack) {
        if (call_frame->this_value.is_cell())
            roots.set(&call_frame->this_value.as_cell());
        roots.set(call_frame->arguments_object);
        for (auto& argument : call_frame->arguments) {
            if (argument.is_cell())
                roots.set(&argument.as_cell());
        }
        roots.set(call_frame->scope);
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

void VM::set_variable(const FlyString& name, Value value, GlobalObject& global_object, bool first_assignment, ScopeObject* specific_scope)
{
    Optional<Variable> possible_match;
    if (!specific_scope && m_call_stack.size()) {
        for (auto* scope = current_scope(); scope; scope = scope->parent()) {
            possible_match = scope->get_from_scope(name);
            if (possible_match.has_value()) {
                specific_scope = scope;
                break;
            }
        }
    }

    if (specific_scope && possible_match.has_value()) {
        if (!first_assignment && possible_match.value().declaration_kind == DeclarationKind::Const) {
            throw_exception<TypeError>(global_object, ErrorType::InvalidAssignToConst);
            return;
        }

        specific_scope->put_to_scope(name, { value, possible_match.value().declaration_kind });
        return;
    }

    if (specific_scope) {
        specific_scope->put_to_scope(name, { value, DeclarationKind::Var });
        return;
    }

    global_object.put(name, value);
}

void VM::assign(const FlyString& target, Value value, GlobalObject& global_object, bool first_assignment, ScopeObject* specific_scope)
{
    set_variable(target, move(value), global_object, first_assignment, specific_scope);
}

void VM::assign(const Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>>& target, Value value, GlobalObject& global_object, bool first_assignment, ScopeObject* specific_scope)
{
    if (auto id_ptr = target.get_pointer<NonnullRefPtr<Identifier>>())
        return assign((*id_ptr)->string(), move(value), global_object, first_assignment, specific_scope);

    assign(target.get<NonnullRefPtr<BindingPattern>>(), move(value), global_object, first_assignment, specific_scope);
}

void VM::assign(const NonnullRefPtr<BindingPattern>& target, Value value, GlobalObject& global_object, bool first_assignment, ScopeObject* specific_scope)
{
    auto& binding = *target;

    switch (binding.kind) {
    case BindingPattern::Kind::Array: {
        auto iterator = get_iterator(global_object, value);
        if (!iterator)
            return;

        size_t index = 0;
        while (true) {
            if (exception())
                return;

            if (index >= binding.properties.size())
                break;

            auto pattern_property = binding.properties[index];
            ++index;

            if (pattern_property.is_rest) {
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
            } else {
                auto next_object = iterator_next(*iterator);
                if (!next_object)
                    return;

                auto done_property = next_object->get(names.done);
                if (exception())
                    return;

                if (!done_property.is_empty() && done_property.to_boolean())
                    break;

                value = next_object->get(names.value);
                if (exception())
                    return;
            }

            if (value.is_undefined() && pattern_property.initializer)
                value = pattern_property.initializer->execute(interpreter(), global_object);

            if (exception())
                return;

            if (pattern_property.name) {
                set_variable(pattern_property.name->string(), value, global_object, first_assignment, specific_scope);
                if (pattern_property.is_rest)
                    break;
                continue;
            }

            if (pattern_property.pattern) {
                assign(NonnullRefPtr(*pattern_property.pattern), value, global_object, first_assignment, specific_scope);
                if (pattern_property.is_rest)
                    break;
                continue;
            }
        }
        break;
    }
    case BindingPattern::Kind::Object: {
        auto object = value.to_object(global_object);
        HashTable<FlyString> seen_names;
        for (auto& property : binding.properties) {
            VERIFY(!property.pattern);
            JS::Value value_to_assign;
            if (property.is_rest) {
                auto* rest_object = Object::create_empty(global_object);
                rest_object->set_prototype(nullptr);
                for (auto& property : object->shape().property_table()) {
                    if (!property.value.attributes.has_enumerable())
                        continue;
                    if (seen_names.contains(property.key.to_display_string()))
                        continue;
                    rest_object->put(property.key, object->get(property.key));
                    if (exception())
                        return;
                }
                value_to_assign = rest_object;
            } else {
                value_to_assign = object->get(property.name->string());
            }

            seen_names.set(property.name->string());
            if (exception())
                break;

            auto assignment_name = property.name->string();
            if (property.alias)
                assignment_name = property.alias->string();

            if (value_to_assign.is_empty())
                value_to_assign = js_undefined();

            if (value_to_assign.is_undefined() && property.initializer)
                value_to_assign = property.initializer->execute(interpreter(), global_object);

            if (exception())
                break;

            set_variable(assignment_name, value_to_assign, global_object, first_assignment, specific_scope);

            if (property.is_rest)
                break;
        }
        break;
    }
    }
}

Value VM::get_variable(const FlyString& name, GlobalObject& global_object)
{
    if (!m_call_stack.is_empty()) {
        if (name == names.arguments && !call_frame().callee.is_empty()) {
            // HACK: Special handling for the name "arguments":
            //       If the name "arguments" is defined in the current scope, for example via
            //       a function parameter, or by a local var declaration, we use that.
            //       Otherwise, we return a lazily constructed Array with all the argument values.
            // FIXME: Do something much more spec-compliant.
            auto possible_match = current_scope()->get_from_scope(name);
            if (possible_match.has_value())
                return possible_match.value().value;
            if (!call_frame().arguments_object) {
                call_frame().arguments_object = Array::create(global_object);
                call_frame().arguments_object->put(names.callee, call_frame().callee);
                for (auto argument : call_frame().arguments) {
                    call_frame().arguments_object->indexed_properties().append(argument);
                }
            }
            return call_frame().arguments_object;
        }

        for (auto* scope = current_scope(); scope; scope = scope->parent()) {
            auto possible_match = scope->get_from_scope(name);
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
    if (m_call_stack.size()) {
        for (auto* scope = current_scope(); scope; scope = scope->parent()) {
            if (is<GlobalObject>(scope))
                break;
            auto possible_match = scope->get_from_scope(name);
            if (possible_match.has_value())
                return { Reference::LocalVariable, name };
        }
    }
    return { Reference::GlobalVariable, name };
}

Value VM::construct(Function& function, Function& new_target, Optional<MarkedValueList> arguments, GlobalObject& global_object)
{
    CallFrame call_frame;
    call_frame.callee = &function;
    if (auto* interpreter = interpreter_if_exists())
        call_frame.current_node = interpreter->current_node();
    call_frame.is_strict_mode = function.is_strict_mode();

    push_call_frame(call_frame, function.global_object());
    if (exception())
        return {};
    ArmedScopeGuard call_frame_popper = [&] {
        pop_call_frame();
    };

    call_frame.function_name = function.name();
    call_frame.arguments = function.bound_arguments();
    if (arguments.has_value())
        call_frame.arguments.append(arguments.value().values());
    auto* environment = function.create_environment();
    call_frame.scope = environment;
    environment->set_new_target(&new_target);

    Object* new_object = nullptr;
    if (function.constructor_kind() == Function::ConstructorKind::Base) {
        new_object = Object::create_empty(global_object);
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
    call_frame.this_value = this_value;
    auto result = function.construct(new_target);

    this_value = call_frame.scope->get_this_binding(global_object);
    pop_call_frame();
    call_frame_popper.disarm();

    // If we are constructing an instance of a derived class,
    // set the prototype on objects created by constructors that return an object (i.e. NativeFunction subclasses).
    if (function.constructor_kind() == Function::ConstructorKind::Base && new_target.constructor_kind() == Function::ConstructorKind::Derived && result.is_object()) {
        VERIFY(is<LexicalEnvironment>(current_scope()));
        static_cast<LexicalEnvironment*>(current_scope())->replace_this_binding(result);
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

Value VM::resolve_this_binding(GlobalObject& global_object) const
{
    return find_this_scope()->get_this_binding(global_object);
}

const ScopeObject* VM::find_this_scope() const
{
    // We will always return because the Global environment will always be reached, which has a |this| binding.
    for (auto* scope = current_scope(); scope; scope = scope->parent()) {
        if (scope->has_this_binding())
            return scope;
    }
    VERIFY_NOT_REACHED();
}

Value VM::get_new_target() const
{
    VERIFY(is<LexicalEnvironment>(find_this_scope()));
    return static_cast<const LexicalEnvironment*>(find_this_scope())->new_target();
}

Value VM::call_internal(Function& function, Value this_value, Optional<MarkedValueList> arguments)
{
    VERIFY(!exception());
    VERIFY(!this_value.is_empty());

    CallFrame call_frame;
    call_frame.callee = &function;
    if (auto* interpreter = interpreter_if_exists())
        call_frame.current_node = interpreter->current_node();
    call_frame.is_strict_mode = function.is_strict_mode();
    call_frame.function_name = function.name();
    call_frame.this_value = function.bound_this().value_or(this_value);
    call_frame.arguments = function.bound_arguments();
    if (arguments.has_value())
        call_frame.arguments.append(arguments.value().values());
    auto* environment = function.create_environment();
    call_frame.scope = environment;

    VERIFY(environment->this_binding_status() == LexicalEnvironment::ThisBindingStatus::Uninitialized);
    environment->bind_this_value(function.global_object(), call_frame.this_value);
    if (exception())
        return {};

    push_call_frame(call_frame, function.global_object());
    if (exception())
        return {};
    auto result = function.call();
    pop_call_frame();
    return result;
}

bool VM::in_strict_mode() const
{
    if (call_stack().is_empty())
        return false;
    return call_frame().is_strict_mode;
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

// 9.4.4 HostEnqueuePromiseJob, https://tc39.es/ecma262/#sec-hostenqueuepromisejob
void VM::enqueue_promise_job(NativeFunction& job)
{
    m_promise_jobs.append(&job);
}

// 27.2.1.9 HostPromiseRejectionTracker, https://tc39.es/ecma262/#sec-host-promise-rejection-tracker
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

}
