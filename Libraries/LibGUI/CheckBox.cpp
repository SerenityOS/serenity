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

#include <LibGUI/CheckBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

namespace GUI {

static const int s_box_width = 13;
static const int s_box_height = 13;

CheckBox::CheckBox(const StringView& text)
    : AbstractButton(text)
{
}

CheckBox::~CheckBox()
{
}

void CheckBox::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto text_rect = rect();
    text_rect.set_left(s_box_width + 4);
    text_rect.set_width(font().width(text()));
    text_rect.set_top(height() / 2 - font().glyph_height() / 2);
    text_rect.set_height(font().glyph_height());

    if (fill_with_background_color())
        painter.fill_rect(rect(), palette().window());

    if (is_enabled() && is_hovered())
        painter.fill_rect(rect(), palette().hover_highlight());

    Gfx::IntRect box_rect {
        0, height() / 2 - s_box_height / 2 - 1,
        s_box_width, s_box_height
    };

    Gfx::StylePainter::paint_check_box(painter, box_rect, palette(), is_enabled(), is_checked(), is_being_pressed());

    paint_text(painter, text_rect, font(), Gfx::TextAlignment::TopLeft);
}

void CheckBox::click(unsigned)
{
    if (!is_enabled())
        return;
    set_checked(!is_checked());
}

}
