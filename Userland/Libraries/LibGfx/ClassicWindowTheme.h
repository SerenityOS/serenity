/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Color.h>
#include <LibGfx/WindowTheme.h>

namespace Gfx {

class ClassicWindowTheme final : public WindowTheme {
public:
    ClassicWindowTheme();
    virtual ~ClassicWindowTheme() override;

    virtual void paint_normal_frame(Painter& painter, WindowState window_state, const IntRect& window_rect, StringView window_title, const Bitmap& icon, const Palette& palette, const IntRect& leftmost_button_rect, int menu_row_count, bool window_modified) const override;
    virtual void paint_tool_window_frame(Painter&, WindowState, const IntRect& window_rect, StringView title, const Palette&, const IntRect& leftmost_button_rect) const override;
    virtual void paint_notification_frame(Painter&, const IntRect& window_rect, const Palette&, const IntRect& close_button_rect) const override;

    virtual int titlebar_height(WindowType, const Palette&) const override;
    virtual IntRect titlebar_rect(WindowType, const IntRect& window_rect, const Palette&) const override;
    virtual IntRect titlebar_icon_rect(WindowType, const IntRect& window_rect, const Palette&) const override;
    virtual IntRect titlebar_text_rect(WindowType, const IntRect& window_rect, const Palette&) const override;

    virtual IntRect menubar_rect(WindowType, const IntRect& window_rect, const Palette&, int menu_row_count) const override;

    virtual IntRect frame_rect_for_window(WindowType, const IntRect& window_rect, const Palette&, int menu_row_count) const override;

    virtual Vector<IntRect> layout_buttons(WindowType, const IntRect& window_rect, const Palette&, size_t buttons) const override;
    virtual bool is_simple_rect_frame() const override { return true; }
    virtual bool frame_uses_alpha(WindowState state, const Palette& palette) const override
    {
        return compute_frame_colors(state, palette).uses_alpha();
    }
    virtual float frame_alpha_hit_threshold(WindowState) const override { return 1.0f; }

private:
    struct FrameColors {
        Color title_color;
        Color border_color;
        Color border_color2;
        Color title_stripes_color;
        Color title_shadow_color;

        bool uses_alpha() const
        {
            // We don't care about the title_stripes_color or title_shadow_color alpha channels because they are
            // effectively rendered on top of the borders and don't mean whether the frame itself atually has
            // any alpha channels that would require the entire frame to be rendered as transparency.
            return title_color.alpha() != 0xff || border_color.alpha() != 0xff || border_color2.alpha() != 0xff;
        }
    };

    FrameColors compute_frame_colors(WindowState, const Palette&) const;
};

}
