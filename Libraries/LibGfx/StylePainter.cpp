/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

void StylePainter::paint_button(Painter& painter, const IntRect& rect, const Palette& palette, ButtonStyle button_style, bool pressed, bool hovered, bool checked, bool enabled)
{
    current().paint_button(painter, rect, palette, button_style, pressed, hovered, checked, enabled);
}

void StylePainter::paint_surface(Painter& painter, const IntRect& rect, const Palette& palette, bool paint_vertical_lines, bool paint_top_line)
{
    current().paint_surface(painter, rect, palette, paint_vertical_lines, paint_top_line);
}

void StylePainter::paint_frame(Painter& painter, const IntRect& rect, const Palette& palette, FrameShape shape, FrameShadow shadow, int thickness, bool skip_vertical_lines)
{
    current().paint_frame(painter, rect, palette, shape, shadow, thickness, skip_vertical_lines);
}

void StylePainter::paint_window_frame(Painter& painter, const IntRect& rect, const Palette& palette)
{
    current().paint_window_frame(painter, rect, palette);
}

void StylePainter::paint_progress_bar(Painter& painter, const IntRect& rect, const Palette& palette, int min, int max, int value, const StringView& text)
{
    current().paint_progress_bar(painter, rect, palette, min, max, value, text);
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
