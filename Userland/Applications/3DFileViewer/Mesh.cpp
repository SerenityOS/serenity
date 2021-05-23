/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Mathieu Gaillard <gaillard.mathieu.39@gmail.com>
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

Mesh::Mesh(Vector<Vertex> vertices, Vector<TexCoord> tex_coords, Vector<Triangle> triangles)
    : m_vertex_list(move(vertices))
    , m_tex_coords(move(tex_coords))
    , m_triangle_list(move(triangles))
{
}

void Mesh::draw()
{
    // Light direction
    const FloatVector3 light_direction(1.f, 1.f, 1.f);

    // Mesh color
    const FloatVector4 mesh_ambient_color(0.2f, 0.2f, 0.2f, 1.f);
    const FloatVector4 mesh_diffuse_color(0.6f, 0.6f, 0.6f, 1.f);

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

        // Compute the triangle normal
        const FloatVector3 vec_ab = vertex_b - vertex_a;
        const FloatVector3 vec_ac = vertex_c - vertex_a;
        const FloatVector3 normal = vec_ab.cross(vec_ac).normalized();

        // Compute lighting with a Lambertian color model
        const auto light_intensity = max(light_direction.dot(normal), 0.f);
        const FloatVector4 color = mesh_ambient_color
            + mesh_diffuse_color * light_intensity;

        glBegin(GL_TRIANGLES);
        glColor4f(color.x(), color.y(), color.z(), color.w());

        // Vertex 1
        glVertex3f(
            m_vertex_list.at(m_triangle_list[i].a).x,
            m_vertex_list.at(m_triangle_list[i].a).y,
            m_vertex_list.at(m_triangle_list[i].a).z);

        if (is_textured())
            glTexCoord2f(m_tex_coords.at(m_triangle_list[i].tex_coord_index0).u, 1.0f - m_tex_coords.at(m_triangle_list[i].tex_coord_index0).v);

        // Vertex 2
        glVertex3f(
            m_vertex_list.at(m_triangle_list[i].b).x,
            m_vertex_list.at(m_triangle_list[i].b).y,
            m_vertex_list.at(m_triangle_list[i].b).z);

        if (is_textured())
            glTexCoord2f(m_tex_coords.at(m_triangle_list[i].tex_coord_index1).u, 1.0f - m_tex_coords.at(m_triangle_list[i].tex_coord_index1).v);

        // Vertex 3
        glVertex3f(
            m_vertex_list.at(m_triangle_list[i].c).x,
            m_vertex_list.at(m_triangle_list[i].c).y,
            m_vertex_list.at(m_triangle_list[i].c).z);

        if (is_textured())
            glTexCoord2f(m_tex_coords.at(m_triangle_list[i].tex_coord_index2).u, 1.0f - m_tex_coords.at(m_triangle_list[i].tex_coord_index2).v);

        glEnd();
    }
}
