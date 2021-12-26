/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/Memory.h>

namespace AK {

static inline bool is_whitespace(char ch)
{
    return ch == ' ' || ch == '\n' || ch == '\t' || ch == '\v' || ch == '\r';
}

char JsonParser::peek() const
{
    if (m_index < m_input.length())
        return m_input[m_index];
    return '\0';
}

char JsonParser::consume()
{
    if (m_index < m_input.length())
        return m_input[m_index++];
    return '\0';
}

template<typename C>
void JsonParser::consume_while(C condition)
{
    while (condition(peek()))
        consume();
}

void JsonParser::consume_whitespace()
{
    consume_while([](char ch) { return is_whitespace(ch); });
}

void JsonParser::consume_specific(char expected_ch)
{
    char consumed_ch = consume();
    ASSERT(consumed_ch == expected_ch);
}

String JsonParser::consume_quoted_string()
{
    consume_specific('"');
    Vector<char, 1024> buffer;

    for (;;) {
        size_t peek_index = m_index;
        char ch = 0;
        for (;;) {
            if (peek_index == m_input.length())
                break;
            ch = m_input[peek_index];
            if (ch == '"' || ch == '\\')
                break;
            ++peek_index;
        }

        if (peek_index != m_index) {
            buffer.append(m_input.characters_without_null_termination() + m_index, peek_index - m_index);
            m_index = peek_index;
        }

        if (m_index == m_input.length())
            break;
        if (ch == '"')
            break;
        if (ch != '\\') {
            buffer.append(consume());
            continue;
        }
        consume();
        char escaped_ch = consume();
        switch (escaped_ch) {
        case 'n':
            buffer.append('\n');
            break;
        case 'r':
            buffer.append('\r');
            break;
        case 't':
            buffer.append('\t');
            break;
        case 'b':
            buffer.append('\b');
            break;
        case 'f':
            buffer.append('\f');
            break;
        case 'u': {
            StringBuilder sb;
            sb.append(consume());
            sb.append(consume());
            sb.append(consume());
            sb.append(consume());

            bool ok;
            u32 codepoint = AK::StringUtils::convert_to_uint_from_hex(sb.to_string(), ok);
            if (ok && codepoint < 128) {
                buffer.append((char)codepoint);
            } else {
                // FIXME: This is obviously not correct, but we don't have non-ASCII support so meh.
                buffer.append('?');
            }
        } break;
        default:
            buffer.append(escaped_ch);
            break;
        }
    }
    consume_specific('"');

    if (buffer.is_empty())
        return String::empty();

    auto& last_string_starting_with_character = m_last_string_starting_with_character[(u8)buffer.first()];
    if (last_string_starting_with_character.length() == buffer.size()) {
        if (!memcmp(last_string_starting_with_character.characters(), buffer.data(), buffer.size()))
            return last_string_starting_with_character;
    }

    last_string_starting_with_character = String::copy(buffer);
    return last_string_starting_with_character;
}

JsonObject JsonParser::parse_object()
{
    JsonObject object;
    consume_specific('{');
    for (;;) {
        consume_whitespace();
        if (peek() == '}')
            break;
        consume_whitespace();
        auto name = consume_quoted_string();
        consume_whitespace();
        consume_specific(':');
        consume_whitespace();
        auto value = parse();
        object.set(name, move(value));
        consume_whitespace();
        if (peek() == '}')
            break;
        consume_specific(',');
    }
    consume_specific('}');
    return object;
}

JsonArray JsonParser::parse_array()
{
    JsonArray array;
    consume_specific('[');
    for (;;) {
        consume_whitespace();
        if (peek() == ']')
            break;
        array.append(parse());
        consume_whitespace();
        if (peek() == ']')
            break;
        consume_specific(',');
    }
    consume_whitespace();
    consume_specific(']');
    return array;
}

JsonValue JsonParser::parse_string()
{
    return consume_quoted_string();
}

JsonValue JsonParser::parse_number()
{
    bool ok;
    JsonValue value;
    Vector<char, 128> number_buffer;
    Vector<char, 128> fraction_buffer;

    bool is_double = false;
    for (;;) {
        char ch = peek();
        if (ch == '.') {
            is_double = true;
            ++m_index;
            continue;
        }
        if (ch == '-' || (ch >= '0' && ch <= '9')) {
            if (is_double)
                fraction_buffer.append(ch);
            else
                number_buffer.append(ch);
            ++m_index;
            continue;
        }
        break;
    }

    StringView number_string(number_buffer.data(), number_buffer.size());
    StringView fraction_string(fraction_buffer.data(), fraction_buffer.size());

#ifndef KERNEL
    if (is_double) {
        int whole = number_string.to_uint(ok);
        if (!ok)
            whole = number_string.to_int(ok);
        ASSERT(ok);

        int fraction = fraction_string.to_uint(ok);
        fraction *= (whole < 0) ? -1 : 1;
        ASSERT(ok);

        auto divider = 1;
        for (size_t i = 0; i < fraction_buffer.size(); ++i) {
            divider *= 10;
        }
        value = JsonValue((double)whole + ((double)fraction / divider));
    } else {
#endif
        value = JsonValue(number_string.to_uint(ok));
        if (!ok)
            value = JsonValue(number_string.to_int(ok));
        ASSERT(ok);
#ifndef KERNEL
    }
#endif

    return value;
}

void JsonParser::consume_string(const char* str)
{
    for (size_t i = 0, length = strlen(str); i < length; ++i)
        consume_specific(str[i]);
}

JsonValue JsonParser::parse_true()
{
    consume_string("true");
    return JsonValue(true);
}

JsonValue JsonParser::parse_false()
{
    consume_string("false");
    return JsonValue(false);
}

JsonValue JsonParser::parse_null()
{
    consume_string("null");
    return JsonValue(JsonValue::Type::Null);
}

JsonValue JsonParser::parse_undefined()
{
    consume_string("undefined");
    return JsonValue(JsonValue::Type::Undefined);
}

JsonValue JsonParser::parse()
{
    consume_whitespace();
    auto type_hint = peek();
    switch (type_hint) {
    case '{':
        return parse_object();
    case '[':
        return parse_array();
    case '"':
        return parse_string();
    case '-':
    case '0':
    case '1':
    case '2':
    case '3':
    case '4':
    case '5':
    case '6':
    case '7':
    case '8':
    case '9':
        return parse_number();
    case 'f':
        return parse_false();
    case 't':
        return parse_true();
    case 'n':
        return parse_null();
    case 'u':
        return parse_undefined();
    }

    return JsonValue();
}
}
