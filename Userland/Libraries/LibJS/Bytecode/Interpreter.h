/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Generator.h"
#include "PassManager.h"
#include <LibJS/Bytecode/Label.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Bytecode {

struct RegisterWindow {
    MarkedVector<Value> registers;
    MarkedVector<Environment*> saved_lexical_environments;
    MarkedVector<Environment*> saved_variable_environments;
    Vector<UnwindInfo> unwind_contexts;
};

class Interpreter {
public:
    explicit Interpreter(Realm&);
    ~Interpreter();

    // FIXME: Remove this thing once we don't need it anymore!
    static Interpreter* current();

    Realm& realm() { return m_realm; }
    VM& vm() { return m_vm; }

    ThrowCompletionOr<Value> run(Bytecode::Executable const& executable, Bytecode::BasicBlock const* entry_point = nullptr)
    {
        auto value_and_frame = run_and_return_frame(executable, entry_point);
        return move(value_and_frame.value);
    }

    struct ValueAndFrame {
        ThrowCompletionOr<Value> value;
        OwnPtr<RegisterWindow> frame;
    };
    ValueAndFrame run_and_return_frame(Bytecode::Executable const&, Bytecode::BasicBlock const* entry_point, RegisterWindow* = nullptr);

    ALWAYS_INLINE Value& accumulator() { return reg(Register::accumulator()); }
    Value& reg(Register const& r) { return registers()[r.index()]; }

    auto& saved_lexical_environment_stack() { return window().saved_lexical_environments; }
    auto& saved_variable_environment_stack() { return window().saved_variable_environments; }
    auto& unwind_contexts() { return window().unwind_contexts; }

    void jump(Label const& label)
    {
        m_pending_jump = &label.block();
    }
    void do_return(Value return_value) { m_return_value = return_value; }

    void enter_unwind_context(Optional<Label> handler_target, Optional<Label> finalizer_target);
    void leave_unwind_context();
    ThrowCompletionOr<void> continue_pending_unwind(Label const& resume_label);

    Executable const& current_executable() { return *m_current_executable; }
    BasicBlock const& current_block() const { return *m_current_block; }
    size_t pc() const { return m_pc ? m_pc->offset() : 0; }
    String debug_position()
    {
        return String::formatted("{}:{:2}:{:4x}", m_current_executable->name, m_current_block->name(), pc());
    }

    enum class OptimizationLevel {
        None,
        Optimize,
        __Count,
        Default = None,
    };
    static Bytecode::PassManager& optimization_pipeline(OptimizationLevel = OptimizationLevel::Default);

    VM::InterpreterExecutionScope ast_interpreter_scope();

private:
    RegisterWindow& window()
    {
        return m_register_windows.last().visit([](auto& x) -> RegisterWindow& { return *x; });
    }

    RegisterWindow const& window() const
    {
        return const_cast<Interpreter*>(this)->window();
    }

    MarkedVector<Value>& registers() { return window().registers; }

    static AK::Array<OwnPtr<PassManager>, static_cast<UnderlyingType<Interpreter::OptimizationLevel>>(Interpreter::OptimizationLevel::__Count)> s_optimization_pipelines;

    VM& m_vm;
    Realm& m_realm;
    Vector<Variant<NonnullOwnPtr<RegisterWindow>, RegisterWindow*>> m_register_windows;
    Optional<BasicBlock const*> m_pending_jump;
    Value m_return_value;
    Executable const* m_current_executable { nullptr };
    Handle<Value> m_saved_exception;
    OwnPtr<JS::Interpreter> m_ast_interpreter;
    BasicBlock const* m_current_block { nullptr };
    InstructionStreamIterator* m_pc { nullptr };
};

extern bool g_dump_bytecode;

}
