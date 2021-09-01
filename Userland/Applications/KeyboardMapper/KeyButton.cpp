/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeyButton.h"
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

        painter.draw_text(text_rect, text(), font, Gfx::TextAlignment::Center, Color::Black, Gfx::TextElision::Right);
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
