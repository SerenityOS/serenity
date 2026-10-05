/*
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Buffer.h"
#include "ControlList.h"
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibGPU/Device.h>

namespace V3DGPU {

struct VertexData;

class Device final : public GPU::Device {
public:
    Device(NonnullOwnPtr<Core::File>);

    static ErrorOr<NonnullOwnPtr<Device>> create(Gfx::IntSize min_size);

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
    ErrorOr<void> initialize_context(Gfx::IntSize min_size);

    ErrorOr<ControlList> generate_initial_binner_control_list();

    struct RenderControlList {
        ControlList control_list;
        ControlList tile_list;
    };
    ErrorOr<RenderControlList> generate_render_control_list(u32 target_buffer_pitch, u32 target_buffer_address);

    ErrorOr<ControlList> generate_tile_list(u32 target_buffer_pitch, u32 target_buffer_address);

    // This member should be before any Buffers (or anything containing Buffers, like ControlLists)
    // to ensure that its destructor gets called last.
    NonnullOwnPtr<Core::File> m_gpu_file;

    Gfx::IntSize m_framebuffer_size { 0, 0 };
    Bytes m_framebuffer_data;
    Optional<Buffer> m_framebuffer;

    ControlList m_binner_control_list;
    RenderControlList m_render_control_list;

    Optional<Buffer> m_tile_alloc_memory_buffer;
    Optional<Buffer> m_tile_state_data_array_buffer;

    u32 m_clear_color { 0x00'00'00'00 };
    f32 m_clear_depth { 1.0f };

    struct RenderControlListState {
        // This struct contains all of the state that is "baked" into the render control list.
        // If any of these members change, the render control list needs to be regenerated.

        u32 clear_color { 0x00'00'00'00 };
        f32 clear_depth { 1.0f };

        bool operator==(RenderControlListState const&) const = default;
    };

    // This Optional is only empty before the first frame is rendered, so when we haven't generated a render control list yet.
    Optional<RenderControlListState> m_render_control_list_state;

    RenderControlListState render_control_list_state_needed_for_current_frame()
    {
        return {
            .clear_color = m_clear_color,
            .clear_depth = m_clear_depth,
        };
    }
};

}
