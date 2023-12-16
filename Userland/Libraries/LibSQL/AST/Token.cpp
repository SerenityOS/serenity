/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Token.h"
#include <AK/Assertions.h>
#include <AK/ByteString.h>
#include <stdlib.h>

namespace SQL::AST {

StringView Token::name(TokenType type)
{
    switch (type) {
#define __ENUMERATE_SQL_TOKEN(value, type, category) \
    case TokenType::type:                            \
        return #type##sv;
        ENUMERATE_SQL_TOKENS
#undef __ENUMERATE_SQL_TOKEN
    default:
        VERIFY_NOT_REACHED();
    }
}

TokenCategory Token::category(TokenType type)
{
    switch (type) {
#define __ENUMERATE_SQL_TOKEN(value, type, category) \
    case TokenType::type:                            \
        return TokenCategory::category;
        ENUMERATE_SQL_TOKENS
#undef __ENUMERATE_SQL_TOKEN
    default:
        VERIFY_NOT_REACHED();
    }
}

double Token::double_value() const
{
    VERIFY(type() == TokenType::NumericLiteral);
    ByteString value(m_value);

    if (value[0] == '0' && value.length() >= 2) {
        if (value[1] == 'x' || value[1] == 'X')
            return static_cast<double>(strtoul(value.characters() + 2, nullptr, 16));
    }

    return strtod(value.characters(), nullptr);
}

}
