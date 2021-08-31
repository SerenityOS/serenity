/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Mathieu Gaillard <gaillard.mathieu.39@gmail.com>
 * Copyright (c) 2021, Conrad Pankoff <deoxxa@fknsrs.biz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGL/GL/gl.h>

namespace ThreeDee {

// A triangle defines a single "face" of a mesh
struct Triangle {
    GLuint a;
    GLuint b;
    GLuint c;

    GLuint tex_coord_index0;
    GLuint tex_coord_index1;
    GLuint tex_coord_index2;
};

}
