/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Utf8View.h>

namespace GUI {

#define FOR_EACH_TOKEN_TYPE \
    __TOKEN(Unknown)        \
    __TOKEN(Comment)        \
    __TOKEN(Whitespace)     \
    __TOKEN(Section)        \
    __TOKEN(LeftBracket)    \
    __TOKEN(RightBracket)   \
    __TOKEN(Name)           \
    __TOKEN(Value)          \
    __TOKEN(Equal)

struct IniPosition {
    size_t line;
    size_t column;
};

struct IniToken {
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
    IniPosition m_start;
    IniPosition m_end;
};

class IniLexer {
public:
    IniLexer(StringView);

    Vector<IniToken> lex();

private:
    u32 peek(size_t offset = 0) const;
    u32 consume();

    Utf8View m_input;
    Utf8CodePointIterator m_iterator;
    IniPosition m_position { 0, 0 };
};

}

#undef FOR_EACH_TOKEN_TYPE
