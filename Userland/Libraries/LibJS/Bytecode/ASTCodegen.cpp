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
#include <LibJS/Runtime/Environment.h>

namespace JS {

void ASTNode::generate_bytecode(Bytecode::Generator&) const
{
    dbgln("Missing generate_bytecode() in {}", class_name());
    TODO();
}

void ScopeNode::generate_bytecode(Bytecode::Generator& generator) const
{
    // FIXME: This is an ad-hoc fix but should be done as the spec says in
    //        {Global, Block, Function, Eval}DeclarationInstantiation.
    for (auto& function : m_functions_hoistable_with_annexB_extension) {
        generator.emit<Bytecode::Op::NewFunction>(function);
        generator.emit<Bytecode::Op::SetVariable>(generator.intern_identifier(function.name()));
    }

    HashTable<FlyString> functions_initialized;
    for_each_var_function_declaration_in_reverse_order([&](FunctionDeclaration const& function) {
        if (functions_initialized.set(function.name()) != AK::HashSetResult::InsertedNewEntry)
            return IterationDecision::Continue;

        generator.emit<Bytecode::Op::NewFunction>(function);
        generator.emit<Bytecode::Op::SetVariable>(generator.intern_identifier(function.name()));

        return IterationDecision::Continue;
    });

    // FIXME: Register lexical and variable scope declarations
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
    case BinaryOp::LooselyInequals:
        generator.emit<Bytecode::Op::LooselyInequals>(lhs_reg);
        break;
    case BinaryOp::LooselyEquals:
        generator.emit<Bytecode::Op::LooselyEquals>(lhs_reg);
        break;
    case BinaryOp::StrictlyInequals:
        generator.emit<Bytecode::Op::StrictlyInequals>(lhs_reg);
        break;
    case BinaryOp::StrictlyEquals:
        generator.emit<Bytecode::Op::StrictlyEquals>(lhs_reg);
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
    generator.emit<Bytecode::Op::NewBigInt>(Crypto::SignedBigInteger::from_base(10, m_value.substring(0, m_value.length() - 1)));
}

void StringLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewString>(generator.intern_string(m_value));
}

void RegExpLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    auto source_index = generator.intern_string(m_pattern);
    auto flags_index = generator.intern_string(m_flags);
    generator.emit<Bytecode::Op::NewRegExp>(source_index, flags_index);
}

void Identifier::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::GetVariable>(generator.intern_identifier(m_string));
}

void AssignmentExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    // FIXME: Implement this for BindingPatterns too.
    auto& lhs = m_lhs.get<NonnullRefPtr<Expression>>();

    if (m_op == AssignmentOp::Assignment) {
        m_rhs->generate_bytecode(generator);
        generator.emit_store_to_reference(lhs);
        return;
    }

    generator.emit_load_from_reference(lhs);

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

    generator.emit_store_to_reference(lhs);

    if (end_block_ptr) {
        generator.emit<Bytecode::Op::Jump>().set_targets(
            Bytecode::Label { *end_block_ptr },
            {});

        generator.switch_to_basic_block(*end_block_ptr);
    }
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
            Bytecode::IdentifierTableIndex key_name = generator.intern_identifier(string_literal.value());

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
    generator.emit_load_from_reference(*this);
}

void FunctionDeclaration::generate_bytecode(Bytecode::Generator&) const
{
}

void FunctionExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewFunction>(*this);
}

static void generate_binding_pattern_bytecode(Bytecode::Generator& generator, BindingPattern const& pattern, Bytecode::Register const& value_reg);

