/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2021, Marcin Gasperowicz <xnooga@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Find.h>
#include <AK/Format.h>
#include <LibJS/AST.h>
#include <LibJS/Bytecode/Generator.h>
#include <LibJS/Bytecode/Instruction.h>
#include <LibJS/Bytecode/Op.h>
#include <LibJS/Bytecode/Register.h>
#include <LibJS/Bytecode/StringTable.h>
#include <LibJS/Runtime/Environment.h>

namespace JS {

Bytecode::CodeGenerationErrorOr<void> ASTNode::generate_bytecode(Bytecode::Generator&) const
{
    return Bytecode::CodeGenerationError {
        this,
        "Missing generate_bytecode()"sv,
    };
}

Bytecode::CodeGenerationErrorOr<void> ScopeNode::generate_bytecode(Bytecode::Generator& generator) const
{
    Optional<Bytecode::CodeGenerationError> maybe_error;
    size_t pushed_scope_count = 0;
    auto const failing_completion = Completion(Completion::Type::Throw, {}, {});

    // Note: SwitchStatement has its own codegen, but still calls into this function to handle the scoping of the switch body.
    auto is_switch_statement = is<SwitchStatement>(*this);
    if (is<BlockStatement>(*this) || is_switch_statement) {
        // Perform the steps of BlockDeclarationInstantiation.
        if (has_lexical_declarations()) {
            generator.begin_variable_scope(Bytecode::Generator::BindingMode::Lexical, Bytecode::Generator::SurroundingScopeKind::Block);
            pushed_scope_count++;
        }

        (void)for_each_lexically_scoped_declaration([&](Declaration const& declaration) -> ThrowCompletionOr<void> {
            auto is_constant_declaration = declaration.is_constant_declaration();
            declaration.for_each_bound_name([&](auto const& name) {
                auto index = generator.intern_identifier(name);
                // NOTE: BlockDeclarationInstantiation takes as input the new lexical environment that was created and checks if there is a binding for the current name only in this new scope.
                //       For example: `{ let a = 1; { let a = 2; } }`. The second `a` will shadow the first `a` instead of re-initializing or setting it.
                if (is_constant_declaration || !generator.has_binding_in_current_scope(index)) {
                    generator.register_binding(index);
                    generator.emit<Bytecode::Op::CreateVariable>(index, Bytecode::Op::EnvironmentMode::Lexical, is_constant_declaration);
                }
            });

            if (is<FunctionDeclaration>(declaration)) {
                auto& function_declaration = static_cast<FunctionDeclaration const&>(declaration);
                auto const& name = function_declaration.name();
                auto index = generator.intern_identifier(name);
                generator.emit<Bytecode::Op::NewFunction>(function_declaration);
                generator.emit<Bytecode::Op::SetVariable>(index, Bytecode::Op::SetVariable::InitializationMode::InitializeOrSet);
            }

            return {};
        });

        if (is_switch_statement)
            return {};

    } else if (is<Program>(*this)) {
        // Perform the steps of GlobalDeclarationInstantiation.
        generator.begin_variable_scope(Bytecode::Generator::BindingMode::Global, Bytecode::Generator::SurroundingScopeKind::Global);
        pushed_scope_count++;

        // 1. Let lexNames be the LexicallyDeclaredNames of script.
        // 2. Let varNames be the VarDeclaredNames of script.
        // 3. For each element name of lexNames, do
        (void)for_each_lexically_declared_name([&](auto const& name) -> ThrowCompletionOr<void> {
            auto identifier = generator.intern_identifier(name);
            // a. If env.HasVarDeclaration(name) is true, throw a SyntaxError exception.
            // b. If env.HasLexicalDeclaration(name) is true, throw a SyntaxError exception.
            if (generator.has_binding(identifier)) {
                // FIXME: Throw an actual SyntaxError instance.
                generator.emit<Bytecode::Op::NewString>(generator.intern_string(String::formatted("SyntaxError: toplevel variable already declared: {}", name)));
                generator.emit<Bytecode::Op::Throw>();
                return {};
            }

            // FIXME: c. If hasRestrictedGlobalProperty is true, throw a SyntaxError exception.
            //        d. If hasRestrictedGlobal is true, throw a SyntaxError exception.
            return {};
        });

        // 4. For each element name of varNames, do
        (void)for_each_var_declared_name([&](auto const& name) -> ThrowCompletionOr<void> {
            auto identifier = generator.intern_identifier(name);
            // a. If env.HasLexicalDeclaration(name) is true, throw a SyntaxError exception.
            if (generator.has_binding(identifier)) {
                // FIXME: Throw an actual SyntaxError instance.
                generator.emit<Bytecode::Op::NewString>(generator.intern_string(String::formatted("SyntaxError: toplevel variable already declared: {}", name)));
                generator.emit<Bytecode::Op::Throw>();
            }
            return {};
        });

        // 5. Let varDeclarations be the VarScopedDeclarations of script.
        // 6. Let functionsToInitialize be a new empty List.
        Vector<FunctionDeclaration const&> functions_to_initialize;

        // 7. Let declaredFunctionNames be a new empty List.
        HashTable<FlyString> declared_function_names;

        // 8. For each element d of varDeclarations, in reverse List order, do
        (void)for_each_var_function_declaration_in_reverse_order([&](FunctionDeclaration const& function) -> ThrowCompletionOr<void> {
            // a. If d is neither a VariableDeclaration nor a ForBinding nor a BindingIdentifier, then
            // i. Assert: d is either a FunctionDeclaration, a GeneratorDeclaration, an AsyncFunctionDeclaration, or an AsyncGeneratorDeclaration.
            // Note: This is checked in for_each_var_function_declaration_in_reverse_order.
            // ii. NOTE: If there are multiple function declarations for the same name, the last declaration is used.
            // iii. Let fn be the sole element of the BoundNames of d.

            // iv. If fn is not an element of declaredFunctionNames, then
            if (declared_function_names.set(function.name()) != AK::HashSetResult::InsertedNewEntry)
                return {};

            // FIXME: 1. Let fnDefinable be ? env.CanDeclareGlobalFunction(fn).
            // FIXME: 2. If fnDefinable is false, throw a TypeError exception.

            // 3. Append fn to declaredFunctionNames.
            // Note: Already done in step iv. above.

            // 4. Insert d as the first element of functionsToInitialize.
            functions_to_initialize.prepend(function);
            return {};
        });

        // 9. Let declaredVarNames be a new empty List.
        HashTable<FlyString> declared_var_names;

        // 10. For each element d of varDeclarations, do
        (void)for_each_var_scoped_variable_declaration([&](Declaration const& declaration) {
            // a. If d is a VariableDeclaration, a ForBinding, or a BindingIdentifier, then
            // Note: This is done in for_each_var_scoped_variable_declaration.

            // i. For each String vn of the BoundNames of d, do
            return declaration.for_each_bound_name([&](auto const& name) -> ThrowCompletionOr<void> {
                // 1. If vn is not an element of declaredFunctionNames, then
                if (declared_function_names.contains(name))
                    return {};

                // FIXME: a. Let vnDefinable be ? env.CanDeclareGlobalVar(vn).
                // FIXME: b. If vnDefinable is false, throw a TypeError exception.

                // c. If vn is not an element of declaredVarNames, then
                // i. Append vn to declaredVarNames.
                declared_var_names.set(name);
                return {};
            });
        });

        // 11. NOTE: No abnormal terminations occur after this algorithm step if the global object is an ordinary object. However, if the global object is a Proxy exotic object it may exhibit behaviours that cause abnormal terminations in some of the following steps.
        // 12. NOTE: Annex B.3.2.2 adds additional steps at this point.

        // 12. Let strict be IsStrict of script.
        // 13. If strict is false, then
        if (!verify_cast<Program>(*this).is_strict_mode()) {
            // a. Let declaredFunctionOrVarNames be the list-concatenation of declaredFunctionNames and declaredVarNames.
            // b. For each FunctionDeclaration f that is directly contained in the StatementList of a Block, CaseClause, or DefaultClause Contained within script, do
            (void)for_each_function_hoistable_with_annexB_extension([&](FunctionDeclaration& function_declaration) {
                // i. Let F be StringValue of the BindingIdentifier of f.
                auto& function_name = function_declaration.name();

                // ii. If replacing the FunctionDeclaration f with a VariableStatement that has F as a BindingIdentifier would not produce any Early Errors for script, then
                // Note: This step is already performed during parsing and for_each_function_hoistable_with_annexB_extension so this always passes here.

                // 1. If env.HasLexicalDeclaration(F) is false, then
                auto index = generator.intern_identifier(function_name);
                if (generator.has_binding(index, Bytecode::Generator::BindingMode::Lexical))
                    return;

                // FIXME: a. Let fnDefinable be ? env.CanDeclareGlobalVar(F).
                // b. If fnDefinable is true, then
                // i. NOTE: A var binding for F is only instantiated here if it is neither a VarDeclaredName nor the name of another FunctionDeclaration.
                // ii. If declaredFunctionOrVarNames does not contain F, then
                if (!declared_function_names.contains(function_name) && !declared_var_names.contains(function_name)) {
                    // i. Perform ? env.CreateGlobalVarBinding(F, false).
                    generator.emit<Bytecode::Op::CreateVariable>(index, Bytecode::Op::EnvironmentMode::Var, false, true);

                    // ii. Append F to declaredFunctionOrVarNames.
                    declared_function_names.set(function_name);
                }

                // iii. When the FunctionDeclaration f is evaluated, perform the following steps in place of the FunctionDeclaration Evaluation algorithm provided in 15.2.6:
                //     i. Let genv be the running execution context's VariableEnvironment.
                //     ii. Let benv be the running execution context's LexicalEnvironment.
                //     iii. Let fobj be ! benv.GetBindingValue(F, false).
                //     iv. Perform ? genv.SetMutableBinding(F, fobj, false).
                //     v. Return unused.
                function_declaration.set_should_do_additional_annexB_steps();
            });
        }

        // 15. For each element d of lexDeclarations, do
        (void)for_each_lexically_scoped_declaration([&](Declaration const& declaration) -> ThrowCompletionOr<void> {
            // a. NOTE: Lexically declared names are only instantiated here but not initialized.
            // b. For each element dn of the BoundNames of d, do
            return declaration.for_each_bound_name([&](auto const& name) -> ThrowCompletionOr<void> {
                auto identifier = generator.intern_identifier(name);
                // i. If IsConstantDeclaration of d is true, then
                generator.register_binding(identifier);
                if (declaration.is_constant_declaration()) {
                    // 1. Perform ? env.CreateImmutableBinding(dn, true).
                    generator.emit<Bytecode::Op::CreateVariable>(identifier, Bytecode::Op::EnvironmentMode::Lexical, true);
                } else {
                    // ii. Else,
                    // 1. Perform ? env.CreateMutableBinding(dn, false).
                    generator.emit<Bytecode::Op::CreateVariable>(identifier, Bytecode::Op::EnvironmentMode::Lexical, false);
                }

                return {};
            });
        });

        // 16. For each Parse Node f of functionsToInitialize, do
        for (auto& function_declaration : functions_to_initialize) {
            // FIXME: Do this more correctly.
            // a. Let fn be the sole element of the BoundNames of f.
            // b. Let fo be InstantiateFunctionObject of f with arguments env and privateEnv.
            generator.emit<Bytecode::Op::NewFunction>(function_declaration);

            // c. Perform ? env.CreateGlobalFunctionBinding(fn, fo, false).
            auto const& name = function_declaration.name();
            auto index = generator.intern_identifier(name);
            if (!generator.has_binding(index)) {
                generator.register_binding(index, Bytecode::Generator::BindingMode::Var);
                generator.emit<Bytecode::Op::CreateVariable>(index, Bytecode::Op::EnvironmentMode::Lexical, false);
            }
            generator.emit<Bytecode::Op::SetVariable>(index, Bytecode::Op::SetVariable::InitializationMode::Initialize);
        }

        // 17. For each String vn of declaredVarNames, do
        for (auto& var_name : declared_var_names) {
            // a. Perform ? env.CreateGlobalVarBinding(vn, false).
            auto index = generator.intern_identifier(var_name);
            generator.register_binding(index, Bytecode::Generator::BindingMode::Var);
            generator.emit<Bytecode::Op::CreateVariable>(index, Bytecode::Op::EnvironmentMode::Var, false, true);
        }
    } else {
        // Perform the steps of FunctionDeclarationInstantiation.
        generator.begin_variable_scope(Bytecode::Generator::BindingMode::Var, Bytecode::Generator::SurroundingScopeKind::Function);
        pushed_scope_count++;
        if (has_lexical_declarations()) {
            generator.begin_variable_scope(Bytecode::Generator::BindingMode::Lexical, Bytecode::Generator::SurroundingScopeKind::Function);
            pushed_scope_count++;
        }

        // FIXME: Implement this boi correctly.
        (void)for_each_lexically_scoped_declaration([&](Declaration const& declaration) -> ThrowCompletionOr<void> {
            auto is_constant_declaration = declaration.is_constant_declaration();
            declaration.for_each_bound_name([&](auto const& name) {
                auto index = generator.intern_identifier(name);
                if (is_constant_declaration || !generator.has_binding(index)) {
                    generator.register_binding(index);
                    generator.emit<Bytecode::Op::CreateVariable>(index, Bytecode::Op::EnvironmentMode::Lexical, is_constant_declaration);
                }
            });

            if (is<FunctionDeclaration>(declaration)) {
                auto& function_declaration = static_cast<FunctionDeclaration const&>(declaration);
                if (auto result = function_declaration.generate_bytecode(generator); result.is_error()) {
                    maybe_error = result.release_error();
                    // To make `for_each_lexically_scoped_declaration` happy.
                    return failing_completion;
                }
                auto const& name = function_declaration.name();
                auto index = generator.intern_identifier(name);
                if (!generator.has_binding(index)) {
                    generator.register_binding(index);
                    generator.emit<Bytecode::Op::CreateVariable>(index, Bytecode::Op::EnvironmentMode::Lexical, false);
                }
                generator.emit<Bytecode::Op::SetVariable>(index, Bytecode::Op::SetVariable::InitializationMode::InitializeOrSet);
            }

            return {};
        });
    }

    if (maybe_error.has_value())
        return maybe_error.release_value();

    for (auto& child : children()) {
        TRY(child.generate_bytecode(generator));
        if (generator.is_current_block_terminated())
            break;
    }

    for (size_t i = 0; i < pushed_scope_count; ++i)
        generator.end_variable_scope();

    return {};
}

Bytecode::CodeGenerationErrorOr<void> EmptyStatement::generate_bytecode(Bytecode::Generator&) const
{
    return {};
}

Bytecode::CodeGenerationErrorOr<void> ExpressionStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    return m_expression->generate_bytecode(generator);
}

