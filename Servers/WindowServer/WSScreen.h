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

#pragma once

#include <Kernel/KeyCode.h>
#include <LibGfx/Color.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>

struct MousePacket;

class WSScreen {
public:
    WSScreen(unsigned width, unsigned height);
    ~WSScreen();

    void set_resolution(int width, int height);
    bool can_set_buffer() { return m_can_set_buffer; }
    void set_buffer(int index);

    int width() const { return m_width; }
    int height() const { return m_height; }
    size_t pitch() const { return m_pitch; }
    Gfx::RGBA32* scanline(int y);

    static WSScreen& the();

    Gfx::Size size() const { return { width(), height() }; }
    Gfx::Rect rect() const { return { 0, 0, width(), height() }; }

    Gfx::Point cursor_location() const { return m_cursor_location; }
    unsigned mouse_button_state() const { return m_mouse_button_state; }

    void on_receive_mouse_data(const MousePacket&);
    void on_receive_keyboard_data(KeyEvent);

private:
    void on_change_resolution(int pitch, int width, int height);

    size_t m_size_in_bytes;

    Gfx::RGBA32* m_framebuffer { nullptr };
    bool m_can_set_buffer { false };

    int m_pitch { 0 };
    int m_width { 0 };
    int m_height { 0 };
    int m_framebuffer_fd { -1 };

    Gfx::Point m_cursor_location;
    unsigned m_mouse_button_state { 0 };
    unsigned m_modifiers { 0 };
};

inline Gfx::RGBA32* WSScreen::scanline(int y)
{
    return reinterpret_cast<Gfx::RGBA32*>(((u8*)m_framebuffer) + (y * m_pitch));
}
