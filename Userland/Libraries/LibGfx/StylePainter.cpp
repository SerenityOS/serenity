/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibGfx/ClassicStylePainter.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

namespace Gfx {

BaseStylePainter& StylePainter::current()
{
    static ClassicStylePainter style;
    return style;
}

void StylePainter::paint_tab_button(Painter& painter, IntRect const& rect, Palette const& palette, bool active, bool hovered, bool enabled, TabPosition position, bool in_active_window, bool accented)
{
    current().paint_tab_button(painter, rect, palette, active, hovered, enabled, position, in_active_window, accented);
}

void StylePainter::paint_button(Painter& painter, IntRect const& rect, Palette const& palette, ButtonStyle button_style, bool pressed, bool hovered, bool checked, bool enabled, bool focused, bool default_button)
{
    current().paint_button(painter, rect, palette, button_style, pressed, hovered, checked, enabled, focused, default_button);
}

void StylePainter::paint_frame(Painter& painter, IntRect const& rect, Palette const& palette, FrameStyle style, bool skip_vertical_lines)
{
    current().paint_frame(painter, rect, palette, style, skip_vertical_lines);
}

void StylePainter::paint_window_frame(Painter& painter, IntRect const& rect, Palette const& palette)
{
    current().paint_window_frame(painter, rect, palette);
}

void StylePainter::paint_progressbar(Painter& painter, IntRect const& rect, Palette const& palette, int min, int max, int value, StringView text, Orientation orientation)
{
    current().paint_progressbar(painter, rect, palette, min, max, value, text, orientation);
}

void StylePainter::paint_radio_button(Painter& painter, IntRect const& rect, Palette const& palette, bool is_checked, bool is_being_pressed)
{
    current().paint_radio_button(painter, rect, palette, is_checked, is_being_pressed);
}

void StylePainter::paint_check_box(Painter& painter, IntRect const& rect, Palette const& palette, bool is_enabled, bool is_checked, bool is_being_pressed)
{
    current().paint_check_box(painter, rect, palette, is_enabled, is_checked, is_being_pressed);
}

void StylePainter::paint_transparency_grid(Painter& painter, IntRect const& rect, Palette const& palette)
{
    current().paint_transparency_grid(painter, rect, palette);
}

void StylePainter::paint_simple_rect_shadow(Painter& painter, IntRect const& rect, Bitmap const& shadow_bitmap, bool shadow_includes_frame, bool fill_content)
{
    current().paint_simple_rect_shadow(painter, rect, shadow_bitmap, shadow_includes_frame, fill_content);
}

}
