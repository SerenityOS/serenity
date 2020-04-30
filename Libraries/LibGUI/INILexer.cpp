/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
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

#include "INILexer.h"
#include <AK/Vector.h>
#include <ctype.h>

namespace GUI {

IniLexer::IniLexer(const StringView& input)
    : m_input(input)
{
}

char IniLexer::peek(size_t offset) const
{
    if ((m_index + offset) >= m_input.length())
        return 0;
    return m_input[m_index + offset];
}

char IniLexer::consume()
{
    ASSERT(m_index < m_input.length());
    char ch = m_input[m_index++];
    m_previous_position = m_position;
    if (ch == '\n') {
        m_position.line++;
        m_position.column = 0;
    } else {
        m_position.column++;
    }
    return ch;
}

Vector<IniToken> IniLexer::lex()
{
    Vector<IniToken> tokens;

    size_t token_start_index = 0;
    IniPosition token_start_position;

    auto emit_token = [&](auto type) {
        IniToken token;
        token.m_type = type;
        token.m_start = m_position;
        token.m_end = m_position;
        tokens.append(token);
        consume();
    };

    auto begin_token = [&] {
        token_start_index = m_index;
        token_start_position = m_position;
    };

    auto commit_token = [&](auto type) {
        IniToken token;
        token.m_type = type;
        token.m_start = token_start_position;
        token.m_end = m_previous_position;
        tokens.append(token);
    };

    while (m_index < m_input.length()) {
        auto ch = peek();

        if (isspace(ch)) {
            begin_token();
            while (isspace(peek()))
                consume();
            commit_token(IniToken::Type::Whitespace);
            continue;
        }

        // ;Comment
        if (ch == ';') {
            begin_token();
            while (peek() && peek() != '\n')
                consume();
            commit_token(IniToken::Type::Comment);
            continue;
        }

        // [Section]
        if (ch == '[') {
            // [ Token
            begin_token();
            consume();
            commit_token(IniToken::Type::LeftBracket);

            // Section
            begin_token();
            while (peek() && !(peek() == ']' || peek() == '\n'))
                consume();
            commit_token(IniToken::Type::section);

            // ] Token
            if (peek() && peek() == ']') {
                begin_token();
                consume();
                commit_token(IniToken::Type::RightBracket);
            }

            continue;
        }

        // Empty Line
        if (ch == '\n') {
            consume();
            emit_token(IniToken::Type::Unknown);
            continue;
        }

        //  Name=Value
        begin_token();
        while (peek() && !(peek() == '=' || peek() == '\n'))
            consume();
        commit_token(IniToken::Type::Name);

        if (peek() && peek() == '=') {
            begin_token();
            consume();
            commit_token(IniToken::Type::Equal);
        }

        if (peek()) {
            begin_token();
            while (peek() && peek() != '\n')
                consume();
            commit_token(IniToken::Type::Value);
        }
    }
    return tokens;
}

}
