/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>

namespace AK {

constexpr bool is_space(int ch)
{
    return ch == '\t' || ch == '\n' || ch == '\r' || ch == ' ';
}

ErrorOr<String> JsonParser::consume_and_unescape_string()
{
    if (!consume_specific('"'))
        return Error::from_string_literal("JsonParser: Expected '\"'"sv);
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
            if (is_ascii_c0_control(ch))
                return Error::from_string_literal("JsonParser: Error while parsing string"sv);
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
        if (next_is('"')) {
            ignore();
            final_sb.append('"');
            continue;
        }

        if (next_is('\\')) {
            ignore();
            final_sb.append('\\');
            continue;
        }

        if (next_is('/')) {
            ignore();
            final_sb.append('/');
            continue;
        }

        if (next_is('n')) {
            ignore();
            final_sb.append('\n');
            continue;
        }

        if (next_is('r')) {
            ignore();
            final_sb.append('\r');
            continue;
        }

        if (next_is('t')) {
            ignore();
            final_sb.append('\t');
            continue;
        }

        if (next_is('b')) {
            ignore();
            final_sb.append('\b');
            continue;
        }

        if (next_is('f')) {
            ignore();
            final_sb.append('\f');
            continue;
        }

        if (next_is('u')) {
            ignore();
            if (tell_remaining() < 4)
                return Error::from_string_literal("JsonParser: EOF while parsing Unicode escape"sv);

            auto code_point = AK::StringUtils::convert_to_uint_from_hex(consume(4));
            if (code_point.has_value()) {
                final_sb.append_code_point(code_point.value());
                continue;
            }
            return Error::from_string_literal("JsonParser: Error while parsing Unicode escape"sv);
        }

        return Error::from_string_literal("JsonParser: Error while parsing string"sv);
    }
    if (!consume_specific('"'))
        return Error::from_string_literal("JsonParser: Expected '\"'"sv);

    return final_sb.to_string();
}

ErrorOr<JsonValue> JsonParser::parse_object()
{
    JsonObject object;
    if (!consume_specific('{'))
        return Error::from_string_literal("JsonParser: Expected '{'"sv);
    for (;;) {
        ignore_while(is_space);
        if (peek() == '}')
            break;
        ignore_while(is_space);
        auto name = TRY(consume_and_unescape_string());
        if (name.is_null())
            return Error::from_string_literal("JsonParser: Expected object property name"sv);
        ignore_while(is_space);
        if (!consume_specific(':'))
            return Error::from_string_literal("JsonParser: Expected ':'"sv);
        ignore_while(is_space);
        auto value = TRY(parse_helper());
        object.set(name, move(value));
        ignore_while(is_space);
        if (peek() == '}')
            break;
        if (!consume_specific(','))
            return Error::from_string_literal("JsonParser: Expected ','"sv);
        ignore_while(is_space);
        if (peek() == '}')
            return Error::from_string_literal("JsonParser: Unexpected '}'"sv);
    }
    if (!consume_specific('}'))
        return Error::from_string_literal("JsonParser: Expected '}'"sv);
    return JsonValue { move(object) };
}

ErrorOr<JsonValue> JsonParser::parse_array()
{
    JsonArray array;
    if (!consume_specific('['))
        return Error::from_string_literal("JsonParser: Expected '['"sv);
    for (;;) {
        ignore_while(is_space);
        if (peek() == ']')
            break;
        auto element = TRY(parse_helper());
        array.append(move(element));
        ignore_while(is_space);
        if (peek() == ']')
            break;
        if (!consume_specific(','))
            return Error::from_string_literal("JsonParser: Expected ','"sv);
        ignore_while(is_space);
        if (peek() == ']')
            return Error::from_string_literal("JsonParser: Unexpected ']'"sv);
    }
    ignore_while(is_space);
    if (!consume_specific(']'))
        return Error::from_string_literal("JsonParser: Expected ']'"sv);
    return JsonValue { move(array) };
}

ErrorOr<JsonValue> JsonParser::parse_string()
{
    auto string = TRY(consume_and_unescape_string());
    return JsonValue(move(string));
}

