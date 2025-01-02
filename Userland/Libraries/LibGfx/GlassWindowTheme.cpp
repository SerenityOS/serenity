/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/GlassWindowTheme.h>
#include <LibGfx/Gradients.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>
namespace Gfx {

// TODO: Somehow allow colors to be configured in the theme .ini file.
static Array const s_title_gradient {
    ColorStop { Color(25, 40, 55, 191), 0.35f },
    ColorStop { Color(65, 85, 100, 191), 0.40f },
    ColorStop { Color(65, 85, 100, 191), 0.42f },
    ColorStop { Color(25, 40, 55, 191), 0.50f },
    ColorStop { Color(25, 40, 55, 191), 0.55f },
    ColorStop { Color(70, 85, 100, 191), 0.60f },
    ColorStop { Color(70, 85, 100, 191), 0.75f },
    ColorStop { Color(25, 40, 55, 191), 0.90f }
};

static constexpr struct {
    Color base { 235, 235, 236 };
    Color border { 2, 3, 4, 219 };
} s_frame_colors;

static constexpr Gfx::CharacterBitmap s_window_border_radius_mask {
    "#####"
    "###  "
    "##   "
    "#    "
    "#    "sv,
    5, 5
};

static constexpr Gfx::CharacterBitmap s_window_border_radius_accent {
    "     "
    "   ##"
    "  #  "
    " #   "
    " #   "sv,
    5, 5
};

static constexpr Gfx::CharacterBitmap s_window_border_radius_accent2 {
    "     "
    "     "
    "   ##"
    "  #  "
    "  #  "sv,
    5, 5
};

IntRect GlassWindowTheme::titlebar_rect(WindowType window_type, WindowMode window_mode, IntRect const& window_rect, Palette const& palette) const
{
    auto window_titlebar_height = titlebar_height(window_type, window_mode, palette);
    // FIXME: Theme notifications.
    if (window_type == WindowType::Notification)
        return ClassicWindowTheme::titlebar_rect(window_type, window_mode, window_rect, palette);
    return { 0, 0, window_rect.width() + palette.window_border_thickness() * 2, window_titlebar_height };
}

void GlassWindowTheme::paint_normal_frame(Painter& painter, WindowState window_state, WindowMode window_mode, IntRect const& window_rect, StringView window_title, Bitmap const& icon, Palette const& palette, IntRect const& leftmost_button_rect, int menu_row_count, bool window_modified) const
{
    // FIXME: Handle these cases.
    (void)window_state;
    (void)icon;
    (void)window_modified;

    auto frame_rect = frame_rect_for_window(WindowType::Normal, window_mode, window_rect, palette, menu_row_count);
    auto relative_window_location = window_rect.location() - frame_rect.location();
    frame_rect.set_location({ 0, 0 });
    frame_rect.shrink(0, 1, 1, 1);

    auto frame_rects = frame_rect.shatter({ relative_window_location, window_rect.size() });
    for (auto const& clip_rect : frame_rects) {
        // Paint Aero-style gradient.
        PainterStateSaver saver { painter };
        painter.add_clip_rect(clip_rect);
        painter.fill_rect(frame_rect, s_frame_colors.base.with_alpha(150));
        painter.fill_rect_with_linear_gradient(frame_rect, s_title_gradient, 45, 0.9f);
    }

    // Draw frame title.
    auto titlebar_rect = this->titlebar_rect(WindowType::Normal, window_mode, window_rect, palette);
    titlebar_rect.set_height(titlebar_rect.height() + palette.window_border_thickness() + 1);
    auto& title_font = FontDatabase::window_title_font();
    auto clipped_title_rect = titlebar_rect.translated(7, 0);
    clipped_title_rect.set_width(leftmost_button_rect.left() - clipped_title_rect.x());
    if (!clipped_title_rect.is_empty()) {
        auto title_alignment = palette.title_alignment();
        painter.draw_text(clipped_title_rect.translated(1, 2), window_title, title_font, title_alignment, Color(15, 16, 137), Gfx::TextElision::Right);
        // FIXME: The translated(0, 1) wouldn't be necessary if we could center text based on its baseline.
        painter.draw_text(clipped_title_rect.translated(0, 1), window_title, title_font, title_alignment, Color::White, Gfx::TextElision::Right);
    }

    // Draw frame border.
    auto inner = frame_rect.shrunken(palette.window_title_height() + palette.window_border_thickness(), palette.window_border_thickness(), palette.window_border_thickness(), palette.window_border_thickness());
    painter.draw_rect_with_thickness(frame_rect, s_frame_colors.border, 1);
    painter.draw_rect_with_thickness(frame_rect.shrunken(1, 1, 1, 1), s_frame_colors.base.with_alpha(170), 1);
    painter.draw_rect_with_thickness(inner.inflated(1, 1, 1, 1), s_frame_colors.base.with_alpha(110), 1);
    painter.draw_rect_with_thickness(inner, s_frame_colors.border.with_alpha(110), 1);

    // Paint/clip border radii.
    Gfx::IntRect point(0, 0, 1, 1);
    auto border_radius = s_window_border_radius_mask.width();
    auto left_border_radius_pos = frame_rect.location();
    auto right_border_radius_pos = frame_rect.location().translated(frame_rect.width() - border_radius, 0);
    for (unsigned y = 0; y < s_window_border_radius_mask.height(); y++) {
        for (unsigned x = 0; x < s_window_border_radius_mask.width(); x++) {
            if (s_window_border_radius_mask.bit_at(x, y)) {
                painter.clear_rect(point.translated(left_border_radius_pos).translated(x, y), Color::Transparent);
                painter.clear_rect(point.translated(right_border_radius_pos).translated(border_radius - x, y), Color::Transparent);
            }
            if (s_window_border_radius_accent.bit_at(x, y)) {
                painter.fill_rect(point.translated(left_border_radius_pos).translated(x, y), s_frame_colors.border);
                painter.fill_rect(point.translated(right_border_radius_pos).translated(border_radius - x, y), s_frame_colors.border);
            }
            if (s_window_border_radius_accent2.bit_at(x, y)) {
                painter.fill_rect(point.translated(left_border_radius_pos).translated(x, y), s_frame_colors.base.with_alpha(170));
                painter.fill_rect(point.translated(right_border_radius_pos).translated(border_radius - x, y), s_frame_colors.base.with_alpha(170));
            }
        }
    }
}

void GlassWindowTheme::paint_notification_frame(Painter& painter, WindowMode window_mode, IntRect const& window_rect, Palette const& palette, IntRect const& close_button_rect) const
{
    (void)close_button_rect;

    auto frame_rect = frame_rect_for_window(WindowType::Notification, window_mode, window_rect, palette, 0);
    frame_rect.set_location({ 0, 0 });
    frame_rect.shrink(0, 1, 1, 0);

    painter.fill_rect(frame_rect, s_frame_colors.base.with_alpha(150));
    painter.fill_rect_with_linear_gradient(frame_rect, s_title_gradient, 45, 0.9f);

    painter.draw_rect_with_thickness(frame_rect, s_frame_colors.border, 1);
    painter.draw_rect_with_thickness(frame_rect.shrunken(1, 1, 1, 1), s_frame_colors.base.with_alpha(170), 1);
}

Vector<IntRect> GlassWindowTheme::layout_buttons(WindowType window_type, WindowMode window_mode, IntRect const& window_rect, Palette const& palette, size_t buttons, bool is_maximized) const
{
    auto button_rects = ClassicWindowTheme::layout_buttons(window_type, window_mode, window_rect, palette, buttons, is_maximized);
    auto button_offset = [&](IntRect button_rect) -> IntPoint {
        if (window_type == WindowType::Notification)
            return { 1, -1 };
        return { -max(palette.window_border_thickness(), s_window_border_radius_mask.width()) - 1,
            -button_rect.y() + (is_maximized ? palette.window_border_thickness() : 1) + 3 };
    };

    for (auto& button_rect : button_rects)
        button_rect.translate_by(button_offset(button_rect));
    return button_rects;
}

void GlassWindowTheme::paint_taskbar(Painter& painter, IntRect const& taskbar_rect, Palette const&) const
{
    painter.clear_rect(taskbar_rect, Color::Transparent);
    painter.fill_rect_with_linear_gradient(taskbar_rect, s_title_gradient, 45, 0.9f);
    painter.draw_line(taskbar_rect.top_left(), taskbar_rect.top_right(), s_frame_colors.border, 1);
    painter.draw_line(taskbar_rect.top_left().translated(0, 1), taskbar_rect.top_right().translated(0, 1), s_frame_colors.base.with_alpha(170), 1);
}

}
