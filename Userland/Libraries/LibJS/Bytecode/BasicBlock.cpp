/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Op.h>

namespace JS::Bytecode {

NonnullOwnPtr<BasicBlock> BasicBlock::create(DeprecatedString name)
{
    return adopt_own(*new BasicBlock(move(name)));
}

BasicBlock::BasicBlock(DeprecatedString name)
    : m_name(move(name))
{
}

BasicBlock::~BasicBlock()
{
    Bytecode::InstructionStreamIterator it(instruction_stream());
    while (!it.at_end()) {
        auto& to_destroy = (*it);
        ++it;
        Instruction::destroy(const_cast<Instruction&>(to_destroy));
    }
}

void BasicBlock::dump(Bytecode::Executable const& executable) const
{
    Bytecode::InstructionStreamIterator it(instruction_stream());
    if (!m_name.is_empty())
        warnln("{}:", m_name);
    while (!it.at_end()) {
        warnln("[{:4x}] {}", it.offset(), (*it).to_deprecated_string(executable));
        ++it;
    }
}

void BasicBlock::grow(size_t additional_size)
{
    m_buffer.grow_capacity(m_buffer.size() + additional_size);
    m_buffer.resize(m_buffer.size() + additional_size);
}

}
