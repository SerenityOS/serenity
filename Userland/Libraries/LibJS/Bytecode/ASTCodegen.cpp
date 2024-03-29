/*
 * Copyright (c) 2021-2024, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2021, Marcin Gasperowicz <xnooga@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Find.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Bytecode/StringTable.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/ErrorTypes.h>

namespace JS {

static Bytecode::Operand choose_dst(Bytecode::Generator& generator, Optional<Bytecode::Operand> const& preferred_dst)
{
    if (preferred_dst.has_value())
        return preferred_dst.value();
    return Bytecode::Operand(generator.allocate_register());
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ASTNode::generate_bytecode(Bytecode::Generator&, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    return Bytecode::CodeGenerationError {
        this,
        "Missing generate_bytecode()"sv,
    };
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ScopeNode::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    bool did_create_lexical_environment = false;

    if (is<BlockStatement>(*this)) {
        if (has_lexical_declarations()) {
            generator.block_declaration_instantiation(*this);
            did_create_lexical_environment = true;
        }
    } else if (is<Program>(*this)) {
        // GlobalDeclarationInstantiation is handled by the C++ AO.
    } else {
        // FunctionDeclarationInstantiation is handled by the C++ AO.
    }

    Optional<Bytecode::Operand> last_result;
    for (auto& child : children()) {
        auto result = TRY(child->generate_bytecode(generator));
        if (result.has_value())
            last_result = result;
        if (generator.is_current_block_terminated())
            break;
    }

    if (did_create_lexical_environment)
        generator.end_variable_scope();

    return last_result;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> EmptyStatement::generate_bytecode(Bytecode::Generator&, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    return Optional<Bytecode::Operand> {};
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ExpressionStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return m_expression->generate_bytecode(generator);
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> BinaryExpression::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    if (m_op == BinaryOp::In && is<PrivateIdentifier>(*m_lhs)) {
        auto const& private_identifier = static_cast<PrivateIdentifier const&>(*m_lhs).string();
        auto base = TRY(m_rhs->generate_bytecode(generator)).value();
        auto dst = choose_dst(generator, preferred_dst);
        generator.emit<Bytecode::Op::HasPrivateId>(dst, base, generator.intern_identifier(private_identifier));
        return dst;
    }

    auto lhs = TRY(m_lhs->generate_bytecode(generator)).value();
    auto rhs = TRY(m_rhs->generate_bytecode(generator)).value();

    auto dst = choose_dst(generator, preferred_dst);
    switch (m_op) {
    case BinaryOp::Addition:
        generator.emit<Bytecode::Op::Add>(dst, lhs, rhs);
        break;
    case BinaryOp::Subtraction:
        generator.emit<Bytecode::Op::Sub>(dst, lhs, rhs);
        break;
    case BinaryOp::Multiplication:
        generator.emit<Bytecode::Op::Mul>(dst, lhs, rhs);
        break;
    case BinaryOp::Division:
        generator.emit<Bytecode::Op::Div>(dst, lhs, rhs);
        break;
    case BinaryOp::Modulo:
        generator.emit<Bytecode::Op::Mod>(dst, lhs, rhs);
        break;
    case BinaryOp::Exponentiation:
        generator.emit<Bytecode::Op::Exp>(dst, lhs, rhs);
        break;
    case BinaryOp::GreaterThan:
        generator.emit<Bytecode::Op::GreaterThan>(dst, lhs, rhs);
        break;
    case BinaryOp::GreaterThanEquals:
        generator.emit<Bytecode::Op::GreaterThanEquals>(dst, lhs, rhs);
        break;
    case BinaryOp::LessThan:
        generator.emit<Bytecode::Op::LessThan>(dst, lhs, rhs);
        break;
    case BinaryOp::LessThanEquals:
        generator.emit<Bytecode::Op::LessThanEquals>(dst, lhs, rhs);
        break;
    case BinaryOp::LooselyInequals:
        generator.emit<Bytecode::Op::LooselyInequals>(dst, lhs, rhs);
        break;
    case BinaryOp::LooselyEquals:
        generator.emit<Bytecode::Op::LooselyEquals>(dst, lhs, rhs);
        break;
    case BinaryOp::StrictlyInequals:
        generator.emit<Bytecode::Op::StrictlyInequals>(dst, lhs, rhs);
        break;
    case BinaryOp::StrictlyEquals:
        generator.emit<Bytecode::Op::StrictlyEquals>(dst, lhs, rhs);
        break;
    case BinaryOp::BitwiseAnd:
        generator.emit<Bytecode::Op::BitwiseAnd>(dst, lhs, rhs);
        break;
    case BinaryOp::BitwiseOr:
        generator.emit<Bytecode::Op::BitwiseOr>(dst, lhs, rhs);
        break;
    case BinaryOp::BitwiseXor:
        generator.emit<Bytecode::Op::BitwiseXor>(dst, lhs, rhs);
        break;
    case BinaryOp::LeftShift:
        generator.emit<Bytecode::Op::LeftShift>(dst, lhs, rhs);
        break;
    case BinaryOp::RightShift:
        generator.emit<Bytecode::Op::RightShift>(dst, lhs, rhs);
        break;
    case BinaryOp::UnsignedRightShift:
        generator.emit<Bytecode::Op::UnsignedRightShift>(dst, lhs, rhs);
        break;
    case BinaryOp::In:
        generator.emit<Bytecode::Op::In>(dst, lhs, rhs);
        break;
    case BinaryOp::InstanceOf:
        generator.emit<Bytecode::Op::InstanceOf>(dst, lhs, rhs);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> LogicalExpression::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto dst = choose_dst(generator, preferred_dst);
    auto lhs = TRY(m_lhs->generate_bytecode(generator, preferred_dst)).value();
    // FIXME: Only mov lhs into dst in case lhs is the value taken.
    generator.emit<Bytecode::Op::Mov>(dst, lhs);

    // lhs
    // jump op (true) end (false) rhs
    // rhs
    // jump always (true) end
    // end

    auto& rhs_block = generator.make_block();
    auto& end_block = generator.make_block();

    switch (m_op) {
    case LogicalOp::And:
        generator.emit<Bytecode::Op::JumpIf>(
            lhs,
            Bytecode::Label { rhs_block },
            Bytecode::Label { end_block });
        break;
    case LogicalOp::Or:
        generator.emit<Bytecode::Op::JumpIf>(
            lhs,
            Bytecode::Label { end_block },
            Bytecode::Label { rhs_block });
        break;
    case LogicalOp::NullishCoalescing:
        generator.emit<Bytecode::Op::JumpNullish>(
            lhs,
            Bytecode::Label { rhs_block },
            Bytecode::Label { end_block });
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    generator.switch_to_basic_block(rhs_block);
    auto rhs = TRY(m_rhs->generate_bytecode(generator)).value();

    generator.emit<Bytecode::Op::Mov>(dst, rhs);
    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { end_block });
    generator.switch_to_basic_block(end_block);
    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> UnaryExpression::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    if (m_op == UnaryOp::Delete)
        return generator.emit_delete_reference(m_lhs);

    Optional<Bytecode::Operand> src;
    // Typeof needs some special handling for when the LHS is an Identifier. Namely, it shouldn't throw on unresolvable references, but instead return "undefined".
    if (m_op != UnaryOp::Typeof)
        src = TRY(m_lhs->generate_bytecode(generator)).value();

    auto dst = choose_dst(generator, preferred_dst);

    switch (m_op) {
    case UnaryOp::BitwiseNot:
        generator.emit<Bytecode::Op::BitwiseNot>(dst, *src);
        break;
    case UnaryOp::Not:
        generator.emit<Bytecode::Op::Not>(dst, *src);
        break;
    case UnaryOp::Plus:
        generator.emit<Bytecode::Op::UnaryPlus>(dst, *src);
        break;
    case UnaryOp::Minus:
        generator.emit<Bytecode::Op::UnaryMinus>(dst, *src);
        break;
    case UnaryOp::Typeof:
        if (is<Identifier>(*m_lhs)) {
            auto& identifier = static_cast<Identifier const&>(*m_lhs);
            if (!identifier.is_local()) {
                generator.emit<Bytecode::Op::TypeofVariable>(dst, generator.intern_identifier(identifier.string()));
                break;
            }
        }

        src = TRY(m_lhs->generate_bytecode(generator)).value();
        generator.emit<Bytecode::Op::Typeof>(dst, *src);
        break;
    case UnaryOp::Void:
        return generator.add_constant(js_undefined());
    case UnaryOp::Delete: // Delete is implemented above.
    default:
        VERIFY_NOT_REACHED();
    }

    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> NumericLiteral::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generator.add_constant(Value(m_value), Bytecode::Generator::DeduplicateConstant::No);
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> BooleanLiteral::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generator.add_constant(Value(m_value));
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> NullLiteral::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generator.add_constant(js_null());
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> BigIntLiteral::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    // 1. Return the NumericValue of NumericLiteral as defined in 12.8.3.
    auto integer = [&] {
        if (m_value[0] == '0' && m_value.length() >= 3)
            if (m_value[1] == 'x' || m_value[1] == 'X')
                return MUST(Crypto::SignedBigInteger::from_base(16, m_value.substring(2, m_value.length() - 3)));
        if (m_value[1] == 'o' || m_value[1] == 'O')
            return MUST(Crypto::SignedBigInteger::from_base(8, m_value.substring(2, m_value.length() - 3)));
        if (m_value[1] == 'b' || m_value[1] == 'B')
            return MUST(Crypto::SignedBigInteger::from_base(2, m_value.substring(2, m_value.length() - 3)));
        return MUST(Crypto::SignedBigInteger::from_base(10, m_value.substring(0, m_value.length() - 1)));
    }();
    return generator.add_constant(BigInt::create(generator.vm(), move(integer)), Bytecode::Generator::DeduplicateConstant::No);
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> StringLiteral::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generator.add_constant(PrimitiveString::create(generator.vm(), m_value), Bytecode::Generator::DeduplicateConstant::No);
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> RegExpLiteral::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto source_index = generator.intern_string(m_pattern);
    auto flags_index = generator.intern_string(m_flags);
    auto regex_index = generator.intern_regex(Bytecode::ParsedRegex {
        .regex = m_parsed_regex,
        .pattern = m_parsed_pattern,
        .flags = m_parsed_flags,
    });
    auto dst = choose_dst(generator, preferred_dst);
    generator.emit<Bytecode::Op::NewRegExp>(dst, source_index, flags_index, regex_index);
    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> Identifier::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);

    if (is_local()) {
        auto local = Bytecode::Operand(Bytecode::Operand::Type::Local, local_variable_index());
        if (!generator.is_local_initialized(local_variable_index())) {
            generator.emit<Bytecode::Op::ThrowIfTDZ>(local);
        }
        return local;
    }

    auto dst = choose_dst(generator, preferred_dst);
    if (is_global()) {
        generator.emit<Bytecode::Op::GetGlobal>(dst, generator.intern_identifier(m_string), generator.next_global_variable_cache());
    } else {
        generator.emit<Bytecode::Op::GetVariable>(dst, generator.intern_identifier(m_string), generator.next_environment_variable_cache());
    }
    return dst;
}

static Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> arguments_to_array_for_call(Bytecode::Generator& generator, ReadonlySpan<CallExpression::Argument> arguments)
{
    auto dst = Bytecode::Operand(generator.allocate_register());
    if (arguments.is_empty()) {
        generator.emit<Bytecode::Op::NewArray>(dst);
        return dst;
    }

    auto first_spread = find_if(arguments.begin(), arguments.end(), [](auto el) { return el.is_spread; });

    Bytecode::Register args_start_reg { 0 };
    for (auto it = arguments.begin(); it != first_spread; ++it) {
        auto reg = generator.allocate_register();
        if (args_start_reg.index() == 0)
            args_start_reg = reg;
    }
    u32 i = 0;
    for (auto it = arguments.begin(); it != first_spread; ++it, ++i) {
        VERIFY(it->is_spread == false);
        Bytecode::Register reg { args_start_reg.index() + i };
        auto value = TRY(it->value->generate_bytecode(generator)).value();
        generator.emit<Bytecode::Op::Mov>(Bytecode::Operand(reg), value);
    }

    if (first_spread.index() != 0)
        generator.emit_with_extra_operand_slots<Bytecode::Op::NewArray>(2u, dst, AK::Array { Bytecode::Operand(args_start_reg), Bytecode::Operand { Bytecode::Register { args_start_reg.index() + static_cast<u32>(first_spread.index() - 1) } } });
    else
        generator.emit<Bytecode::Op::NewArray>(dst);

    if (first_spread != arguments.end()) {
        for (auto it = first_spread; it != arguments.end(); ++it) {
            auto value = TRY(it->value->generate_bytecode(generator)).value();
            generator.emit<Bytecode::Op::ArrayAppend>(dst, value, it->is_spread);
        }
    }

    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> SuperCall::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    Optional<Bytecode::Operand> arguments;
    if (m_is_synthetic == IsPartOfSyntheticConstructor::Yes) {
        // NOTE: This is the case where we have a fake constructor(...args) { super(...args); } which
        //       shouldn't call @@iterator of %Array.prototype%.
        VERIFY(m_arguments.size() == 1);
        VERIFY(m_arguments[0].is_spread);
        auto const& argument = m_arguments[0];
        // This generates a single argument.
        arguments = MUST(argument.value->generate_bytecode(generator));
    } else {
        arguments = TRY(arguments_to_array_for_call(generator, m_arguments)).value();
    }

    auto dst = choose_dst(generator, preferred_dst);
    generator.emit<Bytecode::Op::SuperCallWithArgumentArray>(dst, *arguments, m_is_synthetic == IsPartOfSyntheticConstructor::Yes);
    return dst;
}

static Bytecode::CodeGenerationErrorOr<void> generate_binding_pattern_bytecode(Bytecode::Generator& generator, BindingPattern const& pattern, Bytecode::Op::SetVariable::InitializationMode, Bytecode::Operand const& input_value, bool create_variables);

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> AssignmentExpression::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    if (m_op == AssignmentOp::Assignment) {
        // AssignmentExpression : LeftHandSideExpression = AssignmentExpression
        return m_lhs.visit(
            // 1. If LeftHandSideExpression is neither an ObjectLiteral nor an ArrayLiteral, then
            [&](NonnullRefPtr<Expression const> const& lhs) -> Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> {
                // a. Let lref be the result of evaluating LeftHandSideExpression.
                // b. ReturnIfAbrupt(lref).
                Optional<Bytecode::Operand> base;
                Optional<Bytecode::Operand> computed_property;
                Optional<Bytecode::Operand> this_value;

                bool lhs_is_super_expression = false;

                if (is<MemberExpression>(*lhs)) {
                    auto& expression = static_cast<MemberExpression const&>(*lhs);
                    lhs_is_super_expression = is<SuperExpression>(expression.object());

                    if (!lhs_is_super_expression) {
                        base = TRY(expression.object().generate_bytecode(generator)).value();
                    } else {
                        // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
                        // 1. Let env be GetThisEnvironment().
                        // 2. Let actualThis be ? env.GetThisBinding().
                        this_value = Bytecode::Operand(generator.allocate_register());
                        generator.emit<Bytecode::Op::ResolveThisBinding>(*this_value);

                        // SuperProperty : super [ Expression ]
                        // 3. Let propertyNameReference be ? Evaluation of Expression.
                        // 4. Let propertyNameValue be ? GetValue(propertyNameReference).
                    }

                    if (expression.is_computed()) {
                        auto property = TRY(expression.property().generate_bytecode(generator)).value();
                        computed_property = Bytecode::Operand(generator.allocate_register());
                        generator.emit<Bytecode::Op::Mov>(*computed_property, property);

                        // To be continued later with PutByValue.
                    } else if (expression.property().is_identifier()) {
                        // Do nothing, this will be handled by PutById later.
                    } else if (expression.property().is_private_identifier()) {
                        // Do nothing, this will be handled by PutPrivateById later.
                    } else {
                        return Bytecode::CodeGenerationError {
                            &expression,
                            "Unimplemented non-computed member expression"sv
                        };
                    }

                    if (lhs_is_super_expression) {
                        // 5/7. Return ? MakeSuperPropertyReference(actualThis, propertyKey, strict).

                        // https://tc39.es/ecma262/#sec-makesuperpropertyreference
                        // 1. Let env be GetThisEnvironment().
                        // 2. Assert: env.HasSuperBinding() is true.
                        // 3. Let baseValue be ? env.GetSuperBase().
                        // 4. Return the Reference Record { [[Base]]: baseValue, [[ReferencedName]]: propertyKey, [[Strict]]: strict, [[ThisValue]]: actualThis }.
                        base = Bytecode::Operand(generator.allocate_register());
                        generator.emit<Bytecode::Op::ResolveSuperBase>(*base);
                    }
                } else if (is<Identifier>(*lhs)) {
                    // NOTE: For Identifiers, we cannot perform GetVariable and then write into the reference it retrieves, only SetVariable can do this.
                    // FIXME: However, this breaks spec as we are doing variable lookup after evaluating the RHS. This is observable in an object environment, where we visibly perform HasOwnProperty and Get(@@unscopables) on the binded object.
                } else {
                    (void)TRY(lhs->generate_bytecode(generator));
                }

                // FIXME: c. If IsAnonymousFunctionDefinition(AssignmentExpression) and IsIdentifierRef of LeftHandSideExpression are both true, then
                //           i. Let rval be ? NamedEvaluation of AssignmentExpression with argument lref.[[ReferencedName]].

                // d. Else,
                // i. Let rref be the result of evaluating AssignmentExpression.
                // ii. Let rval be ? GetValue(rref).
                auto rval = TRY([&]() -> Bytecode::CodeGenerationErrorOr<Bytecode::Operand> {
                    if (lhs->is_identifier()) {
                        return TRY(generator.emit_named_evaluation_if_anonymous_function(*m_rhs, generator.intern_identifier(static_cast<Identifier const&>(*lhs).string()))).value();
                    } else {
                        return TRY(m_rhs->generate_bytecode(generator)).value();
                    }
                }());

                // e. Perform ? PutValue(lref, rval).
                if (is<Identifier>(*lhs)) {
                    auto& identifier = static_cast<Identifier const&>(*lhs);
                    generator.emit_set_variable(identifier, rval);
                } else if (is<MemberExpression>(*lhs)) {
                    auto& expression = static_cast<MemberExpression const&>(*lhs);
                    auto base_identifier = generator.intern_identifier_for_expression(expression.object());

                    if (expression.is_computed()) {
                        if (!lhs_is_super_expression)
                            generator.emit<Bytecode::Op::PutByValue>(*base, *computed_property, rval, Bytecode::Op::PropertyKind::KeyValue, move(base_identifier));
                        else
                            generator.emit<Bytecode::Op::PutByValueWithThis>(*base, *computed_property, *this_value, rval);
                    } else if (expression.property().is_identifier()) {
                        auto identifier_table_ref = generator.intern_identifier(verify_cast<Identifier>(expression.property()).string());
                        if (!lhs_is_super_expression)
                            generator.emit<Bytecode::Op::PutById>(*base, identifier_table_ref, rval, Bytecode::Op::PropertyKind::KeyValue, generator.next_property_lookup_cache(), move(base_identifier));
                        else
                            generator.emit<Bytecode::Op::PutByIdWithThis>(*base, *this_value, identifier_table_ref, rval, Bytecode::Op::PropertyKind::KeyValue, generator.next_property_lookup_cache());
                    } else if (expression.property().is_private_identifier()) {
                        auto identifier_table_ref = generator.intern_identifier(verify_cast<PrivateIdentifier>(expression.property()).string());
                        generator.emit<Bytecode::Op::PutPrivateById>(*base, identifier_table_ref, rval);
                    } else {
                        return Bytecode::CodeGenerationError {
                            &expression,
                            "Unimplemented non-computed member expression"sv
                        };
                    }
                } else {
                    return Bytecode::CodeGenerationError {
                        lhs,
                        "Unimplemented/invalid node used a reference"sv
                    };
                }

                // f. Return rval.
                return rval;
            },
            // 2. Let assignmentPattern be the AssignmentPattern that is covered by LeftHandSideExpression.
            [&](NonnullRefPtr<BindingPattern const> const& pattern) -> Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> {
                // 3. Let rref be the result of evaluating AssignmentExpression.
                // 4. Let rval be ? GetValue(rref).
                auto rval = TRY(m_rhs->generate_bytecode(generator)).value();

                // 5. Perform ? DestructuringAssignmentEvaluation of assignmentPattern with argument rval.
                TRY(generate_binding_pattern_bytecode(generator, pattern, Bytecode::Op::SetVariable::InitializationMode::Set, rval, false));

                // 6. Return rval.
                return rval;
            });
    }

    VERIFY(m_lhs.has<NonnullRefPtr<Expression const>>());
    auto& lhs_expression = m_lhs.get<NonnullRefPtr<Expression const>>();

    auto reference_operands = TRY(generator.emit_load_from_reference(lhs_expression));
    auto lhs = reference_operands.loaded_value.value();

    Bytecode::BasicBlock* rhs_block_ptr { nullptr };
    Bytecode::BasicBlock* lhs_block_ptr { nullptr };
    Bytecode::BasicBlock* end_block_ptr { nullptr };

    // Logical assignments short circuit.
    if (m_op == AssignmentOp::AndAssignment) { // &&=
        rhs_block_ptr = &generator.make_block();
        lhs_block_ptr = &generator.make_block();
        end_block_ptr = &generator.make_block();

        generator.emit<Bytecode::Op::JumpIf>(
            lhs,
            Bytecode::Label { *rhs_block_ptr },
            Bytecode::Label { *lhs_block_ptr });
    } else if (m_op == AssignmentOp::OrAssignment) { // ||=
        rhs_block_ptr = &generator.make_block();
        lhs_block_ptr = &generator.make_block();
        end_block_ptr = &generator.make_block();

        generator.emit<Bytecode::Op::JumpIf>(
            lhs,
            Bytecode::Label { *lhs_block_ptr },
            Bytecode::Label { *rhs_block_ptr });
    } else if (m_op == AssignmentOp::NullishAssignment) { // ??=
        rhs_block_ptr = &generator.make_block();
        lhs_block_ptr = &generator.make_block();
        end_block_ptr = &generator.make_block();

        generator.emit<Bytecode::Op::JumpNullish>(
            lhs,
            Bytecode::Label { *rhs_block_ptr },
            Bytecode::Label { *lhs_block_ptr });
    }

    if (rhs_block_ptr)
        generator.switch_to_basic_block(*rhs_block_ptr);

    auto rhs = TRY([&]() -> Bytecode::CodeGenerationErrorOr<Bytecode::Operand> {
        if (lhs_expression->is_identifier()) {
            return TRY(generator.emit_named_evaluation_if_anonymous_function(*m_rhs, generator.intern_identifier(static_cast<Identifier const&>(*lhs_expression).string()))).value();
        }
        return TRY(m_rhs->generate_bytecode(generator)).value();
    }());

    auto dst = choose_dst(generator, preferred_dst);

    switch (m_op) {
    case AssignmentOp::AdditionAssignment:
        generator.emit<Bytecode::Op::Add>(dst, lhs, rhs);
        break;
    case AssignmentOp::SubtractionAssignment:
        generator.emit<Bytecode::Op::Sub>(dst, lhs, rhs);
        break;
    case AssignmentOp::MultiplicationAssignment:
        generator.emit<Bytecode::Op::Mul>(dst, lhs, rhs);
        break;
    case AssignmentOp::DivisionAssignment:
        generator.emit<Bytecode::Op::Div>(dst, lhs, rhs);
        break;
    case AssignmentOp::ModuloAssignment:
        generator.emit<Bytecode::Op::Mod>(dst, lhs, rhs);
        break;
    case AssignmentOp::ExponentiationAssignment:
        generator.emit<Bytecode::Op::Exp>(dst, lhs, rhs);
        break;
    case AssignmentOp::BitwiseAndAssignment:
        generator.emit<Bytecode::Op::BitwiseAnd>(dst, lhs, rhs);
        break;
    case AssignmentOp::BitwiseOrAssignment:
        generator.emit<Bytecode::Op::BitwiseOr>(dst, lhs, rhs);
        break;
    case AssignmentOp::BitwiseXorAssignment:
        generator.emit<Bytecode::Op::BitwiseXor>(dst, lhs, rhs);
        break;
    case AssignmentOp::LeftShiftAssignment:
        generator.emit<Bytecode::Op::LeftShift>(dst, lhs, rhs);
        break;
    case AssignmentOp::RightShiftAssignment:
        generator.emit<Bytecode::Op::RightShift>(dst, lhs, rhs);
        break;
    case AssignmentOp::UnsignedRightShiftAssignment:
        generator.emit<Bytecode::Op::UnsignedRightShift>(dst, lhs, rhs);
        break;
    case AssignmentOp::AndAssignment:
    case AssignmentOp::OrAssignment:
    case AssignmentOp::NullishAssignment:
        generator.emit<Bytecode::Op::Mov>(dst, rhs);
        break;
    default:
        return Bytecode::CodeGenerationError {
            this,
            "Unimplemented operation"sv,
        };
    }

    if (lhs_expression->is_identifier())
        generator.emit_set_variable(static_cast<Identifier const&>(*lhs_expression), dst);
    else
        (void)TRY(generator.emit_store_to_reference(reference_operands, dst));

    if (rhs_block_ptr) {
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { *end_block_ptr });
    }

    if (lhs_block_ptr) {
        generator.switch_to_basic_block(*lhs_block_ptr);
        generator.emit<Bytecode::Op::Mov>(dst, lhs);
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { *end_block_ptr });
    }

    if (end_block_ptr) {
        generator.switch_to_basic_block(*end_block_ptr);
    }

    return dst;
}

// 14.13.3 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-labelled-statements-runtime-semantics-evaluation
//  LabelledStatement : LabelIdentifier : LabelledItem
Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> LabelledStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    // Return ? LabelledEvaluation of this LabelledStatement with argument « ».
    return generate_labelled_evaluation(generator, {});
}

// 14.13.4 Runtime Semantics: LabelledEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-labelledevaluation
// LabelledStatement : LabelIdentifier : LabelledItem
Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> LabelledStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<DeprecatedFlyString> const& label_set, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    // Convert the m_labelled_item NNRP to a reference early so we don't have to do it every single time we want to use it.
    auto const& labelled_item = *m_labelled_item;

    // 1. Let label be the StringValue of LabelIdentifier.
    // NOTE: Not necessary, this is m_label.

    // 2. Let newLabelSet be the list-concatenation of labelSet and « label ».
    // FIXME: Avoid copy here.
    auto new_label_set = label_set;
    new_label_set.append(m_label);

    // 3. Let stmtResult be LabelledEvaluation of LabelledItem with argument newLabelSet.
    Optional<Bytecode::Operand> stmt_result;
    if (is<IterationStatement>(labelled_item)) {
        auto const& iteration_statement = static_cast<IterationStatement const&>(labelled_item);
        stmt_result = TRY(iteration_statement.generate_labelled_evaluation(generator, new_label_set));
    } else if (is<SwitchStatement>(labelled_item)) {
        auto const& switch_statement = static_cast<SwitchStatement const&>(labelled_item);
        stmt_result = TRY(switch_statement.generate_labelled_evaluation(generator, new_label_set));
    } else if (is<LabelledStatement>(labelled_item)) {
        auto const& labelled_statement = static_cast<LabelledStatement const&>(labelled_item);
        stmt_result = TRY(labelled_statement.generate_labelled_evaluation(generator, new_label_set));
    } else {
        auto& labelled_break_block = generator.make_block();

        // NOTE: We do not need a continuable scope as `continue;` is not allowed outside of iteration statements, throwing a SyntaxError in the parser.
        generator.begin_breakable_scope(Bytecode::Label { labelled_break_block }, new_label_set);
        stmt_result = TRY(labelled_item.generate_bytecode(generator));
        generator.end_breakable_scope();

        if (!generator.is_current_block_terminated()) {
            generator.emit<Bytecode::Op::Jump>(Bytecode::Label { labelled_break_block });
        }

        generator.switch_to_basic_block(labelled_break_block);
    }

    // 4. If stmtResult.[[Type]] is break and SameValue(stmtResult.[[Target]], label) is true, then
    //    a. Set stmtResult to NormalCompletion(stmtResult.[[Value]]).
    // NOTE: These steps are performed by making labelled break jump straight to the appropriate break block, which preserves the statement result's value in the accumulator.

    // 5. Return Completion(stmtResult).
    return stmt_result;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> IterationStatement::generate_labelled_evaluation(Bytecode::Generator&, Vector<DeprecatedFlyString> const&, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    return Bytecode::CodeGenerationError {
        this,
        "Missing generate_labelled_evaluation()"sv,
    };
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> WhileStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generate_labelled_evaluation(generator, {});
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> WhileStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<DeprecatedFlyString> const& label_set, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    // test
    // jump if_false (true) end (false) body
    // body
    // jump always (true) test
    // end
    auto& test_block = generator.make_block();
    auto& body_block = generator.make_block();
    auto& end_block = generator.make_block();

    auto result = Bytecode::Operand(generator.allocate_register());
    generator.emit<Bytecode::Op::Mov>(result, generator.add_constant(js_undefined()));

    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { test_block });

    generator.switch_to_basic_block(test_block);
    auto test = TRY(m_test->generate_bytecode(generator)).value();
    generator.emit<Bytecode::Op::JumpIf>(
        test,
        Bytecode::Label { body_block },
        Bytecode::Label { end_block });

    generator.switch_to_basic_block(body_block);
    generator.begin_continuable_scope(Bytecode::Label { test_block }, label_set);
    generator.begin_breakable_scope(Bytecode::Label { end_block }, label_set);
    auto body = TRY(m_body->generate_bytecode(generator));
    generator.end_breakable_scope();
    generator.end_continuable_scope();

    if (!generator.is_current_block_terminated()) {
        if (body.has_value())
            generator.emit<Bytecode::Op::Mov>(result, body.value());
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { test_block });
    }

    generator.switch_to_basic_block(end_block);
    return result;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> DoWhileStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generate_labelled_evaluation(generator, {});
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> DoWhileStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<DeprecatedFlyString> const& label_set, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    // jump always (true) body
    // test
    // jump if_false (true) end (false) body
    // body
    // jump always (true) test
    // end
    auto& test_block = generator.make_block();
    auto& body_block = generator.make_block();
    auto& load_result_and_jump_to_end_block = generator.make_block();
    auto& end_block = generator.make_block();

    auto completion_value = Bytecode::Operand(generator.allocate_register());
    generator.emit<Bytecode::Op::Mov>(completion_value, generator.add_constant(js_undefined()));

    // jump to the body block
    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { body_block });

    generator.switch_to_basic_block(test_block);
    auto test = TRY(m_test->generate_bytecode(generator)).value();
    generator.emit<Bytecode::Op::JumpIf>(
        test,
        Bytecode::Label { body_block },
        Bytecode::Label { load_result_and_jump_to_end_block });

    generator.switch_to_basic_block(body_block);
    generator.begin_continuable_scope(Bytecode::Label { test_block }, label_set);
    generator.begin_breakable_scope(Bytecode::Label { end_block }, label_set);
    auto body_result = TRY(m_body->generate_bytecode(generator));
    generator.end_breakable_scope();
    generator.end_continuable_scope();

    if (!generator.is_current_block_terminated()) {
        if (body_result.has_value())
            generator.emit<Bytecode::Op::Mov>(completion_value, body_result.value());
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { test_block });
    }

    generator.switch_to_basic_block(load_result_and_jump_to_end_block);
    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { end_block });

    generator.switch_to_basic_block(end_block);
    return completion_value;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ForStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generate_labelled_evaluation(generator, {});
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ForStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<DeprecatedFlyString> const& label_set, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
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
    Bytecode::BasicBlock* load_result_and_jump_to_end_block_ptr { nullptr };

    auto& end_block = generator.make_block();

    bool has_lexical_environment = false;

    if (m_init) {
        if (m_init->is_variable_declaration()) {
            auto& variable_declaration = verify_cast<VariableDeclaration>(*m_init);

            auto has_non_local_variables = false;
            MUST(variable_declaration.for_each_bound_identifier([&](auto const& identifier) {
                if (!identifier.is_local())
                    has_non_local_variables = true;
            }));

            if (variable_declaration.is_lexical_declaration() && has_non_local_variables) {
                has_lexical_environment = true;

                // FIXME: Is Block correct?
                generator.begin_variable_scope();

                bool is_const = variable_declaration.is_constant_declaration();
                // NOTE: Nothing in the callback throws an exception.
                MUST(variable_declaration.for_each_bound_identifier([&](auto const& identifier) {
                    if (identifier.is_local())
                        return;
                    auto index = generator.intern_identifier(identifier.string());
                    generator.emit<Bytecode::Op::CreateVariable>(index, Bytecode::Op::EnvironmentMode::Lexical, is_const);
                }));
            }
        }

        (void)TRY(m_init->generate_bytecode(generator));
    }

    body_block_ptr = &generator.make_block();

    if (m_test)
        test_block_ptr = &generator.make_block();
    else
        test_block_ptr = body_block_ptr;

    if (m_update)
        update_block_ptr = &generator.make_block();
    else
        update_block_ptr = body_block_ptr;

    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { *test_block_ptr });

    if (m_test) {
        load_result_and_jump_to_end_block_ptr = &generator.make_block();
        generator.switch_to_basic_block(*test_block_ptr);

        auto test = TRY(m_test->generate_bytecode(generator)).value();
        generator.emit<Bytecode::Op::JumpIf>(
            test,
            Bytecode::Label { *body_block_ptr },
            Bytecode::Label { *load_result_and_jump_to_end_block_ptr });
    }

    if (m_update) {
        generator.switch_to_basic_block(*update_block_ptr);

        (void)TRY(m_update->generate_bytecode(generator));
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { *test_block_ptr });
    }

    generator.switch_to_basic_block(*body_block_ptr);
    generator.begin_continuable_scope(Bytecode::Label { m_update ? *update_block_ptr : *test_block_ptr }, label_set);
    generator.begin_breakable_scope(Bytecode::Label { end_block }, label_set);
    auto body_result = TRY(m_body->generate_bytecode(generator));
    generator.end_breakable_scope();
    generator.end_continuable_scope();

    if (!generator.is_current_block_terminated()) {
        if (m_update) {
            generator.emit<Bytecode::Op::Jump>(Bytecode::Label { *update_block_ptr });
        } else {
            generator.emit<Bytecode::Op::Jump>(Bytecode::Label { *test_block_ptr });
        }
    }

    if (load_result_and_jump_to_end_block_ptr) {
        generator.switch_to_basic_block(*load_result_and_jump_to_end_block_ptr);
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { end_block });
    }

    generator.switch_to_basic_block(end_block);

    if (has_lexical_environment)
        generator.end_variable_scope();

    return body_result;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ObjectExpression::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);

    auto object = Bytecode::Operand(generator.allocate_register());

    generator.emit<Bytecode::Op::NewObject>(object);
    if (m_properties.is_empty())
        return object;

    generator.push_home_object(object);

    for (auto& property : m_properties) {
        Bytecode::Op::PropertyKind property_kind;
        switch (property->type()) {
        case ObjectProperty::Type::KeyValue:
            property_kind = Bytecode::Op::PropertyKind::DirectKeyValue;
            break;
        case ObjectProperty::Type::Getter:
            property_kind = Bytecode::Op::PropertyKind::Getter;
            break;
        case ObjectProperty::Type::Setter:
            property_kind = Bytecode::Op::PropertyKind::Setter;
            break;
        case ObjectProperty::Type::Spread:
            property_kind = Bytecode::Op::PropertyKind::Spread;
            break;
        case ObjectProperty::Type::ProtoSetter:
            property_kind = Bytecode::Op::PropertyKind::ProtoSetter;
            break;
        }

        if (is<StringLiteral>(property->key())) {
            auto& string_literal = static_cast<StringLiteral const&>(property->key());
            Bytecode::IdentifierTableIndex key_name = generator.intern_identifier(string_literal.value());

            Optional<Bytecode::Operand> value;
            if (property_kind == Bytecode::Op::PropertyKind::ProtoSetter) {
                value = TRY(property->value().generate_bytecode(generator)).value();
            } else if (property_kind != Bytecode::Op::PropertyKind::Spread) {
                ByteString identifier = string_literal.value();
                if (property_kind == Bytecode::Op::PropertyKind::Getter)
                    identifier = ByteString::formatted("get {}", identifier);
                else if (property_kind == Bytecode::Op::PropertyKind::Setter)
                    identifier = ByteString::formatted("set {}", identifier);
                auto name = generator.intern_identifier(identifier);
                value = TRY(generator.emit_named_evaluation_if_anonymous_function(property->value(), name)).value();
            } else {
                // Spread the key.
                value = TRY(property->key().generate_bytecode(generator)).value();
            }

            generator.emit<Bytecode::Op::PutById>(object, key_name, *value, property_kind, generator.next_property_lookup_cache());
        } else {
            auto property_name = TRY(property->key().generate_bytecode(generator)).value();
            Optional<Bytecode::Operand> value;
            if (property_kind != Bytecode::Op::PropertyKind::Spread)
                value = TRY(property->value().generate_bytecode(generator)).value();
            else
                value = property_name;

            generator.emit<Bytecode::Op::PutByValue>(object, property_name, *value, property_kind);
        }
    }

    generator.pop_home_object();
    return object;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ArrayExpression::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    if (m_elements.is_empty()) {
        auto dst = choose_dst(generator, preferred_dst);
        generator.emit<Bytecode::Op::NewArray>(dst);
        return dst;
    }

    if (all_of(m_elements, [](auto element) { return !element || is<PrimitiveLiteral>(*element); })) {
        // If all elements are constant primitives, we can just emit a single instruction to initialize the array,
        // instead of emitting instructions to manually evaluate them one-by-one
        Vector<Value> values;
        values.resize(m_elements.size());
        for (auto i = 0u; i < m_elements.size(); ++i) {
            if (!m_elements[i])
                continue;
            values[i] = static_cast<PrimitiveLiteral const&>(*m_elements[i]).value();
        }
        auto dst = choose_dst(generator, preferred_dst);
        generator.emit_with_extra_value_slots<Bytecode::Op::NewPrimitiveArray>(values.size(), dst, values);
        return dst;
    }

    auto first_spread = find_if(m_elements.begin(), m_elements.end(), [](auto el) { return el && is<SpreadExpression>(*el); });

    Bytecode::Register args_start_reg { 0 };
    for (auto it = m_elements.begin(); it != first_spread; ++it) {
        auto reg = generator.allocate_register();
        if (args_start_reg.index() == 0)
            args_start_reg = reg;
    }
    u32 i = 0;
    for (auto it = m_elements.begin(); it != first_spread; ++it, ++i) {
        Bytecode::Register reg { args_start_reg.index() + i };
        if (*it) {
            auto value = TRY((*it)->generate_bytecode(generator)).value();
            generator.emit<Bytecode::Op::Mov>(Bytecode::Operand(reg), value);
        }
    }

    auto dst = choose_dst(generator, preferred_dst);
    if (first_spread.index() != 0) {
        auto reg = Bytecode::Register { args_start_reg.index() + static_cast<u32>(first_spread.index() - 1) };
        generator.emit_with_extra_operand_slots<Bytecode::Op::NewArray>(2u, dst, AK::Array { Bytecode::Operand(args_start_reg), Bytecode::Operand(reg) });
    } else {
        generator.emit<Bytecode::Op::NewArray>(dst);
    }

    if (first_spread != m_elements.end()) {
        for (auto it = first_spread; it != m_elements.end(); ++it) {
            if (!*it) {
                generator.emit<Bytecode::Op::ArrayAppend>(dst, generator.add_constant(Value()), false);
            } else {
                auto value = TRY((*it)->generate_bytecode(generator)).value();
                generator.emit<Bytecode::Op::ArrayAppend>(dst, value, *it && is<SpreadExpression>(**it));
            }
        }
    }

    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> MemberExpression::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto reference = TRY(generator.emit_load_from_reference(*this, preferred_dst));
    return reference.loaded_value;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> FunctionDeclaration::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    if (m_is_hoisted) {
        Bytecode::Generator::SourceLocationScope scope(generator, *this);
        auto index = generator.intern_identifier(name());
        auto value = Bytecode::Operand(generator.allocate_register());
        generator.emit<Bytecode::Op::GetVariable>(value, index, generator.next_environment_variable_cache());
        generator.emit<Bytecode::Op::SetVariable>(index, value, generator.next_environment_variable_cache(), Bytecode::Op::SetVariable::InitializationMode::Set, Bytecode::Op::EnvironmentMode::Var);
    }
    return Optional<Bytecode::Operand> {};
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> FunctionExpression::generate_bytecode_with_lhs_name(Bytecode::Generator& generator, Optional<Bytecode::IdentifierTableIndex> lhs_name, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    bool has_name = !name().is_empty();
    Optional<Bytecode::IdentifierTableIndex> name_identifier;

    if (has_name) {
        generator.begin_variable_scope();

        name_identifier = generator.intern_identifier(name());
        generator.emit<Bytecode::Op::CreateVariable>(*name_identifier, Bytecode::Op::EnvironmentMode::Lexical, true);
    }

    auto new_function = choose_dst(generator, preferred_dst);
    generator.emit_new_function(new_function, *this, lhs_name);

    if (has_name) {
        generator.emit<Bytecode::Op::SetVariable>(*name_identifier, new_function, generator.next_environment_variable_cache(), Bytecode::Op::SetVariable::InitializationMode::Initialize, Bytecode::Op::EnvironmentMode::Lexical);
        generator.end_variable_scope();
    }

    return new_function;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> FunctionExpression::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generate_bytecode_with_lhs_name(generator, {}, preferred_dst);
}

static Bytecode::CodeGenerationErrorOr<void> generate_object_binding_pattern_bytecode(Bytecode::Generator& generator, BindingPattern const& pattern, Bytecode::Op::SetVariable::InitializationMode initialization_mode, Bytecode::Operand const& object, bool create_variables)
{
    generator.emit<Bytecode::Op::ThrowIfNullish>(object);

    Vector<Bytecode::Operand> excluded_property_names;
    auto has_rest = false;
    if (pattern.entries.size() > 0)
        has_rest = pattern.entries[pattern.entries.size() - 1].is_rest;

    for (auto& [name, alias, initializer, is_rest] : pattern.entries) {
        if (is_rest) {
            VERIFY(!initializer);
            if (name.has<NonnullRefPtr<Identifier const>>()) {
                auto identifier = name.get<NonnullRefPtr<Identifier const>>();
                auto interned_identifier = generator.intern_identifier(identifier->string());

                auto copy = Bytecode::Operand(generator.allocate_register());
                generator.emit_with_extra_operand_slots<Bytecode::Op::CopyObjectExcludingProperties>(
                    excluded_property_names.size(), copy, object, excluded_property_names);
                if (create_variables) {
                    VERIFY(!identifier->is_local());
                    generator.emit<Bytecode::Op::CreateVariable>(interned_identifier, Bytecode::Op::EnvironmentMode::Lexical, false);
                }
                generator.emit_set_variable(*identifier, copy, initialization_mode);

                return {};
            }
            if (alias.has<NonnullRefPtr<MemberExpression const>>()) {
                auto copy = Bytecode::Operand(generator.allocate_register());
                generator.emit_with_extra_operand_slots<Bytecode::Op::CopyObjectExcludingProperties>(
                    excluded_property_names.size(), copy, object, excluded_property_names);
                (void)TRY(generator.emit_store_to_reference(alias.get<NonnullRefPtr<MemberExpression const>>(), object));
                return {};
            }
            VERIFY_NOT_REACHED();
        }

        auto value = Bytecode::Operand(generator.allocate_register());

        if (name.has<NonnullRefPtr<Identifier const>>()) {
            auto const& identifier = name.get<NonnullRefPtr<Identifier const>>()->string();
            if (has_rest) {
                excluded_property_names.append(generator.add_constant(PrimitiveString::create(generator.vm(), identifier), Bytecode::Generator::DeduplicateConstant::No));
            }
            generator.emit_get_by_id(value, object, generator.intern_identifier(identifier));
        } else {
            auto expression = name.get<NonnullRefPtr<Expression const>>();
            auto property_name = TRY(expression->generate_bytecode(generator)).value();

            if (has_rest) {
                auto excluded_name = Bytecode::Operand(generator.allocate_register());
                excluded_property_names.append(excluded_name);
                generator.emit<Bytecode::Op::Mov>(excluded_name, property_name);
            }

            generator.emit<Bytecode::Op::GetByValue>(value, object, property_name);
        }

        if (initializer) {
            auto& if_undefined_block = generator.make_block();
            auto& if_not_undefined_block = generator.make_block();

            generator.emit<Bytecode::Op::JumpUndefined>(
                value,
                Bytecode::Label { if_undefined_block },
                Bytecode::Label { if_not_undefined_block });

            generator.switch_to_basic_block(if_undefined_block);
            Optional<Bytecode::Operand> default_value;
            if (auto const* alias_identifier = alias.get_pointer<NonnullRefPtr<Identifier const>>()) {
                default_value = TRY(generator.emit_named_evaluation_if_anonymous_function(*initializer, generator.intern_identifier((*alias_identifier)->string()))).value();
            } else if (auto const* lhs = name.get_pointer<NonnullRefPtr<Identifier const>>()) {
                default_value = TRY(generator.emit_named_evaluation_if_anonymous_function(*initializer, generator.intern_identifier((*lhs)->string()))).value();
            } else {
                default_value = TRY(initializer->generate_bytecode(generator)).value();
            }
            generator.emit<Bytecode::Op::Mov>(value, *default_value);
            generator.emit<Bytecode::Op::Jump>(Bytecode::Label { if_not_undefined_block });

            generator.switch_to_basic_block(if_not_undefined_block);
        }

        if (alias.has<NonnullRefPtr<BindingPattern const>>()) {
            auto& binding_pattern = *alias.get<NonnullRefPtr<BindingPattern const>>();
            auto nested_value = Bytecode::Operand(generator.allocate_register());
            generator.emit<Bytecode::Op::Mov>(nested_value, value);
            TRY(generate_binding_pattern_bytecode(generator, binding_pattern, initialization_mode, nested_value, create_variables));
        } else if (alias.has<Empty>()) {
            if (name.has<NonnullRefPtr<Expression const>>()) {
                // This needs some sort of SetVariableByValue opcode, as it's a runtime binding
                return Bytecode::CodeGenerationError {
                    name.get<NonnullRefPtr<Expression const>>().ptr(),
                    "Unimplemented name/alias pair: Empty/Expression"sv,
                };
            }

            auto const& identifier = *name.get<NonnullRefPtr<Identifier const>>();
            auto identifier_ref = generator.intern_identifier(identifier.string());
            if (create_variables)
                generator.emit<Bytecode::Op::CreateVariable>(identifier_ref, Bytecode::Op::EnvironmentMode::Lexical, false);
            generator.emit_set_variable(identifier, value, initialization_mode);
        } else if (alias.has<NonnullRefPtr<MemberExpression const>>()) {
            TRY(generator.emit_store_to_reference(alias.get<NonnullRefPtr<MemberExpression const>>(), value));
        } else {
            auto const& identifier = *alias.get<NonnullRefPtr<Identifier const>>();
            auto identifier_ref = generator.intern_identifier(identifier.string());
            if (create_variables)
                generator.emit<Bytecode::Op::CreateVariable>(identifier_ref, Bytecode::Op::EnvironmentMode::Lexical, false);
            generator.emit_set_variable(identifier, value, initialization_mode);
        }
    }
    return {};
}

static Bytecode::CodeGenerationErrorOr<void> generate_array_binding_pattern_bytecode(Bytecode::Generator& generator, BindingPattern const& pattern, Bytecode::Op::SetVariable::InitializationMode initialization_mode, Bytecode::Operand const& input_array, bool create_variables, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst = {})
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
     *   If it is 'true', we load undefined. Otherwise, we grab the next value from the iterator.
     *
     * Note that the is_exhausted register does not need to be loaded with false because the
     * first IteratorNext bytecode is _not_ proceeded by an exhausted check, as it is
     * unnecessary.
     */

    auto is_iterator_exhausted = Bytecode::Operand(generator.allocate_register());
    generator.emit<Bytecode::Op::Mov>(is_iterator_exhausted, generator.add_constant(Value(false)));

    auto iterator = Bytecode::Operand(generator.allocate_register());
    generator.emit<Bytecode::Op::GetIterator>(iterator, input_array);
    bool first = true;

    auto assign_value_to_alias = [&](auto& alias, Bytecode::Operand value) {
        return alias.visit(
            [&](Empty) -> Bytecode::CodeGenerationErrorOr<void> {
                // This element is an elision
                return {};
            },
            [&](NonnullRefPtr<Identifier const> const& identifier) -> Bytecode::CodeGenerationErrorOr<void> {
                auto interned_index = generator.intern_identifier(identifier->string());
                if (create_variables)
                    generator.emit<Bytecode::Op::CreateVariable>(interned_index, Bytecode::Op::EnvironmentMode::Lexical, false);
                generator.emit_set_variable(*identifier, value, initialization_mode);
                return {};
            },
            [&](NonnullRefPtr<BindingPattern const> const& pattern) -> Bytecode::CodeGenerationErrorOr<void> {
                return generate_binding_pattern_bytecode(generator, pattern, initialization_mode, value, create_variables);
            },
            [&](NonnullRefPtr<MemberExpression const> const& expr) -> Bytecode::CodeGenerationErrorOr<void> {
                (void)generator.emit_store_to_reference(*expr, value);
                return {};
            });
    };

    auto temp_iterator_result = Bytecode::Operand(generator.allocate_register());

    for (auto& [name, alias, initializer, is_rest] : pattern.entries) {
        VERIFY(name.has<Empty>());

        if (is_rest) {
            VERIFY(!initializer);

            auto value = Bytecode::Operand(generator.allocate_register());

            if (first) {
                // The iterator has not been called, and is thus known to be not exhausted
                generator.emit<Bytecode::Op::IteratorToArray>(value, iterator);
            } else {
                auto& if_exhausted_block = generator.make_block();
                auto& if_not_exhausted_block = generator.make_block();
                auto& continuation_block = generator.make_block();

                generator.emit<Bytecode::Op::JumpIf>(
                    is_iterator_exhausted,
                    Bytecode::Label { if_exhausted_block },
                    Bytecode::Label { if_not_exhausted_block });

                value = Bytecode::Operand(generator.allocate_register());

                generator.switch_to_basic_block(if_exhausted_block);
                generator.emit<Bytecode::Op::NewArray>(value);
                generator.emit<Bytecode::Op::Jump>(Bytecode::Label { continuation_block });

                generator.switch_to_basic_block(if_not_exhausted_block);
                generator.emit<Bytecode::Op::IteratorToArray>(value, iterator);
                generator.emit<Bytecode::Op::Jump>(Bytecode::Label { continuation_block });

                generator.switch_to_basic_block(continuation_block);
            }

            return assign_value_to_alias(alias, value);
        }

        auto& iterator_is_exhausted_block = generator.make_block();

        if (!first) {
            auto& iterator_is_not_exhausted_block = generator.make_block();

            generator.emit<Bytecode::Op::JumpIf>(
                is_iterator_exhausted,
                Bytecode::Label { iterator_is_exhausted_block },
                Bytecode::Label { iterator_is_not_exhausted_block });

            generator.switch_to_basic_block(iterator_is_not_exhausted_block);
        }

        generator.emit<Bytecode::Op::IteratorNext>(temp_iterator_result, iterator);
        generator.emit_iterator_complete(is_iterator_exhausted, temp_iterator_result);

        // We still have to check for exhaustion here. If the iterator is exhausted,
        // we need to bail before trying to get the value
        auto& no_bail_block = generator.make_block();
        generator.emit<Bytecode::Op::JumpIf>(
            is_iterator_exhausted,
            Bytecode::Label { iterator_is_exhausted_block },
            Bytecode::Label { no_bail_block });

        generator.switch_to_basic_block(no_bail_block);

        // Get the next value in the iterator
        auto value = Bytecode::Operand { generator.allocate_register() };
        generator.emit_iterator_value(value, temp_iterator_result);

        auto& create_binding_block = generator.make_block();
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { create_binding_block });

        // The iterator is exhausted, so we just load undefined and continue binding
        generator.switch_to_basic_block(iterator_is_exhausted_block);
        generator.emit<Bytecode::Op::Mov>(value, generator.add_constant(js_undefined()));
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { create_binding_block });

        generator.switch_to_basic_block(create_binding_block);

        if (initializer) {
            auto& value_is_undefined_block = generator.make_block();
            auto& value_is_not_undefined_block = generator.make_block();

            generator.emit<Bytecode::Op::JumpUndefined>(
                value,
                Bytecode::Label { value_is_undefined_block },
                Bytecode::Label { value_is_not_undefined_block });

            generator.switch_to_basic_block(value_is_undefined_block);

            Optional<Bytecode::Operand> default_value;
            if (auto const* alias_identifier = alias.get_pointer<NonnullRefPtr<Identifier const>>()) {
                default_value = TRY(generator.emit_named_evaluation_if_anonymous_function(*initializer, generator.intern_identifier((*alias_identifier)->string()))).value();
            } else if (auto const* name_identifier = name.get_pointer<NonnullRefPtr<Identifier const>>()) {
                default_value = TRY(generator.emit_named_evaluation_if_anonymous_function(*initializer, generator.intern_identifier((*name_identifier)->string()))).value();
            } else {
                default_value = TRY(initializer->generate_bytecode(generator)).value();
            }
            generator.emit<Bytecode::Op::Mov>(value, *default_value);
            generator.emit<Bytecode::Op::Jump>(Bytecode::Label { value_is_not_undefined_block });

            generator.switch_to_basic_block(value_is_not_undefined_block);
        }

        TRY(assign_value_to_alias(alias, value));

        first = false;
    }

    auto& done_block = generator.make_block();
    auto& not_done_block = generator.make_block();

    generator.emit<Bytecode::Op::JumpIf>(
        is_iterator_exhausted,
        Bytecode::Label { done_block },
        Bytecode::Label { not_done_block });

    generator.switch_to_basic_block(not_done_block);
    generator.emit<Bytecode::Op::IteratorClose>(iterator, Completion::Type::Normal, Optional<Value> {});
    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { done_block });

    generator.switch_to_basic_block(done_block);
    return {};
}

