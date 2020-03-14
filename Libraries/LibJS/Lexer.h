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
    explicit Lexer(StringView source);
    Token next();
    bool has_errors() const { return m_has_errors; }

private:
    void consume();
    bool is_eof() const;
    bool is_identifier_start() const;
    bool is_identifier_middle() const;
    bool is_line_comment_start() const;
    bool is_block_comment_start() const;
    bool is_block_comment_end() const;

    void syntax_error(const char*);

    StringView m_source;
    size_t m_position = 0;
    Token m_current_token;
    int m_current_char;
    bool m_has_errors = false;

    static HashMap<String, TokenType> s_keywords;
    static HashMap<String, TokenType> s_three_char_tokens;
    static HashMap<String, TokenType> s_two_char_tokens;
    static HashMap<char, TokenType> s_single_char_tokens;
};

}
