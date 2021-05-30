/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SoftwareGLContext.h"
#include "GLStruct.h"
#include "SoftwareRasterizer.h"
#include <AK/Assertions.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/QuickSort.h>
#include <AK/TemporaryChange.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Vector4.h>
#include <math.h>

using AK::dbgln;

namespace GL {

// FIXME: We should set this up when we create the context!
static constexpr size_t MATRIX_STACK_LIMIT = 1024;

#define APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(name, ...)       \
    if (should_append_to_listing()) {                             \
        append_to_listing<&SoftwareGLContext::name>(__VA_ARGS__); \
        if (!should_execute_after_appending_to_listing())         \
            return;                                               \
    }

#define RETURN_WITH_ERROR_IF(condition, error) \
    if (condition) {                           \
        if (m_error == GL_NO_ERROR)            \
            m_error = error;                   \
        return;                                \
    }

#define RETURN_VALUE_WITH_ERROR_IF(condition, error, return_value) \
    if (condition) {                                               \
        if (m_error == GL_NO_ERROR)                                \
            m_error = error;                                       \
        return return_value;                                       \
    }

SoftwareGLContext::SoftwareGLContext(Gfx::Bitmap& frontbuffer)
    : m_frontbuffer(frontbuffer)
    , m_rasterizer(frontbuffer.size())
{
}

void SoftwareGLContext::gl_begin(GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_begin, mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(mode < GL_TRIANGLES || mode > GL_POLYGON, GL_INVALID_ENUM);

    m_current_draw_mode = mode;
    m_in_draw_state = true; // Certain commands will now generate an error
}

void SoftwareGLContext::gl_clear(GLbitfield mask)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clear, mask);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(mask & ~(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT), GL_INVALID_ENUM);

    if (mask & GL_COLOR_BUFFER_BIT)
        m_rasterizer.clear_color(m_clear_color);

    if (mask & GL_DEPTH_BUFFER_BIT)
        m_rasterizer.clear_depth(static_cast<float>(m_clear_depth));
}

void SoftwareGLContext::gl_clear_color(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clear_color, red, green, blue, alpha);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_clear_color = { red, green, blue, alpha };
}

void SoftwareGLContext::gl_clear_depth(GLdouble depth)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_clear_depth, depth);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_clear_depth = depth;
}

void SoftwareGLContext::gl_color(GLdouble r, GLdouble g, GLdouble b, GLdouble a)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_color, r, g, b, a);

    m_current_vertex_color = { (float)r, (float)g, (float)b, (float)a };
}

