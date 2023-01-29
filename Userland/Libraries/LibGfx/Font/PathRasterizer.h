/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Path.h>

namespace Gfx {

class PathRasterizer {
public:
    PathRasterizer(Gfx::IntSize);
    void draw_path(Gfx::Path&);
    RefPtr<Gfx::Bitmap> accumulate();

private:
    void draw_line(Gfx::FloatPoint, Gfx::FloatPoint);

    Gfx::IntSize m_size;
    Vector<float> m_data;
};

}
