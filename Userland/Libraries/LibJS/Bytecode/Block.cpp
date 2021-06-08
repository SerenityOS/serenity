/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/Block.h>
#include <LibJS/Bytecode/Op.h>

namespace JS::Bytecode {

NonnullOwnPtr<Block> Block::create()
{
    return adopt_own(*new Block);
}

Block::~Block()
{
    Bytecode::InstructionStreamIterator it(instruction_stream());
    while (!it.at_end()) {
        auto& to_destroy = (*it);
        ++it;
        Instruction::destroy(const_cast<Instruction&>(to_destroy));
    }
}

void Block::dump() const
{
    Bytecode::InstructionStreamIterator it(instruction_stream());
    while (!it.at_end()) {
        warnln("[{:4x}] {}", it.offset(), (*it).to_string());
        ++it;
    }
}

void InstructionStreamIterator::operator++()
{
    m_offset += dereference().length();
}

}
