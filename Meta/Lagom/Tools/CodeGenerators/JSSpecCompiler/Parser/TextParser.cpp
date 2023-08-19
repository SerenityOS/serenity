/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>

#include "Parser/TextParser.h"

namespace JSSpecCompiler {

void TextParser::retreat()
{
    --m_next_token_index;
}

auto TextParser::rollback_point()
{
    return ArmedScopeGuard {
        [this, index = this->m_next_token_index] {
            m_next_token_index = index;
        }
    };
}

ParseErrorOr<Token const*> TextParser::peek_token()
{
    if (m_next_token_index == m_tokens.size())
        return ParseError::create("Expected token but found EOF"sv, m_node);
    return &m_tokens[m_next_token_index];
}

ParseErrorOr<Token const*> TextParser::consume_token()
{
    auto result = peek_token();
    if (!result.is_error())
        ++m_next_token_index;
    return result;
}

ParseErrorOr<Token const*> TextParser::consume_token_with_one_of_types(std::initializer_list<TokenType> types)
{
    auto token = TRY(consume_token());
    for (TokenType type : types)
        if (token->type == type)
            return token;
    retreat();

    return ParseError::create(String::formatted("Unexpected token type {}", token->name()), token->node);
}

ParseErrorOr<Token const*> TextParser::consume_token_with_type(TokenType type)
{
    return consume_token_with_one_of_types({ type });
}

ParseErrorOr<void> TextParser::consume_word(StringView word)
{
    auto token = TRY(consume_token_with_type(TokenType::Word));
    if (!token->data.equals_ignoring_ascii_case(word)) {
        retreat();
        return ParseError::create("Unexpected word"sv, token->node);
    }
    return {};
}

ParseErrorOr<void> TextParser::consume_words(std::initializer_list<StringView> words)
{
    for (auto word : words)
        TRY(consume_word(word));
    return {};
}

bool TextParser::is_eof() const
{
    return m_next_token_index == m_tokens.size();
}

ParseErrorOr<void> TextParser::expect_eof() const
{
    if (!is_eof())
        return ParseError::create("Expected EOF"sv, m_node);
    return {};
}

ParseErrorOr<Tree> TextParser::parse_record_direct_list_initialization()
{
    auto rollback = rollback_point();

    (void)consume_word("the"sv);

    auto identifier = TRY(consume_token_with_type(TokenType::Identifier));
    TRY(consume_token_with_type(TokenType::BraceOpen));
    Vector<RecordDirectListInitialization::Argument> arguments;
    while (true) {
        auto name = TRY(consume_token_with_one_of_types({ TokenType::Identifier, TokenType::BraceClose }));

        if (name->is_bracket()) {
            break;
        } else {
            TRY(consume_token_with_type(TokenType::Colon));
            auto value = TRY(parse_expression());
            (void)consume_token_with_type(TokenType::Comma);
            arguments.append({ make_ref_counted<UnresolvedReference>(name->data), value });
        }
    }

    rollback.disarm();
    return make_ref_counted<RecordDirectListInitialization>(
        make_ref_counted<UnresolvedReference>(identifier->data), move(arguments));
}

// <expr>
ParseErrorOr<Tree> TextParser::parse_expression()
{
    auto rollback = rollback_point();

    // (the)? <record_name> { (<name>: <value>,)* }
    if (auto record_init = parse_record_direct_list_initialization(); !record_init.is_error()) {
        rollback.disarm();
        return record_init.release_value();
    }

#define THROW_PARSE_ERROR_IF(expr)                                      \
    do {                                                                \
        if (expr)                                                       \
            return ParseError::create("Expected expression"sv, m_node); \
    } while (false)
#define THROW_PARSE_ERROR THROW_PARSE_ERROR_IF(true)

    Vector<Variant<Tree, Token>> stack;

    auto merge_stack = [&](i32 precedence) {
        if (!stack.last().has<Tree>())
            return;

        while (stack.size() >= 2) {
            auto const& maybe_operator = stack[stack.size() - 2];
            if (!maybe_operator.has<Token>())
                break;
            auto last_operator = maybe_operator.get<Token>();

            auto right = stack.last().get<Tree>();

            if (last_operator.is_unary_operator()) {
                auto operation = make_ref_counted<UnaryOperation>(last_operator.as_unary_operator(), right);
                stack.shrink(stack.size() - 2);
                stack.empend(operation);
            } else if (last_operator.is_binary_operator() && last_operator.precedence() < precedence) {
                auto left = stack[stack.size() - 3].get<Tree>();
                auto operation = make_ref_counted<BinaryOperation>(last_operator.as_binary_operator(), left, right);
                stack.shrink(stack.size() - 3);
                stack.empend(operation);
            } else {
                break;
            }
        }
    };

    auto merge_pre_merged = [&] {
        if (stack.size() < 3)
            return;

        auto const& maybe_left = stack[stack.size() - 3];
        auto const& maybe_operator = stack[stack.size() - 2];
        auto const& maybe_right = stack.last();

        if (!maybe_left.has<Tree>() || !maybe_operator.has<Token>() || !maybe_right.has<Tree>())
            return;

        auto last_operator = maybe_operator.get<Token>();
        if (!last_operator.is_pre_merged_binary_operator())
            return;

        auto expression = make_ref_counted<BinaryOperation>(last_operator.as_binary_operator(), maybe_left.get<Tree>(), maybe_right.get<Tree>());

        stack.shrink(stack.size() - 3);
        stack.empend(expression);
    };

    i32 bracket_balance = 0;

    while (true) {
        auto token_or_error = peek_token();
        if (token_or_error.is_error())
            break;
        auto token = *token_or_error.release_value();

        enum {
            NoneType,
            ExpressionType,
            PreMergedBinaryOperatorType,
            UnaryOperatorType,
            BinaryOperatorType,
            BracketType,
        } last_element_type;

        if (stack.is_empty())
            last_element_type = NoneType;
        else if (stack.last().has<Tree>())
            last_element_type = ExpressionType;
        else if (stack.last().get<Token>().is_pre_merged_binary_operator())
            last_element_type = PreMergedBinaryOperatorType;
        else if (stack.last().get<Token>().is_unary_operator())
            last_element_type = UnaryOperatorType;
        else if (stack.last().get<Token>().is_binary_operator())
            last_element_type = BinaryOperatorType;
        else if (stack.last().get<Token>().is_bracket())
            last_element_type = BracketType;
        else
            VERIFY_NOT_REACHED();

        if (token.is_ambiguous_operator()) {
            if (token.type == TokenType::AmbiguousMinus)
                token.type = last_element_type == ExpressionType ? TokenType::BinaryMinus : TokenType::UnaryMinus;
            else
                VERIFY_NOT_REACHED();
        }

        bracket_balance += token.is_opening_bracket();
        bracket_balance -= token.is_closing_bracket();

        if (bracket_balance < 0)
            break;

        if (token.type == TokenType::ParenOpen) {
            if (last_element_type == ExpressionType)
                stack.append(Token { TokenType::FunctionCall, ""sv, m_node });
            stack.append(token);
        } else if (token.is_pre_merged_binary_operator()) {
            THROW_PARSE_ERROR_IF(last_element_type != ExpressionType);
            stack.append(token);
        } else if (token.is_unary_operator()) {
            THROW_PARSE_ERROR_IF(last_element_type == PreMergedBinaryOperatorType);
            stack.append(token);
        } else if (token.is_binary_operator() || token.is_closing_bracket()) {
            if (bracket_balance == 0 && token.type == TokenType::Comma)
                break;

            THROW_PARSE_ERROR_IF(last_element_type != ExpressionType);

            merge_stack(token.precedence());
            if (token.is_closing_bracket()) {
                THROW_PARSE_ERROR_IF(stack.size() == 1);
                THROW_PARSE_ERROR_IF(!stack[stack.size() - 2].get<Token>().matches_with(token));
                stack.remove(stack.size() - 2);
                merge_pre_merged();
            } else {
                stack.append(token);
            }
        } else {
            NullableTree expression;
            if (token.type == TokenType::Identifier) {
                expression = make_ref_counted<UnresolvedReference>(token.data);
            } else if (token.type == TokenType::Number) {
                expression = make_ref_counted<MathematicalConstant>(token.data.to_int<i64>().value());
            } else if (token.type == TokenType::String) {
                expression = make_ref_counted<StringLiteral>(token.data);
            } else {
                break;
            }
            THROW_PARSE_ERROR_IF(last_element_type == ExpressionType);
            stack.append(expression.release_nonnull());
            merge_pre_merged();
        }

        MUST(consume_token());
    }

    THROW_PARSE_ERROR_IF(stack.is_empty());
    merge_stack(closing_bracket_precedence);
    THROW_PARSE_ERROR_IF(stack.size() != 1 || !stack[0].has<Tree>());

    rollback.disarm();
    return stack[0].get<Tree>();
#undef THROW_PARSE_ERROR
#undef THROW_PARSE_ERROR_IF
}

// <condition> :== <expr> | (<expr> is <expr> (or <expr>)?)
ParseErrorOr<Tree> TextParser::parse_condition()
{
    auto rollback = rollback_point();
    auto expression = TRY(parse_expression());

    if (!consume_token_with_type(TokenType::Is).is_error()) {
        Vector compare_values { TRY(parse_expression()) };
        if (!consume_word("or"sv).is_error())
            compare_values.append(TRY(parse_expression()));

        rollback.disarm();
        return make_ref_counted<IsOneOfOperation>(expression, move(compare_values));
    }

    rollback.disarm();
    return expression;
}

// return <expr>
ParseErrorOr<Tree> TextParser::parse_return_statement()
{
    auto rollback = rollback_point();

    TRY(consume_word("return"sv));
    auto return_value = TRY(parse_expression());

    rollback.disarm();
    return make_ref_counted<ReturnNode>(return_value);
}

// assert: <condition>
ParseErrorOr<Tree> TextParser::parse_assert()
{
    auto rollback = rollback_point();

    auto identifier = TRY(consume_token_with_type(TokenType::Identifier))->data;
    if (!identifier.equals_ignoring_ascii_case("assert"sv)) {
        return ParseError::create("Expected identifier \"Assert\""sv, m_node);
    }

    TRY(consume_token_with_type(TokenType::Colon));
    auto condition = TRY(parse_condition());

    rollback.disarm();
    return make_ref_counted<AssertExpression>(condition);
}

// (let <expr> be <expr>) | (set <expr> to <expr>)
ParseErrorOr<Tree> TextParser::parse_assignment()
{
    auto rollback = rollback_point();

    bool is_let = !consume_word("let"sv).is_error();
    if (!is_let)
        TRY(consume_word("set"sv));
    auto lvalue = TRY(parse_expression());
    TRY(consume_word(is_let ? "be"sv : "to"sv));
    auto rvalue = TRY(parse_expression());

    rollback.disarm();
    auto op = is_let ? BinaryOperator::Declaration : BinaryOperator::Assignment;
    return make_ref_counted<BinaryOperation>(op, lvalue, rvalue);
}

// <simple_step>
ParseErrorOr<Tree> TextParser::parse_simple_step_or_inline_if_branch()
{
    auto rollback = rollback_point();

    // Return <expr>.$
    if (auto result = parse_return_statement(); !result.is_error()) {
        TRY(consume_token_with_type(TokenType::Dot));
        TRY(expect_eof());
        rollback.disarm();
        return result.release_value();
    }

    // Assert: <expr>.$
    if (auto result = parse_assert(); !result.is_error()) {
        TRY(consume_token_with_type(TokenType::Dot));
        TRY(expect_eof());
        rollback.disarm();
        return result.release_value();
    }

    // Let <expr> be <expr>.$
    // Set <expr> to <expr>.$
    if (auto result = parse_assignment(); !result.is_error()) {
        TRY(consume_token_with_type(TokenType::Dot));
        TRY(expect_eof());
        rollback.disarm();
        return result.release_value();
    }

    return ParseError::create("Unable to parse simple step or inline if branch"sv, m_node);
}

// <if_condition> :== (If <condition>) | (Else) | (Else if <condition>),
ParseErrorOr<TextParser::IfConditionParseResult> TextParser::parse_if_beginning()
{
    auto rollback = rollback_point();

    bool is_if_branch = !consume_word("if"sv).is_error();
    NullableTree condition = nullptr;
    if (is_if_branch) {
        condition = TRY(parse_condition());
    } else {
        TRY(consume_word("else"sv));
        if (!consume_word("if"sv).is_error())
            condition = TRY(parse_condition());
    }
    TRY(consume_token_with_type(TokenType::Comma));

    rollback.disarm();
    return IfConditionParseResult { is_if_branch, condition };
}

// <inline_if> :== <if_condition> <simple_step>.$
ParseErrorOr<Tree> TextParser::parse_inline_if_else()
{
    auto rollback = rollback_point();

    auto [is_if_branch, condition] = TRY(parse_if_beginning());
    auto then_branch = TRY(parse_simple_step_or_inline_if_branch());

    rollback.disarm();
    if (is_if_branch)
        return make_ref_counted<IfBranch>(condition.release_nonnull(), then_branch);
    return make_ref_counted<ElseIfBranch>(condition, then_branch);
}

// <if> :== <if_condition> then$ <substeps>
ParseErrorOr<Tree> TextParser::parse_if(Tree then_branch)
{
    auto rollback = rollback_point();

    auto [is_if_branch, condition] = TRY(parse_if_beginning());
    TRY(consume_word("then"sv));
    TRY(expect_eof());

    rollback.disarm();
    if (is_if_branch)
        return make_ref_counted<IfBranch>(*condition, then_branch);
    else
        return make_ref_counted<ElseIfBranch>(condition, then_branch);
}

// <else> :== Else,$ <substeps>
ParseErrorOr<Tree> TextParser::parse_else(Tree else_branch)
{
    auto rollback = rollback_point();

    TRY(consume_word("else"sv));
    TRY(consume_token_with_type(TokenType::Comma));
    TRY(expect_eof());

    rollback.disarm();
    return make_ref_counted<ElseIfBranch>(nullptr, else_branch);
}

// <simple_step> | <inline_if>
ParseErrorOr<Tree> TextParser::parse_step_without_substeps()
{
    auto rollback = rollback_point();

    // <simple_step>
    if (auto result = parse_simple_step_or_inline_if_branch(); !result.is_error()) {
        rollback.disarm();
        return result.release_value();
    }

    // <inline_if>
    if (auto result = parse_inline_if_else(); !result.is_error()) {
        rollback.disarm();
        return result.release_value();
    }

    return ParseError::create("Unable to parse step without substeps"sv, m_node);
}

// <if> | <else>
ParseErrorOr<Tree> TextParser::parse_step_with_substeps(Tree substeps)
{
    auto rollback = rollback_point();

    // <if>
    if (auto result = parse_if(substeps); !result.is_error()) {
        rollback.disarm();
        return result.release_value();
    }

    // <else>
    if (auto result = parse_else(substeps); !result.is_error()) {
        rollback.disarm();
        return result.release_value();
    }

    return ParseError::create("Unable to parse step with substeps"sv, m_node);
}

ParseErrorOr<TextParser::DefinitionParseResult> TextParser::parse_definition()
{
    DefinitionParseResult result;

    auto section_number_token = TRY(consume_token_with_type(TokenType::SectionNumber));
    result.section_number = section_number_token->data;

    result.function_name = TRY(consume_token())->data;

    TRY(consume_token_with_type(TokenType::ParenOpen));
    while (true) {
        result.arguments.append({ TRY(consume_token_with_type(TokenType::Identifier))->data });
        auto next_token = TRY(consume_token_with_one_of_types({ TokenType::ParenClose, TokenType::Comma }));
        if (next_token->type == TokenType::ParenClose)
            break;
    }
    TRY(expect_eof());

    return result;
}

}
