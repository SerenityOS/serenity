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
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

enum class ScopeType {
    Function,
    Block,
};

struct Variable {
    Value value;
    DeclarationType declaration_type;
};

struct ScopeFrame {
    ScopeType type;
    NonnullRefPtr<ScopeNode> scope_node;
    HashMap<String, Variable> variables;
};

struct CallFrame {
    Value this_value;
    Vector<Value> arguments;
};

struct Argument {
    String name;
    Value value;
};

class Interpreter {
public:
    Interpreter();
    ~Interpreter();

    Value run(const ScopeNode&, Vector<Argument> = {}, ScopeType = ScopeType::Block);

    Object& global_object() { return *m_global_object; }
    const Object& global_object() const { return *m_global_object; }

    Heap& heap() { return m_heap; }

    void do_return();

    Value get_variable(const String& name);
    void set_variable(String name, Value, bool first_assignment = false);
    void declare_variable(String name, DeclarationType);

    void gather_roots(Badge<Heap>, HashTable<Cell*>&);

    void enter_scope(const ScopeNode&, Vector<Argument>, ScopeType);
    void exit_scope(const ScopeNode&);

    Value call(Function*, Value this_value, const Vector<Value>& arguments);

    CallFrame& push_call_frame() { m_call_stack.append({ js_undefined(), {} }); return m_call_stack.last(); }
    void pop_call_frame() { m_call_stack.take_last(); }
    Value this_value() const
    {
        if (m_call_stack.is_empty())
            return m_global_object;
        return m_call_stack.last().this_value;
    }

    Object* string_prototype() { return m_string_prototype; }
    Object* object_prototype() { return m_object_prototype; }
    Object* array_prototype() { return m_array_prototype; }

private:
    Heap m_heap;

    Vector<ScopeFrame> m_scope_stack;
    Vector<CallFrame> m_call_stack;

    Object* m_global_object { nullptr };
    Object* m_string_prototype { nullptr };
    Object* m_object_prototype { nullptr };
    Object* m_array_prototype { nullptr };
};

}
