/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibXML/Forward.h>

#include "AST/AST.h"
#include "DiagnosticEngine.h"

namespace JSSpecCompiler {

constexpr i32 ambiguous_operator_precedence = -2;
constexpr i32 pre_merged_operator_precedence = 2;
constexpr i32 unary_operator_precedence = 3;
constexpr i32 closing_bracket_precedence = 18;

// NOTE: Operator precedence is generally the same as in
//       https://en.cppreference.com/w/cpp/language/operator_precedence (common sense applies).
#define ENUMERATE_TOKENS(F)                                                          \
    F(Invalid, -1, Invalid, Invalid, Invalid, "")                                    \
    F(AmbiguousMinus, -2, Invalid, Invalid, Invalid, "minus")                        \
    F(BinaryMinus, 6, Invalid, Minus, Invalid, "binary minus")                       \
    F(BraceClose, 18, Invalid, Invalid, BraceOpen, "'}'")                            \
    F(BraceOpen, -1, Invalid, Invalid, BraceClose, "'{'")                            \
    F(Colon, -1, Invalid, Invalid, Invalid, "':'")                                   \
    F(Comma, 17, Invalid, Comma, Invalid, "','")                                     \
    F(Division, 5, Invalid, Division, Invalid, "division")                           \
    F(Dot, -1, Invalid, Invalid, Invalid, "punctuation mark '.'")                    \
    F(Enumerator, -1, Invalid, Invalid, Invalid, "enumerator")                       \
    F(Equals, 10, Invalid, CompareEqual, Invalid, "equals")                          \
    F(ExclamationMark, 3, AssertCompletion, Invalid, Invalid, "exclamation mark")    \
    F(Greater, 9, Invalid, CompareGreater, Invalid, "greater than")                  \
    F(Identifier, -1, Invalid, Invalid, Invalid, "identifier")                       \
    F(Is, -1, Invalid, Invalid, Invalid, "operator is")                              \
    F(Less, 9, Invalid, CompareLess, Invalid, "less than")                           \
    F(ListEnd, -1, Invalid, Invalid, Invalid, "»")                                   \
    F(ListStart, -1, Invalid, Invalid, Invalid, "«")                                 \
    F(MemberAccess, 2, Invalid, MemberAccess, Invalid, "member access operator '.'") \
    F(Multiplication, 5, Invalid, Multiplication, Invalid, "multiplication")         \
    F(NotEquals, 10, Invalid, CompareNotEqual, Invalid, "not equals")                \
    F(Number, -1, Invalid, Invalid, Invalid, "number")                               \
    F(ParenClose, 18, Invalid, Invalid, ParenOpen, "')'")                            \
    F(ParenOpen, -1, Invalid, Invalid, ParenClose, "'('")                            \
    F(Plus, 6, Invalid, Plus, Invalid, "plus")                                       \
    F(QuestionMark, 3, ReturnIfAbrubt, Invalid, Invalid, "question mark")            \
    F(SectionNumber, -1, Invalid, Invalid, Invalid, "section number")                \
    F(SquareBracketClose, -1, Invalid, Invalid, Invalid, "']'")                      \
    F(SquareBracketOpen, -1, Invalid, Invalid, Invalid, "'['")                       \
    F(String, -1, Invalid, Invalid, Invalid, "string literal")                       \
    F(Superscript, 4, Invalid, Power, Invalid, "subscript")                          \
    F(UnaryMinus, 3, Minus, Invalid, Invalid, "unary minus")                         \
    F(WellKnownValue, -1, Invalid, Invalid, Invalid, "constant")                     \
    F(Word, -1, Invalid, Invalid, Invalid, "word")

enum class TokenType {
#define ID(name, precedence, unary_name, binary_name, matching_bracket, name_for_diagnostic) name,
    ENUMERATE_TOKENS(ID)
#undef ID
};

constexpr struct TokenInfo {
    StringView name;
    i32 precedence;
    UnaryOperator as_unary_operator;
    BinaryOperator as_binary_operator;
    TokenType matching_bracket;
    StringView name_for_diagnostic;
} token_info[] = {
#define TOKEN_INFO(name, precedence, unary_name, binary_name, matching_bracket, name_for_diagnostic) \
    {                                                                                                \
        #name##sv,                                                                                   \
        precedence,                                                                                  \
        UnaryOperator::unary_name,                                                                   \
        BinaryOperator::binary_name,                                                                 \
        TokenType::matching_bracket,                                                                 \
        name_for_diagnostic##sv                                                                      \
    },
    ENUMERATE_TOKENS(TOKEN_INFO)
#undef TOKEN_INFO
};

struct Token {
    TokenInfo const& info() const { return token_info[to_underlying(type)]; }

    StringView name() const { return info().name; }
    StringView name_for_diagnostic() const { return info().name_for_diagnostic; }
    i32 precedence() const { return info().precedence; }
    bool is_operator() const { return precedence() > 0 && precedence() < closing_bracket_precedence; }
    bool is_ambiguous_operator() const { return precedence() == ambiguous_operator_precedence; }
    bool is_pre_merged_binary_operator() const { return precedence() == pre_merged_operator_precedence; }
    bool is_unary_operator() const { return precedence() == unary_operator_precedence; }
    bool is_binary_operator() const { return is_operator() && !is_unary_operator(); }
    bool is_bracket() const { return info().matching_bracket != TokenType::Invalid; }
    bool is_opening_bracket() const { return is_bracket() && precedence() == -1; }
    bool is_closing_bracket() const { return is_bracket() && precedence() == closing_bracket_precedence; }

    UnaryOperator as_unary_operator() const
    {
        VERIFY(is_unary_operator());
        return info().as_unary_operator;
    }

    BinaryOperator as_binary_operator() const
    {
        VERIFY(is_binary_operator());
        return info().as_binary_operator;
    }

    bool matches_with(Token const& bracket)
    {
        VERIFY(is_bracket());
        return info().matching_bracket == bracket.type;
    }

    TokenType type;
    StringView data;
    Location location;
};

}
