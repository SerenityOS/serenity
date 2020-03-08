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

#include <LibJS/AST.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Object.h>
#include <LibJS/Value.h>
#include <stdio.h>

namespace JS {

ScopeFrame::ScopeFrame(const ScopeNode& scope_node, const ScopeFrame* parent)
    : m_scope_node(scope_node)
    , m_parent(parent)
{
}

Optional<Value> ScopeFrame::get_var(String name) const
{
    auto found = Optional<Value>();

    if (m_parent) {
        found = m_parent->get_var(name);
    }
    if (!found.has_value()) {
        found = m_vars.get(name);
    }

    return found;
}

void ScopeFrame::put_var(String name, Value value)
{
    m_vars.set(move(name), move(value));
}

Interpreter::Interpreter()
    : m_heap(*this)
{
    m_global_object = heap().allocate<Object>();
}

Interpreter::~Interpreter()
{
}

Value Interpreter::run(const ScopeNode& scope_node, bool parented)
{
    if (parented) {
        enter_scope(scope_node);
    } else {
        enter_scope_unparented(scope_node);
    }

    Value last_value = js_undefined();
    for (auto& node : scope_node.children()) {
        last_value = node.execute(*this);
    }

    exit_scope(scope_node);
    return last_value;
}

void Interpreter::enter_scope(const ScopeNode& scope_node)
{
    if (m_scope_stack.size() == 0) {
        enter_scope_unparented(scope_node);
    } else {
        m_scope_stack.append(ScopeFrame(scope_node, &m_scope_stack.last()));
    }
}

void Interpreter::enter_scope_unparented(const ScopeNode& scope_node)
{
    m_scope_stack.append(ScopeFrame(scope_node, nullptr));
}

void Interpreter::exit_scope(const ScopeNode& scope_node)
{
    ASSERT(&m_scope_stack.last().scope_node() == &scope_node);
    m_scope_stack.take_last();
}

void Interpreter::do_return()
{
    dbg() << "FIXME: Implement Interpreter::do_return()";
}

}
