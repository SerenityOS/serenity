/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Token.h"
#include <AK/Assertions.h>
#include <AK/String.h>
#include <stdlib.h>

namespace SQL {

StringView Token::name(TokenType type)
{
    switch (type) {
#define __ENUMERATE_SQL_TOKEN(value, type, category) \
    case TokenType::type:                            \
        return #type;
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
    String value(m_value);

    if (value[0] == '0' && value.length() >= 2) {
        if (value[1] == 'x' || value[1] == 'X')
            return static_cast<double>(strtoul(value.characters() + 2, nullptr, 16));
    }

    return strtod(value.characters(), nullptr);
}

}
