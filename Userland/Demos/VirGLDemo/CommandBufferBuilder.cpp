/*
 * Copyright (c) 2022, Sahan Fernando <sahan.h.fernando@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/API/VirGL.h>
#include <sys/ioctl_numbers.h>

#include "CommandBufferBuilder.h"
#include "VirGLProtocol.h"
#include "Widget.h"

static u32 encode_command(u32 length, u32 mid, Protocol::VirGLCommand command)
{
    u32 command_value = to_underlying(command);
    return (length << 16) | ((mid & 0xff) << 8) | (command_value & 0xff);
};

class CommandBuilder {
public:
    CommandBuilder() = delete;
    CommandBuilder(Vector<u32>& buffer, Protocol::VirGLCommand command, u32 mid)
        : m_buffer(buffer)
        , m_start_offset(buffer.size())
        , m_command(command)
        , m_command_mid(mid)
    {
        m_buffer.append(0);
    }
    void appendu32(u32 value)
    {
        VERIFY(!m_finalized);
        m_buffer.append(value);
    }
    void appendf32(float value)
    {
        VERIFY(!m_finalized);
        m_buffer.append(bit_cast<u32>(value));
    }
    void appendf64(double value)
    {
        VERIFY(!m_finalized);
        m_buffer.append(0);
        m_buffer.append(0);
        auto* depth = (u64*)(&m_buffer[m_buffer.size() - 2]);
        *depth = bit_cast<u64>(value);
    }
    void append_string_null_padded(StringView string)
    {
        VERIFY(!m_finalized);
        // Remember to have at least one null terminator byte
        auto length = string.length() + 1;
        auto num_required_words = (length + sizeof(u32) - 1) / sizeof(u32);
        m_buffer.resize(m_buffer.size() + num_required_words);
        char* dest = (char*)&m_buffer[m_buffer.size() - num_required_words];
        memcpy(dest, string.characters_without_null_termination(), string.length());
        // Pad end with null bytes
        memset(&dest[string.length()], 0, 4 * num_required_words - string.length());
    }
    void finalize()
    {
        if (!m_finalized) {
            m_finalized = true;
            size_t num_elems = m_buffer.size() - m_start_offset - 1;
            m_buffer[m_start_offset] = encode_command(num_elems, m_command_mid, m_command);
        }
    }
    ~CommandBuilder()
    {
        if (!m_finalized)
            finalize();
    }

private:
    Vector<u32>& m_buffer;
    size_t m_start_offset;
    Protocol::VirGLCommand m_command;
    u32 m_command_mid;
    bool m_finalized { false };
};

void CommandBufferBuilder::append_set_tweaks(u32 id, u32 value)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::SET_TWEAKS, 0);
    builder.appendu32(id);
    builder.appendu32(value);
}

void CommandBufferBuilder::append_transfer3d(ResourceID resource, size_t width, size_t height, size_t depth, size_t direction)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::TRANSFER3D, 0);
    builder.appendu32(resource.value()); // res_handle
    builder.appendu32(0);                // level
    builder.appendu32(242);              // usage
    builder.appendu32(0);                // stride
    builder.appendu32(0);                // layer_stride
    builder.appendu32(0);                // x
    builder.appendu32(0);                // y
    builder.appendu32(0);                // z
    builder.appendu32(width);            // width
    builder.appendu32(height);           // height
    builder.appendu32(depth);            // depth
    builder.appendu32(0);                // data_offset
    builder.appendu32(direction);        // direction
}

void CommandBufferBuilder::append_end_transfers_3d()
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::END_TRANSFERS, 0);
}

void CommandBufferBuilder::append_draw_vbo(u32 count)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::DRAW_VBO, 0);
    builder.appendu32(0);                                                      // start
    builder.appendu32(count);                                                  // count
    builder.appendu32(to_underlying(Protocol::PipePrimitiveTypes::TRIANGLES)); // mode
    builder.appendu32(0);                                                      // indexed
    builder.appendu32(1);                                                      // instance_count
    builder.appendu32(0);                                                      // index_bias
    builder.appendu32(0);                                                      // start_instance
    builder.appendu32(0);                                                      // primitive_restart
    builder.appendu32(0);                                                      // restart_index
    builder.appendu32(0);                                                      // min_index
    builder.appendu32(0xffffffff);                                             // max_index
    builder.appendu32(0);                                                      // cso
}

void CommandBufferBuilder::append_gl_clear(float r, float g, float b)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CLEAR, 0);
    Protocol::ClearType clear_flags {};
    clear_flags.flags.depth = 1;
    clear_flags.flags.color0 = 1;
    builder.appendu32(clear_flags.value);
    builder.appendf32(r);
    builder.appendf32(g);
    builder.appendf32(b);
    builder.appendf32(1.0f); // Alpha
    builder.appendf64(1.0);  // Depth
    builder.appendu32(0);    // Stencil
}

void CommandBufferBuilder::append_set_vertex_buffers(u32 stride, u32 offset, ResourceID resource)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::SET_VERTEX_BUFFERS, 0);
    builder.appendu32(stride);
    builder.appendu32(offset);
    builder.appendu32(resource.value());
}

void CommandBufferBuilder::append_create_blend(ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CREATE_OBJECT, to_underlying(Protocol::ObjectType::BLEND));
    builder.appendu32(handle.value());
    builder.appendu32(4); // Enable dither flag, and nothing else
    builder.appendu32(0);
    builder.appendu32(0x78000000); // Enable all bits of color mask for color buffer 0, and nothing else
    for (size_t i = 1; i < 8; ++i) {
        builder.appendu32(0); // Explicitly disable all flags for other color buffers
    }
}

void CommandBufferBuilder::append_bind_blend(ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::BIND_OBJECT, to_underlying(Protocol::ObjectType::BLEND));
    builder.appendu32(handle.value()); // VIRGL_OBJ_BIND_HANDLE
}

void CommandBufferBuilder::append_create_vertex_elements(ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CREATE_OBJECT, to_underlying(Protocol::ObjectType::VERTEX_ELEMENTS));
    builder.appendu32(handle.value());
    builder.appendu32(12); // src_offset_0
    builder.appendu32(0);  // instance_divisor_0
    builder.appendu32(0);  // vertex_buffer_index_0
    builder.appendu32(30); // src_format_0 (PIPE_FORMAT_R32G32B32_FLOAT = 30)
    builder.appendu32(0);  // src_offset_1
    builder.appendu32(0);  // instance_divisor_1
    builder.appendu32(0);  // vertex_buffer_index_1
    builder.appendu32(30); // src_format_1 (PIPE_FORMAT_R32G32B32_FLOAT = 30)
}

void CommandBufferBuilder::append_bind_vertex_elements(ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::BIND_OBJECT, to_underlying(Protocol::ObjectType::VERTEX_ELEMENTS));
    builder.appendu32(handle.value()); // VIRGL_OBJ_BIND_HANDLE
}

void CommandBufferBuilder::append_create_surface(ResourceID drawtarget_resource, ObjectHandle drawtarget_handle, Protocol::TextureFormat format)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CREATE_OBJECT, to_underlying(Protocol::ObjectType::SURFACE));
    builder.appendu32(drawtarget_handle.value());
    builder.appendu32(drawtarget_resource.value());
    builder.appendu32(to_underlying(format));
    builder.appendu32(0); // First element / Texture Level
    builder.appendu32(0); // Last element / Texture Element
}

void CommandBufferBuilder::append_set_framebuffer_state(ObjectHandle drawtarget, ObjectHandle depthbuffer)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::SET_FRAMEBUFFER_STATE, 0);
    builder.appendu32(1);                   // nr_cbufs
    builder.appendu32(depthbuffer.value()); // zsurf_handle
    builder.appendu32(drawtarget.value());  // surf_handle
}

void CommandBufferBuilder::append_gl_viewport()
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::SET_VIEWPORT_STATE, 0);
    builder.appendu32(0);
    builder.appendf32(DRAWTARGET_WIDTH / 2);    // scale_x
    builder.appendf32((DRAWTARGET_HEIGHT / 2)); // scale_y (flipped, due to VirGL being different from our coordinate space)
    builder.appendf32(0.5f);                    // scale_z
    builder.appendf32(DRAWTARGET_WIDTH / 2);    // translate_x
    builder.appendf32(DRAWTARGET_HEIGHT / 2);   // translate_y
    builder.appendf32(0.5f);                    // translate_z
}

void CommandBufferBuilder::append_set_framebuffer_state_no_attach()
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::SET_FRAMEBUFFER_STATE_NO_ATTACH, 0);
    builder.appendu32((DRAWTARGET_HEIGHT << 16) | DRAWTARGET_WIDTH); // (height << 16) | width
    builder.appendu32(0);                                            // (samples << 16) | layers
}

void CommandBufferBuilder::append_set_constant_buffer(Vector<float> const& constant_buffer)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::SET_CONSTANT_BUFFER, 0);
    builder.appendu32(to_underlying(Gallium::ShaderType::SHADER_VERTEX));
    builder.appendu32(0); // index (currently unused according to virglrenderer source code)
    for (auto v : constant_buffer) {
        builder.appendf32(v);
    }
}

void CommandBufferBuilder::append_create_shader(ObjectHandle handle, Gallium::ShaderType shader_type, StringView shader_data)
{
    size_t shader_len = shader_data.length() + 1; // Need to remember to copy null terminator as well if needed
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CREATE_OBJECT, to_underlying(Protocol::ObjectType::SHADER));
    builder.appendu32(handle.value()); // VIRGL_OBJ_CREATE_HANDLE
    builder.appendu32(to_underlying(shader_type));
    builder.appendu32(0); // VIRGL_OBJ_SHADER_OFFSET
    builder.appendu32(shader_len);
    builder.appendu32(0); // VIRGL_OBJ_SHADER_NUM_TOKENS
    builder.append_string_null_padded(shader_data);
}

void CommandBufferBuilder::append_bind_shader(ObjectHandle handle, Gallium::ShaderType shader_type)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::BIND_SHADER, 0);
    builder.appendu32(handle.value()); // VIRGL_OBJ_BIND_HANDLE
    builder.appendu32(to_underlying(shader_type));
}

void CommandBufferBuilder::append_create_rasterizer(ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CREATE_OBJECT, to_underlying(Protocol::ObjectType::RASTERIZER));
    builder.appendu32(handle.value()); // Handle
    builder.appendu32(0x00000002);     // S0 (bitfield of state bits)
    builder.appendf32(1.0);            // Point size
    builder.appendu32(0);              // Sprite coord enable
    builder.appendu32(0x00000000);     // S3 (bitfield of state bits)
    builder.appendf32(0.1);            // Line width
    builder.appendf32(0.0);            // Offset units
    builder.appendf32(0.0);            // offset scale
    builder.appendf32(0.0);            // Offset clamp
}

void CommandBufferBuilder::append_bind_rasterizer(ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::BIND_OBJECT, to_underlying(Protocol::ObjectType::RASTERIZER));
    builder.appendu32(handle.value()); // VIRGL_OBJ_BIND_HANDLE
}

void CommandBufferBuilder::append_create_dsa(ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CREATE_OBJECT, to_underlying(Protocol::ObjectType::DSA));
    builder.appendu32(handle.value()); // Handle
    builder.appendu32(0x00000007);     // S0 (bitset: (v >> 0) & 1 = depth.enabled, (v >> 1) & 1 = depth.writemask,  (v >> 2) & 7 = depth.func)
    builder.appendu32(0x00000000);     // S1 (bitset for 1st stencil buffer)
    builder.appendu32(0x00000000);     // S2 (bitset for 2nd stencil buffer)
    builder.appendf32(1.0);            // Alpha Ref
}

void CommandBufferBuilder::append_bind_dsa(ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::BIND_OBJECT, to_underlying(Protocol::ObjectType::DSA));
    builder.appendu32(handle.value()); // VIRGL_OBJ_BIND_HANDLE
}
