/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "INILexer.h"
#include <AK/CharacterTypes.h>
#include <AK/Vector.h>

namespace GUI {

IniLexer::IniLexer(StringView input)
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
    VERIFY(m_index < m_input.length());
    char ch = m_input[m_index++];
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
        consume();
        token.m_end = m_position;
        tokens.append(token);
    };

    auto begin_token = [&] {
        token_start_index = m_index;
        token_start_position = m_position;
    };

    auto commit_token = [&](auto type) {
        IniToken token;
        token.m_type = type;
        token.m_start = token_start_position;
        token.m_end = m_position;
        tokens.append(token);
    };

    while (m_index < m_input.length()) {
        auto ch = peek();

        if (is_ascii_space(ch)) {
            begin_token();
            while (is_ascii_space(peek()))
                consume();
            commit_token(IniToken::Type::Whitespace);
            continue;
        }

        // ;Comment or #Comment
        if (ch == ';' || ch == '#') {
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
            commit_token(IniToken::Type::Section);

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
