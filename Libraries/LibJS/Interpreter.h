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

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Exception.h>
#include <LibJS/Runtime/LexicalEnvironment.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

enum class ScopeType {
    None,
    Function,
    Block,
    Try,
    Breakable,
    Continuable,
};

struct ScopeFrame {
    ScopeType type;
    NonnullRefPtr<ScopeNode> scope_node;
    bool pushed_environment { false };
};

struct CallFrame {
    FlyString function_name;
    Value this_value;
    Vector<Value> arguments;
    LexicalEnvironment* environment { nullptr };
};

struct Argument {
    FlyString name;
    Value value;
};

typedef Vector<Argument, 8> ArgumentVector;

class Interpreter {
public:
    template<typename GlobalObjectType, typename... Args>
    static NonnullOwnPtr<Interpreter> create(Args&&... args)
    {
        auto interpreter = adopt_own(*new Interpreter);
        interpreter->m_global_object = interpreter->heap().allocate<GlobalObjectType>(forward<Args>(args)...);
        static_cast<GlobalObjectType*>(interpreter->m_global_object)->initialize();
        return interpreter;
    }

    ~Interpreter();

    Value run(const Statement&, ArgumentVector = {}, ScopeType = ScopeType::Block);

    GlobalObject& global_object();
    const GlobalObject& global_object() const;

    Heap& heap() { return m_heap; }

    void unwind(ScopeType type) { m_unwind_until = type; }
    void stop_unwind() { m_unwind_until = ScopeType::None; }
    bool should_unwind_until(ScopeType type) const { return m_unwind_until == type; }
    bool should_unwind() const { return m_unwind_until != ScopeType::None; }

    Optional<Value> get_variable(const FlyString& name);
    void set_variable(const FlyString& name, Value, bool first_assignment = false);

    void gather_roots(Badge<Heap>, HashTable<Cell*>&);

    void enter_scope(const ScopeNode&, ArgumentVector, ScopeType);
    void exit_scope(const ScopeNode&);

    Value call(Function*, Value this_value = {}, const Vector<Value>& arguments = {});

    CallFrame& push_call_frame()
    {
        m_call_stack.append({ {}, js_undefined(), {}, nullptr });
        return m_call_stack.last();
    }
    void pop_call_frame() { m_call_stack.take_last(); }
    const CallFrame& call_frame() { return m_call_stack.last(); }
    const Vector<CallFrame>& call_stack() { return m_call_stack; }

    void push_environment(LexicalEnvironment*);
    void pop_environment();

    const LexicalEnvironment* current_environment() const { return m_call_stack.last().environment; }
    LexicalEnvironment* current_environment() { return m_call_stack.last().environment; }

    size_t argument_count() const
    {
        if (m_call_stack.is_empty())
            return 0;
        return m_call_stack.last().arguments.size();
    }

    Value argument(size_t index) const
    {
        if (m_call_stack.is_empty())
            return {};
        auto& arguments = m_call_stack.last().arguments;
        return index < arguments.size() ? arguments[index] : js_undefined();
    }

    Value this_value() const
    {
        if (m_call_stack.is_empty())
            return m_global_object;
        return m_call_stack.last().this_value;
    }

    Shape* empty_object_shape() { return m_empty_object_shape; }

    Exception* exception()
    {
        return m_exception;
    }
    void clear_exception() { m_exception = nullptr; }

    template<typename T, typename... Args>
    Value throw_exception(Args&&... args)
    {
        return throw_exception(T::create(global_object(), forward<Args>(args)...));
    }

    Value throw_exception(Exception*);
    Value throw_exception(Value value)
    {
        return throw_exception(heap().allocate<Exception>(value));
    }

    Value last_value() const { return m_last_value; }

private:
    Interpreter();

    Heap m_heap;

    Value m_last_value;

    Vector<ScopeFrame> m_scope_stack;
    Vector<CallFrame> m_call_stack;

    Shape* m_empty_object_shape { nullptr };
    Object* m_global_object { nullptr };

    Exception* m_exception { nullptr };

    ScopeType m_unwind_until { ScopeType::None };
};

}
