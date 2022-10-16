/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Debug.h>
#include <LibGL/GLContext.h>

namespace GL {

void GLContext::gl_array_element(GLint i)
{
    // NOTE: This always dereferences data; display list support is deferred to the
    //       individual vertex attribute calls such as `gl_color`, `gl_normal` etc.
    RETURN_WITH_ERROR_IF(i < 0, GL_INVALID_VALUE);

    // This is effectively the same as `gl_draw_elements`, except we only output a single
    // vertex (this is done between a `gl_begin/end` call) that is to be rendered.
    if (!m_client_side_vertex_array_enabled)
        return;

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

    float vertex[4] { 0.f, 0.f, 0.f, 1.f };
    read_from_vertex_attribute_pointer(m_client_vertex_pointer, i, vertex);
    gl_vertex(vertex[0], vertex[1], vertex[2], vertex[3]);
}

void GLContext::gl_color(GLdouble r, GLdouble g, GLdouble b, GLdouble a)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_color, r, g, b, a);

    m_current_vertex_color = {
        static_cast<float>(r),
        static_cast<float>(g),
        static_cast<float>(b),
        static_cast<float>(a),
    };
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

    m_client_color_pointer = { .size = size, .type = type, .stride = stride, .pointer = pointer };
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

    // At least the vertex array needs to be enabled
    if (!m_client_side_vertex_array_enabled)
        return;

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

        float vertex[4] { 0.f, 0.f, 0.f, 1.f };
        read_from_vertex_attribute_pointer(m_client_vertex_pointer, i, vertex);
        gl_vertex(vertex[0], vertex[1], vertex[2], vertex[3]);
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

    // At least the vertex array needs to be enabled
    if (!m_client_side_vertex_array_enabled)
        return;

    gl_begin(mode);
    for (int index = 0; index < count; index++) {
        int i = 0;
        switch (type) {
        case GL_UNSIGNED_BYTE:
            i = reinterpret_cast<GLubyte const*>(indices)[index];
            break;
        case GL_UNSIGNED_SHORT:
            i = reinterpret_cast<GLushort const*>(indices)[index];
            break;
        case GL_UNSIGNED_INT:
            i = reinterpret_cast<GLuint const*>(indices)[index];
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

        float vertex[4] { 0.f, 0.f, 0.f, 1.f };
        read_from_vertex_attribute_pointer(m_client_vertex_pointer, i, vertex);
        gl_vertex(vertex[0], vertex[1], vertex[2], vertex[3]);
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

    m_client_normal_pointer = { .size = 3, .type = type, .stride = stride, .pointer = pointer };
}

void GLContext::gl_vertex(GLdouble x, GLdouble y, GLdouble z, GLdouble w)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_vertex, x, y, z, w);

    GPU::Vertex vertex;

    vertex.position = { static_cast<float>(x), static_cast<float>(y), static_cast<float>(z), static_cast<float>(w) };
    vertex.color = m_current_vertex_color;
    for (size_t i = 0; i < m_device_info.num_texture_units; ++i)
        vertex.tex_coords[i] = m_current_vertex_tex_coord[i];
    vertex.normal = m_current_vertex_normal;

    m_vertex_list.append(vertex);
}

void GLContext::gl_vertex_pointer(GLint size, GLenum type, GLsizei stride, void const* pointer)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(!(size == 2 || size == 3 || size == 4), GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(!(type == GL_SHORT || type == GL_INT || type == GL_FLOAT || type == GL_DOUBLE), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(stride < 0, GL_INVALID_VALUE);

    m_client_vertex_pointer = { .size = size, .type = type, .stride = stride, .pointer = pointer };
}

// General helper function to read arbitrary vertex attribute data into a float array
void GLContext::read_from_vertex_attribute_pointer(VertexAttribPointer const& attrib, int index, float* elements)
{
    auto byte_ptr = reinterpret_cast<char const*>(attrib.pointer);
    auto normalize = attrib.normalize;
    size_t stride = attrib.stride;

    switch (attrib.type) {
    case GL_BYTE: {
        if (stride == 0)
            stride = sizeof(GLbyte) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<GLbyte const*>(byte_ptr + stride * index) + i);
            if (normalize)
                elements[i] /= 0x80;
        }
        break;
    }
    case GL_UNSIGNED_BYTE: {
        if (stride == 0)
            stride = sizeof(GLubyte) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<GLubyte const*>(byte_ptr + stride * index) + i);
            if (normalize)
                elements[i] /= 0xff;
        }
        break;
    }
    case GL_SHORT: {
        if (stride == 0)
            stride = sizeof(GLshort) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<GLshort const*>(byte_ptr + stride * index) + i);
            if (normalize)
                elements[i] /= 0x8000;
        }
        break;
    }
    case GL_UNSIGNED_SHORT: {
        if (stride == 0)
            stride = sizeof(GLushort) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<GLushort const*>(byte_ptr + stride * index) + i);
            if (normalize)
                elements[i] /= 0xffff;
        }
        break;
    }
    case GL_INT: {
        if (stride == 0)
            stride = sizeof(GLint) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<GLint const*>(byte_ptr + stride * index) + i);
            if (normalize)
                elements[i] /= 0x80000000;
        }
        break;
    }
    case GL_UNSIGNED_INT: {
        if (stride == 0)
            stride = sizeof(GLuint) * attrib.size;

        for (int i = 0; i < attrib.size; i++) {
            elements[i] = *(reinterpret_cast<GLuint const*>(byte_ptr + stride * index) + i);
            if (normalize)
                elements[i] /= 0xffffffff;
        }
        break;
    }
    case GL_FLOAT: {
        if (stride == 0)
            stride = sizeof(GLfloat) * attrib.size;

        for (int i = 0; i < attrib.size; i++)
            elements[i] = *(reinterpret_cast<GLfloat const*>(byte_ptr + stride * index) + i);
        break;
    }
    case GL_DOUBLE: {
        if (stride == 0)
            stride = sizeof(GLdouble) * attrib.size;

        for (int i = 0; i < attrib.size; i++)
            elements[i] = static_cast<float>(*(reinterpret_cast<GLdouble const*>(byte_ptr + stride * index) + i));
        break;
    }
    }
}

}
