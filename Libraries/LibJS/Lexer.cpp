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

#include "Lexer.h"
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>
#include <ctype.h>
#include <stdio.h>

namespace JS {

namespace {
template<typename K = const char*, typename V = const TokenType>
struct JsToken {
    K key;
    V value;
};

}

constexpr const JsToken<> s_known_keywords[] = {
    { "await", TokenType::Await },
    { "break", TokenType::Break },
    { "case", TokenType::Case },
    { "catch", TokenType::Catch },
    { "class", TokenType::Class },
    { "const", TokenType::Const },
    { "continue", TokenType::Continue },
    { "debugger", TokenType::Debugger },
    { "default", TokenType::Default },
    { "delete", TokenType::Delete },
    { "do", TokenType::Do },
    { "else", TokenType::Else },
    { "enum", TokenType::Enum },
    { "export", TokenType::Export },
    { "extends", TokenType::Extends },
    { "false", TokenType::BoolLiteral },
    { "finally", TokenType::Finally },
    { "for", TokenType::For },
    { "function", TokenType::Function },
    { "if", TokenType::If },
    { "import", TokenType::Import },
    { "in", TokenType::In },
    { "instanceof", TokenType::Instanceof },
    { "let", TokenType::Let },
    { "new", TokenType::New },
    { "null", TokenType::NullLiteral },
    { "return", TokenType::Return },
    { "super", TokenType::Super },
    { "switch", TokenType::Switch },
    { "this", TokenType::This },
    { "throw", TokenType::Throw },
    { "true", TokenType::BoolLiteral },
    { "try", TokenType::Try },
    { "typeof", TokenType::Typeof },
    { "var", TokenType::Var },
    { "void", TokenType::Void },
    { "while", TokenType::While },
    { "with", TokenType::With },
    { "yield", TokenType::Yield }
};

constexpr const JsToken<> s_known_three_char_tokens[] = {
    { "===", TokenType::EqualsEqualsEquals },
    { "!==", TokenType::ExclamationMarkEqualsEquals },
    { "**=", TokenType::DoubleAsteriskEquals },
    { "<<=", TokenType::ShiftLeftEquals },
    { ">>=", TokenType::ShiftRightEquals },
    { ">>>", TokenType::UnsignedShiftRight },
    { "...", TokenType::TripleDot }
};

constexpr const JsToken<> s_known_two_char_tokens[] = {
    { "=>", TokenType::Arrow },
    { "+=", TokenType::PlusEquals },
    { "-=", TokenType::MinusEquals },
    { "*=", TokenType::AsteriskEquals },
    { "/=", TokenType::SlashEquals },
    { "%=", TokenType::PercentEquals },
    { "&=", TokenType::AmpersandEquals },
    { "|=", TokenType::PipeEquals },
    { "^=", TokenType::CaretEquals },
    { "&&", TokenType::DoubleAmpersand },
    { "||", TokenType::DoublePipe },
    { "??", TokenType::DoubleQuestionMark },
    { "**", TokenType::DoubleAsterisk },
    { "==", TokenType::EqualsEquals },
    { "<=", TokenType::LessThanEquals },
    { ">=", TokenType::GreaterThanEquals },
    { "!=", TokenType::ExclamationMarkEquals },
    { "--", TokenType::MinusMinus },
    { "++", TokenType::PlusPlus },
    { "<<", TokenType::ShiftLeft },
    { ">>", TokenType::ShiftRight },
    { "?.", TokenType::QuestionMarkPeriod }
};

constexpr const JsToken<const char> s_known_single_char_tokens[] = {
    { '&', TokenType::Ampersand },
    { '*', TokenType::Asterisk },
    { '[', TokenType::BracketOpen },
    { ']', TokenType::BracketClose },
    { '^', TokenType::Caret },
    { ':', TokenType::Colon },
    { ',', TokenType::Comma },
    { '{', TokenType::CurlyOpen },
    { '}', TokenType::CurlyClose },
    { '=', TokenType::Equals },
    { '!', TokenType::ExclamationMark },
    { '-', TokenType::Minus },
    { '(', TokenType::ParenOpen },
    { ')', TokenType::ParenClose },
    { '%', TokenType::Percent },
    { '.', TokenType::Period },
    { '|', TokenType::Pipe },
    { '+', TokenType::Plus },
    { '?', TokenType::QuestionMark },
    { ';', TokenType::Semicolon },
    { '/', TokenType::Slash },
    { '~', TokenType::Tilde },
    { '<', TokenType::LessThan },
    { '>', TokenType::GreaterThan },

};

HashMap<String, TokenType> Lexer::s_keywords(array_size(s_known_keywords));
HashMap<String, TokenType> Lexer::s_three_char_tokens(array_size(s_known_three_char_tokens));
HashMap<String, TokenType> Lexer::s_two_char_tokens(array_size(s_known_two_char_tokens));
HashMap<char, TokenType> Lexer::s_single_char_tokens(array_size(s_known_single_char_tokens));

Lexer::Lexer(StringView source)
    : m_source(source)
    , m_current_token(TokenType::Eof, StringView(nullptr), StringView(nullptr), 0, 0)
{
    if (s_keywords.is_empty()) {
        s_keywords.set_from(s_known_keywords);
    }

    if (s_three_char_tokens.is_empty()) {
        s_three_char_tokens.set_from(s_known_three_char_tokens);
    }

    if (s_two_char_tokens.is_empty()) {
        s_three_char_tokens.set_from(s_known_two_char_tokens);
    }

    if (s_single_char_tokens.is_empty()) {
        s_single_char_tokens.set_from(s_known_single_char_tokens);
    }
    consume();
}

void Lexer::consume()
{
    if (m_position > m_source.length())
        return;

    if (m_position == m_source.length()) {
        m_position++;
        m_line_column++;
        m_current_char = EOF;
        return;
    }

    if (m_current_char == '\n') {
        m_line_number++;
        m_line_column = 1;
    } else {
        m_line_column++;
    }

    m_current_char = m_source[m_position++];
}

void Lexer::consume_exponent()
{
    consume();
    if (m_current_char == '-' || m_current_char == '+')
        consume();
    while (isdigit(m_current_char)) {
        consume();
    }
}

bool Lexer::match(char a, char b) const
{
    if (m_position >= m_source.length())
        return false;

    return m_current_char == a
        && m_source[m_position] == b;
}

bool Lexer::match(char a, char b, char c) const
{
    if (m_position + 1 >= m_source.length())
        return false;

    return m_current_char == a
        && m_source[m_position] == b
        && m_source[m_position + 1] == c;
}

bool Lexer::match(char a, char b, char c, char d) const
{
    if (m_position + 2 >= m_source.length())
        return false;

    return m_current_char == a
        && m_source[m_position] == b
        && m_source[m_position + 1] == c
        && m_source[m_position + 2] == d;
}

bool Lexer::is_eof() const
{
    return m_current_char == EOF;
}

bool Lexer::is_identifier_start() const
{
    return isalpha(m_current_char) || m_current_char == '_' || m_current_char == '$';
}

bool Lexer::is_identifier_middle() const
{
    return is_identifier_start() || isdigit(m_current_char);
}

bool Lexer::is_line_comment_start() const
{
    return match('/', '/') || match('<', '!', '-', '-') || match('-', '-', '>');
}

bool Lexer::is_block_comment_start() const
{
    return match('/', '*');
}

bool Lexer::is_block_comment_end() const
{
    return match('*', '/');
}

bool Lexer::is_numeric_literal_start() const
{
    return isdigit(m_current_char) || (m_current_char == '.' && m_position < m_source.length() && isdigit(m_source[m_position]));
}

bool Lexer::slash_means_division() const
{
    auto type = m_current_token.type();
    return type == TokenType::BigIntLiteral
        || type == TokenType::BoolLiteral
        || type == TokenType::BracketClose
        || type == TokenType::CurlyClose
        || type == TokenType::Identifier
        || type == TokenType::NullLiteral
        || type == TokenType::NumericLiteral
        || type == TokenType::ParenClose
        || type == TokenType::RegexLiteral
        || type == TokenType::StringLiteral
        || type == TokenType::TemplateLiteralEnd
        || type == TokenType::This;
}

Token Lexer::next()
{
    size_t trivia_start = m_position;
    auto in_template = !m_template_states.is_empty();

    if (!in_template || m_template_states.last().in_expr) {
        // consume whitespace and comments
        while (true) {
            if (isspace(m_current_char)) {
                do {
                    consume();
                } while (isspace(m_current_char));
            } else if (is_line_comment_start()) {
                consume();
                do {
                    consume();
                } while (!is_eof() && m_current_char != '\n');
            } else if (is_block_comment_start()) {
                consume();
                do {
                    consume();
                } while (!is_eof() && !is_block_comment_end());
                consume(); // consume *
                consume(); // consume /
            } else {
                break;
            }
        }
    }

    size_t value_start = m_position;
    auto token_type = TokenType::Invalid;

    if (m_current_token.type() == TokenType::RegexLiteral && !is_eof() && isalpha(m_current_char)) {
        token_type = TokenType::RegexFlags;
        while (!is_eof() && isalpha(m_current_char))
            consume();
    } else if (m_current_char == '`') {
        consume();

        if (!in_template) {
            token_type = TokenType::TemplateLiteralStart;
            m_template_states.append({ false, 0 });
        } else {
            if (m_template_states.last().in_expr) {
                m_template_states.append({ false, 0 });
                token_type = TokenType::TemplateLiteralStart;
            } else {
                m_template_states.take_last();
                token_type = TokenType::TemplateLiteralEnd;
            }
        }
    } else if (in_template && m_template_states.last().in_expr && m_template_states.last().open_bracket_count == 0 && m_current_char == '}') {
        consume();
        token_type = TokenType::TemplateLiteralExprEnd;
        m_template_states.last().in_expr = false;
    } else if (in_template && !m_template_states.last().in_expr) {
        if (is_eof()) {
            token_type = TokenType::UnterminatedTemplateLiteral;
            m_template_states.take_last();
        } else if (match('$', '{')) {
            token_type = TokenType::TemplateLiteralExprStart;
            consume();
            consume();
            m_template_states.last().in_expr = true;
        } else {
            while (!match('$', '{') && m_current_char != '`' && !is_eof()) {
                if (match('\\', '$') || match('\\', '`'))
                    consume();
                consume();
            }

            token_type = TokenType::TemplateLiteralString;
        }
    } else if (is_identifier_start()) {
        // identifier or keyword
        do {
            consume();
        } while (is_identifier_middle());

        StringView value = m_source.substring_view(value_start - 1, m_position - value_start);
        auto it = s_keywords.find(value);
        if (it == s_keywords.end()) {
            token_type = TokenType::Identifier;
        } else {
            token_type = it->value;
        }
    } else if (is_numeric_literal_start()) {
        token_type = TokenType::NumericLiteral;
        if (m_current_char == '0') {
            consume();
            if (m_current_char == '.') {
                // decimal
                consume();
                while (isdigit(m_current_char))
                    consume();
                if (m_current_char == 'e' || m_current_char == 'E')
                    consume_exponent();
            } else if (m_current_char == 'e' || m_current_char == 'E') {
                consume_exponent();
            } else if (m_current_char == 'o' || m_current_char == 'O') {
                // octal
                consume();
                while (m_current_char >= '0' && m_current_char <= '7')
                    consume();
            } else if (m_current_char == 'b' || m_current_char == 'B') {
                // binary
                consume();
                while (m_current_char == '0' || m_current_char == '1')
                    consume();
            } else if (m_current_char == 'x' || m_current_char == 'X') {
                // hexadecimal
                consume();
                while (isxdigit(m_current_char))
                    consume();
            } else if (m_current_char == 'n') {
                consume();
                token_type = TokenType::BigIntLiteral;
            } else if (isdigit(m_current_char)) {
                // octal without 'O' prefix. Forbidden in 'strict mode'
                // FIXME: We need to make sure this produces a syntax error when in strict mode
                do {
                    consume();
                } while (isdigit(m_current_char));
            }
        } else {
            // 1...9 or period
            while (isdigit(m_current_char))
                consume();
            if (m_current_char == 'n') {
                consume();
                token_type = TokenType::BigIntLiteral;
            } else {
                if (m_current_char == '.') {
                    consume();
                    while (isdigit(m_current_char))
                        consume();
                }
                if (m_current_char == 'e' || m_current_char == 'E')
                    consume_exponent();
            }
        }
    } else if (m_current_char == '"' || m_current_char == '\'') {
        char stop_char = m_current_char;
        consume();
        while (m_current_char != stop_char && m_current_char != '\n' && !is_eof()) {
            if (m_current_char == '\\') {
                consume();
            }
            consume();
        }
        if (m_current_char != stop_char) {
            token_type = TokenType::UnterminatedStringLiteral;
        } else {
            consume();
            token_type = TokenType::StringLiteral;
        }
    } else if (m_current_char == '/' && !slash_means_division()) {
        consume();
        token_type = TokenType::RegexLiteral;

        while (!is_eof()) {
            if (m_current_char == '[') {
                m_regex_is_in_character_class = true;
            } else if (m_current_char == ']') {
                m_regex_is_in_character_class = false;
            } else if (!m_regex_is_in_character_class && m_current_char == '/') {
                break;
            }

            if (match('\\', '/') || match('\\', '[') || match('\\', '\\') || (m_regex_is_in_character_class && match('\\', ']')))
                consume();
            consume();
        }

        if (is_eof()) {
            token_type = TokenType::UnterminatedRegexLiteral;
        } else {
            consume();
        }
    } else if (m_current_char == EOF) {
        token_type = TokenType::Eof;
    } else {
        // There is only one four-char operator: >>>=
        bool found_four_char_token = false;
        if (match('>', '>', '>', '=')) {
            found_four_char_token = true;
            consume();
            consume();
            consume();
            consume();
            token_type = TokenType::UnsignedShiftRightEquals;
        }

        bool found_three_char_token = false;
        if (!found_four_char_token && m_position + 1 < m_source.length()) {
            char second_char = m_source[m_position];
            char third_char = m_source[m_position + 1];
            char three_chars[] { (char)m_current_char, second_char, third_char, 0 };
            auto it = s_three_char_tokens.find(three_chars);
            if (it != s_three_char_tokens.end()) {
                found_three_char_token = true;
                consume();
                consume();
                consume();
                token_type = it->value;
            }
        }

        bool found_two_char_token = false;
        if (!found_four_char_token && !found_three_char_token && m_position < m_source.length()) {
            char second_char = m_source[m_position];
            char two_chars[] { (char)m_current_char, second_char, 0 };
            auto it = s_two_char_tokens.find(two_chars);
            if (it != s_two_char_tokens.end()) {
                found_two_char_token = true;
                consume();
                consume();
                token_type = it->value;
            }
        }

        bool found_one_char_token = false;
        if (!found_four_char_token && !found_three_char_token && !found_two_char_token) {
            auto it = s_single_char_tokens.find(m_current_char);
            if (it != s_single_char_tokens.end()) {
                found_one_char_token = true;
                consume();
                token_type = it->value;
            }
        }

        if (!found_four_char_token && !found_three_char_token && !found_two_char_token && !found_one_char_token) {
            consume();
            token_type = TokenType::Invalid;
        }
    }

    if (!m_template_states.is_empty() && m_template_states.last().in_expr) {
        if (token_type == TokenType::CurlyOpen) {
            m_template_states.last().open_bracket_count++;
        } else if (token_type == TokenType::CurlyClose) {
            m_template_states.last().open_bracket_count--;
        }
    }

    m_current_token = Token(
        token_type,
        m_source.substring_view(trivia_start - 1, value_start - trivia_start),
        m_source.substring_view(value_start - 1, m_position - value_start),
        m_line_number,
        m_line_column - m_position + value_start);

    return m_current_token;
}

}
