/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Intl/DateTimeFormat.h>

namespace JS::Intl {

Vector<StringView> const& DateTimeFormat::relevant_extension_keys()
{
    // 11.3.3 Internal slots, https://tc39.es/ecma402/#sec-intl.datetimeformat-internal-slots
    // The value of the [[RelevantExtensionKeys]] internal slot is « "ca", "hc", "nu" ».
    static Vector<StringView> relevant_extension_keys { "ca"sv, "hc"sv, "nu"sv };
    return relevant_extension_keys;
}

// 11 DateTimeFormat Objects, https://tc39.es/ecma402/#datetimeformat-objects
DateTimeFormat::DateTimeFormat(Object& prototype)
    : Object(prototype)
{
}

DateTimeFormat::Style DateTimeFormat::style_from_string(StringView style)
{
    if (style == "full"sv)
        return Style::Full;
    if (style == "long"sv)
        return Style::Long;
    if (style == "medium"sv)
        return Style::Medium;
    if (style == "short"sv)
        return Style::Short;
    VERIFY_NOT_REACHED();
}

StringView DateTimeFormat::style_to_string(Style style)
{
    switch (style) {
    case Style::Full:
        return "full"sv;
    case Style::Long:
        return "long"sv;
    case Style::Medium:
        return "medium"sv;
    case Style::Short:
        return "short"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

}
