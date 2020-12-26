/*
 * Copyright (c) 2020, Alex McGrath <amk@amk.ie>
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

#include <LibGUI/Event.h>
#include <LibGUI/LinkLabel.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

namespace GUI {

LinkLabel::LinkLabel(String text)
    : Label(move(text))
{
    set_foreground_role(Gfx::ColorRole::Link);
}

void LinkLabel::mousedown_event(MouseEvent&)
{
    if (on_click) {
        on_click();
    }
}

void LinkLabel::paint_event(PaintEvent& event)
{
    Label::paint_event(event);
    GUI::Painter painter(*this);

    if (m_hovered)
        painter.draw_line({ 0, rect().bottom() }, { font().width(text()), rect().bottom() },
            Widget::palette().link());
}

void LinkLabel::enter_event(Core::Event&)
{
    m_hovered = true;
    update();
}

void LinkLabel::leave_event(Core::Event&)
{
    m_hovered = false;
    update();
}

void LinkLabel::second_paint_event(PaintEvent&)
{
    if (window()->width() < font().width(text())) {
        set_tooltip(text());
    }
}

void LinkLabel::resize_event(ResizeEvent&)
{
    if (window()->width() < font().width(text())) {
        set_tooltip(text());
    } else {
        set_tooltip({});
    }
}

}
