#pragma once

#include <AK/StringView.h>
#include <AK/Vector.h>

#define FOR_EACH_TOKEN_TYPE        \
    __TOKEN(Unknown)               \
    __TOKEN(Whitespace)            \
    __TOKEN(PreprocessorStatement) \
    __TOKEN(LeftParen)             \
    __TOKEN(RightParen)            \
    __TOKEN(LeftCurly)             \
    __TOKEN(RightCurly)            \
    __TOKEN(LeftBracket)           \
    __TOKEN(RightBracket)          \
    __TOKEN(Comma)                 \
    __TOKEN(Asterisk)              \
    __TOKEN(Semicolon)             \
    __TOKEN(DoubleQuotedString)    \
    __TOKEN(SingleQuotedString)    \
    __TOKEN(Comment)               \
    __TOKEN(Number)                \
    __TOKEN(Keyword)               \
    __TOKEN(Identifier)

struct CppPosition {
    int line { -1 };
    int column { -1 };
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
    char peek(int offset = 0) const;
    char consume();

    StringView m_input;
    int m_index { 0 };
    CppPosition m_previous_position { 0, 0 };
    CppPosition m_position { 0, 0 };
};
