/*
 * Copyright (c) 2024, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rect.h>

namespace Gfx {
struct CornerRadius {
    int horizontal_radius;
    int vertical_radius;

    inline operator bool() const
    {
        return horizontal_radius > 0 && vertical_radius > 0;
    }

    Gfx::IntRect as_rect() const
    {
        return { 0, 0, horizontal_radius, vertical_radius };
    }
};
}
