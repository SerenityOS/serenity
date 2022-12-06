/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/FloatingPointStringConversions.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <math.h>

namespace AK {

constexpr bool is_space(int ch)
{
    return ch == '\t' || ch == '\n' || ch == '\r' || ch == ' ';
}

ErrorOr<DeprecatedString> JsonParser::consume_and_unescape_string()
{
    if (!consume_specific('"'))
        return Error::from_string_literal("JsonParser: Expected '\"'");
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
                return Error::from_string_literal("JsonParser: Error while parsing string");
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
                return Error::from_string_literal("JsonParser: EOF while parsing Unicode escape");

            auto code_point = AK::StringUtils::convert_to_uint_from_hex(consume(4));
            if (code_point.has_value()) {
                final_sb.append_code_point(code_point.value());
                continue;
            }
            return Error::from_string_literal("JsonParser: Error while parsing Unicode escape");
        }

        return Error::from_string_literal("JsonParser: Error while parsing string");
    }
    if (!consume_specific('"'))
        return Error::from_string_literal("JsonParser: Expected '\"'");

    return final_sb.to_deprecated_string();
}

ErrorOr<JsonValue> JsonParser::parse_object()
{
    JsonObject object;
    if (!consume_specific('{'))
        return Error::from_string_literal("JsonParser: Expected '{'");
    for (;;) {
        ignore_while(is_space);
        if (peek() == '}')
            break;
        ignore_while(is_space);
        auto name = TRY(consume_and_unescape_string());
        if (name.is_null())
            return Error::from_string_literal("JsonParser: Expected object property name");
        ignore_while(is_space);
        if (!consume_specific(':'))
            return Error::from_string_literal("JsonParser: Expected ':'");
        ignore_while(is_space);
        auto value = TRY(parse_helper());
        object.set(name, move(value));
        ignore_while(is_space);
        if (peek() == '}')
            break;
        if (!consume_specific(','))
            return Error::from_string_literal("JsonParser: Expected ','");
        ignore_while(is_space);
        if (peek() == '}')
            return Error::from_string_literal("JsonParser: Unexpected '}'");
    }
    if (!consume_specific('}'))
        return Error::from_string_literal("JsonParser: Expected '}'");
    return JsonValue { move(object) };
}

ErrorOr<JsonValue> JsonParser::parse_array()
{
    JsonArray array;
    if (!consume_specific('['))
        return Error::from_string_literal("JsonParser: Expected '['");
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
            return Error::from_string_literal("JsonParser: Expected ','");
        ignore_while(is_space);
        if (peek() == ']')
            return Error::from_string_literal("JsonParser: Unexpected ']'");
    }
    ignore_while(is_space);
    if (!consume_specific(']'))
        return Error::from_string_literal("JsonParser: Expected ']'");
    return JsonValue { move(array) };
}

ErrorOr<JsonValue> JsonParser::parse_string()
{
    auto string = TRY(consume_and_unescape_string());
    return JsonValue(move(string));
}

ErrorOr<JsonValue> JsonParser::parse_number()
{
    Vector<char, 32> number_buffer;

    auto start_index = tell();

    bool negative = false;
    if (peek() == '-') {
        number_buffer.append('-');
        ++m_index;
        negative = true;

        if (!is_ascii_digit(peek()))
            return Error::from_string_literal("JsonParser: Unexpected '-' without further digits");
    }

    auto fallback_to_double_parse = [&]() -> ErrorOr<JsonValue> {
#ifdef KERNEL
#    error JSONParser is currently not available for the Kernel because it disallows floating point. \
       If you want to make this KERNEL compatible you can just make this fallback_to_double \
       function fail with an error in KERNEL mode.
#endif
        // FIXME: Since we know all the characters so far are ascii digits (and one . or e) we could
        //        use that in the floating point parser.

        // The first part should be just ascii digits
        StringView view = m_input.substring_view(start_index);

        char const* start = view.characters_without_null_termination();
        auto parse_result = parse_first_floating_point(start, start + view.length());

        if (parse_result.parsed_value()) {
            auto characters_parsed = parse_result.end_ptr - start;
            m_index = start_index + characters_parsed;

            return JsonValue(parse_result.value);
        }
        return Error::from_string_literal("JsonParser: Invalid floating point");
    };

    if (peek() == '0') {
        if (is_ascii_digit(peek(1)))
            return Error::from_string_literal("JsonParser: Cannot have leading zeros");

        // Leading zeros are not allowed, however we can have a '.' or 'e' with
        // valid digits after just a zero. These cases will be detected by having the next element
        // start with a '.' or 'e'.
    }

    bool all_zero = true;
    for (;;) {
        char ch = peek();
        if (ch == '.') {
            if (!is_ascii_digit(peek(1)))
                return Error::from_string_literal("JsonParser: Must have digits after decimal point");

            return fallback_to_double_parse();
        }
        if (ch == 'e' || ch == 'E') {
            char next = peek(1);
            if (!is_ascii_digit(next) && ((next != '+' && next != '-') || !is_ascii_digit(peek(2))))
                return Error::from_string_literal("JsonParser: Must have digits after exponent with an optional sign inbetween");

            return fallback_to_double_parse();
        }

        if (is_ascii_digit(ch)) {
            if (ch != '0')
                all_zero = false;

            number_buffer.append(ch);
            ++m_index;
            continue;
        }

        break;
    }

    // Negative zero is always a double
    if (negative && all_zero)
        return JsonValue(-0.0);

    StringView number_string(number_buffer.data(), number_buffer.size());

    auto to_unsigned_result = number_string.to_uint<u64>();
    if (to_unsigned_result.has_value()) {
        if (*to_unsigned_result <= NumericLimits<u32>::max())
            return JsonValue((u32)*to_unsigned_result);

        return JsonValue(*to_unsigned_result);
    } else if (auto signed_number = number_string.to_int<i64>(); signed_number.has_value()) {

        if (*signed_number <= NumericLimits<i32>::max())
            return JsonValue((i32)*signed_number);

        return JsonValue(*signed_number);
    }

    // It's possible the unsigned value is bigger than u64 max
    return fallback_to_double_parse();
}

ErrorOr<JsonValue> JsonParser::parse_true()
{
    if (!consume_specific("true"))
        return Error::from_string_literal("JsonParser: Expected 'true'");
    return JsonValue(true);
}

ErrorOr<JsonValue> JsonParser::parse_false()
{
    if (!consume_specific("false"))
        return Error::from_string_literal("JsonParser: Expected 'false'");
    return JsonValue(false);
}

ErrorOr<JsonValue> JsonParser::parse_null()
{
    if (!consume_specific("null"))
        return Error::from_string_literal("JsonParser: Expected 'null'");
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

    return Error::from_string_literal("JsonParser: Unexpected character");
}

ErrorOr<JsonValue> JsonParser::parse()
{
    auto result = TRY(parse_helper());
    ignore_while(is_space);
    if (!is_eof())
        return Error::from_string_literal("JsonParser: Didn't consume all input");
    return result;
}

}
