/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "INILexer.h"
#include <AK/CharacterTypes.h>
#include <AK/Vector.h>

namespace GUI {

IniLexer::IniLexer(StringView input)
    : m_input(input)
    , m_iterator(m_input.begin())
{
}

u32 IniLexer::peek(size_t offset) const
{
    return m_iterator.peek(offset).value_or(0);
}

u32 IniLexer::consume()
{
    VERIFY(m_iterator != m_input.end());
    u32 ch = *m_iterator;
    ++m_iterator;
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
        token_start_position = m_position;
    };

    auto commit_token = [&](auto type) {
        IniToken token;
        token.m_type = type;
        token.m_start = token_start_position;
        token.m_end = m_position;
        tokens.append(token);
    };

    while (m_iterator != m_input.end()) {
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
