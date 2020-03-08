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

#pragma once

#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap.h>

namespace JS {

class ScopeFrame {
public:
    ScopeFrame(const ScopeNode& scope_node, const ScopeFrame* parent);

    Optional<Value> get_var(String name) const;
    void put_var(String name, Value value);

    const ScopeNode& scope_node() const { return m_scope_node; }

private:
    const ScopeNode& m_scope_node;
    const ScopeFrame* m_parent;
    HashMap<String, Value> m_vars;
};

class Interpreter {
public:
    Interpreter();
    ~Interpreter();

    Value run(const ScopeNode& scope_node, bool parented = true);

    ScopeFrame& current_frame() { return m_scope_stack.last(); }
    const ScopeFrame& current_frame() const { return m_scope_stack.last(); }

    Object& global_object() { return *m_global_object; }
    const Object& global_object() const { return *m_global_object; }

    Heap& heap() { return m_heap; }

    void do_return();

private:
    void enter_scope_unparented(const ScopeNode&);
    void enter_scope(const ScopeNode&);
    void exit_scope(const ScopeNode&);

    Heap m_heap;
    Object* m_global_object { nullptr };
    Vector<ScopeFrame> m_scope_stack;
};
}
