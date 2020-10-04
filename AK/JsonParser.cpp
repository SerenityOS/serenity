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
#include <ctype.h>

namespace AK {

String JsonParser::consume_and_unescape_string()
{
    if (!consume_specific('"'))
        return {};
    StringBuilder final_sb;

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

        while (peek_index != m_index) {
            final_sb.append(m_input[m_index]);
            m_index++;
        }

        if (m_index == m_input.length())
            break;
        if (ch == '"')
            break;
        if (ch != '\\') {
            final_sb.append(consume());
            continue;
        }
        ignore();
        char escaped_ch = consume();
        switch (escaped_ch) {
        case 'n':
            final_sb.append('\n');
            break;
        case 'r':
            final_sb.append('\r');
            break;
        case 't':
            final_sb.append('\t');
            break;
        case 'b':
            final_sb.append('\b');
            break;
        case 'f':
            final_sb.append('\f');
            break;
        case 'u': {
            auto code_point = AK::StringUtils::convert_to_uint_from_hex(consume(4));
            if (code_point.has_value())
                final_sb.append_code_point(code_point.value());
            else
                final_sb.append('?');
        } break;
        default:
            final_sb.append(escaped_ch);
            break;
        }
    }
    if (!consume_specific('"'))
        return {};

    return final_sb.to_string();
}

Optional<JsonValue> JsonParser::parse_object()
{
    JsonObject object;
    if (!consume_specific('{'))
        return {};
    for (;;) {
        ignore_while(isspace);
        if (peek() == '}')
            break;
        ignore_while(isspace);
        auto name = consume_and_unescape_string();
        if (name.is_null())
            return {};
        ignore_while(isspace);
        if (!consume_specific(':'))
            return {};
        ignore_while(isspace);
        auto value = parse_helper();
        if (!value.has_value())
            return {};
        object.set(name, move(value.value()));
        ignore_while(isspace);
        if (peek() == '}')
            break;
        if (!consume_specific(','))
            return {};
        ignore_while(isspace);
        if (peek() == '}')
            return {};
    }
    if (!consume_specific('}'))
        return {};
    return object;
}

Optional<JsonValue> JsonParser::parse_array()
{
    JsonArray array;
    if (!consume_specific('['))
        return {};
    for (;;) {
        ignore_while(isspace);
        if (peek() == ']')
            break;
        auto element = parse_helper();
        if (!element.has_value())
            return {};
        array.append(element.value());
        ignore_while(isspace);
        if (peek() == ']')
            break;
        if (!consume_specific(','))
            return {};
        ignore_while(isspace);
        if (peek() == ']')
            return {};
    }
    ignore_while(isspace);
    if (!consume_specific(']'))
        return {};
    return array;
}

Optional<JsonValue> JsonParser::parse_string()
{
    auto result = consume_and_unescape_string();
    if (result.is_null())
        return {};
    return JsonValue(result);
}

Optional<JsonValue> JsonParser::parse_number()
{
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
        // FIXME: This logic looks shaky.
        int whole = 0;
        auto to_signed_result = number_string.to_uint();
        if (to_signed_result.has_value()) {
            whole = to_signed_result.value();
        } else {
            auto number = number_string.to_int();
            if (!number.has_value())
                return {};
            whole = number.value();
        }

        auto fraction_string_uint = fraction_string.to_uint();
        if (!fraction_string_uint.has_value())
            return {};
        int fraction = fraction_string_uint.value();
        fraction *= (whole < 0) ? -1 : 1;

        auto divider = 1;
        for (size_t i = 0; i < fraction_buffer.size(); ++i) {
            divider *= 10;
        }
        value = JsonValue((double)whole + ((double)fraction / divider));
    } else {
#endif
        auto to_unsigned_result = number_string.to_uint();
        if (to_unsigned_result.has_value()) {
            value = JsonValue(to_unsigned_result.value());
        } else {
            auto number = number_string.to_int();
            if (!number.has_value())
                return {};
            value = JsonValue(number.value());
        }
#ifndef KERNEL
    }
#endif

    return value;
}

Optional<JsonValue> JsonParser::parse_true()
{
    if (!consume_specific("true"))
        return {};
    return JsonValue(true);
}

Optional<JsonValue> JsonParser::parse_false()
{
    if (!consume_specific("false"))
        return {};
    return JsonValue(false);
}

Optional<JsonValue> JsonParser::parse_null()
{
    if (!consume_specific("null"))
        return {};
    return JsonValue(JsonValue::Type::Null);
}

Optional<JsonValue> JsonParser::parse_helper()
{
    ignore_while(isspace);
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
    }

    return {};
}

Optional<JsonValue> JsonParser::parse()
{
    auto result = parse_helper();
    if (!result.has_value())
        return {};
    ignore_while(isspace);
    if (!is_eof())
        return {};
    return result;
}

}
