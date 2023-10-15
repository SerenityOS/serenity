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
    void compile_load_immediate(Bytecode::Op::LoadImmediate const&);
    void compile_load(Bytecode::Op::Load const&);
    void compile_store(Bytecode::Op::Store const&);
    void compile_get_local(Bytecode::Op::GetLocal const&);
    void compile_set_local(Bytecode::Op::SetLocal const&);
    void compile_jump(Bytecode::Op::Jump const&);
    void compile_jump_conditional(Bytecode::Op::JumpConditional const&);
    void compile_less_than(Bytecode::Op::LessThan const&);
    void compile_increment(Bytecode::Op::Increment const&);

    void store_vm_register(Bytecode::Register, Assembler::Reg);
    void load_vm_register(Assembler::Reg, Bytecode::Register);

    void store_vm_local(size_t, Assembler::Reg);
    void load_vm_local(Assembler::Reg, size_t);

    void compile_to_boolean(Assembler::Reg dst, Assembler::Reg src);

    Vector<u8> m_output;
    Assembler m_assembler { m_output };
};

}
