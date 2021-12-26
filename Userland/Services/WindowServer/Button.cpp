/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <WindowServer/Button.h>
#include <WindowServer/Event.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowManager.h>

namespace WindowServer {

Button::Button(WindowFrame& frame, Function<void(Button&)>&& on_click_handler)
    : on_click(move(on_click_handler))
    , m_frame(frame)
{
}

Button::~Button()
{
}

void Button::paint(Screen& screen, Gfx::Painter& painter)
{
    auto palette = WindowManager::the().palette();
    Gfx::PainterStateSaver saver(painter);
    painter.translate(relative_rect().location());
    Gfx::StylePainter::paint_button(painter, rect(), palette, Gfx::ButtonStyle::Normal, m_pressed, m_hovered);

    if (m_icon) {
        auto& bitmap = m_icon->bitmap(screen.scale_factor());
        auto icon_location = rect().center().translated(-(bitmap.width() / 2), -(bitmap.height() / 2));
        if (m_pressed)
            painter.translate(1, 1);
        painter.blit(icon_location, bitmap, bitmap.rect());
    }
}

void Button::on_mouse_event(const MouseEvent& event)
{
    auto interesting_button = false;

    switch (event.button()) {
    case MouseButton::Left:
        interesting_button = !!on_click;
        break;
    case MouseButton::Middle:
        interesting_button = !!on_middle_click;
        break;
    case MouseButton::Right:
        interesting_button = !!on_right_click;
        break;
    default:
        break;
    }

    if (event.type() != Event::Type::MouseMove && !interesting_button)
        return;

    auto& wm = WindowManager::the();

    if (event.type() == Event::MouseDown && (event.button() == MouseButton::Left || event.button() == MouseButton::Right || event.button() == MouseButton::Middle)) {
        m_pressed = true;
        wm.set_cursor_tracking_button(this);
        m_frame.invalidate(m_relative_rect);
        return;
    }

    if (event.type() == Event::MouseUp && (event.button() == MouseButton::Left || event.button() == MouseButton::Right || event.button() == MouseButton::Middle)) {
        if (wm.cursor_tracking_button() != this)
            return;
        wm.set_cursor_tracking_button(nullptr);
        bool old_pressed = m_pressed;
        m_pressed = false;
        if (rect().contains(event.position())) {
            switch (event.button()) {
            case MouseButton::Left:
                if (on_click)
                    on_click(*this);
                break;

            case MouseButton::Right:
                if (on_right_click)
                    on_right_click(*this);
                break;

            default:
                if (on_middle_click)
                    on_middle_click(*this);
                break;
            }
        }
        if (old_pressed != m_pressed) {
            // Would like to compute:
            // m_hovered = rect_after_action().contains(event.position());
            // However, we don't know that rect yet. We can make an educated
            // guess which also looks okay even when wrong:
            m_hovered = false;
            m_frame.invalidate(m_relative_rect);
        }
        return;
    }

    if (event.type() == Event::MouseMove) {
        bool old_hovered = m_hovered;
        m_hovered = rect().contains(event.position());
        wm.set_hovered_button(m_hovered ? this : nullptr);
        if (old_hovered != m_hovered)
            m_frame.invalidate(m_relative_rect);
    }

    if (event.type() == Event::MouseMove && event.buttons() & (unsigned)MouseButton::Left) {
        if (wm.cursor_tracking_button() != this)
            return;
        bool old_pressed = m_pressed;
        m_pressed = m_hovered;
        if (old_pressed != m_pressed)
            m_frame.invalidate(m_relative_rect);
    }
}

Gfx::IntRect Button::screen_rect() const
{
    return m_relative_rect.translated(m_frame.rect().location());
}

}
