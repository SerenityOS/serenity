/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WindowStack.h"
#include "WindowManager.h"

namespace WindowServer {

WindowStack::WindowStack(unsigned row, unsigned column)
    : m_row(row)
    , m_column(column)
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

void WindowStack::add_to_back(Window& window)
{
    VERIFY(window.outer_stack() == nullptr);
    m_windows.prepend(window);
    window.set_outer_stack({}, this);
}

void WindowStack::remove(Window& window)
{
    VERIFY(window.outer_stack() == this);
    m_windows.remove(window);
    window.set_outer_stack({}, nullptr);
    if (m_active_window == &window)
        m_active_window = nullptr;
    if (m_active_input_window == &window)
        m_active_input_window = nullptr;
    if (m_active_input_tracking_window == &window)
        m_active_input_tracking_window = nullptr;
}

void WindowStack::move_to_front(Window& window)
{
    if (m_windows.last() != &window)
        window.invalidate();
    m_windows.remove(window);
    m_windows.append(window);
}

void WindowStack::move_all_windows(WindowStack& new_window_stack, Vector<Window*, 32>& windows_moved, MoveAllWindowsTo move_to)
{
    VERIFY(this != &new_window_stack);
    if (move_to == MoveAllWindowsTo::Front) {
        while (auto* window = m_windows.take_first()) {
            window->set_outer_stack({}, nullptr);
            new_window_stack.add(*window);
            windows_moved.append(window);
        }
    } else {
        while (auto* window = m_windows.take_last()) {
            window->set_outer_stack({}, nullptr);
            new_window_stack.add_to_back(*window);
            windows_moved.append(window);
        }
    }
    m_active_window = nullptr;
    m_active_input_window = nullptr;
    m_active_input_tracking_window = nullptr;
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

Window* WindowStack::highlight_window() const
{
    if (auto* window = WindowManager::the().highlight_window(); window && window->outer_stack() == this)
        return window;
    return nullptr;
}

void WindowStack::set_active_window(Window* window)
{
    if (!window)
        m_active_window = nullptr;
    else
        m_active_window = window->make_weak_ptr<Window>();
}

void WindowStack::set_all_occluded(bool occluded)
{
    for (auto& window : m_windows) {
        if (!WindowManager::is_stationary_window_type(window.type()))
            window.set_occluded(occluded);
    }
}

Optional<HitTestResult> WindowStack::hit_test(Gfx::IntPoint const& position) const
{
    Optional<HitTestResult> result;
    WindowManager::the().for_each_visible_window_from_front_to_back([&](Window& window) {
        result = window.hit_test(position);
        if (result.has_value())
            return IterationDecision::Break;
        return IterationDecision::Continue;
    },
        const_cast<WindowStack*>(this));
    return result;
}

}
