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

// Point in 3D space
struct Vertex {
    GLfloat x;
    GLfloat y;
    GLfloat z;
};

}
