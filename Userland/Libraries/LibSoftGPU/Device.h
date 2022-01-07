/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/NonnullRefPtr.h>
#include <AK/OwnPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Matrix3x3.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/AlphaBlendFactors.h>
#include <LibSoftGPU/Clipper.h>
#include <LibSoftGPU/Config.h>
#include <LibSoftGPU/DepthBuffer.h>
#include <LibSoftGPU/DeviceInfo.h>
#include <LibSoftGPU/Enums.h>
#include <LibSoftGPU/Image.h>
#include <LibSoftGPU/ImageFormat.h>
#include <LibSoftGPU/Light/Light.h>
#include <LibSoftGPU/Sampler.h>
#include <LibSoftGPU/Triangle.h>
#include <LibSoftGPU/Vertex.h>

namespace SoftGPU {

struct TexCoordGenerationConfig {
    TexCoordGenerationMode mode { TexCoordGenerationMode::EyeLinear };
    FloatVector4 coefficients {};
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
    float depth_min { 0.f };
    float depth_max { 1.f };
    DepthTestFunction depth_func { DepthTestFunction::Less };
    PolygonMode polygon_mode { PolygonMode::Fill };
    FloatVector4 fog_color { 0.0f, 0.0f, 0.0f, 0.0f };
    float fog_density { 1.0f };
    FogMode fog_mode { FogMode::Exp };
    bool fog_enabled { false };
    float fog_start { 0.0f };
    float fog_end { 1.0f };
    bool scissor_enabled { false };
    bool normalization_enabled { false };
    Gfx::IntRect scissor_box;
    bool enable_color_write { true };
    float depth_offset_factor { 0 };
    float depth_offset_constant { 0 };
    bool enable_culling { false };
    WindingOrder front_face { WindingOrder::CounterClockwise };
    bool cull_back { true };
    bool cull_front { false };
    u8 texcoord_generation_enabled_coordinates { TexCoordGenerationCoordinate::None };
    Array<TexCoordGenerationConfig, 4> texcoord_generation_config {};
    Gfx::IntRect viewport;
};

struct PixelQuad;

class Device final {
public:
    Device(const Gfx::IntSize& min_size);

    DeviceInfo info() const;

    void draw_primitives(PrimitiveType, FloatMatrix4x4 const& model_view_transform, FloatMatrix3x3 const& normal_transform, FloatMatrix4x4 const& projection_transform, FloatMatrix4x4 const& texture_transform, Vector<Vertex> const& vertices, Vector<size_t> const& enabled_texture_units);
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
    void set_light_state(unsigned, Light const&);

private:
    void draw_statistics_overlay(Gfx::Bitmap&);

    void rasterize_triangle(const Triangle& triangle);
    void setup_blend_factors();
    void shade_fragments(PixelQuad&);
    bool test_alpha(PixelQuad&);

private:
    RefPtr<Gfx::Bitmap> m_render_target;
    OwnPtr<DepthBuffer> m_depth_buffer;
    RasterizerOptions m_options;
    Clipper m_clipper;
    Vector<Triangle> m_triangle_list;
    Vector<Triangle> m_processed_triangles;
    Vector<Vertex> m_clipped_vertices;
    Array<Sampler, NUM_SAMPLERS> m_samplers;
    Vector<size_t> m_enabled_texture_units;
    AlphaBlendFactors m_alpha_blend_factors;
    Array<Light, NUM_LIGHTS> m_lights;
};

}
