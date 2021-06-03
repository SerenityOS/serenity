/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibJS/Bytecode/Block.h>
#include <LibJS/Bytecode/Instruction.h>

namespace JS::Bytecode {

NonnullOwnPtr<Block> Block::create()
{
    return adopt_own(*new Block);
}

Block::Block()
{
}

Block::~Block()
{
}

void Block::append(Badge<Bytecode::Generator>, NonnullOwnPtr<Instruction> instruction)
{
    m_instructions.append(move(instruction));
}

void Block::dump() const
{
    for (size_t i = 0; i < m_instructions.size(); ++i) {
        warnln("[{:3}] {}", i, m_instructions[i].to_string());
    }
}

}
