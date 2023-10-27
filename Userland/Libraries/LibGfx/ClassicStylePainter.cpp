/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Sarah Taube <metalflakecobaltpaint@gmail.com>
 * Copyright (c) 2021, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 * Copyright (c) 2022, Cameron Youell <cameronyouell@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/ClassicStylePainter.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>

namespace Gfx {

void ClassicStylePainter::paint_tab_button(Painter& painter, IntRect const& rect, Palette const& palette, bool active, bool hovered, bool enabled, TabPosition position, bool in_active_window, bool accented)
{
    Color base_color = palette.button();
    Color highlight_color2 = palette.threed_highlight();
    Color shadow_color1 = palette.threed_shadow1();
    Color shadow_color2 = palette.threed_shadow2();

    if (hovered && enabled && !active)
        base_color = palette.hover_highlight();

    PainterStateSaver saver(painter);
    painter.translate(rect.location());

    auto accent = palette.accent();
    if (!in_active_window)
        accent = accent.to_grayscale();

    switch (position) {
    case TabPosition::Top:
        // Base
        painter.fill_rect({ 1, 1, rect.width() - 2, rect.height() - 1 }, base_color);

        // Top line
        if (active && accented) {
            painter.draw_line({ 3, 0 }, { rect.width() - 3, 0 }, accent.darkened());
            painter.fill_rect_with_gradient({ 1, 1, rect.width() - 2, 2 }, accent, accent.lightened(1.5f));
            painter.set_pixel({ 2, 0 }, highlight_color2);
        } else {
            painter.draw_line({ 2, 0 }, { rect.width() - 3, 0 }, highlight_color2);
        }

        // Left side
        painter.draw_line({ 0, 2 }, { 0, rect.height() - 1 }, highlight_color2);
        painter.set_pixel({ 1, 1 }, highlight_color2);

        // Right side
        painter.draw_line({ rect.width() - 1, 2 }, { rect.width() - 1, rect.height() - 1 }, shadow_color2);
        painter.draw_line({ rect.width() - 2, 2 }, { rect.width() - 2, rect.height() - 1 }, shadow_color1);
        painter.set_pixel(rect.width() - 2, 1, shadow_color2);
        break;
    case TabPosition::Bottom:
        // Base
        painter.fill_rect({ 0, 0, rect.width() - 1, rect.height() }, base_color);

        // Bottom line
        if (active && accented) {
            painter.fill_rect_with_gradient({ 1, rect.height() - 3, rect.width() - 2, 2 }, accent, accent.lightened(1.5f));
            painter.draw_line({ 2, rect.height() - 1 }, { rect.width() - 3, rect.height() - 1 }, accent.darkened());
        } else {
            painter.draw_line({ 2, rect.height() - 1 }, { rect.width() - 3, rect.height() - 1 }, shadow_color2);
        }

        // Left side
        painter.draw_line({ 0, 0 }, { 0, rect.height() - 3 }, highlight_color2);
        painter.set_pixel({ 1, rect.height() - 2 }, highlight_color2);

        // Right side
        painter.draw_line({ rect.width() - 1, 0 }, { rect.width() - 1, rect.height() - 3 }, shadow_color2);
        painter.draw_line({ rect.width() - 2, 0 }, { rect.width() - 2, rect.height() - 3 }, shadow_color1);
        painter.set_pixel({ rect.width() - 2, rect.height() - 2 }, shadow_color2);
        break;
    case TabPosition::Left:
        // Base tab
        painter.fill_rect({ 1, 1, rect.width(), rect.height() - 1 }, base_color);
        painter.draw_line({ 2, 0 }, { rect.width(), 0 }, highlight_color2);
        painter.draw_line({ 2, rect.height() - 1 }, { rect.width(), rect.height() - 1 }, shadow_color2);

        // If the tab is active, draw the accent line
        if (active && accented) {
            painter.fill_rect_with_gradient({ 1, 1, 2, rect.height() - 2 }, accent, accent.lightened(1.5f));
            painter.draw_line({ 0, 2 }, { 0, rect.height() - 3 }, accent.darkened());
        } else {
            painter.draw_line({ 0, 2 }, { 0, rect.height() - 3 }, highlight_color2);
            painter.draw_line({ rect.width(), 1 }, { rect.width(), rect.height() - 1 }, shadow_color1);
        }

        // Make appear as if the tab is rounded
        painter.set_pixel({ 1, 1 }, highlight_color2);
        painter.set_pixel({ 1, rect.height() - 2 }, shadow_color2);
        break;
    case TabPosition::Right:
        // Base tab
        painter.fill_rect({ 0, 1, rect.width() - 1, rect.height() - 1 }, base_color);
        painter.draw_line({ 0, 0 }, { rect.width() - 2, 0 }, highlight_color2);
        painter.draw_line({ 0, rect.height() - 1 }, { rect.width() - 2, rect.height() - 1 }, shadow_color2);

        // If the tab is active, draw the accent line
        if (active && accented) {
            painter.fill_rect_with_gradient({ rect.width() - 2, 1, 2, rect.height() - 2 }, accent.lightened(1.5f), accent);
            painter.draw_line({ rect.width(), 2 }, { rect.width(), rect.height() - 3 }, accent.darkened());
        } else {
            painter.draw_line({ rect.width(), 2 }, { rect.width(), rect.height() - 3 }, shadow_color2);
            painter.draw_line({ 0, 0 }, { 0, rect.height() - 1 }, shadow_color1);
        }

        // Make appear as if the tab is rounded
        painter.set_pixel({ rect.width() - 1, 1 }, shadow_color1);
        painter.set_pixel({ rect.width() - 1, rect.height() - 2 }, shadow_color2);
        break;
    }
}

static void paint_button_new(Painter& painter, IntRect const& a_rect, Palette const& palette, ButtonStyle style, bool pressed, bool checked, bool hovered, bool enabled, bool focused, bool default_button)
{
    Color button_color = palette.button();
    Color highlight_color = palette.threed_highlight();
    Color shadow_color1 = palette.threed_shadow1();
    Color shadow_color2 = palette.threed_shadow2();

    if (checked && enabled) {
        if (hovered)
            button_color = palette.hover_highlight();
        else
            button_color = palette.button();
    } else if (hovered && enabled)
        button_color = palette.hover_highlight();

    PainterStateSaver saver(painter);

    auto rect = a_rect;
    if (focused || default_button) {
        painter.draw_rect(a_rect, palette.threed_shadow2());
        rect.shrink(2, 2);
    }

    painter.translate(rect.location());

    if (pressed || checked) {
        // Base
        Gfx::IntRect base_rect { 1, 1, rect.width() - 2, rect.height() - 2 };

        if (checked && !pressed)
            painter.fill_rect_with_dither_pattern(base_rect, palette.button().lightened(1.3f), palette.button());
        else
            painter.fill_rect(base_rect, button_color);

        // Top shadow
        painter.draw_line({ 0, 0 }, { rect.width() - 2, 0 }, shadow_color2);
        painter.draw_line({ 0, 0 }, { 0, rect.height() - 2 }, shadow_color2);

        // Sunken shadow
        painter.draw_line({ 1, 1 }, { rect.width() - 3, 1 }, shadow_color1);
        painter.draw_line({ 1, 2 }, { 1, rect.height() - 3 }, shadow_color1);

        // Outer highlight
        painter.draw_line({ 0, rect.height() - 1 }, { rect.width() - 1, rect.height() - 1 }, highlight_color);
        painter.draw_line({ rect.width() - 1, 0 }, { rect.width() - 1, rect.height() - 2 }, highlight_color);

        // Inner highlight
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, palette.button());
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, palette.button());
    } else {
        // Base
        painter.fill_rect({ 0, 0, rect.width(), rect.height() }, button_color);

        // Top highlight
        if (style == ButtonStyle::Normal) {
            painter.draw_line({ 0, 0 }, { rect.width() - 2, 0 }, highlight_color);
            painter.draw_line({ 0, 0 }, { 0, rect.height() - 2 }, highlight_color);
        } else if (style == ButtonStyle::ThickCap) {
            painter.draw_line({ 1, 1 }, { rect.width() - 2, 1 }, highlight_color);
            painter.draw_line({ 1, 1 }, { 1, rect.height() - 2 }, highlight_color);
        }

        // Outer shadow
        painter.draw_line({ 0, rect.height() - 1 }, { rect.width() - 1, rect.height() - 1 }, shadow_color2);
        painter.draw_line({ rect.width() - 1, 0 }, { rect.width() - 1, rect.height() - 2 }, shadow_color2);

        // Inner shadow
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, shadow_color1);
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, shadow_color1);
    }
}

