/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Mathieu Gaillard <gaillard.mathieu.39@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGL/GL/gl.h>
#include <LibGfx/Color.h>
#include <stdlib.h>

#include "Mesh.h"

const Color colors[] {
    Color::Red,
    Color::Green,
    Color::Blue,
    Color::Blue,
    Color::Magenta,
    Color::White,
    Color::Yellow,
};

Mesh::Mesh(Vector<Vertex> vertices, Vector<Triangle> triangles)
    : m_vertex_list(move(vertices))
    , m_triangle_list(move(triangles))
{
}

void Mesh::draw()
{
    u32 color_index = 0;
    Color cur_color;

    for (const auto& triangle : m_triangle_list) {
        cur_color = colors[color_index];

        glBegin(GL_TRIANGLES);
        glColor4ub(cur_color.red(), cur_color.green(), cur_color.blue(), 255);

        // Vertex 1
        glVertex3f(
            m_vertex_list.at(triangle.a).x,
            m_vertex_list.at(triangle.a).y,
            m_vertex_list.at(triangle.a).z);

        // Vertex 2
        glVertex3f(
            m_vertex_list.at(triangle.b).x,
            m_vertex_list.at(triangle.b).y,
            m_vertex_list.at(triangle.b).z);

        // Vertex 3
        glVertex3f(
            m_vertex_list.at(triangle.c).x,
            m_vertex_list.at(triangle.c).y,
            m_vertex_list.at(triangle.c).z);

        glEnd();

        color_index = ((color_index + 1) % 7);
    }
}