Bytecode::CodeGenerationErrorOr<void> BinaryExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    TRY(m_lhs->generate_bytecode(generator));
    auto lhs_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(lhs_reg);

    TRY(m_rhs->generate_bytecode(generator));

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
    return {};
}

Bytecode::CodeGenerationErrorOr<void> LogicalExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    TRY(m_lhs->generate_bytecode(generator));

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
    TRY(m_rhs->generate_bytecode(generator));

    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { end_block },
        {});

    generator.switch_to_basic_block(end_block);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> UnaryExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    if (m_op == UnaryOp::Delete)
        return generator.emit_delete_reference(m_lhs);

    // Typeof needs some special handling for when the LHS is an Identifier. Namely, it shouldn't throw on unresolvable references, but instead return "undefined".
    if (m_op != UnaryOp::Typeof)
        TRY(m_lhs->generate_bytecode(generator));

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
        if (is<Identifier>(*m_lhs)) {
            auto& identifier = static_cast<Identifier const&>(*m_lhs);
            generator.emit<Bytecode::Op::TypeofVariable>(generator.intern_identifier(identifier.string()));
            break;
        }

        TRY(m_lhs->generate_bytecode(generator));
        generator.emit<Bytecode::Op::Typeof>();
        break;
    case UnaryOp::Void:
        generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
        break;
    case UnaryOp::Delete: // Delete is implemented above.
    default:
        VERIFY_NOT_REACHED();
    }

    return {};
}

Bytecode::CodeGenerationErrorOr<void> NumericLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::LoadImmediate>(m_value);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> BooleanLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::LoadImmediate>(Value(m_value));
    return {};
}

Bytecode::CodeGenerationErrorOr<void> NullLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::LoadImmediate>(js_null());
    return {};
}

Bytecode::CodeGenerationErrorOr<void> BigIntLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewBigInt>(Crypto::SignedBigInteger::from_base(10, m_value.substring(0, m_value.length() - 1)));
    return {};
}

Bytecode::CodeGenerationErrorOr<void> StringLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewString>(generator.intern_string(m_value));
    return {};
}

Bytecode::CodeGenerationErrorOr<void> RegExpLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    auto source_index = generator.intern_string(m_pattern);
    auto flags_index = generator.intern_string(m_flags);
    generator.emit<Bytecode::Op::NewRegExp>(source_index, flags_index);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> Identifier::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::GetVariable>(generator.intern_identifier(m_string));
    return {};
}

static Bytecode::CodeGenerationErrorOr<void> arguments_to_array_for_call(Bytecode::Generator& generator, Span<CallExpression::Argument const> arguments)
{

    if (arguments.is_empty()) {
        generator.emit<Bytecode::Op::NewArray>();
        return {};
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
        TRY(it->value->generate_bytecode(generator));
        generator.emit<Bytecode::Op::Store>(reg);
    }

    if (first_spread.index() != 0)
        generator.emit_with_extra_register_slots<Bytecode::Op::NewArray>(2u, AK::Array { args_start_reg, Bytecode::Register { args_start_reg.index() + static_cast<u32>(first_spread.index() - 1) } });
    else
        generator.emit<Bytecode::Op::NewArray>();

    if (first_spread != arguments.end()) {
        auto array_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(array_reg);
        for (auto it = first_spread; it != arguments.end(); ++it) {
            TRY(it->value->generate_bytecode(generator));
            generator.emit<Bytecode::Op::Append>(array_reg, it->is_spread);
        }
        generator.emit<Bytecode::Op::Load>(array_reg);
    }

    return {};
}

