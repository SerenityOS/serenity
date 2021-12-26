/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/NonnullRefPtr.h>
#include <AK/OwnPtr.h>
#include <LibGL/GL/gl.h>
#include <LibGL/Tex/Texture2D.h>
#include <LibGL/Tex/TextureUnit.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Clipper.h>
#include <LibSoftGPU/DepthBuffer.h>
#include <LibSoftGPU/Image.h>
#include <LibSoftGPU/ImageFormat.h>
#include <LibSoftGPU/Sampler.h>
#include <LibSoftGPU/Triangle.h>
#include <LibSoftGPU/Vertex.h>

namespace SoftGPU {

enum class AlphaTestFunction {
    Never,
    Always,
    Less,
    LessOrEqual,
    Equal,
    NotEqual,
    GreaterOrEqual,
    Greater,
};

enum class BlendFactor {
    Zero,
    One,
    SrcAlpha,
    OneMinusSrcAlpha,
    SrcColor,
    OneMinusSrcColor,
    DstAlpha,
    OneMinusDstAlpha,
    DstColor,
    OneMinusDstColor,
    SrcAlphaSaturate,
};

enum class DepthTestFunction {
    Never,
    Always,
    Less,
    LessOrEqual,
    Equal,
    NotEqual,
    GreaterOrEqual,
    Greater,
};

enum FogMode {
    Linear,
    Exp,
    Exp2
};

enum class WindingOrder {
    Clockwise,
    CounterClockwise,
};

struct RasterizerOptions {
    bool shade_smooth { true };
    bool enable_depth_test { false };
    bool enable_depth_write { true };
    bool enable_alpha_test { false };
    AlphaTestFunction alpha_test_func { AlphaTestFunction::Always };
    float alpha_test_ref_value { 0 };
    bool enable_blending { false };
    BlendFactor blend_source_factor { BlendFactor::One };
    BlendFactor blend_destination_factor { BlendFactor::One };
    u32 color_mask { 0xffffffff };
    float depth_min { 0 };
    float depth_max { 1 };
    DepthTestFunction depth_func { DepthTestFunction::Less };
    GLenum polygon_mode { GL_FILL };
    FloatVector4 fog_color { 0.0f, 0.0f, 0.0f, 0.0f };
    float fog_density { 1.0f };
    FogMode fog_mode { FogMode::Exp };
    bool fog_enabled { false };
    float fog_start { 0.0f };
    float fog_end { 1.0f };
    bool scissor_enabled { false };
    Gfx::IntRect scissor_box;
    bool enable_color_write { true };
    float depth_offset_factor { 0 };
    float depth_offset_constant { 0 };
    bool enable_culling { false };
    WindingOrder front_face { WindingOrder::CounterClockwise };
    bool cull_back { true };
    bool cull_front { false };
};

inline static constexpr size_t const num_samplers = 32;

class Device final {
public:
    Device(const Gfx::IntSize& min_size);

    void draw_primitives(GLenum primitive_type, FloatMatrix4x4 const& transform, FloatMatrix4x4 const& texture_matrix, Vector<Vertex> const& vertices, Vector<size_t> const& enabled_texture_units);
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

    NonnullRefPtr<Image> create_image(ImageFormat, unsigned width, unsigned height, unsigned depth, unsigned levels, unsigned layers);

    void set_sampler_config(unsigned, SamplerConfig const&);

private:
    void submit_triangle(Triangle const& triangle, Vector<size_t> const& enabled_texture_units);

private:
    RefPtr<Gfx::Bitmap> m_render_target;
    OwnPtr<DepthBuffer> m_depth_buffer;
    RasterizerOptions m_options;
    Clipper m_clipper;
    Vector<Triangle> m_triangle_list;
    Vector<Triangle> m_processed_triangles;
    Vector<Vertex> m_clipped_vertices;
    Sampler m_samplers[num_samplers];
};

}
