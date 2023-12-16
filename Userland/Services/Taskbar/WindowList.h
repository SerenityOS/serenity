/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "WindowIdentifier.h"
#include <AK/ByteString.h>
#include <AK/HashMap.h>
#include <LibGUI/Button.h>
#include <LibGfx/Rect.h>

class Window {
public:
    explicit Window(WindowIdentifier const& identifier)
        : m_identifier(identifier)
    {
    }

    ~Window()
    {
        if (m_button)
            m_button->remove_from_parent();
    }

    WindowIdentifier const& identifier() const { return m_identifier; }

    ByteString title() const { return m_title; }
    void set_title(ByteString const& title) { m_title = title; }

    Gfx::IntRect rect() const { return m_rect; }
    void set_rect(Gfx::IntRect const& rect) { m_rect = rect; }

    GUI::Button* button() { return m_button; }
    void set_button(GUI::Button* button) { m_button = button; }

    void set_active(bool active) { m_active = active; }
    bool is_active() const { return m_active; }

    void set_blocked(bool blocked) { m_blocked = blocked; }
    bool is_blocked() const { return m_blocked; }

    void set_minimized(bool minimized) { m_minimized = minimized; }
    bool is_minimized() const { return m_minimized; }

    void set_workspace(unsigned row, unsigned column)
    {
        m_workspace_row = row;
        m_workspace_column = column;
    }
    unsigned workspace_row() const { return m_workspace_row; }
    unsigned workspace_column() const { return m_workspace_column; }

    void set_progress(Optional<int> progress)
    {
        if (m_progress == progress)
            return;
        m_progress = progress;
        if (m_button)
            m_button->update();
    }

    Optional<int> progress() const { return m_progress; }

    Gfx::Bitmap const* icon() const { return m_icon.ptr(); }

private:
    WindowIdentifier m_identifier;
    ByteString m_title;
    Gfx::IntRect m_rect;
    RefPtr<GUI::Button> m_button;
    RefPtr<Gfx::Bitmap> m_icon;
    unsigned m_workspace_row { 0 };
    unsigned m_workspace_column { 0 };
    bool m_active { false };
    bool m_blocked { false };
    bool m_minimized { false };
    Optional<int> m_progress;
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

    Window* window(WindowIdentifier const&);
    Window& ensure_window(WindowIdentifier const&);
    void remove_window(WindowIdentifier const&);

private:
    HashMap<WindowIdentifier, NonnullOwnPtr<Window>> m_windows;
};
