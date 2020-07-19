/*
 * Copyright (c) 2020, Charles Chaucer <thankyouverycool@github>
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

#include <LibGUI/ControlBoxButton.h>
#include <LibGUI/Painter.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Palette.h>

namespace GUI {

static const char* s_up_arrow_bitmap_data = {
    "         "
    "    #    "
    "   ###   "
    "  #####  "
    " ####### "
    "         "
};

static const char* s_down_arrow_bitmap_data = {
    "         "
    " ####### "
    "  #####  "
    "   ###   "
    "    #    "
    "         "
};

static Gfx::CharacterBitmap* s_up_arrow_bitmap;
static Gfx::CharacterBitmap* s_down_arrow_bitmap;
static const int s_bitmap_width = 9;
static const int s_bitmap_height = 6;

ControlBoxButton::ControlBoxButton(Type type)
    : m_type(type)
{
}

ControlBoxButton::~ControlBoxButton()
{
}

void ControlBoxButton::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    Gfx::StylePainter::paint_button(painter, rect(), palette(), Gfx::ButtonStyle::Normal, is_being_pressed(), is_hovered(), is_checked(), is_enabled());

    auto button_location = rect().location().translated((width() - s_bitmap_width) / 2, (height() - s_bitmap_height) / 2);

    if (is_being_pressed())
        button_location.move_by(1, 1);

    if (type() == UpArrow) {
        if (!s_up_arrow_bitmap)
            s_up_arrow_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_up_arrow_bitmap_data, s_bitmap_width, s_bitmap_height).leak_ref();
        painter.draw_bitmap(button_location, *s_up_arrow_bitmap, is_enabled() ? palette().button_text() : palette().threed_shadow1());
    } else {
        if (!s_down_arrow_bitmap)
            s_down_arrow_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_down_arrow_bitmap_data, s_bitmap_width, s_bitmap_height).leak_ref();
        painter.draw_bitmap(button_location, *s_down_arrow_bitmap, is_enabled() ? palette().button_text() : palette().threed_shadow1());
    }
}

}
