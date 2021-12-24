/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/OwnPtr.h>
#include <LibGL/GL/gl.h>
#include <LibGL/GLStruct.h>
#include <LibGL/Tex/Texture2D.h>
#include <LibGL/Tex/TextureUnit.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Clipper.h>
#include <LibSoftGPU/DepthBuffer.h>
#include <LibSoftGPU/Triangle.h>
#include <LibSoftGPU/Vertex.h>

namespace SoftGPU {

struct RasterizerOptions {
    bool shade_smooth { true };
    bool enable_depth_test { false };
    bool enable_depth_write { true };
    bool enable_alpha_test { false };
    GLenum alpha_test_func { GL_ALWAYS };
    float alpha_test_ref_value { 0 };
    bool enable_blending { false };
    GLenum blend_source_factor { GL_ONE };
    GLenum blend_destination_factor { GL_ONE };
    u32 color_mask { 0xffffffff };
    float depth_min { 0 };
    float depth_max { 1 };
    GLenum depth_func { GL_LESS };
    GLenum polygon_mode { GL_FILL };
    FloatVector4 fog_color {
        0.0f,
        0.0f,
        0.0f,
        0.0f,
    };
    GLfloat fog_density { 1.0f };
    GLenum fog_mode { GL_EXP };
    GLboolean fog_enabled { false };
    GLfloat fog_start { 0.0f };
    GLfloat fog_end { 1.0f };
    bool scissor_enabled { false };
    Gfx::IntRect scissor_box;
    GLenum draw_buffer { GL_BACK };
    GLfloat depth_offset_factor { 0 };
    GLfloat depth_offset_constant { 0 };
    bool enable_culling { false };
    GLenum front_face { GL_CCW };
    GLenum culled_sides { GL_BACK };
};

class SoftwareRasterizer final {
public:
    SoftwareRasterizer(const Gfx::IntSize& min_size);

    void draw_primitives(GLenum primitive_type, FloatMatrix4x4 const& transform, FloatMatrix4x4 const& texture_matrix, Vector<Vertex> const& vertices, GL::TextureUnit::BoundList const& bound_texture_units);
    void resize(const Gfx::IntSize& min_size);
    void clear_color(const FloatVector4&);
    void clear_depth(float);
    void blit(Gfx::Bitmap const&, int x, int y);
    void blit_to(Gfx::Bitmap&);
    void wait_for_all_threads() const;
    void set_options(const RasterizerOptions&);
    RasterizerOptions options() const { return m_options; }
    Gfx::RGBA32 get_backbuffer_pixel(int x, int y);
    float get_depthbuffer_value(int x, int y);

private:
    void submit_triangle(Triangle const& triangle, GL::TextureUnit::BoundList const& bound_texture_units);

private:
    RefPtr<Gfx::Bitmap> m_render_target;
    OwnPtr<DepthBuffer> m_depth_buffer;
    RasterizerOptions m_options;
    Clipper m_clipper;
    Vector<Triangle> m_triangle_list;
    Vector<Triangle> m_processed_triangles;
    Vector<Vertex> m_clipped_vertices;
};

}
