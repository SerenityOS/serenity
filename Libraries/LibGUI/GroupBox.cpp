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

#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Painter.h>

namespace GUI {

GroupBox::GroupBox(Widget* parent)
    : GroupBox({}, parent)
{
}

GroupBox::GroupBox(const StringView& title, Widget* parent)
    : Widget(parent)
    , m_title(title)
{
}

GroupBox::~GroupBox()
{
}

void GroupBox::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    Gfx::Rect frame_rect {
        0, font().glyph_height() / 2,
        width(), height() - font().glyph_height() / 2
    };
    Gfx::StylePainter::paint_frame(painter, frame_rect, palette(), Gfx::FrameShape::Box, Gfx::FrameShadow::Sunken, 2);

    Gfx::Rect text_rect { 4, 0, font().width(m_title) + 6, font().glyph_height() };
    painter.fill_rect(text_rect, palette().button());
    painter.draw_text(text_rect, m_title, Gfx::TextAlignment::Center, palette().button_text());
}

void GroupBox::set_title(const StringView& title)
{
    if (m_title == title)
        return;
    m_title = title;
    update();
}

}