Bytecode::CodeGenerationErrorOr<void> SuperCall::generate_bytecode(Bytecode::Generator& generator) const
{
    if (m_is_synthetic == IsPartOfSyntheticConstructor::Yes) {
        // NOTE: This is the case where we have a fake constructor(...args) { super(...args); } which
        //       shouldn't call @@iterator of %Array.prototype%.
        VERIFY(m_arguments.size() == 1);
        VERIFY(m_arguments[0].is_spread);
        auto const& argument = m_arguments[0];
        // This generates a single argument, which will be implicitly passed in accumulator
        MUST(argument.value->generate_bytecode(generator));
    } else {
        TRY(arguments_to_array_for_call(generator, m_arguments));
    }

    generator.emit<Bytecode::Op::SuperCall>(m_is_synthetic == IsPartOfSyntheticConstructor::Yes);

    return {};
}

static Bytecode::CodeGenerationErrorOr<void> generate_binding_pattern_bytecode(Bytecode::Generator& generator, BindingPattern const& pattern, Bytecode::Op::SetVariable::InitializationMode, Bytecode::Register const& value_reg);

Bytecode::CodeGenerationErrorOr<void> AssignmentExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    if (m_op == AssignmentOp::Assignment) {
        // AssignmentExpression : LeftHandSideExpression = AssignmentExpression
        return m_lhs.visit(
            // 1. If LeftHandSideExpression is neither an ObjectLiteral nor an ArrayLiteral, then
            [&](NonnullRefPtr<Expression> const& lhs) -> Bytecode::CodeGenerationErrorOr<void> {
                // a. Let lref be the result of evaluating LeftHandSideExpression.
                // b. ReturnIfAbrupt(lref).
                Optional<Bytecode::Register> base_object_register;
                Optional<Bytecode::Register> computed_property_register;

                if (is<MemberExpression>(*lhs)) {
                    auto& expression = static_cast<MemberExpression const&>(*lhs);
                    TRY(expression.object().generate_bytecode(generator));

                    base_object_register = generator.allocate_register();
                    generator.emit<Bytecode::Op::Store>(*base_object_register);

                    if (expression.is_computed()) {
                        TRY(expression.property().generate_bytecode(generator));
                        computed_property_register = generator.allocate_register();
                        generator.emit<Bytecode::Op::Store>(*computed_property_register);

                        // To be continued later with PutByValue.
                    } else if (expression.property().is_identifier()) {
                        // Do nothing, this will be handled by PutById later.
                    } else {
                        return Bytecode::CodeGenerationError {
                            &expression,
                            "Unimplemented non-computed member expression"sv
                        };
                    }
                } else if (is<Identifier>(*lhs)) {
                    // NOTE: For Identifiers, we cannot perform GetVariable and then write into the reference it retrieves, only SetVariable can do this.
                    // FIXME: However, this breaks spec as we are doing variable lookup after evaluating the RHS. This is observable in an object environment, where we visibly perform HasOwnProperty and Get(@@unscopables) on the binded object.
                } else {
                    TRY(lhs->generate_bytecode(generator));
                }

                // FIXME: c. If IsAnonymousFunctionDefinition(AssignmentExpression) and IsIdentifierRef of LeftHandSideExpression are both true, then
                //           i. Let rval be ? NamedEvaluation of AssignmentExpression with argument lref.[[ReferencedName]].

                // d. Else,
                // i. Let rref be the result of evaluating AssignmentExpression.
                // ii. Let rval be ? GetValue(rref).
                TRY(m_rhs->generate_bytecode(generator));

                // e. Perform ? PutValue(lref, rval).
                if (is<Identifier>(*lhs)) {
                    auto& identifier = static_cast<Identifier const&>(*lhs);
                    generator.emit<Bytecode::Op::SetVariable>(generator.intern_identifier(identifier.string()));
                } else if (is<MemberExpression>(*lhs)) {
                    auto& expression = static_cast<MemberExpression const&>(*lhs);

                    if (expression.is_computed()) {
                        generator.emit<Bytecode::Op::PutByValue>(*base_object_register, *computed_property_register);
                    } else if (expression.property().is_identifier()) {
                        auto identifier_table_ref = generator.intern_identifier(verify_cast<Identifier>(expression.property()).string());
                        generator.emit<Bytecode::Op::PutById>(*base_object_register, identifier_table_ref);
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
                // NOTE: This is already in the accumulator.
                return {};
            },
            // 2. Let assignmentPattern be the AssignmentPattern that is covered by LeftHandSideExpression.
            [&](NonnullRefPtr<BindingPattern> const& pattern) -> Bytecode::CodeGenerationErrorOr<void> {
                // 3. Let rref be the result of evaluating AssignmentExpression.
                // 4. Let rval be ? GetValue(rref).
                TRY(m_rhs->generate_bytecode(generator));
                auto value_register = generator.allocate_register();
                generator.emit<Bytecode::Op::Store>(value_register);

                // 5. Perform ? DestructuringAssignmentEvaluation of assignmentPattern with argument rval.
                TRY(generate_binding_pattern_bytecode(generator, pattern, Bytecode::Op::SetVariable::InitializationMode::Set, value_register));

                // 6. Return rval.
                generator.emit<Bytecode::Op::Load>(value_register);
                return {};
            });
    }

    VERIFY(m_lhs.has<NonnullRefPtr<Expression>>());
    auto& lhs = m_lhs.get<NonnullRefPtr<Expression>>();

    TRY(generator.emit_load_from_reference(lhs));

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
    TRY(m_rhs->generate_bytecode(generator));

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
        return Bytecode::CodeGenerationError {
            this,
            "Unimplemented operation"sv,
        };
    }

    TRY(generator.emit_store_to_reference(lhs));

    if (end_block_ptr) {
        generator.emit<Bytecode::Op::Jump>().set_targets(
            Bytecode::Label { *end_block_ptr },
            {});

        generator.switch_to_basic_block(*end_block_ptr);
    }

    return {};
}

// 14.13.3 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-labelled-statements-runtime-semantics-evaluation
//  LabelledStatement : LabelIdentifier : LabelledItem
Bytecode::CodeGenerationErrorOr<void> LabelledStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    // Return ? LabelledEvaluation of this LabelledStatement with argument « ».
    return generate_labelled_evaluation(generator, {});
}

// 14.13.4 Runtime Semantics: LabelledEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-labelledevaluation
// LabelledStatement : LabelIdentifier : LabelledItem
Bytecode::CodeGenerationErrorOr<void> LabelledStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<FlyString> const& label_set) const
{
    // Convert the m_labelled_item NNRP to a reference early so we don't have to do it every single time we want to use it.
    auto const& labelled_item = *m_labelled_item;

    // 1. Let label be the StringValue of LabelIdentifier.
    // NOTE: Not necessary, this is m_label.

    // 2. Let newLabelSet be the list-concatenation of labelSet and « label ».
    // FIXME: Avoid copy here.
    auto new_label_set = label_set;
    new_label_set.append(m_label);

    // 3. Let stmtResult be LabelledEvaluation of LabelledItem with argument newLabelSet.
    // NOTE: stmtResult will be in the accumulator after running the generated bytecode.
    if (is<IterationStatement>(labelled_item)) {
        auto const& iteration_statement = static_cast<IterationStatement const&>(labelled_item);
        TRY(iteration_statement.generate_labelled_evaluation(generator, new_label_set));
    } else if (is<SwitchStatement>(labelled_item)) {
        auto const& switch_statement = static_cast<SwitchStatement const&>(labelled_item);
        TRY(switch_statement.generate_labelled_evaluation(generator, new_label_set));
    } else if (is<LabelledStatement>(labelled_item)) {
        auto const& labelled_statement = static_cast<LabelledStatement const&>(labelled_item);
        TRY(labelled_statement.generate_labelled_evaluation(generator, new_label_set));
    } else {
        auto& labelled_break_block = generator.make_block();

        // NOTE: We do not need a continuable scope as `continue;` is not allowed outside of iteration statements, throwing a SyntaxError in the parser.
        generator.begin_breakable_scope(Bytecode::Label { labelled_break_block }, new_label_set);
        TRY(labelled_item.generate_bytecode(generator));
        generator.end_breakable_scope();

        if (!generator.is_current_block_terminated()) {
            generator.emit<Bytecode::Op::Jump>().set_targets(
                Bytecode::Label { labelled_break_block },
                {});
        }

        generator.switch_to_basic_block(labelled_break_block);
    }

    // 4. If stmtResult.[[Type]] is break and SameValue(stmtResult.[[Target]], label) is true, then
    //    a. Set stmtResult to NormalCompletion(stmtResult.[[Value]]).
    // NOTE: These steps are performed by making labelled break jump straight to the appropriate break block, which preserves the statement result's value in the accumulator.

    // 5. Return Completion(stmtResult).
    // NOTE: This is in the accumulator.
    return {};
}

Bytecode::CodeGenerationErrorOr<void> IterationStatement::generate_labelled_evaluation(Bytecode::Generator&, Vector<FlyString> const&) const
{
    return Bytecode::CodeGenerationError {
        this,
        "Missing generate_labelled_evaluation()"sv,
    };
}

Bytecode::CodeGenerationErrorOr<void> WhileStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    return generate_labelled_evaluation(generator, {});
}

