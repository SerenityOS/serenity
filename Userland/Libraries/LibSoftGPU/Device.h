/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibGPU/DeviceInfo.h>
#include <LibGPU/Enums.h>
#include <LibGPU/ImageFormat.h>
#include <LibGPU/Light.h>
#include <LibGPU/LightModelParameters.h>
#include <LibGPU/Material.h>
#include <LibGPU/RasterPosition.h>
#include <LibGPU/RasterizerOptions.h>
#include <LibGPU/SamplerConfig.h>
#include <LibGPU/StencilConfiguration.h>
#include <LibGPU/TexCoordGenerationConfig.h>
#include <LibGPU/Vertex.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Matrix3x3.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/AlphaBlendFactors.h>
#include <LibSoftGPU/Buffer/FrameBuffer.h>
#include <LibSoftGPU/Buffer/Typed2DBuffer.h>
#include <LibSoftGPU/Clipper.h>
#include <LibSoftGPU/Config.h>
#include <LibSoftGPU/Image.h>
#include <LibSoftGPU/Sampler.h>
#include <LibSoftGPU/Triangle.h>

namespace SoftGPU {

struct PixelQuad;

class Device final {
public:
    Device(Gfx::IntSize const& min_size);

    GPU::DeviceInfo info() const;

    void draw_primitives(GPU::PrimitiveType, FloatMatrix4x4 const& model_view_transform, FloatMatrix4x4 const& projection_transform, FloatMatrix4x4 const& texture_transform, Vector<GPU::Vertex> const& vertices, Vector<size_t> const& enabled_texture_units);
    void resize(Gfx::IntSize const& min_size);
    void clear_color(FloatVector4 const&);
    void clear_depth(GPU::DepthType);
    void clear_stencil(GPU::StencilType);
    void blit_color_buffer_to(Gfx::Bitmap& target);
    void blit_to_color_buffer_at_raster_position(Gfx::Bitmap const&);
    void blit_to_depth_buffer_at_raster_position(Vector<GPU::DepthType> const&, int, int);
    void set_options(GPU::RasterizerOptions const&);
    void set_light_model_params(GPU::LightModelParameters const&);
    GPU::RasterizerOptions options() const { return m_options; }
    GPU::LightModelParameters light_model() const { return m_lighting_model; }
    GPU::ColorType get_color_buffer_pixel(int x, int y);
    GPU::DepthType get_depthbuffer_value(int x, int y);

    NonnullRefPtr<Image> create_image(GPU::ImageFormat format, unsigned width, unsigned height, unsigned depth, unsigned levels, unsigned layers);

    void set_sampler_config(unsigned, GPU::SamplerConfig const&);
    void set_light_state(unsigned, GPU::Light const&);
    void set_material_state(GPU::Face, GPU::Material const&);
    void set_stencil_configuration(GPU::Face, GPU::StencilConfiguration const&);

    GPU::RasterPosition raster_position() const { return m_raster_position; }
    void set_raster_position(GPU::RasterPosition const& raster_position);
    void set_raster_position(FloatVector4 const& position, FloatMatrix4x4 const& model_view_transform, FloatMatrix4x4 const& projection_transform);

private:
    void draw_statistics_overlay(Gfx::Bitmap&);
    Gfx::IntRect get_rasterization_rect_of_size(Gfx::IntSize size);

    void rasterize_triangle(Triangle const& triangle);
    void setup_blend_factors();
    void shade_fragments(PixelQuad&);
    bool test_alpha(PixelQuad&);

    RefPtr<FrameBuffer<GPU::ColorType, GPU::DepthType, GPU::StencilType>> m_frame_buffer {};
    GPU::RasterizerOptions m_options;
    GPU::LightModelParameters m_lighting_model;
    Clipper m_clipper;
    Vector<Triangle> m_triangle_list;
    Vector<Triangle> m_processed_triangles;
    Vector<GPU::Vertex> m_clipped_vertices;
    Array<Sampler, GPU::NUM_SAMPLERS> m_samplers;
    Vector<size_t> m_enabled_texture_units;
    AlphaBlendFactors m_alpha_blend_factors;
    Array<GPU::Light, NUM_LIGHTS> m_lights;
    Array<GPU::Material, 2u> m_materials;
    GPU::RasterPosition m_raster_position;
    Array<GPU::StencilConfiguration, 2u> m_stencil_configuration;
};

}