void SoftwareGLContext::gl_end()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_end);

    // At this point, the user has effectively specified that they are done with defining the geometry
    // of what they want to draw. We now need to do a few things (https://www.khronos.org/opengl/wiki/Rendering_Pipeline_Overview):
    //
    // 1.   Transform all of the vertices in the current vertex list into eye space by mulitplying the model-view matrix
    // 2.   Transform all of the vertices from eye space into clip space by multiplying by the projection matrix
    // 3.   If culling is enabled, we cull the desired faces (https://learnopengl.com/Advanced-OpenGL/Face-culling)
    // 4.   Each element of the vertex is then divided by w to bring the positions into NDC (Normalized Device Coordinates)
    // 5.   The vertices are sorted (for the rasteriser, how are we doing this? 3Dfx did this top to bottom in terms of vertex y coordinates)
    // 6.   The vertices are then sent off to the rasteriser and drawn to the screen

    float scr_width = m_frontbuffer->width();
    float scr_height = m_frontbuffer->height();

    // Make sure we had a `glBegin` before this call...
    RETURN_WITH_ERROR_IF(!m_in_draw_state, GL_INVALID_OPERATION);

    // Let's construct some triangles
    if (m_current_draw_mode == GL_TRIANGLES) {
        GLTriangle triangle;
        for (size_t i = 0; i < vertex_list.size(); i += 3) {
            triangle.vertices[0] = vertex_list.at(i);
            triangle.vertices[1] = vertex_list.at(i + 1);
            triangle.vertices[2] = vertex_list.at(i + 2);

            triangle_list.append(triangle);
        }
    } else if (m_current_draw_mode == GL_QUADS) {
        // We need to construct two triangles to form the quad
        GLTriangle triangle;
        VERIFY(vertex_list.size() % 4 == 0);
        for (size_t i = 0; i < vertex_list.size(); i += 4) {
            // Triangle 1
            triangle.vertices[0] = vertex_list.at(i);
            triangle.vertices[1] = vertex_list.at(i + 1);
            triangle.vertices[2] = vertex_list.at(i + 2);
            triangle_list.append(triangle);

            // Triangle 2
            triangle.vertices[0] = vertex_list.at(i + 2);
            triangle.vertices[1] = vertex_list.at(i + 3);
            triangle.vertices[2] = vertex_list.at(i);
            triangle_list.append(triangle);
        }
    } else if (m_current_draw_mode == GL_TRIANGLE_FAN) {
        GLTriangle triangle;
        triangle.vertices[0] = vertex_list.at(0); // Root vertex is always the vertex defined first

        for (size_t i = 1; i < vertex_list.size() - 1; i++) // This is technically `n-2` triangles. We start at index 1
        {
            triangle.vertices[1] = vertex_list.at(i);
            triangle.vertices[2] = vertex_list.at(i + 1);
            triangle_list.append(triangle);
        }
    } else if (m_current_draw_mode == GL_TRIANGLE_STRIP) {
        GLTriangle triangle;
        for (size_t i = 0; i < vertex_list.size() - 2; i++) {
            triangle.vertices[0] = vertex_list.at(i);
            triangle.vertices[1] = vertex_list.at(i + 1);
            triangle.vertices[2] = vertex_list.at(i + 2);
            triangle_list.append(triangle);
        }
    } else {
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    // Now let's transform each triangle and send that to the GPU
    for (size_t i = 0; i < triangle_list.size(); i++) {
        GLTriangle& triangle = triangle_list.at(i);
        GLVertex& vertexa = triangle.vertices[0];
        GLVertex& vertexb = triangle.vertices[1];
        GLVertex& vertexc = triangle.vertices[2];

        FloatVector4 veca({ vertexa.x, vertexa.y, vertexa.z, 1.0f });
        FloatVector4 vecb({ vertexb.x, vertexb.y, vertexb.z, 1.0f });
        FloatVector4 vecc({ vertexc.x, vertexc.y, vertexc.z, 1.0f });

        // First multiply the vertex by the MODELVIEW matrix and then the PROJECTION matrix
        veca = m_model_view_matrix * veca;
        veca = m_projection_matrix * veca;

        vecb = m_model_view_matrix * vecb;
        vecb = m_projection_matrix * vecb;

        vecc = m_model_view_matrix * vecc;
        vecc = m_projection_matrix * vecc;

        // At this point, we're in clip space
        // Here's where we do the clipping. This is a really crude implementation of the
        // https://learnopengl.com/Getting-started/Coordinate-Systems
        // "Note that if only a part of a primitive e.g. a triangle is outside the clipping volume OpenGL
        // will reconstruct the triangle as one or more triangles to fit inside the clipping range. "
        //
        // ALL VERTICES ARE DEFINED IN A CLOCKWISE ORDER

        // Okay, let's do some face culling first

        Vector<FloatVector4> vecs;
        Vector<GLVertex> verts;

        vecs.append(veca);
        vecs.append(vecb);
        vecs.append(vecc);
        m_clipper.clip_triangle_against_frustum(vecs);

        // TODO: Copy color and UV information too!
        for (size_t vec_idx = 0; vec_idx < vecs.size(); vec_idx++) {
            FloatVector4& vec = vecs.at(vec_idx);
            GLVertex vertex;

            // Perform the perspective divide
            if (vec.w() != 0.0f) {
                vec.set_x(vec.x() / vec.w());
                vec.set_y(vec.y() / vec.w());
                vec.set_z(vec.z() / vec.w());
                vec.set_w(1 / vec.w());
            }

            vertex.x = vec.x();
            vertex.y = vec.y();
            vertex.z = vec.z();
            vertex.w = vec.w();

            // FIXME: This is to suppress any -Wunused errors
            vertex.u = 0.0f;
            vertex.v = 0.0f;

            if (vec_idx == 0) {
                vertex.r = vertexa.r;
                vertex.g = vertexa.g;
                vertex.b = vertexa.b;
                vertex.a = vertexa.a;
                vertex.u = vertexa.u;
                vertex.v = vertexa.v;
            } else if (vec_idx == 1) {
                vertex.r = vertexb.r;
                vertex.g = vertexb.g;
                vertex.b = vertexb.b;
                vertex.a = vertexb.a;
                vertex.u = vertexb.u;
                vertex.v = vertexb.v;
            } else {
                vertex.r = vertexc.r;
                vertex.g = vertexc.g;
                vertex.b = vertexc.b;
                vertex.a = vertexc.a;
                vertex.u = vertexc.u;
                vertex.v = vertexc.v;
            }

            vertex.x = (vec.x() + 1.0f) * (scr_width / 2.0f) + 0.0f; // TODO: 0.0f should be something!?
            vertex.y = scr_height - ((vec.y() + 1.0f) * (scr_height / 2.0f) + 0.0f);
            vertex.z = vec.z();
            verts.append(vertex);
        }

        if (verts.size() == 0) {
            continue;
        } else if (verts.size() == 3) {
            GLTriangle tri;

            tri.vertices[0] = verts.at(0);
            tri.vertices[1] = verts.at(1);
            tri.vertices[2] = verts.at(2);
            processed_triangles.append(tri);
        } else if (verts.size() == 4) {
            GLTriangle tri1;
            GLTriangle tri2;

            tri1.vertices[0] = verts.at(0);
            tri1.vertices[1] = verts.at(1);
            tri1.vertices[2] = verts.at(2);
            processed_triangles.append(tri1);

            tri2.vertices[0] = verts.at(0);
            tri2.vertices[1] = verts.at(2);
            tri2.vertices[2] = verts.at(3);
            processed_triangles.append(tri2);
        }
    }

    for (size_t i = 0; i < processed_triangles.size(); i++) {
        GLTriangle& triangle = processed_triangles.at(i);

        // Let's calculate the (signed) area of the triangle
        // https://cp-algorithms.com/geometry/oriented-triangle-area.html
        float dxAB = triangle.vertices[0].x - triangle.vertices[1].x; // A.x - B.x
        float dxBC = triangle.vertices[1].x - triangle.vertices[2].x; // B.X - C.x
        float dyAB = triangle.vertices[0].y - triangle.vertices[1].y;
        float dyBC = triangle.vertices[1].y - triangle.vertices[2].y;
        float area = (dxAB * dyBC) - (dxBC * dyAB);

        if (area == 0.0f)
            continue;

        if (m_cull_faces) {
            bool is_front = (m_front_face == GL_CCW ? area > 0 : area < 0);

            if (is_front && (m_culled_sides == GL_FRONT || m_culled_sides == GL_FRONT_AND_BACK))
                continue;

            if (!is_front && (m_culled_sides == GL_BACK || m_culled_sides == GL_FRONT_AND_BACK))
                continue;
        }

        m_rasterizer.submit_triangle(triangle, m_texture_units);
    }

    triangle_list.clear();
    processed_triangles.clear();
    vertex_list.clear();

    m_in_draw_state = false;
}

