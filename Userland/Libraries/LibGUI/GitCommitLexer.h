/*
 * Copyright (c) 2022, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

namespace GUI {

#define FOR_EACH_TOKEN_TYPE \
    __TOKEN(Comment)        \
    __TOKEN(Unknown)

struct GitCommitPosition {
    size_t line;
    size_t column;
};

struct GitCommitToken {
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
    GitCommitPosition m_start;
    GitCommitPosition m_end;
};

class GitCommitLexer {
public:
    GitCommitLexer(StringView);

    Vector<GitCommitToken> lex();

private:
    char peek(size_t offset = 0) const;
    char consume();

    StringView m_input;
    size_t m_index { 0 };
    GitCommitPosition m_position { 0, 0 };
};

}

#undef FOR_EACH_TOKEN_TYPE
