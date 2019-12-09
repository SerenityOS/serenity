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
    __TOKEN(KnownType)             \
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
