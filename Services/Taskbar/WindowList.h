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

#include "WindowIdentifier.h"
#include <AK/HashMap.h>
#include <AK/String.h>
#include <LibGUI/Button.h>
#include <LibGfx/Rect.h>

class Window {
public:
    explicit Window(const WindowIdentifier& identifier)
        : m_identifier(identifier)
    {
    }

    ~Window()
    {
        if (m_button)
            m_button->remove_from_parent();
    }

    const WindowIdentifier& identifier() const { return m_identifier; }

    void set_parent_identifier(const WindowIdentifier& parent_identifier) { m_parent_identifier = parent_identifier; }
    const WindowIdentifier& parent_identifier() const { return m_parent_identifier; }

    String title() const { return m_title; }
    void set_title(const String& title) { m_title = title; }

    Gfx::IntRect rect() const { return m_rect; }
    void set_rect(const Gfx::IntRect& rect) { m_rect = rect; }

    GUI::Button* button() { return m_button; }
    void set_button(GUI::Button* button) { m_button = button; }

    void set_active(bool active) { m_active = active; }
    bool is_active() const { return m_active; }

    void set_minimized(bool minimized) { m_minimized = minimized; }
    bool is_minimized() const { return m_minimized; }

    void set_modal(bool modal) { m_modal = modal; }
    bool is_modal() const { return m_modal; }

    void set_progress(int progress)
    {
        if (m_progress == progress)
            return;
        m_progress = progress;
        if (m_button)
            m_button->update();
    }

    int progress() const { return m_progress; }

    const Gfx::Bitmap* icon() const { return m_icon.ptr(); }

private:
    WindowIdentifier m_identifier;
    WindowIdentifier m_parent_identifier;
    String m_title;
    Gfx::IntRect m_rect;
    RefPtr<GUI::Button> m_button;
    RefPtr<Gfx::Bitmap> m_icon;
    bool m_active { false };
    bool m_minimized { false };
    bool m_modal { false };
    int m_progress { -1 };
};

class WindowList {
public:
    static WindowList& the();

    template<typename Callback>
    void for_each_window(Callback callback)
    {
        for (auto& it : m_windows)
            callback(*it.value);
    }

    Window* find_parent(const Window&);
    Window* window(const WindowIdentifier&);
    Window& ensure_window(const WindowIdentifier&);
    void remove_window(const WindowIdentifier&);

private:
    HashMap<WindowIdentifier, NonnullOwnPtr<Window>> m_windows;
};
