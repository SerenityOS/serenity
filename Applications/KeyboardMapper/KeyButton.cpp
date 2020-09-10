/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
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

#include "KeyButton.h"
#include <LibGUI/Button.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

KeyButton::~KeyButton()
{
}

void KeyButton::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto cont_rect = rect();
    auto& font = this->font();

    Color color;
    if (m_pressed) {
        color = Color::Cyan;
    } else if (!is_enabled()) {
        color = Color::LightGray;
    } else {
        color = Color::White;
    }

    painter.fill_rect(cont_rect, Color::Black);
    painter.fill_rect({ cont_rect.x() + 1, cont_rect.y() + 1, cont_rect.width() - 2, cont_rect.height() - 2 }, Color::from_rgb(0x999999));
    painter.fill_rect({ cont_rect.x() + 6, cont_rect.y() + 3, cont_rect.width() - 12, cont_rect.height() - 12 }, Color::from_rgb(0x8C7272));
    painter.fill_rect({ cont_rect.x() + 7, cont_rect.y() + 4, cont_rect.width() - 14, cont_rect.height() - 14 }, color);

    if (!text().is_empty()) {
        Gfx::IntRect text_rect { 0, 0, font.width(text()), font.glyph_height() };
        text_rect.align_within({ cont_rect.x() + 7, cont_rect.y() + 4, cont_rect.width() - 14, cont_rect.height() - 14 }, Gfx::TextAlignment::Center);

        painter.draw_text(text_rect, text(), font, Gfx::TextAlignment::Center, palette().button_text(), Gfx::TextElision::Right);
        if (is_focused())
            painter.draw_rect(text_rect.inflated(6, 4), palette().focus_outline());
    }
}

void KeyButton::click(unsigned)
{
    if (on_click)
        on_click();
}

void KeyButton::mousemove_event(GUI::MouseEvent& event)
{
    if (!is_enabled())
        return;

    Gfx::IntRect c = { rect().x() + 7, rect().y() + 4, rect().width() - 14, rect().height() - 14 };

    if (c.contains(event.position())) {
        window()->set_cursor(Gfx::StandardCursor::Hand);
        return;
    }
    window()->set_cursor(Gfx::StandardCursor::Arrow);

    AbstractButton::mousemove_event(event);
}

void KeyButton::leave_event(Core::Event& event)
{
    window()->set_cursor(Gfx::StandardCursor::Arrow);
    AbstractButton::leave_event(event);
}
