/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/GenericLexer.h>
#include <AK/StringView.h>

namespace GUI {

#define ENUMERATE_TOKEN_TYPES(M) \
    M(Comment)                   \
    M(Word)                      \
    M(FormName)                  \
    M(DoubleQuotedString)        \
    M(SingleQuotedString)        \
    M(Number)                    \
    M(Unknown)                   \
    M(OpenParen)                 \
    M(CloseParen)                \
    M(OpenBrace)                 \
    M(CloseBrace)                \
    M(OpenBracket)               \
    M(CloseBracket)

struct BasicSExpressionPosition {
    size_t line;
    size_t column;
};

struct BasicSExpressionToken {
    enum class Type {
#define M(x) x,
        ENUMERATE_TOKEN_TYPES(M)
#undef M
    };

    char const* to_string() const
    {
        switch (m_type) {
#define M(x)      \
    case Type::x: \
        return #x;
            ENUMERATE_TOKEN_TYPES(M)
#undef M
        }
        VERIFY_NOT_REACHED();
    }

    Type m_type { Type::Unknown };
    BasicSExpressionPosition m_start;
    BasicSExpressionPosition m_end;
};

class BasicSExpressionLexer {
public:
    BasicSExpressionLexer(StringView);

    Vector<BasicSExpressionToken> lex();

private:
    GenericLexer m_lexer;
    BasicSExpressionPosition m_position { 0, 0 };
};

}
