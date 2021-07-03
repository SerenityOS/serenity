/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WindowStack.h"

namespace WindowServer {

WindowStack::WindowStack()
{
}

WindowStack::~WindowStack()
{
}

void WindowStack::add(Window& window)
{
    VERIFY(window.outer_stack() == nullptr);
    m_windows.append(window);
    window.set_outer_stack({}, this);
}

void WindowStack::remove(Window& window)
{
    VERIFY(window.outer_stack() == this);
    m_windows.remove(window);
    window.set_outer_stack({}, nullptr);
}

void WindowStack::move_to_front(Window& window)
{
    if (m_windows.last() != &window)
        window.invalidate();
    m_windows.remove(window);
    m_windows.append(window);
}

Window* WindowStack::window_at(Gfx::IntPoint const& position, IncludeWindowFrame include_window_frame) const
{
    auto result = hit_test(position);
    if (!result.has_value())
        return nullptr;
    if (include_window_frame == IncludeWindowFrame::No && result->is_frame_hit)
        return nullptr;
    return result->window;
}

void WindowStack::set_highlight_window(Window* window)
{
    if (!window)
        m_highlight_window = nullptr;
    else
        m_highlight_window = window->make_weak_ptr<Window>();
}

void WindowStack::set_active_window(Window* window)
{
    if (!window)
        m_active_window = nullptr;
    else
        m_active_window = window->make_weak_ptr<Window>();
}

Optional<HitTestResult> WindowStack::hit_test(Gfx::IntPoint const& position) const
{
    Optional<HitTestResult> result;
    const_cast<WindowStack*>(this)->for_each_visible_window_from_front_to_back([&](Window& window) {
        result = window.hit_test(position);
        if (result.has_value())
            return IterationDecision::Break;
        return IterationDecision::Continue;
    });
    return result;
}

}