void SoftwareGLContext::gl_frustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_frustum, left, right, bottom, top, near_val, far_val);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // Let's do some math!
    // FIXME: Are we losing too much precision by doing this?
    float a = static_cast<float>((right + left) / (right - left));
    float b = static_cast<float>((top + bottom) / (top - bottom));
    float c = static_cast<float>(-((far_val + near_val) / (far_val - near_val)));
    float d = static_cast<float>(-((2 * (far_val * near_val)) / (far_val - near_val)));

    FloatMatrix4x4 frustum {
        ((2 * (float)near_val) / ((float)right - (float)left)), 0, a, 0,
        0, ((2 * (float)near_val) / ((float)top - (float)bottom)), b, 0,
        0, 0, c, d,
        0, 0, -1, 0
    };

    if (m_current_matrix_mode == GL_PROJECTION) {
        m_projection_matrix = m_projection_matrix * frustum;
    } else if (m_current_matrix_mode == GL_MODELVIEW) {
        dbgln_if(GL_DEBUG, "glFrustum(): frustum created with curr_matrix_mode == GL_MODELVIEW!!!");
        m_projection_matrix = m_model_view_matrix * frustum;
    }
}

void SoftwareGLContext::gl_ortho(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_ortho, left, right, bottom, top, near_val, far_val);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(left == right || bottom == top || near_val == far_val, GL_INVALID_VALUE);

    auto rl = right - left;
    auto tb = top - bottom;
    auto fn = far_val - near_val;
    auto tx = -(right + left) / rl;
    auto ty = -(top + bottom) / tb;
    auto tz = -(far_val + near_val) / fn;

    FloatMatrix4x4 projection {
        static_cast<float>(2 / rl), 0, 0, static_cast<float>(tx),
        0, static_cast<float>(2 / tb), 0, static_cast<float>(ty),
        0, 0, static_cast<float>(-2 / fn), static_cast<float>(tz),
        0, 0, 0, 1
    };

    if (m_current_matrix_mode == GL_PROJECTION) {
        m_projection_matrix = m_projection_matrix * projection;
    } else if (m_current_matrix_mode == GL_MODELVIEW) {
        m_projection_matrix = m_model_view_matrix * projection;
    }
}

GLenum SoftwareGLContext::gl_get_error()
{
    if (m_in_draw_state)
        return GL_INVALID_OPERATION;

    auto last_error = m_error;
    m_error = GL_NO_ERROR;
    return last_error;
}

GLubyte* SoftwareGLContext::gl_get_string(GLenum name)
{
    RETURN_VALUE_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION, nullptr);

    switch (name) {
    case GL_VENDOR:
        return reinterpret_cast<GLubyte*>(const_cast<char*>("The SerenityOS Developers"));
    case GL_RENDERER:
        return reinterpret_cast<GLubyte*>(const_cast<char*>("SerenityOS OpenGL"));
    case GL_VERSION:
        return reinterpret_cast<GLubyte*>(const_cast<char*>("OpenGL 1.2 SerenityOS"));
    default:
        dbgln_if(GL_DEBUG, "glGetString(): Unknown enum name!");
        break;
    }

    RETURN_VALUE_WITH_ERROR_IF(true, GL_INVALID_ENUM, nullptr);
}

void SoftwareGLContext::gl_load_identity()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_load_identity);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = FloatMatrix4x4::identity();
    else if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = FloatMatrix4x4::identity();
    else
        VERIFY_NOT_REACHED();
}

void SoftwareGLContext::gl_load_matrix(const FloatMatrix4x4& matrix)
{
    if (should_append_to_listing()) {
        auto ptr = store_in_listing(matrix);
        append_to_listing<&SoftwareGLContext::gl_load_matrix>(*ptr);
        if (!should_execute_after_appending_to_listing())
            return;
    }

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = matrix;
    else if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = matrix;
    else
        VERIFY_NOT_REACHED();
}

void SoftwareGLContext::gl_matrix_mode(GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_matrix_mode, mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(mode < GL_MODELVIEW || mode > GL_PROJECTION, GL_INVALID_ENUM);

    m_current_matrix_mode = mode;
}

void SoftwareGLContext::gl_push_matrix()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_push_matrix);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    dbgln_if(GL_DEBUG, "glPushMatrix(): Pushing matrix to the matrix stack (matrix_mode {})", m_current_matrix_mode);

    switch (m_current_matrix_mode) {
    case GL_PROJECTION:
        RETURN_WITH_ERROR_IF(m_projection_matrix_stack.size() >= MATRIX_STACK_LIMIT, GL_STACK_OVERFLOW);
        m_projection_matrix_stack.append(m_projection_matrix);
        break;
    case GL_MODELVIEW:
        RETURN_WITH_ERROR_IF(m_model_view_matrix_stack.size() >= MATRIX_STACK_LIMIT, GL_STACK_OVERFLOW);
        m_model_view_matrix_stack.append(m_model_view_matrix);
        break;
    default:
        dbgln_if(GL_DEBUG, "glPushMatrix(): Attempt to push matrix with invalid matrix mode {})", m_current_matrix_mode);
        return;
    }
}

void SoftwareGLContext::gl_pop_matrix()
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_pop_matrix);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    dbgln_if(GL_DEBUG, "glPopMatrix(): Popping matrix from matrix stack (matrix_mode = {})", m_current_matrix_mode);

    // FIXME: Make sure stack::top() doesn't cause any  nasty issues if it's empty (that could result in a lockup/hang)
    switch (m_current_matrix_mode) {
    case GL_PROJECTION:
        RETURN_WITH_ERROR_IF(m_projection_matrix_stack.size() == 0, GL_STACK_UNDERFLOW);
        m_projection_matrix = m_projection_matrix_stack.take_last();
        break;
    case GL_MODELVIEW:
        RETURN_WITH_ERROR_IF(m_model_view_matrix_stack.size() == 0, GL_STACK_UNDERFLOW);
        m_model_view_matrix = m_model_view_matrix_stack.take_last();
        break;
    default:
        dbgln_if(GL_DEBUG, "glPopMatrix(): Attempt to pop matrix with invalid matrix mode, {}", m_current_matrix_mode);
        return;
    }
}

