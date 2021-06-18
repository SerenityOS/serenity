/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/StackInfo.h>
#include <AK/Variant.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/CommonPropertyNames.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/ErrorTypes.h>
#include <LibJS/Runtime/Exception.h>
#include <LibJS/Runtime/MarkedValueList.h>
#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Identifier;
struct BindingPattern;

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
    const ASTNode* current_node { nullptr };
    FlyString function_name;
    Value callee;
    Value this_value;
    Vector<Value> arguments;
    Array* arguments_object { nullptr };
    ScopeObject* scope { nullptr };
    bool is_strict_mode { false };
};

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

    Exception* exception() { return m_exception; }
    void set_exception(Exception& exception) { m_exception = &exception; }
    void clear_exception() { m_exception = nullptr; }

    void dump_backtrace() const;

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
    PrimitiveString& single_ascii_character_string(u8 character)
    {
        VERIFY(character < 0x80);
        return *m_single_ascii_character_strings[character];
    }

    void push_call_frame(CallFrame& call_frame, GlobalObject& global_object)
    {
        VERIFY(!exception());
        // Ensure we got some stack space left, so the next function call doesn't kill us.
        // Note: the 32 kiB used to be 16 kiB, but that turned out to not be enough with ASAN enabled.
        if (m_stack_info.size_free() < 32 * KiB)
            throw_exception<Error>(global_object, "Call stack size limit exceeded");
        else
            m_call_stack.append(&call_frame);
    }

    void pop_call_frame()
    {
        m_call_stack.take_last();
        if (m_call_stack.is_empty() && on_call_stack_emptied)
            on_call_stack_emptied();
    }

    CallFrame& call_frame() { return *m_call_stack.last(); }
    const CallFrame& call_frame() const { return *m_call_stack.last(); }
    const Vector<CallFrame*>& call_stack() const { return m_call_stack; }
    Vector<CallFrame*>& call_stack() { return m_call_stack; }

    const ScopeObject* current_scope() const { return call_frame().scope; }
    ScopeObject* current_scope() { return call_frame().scope; }

    bool in_strict_mode() const;

    template<typename Callback>
    void for_each_argument(Callback callback)
    {
        if (m_call_stack.is_empty())
            return;
        for (auto& value : call_frame().arguments)
            callback(value);
    }

    size_t argument_count() const
    {
        if (m_call_stack.is_empty())
            return 0;
        return call_frame().arguments.size();
    }

    Value argument(size_t index) const
    {
        if (m_call_stack.is_empty())
            return {};
        auto& arguments = call_frame().arguments;
        return index < arguments.size() ? arguments[index] : js_undefined();
    }

    Value this_value(Object& global_object) const
    {
        if (m_call_stack.is_empty())
            return &global_object;
        return call_frame().this_value;
    }

    Value last_value() const { return m_last_value; }
    void set_last_value(Badge<Bytecode::Interpreter>, Value value) { m_last_value = value; }
    void set_last_value(Badge<Interpreter>, Value value) { m_last_value = value; }

    const StackInfo& stack_info() const { return m_stack_info; };

    bool underscore_is_last_value() const { return m_underscore_is_last_value; }
    void set_underscore_is_last_value(bool b) { m_underscore_is_last_value = b; }

    u32 execution_generation() const { return m_execution_generation; }
    void finish_execution_generation() { ++m_execution_generation; }

    void unwind(ScopeType type, FlyString label = {})
    {
        m_unwind_until = type;
        m_unwind_until_label = move(label);
    }
    void stop_unwind()
    {
        m_unwind_until = ScopeType::None;
        m_unwind_until_label = {};
    }
    bool should_unwind_until(ScopeType type, FlyString const& label) const
    {
        if (m_unwind_until_label.is_null())
            return m_unwind_until == type;
        return m_unwind_until == type && m_unwind_until_label == label;
    }
    bool should_unwind() const { return m_unwind_until != ScopeType::None; }

    ScopeType unwind_until() const { return m_unwind_until; }
    FlyString unwind_until_label() const { return m_unwind_until_label; }

    Value get_variable(const FlyString& name, GlobalObject&);
    void set_variable(const FlyString& name, Value, GlobalObject&, bool first_assignment = false, ScopeObject* specific_scope = nullptr);
    bool delete_variable(FlyString const& name);
    void assign(const Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>>& target, Value, GlobalObject&, bool first_assignment = false, ScopeObject* specific_scope = nullptr);
    void assign(const FlyString& target, Value, GlobalObject&, bool first_assignment = false, ScopeObject* specific_scope = nullptr);
    void assign(const NonnullRefPtr<BindingPattern>& target, Value, GlobalObject&, bool first_assignment = false, ScopeObject* specific_scope = nullptr);

    Reference get_reference(const FlyString& name);

    template<typename T, typename... Args>
    void throw_exception(GlobalObject& global_object, Args&&... args)
    {
        return throw_exception(global_object, T::create(global_object, forward<Args>(args)...));
    }

    void throw_exception(Exception&);
    void throw_exception(GlobalObject& global_object, Value value)
    {
        return throw_exception(*heap().allocate<Exception>(global_object, value));
    }

    template<typename T, typename... Args>
    void throw_exception(GlobalObject& global_object, ErrorType type, Args&&... args)
    {
        return throw_exception(global_object, T::create(global_object, String::formatted(type.message(), forward<Args>(args)...)));
    }

    Value construct(Function&, Function& new_target, Optional<MarkedValueList> arguments);

    String join_arguments(size_t start_index = 0) const;

    Value resolve_this_binding(GlobalObject&) const;
    const ScopeObject* find_this_scope() const;
    Value get_new_target() const;

    template<typename... Args>
    [[nodiscard]] ALWAYS_INLINE Value call(Function& function, Value this_value, Args... args)
    {
        if constexpr (sizeof...(Args) > 0) {
            MarkedValueList arglist { heap() };
            (..., arglist.append(move(args)));
            return call(function, this_value, move(arglist));
        }

        return call(function, this_value);
    }

    CommonPropertyNames names;

    Shape& scope_object_shape() { return *m_scope_object_shape; }

    void run_queued_promise_jobs();
    void enqueue_promise_job(NativeFunction&);

    void run_queued_finalization_registry_cleanup_jobs();
    void enqueue_finalization_registry_cleanup_job(FinalizationRegistry&);

    void promise_rejection_tracker(const Promise&, Promise::RejectionOperation) const;

    AK::Function<void()> on_call_stack_emptied;
    AK::Function<void(const Promise&)> on_promise_unhandled_rejection;
    AK::Function<void(const Promise&)> on_promise_rejection_handled;

