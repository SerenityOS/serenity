/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CharacterTypes.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>

namespace Shell {

enum class SpecialCharacterEscapeMode {
    Untouched,
    Escaped,
    QuotedAsEscape,
    QuotedAsHex,
};

String escape_token_for_single_quotes(StringView token);
String escape_token_for_double_quotes(StringView token);
SpecialCharacterEscapeMode special_character_escape_mode(u32 code_point);
String escape_token(StringView token);
String unescape_token(StringView token);

inline String escape_token_for_single_quotes(const StringView token)
{
    // `foo bar \n '` -> `'foo bar \n '"'"`

    StringBuilder builder;
    builder.append("'");
    auto started_single_quote = true;

    for (auto c : token) {
        switch (c) {
        case '\'':
            builder.append("\"'\"");
            started_single_quote = false;
            continue;
        default:
            builder.append(c);
            if (!started_single_quote) {
                started_single_quote = true;
                builder.append("'");
            }
            break;
        }
    }

    if (started_single_quote)
        builder.append("'");

    return builder.build();
}

inline String escape_token_for_double_quotes(const StringView token)
{
    // `foo bar \n $x 'blah "hello` -> `"foo bar \\n $x 'blah \"hello"`

    StringBuilder builder;
    builder.append('"');

    for (auto c : token) {
        switch (c) {
        case '\"':
            builder.append("\\\"");
            continue;
        case '\\':
            builder.append("\\\\");
            continue;
        default:
            builder.append(c);
            break;
        }
    }

    builder.append('"');

    return builder.build();
}

inline SpecialCharacterEscapeMode special_character_escape_mode(u32 code_point)
{
    switch (code_point) {
    case '\'':
    case '"':
    case '$':
    case '|':
    case '>':
    case '<':
    case '(':
    case ')':
    case '{':
    case '}':
    case '&':
    case ';':
    case '\\':
    case ' ':
        return SpecialCharacterEscapeMode::Escaped;
    case '\n':
    case '\t':
    case '\r':
        return SpecialCharacterEscapeMode::QuotedAsEscape;
    default:
        // FIXME: Should instead use unicode's "graphic" property (categories L, M, N, P, S, Zs)
        if (is_ascii(code_point))
            return is_ascii_printable(code_point) ? SpecialCharacterEscapeMode::Untouched : SpecialCharacterEscapeMode::QuotedAsHex;
        return SpecialCharacterEscapeMode::Untouched;
    }
}

inline String escape_token(const StringView token)
{
    auto do_escape = [](auto& token) {
        StringBuilder builder;
        for (auto c : token) {
            static_assert(sizeof(c) == sizeof(u32) || sizeof(c) == sizeof(u8));
            switch (special_character_escape_mode(c)) {
            case SpecialCharacterEscapeMode::Untouched:
                if constexpr (sizeof(c) == sizeof(u8))
                    builder.append(c);
                else
                    builder.append(Utf32View { &c, 1 });
                break;
            case SpecialCharacterEscapeMode::Escaped:
                builder.append('\\');
                builder.append(c);
                break;
            case SpecialCharacterEscapeMode::QuotedAsEscape:
                switch (c) {
                case '\n':
                    builder.append(R"("\n")");
                    break;
                case '\t':
                    builder.append(R"("\t")");
                    break;
                case '\r':
                    builder.append(R"("\r")");
                    break;
                default:
                    VERIFY_NOT_REACHED();
                }
                break;
            case SpecialCharacterEscapeMode::QuotedAsHex:
                if (c <= NumericLimits<u8>::max())
                    builder.appendff(R"("\x{:0>2x}")", static_cast<u8>(c));
                else
                    builder.appendff(R"("\u{:0>8x}")", static_cast<u32>(c));
                break;
            }
        }

        return builder.build();
    };

    Utf8View view { token };
    if (view.validate())
        return do_escape(view);
    return do_escape(token);
}

inline String unescape_token(const StringView token)
{
    StringBuilder builder;

    enum {
        Free,
        Escaped
    } state { Free };

    for (auto c : token) {
        switch (state) {
        case Escaped:
            builder.append(c);
            state = Free;
            break;
        case Free:
            if (c == '\\')
                state = Escaped;
            else
                builder.append(c);
            break;
        }
    }

    if (state == Escaped)
        builder.append('\\');

    return builder.build();
}

}
