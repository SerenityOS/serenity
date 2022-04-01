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
    ClassicWindowTheme() = default;
    virtual ~ClassicWindowTheme() override = default;

    virtual void paint_normal_frame(Painter& painter, WindowState window_state, IntRect const& window_rect, StringView window_title, Bitmap const& icon, Palette const& palette, IntRect const& leftmost_button_rect, int menu_row_count, bool window_modified) const override;
    virtual void paint_tool_window_frame(Painter&, WindowState, IntRect const& window_rect, StringView title, Palette const&, IntRect const& leftmost_button_rect) const override;
    virtual void paint_notification_frame(Painter&, IntRect const& window_rect, Palette const&, IntRect const& close_button_rect) const override;

    virtual int titlebar_height(WindowType, Palette const&) const override;
    virtual IntRect titlebar_rect(WindowType, IntRect const& window_rect, Palette const&) const override;
    virtual IntRect titlebar_icon_rect(WindowType, IntRect const& window_rect, Palette const&) const override;
    virtual IntRect titlebar_text_rect(WindowType, IntRect const& window_rect, Palette const&) const override;

    virtual IntRect menubar_rect(WindowType, IntRect const& window_rect, Palette const&, int menu_row_count) const override;

    virtual IntRect frame_rect_for_window(WindowType, IntRect const& window_rect, Palette const&, int menu_row_count) const override;

    virtual Vector<IntRect> layout_buttons(WindowType, IntRect const& window_rect, Palette const&, size_t buttons) const override;
    virtual bool is_simple_rect_frame() const override { return true; }
    virtual bool frame_uses_alpha(WindowState state, Palette const& palette) const override
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
            // effectively rendered on top of the borders and don't mean whether the frame itself actually has
            // any alpha channels that would require the entire frame to be rendered as transparency.
            return title_color.alpha() != 0xff || border_color.alpha() != 0xff || border_color2.alpha() != 0xff;
        }
    };

    FrameColors compute_frame_colors(WindowState, Palette const&) const;
};

}