static void generate_object_binding_pattern_bytecode(Bytecode::Generator& generator, BindingPattern const& pattern, Bytecode::Register const& value_reg)
{
    Vector<Bytecode::Register> excluded_property_names;
    auto has_rest = false;
    if (pattern.entries.size() > 0)
        has_rest = pattern.entries[pattern.entries.size() - 1].is_rest;

    for (auto& [name, alias, initializer, is_rest] : pattern.entries) {
        if (is_rest) {
            VERIFY(name.has<NonnullRefPtr<Identifier>>());
            VERIFY(alias.has<Empty>());
            VERIFY(!initializer);

            auto identifier = name.get<NonnullRefPtr<Identifier>>()->string();
            auto interned_identifier = generator.intern_identifier(identifier);

            generator.emit_with_extra_register_slots<Bytecode::Op::CopyObjectExcludingProperties>(excluded_property_names.size(), value_reg, excluded_property_names);
            generator.emit<Bytecode::Op::SetVariable>(interned_identifier);

            return;
        }

        Bytecode::StringTableIndex name_index;

        if (name.has<NonnullRefPtr<Identifier>>()) {
            auto identifier = name.get<NonnullRefPtr<Identifier>>()->string();
            name_index = generator.intern_string(identifier);

            if (has_rest) {
                auto excluded_name_reg = generator.allocate_register();
                excluded_property_names.append(excluded_name_reg);
                generator.emit<Bytecode::Op::NewString>(name_index);
                generator.emit<Bytecode::Op::Store>(excluded_name_reg);
            }

            generator.emit<Bytecode::Op::Load>(value_reg);
            generator.emit<Bytecode::Op::GetById>(generator.intern_identifier(identifier));
        } else {
            auto expression = name.get<NonnullRefPtr<Expression>>();
            expression->generate_bytecode(generator);

            if (has_rest) {
                auto excluded_name_reg = generator.allocate_register();
                excluded_property_names.append(excluded_name_reg);
                generator.emit<Bytecode::Op::Store>(excluded_name_reg);
            }

            generator.emit<Bytecode::Op::GetByValue>(value_reg);
        }

        if (initializer) {
            auto& if_undefined_block = generator.make_block();
            auto& if_not_undefined_block = generator.make_block();

            generator.emit<Bytecode::Op::JumpUndefined>().set_targets(
                Bytecode::Label { if_undefined_block },
                Bytecode::Label { if_not_undefined_block });

            generator.switch_to_basic_block(if_undefined_block);
            initializer->generate_bytecode(generator);
            generator.emit<Bytecode::Op::Jump>().set_targets(
                Bytecode::Label { if_not_undefined_block },
                {});

            generator.switch_to_basic_block(if_not_undefined_block);
        }

        if (alias.has<NonnullRefPtr<BindingPattern>>()) {
            auto& binding_pattern = *alias.get<NonnullRefPtr<BindingPattern>>();
            auto nested_value_reg = generator.allocate_register();
            generator.emit<Bytecode::Op::Store>(nested_value_reg);
            generate_binding_pattern_bytecode(generator, binding_pattern, nested_value_reg);
        } else if (alias.has<Empty>()) {
            if (name.has<NonnullRefPtr<Expression>>()) {
                // This needs some sort of SetVariableByValue opcode, as it's a runtime binding
                TODO();
            }

            auto& identifier = alias.get<NonnullRefPtr<Identifier>>()->string();
            generator.emit<Bytecode::Op::SetVariable>(generator.intern_identifier(identifier));
        } else {
            auto& identifier = alias.get<NonnullRefPtr<Identifier>>()->string();
            generator.emit<Bytecode::Op::SetVariable>(generator.intern_identifier(identifier));
        }
    }
}

