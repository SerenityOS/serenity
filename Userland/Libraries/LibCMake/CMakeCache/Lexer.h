/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/GenericLexer.h>
#include <AK/Vector.h>
#include <LibCMake/CMakeCache/Token.h>

namespace CMake::Cache {

class Lexer : private GenericLexer {
public:
    static ErrorOr<Vector<Token>> lex(StringView input);

private:
    Lexer(StringView input);

    ErrorOr<Vector<Token>> lex_file();

    void skip_whitespace();

    void consume_comment();
    void consume_help_text();
    void consume_variable_definition();
    void consume_key();
    void consume_colon();
    void consume_type();
    void consume_equals();
    void consume_value();
    void consume_garbage();

    Position position() const;
    void next_line();

    void emit_token(Token::Type, StringView value, Position start, Position end);

    Vector<Token> m_tokens;
    size_t m_line { 0 };
    size_t m_string_offset_after_previous_newline { 0 };
};

}
