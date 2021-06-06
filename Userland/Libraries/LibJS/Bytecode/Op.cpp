/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ScriptFunction.h>
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

void AbstractInequals::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = Value(!abstract_eq(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2)));
}

void NewString::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = js_string(interpreter.vm(), m_string);
}

void NewObject::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = Object::create_empty(interpreter.global_object());
}

void GetVariable::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = interpreter.vm().get_variable(m_identifier, interpreter.global_object());
}

void SetVariable::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.vm().set_variable(m_identifier, interpreter.reg(m_src), interpreter.global_object());
}

void GetById::execute(Bytecode::Interpreter& interpreter) const
{
    if (auto* object = interpreter.reg(m_base).to_object(interpreter.global_object()))
        interpreter.reg(m_dst) = object->get(m_property);
}

void PutById::execute(Bytecode::Interpreter& interpreter) const
{
    if (auto* object = interpreter.reg(m_base).to_object(interpreter.global_object()))
        object->put(m_property, interpreter.reg(m_src));
}

void Jump::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.jump(*m_target);
}

void JumpIfFalse::execute(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_target.has_value());
    auto result = interpreter.reg(m_result);
    if (!result.as_bool())
        interpreter.jump(m_target.value());
}

void JumpIfTrue::execute(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_target.has_value());
    auto result = interpreter.reg(m_result);
    if (result.as_bool())
        interpreter.jump(m_target.value());
}

void Call::execute(Bytecode::Interpreter& interpreter) const
{
    auto callee = interpreter.reg(m_callee);
    if (!callee.is_function()) {
        TODO();
    }
    auto& function = callee.as_function();

    auto this_value = interpreter.reg(m_this_value);

    Value return_value;

    if (m_arguments.is_empty()) {
        return_value = interpreter.vm().call(function, this_value);
    } else {
        MarkedValueList argument_values { interpreter.vm().heap() };
        for (auto& arg : m_arguments) {
            argument_values.append(interpreter.reg(arg));
        }
        return_value = interpreter.vm().call(function, this_value, move(argument_values));
    }

    interpreter.reg(m_dst) = return_value;
}

void EnterScope::execute(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.vm();
    auto& global_object = interpreter.global_object();

    for (auto& declaration : m_scope_node.functions())
        vm.current_scope()->put_to_scope(declaration.name(), { js_undefined(), DeclarationKind::Var });

    for (auto& declaration : m_scope_node.functions()) {
        auto* function = ScriptFunction::create(global_object, declaration.name(), declaration.body(), declaration.parameters(), declaration.function_length(), vm.current_scope(), declaration.is_strict_mode());
        vm.set_variable(declaration.name(), function, global_object);
    }

    // FIXME: Process variable declarations.
    // FIXME: Whatever else JS::Interpreter::enter_scope() does.
}

void Return::execute(Bytecode::Interpreter& interpreter) const
{
    auto return_value = m_argument.has_value() ? interpreter.reg(m_argument.value()) : js_undefined();
    interpreter.do_return(return_value);
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

String AbstractInequals::to_string() const
{
    return String::formatted("AbstractInequals dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String NewString::to_string() const
{
    return String::formatted("NewString dst:{}, string:\"{}\"", m_dst, m_string);
}

String NewObject::to_string() const
{
    return String::formatted("NewObject dst:{}", m_dst);
}

String GetVariable::to_string() const
{
    return String::formatted("GetVariable dst:{}, identifier:{}", m_dst, m_identifier);
}

String SetVariable::to_string() const
{
    return String::formatted("SetVariable identifier:{}, src:{}", m_identifier, m_src);
}

String PutById::to_string() const
{
    return String::formatted("PutById base:{}, property:{}, src:{}", m_base, m_property, m_src);
}

String GetById::to_string() const
{
    return String::formatted("GetById dst:{}, base:{}, property:{}", m_dst, m_base, m_property);
}

String Jump::to_string() const
{
    return String::formatted("Jump {}", *m_target);
}

String JumpIfFalse::to_string() const
{
    if (m_target.has_value())
        return String::formatted("JumpIfFalse result:{}, target:{}", m_result, m_target.value());
    return String::formatted("JumpIfFalse result:{}, target:<empty>", m_result);
}

String JumpIfTrue::to_string() const
{
    if (m_target.has_value())
        return String::formatted("JumpIfTrue result:{}, target:{}", m_result, m_target.value());
    return String::formatted("JumpIfTrue result:{}, target:<empty>", m_result);
}

String Call::to_string() const
{
    StringBuilder builder;
    builder.appendff("Call dst:{}, callee:{}, this:{}", m_dst, m_callee, m_this_value);
    if (!m_arguments.is_empty()) {
        builder.append(", arguments:[");
        for (size_t i = 0; i < m_arguments.size(); ++i) {
            builder.appendff("{}", m_arguments[i]);
            if (i != m_arguments.size() - 1)
                builder.append(',');
        }
        builder.append(']');
    }
    return builder.to_string();
}

String EnterScope::to_string() const
{
    return "EnterScope";
}

String Return::to_string() const
{
    if (m_argument.has_value())
        return String::formatted("Return {}", m_argument.value());
    return "Return";
}

}
