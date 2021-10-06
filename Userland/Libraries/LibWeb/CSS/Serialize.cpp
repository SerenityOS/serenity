/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS {

// https://www.w3.org/TR/cssom-1/#escape-a-character
String escape_a_character(u32 character)
{
    StringBuilder builder;
    builder.append('\\');
    builder.append_code_point(character);
    return builder.to_string();
}

// https://www.w3.org/TR/cssom-1/#escape-a-character-as-code-point
String escape_a_character_as_code_point(u32 character)
{
    return String::formatted("\\{:x} ", character);
}

// https://www.w3.org/TR/cssom-1/#serialize-an-identifier
String serialize_an_identifier(StringView const& ident)
{
    StringBuilder builder;
    Utf8View characters { ident };
    auto first_character = characters.is_empty() ? 0 : *characters.begin();

    // To serialize an identifier means to create a string represented by the concatenation of,
    // for each character of the identifier:
    for (auto character : characters) {
        // If the character is NULL (U+0000), then the REPLACEMENT CHARACTER (U+FFFD).
        if (character == 0) {
            builder.append_code_point(0xFFFD);
            continue;
        }
        // If the character is in the range [\1-\1f] (U+0001 to U+001F) or is U+007F,
        // then the character escaped as code point.
        if ((character >= 0x0001 && character <= 0x001F) || (character == 0x007F)) {
            builder.append(escape_a_character_as_code_point(character));
            continue;
        }
        // If the character is the first character and is in the range [0-9] (U+0030 to U+0039),
        // then the character escaped as code point.
        if (builder.is_empty() && character >= '0' && character <= '9') {
            builder.append(escape_a_character_as_code_point(character));
            continue;
        }
        // If the character is the second character and is in the range [0-9] (U+0030 to U+0039)
        // and the first character is a "-" (U+002D), then the character escaped as code point.
        if (builder.length() == 1 && first_character == '-' && character >= '0' && character <= '9') {
            builder.append(escape_a_character_as_code_point(character));
            continue;
        }
        // If the character is the first character and is a "-" (U+002D), and there is no second
        // character, then the escaped character.
        if (builder.is_empty() && character == '-' && characters.length() == 1) {
            builder.append(escape_a_character(character));
            continue;
        }
        // If the character is not handled by one of the above rules and is greater than or equal to U+0080, is "-" (U+002D) or "_" (U+005F), or is in one of the ranges [0-9] (U+0030 to U+0039), [A-Z] (U+0041 to U+005A), or \[a-z] (U+0061 to U+007A), then the character itself.
        if ((character >= 0x0080)
            || (character == '-') || (character == '_')
            || (character >= '0' && character <= '9')
            || (character >= 'A' && character <= 'Z')
            || (character >= 'a' && character <= 'z')) {
            builder.append_code_point(character);
            continue;
        }
        // Otherwise, the escaped character.
        builder.append(escape_a_character(character));
    }
    return builder.to_string();
}

}
