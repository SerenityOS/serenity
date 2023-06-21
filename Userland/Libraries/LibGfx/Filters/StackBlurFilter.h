/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>

namespace Gfx {

class StackBlurFilter {
public:
    StackBlurFilter(Bitmap& bitmap)
        : m_bitmap(bitmap)
    {
    }

    // Note: The radius is a u8 for reason! This implementation can only handle radii from 0 to 255.
    void process_rgba(u8 radius, Color fill_color = Color::NamedColor::White);

private:
    Bitmap& m_bitmap;
};

}
