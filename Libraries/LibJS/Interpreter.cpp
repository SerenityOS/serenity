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
#include <LibJS/Object.h>
#include <LibJS/Value.h>

namespace JS {

Interpreter::Interpreter()
    : m_heap(*this)
{
    m_global_object = heap().allocate<Object>();
}

Interpreter::~Interpreter()
{
}

Value Interpreter::run(const ScopeNode& scope_node, ScopeType scope_type)
{
    enter_scope(scope_node, scope_type);

    Value last_value = js_undefined();
    for (auto& node : scope_node.children()) {
        last_value = node.execute(*this);
    }

    exit_scope(scope_node);
    return last_value;
}

void Interpreter::enter_scope(const ScopeNode& scope_node, ScopeType scope_type)
{
    m_scope_stack.append({ scope_type, scope_node, {} });
}

void Interpreter::exit_scope(const ScopeNode& scope_node)
{
    ASSERT(&m_scope_stack.last().scope_node == &scope_node);
    m_scope_stack.take_last();
}

void Interpreter::do_return()
{
    dbg() << "FIXME: Implement Interpreter::do_return()";
}

void Interpreter::declare_variable(String name, DeclarationType declaration_type)
{
    switch (declaration_type) {
    case DeclarationType::Var:
        for (ssize_t i = m_scope_stack.size() - 1; i >= 0; --i) {
            auto& scope = m_scope_stack.at(i);
            if (scope.type == ScopeType::Function) {
                scope.variables.set(move(name), js_undefined());
                return;
            }
        }

        global_object().put(move(name), js_undefined());
        break;
    case DeclarationType::Let:
        m_scope_stack.last().variables.set(move(name), js_undefined());
        break;
    }
}

void Interpreter::set_variable(String name, Value value)
{
    for (ssize_t i = m_scope_stack.size() - 1; i >= 0; --i) {
        auto& scope = m_scope_stack.at(i);
        if (scope.variables.contains(name)) {
            scope.variables.set(move(name), move(value));
            return;
        }
    }

    global_object().put(move(name), move(value));
}

Value Interpreter::get_variable(const String& name)
{
    for (ssize_t i = m_scope_stack.size() - 1; i >= 0; --i) {
        auto& scope = m_scope_stack.at(i);
        auto value = scope.variables.get(name);
        if (value.has_value())
            return value.value();
    }

    return global_object().get(name);
}

void Interpreter::collect_roots(Badge<Heap>, HashTable<Cell*>& roots)
{
    roots.set(m_global_object);

    for (auto& scope : m_scope_stack) {
        for (auto& it : scope.variables) {
            if (it.value.is_object())
                roots.set(it.value.as_object());
        }
    }
}

}
