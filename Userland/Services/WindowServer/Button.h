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

#include <AK/Function.h>
#include <AK/Weakable.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>

namespace WindowServer {

class MouseEvent;
class WindowFrame;

class Button : public Weakable<Button> {
public:
    Button(WindowFrame&, Function<void(Button&)>&& on_click_handler);
    ~Button();

    Gfx::IntRect relative_rect() const { return m_relative_rect; }
    void set_relative_rect(const Gfx::IntRect& rect) { m_relative_rect = rect; }

    Gfx::IntRect rect() const { return { {}, m_relative_rect.size() }; }
    Gfx::IntRect screen_rect() const;

    void paint(Gfx::Painter&);

    void on_mouse_event(const MouseEvent&);

    Function<void(Button&)> on_click;

    bool is_visible() const { return m_visible; }

    void set_icon(const Gfx::Bitmap& icon) { m_icon = icon; }

private:
    WindowFrame& m_frame;
    Gfx::IntRect m_relative_rect;
    RefPtr<Gfx::Bitmap> m_icon;
    bool m_pressed { false };
    bool m_visible { true };
    bool m_hovered { false };
};

}
