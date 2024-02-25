/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/Executable.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Op.h>

namespace JS::Bytecode {

bool Instruction::is_terminator() const
{
#define __BYTECODE_OP(op) \
    case Type::op:        \
        return Op::op::IsTerminator;

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }

#undef __BYTECODE_OP
}

void Instruction::replace_references(BasicBlock const& from, BasicBlock const& to)
{
#define __BYTECODE_OP(op)       \
    case Instruction::Type::op: \
        return static_cast<Bytecode::Op::op&>(*this).replace_references_impl(from, to);

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }

#undef __BYTECODE_OP
}

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

UnrealizedSourceRange InstructionStreamIterator::source_range() const
{
    VERIFY(m_executable);
    auto record = dereference().source_record();
    return {
        .source_code = m_executable->source_code,
        .start_offset = record.source_start_offset,
        .end_offset = record.source_end_offset,
    };
}

RefPtr<SourceCode> InstructionStreamIterator::source_code() const
{
    return m_executable ? m_executable->source_code.ptr() : nullptr;
}

Operand::Operand(Register reg)
    : m_type(Type::Register)
    , m_index(reg.index())
{
}

}
