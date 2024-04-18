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

Color const colors[] {
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
        auto const& triangle = m_triangle_list[i];

        FloatVector3 const vertex_a(
            m_vertex_list.at(triangle.a).x,
            m_vertex_list.at(triangle.a).y,
            m_vertex_list.at(triangle.a).z);

        FloatVector3 const vertex_b(
            m_vertex_list.at(triangle.b).x,
            m_vertex_list.at(triangle.b).y,
            m_vertex_list.at(triangle.b).z);

        FloatVector3 const vertex_c(
            m_vertex_list.at(triangle.c).x,
            m_vertex_list.at(triangle.c).y,
            m_vertex_list.at(triangle.c).z);

        FloatVector3 normal_a, normal_b, normal_c;
        if (has_normals()) {
            normal_a = FloatVector3(
                m_normal_list.at(triangle.normal_index0).x,
                m_normal_list.at(triangle.normal_index0).y,
                m_normal_list.at(triangle.normal_index0).z);

            normal_b = FloatVector3(
                m_normal_list.at(triangle.normal_index1).x,
                m_normal_list.at(triangle.normal_index1).y,
                m_normal_list.at(triangle.normal_index1).z);

            normal_c = FloatVector3(
                m_normal_list.at(triangle.normal_index2).x,
                m_normal_list.at(triangle.normal_index2).y,
                m_normal_list.at(triangle.normal_index2).z);

        } else {
            // Compute the triangle normal
            FloatVector3 const vec_ab = vertex_b - vertex_a;
            FloatVector3 const vec_ac = vertex_c - vertex_a;
            normal_a = vec_ab.cross(vec_ac).normalized();
            normal_b = normal_a;
            normal_c = normal_a;
        }

        glBegin(GL_TRIANGLES);

        if (is_textured())
            glTexCoord2f(m_tex_coords.at(triangle.tex_coord_index0).u * uv_scale, (1.0f - m_tex_coords.at(triangle.tex_coord_index0).v) * uv_scale);

        // Vertex 1
        glNormal3f(normal_a.x(), normal_a.y(), normal_a.z());
        glVertex3f(
            m_vertex_list.at(triangle.a).x,
            m_vertex_list.at(triangle.a).y,
            m_vertex_list.at(triangle.a).z);

        if (is_textured())
            glTexCoord2f(m_tex_coords.at(triangle.tex_coord_index1).u * uv_scale, (1.0f - m_tex_coords.at(triangle.tex_coord_index1).v) * uv_scale);

        // Vertex 2
        glNormal3f(normal_b.x(), normal_b.y(), normal_b.z());
        glVertex3f(
            m_vertex_list.at(triangle.b).x,
            m_vertex_list.at(triangle.b).y,
            m_vertex_list.at(triangle.b).z);

        if (is_textured())
            glTexCoord2f(m_tex_coords.at(triangle.tex_coord_index2).u * uv_scale, (1.0f - m_tex_coords.at(triangle.tex_coord_index2).v) * uv_scale);

        // Vertex 3
        glNormal3f(normal_c.x(), normal_c.y(), normal_c.z());
        glVertex3f(
            m_vertex_list.at(triangle.c).x,
            m_vertex_list.at(triangle.c).y,
            m_vertex_list.at(triangle.c).z);

        glEnd();
    }
}
