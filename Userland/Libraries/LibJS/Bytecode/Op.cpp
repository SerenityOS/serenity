/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Bytecode::Op {

void Load::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = m_value;
}

void Add::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = add(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

String Load::to_string() const
{
    return String::formatted("Load dst:r{}, value:{}", m_dst.index(), m_value.to_string_without_side_effects());
}

String Add::to_string() const
{
    return String::formatted("Add dst:r{}, src1:r{}, src2:r{}", m_dst.index(), m_src1.index(), m_src2.index());
}

}
