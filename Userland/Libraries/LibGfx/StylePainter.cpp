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

void StylePainter::paint_tab_button(Painter& painter, const IntRect& rect, const Palette& palette, bool active, bool hovered, bool enabled, bool top)
{
    current().paint_tab_button(painter, rect, palette, active, hovered, enabled, top);
}

void StylePainter::paint_button(Painter& painter, const IntRect& rect, const Palette& palette, ButtonStyle button_style, bool pressed, bool hovered, bool checked, bool enabled, bool focused)
{
    current().paint_button(painter, rect, palette, button_style, pressed, hovered, checked, enabled, focused);
}

void StylePainter::paint_frame(Painter& painter, const IntRect& rect, const Palette& palette, FrameShape shape, FrameShadow shadow, int thickness, bool skip_vertical_lines)
{
    current().paint_frame(painter, rect, palette, shape, shadow, thickness, skip_vertical_lines);
}

void StylePainter::paint_window_frame(Painter& painter, const IntRect& rect, const Palette& palette)
{
    current().paint_window_frame(painter, rect, palette);
}

void StylePainter::paint_progressbar(Painter& painter, const IntRect& rect, const Palette& palette, int min, int max, int value, const StringView& text, Orientation orientation)
{
    current().paint_progressbar(painter, rect, palette, min, max, value, text, orientation);
}

void StylePainter::paint_radio_button(Painter& painter, const IntRect& rect, const Palette& palette, bool is_checked, bool is_being_pressed)
{
    current().paint_radio_button(painter, rect, palette, is_checked, is_being_pressed);
}

void StylePainter::paint_check_box(Painter& painter, const IntRect& rect, const Palette& palette, bool is_enabled, bool is_checked, bool is_being_pressed)
{
    current().paint_check_box(painter, rect, palette, is_enabled, is_checked, is_being_pressed);
}

void StylePainter::paint_transparency_grid(Painter& painter, const IntRect& rect, const Palette& palette)
{
    current().paint_transparency_grid(painter, rect, palette);
}

}
