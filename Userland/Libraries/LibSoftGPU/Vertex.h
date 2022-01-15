/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Config.h>

namespace SoftGPU {

struct Vertex {
    FloatVector4 position;
    FloatVector4 eye_coordinates;
    FloatVector4 clip_coordinates;
    FloatVector4 window_coordinates;
    FloatVector4 color;
    Array<FloatVector4, NUM_SAMPLERS> tex_coords;
    FloatVector3 normal;
};

}
