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

    // lhs
    // jump op (true) end (false) rhs
    // rhs
    // jump always (true) end
    // end

    auto& rhs_block = generator.make_block();
    auto& end_block = generator.make_block();

    switch (m_op) {
    case LogicalOp::And:
        generator.emit<Bytecode::Op::JumpConditional>().set_targets(
            Bytecode::Label { rhs_block },
            Bytecode::Label { end_block });
        break;
    case LogicalOp::Or:
        generator.emit<Bytecode::Op::JumpConditional>().set_targets(
            Bytecode::Label { end_block },
            Bytecode::Label { rhs_block });
        break;
    case LogicalOp::NullishCoalescing:
        generator.emit<Bytecode::Op::JumpNullish>().set_targets(
            Bytecode::Label { rhs_block },
            Bytecode::Label { end_block });
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    generator.switch_to_basic_block(rhs_block);
    m_rhs->generate_bytecode(generator);

    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { end_block },
        {});

    generator.switch_to_basic_block(end_block);
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
    // test
    // jump if_false (true) end (false) body
    // body
    // jump always (true) test
    // end
    auto& test_block = generator.make_block();
    auto& body_block = generator.make_block();
    auto& end_block = generator.make_block();

    // Init result register
    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    auto result_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(result_reg);

    // jump to the test block
    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { test_block },
        {});

    generator.switch_to_basic_block(test_block);
    m_test->generate_bytecode(generator);
    generator.emit<Bytecode::Op::JumpConditional>().set_targets(
        Bytecode::Label { body_block },
        Bytecode::Label { end_block });

    generator.switch_to_basic_block(body_block);
    generator.begin_continuable_scope(Bytecode::Label { test_block });
    m_body->generate_bytecode(generator);
    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { test_block },
        {});
    generator.end_continuable_scope();

    generator.switch_to_basic_block(end_block);
    generator.emit<Bytecode::Op::Load>(result_reg);
}

void DoWhileStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    // jump always (true) body
    // test
    // jump if_false (true) end (false) body
    // body
    // jump always (true) test
    // end
    auto& test_block = generator.make_block();
    auto& body_block = generator.make_block();
    auto& end_block = generator.make_block();

    // Init result register
    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    auto result_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(result_reg);

    // jump to the body block
    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { body_block },
        {});

    generator.switch_to_basic_block(test_block);
    m_test->generate_bytecode(generator);
    generator.emit<Bytecode::Op::JumpConditional>().set_targets(
        Bytecode::Label { body_block },
        Bytecode::Label { end_block });

    generator.switch_to_basic_block(body_block);
    generator.begin_continuable_scope(Bytecode::Label { test_block });
    m_body->generate_bytecode(generator);
    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { test_block },
        {});
    generator.end_continuable_scope();

    generator.switch_to_basic_block(end_block);
    generator.emit<Bytecode::Op::Load>(result_reg);
}

void ForStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    // init
    // jump always (true) test
    // test
    // jump if_true (true) body (false) end
    // body
    // jump always (true) update
    // update
    // jump always (true) test
    // end

    // If 'test' is missing, fuse the 'test' and 'body' basic blocks
    // If 'update' is missing, fuse the 'body' and 'update' basic blocks

    Bytecode::BasicBlock* test_block_ptr { nullptr };
    Bytecode::BasicBlock* body_block_ptr { nullptr };
    Bytecode::BasicBlock* update_block_ptr { nullptr };

    auto& end_block = generator.make_block();

    if (m_init)
        m_init->generate_bytecode(generator);

    body_block_ptr = &generator.make_block();

    if (m_test)
        test_block_ptr = &generator.make_block();
    else
        test_block_ptr = body_block_ptr;

    if (m_update)
        update_block_ptr = &generator.make_block();
    else
        update_block_ptr = body_block_ptr;

    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    auto result_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(result_reg);

    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { *test_block_ptr },
        {});

    if (m_test) {
        generator.switch_to_basic_block(*test_block_ptr);
        m_test->generate_bytecode(generator);
        generator.emit<Bytecode::Op::JumpConditional>().set_targets(
            Bytecode::Label { *body_block_ptr },
            Bytecode::Label { end_block });
    }

    generator.switch_to_basic_block(*body_block_ptr);
    generator.begin_continuable_scope(Bytecode::Label { *update_block_ptr });
    m_body->generate_bytecode(generator);
    generator.end_continuable_scope();

    if (m_update) {
        generator.emit<Bytecode::Op::Jump>().set_targets(
            Bytecode::Label { *update_block_ptr },
            {});

        generator.switch_to_basic_block(*update_block_ptr);
        m_update->generate_bytecode(generator);
    }

    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { *test_block_ptr },
        {});

    generator.switch_to_basic_block(end_block);
    generator.emit<Bytecode::Op::Load>(result_reg);
}

void ObjectExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewObject>();

    if (!m_properties.is_empty())
        TODO();
}

void ArrayExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    Vector<Bytecode::Register> element_regs;
    for (auto& element : m_elements) {
        generator.emit<Bytecode::Op::LoadImmediate>(Value {});
        if (element) {
            element->generate_bytecode(generator);

            if (is<SpreadExpression>(*element)) {
                TODO();
                continue;
            }
        }
        auto element_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(element_reg);
        element_regs.append(element_reg);
    }
    generator.emit_with_extra_register_slots<Bytecode::Op::NewArray>(element_regs.size(), element_regs);
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
    if (m_argument)
        m_argument->generate_bytecode(generator);
    generator.emit<Bytecode::Op::Return>();
}

void IfStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    // test
    // jump if_true (true) true (false) false
    // true
    // jump always (true) end
    // false
    // jump always (true) end
    // end

    // If the 'false' branch doesn't exist, we're just gonna substitute it for 'end' and elide the last two entries above.

    auto& true_block = generator.make_block();
    auto& false_block = generator.make_block();

    m_predicate->generate_bytecode(generator);
    generator.emit<Bytecode::Op::JumpConditional>().set_targets(
        Bytecode::Label { true_block },
        Bytecode::Label { false_block });

    Bytecode::Op::Jump* true_block_jump { nullptr };

    generator.switch_to_basic_block(true_block);
    m_consequent->generate_bytecode(generator);
    if (!generator.is_current_block_terminated())
        true_block_jump = &generator.emit<Bytecode::Op::Jump>();

    generator.switch_to_basic_block(false_block);
    if (m_alternate) {
        auto& end_block = generator.make_block();

        m_alternate->generate_bytecode(generator);
        if (!generator.is_current_block_terminated())
            generator.emit<Bytecode::Op::Jump>().set_targets(
                Bytecode::Label { end_block },
                {});

        if (true_block_jump)
            true_block_jump->set_targets(
                Bytecode::Label { end_block },
                {});

        generator.switch_to_basic_block(end_block);
    } else {
        if (true_block_jump)
            true_block_jump->set_targets(
                Bytecode::Label { false_block },
                {});
    }
}

void ContinueStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::Jump>().set_targets(
        generator.nearest_continuable_scope(),
        {});
}

void DebuggerStatement::generate_bytecode(Bytecode::Generator&) const
{
}

void ConditionalExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    // test
    // jump if_true (true) true (false) false
    // true
    // jump always (true) end
    // false
    // jump always (true) end
    // end

    auto& true_block = generator.make_block();
    auto& false_block = generator.make_block();
    auto& end_block = generator.make_block();

    m_test->generate_bytecode(generator);
    generator.emit<Bytecode::Op::JumpConditional>().set_targets(
        Bytecode::Label { true_block },
        Bytecode::Label { false_block });

    generator.switch_to_basic_block(true_block);
    m_consequent->generate_bytecode(generator);
    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { end_block },
        {});

    generator.switch_to_basic_block(false_block);
    m_alternate->generate_bytecode(generator);
    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { end_block },
        {});

    generator.switch_to_basic_block(end_block);
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
