/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Op.h>

namespace JS::Bytecode {

NonnullOwnPtr<BasicBlock> BasicBlock::create(String name)
{
    return adopt_own(*new BasicBlock(move(name)));
}

BasicBlock::BasicBlock(String name)
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
        warn("{}", m_name);
    if (m_handler || m_finalizer) {
        warn(" [");
        if (m_handler)
            warn(" Handler: {}", Label { *m_handler });
        if (m_finalizer)
            warn(" Finalizer: {}", Label { *m_finalizer });
        warn(" ]");
    }
    warnln(":");
    while (!it.at_end()) {
        warnln("[{:4x}] {}", it.offset(), (*it).to_byte_string(executable));
        ++it;
    }
}

void BasicBlock::grow(size_t additional_size)
{
    m_buffer.grow_capacity(m_buffer.size() + additional_size);
    m_buffer.resize(m_buffer.size() + additional_size);
}

}
