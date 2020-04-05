/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Parser.h"
#include <AK/HashMap.h>
#include <AK/StdLibExtras.h>
#include <stdio.h>

namespace JS {

static HashMap<TokenType, int> g_operator_precedence;
Parser::ParserState::ParserState(Lexer lexer)
    : m_lexer(move(lexer))
    , m_current_token(m_lexer.next())
{
}

Parser::Parser(Lexer lexer)
    : m_parser_state(move(lexer))
{
    if (g_operator_precedence.is_empty()) {
        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Operator_Precedence
        g_operator_precedence.set(TokenType::Period, 20);
        g_operator_precedence.set(TokenType::BracketOpen, 20);
        g_operator_precedence.set(TokenType::ParenOpen, 20);
        g_operator_precedence.set(TokenType::QuestionMarkPeriod, 20);

        g_operator_precedence.set(TokenType::New, 19);

        g_operator_precedence.set(TokenType::PlusPlus, 18);
        g_operator_precedence.set(TokenType::MinusMinus, 18);

        g_operator_precedence.set(TokenType::ExclamationMark, 17);
        g_operator_precedence.set(TokenType::Tilde, 17);
        g_operator_precedence.set(TokenType::Typeof, 17);
        g_operator_precedence.set(TokenType::Void, 17);
        g_operator_precedence.set(TokenType::Delete, 17);
        g_operator_precedence.set(TokenType::Await, 17);

        g_operator_precedence.set(TokenType::DoubleAsterisk, 16);

        g_operator_precedence.set(TokenType::Asterisk, 15);
        g_operator_precedence.set(TokenType::Slash, 15);
        g_operator_precedence.set(TokenType::Percent, 15);

        g_operator_precedence.set(TokenType::Plus, 14);
        g_operator_precedence.set(TokenType::Minus, 14);

        g_operator_precedence.set(TokenType::ShiftLeft, 13);
        g_operator_precedence.set(TokenType::ShiftRight, 13);
        g_operator_precedence.set(TokenType::UnsignedShiftRight, 13);

        g_operator_precedence.set(TokenType::LessThan, 12);
        g_operator_precedence.set(TokenType::LessThanEquals, 12);
        g_operator_precedence.set(TokenType::GreaterThan, 12);
        g_operator_precedence.set(TokenType::GreaterThanEquals, 12);
        g_operator_precedence.set(TokenType::In, 12);
        g_operator_precedence.set(TokenType::Instanceof, 12);

        g_operator_precedence.set(TokenType::EqualsEquals, 11);
        g_operator_precedence.set(TokenType::ExclamationMarkEquals, 11);
        g_operator_precedence.set(TokenType::EqualsEqualsEquals, 11);
        g_operator_precedence.set(TokenType::ExclamationMarkEqualsEquals, 11);

        g_operator_precedence.set(TokenType::Ampersand, 10);

        g_operator_precedence.set(TokenType::Caret, 9);

        g_operator_precedence.set(TokenType::Pipe, 8);

        g_operator_precedence.set(TokenType::DoubleQuestionMark, 7);

        g_operator_precedence.set(TokenType::DoubleAmpersand, 6);

        g_operator_precedence.set(TokenType::DoublePipe, 5);

        g_operator_precedence.set(TokenType::QuestionMark, 4);

        g_operator_precedence.set(TokenType::Equals, 3);
        g_operator_precedence.set(TokenType::PlusEquals, 3);
        g_operator_precedence.set(TokenType::MinusEquals, 3);
        g_operator_precedence.set(TokenType::AsteriskAsteriskEquals, 3);
        g_operator_precedence.set(TokenType::AsteriskEquals, 3);
        g_operator_precedence.set(TokenType::SlashEquals, 3);
        g_operator_precedence.set(TokenType::PercentEquals, 3);
        g_operator_precedence.set(TokenType::ShiftLeftEquals, 3);
        g_operator_precedence.set(TokenType::ShiftRightEquals, 3);
        g_operator_precedence.set(TokenType::UnsignedShiftRightEquals, 3);
        g_operator_precedence.set(TokenType::PipeEquals, 3);

        g_operator_precedence.set(TokenType::Yield, 2);

        g_operator_precedence.set(TokenType::Comma, 1);
    }
}

int Parser::operator_precedence(TokenType type) const
{
    auto it = g_operator_precedence.find(type);
    if (it == g_operator_precedence.end()) {
        fprintf(stderr, "No precedence for operator %s\n", Token::name(type));
        ASSERT_NOT_REACHED();
        return -1;
    }

    return it->value;
}

Associativity Parser::operator_associativity(TokenType type) const
{
    switch (type) {
    case TokenType::Period:
    case TokenType::BracketOpen:
    case TokenType::ParenOpen:
    case TokenType::QuestionMarkPeriod:
    case TokenType::Asterisk:
    case TokenType::Slash:
    case TokenType::Percent:
    case TokenType::Plus:
    case TokenType::Minus:
    case TokenType::ShiftLeft:
    case TokenType::ShiftRight:
    case TokenType::UnsignedShiftRight:
    case TokenType::LessThan:
    case TokenType::LessThanEquals:
    case TokenType::GreaterThan:
    case TokenType::GreaterThanEquals:
    case TokenType::In:
    case TokenType::Instanceof:
    case TokenType::EqualsEquals:
    case TokenType::ExclamationMarkEquals:
    case TokenType::EqualsEqualsEquals:
    case TokenType::ExclamationMarkEqualsEquals:
    case TokenType::Typeof:
    case TokenType::Ampersand:
    case TokenType::Caret:
    case TokenType::Pipe:
    case TokenType::DoubleQuestionMark:
    case TokenType::DoubleAmpersand:
    case TokenType::DoublePipe:
    case TokenType::Comma:
        return Associativity::Left;
    default:
        return Associativity::Right;
    }
}

NonnullRefPtr<Program> Parser::parse_program()
{
    auto program = adopt(*new Program);
    while (!done()) {
        if (match(TokenType::Semicolon)) {
            consume();
        } else if (match_statement()) {
            program->append(parse_statement());
        } else {
            expected("statement");
            consume();
        }
    }
    return program;
}

NonnullRefPtr<Statement> Parser::parse_statement()
{
    auto statement = [this]() -> NonnullRefPtr<Statement> {
    switch (m_parser_state.m_current_token.type()) {
    case TokenType::Function:
        return parse_function_node<FunctionDeclaration>();
    case TokenType::CurlyOpen:
        return parse_block_statement();
    case TokenType::Return:
        return parse_return_statement();
    case TokenType::Var:
    case TokenType::Let:
    case TokenType::Const:
        return parse_variable_declaration();
    case TokenType::For:
        return parse_for_statement();
    case TokenType::If:
        return parse_if_statement();
    case TokenType::Throw:
        return parse_throw_statement();
    case TokenType::Try:
        return parse_try_statement();
    case TokenType::Break:
        return parse_break_statement();
    case TokenType::Continue:
         return parse_continue_statement();
    case TokenType::Switch:
        return parse_switch_statement();
    case TokenType::Do:
        return parse_do_while_statement();
    default:
        if (match_expression())
            return adopt(*new ExpressionStatement(parse_expression(0)));
        m_parser_state.m_has_errors = true;
        expected("statement (missing switch case)");
        consume();
        return create_ast_node<ErrorStatement>();
    } }();
    if (match(TokenType::Semicolon))
        consume();
    return statement;
}

RefPtr<FunctionExpression> Parser::try_parse_arrow_function_expression(bool expect_parens)
{
    save_state();

    Vector<FlyString> parameters;
    bool parse_failed = false;
    while (true) {
        if (match(TokenType::Comma)) {
            consume(TokenType::Comma);
        } else if (match(TokenType::Identifier)) {
            auto token = consume(TokenType::Identifier);
            parameters.append(token.value());
        } else if (match(TokenType::ParenClose)) {
            if (expect_parens) {
                consume(TokenType::ParenClose);
                if (match(TokenType::Arrow)) {
                    consume(TokenType::Arrow);
                } else {
                    parse_failed = true;
                }
                break;
            }
            parse_failed = true;
            break;
        } else if (match(TokenType::Arrow)) {
            if (!expect_parens) {
                consume(TokenType::Arrow);
                break;
            }
            parse_failed = true;
            break;
        } else {
            parse_failed = true;
            break;
        }
    }

    if (parse_failed) {
        load_state();
        return nullptr;
    }

    auto function_body_result = [this]() -> RefPtr<BlockStatement> {
        if (match(TokenType::CurlyOpen)) {
            // Parse a function body with statements
            return parse_block_statement();
        }
        if (match_expression()) {
            // Parse a function body which returns a single expression

            // FIXME: We synthesize a block with a return statement
            // for arrow function bodies which are a single expression.
            // Esprima generates a single "ArrowFunctionExpression"
            // with a "body" property.
            auto return_expression = parse_expression(0);
            auto return_block = create_ast_node<BlockStatement>();
            return_block->append<ReturnStatement>(move(return_expression));
            return return_block;
        }
        // Invalid arrow function body
        return nullptr;
    }();

    if (!function_body_result.is_null()) {
        auto body = function_body_result.release_nonnull();
        return create_ast_node<FunctionExpression>("", move(body), move(parameters));
    }

    load_state();
    return nullptr;
}

NonnullRefPtr<Expression> Parser::parse_primary_expression()
{
    if (match_unary_prefixed_expression())
        return parse_unary_prefixed_expression();

    switch (m_parser_state.m_current_token.type()) {
    case TokenType::ParenOpen: {
        consume(TokenType::ParenOpen);
        if (match(TokenType::ParenClose) || match(TokenType::Identifier)) {
            auto arrow_function_result = try_parse_arrow_function_expression(true);
            if (!arrow_function_result.is_null()) {
                return arrow_function_result.release_nonnull();
            }
        }
        auto expression = parse_expression(0);
        consume(TokenType::ParenClose);
        return expression;
    }
    case TokenType::Identifier: {
        auto arrow_function_result = try_parse_arrow_function_expression(false);
        if (!arrow_function_result.is_null()) {
            return arrow_function_result.release_nonnull();
        }
        return create_ast_node<Identifier>(consume().value());
    }
    case TokenType::NumericLiteral:
        return create_ast_node<NumericLiteral>(consume().double_value());
    case TokenType::BoolLiteral:
        return create_ast_node<BooleanLiteral>(consume().bool_value());
    case TokenType::StringLiteral:
        return create_ast_node<StringLiteral>(consume().string_value());
    case TokenType::NullLiteral:
        consume();
        return create_ast_node<NullLiteral>();
    case TokenType::CurlyOpen:
        return parse_object_expression();
    case TokenType::Function:
        return parse_function_node<FunctionExpression>();
    case TokenType::BracketOpen:
        return parse_array_expression();
    case TokenType::New:
        return parse_new_expression();
    default:
        m_parser_state.m_has_errors = true;
        expected("primary expression (missing switch case)");
        consume();
        return create_ast_node<ErrorExpression>();
    }
}

NonnullRefPtr<Expression> Parser::parse_unary_prefixed_expression()
{
    auto precedence = operator_precedence(m_parser_state.m_current_token.type());
    auto associativity = operator_associativity(m_parser_state.m_current_token.type());
    switch (m_parser_state.m_current_token.type()) {
    case TokenType::PlusPlus:
        consume();
        return create_ast_node<UpdateExpression>(UpdateOp::Increment, parse_expression(precedence, associativity), true);
    case TokenType::MinusMinus:
        consume();
        return create_ast_node<UpdateExpression>(UpdateOp::Decrement, parse_expression(precedence, associativity), true);
    case TokenType::ExclamationMark:
        consume();
        return create_ast_node<UnaryExpression>(UnaryOp::Not, parse_expression(precedence, associativity));
    case TokenType::Tilde:
        consume();
        return create_ast_node<UnaryExpression>(UnaryOp::BitwiseNot, parse_expression(precedence, associativity));
    case TokenType::Plus:
        consume();
        return create_ast_node<UnaryExpression>(UnaryOp::Plus, parse_expression(precedence, associativity));
    case TokenType::Minus:
        consume();
        return create_ast_node<UnaryExpression>(UnaryOp::Minus, parse_expression(precedence, associativity));
    case TokenType::Typeof:
        consume();
        return create_ast_node<UnaryExpression>(UnaryOp::Typeof, parse_expression(precedence, associativity));
    default:
        m_parser_state.m_has_errors = true;
        expected("primary expression (missing switch case)");
        consume();
        return create_ast_node<ErrorExpression>();
    }
}

NonnullRefPtr<ObjectExpression> Parser::parse_object_expression()
{
    HashMap<FlyString, NonnullRefPtr<Expression>> properties;
    consume(TokenType::CurlyOpen);

    while (!match(TokenType::CurlyClose)) {
        auto identifier = create_ast_node<Identifier>(consume(TokenType::Identifier).value());

        if (match(TokenType::Colon)) {
            consume(TokenType::Colon);
            properties.set(identifier->string(), parse_expression(0));
        } else {
            properties.set(identifier->string(), identifier);
        }

        if (!match(TokenType::Comma))
            break;

        consume(TokenType::Comma);
    }

    consume(TokenType::CurlyClose);
    return create_ast_node<ObjectExpression>(properties);
}

NonnullRefPtr<ArrayExpression> Parser::parse_array_expression()
{
    consume(TokenType::BracketOpen);

    NonnullRefPtrVector<Expression> elements;
    while (match_expression()) {
        elements.append(parse_expression(0));
        if (!match(TokenType::Comma))
            break;
        consume(TokenType::Comma);
    }

    consume(TokenType::BracketClose);
    return create_ast_node<ArrayExpression>(move(elements));
}

NonnullRefPtr<Expression> Parser::parse_expression(int min_precedence, Associativity associativity)
{
    auto expression = parse_primary_expression();
    while (match_secondary_expression()) {
        int new_precedence = operator_precedence(m_parser_state.m_current_token.type());
        if (new_precedence < min_precedence)
            break;
        if (new_precedence == min_precedence && associativity == Associativity::Left)
            break;

        Associativity new_associativity = operator_associativity(m_parser_state.m_current_token.type());
        expression = parse_secondary_expression(move(expression), new_precedence, new_associativity);
    }
    return expression;
}

NonnullRefPtr<Expression> Parser::parse_secondary_expression(NonnullRefPtr<Expression> lhs, int min_precedence, Associativity associativity)
{
    switch (m_parser_state.m_current_token.type()) {
    case TokenType::Plus:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::Plus, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::PlusEquals:
        consume();
        return create_ast_node<AssignmentExpression>(AssignmentOp::AdditionAssignment, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Minus:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::Minus, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::MinusEquals:
        consume();
        return create_ast_node<AssignmentExpression>(AssignmentOp::SubtractionAssignment, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Asterisk:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::Asterisk, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::AsteriskEquals:
        consume();
        return create_ast_node<AssignmentExpression>(AssignmentOp::MultiplicationAssignment, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Slash:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::Slash, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::SlashEquals:
        consume();
        return create_ast_node<AssignmentExpression>(AssignmentOp::DivisionAssignment, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Percent:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::Modulo, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::GreaterThan:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::GreaterThan, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::GreaterThanEquals:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::GreaterThanEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::LessThan:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::LessThan, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::LessThanEquals:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::LessThanEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::EqualsEqualsEquals:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::TypedEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ExclamationMarkEqualsEquals:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::TypedInequals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::EqualsEquals:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::AbstractEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ExclamationMarkEquals:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::AbstractInequals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Instanceof:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::InstanceOf, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Ampersand:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::BitwiseAnd, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Pipe:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::BitwiseOr, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Caret:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::BitwiseXor, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ParenOpen:
        return parse_call_expression(move(lhs));
    case TokenType::Equals:
        consume();
        return create_ast_node<AssignmentExpression>(AssignmentOp::Assignment, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Period:
        consume();
        return create_ast_node<MemberExpression>(move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::BracketOpen: {
        consume(TokenType::BracketOpen);
        auto expression = create_ast_node<MemberExpression>(move(lhs), parse_expression(0), true);
        consume(TokenType::BracketClose);
        return expression;
    }
    case TokenType::PlusPlus:
        consume();
        return create_ast_node<UpdateExpression>(UpdateOp::Increment, move(lhs));
    case TokenType::MinusMinus:
        consume();
        return create_ast_node<UpdateExpression>(UpdateOp::Decrement, move(lhs));
    case TokenType::DoubleAmpersand:
        consume();
        return create_ast_node<LogicalExpression>(LogicalOp::And, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoublePipe:
        consume();
        return create_ast_node<LogicalExpression>(LogicalOp::Or, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::QuestionMark:
        return parse_conditional_expression(move(lhs));
    default:
        m_parser_state.m_has_errors = true;
        expected("secondary expression (missing switch case)");
        consume();
        return create_ast_node<ErrorExpression>();
    }
}

NonnullRefPtr<CallExpression> Parser::parse_call_expression(NonnullRefPtr<Expression> lhs)
{
    consume(TokenType::ParenOpen);

    NonnullRefPtrVector<Expression> arguments;

    while (match_expression()) {
        arguments.append(parse_expression(0));
        if (!match(TokenType::Comma))
            break;
        consume();
    }

    consume(TokenType::ParenClose);

    return create_ast_node<CallExpression>(move(lhs), move(arguments));
}

NonnullRefPtr<NewExpression> Parser::parse_new_expression()
{
    consume(TokenType::New);

    // FIXME: Support full expressions as the callee as well.
    auto callee = create_ast_node<Identifier>(consume(TokenType::Identifier).value());

    NonnullRefPtrVector<Expression> arguments;

    if (match(TokenType::ParenOpen)) {
        consume(TokenType::ParenOpen);
        while (match_expression()) {
            arguments.append(parse_expression(0));
            if (!match(TokenType::Comma))
                break;
            consume();
        }
        consume(TokenType::ParenClose);
    }

    return create_ast_node<NewExpression>(move(callee), move(arguments));
}

NonnullRefPtr<ReturnStatement> Parser::parse_return_statement()
{
    consume(TokenType::Return);
    if (match_expression()) {
        return create_ast_node<ReturnStatement>(parse_expression(0));
    }
    return create_ast_node<ReturnStatement>(nullptr);
}

NonnullRefPtr<BlockStatement> Parser::parse_block_statement()
{
    auto block = create_ast_node<BlockStatement>();
    consume(TokenType::CurlyOpen);
    while (!done() && !match(TokenType::CurlyClose)) {
        if (match(TokenType::Semicolon)) {
            consume();
        } else if (match_statement()) {
            block->append(parse_statement());
        } else {
            expected("statement");
            consume();
        }
    }
    consume(TokenType::CurlyClose);
    return block;
}

template<typename FunctionNodeType>
NonnullRefPtr<FunctionNodeType> Parser::parse_function_node()
{
    consume(TokenType::Function);
    String name;
    if (FunctionNodeType::must_have_name()) {
        name = consume(TokenType::Identifier).value();
    } else {
        if (match(TokenType::Identifier))
            name = consume(TokenType::Identifier).value();
    }
    consume(TokenType::ParenOpen);
    Vector<FlyString> parameters;
    while (match(TokenType::Identifier)) {
        auto parameter = consume(TokenType::Identifier).value();
        parameters.append(parameter);
        if (match(TokenType::ParenClose)) {
            break;
        }
        consume(TokenType::Comma);
    }
    consume(TokenType::ParenClose);
    auto body = parse_block_statement();
    return create_ast_node<FunctionNodeType>(name, move(body), move(parameters));
}

NonnullRefPtr<VariableDeclaration> Parser::parse_variable_declaration()
{
    DeclarationType declaration_type;

    switch (m_parser_state.m_current_token.type()) {
    case TokenType::Var:
        declaration_type = DeclarationType::Var;
        consume(TokenType::Var);
        break;
    case TokenType::Let:
        declaration_type = DeclarationType::Let;
        consume(TokenType::Let);
        break;
    case TokenType::Const:
        declaration_type = DeclarationType::Const;
        consume(TokenType::Const);
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    NonnullRefPtrVector<VariableDeclarator> declarations;
    for (;;) {
        auto id = consume(TokenType::Identifier).value();
        RefPtr<Expression> init;
        if (match(TokenType::Equals)) {
            consume();
            init = parse_expression(0);
        }
        declarations.append(create_ast_node<VariableDeclarator>(create_ast_node<Identifier>(move(id)), move(init)));
        if (match(TokenType::Comma)) {
            consume();
            continue;
        }
        break;
    }
    return create_ast_node<VariableDeclaration>(declaration_type, move(declarations));
}

NonnullRefPtr<ThrowStatement> Parser::parse_throw_statement()
{
    consume(TokenType::Throw);
    return create_ast_node<ThrowStatement>(parse_expression(0));
}

NonnullRefPtr<BreakStatement> Parser::parse_break_statement()
{
    consume(TokenType::Break);
    // FIXME: Handle labels.
    return create_ast_node<BreakStatement>();
}

NonnullRefPtr<ContinueStatement> Parser::parse_continue_statement()
{
    consume(TokenType::Continue);
    // FIXME: Handle labels.
    return create_ast_node<ContinueStatement>();
}

NonnullRefPtr<ConditionalExpression> Parser::parse_conditional_expression(NonnullRefPtr<Expression> test)
{
    consume(TokenType::QuestionMark);
    auto consequent = parse_expression(0);
    consume(TokenType::Colon);
    auto alternate = parse_expression(0);
    return create_ast_node<ConditionalExpression>(move(test), move(consequent), move(alternate));
}

NonnullRefPtr<TryStatement> Parser::parse_try_statement()
{
    consume(TokenType::Try);

    auto block = parse_block_statement();

    RefPtr<CatchClause> handler;
    if (match(TokenType::Catch))
        handler = parse_catch_clause();

    RefPtr<BlockStatement> finalizer;
    if (match(TokenType::Finally)) {
        consume();
        finalizer = parse_block_statement();
    }

    return create_ast_node<TryStatement>(move(block), move(handler), move(finalizer));
}

NonnullRefPtr<DoWhileStatement> Parser::parse_do_while_statement()
{
    consume(TokenType::Do);

    auto body = parse_statement();

    consume(TokenType::While);
    consume(TokenType::ParenOpen);

    auto test = parse_expression(0);

    consume(TokenType::ParenClose);

    return create_ast_node<DoWhileStatement>(move(test), move(body));
}

NonnullRefPtr<SwitchStatement> Parser::parse_switch_statement()
{
    consume(TokenType::Switch);

    consume(TokenType::ParenOpen);
    auto determinant = parse_expression(0);
    consume(TokenType::ParenClose);

    consume(TokenType::CurlyOpen);

    NonnullRefPtrVector<SwitchCase> cases;

    while (match(TokenType::Case) || match(TokenType::Default))
        cases.append(parse_switch_case());

    consume(TokenType::CurlyClose);

    return create_ast_node<SwitchStatement>(move(determinant), move(cases));
}

NonnullRefPtr<SwitchCase> Parser::parse_switch_case()
{
    RefPtr<Expression> test;

    if (consume().type() == TokenType::Case) {
        test = parse_expression(0);
    }

    consume(TokenType::Colon);

    NonnullRefPtrVector<Statement> consequent;
    while (match_statement())
        consequent.append(parse_statement());

    return create_ast_node<SwitchCase>(move(test), move(consequent));
}

NonnullRefPtr<CatchClause> Parser::parse_catch_clause()
{
    consume(TokenType::Catch);

    String parameter;
    if (match(TokenType::ParenOpen)) {
        consume();
        parameter = consume(TokenType::Identifier).value();
        consume(TokenType::ParenClose);
    }

    auto body = parse_block_statement();
    return create_ast_node<CatchClause>(parameter, move(body));
}

NonnullRefPtr<IfStatement> Parser::parse_if_statement()
{
    consume(TokenType::If);
    consume(TokenType::ParenOpen);
    auto predicate = parse_expression(0);
    consume(TokenType::ParenClose);
    auto consequent = parse_statement();
    RefPtr<Statement> alternate;
    if (match(TokenType::Else)) {
        consume(TokenType::Else);
        alternate = parse_statement();
    }
    return create_ast_node<IfStatement>(move(predicate), move(consequent), move(alternate));
}

NonnullRefPtr<ForStatement> Parser::parse_for_statement()
{
    consume(TokenType::For);

    consume(TokenType::ParenOpen);

    RefPtr<ASTNode> init;
    switch (m_parser_state.m_current_token.type()) {
    case TokenType::Semicolon:
        break;
    default:
        if (match_expression())
            init = parse_expression(0);
        else if (match_variable_declaration())
            init = parse_variable_declaration();
        else
            ASSERT_NOT_REACHED();
        break;
    }

    consume(TokenType::Semicolon);

    RefPtr<Expression> test;
    switch (m_parser_state.m_current_token.type()) {
    case TokenType::Semicolon:
        break;
    default:
        test = parse_expression(0);
        break;
    }

    consume(TokenType::Semicolon);

    RefPtr<Expression> update;
    switch (m_parser_state.m_current_token.type()) {
    case TokenType::ParenClose:
        break;
    default:
        update = parse_expression(0);
        break;
    }

    consume(TokenType::ParenClose);

    auto body = parse_statement();

    return create_ast_node<ForStatement>(move(init), move(test), move(update), move(body));
}

bool Parser::match(TokenType type) const
{
    return m_parser_state.m_current_token.type() == type;
}

bool Parser::match_variable_declaration() const
{
    switch (m_parser_state.m_current_token.type()) {
    case TokenType::Var:
    case TokenType::Let:
    case TokenType::Const:
        return true;
    default:
        return false;
    }
}

bool Parser::match_expression() const
{
    auto type = m_parser_state.m_current_token.type();
    return type == TokenType::BoolLiteral
        || type == TokenType::NumericLiteral
        || type == TokenType::StringLiteral
        || type == TokenType::NullLiteral
        || type == TokenType::Identifier
        || type == TokenType::New
        || type == TokenType::CurlyOpen
        || type == TokenType::BracketOpen
        || type == TokenType::ParenOpen
        || type == TokenType::Function
        || match_unary_prefixed_expression();
}

bool Parser::match_unary_prefixed_expression() const
{
    auto type = m_parser_state.m_current_token.type();
    return type == TokenType::PlusPlus
        || type == TokenType::MinusMinus
        || type == TokenType::ExclamationMark
        || type == TokenType::Tilde
        || type == TokenType::Plus
        || type == TokenType::Minus
        || type == TokenType::Typeof;
}

bool Parser::match_secondary_expression() const
{
    auto type = m_parser_state.m_current_token.type();
    return type == TokenType::Plus
        || type == TokenType::PlusEquals
        || type == TokenType::Minus
        || type == TokenType::MinusEquals
        || type == TokenType::Asterisk
        || type == TokenType::AsteriskEquals
        || type == TokenType::Slash
        || type == TokenType::SlashEquals
        || type == TokenType::Percent
        || type == TokenType::Equals
        || type == TokenType::EqualsEqualsEquals
        || type == TokenType::ExclamationMarkEqualsEquals
        || type == TokenType::EqualsEquals
        || type == TokenType::ExclamationMarkEquals
        || type == TokenType::GreaterThan
        || type == TokenType::GreaterThanEquals
        || type == TokenType::LessThan
        || type == TokenType::LessThanEquals
        || type == TokenType::ParenOpen
        || type == TokenType::Period
        || type == TokenType::BracketOpen
        || type == TokenType::PlusPlus
        || type == TokenType::MinusMinus
        || type == TokenType::Instanceof
        || type == TokenType::QuestionMark
        || type == TokenType::Ampersand
        || type == TokenType::Pipe
        || type == TokenType::Caret
        || type == TokenType::DoubleAmpersand
        || type == TokenType::DoublePipe;
}

bool Parser::match_statement() const
{
    auto type = m_parser_state.m_current_token.type();
    return match_expression()
        || type == TokenType::Function
        || type == TokenType::Return
        || type == TokenType::Let
        || type == TokenType::Class
        || type == TokenType::Delete
        || type == TokenType::Do
        || type == TokenType::If
        || type == TokenType::Throw
        || type == TokenType::Try
        || type == TokenType::While
        || type == TokenType::For
        || type == TokenType::Const
        || type == TokenType::CurlyOpen
        || type == TokenType::Switch
        || type == TokenType::Break
        || type == TokenType::Continue
        || type == TokenType::Var;
}

bool Parser::done() const
{
    return match(TokenType::Eof);
}

Token Parser::consume()
{
    auto old_token = m_parser_state.m_current_token;
    m_parser_state.m_current_token = m_parser_state.m_lexer.next();
    return old_token;
}

Token Parser::consume(TokenType type)
{
    if (m_parser_state.m_current_token.type() != type) {
        m_parser_state.m_has_errors = true;
        auto& current_token = m_parser_state.m_current_token;
        fprintf(stderr, "Error: Unexpected token %s. Expected %s (line: %zu, column: %zu))\n",
                current_token.name(),
                Token::name(type),
                current_token.line_number(),
                current_token.line_column());
    }
    return consume();
}

void Parser::expected(const char* what)
{
    m_parser_state.m_has_errors = true;
    auto& current_token = m_parser_state.m_current_token;
    fprintf(stderr, "Error: Unexpected token %s. Expected %s (line: %zu, column: %zu)\n",
            current_token.name(),
            what,
            current_token.line_number(),
            current_token.line_column());
}

void Parser::save_state()
{
    m_saved_state = m_parser_state;
}

void Parser::load_state()
{
    ASSERT(m_saved_state.has_value());
    m_parser_state = m_saved_state.value();
    m_saved_state.clear();
}
}
