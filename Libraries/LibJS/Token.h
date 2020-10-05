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

#define ENUMERATE_JS_TOKENS                                     \
    __ENUMERATE_JS_TOKEN(Ampersand, Operator)                   \
    __ENUMERATE_JS_TOKEN(AmpersandEquals, Operator)             \
    __ENUMERATE_JS_TOKEN(Arrow, Operator)                       \
    __ENUMERATE_JS_TOKEN(Asterisk, Operator)                    \
    __ENUMERATE_JS_TOKEN(AsteriskEquals, Operator)              \
    __ENUMERATE_JS_TOKEN(Async, Keyword)                        \
    __ENUMERATE_JS_TOKEN(Await, Keyword)                        \
    __ENUMERATE_JS_TOKEN(BigIntLiteral, Number)                 \
    __ENUMERATE_JS_TOKEN(BoolLiteral, Keyword)                  \
    __ENUMERATE_JS_TOKEN(BracketClose, Punctuation)             \
    __ENUMERATE_JS_TOKEN(BracketOpen, Punctuation)              \
    __ENUMERATE_JS_TOKEN(Break, Keyword)                        \
    __ENUMERATE_JS_TOKEN(Caret, Operator)                       \
    __ENUMERATE_JS_TOKEN(CaretEquals, Operator)                 \
    __ENUMERATE_JS_TOKEN(Case, ControlKeyword)                  \
    __ENUMERATE_JS_TOKEN(Catch, ControlKeyword)                 \
    __ENUMERATE_JS_TOKEN(Class, Keyword)                        \
    __ENUMERATE_JS_TOKEN(Colon, Punctuation)                    \
    __ENUMERATE_JS_TOKEN(Comma, Punctuation)                    \
    __ENUMERATE_JS_TOKEN(Const, Keyword)                        \
    __ENUMERATE_JS_TOKEN(Continue, ControlKeyword)              \
    __ENUMERATE_JS_TOKEN(CurlyClose, Punctuation)               \
    __ENUMERATE_JS_TOKEN(CurlyOpen, Punctuation)                \
    __ENUMERATE_JS_TOKEN(Debugger, Keyword)                     \
    __ENUMERATE_JS_TOKEN(Default, ControlKeyword)               \
    __ENUMERATE_JS_TOKEN(Delete, Keyword)                       \
    __ENUMERATE_JS_TOKEN(Do, ControlKeyword)                    \
    __ENUMERATE_JS_TOKEN(DoubleAmpersand, Operator)             \
    __ENUMERATE_JS_TOKEN(DoubleAmpersandEquals, Operator)       \
    __ENUMERATE_JS_TOKEN(DoubleAsterisk, Operator)              \
    __ENUMERATE_JS_TOKEN(DoubleAsteriskEquals, Operator)        \
    __ENUMERATE_JS_TOKEN(DoublePipe, Operator)                  \
    __ENUMERATE_JS_TOKEN(DoublePipeEquals, Operator)            \
    __ENUMERATE_JS_TOKEN(DoubleQuestionMark, Operator)          \
    __ENUMERATE_JS_TOKEN(DoubleQuestionMarkEquals, Operator)    \
    __ENUMERATE_JS_TOKEN(Else, ControlKeyword)                  \
    __ENUMERATE_JS_TOKEN(Enum, Keyword)                         \
    __ENUMERATE_JS_TOKEN(Eof, Invalid)                          \
    __ENUMERATE_JS_TOKEN(Equals, Operator)                      \
    __ENUMERATE_JS_TOKEN(EqualsEquals, Operator)                \
    __ENUMERATE_JS_TOKEN(EqualsEqualsEquals, Operator)          \
    __ENUMERATE_JS_TOKEN(ExclamationMark, Operator)             \
    __ENUMERATE_JS_TOKEN(ExclamationMarkEquals, Operator)       \
    __ENUMERATE_JS_TOKEN(ExclamationMarkEqualsEquals, Operator) \
    __ENUMERATE_JS_TOKEN(Export, Keyword)                       \
    __ENUMERATE_JS_TOKEN(Extends, Keyword)                      \
    __ENUMERATE_JS_TOKEN(Finally, ControlKeyword)               \
    __ENUMERATE_JS_TOKEN(For, ControlKeyword)                   \
    __ENUMERATE_JS_TOKEN(Function, Keyword)                     \
    __ENUMERATE_JS_TOKEN(GreaterThan, Operator)                 \
    __ENUMERATE_JS_TOKEN(GreaterThanEquals, Operator)           \
    __ENUMERATE_JS_TOKEN(Identifier, Identifier)                \
    __ENUMERATE_JS_TOKEN(If, ControlKeyword)                    \
    __ENUMERATE_JS_TOKEN(Implements, Keyword)                   \
    __ENUMERATE_JS_TOKEN(Import, Keyword)                       \
    __ENUMERATE_JS_TOKEN(In, Keyword)                           \
    __ENUMERATE_JS_TOKEN(Instanceof, Keyword)                   \
    __ENUMERATE_JS_TOKEN(Interface, Keyword)                    \
    __ENUMERATE_JS_TOKEN(Invalid, Invalid)                      \
    __ENUMERATE_JS_TOKEN(LessThan, Operator)                    \
    __ENUMERATE_JS_TOKEN(LessThanEquals, Operator)              \
    __ENUMERATE_JS_TOKEN(Let, Keyword)                          \
    __ENUMERATE_JS_TOKEN(Minus, Operator)                       \
    __ENUMERATE_JS_TOKEN(MinusEquals, Operator)                 \
    __ENUMERATE_JS_TOKEN(MinusMinus, Operator)                  \
    __ENUMERATE_JS_TOKEN(New, Keyword)                          \
    __ENUMERATE_JS_TOKEN(NullLiteral, Keyword)                  \
    __ENUMERATE_JS_TOKEN(NumericLiteral, Number)                \
    __ENUMERATE_JS_TOKEN(Package, Keyword)                      \
    __ENUMERATE_JS_TOKEN(ParenClose, Punctuation)               \
    __ENUMERATE_JS_TOKEN(ParenOpen, Punctuation)                \
    __ENUMERATE_JS_TOKEN(Percent, Operator)                     \
    __ENUMERATE_JS_TOKEN(PercentEquals, Operator)               \
    __ENUMERATE_JS_TOKEN(Period, Operator)                      \
    __ENUMERATE_JS_TOKEN(Pipe, Operator)                        \
    __ENUMERATE_JS_TOKEN(PipeEquals, Operator)                  \
    __ENUMERATE_JS_TOKEN(Plus, Operator)                        \
    __ENUMERATE_JS_TOKEN(PlusEquals, Operator)                  \
    __ENUMERATE_JS_TOKEN(PlusPlus, Operator)                    \
    __ENUMERATE_JS_TOKEN(Private, Keyword)                      \
    __ENUMERATE_JS_TOKEN(Protected, Keyword)                    \
    __ENUMERATE_JS_TOKEN(Public, Keyword)                       \
    __ENUMERATE_JS_TOKEN(QuestionMark, Operator)                \
    __ENUMERATE_JS_TOKEN(QuestionMarkPeriod, Operator)          \
    __ENUMERATE_JS_TOKEN(RegexFlags, String)                    \
    __ENUMERATE_JS_TOKEN(RegexLiteral, String)                  \
    __ENUMERATE_JS_TOKEN(Return, ControlKeyword)                \
    __ENUMERATE_JS_TOKEN(Semicolon, Punctuation)                \
    __ENUMERATE_JS_TOKEN(ShiftLeft, Operator)                   \
    __ENUMERATE_JS_TOKEN(ShiftLeftEquals, Operator)             \
    __ENUMERATE_JS_TOKEN(ShiftRight, Operator)                  \
    __ENUMERATE_JS_TOKEN(ShiftRightEquals, Operator)            \
    __ENUMERATE_JS_TOKEN(Slash, Operator)                       \
    __ENUMERATE_JS_TOKEN(SlashEquals, Operator)                 \
    __ENUMERATE_JS_TOKEN(Static, Keyword)                       \
    __ENUMERATE_JS_TOKEN(StringLiteral, String)                 \
    __ENUMERATE_JS_TOKEN(Super, Keyword)                        \
    __ENUMERATE_JS_TOKEN(Switch, ControlKeyword)                \
    __ENUMERATE_JS_TOKEN(TemplateLiteralEnd, String)            \
    __ENUMERATE_JS_TOKEN(TemplateLiteralExprEnd, Punctuation)   \
    __ENUMERATE_JS_TOKEN(TemplateLiteralExprStart, Punctuation) \
    __ENUMERATE_JS_TOKEN(TemplateLiteralStart, String)          \
    __ENUMERATE_JS_TOKEN(TemplateLiteralString, String)         \
    __ENUMERATE_JS_TOKEN(This, Keyword)                         \
    __ENUMERATE_JS_TOKEN(Throw, ControlKeyword)                 \
    __ENUMERATE_JS_TOKEN(Tilde, Operator)                       \
    __ENUMERATE_JS_TOKEN(TripleDot, Operator)                   \
    __ENUMERATE_JS_TOKEN(Try, ControlKeyword)                   \
    __ENUMERATE_JS_TOKEN(Typeof, Keyword)                       \
    __ENUMERATE_JS_TOKEN(UnsignedShiftRight, Operator)          \
    __ENUMERATE_JS_TOKEN(UnsignedShiftRightEquals, Operator)    \
    __ENUMERATE_JS_TOKEN(UnterminatedRegexLiteral, String)      \
    __ENUMERATE_JS_TOKEN(UnterminatedStringLiteral, String)     \
    __ENUMERATE_JS_TOKEN(UnterminatedTemplateLiteral, String)   \
    __ENUMERATE_JS_TOKEN(Var, Keyword)                          \
    __ENUMERATE_JS_TOKEN(Void, Keyword)                         \
    __ENUMERATE_JS_TOKEN(While, ControlKeyword)                 \
    __ENUMERATE_JS_TOKEN(With, ControlKeyword)                  \
    __ENUMERATE_JS_TOKEN(Yield, ControlKeyword)