Bytecode::CodeGenerationErrorOr<void> WhileStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<FlyString> const& label_set) const
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
    TRY(m_test->generate_bytecode(generator));
    generator.emit<Bytecode::Op::JumpConditional>().set_targets(
        Bytecode::Label { body_block },
        Bytecode::Label { end_block });

    generator.switch_to_basic_block(body_block);
    generator.begin_continuable_scope(Bytecode::Label { test_block }, label_set);
    generator.begin_breakable_scope(Bytecode::Label { end_block }, label_set);
    TRY(m_body->generate_bytecode(generator));
    generator.end_breakable_scope();
    generator.end_continuable_scope();

    if (!generator.is_current_block_terminated()) {
        generator.emit<Bytecode::Op::Jump>().set_targets(
            Bytecode::Label { test_block },
            {});
    }

    generator.switch_to_basic_block(end_block);
    generator.emit<Bytecode::Op::Load>(result_reg);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> DoWhileStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    return generate_labelled_evaluation(generator, {});
}

Bytecode::CodeGenerationErrorOr<void> DoWhileStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<FlyString> const& label_set) const
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
    TRY(m_test->generate_bytecode(generator));
    generator.emit<Bytecode::Op::JumpConditional>().set_targets(
        Bytecode::Label { body_block },
        Bytecode::Label { end_block });

    generator.switch_to_basic_block(body_block);
    generator.begin_continuable_scope(Bytecode::Label { test_block }, label_set);
    generator.begin_breakable_scope(Bytecode::Label { end_block }, label_set);
    TRY(m_body->generate_bytecode(generator));
    generator.end_breakable_scope();
    generator.end_continuable_scope();

    if (!generator.is_current_block_terminated()) {
        generator.emit<Bytecode::Op::Jump>().set_targets(
            Bytecode::Label { test_block },
            {});
    }

    generator.switch_to_basic_block(end_block);
    generator.emit<Bytecode::Op::Load>(result_reg);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> ForStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    return generate_labelled_evaluation(generator, {});
}

Bytecode::CodeGenerationErrorOr<void> ForStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<FlyString> const& label_set) const
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

    bool has_lexical_environment = false;

    if (m_init) {
        if (m_init->is_variable_declaration()) {
            auto& variable_declaration = verify_cast<VariableDeclaration>(*m_init);

            if (variable_declaration.is_lexical_declaration()) {
                has_lexical_environment = true;

                // FIXME: Is Block correct?
                generator.begin_variable_scope(Bytecode::Generator::BindingMode::Lexical, Bytecode::Generator::SurroundingScopeKind::Block);

                bool is_const = variable_declaration.is_constant_declaration();
                variable_declaration.for_each_bound_name([&](auto const& name) {
                    auto index = generator.intern_identifier(name);
                    generator.register_binding(index);
                    generator.emit<Bytecode::Op::CreateVariable>(index, Bytecode::Op::EnvironmentMode::Lexical, is_const);
                });
            }
        }

        TRY(m_init->generate_bytecode(generator));
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

    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    auto result_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(result_reg);

    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { *test_block_ptr },
        {});

    if (m_test) {
        generator.switch_to_basic_block(*test_block_ptr);
        TRY(m_test->generate_bytecode(generator));
        generator.emit<Bytecode::Op::JumpConditional>().set_targets(
            Bytecode::Label { *body_block_ptr },
            Bytecode::Label { end_block });
    }

    generator.switch_to_basic_block(*body_block_ptr);
    generator.begin_continuable_scope(Bytecode::Label { *update_block_ptr }, label_set);
    generator.begin_breakable_scope(Bytecode::Label { end_block }, label_set);
    TRY(m_body->generate_bytecode(generator));
    generator.end_breakable_scope();
    generator.end_continuable_scope();

    if (!generator.is_current_block_terminated()) {
        if (m_update) {
            generator.emit<Bytecode::Op::Jump>().set_targets(
                Bytecode::Label { *update_block_ptr },
                {});

            generator.switch_to_basic_block(*update_block_ptr);
            TRY(m_update->generate_bytecode(generator));
        }

        generator.emit<Bytecode::Op::Jump>().set_targets(
            Bytecode::Label { *test_block_ptr },
            {});
    }

    generator.switch_to_basic_block(end_block);
    generator.emit<Bytecode::Op::Load>(result_reg);

    if (has_lexical_environment)
        generator.end_variable_scope();

    return {};
}

Bytecode::CodeGenerationErrorOr<void> ObjectExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewObject>();
    if (m_properties.is_empty())
        return {};

    auto object_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(object_reg);

    for (auto& property : m_properties) {
        Bytecode::Op::PropertyKind property_kind;
        switch (property.type()) {
        case ObjectProperty::Type::KeyValue:
            property_kind = Bytecode::Op::PropertyKind::KeyValue;
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

        if (is<StringLiteral>(property.key())) {
            auto& string_literal = static_cast<StringLiteral const&>(property.key());
            Bytecode::IdentifierTableIndex key_name = generator.intern_identifier(string_literal.value());

            if (property_kind != Bytecode::Op::PropertyKind::Spread)
                TRY(property.value().generate_bytecode(generator));

            generator.emit<Bytecode::Op::PutById>(object_reg, key_name, property_kind);
        } else {
            TRY(property.key().generate_bytecode(generator));
            auto property_reg = generator.allocate_register();
            generator.emit<Bytecode::Op::Store>(property_reg);

            if (property_kind != Bytecode::Op::PropertyKind::Spread)
                TRY(property.value().generate_bytecode(generator));

            generator.emit<Bytecode::Op::PutByValue>(object_reg, property_reg, property_kind);
        }
    }

    generator.emit<Bytecode::Op::Load>(object_reg);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> ArrayExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    if (m_elements.is_empty()) {
        generator.emit<Bytecode::Op::NewArray>();
        return {};
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
        if (!*it)
            generator.emit<Bytecode::Op::LoadImmediate>(Value {});
        else {
            TRY((*it)->generate_bytecode(generator));
        }
        generator.emit<Bytecode::Op::Store>(reg);
    }

    if (first_spread.index() != 0)
        generator.emit_with_extra_register_slots<Bytecode::Op::NewArray>(2u, AK::Array { args_start_reg, Bytecode::Register { args_start_reg.index() + static_cast<u32>(first_spread.index() - 1) } });
    else
        generator.emit<Bytecode::Op::NewArray>();

    if (first_spread != m_elements.end()) {
        auto array_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(array_reg);
        for (auto it = first_spread; it != m_elements.end(); ++it) {
            if (!*it) {
                generator.emit<Bytecode::Op::LoadImmediate>(Value {});
                generator.emit<Bytecode::Op::Append>(array_reg, false);
            } else {
                TRY((*it)->generate_bytecode(generator));
                generator.emit<Bytecode::Op::Append>(array_reg, *it && is<SpreadExpression>(**it));
            }
        }
        generator.emit<Bytecode::Op::Load>(array_reg);
    }

    return {};
}

Bytecode::CodeGenerationErrorOr<void> MemberExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    return generator.emit_load_from_reference(*this);
}

Bytecode::CodeGenerationErrorOr<void> FunctionDeclaration::generate_bytecode(Bytecode::Generator& generator) const
{
    if (m_is_hoisted) {
        auto index = generator.intern_identifier(name());
        generator.emit<Bytecode::Op::GetVariable>(index);
        generator.emit<Bytecode::Op::SetVariable>(index, Bytecode::Op::SetVariable::InitializationMode::Set, Bytecode::Op::EnvironmentMode::Var);
    }
    return {};
}

Bytecode::CodeGenerationErrorOr<void> FunctionExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    bool has_name = !name().is_empty();
    Optional<Bytecode::IdentifierTableIndex> name_identifier;

    if (has_name) {
        generator.begin_variable_scope(Bytecode::Generator::BindingMode::Lexical);

        name_identifier = generator.intern_identifier(name());
        generator.emit<Bytecode::Op::CreateVariable>(*name_identifier, Bytecode::Op::EnvironmentMode::Lexical, true);
    }

    generator.emit<Bytecode::Op::NewFunction>(*this);

    if (has_name) {
        generator.emit<Bytecode::Op::SetVariable>(*name_identifier, Bytecode::Op::SetVariable::InitializationMode::Initialize, Bytecode::Op::EnvironmentMode::Lexical);
        generator.end_variable_scope();
    }

    return {};
}

static Bytecode::CodeGenerationErrorOr<void> generate_object_binding_pattern_bytecode(Bytecode::Generator& generator, BindingPattern const& pattern, Bytecode::Op::SetVariable::InitializationMode initialization_mode, Bytecode::Register const& value_reg)
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
            generator.emit<Bytecode::Op::SetVariable>(interned_identifier, initialization_mode);

            return {};
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
            TRY(expression->generate_bytecode(generator));

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
            TRY(initializer->generate_bytecode(generator));
            generator.emit<Bytecode::Op::Jump>().set_targets(
                Bytecode::Label { if_not_undefined_block },
                {});

            generator.switch_to_basic_block(if_not_undefined_block);
        }

        if (alias.has<NonnullRefPtr<BindingPattern>>()) {
            auto& binding_pattern = *alias.get<NonnullRefPtr<BindingPattern>>();
            auto nested_value_reg = generator.allocate_register();
            generator.emit<Bytecode::Op::Store>(nested_value_reg);
            TRY(generate_binding_pattern_bytecode(generator, binding_pattern, initialization_mode, nested_value_reg));
        } else if (alias.has<Empty>()) {
            if (name.has<NonnullRefPtr<Expression>>()) {
                // This needs some sort of SetVariableByValue opcode, as it's a runtime binding
                return Bytecode::CodeGenerationError {
                    name.get<NonnullRefPtr<Expression>>().ptr(),
                    "Unimplemented name/alias pair: Empty/Expression"sv,
                };
            }

            auto& identifier = name.get<NonnullRefPtr<Identifier>>()->string();
            generator.emit<Bytecode::Op::SetVariable>(generator.intern_identifier(identifier), initialization_mode);
        } else {
            auto& identifier = alias.get<NonnullRefPtr<Identifier>>()->string();
            generator.emit<Bytecode::Op::SetVariable>(generator.intern_identifier(identifier), initialization_mode);
        }
    }
    return {};
}

