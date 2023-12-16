/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Lexer.h"
#include <AK/Debug.h>
#include <ctype.h>

namespace SQL::AST {

HashMap<ByteString, TokenType> Lexer::s_keywords;
HashMap<char, TokenType> Lexer::s_one_char_tokens;
HashMap<ByteString, TokenType> Lexer::s_two_char_tokens;

Lexer::Lexer(StringView source)
    : m_source(source)
{
    if (s_keywords.is_empty()) {
#define __ENUMERATE_SQL_TOKEN(value, type, category)       \
    if (TokenCategory::category == TokenCategory::Keyword) \
        s_keywords.set(value, TokenType::type);
        ENUMERATE_SQL_TOKENS
#undef __ENUMERATE_SQL_TOKEN
    }

    if (s_one_char_tokens.is_empty()) {
#define __ENUMERATE_SQL_TOKEN(value, type, category)                                  \
    if (TokenCategory::category != TokenCategory::Keyword && value##sv.length() == 1) \
        s_one_char_tokens.set(value[0], TokenType::type);
        ENUMERATE_SQL_TOKENS
#undef __ENUMERATE_SQL_TOKEN
    }

    if (s_two_char_tokens.is_empty()) {
#define __ENUMERATE_SQL_TOKEN(value, type, category)                                  \
    if (TokenCategory::category != TokenCategory::Keyword && value##sv.length() == 2) \
        s_two_char_tokens.set(value, TokenType::type);
        ENUMERATE_SQL_TOKENS
#undef __ENUMERATE_SQL_TOKEN
    }

    consume();
}

Token Lexer::next()
{
    bool found_invalid_comment = consume_whitespace_and_comments();

    size_t value_start_line_number = m_line_number;
    size_t value_start_column_number = m_line_column;
    auto token_type = TokenType::Invalid;
    StringBuilder current_token;

    if (is_eof()) {
        token_type = found_invalid_comment ? TokenType::Invalid : TokenType::Eof;
    } else if (is_numeric_literal_start()) {
        token_type = TokenType::NumericLiteral;
        if (!consume_numeric_literal(current_token))
            token_type = TokenType::Invalid;
    } else if (is_string_literal_start()) {
        token_type = TokenType::StringLiteral;
        if (!consume_string_literal(current_token))
            token_type = TokenType::Invalid;
    } else if (is_quoted_identifier_start()) {
        token_type = TokenType::Identifier;
        if (!consume_quoted_identifier(current_token))
            token_type = TokenType::Invalid;
    } else if (is_blob_literal_start()) {
        token_type = TokenType::BlobLiteral;
        if (!consume_blob_literal(current_token))
            token_type = TokenType::Invalid;
    } else if (is_identifier_start()) {
        do {
            current_token.append((char)toupper(m_current_char));
            consume();
        } while (is_identifier_middle());

        if (auto it = s_keywords.find(current_token.string_view()); it != s_keywords.end()) {
            token_type = it->value;
        } else {
            token_type = TokenType::Identifier;
        }
    } else {
        bool found_two_char_token = false;
        if (m_position < m_source.length()) {
            if (auto it = s_two_char_tokens.find(m_source.substring_view(m_position - 1, 2)); it != s_two_char_tokens.end()) {
                found_two_char_token = true;
                token_type = it->value;
                consume(&current_token);
                consume(&current_token);
            }
        }

        bool found_one_char_token = false;
        if (!found_two_char_token) {
            if (auto it = s_one_char_tokens.find(m_current_char); it != s_one_char_tokens.end()) {
                found_one_char_token = true;
                token_type = it->value;
                consume(&current_token);
            }
        }

        if (!found_two_char_token && !found_one_char_token) {
            token_type = TokenType::Invalid;
            consume(&current_token);
        }
    }

    Token token(token_type, current_token.to_byte_string(),
        { value_start_line_number, value_start_column_number },
        { m_line_number, m_line_column });

    if constexpr (SQL_DEBUG) {
        dbgln("------------------------------");
        dbgln("Token: {}", token.name());
        dbgln("Value: {}", token.value());
        dbgln("Line: {}, Column: {}", token.start_position().line, token.start_position().column);
        dbgln("------------------------------");
    }

    return token;
}

void Lexer::consume(StringBuilder* current_token)
{
    auto did_reach_eof = [this] {
        if (m_position != m_source.length())
            return false;
        m_eof = true;
        m_current_char = '\0';
        ++m_line_column;
        ++m_position;
        return true;
    };

    if (current_token)
        current_token->append(m_current_char);

    if (m_position > m_source.length())
        return;

    if (did_reach_eof())
        return;

    if (is_line_break()) {
        ++m_line_number;
        m_line_column = 1;
    } else {
        ++m_line_column;
    }

    m_current_char = m_source[m_position++];
}

bool Lexer::consume_whitespace_and_comments()
{
    bool found_invalid_comment = false;

    while (true) {
        if (isspace(m_current_char)) {
            do {
                consume();
            } while (isspace(m_current_char));
        } else if (is_line_comment_start()) {
            consume();
            do {
                consume();
            } while (!is_eof() && !is_line_break());
        } else if (is_block_comment_start()) {
            consume();
            do {
                consume();
            } while (!is_eof() && !is_block_comment_end());
            if (is_eof())
                found_invalid_comment = true;
            consume(); // consume *
            if (is_eof())
                found_invalid_comment = true;
            consume(); // consume /
        } else {
            break;
        }
    }

    return found_invalid_comment;
}

bool Lexer::consume_numeric_literal(StringBuilder& current_token)
{
    // https://sqlite.org/syntax/numeric-literal.html
    bool is_valid_numeric_literal = true;

    if (m_current_char == '0') {
        consume(&current_token);
        if (m_current_char == '.') {
            consume(&current_token);
            while (isdigit(m_current_char))
                consume(&current_token);
            if (m_current_char == 'e' || m_current_char == 'E')
                is_valid_numeric_literal = consume_exponent(current_token);
        } else if (m_current_char == 'e' || m_current_char == 'E') {
            is_valid_numeric_literal = consume_exponent(current_token);
        } else if (m_current_char == 'x' || m_current_char == 'X') {
            is_valid_numeric_literal = consume_hexadecimal_number(current_token);
        } else if (isdigit(m_current_char)) {
            do {
                consume(&current_token);
            } while (isdigit(m_current_char));
        }
    } else {
        do {
            consume(&current_token);
        } while (isdigit(m_current_char));

        if (m_current_char == '.') {
            consume(&current_token);
            while (isdigit(m_current_char))
                consume(&current_token);
        }
        if (m_current_char == 'e' || m_current_char == 'E')
            is_valid_numeric_literal = consume_exponent(current_token);
    }

    return is_valid_numeric_literal;
}

bool Lexer::consume_string_literal(StringBuilder& current_token)
{
    // https://sqlite.org/lang_expr.html - See "3. Literal Values (Constants)"
    bool is_valid_string_literal = true;

    // Skip the opening single quote:
    consume();

    while (!is_eof() && !is_string_literal_end()) {
        // If both the current character and the next one are single quotes,
        // consume one single quote into the current token, and drop the
        // other one on the floor:
        if (match('\'', '\''))
            consume();
        consume(&current_token);
    }

    if (is_eof())
        is_valid_string_literal = false;
    // Drop the closing quote on the floor:
    consume();

    return is_valid_string_literal;
}

bool Lexer::consume_quoted_identifier(StringBuilder& current_token)
{
    // I have not found a reference to the syntax for identifiers in the
    // SQLite documentation, but PostgreSQL has this:
    // https://www.postgresql.org/docs/current/sql-syntax-lexical.html#SQL-SYNTAX-IDENTIFIERS
    bool is_valid_identifier = true;

    // Skip the opening double quote:
    consume();

    while (!is_eof() && !is_quoted_identifier_end()) {
        // If both the current character and the next one are double quotes,
        // consume one single quote into the current token, and drop the
        // other one on the floor:
        if (match('"', '"'))
            consume();
        consume(&current_token);
    }

    if (is_eof())
        is_valid_identifier = false;
    // Drop the closing double quote on the floor:
    consume();

    return is_valid_identifier;
}

bool Lexer::consume_blob_literal(StringBuilder& current_token)
{
    // https://sqlite.org/lang_expr.html - See "3. Literal Values (Constants)"

    // Skip starting 'X'/'x' character:
    consume();

    if (!consume_string_literal(current_token))
        return false;
    for (auto ix = 0u; ix < current_token.length(); ix++) {
        if (!isxdigit(current_token.string_view()[ix]))
            return false;
    }
    return true;
}

bool Lexer::consume_exponent(StringBuilder& current_token)
{
    consume(&current_token);
    if (m_current_char == '-' || m_current_char == '+')
        consume(&current_token);

    if (!isdigit(m_current_char))
        return false;

    // FIXME This code results in the string "1e" being rejected as a
    //       malformed numeric literal. We do however accept "1a" which
    //       is inconsistent. We have to decide what we want to do:
    //        - Be like `SQLite` and reject both "1a" and "1e" because we
    //          require a space between the two tokens. This is pretty invasive;
    //          we would have to decide where all spaces are required and fix
    //          the lexer accordingly.
    //        - Be like `PostgreSQL` and accept both "1e" and "1a" as two
    //          separate tokens, and accept "1e3" as a single token. This would
    //          would require pushing back the "e" we lexed here, terminate the
    //          numeric literal, and re-process the "e" as the first char of
    //          a new token.
    while (isdigit(m_current_char)) {
        consume(&current_token);
    }
    return true;
}

bool Lexer::consume_hexadecimal_number(StringBuilder& current_token)
{
    consume(&current_token);
    if (!isxdigit(m_current_char))
        return false;

    while (isxdigit(m_current_char))
        consume(&current_token);

    return true;
}

bool Lexer::match(char a, char b) const
{
    if (m_position >= m_source.length())
        return false;

    return m_current_char == a && m_source[m_position] == b;
}

bool Lexer::is_identifier_start() const
{
    return isalpha(m_current_char) || m_current_char == '_';
}

bool Lexer::is_identifier_middle() const
{
    return is_identifier_start() || isdigit(m_current_char);
}

bool Lexer::is_numeric_literal_start() const
{
    return isdigit(m_current_char) || (m_current_char == '.' && m_position < m_source.length() && isdigit(m_source[m_position]));
}

bool Lexer::is_string_literal_start() const
{
    return m_current_char == '\'';
}

bool Lexer::is_string_literal_end() const
{
    return m_current_char == '\'' && !(m_position < m_source.length() && m_source[m_position] == '\'');
}

bool Lexer::is_quoted_identifier_start() const
{
    return m_current_char == '"';
}

bool Lexer::is_quoted_identifier_end() const
{
    return m_current_char == '"' && !(m_position < m_source.length() && m_source[m_position] == '"');
}

bool Lexer::is_blob_literal_start() const
{
    return match('x', '\'') || match('X', '\'');
}

bool Lexer::is_line_comment_start() const
{
    return match('-', '-');
}

bool Lexer::is_block_comment_start() const
{
    return match('/', '*');
}

bool Lexer::is_block_comment_end() const
{
    return match('*', '/');
}

bool Lexer::is_line_break() const
{
    return m_current_char == '\n';
}

bool Lexer::is_eof() const
{
    return m_eof;
}

}
