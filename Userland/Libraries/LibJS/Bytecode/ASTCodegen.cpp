/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Bytecode/Register.h>

namespace JS {

Optional<Bytecode::Register> ASTNode::generate_bytecode(Bytecode::Generator&) const
{
    dbgln("Missing generate_bytecode()");
    TODO();
}

Optional<Bytecode::Register> ScopeNode::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::EnterScope>(*this);
    for (auto& child : children()) {
        [[maybe_unused]] auto reg = child.generate_bytecode(generator);
    }
    return {};
}

Optional<Bytecode::Register> ExpressionStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    return m_expression->generate_bytecode(generator);
}

Optional<Bytecode::Register> BinaryExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    auto lhs_reg = m_lhs->generate_bytecode(generator);
    auto rhs_reg = m_rhs->generate_bytecode(generator);

    VERIFY(lhs_reg.has_value());
    VERIFY(rhs_reg.has_value());

    auto dst_reg = generator.allocate_register();

    switch (m_op) {
    case BinaryOp::Addition:
        generator.emit<Bytecode::Op::Add>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::Subtraction:
        generator.emit<Bytecode::Op::Sub>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::Multiplication:
        generator.emit<Bytecode::Op::Mul>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::Division:
        generator.emit<Bytecode::Op::Div>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::LessThan:
        generator.emit<Bytecode::Op::LessThan>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::AbstractInequals:
        generator.emit<Bytecode::Op::AbstractInequals>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::AbstractEquals:
        generator.emit<Bytecode::Op::AbstractEquals>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    default:
        TODO();
    }
}

Optional<Bytecode::Register> NumericLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    auto dst = generator.allocate_register();
    generator.emit<Bytecode::Op::Load>(dst, m_value);
    return dst;
}

Optional<Bytecode::Register> StringLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    auto dst = generator.allocate_register();
    generator.emit<Bytecode::Op::NewString>(dst, m_value);
    return dst;
}

Optional<Bytecode::Register> Identifier::generate_bytecode(Bytecode::Generator& generator) const
{
    auto reg = generator.allocate_register();
    generator.emit<Bytecode::Op::GetVariable>(reg, m_string);
    return reg;
}

Optional<Bytecode::Register> AssignmentExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    if (is<Identifier>(*m_lhs)) {
        auto& identifier = static_cast<Identifier const&>(*m_lhs);
        auto rhs_reg = m_rhs->generate_bytecode(generator);
        VERIFY(rhs_reg.has_value());
        generator.emit<Bytecode::Op::SetVariable>(identifier.string(), *rhs_reg);
        return rhs_reg;
    }

    if (is<MemberExpression>(*m_lhs)) {
        auto& expression = static_cast<MemberExpression const&>(*m_lhs);
        auto object_reg = expression.object().generate_bytecode(generator);

        if (expression.is_computed()) {
            TODO();
        } else {
            VERIFY(is<Identifier>(expression.property()));
            auto rhs_reg = m_rhs->generate_bytecode(generator);
            generator.emit<Bytecode::Op::PutById>(*object_reg, static_cast<Identifier const&>(expression.property()).string(), *rhs_reg);
            return rhs_reg;
        }
    }

    TODO();
}

Optional<Bytecode::Register> WhileStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.begin_continuable_scope();
    auto test_label = generator.make_label();
    auto test_result_reg = m_test->generate_bytecode(generator);
    VERIFY(test_result_reg.has_value());
    auto& test_jump = generator.emit<Bytecode::Op::JumpIfFalse>(*test_result_reg);
    auto body_result_reg = m_body->generate_bytecode(generator);
    generator.emit<Bytecode::Op::Jump>(test_label);
    test_jump.set_target(generator.make_label());
    generator.end_continuable_scope();
    return body_result_reg;
}

Optional<Bytecode::Register> DoWhileStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.begin_continuable_scope();
    auto head_label = generator.make_label();
    auto body_result_reg = m_body->generate_bytecode(generator);
    generator.end_continuable_scope();
    auto test_result_reg = m_test->generate_bytecode(generator);
    VERIFY(test_result_reg.has_value());
    generator.emit<Bytecode::Op::JumpIfTrue>(*test_result_reg, head_label);
    return body_result_reg;
}

Optional<Bytecode::Register> ObjectExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    auto reg = generator.allocate_register();
    generator.emit<Bytecode::Op::NewObject>(reg);

    if (!m_properties.is_empty()) {
        TODO();
    }

    return reg;
}

Optional<Bytecode::Register> MemberExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    auto object_reg = object().generate_bytecode(generator);

    if (is_computed()) {
        TODO();
    } else {
        VERIFY(is<Identifier>(property()));
        auto dst_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::GetById>(dst_reg, *object_reg, static_cast<Identifier const&>(property()).string());
        return dst_reg;
    }
}

Optional<Bytecode::Register> FunctionDeclaration::generate_bytecode(Bytecode::Generator&) const
{
    return {};
}

Optional<Bytecode::Register> CallExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    auto callee_reg = m_callee->generate_bytecode(generator);

    // FIXME: Load the correct 'this' value into 'this_reg'.
    auto this_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Load>(this_reg, js_undefined());

    Vector<Bytecode::Register> argument_registers;
    for (auto& arg : m_arguments)
        argument_registers.append(*arg.value->generate_bytecode(generator));
    auto dst_reg = generator.allocate_register();
    generator.emit_with_extra_register_slots<Bytecode::Op::Call>(argument_registers.size(), dst_reg, *callee_reg, this_reg, argument_registers);
    return dst_reg;
}

Optional<Bytecode::Register> ReturnStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    Optional<Bytecode::Register> argument_reg;
    if (m_argument)
        argument_reg = m_argument->generate_bytecode(generator);

    generator.emit<Bytecode::Op::Return>(argument_reg);
    return argument_reg;
}

Optional<Bytecode::Register> IfStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    auto predicate_reg = m_predicate->generate_bytecode(generator);
    auto& if_jump = generator.emit<Bytecode::Op::JumpIfTrue>(*predicate_reg);
    auto& else_jump = generator.emit<Bytecode::Op::JumpIfFalse>(*predicate_reg);

    if_jump.set_target(generator.make_label());
    (void)m_consequent->generate_bytecode(generator);
    auto& end_jump = generator.emit<Bytecode::Op::Jump>();

    else_jump.set_target(generator.make_label());
    if (m_alternate) {
        (void)m_alternate->generate_bytecode(generator);
    }

    end_jump.set_target(generator.make_label());

    // FIXME: Do we need IfStatement to return the consequent/alternate result value?
    //        (That's what the AST interpreter currently does)
    return {};
}

Optional<Bytecode::Register> ContinueStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::Jump>(generator.nearest_continuable_scope());
    return {};
}

}