void SoftwareGLContext::gl_rotate(GLdouble angle, GLdouble x, GLdouble y, GLdouble z)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_rotate, angle, x, y, z);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    FloatVector3 axis = { (float)x, (float)y, (float)z };
    axis.normalize();
    auto rotation_mat = Gfx::rotation_matrix(axis, static_cast<float>(angle));

    if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = m_model_view_matrix * rotation_mat;
    else if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = m_projection_matrix * rotation_mat;
}

void SoftwareGLContext::gl_scale(GLdouble x, GLdouble y, GLdouble z)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_scale, x, y, z);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    if (m_current_matrix_mode == GL_MODELVIEW) {
        m_model_view_matrix = m_model_view_matrix * Gfx::scale_matrix(FloatVector3 { static_cast<float>(x), static_cast<float>(y), static_cast<float>(z) });
    } else if (m_current_matrix_mode == GL_PROJECTION) {
        m_projection_matrix = m_projection_matrix * Gfx::scale_matrix(FloatVector3 { static_cast<float>(x), static_cast<float>(y), static_cast<float>(z) });
    }
}

void SoftwareGLContext::gl_translate(GLdouble x, GLdouble y, GLdouble z)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_translate, x, y, z);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    if (m_current_matrix_mode == GL_MODELVIEW) {
        m_model_view_matrix = m_model_view_matrix * Gfx::translation_matrix(FloatVector3 { (float)x, (float)y, (float)z });
    } else if (m_current_matrix_mode == GL_PROJECTION) {
        m_projection_matrix = m_projection_matrix * Gfx::translation_matrix(FloatVector3 { (float)x, (float)y, (float)z });
    }
}

void SoftwareGLContext::gl_vertex(GLdouble x, GLdouble y, GLdouble z, GLdouble w)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_vertex, x, y, z, w);

    GLVertex vertex;

    vertex.x = x;
    vertex.y = y;
    vertex.z = z;
    vertex.w = w;
    vertex.r = m_current_vertex_color.x();
    vertex.g = m_current_vertex_color.y();
    vertex.b = m_current_vertex_color.z();
    vertex.a = m_current_vertex_color.w();

    // FIXME: This is to suppress any -Wunused errors
    vertex.w = 0.0f;
    vertex.u = 0.0f;
    vertex.v = 0.0f;

    vertex_list.append(vertex);
}

// FIXME: We need to add `r` and `q` to our GLVertex?!
void SoftwareGLContext::gl_tex_coord(GLfloat s, GLfloat t, GLfloat, GLfloat)
{
    auto& vertex = vertex_list.last(); // Get the last created vertex

    vertex.u = s;
    vertex.v = t;
}

void SoftwareGLContext::gl_viewport(GLint x, GLint y, GLsizei width, GLsizei height)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_viewport, x, y, width, height);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    (void)(x);
    (void)(y);
    (void)(width);
    (void)(height);
}

void SoftwareGLContext::gl_enable(GLenum capability)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_enable, capability);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto rasterizer_options = m_rasterizer.options();
    bool update_rasterizer_options = false;

    switch (capability) {
    case GL_CULL_FACE:
        m_cull_faces = true;
        break;
    case GL_DEPTH_TEST:
        m_depth_test_enabled = true;
        rasterizer_options.enable_depth_test = true;
        update_rasterizer_options = true;
        break;
    case GL_BLEND:
        m_blend_enabled = true;
        rasterizer_options.enable_blending = true;
        update_rasterizer_options = true;
        break;
    case GL_ALPHA_TEST:
        m_alpha_test_enabled = true;
        rasterizer_options.enable_alpha_test = true;
        update_rasterizer_options = true;
        break;
    default:
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    if (update_rasterizer_options)
        m_rasterizer.set_options(rasterizer_options);
}

void SoftwareGLContext::gl_disable(GLenum capability)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_disable, capability);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    auto rasterizer_options = m_rasterizer.options();
    bool update_rasterizer_options = false;

    switch (capability) {
    case GL_CULL_FACE:
        m_cull_faces = false;
        break;
    case GL_DEPTH_TEST:
        m_depth_test_enabled = false;
        rasterizer_options.enable_depth_test = false;
        update_rasterizer_options = true;
        break;
    case GL_BLEND:
        m_blend_enabled = false;
        rasterizer_options.enable_blending = false;
        update_rasterizer_options = true;
        break;
    case GL_ALPHA_TEST:
        m_alpha_test_enabled = false;
        rasterizer_options.enable_alpha_test = false;
        update_rasterizer_options = true;
        break;
    default:
        RETURN_WITH_ERROR_IF(true, GL_INVALID_ENUM);
    }

    if (update_rasterizer_options)
        m_rasterizer.set_options(rasterizer_options);
}

