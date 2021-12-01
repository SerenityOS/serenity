/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GL/gl.h"
#include <LibGfx/Vector2.h>
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>

namespace GL {

struct GLColor {
    GLclampf r, g, b, a;
};

struct GLVertex {
    FloatVector4 position;
    FloatVector4 color;
    FloatVector2 tex_coord;
    FloatVector3 normal;
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