void ClassicStylePainter::paint_button(Painter& painter, IntRect const& rect, Palette const& palette, ButtonStyle button_style, bool pressed, bool hovered, bool checked, bool enabled, bool focused, bool default_button)
{
    if (button_style == ButtonStyle::Normal || button_style == ButtonStyle::ThickCap)
        return paint_button_new(painter, rect, palette, button_style, pressed, checked, hovered, enabled, focused, default_button);

    if (button_style == ButtonStyle::Coolbar && !enabled)
        return;

    Color button_color = palette.button();
    Color highlight_color = palette.threed_highlight();
    Color shadow_color = button_style == ButtonStyle::Coolbar ? palette.threed_shadow1() : palette.threed_shadow2();

    PainterStateSaver saver(painter);
    painter.translate(rect.location());

    if (pressed || checked) {
        // Base
        IntRect base_rect { 1, 1, rect.width() - 2, rect.height() - 2 };
        if (button_style == ButtonStyle::Coolbar) {
            if (checked && !pressed) {
                painter.fill_rect_with_dither_pattern(base_rect, palette.button().lightened(1.3f), Color());
            } else {
                painter.fill_rect(base_rect, button_color);
            }
        }

        // Sunken shadow
        painter.draw_line({ 1, 1 }, { rect.width() - 2, 1 }, shadow_color);
        painter.draw_line({ 1, 2 }, { 1, rect.height() - 2 }, shadow_color);

        // Bottom highlight
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, highlight_color);
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, highlight_color);
    } else if (hovered) {
        if (button_style == ButtonStyle::Coolbar) {
            // Base
            painter.fill_rect({ 1, 1, rect.width() - 2, rect.height() - 2 }, button_color);
        }

        // Top highlight
        painter.draw_line({ 1, 1 }, { rect.width() - 2, 1 }, highlight_color);
        painter.draw_line({ 1, 2 }, { 1, rect.height() - 2 }, highlight_color);

        // Bottom shadow
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, shadow_color);
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, shadow_color);
    }
}