static Bytecode::CodeGenerationErrorOr<void> generate_binding_pattern_bytecode(Bytecode::Generator& generator, BindingPattern const& pattern, Bytecode::Op::SetVariable::InitializationMode initialization_mode, Bytecode::Operand const& input_value, bool create_variables)
{
    if (pattern.kind == BindingPattern::Kind::Object)
        return generate_object_binding_pattern_bytecode(generator, pattern, initialization_mode, input_value, create_variables);

    return generate_array_binding_pattern_bytecode(generator, pattern, initialization_mode, input_value, create_variables);
}

static Bytecode::CodeGenerationErrorOr<void> assign_value_to_variable_declarator(Bytecode::Generator& generator, VariableDeclarator const& declarator, VariableDeclaration const& declaration, Bytecode::Operand value)
{
    auto initialization_mode = declaration.is_lexical_declaration() ? Bytecode::Op::SetVariable::InitializationMode::Initialize : Bytecode::Op::SetVariable::InitializationMode::Set;

    return declarator.target().visit(
        [&](NonnullRefPtr<Identifier const> const& id) -> Bytecode::CodeGenerationErrorOr<void> {
            generator.emit_set_variable(*id, value, initialization_mode);
            return {};
        },
        [&](NonnullRefPtr<BindingPattern const> const& pattern) -> Bytecode::CodeGenerationErrorOr<void> {
            return generate_binding_pattern_bytecode(generator, pattern, initialization_mode, value, false);
        });
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> VariableDeclaration::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);

    for (auto& declarator : m_declarations) {
        // NOTE: `var` declarations can have duplicates, but duplicate `let` or `const` bindings are a syntax error.
        //       Because of this, we can sink `let` and `const` directly into the preferred_dst if available.
        //       This is not safe for `var` since the preferred_dst may be used in the initializer.
        Optional<Bytecode::Operand> init_dst;
        if (declaration_kind() != DeclarationKind::Var) {
            if (auto const* identifier = declarator->target().get_pointer<NonnullRefPtr<Identifier const>>()) {
                if ((*identifier)->is_local()) {
                    init_dst = Bytecode::Operand(Bytecode::Operand::Type::Local, (*identifier)->local_variable_index());
                }
            }
        }

        if (declarator->init()) {
            auto value = TRY([&]() -> Bytecode::CodeGenerationErrorOr<Bytecode::Operand> {
                if (auto const* lhs = declarator->target().get_pointer<NonnullRefPtr<Identifier const>>()) {
                    return TRY(generator.emit_named_evaluation_if_anonymous_function(*declarator->init(), generator.intern_identifier((*lhs)->string()), init_dst)).value();
                } else {
                    return TRY(declarator->init()->generate_bytecode(generator, init_dst)).value();
                }
            }());
            (void)TRY(assign_value_to_variable_declarator(generator, declarator, *this, value));
        } else if (m_declaration_kind != DeclarationKind::Var) {
            (void)TRY(assign_value_to_variable_declarator(generator, declarator, *this, generator.add_constant(js_undefined())));
        }
    }

    for (auto& declarator : m_declarations) {
        if (auto const* identifier = declarator->target().get_pointer<NonnullRefPtr<Identifier const>>()) {
            if ((*identifier)->is_local()) {
                generator.set_local_initialized((*identifier)->local_variable_index());
            }
        }
    }

    // NOTE: VariableDeclaration doesn't return a completion value.
    return Optional<Bytecode::Operand> {};
}

