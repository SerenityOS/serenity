/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>

namespace Gfx {

class FastBoxBlurFilter {
public:
    FastBoxBlurFilter(Bitmap&);

    void apply_single_pass(size_t radius);
    void apply_single_pass(size_t radius_x, size_t radius_y);

    void apply_three_passes(size_t radius);

private:
    Bitmap& m_bitmap;
};

}
