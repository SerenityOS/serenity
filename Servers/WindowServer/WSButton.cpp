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

#include <LibDraw/CharacterBitmap.h>
#include <LibDraw/Painter.h>
#include <LibDraw/StylePainter.h>
#include <WindowServer/WSButton.h>
#include <WindowServer/WSEvent.h>
#include <WindowServer/WSWindowManager.h>

WSButton::WSButton(WSWindowFrame& frame, NonnullRefPtr<Gfx::CharacterBitmap>&& bitmap, Function<void(WSButton&)>&& on_click_handler)
    : on_click(move(on_click_handler))
    , m_frame(frame)
    , m_bitmap(move(bitmap))
{
}

WSButton::~WSButton()
{
}

void WSButton::paint(Gfx::Painter& painter)
{
    auto palette = WSWindowManager::the().palette();
    Gfx::PainterStateSaver saver(painter);
    painter.translate(relative_rect().location());
    Gfx::StylePainter::paint_button(painter, rect(), palette, Gfx::ButtonStyle::Normal, m_pressed, m_hovered);
    auto x_location = rect().center();
    x_location.move_by(-(m_bitmap->width() / 2), -(m_bitmap->height() / 2));
    if (m_pressed)
        x_location.move_by(1, 1);
    painter.draw_bitmap(x_location, *m_bitmap, palette.button_text());
}

void WSButton::on_mouse_event(const WSMouseEvent& event)
{
    auto& wm = WSWindowManager::the();

    if (event.type() == WSEvent::MouseDown && event.button() == MouseButton::Left) {
        m_pressed = true;
        wm.set_cursor_tracking_button(this);
        wm.invalidate(screen_rect());
        return;
    }

    if (event.type() == WSEvent::MouseUp && event.button() == MouseButton::Left) {
        if (wm.cursor_tracking_button() != this)
            return;
        wm.set_cursor_tracking_button(nullptr);
        bool old_pressed = m_pressed;
        m_pressed = false;
        if (rect().contains(event.position())) {
            if (on_click)
                on_click(*this);
        }
        if (old_pressed != m_pressed)
            wm.invalidate(screen_rect());
        return;
    }

    if (event.type() == WSEvent::MouseMove) {
        bool old_hovered = m_hovered;
        m_hovered = rect().contains(event.position());
        wm.set_hovered_button(m_hovered ? this : nullptr);
        if (old_hovered != m_hovered)
            wm.invalidate(screen_rect());
    }

    if (event.type() == WSEvent::MouseMove && event.buttons() & (unsigned)MouseButton::Left) {
        if (wm.cursor_tracking_button() != this)
            return;
        bool old_pressed = m_pressed;
        m_pressed = m_hovered;
        if (old_pressed != m_pressed)
            wm.invalidate(screen_rect());
    }
}

Gfx::Rect WSButton::screen_rect() const
{
    return m_relative_rect.translated(m_frame.rect().location());
}
