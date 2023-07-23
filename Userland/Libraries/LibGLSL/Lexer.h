/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibGLSL/Token.h>

namespace GLSL {

class Lexer {
public:
    explicit Lexer(StringView, size_t start_line = 0);

    Vector<Token> lex();
    template<typename Callback>
    void lex_iterable(Callback);

    void set_ignore_whitespace(bool value) { m_options.ignore_whitespace = value; }

private:
    char peek(size_t offset = 0) const;
    char consume();
    void lex_impl(Function<void(Token)>);

    StringView m_input;
    size_t m_index { 0 };
    Position m_previous_position { 0, 0 };
    Position m_position { 0, 0 };

    struct Options {
        bool ignore_whitespace { false };
    } m_options;
};

template<typename Callback>
void Lexer::lex_iterable(Callback callback)
{
    return lex_impl(move(callback));
}

}
