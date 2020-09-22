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
#include <AK/Weakable.h>
#include <LibJS/AST.h>
#include <LibJS/Console.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/DeferGC.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/Exception.h>
#include <LibJS/Runtime/LexicalEnvironment.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/VM.h>
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

class Interpreter : public Weakable<Interpreter> {
public:
    template<typename GlobalObjectType, typename... Args>
    static NonnullOwnPtr<Interpreter> create(VM& vm, Args&&... args)
    {
        DeferGC defer_gc(vm.heap());
        auto interpreter = adopt_own(*new Interpreter(vm));
        VM::InterpreterExecutionScope scope(*interpreter);
        interpreter->m_global_object = make_handle(static_cast<Object*>(interpreter->heap().allocate_without_global_object<GlobalObjectType>(forward<Args>(args)...)));
        static_cast<GlobalObjectType*>(interpreter->m_global_object.cell())->initialize();
        return interpreter;
    }

    template<typename... Args>
    [[nodiscard]] ALWAYS_INLINE Value call(Function& function, Value this_value, Args... args)
    {
        // Are there any values in this argpack?
        // args = [] -> if constexpr (false)
        // args = [x, y, z] -> if constexpr ((void)x, true || ...)
        if constexpr ((((void)args, true) || ...)) {
            MarkedValueList arglist { heap() };
            (..., arglist.append(move(args)));
            return call(function, this_value, move(arglist));
        }

        return call(function, this_value);
    }

    ~Interpreter();

    Value run(GlobalObject&, const Program&);

    Value execute_statement(GlobalObject&, const Statement&, ArgumentVector = {}, ScopeType = ScopeType::Block);

    GlobalObject& global_object();
    const GlobalObject& global_object() const;

    VM& vm() { return *m_vm; }
    Heap& heap() { return vm().heap(); }
    Exception* exception() { return vm().exception(); }

    void unwind(ScopeType type, FlyString label = {})
    {
        m_unwind_until = type;
        m_unwind_until_label = label;
    }
    void stop_unwind() { m_unwind_until = ScopeType::None; }
    bool should_unwind_until(ScopeType type, FlyString label) const
    {
        if (m_unwind_until_label.is_null())
            return m_unwind_until == type;
        return m_unwind_until == type && m_unwind_until_label == label;
    }
    bool should_unwind() const { return m_unwind_until != ScopeType::None; }

    Value get_variable(const FlyString& name, GlobalObject&);
    void set_variable(const FlyString& name, Value, GlobalObject&, bool first_assignment = false);

    Reference get_reference(const FlyString& name);

    void gather_roots(HashTable<Cell*>&);

    void enter_scope(const ScopeNode&, ArgumentVector, ScopeType, GlobalObject&);
    void exit_scope(const ScopeNode&);

    Value construct(Function&, Function& new_target, Optional<MarkedValueList> arguments, GlobalObject&);

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

    bool in_strict_mode() const
    {
        if (m_scope_stack.is_empty())
            return true;
        return m_scope_stack.last().scope_node->in_strict_mode();
    }

    template<typename Callback>
    void for_each_argument(Callback callback)
    {
        if (m_call_stack.is_empty())
            return;
        for (auto& value : m_call_stack.last().arguments)
            callback(value);
    }

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

    Value this_value(Object& global_object) const
    {
        if (m_call_stack.is_empty())
            return &global_object;
        return m_call_stack.last().this_value;
    }

    template<typename T, typename... Args>
    void throw_exception(Args&&... args)
    {
        return throw_exception(T::create(global_object(), forward<Args>(args)...));
    }

    void throw_exception(Exception*);
    void throw_exception(Value value)
    {
        return throw_exception(heap().allocate<Exception>(global_object(), value));
    }

    template<typename T, typename... Args>
    void throw_exception(ErrorType type, Args&&... args)
    {
        return throw_exception(T::create(global_object(), String::format(type.message(), forward<Args>(args)...)));
    }

    Value last_value() const { return m_last_value; }

    bool underscore_is_last_value() const { return m_underscore_is_last_value; }
    void set_underscore_is_last_value(bool b) { m_underscore_is_last_value = b; }

    Console& console() { return m_console; }
    const Console& console() const { return m_console; }

    String join_arguments() const;

    Value resolve_this_binding() const;
    const LexicalEnvironment* get_this_environment() const;
    Value get_new_target() const;

private:
    explicit Interpreter(VM&);

    [[nodiscard]] Value call_internal(Function&, Value this_value, Optional<MarkedValueList>);

    NonnullRefPtr<VM> m_vm;

    Value m_last_value;

    Vector<ScopeFrame> m_scope_stack;
    Vector<CallFrame> m_call_stack;

    Handle<Object> m_global_object;

    ScopeType m_unwind_until { ScopeType::None };
    FlyString m_unwind_until_label;

    bool m_underscore_is_last_value { false };

    Console m_console;
};

template<>
[[nodiscard]] ALWAYS_INLINE Value Interpreter::call(Function& function, Value this_value, MarkedValueList arguments) { return call_internal(function, this_value, move(arguments)); }

template<>
[[nodiscard]] ALWAYS_INLINE Value Interpreter::call(Function& function, Value this_value, Optional<MarkedValueList> arguments) { return call_internal(function, this_value, move(arguments)); }

template<>
[[nodiscard]] ALWAYS_INLINE Value Interpreter::call(Function& function, Value this_value) { return call(function, this_value, Optional<MarkedValueList> {}); }

}
