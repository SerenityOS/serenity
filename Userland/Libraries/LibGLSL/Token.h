/*
 * Copyright (c) 2022, Melody Goad <mszoopers@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/FlyString.h>
#include <AK/StringView.h>
#include <AK/Variant.h>

namespace GLSL {

#define ENUMERATE_GLSL_TOKENS                               \
    __ENUMERATE_GLSL_TOKEN(Ampersand, Operator)             \
    __ENUMERATE_GLSL_TOKEN(AmpersandEquals, Operator)       \
    __ENUMERATE_GLSL_TOKEN(Asm, Keyword)                    \
    __ENUMERATE_GLSL_TOKEN(Asterisk, Operator)              \
    __ENUMERATE_GLSL_TOKEN(AsteriskEquals, Operator)        \
    __ENUMERATE_GLSL_TOKEN(Attribute, Keyword)              \
    __ENUMERATE_GLSL_TOKEN(Bool, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(BoolLiteral, Keyword)            \
    __ENUMERATE_GLSL_TOKEN(BracketClose, Punctuation)       \
    __ENUMERATE_GLSL_TOKEN(BracketOpen, Punctuation)        \
    __ENUMERATE_GLSL_TOKEN(Break, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Bvec2, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Bvec3, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Bvec4, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Cast, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Caret, Operator)                 \
    __ENUMERATE_GLSL_TOKEN(CaretEquals, Operator)           \
    __ENUMERATE_GLSL_TOKEN(Centroid, Keyword)               \
    __ENUMERATE_GLSL_TOKEN(Class, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Colon, Punctuation)              \
    __ENUMERATE_GLSL_TOKEN(Comma, Punctuation)              \
    __ENUMERATE_GLSL_TOKEN(Const, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Continue, ControlKeyword)        \
    __ENUMERATE_GLSL_TOKEN(CurlyClose, Punctuation)         \
    __ENUMERATE_GLSL_TOKEN(CurlyOpen, Punctuation)          \
    __ENUMERATE_GLSL_TOKEN(Default, ControlKeyword)         \
    __ENUMERATE_GLSL_TOKEN(Discard, ControlKeyword)         \
    __ENUMERATE_GLSL_TOKEN(Do, ControlKeyword)              \
    __ENUMERATE_GLSL_TOKEN(Double, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(DoubleAmpersand, Operator)       \
    __ENUMERATE_GLSL_TOKEN(DoublePipe, Operator)            \
    __ENUMERATE_GLSL_TOKEN(Dvec2, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Dvec3, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Dvec4, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Else, ControlKeyword)            \
    __ENUMERATE_GLSL_TOKEN(Enum, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Eof, Invalid)                    \
    __ENUMERATE_GLSL_TOKEN(Equals, Operator)                \
    __ENUMERATE_GLSL_TOKEN(EqualsEquals, Operator)          \
    __ENUMERATE_GLSL_TOKEN(ExclamationMark, Operator)       \
    __ENUMERATE_GLSL_TOKEN(ExclamationMarkEquals, Operator) \
    __ENUMERATE_GLSL_TOKEN(Extern, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(External, Keyword)               \
    __ENUMERATE_GLSL_TOKEN(Fixed, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Float, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(For, ControlKeyword)             \
    __ENUMERATE_GLSL_TOKEN(Fvec2, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Fvec3, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Fvec4, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Goto, ControlKeyword)            \
    __ENUMERATE_GLSL_TOKEN(GreaterThan, Operator)           \
    __ENUMERATE_GLSL_TOKEN(GreaterThanEquals, Operator)     \
    __ENUMERATE_GLSL_TOKEN(Half, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Highp, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Hvec2, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Hvec3, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Hvec4, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Identifier, Identifier)          \
    __ENUMERATE_GLSL_TOKEN(If, ControlKeyword)              \
    __ENUMERATE_GLSL_TOKEN(In, Keyword)                     \
    __ENUMERATE_GLSL_TOKEN(Inline, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Inout, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Input, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Int, Keyword)                    \
    __ENUMERATE_GLSL_TOKEN(Interface, Keyword)              \
    __ENUMERATE_GLSL_TOKEN(Invariant, Keyword)              \
    __ENUMERATE_GLSL_TOKEN(Invalid, Invalid)                \
    __ENUMERATE_GLSL_TOKEN(Ivec2, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Ivec3, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Ivec4, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(LessThan, Operator)              \
    __ENUMERATE_GLSL_TOKEN(LessThanEquals, Operator)        \
    __ENUMERATE_GLSL_TOKEN(Long, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Lowp, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Mat2, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Mat2x2, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Mat2x3, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Mat2x4, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Mat3, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Mat3x2, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Mat3x3, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Mat3x4, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Mat4, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Mat4x2, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Mat4x3, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Mat4x4, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Mediump, Keyword)                \
    __ENUMERATE_GLSL_TOKEN(Minus, Operator)                 \
    __ENUMERATE_GLSL_TOKEN(MinusEquals, Operator)           \
    __ENUMERATE_GLSL_TOKEN(MinusMinus, Operator)            \
    __ENUMERATE_GLSL_TOKEN(Namespace, Keyword)              \
    __ENUMERATE_GLSL_TOKEN(Noinline, Keyword)               \
    __ENUMERATE_GLSL_TOKEN(NumberSign, Punctuation)         \
    __ENUMERATE_GLSL_TOKEN(NumericLiteral, Number)          \
    __ENUMERATE_GLSL_TOKEN(Out, Keyword)                    \
    __ENUMERATE_GLSL_TOKEN(Output, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Packed, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(ParenClose, Punctuation)         \
    __ENUMERATE_GLSL_TOKEN(ParenOpen, Punctuation)          \
    __ENUMERATE_GLSL_TOKEN(Percent, Operator)               \
    __ENUMERATE_GLSL_TOKEN(PercentEquals, Operator)         \
    __ENUMERATE_GLSL_TOKEN(Period, Operator)                \
    __ENUMERATE_GLSL_TOKEN(Pipe, Operator)                  \
    __ENUMERATE_GLSL_TOKEN(PipeEquals, Operator)            \
    __ENUMERATE_GLSL_TOKEN(Plus, Operator)                  \
    __ENUMERATE_GLSL_TOKEN(PlusEquals, Operator)            \
    __ENUMERATE_GLSL_TOKEN(PlusPlus, Operator)              \
    __ENUMERATE_GLSL_TOKEN(Precision, Keyword)              \
    __ENUMERATE_GLSL_TOKEN(Public, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Return, ControlKeyword)          \
    __ENUMERATE_GLSL_TOKEN(Sampler1D, Keyword)              \
    __ENUMERATE_GLSL_TOKEN(Sampler1DShadow, Keyword)        \
    __ENUMERATE_GLSL_TOKEN(Sampler2D, Keyword)              \
    __ENUMERATE_GLSL_TOKEN(Sampler2DRect, Keyword)          \
    __ENUMERATE_GLSL_TOKEN(Sampler2DRectShadow, Keyword)    \
    __ENUMERATE_GLSL_TOKEN(Sampler2DShadow, Keyword)        \
    __ENUMERATE_GLSL_TOKEN(Sampler3D, Keyword)              \
    __ENUMERATE_GLSL_TOKEN(Sampler3DRect, Keyword)          \
    __ENUMERATE_GLSL_TOKEN(SamplerCube, Keyword)            \
    __ENUMERATE_GLSL_TOKEN(Semicolon, Punctuation)          \
    __ENUMERATE_GLSL_TOKEN(ShiftLeft, Operator)             \
    __ENUMERATE_GLSL_TOKEN(ShiftLeftEquals, Operator)       \
    __ENUMERATE_GLSL_TOKEN(ShiftRight, Operator)            \
    __ENUMERATE_GLSL_TOKEN(ShiftRightEquals, Operator)      \
    __ENUMERATE_GLSL_TOKEN(Short, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Sizeof, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Slash, Operator)                 \
    __ENUMERATE_GLSL_TOKEN(SlashEquals, Operator)           \
    __ENUMERATE_GLSL_TOKEN(Static, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Struct, Keyword)                 \
    __ENUMERATE_GLSL_TOKEN(Switch, ControlKeyword)          \
    __ENUMERATE_GLSL_TOKEN(Template, Keyword)               \
    __ENUMERATE_GLSL_TOKEN(Tilde, Operator)                 \
    __ENUMERATE_GLSL_TOKEN(This, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Typedef, Keyword)                \
    __ENUMERATE_GLSL_TOKEN(Uniform, Keyword)                \
    __ENUMERATE_GLSL_TOKEN(Union, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Unsigned, Keyword)               \
    __ENUMERATE_GLSL_TOKEN(Using, Keyword)                  \
    __ENUMERATE_GLSL_TOKEN(Varying, Keyword)                \
    __ENUMERATE_GLSL_TOKEN(Vec2, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Vec3, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Vec4, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Void, Keyword)                   \
    __ENUMERATE_GLSL_TOKEN(Volatile, Keyword)               \
    __ENUMERATE_GLSL_TOKEN(While, ControlKeyword)

enum class TokenType {
#define __ENUMERATE_GLSL_TOKEN(type, category) type,
    ENUMERATE_GLSL_TOKENS
#undef __ENUMERATE_GLSL_TOKEN
        _COUNT_OF_TOKENS
};
constexpr size_t cs_num_of_glsl_tokens = static_cast<size_t>(TokenType::_COUNT_OF_TOKENS);

enum class TokenCategory {
    Invalid,
    Number,
    Punctuation,
    Operator,
    Keyword,
    ControlKeyword,
    Identifier
};

class Token {
public:
    Token() = default;

    Token(TokenType type, DeprecatedString message, StringView trivia, StringView value, size_t line_number, size_t line_column, size_t offset)
        : m_type(type)
        , m_message(message)
        , m_trivia(trivia)
        , m_original_value(value)
        , m_value(value)
        , m_line_number(line_number)
        , m_line_column(line_column)
        , m_offset(offset)
    {
    }

    TokenType type() const { return m_type; }
    TokenCategory category() const;
    static TokenCategory category(TokenType);
    char const* name() const;
    static char const* name(TokenType);

    DeprecatedString const& message() const { return m_message; }
    StringView trivia() const { return m_trivia; }
    StringView original_value() const { return m_original_value; }
    StringView value() const
    {
        return m_value.visit(
            [](StringView view) { return view; },
            [](FlyString const& identifier) { return identifier.view(); },
            [](Empty) -> StringView { VERIFY_NOT_REACHED(); });
    }

    FlyString flystring_value() const
    {
        return m_value.visit(
            [](StringView view) -> FlyString { return view; },
            [](FlyString const& identifier) -> FlyString { return identifier; },
            [](Empty) -> FlyString { VERIFY_NOT_REACHED(); });
    }

    size_t line_number() const { return m_line_number; }
    size_t line_column() const { return m_line_column; }
    size_t offset() const { return m_offset; }
    double double_value() const;
    bool bool_value() const;

    void set_identifier_value(FlyString value)
    {
        m_value = move(value);
    }

    bool is_identifier_name() const;
    bool trivia_contains_line_terminator() const;

private:
    TokenType m_type { TokenType::Invalid };
    DeprecatedString m_message;
    StringView m_trivia;
    StringView m_original_value;
    Variant<Empty, StringView, FlyString> m_value {};
    size_t m_line_number { 0 };
    size_t m_line_column { 0 };
    size_t m_offset { 0 };
};

}
