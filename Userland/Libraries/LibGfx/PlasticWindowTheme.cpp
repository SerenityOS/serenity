/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Gradients.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/PlasticWindowTheme.h>
#include <LibGfx/StylePainter.h>

namespace Gfx {

constexpr float inactive_tint_amount = 0.3f;

static constexpr Color tint_color(Color base, Color tint, float amount)
{
    return base.mixed_with(tint, amount).with_alpha(base.alpha());
}

template<size_t Size>
Array<ColorStop, Size> tint_color_stops(Array<ColorStop, Size> const& base_color_stops, Color tint, float amount)
{
    if (amount == 0.0f)
        return base_color_stops;
    Array<ColorStop, Size> tinted_color_stops;
    for (size_t i = 0; i < Size; i++) {
        tinted_color_stops[i] = base_color_stops[i];
        tinted_color_stops[i].color = tint_color(tinted_color_stops[i].color, tint, amount);
    }
    return tinted_color_stops;
}

// TODO: Somehow allow colors to be configured in the theme .ini file.
static Array const s_title_gradient {
    ColorStop { Color(9, 151, 255), 0.00f },
    ColorStop { Color(0, 83, 238), 0.14f },
    ColorStop { Color(0, 80, 238), 0.40f },
    ColorStop { Color(0, 102, 255), 0.88f },
    ColorStop { Color(0, 102, 255), 0.93f },
    ColorStop { Color(0, 91, 255), 0.95f },
    ColorStop { Color(0, 61, 215), 0.96f },
    ColorStop { Color(0, 61, 215), 1.00f }
};

static Array const s_inactive_title_gradient = tint_color_stops(s_title_gradient, Color::White, inactive_tint_amount);

static Array const s_button_gradient_base {
    ColorStop { Color(72, 146, 247), 0.0f },
    ColorStop { Color(57, 128, 244), 0.05f },
    ColorStop { Color(57, 128, 244), 1.0f }
};

static Array const s_button_gradient_overlay {
    ColorStop { Color(109, 164, 246), 0.0f },
    ColorStop { Color::Transparent, 0.05f },
    ColorStop { Color::Transparent, 1.0f }
};

static constexpr struct FrameColors {
    Color base { 22, 39, 213 };
    Color middle_shade { 22, 80, 217 };
    Color light_shade { 32, 102, 234 };
    Color close_button { 246, 63, 0 };
} s_frame_colors;

static FrameColors const s_inactive_frame_colors {
    .base = tint_color(s_frame_colors.base, Color::White, inactive_tint_amount),
    .middle_shade = tint_color(s_frame_colors.middle_shade, Color::White, inactive_tint_amount),
    .light_shade = tint_color(s_frame_colors.light_shade, Color::White, inactive_tint_amount),
    .close_button = tint_color(s_frame_colors.close_button, Color::White, inactive_tint_amount)
};

static constexpr struct {
    Color border { 38, 83, 174 };
} s_button_colors;

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

IntRect PlasticWindowTheme::titlebar_rect(WindowType window_type, WindowMode window_mode, IntRect const& window_rect, Palette const& palette) const
{
    auto window_titlebar_height = titlebar_height(window_type, window_mode, palette);
    // FIXME: Theme notifications.
    if (window_type == WindowType::Notification)
        return ClassicWindowTheme::titlebar_rect(window_type, window_mode, window_rect, palette);
    return { 0, 0, window_rect.width() + palette.window_border_thickness() * 2, window_titlebar_height };
}

static void paint_window_frame(IntRect rect, Painter& painter, Palette const& palette, FrameColors const& frame_colors)
{
    int border_thickness = palette.window_border_thickness();
    auto border_rect = rect.shrunken(border_thickness, border_thickness);
    painter.draw_rect_with_thickness(border_rect, frame_colors.base, border_thickness);
    painter.draw_line(rect.top_left().translated(0, 1), rect.bottom_left(), frame_colors.base);
    painter.draw_line(rect.top_left().translated(1, 1), rect.top_right().translated(-1, 1), frame_colors.light_shade);
    painter.draw_line(rect.top_left().translated(1, 1), rect.bottom_left().translated(1, -1), frame_colors.light_shade);
    painter.draw_line(rect.top_right(), rect.bottom_right(), frame_colors.base);
    painter.draw_line(rect.top_right().translated(-1, 1), rect.bottom_right().translated(-1, -1), frame_colors.middle_shade);
    painter.draw_line(rect.bottom_left(), rect.bottom_right(), frame_colors.base);
    painter.draw_line(rect.bottom_left().translated(1, -1), rect.bottom_right().translated(-1, -1), frame_colors.middle_shade);
}

void PlasticWindowTheme::paint_normal_frame(Painter& painter, WindowState window_state, WindowMode window_mode, IntRect const& window_rect, StringView window_title, Bitmap const& icon, Palette const& palette, IntRect const& leftmost_button_rect, int menu_row_count, bool window_modified) const
{
    // FIXME: Handle these cases.
    (void)icon;
    (void)window_modified;

    bool is_inactive = window_state == WindowState::Inactive;
    auto const& frame_colors = is_inactive ? s_inactive_frame_colors : s_frame_colors;
    auto const& title_gradient = is_inactive ? s_inactive_title_gradient : s_title_gradient;

    auto frame_rect = frame_rect_for_window(WindowType::Normal, window_mode, window_rect, palette, menu_row_count);
    frame_rect.set_location({ 0, 0 });
    paint_window_frame(frame_rect, painter, palette, frame_colors);

    // Draw frame title.
    auto titlebar_rect = this->titlebar_rect(WindowType::Normal, window_mode, window_rect, palette);
    titlebar_rect.set_height(titlebar_rect.height() + palette.window_border_thickness() + 1);
    painter.fill_rect_with_linear_gradient(titlebar_rect, title_gradient, 180);

    auto& title_font = FontDatabase::window_title_font();
    auto clipped_title_rect = titlebar_rect.translated(7, 0);
    clipped_title_rect.set_width(leftmost_button_rect.left() - clipped_title_rect.x());
    if (!clipped_title_rect.is_empty()) {
        auto title_alignment = palette.title_alignment();
        painter.draw_text(clipped_title_rect.translated(1, 2), window_title, title_font, title_alignment, Color(15, 16, 137), Gfx::TextElision::Right);
        // FIXME: The translated(0, 1) wouldn't be necessary if we could center text based on its baseline.
        painter.draw_text(clipped_title_rect.translated(0, 1), window_title, title_font, title_alignment, Color::White, Gfx::TextElision::Right);
    }

    // Paint/clip border radii.
    Gfx::IntRect point(0, 0, 1, 1);
    auto border_radius = s_window_border_radius_mask.width();
    auto left_border_radius_pos = titlebar_rect.location();
    auto right_border_radius_pos = titlebar_rect.location().translated(titlebar_rect.width() - border_radius, 0);
    painter.draw_rect(titlebar_rect, frame_colors.base);
    for (unsigned y = 0; y < s_window_border_radius_mask.height(); y++) {
        for (unsigned x = 0; x < s_window_border_radius_mask.width(); x++) {
            if (s_window_border_radius_mask.bit_at(x, y)) {
                painter.clear_rect(point.translated(left_border_radius_pos).translated(x, y), Color::Transparent);
                painter.clear_rect(point.translated(right_border_radius_pos).translated(border_radius - x, y), Color::Transparent);
            }
            if (s_window_border_radius_accent.bit_at(x, y)) {
                painter.clear_rect(point.translated(left_border_radius_pos).translated(x, y), frame_colors.base);
                painter.clear_rect(point.translated(right_border_radius_pos).translated(border_radius - x, y), frame_colors.base);
            }
        }
    }
}

void PlasticWindowTheme::paint_notification_frame(Painter& painter, WindowMode window_mode, IntRect const& window_rect, Palette const& palette, IntRect const& close_button_rect) const
{
    (void)close_button_rect;

    auto frame_rect = frame_rect_for_window(WindowType::Notification, window_mode, window_rect, palette, 0);
    frame_rect.set_location({ 0, 0 });

    paint_window_frame(frame_rect, painter, palette, s_frame_colors);

    auto titlebar_rect = this->titlebar_rect(WindowType::Notification, window_mode, window_rect, palette);
    painter.fill_rect_with_linear_gradient(titlebar_rect, s_title_gradient, 270);
}

Vector<IntRect> PlasticWindowTheme::layout_buttons(WindowType window_type, WindowMode window_mode, IntRect const& window_rect, Palette const& palette, size_t buttons, bool is_maximized) const
{
    auto button_rects = ClassicWindowTheme::layout_buttons(window_type, window_mode, window_rect, palette, buttons, is_maximized);
    if (window_type != WindowType::Notification) {
        IntPoint offset(-s_window_border_radius_mask.width(), 2);
        for (auto& rect : button_rects)
            rect.translate_by(offset);
    }
    return button_rects;
}

void PlasticWindowTheme::paint_taskbar(Painter& painter, IntRect const& taskbar_rect, Palette const&) const
{
    painter.fill_rect_with_linear_gradient(taskbar_rect, s_title_gradient, 180);
}

void PlasticWindowTheme::paint_button(Painter& painter, IntRect const& rect, Palette const&, ButtonStyle button_style, bool pressed, bool hovered, bool checked, bool enabled, bool focused, bool default_button) const
{
    // FIXME: Handle these cases.
    (void)button_style;
    (void)pressed;
    (void)hovered;
    (void)checked;
    (void)enabled;
    (void)focused;
    (void)default_button;

    if (focused)
        return;

    painter.fill_rect_with_linear_gradient(rect, s_button_gradient_base, 180);
    painter.fill_rect_with_linear_gradient(rect, s_button_gradient_overlay, 160);
    painter.draw_rect(rect, s_button_colors.border);
}

}
