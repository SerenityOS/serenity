/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/ClassicWindowTheme.h>
#include <LibGfx/Rect.h>
#include <LibGfx/StylePainter.h>

namespace Gfx {

class PlasticWindowTheme : public ClassicWindowTheme {
    virtual void paint_normal_frame(Painter& painter, WindowState window_state, WindowMode window_mode, IntRect const& window_rect, StringView window_title, Bitmap const& icon, Palette const& palette, IntRect const& leftmost_button_rect, int menu_row_count, bool window_modified) const override;
    virtual void paint_notification_frame(Painter&, WindowMode, IntRect const& window_rect, Palette const&, IntRect const& close_button_rect) const override;

    virtual IntRect titlebar_rect(WindowType, WindowMode, IntRect const& window_rect, Palette const&) const override;
    virtual IntRect titlebar_text_rect(WindowType type, WindowMode mode, IntRect const& window_rect, Palette const& palette) const override
    {
        return titlebar_rect(type, mode, window_rect, palette);
    }
    virtual bool frame_uses_alpha(WindowState, Palette const&) const override
    {
        return true;
    }
    virtual Vector<IntRect> layout_buttons(WindowType, WindowMode, IntRect const& window_rect, Palette const&, size_t buttons, bool is_maximized = false) const override;

    virtual void paint_taskbar(Painter&, IntRect const& taskbar_rect, Palette const&) const override;

    virtual void paint_button(Painter&, IntRect const&, Palette const&, ButtonStyle, bool pressed, bool hovered = false, bool checked = false, bool enabled = true, bool focused = false, bool default_button = false) const override;
};

}
