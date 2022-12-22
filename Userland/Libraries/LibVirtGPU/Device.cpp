/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Sahan Fernando <sahan.h.fernando@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <Kernel/API/VirGL.h>
#include <LibCore/System.h>
#include <LibVirtGPU/CommandBufferBuilder.h>
#include <LibVirtGPU/Device.h>
#include <LibVirtGPU/Image.h>
#include <LibVirtGPU/Shader.h>
#include <LibVirtGPU/VirGLProtocol.h>

namespace VirtGPU {

static constexpr auto frag_shader = "FRAG\n"
                                    "PROPERTY FS_COLOR0_WRITES_ALL_CBUFS 1\n"
                                    "DCL IN[0], COLOR, COLOR\n"
                                    "DCL OUT[0], COLOR\n"
                                    "  0: MOV OUT[0], IN[0]\n"
                                    "  1: END\n"sv;

static constexpr auto vert_shader = "VERT\n"
                                    "DCL IN[0]\n"
                                    "DCL IN[1]\n"
                                    "DCL OUT[0], POSITION\n"
                                    "DCL OUT[1], COLOR\n"
                                    "DCL CONST[0..3]\n"
                                    "DCL TEMP[0..1]\n"
                                    "  0: MUL TEMP[0], IN[0].xxxx, CONST[0]\n"
                                    "  1: MAD TEMP[1], IN[0].yyyy, CONST[1], TEMP[0]\n"
                                    "  2: MAD TEMP[0], IN[0].zzzz, CONST[2], TEMP[1]\n"
                                    "  3: MAD OUT[0], IN[0].wwww, CONST[3], TEMP[0]\n"
                                    "  4: MOV_SAT OUT[1], IN[1]\n"
                                    "  5: END\n"sv;

Device::Device(NonnullRefPtr<Core::File> gpu_file)
    : m_gpu_file { gpu_file }
{
}

ErrorOr<NonnullOwnPtr<Device>> Device::create(Gfx::IntSize min_size)
{
    auto file = TRY(Core::File::open("/dev/gpu/render0", Core::OpenMode::ReadWrite));
    auto device = make<Device>(file);
    TRY(device->initialize_context(min_size));
    return device;
}

ErrorOr<void> Device::initialize_context(Gfx::IntSize min_size)
{
    // Create a virgl context for this file descriptor
    TRY(Core::System::ioctl(m_gpu_file->fd(), VIRGL_IOCTL_CREATE_CONTEXT));

    // Create a VertexElements resource
    VirGL3DResourceSpec vbo_spec {
        .target = to_underlying(Gallium::PipeTextureTarget::BUFFER), // pipe_texture_target
        .format = 0,                                                 // untyped buffer
        .bind = to_underlying(Protocol::BindTarget::VIRGL_BIND_VERTEX_BUFFER),
        .width = PAGE_SIZE * 256,
        .height = 1,
        .depth = 1,
        .array_size = 1,
        .last_level = 0,
        .nr_samples = 0,
        .flags = 0,
        .created_resource_id = 0,
    };
    m_vbo_resource_id = TRY(create_virgl_resource(vbo_spec));

    // Create a texture to draw to
    VirGL3DResourceSpec drawtarget_spec {
        .target = to_underlying(Gallium::PipeTextureTarget::TEXTURE_RECT),                  // pipe_texture_target
        .format = to_underlying(Protocol::TextureFormat::VIRTIO_GPU_FORMAT_B8G8R8A8_UNORM), // pipe_to_virgl_format
        .bind = to_underlying(Protocol::BindTarget::VIRGL_BIND_RENDER_TARGET),
        .width = static_cast<u32>(min_size.width()),
        .height = static_cast<u32>(min_size.height()),
        .depth = 1,
        .array_size = 1,
        .last_level = 0,
        .nr_samples = 0,
        .flags = 0,
        .created_resource_id = 0,
    };
    m_drawtarget = TRY(create_virgl_resource(drawtarget_spec));

    // Create a depthbuffer surface
    VirGL3DResourceSpec depthbuffer_surface_spec {
        .target = to_underlying(Gallium::PipeTextureTarget::TEXTURE_RECT),             // pipe_texture_target
        .format = to_underlying(Protocol::TextureFormat::VIRTIO_GPU_FORMAT_Z32_FLOAT), // pipe_to_virgl_format
        .bind = to_underlying(Protocol::BindTarget::VIRGL_BIND_RENDER_TARGET) | to_underlying(Protocol::BindTarget::VIRGL_BIND_DEPTH_STENCIL),
        .width = static_cast<u32>(min_size.width()),
        .height = static_cast<u32>(min_size.height()),
        .depth = 1,
        .array_size = 1,
        .last_level = 0,
        .nr_samples = 0,
        .flags = 0,
        .created_resource_id = 0,
    };
    m_depthbuffer_surface = TRY(create_virgl_resource(depthbuffer_surface_spec));

    // Initialize all required state
    CommandBufferBuilder builder;

    // Create and set the blend, to control the color mask
    m_blend_handle = allocate_handle();
    builder.append_create_blend(m_blend_handle);
    builder.append_bind_blend(m_blend_handle);

    // Create drawtarget surface
    m_drawtarget_surface_handle = allocate_handle();
    builder.append_create_surface(m_drawtarget, m_drawtarget_surface_handle, Protocol::TextureFormat::VIRTIO_GPU_FORMAT_B8G8R8A8_UNORM);

    // Create depthbuffer surface
    m_depthbuffer_surface_handle = allocate_handle();
    builder.append_create_surface(m_depthbuffer_surface, m_depthbuffer_surface_handle, Protocol::TextureFormat::VIRTIO_GPU_FORMAT_Z32_FLOAT);

    // Set some framebuffer state (attached handle, framebuffer size, etc)
    builder.append_set_framebuffer_state(m_drawtarget_surface_handle, m_depthbuffer_surface_handle);
    builder.append_set_framebuffer_state_no_attach(min_size);

    // Set the vertex buffer
    builder.append_set_vertex_buffers(sizeof(VertexData), 0, m_vbo_resource_id);

    // Create and bind fragment shader
    m_frag_shader_handle = allocate_handle();
    builder.append_create_shader(m_frag_shader_handle, Gallium::ShaderType::SHADER_FRAGMENT, frag_shader);
    builder.append_bind_shader(m_frag_shader_handle, Gallium::ShaderType::SHADER_FRAGMENT);

    // Create and bind vertex shader
    m_vert_shader_handle = allocate_handle();
    builder.append_create_shader(m_vert_shader_handle, Gallium::ShaderType::SHADER_VERTEX, vert_shader);
    builder.append_bind_shader(m_vert_shader_handle, Gallium::ShaderType::SHADER_VERTEX);

    // Create a VertexElements object (used to specify layout of vertex data)
    m_ve_handle = allocate_handle();
    Vector<CreateVertexElementsCommand::ElementBinding> element_bindings {
        { .offset = 12, .divisor = 0, .vertex_buffer_index = 0, .format = Gallium::PipeFormat::R32G32B32_FLOAT },
        { .offset = 0, .divisor = 0, .vertex_buffer_index = 0, .format = Gallium::PipeFormat::R32G32B32_FLOAT },
    };
    builder.append_create_vertex_elements(m_ve_handle, element_bindings);
    builder.append_bind_vertex_elements(m_ve_handle);

    // Create a DepthStencilAlpha (DSA) object
    m_dsa_handle = allocate_handle();
    builder.append_create_dsa(m_dsa_handle);
    builder.append_bind_dsa(m_dsa_handle);

    // Create a Rasterizer object
    m_rasterizer_handle = allocate_handle();
    builder.append_create_rasterizer(m_rasterizer_handle);
    builder.append_bind_rasterizer(m_rasterizer_handle);

    // Set the Viewport
    builder.append_viewport(min_size);

    // Upload buffer
    TRY(upload_command_buffer(builder.build()));

    return {};
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

Protocol::ObjectHandle Device::allocate_handle()
{
    return { ++m_last_allocated_handle };
}

ErrorOr<void> Device::upload_command_buffer(Vector<u32> const& command_buffer)
{
    VERIFY(command_buffer.size() <= NumericLimits<u32>::max());
    VirGLCommandBuffer command_buffer_descriptor {
        .data = command_buffer.data(),
        .num_elems = static_cast<u32>(command_buffer.size()),
    };
    TRY(Core::System::ioctl(m_gpu_file->fd(), VIRGL_IOCTL_SUBMIT_CMD, &command_buffer_descriptor));

    return {};
}

ErrorOr<Protocol::ResourceID> Device::create_virgl_resource(VirGL3DResourceSpec& spec)
{
    TRY(Core::System::ioctl(m_gpu_file->fd(), VIRGL_IOCTL_CREATE_RESOURCE, &spec));
    return Protocol::ResourceID { spec.created_resource_id };
}

}

extern "C" GPU::Device* serenity_gpu_create_device(Gfx::IntSize size)
{
    auto device_or_error = VirtGPU::Device::create(size);
    if (device_or_error.is_error())
        return nullptr;

    return device_or_error.release_value().leak_ptr();
}
