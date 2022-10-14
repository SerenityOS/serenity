/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <Demos/Tubes/Shapes.h>
#include <LibGL/GL/gl.h>
#include <LibGfx/Vector3.h>
#include <math.h>

constexpr u8 sphere_number_of_segments = 4;
constexpr u8 tube_number_of_segments = 12;

void draw_sphere()
{
    // Draw a sphere by drawing a cube with many segments with normalized coordinates
    glBegin(GL_QUADS);
    auto draw_segment = [](Array<DoubleVector3, 4> corners, Optional<int> flip_a, Optional<int> flip_b, Optional<int> swap_a, Optional<int> swap_b) {
        for (DoubleVector3& corner : corners) {
            if (flip_a.has_value())
                corner[flip_a.value()] *= -1;
            if (flip_b.has_value())
                corner[flip_b.value()] *= -1;
            if (swap_a.has_value() && swap_b.has_value())
                swap(corner[swap_a.value()], corner[swap_b.value()]);

            glNormal3d(corner.x(), corner.y(), corner.z());
            glVertex3d(corner.x(), corner.y(), corner.z());
        }
    };
    double const segment_size = 2. / sphere_number_of_segments;
    for (int y = 0; y < sphere_number_of_segments; y++) {
        for (int x = 0; x < sphere_number_of_segments; x++) {
            DoubleVector3 bottomleft = { -1. + x * segment_size, -1. + y * segment_size, 1. };
            DoubleVector3 bottomright = { -1. + (x + 1) * segment_size, -1. + y * segment_size, 1. };
            DoubleVector3 topright = { -1. + (x + 1) * segment_size, -1. + (y + 1) * segment_size, 1. };
            DoubleVector3 topleft = { -1. + x * segment_size, -1. + (y + 1) * segment_size, 1. };

            Array<DoubleVector3, 4> normalized_corners = {
                bottomleft.normalized(),
                bottomright.normalized(),
                topright.normalized(),
                topleft.normalized(),
            };

            draw_segment(normalized_corners, {}, {}, {}, {}); // front face
            draw_segment(normalized_corners, 0, 2, {}, {});   // back face
            draw_segment(normalized_corners, 2, {}, 0, 2);    // left face
            draw_segment(normalized_corners, 0, {}, 0, 2);    // right face
            draw_segment(normalized_corners, 1, {}, 1, 2);    // top face
            draw_segment(normalized_corners, 2, {}, 1, 2);    // bottom face
        }
    }
    glEnd();
}

void draw_tube()
{
    glBegin(GL_QUADS);
    double const segment_angle = 2 * M_PI / tube_number_of_segments;
    double last_x = 0.;
    double last_y = 1.;
    for (int i = 1; i <= tube_number_of_segments; ++i) {
        double angle = i * segment_angle;
        double segment_x = sin(angle);
        double segment_y = cos(angle);

        glNormal3d(last_x, last_y, 0.);
        glVertex3d(last_x, last_y, 0.);
        glNormal3d(segment_x, segment_y, 0.);
        glVertex3d(segment_x, segment_y, 0.);
        glVertex3d(segment_x, segment_y, -2.);
        glNormal3d(last_x, last_y, 0.);
        glVertex3d(last_x, last_y, -2.);

        last_x = segment_x;
        last_y = segment_y;
    }
    glEnd();
}
