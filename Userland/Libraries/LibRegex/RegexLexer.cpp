/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include "RegexLexer.h"
#include <AK/Assertions.h>
#include <AK/Debug.h>
#include <stdio.h>

namespace regex {

const char* Token::name(const TokenType type)
{
    switch (type) {
#define __ENUMERATE_REGEX_TOKEN(x) \
    case TokenType::x:             \
        return #x;
        ENUMERATE_REGEX_TOKENS
#undef __ENUMERATE_REGEX_TOKEN
    default:
        VERIFY_NOT_REACHED();
        return "<Unknown>";
    }
}

const char* Token::name() const
{
    return name(m_type);
}

Lexer::Lexer(const StringView source)
    : m_source(source)
{
}

ALWAYS_INLINE char Lexer::peek(size_t offset) const
{
    if ((m_position + offset) >= m_source.length())
        return EOF;
    return m_source[m_position + offset];
}

void Lexer::back(size_t offset)
{
    if (offset == m_position + 1)
        offset = m_position; // 'position == 0' occurs twice.

    VERIFY(offset <= m_position);
    if (!offset)
        return;
    m_position -= offset;
    m_previous_position = (m_position > 0) ? m_position - 1 : 0;
    m_current_char = m_source[m_position];
}

ALWAYS_INLINE void Lexer::consume()
{
    m_previous_position = m_position;

    if (m_position >= m_source.length()) {
        m_position = m_source.length() + 1;
        m_current_char = EOF;
        return;
    }

    m_current_char = m_source[m_position++];
}

void Lexer::reset()
{
    m_position = 0;
    m_current_token = { TokenType::Eof, 0, StringView(nullptr) };
    m_current_char = 0;
    m_previous_position = 0;
}

bool Lexer::try_skip(char c)
{
    if (peek() != c)
        return false;

    consume();
    return true;
}

char Lexer::skip()
{
    auto c = peek();
    consume();
    return c;
}

Token Lexer::next()
{
    size_t token_start_position;

    auto begin_token = [&] {
        token_start_position = m_position;
    };

    auto commit_token = [&](auto type) -> Token& {
        VERIFY(token_start_position + m_previous_position - token_start_position + 1 <= m_source.length());
        auto substring = m_source.substring_view(token_start_position, m_previous_position - token_start_position + 1);
        m_current_token = Token(type, token_start_position, substring);
        return m_current_token;
    };

    auto emit_token = [&](auto type) -> Token& {
        m_current_token = Token(type, m_position, m_source.substring_view(m_position, 1));
        consume();
        return m_current_token;
    };

    auto match_escape_sequence = [&]() -> size_t {
        switch (peek(1)) {
        case '^':
        case '.':
        case '[':
        case ']':
        case '$':
        case '(':
        case ')':
        case '|':
        case '*':
        case '+':
        case '?':
        case '{':
        case '\\':
            return 2;
        default:
#if REGEX_DEBUG
            fprintf(stderr, "[LEXER] Found invalid escape sequence: \\%c (the parser will have to deal with this!)\n", peek(1));
#endif
            return 0;
        }
    };

    while (m_position <= m_source.length()) {
        auto ch = peek();
        if (ch == '(')
            return emit_token(TokenType::LeftParen);

        if (ch == ')')
            return emit_token(TokenType::RightParen);

        if (ch == '{')
            return emit_token(TokenType::LeftCurly);

        if (ch == '}')
            return emit_token(TokenType::RightCurly);

        if (ch == '[')
            return emit_token(TokenType::LeftBracket);

        if (ch == ']')
            return emit_token(TokenType::RightBracket);

        if (ch == '.')
            return emit_token(TokenType::Period);

        if (ch == '*')
            return emit_token(TokenType::Asterisk);

        if (ch == '+')
            return emit_token(TokenType::Plus);

        if (ch == '$')
            return emit_token(TokenType::Dollar);

        if (ch == '^')
            return emit_token(TokenType::Circumflex);

        if (ch == '|')
            return emit_token(TokenType::Pipe);

        if (ch == '?')
            return emit_token(TokenType::Questionmark);

        if (ch == ',')
            return emit_token(TokenType::Comma);

        if (ch == '/')
            return emit_token(TokenType::Slash);

        if (ch == '=')
            return emit_token(TokenType::EqualSign);

        if (ch == ':')
            return emit_token(TokenType::Colon);

        if (ch == '-')
            return emit_token(TokenType::HyphenMinus);

        if (ch == '\\') {
            size_t escape = match_escape_sequence();
            if (escape > 0) {
                begin_token();
                for (size_t i = 0; i < escape; ++i)
                    consume();
                return commit_token(TokenType::EscapeSequence);
            }
        }

        if (ch == EOF)
            break;

        return emit_token(TokenType::Char);
    }

    return Token(TokenType::Eof, m_position, nullptr);
}

}
