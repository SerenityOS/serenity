/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Window.h"

namespace WindowServer {

class WindowStack {
public:
    WindowStack();
    ~WindowStack();

    bool is_empty() const { return m_windows.is_empty(); }
    void add(Window&);
    void remove(Window&);
    void move_to_front(Window&);

    enum class IncludeWindowFrame {
        Yes,
        No,
    };
    Window* window_at(Gfx::IntPoint const&, IncludeWindowFrame = IncludeWindowFrame::Yes) const;

    template<typename Callback>
    IterationDecision for_each_visible_window_from_back_to_front(Callback);
    template<typename Callback>
    IterationDecision for_each_visible_window_from_front_to_back(Callback);
    template<typename Callback>
    IterationDecision for_each_visible_window_of_type_from_front_to_back(WindowType, Callback, bool ignore_highlight = false);
    template<typename Callback>
    IterationDecision for_each_visible_window_of_type_from_back_to_front(WindowType, Callback, bool ignore_highlight = false);
    template<typename Callback>
    IterationDecision for_each_window_of_type_from_front_to_back(WindowType, Callback, bool ignore_highlight = false);

    template<typename Callback>
    void for_each_window(Callback);

    Window::List& windows() { return m_windows; }

    Window* highlight_window() { return m_highlight_window; }
    Window const* highlight_window() const { return m_highlight_window; }
    void set_highlight_window(Window*);

    Window* active_window() { return m_active_window; }
    Window const* active_window() const { return m_active_window; }
    void set_active_window(Window*);

    Optional<HitTestResult> hit_test(Gfx::IntPoint const&) const;

private:
    WeakPtr<Window> m_highlight_window;
    WeakPtr<Window> m_active_window;

    Window::List m_windows;
};

template<typename Callback>
inline IterationDecision WindowStack::for_each_visible_window_of_type_from_back_to_front(WindowType type, Callback callback, bool ignore_highlight)
{
    bool do_highlight_window_at_end = false;
    for (auto& window : m_windows) {
        if (!window.is_visible())
            continue;
        if (window.is_minimized())
            continue;
        if (window.type() != type)
            continue;
        if (!ignore_highlight && m_highlight_window == &window) {
            do_highlight_window_at_end = true;
            continue;
        }
        if (callback(window) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    if (do_highlight_window_at_end) {
        if (callback(*m_highlight_window) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
inline IterationDecision WindowStack::for_each_visible_window_of_type_from_front_to_back(WindowType type, Callback callback, bool ignore_highlight)
{
    if (!ignore_highlight && m_highlight_window && m_highlight_window->type() == type && m_highlight_window->is_visible()) {
        if (callback(*m_highlight_window) == IterationDecision::Break)
            return IterationDecision::Break;
    }

    auto reverse_iterator = m_windows.rbegin();
    for (; reverse_iterator != m_windows.rend(); ++reverse_iterator) {
        auto& window = *reverse_iterator;
        if (!window.is_visible())
            continue;
        if (window.is_minimized())
            continue;
        if (window.type() != type)
            continue;
        if (!ignore_highlight && &window == m_highlight_window)
            continue;
        if (callback(window) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
inline void WindowStack::for_each_window(Callback callback)
{
    auto reverse_iterator = m_windows.rbegin();
    for (; reverse_iterator != m_windows.rend(); ++reverse_iterator) {
        auto& window = *reverse_iterator;
        if (callback(window) == IterationDecision::Break)
            return;
    }
}

template<typename Callback>
inline IterationDecision WindowStack::for_each_visible_window_from_back_to_front(Callback callback)
{
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::Desktop, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::Normal, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::ToolWindow, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::Taskbar, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::AppletArea, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::Notification, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::Tooltip, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_back_to_front(WindowType::Menu, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    return for_each_visible_window_of_type_from_back_to_front(WindowType::WindowSwitcher, callback);
}

template<typename Callback>
inline IterationDecision WindowStack::for_each_visible_window_from_front_to_back(Callback callback)
{
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::WindowSwitcher, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::Menu, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::Tooltip, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::Notification, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::AppletArea, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::Taskbar, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::ToolWindow, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    if (for_each_visible_window_of_type_from_front_to_back(WindowType::Normal, callback) == IterationDecision::Break)
        return IterationDecision::Break;
    return for_each_visible_window_of_type_from_front_to_back(WindowType::Desktop, callback);
}

template<typename Callback>
inline IterationDecision WindowStack::for_each_window_of_type_from_front_to_back(WindowType type, Callback callback, bool ignore_highlight)
{
    if (!ignore_highlight && m_highlight_window && m_highlight_window->type() == type && m_highlight_window->is_visible()) {
        if (callback(*m_highlight_window) == IterationDecision::Break)
            return IterationDecision::Break;
    }

    auto reverse_iterator = m_windows.rbegin();
    for (; reverse_iterator != m_windows.rend(); ++reverse_iterator) {
        auto& window = *reverse_iterator;
        if (window.type() != type)
            continue;
        if (!ignore_highlight && &window == m_highlight_window)
            continue;
        if (callback(window) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    return IterationDecision::Continue;
}

}
