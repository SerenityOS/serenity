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
    node.generate_bytecode(generator);
    generator.m_block->set_register_count({}, generator.m_next_register);
    generator.m_block->seal();
    return move(generator.m_block);
}

void Generator::grow(size_t additional_size)
{
    m_block->grow(additional_size);
}

void* Generator::next_slot()
{
    return m_block->next_slot();
}

Register Generator::allocate_register()
{
    VERIFY(m_next_register != NumericLimits<u32>::max());
    return Register { m_next_register++ };
}

Label Generator::make_label() const
{
    return Label { m_block->instruction_stream().size() };
}

Label Generator::nearest_continuable_scope() const
{
    return m_continuable_scopes.last();
}

void Generator::begin_continuable_scope()
{
    m_continuable_scopes.append(make_label());
}

void Generator::end_continuable_scope()
{
    m_continuable_scopes.take_last();
}

}
