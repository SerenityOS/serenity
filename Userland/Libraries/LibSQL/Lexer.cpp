/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Lexer.h"
#include <AK/Debug.h>
#include <ctype.h>

namespace SQL {

HashMap<String, TokenType> Lexer::s_keywords;
HashMap<char, TokenType> Lexer::s_one_char_tokens;
HashMap<String, TokenType> Lexer::s_two_char_tokens;

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
#define __ENUMERATE_SQL_TOKEN(value, type, category)                                          \
    if (TokenCategory::category != TokenCategory::Keyword && StringView(value).length() == 1) \
        s_one_char_tokens.set(value[0], TokenType::type);
        ENUMERATE_SQL_TOKENS
#undef __ENUMERATE_SQL_TOKEN
    }

    if (s_two_char_tokens.is_empty()) {
#define __ENUMERATE_SQL_TOKEN(value, type, category)                                          \
    if (TokenCategory::category != TokenCategory::Keyword && StringView(value).length() == 2) \
        s_two_char_tokens.set(value, TokenType::type);
        ENUMERATE_SQL_TOKENS
#undef __ENUMERATE_SQL_TOKEN
    }

    consume();
}

Token Lexer::next()
{
    bool found_invalid_comment = consume_whitespace_and_comments();

    size_t value_start = m_position;
    size_t value_start_line_number = m_line_number;
    size_t value_start_column_number = m_line_column;
    auto token_type = TokenType::Invalid;

    if (is_eof()) {
        token_type = found_invalid_comment ? TokenType::Invalid : TokenType::Eof;
    } else if (is_numeric_literal_start()) {
        token_type = TokenType::NumericLiteral;
        if (!consume_numeric_literal())
            token_type = TokenType::Invalid;
    } else if (is_string_literal_start()) {
        token_type = TokenType::StringLiteral;
        if (!consume_string_literal())
            token_type = TokenType::Invalid;
    } else if (is_blob_literal_start()) {
        token_type = TokenType::BlobLiteral;
        if (!consume_blob_literal())
            token_type = TokenType::Invalid;
    } else if (is_identifier_start()) {
        do {
            consume();
        } while (is_identifier_middle());

        if (auto it = s_keywords.find(m_source.substring_view(value_start - 1, m_position - value_start)); it != s_keywords.end()) {
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
                consume();
                consume();
            }
        }

        bool found_one_char_token = false;
        if (!found_two_char_token) {
            if (auto it = s_one_char_tokens.find(m_current_char); it != s_one_char_tokens.end()) {
                found_one_char_token = true;
                token_type = it->value;
                consume();
            }
        }

        if (!found_two_char_token && !found_one_char_token) {
            token_type = TokenType::Invalid;
            consume();
        }
    }

    Token token(token_type, m_source.substring_view(value_start - 1, m_position - value_start), value_start_line_number, value_start_column_number);

    if constexpr (SQL_DEBUG) {
        dbgln("------------------------------");
        dbgln("Token: {}", token.name());
        dbgln("Value: {}", token.value());
        dbgln("Line: {}, Column: {}", token.line_number(), token.line_column());
        dbgln("------------------------------");
    }

    return token;
}

void Lexer::consume()
{
    auto did_reach_eof = [this] {
        if (m_position != m_source.length())
            return false;
        m_current_char = EOF;
        ++m_line_column;
        ++m_position;
        return true;
    };

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

bool Lexer::consume_numeric_literal()
{
    // https://sqlite.org/syntax/numeric-literal.html
    bool is_valid_numeric_literal = true;

    if (m_current_char == '0') {
        consume();
        if (m_current_char == '.') {
            consume();
            while (isdigit(m_current_char))
                consume();
            if (m_current_char == 'e' || m_current_char == 'E')
                is_valid_numeric_literal = consume_exponent();
        } else if (m_current_char == 'e' || m_current_char == 'E') {
            is_valid_numeric_literal = consume_exponent();
        } else if (m_current_char == 'x' || m_current_char == 'X') {
            is_valid_numeric_literal = consume_hexadecimal_number();
        } else if (isdigit(m_current_char)) {
            do {
                consume();
            } while (isdigit(m_current_char));
        }
    } else {
        do {
            consume();
        } while (isdigit(m_current_char));

        if (m_current_char == '.') {
            consume();
            while (isdigit(m_current_char))
                consume();
        }
        if (m_current_char == 'e' || m_current_char == 'E')
            is_valid_numeric_literal = consume_exponent();
    }

    return is_valid_numeric_literal;
}

bool Lexer::consume_string_literal()
{
    // https://sqlite.org/lang_expr.html - See "3. Literal Values (Constants)"
    bool is_valid_string_literal = true;
    consume();

    while (!is_eof() && !is_string_literal_end())
        consume();

    if (is_eof())
        is_valid_string_literal = false;
    consume();

    return is_valid_string_literal;
}

bool Lexer::consume_blob_literal()
{
    // https://sqlite.org/lang_expr.html - See "3. Literal Values (Constants)"
    consume();
    return consume_string_literal();
}

bool Lexer::consume_exponent()
{
    consume();
    if (m_current_char == '-' || m_current_char == '+')
        consume();

    if (!isdigit(m_current_char))
        return false;

    while (isdigit(m_current_char)) {
        consume();
    }
    return true;
}

bool Lexer::consume_hexadecimal_number()
{
    consume();
    if (!isxdigit(m_current_char))
        return false;

    while (isxdigit(m_current_char))
        consume();

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
    return m_current_char == EOF;
}

}
