/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/MouseTracker.h>
#include <LibGUI/WindowServerConnection.h>

namespace GUI {

MouseTracker::List MouseTracker::s_trackers;

MouseTracker::MouseTracker()
{
    if (s_trackers.is_empty()) {
        WindowServerConnection::the().async_set_global_mouse_tracking(true);
    }
    s_trackers.append(*this);
}
MouseTracker::~MouseTracker()
{
    m_list_node.remove();
    if (s_trackers.is_empty()) {
        WindowServerConnection::the().async_set_global_mouse_tracking(false);
    }
}

void MouseTracker::track_mouse_move(Badge<WindowServerConnection>, Gfx::IntPoint const& point)
{
    for (auto& tracker : s_trackers) {
        tracker.track_mouse_move(point);
    }
}

}
