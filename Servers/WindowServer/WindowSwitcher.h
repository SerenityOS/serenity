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

#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGfx/Rect.h>

namespace Gfx {
class Painter;
}

namespace WindowServer {

class KeyEvent;
class Window;

class WindowSwitcher final : public Core::Object {
    C_OBJECT(WindowSwitcher)
public:
    static WindowSwitcher& the();

    WindowSwitcher();
    virtual ~WindowSwitcher() override;

    bool is_visible() const { return m_visible; }
    void set_visible(bool);

    void show() { set_visible(true); }
    void hide() { set_visible(false); }

    void on_key_event(const KeyEvent&);

    void refresh();
    void refresh_if_needed();

    void draw();

    int thumbnail_width() const { return 40; }
    int thumbnail_height() const { return 40; }

    int item_height() const { return 10 + thumbnail_height(); }
    int padding() const { return 8; }
    int item_padding() const { return 8; }

    void select_window(Window&);

    Window* selected_window();

    Window* switcher_window() { return m_switcher_window.ptr(); }

private:
    void select_window_at_index(int index);
    Gfx::Rect item_rect(int index) const;

    virtual void event(Core::Event&) override;

    RefPtr<Window> m_switcher_window;
    Gfx::Rect m_rect;
    bool m_visible { false };
    Vector<WeakPtr<Window>> m_windows;
    int m_selected_index { 0 };
};

}
