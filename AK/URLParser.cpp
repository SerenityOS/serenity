/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringUtils.h>
#include <AK/URLParser.h>

namespace AK {

static bool is_ascii_hex_digit(u8 ch)
{
    return (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
}

String urldecode(const StringView& input)
{
    size_t cursor = 0;

    auto peek = [&](size_t offset = 0) -> u8 {
        if (cursor + offset >= input.length())
            return 0;
        return input[cursor + offset];
    };

    auto consume = [&] {
        return input[cursor++];
    };

    StringBuilder builder;
    while (cursor < input.length()) {
        if (peek() != '%') {
            builder.append(consume());
            continue;
        }
        if (!is_ascii_hex_digit(peek(1)) || !is_ascii_hex_digit(peek(2))) {
            builder.append(consume());
            continue;
        }
        auto byte_point = StringUtils::convert_to_uint_from_hex(input.substring_view(cursor + 1, 2));
        builder.append(byte_point.value());
        consume();
        consume();
        consume();
    }
    return builder.to_string();
}

static inline bool in_c0_control_set(u32 c)
{
    return c <= 0x1f || c > '~';
}

static inline bool in_fragment_set(u32 c)
{
    return in_c0_control_set(c) || c == ' ' || c == '"' || c == '<' || c == '>' || c == '`';
}

static inline bool in_path_set(u32 c)
{
    return in_fragment_set(c) || c == '#' || c == '?' || c == '{' || c == '}';
}

static inline bool in_userinfo_set(u32 c)
{
    return in_path_set(c) || c == '/' || c == ':' || c == ';' || c == '=' || c == '@' || (c >= '[' && c <= '^') || c == '|';
}

String urlencode(const StringView& input, const StringView& exclude)
{
    StringBuilder builder;
    for (unsigned char ch : input) {
        if (in_userinfo_set((u8)ch) && !exclude.contains(ch)) {
            builder.append('%');
            builder.appendff("{:02X}", ch);
        } else {
            builder.append(ch);
        }
    }
    return builder.to_string();
}

}
