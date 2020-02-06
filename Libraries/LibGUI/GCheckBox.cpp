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

#include <Kernel/KeyCode.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>
#include <LibGUI/GCheckBox.h>
#include <LibGUI/GPainter.h>

namespace GUI {

static const char* s_checked_bitmap_data = {
    "         "
    "       # "
    "      ## "
    "     ### "
    " ## ###  "
    " #####   "
    "  ###    "
    "   #     "
    "         "
};

static Gfx::CharacterBitmap* s_checked_bitmap;
static const int s_checked_bitmap_width = 9;
static const int s_checked_bitmap_height = 9;
static const int s_box_width = 13;
static const int s_box_height = 13;

CheckBox::CheckBox(Widget* parent)
    : AbstractButton(parent)
{
}

CheckBox::CheckBox(const StringView& text, Widget* parent)
    : AbstractButton(text, parent)
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

    Gfx::Rect box_rect {
        0, height() / 2 - s_box_height / 2 - 1,
        s_box_width, s_box_height
    };
    painter.fill_rect(box_rect, palette().base());
    Gfx::StylePainter::paint_frame(painter, box_rect, palette(), Gfx::FrameShape::Container, Gfx::FrameShadow::Sunken, 2);

    if (is_being_pressed())
        painter.draw_rect(box_rect.shrunken(4, 4), Color::MidGray);

    if (is_checked()) {
        if (!s_checked_bitmap)
            s_checked_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_checked_bitmap_data, s_checked_bitmap_width, s_checked_bitmap_height).leak_ref();
        painter.draw_bitmap(box_rect.shrunken(4, 4).location(), *s_checked_bitmap, palette().base_text());
    }

    paint_text(painter, text_rect, font(), Gfx::TextAlignment::TopLeft);
}

void CheckBox::click()
{
    if (!is_enabled())
        return;
    set_checked(!is_checked());
}

}
