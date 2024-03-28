/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ColorStyleValue.h"
#include <LibWeb/CSS/Serialize.h>

namespace Web::CSS {

ValueComparingNonnullRefPtr<ColorStyleValue> ColorStyleValue::create(Color color)
{
    if (color.value() == 0) {
        static auto transparent = adopt_ref(*new (nothrow) ColorStyleValue(color));
        return transparent;
    }

    if (color == Color::from_rgb(0x000000)) {
        static auto black = adopt_ref(*new (nothrow) ColorStyleValue(color));
        return black;
    }

    if (color == Color::from_rgb(0xffffff)) {
        static auto white = adopt_ref(*new (nothrow) ColorStyleValue(color));
        return white;
    }

    return adopt_ref(*new (nothrow) ColorStyleValue(color));
}

String ColorStyleValue::to_string() const
{
    return serialize_a_srgb_value(m_color);
}

ValueComparingNonnullRefPtr<ColorStyleValue> NamedColorStyleValue::create(Color color, FlyString const& color_name)
{
    return adopt_ref(*new (nothrow) NamedColorStyleValue(color, color_name));
}

String NamedColorStyleValue::to_string() const
{
    return MUST(m_color_name.to_string().to_lowercase());
}

}
