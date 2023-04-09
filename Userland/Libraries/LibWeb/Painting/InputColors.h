/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibGfx/Color.h>
#include <LibGfx/Palette.h>

namespace Web::Painting {

// Note: the color names reflect what the colors would be for a light theme,
// not necessary the actual colors.
struct InputColors {
    Color accent;
    Color base;
    Color dark_gray;
    Color gray;
    Color mid_gray;
    Color light_gray;

    Color background_color(bool enabled) { return enabled ? base : light_gray; }
    Color border_color(bool enabled) { return enabled ? gray : mid_gray; }

    static Color get_shade(Color color, float amount, bool is_dark_theme)
    {
        return color.mixed_with(is_dark_theme ? Color::Black : Color::White, amount);
    }
};

InputColors compute_input_colors(Palette const& palette, Optional<Color> accent_color);

}