static Bytecode::CodeGenerationErrorOr<void> generate_array_binding_pattern_bytecode(Bytecode::Generator& generator, BindingPattern const& pattern, Bytecode::Op::SetVariable::InitializationMode initialization_mode, Bytecode::Register const& value_reg)
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
        return alias.visit(
            [&](Empty) -> Bytecode::CodeGenerationErrorOr<void> {
                // This element is an elision
                return {};
            },
            [&](NonnullRefPtr<Identifier> const& identifier) -> Bytecode::CodeGenerationErrorOr<void> {
                auto interned_index = generator.intern_identifier(identifier->string());
                generator.emit<Bytecode::Op::SetVariable>(interned_index, initialization_mode);
                return {};
            },
            [&](NonnullRefPtr<BindingPattern> const& pattern) -> Bytecode::CodeGenerationErrorOr<void> {
                // Store the accumulator value in a permanent register
                auto target_reg = generator.allocate_register();
                generator.emit<Bytecode::Op::Store>(target_reg);
                return generate_binding_pattern_bytecode(generator, pattern, initialization_mode, target_reg);
            },
            [&](NonnullRefPtr<MemberExpression> const& expr) -> Bytecode::CodeGenerationErrorOr<void> {
                return generator.emit_store_to_reference(*expr);
            });
    };

    for (auto& [name, alias, initializer, is_rest] : pattern.entries) {
        VERIFY(name.has<Empty>());

        if (is_rest) {
            VERIFY(!initializer);

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

            return assign_accumulator_to_alias(alias);
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

        if (initializer) {
            auto& value_is_undefined_block = generator.make_block();
            auto& value_is_not_undefined_block = generator.make_block();

            generator.emit<Bytecode::Op::JumpUndefined>().set_targets(
                Bytecode::Label { value_is_undefined_block },
                Bytecode::Label { value_is_not_undefined_block });

            generator.switch_to_basic_block(value_is_undefined_block);
            TRY(initializer->generate_bytecode(generator));
            generator.emit<Bytecode::Op::Jump>(Bytecode::Label { value_is_not_undefined_block });

            generator.switch_to_basic_block(value_is_not_undefined_block);
        }

        TRY(assign_accumulator_to_alias(alias));

        first = false;
    }

    return {};
}

static Bytecode::CodeGenerationErrorOr<void> generate_binding_pattern_bytecode(Bytecode::Generator& generator, BindingPattern const& pattern, Bytecode::Op::SetVariable::InitializationMode initialization_mode, Bytecode::Register const& value_reg)
{
    if (pattern.kind == BindingPattern::Kind::Object)
        return generate_object_binding_pattern_bytecode(generator, pattern, initialization_mode, value_reg);

    return generate_array_binding_pattern_bytecode(generator, pattern, initialization_mode, value_reg);
}

static Bytecode::CodeGenerationErrorOr<void> assign_accumulator_to_variable_declarator(Bytecode::Generator& generator, VariableDeclarator const& declarator, VariableDeclaration const& declaration)
{
    auto initialization_mode = declaration.is_lexical_declaration() ? Bytecode::Op::SetVariable::InitializationMode::Initialize : Bytecode::Op::SetVariable::InitializationMode::Set;
    auto environment_mode = declaration.is_lexical_declaration() ? Bytecode::Op::EnvironmentMode::Lexical : Bytecode::Op::EnvironmentMode::Var;

    return declarator.target().visit(
        [&](NonnullRefPtr<Identifier> const& id) -> Bytecode::CodeGenerationErrorOr<void> {
            generator.emit<Bytecode::Op::SetVariable>(generator.intern_identifier(id->string()), initialization_mode, environment_mode);
            return {};
        },
        [&](NonnullRefPtr<BindingPattern> const& pattern) -> Bytecode::CodeGenerationErrorOr<void> {
            auto value_register = generator.allocate_register();
            generator.emit<Bytecode::Op::Store>(value_register);
            return generate_binding_pattern_bytecode(generator, pattern, initialization_mode, value_register);
        });
}

Bytecode::CodeGenerationErrorOr<void> VariableDeclaration::generate_bytecode(Bytecode::Generator& generator) const
{
    for (auto& declarator : m_declarations) {
        if (declarator.init())
            TRY(declarator.init()->generate_bytecode(generator));
        else
            generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
        TRY(assign_accumulator_to_variable_declarator(generator, declarator, *this));
    }

    return {};
}

Bytecode::CodeGenerationErrorOr<void> CallExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    auto callee_reg = generator.allocate_register();
    auto this_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    generator.emit<Bytecode::Op::Store>(this_reg);

    if (is<NewExpression>(this)) {
        TRY(m_callee->generate_bytecode(generator));
        generator.emit<Bytecode::Op::Store>(callee_reg);
    } else if (is<SuperExpression>(*m_callee)) {
        return Bytecode::CodeGenerationError {
            this,
            "Unimplemented callee kind: SuperExpression"sv,
        };
    } else if (is<MemberExpression>(*m_callee)) {
        auto& member_expression = static_cast<MemberExpression const&>(*m_callee);
        if (is<SuperExpression>(member_expression.object())) {
            return Bytecode::CodeGenerationError {
                this,
                "Unimplemented callee kind: MemberExpression on SuperExpression"sv,
            };
        }

        TRY(member_expression.object().generate_bytecode(generator));
        generator.emit<Bytecode::Op::Store>(this_reg);
        if (member_expression.is_computed()) {
            TRY(member_expression.property().generate_bytecode(generator));
            generator.emit<Bytecode::Op::GetByValue>(this_reg);
        } else {
            auto identifier_table_ref = generator.intern_identifier(verify_cast<Identifier>(member_expression.property()).string());
            generator.emit<Bytecode::Op::GetById>(identifier_table_ref);
        }
        generator.emit<Bytecode::Op::Store>(callee_reg);
    } else {
        // FIXME: this = global object in sloppy mode.
        TRY(m_callee->generate_bytecode(generator));
        generator.emit<Bytecode::Op::Store>(callee_reg);
    }

    TRY(arguments_to_array_for_call(generator, m_arguments));

    Bytecode::Op::Call::CallType call_type;
    if (is<NewExpression>(*this)) {
        call_type = Bytecode::Op::Call::CallType::Construct;
    } else {
        call_type = Bytecode::Op::Call::CallType::Call;
    }

    Optional<Bytecode::StringTableIndex> expression_string_index;
    if (auto expression_string = this->expression_string(); expression_string.has_value())
        expression_string_index = generator.intern_string(expression_string.release_value());

    generator.emit<Bytecode::Op::Call>(call_type, callee_reg, this_reg, expression_string_index);

    return {};
}

Bytecode::CodeGenerationErrorOr<void> ReturnStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    if (m_argument)
        TRY(m_argument->generate_bytecode(generator));
    else
        generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());

    if (generator.is_in_generator_or_async_function()) {
        generator.perform_needed_unwinds<Bytecode::Op::Yield>();
        generator.emit<Bytecode::Op::Yield>(nullptr);
    } else {
        generator.perform_needed_unwinds<Bytecode::Op::Return>();
        generator.emit<Bytecode::Op::Return>();
    }

    return {};
}

Bytecode::CodeGenerationErrorOr<void> YieldExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    VERIFY(generator.is_in_generator_function());

    if (m_is_yield_from) {
        return Bytecode::CodeGenerationError {
            this,
            "Unimplemented form: `yield*`"sv,
        };
    }

    if (m_argument)
        TRY(m_argument->generate_bytecode(generator));

    auto& continuation_block = generator.make_block();
    generator.emit<Bytecode::Op::Yield>(Bytecode::Label { continuation_block });
    generator.switch_to_basic_block(continuation_block);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> IfStatement::generate_bytecode(Bytecode::Generator& generator) const
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

    TRY(m_predicate->generate_bytecode(generator));
    generator.emit<Bytecode::Op::JumpConditional>().set_targets(
        Bytecode::Label { true_block },
        Bytecode::Label { false_block });

    Bytecode::Op::Jump* true_block_jump { nullptr };

    generator.switch_to_basic_block(true_block);
    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    TRY(m_consequent->generate_bytecode(generator));
    if (!generator.is_current_block_terminated())
        true_block_jump = &generator.emit<Bytecode::Op::Jump>();

    generator.switch_to_basic_block(false_block);
    auto& end_block = generator.make_block();

    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    if (m_alternate)
        TRY(m_alternate->generate_bytecode(generator));
    if (!generator.is_current_block_terminated())
        generator.emit<Bytecode::Op::Jump>().set_targets(Bytecode::Label { end_block }, {});

    if (true_block_jump)
        true_block_jump->set_targets(Bytecode::Label { end_block }, {});

    generator.switch_to_basic_block(end_block);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> ContinueStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    if (m_target_label.is_null()) {
        generator.perform_needed_unwinds<Bytecode::Op::Jump>();
        generator.emit<Bytecode::Op::Jump>().set_targets(
            generator.nearest_continuable_scope(),
            {});
        return {};
    }

    auto target_to_jump_to = generator.perform_needed_unwinds_for_labelled_continue_and_return_target_block(m_target_label);
    generator.emit<Bytecode::Op::Jump>().set_targets(
        target_to_jump_to,
        {});
    return {};
}

