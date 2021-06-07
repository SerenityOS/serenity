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
#include <LibJS/Bytecode/VirtualRegister.h>
#include <LibJS/Runtime/BigInt.h>

namespace JS {

static Bytecode::Register emit_undefined(Bytecode::Generator& generator)
{
    auto result_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Load>(result_reg, js_undefined());
    return result_reg;
}

Optional<Bytecode::Register> ASTNode::generate_bytecode(Bytecode::Generator&) const
{
    dbgln("Missing generate_bytecode()");
    TODO();
}

Optional<Bytecode::Register> ScopeNode::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::EnterScope>(*this);
    Optional<Bytecode::Register> last_value_reg;
    for (size_t i = 0; i < children().size(); i++) {
        auto& child = children()[i];
        auto child_value = Bytecode::VirtualRegister { generator, child };
        if (i != children().size() - 1 && child_value.is_constant())
            continue;
        last_value_reg = child_value.materialize();
    }
    return last_value_reg;
}

Optional<Bytecode::Register> EmptyStatement::generate_bytecode(Bytecode::Generator&) const
{
    return {};
}

Optional<Bytecode::Register> ExpressionStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    return m_expression->generate_bytecode(generator);
}

Optional<Bytecode::Register> BinaryExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    auto lhs_value = Bytecode::VirtualRegister { generator, *m_lhs };
    auto lhs_reg = lhs_value.materialize();
    auto rhs_value = Bytecode::VirtualRegister { generator, *m_rhs };
    auto rhs_reg = rhs_value.materialize();

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
    case BinaryOp::Modulo:
        generator.emit<Bytecode::Op::Mod>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::Exponentiation:
        generator.emit<Bytecode::Op::Exp>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::GreaterThan:
        generator.emit<Bytecode::Op::GreaterThan>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::GreaterThanEquals:
        generator.emit<Bytecode::Op::GreaterThanEquals>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::LessThan:
        generator.emit<Bytecode::Op::LessThan>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::LessThanEquals:
        generator.emit<Bytecode::Op::LessThanEquals>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::AbstractInequals:
        generator.emit<Bytecode::Op::AbstractInequals>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::AbstractEquals:
        generator.emit<Bytecode::Op::AbstractEquals>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::TypedInequals:
        generator.emit<Bytecode::Op::TypedInequals>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::TypedEquals:
        generator.emit<Bytecode::Op::TypedEquals>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::BitwiseAnd:
        generator.emit<Bytecode::Op::BitwiseAnd>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::BitwiseOr:
        generator.emit<Bytecode::Op::BitwiseOr>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::BitwiseXor:
        generator.emit<Bytecode::Op::BitwiseXor>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::LeftShift:
        generator.emit<Bytecode::Op::LeftShift>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::RightShift:
        generator.emit<Bytecode::Op::RightShift>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::UnsignedRightShift:
        generator.emit<Bytecode::Op::UnsignedRightShift>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::In:
        generator.emit<Bytecode::Op::In>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    case BinaryOp::InstanceOf:
        generator.emit<Bytecode::Op::InstanceOf>(dst_reg, *lhs_reg, *rhs_reg);
        return dst_reg;
    default:
        VERIFY_NOT_REACHED();
    }
}

