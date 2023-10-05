/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibGPU/DeviceInfo.h>
#include <LibGPU/Enums.h>
#include <LibGPU/IR.h>
#include <LibGPU/Image.h>
#include <LibGPU/ImageDataLayout.h>
#include <LibGPU/Light.h>
#include <LibGPU/LightModelParameters.h>
#include <LibGPU/Material.h>
#include <LibGPU/RasterPosition.h>
#include <LibGPU/RasterizerOptions.h>
#include <LibGPU/SamplerConfig.h>
#include <LibGPU/Shader.h>
#include <LibGPU/StencilConfiguration.h>
#include <LibGPU/TextureUnitConfiguration.h>
#include <LibGPU/Vertex.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Matrix3x3.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Size.h>
#include <LibGfx/Vector2.h>
#include <LibGfx/Vector4.h>

namespace GPU {

class Device {
public:
    virtual ~Device() = default;

    virtual DeviceInfo info() const = 0;

    virtual void draw_primitives(PrimitiveType, Vector<Vertex>& vertices) = 0;
    virtual void resize(Gfx::IntSize min_size) = 0;
    virtual void clear_color(FloatVector4 const&) = 0;
    virtual void clear_depth(DepthType) = 0;
    virtual void clear_stencil(StencilType) = 0;
    virtual void blit_from_color_buffer(Gfx::Bitmap& target) = 0;
    virtual void blit_from_color_buffer(NonnullRefPtr<Image>, u32 level, Vector2<u32> input_size, Vector2<i32> input_offset, Vector3<i32> output_offset) = 0;
    virtual void blit_from_color_buffer(void*, Vector2<i32> offset, GPU::ImageDataLayout const&) = 0;
    virtual void blit_from_depth_buffer(void*, Vector2<i32> offset, GPU::ImageDataLayout const&) = 0;
    virtual void blit_from_depth_buffer(NonnullRefPtr<Image>, u32 level, Vector2<u32> input_size, Vector2<i32> input_offset, Vector3<i32> output_offset) = 0;
    virtual void blit_to_color_buffer_at_raster_position(void const*, GPU::ImageDataLayout const&) = 0;
    virtual void blit_to_depth_buffer_at_raster_position(void const*, GPU::ImageDataLayout const&) = 0;
    virtual void set_options(RasterizerOptions const&) = 0;
    virtual void set_light_model_params(LightModelParameters const&) = 0;
    virtual RasterizerOptions options() const = 0;
    virtual LightModelParameters light_model() const = 0;

    virtual NonnullRefPtr<Image> create_image(PixelFormat const&, u32 width, u32 height, u32 depth, u32 max_levels) = 0;
    virtual ErrorOr<NonnullRefPtr<Shader>> create_shader(IR::Shader const&) = 0;

    virtual void set_model_view_transform(FloatMatrix4x4 const&) = 0;
    virtual void set_projection_transform(FloatMatrix4x4 const&) = 0;
    virtual void set_sampler_config(unsigned, SamplerConfig const&) = 0;
    virtual void set_light_state(unsigned, Light const&) = 0;
    virtual void set_material_state(Face, Material const&) = 0;
    virtual void set_stencil_configuration(Face, StencilConfiguration const&) = 0;
    virtual void set_texture_unit_configuration(TextureUnitIndex, TextureUnitConfiguration const&) = 0;
    virtual void set_clip_planes(Vector<FloatVector4> const&) = 0;

    virtual RasterPosition raster_position() const = 0;
    virtual void set_raster_position(RasterPosition const& raster_position) = 0;
    virtual void set_raster_position(FloatVector4 const& position) = 0;

    virtual void bind_fragment_shader(RefPtr<Shader>) = 0;
};

}

typedef GPU::Device* (*serenity_gpu_create_device_t)(Gfx::IntSize size);

extern "C" GPU::Device* serenity_gpu_create_device(Gfx::IntSize size);
