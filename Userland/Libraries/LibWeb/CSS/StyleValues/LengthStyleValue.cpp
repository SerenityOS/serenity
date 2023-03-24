/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LengthStyleValue.h"

namespace Web::CSS {

ValueComparingNonnullRefPtr<LengthStyleValue> LengthStyleValue::create(Length const& length)
{
    if (length.is_auto()) {
        static auto value = adopt_ref(*new LengthStyleValue(CSS::Length::make_auto()));
        return value;
    }
    if (length.is_px()) {
        if (length.raw_value() == 0) {
            static auto value = adopt_ref(*new LengthStyleValue(CSS::Length::make_px(0)));
            return value;
        }
        if (length.raw_value() == 1) {
            static auto value = adopt_ref(*new LengthStyleValue(CSS::Length::make_px(1)));
            return value;
        }
    }
    return adopt_ref(*new LengthStyleValue(length));
}

ValueComparingNonnullRefPtr<StyleValue const> LengthStyleValue::absolutized(CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, CSSPixels font_size, CSSPixels root_font_size, CSSPixels line_height, CSSPixels root_line_height) const
{
    if (auto length = absolutized_length(m_length, viewport_rect, font_metrics, font_size, root_font_size, line_height, root_line_height); length.has_value())
        return LengthStyleValue::create(length.release_value());
    return *this;
}

}
