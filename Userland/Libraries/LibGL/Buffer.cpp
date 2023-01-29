/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2022, cflip <cflip@cflip.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GLContext.h>

namespace GL {

void GLContext::gl_bind_buffer(GLenum target, GLuint buffer)
{
    RETURN_WITH_ERROR_IF(target != GL_ARRAY_BUFFER && target != GL_ELEMENT_ARRAY_BUFFER, GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(!m_buffer_name_allocator.has_allocated_name(buffer), GL_INVALID_VALUE);

    auto& target_buffer = target == GL_ELEMENT_ARRAY_BUFFER ? m_element_array_buffer : m_array_buffer;
    target_buffer = nullptr;

    if (buffer != 0) {
        auto it = m_allocated_buffers.find(buffer);
        if (it != m_allocated_buffers.end()) {
            auto buffer_object = it->value;
            if (!buffer_object.is_null()) {
                target_buffer = buffer_object;
            }
        }

        if (!target_buffer) {
            target_buffer = adopt_ref(*new Buffer());
            m_allocated_buffers.set(buffer, target_buffer);
        }
    }
}

void GLContext::gl_buffer_data(GLenum target, GLsizeiptr size, void const* data, GLenum usage)
{
    RETURN_WITH_ERROR_IF(usage != GL_STREAM_DRAW
            && usage != GL_STREAM_READ
            && usage != GL_STREAM_COPY
            && usage != GL_STATIC_DRAW
            && usage != GL_STATIC_READ
            && usage != GL_STATIC_COPY
            && usage != GL_DYNAMIC_DRAW
            && usage != GL_DYNAMIC_READ
            && usage != GL_DYNAMIC_COPY,
        GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(target != GL_ARRAY_BUFFER && target != GL_ELEMENT_ARRAY_BUFFER, GL_INVALID_ENUM);

    auto& target_buffer = target == GL_ELEMENT_ARRAY_BUFFER ? m_element_array_buffer : m_array_buffer;
    RETURN_WITH_ERROR_IF(!target_buffer, GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(target_buffer->set_data(data, size).is_error(), GL_OUT_OF_MEMORY);
}

void GLContext::gl_buffer_sub_data(GLenum target, GLintptr offset, GLsizeiptr size, void const* data)
{
    RETURN_WITH_ERROR_IF(target != GL_ARRAY_BUFFER && target != GL_ELEMENT_ARRAY_BUFFER, GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(offset < 0, GL_INVALID_VALUE);
    // FIXME: Support buffer storage mutability flags.

    auto& target_buffer = target == GL_ELEMENT_ARRAY_BUFFER ? m_element_array_buffer : m_array_buffer;
    RETURN_WITH_ERROR_IF(!target_buffer, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(static_cast<size_t>(offset + size) > target_buffer->size(), GL_INVALID_VALUE);

    target_buffer->replace_data(data, offset, size);
}

void GLContext::gl_delete_buffers(GLsizei n, GLuint const* buffers)
{
    RETURN_WITH_ERROR_IF(n < 0, GL_INVALID_VALUE);

    for (auto i = 0; i < n; i++) {
        GLuint name = buffers[i];
        if (name == 0)
            continue;

        auto buffer_object = m_allocated_buffers.find(name);
        if (buffer_object == m_allocated_buffers.end() || buffer_object->value.is_null())
            continue;

        Buffer* buffer = buffer_object->value;

        if (m_array_buffer == buffer)
            m_array_buffer = nullptr;

        if (m_element_array_buffer == buffer)
            m_element_array_buffer = nullptr;

        m_buffer_name_allocator.free(name);
        m_allocated_buffers.remove(name);
    }
}

void GLContext::gl_gen_buffers(GLsizei n, GLuint* buffers)
{
    RETURN_WITH_ERROR_IF(n < 0, GL_INVALID_VALUE);

    m_buffer_name_allocator.allocate(n, buffers);

    // Initialize all buffer names with a nullptr
    for (auto i = 0; i < n; ++i) {
        GLuint name = buffers[i];
        m_allocated_buffers.set(name, nullptr);
    }
}

}
