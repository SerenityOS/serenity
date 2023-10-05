/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Sahan Fernando <sahan.h.fernando@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <Kernel/API/VirGL.h>
#include <LibCore/File.h>
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

Device::Device(NonnullOwnPtr<Core::File> gpu_file)
    : m_gpu_file { move(gpu_file) }
{
}

ErrorOr<NonnullOwnPtr<Device>> Device::create(Gfx::IntSize min_size)
{
    auto file = TRY(Core::File::open("/dev/gpu/render0"sv, Core::File::OpenMode::ReadWrite));
    auto device = make<Device>(move(file));
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

void Device::encode_constant_buffer(Gfx::FloatMatrix4x4 const& matrix, Vector<float>& buffer)
{
    buffer.clear_with_capacity();
    for (int i = 0; i < 4; ++i) {
        for (int j = 0; j < 4; ++j) {
            buffer.append(matrix.elements()[i][j]);
        }
    }
}

void Device::draw_primitives(GPU::PrimitiveType primitive_type, Vector<GPU::Vertex>& vertices)
{
    // Transform incoming vertices to our own format.
    m_vertices.clear_with_capacity();
    for (auto& vertex : vertices) {
        m_vertices.append({
            vertex.tex_coords[0].x(),
            vertex.tex_coords[0].y(),
            vertex.tex_coords[0].z(),
            vertex.position.x(),
            vertex.position.y(),
            vertex.position.z(),
        });
    }

    // Compute combined transform matrix
    // Flip the y axis. This is done because OpenGLs coordinate space has a Y-axis of
    // Opposite direction to that of LibGfx
    auto combined_matrix = (Gfx::scale_matrix(FloatVector3 { 1, -1, 1 }) * m_projection_transform * m_model_view_transform).transpose();
    encode_constant_buffer(combined_matrix, m_constant_buffer_data);

    // Create command buffer
    CommandBufferBuilder builder;

    // Set the constant buffer to the combined transformation matrix
    builder.append_set_constant_buffer(m_constant_buffer_data);

    // Transfer data from vertices array to kernel virgl transfer region
    VirGLTransferDescriptor descriptor {
        .data = m_vertices.data(),
        .offset_in_region = 0,
        .num_bytes = sizeof(VertexData) * m_vertices.size(),
        .direction = VIRGL_DATA_DIR_GUEST_TO_HOST,
    };
    MUST(Core::System::ioctl(m_gpu_file->fd(), VIRGL_IOCTL_TRANSFER_DATA, &descriptor));

    // Transfer data from kernel virgl transfer region to host resource
    builder.append_transfer3d(m_vbo_resource_id, sizeof(VertexData) * m_vertices.size(), 1, 1, VIRGL_DATA_DIR_GUEST_TO_HOST);
    builder.append_end_transfers_3d();

    // Set the constant buffer to the identity matrix
    builder.append_set_constant_buffer(m_constant_buffer_data);

    constexpr auto map_primitive_type = [](GPU::PrimitiveType type) constexpr {
        switch (type) {
        case GPU::PrimitiveType::Lines:
            return Protocol::PipePrimitiveTypes::LINES;
        case GPU::PrimitiveType::LineLoop:
            return Protocol::PipePrimitiveTypes::LINE_LOOP;
        case GPU::PrimitiveType::LineStrip:
            return Protocol::PipePrimitiveTypes::LINE_STRIP;
        case GPU::PrimitiveType::Points:
            return Protocol::PipePrimitiveTypes::POINTS;
        case GPU::PrimitiveType::TriangleFan:
            return Protocol::PipePrimitiveTypes::TRIANGLE_FAN;
        case GPU::PrimitiveType::Triangles:
            return Protocol::PipePrimitiveTypes::TRIANGLES;
        case GPU::PrimitiveType::TriangleStrip:
            return Protocol::PipePrimitiveTypes::TRIANGLE_STRIP;
        case GPU::PrimitiveType::Quads:
            return Protocol::PipePrimitiveTypes::QUADS;
        default:
            VERIFY_NOT_REACHED();
        }
    };

    // Draw the vbo
    builder.append_draw_vbo(map_primitive_type(primitive_type), m_vertices.size());

    // Upload the buffer
    MUST(upload_command_buffer(builder.build()));
}

void Device::resize(Gfx::IntSize)
{
    dbgln("VirtGPU::Device::resize(): unimplemented");
}

void Device::clear_color(FloatVector4 const& color)
{
    CommandBufferBuilder builder;
    builder.append_clear(color.x(), color.y(), color.z(), color.w());
    MUST(upload_command_buffer(builder.build()));
}

void Device::clear_depth(GPU::DepthType depth)
{
    CommandBufferBuilder builder;
    builder.append_clear(depth);
    MUST(upload_command_buffer(builder.build()));
}

void Device::clear_stencil(GPU::StencilType)
{
    dbgln("VirtGPU::Device::clear_stencil(): unimplemented");
}

void Device::blit_from_color_buffer(Gfx::Bitmap& front_buffer)
{
    // Transfer data back from hypervisor to kernel transfer region
    CommandBufferBuilder builder;
    builder.append_transfer3d(m_drawtarget, front_buffer.size().width(), front_buffer.size().height(), 1, VIRGL_DATA_DIR_HOST_TO_GUEST);
    builder.append_end_transfers_3d();
    MUST(upload_command_buffer(builder.build()));

    // Copy from kernel transfer region to userspace
    VirGLTransferDescriptor descriptor {
        .data = front_buffer.scanline_u8(0),
        .offset_in_region = 0,
        .num_bytes = front_buffer.size().width() * front_buffer.size().height() * sizeof(u32),
        .direction = VIRGL_DATA_DIR_HOST_TO_GUEST,
    };
    MUST(Core::System::ioctl(m_gpu_file->fd(), VIRGL_IOCTL_TRANSFER_DATA, &descriptor));
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

void Device::set_model_view_transform(Gfx::FloatMatrix4x4 const& model_view_transform)
{
    m_model_view_transform = model_view_transform;
}

void Device::set_projection_transform(Gfx::FloatMatrix4x4 const& projection_transform)
{
    m_projection_transform = projection_transform;
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

void Device::set_raster_position(FloatVector4 const&)
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
