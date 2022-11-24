/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>

namespace ColorScheme {

Vector<String> get_color_scheme_names();

class ColorScheme {
public:
    ErrorOr<void> set_color_scheme_from_string(StringView name);
    Vector<Gfx::Color> get_colors() { return m_colors; };
    Vector<Gfx::Color> get_bright_colors() { return m_bright_colors; };
    Optional<Gfx::Color> get_background_color() { return m_background_color; };
    Optional<Gfx::Color> get_foreground_color() { return m_foreground_color; };
    bool should_show_bold_text_as_bright() { return m_show_bold_text_as_bright; };

private:
    Vector<Gfx::Color> m_colors;
    Vector<Gfx::Color> m_bright_colors;
    Optional<Gfx::Color> m_background_color;
    Optional<Gfx::Color> m_foreground_color;
    bool m_show_bold_text_as_bright;
};

}
