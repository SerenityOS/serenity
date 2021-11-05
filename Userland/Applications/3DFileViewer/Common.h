/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Mathieu Gaillard <gaillard.mathieu.39@gmail.com>
 * Copyright (c) 2021, Pedro Pereira <pmh.pereira@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGL/GL/gl.h>

// Point in 3D space
struct Vertex {
    GLfloat x;
    GLfloat y;
    GLfloat z;
};

struct TexCoord {
    GLfloat u;
    GLfloat v;
};

// A triangle defines a single "face" of a mesh
struct Triangle {
    GLuint a;
    GLuint b;
    GLuint c;

    GLuint tex_coord_index0;
    GLuint tex_coord_index1;
    GLuint tex_coord_index2;

    GLuint normal_index0;
    GLuint normal_index1;
    GLuint normal_index2;
};
