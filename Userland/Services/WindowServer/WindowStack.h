/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Window.h"

namespace WindowServer {

class Compositor;

class WindowStack {
public:
    WindowStack(unsigned row, unsigned column);
    ~WindowStack() = default;

    bool is_empty() const { return m_windows.is_empty(); }
    void add(Window&);
    void add_to_back(Window&);
    void remove(Window&);
    void move_to_front(Window&);
    void move_always_on_top_windows_to_front();

    enum class MoveAllWindowsTo {
        Front,
        Back
    };
    void move_all_windows(WindowStack&, Vector<Window*, 32>&, MoveAllWindowsTo);

    enum class IncludeWindowFrame {
        Yes,
        No,
    };
    Window* window_at(Gfx::IntPoint, IncludeWindowFrame = IncludeWindowFrame::Yes) const;
    Window* highlight_window() const;

    template<typename Callback>
    IterationDecision for_each_visible_window_of_type_from_front_to_back(WindowType, Callback, bool ignore_highlight = false);
    template<typename Callback>
    IterationDecision for_each_visible_window_of_type_from_back_to_front(WindowType, Callback, bool ignore_highlight = false);
    template<typename Callback>
    IterationDecision for_each_window_of_type_from_front_to_back(WindowType, Callback, bool ignore_highlight = false);

    template<typename Callback>
    void for_each_window(Callback);
    template<typename Callback>
    IterationDecision for_each_window_from_back_to_front(Callback);

    Window::List& windows() { return m_windows; }

    Window* active_window() { return m_active_window; }
    Window const* active_window() const { return m_active_window; }
    void set_active_window(Window*);

    Optional<HitTestResult> hit_test(Gfx::IntPoint) const;

    unsigned row() const { return m_row; }
    unsigned column() const { return m_column; }

    void set_transition_offset(Badge<Compositor>, Gfx::IntPoint transition_offset) { m_transition_offset = transition_offset; }
    Gfx::IntPoint transition_offset() const { return m_transition_offset; }

    void set_stationary_window_stack(WindowStack& window_stack) { m_stationary_window_stack = &window_stack; }
    WindowStack& stationary_window_stack()
    {
        VERIFY(m_stationary_window_stack);
        return *m_stationary_window_stack;
    }

    void set_all_occluded(bool);

private:
    WeakPtr<Window> m_active_window;

    Window::List m_windows;
    unsigned m_row { 0 };
    unsigned m_column { 0 };
    Gfx::IntPoint m_transition_offset;
    WindowStack* m_stationary_window_stack { nullptr };
};

template<typename Callback>
inline IterationDecision WindowStack::for_each_visible_window_of_type_from_back_to_front(WindowType type, Callback callback, bool ignore_highlight)
{
    auto* highlight_window = this->highlight_window();
    bool do_highlight_window_at_end = false;
    for (auto& window : m_windows) {
        if (!window.is_visible())
            continue;
        if (window.is_minimized())
            continue;
        if (window.type() != type)
            continue;
        if (!ignore_highlight && highlight_window == &window) {
            do_highlight_window_at_end = true;
            continue;
        }
        if (callback(window) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    if (do_highlight_window_at_end) {
        if (callback(*highlight_window) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
inline IterationDecision WindowStack::for_each_visible_window_of_type_from_front_to_back(WindowType type, Callback callback, bool ignore_highlight)
{
    auto* highlight_window = this->highlight_window();
    if (!ignore_highlight && highlight_window && highlight_window->type() == type && highlight_window->is_visible() && !highlight_window->is_minimized()) {
        if (callback(*highlight_window) == IterationDecision::Break)
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
        if (!ignore_highlight && &window == highlight_window)
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
inline IterationDecision WindowStack::for_each_window_from_back_to_front(Callback callback)
{
    for (auto& window : m_windows) {
        IterationDecision decision = callback(window);
        if (decision != IterationDecision::Break)
            return decision;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
inline IterationDecision WindowStack::for_each_window_of_type_from_front_to_back(WindowType type, Callback callback, bool ignore_highlight)
{
    auto* highlight_window = this->highlight_window();
    if (!ignore_highlight && highlight_window && highlight_window->type() == type && highlight_window->is_visible()) {
        if (callback(*highlight_window) == IterationDecision::Break)
            return IterationDecision::Break;
    }

    auto reverse_iterator = m_windows.rbegin();
    for (; reverse_iterator != m_windows.rend(); ++reverse_iterator) {
        auto& window = *reverse_iterator;
        if (window.type() != type)
            continue;
        if (!ignore_highlight && &window == highlight_window)
            continue;
        if (callback(window) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    return IterationDecision::Continue;
}

}
