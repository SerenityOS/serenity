/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
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
#include <AK/StringBuilder.h>

namespace JS {

const char* Token::name(TokenType type)
{
    switch (type) {
#define __ENUMERATE_JS_TOKEN(x) \
    case TokenType::x:          \
        return #x;
        ENUMERATE_JS_TOKENS
#undef __ENUMERATE_JS_TOKEN
    default:
        ASSERT_NOT_REACHED();
        return "<Unknown>";
    }
}

const char* Token::name() const
{
    return name(m_type);
}

double Token::double_value() const
{
    ASSERT(type() == TokenType::NumericLiteral);
    // FIXME: need to parse double instead of int
    bool ok;
    return m_value.to_int(ok);
}

String Token::string_value() const
{
    ASSERT(type() == TokenType::StringLiteral);
    StringBuilder builder;
    for (size_t i = 1; i < m_value.length() - 1; ++i) {
        if (m_value[i] == '\\' && i + 1 < m_value.length() - 1) {
            i++;
            switch (m_value[i]) {
            case 'b':
                builder.append('\b');
                break;
            case 'f':
                builder.append('\f');
                break;
            case 'n':
                builder.append('\n');
                break;
            case 'r':
                builder.append('\r');
                break;
            case 't':
                builder.append('\t');
                break;
            case 'v':
                builder.append('\v');
                break;
            case '0':
                builder.append((char)0);
                break;
            case '\'':
                builder.append('\'');
                break;
            case '"':
                builder.append('"');
                break;
            case '\\':
                builder.append('\\');
                break;
            default:
                // FIXME: Also parse octal, hex and unicode sequences
                // should anything else generate a syntax error?
                builder.append(m_value[i]);
            }

        } else {
            builder.append(m_value[i]);
        }
    }
    return builder.to_string();
}

bool Token::bool_value() const
{
    ASSERT(type() == TokenType::BoolLiteral);
    return m_value == "true";
}

}
