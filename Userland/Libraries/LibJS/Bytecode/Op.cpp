/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Runtime/BigInt.h>
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
    interpreter.accumulator() = interpreter.reg(m_src);
}

void LoadImmediate::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = m_value;
}

void Store::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_dst) = interpreter.accumulator();
}

static Value abstract_inequals(GlobalObject& global_object, Value src1, Value src2)
{
    return Value(!abstract_eq(global_object, src1, src2));
}

static Value abstract_equals(GlobalObject& global_object, Value src1, Value src2)
{
    return Value(abstract_eq(global_object, src1, src2));
}

static Value typed_inequals(GlobalObject&, Value src1, Value src2)
{
    return Value(!strict_eq(src1, src2));
}

static Value typed_equals(GlobalObject&, Value src1, Value src2)
{
    return Value(strict_eq(src1, src2));
}

#define JS_DEFINE_COMMON_BINARY_OP(OpTitleCase, op_snake_case)                            \
    void OpTitleCase::execute(Bytecode::Interpreter& interpreter) const                   \
    {                                                                                     \
        auto lhs = interpreter.reg(m_lhs_reg);                                            \
        auto rhs = interpreter.accumulator();                                             \
        interpreter.accumulator() = op_snake_case(interpreter.global_object(), lhs, rhs); \
    }                                                                                     \
    String OpTitleCase::to_string() const                                                 \
    {                                                                                     \
        return String::formatted(#OpTitleCase " lhs:{}", m_lhs_reg);                      \
    }

JS_ENUMERATE_COMMON_BINARY_OPS(JS_DEFINE_COMMON_BINARY_OP)

static Value not_(GlobalObject&, Value value)
{
    return Value(!value.to_boolean());
}

static Value typeof_(GlobalObject& global_object, Value value)
{
    return js_string(global_object.vm(), value.typeof());
}

#define JS_DEFINE_COMMON_UNARY_OP(OpTitleCase, op_snake_case)                                              \
    void OpTitleCase::execute(Bytecode::Interpreter& interpreter) const                                    \
    {                                                                                                      \
        interpreter.accumulator() = op_snake_case(interpreter.global_object(), interpreter.accumulator()); \
    }                                                                                                      \
    String OpTitleCase::to_string() const                                                                  \
    {                                                                                                      \
        return #OpTitleCase;                                                                               \
    }

JS_ENUMERATE_COMMON_UNARY_OPS(JS_DEFINE_COMMON_UNARY_OP)

void NewBigInt::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = js_bigint(interpreter.vm().heap(), m_bigint);
}

void NewString::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = js_string(interpreter.vm(), m_string);
}

void NewObject::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = Object::create_empty(interpreter.global_object());
}

void ConcatString::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.reg(m_lhs) = add(interpreter.global_object(), interpreter.reg(m_lhs), interpreter.accumulator());
}

void GetVariable::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.accumulator() = interpreter.vm().get_variable(m_identifier, interpreter.global_object());
}

void SetVariable::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.vm().set_variable(m_identifier, interpreter.accumulator(), interpreter.global_object());
}

void GetById::execute(Bytecode::Interpreter& interpreter) const
{
    if (auto* object = interpreter.accumulator().to_object(interpreter.global_object()))
        interpreter.accumulator() = object->get(m_property);
}

void PutById::execute(Bytecode::Interpreter& interpreter) const
{
    if (auto* object = interpreter.reg(m_base).to_object(interpreter.global_object()))
        object->put(m_property, interpreter.accumulator());
}

void Jump::execute(Bytecode::Interpreter& interpreter) const
{
    interpreter.jump(*m_target);
}

void JumpIfFalse::execute(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_target.has_value());
    auto result = interpreter.accumulator();
    if (!result.to_boolean())
        interpreter.jump(m_target.value());
}

void JumpIfTrue::execute(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_target.has_value());
    auto result = interpreter.accumulator();
    if (result.to_boolean())
        interpreter.jump(m_target.value());
}

void JumpIfNotNullish::execute(Bytecode::Interpreter& interpreter) const
{
    VERIFY(m_target.has_value());
    auto result = interpreter.accumulator();
    if (!result.is_nullish())
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

    interpreter.accumulator() = return_value;
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
    interpreter.do_return(interpreter.accumulator().value_or(js_undefined()));
}

String Load::to_string() const
{
    return String::formatted("Load src:{}", m_src);
}

String LoadImmediate::to_string() const
{
    return String::formatted("LoadImmediate value:{}", m_value);
}

String Store::to_string() const
{
    return String::formatted("Store dst:{}", m_dst);
}

String NewBigInt::to_string() const
{
    return String::formatted("NewBigInt bigint:\"{}\"", m_bigint.to_base10());
}

String NewString::to_string() const
{
    return String::formatted("NewString string:\"{}\"", m_string);
}

String NewObject::to_string() const
{
    return "NewObject";
}

String ConcatString::to_string() const
{
    return String::formatted("ConcatString lhs:{}", m_lhs);
}

String GetVariable::to_string() const
{
    return String::formatted("GetVariable identifier:{}", m_identifier);
}

String SetVariable::to_string() const
{
    return String::formatted("SetVariable identifier:{}", m_identifier);
}

String PutById::to_string() const
{
    return String::formatted("PutById base:{}, property:{}", m_base, m_property);
}

String GetById::to_string() const
{
    return String::formatted("GetById property:{}", m_property);
}

String Jump::to_string() const
{
    return String::formatted("Jump {}", *m_target);
}

String JumpIfFalse::to_string() const
{
    if (m_target.has_value())
        return String::formatted("JumpIfFalse target:{}", m_target.value());
    return "JumpIfFalse target:<empty>";
}

String JumpIfTrue::to_string() const
{
    if (m_target.has_value())
        return String::formatted("JumpIfTrue target:{}", m_target.value());
    return "JumpIfTrue result:{}, target:<empty>";
}

String JumpIfNotNullish::to_string() const
{
    if (m_target.has_value())
        return String::formatted("JumpIfNotNullish target:{}", m_target.value());
    return "JumpIfNotNullish target:<empty>";
}

String Call::to_string() const
{
    StringBuilder builder;
    builder.appendff("Call callee:{}, this:{}", m_callee, m_this_value);
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
    return "Return";
}

}
