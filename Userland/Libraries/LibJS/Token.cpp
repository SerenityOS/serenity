/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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
#include <AK/GenericLexer.h>
#include <AK/StringBuilder.h>
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
        VERIFY_NOT_REACHED();
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
            if (!m_value.contains('8') && !m_value.contains('9'))
                return static_cast<double>(strtoul(value_string.characters() + 1, nullptr, 8));
        }
    }
    return strtod(value_string.characters(), nullptr);
}

static u32 hex2int(char x)
{
    VERIFY(isxdigit(x));
    if (x >= '0' && x <= '9')
        return x - '0';
    return 10u + (tolower(x) - 'a');
}

String Token::string_value(StringValueStatus& status) const
{
    VERIFY(type() == TokenType::StringLiteral || type() == TokenType::TemplateLiteralString);

    auto is_template = type() == TokenType::TemplateLiteralString;
    GenericLexer lexer(is_template ? m_value : m_value.substring_view(1, m_value.length() - 2));

    auto encoding_failure = [&status](StringValueStatus parse_status) -> String {
        status = parse_status;
        return {};
    };

    StringBuilder builder;
    while (!lexer.is_eof()) {
        // No escape, consume one char and continue
        if (!lexer.next_is('\\')) {
            builder.append(lexer.consume());
            continue;
        }

        lexer.ignore();
        VERIFY(!lexer.is_eof());

        // Line continuation
        if (lexer.next_is('\n') || lexer.next_is('\r')) {
            lexer.ignore();
            continue;
        }
        // Line continuation
        if (lexer.next_is(LINE_SEPARATOR) || lexer.next_is(PARAGRAPH_SEPARATOR)) {
            lexer.ignore(3);
            continue;
        }
        // Null-byte escape
        if (lexer.next_is('0') && !isdigit(lexer.peek(1))) {
            lexer.ignore();
            builder.append('\0');
            continue;
        }
        // Hex escape
        if (lexer.next_is('x')) {
            lexer.ignore();
            if (!isxdigit(lexer.peek()) || !isxdigit(lexer.peek(1)))
                return encoding_failure(StringValueStatus::MalformedHexEscape);
            auto code_point = hex2int(lexer.consume()) * 16 + hex2int(lexer.consume());
            VERIFY(code_point <= 255);
            builder.append_code_point(code_point);
            continue;
        }
        // Unicode escape
        if (lexer.next_is('u')) {
            lexer.ignore();
            u32 code_point = 0;
            if (lexer.next_is('{')) {
                lexer.ignore();
                while (true) {
                    if (!lexer.next_is(isxdigit))
                        return encoding_failure(StringValueStatus::MalformedUnicodeEscape);
                    auto new_code_point = (code_point << 4u) | hex2int(lexer.consume());
                    if (new_code_point < code_point)
                        return encoding_failure(StringValueStatus::UnicodeEscapeOverflow);
                    code_point = new_code_point;
                    if (lexer.next_is('}'))
                        break;
                }
                lexer.ignore();
            } else {
                for (int j = 0; j < 4; ++j) {
                    if (!lexer.next_is(isxdigit))
                        return encoding_failure(StringValueStatus::MalformedUnicodeEscape);
                    code_point = (code_point << 4u) | hex2int(lexer.consume());
                }
            }
            builder.append_code_point(code_point);
            continue;
        }

        // In non-strict mode LegacyOctalEscapeSequence is allowed in strings:
        // https://tc39.es/ecma262/#sec-additional-syntax-string-literals
        String octal_str;

        auto is_octal_digit = [](char ch) { return ch >= '0' && ch <= '7'; };
        auto is_zero_to_three = [](char ch) { return ch >= '0' && ch <= '3'; };
        auto is_four_to_seven = [](char ch) { return ch >= '4' && ch <= '7'; };

        // OctalDigit [lookahead ∉ OctalDigit]
        if (is_octal_digit(lexer.peek()) && !is_octal_digit(lexer.peek(1)))
            octal_str = lexer.consume(1);
        // ZeroToThree OctalDigit [lookahead ∉ OctalDigit]
        else if (is_zero_to_three(lexer.peek()) && is_octal_digit(lexer.peek(1)) && !is_octal_digit(lexer.peek(2)))
            octal_str = lexer.consume(2);
        // FourToSeven OctalDigit
        else if (is_four_to_seven(lexer.peek()) && is_octal_digit(lexer.peek(1)))
            octal_str = lexer.consume(2);
        // ZeroToThree OctalDigit OctalDigit
        else if (is_zero_to_three(lexer.peek()) && is_octal_digit(lexer.peek(1)) && is_octal_digit(lexer.peek(2)))
            octal_str = lexer.consume(3);

        if (!octal_str.is_null()) {
            status = StringValueStatus::LegacyOctalEscapeSequence;
            auto code_point = strtoul(octal_str.characters(), nullptr, 8);
            VERIFY(code_point <= 255);
            builder.append_code_point(code_point);
            continue;
        }

        lexer.retreat();
        builder.append(lexer.consume_escaped_character('\\', "b\bf\fn\nr\rt\tv\v"));
    }
    return builder.to_string();
}

bool Token::bool_value() const
{
    VERIFY(type() == TokenType::BoolLiteral);
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

bool Token::trivia_contains_line_terminator() const
{
    return m_trivia.contains('\n') || m_trivia.contains('\r') || m_trivia.contains(LINE_SEPARATOR) || m_trivia.contains(PARAGRAPH_SEPARATOR);
}

}