static void generate_array_binding_pattern_bytecode(Bytecode::Generator& generator, BindingPattern const& pattern, Bytecode::Register const& value_reg)
{
    /*
     * Consider the following destructuring assignment:
     *
     *     let [a, b, c, d, e] = o;
     *
     * It would be fairly trivial to just loop through this iterator, getting the value
     * at each step and assigning them to the binding sequentially. However, this is not
     * correct: once an iterator is exhausted, it must not be called again. This complicates
     * the bytecode. In order to accomplish this, we do the following:
     *
     * - Reserve a special boolean register which holds 'true' if the iterator is exhausted,
     *   and false otherwise
     * - When we are retrieving the value which should be bound, we first check this register.
     *   If it is 'true', we load undefined into the accumulator. Otherwise, we grab the next
     *   value from the iterator and store it into the accumulator.
     *
     * Note that the is_exhausted register does not need to be loaded with false because the
     * first IteratorNext bytecode is _not_ proceeded by an exhausted check, as it is
     * unnecessary.
     */

    auto is_iterator_exhausted_register = generator.allocate_register();

    auto iterator_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Load>(value_reg);
    generator.emit<Bytecode::Op::GetIterator>();
    generator.emit<Bytecode::Op::Store>(iterator_reg);
    bool first = true;

    auto temp_iterator_result_reg = generator.allocate_register();

    auto assign_accumulator_to_alias = [&](auto& alias) {
        alias.visit(
            [&](Empty) {
                // This element is an elision
            },
            [&](NonnullRefPtr<Identifier> const& identifier) {
                auto interned_index = generator.intern_identifier(identifier->string());
                generator.emit<Bytecode::Op::SetVariable>(interned_index);
            },
            [&](NonnullRefPtr<BindingPattern> const& pattern) {
                // Store the accumulator value in a permanent register
                auto target_reg = generator.allocate_register();
                generator.emit<Bytecode::Op::Store>(target_reg);
                generate_binding_pattern_bytecode(generator, pattern, target_reg);
            },
            [&](NonnullRefPtr<MemberExpression> const&) {
                TODO();
            });
    };

    for (auto& [name, alias, initializer, is_rest] : pattern.entries) {
        VERIFY(name.has<Empty>());

        if (is_rest) {
            if (first) {
                // The iterator has not been called, and is thus known to be not exhausted
                generator.emit<Bytecode::Op::Load>(iterator_reg);
                generator.emit<Bytecode::Op::IteratorToArray>();
            } else {
                auto& if_exhausted_block = generator.make_block();
                auto& if_not_exhausted_block = generator.make_block();
                auto& continuation_block = generator.make_block();

                generator.emit<Bytecode::Op::Load>(is_iterator_exhausted_register);
                generator.emit<Bytecode::Op::JumpConditional>().set_targets(
                    Bytecode::Label { if_exhausted_block },
                    Bytecode::Label { if_not_exhausted_block });

                generator.switch_to_basic_block(if_exhausted_block);
                generator.emit<Bytecode::Op::NewArray>();
                generator.emit<Bytecode::Op::Jump>().set_targets(
                    Bytecode::Label { continuation_block },
                    {});

                generator.switch_to_basic_block(if_not_exhausted_block);
                generator.emit<Bytecode::Op::Load>(iterator_reg);
                generator.emit<Bytecode::Op::IteratorToArray>();
                generator.emit<Bytecode::Op::Jump>().set_targets(
                    Bytecode::Label { continuation_block },
                    {});

                generator.switch_to_basic_block(continuation_block);
            }

            assign_accumulator_to_alias(alias);

            return;
        }

        // In the first iteration of the loop, a few things are true which can save
        // us some bytecode:
        //  - the iterator result is still in the accumulator, so we can avoid a load
        //  - the iterator is not yet exhausted, which can save us a jump and some
        //    creation

        auto& iterator_is_exhausted_block = generator.make_block();

        if (!first) {
            auto& iterator_is_not_exhausted_block = generator.make_block();

            generator.emit<Bytecode::Op::Load>(is_iterator_exhausted_register);
            generator.emit<Bytecode::Op::JumpConditional>().set_targets(
                Bytecode::Label { iterator_is_exhausted_block },
                Bytecode::Label { iterator_is_not_exhausted_block });

            generator.switch_to_basic_block(iterator_is_not_exhausted_block);
            generator.emit<Bytecode::Op::Load>(iterator_reg);
        }

        generator.emit<Bytecode::Op::IteratorNext>();
        generator.emit<Bytecode::Op::Store>(temp_iterator_result_reg);
        generator.emit<Bytecode::Op::IteratorResultDone>();
        generator.emit<Bytecode::Op::Store>(is_iterator_exhausted_register);

        // We still have to check for exhaustion here. If the iterator is exhausted,
        // we need to bail before trying to get the value
        auto& no_bail_block = generator.make_block();
        generator.emit<Bytecode::Op::JumpConditional>().set_targets(
            Bytecode::Label { iterator_is_exhausted_block },
            Bytecode::Label { no_bail_block });

        generator.switch_to_basic_block(no_bail_block);

        // Get the next value in the iterator
        generator.emit<Bytecode::Op::Load>(temp_iterator_result_reg);
        generator.emit<Bytecode::Op::IteratorResultValue>();

        auto& create_binding_block = generator.make_block();
        generator.emit<Bytecode::Op::Jump>().set_targets(
            Bytecode::Label { create_binding_block },
            {});

        // The iterator is exhausted, so we just load undefined and continue binding
        generator.switch_to_basic_block(iterator_is_exhausted_block);
        generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
        generator.emit<Bytecode::Op::Jump>().set_targets(
            Bytecode::Label { create_binding_block },
            {});

        // Create the actual binding. The value which this entry must bind is now in the
        // accumulator. We can proceed, processing the alias as a nested  destructuring
        // pattern if necessary.
        generator.switch_to_basic_block(create_binding_block);

        assign_accumulator_to_alias(alias);

        first = false;
    }
}

static void generate_binding_pattern_bytecode(Bytecode::Generator& generator, BindingPattern const& pattern, Bytecode::Register const& value_reg)
{
    if (pattern.kind == BindingPattern::Kind::Object) {
        generate_object_binding_pattern_bytecode(generator, pattern, value_reg);
    } else {
        generate_array_binding_pattern_bytecode(generator, pattern, value_reg);
    }
};

