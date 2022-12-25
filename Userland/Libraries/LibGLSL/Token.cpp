/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Melody Goad <mszoopers@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Token.h"
#include <AK/Assertions.h>
#include <AK/CharacterTypes.h>
#include <AK/FloatingPointStringConversions.h>
#include <AK/GenericLexer.h>
#include <AK/StringBuilder.h>

namespace GLSL {

char const* Token::name(TokenType type)
{
    switch (type) {
#define __ENUMERATE_GLSL_TOKEN(type, category) \
    case TokenType::type:                      \
        return #type;
        ENUMERATE_GLSL_TOKENS
#undef __ENUMERATE_GLSL_TOKEN
    default:
        VERIFY_NOT_REACHED();
        return "<Unknown>";
    }
}

char const* Token::name() const
{
    return name(m_type);
}

TokenCategory Token::category(TokenType type)
{
    switch (type) {
#define __ENUMERATE_GLSL_TOKEN(type, category) \
    case TokenType::type:                      \
        return TokenCategory::category;
        ENUMERATE_GLSL_TOKENS
#undef __ENUMERATE_GLSL_TOKEN
    default:
        VERIFY_NOT_REACHED();
    }
}

TokenCategory Token::category() const
{
    return category(m_type);
}

double Token::double_value() const
{
    VERIFY(type() == TokenType::NumericLiteral);

    StringBuilder builder;

    for (auto ch : value()) {
        if (ch == '_')
            continue;
        builder.append(ch);
    }

    DeprecatedString value_string = builder.to_deprecated_string();
    if (value_string[0] == '0' && value_string.length() >= 2) {
        if (value_string[1] == 'x' || value_string[1] == 'X') {
            // hexadecimal
            return static_cast<double>(strtoul(value_string.characters() + 2, nullptr, 16));
        } else if (value_string[1] == 'o' || value_string[1] == 'O') {
            // octal
            return static_cast<double>(strtoul(value_string.characters() + 2, nullptr, 8));
        } else if (value_string[1] == 'b' || value_string[1] == 'B') {
            // binary
            return static_cast<double>(strtoul(value_string.characters() + 2, nullptr, 2));
        } else if (is_ascii_digit(value_string[1])) {
            // also octal, but syntax error in strict mode
            if (!value().contains('8') && !value().contains('9'))
                return static_cast<double>(strtoul(value_string.characters() + 1, nullptr, 8));
        }
    }
    // This should always be a valid double
    return value_string.to_double().release_value();
}

bool Token::bool_value() const
{
    VERIFY(type() == TokenType::BoolLiteral);
    return value() == "true";
}

bool Token::is_identifier_name() const
{
    return m_type == TokenType::Identifier;
}

bool Token::trivia_contains_line_terminator() const
{
    return m_trivia.contains('\n') || m_trivia.contains('\r');
}

}
