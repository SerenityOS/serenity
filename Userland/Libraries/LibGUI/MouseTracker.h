/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/IntrusiveList.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Point.h>

namespace GUI {

class MouseTracker {
public:
    MouseTracker();
    virtual ~MouseTracker();

    static void track_mouse_move(Badge<ConnectionToWindowServer>, Gfx::IntPoint);

protected:
    virtual void track_mouse_move(Gfx::IntPoint) = 0;

private:
    IntrusiveListNode<MouseTracker> m_list_node;
    using List = IntrusiveList<&MouseTracker::m_list_node>;
    static List s_trackers;
};

}
