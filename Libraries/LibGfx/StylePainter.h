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

#pragma once

#include <AK/Forward.h>
#include <LibGfx/Forward.h>

namespace Gfx {

enum class ButtonStyle {
    Normal,
    CoolBar
};
enum class FrameShadow {
    Plain,
    Raised,
    Sunken
};
enum class FrameShape {
    NoFrame,
    Box,
    Container,
    Panel,
    VerticalLine,
    HorizontalLine
};

// FIXME: should this be in its own header?
class BaseStylePainter {
public:
    virtual ~BaseStylePainter() { }

    virtual void paint_button(Painter&, const IntRect&, const Palette&, ButtonStyle, bool pressed, bool hovered = false, bool checked = false, bool enabled = true) = 0;
    virtual void paint_tab_button(Painter&, const IntRect&, const Palette&, bool active, bool hovered, bool enabled, bool top) = 0;
    virtual void paint_surface(Painter&, const IntRect&, const Palette&, bool paint_vertical_lines = true, bool paint_top_line = true) = 0;
    virtual void paint_frame(Painter&, const IntRect&, const Palette&, FrameShape, FrameShadow, int thickness, bool skip_vertical_lines = false) = 0;
    virtual void paint_window_frame(Painter&, const IntRect&, const Palette&) = 0;
    virtual void paint_progress_bar(Painter&, const IntRect&, const Palette&, int min, int max, int value, const StringView& text) = 0;
    virtual void paint_radio_button(Painter&, const IntRect&, const Palette&, bool is_checked, bool is_being_pressed) = 0;
    virtual void paint_check_box(Painter&, const IntRect&, const Palette&, bool is_enabled, bool is_checked, bool is_being_pressed) = 0;
    virtual void paint_transparency_grid(Painter&, const IntRect&, const Palette&) = 0;

protected:
    BaseStylePainter() { }
};

class StylePainter {
public:
    static BaseStylePainter& current();

    // FIXME: These are here for API compatibility, we should probably remove them and move BaseStylePainter into here
    static void paint_button(Painter&, const IntRect&, const Palette&, ButtonStyle, bool pressed, bool hovered = false, bool checked = false, bool enabled = true);
    static void paint_tab_button(Painter&, const IntRect&, const Palette&, bool active, bool hovered, bool enabled, bool top);
    static void paint_surface(Painter&, const IntRect&, const Palette&, bool paint_vertical_lines = true, bool paint_top_line = true);
    static void paint_frame(Painter&, const IntRect&, const Palette&, FrameShape, FrameShadow, int thickness, bool skip_vertical_lines = false);
    static void paint_window_frame(Painter&, const IntRect&, const Palette&);
    static void paint_progress_bar(Painter&, const IntRect&, const Palette&, int min, int max, int value, const StringView& text);
    static void paint_radio_button(Painter&, const IntRect&, const Palette&, bool is_checked, bool is_being_pressed);
    static void paint_check_box(Painter&, const IntRect&, const Palette&, bool is_enabled, bool is_checked, bool is_being_pressed);
    static void paint_transparency_grid(Painter&, const IntRect&, const Palette&);
};

}
