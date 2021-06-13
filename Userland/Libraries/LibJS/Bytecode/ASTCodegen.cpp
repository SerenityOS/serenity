/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2021, Marcin Gasperowicz <xnooga@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Bytecode/StringTable.h>
#include <LibJS/Runtime/ScopeObject.h>

namespace JS {

void ASTNode::generate_bytecode(Bytecode::Generator&) const
{
    dbgln("Missing generate_bytecode() in {}", class_name());
    TODO();
}

void ScopeNode::generate_bytecode(Bytecode::Generator& generator) const
{
    for (auto& function : functions()) {
        generator.emit<Bytecode::Op::NewFunction>(function);
        generator.emit<Bytecode::Op::SetVariable>(generator.intern_string(function.name()));
    }

    HashMap<u32, Variable> scope_variables_with_declaration_kind;

    bool is_program_node = is<Program>(*this);
    for (auto& declaration : variables()) {
        for (auto& declarator : declaration.declarations()) {
            if (is_program_node && declaration.declaration_kind() == DeclarationKind::Var) {
                declarator.target().visit(
                    [&](const NonnullRefPtr<Identifier>& id) {
                        generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
                        generator.emit<Bytecode::Op::PutById>(Bytecode::Register::global_object(), generator.intern_string(id->string()));
                    },
                    [&](const NonnullRefPtr<BindingPattern>& binding) {
                        binding->for_each_assigned_name([&](const auto& name) {
                            generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
                            generator.emit<Bytecode::Op::PutById>(Bytecode::Register::global_object(), generator.intern_string(name));
                        });
                    });
            } else {
                declarator.target().visit(
                    [&](const NonnullRefPtr<Identifier>& id) {
                        scope_variables_with_declaration_kind.set((size_t)generator.intern_string(id->string()).value(), { js_undefined(), declaration.declaration_kind() });
                    },
                    [&](const NonnullRefPtr<BindingPattern>& binding) {
                        binding->for_each_assigned_name([&](const auto& name) {
                            scope_variables_with_declaration_kind.set((size_t)generator.intern_string(name).value(), { js_undefined(), declaration.declaration_kind() });
                        });
                    });
            }
        }
    }

    if (!scope_variables_with_declaration_kind.is_empty()) {
        generator.emit<Bytecode::Op::PushLexicalEnvironment>(move(scope_variables_with_declaration_kind));
    }

    for (auto& child : children()) {
        child.generate_bytecode(generator);
        if (generator.is_current_block_terminated())
            break;
    }
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
    generator.emit<Bytecode::Op::NewString>(generator.intern_string(m_value));
}

void Identifier::generate_bytecode(Bytecode::Generator& generator) const
{
    if (m_argument_index.has_value())
        generator.emit<Bytecode::Op::LoadArgument>(m_argument_index.value());
    else
        generator.emit<Bytecode::Op::GetVariable>(generator.intern_string(m_string));
}

void AssignmentExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    if (is<Identifier>(*m_lhs)) {
        auto& identifier = static_cast<Identifier const&>(*m_lhs);

        if (m_op == AssignmentOp::Assignment) {
            m_rhs->generate_bytecode(generator);
            generator.emit<Bytecode::Op::SetVariable>(generator.intern_string(identifier.string()));
            return;
        }

        m_lhs->generate_bytecode(generator);

        Bytecode::BasicBlock* rhs_block_ptr { nullptr };
        Bytecode::BasicBlock* end_block_ptr { nullptr };

        // Logical assignments short circuit.
        if (m_op == AssignmentOp::AndAssignment) { // &&=
            rhs_block_ptr = &generator.make_block();
            end_block_ptr = &generator.make_block();

            generator.emit<Bytecode::Op::JumpConditional>().set_targets(
                Bytecode::Label { *rhs_block_ptr },
                Bytecode::Label { *end_block_ptr });
        } else if (m_op == AssignmentOp::OrAssignment) { // ||=
            rhs_block_ptr = &generator.make_block();
            end_block_ptr = &generator.make_block();

            generator.emit<Bytecode::Op::JumpConditional>().set_targets(
                Bytecode::Label { *end_block_ptr },
                Bytecode::Label { *rhs_block_ptr });
        } else if (m_op == AssignmentOp::NullishAssignment) { // ??=
            rhs_block_ptr = &generator.make_block();
            end_block_ptr = &generator.make_block();

            generator.emit<Bytecode::Op::JumpNullish>().set_targets(
                Bytecode::Label { *rhs_block_ptr },
                Bytecode::Label { *end_block_ptr });
        }

        if (rhs_block_ptr)
            generator.switch_to_basic_block(*rhs_block_ptr);

        // lhs_reg is a part of the rhs_block because the store isn't necessary
        // if the logical assignment condition fails.
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
        case AssignmentOp::AndAssignment:
        case AssignmentOp::OrAssignment:
        case AssignmentOp::NullishAssignment:
            break; // These are handled above.
        default:
            TODO();
        }

