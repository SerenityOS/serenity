/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/InputColors.h>

namespace Web::Painting {

InputColors compute_input_colors(Palette const& palette, Optional<Color> accent_color)
{
    // These shades have been picked to work well for all themes and have enough variation to paint
    // all input states (disabled, enabled, checked, etc).
    bool dark_theme = palette.is_dark();
    auto base_text_color = palette.color(ColorRole::BaseText);
    auto accent = accent_color.value_or(palette.color(ColorRole::Accent));
    auto base = InputColors::get_shade(base_text_color.inverted(), 0.8f, dark_theme);
    auto dark_gray = InputColors::get_shade(base_text_color, 0.3f, dark_theme);
    auto gray = InputColors::get_shade(dark_gray, 0.4f, dark_theme);
    auto mid_gray = InputColors::get_shade(gray, 0.3f, dark_theme);
    auto light_gray = InputColors::get_shade(mid_gray, 0.3f, dark_theme);
    return InputColors {
        .accent = accent,
        .base = base,
        .dark_gray = dark_gray,
        .gray = gray,
        .mid_gray = mid_gray,
        .light_gray = light_gray
    };
}

}
