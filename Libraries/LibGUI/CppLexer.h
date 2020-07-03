/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/StringView.h>
#include <AK/Vector.h>

namespace GUI {

#define FOR_EACH_TOKEN_TYPE        \
    __TOKEN(Unknown)               \
    __TOKEN(Whitespace)            \
    __TOKEN(PreprocessorStatement) \
    __TOKEN(IncludeStatement)      \
    __TOKEN(IncludePath)           \
    __TOKEN(LeftParen)             \
    __TOKEN(RightParen)            \
    __TOKEN(LeftCurly)             \
    __TOKEN(RightCurly)            \
    __TOKEN(LeftBracket)           \
    __TOKEN(RightBracket)          \
    __TOKEN(Comma)                 \
    __TOKEN(Semicolon)             \
    __TOKEN(DoubleQuotedString)    \
    __TOKEN(SingleQuotedString)    \
    __TOKEN(EscapeSequence)        \
    __TOKEN(Comment)               \
    __TOKEN(Integer)               \
    __TOKEN(Float)                 \
    __TOKEN(Keyword)               \
    __TOKEN(KnownType)             \
    __TOKEN(Ellipsis)              \
    __TOKEN(Period)                \
    __TOKEN(Plus)                  \
    __TOKEN(PlusEquals)            \
    __TOKEN(PlusPlus)              \
    __TOKEN(Arrow)                 \
    __TOKEN(Minus)                 \
    __TOKEN(MinusEquals)           \
    __TOKEN(MinusMinus)            \
    __TOKEN(Equals)                \
    __TOKEN(EqualsEquals)          \
    __TOKEN(Ampersand)             \
    __TOKEN(AmpersandEquals)       \
    __TOKEN(DoubleAmpersand)       \
    __TOKEN(Tilde)                 \
    __TOKEN(Pipe)                  \
    __TOKEN(DoublePipe)            \
    __TOKEN(PipeEquals)            \
    __TOKEN(Caret)                 \
    __TOKEN(CaretEquals)           \
    __TOKEN(Percent)               \
    __TOKEN(PercentEquals)         \
    __TOKEN(Asterisk)              \
    __TOKEN(AsteriskEquals)        \
    __TOKEN(Slash)                 \
    __TOKEN(SlashEquals)           \
    __TOKEN(ExclamationMark)       \
    __TOKEN(ExclamationMarkEquals) \
    __TOKEN(LessThan)              \
    __TOKEN(LessThanEquals)        \
    __TOKEN(ShiftLeft)             \
    __TOKEN(ShiftLeftEquals)       \
    __TOKEN(GreaterThan)           \
    __TOKEN(GreaterThanEquals)     \
    __TOKEN(ShiftRight)            \
    __TOKEN(ShiftRightEquals)      \
    __TOKEN(QuestionMark)          \
    __TOKEN(Colon)                 \
    __TOKEN(ColonColon)            \
    __TOKEN(Identifier)

struct CppPosition {
    size_t line;
    size_t column;
};

struct CppToken {
    enum class Type {
#define __TOKEN(x) x,
        FOR_EACH_TOKEN_TYPE
#undef __TOKEN
    };

    const char* to_string() const
    {
        switch (m_type) {
#define __TOKEN(x) \
    case Type::x:  \
        return #x;
            FOR_EACH_TOKEN_TYPE
#undef __TOKEN
        }
        ASSERT_NOT_REACHED();
    }

    Type m_type { Type::Unknown };
    CppPosition m_start;
    CppPosition m_end;
};

class CppLexer {
public:
    CppLexer(const StringView&);

    Vector<CppToken> lex();

private:
    char peek(size_t offset = 0) const;
    char consume();

    StringView m_input;
    size_t m_index { 0 };
    CppPosition m_previous_position { 0, 0 };
    CppPosition m_position { 0, 0 };
};

}