Bytecode::CodeGenerationErrorOr<void> DebuggerStatement::generate_bytecode(Bytecode::Generator&) const
{
    return {};
}

Bytecode::CodeGenerationErrorOr<void> ConditionalExpression::generate_bytecode(Bytecode::Generator& generator) const
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

    TRY(m_test->generate_bytecode(generator));
    generator.emit<Bytecode::Op::JumpConditional>().set_targets(
        Bytecode::Label { true_block },
        Bytecode::Label { false_block });

    generator.switch_to_basic_block(true_block);
    TRY(m_consequent->generate_bytecode(generator));
    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { end_block },
        {});

    generator.switch_to_basic_block(false_block);
    TRY(m_alternate->generate_bytecode(generator));
    generator.emit<Bytecode::Op::Jump>().set_targets(
        Bytecode::Label { end_block },
        {});

    generator.switch_to_basic_block(end_block);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> SequenceExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    for (auto& expression : m_expressions)
        TRY(expression.generate_bytecode(generator));

    return {};
}

Bytecode::CodeGenerationErrorOr<void> TemplateLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    auto string_reg = generator.allocate_register();

    for (size_t i = 0; i < m_expressions.size(); i++) {
        TRY(m_expressions[i].generate_bytecode(generator));
        if (i == 0) {
            generator.emit<Bytecode::Op::Store>(string_reg);
        } else {
            generator.emit<Bytecode::Op::ConcatString>(string_reg);
        }
    }

    generator.emit<Bytecode::Op::Load>(string_reg);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> TaggedTemplateLiteral::generate_bytecode(Bytecode::Generator& generator) const
{
    TRY(m_tag->generate_bytecode(generator));
    auto tag_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(tag_reg);

    // FIXME: We only need to record the first and last register,
    //        due to packing everything in an array, same goes for argument_regs
    Vector<Bytecode::Register> string_regs;
    auto& expressions = m_template_literal->expressions();
    for (size_t i = 0; i < expressions.size(); ++i) {
        if (i % 2 != 0)
            continue;
        string_regs.append(generator.allocate_register());
    }

    size_t reg_index = 0;
    for (size_t i = 0; i < expressions.size(); ++i) {
        if (i % 2 != 0)
            continue;

        TRY(expressions[i].generate_bytecode(generator));
        auto string_reg = string_regs[reg_index++];
        generator.emit<Bytecode::Op::Store>(string_reg);
    }

    if (string_regs.is_empty()) {
        generator.emit<Bytecode::Op::NewArray>();
    } else {
        generator.emit_with_extra_register_slots<Bytecode::Op::NewArray>(2u, AK::Array { string_regs.first(), string_regs.last() });
    }
    auto strings_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(strings_reg);

    Vector<Bytecode::Register> argument_regs;
    argument_regs.append(strings_reg);
    for (size_t i = 1; i < expressions.size(); i += 2)
        argument_regs.append(generator.allocate_register());

    for (size_t i = 1; i < expressions.size(); i += 2) {
        auto string_reg = argument_regs[1 + i / 2];
        TRY(expressions[i].generate_bytecode(generator));
        generator.emit<Bytecode::Op::Store>(string_reg);
    }

    Vector<Bytecode::Register> raw_string_regs;
    for ([[maybe_unused]] auto& raw_string : m_template_literal->raw_strings())
        string_regs.append(generator.allocate_register());

    reg_index = 0;
    for (auto& raw_string : m_template_literal->raw_strings()) {
        TRY(raw_string.generate_bytecode(generator));
        auto raw_string_reg = string_regs[reg_index++];
        generator.emit<Bytecode::Op::Store>(raw_string_reg);
        raw_string_regs.append(raw_string_reg);
    }

    if (raw_string_regs.is_empty()) {
        generator.emit<Bytecode::Op::NewArray>();
    } else {
        generator.emit_with_extra_register_slots<Bytecode::Op::NewArray>(2u, AK::Array { raw_string_regs.first(), raw_string_regs.last() });
    }
    auto raw_strings_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(raw_strings_reg);

    generator.emit<Bytecode::Op::Load>(strings_reg);
    generator.emit<Bytecode::Op::PutById>(raw_strings_reg, generator.intern_identifier("raw"));

    generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
    auto this_reg = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(this_reg);

    if (!argument_regs.is_empty())
        generator.emit_with_extra_register_slots<Bytecode::Op::NewArray>(2, AK::Array { argument_regs.first(), argument_regs.last() });
    else
        generator.emit<Bytecode::Op::NewArray>();

    generator.emit<Bytecode::Op::Call>(Bytecode::Op::Call::CallType::Call, tag_reg, this_reg);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> UpdateExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    TRY(generator.emit_load_from_reference(*m_argument));

    Optional<Bytecode::Register> previous_value_for_postfix_reg;
    if (!m_prefixed) {
        previous_value_for_postfix_reg = generator.allocate_register();
        generator.emit<Bytecode::Op::Store>(*previous_value_for_postfix_reg);
    }

    if (m_op == UpdateOp::Increment)
        generator.emit<Bytecode::Op::Increment>();
    else
        generator.emit<Bytecode::Op::Decrement>();

    TRY(generator.emit_store_to_reference(*m_argument));

    if (!m_prefixed)
        generator.emit<Bytecode::Op::Load>(*previous_value_for_postfix_reg);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> ThrowStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    TRY(m_argument->generate_bytecode(generator));
    generator.perform_needed_unwinds<Bytecode::Op::Throw>();
    generator.emit<Bytecode::Op::Throw>();
    return {};
}

Bytecode::CodeGenerationErrorOr<void> BreakStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    if (m_target_label.is_null()) {
        generator.perform_needed_unwinds<Bytecode::Op::Jump>(true);
        generator.emit<Bytecode::Op::Jump>().set_targets(
            generator.nearest_breakable_scope(),
            {});
        return {};
    }

    auto target_to_jump_to = generator.perform_needed_unwinds_for_labelled_break_and_return_target_block(m_target_label);
    generator.emit<Bytecode::Op::Jump>().set_targets(
        target_to_jump_to,
        {});
    return {};
}

Bytecode::CodeGenerationErrorOr<void> TryStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    auto& saved_block = generator.current_block();

    Optional<Bytecode::Label> handler_target;
    Optional<Bytecode::Label> finalizer_target;

    Bytecode::BasicBlock* next_block { nullptr };

    if (m_finalizer) {
        auto& finalizer_block = generator.make_block();
        generator.switch_to_basic_block(finalizer_block);
        TRY(m_finalizer->generate_bytecode(generator));
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
        generator.begin_variable_scope(Bytecode::Generator::BindingMode::Lexical, Bytecode::Generator::SurroundingScopeKind::Block);
        TRY(m_handler->parameter().visit(
            [&](FlyString const& parameter) -> Bytecode::CodeGenerationErrorOr<void> {
                if (!parameter.is_empty()) {
                    auto parameter_identifier = generator.intern_identifier(parameter);
                    generator.register_binding(parameter_identifier);
                    generator.emit<Bytecode::Op::CreateVariable>(parameter_identifier, Bytecode::Op::EnvironmentMode::Lexical, false);
                    generator.emit<Bytecode::Op::SetVariable>(parameter_identifier, Bytecode::Op::SetVariable::InitializationMode::Initialize);
                }
                return {};
            },
            [&](NonnullRefPtr<BindingPattern> const&) -> Bytecode::CodeGenerationErrorOr<void> {
                // FIXME: Implement this path when the above DeclarativeEnvironment issue is dealt with.
                return Bytecode::CodeGenerationError {
                    this,
                    "Unimplemented catch argument: BindingPattern"sv,
                };
            }));

        TRY(m_handler->body().generate_bytecode(generator));
        handler_target = Bytecode::Label { handler_block };
        generator.end_variable_scope();

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
    generator.start_boundary(Bytecode::Generator::BlockBoundaryType::Unwind);

    generator.switch_to_basic_block(target_block);
    TRY(m_block->generate_bytecode(generator));
    if (!generator.is_current_block_terminated()) {
        if (m_finalizer) {
            generator.emit<Bytecode::Op::Jump>(finalizer_target);
        } else {
            auto& block = generator.make_block();
            generator.emit<Bytecode::Op::FinishUnwind>(Bytecode::Label { block });
            next_block = &block;
        }
    }
    generator.end_boundary(Bytecode::Generator::BlockBoundaryType::Unwind);

    generator.switch_to_basic_block(next_block ? *next_block : saved_block);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> SwitchStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    return generate_labelled_evaluation(generator, {});
}