struct BaseAndValue {
    Bytecode::Operand base;
    Bytecode::Operand value;
};

static Bytecode::CodeGenerationErrorOr<BaseAndValue> get_base_and_value_from_member_expression(Bytecode::Generator& generator, MemberExpression const& member_expression)
{
    // https://tc39.es/ecma262/#sec-super-keyword-runtime-semantics-evaluation
    if (is<SuperExpression>(member_expression.object())) {
        // 1. Let env be GetThisEnvironment().
        // 2. Let actualThis be ? env.GetThisBinding().
        auto this_value = Bytecode::Operand(generator.allocate_register());
        generator.emit<Bytecode::Op::ResolveThisBinding>(this_value);

        Optional<Bytecode::Operand> computed_property;

        if (member_expression.is_computed()) {
            // SuperProperty : super [ Expression ]
            // 3. Let propertyNameReference be ? Evaluation of Expression.
            // 4. Let propertyNameValue be ? GetValue(propertyNameReference).
            computed_property = TRY(member_expression.property().generate_bytecode(generator));
        }

        // 5/7. Return ? MakeSuperPropertyReference(actualThis, propertyKey, strict).

        // https://tc39.es/ecma262/#sec-makesuperpropertyreference
        // 1. Let env be GetThisEnvironment().
        // 2. Assert: env.HasSuperBinding() is true.
        // 3. Let baseValue be ? env.GetSuperBase().
        auto super_base = Bytecode::Operand(generator.allocate_register());
        generator.emit<Bytecode::Op::ResolveSuperBase>(super_base);

        auto value = Bytecode::Operand { generator.allocate_register() };

        // 4. Return the Reference Record { [[Base]]: baseValue, [[ReferencedName]]: propertyKey, [[Strict]]: strict, [[ThisValue]]: actualThis }.
        if (computed_property.has_value()) {
            // 5. Let propertyKey be ? ToPropertyKey(propertyNameValue).
            // FIXME: This does ToPropertyKey out of order, which is observable by Symbol.toPrimitive!
            generator.emit<Bytecode::Op::GetByValueWithThis>(value, super_base, *computed_property, this_value);
        } else {
            // 3. Let propertyKey be StringValue of IdentifierName.
            auto identifier_table_ref = generator.intern_identifier(verify_cast<Identifier>(member_expression.property()).string());
            generator.emit_get_by_id_with_this(value, super_base, identifier_table_ref, this_value);
        }

        return BaseAndValue { this_value, value };
    }

    auto base = TRY(member_expression.object().generate_bytecode(generator)).value();
    auto value = Bytecode::Operand { generator.allocate_register() };
    if (member_expression.is_computed()) {
        auto property = TRY(member_expression.property().generate_bytecode(generator)).value();
        generator.emit<Bytecode::Op::GetByValue>(value, base, property);
    } else if (is<PrivateIdentifier>(member_expression.property())) {
        generator.emit<Bytecode::Op::GetPrivateById>(
            value,
            base,
            generator.intern_identifier(verify_cast<PrivateIdentifier>(member_expression.property()).string()));
    } else {
        auto base_identifier = generator.intern_identifier_for_expression(member_expression.object());
        generator.emit_get_by_id(value, base, generator.intern_identifier(verify_cast<Identifier>(member_expression.property()).string()), move(base_identifier));
    }

    return BaseAndValue { base, value };
}

