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
#include <LibGPU/Device.h>
#include <LibGPU/DeviceInfo.h>
#include <LibGPU/Enums.h>
#include <LibGPU/Image.h>
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
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/AlphaBlendFactors.h>
#include <LibSoftGPU/Buffer/FrameBuffer.h>
#include <LibSoftGPU/Buffer/Typed2DBuffer.h>
#include <LibSoftGPU/Clipper.h>
#include <LibSoftGPU/Config.h>
#include <LibSoftGPU/Sampler.h>
#include <LibSoftGPU/Triangle.h>

namespace SoftGPU {

struct PixelQuad;

class Device final : public GPU::Device {
public:
    Device(Gfx::IntSize const& min_size);

    virtual GPU::DeviceInfo info() const override;

    virtual void draw_primitives(GPU::PrimitiveType, FloatMatrix4x4 const& model_view_transform, FloatMatrix4x4 const& projection_transform, FloatMatrix4x4 const& texture_transform, Vector<GPU::Vertex>& vertices, Vector<size_t> const& enabled_texture_units) override;
    virtual void resize(Gfx::IntSize const& min_size) override;
    virtual void clear_color(FloatVector4 const&) override;
    virtual void clear_depth(GPU::DepthType) override;
    virtual void clear_stencil(GPU::StencilType) override;
    virtual void blit_color_buffer_to(Gfx::Bitmap& target) override;
    virtual void blit_to_color_buffer_at_raster_position(Gfx::Bitmap const&) override;
    virtual void blit_to_depth_buffer_at_raster_position(Vector<GPU::DepthType> const&, int, int) override;
    virtual void set_options(GPU::RasterizerOptions const&) override;
    virtual void set_light_model_params(GPU::LightModelParameters const&) override;
    virtual GPU::RasterizerOptions options() const override { return m_options; }
    virtual GPU::LightModelParameters light_model() const override { return m_lighting_model; }
    virtual GPU::ColorType get_color_buffer_pixel(int x, int y) override;
    virtual GPU::DepthType get_depthbuffer_value(int x, int y) override;

    virtual NonnullRefPtr<GPU::Image> create_image(GPU::ImageFormat format, unsigned width, unsigned height, unsigned depth, unsigned levels, unsigned layers) override;

    virtual void set_sampler_config(unsigned, GPU::SamplerConfig const&) override;
    virtual void set_light_state(unsigned, GPU::Light const&) override;
    virtual void set_material_state(GPU::Face, GPU::Material const&) override;
    virtual void set_stencil_configuration(GPU::Face, GPU::StencilConfiguration const&) override;
    virtual void set_clip_planes(Vector<FloatVector4> const&) override;

    virtual GPU::RasterPosition raster_position() const override { return m_raster_position; }
    virtual void set_raster_position(GPU::RasterPosition const& raster_position) override;
    virtual void set_raster_position(FloatVector4 const& position, FloatMatrix4x4 const& model_view_transform, FloatMatrix4x4 const& projection_transform) override;

private:
    void calculate_vertex_lighting(GPU::Vertex& vertex) const;
    void draw_statistics_overlay(Gfx::Bitmap&);
    Gfx::IntRect get_rasterization_rect_of_size(Gfx::IntSize size) const;

    template<typename CB1, typename CB2, typename CB3>
    void rasterize(Gfx::IntRect& render_bounds, CB1 set_coverage_mask, CB2 set_quad_depth, CB3 set_quad_attributes);

    void rasterize_line_aliased(GPU::Vertex&, GPU::Vertex&);
    void rasterize_line_antialiased(GPU::Vertex&, GPU::Vertex&);
    void rasterize_line(GPU::Vertex&, GPU::Vertex&);

    void rasterize_point_aliased(GPU::Vertex&);
    void rasterize_point_antialiased(GPU::Vertex&);
    void rasterize_point(GPU::Vertex&);

    void rasterize_triangle(Triangle&);
    void setup_blend_factors();
    void shade_fragments(PixelQuad&);
    void test_alpha(PixelQuad&);

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
    Vector<FloatVector4> m_clip_planes;
    Array<GPU::StencilConfiguration, 2u> m_stencil_configuration;
};

}