Optional<Bytecode::Register> LogicalExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    auto lhs_value = Bytecode::VirtualRegister { generator, *m_lhs };
    auto rhs_value = Bytecode::VirtualRegister { generator, *m_rhs };
    if (lhs_value.is_constant()) {
        switch (m_op) {
        case LogicalOp::And:
            if (lhs_value->to_boolean())
                return rhs_value.materialize();
            else
                return lhs_value.materialize();
        case LogicalOp::Or:
            if (lhs_value->to_boolean())
                return lhs_value.materialize();
            else
                return rhs_value.materialize();
            break;
        case LogicalOp::NullishCoalescing:
            if (lhs_value->is_nullish())
                return rhs_value.materialize();
            else
                return lhs_value.materialize();
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    auto result_reg = generator.allocate_register();
    auto lhs_reg = lhs_value.materialize();

    Bytecode::Instruction* test_instr;
    switch (m_op) {
    case LogicalOp::And:
        test_instr = &generator.emit<Bytecode::Op::JumpIfTrue>(*lhs_reg);
        break;
    case LogicalOp::Or:
        test_instr = &generator.emit<Bytecode::Op::JumpIfFalse>(*lhs_reg);
        break;
    case LogicalOp::NullishCoalescing:
        test_instr = &generator.emit<Bytecode::Op::JumpIfNullish>(*lhs_reg);
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    generator.emit<Bytecode::Op::LoadRegister>(result_reg, *lhs_reg);
    auto& end_jump = generator.emit<Bytecode::Op::Jump>();

    auto rhs_label = generator.make_label();

    switch (m_op) {
    case LogicalOp::And:
        static_cast<Bytecode::Op::JumpIfTrue*>(test_instr)->set_target(rhs_label);
        break;
    case LogicalOp::Or:
        static_cast<Bytecode::Op::JumpIfFalse*>(test_instr)->set_target(rhs_label);
        break;
    case LogicalOp::NullishCoalescing:
        static_cast<Bytecode::Op::JumpIfNullish*>(test_instr)->set_target(rhs_label);
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    auto rhs_reg = rhs_value.materialize();
    generator.emit<Bytecode::Op::LoadRegister>(result_reg, *rhs_reg);

    end_jump.set_target(generator.make_label());

    return result_reg;
}

Optional<Bytecode::Register> UnaryExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    auto lhs_value = Bytecode::VirtualRegister { generator, *m_lhs };
    auto lhs_reg = lhs_value.materialize();

    VERIFY(lhs_reg.has_value());

    auto dst_reg = generator.allocate_register();

    switch (m_op) {
    case UnaryOp::BitwiseNot:
        generator.emit<Bytecode::Op::BitwiseNot>(dst_reg, *lhs_reg);
        return dst_reg;
    case UnaryOp::Not:
        generator.emit<Bytecode::Op::Not>(dst_reg, *lhs_reg);
        return dst_reg;
    case UnaryOp::Plus:
        generator.emit<Bytecode::Op::UnaryPlus>(dst_reg, *lhs_reg);
        return dst_reg;
    case UnaryOp::Minus:
        generator.emit<Bytecode::Op::UnaryMinus>(dst_reg, *lhs_reg);
        return dst_reg;
    case UnaryOp::Typeof:
        generator.emit<Bytecode::Op::Typeof>(dst_reg, *lhs_reg);
        return dst_reg;
    case UnaryOp::Void:
        generator.emit<Bytecode::Op::Load>(dst_reg, js_undefined());
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

Optional<Bytecode::Register> BooleanLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    auto dst = generator.allocate_register();
    generator.emit<Bytecode::Op::Load>(dst, Value(m_value));
    return dst;
}

Optional<Bytecode::Register> NullLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    auto dst = generator.allocate_register();
    generator.emit<Bytecode::Op::Load>(dst, js_null());
    return dst;
}

Optional<Bytecode::Register> BigIntLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    auto dst = generator.allocate_register();
    generator.emit<Bytecode::Op::NewBigInt>(dst, Crypto::SignedBigInteger::from_base10(m_value.substring(0, m_value.length() - 1)));
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
        auto rhs_value = Bytecode::VirtualRegister { generator, *m_rhs };
        auto rhs_reg = rhs_value.materialize();
        VERIFY(rhs_reg.has_value());

        if (m_op == AssignmentOp::Assignment) {
            generator.emit<Bytecode::Op::SetVariable>(identifier.string(), *rhs_reg);
            return rhs_reg;
        }

        auto lhs_value = Bytecode::VirtualRegister { generator, *m_lhs };
        auto lhs_reg = lhs_value.materialize();
        auto dst_reg = generator.allocate_register();

        switch (m_op) {
        case AssignmentOp::AdditionAssignment:
            generator.emit<Bytecode::Op::Add>(dst_reg, *lhs_reg, *rhs_reg);
            break;
        case AssignmentOp::SubtractionAssignment:
            generator.emit<Bytecode::Op::Sub>(dst_reg, *lhs_reg, *rhs_reg);
            break;
        case AssignmentOp::MultiplicationAssignment:
            generator.emit<Bytecode::Op::Mul>(dst_reg, *lhs_reg, *rhs_reg);
            break;
        case AssignmentOp::DivisionAssignment:
            generator.emit<Bytecode::Op::Div>(dst_reg, *lhs_reg, *rhs_reg);
            break;
        case AssignmentOp::ModuloAssignment:
            generator.emit<Bytecode::Op::Mod>(dst_reg, *lhs_reg, *rhs_reg);
            break;
        case AssignmentOp::ExponentiationAssignment:
            generator.emit<Bytecode::Op::Exp>(dst_reg, *lhs_reg, *rhs_reg);
            break;
        case AssignmentOp::BitwiseAndAssignment:
            generator.emit<Bytecode::Op::BitwiseAnd>(dst_reg, *lhs_reg, *rhs_reg);
            break;
        case AssignmentOp::BitwiseOrAssignment:
            generator.emit<Bytecode::Op::BitwiseOr>(dst_reg, *lhs_reg, *rhs_reg);
            break;
        case AssignmentOp::BitwiseXorAssignment:
            generator.emit<Bytecode::Op::BitwiseXor>(dst_reg, *lhs_reg, *rhs_reg);
            break;
        case AssignmentOp::LeftShiftAssignment:
            generator.emit<Bytecode::Op::LeftShift>(dst_reg, *lhs_reg, *rhs_reg);
            break;
        case AssignmentOp::RightShiftAssignment:
            generator.emit<Bytecode::Op::RightShift>(dst_reg, *lhs_reg, *rhs_reg);
            break;
        case AssignmentOp::UnsignedRightShiftAssignment:
            generator.emit<Bytecode::Op::UnsignedRightShift>(dst_reg, *lhs_reg, *rhs_reg);
            break;
        default:
            TODO();
        }

        generator.emit<Bytecode::Op::SetVariable>(identifier.string(), dst_reg);

        return dst_reg;
    }

    if (is<MemberExpression>(*m_lhs)) {
        auto& expression = static_cast<MemberExpression const&>(*m_lhs);
        auto object_value = Bytecode::VirtualRegister { generator, expression.object() };
        auto object_reg = object_value.materialize();

        if (expression.is_computed()) {
            TODO();
        } else {
            VERIFY(is<Identifier>(expression.property()));
            auto rhs_value = Bytecode::VirtualRegister { generator, *m_rhs };
            auto rhs_reg = rhs_value.materialize();
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
    auto test_result_value = Bytecode::VirtualRegister { generator, *m_test };
    Bytecode::Op::JumpIfFalse* test_jump { nullptr };
    if (!test_result_value.is_constant()) {
        auto test_result_reg = test_result_value.materialize();
        VERIFY(test_result_reg.has_value());
        test_jump = &generator.emit<Bytecode::Op::JumpIfFalse>(*test_result_reg);
    } else if (!test_result_value->to_boolean())
        return emit_undefined(generator);
    auto body_result_value = Bytecode::VirtualRegister { generator, *m_body };
    auto body_result_reg = body_result_value.materialize();
    generator.emit<Bytecode::Op::Jump>(test_label);
    if (test_jump)
        test_jump->set_target(generator.make_label());
    generator.end_continuable_scope();
    return body_result_reg;
}

Optional<Bytecode::Register> DoWhileStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.begin_continuable_scope();
    auto head_label = generator.make_label();
    auto body_result_value = Bytecode::VirtualRegister { generator, *m_body };
    auto body_result_reg = body_result_value.materialize();
    generator.end_continuable_scope();
    auto test_result_value = Bytecode::VirtualRegister { generator, *m_test };
    if (!test_result_value.is_constant()) {
        auto test_result_reg = test_result_value.materialize();
        VERIFY(test_result_reg.has_value());
        generator.emit<Bytecode::Op::JumpIfTrue>(*test_result_reg, head_label);
    } else if (test_result_value->to_boolean())
        generator.emit<Bytecode::Op::Jump>(head_label);
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
    auto object_value = Bytecode::VirtualRegister { generator, object() };
    auto object_reg = object_value.materialize();

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
    auto callee_value = Bytecode::VirtualRegister { generator, *m_callee };
    auto callee_reg = callee_value.materialize();

    // FIXME: Load the correct 'this' value into 'this_reg'.
    auto this_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Load>(this_reg, js_undefined());

    Vector<Bytecode::Register> argument_registers;
    for (auto& arg : m_arguments) {
        auto arg_value = Bytecode::VirtualRegister { generator, *arg.value };
        argument_registers.append(*arg_value.materialize());
    }
    auto dst_reg = generator.allocate_register();
    generator.emit_with_extra_register_slots<Bytecode::Op::Call>(argument_registers.size(), dst_reg, *callee_reg, this_reg, argument_registers);
    return dst_reg;
}

Optional<Bytecode::Register> ReturnStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    Optional<Bytecode::Register> argument_reg;
    if (m_argument) {
        auto argument_value = Bytecode::VirtualRegister { generator, *m_argument };
        argument_reg = argument_value.materialize();
    }

    generator.emit<Bytecode::Op::Return>(argument_reg);
    return argument_reg;
}

