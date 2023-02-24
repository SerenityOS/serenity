/*
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibWeb/CSS/UnicodeRange.h>

namespace Web::CSS {

ErrorOr<void> escape_a_character(StringBuilder&, u32 character);
ErrorOr<void> escape_a_character_as_code_point(StringBuilder&, u32 character);
ErrorOr<void> serialize_an_identifier(StringBuilder&, StringView ident);
ErrorOr<void> serialize_a_string(StringBuilder&, StringView string);
ErrorOr<void> serialize_a_url(StringBuilder&, StringView url);
ErrorOr<void> serialize_a_local(StringBuilder&, StringView path);
ErrorOr<void> serialize_unicode_ranges(StringBuilder&, Vector<UnicodeRange> const& unicode_ranges);
ErrorOr<void> serialize_a_srgb_value(StringBuilder&, Color color);

ErrorOr<String> escape_a_character(u32 character);
ErrorOr<String> escape_a_character_as_code_point(u32 character);
ErrorOr<String> serialize_an_identifier(StringView ident);
ErrorOr<String> serialize_a_string(StringView string);
ErrorOr<String> serialize_a_url(StringView url);
ErrorOr<String> serialize_a_srgb_value(Color color);

template<typename T, typename SerializeItem>
ErrorOr<void> serialize_a_comma_separated_list(StringBuilder& builder, Vector<T> const& items, SerializeItem serialize_item)
{
    for (size_t i = 0; i < items.size(); i++) {
        auto& item = items.at(i);
        TRY(serialize_item(builder, item));
        if ((i + 1) < items.size()) {
            TRY(builder.try_append(",\n"sv));
        }
    }
    return {};
}

}