static Bytecode::CodeGenerationErrorOr<void> generate_optional_chain(Bytecode::Generator& generator, OptionalChain const& optional_chain, Bytecode::Operand current_value, Bytecode::Operand current_base, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst = {});

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> CallExpression::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);

    Optional<Bytecode::Builtin> builtin;

    Optional<Bytecode::Operand> original_callee;
    auto this_value = generator.add_constant(js_undefined());

    if (is<NewExpression>(this)) {
        original_callee = TRY(m_callee->generate_bytecode(generator)).value();
    } else if (is<MemberExpression>(*m_callee)) {
        auto& member_expression = static_cast<MemberExpression const&>(*m_callee);
        auto base_and_value = TRY(get_base_and_value_from_member_expression(generator, member_expression));
        original_callee = base_and_value.value;
        this_value = base_and_value.base;
        builtin = Bytecode::get_builtin(member_expression);
    } else if (is<OptionalChain>(*m_callee)) {
        auto& optional_chain = static_cast<OptionalChain const&>(*m_callee);
        original_callee = Bytecode::Operand(generator.allocate_register());
        this_value = Bytecode::Operand(generator.allocate_register());
        TRY(generate_optional_chain(generator, optional_chain, *original_callee, this_value));
    } else if (is<Identifier>(*m_callee)) {
        // If the original_callee is an identifier, we may need to extract a `this` value.
        // This is important when we're inside a `with` statement and calling a method on
        // the environment's binding object.
        // NOTE: If the identifier refers to a known "local" or "global", we know it can't be
        //       a `with` binding, so we can skip this.
        auto& identifier = static_cast<Identifier const&>(*m_callee);
        if (identifier.is_local()) {
            auto local = Bytecode::Operand(Bytecode::Operand::Type::Local, identifier.local_variable_index());
            if (!generator.is_local_initialized(local.index())) {
                generator.emit<Bytecode::Op::ThrowIfTDZ>(local);
            }
            original_callee = local;
        } else if (identifier.is_global()) {
            original_callee = m_callee->generate_bytecode(generator).value();
        } else {
            original_callee = Bytecode::Operand(generator.allocate_register());
            this_value = Bytecode::Operand(generator.allocate_register());
            generator.emit<Bytecode::Op::GetCalleeAndThisFromEnvironment>(
                *original_callee,
                this_value,
                generator.intern_identifier(identifier.string()),
                generator.next_environment_variable_cache());
        }
    } else {
        // FIXME: this = global object in sloppy mode.
        original_callee = TRY(m_callee->generate_bytecode(generator)).value();
    }

    // NOTE: We copy the callee to a new register to avoid overwriting it while evaluating arguments.
    auto callee = Bytecode::Operand(generator.allocate_register());
    generator.emit<Bytecode::Op::Mov>(callee, *original_callee);

    Bytecode::Op::CallType call_type;
    if (is<NewExpression>(*this)) {
        call_type = Bytecode::Op::CallType::Construct;
    } else if (m_callee->is_identifier() && static_cast<Identifier const&>(*m_callee).string() == "eval"sv) {
        call_type = Bytecode::Op::CallType::DirectEval;
    } else {
        call_type = Bytecode::Op::CallType::Call;
    }

    Optional<Bytecode::StringTableIndex> expression_string_index;
    if (auto expression_string = this->expression_string(); expression_string.has_value())
        expression_string_index = generator.intern_string(expression_string.release_value());

    bool has_spread = any_of(arguments(), [](auto& argument) { return argument.is_spread; });
    auto dst = choose_dst(generator, preferred_dst);

    if (has_spread) {
        auto arguments = TRY(arguments_to_array_for_call(generator, this->arguments())).value();
        generator.emit<Bytecode::Op::CallWithArgumentArray>(call_type, dst, callee, this_value, arguments, expression_string_index);
    } else {
        Vector<Bytecode::Operand> argument_operands;
        for (auto const& argument : arguments()) {
            argument_operands.append(TRY(argument.value->generate_bytecode(generator)).value());
        }
        generator.emit_with_extra_operand_slots<Bytecode::Op::Call>(
            argument_operands.size(),
            call_type,
            dst,
            callee,
            this_value,
            argument_operands,
            expression_string_index,
            builtin);
    }

    return dst;
}

static Bytecode::Operand generate_await(
    Bytecode::Generator& generator,
    Bytecode::Operand argument,
    Bytecode::Operand received_completion,
    Bytecode::Operand received_completion_type,
    Bytecode::Operand received_completion_value,
    Bytecode::IdentifierTableIndex type_identifier,
    Bytecode::IdentifierTableIndex value_identifier);

// https://tc39.es/ecma262/#sec-return-statement-runtime-semantics-evaluation
Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ReturnStatement::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand>) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);

    Optional<Bytecode::Operand> return_value;

    if (m_argument) {
        //  ReturnStatement : return Expression ;
        //     1. Let exprRef be ? Evaluation of Expression.
        //     2. Let exprValue be ? GetValue(exprRef).
        return_value = TRY(m_argument->generate_bytecode(generator)).value();

        //     3. If GetGeneratorKind() is async, set exprValue to ? Await(exprValue).
        // Spec Issue?: The spec doesn't seem to do implicit await on explicit return for async functions, but does for
        //              async generators. However, the major engines do so, and this is observable via constructor lookups
        //              on Promise objects and custom thenables.
        //              See: https://tc39.es/ecma262/#sec-asyncblockstart
        //              c. Assert: If we return here, the async function either threw an exception or performed an implicit or explicit return; all awaiting is done.
        if (generator.is_in_async_function()) {
            auto received_completion = Bytecode::Operand(generator.allocate_register());
            auto received_completion_type = Bytecode::Operand(generator.allocate_register());
            auto received_completion_value = Bytecode::Operand(generator.allocate_register());

            auto type_identifier = generator.intern_identifier("type");
            auto value_identifier = generator.intern_identifier("value");
            return_value = generate_await(generator, *return_value, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);
        }

        //     4. Return Completion Record { [[Type]]: return, [[Value]]: exprValue, [[Target]]: empty }.
    } else {
        //  ReturnStatement : return ;
        //    1. Return Completion Record { [[Type]]: return, [[Value]]: undefined, [[Target]]: empty }.
        return_value = generator.add_constant(js_undefined());
    }

    if (generator.is_in_generator_or_async_function()) {
        generator.perform_needed_unwinds<Bytecode::Op::Yield>();
        generator.emit<Bytecode::Op::Yield>(nullptr, *return_value);
    } else {
        generator.perform_needed_unwinds<Bytecode::Op::Return>();
        generator.emit<Bytecode::Op::Return>(return_value);
    }

    return return_value;
}

static void get_received_completion_type_and_value(
    Bytecode::Generator& generator,
    Bytecode::Operand received_completion,
    Bytecode::Operand received_completion_type,
    Bytecode::Operand received_completion_value,
    Bytecode::IdentifierTableIndex type_identifier,
    Bytecode::IdentifierTableIndex value_identifier)
{
    generator.emit_get_by_id(received_completion_type, received_completion, type_identifier);
    generator.emit_get_by_id(received_completion_value, received_completion, value_identifier);
}

enum class AwaitBeforeYield {
    No,
    Yes,
};

