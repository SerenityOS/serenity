/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Token.h"
#include <AK/ByteString.h>
#include <AK/HashMap.h>
#include <AK/StringView.h>

namespace SQL::AST {

class Lexer {
public:
    explicit Lexer(StringView source);

    Token next();

private:
    void consume(StringBuilder* = nullptr);

    bool consume_whitespace_and_comments();
    bool consume_numeric_literal(StringBuilder&);
    bool consume_string_literal(StringBuilder&);
    bool consume_quoted_identifier(StringBuilder&);
    bool consume_blob_literal(StringBuilder&);
    bool consume_exponent(StringBuilder&);
    bool consume_hexadecimal_number(StringBuilder&);

    bool match(char a, char b) const;
    bool is_identifier_start() const;
    bool is_identifier_middle() const;
    bool is_numeric_literal_start() const;
    bool is_string_literal_start() const;
    bool is_string_literal_end() const;
    bool is_quoted_identifier_start() const;
    bool is_quoted_identifier_end() const;
    bool is_blob_literal_start() const;
    bool is_line_comment_start() const;
    bool is_block_comment_start() const;
    bool is_block_comment_end() const;
    bool is_line_break() const;
    bool is_eof() const;

    static HashMap<ByteString, TokenType> s_keywords;
    static HashMap<char, TokenType> s_one_char_tokens;
    static HashMap<ByteString, TokenType> s_two_char_tokens;

    StringView m_source;
    size_t m_line_number { 1 };
    size_t m_line_column { 0 };
    char m_current_char { 0 };
    bool m_eof { false };
    size_t m_position { 0 };
};

}
