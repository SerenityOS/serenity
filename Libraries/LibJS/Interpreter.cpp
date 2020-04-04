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

#include <AK/Badge.h>
#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/ArrayPrototype.h>
#include <LibJS/Runtime/DatePrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ErrorPrototype.h>
#include <LibJS/Runtime/FunctionPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/ObjectPrototype.h>
#include <LibJS/Runtime/Shape.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

Interpreter::Interpreter()
    : m_heap(*this)
{
    m_empty_object_shape = heap().allocate<Shape>();

    m_object_prototype = heap().allocate<ObjectPrototype>();
    m_function_prototype = heap().allocate<FunctionPrototype>();
    m_string_prototype = heap().allocate<StringPrototype>();
    m_array_prototype = heap().allocate<ArrayPrototype>();
    m_error_prototype = heap().allocate<ErrorPrototype>();
    m_date_prototype = heap().allocate<DatePrototype>();
}

Interpreter::~Interpreter()
{
}

Value Interpreter::run(const Statement& statement, Vector<Argument> arguments, ScopeType scope_type)
{
    if (!statement.is_scope_node())
        return statement.execute(*this);

    auto& block = static_cast<const ScopeNode&>(statement);
    enter_scope(block, move(arguments), scope_type);

    Value last_value = js_undefined();
    for (auto& node : block.children()) {
        last_value = node.execute(*this);
        if (m_unwind_until != ScopeType::None)
            break;
    }

    if (m_unwind_until == scope_type)
        m_unwind_until = ScopeType::None;

    exit_scope(block);
    return last_value;
}

void Interpreter::enter_scope(const ScopeNode& scope_node, Vector<Argument> arguments, ScopeType scope_type)
{
    HashMap<FlyString, Variable> scope_variables_with_declaration_type;
    for (auto& argument : arguments) {
        scope_variables_with_declaration_type.set(argument.name, { argument.value, DeclarationType::Var });
    }
    m_scope_stack.append({ scope_type, scope_node, move(scope_variables_with_declaration_type) });
}

void Interpreter::exit_scope(const ScopeNode& scope_node)
{
    while (!m_scope_stack.is_empty()) {
        auto popped_scope = m_scope_stack.take_last();
        if (popped_scope.scope_node.ptr() == &scope_node)
            break;
    }

    // If we unwind all the way, just reset m_unwind_until so that future "return" doesn't break.
    if (m_scope_stack.is_empty())
        m_unwind_until = ScopeType::None;
}

void Interpreter::declare_variable(const FlyString& name, DeclarationType declaration_type)
{
    switch (declaration_type) {
    case DeclarationType::Var:
        for (ssize_t i = m_scope_stack.size() - 1; i >= 0; --i) {
            auto& scope = m_scope_stack.at(i);
            if (scope.type == ScopeType::Function) {
                if (scope.variables.get(name).has_value() && scope.variables.get(name).value().declaration_type != DeclarationType::Var)
                    ASSERT_NOT_REACHED();

                scope.variables.set(move(name), { js_undefined(), declaration_type });
                return;
            }
        }

        global_object().put(move(name), js_undefined());
        break;
    case DeclarationType::Let:
    case DeclarationType::Const:
        if (m_scope_stack.last().variables.get(name).has_value())
            ASSERT_NOT_REACHED();

        m_scope_stack.last().variables.set(move(name), { js_undefined(), declaration_type });
        break;
    }
}

void Interpreter::set_variable(const FlyString& name, Value value, bool first_assignment)
{
    for (ssize_t i = m_scope_stack.size() - 1; i >= 0; --i) {
        auto& scope = m_scope_stack.at(i);

        auto possible_match = scope.variables.get(name);
        if (possible_match.has_value()) {
            if (!first_assignment && possible_match.value().declaration_type == DeclarationType::Const)
                ASSERT_NOT_REACHED();

            scope.variables.set(move(name), { move(value), possible_match.value().declaration_type });
            return;
        }
    }

    global_object().put(move(name), move(value));
}

Optional<Value> Interpreter::get_variable(const FlyString& name)
{
    if (name == "this")
        return this_value();

    for (ssize_t i = m_scope_stack.size() - 1; i >= 0; --i) {
        auto& scope = m_scope_stack.at(i);
        auto value = scope.variables.get(name);
        if (value.has_value())
            return value.value().value;
    }

    return global_object().get(name);
}

void Interpreter::gather_roots(Badge<Heap>, HashTable<Cell*>& roots)
{
    roots.set(m_empty_object_shape);

    roots.set(m_global_object);
    roots.set(m_string_prototype);
    roots.set(m_object_prototype);
    roots.set(m_array_prototype);
    roots.set(m_error_prototype);
    roots.set(m_date_prototype);
    roots.set(m_function_prototype);

    roots.set(m_exception);

    for (auto& scope : m_scope_stack) {
        for (auto& it : scope.variables) {
            if (it.value.value.is_cell())
                roots.set(it.value.value.as_cell());
        }
    }

    for (auto& call_frame : m_call_stack) {
        if (call_frame.this_value.is_cell())
            roots.set(call_frame.this_value.as_cell());
        for (auto& argument : call_frame.arguments) {
            if (argument.is_cell())
                roots.set(argument.as_cell());
        }
    }
}

Value Interpreter::call(Function* function, Value this_value, const Vector<Value>& arguments)
{
    auto& call_frame = push_call_frame();
    call_frame.this_value = this_value;
    call_frame.arguments = arguments;
    auto result = function->call(*this);
    pop_call_frame();
    return result;
}

Value Interpreter::throw_exception(Exception* exception)
{
    m_exception = exception;
    unwind(ScopeType::Try);
    return {};
}

}