static void generate_yield(Bytecode::Generator& generator,
    Bytecode::Label continuation_label,
    Bytecode::Operand argument,
    Bytecode::Operand received_completion,
    Bytecode::Operand received_completion_type,
    Bytecode::Operand received_completion_value,
    Bytecode::IdentifierTableIndex type_identifier,
    Bytecode::IdentifierTableIndex value_identifier,
    AwaitBeforeYield await_before_yield)
{
    if (!generator.is_in_async_generator_function()) {
        generator.emit<Bytecode::Op::Yield>(Bytecode::Label { continuation_label }, argument);
        return;
    }

    if (await_before_yield == AwaitBeforeYield::Yes)
        argument = generate_await(generator, argument, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);

    auto& unwrap_yield_resumption_block = generator.make_block();
    generator.emit<Bytecode::Op::Yield>(Bytecode::Label { unwrap_yield_resumption_block }, argument);
    generator.switch_to_basic_block(unwrap_yield_resumption_block);

    generator.emit<Bytecode::Op::Mov>(received_completion, Bytecode::Operand(Bytecode::Register(0)));
    get_received_completion_type_and_value(generator, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);

    // 27.6.3.7 AsyncGeneratorUnwrapYieldResumption ( resumptionValue ), https://tc39.es/ecma262/#sec-asyncgeneratorunwrapyieldresumption
    // 1. If resumptionValue.[[Type]] is not return, return ? resumptionValue.
    auto& resumption_value_type_is_return_block = generator.make_block();
    auto resumption_value_type_is_not_return_result = Bytecode::Operand(generator.allocate_register());
    generator.emit<Bytecode::Op::StrictlyInequals>(
        resumption_value_type_is_not_return_result,
        received_completion_type,
        generator.add_constant(Value(to_underlying(Completion::Type::Return))));
    generator.emit<Bytecode::Op::JumpIf>(
        resumption_value_type_is_not_return_result,
        Bytecode::Label { continuation_label },
        Bytecode::Label { resumption_value_type_is_return_block });

    generator.switch_to_basic_block(resumption_value_type_is_return_block);

    // 2. Let awaited be Completion(Await(resumptionValue.[[Value]])).
    generate_await(generator, received_completion_value, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);

    // 3. If awaited.[[Type]] is throw, return ? awaited.
    auto& awaited_type_is_normal_block = generator.make_block();
    auto awaited_type_is_throw_result = Bytecode::Operand(generator.allocate_register());
    generator.emit<Bytecode::Op::StrictlyEquals>(
        awaited_type_is_throw_result,
        received_completion_type,
        generator.add_constant(Value(to_underlying(Completion::Type::Throw))));
    generator.emit<Bytecode::Op::JumpIf>(
        awaited_type_is_throw_result,
        Bytecode::Label { continuation_label },
        Bytecode::Label { awaited_type_is_normal_block });

    // 4. Assert: awaited.[[Type]] is normal.
    generator.switch_to_basic_block(awaited_type_is_normal_block);

    // 5. Return Completion Record { [[Type]]: return, [[Value]]: awaited.[[Value]], [[Target]]: empty }.
    generator.emit<Bytecode::Op::PutById>(
        received_completion,
        type_identifier,
        generator.add_constant(Value(to_underlying(Completion::Type::Return))),
        Bytecode::Op::PropertyKind::KeyValue,
        generator.next_property_lookup_cache());
    generator.emit<Bytecode::Op::Jump>(continuation_label);
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> YieldExpression::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    VERIFY(generator.is_in_generator_function());

    auto received_completion = Bytecode::Operand(generator.allocate_register());
    auto received_completion_type = Bytecode::Operand(generator.allocate_register());
    auto received_completion_value = Bytecode::Operand(generator.allocate_register());

    auto type_identifier = generator.intern_identifier("type");
    auto value_identifier = generator.intern_identifier("value");

    if (m_is_yield_from) {
        // 15.5.5 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-generator-function-definitions-runtime-semantics-evaluation
        // 1. Let generatorKind be GetGeneratorKind().
        // NOTE: is_in_async_generator_function differentiates the generator kind.

        // 2. Let exprRef be ? Evaluation of AssignmentExpression.
        // 3. Let value be ? GetValue(exprRef).
        VERIFY(m_argument);
        auto value = TRY(m_argument->generate_bytecode(generator)).value();

        // 4. Let iteratorRecord be ? GetIterator(value, generatorKind).
        auto iterator_record = Bytecode::Operand(generator.allocate_register());
        auto iterator_hint = generator.is_in_async_generator_function() ? IteratorHint::Async : IteratorHint::Sync;
        generator.emit<Bytecode::Op::GetIterator>(iterator_record, value, iterator_hint);

        // 5. Let iterator be iteratorRecord.[[Iterator]].
        auto iterator = Bytecode::Operand(generator.allocate_register());
        generator.emit<Bytecode::Op::GetObjectFromIteratorRecord>(iterator, iterator_record);

        // Cache iteratorRecord.[[NextMethod]] for use in step 7.a.i.
        auto next_method = Bytecode::Operand(generator.allocate_register());
        generator.emit<Bytecode::Op::GetNextMethodFromIteratorRecord>(next_method, iterator_record);

        // 6. Let received be NormalCompletion(undefined).
        // See get_received_completion_type_and_value above.
        generator.emit<Bytecode::Op::Mov>(received_completion_type, generator.add_constant(Value(to_underlying(Completion::Type::Normal))));

        generator.emit<Bytecode::Op::Mov>(received_completion_value, generator.add_constant(js_undefined()));

        // 7. Repeat,
        auto& loop_block = generator.make_block();
        auto& continuation_block = generator.make_block();
        auto& loop_end_block = generator.make_block();

        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { loop_block });
        generator.switch_to_basic_block(loop_block);

        // a. If received.[[Type]] is normal, then
        auto& type_is_normal_block = generator.make_block();
        auto& is_type_throw_block = generator.make_block();

        auto received_completion_type_register_is_normal = Bytecode::Operand(generator.allocate_register());
        generator.emit<Bytecode::Op::StrictlyEquals>(
            received_completion_type_register_is_normal,
            received_completion_type,
            generator.add_constant(Value(to_underlying(Completion::Type::Normal))));
        generator.emit<Bytecode::Op::JumpIf>(
            received_completion_type_register_is_normal,
            Bytecode::Label { type_is_normal_block },
            Bytecode::Label { is_type_throw_block });

        generator.switch_to_basic_block(type_is_normal_block);

        // i. Let innerResult be ? Call(iteratorRecord.[[NextMethod]], iteratorRecord.[[Iterator]], « received.[[Value]] »).
        auto array = Bytecode::Operand(generator.allocate_register());
        generator.emit_with_extra_operand_slots<Bytecode::Op::NewArray>(2u, array, AK::Array { received_completion_value, received_completion_value });
        auto inner_result = Bytecode::Operand(generator.allocate_register());
        generator.emit<Bytecode::Op::CallWithArgumentArray>(Bytecode::Op::CallType::Call, inner_result, next_method, iterator, array);

        // ii. If generatorKind is async, set innerResult to ? Await(innerResult).
        if (generator.is_in_async_generator_function()) {
            auto new_inner_result = generate_await(generator, inner_result, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);
            generator.emit<Bytecode::Op::Mov>(inner_result, new_inner_result);
        }

        // iii. If innerResult is not an Object, throw a TypeError exception.
        generator.emit<Bytecode::Op::ThrowIfNotObject>(inner_result);

        // iv. Let done be ? IteratorComplete(innerResult).
        auto done = Bytecode::Operand(generator.allocate_register());
        generator.emit_iterator_complete(done, inner_result);

        // v. If done is true, then
        auto& type_is_normal_done_block = generator.make_block();
        auto& type_is_normal_not_done_block = generator.make_block();
        generator.emit<Bytecode::Op::JumpIf>(
            done,
            Bytecode::Label { type_is_normal_done_block },
            Bytecode::Label { type_is_normal_not_done_block });

        generator.switch_to_basic_block(type_is_normal_done_block);

        // 1. Return ? IteratorValue(innerResult).
        auto return_value = Bytecode::Operand(generator.allocate_register());
        generator.emit_iterator_value(return_value, inner_result);
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { loop_end_block });

        generator.switch_to_basic_block(type_is_normal_not_done_block);

        // vi. If generatorKind is async, set received to Completion(AsyncGeneratorYield(? IteratorValue(innerResult))).
        // vii. Else, set received to Completion(GeneratorYield(innerResult)).

        {
            // FIXME: Yield currently only accepts a Value, not an object conforming to the IteratorResult interface, so we have to do an observable lookup of `value` here.
            //        This only matters for non-async generators.
            auto current_value = Bytecode::Operand(generator.allocate_register());
            generator.emit_iterator_value(current_value, inner_result);

            generate_yield(generator,
                Bytecode::Label { continuation_block },
                current_value,
                received_completion,
                received_completion_type,
                received_completion_value,
                type_identifier,
                value_identifier,
                AwaitBeforeYield::No);
        }

        // b. Else if received.[[Type]] is throw, then
        generator.switch_to_basic_block(is_type_throw_block);
        auto& type_is_throw_block = generator.make_block();
        auto& type_is_return_block = generator.make_block();

        auto received_completion_type_register_is_throw = Bytecode::Operand(generator.allocate_register());
        generator.emit<Bytecode::Op::StrictlyEquals>(
            received_completion_type_register_is_throw,
            Bytecode::Operand(received_completion_type),
            generator.add_constant(Value(to_underlying(Completion::Type::Throw))));
        generator.emit<Bytecode::Op::JumpIf>(
            received_completion_type_register_is_throw,
            Bytecode::Label { type_is_throw_block },
            Bytecode::Label { type_is_return_block });

        generator.switch_to_basic_block(type_is_throw_block);

        // i. Let throw be ? GetMethod(iterator, "throw").
        auto throw_method = Bytecode::Operand(generator.allocate_register());
        generator.emit<Bytecode::Op::GetMethod>(throw_method, iterator, generator.intern_identifier("throw"));

        // ii. If throw is not undefined, then
        auto& throw_method_is_defined_block = generator.make_block();
        auto& throw_method_is_undefined_block = generator.make_block();
        generator.emit<Bytecode::Op::JumpUndefined>(
            throw_method,
            Bytecode::Label { throw_method_is_undefined_block },
            Bytecode::Label { throw_method_is_defined_block });

        generator.switch_to_basic_block(throw_method_is_defined_block);

        // 1. Let innerResult be ? Call(throw, iterator, « received.[[Value]] »).
        auto received_value_array = Bytecode::Operand(generator.allocate_register());
        generator.emit_with_extra_operand_slots<Bytecode::Op::NewArray>(2u, received_value_array, AK::Array { received_completion_value, received_completion_value });
        generator.emit<Bytecode::Op::CallWithArgumentArray>(Bytecode::Op::CallType::Call, inner_result, throw_method, iterator, received_value_array);

        // 2. If generatorKind is async, set innerResult to ? Await(innerResult).
        if (generator.is_in_async_generator_function()) {
            auto new_result = generate_await(generator, inner_result, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);
            generator.emit<Bytecode::Op::Mov>(inner_result, new_result);
        }

        // 3. NOTE: Exceptions from the inner iterator throw method are propagated. Normal completions from an inner throw method are processed similarly to an inner next.
        // 4. If innerResult is not an Object, throw a TypeError exception.
        generator.emit<Bytecode::Op::ThrowIfNotObject>(inner_result);

        // 5. Let done be ? IteratorComplete(innerResult).
        generator.emit_iterator_complete(done, inner_result);

        // 6. If done is true, then
        auto& type_is_throw_done_block = generator.make_block();
        auto& type_is_throw_not_done_block = generator.make_block();
        generator.emit<Bytecode::Op::JumpIf>(
            done,
            Bytecode::Label { type_is_throw_done_block },
            Bytecode::Label { type_is_throw_not_done_block });

        generator.switch_to_basic_block(type_is_throw_done_block);

        // a. Return ? IteratorValue(innerResult).
        generator.emit_iterator_value(return_value, inner_result);
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { loop_end_block });

        generator.switch_to_basic_block(type_is_throw_not_done_block);

        {
            // 7. If generatorKind is async, set received to Completion(AsyncGeneratorYield(? IteratorValue(innerResult))).
            // 8. Else, set received to Completion(GeneratorYield(innerResult)).

            // FIXME: Yield currently only accepts a Value, not an object conforming to the IteratorResult interface, so we have to do an observable lookup of `value` here.
            //        This only matters for non-async generators.
            auto yield_value = Bytecode::Operand(generator.allocate_register());
            generator.emit_iterator_value(yield_value, inner_result);
            generate_yield(generator, Bytecode::Label { continuation_block }, yield_value, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier, AwaitBeforeYield::No);
        }

        generator.switch_to_basic_block(throw_method_is_undefined_block);

        // 1. NOTE: If iterator does not have a throw method, this throw is going to terminate the yield* loop. But first we need to give iterator a chance to clean up.

        // 2. Let closeCompletion be Completion Record { [[Type]]: normal, [[Value]]: empty, [[Target]]: empty }.
        // 3. If generatorKind is async, perform ? AsyncIteratorClose(iteratorRecord, closeCompletion).
        if (generator.is_in_async_generator_function()) {
            // FIXME: This performs `await` outside of the generator!
            generator.emit<Bytecode::Op::AsyncIteratorClose>(iterator_record, Completion::Type::Normal, Optional<Value> {});
        }
        // 4. Else, perform ? IteratorClose(iteratorRecord, closeCompletion).
        else {
            generator.emit<Bytecode::Op::IteratorClose>(iterator_record, Completion::Type::Normal, Optional<Value> {});
        }

        // 5. NOTE: The next step throws a TypeError to indicate that there was a yield* protocol violation: iterator does not have a throw method.
        // 6. Throw a TypeError exception.
        auto exception = Bytecode::Operand(generator.allocate_register());
        generator.emit<Bytecode::Op::NewTypeError>(exception, generator.intern_string(ErrorType::YieldFromIteratorMissingThrowMethod.message()));
        generator.perform_needed_unwinds<Bytecode::Op::Throw>();
        generator.emit<Bytecode::Op::Throw>(exception);

        // c. Else,
        // i. Assert: received.[[Type]] is return.
        generator.switch_to_basic_block(type_is_return_block);

        // ii. Let return be ? GetMethod(iterator, "return").
        auto return_method = Bytecode::Operand(generator.allocate_register());
        generator.emit<Bytecode::Op::GetMethod>(return_method, iterator, generator.intern_identifier("return"));

        // iii. If return is undefined, then
        auto& return_is_undefined_block = generator.make_block();
        auto& return_is_defined_block = generator.make_block();
        generator.emit<Bytecode::Op::JumpUndefined>(
            return_method,
            Bytecode::Label { return_is_undefined_block },
            Bytecode::Label { return_is_defined_block });

        generator.switch_to_basic_block(return_is_undefined_block);

        // 1. If generatorKind is async, set received.[[Value]] to ? Await(received.[[Value]]).
        if (generator.is_in_async_generator_function()) {
            generate_await(generator, received_completion_value, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);
        }

        // 2. Return ? received.
        // NOTE: This will always be a return completion.
        generator.perform_needed_unwinds<Bytecode::Op::Yield>();
        generator.emit<Bytecode::Op::Yield>(nullptr, received_completion_value);

        generator.switch_to_basic_block(return_is_defined_block);

        // iv. Let innerReturnResult be ? Call(return, iterator, « received.[[Value]] »).
        auto call_array = Bytecode::Operand(generator.allocate_register());
        generator.emit_with_extra_operand_slots<Bytecode::Op::NewArray>(2, call_array, AK::Array { received_completion_value, received_completion_value });
        auto inner_return_result = Bytecode::Operand(generator.allocate_register());
        generator.emit<Bytecode::Op::CallWithArgumentArray>(Bytecode::Op::CallType::Call, inner_return_result, return_method, iterator, call_array);

        // v. If generatorKind is async, set innerReturnResult to ? Await(innerReturnResult).
        if (generator.is_in_async_generator_function()) {
            auto new_value = generate_await(generator, inner_return_result, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);
            generator.emit<Bytecode::Op::Mov>(inner_return_result, new_value);
        }

        // vi. If innerReturnResult is not an Object, throw a TypeError exception.
        generator.emit<Bytecode::Op::ThrowIfNotObject>(inner_return_result);

        // vii. Let done be ? IteratorComplete(innerReturnResult).
        generator.emit_iterator_complete(done, inner_return_result);

        // viii. If done is true, then
        auto& type_is_return_done_block = generator.make_block();
        auto& type_is_return_not_done_block = generator.make_block();
        generator.emit<Bytecode::Op::JumpIf>(
            done,
            Bytecode::Label { type_is_return_done_block },
            Bytecode::Label { type_is_return_not_done_block });

        generator.switch_to_basic_block(type_is_return_done_block);

        // 1. Let value be ? IteratorValue(innerReturnResult).
        auto inner_return_result_value = Bytecode::Operand(generator.allocate_register());
        generator.emit_iterator_value(inner_return_result_value, inner_return_result);

        // 2. Return Completion Record { [[Type]]: return, [[Value]]: value, [[Target]]: empty }.
        generator.perform_needed_unwinds<Bytecode::Op::Yield>();
        generator.emit<Bytecode::Op::Yield>(nullptr, inner_return_result_value);

        generator.switch_to_basic_block(type_is_return_not_done_block);

        // ix. If generatorKind is async, set received to Completion(AsyncGeneratorYield(? IteratorValue(innerReturnResult))).
        // x. Else, set received to Completion(GeneratorYield(innerReturnResult)).
        // FIXME: Yield currently only accepts a Value, not an object conforming to the IteratorResult interface, so we have to do an observable lookup of `value` here.
        //        This only matters for non-async generators.
        auto received = Bytecode::Operand(generator.allocate_register());
        generator.emit_iterator_value(received, inner_return_result);

        generate_yield(generator, Bytecode::Label { continuation_block }, received, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier, AwaitBeforeYield::No);

        generator.switch_to_basic_block(continuation_block);
        generator.emit<Bytecode::Op::Mov>(received_completion, Bytecode::Operand(Bytecode::Register(0)));
        get_received_completion_type_and_value(generator, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { loop_block });

        generator.switch_to_basic_block(loop_end_block);
        return return_value;
    }

    Optional<Bytecode::Operand> argument;
    if (m_argument)
        argument = TRY(m_argument->generate_bytecode(generator)).value();
    else
        argument = generator.add_constant(js_undefined());

    auto& continuation_block = generator.make_block();
    generate_yield(generator, Bytecode::Label { continuation_block }, *argument, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier, AwaitBeforeYield::Yes);
    generator.switch_to_basic_block(continuation_block);
    generator.emit<Bytecode::Op::Mov>(received_completion, Bytecode::Operand(Bytecode::Register(0)));
    get_received_completion_type_and_value(generator, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);

    auto& normal_completion_continuation_block = generator.make_block();
    auto& throw_completion_continuation_block = generator.make_block();

    auto received_completion_type_is_normal = Bytecode::Operand(generator.allocate_register());
    generator.emit<Bytecode::Op::StrictlyEquals>(
        received_completion_type_is_normal,
        received_completion_type,
        generator.add_constant(Value(to_underlying(Completion::Type::Normal))));
    generator.emit<Bytecode::Op::JumpIf>(
        received_completion_type_is_normal,
        Bytecode::Label { normal_completion_continuation_block },
        Bytecode::Label { throw_completion_continuation_block });

    auto& throw_value_block = generator.make_block();
    auto& return_value_block = generator.make_block();

    generator.switch_to_basic_block(throw_completion_continuation_block);
    auto received_completion_type_is_throw = Bytecode::Operand(generator.allocate_register());
    generator.emit<Bytecode::Op::StrictlyEquals>(
        received_completion_type_is_throw,
        received_completion_type,
        generator.add_constant(Value(to_underlying(Completion::Type::Throw))));

    // If type is not equal to "throw" or "normal", assume it's "return".
    generator.emit<Bytecode::Op::JumpIf>(
        received_completion_type_is_throw,
        Bytecode::Label { throw_value_block },
        Bytecode::Label { return_value_block });

    generator.switch_to_basic_block(throw_value_block);
    generator.perform_needed_unwinds<Bytecode::Op::Throw>();
    generator.emit<Bytecode::Op::Throw>(received_completion_value);

    generator.switch_to_basic_block(return_value_block);
    generator.perform_needed_unwinds<Bytecode::Op::Yield>();
    generator.emit<Bytecode::Op::Yield>(nullptr, received_completion_value);

    generator.switch_to_basic_block(normal_completion_continuation_block);
    return received_completion_value;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> IfStatement::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
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

    auto dst = choose_dst(generator, preferred_dst);
    generator.emit<Bytecode::Op::Mov>(dst, generator.add_constant(js_undefined()));

    auto predicate = TRY(m_predicate->generate_bytecode(generator)).value();
    generator.emit<Bytecode::Op::JumpIf>(
        predicate,
        Bytecode::Label { true_block },
        Bytecode::Label { false_block });

    generator.switch_to_basic_block(true_block);
    auto consequent = TRY(m_consequent->generate_bytecode(generator, dst));
    if (!generator.is_current_block_terminated()) {
        if (consequent.has_value())
            generator.emit<Bytecode::Op::Mov>(dst, *consequent);
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { end_block });
    }

    generator.switch_to_basic_block(false_block);

    Optional<Bytecode::Operand> alternate;
    if (m_alternate) {
        alternate = TRY(m_alternate->generate_bytecode(generator, dst));
    }
    if (!generator.is_current_block_terminated()) {
        if (alternate.has_value())
            generator.emit<Bytecode::Op::Mov>(dst, *alternate);
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { end_block });
    }

    generator.switch_to_basic_block(end_block);

    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ContinueStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    // FIXME: Handle finally blocks in a graceful manner
    //        We need to execute the finally block, but tell it to resume
    //        execution at the designated block
    if (!m_target_label.has_value()) {
        generator.generate_continue();
        return Optional<Bytecode::Operand> {};
    }

    generator.generate_continue(m_target_label.value());
    return Optional<Bytecode::Operand> {};
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> DebuggerStatement::generate_bytecode(Bytecode::Generator&, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    return Optional<Bytecode::Operand> {};
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ConditionalExpression::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
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

    auto test = TRY(m_test->generate_bytecode(generator)).value();
    generator.emit<Bytecode::Op::JumpIf>(
        test,
        Bytecode::Label { true_block },
        Bytecode::Label { false_block });

    auto dst = choose_dst(generator, preferred_dst);

    generator.switch_to_basic_block(true_block);
    auto consequent = TRY(m_consequent->generate_bytecode(generator)).value();
    generator.emit<Bytecode::Op::Mov>(dst, consequent);

    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { end_block });

    generator.switch_to_basic_block(false_block);
    auto alternate = TRY(m_alternate->generate_bytecode(generator)).value();
    generator.emit<Bytecode::Op::Mov>(dst, alternate);
    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { end_block });

    generator.switch_to_basic_block(end_block);
    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> SequenceExpression::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    Optional<Bytecode::Operand> last_value;
    for (auto& expression : m_expressions) {
        last_value = TRY(expression->generate_bytecode(generator));
    }

    return last_value;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> TemplateLiteral::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);

    auto dst = choose_dst(generator, preferred_dst);

    for (size_t i = 0; i < m_expressions.size(); i++) {
        auto value = TRY(m_expressions[i]->generate_bytecode(generator)).value();
        if (i == 0) {
            generator.emit<Bytecode::Op::Mov>(dst, value);
        } else {
            generator.emit<Bytecode::Op::ConcatString>(dst, value);
        }
    }

    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> TaggedTemplateLiteral::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto tag = TRY(m_tag->generate_bytecode(generator)).value();

    // FIXME: We only need to record the first and last register,
    //        due to packing everything in an array, same goes for argument_regs
    // FIXME: Follow
    //        13.2.8.3 GetTemplateObject ( templateLiteral ), https://tc39.es/ecma262/#sec-gettemplateobject
    //        more closely, namely:
    //        * cache this somehow
    //        * add a raw object accessor
    //        * freeze array and raw member
    Vector<Bytecode::Operand> string_regs;
    auto& expressions = m_template_literal->expressions();
    for (size_t i = 0; i < expressions.size(); ++i) {
        if (i % 2 != 0)
            continue;
        string_regs.append(Bytecode::Operand(generator.allocate_register()));
    }

    size_t reg_index = 0;
    for (size_t i = 0; i < expressions.size(); ++i) {
        if (i % 2 != 0)
            continue;
        // NOTE: If the string contains invalid escapes we get a null expression here,
        //       which we then convert to the expected `undefined` TV. See
        //       12.9.6.1 Static Semantics: TV, https://tc39.es/ecma262/#sec-static-semantics-tv
        auto string_reg = Bytecode::Operand(string_regs[reg_index++]);
        if (is<NullLiteral>(expressions[i])) {
            generator.emit<Bytecode::Op::Mov>(string_reg, generator.add_constant(js_undefined()));
        } else {
            auto value = TRY(expressions[i]->generate_bytecode(generator)).value();
            generator.emit<Bytecode::Op::Mov>(string_reg, value);
        }
    }

    auto strings_array = Bytecode::Operand(generator.allocate_register());
    if (string_regs.is_empty()) {
        generator.emit<Bytecode::Op::NewArray>(strings_array);
    } else {
        generator.emit_with_extra_operand_slots<Bytecode::Op::NewArray>(2u, strings_array, AK::Array { string_regs.first(), string_regs.last() });
    }

    Vector<Bytecode::Operand> argument_regs;
    argument_regs.append(strings_array);
    for (size_t i = 1; i < expressions.size(); i += 2)
        argument_regs.append(Bytecode::Operand(generator.allocate_register()));

    for (size_t i = 1; i < expressions.size(); i += 2) {
        auto string_reg = argument_regs[1 + i / 2];
        auto string = TRY(expressions[i]->generate_bytecode(generator)).value();
        generator.emit<Bytecode::Op::Mov>(string_reg, string);
    }

    Vector<Bytecode::Operand> raw_string_regs;
    for ([[maybe_unused]] auto& raw_string : m_template_literal->raw_strings())
        string_regs.append(Bytecode::Operand(generator.allocate_register()));

    reg_index = 0;
    for (auto& raw_string : m_template_literal->raw_strings()) {
        auto value = TRY(raw_string->generate_bytecode(generator)).value();
        auto raw_string_reg = string_regs[reg_index++];
        generator.emit<Bytecode::Op::Mov>(raw_string_reg, value);
        raw_string_regs.append(raw_string_reg);
    }

    auto raw_strings_array = Bytecode::Operand(generator.allocate_register());
    if (raw_string_regs.is_empty()) {
        generator.emit<Bytecode::Op::NewArray>(raw_strings_array);
    } else {
        generator.emit_with_extra_operand_slots<Bytecode::Op::NewArray>(2u, raw_strings_array, AK::Array { raw_string_regs.first(), raw_string_regs.last() });
    }

    generator.emit<Bytecode::Op::PutById>(strings_array, generator.intern_identifier("raw"), raw_strings_array, Bytecode::Op::PropertyKind::KeyValue, generator.next_property_lookup_cache());

    auto arguments = Bytecode::Operand(generator.allocate_register());
    if (!argument_regs.is_empty())
        generator.emit_with_extra_operand_slots<Bytecode::Op::NewArray>(2, arguments, AK::Array { argument_regs.first(), argument_regs.last() });
    else
        generator.emit<Bytecode::Op::NewArray>(arguments);

    auto dst = choose_dst(generator, preferred_dst);
    generator.emit<Bytecode::Op::CallWithArgumentArray>(Bytecode::Op::CallType::Call, dst, tag, generator.add_constant(js_undefined()), arguments);
    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> UpdateExpression::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand>) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto reference = TRY(generator.emit_load_from_reference(*m_argument));

    Optional<Bytecode::Operand> previous_value_for_postfix;

    if (m_op == UpdateOp::Increment) {
        if (m_prefixed) {
            generator.emit<Bytecode::Op::Increment>(*reference.loaded_value);
        } else {
            previous_value_for_postfix = Bytecode::Operand(generator.allocate_register());
            generator.emit<Bytecode::Op::PostfixIncrement>(*previous_value_for_postfix, *reference.loaded_value);
        }
    } else {
        if (m_prefixed) {
            generator.emit<Bytecode::Op::Decrement>(*reference.loaded_value);
        } else {
            previous_value_for_postfix = Bytecode::Operand(generator.allocate_register());
            generator.emit<Bytecode::Op::PostfixDecrement>(*previous_value_for_postfix, *reference.loaded_value);
        }
    }

    if (is<Identifier>(*m_argument))
        (void)TRY(generator.emit_store_to_reference(static_cast<Identifier const&>(*m_argument), *reference.loaded_value));
    else
        (void)TRY(generator.emit_store_to_reference(reference, *reference.loaded_value));

    if (!m_prefixed)
        return *previous_value_for_postfix;
    return *reference.loaded_value;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ThrowStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto argument = TRY(m_argument->generate_bytecode(generator)).value();
    generator.perform_needed_unwinds<Bytecode::Op::Throw>();
    generator.emit<Bytecode::Op::Throw>(argument);
    return Optional<Bytecode::Operand> {};
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> BreakStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    // FIXME: Handle finally blocks in a graceful manner
    //        We need to execute the finally block, but tell it to resume
    //        execution at the designated block
    if (!m_target_label.has_value()) {
        generator.generate_break();
        return Optional<Bytecode::Operand> {};
    }

    generator.generate_break(m_target_label.value());
    return Optional<Bytecode::Operand> {};
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> TryStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto& saved_block = generator.current_block();

    Optional<Bytecode::Label> handler_target;
    Optional<Bytecode::Label> finalizer_target;
    Optional<Bytecode::Generator::UnwindContext> unwind_context;

    Bytecode::BasicBlock* next_block { nullptr };

    Optional<Bytecode::Operand> dst;

    if (m_finalizer) {
        // FIXME: See notes in Op.h->ScheduleJump
        auto& finalizer_block = generator.make_block();
        generator.switch_to_basic_block(finalizer_block);
        generator.emit<Bytecode::Op::LeaveUnwindContext>();
        (void)TRY(m_finalizer->generate_bytecode(generator));
        if (!generator.is_current_block_terminated()) {
            next_block = &generator.make_block();
            auto next_target = Bytecode::Label { *next_block };
            generator.emit<Bytecode::Op::ContinuePendingUnwind>(next_target);
        }
        finalizer_target = Bytecode::Label { finalizer_block };

        generator.start_boundary(Bytecode::Generator::BlockBoundaryType::ReturnToFinally);
        unwind_context.emplace(generator, finalizer_target);
    }
    if (m_handler) {
        auto& handler_block = generator.make_block();
        generator.switch_to_basic_block(handler_block);

        auto caught_value = Bytecode::Operand { generator.allocate_register() };
        generator.emit<Bytecode::Op::Catch>(caught_value);

        if (!m_finalizer)
            generator.emit<Bytecode::Op::LeaveUnwindContext>();

        // OPTIMIZATION: We avoid creating a lexical environment if the catch clause has no parameter.
        bool did_create_variable_scope_for_catch_clause = false;

        TRY(m_handler->parameter().visit(
            [&](DeprecatedFlyString const& parameter) -> Bytecode::CodeGenerationErrorOr<void> {
                if (!parameter.is_empty()) {
                    generator.begin_variable_scope();
                    did_create_variable_scope_for_catch_clause = true;
                    auto parameter_identifier = generator.intern_identifier(parameter);
                    generator.emit<Bytecode::Op::CreateVariable>(parameter_identifier, Bytecode::Op::EnvironmentMode::Lexical, false);
                    generator.emit<Bytecode::Op::SetVariable>(parameter_identifier, caught_value, generator.next_environment_variable_cache(), Bytecode::Op::SetVariable::InitializationMode::Initialize);
                }
                return {};
            },
            [&](NonnullRefPtr<BindingPattern const> const& binding_pattern) -> Bytecode::CodeGenerationErrorOr<void> {
                generator.begin_variable_scope();
                did_create_variable_scope_for_catch_clause = true;
                TRY(generate_binding_pattern_bytecode(generator, *binding_pattern, Bytecode::Op::SetVariable::InitializationMode::Initialize, caught_value, true));
                return {};
            }));

        auto handler_result = TRY(m_handler->body().generate_bytecode(generator));
        if (handler_result.has_value() && !generator.is_current_block_terminated()) {
            dst = Bytecode::Operand(generator.allocate_register());
            generator.emit<Bytecode::Op::Mov>(*dst, *handler_result);
        }
        handler_target = Bytecode::Label { handler_block };

        if (did_create_variable_scope_for_catch_clause)
            generator.end_variable_scope();

        if (!generator.is_current_block_terminated()) {
            if (m_finalizer) {
                generator.emit<Bytecode::Op::Jump>(*finalizer_target);
            } else {
                VERIFY(!next_block);
                VERIFY(!unwind_context.has_value());
                next_block = &generator.make_block();
                auto next_target = Bytecode::Label { *next_block };
                generator.emit<Bytecode::Op::Jump>(next_target);
            }
        }
    }
    if (m_finalizer)
        generator.end_boundary(Bytecode::Generator::BlockBoundaryType::ReturnToFinally);
    if (m_handler) {
        if (!m_finalizer)
            unwind_context.emplace(generator, OptionalNone());
        unwind_context->set_handler(handler_target.value());
    }

    auto& target_block = generator.make_block();
    generator.switch_to_basic_block(saved_block);
    generator.emit<Bytecode::Op::EnterUnwindContext>(Bytecode::Label { target_block });
    generator.start_boundary(Bytecode::Generator::BlockBoundaryType::Unwind);
    if (m_finalizer)
        generator.start_boundary(Bytecode::Generator::BlockBoundaryType::ReturnToFinally);

    generator.switch_to_basic_block(target_block);
    auto block_result = TRY(m_block->generate_bytecode(generator));
    if (!generator.is_current_block_terminated()) {
        if (block_result.has_value()) {
            dst = Bytecode::Operand(generator.allocate_register());
            generator.emit<Bytecode::Op::Mov>(*dst, *block_result);
        }

        if (m_finalizer) {
            generator.emit<Bytecode::Op::Jump>(*finalizer_target);
        } else {
            VERIFY(unwind_context.has_value());
            unwind_context.clear();
            if (!next_block)
                next_block = &generator.make_block();
            generator.emit<Bytecode::Op::LeaveUnwindContext>();
            generator.emit<Bytecode::Op::Jump>(Bytecode::Label { *next_block });
        }
    }

    if (m_finalizer)
        generator.end_boundary(Bytecode::Generator::BlockBoundaryType::ReturnToFinally);
    generator.end_boundary(Bytecode::Generator::BlockBoundaryType::Unwind);

    generator.switch_to_basic_block(next_block ? *next_block : saved_block);
    if (!dst.has_value())
        return generator.add_constant(js_undefined());
    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> SwitchStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generate_labelled_evaluation(generator, {});
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> SwitchStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<DeprecatedFlyString> const& label_set, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto discriminant = TRY(m_discriminant->generate_bytecode(generator)).value();
    Vector<Bytecode::BasicBlock&> case_blocks;
    Bytecode::BasicBlock* entry_block_for_default { nullptr };
    Bytecode::BasicBlock* next_test_block = &generator.make_block();

    auto has_lexical_declarations = this->has_lexical_declarations();
    if (has_lexical_declarations)
        generator.block_declaration_instantiation(*this);

    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { *next_test_block });

    for (auto& switch_case : m_cases) {
        auto& case_block = generator.make_block();
        auto& case_entry_block = generator.make_block();
        if (switch_case->test()) {
            generator.switch_to_basic_block(*next_test_block);
            auto test_value = TRY(switch_case->test()->generate_bytecode(generator)).value();
            auto result = Bytecode::Operand(generator.allocate_register());
            generator.emit<Bytecode::Op::StrictlyEquals>(result, test_value, discriminant);
            next_test_block = &generator.make_block();
            generator.emit<Bytecode::Op::JumpIf>(
                result,
                Bytecode::Label { case_entry_block },
                Bytecode::Label { *next_test_block });
        } else {
            entry_block_for_default = &case_entry_block;
        }

        // Initialize the completion value of the switch statement to empty. We can't do this in the case's basic block directly,
        // as we must not clobber the possible non-empty completion value of the previous case when falling through.
        generator.switch_to_basic_block(case_entry_block);
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { case_block });

        case_blocks.append(case_block);
    }
    generator.switch_to_basic_block(*next_test_block);
    auto& end_block = generator.make_block();

    if (entry_block_for_default != nullptr) {
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { *entry_block_for_default });
    } else {
        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { end_block });
    }
    auto current_block = case_blocks.begin();
    generator.begin_breakable_scope(Bytecode::Label { end_block }, label_set);
    auto dst = Bytecode::Operand(generator.allocate_register());
    for (auto& switch_case : m_cases) {
        generator.switch_to_basic_block(*current_block);
        for (auto& statement : switch_case->children()) {
            auto result = TRY(statement->generate_bytecode(generator));
            if (generator.is_current_block_terminated())
                break;
            if (result.has_value())
                generator.emit<Bytecode::Op::Mov>(dst, *result);
            else
                generator.emit<Bytecode::Op::Mov>(dst, generator.add_constant(js_undefined()));
        }
        if (!generator.is_current_block_terminated()) {
            auto next_block = current_block;
            next_block++;
            if (next_block.is_end()) {
                generator.emit<Bytecode::Op::Jump>(Bytecode::Label { end_block });
            } else {
                generator.emit<Bytecode::Op::Jump>(Bytecode::Label { *next_block });
            }
        }
        current_block++;
    }
    generator.end_breakable_scope();

    generator.switch_to_basic_block(end_block);

    if (has_lexical_declarations)
        generator.end_variable_scope();

    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> SuperExpression::generate_bytecode(Bytecode::Generator&, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    // The semantics for SuperExpression are handled in CallExpression and SuperCall.
    VERIFY_NOT_REACHED();
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ClassDeclaration::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto value = TRY(m_class_expression->generate_bytecode(generator)).value();
    generator.emit_set_variable(*m_class_expression.ptr()->m_name, value, Bytecode::Op::SetVariable::InitializationMode::Initialize);
    // NOTE: ClassDeclaration does not produce a value.
    return Optional<Bytecode::Operand> {};
}

