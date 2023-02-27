/*
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS {

// https://www.w3.org/TR/cssom-1/#escape-a-character
ErrorOr<void> escape_a_character(StringBuilder& builder, u32 character)
{
    TRY(builder.try_append('\\'));
    TRY(builder.try_append_code_point(character));
    return {};
}

// https://www.w3.org/TR/cssom-1/#escape-a-character-as-code-point
ErrorOr<void> escape_a_character_as_code_point(StringBuilder& builder, u32 character)
{
    TRY(builder.try_appendff("\\{:x} ", character));
    return {};
}

// https://www.w3.org/TR/cssom-1/#serialize-an-identifier
ErrorOr<void> serialize_an_identifier(StringBuilder& builder, StringView ident)
{
    Utf8View characters { ident };
    auto first_character = characters.is_empty() ? 0 : *characters.begin();

    // To serialize an identifier means to create a string represented by the concatenation of,
    // for each character of the identifier:
    for (auto character : characters) {
        // If the character is NULL (U+0000), then the REPLACEMENT CHARACTER (U+FFFD).
        if (character == 0) {
            TRY(builder.try_append_code_point(0xFFFD));
            continue;
        }
        // If the character is in the range [\1-\1f] (U+0001 to U+001F) or is U+007F,
        // then the character escaped as code point.
        if ((character >= 0x0001 && character <= 0x001F) || (character == 0x007F)) {
            TRY(escape_a_character_as_code_point(builder, character));
            continue;
        }
        // If the character is the first character and is in the range [0-9] (U+0030 to U+0039),
        // then the character escaped as code point.
        if (builder.is_empty() && character >= '0' && character <= '9') {
            TRY(escape_a_character_as_code_point(builder, character));
            continue;
        }
        // If the character is the second character and is in the range [0-9] (U+0030 to U+0039)
        // and the first character is a "-" (U+002D), then the character escaped as code point.
        if (builder.length() == 1 && first_character == '-' && character >= '0' && character <= '9') {
            TRY(escape_a_character_as_code_point(builder, character));
            continue;
        }
        // If the character is the first character and is a "-" (U+002D), and there is no second
        // character, then the escaped character.
        if (builder.is_empty() && character == '-' && characters.length() == 1) {
            TRY(escape_a_character(builder, character));
            continue;
        }
        // If the character is not handled by one of the above rules and is greater than or equal to U+0080, is "-" (U+002D) or "_" (U+005F), or is in one of the ranges [0-9] (U+0030 to U+0039), [A-Z] (U+0041 to U+005A), or \[a-z] (U+0061 to U+007A), then the character itself.
        if ((character >= 0x0080)
            || (character == '-') || (character == '_')
            || (character >= '0' && character <= '9')
            || (character >= 'A' && character <= 'Z')
            || (character >= 'a' && character <= 'z')) {
            TRY(builder.try_append_code_point(character));
            continue;
        }
        // Otherwise, the escaped character.
        TRY(escape_a_character(builder, character));
    }
    return {};
}

// https://www.w3.org/TR/cssom-1/#serialize-a-string
ErrorOr<void> serialize_a_string(StringBuilder& builder, StringView string)
{
    Utf8View characters { string };

    // To serialize a string means to create a string represented by '"' (U+0022), followed by the result
    // of applying the rules below to each character of the given string, followed by '"' (U+0022):
    TRY(builder.try_append('"'));

    for (auto character : characters) {
        // If the character is NULL (U+0000), then the REPLACEMENT CHARACTER (U+FFFD).
        if (character == 0) {
            TRY(builder.try_append_code_point(0xFFFD));
            continue;
        }
        // If the character is in the range [\1-\1f] (U+0001 to U+001F) or is U+007F, the character escaped as code point.
        if ((character >= 0x0001 && character <= 0x001F) || (character == 0x007F)) {
            TRY(escape_a_character_as_code_point(builder, character));
            continue;
        }
        // If the character is '"' (U+0022) or "\" (U+005C), the escaped character.
        if (character == 0x0022 || character == 0x005C) {
            TRY(escape_a_character(builder, character));
            continue;
        }
        // Otherwise, the character itself.
        TRY(builder.try_append_code_point(character));
    }

    TRY(builder.try_append('"'));
    return {};
}

// https://www.w3.org/TR/cssom-1/#serialize-a-url
ErrorOr<void> serialize_a_url(StringBuilder& builder, StringView url)
{
    // To serialize a URL means to create a string represented by "url(",
    // followed by the serialization of the URL as a string, followed by ")".
    TRY(builder.try_append("url("sv));
    TRY(serialize_a_string(builder, url.to_deprecated_string()));
    TRY(builder.try_append(')'));
    return {};
}

// https://www.w3.org/TR/cssom-1/#serialize-a-local
ErrorOr<void> serialize_a_local(StringBuilder& builder, StringView path)
{
    // To serialize a LOCAL means to create a string represented by "local(",
    // followed by the serialization of the LOCAL as a string, followed by ")".
    TRY(builder.try_append("local("sv));
    TRY(serialize_a_string(builder, path.to_deprecated_string()));
    TRY(builder.try_append(')'));
    return {};
}

// NOTE: No spec currently exists for serializing a <'unicode-range'>.
ErrorOr<void> serialize_unicode_ranges(StringBuilder& builder, Vector<UnicodeRange> const& unicode_ranges)
{
    TRY(serialize_a_comma_separated_list(builder, unicode_ranges, [](auto& builder, UnicodeRange unicode_range) -> ErrorOr<void> {
        return serialize_a_string(builder, TRY(unicode_range.to_string()));
    }));
    return {};
}

// https://www.w3.org/TR/css-color-4/#serializing-sRGB-values
ErrorOr<void> serialize_a_srgb_value(StringBuilder& builder, Color color)
{
    // The serialized form is derived from the computed value and thus, uses either the rgb() or rgba() form
    // (depending on whether the alpha is exactly 1, or not), with lowercase letters for the function name.
    // NOTE: Since we use Gfx::Color, having an "alpha of 1" means its value is 255.
    if (color.alpha() == 255)
        TRY(builder.try_appendff("rgb({}, {}, {})"sv, color.red(), color.green(), color.blue()));
    else
        TRY(builder.try_appendff("rgba({}, {}, {}, {})"sv, color.red(), color.green(), color.blue(), (float)(color.alpha()) / 255.0f));
    return {};
}

ErrorOr<String> escape_a_character(u32 character)
{
    StringBuilder builder;
    TRY(escape_a_character(builder, character));
    return builder.to_string();
}

ErrorOr<String> escape_a_character_as_code_point(u32 character)
{
    StringBuilder builder;
    TRY(escape_a_character_as_code_point(builder, character));
    return builder.to_string();
}

ErrorOr<String> serialize_an_identifier(StringView ident)
{
    StringBuilder builder;
    TRY(serialize_an_identifier(builder, ident));
    return builder.to_string();
}

ErrorOr<String> serialize_a_string(StringView string)
{
    StringBuilder builder;
    TRY(serialize_a_string(builder, string));
    return builder.to_string();
}

ErrorOr<String> serialize_a_url(StringView url)
{
    StringBuilder builder;
    TRY(serialize_a_url(builder, url));
    return builder.to_string();
}

ErrorOr<String> serialize_a_srgb_value(Color color)
{
    StringBuilder builder;
    TRY(serialize_a_srgb_value(builder, color));
    return builder.to_string();
}

}