void SoftwareGLContext::gl_gen_textures(GLsizei n, GLuint* textures)
{
    RETURN_WITH_ERROR_IF(n < 0, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_name_allocator.allocate(n, textures);

    // Initialize all texture names with a nullptr
    for (auto i = 0; i < n; i++) {
        GLuint name = textures[i];

        m_allocated_textures.set(name, nullptr);
    }
}

void SoftwareGLContext::gl_delete_textures(GLsizei n, const GLuint* textures)
{
    RETURN_WITH_ERROR_IF(n < 0, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    m_name_allocator.free(n, textures);

    for (auto i = 0; i < n; i++) {
        GLuint name = textures[i];

        auto texture_object = m_allocated_textures.find(name);
        if (texture_object == m_allocated_textures.end() || texture_object->value.is_null())
            continue;

        // Check all texture units
        for (auto& texture_unit : m_texture_units) {
            if (texture_object->value == texture_unit.bound_texture())
                texture_unit.unbind_texture(GL_TEXTURE_2D);
        }

        m_allocated_textures.remove(name);
    }
}

void SoftwareGLContext::gl_tex_image_2d(GLenum target, GLint level, GLint internal_format, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* data)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // We only support GL_TEXTURE_2D for now
    RETURN_WITH_ERROR_IF(target != GL_TEXTURE_2D, GL_INVALID_ENUM);

    // Check if there is actually a texture bound
    RETURN_WITH_ERROR_IF(target == GL_TEXTURE_2D && m_active_texture_unit->currently_bound_target() != GL_TEXTURE_2D, GL_INVALID_OPERATION);

    // We only support symbolic constants for now
    RETURN_WITH_ERROR_IF(!(internal_format == GL_RGB || internal_format == GL_RGBA), GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(type != GL_UNSIGNED_BYTE, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(level < 0 || level > Texture2D::LOG2_MAX_TEXTURE_SIZE, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(width < 0 || height < 0 || width > (2 + Texture2D::MAX_TEXTURE_SIZE) || height > (2 + Texture2D::MAX_TEXTURE_SIZE), GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF((width & 2) != 0 || (height & 2) != 0, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(border < 0 || border > 1, GL_INVALID_VALUE);

    m_active_texture_unit->bound_texture_2d()->upload_texture_data(target, level, internal_format, width, height, border, format, type, data);
}

void SoftwareGLContext::gl_front_face(GLenum face)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_front_face, face);

    RETURN_WITH_ERROR_IF(face < GL_CW || face > GL_CCW, GL_INVALID_ENUM);

    m_front_face = face;
}

void SoftwareGLContext::gl_cull_face(GLenum cull_mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_cull_face, cull_mode);

    RETURN_WITH_ERROR_IF(cull_mode < GL_FRONT || cull_mode > GL_FRONT_AND_BACK, GL_INVALID_ENUM);

    m_culled_sides = cull_mode;
}

GLuint SoftwareGLContext::gl_gen_lists(GLsizei range)
{
    RETURN_VALUE_WITH_ERROR_IF(range <= 0, GL_INVALID_VALUE, 0);
    RETURN_VALUE_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION, 0);

    auto initial_entry = m_listings.size();
    m_listings.resize(range + initial_entry);
    return initial_entry + 1;
}

void SoftwareGLContext::gl_call_list(GLuint list)
{
    if (m_gl_call_depth > max_allowed_gl_call_depth)
        return;

    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_call_list, list);

    if (m_listings.size() < list)
        return;

    TemporaryChange change { m_gl_call_depth, m_gl_call_depth + 1 };

    auto& listing = m_listings[list - 1];
    for (auto& entry : listing.entries) {
        entry.function.visit([&](auto& function) {
            entry.arguments.visit([&](auto& arguments) {
                auto apply = [&]<typename... Args>(Args && ... args)
                {
                    if constexpr (requires { (this->*function)(forward<Args>(args)...); })
                        (this->*function)(forward<Args>(args)...);
                };

                arguments.apply_as_args(apply);
            });
        });
    }
}

void SoftwareGLContext::gl_delete_lists(GLuint list, GLsizei range)
{
    if (m_listings.size() < list || m_listings.size() <= list + range)
        return;

    for (auto& entry : m_listings.span().slice(list - 1, range))
        entry.entries.clear();
}

void SoftwareGLContext::gl_end_list()
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(!m_current_listing_index.has_value(), GL_INVALID_OPERATION);

    m_listings[m_current_listing_index->index] = move(m_current_listing_index->listing);
    m_current_listing_index.clear();
}

void SoftwareGLContext::gl_new_list(GLuint list, GLenum mode)
{
    RETURN_WITH_ERROR_IF(list == 0, GL_INVALID_VALUE);
    RETURN_WITH_ERROR_IF(mode != GL_COMPILE && mode != GL_COMPILE_AND_EXECUTE, GL_INVALID_ENUM);
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(m_current_listing_index.has_value(), GL_INVALID_OPERATION);

    if (m_listings.size() < list)
        return;

    m_current_listing_index = CurrentListing { {}, static_cast<size_t>(list - 1), mode };
}

void SoftwareGLContext::gl_flush()
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // No-op since SoftwareGLContext is completely synchronous at the moment
}

void SoftwareGLContext::gl_finish()
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // No-op since SoftwareGLContext is completely synchronous at the moment
}

