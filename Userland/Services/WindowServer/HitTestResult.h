/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/WeakPtr.h>
#include <LibGfx/Point.h>

namespace WindowServer {

class Window;

struct HitTestResult {
    WeakPtr<Window> window;
    Gfx::IntPoint screen_position;
    Gfx::IntPoint window_relative_position;
    bool is_frame_hit { false };
};

}
