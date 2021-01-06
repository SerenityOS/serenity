/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GL/gl.h"

namespace GL {

struct GLColor {
    GLclampf r, g, b, a;
};

struct GLVertex {
    GLfloat x, y, z, w;
    GLfloat r, g, b, a;
    GLfloat u, v;
};

struct GLTriangle {
    GLVertex vertices[3];
};

struct GLEdge {
    GLfloat x1;
    GLfloat y1;
    GLfloat x2;
    GLfloat y2;
};

}
