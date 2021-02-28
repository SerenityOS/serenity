/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
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

#pragma once

#include "Token.h"

#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringView.h>

namespace JS {

class Lexer {
public:
    explicit Lexer(StringView source, StringView filename = "(unknown)", size_t line_number = 1, size_t line_column = 0);

    Token next();

    const StringView& source() const { return m_source; };
    const StringView& filename() const { return m_filename; };

private:
    void consume();
    bool consume_exponent();
    bool consume_octal_number();
    bool consume_hexadecimal_number();
    bool consume_binary_number();
    bool is_eof() const;
    bool is_line_terminator() const;
    bool is_identifier_start() const;
    bool is_identifier_middle() const;
    bool is_line_comment_start(bool line_has_token_yet) const;
    bool is_block_comment_start() const;
    bool is_block_comment_end() const;
    bool is_numeric_literal_start() const;
    bool match(char, char) const;
    bool match(char, char, char) const;
    bool match(char, char, char, char) const;
    bool slash_means_division() const;

    StringView m_source;
    size_t m_position { 0 };
    Token m_current_token;
    char m_current_char { 0 };

    StringView m_filename;
    size_t m_line_number { 1 };
    size_t m_line_column { 0 };

    bool m_regex_is_in_character_class { false };

    struct TemplateState {
        bool in_expr;
        u8 open_bracket_count;
    };
    Vector<TemplateState> m_template_states;

    static HashMap<String, TokenType> s_keywords;
    static HashMap<String, TokenType> s_three_char_tokens;
    static HashMap<String, TokenType> s_two_char_tokens;
    static HashMap<char, TokenType> s_single_char_tokens;
};

}
