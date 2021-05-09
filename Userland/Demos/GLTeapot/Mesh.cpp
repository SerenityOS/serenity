/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
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

void Mesh::draw()
{
    u32 color_index = 0;
    Color cur_color;

    for (const auto& triangle : m_triangle_list) {
        cur_color = colors[color_index];

        glBegin(GL_TRIANGLES);
        glColor4ub(cur_color.red(), cur_color.green(), cur_color.blue(), 255);

        glVertex3f(triangle.a.x, triangle.a.y, triangle.a.z); // Vertex 1
        glVertex3f(triangle.b.x, triangle.b.y, triangle.b.z); // Vertex 2
        glVertex3f(triangle.c.x, triangle.c.y, triangle.c.z); // Vertex 3

        glEnd();

        color_index = ((color_index + 1) % 7);
    }
}
