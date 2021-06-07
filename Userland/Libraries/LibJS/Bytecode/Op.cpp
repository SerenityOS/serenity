/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ScriptFunction.h>
#include <LibJS/Runtime/Value.h>

namespace JS::Bytecode {

void Instruction::execute(Bytecode::Interpreter& interpreter) const
{
#define __BYTECODE_OP(op)       \
    case Instruction::Type::op: \
        return static_cast<Bytecode::Op::op const&>(*this).execute(interpreter);

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }

#undef __BYTECODE_OP
}

String Instruction::to_string() const
{
#define __BYTECODE_OP(op)       \
    case Instruction::Type::op: \
        return static_cast<Bytecode::Op::op const&>(*this).to_string();

    switch (type()) {
        ENUMERATE_BYTECODE_OPS(__BYTECODE_OP)
    default:
        VERIFY_NOT_REACHED();
    }

#undef __BYTECODE_OP
}

}

namespace JS::Bytecode::Op {

void Load::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = m_value;
}

void LoadRegister::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = interpreter.reg(m_src);
}

void Add::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = add(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void Sub::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = sub(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void Mul::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = mul(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void Div::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = div(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void Mod::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = mod(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void Exp::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = exp(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void GreaterThan::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = greater_than(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void GreaterThanEquals::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = greater_than_equals(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void LessThan::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = less_than(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void LessThanEquals::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = less_than_equals(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void AbstractInequals::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = Value(!abstract_eq(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2)));
}

void AbstractEquals::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = Value(abstract_eq(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2)));
}

void TypedInequals::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = Value(!strict_eq(interpreter.reg(m_src1), interpreter.reg(m_src2)));
}

void TypedEquals::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = Value(strict_eq(interpreter.reg(m_src1), interpreter.reg(m_src2)));
}

void BitwiseAnd::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = bitwise_and(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void BitwiseOr::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = bitwise_or(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void BitwiseXor::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = bitwise_xor(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void LeftShift::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = left_shift(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void RightShift::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = right_shift(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void UnsignedRightShift::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = unsigned_right_shift(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void In::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = in(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void InstanceOf::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = instance_of(interpreter.global_object(), interpreter.reg(m_src1), interpreter.reg(m_src2));
}

void BitwiseNot::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = bitwise_not(interpreter.global_object(), interpreter.reg(m_src));
}

void Not::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = Value(!interpreter.reg(m_src).to_boolean());
}

void UnaryPlus::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = Value(unary_plus(interpreter.global_object(), interpreter.reg(m_src)));
}

void UnaryMinus::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = Value(unary_minus(interpreter.global_object(), interpreter.reg(m_src)));
}

void Typeof::execute(Bytecode::Interpreter& interpreter) const
{
    auto& vm = interpreter.global_object().vm();
    interpreter.reg(m_dst) = Value(js_string(vm, interpreter.reg(m_src).typeof()));
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

    if (m_argument_count == 0) {
        return_value = interpreter.vm().call(function, this_value);
    } else {
        MarkedValueList argument_values { interpreter.vm().heap() };
        for (size_t i = 0; i < m_argument_count; ++i) {
            argument_values.append(interpreter.reg(m_arguments[i]));
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

String LoadRegister::to_string() const
{
    return String::formatted("LoadRegister dst:{}, src:{}", m_dst, m_src);
}

String Add::to_string() const
{
    return String::formatted("Add dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String Sub::to_string() const
{
    return String::formatted("Sub dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String Mul::to_string() const
{
    return String::formatted("Mul dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String Div::to_string() const
{
    return String::formatted("Div dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String Mod::to_string() const
{
    return String::formatted("Mod dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String Exp::to_string() const
{
    return String::formatted("Exp dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String GreaterThan::to_string() const
{
    return String::formatted("GreaterThan dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String GreaterThanEquals::to_string() const
{
    return String::formatted("GreaterThanEquals dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String LessThan::to_string() const
{
    return String::formatted("LessThan dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String LessThanEquals::to_string() const
{
    return String::formatted("LessThanEquals dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String AbstractInequals::to_string() const
{
    return String::formatted("AbstractInequals dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String AbstractEquals::to_string() const
{
    return String::formatted("AbstractEquals dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String TypedInequals::to_string() const
{
    return String::formatted("TypedInequals dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String TypedEquals::to_string() const
{
    return String::formatted("TypedEquals dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String BitwiseAnd::to_string() const
{
    return String::formatted("BitwiseAnd dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String BitwiseOr::to_string() const
{
    return String::formatted("BitwiseOr dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String BitwiseXor::to_string() const
{
    return String::formatted("BitwiseXor dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String LeftShift::to_string() const
{
    return String::formatted("LeftShift dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String RightShift::to_string() const
{
    return String::formatted("RightShift dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String UnsignedRightShift::to_string() const
{
    return String::formatted("UnsignedRightShift dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String In::to_string() const
{
    return String::formatted("In dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String InstanceOf::to_string() const
{
    return String::formatted("In dst:{}, src1:{}, src2:{}", m_dst, m_src1, m_src2);
}

String BitwiseNot::to_string() const
{
    return String::formatted("BitwiseNot dst:{}, src:{}", m_dst, m_src);
}

String Not::to_string() const
{
    return String::formatted("Not dst:{}, src:{}", m_dst, m_src);
}

String UnaryPlus::to_string() const
{
    return String::formatted("UnaryPlus dst:{}, src:{}", m_dst, m_src);
}

String UnaryMinus::to_string() const
{
    return String::formatted("UnaryMinus dst:{}, src:{}", m_dst, m_src);
}

String Typeof::to_string() const
{
    return String::formatted("Typeof dst:{}, src:{}", m_dst, m_src);
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
    if (m_argument_count != 0) {
        builder.append(", arguments:[");
        for (size_t i = 0; i < m_argument_count; ++i) {
            builder.appendff("{}", m_arguments[i]);
            if (i != m_argument_count - 1)
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
