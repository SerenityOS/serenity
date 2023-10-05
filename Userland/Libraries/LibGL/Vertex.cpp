/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/NumericLimits.h>
#include <LibGL/GLContext.h>

namespace GL {

// General helper function to read arbitrary vertex attribute data into a float array
static void read_from_vertex_attribute_pointer(VertexAttribPointer const& attrib, int index, float* elements)
{
    auto const* byte_ptr = reinterpret_cast<char const*>(attrib.pointer);

    auto read_values = [&]<typename T>() {
        auto const stride = (attrib.stride == 0) ? sizeof(T) * attrib.size : attrib.stride;
        for (int i = 0; i < attrib.size; ++i) {
            elements[i] = *(reinterpret_cast<T const*>(byte_ptr + stride * index) + i);
            if constexpr (IsIntegral<T>) {
                if (attrib.normalize)
                    elements[i] /= NumericLimits<T>::max();
            }
        }
    };

    switch (attrib.type) {
    case GL_BYTE:
        read_values.operator()<GLbyte>();
        break;
    case GL_UNSIGNED_BYTE:
        read_values.operator()<GLubyte>();
        break;
    case GL_SHORT:
        read_values.operator()<GLshort>();
        break;
    case GL_UNSIGNED_SHORT:
        read_values.operator()<GLushort>();
        break;
    case GL_INT:
        read_values.operator()<GLint>();
        break;
    case GL_UNSIGNED_INT:
        read_values.operator()<GLuint>();
        break;
    case GL_FLOAT:
        read_values.operator()<GLfloat>();
        break;
    case GL_DOUBLE:
        read_values.operator()<GLdouble>();
        break;
    }
}

void GLContext::gl_array_element(GLint i)
{
    // NOTE: This always dereferences data; display list support is deferred to the
    //       individual vertex attribute calls such as `gl_color`, `gl_normal` etc.
    RETURN_WITH_ERROR_IF(i < 0, GL_INVALID_VALUE);

    if (m_client_side_color_array_enabled) {
        float color[4] { 0.f, 0.f, 0.f, 1.f };
        read_from_vertex_attribute_pointer(m_client_color_pointer, i, color);
        gl_color(color[0], color[1], color[2], color[3]);
    }

    for (size_t t = 0; t < m_client_tex_coord_pointer.size(); ++t) {
        if (m_client_side_texture_coord_array_enabled[t]) {
            float tex_coords[4] { 0.f, 0.f, 0.f, 1.f };
            read_from_vertex_attribute_pointer(m_client_tex_coord_pointer[t], i, tex_coords);
            gl_multi_tex_coord(GL_TEXTURE0 + t, tex_coords[0], tex_coords[1], tex_coords[2], tex_coords[3]);
        }
    }

    if (m_client_side_normal_array_enabled) {
        float normal[3];
        read_from_vertex_attribute_pointer(m_client_normal_pointer, i, normal);
        gl_normal(normal[0], normal[1], normal[2]);
    }

    if (m_client_side_vertex_array_enabled) {
        float vertex[4] { 0.f, 0.f, 0.f, 1.f };
        read_from_vertex_attribute_pointer(m_client_vertex_pointer, i, vertex);
        gl_vertex(vertex[0], vertex[1], vertex[2], vertex[3]);
    }
}

void GLContext::gl_color(GLfloat r, GLfloat g, GLfloat b, GLfloat a)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_color, r, g, b, a);

    m_current_vertex_color = { r, g, b, a };
}

