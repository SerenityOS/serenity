/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WindowList.h"

WindowList& WindowList::the()
{
    static WindowList s_the;
    return s_the;
}

Window* WindowList::window(WindowIdentifier const& identifier)
{
    auto it = m_windows.find(identifier);
    if (it != m_windows.end())
        return it->value;
    return nullptr;
}

Window& WindowList::ensure_window(WindowIdentifier const& identifier)
{
    auto it = m_windows.find(identifier);
    if (it != m_windows.end())
        return *it->value;
    auto window = make<Window>(identifier);
    auto& window_ref = *window;
    m_windows.set(identifier, move(window));
    return window_ref;
}

void WindowList::remove_window(WindowIdentifier const& identifier)
{
    m_windows.remove(identifier);
}
