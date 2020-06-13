/*
 * Copyright (c) 2020, Hüseyin ASLITÜRK <asliturk@hotmail.com>
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

#include <AK/URL.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/LinkLabel.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

namespace GUI {

LinkLabel::LinkLabel(const StringView& text)
{
    set_text(text);
    set_foreground_role(Gfx::ColorRole::Link);

    set_frame_thickness(0);
    set_frame_shadow(Gfx::FrameShadow::Plain);
    set_frame_shape(Gfx::FrameShape::NoFrame);

    m_href = text;
}

LinkLabel::~LinkLabel()
{
}

void LinkLabel::mousedown_event(MouseEvent&)
{
    URL url = URL::create_with_url_or_path(m_href);
    bool result = Desktop::Launcher::open(url);
    if (!result) {
        dbg() << String::format("Failed to open '%s'", url.path().characters());
    }
}

void LinkLabel::enter_event(Core::Event& event)
{
    window()->set_override_cursor(StandardCursor::Hand);
    Label::enter_event(event);
}

void LinkLabel::leave_event(Core::Event& event)
{
    window()->set_override_cursor(StandardCursor::Arrow);
    Label::leave_event(event);
}

void LinkLabel::paint_event(PaintEvent& event)
{
    Label::paint_event(event);

    Painter painter(*this);

    auto& font = this->font();
    auto link_rect = frame_inner_rect();
    link_rect.set_width(font.width(text()));

    auto start_point = link_rect.bottom_left();
    auto end_point = link_rect.bottom_right();

    painter.draw_line(start_point, end_point, text_color());
}
}
