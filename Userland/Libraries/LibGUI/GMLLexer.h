/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace GUI {

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

struct GMLPosition {
    size_t line;
    size_t column;
};

struct GMLToken {
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
    GMLPosition m_start;
    GMLPosition m_end;
};

class GMLLexer {
public:
    GMLLexer(StringView);

    Vector<GMLToken> lex();

private:
    char peek(size_t offset = 0) const;
    char consume();

    StringView m_input;
    size_t m_index { 0 };
    GMLPosition m_position { 0, 0 };
};

}
