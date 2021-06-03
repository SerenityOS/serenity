/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/Block.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS::Bytecode {

Interpreter::Interpreter(GlobalObject& global_object)
    : m_vm(global_object.vm())
    , m_global_object(global_object)
{
}

Interpreter::~Interpreter()
{
}

void Interpreter::run(Bytecode::Block const& block)
{
    dbgln("Bytecode::Interpreter will run block {:p}", &block);

    m_registers.resize(block.register_count());

    for (auto& instruction : block.instructions())
        instruction.execute(*this);

    dbgln("Bytecode::Interpreter did run block {:p}", &block);
    for (size_t i = 0; i < m_registers.size(); ++i) {
        String value_string;
        if (m_registers[i].is_empty())
            value_string = "(empty)";
        else
            value_string = m_registers[i].to_string_without_side_effects();
        dbgln("[{:3}] {}", i, value_string);
    }
}

}