        generator.emit<Bytecode::Op::SetVariable>(generator.intern_string(identifier.string()));

        if (end_block_ptr) {
            generator.emit<Bytecode::Op::Jump>().set_targets(
                Bytecode::Label { *end_block_ptr },
                {});

            generator.switch_to_basic_block(*end_block_ptr);
        }

        return;
    }

    if (is<MemberExpression>(*m_lhs)) {
        auto& expression = static_cast<MemberExpression const&>(*m_lhs);
        expression.object().generate_bytecode(generator);
        auto object_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(object_reg);

        if (expression.is_computed()) {
            expression.property().generate_bytecode(generator);
            auto property_reg = generator.allocate_register();
            generator.emit<Bytecode::Op::Store>(property_reg);
            m_rhs->generate_bytecode(generator);
            generator.emit<Bytecode::Op::PutByValue>(object_reg, property_reg);
        } else {
            VERIFY(is<Identifier>(expression.property()));
            m_rhs->generate_bytecode(generator);
            auto identifier_table_ref = generator.intern_string(static_cast<Identifier const&>(expression.property()).string());
            generator.emit<Bytecode::Op::PutById>(object_reg, identifier_table_ref);
        }
        return;
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
    generator.begin_breakable_scope(Bytecode::Label { end_block });
    m_body->generate_bytecode(generator);
    if (!generator.is_current_block_terminated()) {
        generator.emit<Bytecode::Op::Jump>().set_targets(
            Bytecode::Label { test_block },
            {});
        generator.end_continuable_scope();
        generator.end_breakable_scope();
        generator.switch_to_basic_block(end_block);
        generator.emit<Bytecode::Op::Load>(result_reg);
    }
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
    generator.begin_breakable_scope(Bytecode::Label { end_block });
    m_body->generate_bytecode(generator);
    if (!generator.is_current_block_terminated()) {
        generator.emit<Bytecode::Op::Jump>().set_targets(
            Bytecode::Label { test_block },
            {});
        generator.end_continuable_scope();
        generator.end_breakable_scope();
        generator.switch_to_basic_block(end_block);
        generator.emit<Bytecode::Op::Load>(result_reg);
    }
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
    generator.begin_breakable_scope(Bytecode::Label { end_block });
    m_body->generate_bytecode(generator);
    generator.end_continuable_scope();

    if (!generator.is_current_block_terminated()) {
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

        generator.end_breakable_scope();
        generator.switch_to_basic_block(end_block);
        generator.emit<Bytecode::Op::Load>(result_reg);
    }
}

void ObjectExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewObject>();
    if (m_properties.is_empty())
        return;

    auto object_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(object_reg);

    for (auto& property : m_properties) {
        if (property.type() != ObjectProperty::Type::KeyValue)
            TODO();

        if (is<StringLiteral>(property.key())) {
            auto& string_literal = static_cast<StringLiteral const&>(property.key());
            Bytecode::StringTableIndex key_name = generator.intern_string(string_literal.value());

            property.value().generate_bytecode(generator);
            generator.emit<Bytecode::Op::PutById>(object_reg, key_name);
        } else {
            property.key().generate_bytecode(generator);
            auto property_reg = generator.allocate_register();
            generator.emit<Bytecode::Op::Store>(property_reg);

            property.value().generate_bytecode(generator);
            generator.emit<Bytecode::Op::PutByValue>(object_reg, property_reg);
        }
    }

    generator.emit<Bytecode::Op::Load>(object_reg);
}

void ArrayExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    Vector<Bytecode::Register> element_regs;
    for (auto& element : m_elements) {
        if (element) {
            element->generate_bytecode(generator);

            if (is<SpreadExpression>(*element)) {
                TODO();
                continue;
            }
        } else {
            generator.emit<Bytecode::Op::LoadImmediate>(Value {});
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
        auto object_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(object_reg);

        property().generate_bytecode(generator);
        generator.emit<Bytecode::Op::GetByValue>(object_reg);
    } else {
        VERIFY(is<Identifier>(property()));
        auto identifier_table_ref = generator.intern_string(static_cast<Identifier const&>(property()).string());
        generator.emit<Bytecode::Op::GetById>(identifier_table_ref);
    }
}

void FunctionDeclaration::generate_bytecode(Bytecode::Generator&) const
{
}

void FunctionExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewFunction>(*this);
}

void VariableDeclaration::generate_bytecode(Bytecode::Generator& generator) const
{
    for (auto& declarator : m_declarations) {
        if (declarator.init())
            declarator.init()->generate_bytecode(generator);
        else
            generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
        declarator.target().visit(
            [&](const NonnullRefPtr<Identifier>& id) {
                generator.emit<Bytecode::Op::SetVariable>(generator.intern_string(id->string()));
            },
            [&](const NonnullRefPtr<BindingPattern>&) {
                TODO();
            });
    }
}

void CallExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    auto callee_reg = generator.allocate_register();
    auto this_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    generator.emit<Bytecode::Op::Store>(this_reg);

    if (is<NewExpression>(this)) {
        m_callee->generate_bytecode(generator);
        generator.emit<Bytecode::Op::Store>(callee_reg);
    } else if (is<SuperExpression>(*m_callee)) {
        TODO();
    } else if (is<MemberExpression>(*m_callee)) {
        auto& member_expression = static_cast<const MemberExpression&>(*m_callee);
        if (is<SuperExpression>(member_expression.object())) {
            TODO();
        } else {
            member_expression.object().generate_bytecode(generator);
            generator.emit<Bytecode::Op::Store>(this_reg);
            // FIXME: Don't copy this logic here, make MemberExpression generate it.
            if (!is<Identifier>(member_expression.property()))
                TODO();
            auto identifier_table_ref = generator.intern_string(static_cast<Identifier const&>(member_expression.property()).string());
            generator.emit<Bytecode::Op::GetById>(identifier_table_ref);
            generator.emit<Bytecode::Op::Store>(callee_reg);
        }
    } else {
        // FIXME: this = global object in sloppy mode.
        m_callee->generate_bytecode(generator);
        generator.emit<Bytecode::Op::Store>(callee_reg);
    }

    Vector<Bytecode::Register> argument_registers;
    for (auto& arg : m_arguments) {
        arg.value->generate_bytecode(generator);
        auto arg_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(arg_reg);
        argument_registers.append(arg_reg);
    }

    Bytecode::Op::Call::CallType call_type;
    if (is<NewExpression>(*this)) {
        call_type = Bytecode::Op::Call::CallType::Construct;
    } else {
        call_type = Bytecode::Op::Call::CallType::Call;
    }

    generator.emit_with_extra_register_slots<Bytecode::Op::Call>(argument_registers.size(), call_type, callee_reg, this_reg, argument_registers);
}

void ReturnStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    if (m_argument)
        m_argument->generate_bytecode(generator);

    if (generator.is_in_generator_function())
        generator.emit<Bytecode::Op::Yield>(nullptr);
    else
        generator.emit<Bytecode::Op::Return>();
}

void YieldExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    VERIFY(generator.is_in_generator_function());

    if (m_is_yield_from)
        TODO();

    if (m_argument)
        m_argument->generate_bytecode(generator);

    auto& continuation_block = generator.make_block();
    generator.emit<Bytecode::Op::Yield>(Bytecode::Label { continuation_block });
    generator.switch_to_basic_block(continuation_block);
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

    generator.emit<Bytecode::Op::Load>(string_reg);
}

void TaggedTemplateLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    m_tag->generate_bytecode(generator);
    auto tag_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(tag_reg);

    Vector<Bytecode::Register> string_regs;
    auto& expressions = m_template_literal->expressions();
    for (size_t i = 0; i < expressions.size(); ++i) {
        if (i % 2 != 0)
            continue;

        expressions[i].generate_bytecode(generator);
        auto string_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(string_reg);
        string_regs.append(string_reg);
    }

    generator.emit_with_extra_register_slots<Bytecode::Op::NewArray>(string_regs.size(), string_regs);
    auto strings_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(strings_reg);

    Vector<Bytecode::Register> argument_regs;
    argument_regs.append(strings_reg);
    for (size_t i = 0; i < expressions.size(); ++i) {
        if (i % 2 == 0)
            continue;

        expressions[i].generate_bytecode(generator);
        auto string_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(string_reg);
        argument_regs.append(string_reg);
    }

    Vector<Bytecode::Register> raw_string_regs;
    for (auto& raw_string : m_template_literal->raw_strings()) {
        raw_string.generate_bytecode(generator);
        auto raw_string_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(raw_string_reg);
        raw_string_regs.append(raw_string_reg);
    }

    generator.emit_with_extra_register_slots<Bytecode::Op::NewArray>(raw_string_regs.size(), raw_string_regs);
    auto raw_strings_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(raw_strings_reg);

    generator.emit<Bytecode::Op::Load>(strings_reg);
    generator.emit<Bytecode::Op::PutById>(raw_strings_reg, generator.intern_string("raw"));

    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    auto this_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(this_reg);

    generator.emit_with_extra_register_slots<Bytecode::Op::Call>(argument_regs.size(), Bytecode::Op::Call::CallType::Call, tag_reg, this_reg, move(argument_regs));
}

void UpdateExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    if (is<Identifier>(*m_argument)) {
        auto& identifier = static_cast<Identifier const&>(*m_argument);
        generator.emit<Bytecode::Op::GetVariable>(generator.intern_string(identifier.string()));

        Optional<Bytecode::Register> previous_value_for_postfix_reg;
        if (!m_prefixed) {
            previous_value_for_postfix_reg = generator.allocate_register();
            generator.emit<Bytecode::Op::Store>(*previous_value_for_postfix_reg);
        }

        if (m_op == UpdateOp::Increment)
            generator.emit<Bytecode::Op::Increment>();
        else
            generator.emit<Bytecode::Op::Decrement>();

        generator.emit<Bytecode::Op::SetVariable>(generator.intern_string(identifier.string()));

        if (!m_prefixed)
            generator.emit<Bytecode::Op::Load>(*previous_value_for_postfix_reg);
        return;
    }

    TODO();
}

void ThrowStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    m_argument->generate_bytecode(generator);
    generator.emit<Bytecode::Op::Throw>();
}

void BreakStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::Jump>().set_targets(
        generator.nearest_breakable_scope(),
        {});
}

void TryStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    auto& saved_block = generator.current_block();

    Optional<Bytecode::Label> handler_target;
    Optional<Bytecode::Label> finalizer_target;

    Bytecode::BasicBlock* next_block { nullptr };

    if (m_finalizer) {
        auto& finalizer_block = generator.make_block();
        generator.switch_to_basic_block(finalizer_block);
        m_finalizer->generate_bytecode(generator);
        if (!generator.is_current_block_terminated()) {
            next_block = &generator.make_block();
            auto next_target = Bytecode::Label { *next_block };
            generator.emit<Bytecode::Op::ContinuePendingUnwind>(next_target);
        }
        finalizer_target = Bytecode::Label { finalizer_block };
    }

    if (m_handler) {
        auto& handler_block = generator.make_block();
        generator.switch_to_basic_block(handler_block);
        if (!m_finalizer)
            generator.emit<Bytecode::Op::LeaveUnwindContext>();
        if (!m_handler->parameter().is_empty()) {
            // FIXME: We need a separate LexicalEnvironment here
            generator.emit<Bytecode::Op::SetVariable>(generator.intern_string(m_handler->parameter()));
        }
        m_handler->body().generate_bytecode(generator);
        handler_target = Bytecode::Label { handler_block };
        if (!generator.is_current_block_terminated()) {
            if (m_finalizer) {
                generator.emit<Bytecode::Op::LeaveUnwindContext>();
                generator.emit<Bytecode::Op::Jump>(finalizer_target);
            } else {
                VERIFY(!next_block);
                next_block = &generator.make_block();
                auto next_target = Bytecode::Label { *next_block };
                generator.emit<Bytecode::Op::Jump>(next_target);
            }
        }
    }

    auto& target_block = generator.make_block();
    generator.switch_to_basic_block(saved_block);
    generator.emit<Bytecode::Op::EnterUnwindContext>(Bytecode::Label { target_block }, handler_target, finalizer_target);

    generator.switch_to_basic_block(target_block);
    m_block->generate_bytecode(generator);
    if (m_finalizer && !generator.is_current_block_terminated())
        generator.emit<Bytecode::Op::Jump>(finalizer_target);

    generator.switch_to_basic_block(next_block ? *next_block : saved_block);
}

void SwitchStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    auto discriminant_reg = generator.allocate_register();
    m_discriminant->generate_bytecode(generator);
    generator.emit<Bytecode::Op::Store>(discriminant_reg);
    Vector<Bytecode::BasicBlock&> case_blocks;
    Bytecode::BasicBlock* default_block { nullptr };
    Bytecode::BasicBlock* next_test_block = &generator.make_block();
    generator.emit<Bytecode::Op::Jump>().set_targets(Bytecode::Label { *next_test_block }, {});
    for (auto& switch_case : m_cases) {
        auto& case_block = generator.make_block();
        if (switch_case.test()) {
            generator.switch_to_basic_block(*next_test_block);
            switch_case.test()->generate_bytecode(generator);
            generator.emit<Bytecode::Op::TypedEquals>(discriminant_reg);
            next_test_block = &generator.make_block();
            generator.emit<Bytecode::Op::JumpConditional>().set_targets(Bytecode::Label { case_block }, Bytecode::Label { *next_test_block });
        } else {
            default_block = &case_block;
        }
        case_blocks.append(case_block);
    }
    generator.switch_to_basic_block(*next_test_block);
    auto& end_block = generator.make_block();

    if (default_block != nullptr) {
        generator.emit<Bytecode::Op::Jump>().set_targets(Bytecode::Label { *default_block }, {});
    } else {
        generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
        generator.emit<Bytecode::Op::Jump>().set_targets(Bytecode::Label { end_block }, {});
    }
    auto current_block = case_blocks.begin();
    generator.begin_breakable_scope(Bytecode::Label { end_block });
    for (auto& switch_case : m_cases) {
        generator.switch_to_basic_block(*current_block);

        generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
        for (auto& statement : switch_case.consequent()) {
            statement.generate_bytecode(generator);
        }
        if (!generator.is_current_block_terminated()) {
            auto next_block = current_block;
            next_block++;
            if (next_block.is_end()) {
                generator.emit<Bytecode::Op::Jump>().set_targets(Bytecode::Label { end_block }, {});
            } else {
                generator.emit<Bytecode::Op::Jump>().set_targets(Bytecode::Label { *next_block }, {});
            }
        }
        current_block++;
    }
    generator.end_breakable_scope();

    generator.switch_to_basic_block(end_block);
}

}
