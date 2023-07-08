/*
 * Copyright (c) 2022, Sahan Fernando <sahan.h.fernando@gmail.com>
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/API/VirGL.h>
#include <sys/ioctl.h>

#include <LibVirtGPU/CommandBufferBuilder.h>
#include <LibVirtGPU/Commands.h>
#include <LibVirtGPU/VirGLProtocol.h>

namespace VirtGPU {

static u32 encode_command(u16 length, Protocol::ObjectType object_type, Protocol::VirGLCommand command)
{
    u8 command_value = to_underlying(command);
    u8 object_type_value = to_underlying(object_type);
    return (length << 16) | (object_type_value << 8) | command_value;
}

class CommandBuilder {
public:
    CommandBuilder(Vector<u32>& buffer, Protocol::VirGLCommand command, Protocol::ObjectType object_type)
        : m_buffer(buffer)
        , m_start_offset(buffer.size())
        , m_command(command)
        , m_object_type(object_type)
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
        auto* depth = reinterpret_cast<u64*>(&m_buffer[m_buffer.size() - 2]);
        *depth = bit_cast<u64>(value);
    }

    void append_string_null_padded(StringView string)
    {
        VERIFY(!m_finalized);
        // Remember to have at least one null terminator byte
        auto length = string.length() + 1;
        auto num_required_words = (length + sizeof(u32) - 1) / sizeof(u32);
        m_buffer.resize(m_buffer.size() + num_required_words);
        char* dest = reinterpret_cast<char*>(&m_buffer[m_buffer.size() - num_required_words]);
        memcpy(dest, string.characters_without_null_termination(), string.length());
        // Pad end with null bytes
        memset(&dest[string.length()], 0, 4 * num_required_words - string.length());
    }

    void finalize()
    {
        if (!m_finalized) {
            m_finalized = true;
            size_t num_elems = m_buffer.size() - m_start_offset - 1;
            VERIFY(num_elems <= NumericLimits<u16>::max());
            m_buffer[m_start_offset] = encode_command(static_cast<u16>(num_elems), m_object_type, m_command);
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
    Protocol::ObjectType m_object_type;
    bool m_finalized { false };
};

void CommandBufferBuilder::append_set_tweaks(u32 id, u32 value)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::SET_TWEAKS, Protocol::ObjectType::NONE);
    builder.appendu32(id);
    builder.appendu32(value);
}

void CommandBufferBuilder::append_transfer3d(Protocol::ResourceID resource, size_t width, size_t height, size_t depth, size_t direction)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::TRANSFER3D, Protocol::ObjectType::NONE);
    builder.appendu32(resource.value()); // res_handle
    builder.appendu32(0);                // level
    // FIXME: It is not clear what this magic 242 value does.
    // According to https://gitlab.freedesktop.org/virgl/virglrenderer/-/blob/master/src/vrend_decode.c#L1398 it is unused
    // But ccapitalK had to specifically set it to prevent rendering failures.
    builder.appendu32(242);       // usage
    builder.appendu32(0);         // stride
    builder.appendu32(0);         // layer_stride
    builder.appendu32(0);         // x
    builder.appendu32(0);         // y
    builder.appendu32(0);         // z
    builder.appendu32(width);     // width
    builder.appendu32(height);    // height
    builder.appendu32(depth);     // depth
    builder.appendu32(0);         // data_offset
    builder.appendu32(direction); // direction
}

void CommandBufferBuilder::append_end_transfers_3d()
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::END_TRANSFERS, Protocol::ObjectType::NONE);
}

void CommandBufferBuilder::append_draw_vbo(Protocol::PipePrimitiveTypes primitive_type, u32 count)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::DRAW_VBO, Protocol::ObjectType::NONE);
    builder.appendu32(0);                             // start
    builder.appendu32(count);                         // count
    builder.appendu32(to_underlying(primitive_type)); // mode
    builder.appendu32(0);                             // indexed
    builder.appendu32(1);                             // instance_count
    builder.appendu32(0);                             // index_bias
    builder.appendu32(0);                             // start_instance
    builder.appendu32(0);                             // primitive_restart
    builder.appendu32(0);                             // restart_index
    builder.appendu32(0);                             // min_index
    builder.appendu32(0xffffffff);                    // max_index
    builder.appendu32(0);                             // cso
}

void CommandBufferBuilder::append_clear(float r, float g, float b, float a)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CLEAR, Protocol::ObjectType::NONE);
    Protocol::ClearType clear_flags {};
    clear_flags.flags.color0 = 1;
    builder.appendu32(clear_flags.value);
    builder.appendf32(r);
    builder.appendf32(g);
    builder.appendf32(b);
    builder.appendf32(a);
    builder.appendf64(1.0);
    builder.appendu32(0);
}

void CommandBufferBuilder::append_clear(double depth)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CLEAR, Protocol::ObjectType::NONE);
    Protocol::ClearType clear_flags {};
    clear_flags.flags.depth = 1;
    builder.appendu32(clear_flags.value);
    builder.appendf32(0);
    builder.appendf32(0);
    builder.appendf32(0);
    builder.appendf32(0);
    builder.appendf64(depth);
    builder.appendu32(0);
}

void CommandBufferBuilder::append_set_vertex_buffers(u32 stride, u32 offset, Protocol::ResourceID resource)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::SET_VERTEX_BUFFERS, Protocol::ObjectType::NONE);
    builder.appendu32(stride);
    builder.appendu32(offset);
    builder.appendu32(resource.value());
}

void CommandBufferBuilder::append_create_blend(Protocol::ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CREATE_OBJECT, Protocol::ObjectType::BLEND);

    CreateBlendCommand::S0Flags s0 {};
    CreateBlendCommand::S1Flags s1 {};
    CreateBlendCommand::S2Flags s2 {};

    s0.dither = 1;
    s2.colormask = 0xf;

    builder.appendu32(handle.value());
    builder.appendu32(s0.u32_value);
    builder.appendu32(s1.u32_value);
    builder.appendu32(s2.u32_value);
    for (size_t i = 1; i < 8; ++i) {
        builder.appendu32(0); // Explicitly disable all flags for other color buffers
    }
}

void CommandBufferBuilder::append_bind_blend(Protocol::ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::BIND_OBJECT, Protocol::ObjectType::BLEND);
    builder.appendu32(handle.value()); // VIRGL_OBJ_BIND_HANDLE
}

void CommandBufferBuilder::append_create_vertex_elements(Protocol::ObjectHandle handle, Vector<CreateVertexElementsCommand::ElementBinding> const& bindings)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CREATE_OBJECT, Protocol::ObjectType::VERTEX_ELEMENTS);
    builder.appendu32(handle.value());
    for (auto& binding : bindings) {
        builder.appendu32(binding.offset);
        builder.appendu32(binding.divisor);
        builder.appendu32(binding.vertex_buffer_index);
        builder.appendu32(to_underlying(binding.format));
    }
}

void CommandBufferBuilder::append_bind_vertex_elements(Protocol::ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::BIND_OBJECT, Protocol::ObjectType::VERTEX_ELEMENTS);
    builder.appendu32(handle.value()); // VIRGL_OBJ_BIND_HANDLE
}

void CommandBufferBuilder::append_create_surface(Protocol::ResourceID drawtarget_resource, Protocol::ObjectHandle drawtarget_handle, Protocol::TextureFormat format)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CREATE_OBJECT, Protocol::ObjectType::SURFACE);
    builder.appendu32(drawtarget_handle.value());
    builder.appendu32(drawtarget_resource.value());
    builder.appendu32(to_underlying(format));
    builder.appendu32(0); // First element / Texture Level
    builder.appendu32(0); // Last element / Texture Element
}

void CommandBufferBuilder::append_set_framebuffer_state(Protocol::ObjectHandle drawtarget, Protocol::ObjectHandle depthbuffer)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::SET_FRAMEBUFFER_STATE, Protocol::ObjectType::NONE);
    builder.appendu32(1);                   // nr_cbufs
    builder.appendu32(depthbuffer.value()); // zsurf_handle
    builder.appendu32(drawtarget.value());  // surf_handle
}

void CommandBufferBuilder::append_viewport(Gfx::IntSize size)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::SET_VIEWPORT_STATE, Protocol::ObjectType::NONE);
    builder.appendu32(0);
    builder.appendf32(size.width() / 2);  // scale_x
    builder.appendf32(size.height() / 2); // scale_y (flipped, due to VirGL being different from our coordinate space)
    builder.appendf32(0.5f);              // scale_z
    builder.appendf32(size.width() / 2);  // translate_x
    builder.appendf32(size.height() / 2); // translate_y
    builder.appendf32(0.5f);              // translate_z
}

void CommandBufferBuilder::append_set_framebuffer_state_no_attach(Gfx::IntSize size)
{
    VERIFY(size.width() <= NumericLimits<u16>::max());
    VERIFY(size.height() <= NumericLimits<u16>::max());

    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::SET_FRAMEBUFFER_STATE_NO_ATTACH, Protocol::ObjectType::NONE);

    u16 samples = 0;
    u16 layers = 0;

    builder.appendu32((size.height() << 16) | size.width());
    builder.appendu32((samples << 16) | layers);
}

void CommandBufferBuilder::append_set_constant_buffer(Vector<float> const& constant_buffer)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::SET_CONSTANT_BUFFER, Protocol::ObjectType::NONE);
    builder.appendu32(to_underlying(Gallium::ShaderType::SHADER_VERTEX));
    builder.appendu32(0); // index (currently unused according to virglrenderer source code)
    for (auto v : constant_buffer) {
        builder.appendf32(v);
    }
}

void CommandBufferBuilder::append_create_shader(Protocol::ObjectHandle handle, Gallium::ShaderType shader_type, StringView shader_data)
{
    size_t shader_len = shader_data.length() + 1; // Need to remember to copy null terminator as well if needed
    VERIFY(shader_len <= NumericLimits<u32>::max());

    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CREATE_OBJECT, Protocol::ObjectType::SHADER);
    builder.appendu32(handle.value()); // VIRGL_OBJ_CREATE_HANDLE
    builder.appendu32(to_underlying(shader_type));
    builder.appendu32(0); // VIRGL_OBJ_SHADER_OFFSET
    builder.appendu32(shader_len);
    builder.appendu32(0); // VIRGL_OBJ_SHADER_NUM_TOKENS
    builder.append_string_null_padded(shader_data);
}

void CommandBufferBuilder::append_bind_shader(Protocol::ObjectHandle handle, Gallium::ShaderType shader_type)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::BIND_SHADER, Protocol::ObjectType::NONE);
    builder.appendu32(handle.value()); // VIRGL_OBJ_BIND_HANDLE
    builder.appendu32(to_underlying(shader_type));
}

void CommandBufferBuilder::append_create_rasterizer(Protocol::ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CREATE_OBJECT, Protocol::ObjectType::RASTERIZER);

    CreateRasterizerCommand::S0Flags s0 {};
    CreateRasterizerCommand::S3Flags s3 {};

    s0.depth_clip = 1;

    builder.appendu32(handle.value()); // Handle
    builder.appendu32(s0.u32_value);   // S0 (bitfield of state bits)
    builder.appendf32(1.0);            // Point size
    builder.appendu32(0);              // Sprite coord enable
    builder.appendu32(s3.u32_value);   // S3 (bitfield of state bits)
    builder.appendf32(0.1);            // Line width
    builder.appendf32(0.0);            // Offset units
    builder.appendf32(0.0);            // offset scale
    builder.appendf32(0.0);            // Offset clamp
}

void CommandBufferBuilder::append_bind_rasterizer(Protocol::ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::BIND_OBJECT, Protocol::ObjectType::RASTERIZER);
    builder.appendu32(handle.value()); // VIRGL_OBJ_BIND_HANDLE
}

void CommandBufferBuilder::append_create_dsa(Protocol::ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::CREATE_OBJECT, Protocol::ObjectType::DSA);

    CreateDSACommand::S0Flags s0 {};
    CreateDSACommand::S1Flags s1[2] {};

    s0.depth_enabled = 1;
    s0.depth_writemask = 1;
    s0.depth_func = 1;

    builder.appendu32(handle.value());  // Handle
    builder.appendu32(s0.u32_value);    // S0 (bitset for depth buffer)
    builder.appendu32(s1[0].u32_value); // S1 (bitset for 1st stencil buffer)
    builder.appendu32(s1[1].u32_value); // S2 (bitset for 2nd stencil buffer)
    builder.appendf32(1.0);             // Alpha Ref
}

void CommandBufferBuilder::append_bind_dsa(Protocol::ObjectHandle handle)
{
    CommandBuilder builder(m_buffer, Protocol::VirGLCommand::BIND_OBJECT, Protocol::ObjectType::DSA);
    builder.appendu32(handle.value()); // VIRGL_OBJ_BIND_HANDLE
}

}
