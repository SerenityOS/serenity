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

enum class TokenType {
    Ampersand,
    AmpersandEquals,
    Asterisk,
    AsteriskAsteriskEquals,
    AsteriskEquals,
    Await,
    BoolLiteral,
    BracketClose,
    BracketOpen,
    Break,
    Caret,
    Case,
    Catch,
    Class,
    Colon,
    Comma,
    Const,
    CurlyClose,
    CurlyOpen,
    Default,
    Delete,
    Do,
    DoubleAmpersand,
    DoubleAsterisk,
    DoublePipe,
    DoubleQuestionMark,
    Else,
    Eof,
    Equals,
    EqualsEquals,
    EqualsEqualsEquals,
    ExclamationMark,
    ExclamationMarkEquals,
    ExclamationMarkEqualsEquals,
    Finally,
    For,
    Function,
    GreaterThan,
    GreaterThanEquals,
    Identifier,
    If,
    In,
    Instanceof,
    Interface,
    Invalid,
    LessThan,
    LessThanEquals,
    Let,
    Minus,
    MinusEquals,
    MinusMinus,
    New,
    NullLiteral,
    NumericLiteral,
    ParenClose,
    ParenOpen,
    Percent,
    PercentEquals,
    Period,
    Pipe,
    PipeEquals,
    Plus,
    PlusEquals,
    PlusPlus,
    QuestionMark,
    QuestionMarkPeriod,
    RegexLiteral,
    Return,
    Semicolon,
    ShiftLeft,
    ShiftLeftEquals,
    ShiftRight,
    ShiftRightEquals,
    Slash,
    SlashEquals,
    StringLiteral,
    Switch,
    Throw,
    Tilde,
    Try,
    Typeof,
    UndefinedLiteral,
    UnsignedShiftRight,
    UnsignedShiftRightEquals,
    UnterminatedStringLiteral,
    Var,
    Void,
    While,
    Yield,
};

class Token {
public:
    Token(TokenType type, StringView trivia, StringView value)
        : m_type(type)
        , m_trivia(trivia)
        , m_value(value)
    {
    }

    TokenType type() const { return m_type; }
    const char* name() const;
    static const char* name(TokenType);

    const StringView& trivia() const { return m_trivia; }
    const StringView& value() const { return m_value; }
    double double_value() const;
    String string_value() const;
    bool bool_value() const;

private:
    TokenType m_type;
    StringView m_trivia;
    StringView m_value;
};

}

namespace AK {
template<>
struct Traits<JS::TokenType> : public GenericTraits<JS::TokenType> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(JS::TokenType t) { return int_hash((int)t); }
};
}