void GLContext::gl_color_pointer(GLint size, GLenum type, GLsizei stride, void const* pointer)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(!(size == 3 || size == 4), GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(type != GL_BYTE
            && type != GL_UNSIGNED_BYTE
            && type != GL_SHORT
            && type != GL_UNSIGNED_SHORT
            && type != GL_INT
            && type != GL_UNSIGNED_INT
            && type != GL_FLOAT
            && type != GL_DOUBLE,
        GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(stride < 0, GL_INVALID_VALUE);

    void const* data_pointer = pointer;
    if (m_array_buffer) {
        size_t data_offset = reinterpret_cast<size_t>(pointer);
        data_pointer = m_array_buffer->offset_data(data_offset);
    }
    m_client_color_pointer = { .size = size, .type = type, .normalize = true, .stride = stride, .pointer = data_pointer };
}

void GLContext::gl_draw_arrays(GLenum mode, GLint first, GLsizei count)
{
    // NOTE: This always dereferences data; display list support is deferred to the
    //       individual vertex attribute calls such as `gl_color`, `gl_normal` etc.
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: Some modes are still missing (GL_POINTS, GL_LINE_STRIP, GL_LINE_LOOP, GL_LINES)
    RETURN_WITH_ERROR_IF(!(mode == GL_TRIANGLE_STRIP
                             || mode == GL_TRIANGLE_FAN
                             || mode == GL_TRIANGLES
                             || mode == GL_QUADS
                             || mode == GL_QUAD_STRIP
                             || mode == GL_POLYGON),
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(count < 0, GL_INVALID_VALUE);

    auto last = first + count;
    gl_begin(mode);
    for (int i = first; i < last; i++) {
        if (m_client_side_color_array_enabled) {
            float color[4] { 0.f, 0.f, 0.f, 1.f };
            read_from_vertex_attribute_pointer(m_client_color_pointer, i, color);
            gl_color(color[0], color[1], color[2], color[3]);
        }

        for (size_t t = 0; t < m_client_tex_coord_pointer.size(); ++t) {
            if (m_client_side_texture_coord_array_enabled[t]) {
                float tex_coords[4] { 0.f, 0.f, 0.f, 1.f };
                read_from_vertex_attribute_pointer(m_client_tex_coord_pointer[t], i, tex_coords);
                gl_multi_tex_coord(GL_TEXTURE0 + t, tex_coords[0], tex_coords[1], tex_coords[2], tex_coords[3]);
            }
        }

        if (m_client_side_normal_array_enabled) {
            float normal[3];
            read_from_vertex_attribute_pointer(m_client_normal_pointer, i, normal);
            gl_normal(normal[0], normal[1], normal[2]);
        }

        if (m_client_side_vertex_array_enabled) {
            float vertex[4] { 0.f, 0.f, 0.f, 1.f };
            read_from_vertex_attribute_pointer(m_client_vertex_pointer, i, vertex);
            gl_vertex(vertex[0], vertex[1], vertex[2], vertex[3]);
        }
    }
    gl_end();
}

void GLContext::gl_draw_elements(GLenum mode, GLsizei count, GLenum type, void const* indices)
{
    // NOTE: This always dereferences data; display list support is deferred to the
    //       individual vertex attribute calls such as `gl_color`, `gl_normal` etc.
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: Some modes are still missing (GL_POINTS, GL_LINE_STRIP, GL_LINE_LOOP, GL_LINES)
    RETURN_WITH_ERROR_IF(!(mode == GL_TRIANGLE_STRIP
                             || mode == GL_TRIANGLE_FAN
                             || mode == GL_TRIANGLES
                             || mode == GL_QUADS
                             || mode == GL_QUAD_STRIP
                             || mode == GL_POLYGON),
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(!(type == GL_UNSIGNED_BYTE
                             || type == GL_UNSIGNED_SHORT
                             || type == GL_UNSIGNED_INT),
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(count < 0, GL_INVALID_VALUE);

    void const* index_data = indices;
    if (m_element_array_buffer) {
        size_t data_offset = reinterpret_cast<size_t>(indices);
        index_data = m_element_array_buffer->offset_data(data_offset);
    }

    gl_begin(mode);
    for (int index = 0; index < count; index++) {
        int i = 0;
        switch (type) {
        case GL_UNSIGNED_BYTE:
            i = reinterpret_cast<GLubyte const*>(index_data)[index];
            break;
        case GL_UNSIGNED_SHORT:
            i = reinterpret_cast<GLushort const*>(index_data)[index];
            break;
        case GL_UNSIGNED_INT:
            i = reinterpret_cast<GLuint const*>(index_data)[index];
            break;
        }

        if (m_client_side_color_array_enabled) {
            float color[4] { 0.f, 0.f, 0.f, 1.f };
            read_from_vertex_attribute_pointer(m_client_color_pointer, i, color);
            gl_color(color[0], color[1], color[2], color[3]);
        }

        for (size_t t = 0; t < m_client_tex_coord_pointer.size(); ++t) {
            if (m_client_side_texture_coord_array_enabled[t]) {
                float tex_coords[4] { 0.f, 0.f, 0.f, 1.f };
                read_from_vertex_attribute_pointer(m_client_tex_coord_pointer[t], i, tex_coords);
                gl_multi_tex_coord(GL_TEXTURE0 + t, tex_coords[0], tex_coords[1], tex_coords[2], tex_coords[3]);
            }
        }

        if (m_client_side_normal_array_enabled) {
            float normal[3];
            read_from_vertex_attribute_pointer(m_client_normal_pointer, i, normal);
            gl_normal(normal[0], normal[1], normal[2]);
        }

        if (m_client_side_vertex_array_enabled) {
            float vertex[4] { 0.f, 0.f, 0.f, 1.f };
            read_from_vertex_attribute_pointer(m_client_vertex_pointer, i, vertex);
            gl_vertex(vertex[0], vertex[1], vertex[2], vertex[3]);
        }
    }
    gl_end();
}

void GLContext::gl_normal(GLfloat nx, GLfloat ny, GLfloat nz)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_normal, nx, ny, nz);

    m_current_vertex_normal = { nx, ny, nz };
}

void GLContext::gl_normal_pointer(GLenum type, GLsizei stride, void const* pointer)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(type != GL_BYTE
            && type != GL_SHORT
            && type != GL_INT
            && type != GL_FLOAT
            && type != GL_DOUBLE,
        GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(stride < 0, GL_INVALID_VALUE);

    void const* data_pointer = pointer;
    if (m_array_buffer) {
        size_t data_offset = reinterpret_cast<size_t>(pointer);
        data_pointer = m_array_buffer->offset_data(data_offset);
    }
    m_client_normal_pointer = { .size = 3, .type = type, .normalize = true, .stride = stride, .pointer = data_pointer };
}

void GLContext::gl_tex_coord_pointer(GLint size, GLenum type, GLsizei stride, void const* pointer)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(!(size == 1 || size == 2 || size == 3 || size == 4), GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(!(type == GL_SHORT || type == GL_INT || type == GL_FLOAT || type == GL_DOUBLE), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(stride < 0, GL_INVALID_VALUE);

    auto& tex_coord_pointer = m_client_tex_coord_pointer[m_client_active_texture];

    void const* data_pointer = pointer;
    if (m_array_buffer) {
        size_t data_offset = reinterpret_cast<size_t>(pointer);
        data_pointer = m_array_buffer->offset_data(data_offset);
    }
    tex_coord_pointer = { .size = size, .type = type, .normalize = false, .stride = stride, .pointer = data_pointer };
}

void GLContext::gl_vertex(GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_vertex, x, y, z, w);

    GPU::Vertex vertex;

    vertex.position = { x, y, z, w };
    vertex.color = m_current_vertex_color;
    for (size_t i = 0; i < m_device_info.num_texture_units; ++i)
        vertex.tex_coords[i] = m_current_vertex_tex_coord[i];
    vertex.normal = m_current_vertex_normal;

    // Optimization: by pulling in the Vector size vs. capacity check, we can always perform an unchecked append
    if (m_vertex_list.size() == m_vertex_list.capacity())
        m_vertex_list.grow_capacity(m_vertex_list.size() + 1);
    m_vertex_list.unchecked_append(vertex);
}

void GLContext::gl_vertex_pointer(GLint size, GLenum type, GLsizei stride, void const* pointer)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(!(size == 2 || size == 3 || size == 4), GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(!(type == GL_SHORT || type == GL_INT || type == GL_FLOAT || type == GL_DOUBLE), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(stride < 0, GL_INVALID_VALUE);

    void const* data_pointer = pointer;
    if (m_array_buffer) {
        size_t data_offset = reinterpret_cast<size_t>(pointer);
        data_pointer = m_array_buffer->offset_data(data_offset);
    }
    m_client_vertex_pointer = { .size = size, .type = type, .normalize = false, .stride = stride, .pointer = data_pointer };
}

}
