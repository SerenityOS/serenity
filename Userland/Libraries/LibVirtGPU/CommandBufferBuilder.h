/*
 * Copyright (c) 2022, Sahan Fernando <sahan.h.fernando@gmail.com>
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibGfx/Size.h>
#include <LibVirtGPU/Commands.h>
#include <LibVirtGPU/VirGLProtocol.h>
#include <sys/ioctl.h>

namespace VirtGPU {

class CommandBufferBuilder {
public:
    void append_set_tweaks(u32 id, u32 value);
    void append_transfer3d(Protocol::ResourceID resource, size_t width, size_t height = 1, size_t depth = 1, size_t direction = VIRGL_DATA_DIR_GUEST_TO_HOST);
    void append_end_transfers_3d();
    void append_draw_vbo(Protocol::PipePrimitiveTypes, u32 count);
    void append_clear(float r, float g, float b, float a);
    void append_clear(double depth);
    void append_set_vertex_buffers(u32 stride, u32 offset, Protocol::ResourceID resource);
    void append_create_blend(Protocol::ObjectHandle handle);
    void append_bind_blend(Protocol::ObjectHandle handle);
    void append_create_surface(Protocol::ResourceID drawtarget_resource, Protocol::ObjectHandle drawtarget_handle, Protocol::TextureFormat format);
    void append_set_framebuffer_state(Protocol::ObjectHandle drawtarget, Protocol::ObjectHandle depthbuffer = 0);
    void append_create_vertex_elements(Protocol::ObjectHandle handle, Vector<CreateVertexElementsCommand::ElementBinding> const&);
    void append_bind_vertex_elements(Protocol::ObjectHandle handle);
    void append_viewport(Gfx::IntSize);
    void append_set_framebuffer_state_no_attach(Gfx::IntSize);
    void append_set_constant_buffer(Vector<float> const& constant_buffer);
    void append_create_shader(Protocol::ObjectHandle handle, Gallium::ShaderType shader_type, StringView shader_data);
    void append_bind_shader(Protocol::ObjectHandle handle, Gallium::ShaderType shader_type);
    void append_create_rasterizer(Protocol::ObjectHandle handle);
    void append_bind_rasterizer(Protocol::ObjectHandle handle);
    void append_create_dsa(Protocol::ObjectHandle handle);
    void append_bind_dsa(Protocol::ObjectHandle handle);
    Vector<u32> const& build() { return m_buffer; }

private:
    Vector<u32> m_buffer;
};

}