private:
    VM();

    [[nodiscard]] Value call_internal(Function&, Value this_value, Optional<MarkedValueList> arguments);

    Exception* m_exception { nullptr };

    Heap m_heap;
    Vector<Interpreter*> m_interpreters;

    Vector<CallFrame*> m_call_stack;

    Value m_last_value;
    ScopeType m_unwind_until { ScopeType::None };
    FlyString m_unwind_until_label;

    StackInfo m_stack_info;

    HashMap<String, Symbol*> m_global_symbol_map;

    Vector<NativeFunction*> m_promise_jobs;

    Vector<FinalizationRegistry*> m_finalization_registry_cleanup_jobs;

    PrimitiveString* m_empty_string { nullptr };
    PrimitiveString* m_single_ascii_character_strings[128] {};

#define __JS_ENUMERATE(SymbolName, snake_name) \
    Symbol* m_well_known_symbol_##snake_name { nullptr };
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE

    Shape* m_scope_object_shape { nullptr };

    bool m_underscore_is_last_value { false };

    u32 m_execution_generation { 0 };
};

template<>
[[nodiscard]] ALWAYS_INLINE Value VM::call(Function& function, Value this_value, MarkedValueList arguments) { return call_internal(function, this_value, move(arguments)); }

template<>
[[nodiscard]] ALWAYS_INLINE Value VM::call(Function& function, Value this_value, Optional<MarkedValueList> arguments) { return call_internal(function, this_value, move(arguments)); }

template<>
[[nodiscard]] ALWAYS_INLINE Value VM::call(Function& function, Value this_value) { return call(function, this_value, Optional<MarkedValueList> {}); }

ALWAYS_INLINE Heap& Cell::heap() const
{
    return HeapBlock::from_cell(this)->heap();
}

ALWAYS_INLINE VM& Cell::vm() const
{
    return heap().vm();
}

}
