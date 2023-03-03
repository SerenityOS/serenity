/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/GenericLexer.h>
#include <AK/Vector.h>
#include <LibCMake/Token.h>

namespace CMake {

class Lexer : private GenericLexer {
public:
    static ErrorOr<Vector<Token>> lex(StringView input);

private:
    Lexer(StringView input);

    ErrorOr<Vector<Token>> lex_file();

    void skip_whitespace();

    void consume_whitespace_or_comments();
    void consume_command_invocation();
    void consume_arguments();
    void consume_argument();
    void consume_bracket_argument();
    void consume_quoted_argument();
    void consume_unquoted_argument();
    void consume_comment();
    void consume_open_paren();
    void consume_close_paren();
    void consume_garbage();

    StringView read_bracket_argument();
    static Vector<VariableReference> parse_variable_references_from_argument(StringView argument_value, Position argument_start);

    Position position() const;
    void next_line();

    void emit_token(Token::Type, StringView value, Position start, Position end, Optional<ControlKeywordType> = {}, Vector<VariableReference> = {});

    Vector<Token> m_tokens;
    size_t m_line { 0 };
    size_t m_string_offset_after_previous_newline { 0 };
};

}
