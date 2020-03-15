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

Parser::Parser(Lexer lexer)
    : m_lexer(move(lexer))
    , m_current_token(m_lexer.next())
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

NonnullOwnPtr<Program> Parser::parse_program()
{
    auto program = make<Program>();
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

NonnullOwnPtr<Statement> Parser::parse_statement()
{
    if (match_expression()) {
        return make<JS::ExpressionStatement>(parse_expression(0));
    }

    switch (m_current_token.type()) {
    case TokenType::Function:
        return parse_function_declaration();
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
    default:
        m_has_errors = true;
        expected("statement (missing switch case)");
        consume();
        return make<ErrorStatement>();
    }
}

NonnullOwnPtr<Expression> Parser::parse_primary_expression()
{
    switch (m_current_token.type()) {
    case TokenType::ParenOpen: {
        consume(TokenType::ParenOpen);
        auto expression = parse_expression(0);
        consume(TokenType::ParenClose);
        return expression;
    }
    case TokenType::Identifier:
        return make<Identifier>(consume().value());
    case TokenType::NumericLiteral:
        return make<NumericLiteral>(consume().double_value());
    case TokenType::BoolLiteral:
        return make<BooleanLiteral>(consume().bool_value());
    case TokenType::StringLiteral:
        return make<StringLiteral>(consume().string_value());
    case TokenType::NullLiteral:
        consume();
        return make<NullLiteral>();
    case TokenType::UndefinedLiteral:
        consume();
        return make<UndefinedLiteral>();
    case TokenType::CurlyOpen:
        return parse_object_expression();
    default:
        m_has_errors = true;
        expected("primary expression (missing switch case)");
        consume();
        return make<ErrorExpression>();
    }
}

NonnullOwnPtr<Expression> Parser::parse_unary_prefixed_expression()
{
    switch (m_current_token.type()) {
    case TokenType::PlusPlus:
        consume();
        return make<UpdateExpression>(UpdateOp::Increment, parse_primary_expression(), true);
    case TokenType::MinusMinus:
        consume();
        return make<UpdateExpression>(UpdateOp::Decrement, parse_primary_expression(), true);
    case TokenType::ExclamationMark:
        consume();
        return make<UnaryExpression>(UnaryOp::Not, parse_primary_expression());
    case TokenType::Tilde:
        consume();
        return make<UnaryExpression>(UnaryOp::BitwiseNot, parse_primary_expression());
    default:
        m_has_errors = true;
        expected("primary expression (missing switch case)");
        consume();
        return make<ErrorExpression>();
    }
}

NonnullOwnPtr<ObjectExpression> Parser::parse_object_expression()
{
    // FIXME: Parse actual object expression
    consume(TokenType::CurlyOpen);
    consume(TokenType::CurlyClose);
    return make<ObjectExpression>();
}

NonnullOwnPtr<Expression> Parser::parse_expression(int min_precedence, Associativity associativity)
{
    if (match_unary_prefixed_expression())
        return parse_unary_prefixed_expression();

    auto expression = parse_primary_expression();
    while (match_secondary_expression()) {
        int new_precedence = operator_precedence(m_current_token.type());
        if (new_precedence < min_precedence)
            break;
        if (new_precedence == min_precedence && associativity == Associativity::Left)
            break;

        Associativity new_associativity = operator_associativity(m_current_token.type());
        expression = parse_secondary_expression(move(expression), new_precedence, new_associativity);
    }
    return expression;
}

NonnullOwnPtr<Expression> Parser::parse_secondary_expression(NonnullOwnPtr<Expression> lhs, int min_precedence, Associativity associativity)
{
    switch (m_current_token.type()) {
    case TokenType::Plus:
        consume();
        return make<BinaryExpression>(BinaryOp::Plus, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::PlusEquals:
        consume();
        return make<AssignmentExpression>(AssignmentOp::AdditionAssignment, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Minus:
        consume();
        return make<BinaryExpression>(BinaryOp::Minus, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::MinusEquals:
        consume();
        return make<AssignmentExpression>(AssignmentOp::SubtractionAssignment, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Asterisk:
        consume();
        return make<BinaryExpression>(BinaryOp::Asterisk, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::AsteriskEquals:
        consume();
        return make<AssignmentExpression>(AssignmentOp::MultiplicationAssignment, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Slash:
        consume();
        return make<BinaryExpression>(BinaryOp::Slash, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::SlashEquals:
        consume();
        return make<AssignmentExpression>(AssignmentOp::DivisionAssignment, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::GreaterThan:
        consume();
        return make<BinaryExpression>(BinaryOp::GreaterThan, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::GreaterThanEquals:
        consume();
        return make<BinaryExpression>(BinaryOp::GreaterThanEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::LessThan:
        consume();
        return make<BinaryExpression>(BinaryOp::LessThan, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::LessThanEquals:
        consume();
        return make<BinaryExpression>(BinaryOp::LessThanEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::EqualsEqualsEquals:
        consume();
        return make<BinaryExpression>(BinaryOp::TypedEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ExclamationMarkEqualsEquals:
        consume();
        return make<BinaryExpression>(BinaryOp::TypedInequals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::EqualsEquals:
        consume();
        return make<BinaryExpression>(BinaryOp::AbstractEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ExclamationMarkEquals:
        consume();
        return make<BinaryExpression>(BinaryOp::AbstractInequals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ParenOpen:
        return parse_call_expression(move(lhs));
    case TokenType::Equals:
        consume();
        return make<AssignmentExpression>(AssignmentOp::Assignment, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Period:
        consume();
        return make<MemberExpression>(move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::PlusPlus:
        consume();
        return make<UpdateExpression>(UpdateOp::Increment, move(lhs));
    case TokenType::MinusMinus:
        consume();
        return make<UpdateExpression>(UpdateOp::Decrement, move(lhs));
    case TokenType::DoubleAmpersand:
        consume();
        return make<LogicalExpression>(LogicalOp::And, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoublePipe:
        consume();
        return make<LogicalExpression>(LogicalOp::Or, move(lhs), parse_expression(min_precedence, associativity));
    default:
        m_has_errors = true;
        expected("secondary expression (missing switch case)");
        consume();
        return make<ErrorExpression>();
    }
}

NonnullOwnPtr<CallExpression> Parser::parse_call_expression(NonnullOwnPtr<Expression> lhs)
{
    consume(TokenType::ParenOpen);

    NonnullOwnPtrVector<Expression> arguments;

    while (match_expression()) {
        arguments.append(parse_expression(0));
        if (!match(TokenType::Comma))
            break;
        consume();
    }

    consume(TokenType::ParenClose);

    return make<CallExpression>(move(lhs), move(arguments));
}

NonnullOwnPtr<ReturnStatement> Parser::parse_return_statement()
{
    consume(TokenType::Return);
    if (match_expression()) {
        return make<ReturnStatement>(parse_expression(0));
    }
    return make<ReturnStatement>(nullptr);
}

NonnullOwnPtr<BlockStatement> Parser::parse_block_statement()
{
    auto block = make<BlockStatement>();
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

NonnullOwnPtr<FunctionDeclaration> Parser::parse_function_declaration()
{
    consume(TokenType::Function);
    auto name = consume(TokenType::Identifier).value();
    consume(TokenType::ParenOpen);
    Vector<String> parameters;
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
    return make<FunctionDeclaration>(name, move(body), move(parameters));
}

NonnullOwnPtr<VariableDeclaration> Parser::parse_variable_declaration()
{
    DeclarationType declaration_type;

    switch (m_current_token.type()) {
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
    auto name = consume(TokenType::Identifier).value();
    OwnPtr<Expression> initializer;
    if (match(TokenType::Equals)) {
        consume();
        initializer = parse_expression(0);
    }
    return make<VariableDeclaration>(make<Identifier>(name), move(initializer), declaration_type);
}

NonnullOwnPtr<ForStatement> Parser::parse_for_statement()
{
    consume(TokenType::For);

    consume(TokenType::ParenOpen);

    OwnPtr<Statement> init = nullptr;
    switch (m_current_token.type()) {
    case TokenType::Semicolon:
        break;
    default:
        init = parse_statement();
        break;
    }

    consume(TokenType::Semicolon);

    OwnPtr<Expression> test = nullptr;
    switch (m_current_token.type()) {
    case TokenType::Semicolon:
        break;
    default:
        test = parse_expression(0);
        break;
    }

    consume(TokenType::Semicolon);

    OwnPtr<Expression> update = nullptr;
    switch (m_current_token.type()) {
    case TokenType::Semicolon:
        break;
    default:
        update = parse_expression(0);
        break;
    }

    consume(TokenType::ParenClose);

    auto body = parse_block_statement();

    return make<ForStatement>(move(init), move(test), move(update), move(body));
}

bool Parser::match(TokenType type) const
{
    return m_current_token.type() == type;
}

bool Parser::match_expression() const
{
    auto type = m_current_token.type();
    return type == TokenType::BoolLiteral
        || type == TokenType::NumericLiteral
        || type == TokenType::StringLiteral
        || type == TokenType::UndefinedLiteral
        || type == TokenType::NullLiteral
        || type == TokenType::Identifier
        || type == TokenType::New
        || type == TokenType::CurlyOpen
        || type == TokenType::BracketOpen
        || type == TokenType::ParenOpen
        || match_unary_prefixed_expression();
}

bool Parser::match_unary_prefixed_expression() const
{
    auto type = m_current_token.type();
    return type == TokenType::PlusPlus
        || type == TokenType::MinusMinus
        || type == TokenType::ExclamationMark
        || type == TokenType::Tilde;
}

bool Parser::match_secondary_expression() const
{
    auto type = m_current_token.type();
    return type == TokenType::Plus
        || type == TokenType::PlusEquals
        || type == TokenType::Minus
        || type == TokenType::MinusEquals
        || type == TokenType::Asterisk
        || type == TokenType::AsteriskEquals
        || type == TokenType::Slash
        || type == TokenType::SlashEquals
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
        || type == TokenType::PlusPlus
        || type == TokenType::MinusMinus;
}

bool Parser::match_statement() const
{
    auto type = m_current_token.type();
    return match_expression()
        || type == TokenType::Function
        || type == TokenType::Return
        || type == TokenType::Let
        || type == TokenType::Catch
        || type == TokenType::Class
        || type == TokenType::Delete
        || type == TokenType::Do
        || type == TokenType::If
        || type == TokenType::Try
        || type == TokenType::While
        || type == TokenType::For
        || type == TokenType::Const
        || type == TokenType::CurlyOpen
        || type == TokenType::Var;
}

bool Parser::done() const
{
    return match(TokenType::Eof);
}

Token Parser::consume()
{
    auto old_token = m_current_token;
    m_current_token = m_lexer.next();
    return old_token;
}

Token Parser::consume(TokenType type)
{
    if (m_current_token.type() != type) {
        m_has_errors = true;
        fprintf(stderr, "Error: Unexpected token %s. Expected %s\n", m_current_token.name(), Token::name(type));
    }
    return consume();
}

void Parser::expected(const char* what)
{
    m_has_errors = true;
    fprintf(stderr, "Error: Unexpected token %s. Expected %s\n", m_current_token.name(), what);
}

}
