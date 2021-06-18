/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Token.h"
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringView.h>

namespace SQL {

class Lexer {
public:
    explicit Lexer(StringView source);

    Token next();

private:
    void consume();

    bool consume_whitespace_and_comments();
    bool consume_numeric_literal();
    bool consume_string_literal();
    bool consume_blob_literal();
    bool consume_exponent();
    bool consume_hexadecimal_number();

    bool match(char a, char b) const;
    bool is_identifier_start() const;
    bool is_identifier_middle() const;
    bool is_numeric_literal_start() const;
    bool is_string_literal_start() const;
    bool is_string_literal_end() const;
    bool is_blob_literal_start() const;
    bool is_line_comment_start() const;
    bool is_block_comment_start() const;
    bool is_block_comment_end() const;
    bool is_line_break() const;
    bool is_eof() const;

    static HashMap<String, TokenType> s_keywords;
    static HashMap<char, TokenType> s_one_char_tokens;
    static HashMap<String, TokenType> s_two_char_tokens;

    StringView m_source;
    size_t m_line_number { 1 };
    size_t m_line_column { 0 };
    char m_current_char { 0 };
    bool m_eof { false };
    size_t m_position { 0 };
};

}