// 15.7.14 Runtime Semantics: ClassDefinitionEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-classdefinitionevaluation
Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ClassExpression::generate_bytecode_with_lhs_name(Bytecode::Generator& generator, Optional<Bytecode::IdentifierTableIndex> lhs_name, Optional<Bytecode::Operand> preferred_dst) const
{
    // NOTE: Step 2 is not a part of NewClass instruction because it is assumed to be done before super class expression evaluation
    generator.emit<Bytecode::Op::CreateLexicalEnvironment>();

    if (has_name() || !lhs_name.has_value()) {
        // NOTE: Step 3.a is not a part of NewClass instruction because it is assumed to be done before super class expression evaluation
        auto interned_index = generator.intern_identifier(name());
        generator.emit<Bytecode::Op::CreateVariable>(interned_index, Bytecode::Op::EnvironmentMode::Lexical, true);
    }

    Optional<Bytecode::Operand> super_class;
    if (m_super_class)
        super_class = TRY(m_super_class->generate_bytecode(generator)).value();

    auto dst = choose_dst(generator, preferred_dst);
    generator.emit<Bytecode::Op::NewClass>(dst, super_class, *this, lhs_name);

    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ClassExpression::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generate_bytecode_with_lhs_name(generator, {}, preferred_dst);
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> SpreadExpression::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    // NOTE: All users of this should handle the behaviour of this on their own,
    //       assuming it returns an Array-like object
    return m_target->generate_bytecode(generator);
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ThisExpression::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto dst = choose_dst(generator, preferred_dst);
    generator.emit<Bytecode::Op::ResolveThisBinding>(dst);
    return dst;
}

