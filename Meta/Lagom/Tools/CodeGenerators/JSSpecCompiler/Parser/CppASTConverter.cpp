/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/File.h>

#include "Function.h"
#include "Parser/CppASTConverter.h"
#include "Parser/SpecificationParsing.h"

namespace JSSpecCompiler {

NonnullRefPtr<FunctionDefinition> CppASTConverter::convert()
{
    StringView name = m_function->name()->full_name();

    Vector<Tree> toplevel_statements;
    for (auto const& statement : m_function->definition()->statements()) {
        auto maybe_tree = as_nullable_tree(statement);
        if (maybe_tree)
            toplevel_statements.append(maybe_tree.release_nonnull());
    }
    auto tree = make_ref_counted<TreeList>(move(toplevel_statements));

    Vector<FunctionArgument> arguments;
    for (auto const& parameter : m_function->parameters())
        arguments.append({ .name = parameter->full_name() });

    return make_ref_counted<FunctionDefinition>(
        AbstractOperationDeclaration {
            .name = MUST(FlyString::from_utf8(name)),
            .arguments = move(arguments),
        },
        Location {},
        tree);
}

template<>
NullableTree CppASTConverter::convert_node(Cpp::VariableDeclaration const& variable_declaration)
{
    static Tree variable_declaration_present_error
        = make_ref_counted<ErrorNode>("Encountered variable declaration with initial value"sv);

    if (variable_declaration.initial_value() != nullptr)
        return variable_declaration_present_error;
    return nullptr;
}

template<>
NullableTree CppASTConverter::convert_node(Cpp::ReturnStatement const& return_statement)
{
    return make_ref_counted<ReturnNode>(as_tree(return_statement.value()));
}

template<>
NullableTree CppASTConverter::convert_node(Cpp::FunctionCall const& function_call)
{
    Vector<Tree> arguments;
    for (auto const& argument : function_call.arguments())
        arguments.append(as_tree(argument));

    return make_ref_counted<FunctionCall>(as_tree(function_call.callee()), move(arguments));
}

template<>
NullableTree CppASTConverter::convert_node(Cpp::Name const& name)
{
    return make_ref_counted<UnresolvedReference>(name.full_name());
}

template<>
NullableTree CppASTConverter::convert_node(Cpp::IfStatement const& if_statement)
{
    // NOTE: This is so complicated since we probably want to test IfBranchMergingPass, which
    //       expects standalone `IfBranch` and `ElseIfBranch` nodes.

    Vector<Tree> trees;
    Cpp::IfStatement const* current = &if_statement;

    while (true) {
        auto predicate = as_tree(current->predicate());
        auto then_branch = as_possibly_empty_tree(current->then_statement());

        if (trees.is_empty())
            trees.append(make_ref_counted<IfBranch>(predicate, then_branch));
        else
            trees.append(make_ref_counted<ElseIfBranch>(predicate, then_branch));

        auto else_statement = dynamic_cast<Cpp::IfStatement const*>(current->else_statement());
        if (else_statement)
            current = else_statement;
        else
            break;
    }

    auto else_statement = current->else_statement();
    if (else_statement)
        trees.append(make_ref_counted<ElseIfBranch>(
            nullptr, as_possibly_empty_tree(else_statement)));

    return make_ref_counted<TreeList>(move(trees));
}

template<>
NullableTree CppASTConverter::convert_node(Cpp::BlockStatement const& block)
{
    Vector<Tree> statements;
    for (auto const& statement : block.statements()) {
        auto maybe_tree = as_nullable_tree(statement);
        if (maybe_tree)
            statements.append(maybe_tree.release_nonnull());
    }
    return make_ref_counted<TreeList>(move(statements));
}

template<>
NullableTree CppASTConverter::convert_node(Cpp::AssignmentExpression const& assignment)
{
    // NOTE: Later stages of the compilation process basically treat `BinaryOperator::Declaration`
    //       the same as `BinaryOperator::Assignment`, so variable shadowing is impossible. The only
    //       difference in their semantics is that "declarations" define names of local variables.
    //       Since we are effectively ignoring actual C++ variable declarations, we need to define
    //       locals somewhere else. Using "declarations" instead of "assignments" here does this job
    //       cleanly.
    return make_ref_counted<BinaryOperation>(
        BinaryOperator::Declaration, as_tree(assignment.lhs()), as_tree(assignment.rhs()));
}

template<>
NullableTree CppASTConverter::convert_node(Cpp::NumericLiteral const& literal)
{
    // TODO: Numerical literals are not limited to i64.
    VERIFY(literal.value().to_number<i64>().has_value());
    return make_ref_counted<MathematicalConstant>(MUST(Crypto::BigFraction::from_string(literal.value())));
}

template<>
NullableTree CppASTConverter::convert_node(Cpp::StringLiteral const& literal)
{
    return make_ref_counted<StringLiteral>(literal.value());
}

template<>
NullableTree CppASTConverter::convert_node(Cpp::BinaryExpression const& expression)
{
    static constexpr auto operator_translation = []() consteval {
        Array<BinaryOperator, to_underlying(Cpp::BinaryOp::Arrow) + 1> table;
#define ASSIGN_TRANSLATION(cpp_name, our_name) \
    table[to_underlying(Cpp::BinaryOp::cpp_name)] = BinaryOperator::our_name
        ASSIGN_TRANSLATION(Addition, Plus);
        ASSIGN_TRANSLATION(Subtraction, Minus);
        ASSIGN_TRANSLATION(Multiplication, Multiplication);
        ASSIGN_TRANSLATION(Division, Division);
        ASSIGN_TRANSLATION(Modulo, Invalid);
        ASSIGN_TRANSLATION(GreaterThan, CompareGreater);
        ASSIGN_TRANSLATION(GreaterThanEquals, Invalid);
        ASSIGN_TRANSLATION(LessThan, CompareLess);
        ASSIGN_TRANSLATION(LessThanEquals, Invalid);
        ASSIGN_TRANSLATION(BitwiseAnd, Invalid);
        ASSIGN_TRANSLATION(BitwiseOr, Invalid);
        ASSIGN_TRANSLATION(BitwiseXor, Invalid);
        ASSIGN_TRANSLATION(LeftShift, Invalid);
        ASSIGN_TRANSLATION(RightShift, Invalid);
        ASSIGN_TRANSLATION(EqualsEquals, CompareEqual);
        ASSIGN_TRANSLATION(NotEqual, CompareNotEqual);
        ASSIGN_TRANSLATION(LogicalOr, Invalid);
        ASSIGN_TRANSLATION(LogicalAnd, Invalid);
        ASSIGN_TRANSLATION(Arrow, Invalid);
#undef ASSIGN_TRANSLATION
        return table;
    }();

    auto translated_operator = operator_translation[to_underlying(expression.op())];
    // TODO: Print nicer error.
    VERIFY(translated_operator != BinaryOperator::Invalid);
    return make_ref_counted<BinaryOperation>(translated_operator, as_tree(expression.lhs()), as_tree(expression.rhs()));
}

NullableTree CppASTConverter::as_nullable_tree(Cpp::Statement const* statement)
{
    static Tree unknown_ast_node_error
        = make_ref_counted<ErrorNode>("Encountered unknown C++ AST node"sv);

    Optional<NullableTree> result;

    auto dispatch_convert_if_one_of = [&]<typename... Ts> {
        (([&]<typename T> {
            if (result.has_value())
                return;
            auto casted_ptr = dynamic_cast<T const*>(statement);
            if (casted_ptr != nullptr)
                result = convert_node<T>(*casted_ptr);
        }).template operator()<Ts>(),
            ...);
    };

    dispatch_convert_if_one_of.operator()<
        Cpp::VariableDeclaration,
        Cpp::ReturnStatement,
        Cpp::FunctionCall,
        Cpp::Name,
        Cpp::IfStatement,
        Cpp::BlockStatement,
        Cpp::AssignmentExpression,
        Cpp::NumericLiteral,
        Cpp::StringLiteral,
        Cpp::BinaryExpression>();

    if (result.has_value())
        return *result;
    return unknown_ast_node_error;
}

Tree CppASTConverter::as_tree(Cpp::Statement const* statement)
{
    static Tree empty_tree_error
        = make_ref_counted<ErrorNode>("AST conversion unexpectedly produced empty tree"sv);

    auto result = as_nullable_tree(statement);
    if (result)
        return result.release_nonnull();
    return empty_tree_error;
}

Tree CppASTConverter::as_possibly_empty_tree(Cpp::Statement const* statement)
{
    auto result = as_nullable_tree(statement);
    if (result)
        return result.release_nonnull();
    return make_ref_counted<TreeList>(Vector<Tree> {});
}

CppParsingStep::CppParsingStep()
    : CompilationStep("parser"sv)
{
}

CppParsingStep::~CppParsingStep() = default;

void CppParsingStep::run(TranslationUnitRef translation_unit)
{
    auto filename = translation_unit->filename();

    auto file = Core::File::open_file_or_standard_stream(filename, Core::File::OpenMode::Read).release_value_but_fixme_should_propagate_errors();
    m_input = file->read_until_eof().release_value_but_fixme_should_propagate_errors();

    Cpp::Preprocessor preprocessor { filename, m_input };
    m_parser = adopt_own_if_nonnull(new Cpp::Parser { preprocessor.process_and_lex(), filename });

    auto cpp_translation_unit = m_parser->parse();
    VERIFY(m_parser->errors().is_empty());

    for (auto const& declaration : cpp_translation_unit->declarations()) {
        if (declaration->is_function()) {
            auto const* cpp_function = AK::verify_cast<Cpp::FunctionDeclaration>(declaration.ptr());
            translation_unit->adopt_function(CppASTConverter(cpp_function).convert());
        }
    }
}

}
