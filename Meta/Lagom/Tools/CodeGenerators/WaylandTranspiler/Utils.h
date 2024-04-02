/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>

namespace Wayland {

static StringBuilder to_code_name(StringView name)
{
    StringBuilder builder;
    auto vec = name.split_view('_');

    for (auto iter = vec.begin(); !iter.is_end(); ++iter) {
        auto split = (*iter);
        if (split == "wl")
            continue;
        builder.append(split.to_titlecase_string());
    }

    return builder;
}

enum class DefaultValue {
    True,
    False,
};

static bool optional_boolean_string_to_bool(Optional<ByteString> bs, DefaultValue default_value = DefaultValue::False)
{
    bool value = default_value == DefaultValue::True;
    if (bs.has_value()) {
        auto const& string = bs.value();
        if (string == "true") {
            value = true;
        } else {
            VERIFY(string == "false");
            value = false;
        }
    }
    return value;
}

static ByteString titlecase_with_split(ByteString const& string, char seperator = '_')
{
    StringBuilder builder;
    auto parts = string.split(seperator);
    for (auto& part : parts) {
        builder.append(part.to_titlecase());
    }

    return builder.to_byte_string();
}

}
