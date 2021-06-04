/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Runtime/GlobalObject.h>
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

void Sub::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = sub(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void LessThan::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = less_than(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void NewString::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = js_string(interpreter.vm(), m_string);
}

void GetVariable::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = interpreter.vm().get_variable(m_identifier, interpreter.global_object());
}

void SetVariable::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.vm().set_variable(m_identifier, interpreter.reg(m_src), interpreter.global_object());
}

String Load::to_string() const
{
    return String::formatted("Load dst:{}, value:{}", m_dst, m_value.to_string_without_side_effects());
}

String Add::to_string() const
{
    return String::formatted("Add dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String Sub::to_string() const
{
    return String::formatted("Sub dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String LessThan::to_string() const
{
    return String::formatted("LessThan dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String NewString::to_string() const
{
    return String::formatted("NewString dst:{}, string:\"{}\"", m_dst, m_string);
}

String GetVariable::to_string() const
{
    return String::formatted("GetVariable dst:{}, identifier:{}", m_dst, m_identifier);
}

String SetVariable::to_string() const
{
    return String::formatted("SetVariable identifier:{}, src:{}", m_identifier, m_src);
}

}
