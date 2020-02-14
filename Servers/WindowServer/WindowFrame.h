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

#include <AK/Forward.h>
#include <AK/NonnullOwnPtrVector.h>
#include <LibGfx/Forward.h>

namespace WindowServer {

class Button;
class MouseEvent;
class Window;

class WindowFrame {
public:
    WindowFrame(Window&);
    ~WindowFrame();

    Gfx::Rect rect() const;
    void paint(Gfx::Painter&);
    void on_mouse_event(const MouseEvent&);
    void notify_window_rect_changed(const Gfx::Rect& old_rect, const Gfx::Rect& new_rect);
    void invalidate_title_bar();

    Gfx::Rect title_bar_rect() const;
    Gfx::Rect title_bar_icon_rect() const;
    Gfx::Rect title_bar_text_rect() const;

    void did_set_maximized(Badge<Window>, bool);

private:
    Window& m_window;
    NonnullOwnPtrVector<Button> m_buttons;
    Button* m_maximize_button { nullptr };
    Button* m_minimize_button { nullptr };
};

}
