/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCMake/Position.h>

namespace CMake::Cache {

struct Token {
    enum class Type {
        Comment,
        HelpText,
        Key,
        Colon,
        Type,
        Equals,
        Value,
        Garbage,
    };
    Type type;
    StringView value;

    Position start;
    Position end;
};

static constexpr StringView to_string(Token::Type type)
{
    switch (type) {
    case Token::Type::Comment:
        return "Comment"sv;
    case Token::Type::HelpText:
        return "HelpText"sv;
    case Token::Type::Key:
        return "Key"sv;
    case Token::Type::Colon:
        return "Colon"sv;
    case Token::Type::Type:
        return "Type"sv;
    case Token::Type::Equals:
        return "Equals"sv;
    case Token::Type::Value:
        return "Value"sv;
    case Token::Type::Garbage:
        return "Garbage"sv;
    }

    VERIFY_NOT_REACHED();
}

}
