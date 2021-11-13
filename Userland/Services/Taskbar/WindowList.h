/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    const Gfx::Bitmap* icon() const { return m_icon.ptr(); }

private:
    WindowIdentifier m_identifier;
    WindowIdentifier m_parent_identifier;
    String m_title;
    Gfx::IntRect m_rect;
    RefPtr<GUI::Button> m_button;
    RefPtr<Gfx::Bitmap> m_icon;
    unsigned m_workspace_row { 0 };
    unsigned m_workspace_column { 0 };
    bool m_active { false };
    bool m_minimized { false };
    bool m_modal { false };
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

    Window* find_parent(const Window&);
    Window* window(const WindowIdentifier&);
    Window& ensure_window(const WindowIdentifier&);
    void remove_window(const WindowIdentifier&);

private:
    HashMap<WindowIdentifier, NonnullOwnPtr<Window>> m_windows;
};