void ClassicStylePainter::paint_frame(Painter& painter, IntRect const& rect, Palette const& palette, FrameStyle style, bool skip_vertical_lines)
{
    if (style == Gfx::FrameStyle::NoFrame)
        return;

    if (style == FrameStyle::Window) {
        StylePainter::paint_window_frame(painter, rect, palette);
        return;
    }

    Color top_left_color;
    Color bottom_right_color;
    Color dark_shade = palette.threed_shadow1();
    Color light_shade = palette.threed_highlight();

    if (style == FrameStyle::RaisedContainer)
        dark_shade = palette.threed_shadow2();

    switch (style) {
    case FrameStyle::RaisedContainer:
    case FrameStyle::RaisedBox:
    case FrameStyle::RaisedPanel:
        top_left_color = light_shade;
        bottom_right_color = dark_shade;
        break;
    case FrameStyle::SunkenContainer:
    case FrameStyle::SunkenBox:
    case FrameStyle::SunkenPanel:
        top_left_color = dark_shade;
        bottom_right_color = light_shade;
        break;
    case FrameStyle::Plain:
        top_left_color = dark_shade;
        bottom_right_color = dark_shade;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    painter.draw_line(rect.top_left(), rect.top_right().moved_left(1), top_left_color);
    painter.draw_line(rect.bottom_left().moved_up(1), rect.bottom_right().translated(-1), bottom_right_color);

    if ((style != FrameStyle::SunkenPanel && style != FrameStyle::RaisedPanel) || !skip_vertical_lines) {
        painter.draw_line(rect.top_left().moved_down(1), rect.bottom_left().moved_up(2), top_left_color);
        painter.draw_line(rect.top_right().moved_left(1), rect.bottom_right().translated(-1, -2), bottom_right_color);
    }

    if (style == FrameStyle::RaisedContainer || style == FrameStyle::SunkenContainer) {
        Color top_left_color;
        Color bottom_right_color;
        Color dark_shade = palette.threed_shadow2();
        Color light_shade = palette.button();
        if (style == FrameStyle::RaisedContainer) {
            dark_shade = palette.threed_shadow1();
            top_left_color = light_shade;
            bottom_right_color = dark_shade;
        } else if (style == FrameStyle::SunkenContainer) {
            top_left_color = dark_shade;
            bottom_right_color = light_shade;
        }
        IntRect inner_container_frame_rect = rect.shrunken(2, 2);
        painter.draw_line(inner_container_frame_rect.top_left(), inner_container_frame_rect.top_right().moved_left(1), top_left_color);
        painter.draw_line(inner_container_frame_rect.bottom_left().moved_up(1), inner_container_frame_rect.bottom_right().translated(-1), bottom_right_color);
        painter.draw_line(inner_container_frame_rect.top_left().moved_down(1), inner_container_frame_rect.bottom_left().moved_up(2), top_left_color);
        painter.draw_line(inner_container_frame_rect.top_right().moved_left(1), inner_container_frame_rect.bottom_right().translated(-1, -2), bottom_right_color);
    }

    if (style == FrameStyle::RaisedBox || style == FrameStyle::SunkenBox) {
        swap(top_left_color, bottom_right_color);
        IntRect inner_rect = rect.shrunken(2, 2);
        painter.draw_line(inner_rect.top_left(), inner_rect.top_right().moved_left(1), top_left_color);
        painter.draw_line(inner_rect.bottom_left().moved_up(1), inner_rect.bottom_right().translated(-1), bottom_right_color);
        painter.draw_line(inner_rect.top_left().moved_down(1), inner_rect.bottom_left().moved_up(2), top_left_color);
        painter.draw_line(inner_rect.top_right().moved_left(1), inner_rect.bottom_right().translated(-1, -2), bottom_right_color);
    }
}

void ClassicStylePainter::paint_window_frame(Painter& painter, IntRect const& rect, Palette const& palette)
{
    Color base_color = palette.button();
    Color dark_shade = palette.threed_shadow2();
    Color mid_shade = palette.threed_shadow1();
    Color light_shade = palette.threed_highlight();
    auto border_thickness = palette.window_border_thickness();
    auto border_radius = palette.window_border_radius();

    if (border_radius > 0) {
        // FIXME: This will draw "useless" pixels that'll get drawn over by the window contents.
        // preferably we should just remove the corner pixels from the completely drawn window
        // but I don't know how to do that yet. :^)
        AntiAliasingPainter aa_painter { painter };
        aa_painter.fill_rect_with_rounded_corners(rect, base_color, border_radius);
        return;
    }

    painter.draw_rect_with_thickness({ rect.x() + border_thickness / 2,
                                         rect.y() + border_thickness / 2,
                                         rect.width() - border_thickness,
                                         rect.height() - border_thickness },
        base_color, border_thickness);

    painter.draw_line(rect.top_left().translated(0, 1), rect.bottom_left().translated(0, -1), base_color);
    painter.draw_line(rect.top_left().translated(1, 1), rect.top_right().translated(-2, 1), light_shade);
    painter.draw_line(rect.top_left().translated(1, 1), rect.bottom_left().translated(1, -2), light_shade);
    painter.draw_line(rect.top_left().translated(2, 2), rect.top_right().translated(-3, 2), base_color);
    painter.draw_line(rect.top_left().translated(2, 2), rect.bottom_left().translated(2, -3), base_color);
    painter.draw_line(rect.top_left().translated(3, 3), rect.top_right().translated(-4, 3), base_color);
    painter.draw_line(rect.top_left().translated(3, 3), rect.bottom_left().translated(3, -4), base_color);

    painter.draw_line(rect.top_right().translated(-1, 0), rect.bottom_right().translated(-1, -1), dark_shade);
    painter.draw_line(rect.top_right().translated(-2, 1), rect.bottom_right().translated(-2, -2), mid_shade);
    painter.draw_line(rect.top_right().translated(-3, 2), rect.bottom_right().translated(-3, -3), base_color);
    painter.draw_line(rect.top_right().translated(-4, 3), rect.bottom_right().translated(-4, -4), base_color);
    painter.draw_line(rect.bottom_left().translated(0, -1), rect.bottom_right().translated(-1, -1), dark_shade);
    painter.draw_line(rect.bottom_left().translated(1, -2), rect.bottom_right().translated(-2, -2), mid_shade);
    painter.draw_line(rect.bottom_left().translated(2, -3), rect.bottom_right().translated(-3, -3), base_color);
    painter.draw_line(rect.bottom_left().translated(3, -4), rect.bottom_right().translated(-4, -4), base_color);
}

void ClassicStylePainter::paint_progressbar(Painter& painter, IntRect const& rect, Palette const& palette, int min, int max, int value, StringView text, Orientation orientation)
{
    // First we fill the entire widget with the gradient. This incurs a bit of
    // overdraw but ensures a consistent look throughout the progression.
    Color start_color = palette.active_window_border1();
    Color end_color = palette.active_window_border2();
    painter.fill_rect_with_gradient(orientation, rect, start_color, end_color);

    if (!text.is_null()) {
        painter.draw_text(rect.translated(1, 1), text, TextAlignment::Center, palette.base_text());
        painter.draw_text(rect, text, TextAlignment::Center, palette.base_text().inverted());
    }

    float range_size = max - min;
    float progress = (value - min) / range_size;

    // Then we carve out a hole in the remaining part of the widget.
    // We draw the text a third time, clipped and inverse, for sharp contrast.
    IntRect hole_rect;
    if (orientation == Orientation::Horizontal) {
        float progress_width = progress * rect.width();
        hole_rect = { (int)progress_width, 0, (int)(rect.width() - progress_width), rect.height() };
    } else {
        float progress_height = progress * rect.height();
        hole_rect = { 0, 0, rect.width(), (int)(rect.height() - progress_height) };
    }
    hole_rect.translate_by(rect.location());
    hole_rect.set_right_without_resize(rect.right());
    PainterStateSaver saver(painter);
    painter.fill_rect(hole_rect, palette.base());

    painter.add_clip_rect(hole_rect);
    if (!text.is_null())
        painter.draw_text(rect.translated(0, 0), text, TextAlignment::Center, palette.base_text());
}

void ClassicStylePainter::paint_radio_button(Painter& painter, IntRect const& a_rect, Palette const& palette, bool is_checked, bool is_being_pressed)
{
    // Outer top left arc, starting at bottom left point.
    constexpr Gfx::IntPoint outer_top_left_arc[] = {
        { 1, 9 },
        { 1, 8 },
        { 0, 7 },
        { 0, 6 },
        { 0, 5 },
        { 0, 4 },
        { 1, 3 },
        { 1, 2 },
        { 2, 1 },
        { 3, 1 },
        { 4, 0 },
        { 5, 0 },
        { 6, 0 },
        { 7, 0 },
        { 8, 1 },
        { 9, 1 },
    };

    // Outer bottom right arc, starting at top right point.
    constexpr Gfx::IntPoint outer_bottom_right_arc[] = {
        { 10, 2 },
        { 10, 3 },
        { 11, 4 },
        { 11, 5 },
        { 11, 6 },
        { 11, 7 },
        { 10, 8 },
        { 10, 9 },
        { 9, 10 },
        { 8, 10 },
        { 7, 11 },
        { 6, 11 },
        { 5, 11 },
        { 4, 11 },
        { 3, 10 },
        { 2, 10 },
    };

    // Inner top left arc, starting at bottom left point.
    constexpr Gfx::IntPoint inner_top_left_arc[] = {
        { 2, 8 },
        { 1, 7 },
        { 1, 6 },
        { 1, 5 },
        { 1, 4 },
        { 2, 3 },
        { 2, 2 },
        { 3, 2 },
        { 3, 2 },
        { 3, 2 },
        { 3, 2 },
        { 4, 1 },
        { 5, 1 },
        { 6, 1 },
        { 7, 1 },
        { 8, 2 },
        { 9, 2 },
    };

    // Inner bottom right arc, starting at top right point.
    constexpr Gfx::IntPoint inner_bottom_right_arc[] = {
        { 9, 3 },
        { 10, 4 },
        { 10, 5 },
        { 10, 6 },
        { 10, 7 },
        { 9, 8 },
        { 9, 9 },
        { 8, 9 },
        { 7, 10 },
        { 6, 10 },
        { 5, 10 },
        { 4, 10 },
        { 3, 9 },
        { 2, 9 },
    };

    // Inner "being pressed" circle, starting at top left corner point.
    constexpr Gfx::IntPoint inner_being_pressed_circle[] = {
        { 3, 3 },
        { 4, 2 },
        { 5, 2 },
        { 6, 2 },
        { 7, 2 },
        { 8, 3 },
        { 9, 4 },
        { 9, 5 },
        { 9, 6 },
        { 9, 7 },
        { 8, 8 },
        { 7, 9 },
        { 6, 9 },
        { 5, 9 },
        { 4, 9 },
        { 3, 8 },
        { 2, 7 },
        { 2, 6 },
        { 2, 5 },
        { 2, 4 },
    };

    // Inner "checked" circle, starting at top left.
    constexpr Gfx::IntPoint checked_circle[] = {
        { 5, 4 },
        { 6, 4 },
        { 4, 5 },
        { 5, 5 },
        { 6, 5 },
        { 7, 5 },
        { 4, 6 },
        { 5, 6 },
        { 6, 6 },
        { 7, 6 },
        { 5, 7 },
        { 6, 7 },
    };

    // FIXME: Support radio buttons at any size.
    Gfx::IntRect rect { a_rect.x(), a_rect.y(), 12, 12 };

    auto set_pixels = [&](auto const& points, Gfx::Color color) {
        for (auto const& p : points) {
            painter.set_pixel(rect.location().translated(p), color);
        }
    };

    // Fill center with base color
    painter.fill_rect(rect.shrunken(4, 4), palette.base());

    set_pixels(outer_top_left_arc, palette.threed_shadow1());
    set_pixels(outer_bottom_right_arc, palette.threed_highlight());
    set_pixels(inner_top_left_arc, palette.threed_shadow2());
    set_pixels(inner_bottom_right_arc, palette.button());
    if (is_being_pressed) {
        set_pixels(inner_being_pressed_circle, palette.threed_shadow1());
    }
    if (is_checked) {
        set_pixels(checked_circle, palette.base_text());
    }
}

static constexpr Gfx::CharacterBitmap s_checked_bitmap {
    "         "
    "       # "
    "      ## "
    "     ### "
    " ## ###  "
    " #####   "
    "  ###    "
    "   #     "
    "         "sv,
    9, 9
};

void ClassicStylePainter::paint_check_box(Painter& painter, IntRect const& rect, Palette const& palette, bool is_enabled, bool is_checked, bool is_being_pressed)
{
    painter.fill_rect(rect, is_enabled ? palette.base() : palette.window());
    paint_frame(painter, rect, palette, Gfx::FrameStyle::SunkenContainer);

    if (is_being_pressed) {
        // FIXME: This color should not be hard-coded.
        painter.draw_rect(rect.shrunken(4, 4), Color::MidGray);
    }

    if (is_checked) {
        auto check_rect = Gfx::IntRect({}, s_checked_bitmap.size()).centered_within(rect);
        painter.draw_bitmap(check_rect.location(), s_checked_bitmap, is_enabled ? palette.base_text() : palette.threed_shadow1());
    }
}

void ClassicStylePainter::paint_transparency_grid(Painter& painter, IntRect const& rect, Palette const& palette)
{
    painter.fill_rect_with_checkerboard(rect, { 8, 8 }, palette.base().darkened(0.9), palette.base());
}

void ClassicStylePainter::paint_simple_rect_shadow(Painter& painter, IntRect const& containing_rect, Bitmap const& shadow_bitmap, bool shadow_includes_frame, bool fill_content)
{
    // The layout of the shadow_bitmap is defined like this:
    // +---------+----+---------+----+----+----+
    // |   TL    | T  |   TR    | LT | L  | LB |
    // +---------+----+---------+----+----+----+
    // |   BL    | B  |   BR    | RT | R  | RB |
    // +---------+----+---------+----+----+----+
    // Located strictly on the top or bottom of the rectangle, above or below of the content:
    //   TL = top-left     T = top     TR = top-right
    //   BL = bottom-left  B = bottom  BR = bottom-right
    // Located on the left or right of the rectangle, but not above or below of the content:
    //   LT = left-top     L = left    LB = left-bottom
    //   RT = right-top    R = right   RB = right-bottom
    // So, the bitmap has two rows and 6 column, two of which are twice as wide.
    // The height divided by two defines a cell size, and width of each
    // column must be the same as the height of the cell, except for the
    // first and third column, which are twice as wide.
    // If fill_content is true, it will use the RGBA color of right-bottom pixel of TL to fill the rectangle enclosed
    if (shadow_bitmap.height() % 2 != 0) {
        dbgln("Can't paint simple rect shadow, shadow bitmap height {} is not even", shadow_bitmap.height());
        return;
    }
    auto base_size = shadow_bitmap.height() / 2;
    if (shadow_bitmap.width() != base_size * (6 + 2)) {
        if (shadow_bitmap.width() % base_size != 0)
            dbgln("Can't paint simple rect shadow, shadow bitmap width {} is not a multiple of {}", shadow_bitmap.width(), base_size);
        else
            dbgln("Can't paint simple rect shadow, shadow bitmap width {} but expected {}", shadow_bitmap.width(), base_size * (6 + 2));
        return;
    }

    // The containing_rect should have been inflated appropriately
    VERIFY(containing_rect.size().contains(Gfx::IntSize { base_size, base_size }));

    auto sides_height = containing_rect.height() - 2 * base_size;
    auto half_height = sides_height / 2;
    auto containing_horizontal_rect = containing_rect;

    int horizontal_shift = 0;
    if (half_height < base_size && !shadow_includes_frame) {
        // If the height is too small we need to shift the left/right accordingly, unless the shadow includes portions of the frame
        horizontal_shift = base_size - half_height;
        containing_horizontal_rect.set_left(containing_horizontal_rect.left() + horizontal_shift);
        containing_horizontal_rect.set_right(containing_horizontal_rect.right() - 2 * horizontal_shift);
    }
    auto half_width = containing_horizontal_rect.width() / 2;
    int corner_piece_width = min(containing_horizontal_rect.width() / 2, base_size * 2);
    int left_corners_right = containing_horizontal_rect.left() + corner_piece_width;
    int right_corners_left = max(containing_horizontal_rect.right() - corner_piece_width, left_corners_right + 1);
    auto paint_horizontal = [&](int y, int src_row) {
        if (half_width <= 0)
            return;
        Gfx::PainterStateSaver save(painter);
        painter.add_clip_rect({ containing_horizontal_rect.left(), y, containing_horizontal_rect.width(), base_size });
        painter.blit({ containing_horizontal_rect.left(), y }, shadow_bitmap, { 0, src_row * base_size, corner_piece_width, base_size });
        painter.blit({ right_corners_left, y }, shadow_bitmap, { 5 * base_size - corner_piece_width, src_row * base_size, corner_piece_width, base_size });
        for (int x = left_corners_right; x < right_corners_left; x += base_size) {
            auto width = min(right_corners_left - x, base_size);
            painter.blit({ x, y }, shadow_bitmap, { corner_piece_width, src_row * base_size, width, base_size });
        }
    };

    paint_horizontal(containing_rect.top(), 0);
    paint_horizontal(containing_rect.bottom() - base_size, 1);

    int corner_piece_height = min(half_height, base_size);
    int top_corners_bottom = base_size + corner_piece_height;
    int bottom_corners_top = base_size + max(half_height, sides_height - corner_piece_height);
    auto paint_vertical = [&](int x, int src_row, int hshift, int hsrcshift) {
        Gfx::PainterStateSaver save(painter);
        painter.add_clip_rect({ x, containing_rect.y() + base_size, base_size, containing_rect.height() - 2 * base_size });
        painter.blit({ x + hshift, containing_rect.top() + top_corners_bottom - corner_piece_height }, shadow_bitmap, { base_size * 5 + hsrcshift, src_row * base_size, base_size - hsrcshift, corner_piece_height });
        painter.blit({ x + hshift, containing_rect.top() + bottom_corners_top }, shadow_bitmap, { base_size * 7 + hsrcshift, src_row * base_size + base_size - corner_piece_height, base_size - hsrcshift, corner_piece_height });
        for (int y = top_corners_bottom; y < bottom_corners_top; y += base_size) {
            auto height = min(bottom_corners_top - y, base_size);
            painter.blit({ x, containing_rect.top() + y }, shadow_bitmap, { base_size * 6, src_row * base_size, base_size, height });
        }
    };

    paint_vertical(containing_rect.left(), 0, horizontal_shift, 0);
    if (shadow_includes_frame)
        horizontal_shift = 0; // TODO: fix off-by-one on rectangles barely wide enough
    paint_vertical(containing_rect.right() - base_size, 1, 0, horizontal_shift);

    if (fill_content) {
        // Fill the enclosed rectangle with the RGBA color of the right-bottom pixel of the TL tile
        auto inner_rect = containing_rect.shrunken(2 * base_size, 2 * base_size);
        if (!inner_rect.is_empty())
            painter.fill_rect(inner_rect, shadow_bitmap.get_pixel(2 * base_size - 1, base_size - 1));
    }
}

}
