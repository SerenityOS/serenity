/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GLStruct.h"
#include <LibGfx/Bitmap.h>
#include <LibGfx/Vector4.h>

namespace GL {

struct RasterizerOptions {
    bool shade_smooth { false };
};

class SoftwareRasterizer final {
public:
    SoftwareRasterizer(const Gfx::IntSize& min_size);

    void submit_triangle(const GLTriangle& triangle);
    void resize(const Gfx::IntSize& min_size);
    void clear_color(const FloatVector4&);
    void clear_depth(float);
    void blit_to(Gfx::Bitmap&);
    void wait_for_all_threads() const;
    void set_options(const RasterizerOptions&);
    RasterizerOptions options() const { return m_options; }

private:
    RefPtr<Gfx::Bitmap> m_render_target;
    RasterizerOptions m_options;
};

}
