/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace GUI::GML {

#define FOR_EACH_TOKEN_TYPE \
    __TOKEN(Unknown)        \
    __TOKEN(Comment)        \
    __TOKEN(ClassMarker)    \
    __TOKEN(ClassName)      \
    __TOKEN(LeftCurly)      \
    __TOKEN(RightCurly)     \
    __TOKEN(Identifier)     \
    __TOKEN(Colon)          \
    __TOKEN(JsonValue)

struct Position {
    size_t line;
    size_t column;
};

struct Token {
    enum class Type {
#define __TOKEN(x) x,
        FOR_EACH_TOKEN_TYPE
#undef __TOKEN
    };

    char const* to_string() const
    {
        switch (m_type) {
#define __TOKEN(x) \
    case Type::x:  \
        return #x;
            FOR_EACH_TOKEN_TYPE
#undef __TOKEN
        }
        VERIFY_NOT_REACHED();
    }

    Type m_type { Type::Unknown };
    StringView m_view;
    Position m_start;
    Position m_end;
};

class Lexer {
public:
    Lexer(StringView);

    Vector<Token> lex();

private:
    char peek(size_t offset = 0) const;
    char consume();

    StringView m_input;
    size_t m_index { 0 };
    Position m_position { 0, 0 };
};

}

#undef FOR_EACH_TOKEN_TYPE
