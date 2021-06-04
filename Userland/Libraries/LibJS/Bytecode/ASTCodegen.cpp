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
    case BinaryOp::LessThan:
        generator.emit<Bytecode::Op::LessThan>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::AbstractInequals:
        generator.emit<Bytecode::Op::AbstractInequals>(dst_reg, *lhs_reg, *rhs_reg);
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

    TODO();
}

Optional<Bytecode::Register> WhileStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    auto test_label = generator.make_label();
    auto test_result_reg = m_test->generate_bytecode(generator);
    VERIFY(test_result_reg.has_value());
    auto& test_jump = generator.emit<Bytecode::Op::JumpIfFalse>(*test_result_reg);
    auto body_result_reg = m_body->generate_bytecode(generator);
    generator.emit<Bytecode::Op::Jump>(test_label);
    test_jump.set_target(generator.make_label());
    return body_result_reg;
}

}
