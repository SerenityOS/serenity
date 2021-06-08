/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Op.h>

namespace JS::Bytecode {

void Instruction::destroy(Instruction& instruction)
{
#define __BYTECODE_OP(op)                        \
    case Type::op:                               \
        static_cast<Op::op&>(instruction).~op(); \
        return;

    switch (instruction.type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }

#undef __BYTECODE_OP
}

size_t Instruction::length() const
{
    if (type() == Type::Call)
        return static_cast<Op::Call const&>(*this).length();

#define __BYTECODE_OP(op) \
    case Type::op:        \
        return sizeof(Op::op);

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }
}

}
