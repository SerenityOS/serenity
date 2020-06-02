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
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

KeyButton::~KeyButton()
{
}

void KeyButton::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto content_rect = rect().shrunken(5, 5);
    auto& font = this->font();

    Gfx::StylePainter::paint_button(painter, rect(), palette(), Gfx::ButtonStyle::Normal, is_being_pressed(), is_hovered(), is_checked(), true);

    if (m_pressed)
        painter.fill_rect(content_rect, Color::Cyan);
    else if (!is_enabled())
        painter.fill_rect(content_rect, Color::from_rgb(0x8C7272));

    if (!text().is_empty()) {
        Gfx::Rect text_rect { 0, 0, font.width(text()), font.glyph_height() };
        text_rect.align_within(content_rect, Gfx::TextAlignment::Center);

        auto clipped_rect = rect().intersected(this->rect());

        painter.draw_text(clipped_rect, text(), font, Gfx::TextAlignment::Center, palette().button_text(), Gfx::TextElision::Right);
        if (is_focused())
            painter.draw_rect(clipped_rect.inflated(6, 4), palette().focus_outline());
    }
}

void KeyButton::click(unsigned)
{
    if (on_click)
        on_click();
}
