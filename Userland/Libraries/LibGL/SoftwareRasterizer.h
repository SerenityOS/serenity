/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DepthBuffer.h"
#include "GL/gl.h"
#include "GLStruct.h"
#include "Tex/Texture2D.h"
#include "Tex/TextureUnit.h"
#include <AK/Array.h>
#include <AK/OwnPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Vector4.h>

namespace GL {

struct RasterizerOptions {
    bool shade_smooth { true };
    bool enable_depth_test { false };
    bool enable_alpha_test { false };
    GLenum alpha_test_func { GL_ALWAYS };
    float alpha_test_ref_value { 0 };
    bool enable_blending { false };
    GLenum blend_source_factor { GL_ONE };
    GLenum blend_destination_factor { GL_ONE };
};

class SoftwareRasterizer final {
public:
    SoftwareRasterizer(const Gfx::IntSize& min_size);

    void submit_triangle(const GLTriangle& triangle, const Array<TextureUnit, 32>& texture_units);
    void submit_triangle(const GLTriangle& triangle);
    void resize(const Gfx::IntSize& min_size);
    void clear_color(const FloatVector4&);
    void clear_depth(float);
    void blit_to(Gfx::Bitmap&);
    void wait_for_all_threads() const;
    void set_options(const RasterizerOptions&);
    RasterizerOptions options() const { return m_options; }
    Gfx::RGBA32 get_backbuffer_pixel(int x, int y);
    float get_depthbuffer_value(int x, int y);

private:
    RefPtr<Gfx::Bitmap> m_render_target;
    OwnPtr<DepthBuffer> m_depth_buffer;
    RasterizerOptions m_options;
};

}
