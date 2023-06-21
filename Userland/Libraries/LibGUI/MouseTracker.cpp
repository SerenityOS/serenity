/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/MouseTracker.h>

namespace GUI {

MouseTracker::List MouseTracker::s_trackers;

MouseTracker::MouseTracker()
{
    if (s_trackers.is_empty()) {
        ConnectionToWindowServer::the().async_set_global_mouse_tracking(true);
    }
    s_trackers.append(*this);
}
MouseTracker::~MouseTracker()
{
    m_list_node.remove();
    if (s_trackers.is_empty()) {
        ConnectionToWindowServer::the().async_set_global_mouse_tracking(false);
    }
}

void MouseTracker::track_mouse_move(Badge<ConnectionToWindowServer>, Gfx::IntPoint point)
{
    for (auto& tracker : s_trackers) {
        tracker.track_mouse_move(point);
    }
}

}
