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
#include <AK/Utf32View.h>
#include <ctype.h>

namespace JS {

const char* Token::name(TokenType type)
{
    switch (type) {
#define __ENUMERATE_JS_TOKEN(type, category) \
    case TokenType::type:                    \
        return #type;
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

TokenCategory Token::category(TokenType type)
{
    switch (type) {
#define __ENUMERATE_JS_TOKEN(type, category) \
    case TokenType::type:                    \
        return TokenCategory::category;
        ENUMERATE_JS_TOKENS
#undef __ENUMERATE_JS_TOKEN
    default:
        ASSERT_NOT_REACHED();
    }
}

TokenCategory Token::category() const
{
    return category(m_type);
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

static u32 hex2int(char x)
{
    ASSERT(isxdigit(x));
    if (x >= '0' && x <= '9')
        return x - '0';
    return 10u + (tolower(x) - 'a');
}

String Token::string_value(StringValueStatus& status) const
{
    ASSERT(type() == TokenType::StringLiteral || type() == TokenType::TemplateLiteralString);
    auto is_template = type() == TokenType::TemplateLiteralString;

    auto offset = type() == TokenType::TemplateLiteralString ? 0 : 1;

    auto encoding_failure = [&status](StringValueStatus parse_status) -> String {
        status = parse_status;
        return {};
    };

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
            case 'x': {
                if (i + 2 >= m_value.length() - offset)
                    return encoding_failure(StringValueStatus::MalformedHexEscape);

                auto digit1 = m_value[++i];
                auto digit2 = m_value[++i];
                if (!isxdigit(digit1) || !isxdigit(digit2))
                    return encoding_failure(StringValueStatus::MalformedHexEscape);
                builder.append_code_point(hex2int(digit1) * 16 + hex2int(digit2));
                break;
            }
            case 'u': {
                if (i + 1 >= m_value.length() - offset)
                    return encoding_failure(StringValueStatus::MalformedUnicodeEscape);
                u32 code_point = m_value[++i];

                if (code_point == '{') {
                    code_point = 0;
                    while (true) {
                        if (i + 1 >= m_value.length() - offset)
                            return encoding_failure(StringValueStatus::MalformedUnicodeEscape);

                        auto ch = m_value[++i];
                        if (ch == '}')
                            break;
                        if (!isxdigit(ch))
                            return encoding_failure(StringValueStatus::MalformedUnicodeEscape);

                        auto new_code_point = (code_point << 4u) | hex2int(ch);
                        if (new_code_point < code_point)
                            return encoding_failure(StringValueStatus::UnicodeEscapeOverflow);
                        code_point = new_code_point;
                    }
                } else {
                    if (i + 3 >= m_value.length() - offset || !isxdigit(code_point))
                        return encoding_failure(StringValueStatus::MalformedUnicodeEscape);

                    code_point = hex2int(code_point);
                    for (int j = 0; j < 3; ++j) {
                        auto ch = m_value[++i];
                        if (!isxdigit(ch))
                            return encoding_failure(StringValueStatus::MalformedUnicodeEscape);
                        code_point = (code_point << 4u) | hex2int(ch);
                    }
                }

                builder.append_code_point(code_point);
                break;
            }
            default:
                if (is_template && (m_value[i] == '$' || m_value[i] == '`')) {
                    builder.append(m_value[i]);
                    break;
                }

                // FIXME: Also parse octal. Should anything else generate a syntax error?
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
        || m_type == TokenType::Enum
        || m_type == TokenType::Export
        || m_type == TokenType::Extends
        || m_type == TokenType::Finally
        || m_type == TokenType::For
        || m_type == TokenType::Function
        || m_type == TokenType::If
        || m_type == TokenType::Import
        || m_type == TokenType::In
        || m_type == TokenType::Instanceof
        || m_type == TokenType::Interface
        || m_type == TokenType::Let
        || m_type == TokenType::New
        || m_type == TokenType::NullLiteral
        || m_type == TokenType::Return
        || m_type == TokenType::Super
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
