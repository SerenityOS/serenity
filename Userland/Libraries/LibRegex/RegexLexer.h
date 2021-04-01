/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include <AK/Forward.h>
#include <AK/StringView.h>

namespace regex {

#define ENUMERATE_REGEX_TOKENS              \
    __ENUMERATE_REGEX_TOKEN(Eof)            \
    __ENUMERATE_REGEX_TOKEN(Char)           \
    __ENUMERATE_REGEX_TOKEN(Circumflex)     \
    __ENUMERATE_REGEX_TOKEN(Period)         \
    __ENUMERATE_REGEX_TOKEN(LeftParen)      \
    __ENUMERATE_REGEX_TOKEN(RightParen)     \
    __ENUMERATE_REGEX_TOKEN(LeftCurly)      \
    __ENUMERATE_REGEX_TOKEN(RightCurly)     \
    __ENUMERATE_REGEX_TOKEN(LeftBracket)    \
    __ENUMERATE_REGEX_TOKEN(RightBracket)   \
    __ENUMERATE_REGEX_TOKEN(Asterisk)       \
    __ENUMERATE_REGEX_TOKEN(EscapeSequence) \
    __ENUMERATE_REGEX_TOKEN(Dollar)         \
    __ENUMERATE_REGEX_TOKEN(Pipe)           \
    __ENUMERATE_REGEX_TOKEN(Plus)           \
    __ENUMERATE_REGEX_TOKEN(Comma)          \
    __ENUMERATE_REGEX_TOKEN(Slash)          \
    __ENUMERATE_REGEX_TOKEN(EqualSign)      \
    __ENUMERATE_REGEX_TOKEN(HyphenMinus)    \
    __ENUMERATE_REGEX_TOKEN(Colon)          \
    __ENUMERATE_REGEX_TOKEN(Questionmark)

enum class TokenType {
#define __ENUMERATE_REGEX_TOKEN(x) x,
    ENUMERATE_REGEX_TOKENS
#undef __ENUMERATE_REGEX_TOKEN
};

class Token {
public:
    Token() = default;
    Token(const TokenType type, const size_t start_position, const StringView value)
        : m_type(type)
        , m_position(start_position)
        , m_value(value)
    {
    }

    TokenType type() const { return m_type; }
    const StringView& value() const { return m_value; }
    size_t position() const { return m_position; }

    const char* name() const;
    static const char* name(const TokenType);

private:
    TokenType m_type { TokenType::Eof };
    size_t m_position { 0 };
    StringView m_value { nullptr };
};

class Lexer {
public:
    Lexer() = default;
    explicit Lexer(const StringView source);
    Token next();
    void reset();
    void back(size_t offset);
    void set_source(const StringView source) { m_source = source; }
    bool try_skip(char);
    char skip();
    const auto& source() const { return m_source; }

private:
    ALWAYS_INLINE char peek(size_t offset = 0) const;
    ALWAYS_INLINE void consume();

    StringView m_source {};
    size_t m_position { 0 };
    size_t m_previous_position { 0 };
    Token m_current_token { TokenType::Eof, 0, StringView(nullptr) };
    int m_current_char { 0 };
};

}

using regex::Lexer;
