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

    Color face_color;
    if (m_pressed) {
        face_color = Color::Cyan;
    } else if (!is_enabled()) {
        face_color = Color::LightGray;
    } else {
        face_color = Color::White;
    }

    Gfx::IntRect key_cap_side_rect = { cont_rect.x() + 1, cont_rect.y() + 1, cont_rect.width() - 2, cont_rect.height() - 2 };
    Gfx::IntRect key_cap_face_border_rect = { cont_rect.x() + 6, cont_rect.y() + 3, cont_rect.width() - 12, cont_rect.height() - 12 };
    Gfx::IntRect key_cap_face_rect = { cont_rect.x() + 7, cont_rect.y() + 4, cont_rect.width() - 14, cont_rect.height() - 14 };

    painter.draw_rect(cont_rect, Color::Black); // Key cap border
    painter.fill_rect(key_cap_side_rect, Color::from_rgb(0x999999));
    painter.draw_rect(key_cap_face_border_rect, Color::from_rgb(0x8C7272), false);
    painter.fill_rect(key_cap_face_rect, face_color);

    if (text().is_empty() || text().starts_with('\0'))
        return;

    Gfx::IntRect text_rect { 0, 0, font.width(text()), font.glyph_height() };
    text_rect.align_within(key_cap_face_rect, Gfx::TextAlignment::Center);

    painter.draw_text(text_rect, text(), font, Gfx::TextAlignment::Center, Color::Black, Gfx::TextElision::Right);
    if (is_focused())
        painter.draw_rect(text_rect.inflated(6, 4), palette().focus_outline());
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

    Gfx::IntRect key_cap_face_rect = { rect().x() + 7, rect().y() + 4, rect().width() - 14, rect().height() - 14 };

    if (key_cap_face_rect.contains(event.position())) {
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
