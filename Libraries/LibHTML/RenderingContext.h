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

#include <LibDraw/Palette.h>
#include <LibDraw/Rect.h>

namespace GUI {
class Painter;
}

class RenderingContext {
public:
    explicit RenderingContext(GUI::Painter& painter, const Palette& palette)
        : m_painter(painter)
        , m_palette(palette)
    {
    }

    GUI::Painter& painter() const { return m_painter; }
    const Palette& palette() const { return m_palette; }

    bool should_show_line_box_borders() const { return m_should_show_line_box_borders; }
    void set_should_show_line_box_borders(bool value) { m_should_show_line_box_borders = value; }

    Rect viewport_rect() const { return m_viewport_rect; }
    void set_viewport_rect(const Gfx::Rect& rect) { m_viewport_rect = rect; }

private:
    GUI::Painter& m_painter;
    Palette m_palette;
    Gfx::Rect m_viewport_rect;
    bool m_should_show_line_box_borders { false };
};