Bytecode::CodeGenerationErrorOr<void> SwitchStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<FlyString> const& label_set) const
{
    auto discriminant_reg = generator.allocate_register();
    TRY(m_discriminant->generate_bytecode(generator));
    generator.emit<Bytecode::Op::Store>(discriminant_reg);
    Vector<Bytecode::BasicBlock&> case_blocks;
    Bytecode::BasicBlock* default_block { nullptr };
    Bytecode::BasicBlock* next_test_block = &generator.make_block();

    auto has_lexical_block = has_lexical_declarations();
    // Note: This call ends up calling begin_variable_scope() if has_lexical_block is true, so we need to clean up after it at the end.
    TRY(ScopeNode::generate_bytecode(generator));

    generator.emit<Bytecode::Op::Jump>().set_targets(Bytecode::Label { *next_test_block }, {});

    for (auto& switch_case : m_cases) {
        auto& case_block = generator.make_block();
        if (switch_case.test()) {
            generator.switch_to_basic_block(*next_test_block);
            TRY(switch_case.test()->generate_bytecode(generator));
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
    generator.begin_breakable_scope(Bytecode::Label { end_block }, label_set);
    for (auto& switch_case : m_cases) {
        generator.switch_to_basic_block(*current_block);

        generator.emit<Bytecode::Op::LoadImmediate>(js_undefined());
        for (auto& statement : switch_case.children()) {
            TRY(statement.generate_bytecode(generator));
            if (generator.is_current_block_terminated())
                break;
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
    if (has_lexical_block)
        generator.end_variable_scope();

    generator.switch_to_basic_block(end_block);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> ClassDeclaration::generate_bytecode(Bytecode::Generator& generator) const
{
    TRY(m_class_expression->generate_bytecode(generator));
    generator.emit<Bytecode::Op::SetVariable>(generator.intern_identifier(m_class_expression.ptr()->name()), Bytecode::Op::SetVariable::InitializationMode::Initialize);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> ClassExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::NewClass>(*this);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> SpreadExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    // NOTE: All users of this should handle the behaviour of this on their own,
    //       assuming it returns an Array-like object
    return m_target->generate_bytecode(generator);
}

Bytecode::CodeGenerationErrorOr<void> ThisExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    generator.emit<Bytecode::Op::ResolveThisBinding>();
    return {};
}

Bytecode::CodeGenerationErrorOr<void> AwaitExpression::generate_bytecode(Bytecode::Generator& generator) const
{
    VERIFY(generator.is_in_async_function());

    // Transform `await expr` to `yield expr`
    TRY(m_argument->generate_bytecode(generator));

    auto& continuation_block = generator.make_block();
    generator.emit<Bytecode::Op::Yield>(Bytecode::Label { continuation_block });
    generator.switch_to_basic_block(continuation_block);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> WithStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    TRY(m_object->generate_bytecode(generator));
    generator.emit<Bytecode::Op::EnterObjectEnvironment>();

    // EnterObjectEnvironment sets the running execution context's lexical_environment to a new Object Environment.
    generator.start_boundary(Bytecode::Generator::BlockBoundaryType::LeaveLexicalEnvironment);
    TRY(m_body->generate_bytecode(generator));
    generator.end_boundary(Bytecode::Generator::BlockBoundaryType::LeaveLexicalEnvironment);

    if (!generator.is_current_block_terminated())
        generator.emit<Bytecode::Op::LeaveEnvironment>(Bytecode::Op::EnvironmentMode::Lexical);

    return {};
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
};
static Bytecode::CodeGenerationErrorOr<ForInOfHeadEvaluationResult> for_in_of_head_evaluation(Bytecode::Generator& generator, IterationKind iteration_kind, Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> const& lhs, NonnullRefPtr<ASTNode> const& rhs)
{
    ForInOfHeadEvaluationResult result {};

    bool entered_lexical_scope = false;
    if (auto* ast_ptr = lhs.get_pointer<NonnullRefPtr<ASTNode>>(); ast_ptr && is<VariableDeclaration>(**ast_ptr)) {
        // Runtime Semantics: ForInOfLoopEvaluation, for any of:
        //  ForInOfStatement : for ( var ForBinding in Expression ) Statement
        //  ForInOfStatement : for ( ForDeclaration in Expression ) Statement
        //  ForInOfStatement : for ( var ForBinding of AssignmentExpression ) Statement
        //  ForInOfStatement : for ( ForDeclaration of AssignmentExpression ) Statement

        auto& variable_declaration = static_cast<VariableDeclaration const&>(**ast_ptr);
        result.is_destructuring = variable_declaration.declarations().first().target().has<NonnullRefPtr<BindingPattern>>();
        result.lhs_kind = variable_declaration.is_lexical_declaration() ? LHSKind::LexicalBinding : LHSKind::VarBinding;

        // 1. Let oldEnv be the running execution context's LexicalEnvironment.

        // NOTE: 'uninitializedBoundNames' refers to the lexical bindings (i.e. Const/Let) present in the second and last form.
        // 2. If uninitializedBoundNames is not an empty List, then

        if (variable_declaration.declaration_kind() != DeclarationKind::Var) {
            entered_lexical_scope = true;
            // a. Assert: uninitializedBoundNames has no duplicate entries.
            // b. Let newEnv be NewDeclarativeEnvironment(oldEnv).
            generator.begin_variable_scope();
            // c. For each String name of uninitializedBoundNames, do
            variable_declaration.for_each_bound_name([&](auto const& name) {
                // i. Perform ! newEnv.CreateMutableBinding(name, false).
                auto identifier = generator.intern_identifier(name);
                generator.register_binding(identifier);
                generator.emit<Bytecode::Op::CreateVariable>(identifier, Bytecode::Op::EnvironmentMode::Lexical, false);
            });
            // d. Set the running execution context's LexicalEnvironment to newEnv.
            // NOTE: Done by CreateEnvironment.
        }
    } else {
        // Runtime Semantics: ForInOfLoopEvaluation, for any of:
        //  ForInOfStatement : for ( LeftHandSideExpression in Expression ) Statement
        //  ForInOfStatement : for ( LeftHandSideExpression of AssignmentExpression ) Statement
        result.lhs_kind = LHSKind::Assignment;
    }

    // 3. Let exprRef be the result of evaluating expr.
    TRY(rhs->generate_bytecode(generator));

    // 4. Set the running execution context's LexicalEnvironment to oldEnv.
    if (entered_lexical_scope)
        generator.end_variable_scope();

    // 5. Let exprValue be ? GetValue(exprRef).
    // NOTE: No need to store this anywhere.

    // 6. If iterationKind is enumerate, then
    if (iteration_kind == IterationKind::Enumerate) {
        // a. If exprValue is undefined or null, then
        auto& nullish_block = generator.make_block();
        auto& continuation_block = generator.make_block();
        auto& jump = generator.emit<Bytecode::Op::JumpNullish>();
        jump.set_targets(Bytecode::Label { nullish_block }, Bytecode::Label { continuation_block });

        // i. Return Completion Record { [[Type]]: break, [[Value]]: empty, [[Target]]: empty }.
        generator.switch_to_basic_block(nullish_block);
        generator.perform_needed_unwinds<Bytecode::Op::Jump>(true);
        generator.emit<Bytecode::Op::Jump>().set_targets(generator.nearest_breakable_scope(), {});

        generator.switch_to_basic_block(continuation_block);
        // b. Let obj be ! ToObject(exprValue).
        // NOTE: GetObjectPropertyIterator does this.
        // c. Let iterator be EnumerateObjectProperties(obj).
        // d. Let nextMethod be ! GetV(iterator, "next").
        // e. Return the Iterator Record { [[Iterator]]: iterator, [[NextMethod]]: nextMethod, [[Done]]: false }.
        generator.emit<Bytecode::Op::GetObjectPropertyIterator>();
    }
    // 7. Else,
    else {
        // a. Assert: iterationKind is iterate or async-iterate.
        // b. If iterationKind is async-iterate, let iteratorHint be async.
        if (iteration_kind == IterationKind::AsyncIterate) {
            return Bytecode::CodeGenerationError {
                rhs.ptr(),
                "Unimplemented iteration mode: AsyncIterate"sv,
            };
        }
        // c. Else, let iteratorHint be sync.

        // d. Return ? GetIterator(exprValue, iteratorHint).
        generator.emit<Bytecode::Op::GetIterator>();
    }

    return result;
}

// 14.7.5.7 ForIn/OfBodyEvaluation ( lhs, stmt, iteratorRecord, iterationKind, lhsKind, labelSet [ , iteratorKind ] ), https://tc39.es/ecma262/#sec-runtime-semantics-forin-div-ofbodyevaluation-lhs-stmt-iterator-lhskind-labelset
static Bytecode::CodeGenerationErrorOr<void> for_in_of_body_evaluation(Bytecode::Generator& generator, ASTNode const& node, Variant<NonnullRefPtr<ASTNode>, NonnullRefPtr<BindingPattern>> const& lhs, ASTNode const& body, ForInOfHeadEvaluationResult const& head_result, Vector<FlyString> const& label_set, Bytecode::BasicBlock& loop_end, Bytecode::BasicBlock& loop_update)
{
    auto iterator_register = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(iterator_register);

    // FIXME: Implement this
    //        1. If iteratorKind is not present, set iteratorKind to sync.

    // 2. Let oldEnv be the running execution context's LexicalEnvironment.
    bool has_lexical_binding = false;

    // 3. Let V be undefined.
    // NOTE: We don't need 'V' as the resulting value will naturally flow through via the accumulator register.

    // 4. Let destructuring be IsDestructuring of lhs.
    auto destructuring = head_result.is_destructuring;

    // 5. If destructuring is true and if lhsKind is assignment, then
    if (destructuring) {
        // a. Assert: lhs is a LeftHandSideExpression.
        // b. Let assignmentPattern be the AssignmentPattern that is covered by lhs.
        // FIXME: Implement this.
        return Bytecode::CodeGenerationError {
            &node,
            "Unimplemented: destructuring in for-in/of"sv,
        };
    }
    // 6. Repeat,
    generator.emit<Bytecode::Op::Jump>(Bytecode::Label { loop_update });
    generator.switch_to_basic_block(loop_update);
    generator.begin_continuable_scope(Bytecode::Label { loop_update }, label_set);

    // a. Let nextResult be ? Call(iteratorRecord.[[NextMethod]], iteratorRecord.[[Iterator]]).
    generator.emit<Bytecode::Op::Load>(iterator_register);
    generator.emit<Bytecode::Op::IteratorNext>();

    // FIXME: Implement this:
    //        b. If iteratorKind is async, set nextResult to ? Await(nextResult).

    // c. If Type(nextResult) is not Object, throw a TypeError exception.
    // NOTE: IteratorComplete already does this.

    // d. Let done be ? IteratorComplete(nextResult).
    auto iterator_result_register = generator.allocate_register();
    generator.emit<Bytecode::Op::Store>(iterator_result_register);

    generator.emit<Bytecode::Op::IteratorResultDone>();
    // e. If done is true, return V.
    auto& loop_continue = generator.make_block();
    generator.emit<Bytecode::Op::JumpConditional>().set_targets(Bytecode::Label { loop_end }, Bytecode::Label { loop_continue });
    generator.switch_to_basic_block(loop_continue);

    // f. Let nextValue be ? IteratorValue(nextResult).
    generator.emit<Bytecode::Op::Load>(iterator_result_register);
    generator.emit<Bytecode::Op::IteratorResultValue>();

    // g. If lhsKind is either assignment or varBinding, then
    if (head_result.lhs_kind != LHSKind::LexicalBinding) {
        // i. If destructuring is false, then
        if (!destructuring) {
            // 1. Let lhsRef be the result of evaluating lhs. (It may be evaluated repeatedly.)
            // NOTE: We're skipping all the completion stuff that the spec does, as the unwinding mechanism will take case of doing that.
            if (head_result.lhs_kind == LHSKind::VarBinding) {
                auto& declaration = static_cast<VariableDeclaration const&>(*lhs.get<NonnullRefPtr<ASTNode>>());
                VERIFY(declaration.declarations().size() == 1);
                TRY(assign_accumulator_to_variable_declarator(generator, declaration.declarations().first(), declaration));
            } else {
                if (auto ptr = lhs.get_pointer<NonnullRefPtr<ASTNode>>()) {
                    TRY(generator.emit_store_to_reference(**ptr));
                } else {
                    auto& binding_pattern = lhs.get<NonnullRefPtr<BindingPattern>>();
                    TRY(generate_binding_pattern_bytecode(generator, *binding_pattern, Bytecode::Op::SetVariable::InitializationMode::Set, Bytecode::Register::accumulator()));
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
        generator.begin_variable_scope(Bytecode::Generator::BindingMode::Lexical);
        has_lexical_binding = true;

        // 14.7.5.4 Runtime Semantics: ForDeclarationBindingInstantiation, https://tc39.es/ecma262/#sec-runtime-semantics-fordeclarationbindinginstantiation
        // 1. Assert: environment is a declarative Environment Record.
        // NOTE: We just made it.
        auto& variable_declaration = static_cast<VariableDeclaration const&>(*lhs.get<NonnullRefPtr<ASTNode>>());
        // 2. For each element name of the BoundNames of ForBinding, do
        variable_declaration.for_each_bound_name([&](auto const& name) {
            auto identifier = generator.intern_identifier(name);
            generator.register_binding(identifier, Bytecode::Generator::BindingMode::Lexical);
            // a. If IsConstantDeclaration of LetOrConst is true, then
            if (variable_declaration.is_constant_declaration()) {
                // i. Perform ! environment.CreateImmutableBinding(name, true).
                generator.emit<Bytecode::Op::CreateVariable>(identifier, Bytecode::Op::EnvironmentMode::Lexical, true);
            }
            // b. Else,
            else {
                // i. Perform ! environment.CreateMutableBinding(name, false).
                generator.emit<Bytecode::Op::CreateVariable>(identifier, Bytecode::Op::EnvironmentMode::Lexical, false);
            }
        });
        // 3. Return unused.
        // NOTE: No need to do that as we've inlined this.

        // vi. If destructuring is false, then
        if (!destructuring) {
            // 1. Assert: lhs binds a single name.
            // 2. Let lhsName be the sole element of BoundNames of lhs.
            auto lhs_name = variable_declaration.declarations().first().target().get<NonnullRefPtr<Identifier>>()->string();
            // 3. Let lhsRef be ! ResolveBinding(lhsName).
            // NOTE: We're skipping all the completion stuff that the spec does, as the unwinding mechanism will take case of doing that.
            auto identifier = generator.intern_identifier(lhs_name);
            generator.emit<Bytecode::Op::SetVariable>(identifier, Bytecode::Op::SetVariable::InitializationMode::Initialize, Bytecode::Op::EnvironmentMode::Lexical);
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
        // FIXME: Implement destructuring
        //  i. If lhsKind is assignment, then
        //      1. Let status be Completion(DestructuringAssignmentEvaluation of assignmentPattern with argument nextValue).
        //  ii. Else if lhsKind is varBinding, then
        //      1. Assert: lhs is a ForBinding.
        //      2. Let status be Completion(BindingInitialization of lhs with arguments nextValue and undefined).
        //  iii. Else,
        //      1. Assert: lhsKind is lexicalBinding.
        //      2. Assert: lhs is a ForDeclaration.
        //      3. Let status be Completion(ForDeclarationBindingInitialization of lhs with arguments nextValue and iterationEnv).
        return Bytecode::CodeGenerationError {
            &node,
            "Unimplemented: destructuring in for-in/of"sv,
        };
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
    TRY(body.generate_bytecode(generator));

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
    if (!generator.is_current_block_terminated())
        generator.emit<Bytecode::Op::Jump>().set_targets(Bytecode::Label { loop_update }, {});

    generator.switch_to_basic_block(loop_end);
    return {};
}

Bytecode::CodeGenerationErrorOr<void> ForInStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    return generate_labelled_evaluation(generator, {});
}

// 14.7.5.5 Runtime Semantics: ForInOfLoopEvaluation, https://tc39.es/ecma262/#sec-runtime-semantics-forinofloopevaluation
Bytecode::CodeGenerationErrorOr<void> ForInStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<FlyString> const& label_set) const
{
    auto& loop_end = generator.make_block();
    auto& loop_update = generator.make_block();
    generator.begin_breakable_scope(Bytecode::Label { loop_end }, label_set);

    auto head_result = TRY(for_in_of_head_evaluation(generator, IterationKind::Enumerate, m_lhs, m_rhs));

    // Now perform the rest of ForInOfLoopEvaluation, given that the accumulator holds the iterator we're supposed to iterate over.
    return for_in_of_body_evaluation(generator, *this, m_lhs, body(), head_result, label_set, loop_end, loop_update);
}

Bytecode::CodeGenerationErrorOr<void> ForOfStatement::generate_bytecode(Bytecode::Generator& generator) const
{
    return generate_labelled_evaluation(generator, {});
}

Bytecode::CodeGenerationErrorOr<void> ForOfStatement::generate_labelled_evaluation(Bytecode::Generator& generator, Vector<FlyString> const& label_set) const
{
    auto& loop_end = generator.make_block();
    auto& loop_update = generator.make_block();
    generator.begin_breakable_scope(Bytecode::Label { loop_end }, label_set);

    auto head_result = TRY(for_in_of_head_evaluation(generator, IterationKind::Iterate, m_lhs, m_rhs));

    // Now perform the rest of ForInOfLoopEvaluation, given that the accumulator holds the iterator we're supposed to iterate over.
    return for_in_of_body_evaluation(generator, *this, m_lhs, body(), head_result, label_set, loop_end, loop_update);
}

// 13.3.12.1 Runtime Semantics: Evaluation, https://tc39.es/ecma262/#sec-meta-properties-runtime-semantics-evaluation
Bytecode::CodeGenerationErrorOr<void> MetaProperty::generate_bytecode(Bytecode::Generator& generator) const
{
    // NewTarget : new . target
    if (m_type == MetaProperty::Type::NewTarget) {
        // 1. Return GetNewTarget().
        generator.emit<Bytecode::Op::GetNewTarget>();
        return {};
    }

    // ImportMeta : import . meta
    if (m_type == MetaProperty::Type::ImportMeta) {
        return Bytecode::CodeGenerationError {
            this,
            "Unimplemented meta property: import.meta"sv,
        };
    }

    VERIFY_NOT_REACHED();
}
}
