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

struct ExecutionContext {
    const ASTNode* current_node { nullptr };
    FlyString function_name;
    FunctionObject* function { nullptr };
    Value this_value;
    Vector<Value> arguments;
    Object* arguments_object { nullptr };
    Environment* lexical_environment { nullptr };
    Environment* variable_environment { nullptr };
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
    void dump_environment_chain() const;

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

    void push_execution_context(ExecutionContext& context, GlobalObject& global_object)
    {
        VERIFY(!exception());
        // Ensure we got some stack space left, so the next function call doesn't kill us.
        // Note: the 32 kiB used to be 16 kiB, but that turned out to not be enough with ASAN enabled.
        if (m_stack_info.size_free() < 32 * KiB)
            throw_exception<Error>(global_object, "Call stack size limit exceeded");
        else
            m_execution_context_stack.append(&context);
    }

    void pop_execution_context()
    {
        m_execution_context_stack.take_last();
        if (m_execution_context_stack.is_empty() && on_call_stack_emptied)
            on_call_stack_emptied();
    }

    ExecutionContext& running_execution_context() { return *m_execution_context_stack.last(); }
    ExecutionContext const& running_execution_context() const { return *m_execution_context_stack.last(); }
    Vector<ExecutionContext*> const& execution_context_stack() const { return m_execution_context_stack; }
    Vector<ExecutionContext*>& execution_context_stack() { return m_execution_context_stack; }

    Environment const* lexical_environment() const { return running_execution_context().lexical_environment; }
    Environment* lexical_environment() { return running_execution_context().lexical_environment; }

    Environment const* variable_environment() const { return running_execution_context().variable_environment; }
    Environment* variable_environment() { return running_execution_context().variable_environment; }

    bool in_strict_mode() const;

    template<typename Callback>
    void for_each_argument(Callback callback)
    {
        if (m_execution_context_stack.is_empty())
            return;
        for (auto& value : running_execution_context().arguments)
            callback(value);
    }

    size_t argument_count() const
    {
        if (m_execution_context_stack.is_empty())
            return 0;
        return running_execution_context().arguments.size();
    }

    Value argument(size_t index) const
    {
        if (m_execution_context_stack.is_empty())
            return {};
        auto& arguments = running_execution_context().arguments;
        return index < arguments.size() ? arguments[index] : js_undefined();
    }

    Value this_value(Object& global_object) const
    {
        if (m_execution_context_stack.is_empty())
            return &global_object;
        return running_execution_context().this_value;
    }

    Value resolve_this_binding(GlobalObject&);

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
    void set_variable(const FlyString& name, Value, GlobalObject&, bool first_assignment = false, Environment* specific_scope = nullptr);
    bool delete_variable(FlyString const& name);
    void assign(const Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>>& target, Value, GlobalObject&, bool first_assignment = false, Environment* specific_scope = nullptr);
    void assign(const FlyString& target, Value, GlobalObject&, bool first_assignment = false, Environment* specific_scope = nullptr);
    void assign(const NonnullRefPtr<BindingPattern>& target, Value, GlobalObject&, bool first_assignment = false, Environment* specific_scope = nullptr);

    Reference resolve_binding(FlyString const&, Environment* = nullptr);
    Reference get_identifier_reference(Environment*, FlyString const&, bool strict);

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

    Value construct(FunctionObject&, FunctionObject& new_target, Optional<MarkedValueList> arguments);

    String join_arguments(size_t start_index = 0) const;

    Value get_new_target();

    template<typename... Args>
    [[nodiscard]] ALWAYS_INLINE Value call(FunctionObject& function, Value this_value, Args... args)
    {
        if constexpr (sizeof...(Args) > 0) {
            MarkedValueList arglist { heap() };
            (..., arglist.append(move(args)));
            return call(function, this_value, move(arglist));
        }

        return call(function, this_value);
    }

    CommonPropertyNames names;

    void run_queued_promise_jobs();
    void enqueue_promise_job(NativeFunction&);

    void run_queued_finalization_registry_cleanup_jobs();
    void enqueue_finalization_registry_cleanup_job(FinalizationRegistry&);

    void promise_rejection_tracker(const Promise&, Promise::RejectionOperation) const;

    Function<void()> on_call_stack_emptied;
    Function<void(const Promise&)> on_promise_unhandled_rejection;
    Function<void(const Promise&)> on_promise_rejection_handled;

private:
    VM();

    void ordinary_call_bind_this(FunctionObject&, ExecutionContext&, Value this_argument);

    [[nodiscard]] Value call_internal(FunctionObject&, Value this_value, Optional<MarkedValueList> arguments);
    void prepare_for_ordinary_call(FunctionObject&, ExecutionContext& callee_context, Value new_target);

    Exception* m_exception { nullptr };

    Heap m_heap;
    Vector<Interpreter*> m_interpreters;

    Vector<ExecutionContext*> m_execution_context_stack;

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

    bool m_underscore_is_last_value { false };

    u32 m_execution_generation { 0 };
};

template<>
[[nodiscard]] ALWAYS_INLINE Value VM::call(FunctionObject& function, Value this_value, MarkedValueList arguments) { return call_internal(function, this_value, move(arguments)); }

template<>
[[nodiscard]] ALWAYS_INLINE Value VM::call(FunctionObject& function, Value this_value, Optional<MarkedValueList> arguments) { return call_internal(function, this_value, move(arguments)); }

template<>
[[nodiscard]] ALWAYS_INLINE Value VM::call(FunctionObject& function, Value this_value) { return call(function, this_value, Optional<MarkedValueList> {}); }

ALWAYS_INLINE Heap& Cell::heap() const
{
    return HeapBlock::from_cell(this)->heap();
}

ALWAYS_INLINE VM& Cell::vm() const
{
    return heap().vm();
}

}
