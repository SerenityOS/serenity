/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Sarah Taube <metalflakecobaltpaint@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGfx/Forward.h>
#include <LibGfx/StylePainter.h>

namespace Gfx {

class ClassicStylePainter : public BaseStylePainter {
public:
    virtual void paint_button(Painter&, IntRect const&, Palette const&, ButtonStyle, bool pressed, bool hovered = false, bool checked = false, bool enabled = true, bool focused = false, bool default_button = false) override;
    virtual void paint_tab_button(Painter&, IntRect const&, Palette const&, bool active, bool hovered, bool enabled, TabPosition position, bool in_active_window, bool accented) override;
    virtual void paint_frame(Painter&, IntRect const&, Palette const&, FrameStyle, bool skip_vertical_lines = false) override;
    virtual void paint_window_frame(Painter&, IntRect const&, Palette const&) override;
    virtual void paint_progressbar(Painter&, IntRect const&, Palette const&, int min, int max, int value, StringView text, Orientation = Orientation::Horizontal) override;
    virtual void paint_radio_button(Painter&, IntRect const&, Palette const&, bool is_checked, bool is_being_pressed) override;
    virtual void paint_check_box(Painter&, IntRect const&, Palette const&, bool is_enabled, bool is_checked, bool is_being_pressed) override;
    virtual void paint_transparency_grid(Painter&, IntRect const&, Palette const&) override;
    virtual void paint_simple_rect_shadow(Painter&, IntRect const&, Bitmap const& shadow_bitmap, bool shadow_includes_frame, bool fill_content) override;
};

}