static Bytecode::Operand generate_await(
    Bytecode::Generator& generator,
    Bytecode::Operand argument,
    Bytecode::Operand received_completion,
    Bytecode::Operand received_completion_type,
    Bytecode::Operand received_completion_value,
    Bytecode::IdentifierTableIndex type_identifier,
    Bytecode::IdentifierTableIndex value_identifier)
{
    VERIFY(generator.is_in_async_function());

    auto& continuation_block = generator.make_block();
    generator.emit<Bytecode::Op::Await>(Bytecode::Label { continuation_block }, argument);
    generator.switch_to_basic_block(continuation_block);

    // FIXME: It's really magical that we can just assume that the completion value is in register 0.
    //        It ends up there because we "return" from the Await instruction above via the synthetic
    //        generator function that actually drives async execution.
    generator.emit<Bytecode::Op::Mov>(received_completion, Bytecode::Operand(Bytecode::Register(0)));
    get_received_completion_type_and_value(generator, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);

    auto& normal_completion_continuation_block = generator.make_block();
    auto& throw_value_block = generator.make_block();

    auto received_completion_type_is_normal = Bytecode::Operand(generator.allocate_register());
    generator.emit<Bytecode::Op::StrictlyEquals>(
        received_completion_type_is_normal,
        received_completion_type,
        generator.add_constant(Value(to_underlying(Completion::Type::Normal))));
    generator.emit<Bytecode::Op::JumpIf>(
        received_completion_type_is_normal,
        Bytecode::Label { normal_completion_continuation_block },
        Bytecode::Label { throw_value_block });

    // Simplification: The only abrupt completion we receive from AsyncFunctionDriverWrapper or AsyncGenerator is Type::Throw
    //                 So we do not need to account for the Type::Return path
    generator.switch_to_basic_block(throw_value_block);
    generator.perform_needed_unwinds<Bytecode::Op::Throw>();
    generator.emit<Bytecode::Op::Throw>(received_completion_value);

    generator.switch_to_basic_block(normal_completion_continuation_block);
    return received_completion_value;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> AwaitExpression::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto argument = TRY(m_argument->generate_bytecode(generator)).value();

    auto received_completion = Bytecode::Operand(generator.allocate_register());
    auto received_completion_type = Bytecode::Operand(generator.allocate_register());
    auto received_completion_value = Bytecode::Operand(generator.allocate_register());

    generator.emit<Bytecode::Op::Mov>(received_completion, Bytecode::Operand(Bytecode::Register(0)));

    auto type_identifier = generator.intern_identifier("type");
    auto value_identifier = generator.intern_identifier("value");

    return generate_await(generator, argument, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> WithStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto object = TRY(m_object->generate_bytecode(generator)).value();
    generator.emit<Bytecode::Op::EnterObjectEnvironment>(object);

    // EnterObjectEnvironment sets the running execution context's lexical_environment to a new Object Environment.
    generator.start_boundary(Bytecode::Generator::BlockBoundaryType::LeaveLexicalEnvironment);

    auto body_result = TRY(m_body->generate_bytecode(generator));
    if (!body_result.has_value())
        body_result = generator.add_constant(js_undefined());
    generator.end_boundary(Bytecode::Generator::BlockBoundaryType::LeaveLexicalEnvironment);

    if (!generator.is_current_block_terminated())
        generator.emit<Bytecode::Op::LeaveLexicalEnvironment>();

    return body_result;
}

enum class LHSKind {
    Assignment,
    VarBinding,
    LexicalBinding,
};

enum class IterationKind {
    Enumerate,
    Iterate,
    AsyncIterate,
};

// 14.7.5.6 ForIn/OfHeadEvaluation ( uninitializedBoundNames, expr, iterationKind ), https://tc39.es/ecma262/#sec-runtime-semantics-forinofheadevaluation
struct ForInOfHeadEvaluationResult {
    bool is_destructuring { false };
    LHSKind lhs_kind { LHSKind::Assignment };
    Optional<Bytecode::Operand> iterator;
};
static Bytecode::CodeGenerationErrorOr<ForInOfHeadEvaluationResult> for_in_of_head_evaluation(Bytecode::Generator& generator, IterationKind iteration_kind, Variant<NonnullRefPtr<ASTNode const>, NonnullRefPtr<BindingPattern const>> const& lhs, NonnullRefPtr<ASTNode const> const& rhs)
{
    ForInOfHeadEvaluationResult result {};

    bool entered_lexical_scope = false;
    if (auto* ast_ptr = lhs.get_pointer<NonnullRefPtr<ASTNode const>>(); ast_ptr && is<VariableDeclaration>(**ast_ptr)) {
        // Runtime Semantics: ForInOfLoopEvaluation, for any of:
        //  ForInOfStatement : for ( var ForBinding in Expression ) Statement
        //  ForInOfStatement : for ( ForDeclaration in Expression ) Statement
        //  ForInOfStatement : for ( var ForBinding of AssignmentExpression ) Statement
        //  ForInOfStatement : for ( ForDeclaration of AssignmentExpression ) Statement

        auto& variable_declaration = static_cast<VariableDeclaration const&>(**ast_ptr);
        result.is_destructuring = variable_declaration.declarations().first()->target().has<NonnullRefPtr<BindingPattern const>>();
        result.lhs_kind = variable_declaration.is_lexical_declaration() ? LHSKind::LexicalBinding : LHSKind::VarBinding;

        if (variable_declaration.declaration_kind() == DeclarationKind::Var) {
            // B.3.5 Initializers in ForIn Statement Heads, https://tc39.es/ecma262/#sec-initializers-in-forin-statement-heads
            auto& variable = variable_declaration.declarations().first();
            if (variable->init()) {
                VERIFY(variable->target().has<NonnullRefPtr<Identifier const>>());
                auto identifier = variable->target().get<NonnullRefPtr<Identifier const>>();
                auto identifier_table_ref = generator.intern_identifier(identifier->string());
                auto value = TRY(generator.emit_named_evaluation_if_anonymous_function(*variable->init(), identifier_table_ref)).value();
                generator.emit_set_variable(*identifier, value);
            }
        } else {
            auto has_non_local_variables = false;
            MUST(variable_declaration.for_each_bound_identifier([&](auto const& identifier) {
                if (!identifier.is_local())
                    has_non_local_variables = true;
            }));

            if (has_non_local_variables) {
                // 1. Let oldEnv be the running execution context's LexicalEnvironment.
                // NOTE: 'uninitializedBoundNames' refers to the lexical bindings (i.e. Const/Let) present in the second and last form.
                // 2. If uninitializedBoundNames is not an empty List, then
                entered_lexical_scope = true;
                // a. Assert: uninitializedBoundNames has no duplicate entries.
                // b. Let newEnv be NewDeclarativeEnvironment(oldEnv).
                generator.begin_variable_scope();
                // c. For each String name of uninitializedBoundNames, do
                // NOTE: Nothing in the callback throws an exception.
                MUST(variable_declaration.for_each_bound_identifier([&](auto const& identifier) {
                    if (identifier.is_local())
                        return;
                    // i. Perform ! newEnv.CreateMutableBinding(name, false).
                    auto interned_identifier = generator.intern_identifier(identifier.string());
                    generator.emit<Bytecode::Op::CreateVariable>(interned_identifier, Bytecode::Op::EnvironmentMode::Lexical, false);
                }));
                // d. Set the running execution context's LexicalEnvironment to newEnv.
                // NOTE: Done by CreateLexicalEnvironment.
            }
        }
    } else {
        // Runtime Semantics: ForInOfLoopEvaluation, for any of:
        //  ForInOfStatement : for ( LeftHandSideExpression in Expression ) Statement
        //  ForInOfStatement : for ( LeftHandSideExpression of AssignmentExpression ) Statement
        result.lhs_kind = LHSKind::Assignment;
    }

    // 3. Let exprRef be the result of evaluating expr.
    auto object = TRY(rhs->generate_bytecode(generator)).value();

    // 4. Set the running execution context's LexicalEnvironment to oldEnv.
    if (entered_lexical_scope)
        generator.end_variable_scope();

    // 5. Let exprValue be ? GetValue(exprRef).
    // NOTE: No need to store this anywhere.

    auto iterator = Bytecode::Operand(generator.allocate_register());

    // 6. If iterationKind is enumerate, then
    if (iteration_kind == IterationKind::Enumerate) {
        // a. If exprValue is undefined or null, then
        auto& nullish_block = generator.make_block();
        auto& continuation_block = generator.make_block();
        generator.emit<Bytecode::Op::JumpNullish>(
            object,
            Bytecode::Label { nullish_block },
            Bytecode::Label { continuation_block });

        // i. Return Completion Record { [[Type]]: break, [[Value]]: empty, [[Target]]: empty }.
        generator.switch_to_basic_block(nullish_block);
        generator.generate_break();

        generator.switch_to_basic_block(continuation_block);
        // b. Let obj be ! ToObject(exprValue).
        // NOTE: GetObjectPropertyIterator does this.
        // c. Let iterator be EnumerateObjectProperties(obj).
        // d. Let nextMethod be ! GetV(iterator, "next").
        // e. Return the Iterator Record { [[Iterator]]: iterator, [[NextMethod]]: nextMethod, [[Done]]: false }.
        generator.emit<Bytecode::Op::GetObjectPropertyIterator>(iterator, object);
    }
    // 7. Else,
    else {
        // a. Assert: iterationKind is iterate or async-iterate.
        // b. If iterationKind is async-iterate, let iteratorKind be async.
        // c. Else, let iteratorKind be sync.
        auto iterator_kind = iteration_kind == IterationKind::AsyncIterate ? IteratorHint::Async : IteratorHint::Sync;

        // d. Return ? GetIterator(exprValue, iteratorKind).
        generator.emit<Bytecode::Op::GetIterator>(iterator, object, iterator_kind);
    }

    result.iterator = iterator;
    return result;
}

// 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset
static Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> for_in_of_body_evaluation(Bytecode::Generator& generator, ASTNode const& node, Variant<NonnullRefPtr<ASTNode const>, NonnullRefPtr<BindingPattern const>> const& lhs, ASTNode const& body, ForInOfHeadEvaluationResult const& head_result, Vector<DeprecatedFlyString> const& label_set, Bytecode::BasicBlock& loop_end, Bytecode::BasicBlock& loop_update, IteratorHint iterator_kind = IteratorHint::Sync, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst = {})
{
    // 1. If iteratorKind is not present, set iteratorKind to sync.

    // 2. Let oldEnv be the running execution context's LexicalEnvironment.
    bool has_lexical_binding = false;

    // 3. Let V be undefined.
    auto completion_value = Bytecode::Operand(generator.allocate_register());
    generator.emit<Bytecode::Op::Mov>(completion_value, generator.add_constant(js_undefined()));

    // 4. Let destructuring be IsDestructuring of lhs.
    auto destructuring = head_result.is_destructuring;

    // 5. If destructuring is true and if lhsKind is assignment, then
    if (destructuring && head_result.lhs_kind == LHSKind::Assignment) {
        // a. Assert: lhs is a LeftHandSideExpression.
        // b. Let assignmentPattern be the AssignmentPattern that is covered by lhs.
        // FIXME: Implement this.
        return Bytecode::CodeGenerationError {
            &node,
            "Unimplemented: assignment destructuring in for/of"sv,
        };
    }
    // 6. Repeat,
    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { loop_update });
    generator.switch_to_basic_block(loop_update);
    generator.begin_continuable_scope(Bytecode::Label { loop_update }, label_set);

    // a. Let nextResult be ? Call(iteratorRecord.[[NextMethod]], iteratorRecord.[[Iterator]]).
    auto next_result = Bytecode::Operand(generator.allocate_register());
    generator.emit<Bytecode::Op::IteratorNext>(next_result, *head_result.iterator);

    // b. If iteratorKind is async, set nextResult to ? Await(nextResult).
    if (iterator_kind == IteratorHint::Async) {
        auto received_completion = Bytecode::Operand(generator.allocate_register());
        auto received_completion_type = Bytecode::Operand(generator.allocate_register());
        auto received_completion_value = Bytecode::Operand(generator.allocate_register());

        auto type_identifier = generator.intern_identifier("type");
        auto value_identifier = generator.intern_identifier("value");

        generator.emit<Bytecode::Op::Mov>(received_completion, Bytecode::Operand(Bytecode::Register(0)));
        auto new_result = generate_await(generator, next_result, received_completion, received_completion_type, received_completion_value, type_identifier, value_identifier);
        generator.emit<Bytecode::Op::Mov>(next_result, new_result);
    }

    // c. If Type(nextResult) is not Object, throw a TypeError exception.
    generator.emit<Bytecode::Op::ThrowIfNotObject>(next_result);

    // d. Let done be ? IteratorComplete(nextResult).
    auto done = Bytecode::Operand(generator.allocate_register());
    generator.emit_iterator_complete(done, next_result);

    // e. If done is true, return V.
    auto& loop_continue = generator.make_block();
    generator.emit<Bytecode::Op::JumpIf>(
        done,
        Bytecode::Label { loop_end },
        Bytecode::Label { loop_continue });
    generator.switch_to_basic_block(loop_continue);

    // f. Let nextValue be ? IteratorValue(nextResult).
    auto next_value = Bytecode::Operand(generator.allocate_register());
    generator.emit_iterator_value(next_value, next_result);

    // g. If lhsKind is either assignment or varBinding, then
    if (head_result.lhs_kind != LHSKind::LexicalBinding) {
        // i. If destructuring is false, then
        if (!destructuring) {
            // 1. Let lhsRef be the result of evaluating lhs. (It may be evaluated repeatedly.)
            // NOTE: We're skipping all the completion stuff that the spec does, as the unwinding mechanism will take case of doing that.
            if (head_result.lhs_kind == LHSKind::VarBinding) {
                auto& declaration = static_cast<VariableDeclaration const&>(*lhs.get<NonnullRefPtr<ASTNode const>>());
                VERIFY(declaration.declarations().size() == 1);
                TRY(assign_value_to_variable_declarator(generator, declaration.declarations().first(), declaration, next_value));
            } else {
                if (auto ptr = lhs.get_pointer<NonnullRefPtr<ASTNode const>>()) {
                    TRY(generator.emit_store_to_reference(**ptr, next_value));
                } else {
                    auto& binding_pattern = lhs.get<NonnullRefPtr<BindingPattern const>>();
                    TRY(generate_binding_pattern_bytecode(generator, *binding_pattern, Bytecode::Op::SetVariable::InitializationMode::Set, next_value, false));
                }
            }
        }
    }
    // h. Else,
    else {
        // i. Assert: lhsKind is lexicalBinding.
        // ii. Assert: lhs is a ForDeclaration.
        // iii. Let iterationEnv be NewDeclarativeEnvironment(oldEnv).
        // iv. Perform ForDeclarationBindingInstantiation of lhs with argument iterationEnv.
        // v. Set the running execution context's LexicalEnvironment to iterationEnv.

        // 14.7.5.4 Runtime Semantics: ForDeclarationBindingInstantiation, https://tc39.es/ecma262/#sec-runtime-semantics-fordeclarationbindinginstantiation
        // 1. Assert: environment is a declarative Environment Record.
        // NOTE: We just made it.
        auto& variable_declaration = static_cast<VariableDeclaration const&>(*lhs.get<NonnullRefPtr<ASTNode const>>());
        // 2. For each element name of the BoundNames of ForBinding, do
        // NOTE: Nothing in the callback throws an exception.

        auto has_non_local_variables = false;
        MUST(variable_declaration.for_each_bound_identifier([&](auto const& identifier) {
            if (!identifier.is_local())
                has_non_local_variables = true;
        }));

        if (has_non_local_variables) {
            generator.begin_variable_scope();
            has_lexical_binding = true;

            MUST(variable_declaration.for_each_bound_identifier([&](auto const& identifier) {
                if (identifier.is_local())
                    return;
                auto interned_identifier = generator.intern_identifier(identifier.string());
                // a. If IsConstantDeclaration of LetOrConst is true, then
                if (variable_declaration.is_constant_declaration()) {
                    // i. Perform ! environment.CreateImmutableBinding(name, true).
                    generator.emit<Bytecode::Op::CreateVariable>(interned_identifier, Bytecode::Op::EnvironmentMode::Lexical, true, false, true);
                }
                // b. Else,
                else {
                    // i. Perform ! environment.CreateMutableBinding(name, false).
                    generator.emit<Bytecode::Op::CreateVariable>(interned_identifier, Bytecode::Op::EnvironmentMode::Lexical, false);
                }
            }));
            // 3. Return unused.
            // NOTE: No need to do that as we've inlined this.
        }
        // vi. If destructuring is false, then
        if (!destructuring) {
            // 1. Assert: lhs binds a single name.
            // 2. Let lhsName be the sole element of BoundNames of lhs.
            auto lhs_name = variable_declaration.declarations().first()->target().get<NonnullRefPtr<Identifier const>>();
            // 3. Let lhsRef be ! ResolveBinding(lhsName).
            // NOTE: We're skipping all the completion stuff that the spec does, as the unwinding mechanism will take case of doing that.

            generator.emit_set_variable(*lhs_name, next_value, Bytecode::Op::SetVariable::InitializationMode::Initialize, Bytecode::Op::EnvironmentMode::Lexical);
        }
    }
    // i. If destructuring is false, then
    if (!destructuring) {
        // i. If lhsRef is an abrupt completion, then
        //     1. Let status be lhsRef.
        // ii. Else if lhsKind is lexicalBinding, then
        //     1. Let status be Completion(InitializeReferencedBinding(lhsRef, nextValue)).
        // iii. Else,
        //     1. Let status be Completion(PutValue(lhsRef, nextValue)).
        // NOTE: This is performed above.
    }
    //    j. Else,
    else {
        // FIXME: i. If lhsKind is assignment, then
        //           1. Let status be Completion(DestructuringAssignmentEvaluation of assignmentPattern with argument nextValue).

        //  ii. Else if lhsKind is varBinding, then
        //      1. Assert: lhs is a ForBinding.
        //      2. Let status be Completion(BindingInitialization of lhs with arguments nextValue and undefined).
        //  iii. Else,
        //      1. Assert: lhsKind is lexicalBinding.
        //      2. Assert: lhs is a ForDeclaration.
        //      3. Let status be Completion(ForDeclarationBindingInitialization of lhs with arguments nextValue and iterationEnv).
        if (head_result.lhs_kind == LHSKind::VarBinding || head_result.lhs_kind == LHSKind::LexicalBinding) {
            auto& declaration = static_cast<VariableDeclaration const&>(*lhs.get<NonnullRefPtr<ASTNode const>>());
            VERIFY(declaration.declarations().size() == 1);
            auto& binding_pattern = declaration.declarations().first()->target().get<NonnullRefPtr<BindingPattern const>>();
            (void)TRY(generate_binding_pattern_bytecode(
                generator,
                *binding_pattern,
                head_result.lhs_kind == LHSKind::VarBinding ? Bytecode::Op::SetVariable::InitializationMode::Set : Bytecode::Op::SetVariable::InitializationMode::Initialize,
                next_value,
                false));
        } else {
            return Bytecode::CodeGenerationError {
                &node,
                "Unimplemented: assignment destructuring in for/of"sv,
            };
        }
    }

    // FIXME: Implement iteration closure.
    // k. If status is an abrupt completion, then
    //     i. Set the running execution context's LexicalEnvironment to oldEnv.
    //     ii. If iteratorKind is async, return ? AsyncIteratorClose(iteratorRecord, status).
    //     iii. If iterationKind is enumerate, then
    //         1. Return ? status.
    //     iv. Else,
    //         1. Assert: iterationKind is iterate.
    //         2. Return ? IteratorClose(iteratorRecord, status).

    // l. Let result be the result of evaluating stmt.
    auto result = TRY(body.generate_bytecode(generator));

    // m. Set the running execution context's LexicalEnvironment to oldEnv.
    if (has_lexical_binding)
        generator.end_variable_scope();
    generator.end_continuable_scope();
    generator.end_breakable_scope();

    // NOTE: If we're here, then the loop definitely continues.
    // n. If LoopContinues(result, labelSet) is false, then
    //     i. If iterationKind is enumerate, then
    //         1. Return ? UpdateEmpty(result, V).
    //     ii. Else,
    //         1. Assert: iterationKind is iterate.
    //         2. Set status to Completion(UpdateEmpty(result, V)).
    //         3. If iteratorKind is async, return ? AsyncIteratorClose(iteratorRecord, status).
    //         4. Return ? IteratorClose(iteratorRecord, status).
    // o. If result.[[Value]] is not empty, set V to result.[[Value]].

    // The body can contain an unconditional block terminator (e.g. return, throw), so we have to check for that before generating the Jump.
    if (!generator.is_current_block_terminated()) {
        if (result.has_value())
            generator.emit<Bytecode::Op::Mov>(completion_value, *result);

        generator.emit<Bytecode::Op::Jump>(Bytecode::Label { loop_update });
    }

    generator.switch_to_basic_block(loop_end);
    return completion_value;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ForInStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generate_labelled_evaluation(generator, {});
}

