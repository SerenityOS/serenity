/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Reference.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

NonnullRefPtr<VM> VM::create()
{
    return adopt(*new VM);
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
        roots.set(m_last_value.as_cell());

    for (auto& call_frame : m_call_stack) {
        if (call_frame->this_value.is_cell())
            roots.set(call_frame->this_value.as_cell());
        roots.set(call_frame->arguments_object);
        for (auto& argument : call_frame->arguments) {
            if (argument.is_cell())
                roots.set(argument.as_cell());
        }
        roots.set(call_frame->scope);
    }

#define __JS_ENUMERATE(SymbolName, snake_name) \
    roots.set(well_known_symbol_##snake_name());
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE

    for (auto& symbol : m_global_symbol_map)
        roots.set(symbol.value);
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

void VM::set_variable(const FlyString& name, Value value, GlobalObject& global_object, bool first_assignment)
{
    if (m_call_stack.size()) {
        for (auto* scope = current_scope(); scope; scope = scope->parent()) {
            auto possible_match = scope->get_from_scope(name);
            if (possible_match.has_value()) {
                if (!first_assignment && possible_match.value().declaration_kind == DeclarationKind::Const) {
                    throw_exception<TypeError>(global_object, ErrorType::InvalidAssignToConst);
                    return;
                }

                scope->put_to_scope(name, { value, possible_match.value().declaration_kind });
                return;
            }
        }
    }

    global_object.put(move(name), move(value));
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

void VM::throw_exception(Exception* exception)
{
    if (should_log_exceptions() && exception->value().is_object() && is<Error>(exception->value().as_object())) {
        auto& error = static_cast<Error&>(exception->value().as_object());
        dbgln("Throwing JavaScript Error: {}, {}", error.name(), error.message());

        for (ssize_t i = m_call_stack.size() - 1; i >= 0; --i) {
            const auto& source_range = m_call_stack[i]->current_node->source_range();
            auto function_name = m_call_stack[i]->function_name;
            if (function_name.is_empty())
                function_name = "<anonymous>";
            dbgln("  {} at {}:{}:{}", function_name, source_range.filename, source_range.start.line, source_range.start.column);
        }
    }

    m_exception = exception;
    unwind(ScopeType::Try);
}

String VM::join_arguments() const
{
    StringBuilder joined_arguments;
    for (size_t i = 0; i < argument_count(); ++i) {
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

    CallFrame call_frame;
    call_frame.callee = &function;
    if (auto* interpreter = interpreter_if_exists())
        call_frame.current_node = interpreter->current_node();
    call_frame.is_strict_mode = function.is_strict_mode();
    call_frame.function_name = function.name();
    call_frame.this_value = function.bound_this().value_or(this_value);
    call_frame.arguments = function.bound_arguments();
    if (arguments.has_value())
        call_frame.arguments.append(move(arguments.release_value().values()));
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

}
