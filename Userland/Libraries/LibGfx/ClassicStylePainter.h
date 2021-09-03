/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Sarah Taube <metalflakecobaltpaint@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Forward.h>
#include <LibGfx/Forward.h>
#include <LibGfx/StylePainter.h>

namespace Gfx {

class ClassicStylePainter : public BaseStylePainter {
public:
    void paint_button(Painter&, const IntRect&, const Palette&, ButtonStyle, bool pressed, bool hovered = false, bool checked = false, bool enabled = true, bool focused = false) override;
    void paint_tab_button(Painter&, const IntRect&, const Palette&, bool active, bool hovered, bool enabled, bool top, bool in_active_window) override;
    void paint_frame(Painter&, const IntRect&, const Palette&, FrameShape, FrameShadow, int thickness, bool skip_vertical_lines = false) override;
    void paint_window_frame(Painter&, const IntRect&, const Palette&) override;
    void paint_progressbar(Painter&, const IntRect&, const Palette&, int min, int max, int value, const StringView& text, Orientation = Orientation::Horizontal) override;
    void paint_radio_button(Painter&, const IntRect&, const Palette&, bool is_checked, bool is_being_pressed) override;
    void paint_check_box(Painter&, const IntRect&, const Palette&, bool is_enabled, bool is_checked, bool is_being_pressed) override;
    void paint_transparency_grid(Painter&, const IntRect&, const Palette&) override;
};

}
