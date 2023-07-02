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
class PassManager;

struct RegisterWindow {
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
    [[nodiscard]] static bool enabled();
    static void set_enabled(bool);
    static void set_optimizations_enabled(bool);

    explicit Interpreter(VM&);
    ~Interpreter();

    Realm& realm();
    VM& vm() { return m_vm; }

    ThrowCompletionOr<Value> run(Script&, JS::GCPtr<Environment> lexical_environment_override = nullptr);
    ThrowCompletionOr<Value> run(SourceTextModule&);

    ThrowCompletionOr<Value> run(Realm& realm, Bytecode::Executable const& executable, Bytecode::BasicBlock const* entry_point = nullptr)
    {
        auto value_and_frame = run_and_return_frame(realm, executable, entry_point);
        return move(value_and_frame.value);
    }

    struct ValueAndFrame {
        ThrowCompletionOr<Value> value;
        OwnPtr<RegisterWindow> frame;
    };
    ValueAndFrame run_and_return_frame(Realm&, Bytecode::Executable const&, Bytecode::BasicBlock const* entry_point, RegisterWindow* = nullptr);

    ALWAYS_INLINE Value& accumulator() { return reg(Register::accumulator()); }
    Value& reg(Register const& r) { return registers()[r.index()]; }

    auto& saved_lexical_environment_stack() { return window().saved_lexical_environments; }
    auto& unwind_contexts() { return window().unwind_contexts; }

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
        m_saved_exception = {};
    }

    void enter_unwind_context(Optional<Label> handler_target, Optional<Label> finalizer_target);
    void leave_unwind_context();
    ThrowCompletionOr<void> continue_pending_unwind(Label const& resume_label);

    Executable const& current_executable() { return *m_current_executable; }
    BasicBlock const& current_block() const { return *m_current_block; }
    size_t pc() const;
    DeprecatedString debug_position() const;

    static Bytecode::PassManager& optimization_pipeline();

    VM::InterpreterExecutionScope ast_interpreter_scope(Realm&);

    void visit_edges(Cell::Visitor&);

private:
    RegisterWindow& window()
    {
        return m_register_windows.last().visit([](auto& x) -> RegisterWindow& { return *x; });
    }

    RegisterWindow const& window() const
    {
        return const_cast<Interpreter*>(this)->window();
    }

    Span<Value> registers() { return m_current_register_window; }
    ReadonlySpan<Value> registers() const { return m_current_register_window; }

    void push_register_window(Variant<NonnullOwnPtr<RegisterWindow>, RegisterWindow*>, size_t register_count);
    [[nodiscard]] Variant<NonnullOwnPtr<RegisterWindow>, RegisterWindow*> pop_register_window();

    VM& m_vm;
    Vector<Variant<NonnullOwnPtr<RegisterWindow>, RegisterWindow*>> m_register_windows;
    Span<Value> m_current_register_window;
    Optional<BasicBlock const*> m_pending_jump;
    BasicBlock const* m_scheduled_jump { nullptr };
    Optional<Value> m_return_value;
    Optional<Value> m_saved_return_value;
    Optional<Value> m_saved_exception;
    Executable const* m_current_executable { nullptr };
    OwnPtr<JS::Interpreter> m_ast_interpreter;
    BasicBlock const* m_current_block { nullptr };
    InstructionStreamIterator* m_pc { nullptr };
};

extern bool g_dump_bytecode;

ThrowCompletionOr<NonnullOwnPtr<Bytecode::Executable>> compile(VM&, ASTNode const& no, JS::FunctionKind kind, DeprecatedFlyString const& name);

}
