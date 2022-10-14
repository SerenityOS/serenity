/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <LibGPU/Config.h>
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>

namespace GPU {

struct Vertex {
    FloatVector4 position;
    FloatVector4 eye_coordinates;
    FloatVector4 clip_coordinates;
    FloatVector4 window_coordinates;
    FloatVector4 color;
    Array<FloatVector4, GPU::NUM_TEXTURE_UNITS> tex_coords;
    FloatVector3 normal;
};

}