Optional<Bytecode::Register> IfStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    auto predicate_value = Bytecode::VirtualRegister { generator, *m_predicate };
    auto consequent_value = Bytecode::VirtualRegister { generator, *m_consequent };
    if (predicate_value.is_constant()) {
        if (predicate_value->to_boolean())
            return consequent_value.materialize();
        else if (m_alternate) {
            auto alternate_value = Bytecode::VirtualRegister { generator, *m_alternate };
            return alternate_value.materialize();
        }
        return emit_undefined(generator);
    }
    auto result_reg = generator.allocate_register();
    auto predicate_reg = predicate_value.materialize();
    auto& if_jump = generator.emit<Bytecode::Op::JumpIfTrue>(*predicate_reg);
    auto& else_jump = generator.emit<Bytecode::Op::JumpIfFalse>(*predicate_reg);

    if_jump.set_target(generator.make_label());
    auto consequent_reg = consequent_value.materialize();
    generator.emit<Bytecode::Op::LoadRegister>(result_reg, *consequent_reg);
    auto& end_jump = generator.emit<Bytecode::Op::Jump>();

    else_jump.set_target(generator.make_label());
    if (m_alternate) {
        auto alternate_value = Bytecode::VirtualRegister { generator, *m_alternate };
        auto alternative_reg = alternate_value.materialize();
        generator.emit<Bytecode::Op::LoadRegister>(result_reg, *alternative_reg);
    } else
        generator.emit<Bytecode::Op::Load>(result_reg, js_undefined());

    end_jump.set_target(generator.make_label());

    return result_reg;
}

Optional<Bytecode::Register> ContinueStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::Jump>(generator.nearest_continuable_scope());
    return {};
}

Optional<Bytecode::Register> DebuggerStatement::generate_bytecode(Bytecode::Generator&) const
{
    return {};
}

}
