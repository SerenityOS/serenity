/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>

namespace SoftGPU {

struct Vertex {
    FloatVector4 position;
    FloatVector4 eye_coordinates;
    FloatVector4 clip_coordinates;
    FloatVector4 window_coordinates;
    FloatVector4 color;
    FloatVector4 tex_coord;
    FloatVector3 normal;
};

}
