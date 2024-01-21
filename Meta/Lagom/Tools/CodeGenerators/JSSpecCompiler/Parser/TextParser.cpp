/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>

#include "Parser/SpecParser.h"
#include "Parser/TextParser.h"

namespace JSSpecCompiler {

void TextParser::save_error(Variant<TokenType, StringView, CustomMessage>&& expected)
{
    if (m_max_parsed_tokens > m_next_token_index)
        return;
    if (m_max_parsed_tokens < m_next_token_index)
        m_suitable_continuations.clear();
    m_max_parsed_tokens = m_next_token_index;
    m_suitable_continuations.append(move(expected));
}

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

Optional<Token> TextParser::peek_token()
{
    if (m_next_token_index == m_tokens.size())
        return {};
    return m_tokens[m_next_token_index];
}

Optional<Token> TextParser::consume_token()
{
    auto result = peek_token();
    if (result.has_value())
        ++m_next_token_index;
    return result;
}

TextParseErrorOr<Token> TextParser::consume_token_with_one_of_types(std::initializer_list<TokenType> types)
{
    auto token = peek_token();
    if (token.has_value()) {
        for (TokenType type : types) {
            if (token->type == type) {
                (void)consume_token();
                return *token;
            } else {
                save_error(type);
            }
        }
    } else {
        for (TokenType type : types)
            save_error(type);
    }

    return TextParseError {};
}

TextParseErrorOr<Token> TextParser::consume_token_with_type(TokenType type)
{
    return consume_token_with_one_of_types({ type });
}

TextParseErrorOr<void> TextParser::consume_token(TokenType type, StringView data)
{
    auto token = consume_token();
    if (!token.has_value() || token->type != type || !token->data.equals_ignoring_ascii_case(data)) {
        retreat();
        save_error(data);
        return TextParseError {};
    }
    return {};
}

TextParseErrorOr<void> TextParser::consume_word(StringView word)
{
    auto token = consume_token();
    if (!token.has_value() || token->type != TokenType::Word || !token->data.equals_ignoring_ascii_case(word)) {
        retreat();
        save_error(word);
        return TextParseError {};
    }
    return {};
}

TextParseErrorOr<void> TextParser::consume_words(std::initializer_list<StringView> words)
{
    for (auto word : words)
        TRY(consume_word(word));
    return {};
}

bool TextParser::is_eof() const
{
    return m_next_token_index == m_tokens.size();
}

TextParseErrorOr<void> TextParser::expect_eof()
{
    if (!is_eof()) {
        save_error(CustomMessage { "EOF"sv });
        return TextParseError {};
    }
    return {};
}

// (the)? <record_name> { (<name>: <value>,)* }
TextParseErrorOr<Tree> TextParser::parse_record_direct_list_initialization()
{
    auto rollback = rollback_point();

    (void)consume_word("the"sv);

    auto identifier = TRY(consume_token_with_type(TokenType::Identifier));
    TRY(consume_token_with_type(TokenType::BraceOpen));
    Vector<RecordDirectListInitialization::Argument> arguments;
    while (true) {
        auto name = TRY(consume_token_with_one_of_types({ TokenType::Identifier, TokenType::BraceClose }));

        if (name.is_bracket()) {
            break;
        } else {
            TRY(consume_token_with_type(TokenType::Colon));
            auto value = TRY(parse_expression());
            (void)consume_token_with_type(TokenType::Comma);
            arguments.append({ make_ref_counted<UnresolvedReference>(name.data), value });
        }
    }

    rollback.disarm();
    return make_ref_counted<RecordDirectListInitialization>(
        make_ref_counted<UnresolvedReference>(identifier.data), move(arguments));
}

// <expr>
TextParseErrorOr<Tree> TextParser::parse_expression()
{
    auto rollback = rollback_point();

    if (auto record_init = parse_record_direct_list_initialization(); !record_init.is_error()) {
        rollback.disarm();
        return record_init.release_value();
    }

#define THROW_PARSE_ERROR_IF(expr)                                                                           \
    do {                                                                                                     \
        if (expr) {                                                                                          \
            save_error(CustomMessage { "valid expression continuation (not valid because " #expr ")"##sv }); \
            return TextParseError {};                                                                        \
        }                                                                                                    \
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
        if (!token_or_error.has_value())
            break;
        auto token = token_or_error.release_value();

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
                stack.append(Token { TokenType::FunctionCall, ""sv, token.location });
            stack.append(token);

            if (m_next_token_index + 1 < m_tokens.size()
                && m_tokens[m_next_token_index + 1].type == TokenType::ParenClose) {
                // This is a call to function which does not take parameters. We cannot handle it
                // normally since we need text between parenthesis to be a valid expression. As a
                // workaround, we push an artificial tree to stack to act as an argument (it'll be
                // removed later during function call canonicalization).
                stack.append(zero_argument_function_call);
            }
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
            } else if (token.type == TokenType::WellKnownValue) {
                static constexpr struct {
                    StringView name;
                    WellKnownNode::Type type;
                } translations[] = {
                    { "false"sv, WellKnownNode::Type::False },
                    { "null"sv, WellKnownNode::Type::Null },
                    { "this"sv, WellKnownNode::Type::This },
                    { "true"sv, WellKnownNode::Type::True },
                    { "undefined"sv, WellKnownNode::Type::Undefined },
                };
                for (auto [name, type] : translations) {
                    if (token.data == name) {
                        expression = make_ref_counted<WellKnownNode>(type);
                        break;
                    }
                }
                VERIFY(expression);
            } else if (token.type == TokenType::Enumerator) {
                expression = m_ctx.translation_unit()->get_node_for_enumerator_value(token.data);
            } else if (token.type == TokenType::Number) {
                expression = make_ref_counted<MathematicalConstant>(MUST(Crypto::BigFraction::from_string(token.data)));
            } else if (token.type == TokenType::String) {
                expression = make_ref_counted<StringLiteral>(token.data);
            } else {
                break;
            }
            THROW_PARSE_ERROR_IF(last_element_type == ExpressionType);
            stack.append(expression.release_nonnull());
            merge_pre_merged();
        }

        VERIFY(consume_token().has_value());
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
TextParseErrorOr<Tree> TextParser::parse_condition()
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
TextParseErrorOr<Tree> TextParser::parse_return_statement()
{
    auto rollback = rollback_point();

    TRY(consume_word("return"sv));
    auto return_value = TRY(parse_expression());

    rollback.disarm();
    return make_ref_counted<ReturnNode>(return_value);
}

// assert: <condition>
TextParseErrorOr<Tree> TextParser::parse_assert()
{
    auto rollback = rollback_point();

    TRY(consume_token(TokenType::Identifier, "assert"sv));
    TRY(consume_token_with_type(TokenType::Colon));
    auto condition = TRY(parse_condition());

    rollback.disarm();
    return make_ref_counted<AssertExpression>(condition);
}

// (let <expr> be <expr>) | (set <expr> to <expr>)
TextParseErrorOr<Tree> TextParser::parse_assignment()
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
TextParseErrorOr<Tree> TextParser::parse_simple_step_or_inline_if_branch()
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

    return TextParseError {};
}

// <if_condition> :== (If <condition>) | (Else) | (Else if <condition>),
TextParseErrorOr<TextParser::IfConditionParseResult> TextParser::parse_if_beginning()
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
TextParseErrorOr<Tree> TextParser::parse_inline_if_else()
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
TextParseErrorOr<Tree> TextParser::parse_if(Tree then_branch)
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
TextParseErrorOr<Tree> TextParser::parse_else(Tree else_branch)
{
    auto rollback = rollback_point();

    TRY(consume_word("else"sv));
    TRY(consume_token_with_type(TokenType::Comma));
    TRY(expect_eof());

    rollback.disarm();
    return make_ref_counted<ElseIfBranch>(nullptr, else_branch);
}

// <simple_step> | <inline_if>
TextParseErrorOr<Tree> TextParser::parse_step_without_substeps()
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

    return TextParseError {};
}

// <if> | <else>
TextParseErrorOr<Tree> TextParser::parse_step_with_substeps(Tree substeps)
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

