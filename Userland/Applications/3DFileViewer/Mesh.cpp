/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Mathieu Gaillard <gaillard.mathieu.39@gmail.com>
 * Copyright (c) 2021, Pedro Pereira <pmh.pereira@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GL/gl.h>
#include <LibGfx/Color.h>
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>

#include "Mesh.h"

const Color colors[] {
    Color::Red,
    Color::Green,
    Color::Blue,
    Color::Magenta,
    Color::Yellow,
    Color::Cyan,
    Color::White
};

Mesh::Mesh(Vector<Vertex> vertices, Vector<TexCoord> tex_coords, Vector<Vertex> normals, Vector<Triangle> triangles)
    : m_vertex_list(move(vertices))
    , m_tex_coords(move(tex_coords))
    , m_normal_list(move(normals))
    , m_triangle_list(move(triangles))
{
}

void Mesh::draw(float uv_scale)
{
    for (u32 i = 0; i < m_triangle_list.size(); i++) {
        const auto& triangle = m_triangle_list[i];

        const FloatVector3 vertex_a(
            m_vertex_list.at(triangle.a).x,
            m_vertex_list.at(triangle.a).y,
            m_vertex_list.at(triangle.a).z);

        const FloatVector3 vertex_b(
            m_vertex_list.at(triangle.b).x,
            m_vertex_list.at(triangle.b).y,
            m_vertex_list.at(triangle.b).z);

        const FloatVector3 vertex_c(
            m_vertex_list.at(triangle.c).x,
            m_vertex_list.at(triangle.c).y,
            m_vertex_list.at(triangle.c).z);

        FloatVector3 normal;
        if (has_normals()) {
            const FloatVector3 normal_a(
                m_normal_list.at(triangle.normal_index0).x,
                m_normal_list.at(triangle.normal_index0).y,
                m_normal_list.at(triangle.normal_index0).z);

            const FloatVector3 normal_b(
                m_normal_list.at(triangle.normal_index1).x,
                m_normal_list.at(triangle.normal_index1).y,
                m_normal_list.at(triangle.normal_index1).z);

            const FloatVector3 normal_c(
                m_normal_list.at(triangle.normal_index2).x,
                m_normal_list.at(triangle.normal_index2).y,
                m_normal_list.at(triangle.normal_index2).z);

            normal = (normal_a + normal_b + normal_c).normalized();
        } else {
            // Compute the triangle normal
            const FloatVector3 vec_ab = vertex_b - vertex_a;
            const FloatVector3 vec_ac = vertex_c - vertex_a;
            normal = vec_ab.cross(vec_ac).normalized();
        }

        glBegin(GL_TRIANGLES);

        if (is_textured())
            glTexCoord2f(m_tex_coords.at(triangle.tex_coord_index0).u * uv_scale, (1.0f - m_tex_coords.at(triangle.tex_coord_index0).v) * uv_scale);

        // Upload the face normal
        glNormal3f(normal.x(), normal.y(), normal.z());

        // Vertex 1
        glVertex3f(
            m_vertex_list.at(triangle.a).x,
            m_vertex_list.at(triangle.a).y,
            m_vertex_list.at(triangle.a).z);

        if (is_textured())
            glTexCoord2f(m_tex_coords.at(triangle.tex_coord_index1).u * uv_scale, (1.0f - m_tex_coords.at(triangle.tex_coord_index1).v) * uv_scale);

        // Vertex 2
        glVertex3f(
            m_vertex_list.at(triangle.b).x,
            m_vertex_list.at(triangle.b).y,
            m_vertex_list.at(triangle.b).z);

        if (is_textured())
            glTexCoord2f(m_tex_coords.at(triangle.tex_coord_index2).u * uv_scale, (1.0f - m_tex_coords.at(triangle.tex_coord_index2).v) * uv_scale);

        // Vertex 3
        glVertex3f(
            m_vertex_list.at(triangle.c).x,
            m_vertex_list.at(triangle.c).y,
            m_vertex_list.at(triangle.c).z);

        glEnd();
    }
}
