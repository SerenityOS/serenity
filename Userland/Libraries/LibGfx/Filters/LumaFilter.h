/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>

namespace Gfx {

class LumaFilter {
public:
    LumaFilter(Bitmap& bitmap)
        : m_bitmap(bitmap) {};

    void apply(u8 lower_bound, u8 upper_bound);

private:
    Bitmap& m_bitmap;
};

}