void SoftwareGLContext::gl_blend_func(GLenum src_factor, GLenum dst_factor)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_blend_func, src_factor, dst_factor);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: The list of allowed enums differs between API versions
    // This was taken from the 2.0 spec on https://docs.gl/gl2/glBlendFunc

    RETURN_WITH_ERROR_IF(!(src_factor == GL_ZERO
                             || src_factor == GL_ONE
                             || src_factor == GL_SRC_COLOR
                             || src_factor == GL_ONE_MINUS_SRC_COLOR
                             || src_factor == GL_DST_COLOR
                             || src_factor == GL_ONE_MINUS_DST_COLOR
                             || src_factor == GL_SRC_ALPHA
                             || src_factor == GL_ONE_MINUS_SRC_ALPHA
                             || src_factor == GL_DST_ALPHA
                             || src_factor == GL_ONE_MINUS_DST_ALPHA
                             || src_factor == GL_CONSTANT_COLOR
                             || src_factor == GL_ONE_MINUS_CONSTANT_COLOR
                             || src_factor == GL_CONSTANT_ALPHA
                             || src_factor == GL_ONE_MINUS_CONSTANT_ALPHA
                             || src_factor == GL_SRC_ALPHA_SATURATE),
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(!(dst_factor == GL_ZERO
                             || dst_factor == GL_ONE
                             || dst_factor == GL_SRC_COLOR
                             || dst_factor == GL_ONE_MINUS_SRC_COLOR
                             || dst_factor == GL_DST_COLOR
                             || dst_factor == GL_ONE_MINUS_DST_COLOR
                             || dst_factor == GL_SRC_ALPHA
                             || dst_factor == GL_ONE_MINUS_SRC_ALPHA
                             || dst_factor == GL_DST_ALPHA
                             || dst_factor == GL_ONE_MINUS_DST_ALPHA
                             || dst_factor == GL_CONSTANT_COLOR
                             || dst_factor == GL_ONE_MINUS_CONSTANT_COLOR
                             || dst_factor == GL_CONSTANT_ALPHA
                             || dst_factor == GL_ONE_MINUS_CONSTANT_ALPHA),
        GL_INVALID_ENUM);

    m_blend_source_factor = src_factor;
    m_blend_destination_factor = dst_factor;

    auto options = m_rasterizer.options();
    options.blend_source_factor = m_blend_source_factor;
    options.blend_destination_factor = m_blend_destination_factor;
    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_shade_model(GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_shade_model, mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(mode != GL_FLAT && mode != GL_SMOOTH, GL_INVALID_ENUM);

    auto options = m_rasterizer.options();
    options.shade_smooth = (mode == GL_SMOOTH);
    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_alpha_func(GLenum func, GLclampf ref)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_alpha_func, func, ref);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(func < GL_NEVER || func > GL_ALWAYS, GL_INVALID_ENUM);

    m_alpha_test_func = func;
    m_alpha_test_ref_value = ref;

    auto options = m_rasterizer.options();
    options.alpha_test_func = m_alpha_test_func;
    options.alpha_test_ref_value = m_alpha_test_ref_value;
    m_rasterizer.set_options(options);
}

void SoftwareGLContext::gl_hint(GLenum target, GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_hint, target, mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    RETURN_WITH_ERROR_IF(target != GL_PERSPECTIVE_CORRECTION_HINT
            && target != GL_POINT_SMOOTH_HINT
            && target != GL_LINE_SMOOTH_HINT
            && target != GL_POLYGON_SMOOTH_HINT
            && target != GL_FOG_HINT
            && target != GL_GENERATE_MIPMAP_HINT
            && target != GL_TEXTURE_COMPRESSION_HINT,
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(mode != GL_DONT_CARE
            && mode != GL_FASTEST
            && mode != GL_NICEST,
        GL_INVALID_ENUM);

    // According to the spec implementors are free to ignore glHint. So we do.
}

void SoftwareGLContext::gl_read_buffer(GLenum mode)
{
    APPEND_TO_CALL_LIST_AND_RETURN_IF_NEEDED(gl_read_buffer, mode);

    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);

    // FIXME: Also allow aux buffers GL_AUX0 through GL_AUX3 here
    // plus any aux buffer between 0 and GL_AUX_BUFFERS
    RETURN_WITH_ERROR_IF(mode != GL_FRONT_LEFT
            && mode != GL_FRONT_RIGHT
            && mode != GL_BACK_LEFT
            && mode != GL_BACK_RIGHT
            && mode != GL_FRONT
            && mode != GL_BACK
            && mode != GL_LEFT
            && mode != GL_RIGHT,
        GL_INVALID_ENUM);

    // FIXME: We do not currently have aux buffers, so make it an invalid
    // operation to select anything but front or back buffers. Also we do
    // not allow selecting the stereoscopic RIGHT buffers since we do not
    // have them configured.
    RETURN_WITH_ERROR_IF(mode != GL_FRONT_LEFT
            && mode != GL_FRONT
            && mode != GL_BACK_LEFT
            && mode != GL_BACK
            && mode != GL_FRONT
            && mode != GL_BACK
            && mode != GL_LEFT,
        GL_INVALID_OPERATION);

    m_current_read_buffer = mode;
}

