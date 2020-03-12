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
#include <AK/StdLibExtras.h>
#include <stdio.h>

namespace JS {
Parser::Parser(Lexer lexer)
    : m_lexer(move(lexer))
    , m_current_token(m_lexer.next())
{
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
        return make<JS::ExpressionStatement>(parse_expression());
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
        auto expression = parse_expression();
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
    case TokenType::CurlyOpen:
        return parse_object_expression();
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

NonnullOwnPtr<Expression> Parser::parse_expression()
{
    auto expression = parse_primary_expression();
    while (match_secondary_expression()) {
        expression = parse_secondary_expression(move(expression));
    }
    return expression;
}

NonnullOwnPtr<Expression> Parser::parse_secondary_expression(NonnullOwnPtr<Expression> lhs)
{
    switch (m_current_token.type()) {
    case TokenType::Plus:
        consume();
        return make<BinaryExpression>(BinaryOp::Plus, move(lhs), parse_expression());
    case TokenType::PlusEquals:
        consume();
        return make<AssignmentExpression>(AssignmentOp::AdditionAssignment, move(lhs), parse_expression());
    case TokenType::Minus:
        consume();
        return make<BinaryExpression>(BinaryOp::Minus, move(lhs), parse_expression());
    case TokenType::MinusEquals:
        consume();
        return make<AssignmentExpression>(AssignmentOp::SubtractionAssignment, move(lhs), parse_expression());
    case TokenType::Asterisk:
        consume();
        return make<BinaryExpression>(BinaryOp::Asterisk, move(lhs), parse_expression());
    case TokenType::AsteriskEquals:
        consume();
        return make<AssignmentExpression>(AssignmentOp::MultiplicationAssignment, move(lhs), parse_expression());
    case TokenType::Slash:
        consume();
        return make<BinaryExpression>(BinaryOp::Slash, move(lhs), parse_expression());
    case TokenType::SlashEquals:
        consume();
        return make<AssignmentExpression>(AssignmentOp::DivisionAssignment, move(lhs), parse_expression());
    case TokenType::GreaterThan:
        consume();
        return make<BinaryExpression>(BinaryOp::GreaterThan, move(lhs), parse_expression());
    case TokenType::GreaterThanEquals:
        consume();
        return make<BinaryExpression>(BinaryOp::GreaterThanEquals, move(lhs), parse_expression());
    case TokenType::LessThan:
        consume();
        return make<BinaryExpression>(BinaryOp::LessThan, move(lhs), parse_expression());
    case TokenType::LessThanEquals:
        consume();
        return make<BinaryExpression>(BinaryOp::LessThanEquals, move(lhs), parse_expression());
    case TokenType::EqualsEqualsEquals:
        consume();
        return make<BinaryExpression>(BinaryOp::TypedEquals, move(lhs), parse_expression());
    case TokenType::ExclamationMarkEqualsEquals:
        consume();
        return make<BinaryExpression>(BinaryOp::TypedInequals, move(lhs), parse_expression());
    case TokenType::ParenOpen:
        return parse_call_expression(move(lhs));
    case TokenType::Equals:
        consume();
        return make<AssignmentExpression>(AssignmentOp::Assignment, move(lhs), parse_expression());
    case TokenType::Period:
        consume();
        return make<MemberExpression>(move(lhs), parse_expression());
    default:
        m_has_errors = true;
        expected("secondary expression (missing switch case)");
        consume();
        return make<ErrorExpression>();
    }
}

NonnullOwnPtr<CallExpression> Parser::parse_call_expression(NonnullOwnPtr<Expression> lhs)
{
    // FIXME: allow arguments
    consume(TokenType::ParenOpen);
    consume(TokenType::ParenClose);

    // FIXME: Allow lhs expression instead of just a string
    if (lhs->is_identifier()) {
        return make<CallExpression>(static_cast<Identifier*>(lhs.ptr())->string());
    }

    m_has_errors = true;
    return make<CallExpression>("***ERROR***");
}

NonnullOwnPtr<ReturnStatement> Parser::parse_return_statement()
{
    consume(TokenType::Return);
    if (match_expression()) {
        return make<ReturnStatement>(parse_expression());
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
    while (match(TokenType::Identifier)) {
        // FIXME: actually add parameters to function
        consume(TokenType::Identifier);
        if (match(TokenType::ParenClose)) {
            break;
        }
        consume(TokenType::Comma);
    }
    consume(TokenType::ParenClose);
    auto body = parse_block_statement();
    return make<FunctionDeclaration>(name, move(body));
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
    default:
        ASSERT_NOT_REACHED();
    }
    auto name = consume(TokenType::Identifier).value();
    OwnPtr<Expression> initializer;
    if (match(TokenType::Equals)) {
        consume();
        initializer = parse_expression();
    }
    return make<VariableDeclaration>(make<Identifier>(name), move(initializer), declaration_type);
}

NonnullOwnPtr<ForStatement> Parser::parse_for_statement()
{
    consume(TokenType::For);

    consume(TokenType::ParenOpen);

    OwnPtr<Statement> init = nullptr;
    switch (m_current_token.type()) {
    case TokenType::Var:
        init = parse_variable_declaration();
        break;
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
        test = parse_expression();
        break;
    }

    consume(TokenType::Semicolon);

    OwnPtr<Expression> update = nullptr;
    switch (m_current_token.type()) {
    case TokenType::Semicolon:
        break;
    default:
        update = parse_expression();
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
        || type == TokenType::NullLiteral
        || type == TokenType::Identifier
        || type == TokenType::New
        || type == TokenType::CurlyOpen
        || type == TokenType::BracketOpen
        || type == TokenType::ParenOpen;
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
        || type == TokenType::GreaterThan
        || type == TokenType::GreaterThanEquals
        || type == TokenType::LessThan
        || type == TokenType::LessThanEquals
        || type == TokenType::ParenOpen
        || type == TokenType::Period;
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
    auto oldToken = m_current_token;
    m_current_token = m_lexer.next();
    return oldToken;
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
