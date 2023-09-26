/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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
    void visit_edges(Cell::Visitor& visitor)
    {
        for (auto const& value : registers)
            visitor.visit(value);
        for (auto const& environment : saved_lexical_environments)
            visitor.visit(environment);
        for (auto& context : unwind_contexts) {
            visitor.visit(context.lexical_environment);
        }
    }
    Vector<Value> registers;
    Vector<GCPtr<Environment>> saved_lexical_environments;
    Vector<UnwindInfo> unwind_contexts;
};

class Interpreter {
public:
    explicit Interpreter(VM&);
    ~Interpreter();

    Realm& realm();
    VM& vm() { return m_vm; }

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

    auto& saved_lexical_environment_stack() { return call_frame().saved_lexical_environments; }
    auto& unwind_contexts() { return call_frame().unwind_contexts; }

    void jump(Label const& label)
    {
        m_pending_jump = &label.block();
    }
    void schedule_jump(Label const& label)
    {
        m_scheduled_jump = &label.block();
        VERIFY(unwind_contexts().last().finalizer);
        jump(Label { *unwind_contexts().last().finalizer });
    }
    void do_return(Value return_value)
    {
        m_return_value = return_value;
        reg(Register::exception()) = {};
    }

    void enter_unwind_context(Optional<Label> handler_target, Optional<Label> finalizer_target);
    void leave_unwind_context();
    ThrowCompletionOr<void> continue_pending_unwind(Label const& resume_label);

    Executable& current_executable() { return *m_current_executable; }
    Executable const& current_executable() const { return *m_current_executable; }
    BasicBlock const& current_block() const { return *m_current_block; }
    auto& instruction_stream_iterator() const { return m_pc; }
    DeprecatedString debug_position() const;

    void visit_edges(Cell::Visitor&);

private:
    CallFrame& call_frame()
    {
        return m_call_frames.last().visit([](auto& x) -> CallFrame& { return *x; });
    }

    CallFrame const& call_frame() const
    {
        return const_cast<Interpreter*>(this)->call_frame();
    }

    Span<Value> registers() { return m_current_call_frame; }
    ReadonlySpan<Value> registers() const { return m_current_call_frame; }

    void push_call_frame(Variant<NonnullOwnPtr<CallFrame>, CallFrame*>, size_t register_count);
    [[nodiscard]] Variant<NonnullOwnPtr<CallFrame>, CallFrame*> pop_call_frame();

    VM& m_vm;
    Vector<Variant<NonnullOwnPtr<CallFrame>, CallFrame*>> m_call_frames;
    Span<Value> m_current_call_frame;
    Optional<BasicBlock const*> m_pending_jump;
    BasicBlock const* m_scheduled_jump { nullptr };
    Optional<Value> m_return_value;
    Executable* m_current_executable { nullptr };
    BasicBlock const* m_current_block { nullptr };
    Optional<InstructionStreamIterator&> m_pc {};
};

extern bool g_dump_bytecode;

ThrowCompletionOr<NonnullOwnPtr<Bytecode::Executable>> compile(VM&, ASTNode const& no, JS::FunctionKind kind, DeprecatedFlyString const& name);

}
