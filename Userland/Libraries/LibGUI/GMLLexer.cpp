/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include "GMLLexer.h"
#include <AK/Vector.h>
#include <ctype.h>

namespace GUI {

GMLLexer::GMLLexer(const StringView& input)
    : m_input(input)
{
}

char GMLLexer::peek(size_t offset) const
{
    if ((m_index + offset) >= m_input.length())
        return 0;
    return m_input[m_index + offset];
}

char GMLLexer::consume()
{
    VERIFY(m_index < m_input.length());
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

static bool is_valid_identifier_start(char ch)
{
    return isalpha(ch) || ch == '_';
}

static bool is_valid_identifier_character(char ch)
{
    return isalnum(ch) || ch == '_';
}

static bool is_valid_class_character(char ch)
{
    return isalnum(ch) || ch == '_' || ch == ':';
}

Vector<GMLToken> GMLLexer::lex()
{
    Vector<GMLToken> tokens;

    size_t token_start_index = 0;
    GMLPosition token_start_position;

    auto begin_token = [&] {
        token_start_index = m_index;
        token_start_position = m_position;
    };

    auto commit_token = [&](auto type) {
        GMLToken token;
        token.m_view = m_input.substring_view(token_start_index, m_index - token_start_index);
        token.m_type = type;
        token.m_start = token_start_position;
        token.m_end = m_previous_position;
        tokens.append(token);
    };

    auto consume_class = [&] {
        begin_token();
        consume();
        commit_token(GMLToken::Type::ClassMarker);
        begin_token();
        while (is_valid_class_character(peek()))
            consume();
        commit_token(GMLToken::Type::ClassName);
    };

    while (m_index < m_input.length()) {
        if (isspace(peek(0))) {
            begin_token();
            while (isspace(peek()))
                consume();
            continue;
        }

        // C++ style comments
        if (peek(0) && peek(0) == '/' && peek(1) == '/') {
            begin_token();
            while (peek() && peek() != '\n')
                consume();
            commit_token(GMLToken::Type::Comment);
            continue;
        }

        if (peek(0) == '{') {
            begin_token();
            consume();
            commit_token(GMLToken::Type::LeftCurly);
            continue;
        }

        if (peek(0) == '}') {
            begin_token();
            consume();
            commit_token(GMLToken::Type::RightCurly);
            continue;
        }

        if (peek(0) == '@') {
            consume_class();
            continue;
        }

        if (is_valid_identifier_start(peek(0))) {
            begin_token();
            consume();
            while (is_valid_identifier_character(peek(0)))
                consume();
            commit_token(GMLToken::Type::Identifier);
            continue;
        }

        if (peek(0) == ':') {
            begin_token();
            consume();
            commit_token(GMLToken::Type::Colon);

            while (isspace(peek()))
                consume();

            if (peek(0) == '@') {
                consume_class();
            } else {
                begin_token();
                while (peek() && peek() != '\n')
                    consume();
                commit_token(GMLToken::Type::JsonValue);
            }
            continue;
        }

        consume();
        commit_token(GMLToken::Type::Unknown);
    }
    return tokens;
}

}