void SoftwareGLContext::gl_read_pixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    RETURN_WITH_ERROR_IF(width < 0 || height < 0, GL_INVALID_VALUE);

    RETURN_WITH_ERROR_IF(format != GL_COLOR_INDEX
            && format != GL_STENCIL_INDEX
            && format != GL_DEPTH_COMPONENT
            && format != GL_RED
            && format != GL_GREEN
            && format != GL_BLUE
            && format != GL_ALPHA
            && format != GL_RGB
            && format != GL_RGBA
            && format != GL_LUMINANCE
            && format != GL_LUMINANCE_ALPHA,
        GL_INVALID_ENUM);

    RETURN_WITH_ERROR_IF(type != GL_UNSIGNED_BYTE
            && type != GL_BYTE
            && type != GL_BITMAP
            && type != GL_UNSIGNED_SHORT
            && type != GL_SHORT
            && type != GL_BLUE
            && type != GL_UNSIGNED_INT
            && type != GL_INT
            && type != GL_FLOAT,
        GL_INVALID_ENUM);

    // FIXME: We only support RGBA buffers for now.
    // Once we add support for indexed color modes do the correct check here
    RETURN_WITH_ERROR_IF(format == GL_COLOR_INDEX, GL_INVALID_OPERATION);

    // FIXME: We do not have stencil buffers yet
    // Once we add support for stencil buffers do the correct check here
    RETURN_WITH_ERROR_IF(format == GL_STENCIL_INDEX, GL_INVALID_OPERATION);

    if (format == GL_DEPTH_COMPONENT) {
        // FIXME: This check needs to be a bit more sophisticated. Currently the buffers
        // are hardcoded. Once we add proper structures for them we need to correct this check

        // Error because only back buffer has a depth buffer
        RETURN_WITH_ERROR_IF(m_current_read_buffer == GL_FRONT
                || m_current_read_buffer == GL_FRONT_LEFT
                || m_current_read_buffer == GL_FRONT_RIGHT,
            GL_INVALID_OPERATION);
    }

    // Some helper functions for converting float values to integer types
    auto float_to_i8 = [](float f) -> GLchar {
        return static_cast<GLchar>((0x7f * min(max(f, 0.0f), 1.0f) - 1) / 2);
    };

    auto float_to_i16 = [](float f) -> GLshort {
        return static_cast<GLshort>((0x7fff * min(max(f, 0.0f), 1.0f) - 1) / 2);
    };

    auto float_to_i32 = [](float f) -> GLint {
        return static_cast<GLint>((0x7fffffff * min(max(f, 0.0f), 1.0f) - 1) / 2);
    };

    auto float_to_u8 = [](float f) -> GLubyte {
        return static_cast<GLubyte>(0xff * min(max(f, 0.0f), 1.0f));
    };

    auto float_to_u16 = [](float f) -> GLushort {
        return static_cast<GLushort>(0xffff * min(max(f, 0.0f), 1.0f));
    };

    auto float_to_u32 = [](float f) -> GLuint {
        return static_cast<GLuint>(0xffffffff * min(max(f, 0.0f), 1.0f));
    };

    if (format == GL_DEPTH_COMPONENT) {
        // Read from depth buffer
        for (GLsizei i = 0; i < height; ++i) {
            for (GLsizei j = 0; j < width; ++j) {
                float depth = m_rasterizer.get_depthbuffer_value(x + j, y + i);

                switch (type) {
                case GL_BYTE:
                    reinterpret_cast<GLchar*>(pixels)[i * width + j] = float_to_i8(depth);
                    break;
                case GL_SHORT:
                    reinterpret_cast<GLshort*>(pixels)[i * width + j] = float_to_i16(depth);
                    break;
                case GL_INT:
                    reinterpret_cast<GLint*>(pixels)[i * width + j] = float_to_i32(depth);
                    break;
                case GL_UNSIGNED_BYTE:
                    reinterpret_cast<GLubyte*>(pixels)[i * width + j] = float_to_u8(depth);
                    break;
                case GL_UNSIGNED_SHORT:
                    reinterpret_cast<GLushort*>(pixels)[i * width + j] = float_to_u16(depth);
                    break;
                case GL_UNSIGNED_INT:
                    reinterpret_cast<GLuint*>(pixels)[i * width + j] = float_to_u32(depth);
                    break;
                case GL_FLOAT:
                    reinterpret_cast<GLfloat*>(pixels)[i * width + j] = min(max(depth, 0.0f), 1.0f);
                    break;
                }
            }
        }
        return;
    }

    bool write_red = false;
    bool write_green = false;
    bool write_blue = false;
    bool write_alpha = false;
    size_t component_count = 0;
    size_t component_size = 0;
    size_t red_offset = 0;
    size_t green_offset = 0;
    size_t blue_offset = 0;
    size_t alpha_offset = 0;
    char* red_ptr = nullptr;
    char* green_ptr = nullptr;
    char* blue_ptr = nullptr;
    char* alpha_ptr = nullptr;

    switch (format) {
    case GL_RGB:
        write_red = true;
        write_green = true;
        write_blue = true;
        component_count = 3;
        red_offset = 2;
        green_offset = 1;
        blue_offset = 0;
        break;
    case GL_RGBA:
        write_red = true;
        write_green = true;
        write_blue = true;
        write_alpha = true;
        component_count = 4;
        red_offset = 3;
        green_offset = 2;
        blue_offset = 1;
        alpha_offset = 0;
        break;
    case GL_RED:
        write_red = true;
        component_count = 1;
        red_offset = 0;
        break;
    case GL_GREEN:
        write_green = true;
        component_count = 1;
        green_offset = 0;
        break;
    case GL_BLUE:
        write_blue = true;
        component_count = 1;
        blue_offset = 0;
        break;
    case GL_ALPHA:
        write_alpha = true;
        component_count = 1;
        alpha_offset = 0;
        break;
    }

    switch (type) {
    case GL_BYTE:
    case GL_UNSIGNED_BYTE:
        component_size = 1;
        break;
    case GL_SHORT:
    case GL_UNSIGNED_SHORT:
        component_size = 2;
        break;
    case GL_INT:
    case GL_UNSIGNED_INT:
    case GL_FLOAT:
        component_size = 4;
        break;
    }

    char* out_ptr = reinterpret_cast<char*>(pixels);
    for (int i = 0; i < (int)height; ++i) {
        for (int j = 0; j < (int)width; ++j) {
            Gfx::RGBA32 color {};
            if (m_current_read_buffer == GL_FRONT || m_current_read_buffer == GL_LEFT || m_current_read_buffer == GL_FRONT_LEFT) {
                if (y + i >= m_frontbuffer->width() || x + j >= m_frontbuffer->height())
                    color = 0;
                else
                    color = m_frontbuffer->scanline(y + i)[x + j];
            } else {
                color = m_rasterizer.get_backbuffer_pixel(x + j, y + i);
            }

            float red = ((color >> 24) & 0xff) / 255.0f;
            float green = ((color >> 16) & 0xff) / 255.0f;
            float blue = ((color >> 8) & 0xff) / 255.0f;
            float alpha = (color & 0xff) / 255.0f;

            // FIXME: Set up write pointers based on selected endianness (glPixelStore)
            red_ptr = out_ptr + (component_size * red_offset);
            green_ptr = out_ptr + (component_size * green_offset);
            blue_ptr = out_ptr + (component_size * blue_offset);
            alpha_ptr = out_ptr + (component_size * alpha_offset);

            switch (type) {
            case GL_BYTE:
                if (write_red)
                    *reinterpret_cast<GLchar*>(red_ptr) = float_to_i8(red);
                if (write_green)
                    *reinterpret_cast<GLchar*>(green_ptr) = float_to_i8(green);
                if (write_blue)
                    *reinterpret_cast<GLchar*>(blue_ptr) = float_to_i8(blue);
                if (write_alpha)
                    *reinterpret_cast<GLchar*>(alpha_ptr) = float_to_i8(alpha);
                break;
            case GL_UNSIGNED_BYTE:
                if (write_red)
                    *reinterpret_cast<GLubyte*>(red_ptr) = float_to_u8(red);
                if (write_green)
                    *reinterpret_cast<GLubyte*>(green_ptr) = float_to_u8(green);
                if (write_blue)
                    *reinterpret_cast<GLubyte*>(blue_ptr) = float_to_u8(blue);
                if (write_alpha)
                    *reinterpret_cast<GLubyte*>(alpha_ptr) = float_to_u8(alpha);
                break;
            case GL_SHORT:
                if (write_red)
                    *reinterpret_cast<GLshort*>(red_ptr) = float_to_i16(red);
                if (write_green)
                    *reinterpret_cast<GLshort*>(green_ptr) = float_to_i16(green);
                if (write_blue)
                    *reinterpret_cast<GLshort*>(blue_ptr) = float_to_i16(blue);
                if (write_alpha)
                    *reinterpret_cast<GLshort*>(alpha_ptr) = float_to_i16(alpha);
                break;
            case GL_UNSIGNED_SHORT:
                if (write_red)
                    *reinterpret_cast<GLushort*>(red_ptr) = float_to_u16(red);
                if (write_green)
                    *reinterpret_cast<GLushort*>(green_ptr) = float_to_u16(green);
                if (write_blue)
                    *reinterpret_cast<GLushort*>(blue_ptr) = float_to_u16(blue);
                if (write_alpha)
                    *reinterpret_cast<GLushort*>(alpha_ptr) = float_to_u16(alpha);
                break;
            case GL_INT:
                if (write_red)
                    *reinterpret_cast<GLint*>(red_ptr) = float_to_i32(red);
                if (write_green)
                    *reinterpret_cast<GLint*>(green_ptr) = float_to_i32(green);
                if (write_blue)
                    *reinterpret_cast<GLint*>(blue_ptr) = float_to_i32(blue);
                if (write_alpha)
                    *reinterpret_cast<GLint*>(alpha_ptr) = float_to_i32(alpha);
                break;
            case GL_UNSIGNED_INT:
                if (write_red)
                    *reinterpret_cast<GLuint*>(red_ptr) = float_to_u32(red);
                if (write_green)
                    *reinterpret_cast<GLuint*>(green_ptr) = float_to_u32(green);
                if (write_blue)
                    *reinterpret_cast<GLuint*>(blue_ptr) = float_to_u32(blue);
                if (write_alpha)
                    *reinterpret_cast<GLuint*>(alpha_ptr) = float_to_u32(alpha);
                break;
            case GL_FLOAT:
                if (write_red)
                    *reinterpret_cast<GLfloat*>(red_ptr) = min(max(red, 0.0f), 1.0f);
                if (write_green)
                    *reinterpret_cast<GLfloat*>(green_ptr) = min(max(green, 0.0f), 1.0f);
                if (write_blue)
                    *reinterpret_cast<GLfloat*>(blue_ptr) = min(max(blue, 0.0f), 1.0f);
                if (write_alpha)
                    *reinterpret_cast<GLfloat*>(alpha_ptr) = min(max(alpha, 0.0f), 1.0f);
                break;
            }

            out_ptr += component_size * component_count;
        }
    }
}