enum class TokenType {
#define __ENUMERATE_JS_TOKEN(type, category) type,
    ENUMERATE_JS_TOKENS
#undef __ENUMERATE_JS_TOKEN
        _COUNT_OF_TOKENS
};
constexpr size_t cs_num_of_js_tokens = static_cast<size_t>(TokenType::_COUNT_OF_TOKENS);

enum class TokenCategory {
    Invalid,
    Number,
    String,
    Punctuation,
    Operator,
    Keyword,
    ControlKeyword,
    Identifier
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
    TokenCategory category() const;
    static TokenCategory category(TokenType);
    const char* name() const;
    static const char* name(TokenType);

    const StringView& trivia() const { return m_trivia; }
    const StringView& value() const { return m_value; }
    size_t line_number() const { return m_line_number; }
    size_t line_column() const { return m_line_column; }
    double double_value() const;
    bool bool_value() const;

    enum class StringValueStatus {
        Ok,
        MalformedHexEscape,
        MalformedUnicodeEscape,
        UnicodeEscapeOverflow,
    };
    String string_value(StringValueStatus& status) const;

    bool is_identifier_name() const;

private:
    TokenType m_type;
    StringView m_trivia;
    StringView m_value;
    size_t m_line_number;
    size_t m_line_column;
};

}
