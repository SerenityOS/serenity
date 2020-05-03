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
#include <ctype.h>

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
    String value_string(m_value);
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
        } else if (isdigit(value_string[1])) {
            // also octal, but syntax error in strict mode
            return static_cast<double>(strtoul(value_string.characters() + 1, nullptr, 8));
        }
    }
    return strtod(value_string.characters(), nullptr);
}

String Token::string_value() const
{
    ASSERT(type() == TokenType::StringLiteral || type() == TokenType::TemplateLiteralString);
    auto is_template = type() == TokenType::TemplateLiteralString;

    auto offset = type() == TokenType::TemplateLiteralString ? 0 : 1;

    StringBuilder builder;
    for (size_t i = offset; i < m_value.length() - offset; ++i) {
        if (m_value[i] == '\\' && i + 1 < m_value.length() - offset) {
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
                if (is_template && (m_value[i] == '$' || m_value[i] == '`')) {
                    builder.append(m_value[i]);
                } else {
                    // FIXME: Also parse octal, hex and unicode sequences
                    // should anything else generate a syntax error?
                    builder.append(m_value[i]);
                }
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

bool Token::is_identifier_name() const
{
    // IdentifierNames are Identifiers + ReservedWords
    // The standard defines this reversed: Identifiers are IdentifierNames except reserved words
    // https://www.ecma-international.org/ecma-262/5.1/#sec-7.6
    return m_type == TokenType::Identifier
        || m_type == TokenType::Await
        || m_type == TokenType::BoolLiteral
        || m_type == TokenType::Break
        || m_type == TokenType::Case
        || m_type == TokenType::Catch
        || m_type == TokenType::Class
        || m_type == TokenType::Const
        || m_type == TokenType::Continue
        || m_type == TokenType::Default
        || m_type == TokenType::Delete
        || m_type == TokenType::Do
        || m_type == TokenType::Else
        || m_type == TokenType::Finally
        || m_type == TokenType::For
        || m_type == TokenType::Function
        || m_type == TokenType::If
        || m_type == TokenType::In
        || m_type == TokenType::Instanceof
        || m_type == TokenType::Interface
        || m_type == TokenType::Let
        || m_type == TokenType::New
        || m_type == TokenType::NullLiteral
        || m_type == TokenType::Return
        || m_type == TokenType::Switch
        || m_type == TokenType::This
        || m_type == TokenType::Throw
        || m_type == TokenType::Try
        || m_type == TokenType::Typeof
        || m_type == TokenType::Var
        || m_type == TokenType::Void
        || m_type == TokenType::While
        || m_type == TokenType::Yield;
}

}
