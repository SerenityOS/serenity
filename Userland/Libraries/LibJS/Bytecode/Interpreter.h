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
        auto result_and_return_register = run_executable(executable, entry_point);
        return move(result_and_return_register.value);
    }

    struct ResultAndReturnRegister {
        ThrowCompletionOr<Value> value;
        Value return_register_value;
    };
    ResultAndReturnRegister run_executable(Bytecode::Executable&, Bytecode::BasicBlock const* entry_point);

    ALWAYS_INLINE Value& accumulator() { return reg(Register::accumulator()); }
    ALWAYS_INLINE Value& saved_return_value() { return reg(Register::saved_return_value()); }
    Value& reg(Register const& r)
    {
        return vm().running_execution_context().registers[r.index()];
    }
    Value reg(Register const& r) const
    {
        return vm().running_execution_context().registers[r.index()];
    }

    [[nodiscard]] Value get(Operand) const;
    void set(Operand, Value);

    void do_return(Value value)
    {
        reg(Register::return_value()) = value;
        reg(Register::exception()) = {};
    }

    void enter_unwind_context();
    void leave_unwind_context();
    void catch_exception(Operand dst);
    void restore_scheduled_jump();
    void leave_finally();

    void enter_object_environment(Object&);

    Executable& current_executable() { return *m_current_executable; }
    Executable const& current_executable() const { return *m_current_executable; }
    BasicBlock const& current_block() const { return *m_current_block; }
    Optional<InstructionStreamIterator const&> instruction_stream_iterator() const { return m_pc; }

    Vector<Value>& registers() { return vm().running_execution_context().registers; }
    Vector<Value> const& registers() const { return vm().running_execution_context().registers; }

private:
    void run_bytecode();

    VM& m_vm;
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
