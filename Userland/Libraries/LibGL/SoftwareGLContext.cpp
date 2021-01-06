/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SoftwareGLContext.h"
#include "GLStruct.h"
#include <AK/Assertions.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/QuickSort.h>
#include <AK/Vector.h>
#include <LibGfx/Vector4.h>
#include <math.h>

using AK::dbgln;

namespace GL {

static constexpr size_t NUM_CLIP_PLANES = 6;

static FloatVector4 clip_planes[] = {
    { -1, 0, 0, 1 }, // Left Plane
    { 1, 0, 0, 1 },  // Right Plane
    { 0, 1, 0, 1 },  // Top Plane
    { 0, -1, 0, 1 }, // Bottom plane
    { 0, 0, 1, 1 },  // Near Plane
    { 0, 0, -1, 1 }  // Far Plane
};

static FloatVector4 clip_plane_normals[] = {
    { 1, 0, 0, 1 },  // Left Plane
    { -1, 0, 0, 1 }, // Right Plane
    { 0, -1, 0, 1 }, // Top Plane
    { 0, 1, 0, 1 },  // Bottom plane
    { 0, 0, -1, 1 }, // Near Plane
    { 0, 0, 1, 1 }   // Far Plane
};

enum ClippingPlane {
    LEFT = 0,
    RIGHT = 1,
    TOP = 2,
    BOTTOM = 3,
    NEAR = 4,
    FAR = 5
};

// FIXME: Change this to accept a vertex!
// Determines whether or not a vertex is inside the frustum for a given plane
static bool vert_inside_plane(const FloatVector4& vec, ClippingPlane plane)
{
    switch (plane) {
    case ClippingPlane::LEFT:
        return vec.x() > -vec.w();
    case ClippingPlane::RIGHT:
        return vec.x() < vec.w();
    case ClippingPlane::TOP:
        return vec.y() < vec.w();
    case ClippingPlane::BOTTOM:
        return vec.y() > -vec.w();
    case ClippingPlane::NEAR:
        return vec.z() > -vec.w();
    case ClippingPlane::FAR:
        return vec.z() < vec.w();
    }

    return false;
}

// FIXME: This needs to interpolate color/UV data as well!
static FloatVector4 clip_intersection_point(const FloatVector4& vec, const FloatVector4& prev_vec, ClippingPlane plane_index)
{
    // https://github.com/fogleman/fauxgl/blob/master/clipping.go#L20
    FloatVector4 u, w;
    FloatVector4 ret = prev_vec;
    FloatVector4 plane = clip_planes[plane_index];
    FloatVector4 plane_normal = clip_plane_normals[plane_index];

    u = vec;
    u -= prev_vec;
    w = prev_vec;
    w -= plane;
    float d = plane_normal.dot(u);
    float n = -plane_normal.dot(w);

    ret += (u * (n / d));
    return ret;
}

// https://groups.csail.mit.edu/graphics/classes/6.837/F04/lectures/07_Pipeline_II.pdf
// This is a really rough implementation of the Sutherland-Hodgman algorithm in clip-space
static void clip_triangle_against_frustum(Vector<FloatVector4>& in_vec)
{
    Vector<FloatVector4> clipped_polygon = in_vec; // in_vec = subjectPolygon, clipped_polygon = outputList
    for (size_t i = 0; i < NUM_CLIP_PLANES; i++)   // Test against each clip plane
    {
        ClippingPlane plane = static_cast<ClippingPlane>(i); // Hahaha, what the fuck
        in_vec = clipped_polygon;
        clipped_polygon.clear();

        // Prevent a crash from .at() undeflow
        if (in_vec.size() == 0)
            return;

        FloatVector4 prev_vec = in_vec.at(in_vec.size() - 1);

        for (size_t j = 0; j < in_vec.size(); j++) // Perform this for each vertex
        {
            const FloatVector4& vec = in_vec.at(j);
            if (vert_inside_plane(vec, plane)) {
                if (!vert_inside_plane(prev_vec, plane)) {
                    FloatVector4 intersect = clip_intersection_point(prev_vec, vec, plane);
                    clipped_polygon.append(intersect);
                }

                clipped_polygon.append(vec);
            } else if (vert_inside_plane(prev_vec, plane)) {
                FloatVector4 intersect = clip_intersection_point(prev_vec, vec, plane);
                clipped_polygon.append(intersect);
            }

            prev_vec = vec;
        }
    }
}

void SoftwareGLContext::gl_begin(GLenum mode)
{
    m_current_draw_mode = mode;
}

void SoftwareGLContext::gl_clear(GLbitfield mask)
{
    if (mask & GL_COLOR_BUFFER_BIT) {
        uint8_t r = static_cast<uint8_t>(floor(m_clear_color.x() * 255.0f));
        uint8_t g = static_cast<uint8_t>(floor(m_clear_color.y() * 255.0f));
        uint8_t b = static_cast<uint8_t>(floor(m_clear_color.z() * 255.0f));

        uint64_t color = r << 16 | g << 8 | b;
        (void)(color);
    } else {
        // set gl error here!?
    }
}

void SoftwareGLContext::gl_clear_color(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    m_clear_color = { red, green, blue, alpha };
}

void SoftwareGLContext::gl_color(GLdouble r, GLdouble g, GLdouble b, GLdouble a)
{
    m_current_vertex_color = { (float)r, (float)g, (float)b, (float)a };
}

void SoftwareGLContext::gl_end()
{
    // At this point, the user has effectively specified that they are done with defining the geometry
    // of what they want to draw. We now need to do a few things (https://www.khronos.org/opengl/wiki/Rendering_Pipeline_Overview):
    //
    // 1.   Transform all of the vertices in the current vertex list into eye space by mulitplying the model-view matrix
    // 2.   Transform all of the vertices from eye space into clip space by multiplying by the projection matrix
    // 3.   If culling is enabled, we cull the desired faces (https://learnopengl.com/Advanced-OpenGL/Face-culling)
    // 4.   Each element of the vertex is then divided by w to bring the positions into NDC (Normalized Device Coordinates)
    // 5.   The vertices are sorted (for the rasteriser, how are we doing this? 3Dfx did this top to bottom in terms of vertex y co-ordinates)
    // 6.   The vertices are then sent off to the rasteriser and drawn to the screen

    // FIXME: Don't assume screen dimensions
    float scr_width = 640.0f;
    float scr_height = 480.0f;

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
        VERIFY_NOT_REACHED();
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
        clip_triangle_against_frustum(vecs);

        // TODO: Copy color and UV information too!
        for (size_t vec_idx = 0; vec_idx < vecs.size(); vec_idx++) {
            FloatVector4& vec = vecs.at(vec_idx);
            GLVertex vertex;

            // Perform the perspective divide
            if (vec.w() != 0.0f) {
                vec.set_x(vec.x() / vec.w());
                vec.set_y(vec.y() / vec.w());
                vec.set_z(vec.z() / vec.w());
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
            } else if (vec_idx == 1) {
                vertex.r = vertexb.r;
                vertex.g = vertexb.g;
                vertex.b = vertexb.b;
                vertex.a = vertexb.a;
            } else {
                vertex.r = vertexc.r;
                vertex.g = vertexc.g;
                vertex.b = vertexc.b;
                vertex.a = vertexc.a;
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
        Vector<GLVertex> sort_vert_list;
        GLTriangle& triangle = processed_triangles.at(i);

        // Now we sort the vertices by their y values. A is the vertex that has the least y value,
        // B is the middle and C is the bottom.
        // These are sorted in groups of 3
        sort_vert_list.append(triangle.vertices[0]);
        sort_vert_list.append(triangle.vertices[1]);
        sort_vert_list.append(triangle.vertices[2]);

        AK::quick_sort(sort_vert_list.begin(), sort_vert_list.end(), [](auto& a, auto& b) { return a.y < b.y; });

        triangle.vertices[0] = sort_vert_list.at(0);
        triangle.vertices[1] = sort_vert_list.at(1);
        triangle.vertices[2] = sort_vert_list.at(2);

        // Let's calculate the (signed) area of the triangle
        // https://cp-algorithms.com/geometry/oriented-triangle-area.html
        float dxAB = triangle.vertices[0].x - triangle.vertices[1].x; // A.x - B.x
        float dxBC = triangle.vertices[1].x - triangle.vertices[2].x; // B.X - C.x
        float dyAB = triangle.vertices[0].y - triangle.vertices[1].y;
        float dyBC = triangle.vertices[1].y - triangle.vertices[2].y;
        float area = (dxAB * dyBC) - (dxBC * dyAB);

        if (area == 0.0f)
            continue;

        int32_t vertexAx = triangle.vertices[0].x;
        int32_t vertexAy = triangle.vertices[0].y;
        int32_t vertexBx = triangle.vertices[1].x;
        int32_t vertexBy = triangle.vertices[1].y;
        int32_t vertexCx = triangle.vertices[2].x;
        int32_t vertexCy = triangle.vertices[2].y;
        (void)(vertexAx);
        (void)(vertexAy);
        (void)(vertexBx);
        (void)(vertexBy);
        (void)(vertexCx);
        (void)(vertexCy);
    }

    triangle_list.clear();
    processed_triangles.clear();
    vertex_list.clear();
}

void SoftwareGLContext::gl_frustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top, GLdouble near_val, GLdouble far_val)
{
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

GLubyte* SoftwareGLContext::gl_get_string(GLenum name)
{
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

    // FIXME: Set glError to GL_INVALID_ENUM here
    return nullptr;
}

void SoftwareGLContext::gl_load_identity()
{
    if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = FloatMatrix4x4::identity();
    else if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = FloatMatrix4x4::identity();
    else
        VERIFY_NOT_REACHED();
}

void SoftwareGLContext::gl_matrix_mode(GLenum mode)
{
    VERIFY(mode == GL_MODELVIEW || mode == GL_PROJECTION);
    m_current_matrix_mode = mode;
}

void SoftwareGLContext::gl_push_matrix()
{
    dbgln_if(GL_DEBUG, "glPushMatrix(): Pushing matrix to the matrix stack (matrix_mode {})", m_current_matrix_mode);

    switch (m_current_matrix_mode) {
    case GL_PROJECTION:
        m_projection_matrix_stack.append(m_projection_matrix);
        break;
    case GL_MODELVIEW:
        m_model_view_matrix_stack.append(m_model_view_matrix);
        break;
    default:
        dbgln_if(GL_DEBUG, "glPushMatrix(): Attempt to push matrix with invalid matrix mode {})", m_current_matrix_mode);
        return;
    }
}

void SoftwareGLContext::gl_pop_matrix()
{
    dbgln_if(GL_DEBUG, "glPopMatrix(): Popping matrix from matrix stack (matrix_mode = {})", m_current_matrix_mode);

    // FIXME: Make sure stack::top() doesn't cause any  nasty issues if it's empty (that could result in a lockup/hang)
    switch (m_current_matrix_mode) {
    case GL_PROJECTION:
        m_projection_matrix = m_projection_matrix_stack.take_last();
        break;
    case GL_MODELVIEW:
        m_model_view_matrix = m_model_view_matrix_stack.take_last();
        break;
    default:
        dbgln_if(GL_DEBUG, "glPopMatrix(): Attempt to pop matrix with invalid matrix mode, {}", m_current_matrix_mode);
        return;
    }
}

void SoftwareGLContext::gl_rotate(GLdouble angle, GLdouble x, GLdouble y, GLdouble z)
{
    FloatVector3 axis = { (float)x, (float)y, (float)z };
    axis.normalize();
    auto rotation_mat = FloatMatrix4x4::rotate(axis, angle);

    if (m_current_matrix_mode == GL_MODELVIEW)
        m_model_view_matrix = m_model_view_matrix * rotation_mat;
    else if (m_current_matrix_mode == GL_PROJECTION)
        m_projection_matrix = m_projection_matrix * rotation_mat;
}

void SoftwareGLContext::gl_translate(GLdouble x, GLdouble y, GLdouble z)
{
    if (m_current_matrix_mode == GL_MODELVIEW) {
        m_model_view_matrix = m_model_view_matrix * FloatMatrix4x4::translate({ (float)x, (float)y, (float)z });
    } else if (m_current_matrix_mode == GL_PROJECTION) {
        m_projection_matrix = m_projection_matrix * FloatMatrix4x4::translate({ (float)x, (float)y, (float)z });
    }
}

void SoftwareGLContext::gl_vertex(GLdouble x, GLdouble y, GLdouble z, GLdouble w)
{
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

void SoftwareGLContext::gl_viewport(GLint x, GLint y, GLsizei width, GLsizei height)
{
    (void)(x);
    (void)(y);
    (void)(width);
    (void)(height);
}

}
