/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <LibVirtGPU/Device.h>
#include <LibVirtGPU/Image.h>
#include <LibVirtGPU/Shader.h>

namespace VirtGPU {

Device::Device(NonnullRefPtr<Core::File> gpu_file)
    : m_gpu_file { gpu_file }
{
}

ErrorOr<NonnullOwnPtr<Device>> Device::create(Gfx::IntSize)
{
    auto file = TRY(Core::File::open("/dev/gpu/render0", Core::OpenMode::ReadWrite));
    auto device = make<Device>(file);
    return device;
}

GPU::DeviceInfo Device::info() const
{
    return {
        .vendor_name = "SerenityOS",
        .device_name = "VirtGPU",
        .num_texture_units = GPU::NUM_TEXTURE_UNITS,
        .num_lights = 8,
        .max_clip_planes = 6,
        .max_texture_size = 4096,
        .max_texture_lod_bias = 2.f,
        .stencil_bits = sizeof(GPU::StencilType) * 8,
        .supports_npot_textures = true,
        .supports_texture_clamp_to_edge = true,
        .supports_texture_env_add = true,
    };
}

void Device::draw_primitives(GPU::PrimitiveType, FloatMatrix4x4 const&, FloatMatrix4x4 const&, Vector<GPU::Vertex>&)
{
    dbgln("VirtGPU::Device::draw_primitives(): unimplemented");
}

void Device::resize(Gfx::IntSize)
{
    dbgln("VirtGPU::Device::resize(): unimplemented");
}

void Device::clear_color(FloatVector4 const&)
{
    dbgln("VirtGPU::Device::clear_color(): unimplemented");
}

void Device::clear_depth(GPU::DepthType)
{
    dbgln("VirtGPU::Device::clear_depth(): unimplemented");
}

void Device::clear_stencil(GPU::StencilType)
{
    dbgln("VirtGPU::Device::clear_stencil(): unimplemented");
}

void Device::blit_from_color_buffer(Gfx::Bitmap&)
{
    dbgln("VirtGPU::Device::blit_from_color_buffer(): unimplemented");
}

void Device::blit_from_color_buffer(NonnullRefPtr<GPU::Image>, u32, Vector2<u32>, Vector2<i32>, Vector3<i32>)
{
    dbgln("VirtGPU::Device::blit_from_color_buffer(): unimplemented");
}

void Device::blit_from_color_buffer(void*, Vector2<i32>, GPU::ImageDataLayout const&)
{
    dbgln("VirtGPU::Device::blit_from_color_buffer(): unimplemented");
}

void Device::blit_from_depth_buffer(void*, Vector2<i32>, GPU::ImageDataLayout const&)
{
    dbgln("VirtGPU::Device::blit_from_depth_buffer(): unimplemented");
}

void Device::blit_from_depth_buffer(NonnullRefPtr<GPU::Image>, u32, Vector2<u32>, Vector2<i32>, Vector3<i32>)
{
    dbgln("VirtGPU::Device::blit_from_depth_buffer(): unimplemented");
}

void Device::blit_to_color_buffer_at_raster_position(void const*, GPU::ImageDataLayout const&)
{
    dbgln("VirtGPU::Device::blit_to_color_buffer_at_raster_position(): unimplemented");
}

void Device::blit_to_depth_buffer_at_raster_position(void const*, GPU::ImageDataLayout const&)
{
    dbgln("VirtGPU::Device::blit_to_depth_buffer_at_raster_position(): unimplemented");
}

void Device::set_options(GPU::RasterizerOptions const&)
{
    dbgln("VirtGPU::Device::set_options(): unimplemented");
}

void Device::set_light_model_params(GPU::LightModelParameters const&)
{
    dbgln("VirtGPU::Device::set_light_model_params(): unimplemented");
}

GPU::RasterizerOptions Device::options() const
{
    dbgln("VirtGPU::Device::options(): unimplemented");
    return {};
}

GPU::LightModelParameters Device::light_model() const
{
    dbgln("VirtGPU::Device::light_model(): unimplemented");
    return {};
}

NonnullRefPtr<GPU::Image> Device::create_image(GPU::PixelFormat const& pixel_format, u32 width, u32 height, u32 depth, u32 max_levels)
{
    dbgln("VirtGPU::Device::create_image(): unimplemented");
    return adopt_ref(*new Image(this, pixel_format, width, height, depth, max_levels));
}

ErrorOr<NonnullRefPtr<GPU::Shader>> Device::create_shader(GPU::IR::Shader const&)
{
    dbgln("VirtGPU::Device::create_shader(): unimplemented");
    return adopt_ref(*new Shader(this));
}

void Device::set_sampler_config(unsigned, GPU::SamplerConfig const&)
{
    dbgln("VirtGPU::Device::set_sampler_config(): unimplemented");
}

void Device::set_light_state(unsigned, GPU::Light const&)
{
    dbgln("VirtGPU::Device::set_light_state(): unimplemented");
}

void Device::set_material_state(GPU::Face, GPU::Material const&)
{
    dbgln("VirtGPU::Device::set_material_state(): unimplemented");
}

void Device::set_stencil_configuration(GPU::Face, GPU::StencilConfiguration const&)
{
    dbgln("VirtGPU::Device::set_stencil_configuration(): unimplemented");
}

void Device::set_texture_unit_configuration(GPU::TextureUnitIndex, GPU::TextureUnitConfiguration const&)
{
    dbgln("VirtGPU::Device::set_texture_unit_configuration(): unimplemented");
}

void Device::set_clip_planes(Vector<FloatVector4> const&)
{
    dbgln("VirtGPU::Device::set_clip_planes(): unimplemented");
}

GPU::RasterPosition Device::raster_position() const
{
    dbgln("VirtGPU::Device::raster_position(): unimplemented");
    return {};
}

void Device::set_raster_position(GPU::RasterPosition const&)
{
    dbgln("VirtGPU::Device::set_raster_position(): unimplemented");
}

void Device::set_raster_position(FloatVector4 const&, FloatMatrix4x4 const&, FloatMatrix4x4 const&)
{
    dbgln("VirtGPU::Device::set_raster_position(): unimplemented");
}

void Device::bind_fragment_shader(RefPtr<GPU::Shader>)
{
    dbgln("VirtGPU::Device::bind_fragment_shader(): unimplemented");
}

}

extern "C" GPU::Device* serenity_gpu_create_device(Gfx::IntSize size)
{
    auto device_or_error = VirtGPU::Device::create(size);
    if (device_or_error.is_error())
        return nullptr;

    return device_or_error.release_value().leak_ptr();
}
