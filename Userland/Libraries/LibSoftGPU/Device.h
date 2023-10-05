/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022-2023, Jelle Raaijmakers <jelle@gmta.nl>
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
#include <LibGPU/TextureUnitConfiguration.h>
#include <LibGPU/Vertex.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Matrix3x3.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Buffer/FrameBuffer.h>
#include <LibSoftGPU/Buffer/Typed2DBuffer.h>
#include <LibSoftGPU/Clipper.h>
#include <LibSoftGPU/Config.h>
#include <LibSoftGPU/Sampler.h>
#include <LibSoftGPU/Shader.h>
#include <LibSoftGPU/ShaderProcessor.h>
#include <LibSoftGPU/Triangle.h>

namespace SoftGPU {

struct PixelQuad;

class Device final : public GPU::Device {
public:
    Device(Gfx::IntSize min_size);

    virtual GPU::DeviceInfo info() const override;

    virtual void draw_primitives(GPU::PrimitiveType, Vector<GPU::Vertex>& vertices) override;
    virtual void resize(Gfx::IntSize min_size) override;
    virtual void clear_color(FloatVector4 const&) override;
    virtual void clear_depth(GPU::DepthType) override;
    virtual void clear_stencil(GPU::StencilType) override;
    virtual void blit_from_color_buffer(Gfx::Bitmap& target) override;
    virtual void blit_from_color_buffer(NonnullRefPtr<GPU::Image>, u32 level, Vector2<u32> input_size, Vector2<i32> input_offset, Vector3<i32> output_offset) override;
    virtual void blit_from_color_buffer(void*, Vector2<i32> offset, GPU::ImageDataLayout const&) override;
    virtual void blit_from_depth_buffer(void*, Vector2<i32> offset, GPU::ImageDataLayout const&) override;
    virtual void blit_from_depth_buffer(NonnullRefPtr<GPU::Image>, u32 level, Vector2<u32> input_size, Vector2<i32> input_offset, Vector3<i32> output_offset) override;
    virtual void blit_to_color_buffer_at_raster_position(void const*, GPU::ImageDataLayout const&) override;
    virtual void blit_to_depth_buffer_at_raster_position(void const*, GPU::ImageDataLayout const&) override;
    virtual void set_options(GPU::RasterizerOptions const&) override;
    virtual void set_light_model_params(GPU::LightModelParameters const&) override;
    virtual GPU::RasterizerOptions options() const override { return m_options; }
    virtual GPU::LightModelParameters light_model() const override { return m_lighting_model; }

    virtual NonnullRefPtr<GPU::Image> create_image(GPU::PixelFormat const&, u32 width, u32 height, u32 depth, u32 max_levels) override;
    virtual ErrorOr<NonnullRefPtr<GPU::Shader>> create_shader(GPU::IR::Shader const&) override;

    virtual void set_model_view_transform(FloatMatrix4x4 const&) override;
    virtual void set_projection_transform(FloatMatrix4x4 const&) override;
    virtual void set_sampler_config(unsigned, GPU::SamplerConfig const&) override;
    virtual void set_light_state(unsigned, GPU::Light const&) override;
    virtual void set_material_state(GPU::Face, GPU::Material const&) override;
    virtual void set_stencil_configuration(GPU::Face, GPU::StencilConfiguration const&) override;
    virtual void set_texture_unit_configuration(GPU::TextureUnitIndex, GPU::TextureUnitConfiguration const&) override;
    virtual void set_clip_planes(Vector<FloatVector4> const&) override;

    virtual GPU::RasterPosition raster_position() const override { return m_raster_position; }
    virtual void set_raster_position(GPU::RasterPosition const& raster_position) override;
    virtual void set_raster_position(FloatVector4 const& position) override;

    virtual void bind_fragment_shader(RefPtr<GPU::Shader>) override;

private:
    void calculate_vertex_lighting(GPU::Vertex& vertex) const;
    void draw_statistics_overlay(Gfx::Bitmap&);
    Gfx::IntRect get_rasterization_rect_of_size(Gfx::IntSize size) const;

    GPU::ImageDataLayout color_buffer_data_layout(Vector2<u32> size, Vector2<i32> offset);
    GPU::ImageDataLayout depth_buffer_data_layout(Vector2<u32> size, Vector2<i32> offset);

    template<typename CB1, typename CB2, typename CB3>
    void rasterize(Gfx::IntRect& render_bounds, CB1 set_coverage_mask, CB2 set_quad_depth, CB3 set_quad_attributes);

    void rasterize_line_aliased(GPU::Vertex&, GPU::Vertex&);
    void rasterize_line_antialiased(GPU::Vertex&, GPU::Vertex&);
    void rasterize_line(GPU::Vertex&, GPU::Vertex&);

    void rasterize_point_aliased(GPU::Vertex&);
    void rasterize_point_antialiased(GPU::Vertex&);
    void rasterize_point(GPU::Vertex&);

    void rasterize_triangle(Triangle&);
    void shade_fragments(PixelQuad&);

    RefPtr<FrameBuffer<GPU::ColorType, GPU::DepthType, GPU::StencilType>> m_frame_buffer {};
    GPU::RasterizerOptions m_options;
    FloatMatrix4x4 m_model_view_transform;
    FloatMatrix3x3 m_normal_transform;
    FloatMatrix4x4 m_projection_transform;
    GPU::LightModelParameters m_lighting_model;
    Clipper m_clipper;
    Vector<Triangle> m_triangle_list;
    Vector<Triangle> m_processed_triangles;
    Vector<GPU::Vertex> m_clipped_vertices;
    float m_one_over_fog_depth;
    Array<Sampler, GPU::NUM_TEXTURE_UNITS> m_samplers;
    bool m_samplers_need_texture_staging { false };
    Array<GPU::Light, NUM_LIGHTS> m_lights;
    Array<GPU::Material, 2u> m_materials;
    GPU::RasterPosition m_raster_position;
    Vector<FloatVector4> m_clip_planes;
    Array<GPU::StencilConfiguration, 2u> m_stencil_configuration;
    Array<GPU::TextureUnitConfiguration, GPU::NUM_TEXTURE_UNITS> m_texture_unit_configuration;
    RefPtr<Shader> m_current_fragment_shader;
    ShaderProcessor m_shader_processor;
};

}
