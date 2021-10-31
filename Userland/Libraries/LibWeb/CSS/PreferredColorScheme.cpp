/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/PreferredColorScheme.h>

namespace Web::CSS {

PreferredColorScheme preferred_color_scheme_from_string(String const& value)
{
    if (value.equals_ignoring_case("light"))
        return PreferredColorScheme::Light;
    if (value.equals_ignoring_case("dark"))
        return PreferredColorScheme::Dark;
    return PreferredColorScheme::Auto;
}

StringView preferred_color_scheme_to_string(PreferredColorScheme value)
{
    switch (value) {
    case PreferredColorScheme::Light:
        return "light"sv;
    case PreferredColorScheme::Dark:
        return "dark"sv;
    case PreferredColorScheme::Auto:
        return "auto"sv;
    }
    VERIFY_NOT_REACHED();
}

}