ErrorOr<JsonValue> JsonParser::parse_number()
{
    JsonValue value;
    Vector<char, 128> number_buffer;
    Vector<char, 128> fraction_buffer;

    bool is_double = false;
    bool all_zero = true;
    for (;;) {
        char ch = peek();
        if (ch == '.') {
            if (is_double)
                return Error::from_string_literal("JsonParser: Multiple '.' in number"sv);

            is_double = true;
            ++m_index;
            continue;
        }
        if (ch == '-' || (ch >= '0' && ch <= '9')) {
            if (ch != '-' && ch != '0')
                all_zero = false;

            if (is_double) {
                if (ch == '-')
                    return Error::from_string_literal("JsonParser: Error while parsing number"sv);

                fraction_buffer.append(ch);
            } else {
                if (number_buffer.size() > 0) {
                    if (number_buffer.at(0) == '0')
                        return Error::from_string_literal("JsonParser: Error while parsing number"sv);
                }

                if (number_buffer.size() > 1) {
                    if (number_buffer.at(0) == '-' && number_buffer.at(1) == '0')
                        return Error::from_string_literal("JsonParser: Error while parsing number"sv);
                }

                number_buffer.append(ch);
            }
            ++m_index;
            continue;
        }
        break;
    }

    StringView number_string(number_buffer.data(), number_buffer.size());

#ifndef KERNEL
    // Check for negative zero which needs to be forced to be represented with a double
    if (number_string.starts_with('-') && all_zero)
        return JsonValue(-0.0);

    if (is_double) {
        // FIXME: This logic looks shaky.
        int whole = 0;
        auto to_signed_result = number_string.to_uint();
        if (to_signed_result.has_value()) {
            whole = to_signed_result.value();
        } else {
            auto number = number_string.to_int();
            if (!number.has_value())
                return Error::from_string_literal("JsonParser: Error while parsing number"sv);
            whole = number.value();
        }

        StringView fraction_string(fraction_buffer.data(), fraction_buffer.size());
        auto fraction_string_uint = fraction_string.to_uint();
        if (!fraction_string_uint.has_value())
            return Error::from_string_literal("JsonParser: Error while parsing number"sv);
        int fraction = fraction_string_uint.value();
        fraction *= (whole < 0) ? -1 : 1;

        auto divider = 1;
        for (size_t i = 0; i < fraction_buffer.size(); ++i) {
            divider *= 10;
        }
        value = JsonValue((double)whole + ((double)fraction / divider));
    } else {
#endif
        auto to_unsigned_result = number_string.to_uint<u64>();
        if (to_unsigned_result.has_value()) {
            auto number = *to_unsigned_result;
            if (number <= NumericLimits<u32>::max())
                value = JsonValue((u32)number);
            else
                value = JsonValue(number);
        } else {
            auto number = number_string.to_int<i64>();
            if (!number.has_value())
                return Error::from_string_literal("JsonParser: Error while parsing number"sv);
            if (number.value() <= NumericLimits<i32>::max()) {
                value = JsonValue((i32)number.value());
            } else {
                value = JsonValue(number.value());
            }
        }
#ifndef KERNEL
    }
#endif

    return value;
}

ErrorOr<JsonValue> JsonParser::parse_true()
{
    if (!consume_specific("true"))
        return Error::from_string_literal("JsonParser: Expected 'true'"sv);
    return JsonValue(true);
}

ErrorOr<JsonValue> JsonParser::parse_false()
{
    if (!consume_specific("false"))
        return Error::from_string_literal("JsonParser: Expected 'false'"sv);
    return JsonValue(false);
}

ErrorOr<JsonValue> JsonParser::parse_null()
{
    if (!consume_specific("null"))
        return Error::from_string_literal("JsonParser: Expected 'null'"sv);
    return JsonValue(JsonValue::Type::Null);
}

ErrorOr<JsonValue> JsonParser::parse_helper()
{
    ignore_while(is_space);
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

    return Error::from_string_literal("JsonParser: Unexpected character"sv);
}

ErrorOr<JsonValue> JsonParser::parse()
{
    auto result = TRY(parse_helper());
    ignore_while(is_space);
    if (!is_eof())
        return Error::from_string_literal("JsonParser: Didn't consume all input"sv);
    return result;
}

}
