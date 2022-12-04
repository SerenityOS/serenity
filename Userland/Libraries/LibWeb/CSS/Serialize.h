/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibWeb/CSS/UnicodeRange.h>

namespace Web::CSS {

void escape_a_character(StringBuilder&, u32 character);
void escape_a_character_as_code_point(StringBuilder&, u32 character);
void serialize_an_identifier(StringBuilder&, StringView ident);
void serialize_a_string(StringBuilder&, StringView string);
void serialize_a_url(StringBuilder&, StringView url);
void serialize_a_local(StringBuilder&, StringView path);
void serialize_unicode_ranges(StringBuilder&, Vector<UnicodeRange> const& unicode_ranges);
void serialize_a_srgb_value(StringBuilder&, Color color);

DeprecatedString escape_a_character(u32 character);
DeprecatedString escape_a_character_as_code_point(u32 character);
DeprecatedString serialize_an_identifier(StringView ident);
DeprecatedString serialize_a_string(StringView string);
DeprecatedString serialize_a_url(StringView url);
DeprecatedString serialize_a_srgb_value(Color color);

template<typename T, typename SerializeItem>
void serialize_a_comma_separated_list(StringBuilder& builder, Vector<T> const& items, SerializeItem serialize_item)
{
    for (size_t i = 0; i < items.size(); i++) {
        auto& item = items.at(i);
        serialize_item(item);
        if ((i + 1) < items.size()) {
            builder.append(",\n"sv);
        }
    }
}

}