void SoftwareGLContext::gl_bind_texture(GLenum target, GLuint texture)
{
    RETURN_WITH_ERROR_IF(m_in_draw_state, GL_INVALID_OPERATION);
    // FIXME: We only support GL_TEXTURE_2D for now
    RETURN_WITH_ERROR_IF(target != GL_TEXTURE_2D, GL_INVALID_ENUM);

    if (texture == 0) {
        switch (target) {
        case GL_TEXTURE_2D:
            m_active_texture_unit->unbind_texture(target);
            return;
        default:
            VERIFY_NOT_REACHED();
            return;
        }
    }

    auto it = m_allocated_textures.find(texture);

    // The texture name does not exist
    RETURN_WITH_ERROR_IF(it == m_allocated_textures.end(), GL_INVALID_VALUE);

    auto texture_object = it->value;

    // Binding a texture to a different target than it was first bound is an invalid operation
    // FIXME: We only support GL_TEXTURE_2D for now
    RETURN_WITH_ERROR_IF(target == GL_TEXTURE_2D && !texture_object.is_null() && !texture_object->is_texture_2d(), GL_INVALID_OPERATION);

    if (!texture_object) {
        // This is the first time the texture is bound. Allocate an actual texture object
        switch (target) {
        case GL_TEXTURE_2D:
            texture_object = adopt_ref(*new Texture2D());
            break;
        default:
            VERIFY_NOT_REACHED();
            break;
        }

        m_allocated_textures.set(texture, texture_object);
    }

    switch (target) {
    case GL_TEXTURE_2D:
        m_active_texture_unit->bind_texture_to_target(target, texture_object);
        break;
    }
}

void SoftwareGLContext::gl_active_texture(GLenum texture)
{
    RETURN_WITH_ERROR_IF(texture < GL_TEXTURE0 || texture > GL_TEXTURE31, GL_INVALID_ENUM);

    m_active_texture_unit = &m_texture_units.at(texture - GL_TEXTURE0);
}

void SoftwareGLContext::present()
{
    m_rasterizer.blit_to(*m_frontbuffer);
}
}
