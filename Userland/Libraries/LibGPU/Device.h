/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
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
#include <LibGfx/Matrix3x3.h>
#include <LibGfx/Matrix4x4.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibGfx/Vector4.h>

namespace GPU {

class Device {
public:
    virtual ~Device() { }

    virtual DeviceInfo info() const = 0;

    virtual void draw_primitives(PrimitiveType, FloatMatrix4x4 const& model_view_transform, FloatMatrix4x4 const& projection_transform, FloatMatrix4x4 const& texture_transform, Vector<Vertex>& vertices, Vector<size_t> const& enabled_texture_units) = 0;
    virtual void resize(Gfx::IntSize const& min_size) = 0;
    virtual void clear_color(FloatVector4 const&) = 0;
    virtual void clear_depth(DepthType) = 0;
    virtual void clear_stencil(StencilType) = 0;
    virtual void blit_color_buffer_to(Gfx::Bitmap& target) = 0;
    virtual void blit_to_color_buffer_at_raster_position(Gfx::Bitmap const&) = 0;
    virtual void blit_to_depth_buffer_at_raster_position(Vector<DepthType> const&, int, int) = 0;
    virtual void set_options(RasterizerOptions const&) = 0;
    virtual void set_light_model_params(LightModelParameters const&) = 0;
    virtual RasterizerOptions options() const = 0;
    virtual LightModelParameters light_model() const = 0;
    virtual ColorType get_color_buffer_pixel(int x, int y) = 0;
    virtual DepthType get_depthbuffer_value(int x, int y) = 0;

    virtual NonnullRefPtr<Image> create_image(ImageFormat format, unsigned width, unsigned height, unsigned depth, unsigned levels, unsigned layers) = 0;

    virtual void set_sampler_config(unsigned, SamplerConfig const&) = 0;
    virtual void set_light_state(unsigned, Light const&) = 0;
    virtual void set_material_state(Face, Material const&) = 0;
    virtual void set_stencil_configuration(Face, StencilConfiguration const&) = 0;
    virtual void set_clip_planes(Vector<FloatVector4> const&) = 0;

    virtual RasterPosition raster_position() const = 0;
    virtual void set_raster_position(RasterPosition const& raster_position) = 0;
    virtual void set_raster_position(FloatVector4 const& position, FloatMatrix4x4 const& model_view_transform, FloatMatrix4x4 const& projection_transform) = 0;
};

}

typedef GPU::Device* (*serenity_gpu_create_device_t)(Gfx::IntSize const& size);

extern "C" GPU::Device* serenity_gpu_create_device(Gfx::IntSize const& size);
