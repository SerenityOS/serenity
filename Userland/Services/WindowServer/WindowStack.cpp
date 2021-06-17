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

void WindowStack::set_highlight_window(Window* window)
{
    if (!window)
        m_highlight_window = nullptr;
    else
        m_highlight_window = window->make_weak_ptr<Window>();
}

}
