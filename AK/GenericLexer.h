/*
 * Copyright (c) 2020, Benoit Lormeau <blormeau@outlook.com>
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

#include <AK/Function.h>
#include <AK/StringView.h>

namespace AK {

class GenericLexer {
public:
    explicit GenericLexer(const StringView& input);
    virtual ~GenericLexer();

    using Condition = Function<bool(char)>;

    size_t tell() const { return m_index; }
    size_t tell_remaining() const { return m_input.length() - m_index; }

    bool is_eof() const;

    char peek(size_t offset = 0) const;

    bool next_is(char) const;
    bool next_is(StringView) const;
    bool next_is(const char*) const;
    bool next_is(Condition) const;

    char consume();
    bool consume_specific(char);
    bool consume_specific(StringView);
    bool consume_specific(const char*);
    StringView consume(size_t count);
    StringView consume_all();
    StringView consume_line();
    StringView consume_while(Condition);
    StringView consume_until(char);
    StringView consume_until(const char*);
    StringView consume_until(Condition);
    // FIXME: provide an escape character
    StringView consume_quoted_string();

    void ignore(size_t count = 1);
    void ignore_while(Condition);
    void ignore_until(char);
    void ignore_until(const char*);
    void ignore_until(Condition);

protected:
    StringView m_input;
    size_t m_index { 0 };
};

constexpr auto is_any_of(const StringView& values)
{
    return [values](auto c) { return values.contains(c); };
}

// ctype adaptors
// FIXME: maybe put them in an another file?
bool is_alpha(char);
bool is_alphanum(char);
bool is_control(char);
bool is_digit(char);
bool is_graphic(char);
bool is_hex_digit(char);
bool is_lowercase(char);
bool is_path_separator(char);
bool is_printable(char);
bool is_punctuation(char);
bool is_quote(char);
bool is_uppercase(char);
bool is_whitespace(char);

}

using AK::GenericLexer;

using AK::is_alpha;
using AK::is_alphanum;
using AK::is_any_of;
using AK::is_control;
using AK::is_digit;
using AK::is_graphic;
using AK::is_hex_digit;
using AK::is_lowercase;
using AK::is_path_separator;
using AK::is_printable;
using AK::is_punctuation;
using AK::is_quote;
using AK::is_uppercase;
using AK::is_whitespace;
