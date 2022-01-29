/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibUnicode/CharacterTypes.h>

template<typename Callback>
void for_each_character_containing(StringView query, Callback callback)
{
    String uppercase_query = query.to_uppercase_string();
    StringView uppercase_query_view = uppercase_query.view();
    constexpr u32 maximum_code_point = 0x10FFFF;
    // FIXME: There's probably a better way to do this than just looping, but it still only takes ~150ms to run for me!
    for (u32 code_point = 1; code_point <= maximum_code_point; ++code_point) {
        if (auto maybe_display_name = Unicode::code_point_display_name(code_point); maybe_display_name.has_value()) {
            auto display_name = maybe_display_name.release_value();
            if (display_name.contains(uppercase_query_view, AK::CaseSensitivity::CaseSensitive))
                callback(code_point, move(display_name));
        }
    }
}