// 14.7.5.5 Runtime Semantics: ForInOfLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forinofloopevaluation
Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ForInStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<DeprecatedFlyString> const& label_set, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    auto& loop_end = generator.make_block();
    auto& loop_update = generator.make_block();
    generator.begin_breakable_scope(Bytecode::Label { loop_end }, label_set);

    auto head_result = TRY(for_in_of_head_evaluation(generator, IterationKind::Enumerate, m_lhs, m_rhs));
    return for_in_of_body_evaluation(generator, *this, m_lhs, body(), head_result, label_set, loop_end, loop_update);
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ForOfStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generate_labelled_evaluation(generator, {});
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ForOfStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<DeprecatedFlyString> const& label_set, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    auto& loop_end = generator.make_block();
    auto& loop_update = generator.make_block();
    generator.begin_breakable_scope(Bytecode::Label { loop_end }, label_set);

    auto head_result = TRY(for_in_of_head_evaluation(generator, IterationKind::Iterate, m_lhs, m_rhs));
    return for_in_of_body_evaluation(generator, *this, m_lhs, body(), head_result, label_set, loop_end, loop_update);
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ForAwaitOfStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    return generate_labelled_evaluation(generator, {});
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ForAwaitOfStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<DeprecatedFlyString> const& label_set, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    auto& loop_end = generator.make_block();
    auto& loop_update = generator.make_block();
    generator.begin_breakable_scope(Bytecode::Label { loop_end }, label_set);

    auto head_result = TRY(for_in_of_head_evaluation(generator, IterationKind::AsyncIterate, m_lhs, m_rhs));
    return for_in_of_body_evaluation(generator, *this, m_lhs, m_body, head_result, label_set, loop_end, loop_update, IteratorHint::Async);
}

// 13.3.12.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-meta-properties-runtime-semantics-evaluation
Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> MetaProperty::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    // NewTarget : new . target
    if (m_type == MetaProperty::Type::NewTarget) {
        // 1. Return GetNewTarget().
        auto dst = choose_dst(generator, preferred_dst);
        generator.emit<Bytecode::Op::GetNewTarget>(dst);
        return dst;
    }

    // ImportMeta : import . meta
    if (m_type == MetaProperty::Type::ImportMeta) {
        auto dst = choose_dst(generator, preferred_dst);
        generator.emit<Bytecode::Op::GetImportMeta>(dst);
        return dst;
    }

    VERIFY_NOT_REACHED();
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ClassFieldInitializerStatement::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto value = TRY(generator.emit_named_evaluation_if_anonymous_function(*m_expression, generator.intern_identifier(m_class_field_identifier_name), preferred_dst));
    generator.perform_needed_unwinds<Bytecode::Op::Return>();
    generator.emit<Bytecode::Op::Return>(value);
    return value;
}

static Bytecode::CodeGenerationErrorOr<void> generate_optional_chain(Bytecode::Generator& generator, OptionalChain const& optional_chain, Bytecode::Operand current_value, Bytecode::Operand current_base, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst)
{
    Optional<Bytecode::Operand> new_current_value;
    if (is<MemberExpression>(optional_chain.base())) {
        auto& member_expression = static_cast<MemberExpression const&>(optional_chain.base());
        auto base_and_value = TRY(get_base_and_value_from_member_expression(generator, member_expression));
        new_current_value = base_and_value.value;
        generator.emit<Bytecode::Op::Mov>(current_base, base_and_value.base);
    } else if (is<OptionalChain>(optional_chain.base())) {
        auto& sub_optional_chain = static_cast<OptionalChain const&>(optional_chain.base());
        TRY(generate_optional_chain(generator, sub_optional_chain, current_value, current_base));
        new_current_value = current_value;
    } else {
        new_current_value = TRY(optional_chain.base().generate_bytecode(generator)).value();
    }

    generator.emit<Bytecode::Op::Mov>(current_value, *new_current_value);

    auto& load_undefined_and_jump_to_end_block = generator.make_block();
    auto& end_block = generator.make_block();

    for (auto& reference : optional_chain.references()) {
        auto is_optional = reference.visit([](auto& ref) { return ref.mode; }) == OptionalChain::Mode::Optional;
        if (is_optional) {
            auto& not_nullish_block = generator.make_block();
            generator.emit<Bytecode::Op::JumpNullish>(
                current_value,
                Bytecode::Label { load_undefined_and_jump_to_end_block },
                Bytecode::Label { not_nullish_block });
            generator.switch_to_basic_block(not_nullish_block);
        }

        TRY(reference.visit(
            [&](OptionalChain::Call const& call) -> Bytecode::CodeGenerationErrorOr<void> {
                auto arguments = TRY(arguments_to_array_for_call(generator, call.arguments)).value();
                generator.emit<Bytecode::Op::CallWithArgumentArray>(Bytecode::Op::CallType::Call, current_value, current_value, current_base, arguments);
                generator.emit<Bytecode::Op::Mov>(current_base, generator.add_constant(js_undefined()));
                return {};
            },
            [&](OptionalChain::ComputedReference const& ref) -> Bytecode::CodeGenerationErrorOr<void> {
                generator.emit<Bytecode::Op::Mov>(current_base, current_value);
                auto property = TRY(ref.expression->generate_bytecode(generator)).value();
                generator.emit<Bytecode::Op::GetByValue>(current_value, current_value, property);
                return {};
            },
            [&](OptionalChain::MemberReference const& ref) -> Bytecode::CodeGenerationErrorOr<void> {
                generator.emit<Bytecode::Op::Mov>(current_base, current_value);
                generator.emit_get_by_id(current_value, current_value, generator.intern_identifier(ref.identifier->string()));
                return {};
            },
            [&](OptionalChain::PrivateMemberReference const& ref) -> Bytecode::CodeGenerationErrorOr<void> {
                generator.emit<Bytecode::Op::Mov>(current_base, current_value);
                generator.emit<Bytecode::Op::GetPrivateById>(current_value, current_value, generator.intern_identifier(ref.private_identifier->string()));

                return {};
            }));
    }

    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { end_block });

    generator.switch_to_basic_block(load_undefined_and_jump_to_end_block);
    generator.emit<Bytecode::Op::Mov>(current_value, generator.add_constant(js_undefined()));
    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { end_block });

    generator.switch_to_basic_block(end_block);
    return {};
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> OptionalChain::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto current_base = Bytecode::Operand(generator.allocate_register());
    auto current_value = choose_dst(generator, preferred_dst);
    generator.emit<Bytecode::Op::Mov>(current_base, generator.add_constant(js_undefined()));
    TRY(generate_optional_chain(generator, *this, current_value, current_base));
    return current_value;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ImportCall::generate_bytecode(Bytecode::Generator& generator, Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    auto specifier = TRY(m_specifier->generate_bytecode(generator)).value();

    Optional<Bytecode::Operand> options;
    if (m_options) {
        options = TRY(m_options->generate_bytecode(generator)).value();
    } else {
        options = generator.add_constant(js_undefined());
    }
    auto dst = choose_dst(generator, preferred_dst);
    generator.emit<Bytecode::Op::ImportCall>(dst, specifier, *options);
    return dst;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ExportStatement::generate_bytecode(Bytecode::Generator& generator, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    Bytecode::Generator::SourceLocationScope scope(generator, *this);
    if (!is_default_export()) {
        if (m_statement) {
            return m_statement->generate_bytecode(generator);
        }
        return Optional<Bytecode::Operand> {};
    }

    VERIFY(m_statement);

    if (is<FunctionDeclaration>(*m_statement) || is<ClassDeclaration>(*m_statement)) {
        return m_statement->generate_bytecode(generator);
    }

    if (is<ClassExpression>(*m_statement)) {
        auto value = TRY(generator.emit_named_evaluation_if_anonymous_function(static_cast<ClassExpression const&>(*m_statement), generator.intern_identifier("default"sv))).value();

        if (!static_cast<ClassExpression const&>(*m_statement).has_name()) {
            generator.emit<Bytecode::Op::SetVariable>(
                generator.intern_identifier(ExportStatement::local_name_for_default),
                value,
                generator.next_environment_variable_cache(),
                Bytecode::Op::SetVariable::InitializationMode::Initialize);
        }

        return value;
    }

    // ExportDeclaration : export default AssignmentExpression ;
    VERIFY(is<Expression>(*m_statement));
    auto value = TRY(generator.emit_named_evaluation_if_anonymous_function(static_cast<Expression const&>(*m_statement), generator.intern_identifier("default"sv))).value();
    generator.emit<Bytecode::Op::SetVariable>(
        generator.intern_identifier(ExportStatement::local_name_for_default),
        value,
        generator.next_environment_variable_cache(),
        Bytecode::Op::SetVariable::InitializationMode::Initialize);
    return value;
}

Bytecode::CodeGenerationErrorOr<Optional<Bytecode::Operand>> ImportStatement::generate_bytecode(Bytecode::Generator&, [[maybe_unused]] Optional<Bytecode::Operand> preferred_dst) const
{
    return Optional<Bytecode::Operand> {};
}

}
