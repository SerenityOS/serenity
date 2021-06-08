/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Bytecode/Register.h>

namespace JS {

void ASTNode::generate_bytecode(Bytecode::Generator&) const
{
    dbgln("Missing generate_bytecode()");
    TODO();
}

void ScopeNode::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::EnterScope>(*this);
    for (auto& child : children())
        child.generate_bytecode(generator);
}

void EmptyStatement::generate_bytecode(Bytecode::Generator&) const
{
}

void ExpressionStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    m_expression->generate_bytecode(generator);
}

void BinaryExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    m_lhs->generate_bytecode(generator);
    auto lhs_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(lhs_reg);

    m_rhs->generate_bytecode(generator);

    switch (m_op) {
    case BinaryOp::Addition:
        generator.emit<Bytecode::Op::Add>(lhs_reg);
        break;
    case BinaryOp::Subtraction:
        generator.emit<Bytecode::Op::Sub>(lhs_reg);
        break;
    case BinaryOp::Multiplication:
        generator.emit<Bytecode::Op::Mul>(lhs_reg);
        break;
    case BinaryOp::Division:
        generator.emit<Bytecode::Op::Div>(lhs_reg);
        break;
    case BinaryOp::Modulo:
        generator.emit<Bytecode::Op::Mod>(lhs_reg);
        break;
    case BinaryOp::Exponentiation:
        generator.emit<Bytecode::Op::Exp>(lhs_reg);
        break;
    case BinaryOp::GreaterThan:
        generator.emit<Bytecode::Op::GreaterThan>(lhs_reg);
        break;
    case BinaryOp::GreaterThanEquals:
        generator.emit<Bytecode::Op::GreaterThanEquals>(lhs_reg);
        break;
    case BinaryOp::LessThan:
        generator.emit<Bytecode::Op::LessThan>(lhs_reg);
        break;
    case BinaryOp::LessThanEquals:
        generator.emit<Bytecode::Op::LessThanEquals>(lhs_reg);
        break;
    case BinaryOp::AbstractInequals:
        generator.emit<Bytecode::Op::AbstractInequals>(lhs_reg);
        break;
    case BinaryOp::AbstractEquals:
        generator.emit<Bytecode::Op::AbstractEquals>(lhs_reg);
        break;
    case BinaryOp::TypedInequals:
        generator.emit<Bytecode::Op::TypedInequals>(lhs_reg);
        break;
    case BinaryOp::TypedEquals:
        generator.emit<Bytecode::Op::TypedEquals>(lhs_reg);
        break;
    case BinaryOp::BitwiseAnd:
        generator.emit<Bytecode::Op::BitwiseAnd>(lhs_reg);
        break;
    case BinaryOp::BitwiseOr:
        generator.emit<Bytecode::Op::BitwiseOr>(lhs_reg);
        break;
    case BinaryOp::BitwiseXor:
        generator.emit<Bytecode::Op::BitwiseXor>(lhs_reg);
        break;
    case BinaryOp::LeftShift:
        generator.emit<Bytecode::Op::LeftShift>(lhs_reg);
        break;
    case BinaryOp::RightShift:
        generator.emit<Bytecode::Op::RightShift>(lhs_reg);
        break;
    case BinaryOp::UnsignedRightShift:
        generator.emit<Bytecode::Op::UnsignedRightShift>(lhs_reg);
        break;
    case BinaryOp::In:
        generator.emit<Bytecode::Op::In>(lhs_reg);
        break;
    case BinaryOp::InstanceOf:
        generator.emit<Bytecode::Op::InstanceOf>(lhs_reg);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void LogicalExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    m_lhs->generate_bytecode(generator);

    Bytecode::Op::Jump* test_instr;
    switch (m_op) {
    case LogicalOp::And:
        test_instr = &generator.emit<Bytecode::Op::JumpIfFalse>();
        break;
    case LogicalOp::Or:
        test_instr = &generator.emit<Bytecode::Op::JumpIfTrue>();
        break;
    case LogicalOp::NullishCoalescing:
        test_instr = &generator.emit<Bytecode::Op::JumpIfNotNullish>();
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    m_rhs->generate_bytecode(generator);
    test_instr->set_target(generator.make_label());
}

void UnaryExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    m_lhs->generate_bytecode(generator);

    switch (m_op) {
    case UnaryOp::BitwiseNot:
        generator.emit<Bytecode::Op::BitwiseNot>();
        break;
    case UnaryOp::Not:
        generator.emit<Bytecode::Op::Not>();
        break;
    case UnaryOp::Plus:
        generator.emit<Bytecode::Op::UnaryPlus>();
        break;
    case UnaryOp::Minus:
        generator.emit<Bytecode::Op::UnaryMinus>();
        break;
    case UnaryOp::Typeof:
        generator.emit<Bytecode::Op::Typeof>();
        break;
    case UnaryOp::Void:
        generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
        break;
    default:
        TODO();
    }
}

void NumericLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::LoadImmediate>(m_value);
}

void BooleanLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::LoadImmediate>(Value(m_value));
}

void NullLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::LoadImmediate>(js_null());
}

void BigIntLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewBigInt>(Crypto::SignedBigInteger::from_base10(m_value.substring(0, m_value.length() - 1)));
}

void StringLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewString>(m_value);
}

void Identifier::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::GetVariable>(m_string);
}

void AssignmentExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    if (is<Identifier>(*m_lhs)) {
        auto& identifier = static_cast<Identifier const&>(*m_lhs);

        if (m_op == AssignmentOp::Assignment) {
            m_rhs->generate_bytecode(generator);
            generator.emit<Bytecode::Op::SetVariable>(identifier.string());
            return;
        }

        m_lhs->generate_bytecode(generator);
        auto lhs_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(lhs_reg);
        m_rhs->generate_bytecode(generator);

        switch (m_op) {
        case AssignmentOp::AdditionAssignment:
            generator.emit<Bytecode::Op::Add>(lhs_reg);
            break;
        case AssignmentOp::SubtractionAssignment:
            generator.emit<Bytecode::Op::Sub>(lhs_reg);
            break;
        case AssignmentOp::MultiplicationAssignment:
            generator.emit<Bytecode::Op::Mul>(lhs_reg);
            break;
        case AssignmentOp::DivisionAssignment:
            generator.emit<Bytecode::Op::Div>(lhs_reg);
            break;
        case AssignmentOp::ModuloAssignment:
            generator.emit<Bytecode::Op::Mod>(lhs_reg);
            break;
        case AssignmentOp::ExponentiationAssignment:
            generator.emit<Bytecode::Op::Exp>(lhs_reg);
            break;
        case AssignmentOp::BitwiseAndAssignment:
            generator.emit<Bytecode::Op::BitwiseAnd>(lhs_reg);
            break;
        case AssignmentOp::BitwiseOrAssignment:
            generator.emit<Bytecode::Op::BitwiseOr>(lhs_reg);
            break;
        case AssignmentOp::BitwiseXorAssignment:
            generator.emit<Bytecode::Op::BitwiseXor>(lhs_reg);
            break;
        case AssignmentOp::LeftShiftAssignment:
            generator.emit<Bytecode::Op::LeftShift>(lhs_reg);
            break;
        case AssignmentOp::RightShiftAssignment:
            generator.emit<Bytecode::Op::RightShift>(lhs_reg);
            break;
        case AssignmentOp::UnsignedRightShiftAssignment:
            generator.emit<Bytecode::Op::UnsignedRightShift>(lhs_reg);
            break;
        default:
            TODO();
        }

        generator.emit<Bytecode::Op::SetVariable>(identifier.string());

        return;
    }

    if (is<MemberExpression>(*m_lhs)) {
        auto& expression = static_cast<MemberExpression const&>(*m_lhs);
        expression.object().generate_bytecode(generator);
        auto object_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(object_reg);

        if (expression.is_computed()) {
            TODO();
        } else {
            VERIFY(is<Identifier>(expression.property()));
            m_rhs->generate_bytecode(generator);
            generator.emit<Bytecode::Op::PutById>(object_reg, static_cast<Identifier const&>(expression.property()).string());
            return;
        }
    }

    TODO();
}

void WhileStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    generator.begin_continuable_scope();
    auto test_label = generator.make_label();
    auto result_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(result_reg);
    m_test->generate_bytecode(generator);
    auto& test_jump = generator.emit<Bytecode::Op::JumpIfFalse>();
    m_body->generate_bytecode(generator);
    generator.emit<Bytecode::Op::Jump>(test_label);
    test_jump.set_target(generator.make_label());
    generator.end_continuable_scope();
    generator.emit<Bytecode::Op::Load>(result_reg);
}

void DoWhileStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    generator.begin_continuable_scope();
    auto head_label = generator.make_label();
    m_body->generate_bytecode(generator);
    generator.end_continuable_scope();
    auto result_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(result_reg);
    m_test->generate_bytecode(generator);
    generator.emit<Bytecode::Op::JumpIfTrue>(head_label);
    generator.emit<Bytecode::Op::Load>(result_reg);
}

void ForStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    Bytecode::Op::Jump* test_jump { nullptr };

    if (m_init)
        m_init->generate_bytecode(generator);

    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    generator.begin_continuable_scope();
    auto jump_label = generator.make_label();
    auto result_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(result_reg);
    if (m_test) {
        m_test->generate_bytecode(generator);
        test_jump = &generator.emit<Bytecode::Op::JumpIfFalse>();
    }

    m_body->generate_bytecode(generator);
    if (m_update)
        m_update->generate_bytecode(generator);
    generator.emit<Bytecode::Op::Jump>(jump_label);
    if (m_test)
        test_jump->set_target(generator.make_label());
    generator.end_continuable_scope();
    generator.emit<Bytecode::Op::Load>(result_reg);
}

void ObjectExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewObject>();

    if (!m_properties.is_empty())
        TODO();
}

void MemberExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    object().generate_bytecode(generator);

    if (is_computed()) {
        TODO();
    } else {
        VERIFY(is<Identifier>(property()));
        generator.emit<Bytecode::Op::GetById>(static_cast<Identifier const&>(property()).string());
    }
}

void FunctionDeclaration::generate_bytecode(Bytecode::Generator&) const
{
}

void CallExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    m_callee->generate_bytecode(generator);
    auto callee_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(callee_reg);

    // FIXME: Load the correct 'this' value into 'this_reg'.
    auto this_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    generator.emit<Bytecode::Op::Store>(this_reg);

    Vector<Bytecode::Register> argument_registers;
    for (auto& arg : m_arguments) {
        arg.value->generate_bytecode(generator);
        auto arg_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(arg_reg);
        argument_registers.append(arg_reg);
    }
    generator.emit_with_extra_register_slots<Bytecode::Op::Call>(argument_registers.size(), callee_reg, this_reg, argument_registers);
}

void ReturnStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::Return>();
}

void IfStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    m_predicate->generate_bytecode(generator);
    auto& else_jump = generator.emit<Bytecode::Op::JumpIfFalse>();

    m_consequent->generate_bytecode(generator);
    if (m_alternate) {
        auto& if_jump = generator.emit<Bytecode::Op::Jump>();
        else_jump.set_target(generator.make_label());
        m_alternate->generate_bytecode(generator);
        if_jump.set_target(generator.make_label());
    } else {
        else_jump.set_target(generator.make_label());
    }
}

void ContinueStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::Jump>(generator.nearest_continuable_scope());
}

void DebuggerStatement::generate_bytecode(Bytecode::Generator&) const
{
}

void ConditionalExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    m_test->generate_bytecode(generator);
    auto& alternate_jump = generator.emit<Bytecode::Op::JumpIfFalse>();

    m_consequent->generate_bytecode(generator);
    auto& end_jump = generator.emit<Bytecode::Op::Jump>();

    alternate_jump.set_target(generator.make_label());
    m_alternate->generate_bytecode(generator);

    end_jump.set_target(generator.make_label());
}

void SequenceExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    for (auto& expression : m_expressions)
        expression.generate_bytecode(generator);
}

void TemplateLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    auto string_reg = generator.allocate_register();

    for (size_t i = 0; i < m_expressions.size(); i++) {
        m_expressions[i].generate_bytecode(generator);
        if (i == 0) {
            generator.emit<Bytecode::Op::Store>(string_reg);
        } else {
            generator.emit<Bytecode::Op::ConcatString>(string_reg);
        }
    }
}

}
