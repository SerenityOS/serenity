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
#include <AK/RefCounted.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/Exception.h>
#include <LibJS/Runtime/MarkedValueList.h>
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
    bool is_strict_mode { false };
};

struct Argument {
    FlyString name;
    Value value;
};

typedef Vector<Argument, 8> ArgumentVector;

class VM : public RefCounted<VM> {
public:
    static NonnullRefPtr<VM> create();
    ~VM();

    Heap& heap() { return m_heap; }
    const Heap& heap() const { return m_heap; }

    Interpreter& interpreter();
    Interpreter* interpreter_if_exists();

    void push_interpreter(Interpreter&);
    void pop_interpreter(Interpreter&);

    Exception* exception()
    {
        return m_exception;
    }

    void clear_exception() { m_exception = nullptr; }

    class InterpreterExecutionScope {
    public:
        InterpreterExecutionScope(Interpreter&);
        ~InterpreterExecutionScope();

    private:
        Interpreter& m_interpreter;
    };

    void gather_roots(HashTable<Cell*>&);

#define __JS_ENUMERATE(SymbolName, snake_name) \
    Symbol* well_known_symbol_##snake_name() const { return m_well_known_symbol_##snake_name; }
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE

    Symbol* get_global_symbol(const String& description);

    PrimitiveString& empty_string() { return *m_empty_string; }

    CallFrame& push_call_frame(bool strict_mode = false)
    {
        m_call_stack.append({ {}, js_undefined(), {}, nullptr, strict_mode });
        return m_call_stack.last();
    }
    void pop_call_frame() { m_call_stack.take_last(); }
    CallFrame& call_frame() { return m_call_stack.last(); }
    const CallFrame& call_frame() const { return m_call_stack.last(); }
    const Vector<CallFrame>& call_stack() const { return m_call_stack; }
    Vector<CallFrame>& call_stack() { return m_call_stack; }

    const LexicalEnvironment* current_environment() const { return m_call_stack.last().environment; }
    LexicalEnvironment* current_environment() { return m_call_stack.last().environment; }

    bool in_strict_mode() const;

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

    Value last_value() const { return m_last_value; }
    void set_last_value(Badge<Interpreter>, Value value) { m_last_value = value; }

    bool underscore_is_last_value() const { return m_underscore_is_last_value; }
    void set_underscore_is_last_value(bool b) { m_underscore_is_last_value = b; }

    void unwind(ScopeType type, FlyString label = {})
    {
        m_unwind_until = type;
        m_unwind_until_label = label;
    }
    void stop_unwind() { m_unwind_until = ScopeType::None; }
    bool should_unwind_until(ScopeType type, FlyString label = {}) const
    {
        if (m_unwind_until_label.is_null())
            return m_unwind_until == type;
        return m_unwind_until == type && m_unwind_until_label == label;
    }
    bool should_unwind() const { return m_unwind_until != ScopeType::None; }

    ScopeType unwind_until() const { return m_unwind_until; }

    Value get_variable(const FlyString& name, GlobalObject&);
    void set_variable(const FlyString& name, Value, GlobalObject&, bool first_assignment = false);

    Reference get_reference(const FlyString& name);

    template<typename T, typename... Args>
    void throw_exception(GlobalObject& global_object, Args&&... args)
    {
        return throw_exception(global_object, T::create(global_object, forward<Args>(args)...));
    }

    void throw_exception(Exception*);
    void throw_exception(GlobalObject& global_object, Value value)
    {
        return throw_exception(heap().allocate<Exception>(global_object, value));
    }

    template<typename T, typename... Args>
    void throw_exception(GlobalObject& global_object, ErrorType type, Args&&... args)
    {
        return throw_exception(global_object, T::create(global_object, String::formatted(type.message(), forward<Args>(args)...)));
    }

    Value construct(Function&, Function& new_target, Optional<MarkedValueList> arguments, GlobalObject&);

    String join_arguments() const;

    Value resolve_this_binding(GlobalObject&) const;
    const LexicalEnvironment* get_this_environment() const;
    Value get_new_target() const;

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

private:
    VM();

    [[nodiscard]] Value call_internal(Function&, Value this_value, Optional<MarkedValueList> arguments);

    Exception* m_exception { nullptr };

    Heap m_heap;
    Vector<Interpreter*> m_interpreters;

    Vector<CallFrame> m_call_stack;

    Value m_last_value;
    ScopeType m_unwind_until { ScopeType::None };
    FlyString m_unwind_until_label;

    bool m_underscore_is_last_value { false };

    HashMap<String, Symbol*> m_global_symbol_map;

    PrimitiveString* m_empty_string { nullptr };

#define __JS_ENUMERATE(SymbolName, snake_name) \
    Symbol* m_well_known_symbol_##snake_name { nullptr };
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE
};

template<>
[[nodiscard]] ALWAYS_INLINE Value VM::call(Function& function, Value this_value, MarkedValueList arguments) { return call_internal(function, this_value, move(arguments)); }

template<>
[[nodiscard]] ALWAYS_INLINE Value VM::call(Function& function, Value this_value, Optional<MarkedValueList> arguments) { return call_internal(function, this_value, move(arguments)); }

template<>
[[nodiscard]] ALWAYS_INLINE Value VM::call(Function& function, Value this_value) { return call(function, this_value, Optional<MarkedValueList> {}); }

}
