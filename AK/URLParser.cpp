/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
        if (!is_ascii_hex_digit(peek(1)) && !is_ascii_hex_digit(peek(2))) {
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

String urlencode(const StringView& input)
{
    StringBuilder builder;
    for (char ch : input) {
        if (in_userinfo_set((u8)ch)) {
            builder.append('%');
            builder.appendff("{:02X}", ch);
        } else {
            builder.append(ch);
        }
    }
    return builder.to_string();
}

}
