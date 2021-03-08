/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/Painter.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, HorizontalSeparator)
REGISTER_WIDGET(GUI, VerticalSeparator)

namespace GUI {

SeparatorWidget::SeparatorWidget(Gfx::Orientation orientation)
    : m_orientation(orientation)
{
    if (m_orientation == Gfx::Orientation::Vertical)
        set_fixed_width(8);
    else
        set_fixed_height(8);
}

SeparatorWidget::~SeparatorWidget()
{
}

void SeparatorWidget::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (m_orientation == Gfx::Orientation::Vertical) {
        painter.translate(rect().center().x() - 1, 0);
        painter.draw_line({ 0, 0 }, { 0, rect().bottom() }, palette().threed_shadow1());
        painter.draw_line({ 1, 0 }, { 1, rect().bottom() }, palette().threed_highlight());
    } else {
        painter.translate(0, rect().center().y() - 1);
        painter.draw_line({ 0, 0 }, { rect().right(), 0 }, palette().threed_shadow1());
        painter.draw_line({ 0, 1 }, { rect().right(), 1 }, palette().threed_highlight());
    }
}

}
