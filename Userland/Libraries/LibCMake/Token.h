/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibCMake/Position.h>

namespace CMake {

struct VariableReference {
    StringView value;
    Position start;
    Position end;
};

enum class ControlKeywordType {
    If,
    ElseIf,
    Else,
    EndIf,
    ForEach,
    EndForEach,
    While,
    EndWhile,
    Break,
    Continue,
    Return,
    Macro,
    EndMacro,
    Function,
    EndFunction,
    Block,
    EndBlock,
};

struct Token {
    enum class Type {
        BracketComment,
        LineComment,
        Identifier,
        ControlKeyword,
        OpenParen,
        CloseParen,
        BracketArgument,
        QuotedArgument,
        UnquotedArgument,
        Garbage,

        // These are elements inside argument tokens
        VariableReference,
    };

    Type type;
    StringView value;

    Position start;
    Position end;

    // Type-specific
    Optional<ControlKeywordType> control_keyword {};
    Vector<VariableReference> variable_references {};
};

static constexpr StringView to_string(Token::Type type)
{
    switch (type) {
    case Token::Type::BracketComment:
        return "BracketComment"sv;
    case Token::Type::LineComment:
        return "LineComment"sv;
    case Token::Type::Identifier:
        return "Identifier"sv;
    case Token::Type::ControlKeyword:
        return "ControlKeyword"sv;
    case Token::Type::OpenParen:
        return "OpenParen"sv;
    case Token::Type::CloseParen:
        return "CloseParen"sv;
    case Token::Type::BracketArgument:
        return "BracketArgument"sv;
    case Token::Type::QuotedArgument:
        return "QuotedArgument"sv;
    case Token::Type::UnquotedArgument:
        return "UnquotedArgument"sv;
    case Token::Type::Garbage:
        return "Garbage"sv;
    case Token::Type::VariableReference:
        return "VariableReference"sv;
    }

    VERIFY_NOT_REACHED();
}

Optional<ControlKeywordType> control_keyword_from_string(StringView value);

}
