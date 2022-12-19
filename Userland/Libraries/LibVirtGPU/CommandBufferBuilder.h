/*
 * Copyright (c) 2022, Sahan Fernando <sahan.h.fernando@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Vector.h>
#include <sys/ioctl_numbers.h>

#include "VirGLProtocol.h"

class CommandBufferBuilder {
public:
    void append_set_tweaks(u32 id, u32 value);
    void append_transfer3d(ResourceID resource, size_t width, size_t height = 1, size_t depth = 1, size_t direction = VIRGL_DATA_DIR_GUEST_TO_HOST);
    void append_end_transfers_3d();
    void append_draw_vbo(u32 count);
    void append_gl_clear(float r, float g, float b);
    void append_set_vertex_buffers(u32 stride, u32 offset, ResourceID resource);
    void append_create_blend(ObjectHandle handle);
    void append_bind_blend(ObjectHandle handle);
    void append_create_surface(ResourceID drawtarget_resource, ObjectHandle drawtarget_handle, Protocol::TextureFormat format);
    void append_set_framebuffer_state(ObjectHandle drawtarget, ObjectHandle depthbuffer = 0);
    void append_create_vertex_elements(ObjectHandle handle);
    void append_bind_vertex_elements(ObjectHandle handle);
    void append_gl_viewport();
    void append_set_framebuffer_state_no_attach();
    void append_set_constant_buffer(Vector<float> const& constant_buffer);
    void append_create_shader(ObjectHandle handle, Gallium::ShaderType shader_type, StringView shader_data);
    void append_bind_shader(ObjectHandle handle, Gallium::ShaderType shader_type);
    void append_create_rasterizer(ObjectHandle handle);
    void append_bind_rasterizer(ObjectHandle handle);
    void append_create_dsa(ObjectHandle handle);
    void append_bind_dsa(ObjectHandle handle);
    Vector<u32> const& build() { return m_buffer; }

private:
    Vector<u32> m_buffer;
};
