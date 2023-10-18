/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Bytecode/Executable.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/JIT/Assembler.h>
#include <LibJS/JIT/NativeExecutable.h>

namespace JS::JIT {

class Compiler {
public:
    static OwnPtr<NativeExecutable> compile(Bytecode::Executable const&);

private:
    static constexpr auto GPR0 = Assembler::Reg::RAX;
    static constexpr auto GPR1 = Assembler::Reg::RCX;
    static constexpr auto ARG0 = Assembler::Reg::RDI;
    static constexpr auto ARG1 = Assembler::Reg::RSI;
    static constexpr auto ARG2 = Assembler::Reg::RDX;
    static constexpr auto RET = Assembler::Reg::RAX;
    static constexpr auto STACK_POINTER = Assembler::Reg::RSP;
    static constexpr auto REGISTER_ARRAY_BASE = Assembler::Reg::R8;
    static constexpr auto LOCALS_ARRAY_BASE = Assembler::Reg::R9;
    static constexpr auto UNWIND_CONTEXT_BASE = Assembler::Reg::R10;

    void compile_load_immediate(Bytecode::Op::LoadImmediate const&);
    void compile_load(Bytecode::Op::Load const&);
    void compile_store(Bytecode::Op::Store const&);
    void compile_get_local(Bytecode::Op::GetLocal const&);
    void compile_set_local(Bytecode::Op::SetLocal const&);
    void compile_jump(Bytecode::Op::Jump const&);
    void compile_jump_conditional(Bytecode::Op::JumpConditional const&);
    void compile_less_than(Bytecode::Op::LessThan const&);
    void compile_increment(Bytecode::Op::Increment const&);
    void compile_enter_unwind_context(Bytecode::Op::EnterUnwindContext const&);
    void compile_leave_unwind_context(Bytecode::Op::LeaveUnwindContext const&);
    void compile_throw(Bytecode::Op::Throw const&);
    void compile_add(Bytecode::Op::Add const&);
    void compile_sub(Bytecode::Op::Sub const&);
    void compile_mul(Bytecode::Op::Mul const&);
    void compile_div(Bytecode::Op::Div const&);

    void store_vm_register(Bytecode::Register, Assembler::Reg);
    void load_vm_register(Assembler::Reg, Bytecode::Register);

    void store_vm_local(size_t, Assembler::Reg);
    void load_vm_local(Assembler::Reg, size_t);

    void compile_to_boolean(Assembler::Reg dst, Assembler::Reg src);

    void check_exception();

    void push_unwind_context(bool valid, Optional<Bytecode::Label> const& handler, Optional<Bytecode::Label> const& finalizer);
    void pop_unwind_context();

    Vector<u8> m_output;
    Assembler m_assembler { m_output };
};

}
