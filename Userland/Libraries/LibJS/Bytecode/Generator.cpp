/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/Block.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Forward.h>

namespace JS::Bytecode {

Generator::Generator()
{
    m_block = Block::create();
}

Generator::~Generator()
{
}

OwnPtr<Block> Generator::generate(ASTNode const& node)
{
    Generator generator;
    [[maybe_unused]] auto dummy = node.generate_bytecode(generator);
    generator.m_block->set_register_count({}, generator.m_next_register);
    return move(generator.m_block);
}

void Generator::append(NonnullOwnPtr<Instruction> instruction)
{
    m_block->append({}, move(instruction));
}

Register Generator::allocate_register()
{
    VERIFY(m_next_register != NumericLimits<u32>::max());
    return Register { m_next_register++ };
}

}