void VariableDeclaration::generate_bytecode(Bytecode::Generator& generator) const
{
    for (auto& declarator : m_declarations) {
        if (declarator.init())
            declarator.init()->generate_bytecode(generator);
        else
            generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
        declarator.target().visit(
            [&](NonnullRefPtr<Identifier> const& id) {
                generator.emit<Bytecode::Op::SetVariable>(generator.intern_identifier(id->string()));
            },
            [&](NonnullRefPtr<BindingPattern> const& pattern) {
                auto value_register = generator.allocate_register();
                generator.emit<Bytecode::Op::Store>(value_register);
                generate_binding_pattern_bytecode(generator, pattern, value_register);
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
            if (member_expression.is_computed()) {
                member_expression.property().generate_bytecode(generator);
                generator.emit<Bytecode::Op::GetByValue>(this_reg);
            } else {
                auto identifier_table_ref = generator.intern_identifier(verify_cast<Identifier>(member_expression.property()).string());
                generator.emit<Bytecode::Op::GetById>(identifier_table_ref);
            }
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

    if (generator.is_in_generator_or_async_function())
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

    auto& true_block = generator.make_block();
    auto& false_block = generator.make_block();

    m_predicate->generate_bytecode(generator);
    generator.emit<Bytecode::Op::JumpConditional>().set_targets(
        Bytecode::Label { true_block },
        Bytecode::Label { false_block });

    Bytecode::Op::Jump* true_block_jump { nullptr };

    generator.switch_to_basic_block(true_block);
    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    m_consequent->generate_bytecode(generator);
    if (!generator.is_current_block_terminated())
        true_block_jump = &generator.emit<Bytecode::Op::Jump>();

    generator.switch_to_basic_block(false_block);
    auto& end_block = generator.make_block();

    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    if (m_alternate)
        m_alternate->generate_bytecode(generator);
    if (!generator.is_current_block_terminated())
        generator.emit<Bytecode::Op::Jump>().set_targets(Bytecode::Label { end_block }, {});

    if (true_block_jump)
        true_block_jump->set_targets(Bytecode::Label { end_block }, {});

    generator.switch_to_basic_block(end_block);
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
    generator.emit<Bytecode::Op::PutById>(raw_strings_reg, generator.intern_identifier("raw"));

    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    auto this_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(this_reg);

    generator.emit_with_extra_register_slots<Bytecode::Op::Call>(argument_regs.size(), Bytecode::Op::Call::CallType::Call, tag_reg, this_reg, move(argument_regs));
}

void UpdateExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit_load_from_reference(*m_argument);

    Optional<Bytecode::Register> previous_value_for_postfix_reg;
    if (!m_prefixed) {
        previous_value_for_postfix_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(*previous_value_for_postfix_reg);
    }

    if (m_op == UpdateOp::Increment)
        generator.emit<Bytecode::Op::Increment>();
    else
        generator.emit<Bytecode::Op::Decrement>();

    generator.emit_store_to_reference(*m_argument);

    if (!m_prefixed)
        generator.emit<Bytecode::Op::Load>(*previous_value_for_postfix_reg);
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
        m_handler->parameter().visit(
            [&](FlyString const& parameter) {
                if (!parameter.is_empty()) {
                    // FIXME: We need a separate DeclarativeEnvironment here
                    generator.emit<Bytecode::Op::SetVariable>(generator.intern_identifier(parameter));
                }
            },
            [&](NonnullRefPtr<BindingPattern> const&) {
                // FIXME: Implement this path when the above DeclrativeEnvironment issue is dealt with.
                TODO();
            });

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
    if (!generator.is_current_block_terminated()) {
        if (m_finalizer) {
            generator.emit<Bytecode::Op::Jump>(finalizer_target);
        } else {
            auto& block = generator.make_block();
            generator.emit<Bytecode::Op::FinishUnwind>(Bytecode::Label { block });
            next_block = &block;
        }
    }

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
            generator.emit<Bytecode::Op::StrictlyEquals>(discriminant_reg);
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
        for (auto& statement : switch_case.children()) {
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

void ClassDeclaration::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewClass>(m_class_expression);
    generator.emit<Bytecode::Op::SetVariable>(generator.intern_identifier(m_class_expression.ptr()->name()));
}

void ThisExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::ResolveThisBinding>();
}

void AwaitExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    VERIFY(generator.is_in_async_function());

    // Transform `await expr` to `yield expr`
    m_argument->generate_bytecode(generator);

    auto& continuation_block = generator.make_block();
    generator.emit<Bytecode::Op::Yield>(Bytecode::Label { continuation_block });
    generator.switch_to_basic_block(continuation_block);
}

}
