/*
 * Copyright (c) 2022, Sahan Fernando <sahan.h.fernando@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <Kernel/API/VirGL.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibGfx/Matrix4x4.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/ioctl_numbers.h>
#include <unistd.h>

#include "CommandBufferBuilder.h"
#include "VirGLProtocol.h"
#include "Widget.h"

static constexpr StringView frag_shader = "FRAG\n"
                                          "PROPERTY FS_COLOR0_WRITES_ALL_CBUFS 1\n"
                                          "DCL IN[0], COLOR, COLOR\n"
                                          "DCL OUT[0], COLOR\n"
                                          "  0: MOV OUT[0], IN[0]\n"
                                          "  1: END\n";

static constexpr StringView vert_shader = "VERT\n"
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
                                          "  5: END\n";

struct VertexData {
    float r;
    float g;
    float b;
    float x;
    float y;
    float z;
};

int gpu_fd;
ResourceID vbo_resource_id;
ResourceID drawtarget;
ResourceID depthbuffer_surface;
ObjectHandle blend_handle;
ObjectHandle drawtarget_surface_handle;
ObjectHandle depthbuffer_surface_handle;
ObjectHandle ve_handle;
ObjectHandle frag_shader_handle;
ObjectHandle vert_shader_handle;
ObjectHandle rasterizer_handle;
ObjectHandle dsa_handle;
Vector<VertexData> g_vertices;

static ObjectHandle allocate_handle()
{
    static u32 last_allocated_handle = 0;
    return { ++last_allocated_handle };
}

static void upload_command_buffer(Vector<u32> const& command_buffer)
{
    VERIFY(command_buffer.size() <= NumericLimits<u32>::max());
    VirGLCommandBuffer command_buffer_descriptor {
        .data = command_buffer.data(),
        .num_elems = (u32)command_buffer.size(),
    };
    VERIFY(ioctl(gpu_fd, VIRGL_IOCTL_SUBMIT_CMD, &command_buffer_descriptor) >= 0);
}

static ResourceID create_virgl_resource(VirGL3DResourceSpec& spec)
{
    VERIFY(ioctl(gpu_fd, VIRGL_IOCTL_CREATE_RESOURCE, &spec) >= 0);
    return spec.created_resource_id;
}

static Vector<VertexData> gen_vertex_data()
{
    Vector<VertexData> data;
    static constexpr Array<VertexData, 8> vertices = {
        VertexData { .r = 0, .g = 0, .b = 0, .x = -0.5, .y = -0.5, .z = -0.5 },
        VertexData { .r = 0, .g = 0, .b = 0, .x = 0.5, .y = -0.5, .z = -0.5 },
        VertexData { .r = 0, .g = 0, .b = 0, .x = -0.5, .y = 0.5, .z = -0.5 },
        VertexData { .r = 0, .g = 0, .b = 0, .x = 0.5, .y = 0.5, .z = -0.5 },
        VertexData { .r = 0, .g = 0, .b = 0, .x = -0.5, .y = -0.5, .z = 0.5 },
        VertexData { .r = 0, .g = 0, .b = 0, .x = 0.5, .y = -0.5, .z = 0.5 },
        VertexData { .r = 0, .g = 0, .b = 0, .x = -0.5, .y = 0.5, .z = 0.5 },
        VertexData { .r = 0, .g = 0, .b = 0, .x = 0.5, .y = 0.5, .z = 0.5 },
    };
    static constexpr Array<size_t, 36> tris = {
        0, 1, 2, 1, 3, 2, // Top
        4, 0, 6, 0, 2, 6, // Left
        4, 5, 0, 5, 1, 0, // Up
        1, 5, 3, 5, 7, 3, // Right
        2, 3, 6, 3, 7, 6, // Down
        5, 4, 7, 4, 6, 7, // Bottom
    };
    for (auto index : tris) {
        data.append(vertices[index]);
    }
    // Choose random colors for each face of the cube
    for (auto i = 0; i < 6; ++i) {
        float red = (rand() % 256) / 255.f;
        float green = (rand() % 256) / 255.f;
        float blue = (rand() % 256) / 255.f;
        for (auto j = 0; j < 6; ++j) {
            auto& vertex = data[i * 6 + j];
            vertex.r = red;
            vertex.g = green;
            vertex.b = blue;
        }
    }
    return data;
}

static void init()
{
    // Open the device
    gpu_fd = open("/dev/gpu/render0", O_RDWR);
    VERIFY(gpu_fd >= 0);
    // Create a virgl context for this file descriptor
    VERIFY(ioctl(gpu_fd, VIRGL_IOCTL_CREATE_CONTEXT) >= 0);
    // Create a VertexElements resource
    VirGL3DResourceSpec vbo_spec {
        .target = to_underlying(Gallium::PipeTextureTarget::BUFFER), // pipe_texture_target
        .format = 45,                                                // pipe_to_virgl_format
        .bind = VIRGL_BIND_VERTEX_BUFFER,
        .width = PAGE_SIZE,
        .height = 1,
        .depth = 1,
        .array_size = 1,
        .last_level = 0,
        .nr_samples = 0,
        .flags = 0,
        .created_resource_id = 0,
    };
    vbo_resource_id = create_virgl_resource(vbo_spec);
    // Create a texture to draw to
    VirGL3DResourceSpec drawtarget_spec {
        .target = to_underlying(Gallium::PipeTextureTarget::TEXTURE_RECT),                  // pipe_texture_target
        .format = to_underlying(Protocol::TextureFormat::VIRTIO_GPU_FORMAT_B8G8R8A8_UNORM), // pipe_to_virgl_format
        .bind = VIRGL_BIND_RENDER_TARGET,
        .width = DRAWTARGET_WIDTH,
        .height = DRAWTARGET_HEIGHT,
        .depth = 1,
        .array_size = 1,
        .last_level = 0,
        .nr_samples = 0,
        .flags = 0,
        .created_resource_id = 0,
    };
    drawtarget = create_virgl_resource(drawtarget_spec);
    // Create a depthbuffer surface
    VirGL3DResourceSpec depthbuffer_surface_spec {
        .target = to_underlying(Gallium::PipeTextureTarget::TEXTURE_RECT),             // pipe_texture_target
        .format = to_underlying(Protocol::TextureFormat::VIRTIO_GPU_FORMAT_Z32_FLOAT), // pipe_to_virgl_format
        .bind = VIRGL_BIND_RENDER_TARGET | VIRGL_BIND_DEPTH_STENCIL,
        .width = DRAWTARGET_WIDTH,
        .height = DRAWTARGET_HEIGHT,
        .depth = 1,
        .array_size = 1,
        .last_level = 0,
        .nr_samples = 0,
        .flags = 0,
        .created_resource_id = 0,
    };
    depthbuffer_surface = create_virgl_resource(depthbuffer_surface_spec);

    // Initialize all required state
    CommandBufferBuilder builder;
    // Create and set the blend, to control the color mask
    blend_handle = allocate_handle();
    builder.append_create_blend(blend_handle);
    builder.append_bind_blend(blend_handle);
    // Create drawtarget surface
    drawtarget_surface_handle = allocate_handle();
    builder.append_create_surface(drawtarget, drawtarget_surface_handle, Protocol::TextureFormat::VIRTIO_GPU_FORMAT_B8G8R8A8_UNORM);
    // Create depthbuffer surface
    depthbuffer_surface_handle = allocate_handle();
    builder.append_create_surface(depthbuffer_surface, depthbuffer_surface_handle, Protocol::TextureFormat::VIRTIO_GPU_FORMAT_Z32_FLOAT);
    // Set some framebuffer state (attached handle, framebuffer size, etc)
    builder.append_set_framebuffer_state(drawtarget_surface_handle, depthbuffer_surface_handle);
    builder.append_set_framebuffer_state_no_attach();
    // Set the vertex buffer
    builder.append_set_vertex_buffers(sizeof(VertexData), 0, vbo_resource_id);
    // Create and bind fragment shader
    frag_shader_handle = allocate_handle();
    builder.append_create_shader(frag_shader_handle, Gallium::ShaderType::SHADER_FRAGMENT, frag_shader);
    builder.append_bind_shader(frag_shader_handle, Gallium::ShaderType::SHADER_FRAGMENT);
    // Create and bind vertex shader
    vert_shader_handle = allocate_handle();
    builder.append_create_shader(vert_shader_handle, Gallium::ShaderType::SHADER_VERTEX, vert_shader);
    builder.append_bind_shader(vert_shader_handle, Gallium::ShaderType::SHADER_VERTEX);
    // Create a VertexElements object (used to specify layout of vertex data)
    ve_handle = allocate_handle();
    builder.append_create_vertex_elements(ve_handle);
    builder.append_bind_vertex_elements(ve_handle);
    // Create a DepthStencilAlpha (DSA) object
    dsa_handle = allocate_handle();
    builder.append_create_dsa(dsa_handle);
    builder.append_bind_dsa(dsa_handle);
    // Create a Rasterizer object
    rasterizer_handle = allocate_handle();
    builder.append_create_rasterizer(rasterizer_handle);
    builder.append_bind_rasterizer(rasterizer_handle);
    // Set the Viewport
    builder.append_gl_viewport();
    // Upload buffer
    upload_command_buffer(builder.build());

    // Setup the vertex data
    g_vertices = gen_vertex_data();
}

static Gfx::FloatMatrix4x4 get_transform_matrix(unsigned step_num)
{
    auto mat = Gfx::FloatMatrix4x4::identity();
    float angle = step_num * 0.02;
    mat = mat * Gfx::rotation_matrix(FloatVector3(1, 0, 0), angle * 1.17356641f);
    mat = mat * Gfx::rotation_matrix(FloatVector3(0, 1, 0), angle * 0.90533273f);
    mat = mat * Gfx::rotation_matrix(FloatVector3(0, 0, 1), angle);
    return mat;
}

static Vector<float> encode_constant_buffer(Gfx::FloatMatrix4x4 const& mat)
{
    // Flip the y axis. This is done because OpenGLs coordinate space has a Y-axis of
    // Opposite direction to that of LibGfx
    Gfx::FloatMatrix4x4 flip_y = Gfx::FloatMatrix4x4::identity();
    flip_y.elements()[1][1] = -1;
    auto real_mat = mat * flip_y;
    Vector<float> values;
    for (int i = 0; i < 4; ++i) {
        for (int j = 0; j < 4; ++j) {
            values.append(real_mat.elements()[i][j]);
        }
    }
    return values;
}

static void draw_frame(unsigned step_num)
{
    // Get model matrix
    auto model_matrix = get_transform_matrix(step_num);
    VirGLTransferDescriptor descriptor {
        .data = (void*)g_vertices.data(),
        .offset_in_region = 0,
        .num_bytes = sizeof(VertexData) * g_vertices.size(),
        .direction = VIRGL_DATA_DIR_GUEST_TO_HOST,
    };
    // Transfer data from vertices array to kernel virgl transfer region
    VERIFY(ioctl(gpu_fd, VIRGL_IOCTL_TRANSFER_DATA, &descriptor) >= 0);
    // Create command buffer
    CommandBufferBuilder builder;
    // Transfer data from kernel virgl transfer region to host resource
    builder.append_transfer3d(vbo_resource_id, sizeof(VertexData) * g_vertices.size(), 1, 1, VIRGL_DATA_DIR_GUEST_TO_HOST);
    builder.append_end_transfers_3d();
    // Set the constant buffer to the identity matrix
    builder.append_set_constant_buffer(encode_constant_buffer(model_matrix));
    // Clear the framebuffer
    builder.append_gl_clear(0, 0, 0);
    // Draw the vbo
    builder.append_draw_vbo(g_vertices.size());
    // Upload the buffer
    upload_command_buffer(builder.build());
}

void update_frame(RefPtr<Gfx::Bitmap> target, unsigned num_cycles)
{
    VERIFY(target->width() == DRAWTARGET_WIDTH);
    VERIFY(target->height() == DRAWTARGET_HEIGHT);
    // Run logic to draw the frame
    draw_frame(num_cycles);
    // Transfer data back from hypervisor to kernel transfer region
    CommandBufferBuilder builder;
    builder.append_transfer3d(drawtarget, DRAWTARGET_WIDTH, DRAWTARGET_HEIGHT, 1, VIRGL_DATA_DIR_HOST_TO_GUEST);
    builder.append_end_transfers_3d();
    upload_command_buffer(builder.build());
    // Copy from kernel transfer region to userspace
    VirGLTransferDescriptor descriptor {
        .data = (void*)target->scanline_u8(0),
        .offset_in_region = 0,
        .num_bytes = DRAWTARGET_WIDTH * DRAWTARGET_HEIGHT * sizeof(u32),
        .direction = VIRGL_DATA_DIR_HOST_TO_GUEST,
    };
    VERIFY(ioctl(gpu_fd, VIRGL_IOCTL_TRANSFER_DATA, &descriptor) >= 0);
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::try_create(arguments));

    auto window = TRY(GUI::Window::try_create());
    window->set_double_buffering_enabled(true);
    window->set_title("VirGLDemo");
    window->set_resizable(false);
    window->resize(DRAWTARGET_WIDTH, DRAWTARGET_HEIGHT);
    window->set_has_alpha_channel(false);
    window->set_alpha_hit_threshold(1);

    auto demo = TRY(window->try_set_main_widget<Demo>());

    auto app_icon = GUI::Icon::default_icon("app-cube");
    window->set_icon(app_icon.bitmap_for_size(16));

    init();
    window->show();

    return app->exec();
}
