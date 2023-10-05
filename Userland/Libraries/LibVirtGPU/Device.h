/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <Kernel/API/VirGL.h>
#include <LibGPU/Device.h>
#include <LibVirtGPU/VirGLProtocol.h>

namespace VirtGPU {

class Device final : public GPU::Device {
public:
    Device(NonnullOwnPtr<Core::File>);

    static ErrorOr<NonnullOwnPtr<Device>> create(Gfx::IntSize min_size);

    // FIXME: Once the kernel driver supports destroying contexts we need to add this functionality here
    ErrorOr<void> initialize_context(Gfx::IntSize min_size);

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
    virtual GPU::RasterizerOptions options() const override;
    virtual GPU::LightModelParameters light_model() const override;

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

    virtual GPU::RasterPosition raster_position() const override;
    virtual void set_raster_position(GPU::RasterPosition const& raster_position) override;
    virtual void set_raster_position(FloatVector4 const& position) override;

    virtual void bind_fragment_shader(RefPtr<GPU::Shader>) override;

private:
    void encode_constant_buffer(Gfx::FloatMatrix4x4 const&, Vector<float>&);
    Protocol::ObjectHandle allocate_handle();
    ErrorOr<Protocol::ResourceID> create_virgl_resource(VirGL3DResourceSpec&);
    ErrorOr<void> upload_command_buffer(Vector<u32> const&);

    NonnullOwnPtr<Core::File> m_gpu_file;

    FloatMatrix4x4 m_model_view_transform;
    FloatMatrix4x4 m_projection_transform;

    Protocol::ResourceID m_vbo_resource_id { 0 };
    Protocol::ResourceID m_drawtarget { 0 };
    Protocol::ResourceID m_depthbuffer_surface { 0 };
    Protocol::ObjectHandle m_blend_handle { 0 };
    Protocol::ObjectHandle m_drawtarget_surface_handle { 0 };
    Protocol::ObjectHandle m_depthbuffer_surface_handle { 0 };
    Protocol::ObjectHandle m_ve_handle { 0 };
    Protocol::ObjectHandle m_frag_shader_handle { 0 };
    Protocol::ObjectHandle m_vert_shader_handle { 0 };
    Protocol::ObjectHandle m_rasterizer_handle { 0 };
    Protocol::ObjectHandle m_dsa_handle { 0 };
    u32 m_last_allocated_handle { 0 };

    struct VertexData {
        float r;
        float g;
        float b;
        float x;
        float y;
        float z;
    };

    Vector<VertexData> m_vertices;

    Vector<float> m_constant_buffer_data;
};

}
