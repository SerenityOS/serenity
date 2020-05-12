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

#include <AK/String.h>
#include <AK/StringView.h>

namespace JS {

#define ENUMERATE_JS_TOKENS                           \
    __ENUMERATE_JS_TOKEN(Ampersand)                   \
    __ENUMERATE_JS_TOKEN(AmpersandEquals)             \
    __ENUMERATE_JS_TOKEN(Arrow)                       \
    __ENUMERATE_JS_TOKEN(Asterisk)                    \
    __ENUMERATE_JS_TOKEN(DoubleAsteriskEquals)        \
    __ENUMERATE_JS_TOKEN(AsteriskEquals)              \
    __ENUMERATE_JS_TOKEN(Async)                       \
    __ENUMERATE_JS_TOKEN(Await)                       \
    __ENUMERATE_JS_TOKEN(BoolLiteral)                 \
    __ENUMERATE_JS_TOKEN(BracketClose)                \
    __ENUMERATE_JS_TOKEN(BracketOpen)                 \
    __ENUMERATE_JS_TOKEN(Break)                       \
    __ENUMERATE_JS_TOKEN(Caret)                       \
    __ENUMERATE_JS_TOKEN(CaretEquals)                 \
    __ENUMERATE_JS_TOKEN(Case)                        \
    __ENUMERATE_JS_TOKEN(Catch)                       \
    __ENUMERATE_JS_TOKEN(Class)                       \
    __ENUMERATE_JS_TOKEN(Colon)                       \
    __ENUMERATE_JS_TOKEN(Comma)                       \
    __ENUMERATE_JS_TOKEN(Const)                       \
    __ENUMERATE_JS_TOKEN(Continue)                    \
    __ENUMERATE_JS_TOKEN(CurlyClose)                  \
    __ENUMERATE_JS_TOKEN(CurlyOpen)                   \
    __ENUMERATE_JS_TOKEN(Debugger)                    \
    __ENUMERATE_JS_TOKEN(Default)                     \
    __ENUMERATE_JS_TOKEN(Delete)                      \
    __ENUMERATE_JS_TOKEN(Do)                          \
    __ENUMERATE_JS_TOKEN(DoubleAmpersand)             \
    __ENUMERATE_JS_TOKEN(DoubleAsterisk)              \
    __ENUMERATE_JS_TOKEN(DoublePipe)                  \
    __ENUMERATE_JS_TOKEN(DoubleQuestionMark)          \
    __ENUMERATE_JS_TOKEN(Else)                        \
    __ENUMERATE_JS_TOKEN(Enum)                        \
    __ENUMERATE_JS_TOKEN(Eof)                         \
    __ENUMERATE_JS_TOKEN(Equals)                      \
    __ENUMERATE_JS_TOKEN(EqualsEquals)                \
    __ENUMERATE_JS_TOKEN(EqualsEqualsEquals)          \
    __ENUMERATE_JS_TOKEN(ExclamationMark)             \
    __ENUMERATE_JS_TOKEN(ExclamationMarkEquals)       \
    __ENUMERATE_JS_TOKEN(ExclamationMarkEqualsEquals) \
    __ENUMERATE_JS_TOKEN(Export)                      \
    __ENUMERATE_JS_TOKEN(Extends)                     \
    __ENUMERATE_JS_TOKEN(Finally)                     \
    __ENUMERATE_JS_TOKEN(For)                         \
    __ENUMERATE_JS_TOKEN(Function)                    \
    __ENUMERATE_JS_TOKEN(GreaterThan)                 \
    __ENUMERATE_JS_TOKEN(GreaterThanEquals)           \
    __ENUMERATE_JS_TOKEN(Identifier)                  \
    __ENUMERATE_JS_TOKEN(If)                          \
    __ENUMERATE_JS_TOKEN(Implements)                  \
    __ENUMERATE_JS_TOKEN(Import)                      \
    __ENUMERATE_JS_TOKEN(In)                          \
    __ENUMERATE_JS_TOKEN(Instanceof)                  \
    __ENUMERATE_JS_TOKEN(Interface)                   \
    __ENUMERATE_JS_TOKEN(Invalid)                     \
    __ENUMERATE_JS_TOKEN(LessThan)                    \
    __ENUMERATE_JS_TOKEN(LessThanEquals)              \
    __ENUMERATE_JS_TOKEN(Let)                         \
    __ENUMERATE_JS_TOKEN(Minus)                       \
    __ENUMERATE_JS_TOKEN(MinusEquals)                 \
    __ENUMERATE_JS_TOKEN(MinusMinus)                  \
    __ENUMERATE_JS_TOKEN(New)                         \
    __ENUMERATE_JS_TOKEN(NullLiteral)                 \
    __ENUMERATE_JS_TOKEN(NumericLiteral)              \
    __ENUMERATE_JS_TOKEN(Package)                     \
    __ENUMERATE_JS_TOKEN(ParenClose)                  \
    __ENUMERATE_JS_TOKEN(ParenOpen)                   \
    __ENUMERATE_JS_TOKEN(Percent)                     \
    __ENUMERATE_JS_TOKEN(PercentEquals)               \
    __ENUMERATE_JS_TOKEN(Period)                      \
    __ENUMERATE_JS_TOKEN(Pipe)                        \
    __ENUMERATE_JS_TOKEN(PipeEquals)                  \
    __ENUMERATE_JS_TOKEN(Plus)                        \
    __ENUMERATE_JS_TOKEN(PlusEquals)                  \
    __ENUMERATE_JS_TOKEN(PlusPlus)                    \
    __ENUMERATE_JS_TOKEN(Private)                     \
    __ENUMERATE_JS_TOKEN(Protected)                   \
    __ENUMERATE_JS_TOKEN(Public)                      \
    __ENUMERATE_JS_TOKEN(QuestionMark)                \
    __ENUMERATE_JS_TOKEN(QuestionMarkPeriod)          \
    __ENUMERATE_JS_TOKEN(RegexLiteral)                \
    __ENUMERATE_JS_TOKEN(Return)                      \
    __ENUMERATE_JS_TOKEN(Semicolon)                   \
    __ENUMERATE_JS_TOKEN(ShiftLeft)                   \
    __ENUMERATE_JS_TOKEN(ShiftLeftEquals)             \
    __ENUMERATE_JS_TOKEN(ShiftRight)                  \
    __ENUMERATE_JS_TOKEN(ShiftRightEquals)            \
    __ENUMERATE_JS_TOKEN(Slash)                       \
    __ENUMERATE_JS_TOKEN(SlashEquals)                 \
    __ENUMERATE_JS_TOKEN(Static)                      \
    __ENUMERATE_JS_TOKEN(StringLiteral)               \
    __ENUMERATE_JS_TOKEN(Super)                       \
    __ENUMERATE_JS_TOKEN(Switch)                      \
    __ENUMERATE_JS_TOKEN(TemplateLiteralEnd)          \
    __ENUMERATE_JS_TOKEN(TemplateLiteralExprEnd)      \
    __ENUMERATE_JS_TOKEN(TemplateLiteralExprStart)    \
    __ENUMERATE_JS_TOKEN(TemplateLiteralStart)        \
    __ENUMERATE_JS_TOKEN(TemplateLiteralString)       \
    __ENUMERATE_JS_TOKEN(This)                        \
    __ENUMERATE_JS_TOKEN(Throw)                       \
    __ENUMERATE_JS_TOKEN(Tilde)                       \
    __ENUMERATE_JS_TOKEN(TripleDot)                   \
    __ENUMERATE_JS_TOKEN(Try)                         \
    __ENUMERATE_JS_TOKEN(Typeof)                      \
    __ENUMERATE_JS_TOKEN(UnsignedShiftRight)          \
    __ENUMERATE_JS_TOKEN(UnsignedShiftRightEquals)    \
    __ENUMERATE_JS_TOKEN(UnterminatedStringLiteral)   \
    __ENUMERATE_JS_TOKEN(UnterminatedTemplateLiteral) \
    __ENUMERATE_JS_TOKEN(Var)                         \
    __ENUMERATE_JS_TOKEN(Void)                        \
    __ENUMERATE_JS_TOKEN(While)                       \
    __ENUMERATE_JS_TOKEN(With)                        \
    __ENUMERATE_JS_TOKEN(Yield)

enum class TokenType {
#define __ENUMERATE_JS_TOKEN(x) x,
    ENUMERATE_JS_TOKENS
#undef __ENUMERATE_JS_TOKEN
};

class Token {
public:
    Token(TokenType type, StringView trivia, StringView value, size_t line_number, size_t line_column)
        : m_type(type)
        , m_trivia(trivia)
        , m_value(value)
        , m_line_number(line_number)
        , m_line_column(line_column)
    {
    }

    TokenType type() const { return m_type; }
    const char* name() const;
    static const char* name(TokenType);

    const StringView& trivia() const { return m_trivia; }
    const StringView& value() const { return m_value; }
    size_t line_number() const { return m_line_number; }
    size_t line_column() const { return m_line_column; }
    double double_value() const;
    String string_value() const;
    bool bool_value() const;

    bool is_identifier_name() const;

private:
    TokenType m_type;
    StringView m_trivia;
    StringView m_value;
    size_t m_line_number;
    size_t m_line_column;
};

}

namespace AK {
template<>
struct Traits<JS::TokenType> : public GenericTraits<JS::TokenType> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(JS::TokenType t) { return int_hash((int)t); }
};
}
