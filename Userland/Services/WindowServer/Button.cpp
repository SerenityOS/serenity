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

#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <WindowServer/Button.h>
#include <WindowServer/Event.h>
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

void Button::paint(Gfx::Painter& painter)
{
    auto palette = WindowManager::the().palette();
    Gfx::PainterStateSaver saver(painter);
    painter.translate(relative_rect().location());
    Gfx::StylePainter::paint_button(painter, rect(), palette, Gfx::ButtonStyle::Normal, m_pressed, m_hovered);

    if (m_icon) {
        auto icon_location = rect().center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2));
        if (m_pressed)
            painter.translate(1, 1);
        painter.blit(icon_location, *m_icon, m_icon->rect());
    }
}

void Button::on_mouse_event(const MouseEvent& event)
{
    auto& wm = WindowManager::the();

    if (event.type() == Event::MouseDown && event.button() == MouseButton::Left) {
        m_pressed = true;
        wm.set_cursor_tracking_button(this);
        m_frame.invalidate(m_relative_rect);
        return;
    }

    if (event.type() == Event::MouseUp && event.button() == MouseButton::Left) {
        if (wm.cursor_tracking_button() != this)
            return;
        wm.set_cursor_tracking_button(nullptr);
        bool old_pressed = m_pressed;
        m_pressed = false;
        if (rect().contains(event.position())) {
            if (on_click)
                on_click(*this);
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
