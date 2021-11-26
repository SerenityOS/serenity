/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibGfx/Forward.h>

namespace Gfx {

class WindowTheme {
public:
    enum class WindowType {
        Normal,
        ToolWindow,
        Notification,
        Other,
    };

    enum class WindowState {
        Active,
        Inactive,
        Highlighted,
        Moving,
    };

    virtual ~WindowTheme();

    static WindowTheme& current();

    virtual void paint_normal_frame(Painter&, WindowState, const IntRect& window_rect, StringView title, const Bitmap& icon, const Palette&, const IntRect& leftmost_button_rect, int menu_row_count, bool window_modified) const = 0;
    virtual void paint_tool_window_frame(Painter&, WindowState, const IntRect& window_rect, StringView title, const Palette&, const IntRect& leftmost_button_rect) const = 0;
    virtual void paint_notification_frame(Painter&, const IntRect& window_rect, const Palette&, const IntRect& close_button_rect) const = 0;

    virtual int titlebar_height(WindowType, const Palette&) const = 0;
    virtual IntRect titlebar_rect(WindowType, const IntRect& window_rect, const Palette&) const = 0;
    virtual IntRect titlebar_icon_rect(WindowType, const IntRect& window_rect, const Palette&) const = 0;
    virtual IntRect titlebar_text_rect(WindowType, const IntRect& window_rect, const Palette&) const = 0;

    virtual IntRect menubar_rect(WindowType, const IntRect& window_rect, const Palette&, int menu_row_count) const = 0;

    virtual IntRect frame_rect_for_window(WindowType, const IntRect& window_rect, const Palette&, int menu_row_count) const = 0;

    virtual Vector<IntRect> layout_buttons(WindowType, const IntRect& window_rect, const Palette&, size_t buttons) const = 0;
    virtual bool is_simple_rect_frame() const = 0;
    virtual bool frame_uses_alpha(WindowState, const Palette&) const = 0;
    virtual float frame_alpha_hit_threshold(WindowState) const = 0;

protected:
    WindowTheme() { }
};

}
