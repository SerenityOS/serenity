/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Bytecode/Executable.h>
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Runtime/FunctionKind.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Bytecode {

class InstructionStreamIterator;

struct CallFrame {
    static NonnullOwnPtr<CallFrame> create(size_t register_count);

    void operator delete(void* ptr) { free(ptr); }

    void visit_edges(Cell::Visitor& visitor)
    {
        for (auto const& value : registers())
            visitor.visit(value);
        for (auto const& environment : saved_lexical_environments)
            visitor.visit(environment);
        for (auto& context : unwind_contexts) {
            visitor.visit(context.lexical_environment);
        }
    }

    Vector<GCPtr<Environment>> saved_lexical_environments;
    Vector<UnwindInfo> unwind_contexts;
    Vector<BasicBlock const*> previously_scheduled_jumps;

    Span<Value> registers() { return { register_values, register_count }; }
    ReadonlySpan<Value> registers() const { return { register_values, register_count }; }

    size_t register_count { 0 };
    Value register_values[];
};

class Interpreter {
public:
    explicit Interpreter(VM&);
    ~Interpreter();

    [[nodiscard]] Realm& realm() { return *m_realm; }
    [[nodiscard]] Object& global_object() { return *m_global_object; }
    [[nodiscard]] DeclarativeEnvironment& global_declarative_environment() { return *m_global_declarative_environment; }
    VM& vm() { return m_vm; }
    VM const& vm() const { return m_vm; }

    ThrowCompletionOr<Value> run(Script&, JS::GCPtr<Environment> lexical_environment_override = nullptr);
    ThrowCompletionOr<Value> run(SourceTextModule&);

    ThrowCompletionOr<Value> run(Bytecode::Executable& executable, Bytecode::BasicBlock const* entry_point = nullptr)
    {
        auto value_and_frame = run_and_return_frame(executable, entry_point);
        return move(value_and_frame.value);
    }

    struct ValueAndFrame {
        ThrowCompletionOr<Value> value;
        OwnPtr<CallFrame> frame;
    };
    ValueAndFrame run_and_return_frame(Bytecode::Executable&, Bytecode::BasicBlock const* entry_point, CallFrame* = nullptr);

    ALWAYS_INLINE Value& accumulator() { return reg(Register::accumulator()); }
    ALWAYS_INLINE Value& saved_return_value() { return reg(Register::saved_return_value()); }
    Value& reg(Register const& r) { return registers()[r.index()]; }
    Value reg(Register const& r) const { return registers()[r.index()]; }

    [[nodiscard]] Value get(Operand) const;
    void set(Operand, Value);

    auto& saved_lexical_environment_stack() { return call_frame().saved_lexical_environments; }
    auto& unwind_contexts() { return call_frame().unwind_contexts; }

    void do_return(Value value)
    {
        reg(Register::return_value()) = value;
        reg(Register::exception()) = {};
    }

    void enter_unwind_context();
    void leave_unwind_context();
    void catch_exception(Operand dst);

    void enter_object_environment(Object&);

    Executable& current_executable() { return *m_current_executable; }
    Executable const& current_executable() const { return *m_current_executable; }
    BasicBlock const& current_block() const { return *m_current_block; }
    Optional<InstructionStreamIterator const&> instruction_stream_iterator() const { return m_pc; }

    void visit_edges(Cell::Visitor&);

    Span<Value> registers() { return m_current_call_frame; }
    ReadonlySpan<Value> registers() const { return m_current_call_frame; }

private:
    void run_bytecode();

    CallFrame& call_frame()
    {
        return m_call_frames.last().visit([](auto& x) -> CallFrame& { return *x; });
    }

    CallFrame const& call_frame() const
    {
        return const_cast<Interpreter*>(this)->call_frame();
    }

    void push_call_frame(Variant<NonnullOwnPtr<CallFrame>, CallFrame*>);
    [[nodiscard]] Variant<NonnullOwnPtr<CallFrame>, CallFrame*> pop_call_frame();

    VM& m_vm;
    Vector<Variant<NonnullOwnPtr<CallFrame>, CallFrame*>> m_call_frames;
    Span<Value> m_current_call_frame;
    BasicBlock const* m_scheduled_jump { nullptr };
    GCPtr<Executable> m_current_executable { nullptr };
    BasicBlock const* m_current_block { nullptr };
    GCPtr<Realm> m_realm { nullptr };
    GCPtr<Object> m_global_object { nullptr };
    GCPtr<DeclarativeEnvironment> m_global_declarative_environment { nullptr };
    Optional<InstructionStreamIterator&> m_pc {};
};

extern bool g_dump_bytecode;

ThrowCompletionOr<NonnullGCPtr<Bytecode::Executable>> compile(VM&, ASTNode const&, ReadonlySpan<FunctionParameter>, JS::FunctionKind kind, DeprecatedFlyString const& name);

}
