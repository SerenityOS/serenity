/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
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

namespace SQL {

Parser::Parser(Lexer lexer)
    : m_parser_state(move(lexer))
{
}

NonnullRefPtr<Statement> Parser::next_statement()
{
    switch (m_parser_state.m_token.type()) {
    case TokenType::Create:
        return parse_create_table_statement();
    default:
        expected("CREATE");
        return create_ast_node<ErrorStatement>();
    }
}

NonnullRefPtr<CreateTable> Parser::parse_create_table_statement()
{
    // https://sqlite.org/lang_createtable.html
    consume(TokenType::Create);

    bool is_temporary = false;
    if (match(TokenType::Temp) || match(TokenType::Temporary)) {
        consume();
        is_temporary = true;
    }

    consume(TokenType::Table);

    bool is_error_if_table_exists = true;
    if (match(TokenType::If)) {
        consume(TokenType::If);
        consume(TokenType::Not);
        consume(TokenType::Exists);
        is_error_if_table_exists = false;
    }

    String schema_or_table_name = consume(TokenType::Identifier).value();
    String schema_name;
    String table_name;

    if (match(TokenType::Period)) {
        consume();
        schema_name = move(schema_or_table_name);
        table_name = consume(TokenType::Identifier).value();
    } else {
        table_name = move(schema_or_table_name);
    }

    // FIXME: Parse "AS select-stmt".

    NonnullRefPtrVector<ColumnDefinition> column_definitions;
    consume(TokenType::ParenOpen);
    do {
        column_definitions.append(parse_column_definition());

        if (match(TokenType::ParenClose))
            break;

        consume(TokenType::Comma);
    } while (!match(TokenType::Eof));

    // FIXME: Parse "table-constraint".

    consume(TokenType::ParenClose);
    consume(TokenType::SemiColon);

    return create_ast_node<CreateTable>(move(schema_name), move(table_name), move(column_definitions), is_temporary, is_error_if_table_exists);
}

NonnullRefPtr<ColumnDefinition> Parser::parse_column_definition()
{
    // https://sqlite.org/syntax/column-def.html
    auto name = consume(TokenType::Identifier).value();

    auto type_name = match(TokenType::Identifier)
        ? parse_type_name()
        // https://www.sqlite.org/datatype3.html: If no type is specified then the column has affinity BLOB.
        : create_ast_node<TypeName>("BLOB", NonnullRefPtrVector<SignedNumber> {});

    // FIXME: Parse "column-constraint".

    return create_ast_node<ColumnDefinition>(move(name), move(type_name));
}

NonnullRefPtr<TypeName> Parser::parse_type_name()
{
    // https: //sqlite.org/syntax/type-name.html
    auto name = consume(TokenType::Identifier).value();
    NonnullRefPtrVector<SignedNumber> signed_numbers;

    if (match(TokenType::ParenOpen)) {
        consume();
        signed_numbers.append(parse_signed_number());

        if (match(TokenType::Comma)) {
            consume();
            signed_numbers.append(parse_signed_number());
        }

        consume(TokenType::ParenClose);
    }

    return create_ast_node<TypeName>(move(name), move(signed_numbers));
}

NonnullRefPtr<SignedNumber> Parser::parse_signed_number()
{
    // https://sqlite.org/syntax/signed-number.html
    bool is_positive = true;

    if (match(TokenType::Plus)) {
        consume();
    } else if (match(TokenType::Minus)) {
        is_positive = false;
        consume();
    }

    if (match(TokenType::NumericLiteral)) {
        auto number = consume(TokenType::NumericLiteral).double_value();
        return create_ast_node<SignedNumber>(is_positive ? number : (number * -1));
    }

    expected("NumericLiteral");
    return create_ast_node<SignedNumber>(0);
}

Token Parser::consume()
{
    auto old_token = m_parser_state.m_token;
    m_parser_state.m_token = m_parser_state.m_lexer.next();
    return old_token;
}

Token Parser::consume(TokenType expected_type)
{
    if (!match(expected_type)) {
        expected(Token::name(expected_type));
    }
    return consume();
}

bool Parser::match(TokenType type) const
{
    return m_parser_state.m_token.type() == type;
}

void Parser::expected(StringView what)
{
    syntax_error(String::formatted("Unexpected token {}, expected {}", m_parser_state.m_token.name(), what));
}

void Parser::syntax_error(String message)
{
    m_parser_state.m_errors.append({ move(message), position() });
}

Parser::Position Parser::position() const
{
    return {
        m_parser_state.m_token.line_number(),
        m_parser_state.m_token.line_column()
    };
}

Parser::ParserState::ParserState(Lexer lexer)
    : m_lexer(move(lexer))
    , m_token(m_lexer.next())
{
}

}