    return TextParseError {};
}

TextParseErrorOr<ClauseHeader> TextParser::parse_clause_header()
{
    ClauseHeader result;

    auto section_number_token = TRY(consume_token_with_type(TokenType::SectionNumber));
    result.section_number = section_number_token.data;

    ClauseHeader::FunctionDefinition function_definition;

    function_definition.name = TRY(consume_token_with_type(TokenType::Word)).data;

    TRY(consume_token_with_type(TokenType::ParenOpen));
    while (true) {
        if (function_definition.arguments.is_empty()) {
            auto argument = TRY(consume_token_with_one_of_types({ TokenType::ParenClose, TokenType::Identifier }));
            if (argument.type == TokenType::ParenClose)
                break;
            function_definition.arguments.append({ argument.data });
        } else {
            function_definition.arguments.append({ TRY(consume_token_with_type(TokenType::Identifier)).data });
        }
        auto next_token = TRY(consume_token_with_one_of_types({ TokenType::ParenClose, TokenType::Comma }));
        if (next_token.type == TokenType::ParenClose)
            break;
    }
    TRY(expect_eof());

    result.header = function_definition;

    return result;
}

FailedTextParseDiagnostic TextParser::get_diagnostic() const
{
    StringBuilder message;

    message.append("unexpected "sv);

    if (m_max_parsed_tokens == m_tokens.size()) {
        message.append("EOF"sv);
    } else {
        auto token = m_tokens[m_max_parsed_tokens];
        if (token.type == TokenType::Word)
            message.appendff("'{}'", token.data);
        else if (token.type == TokenType::Identifier)
            message.appendff("identifier '{}'", token.data);
        else
            message.append(token.name_for_diagnostic());
    }

    message.appendff(", expected ");

    size_t size = m_suitable_continuations.size();
    VERIFY(size > 0);

    for (size_t i = 0; i < size; ++i) {
        m_suitable_continuations[i].visit(
            [&](TokenType type) { message.append(token_info[to_underlying(type)].name_for_diagnostic); },
            [&](StringView word) { message.appendff("'{}'", word); },
            [&](CustomMessage continuation) { message.append(continuation.message); });

        if (i + 1 != size) {
            if (size == 2)
                message.append(" or "sv);
            else if (i + 2 == size)
                message.append(", or "sv);
            else
                message.append(", "sv);
        }
    }

    Location location = Location::global_scope();

    if (m_max_parsed_tokens < m_tokens.size()) {
        location = m_tokens[m_max_parsed_tokens].location;
    } else {
        // FIXME: Would be nice to point to the closing tag not the opening one. This is also the
        //        only place where we use m_location.
        location = m_ctx.location_from_xml_offset(m_node->offset);
    }

    return { location, MUST(message.to_string()) };
}

}
