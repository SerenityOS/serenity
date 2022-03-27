/*
 * Copyright (c) 2022, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Vector4.h>

namespace GPU {

struct Material {
    FloatVector4 ambient { 0.2f, 0.2f, 0.2f, 1.0f };
    FloatVector4 diffuse { 0.8f, 0.8f, 0.8f, 1.0f };
    FloatVector4 specular { 0.0f, 0.0f, 0.0f, 1.0f };
    FloatVector4 emissive { 0.0f, 0.0f, 0.0f, 1.0f };
    float shininess { 0.0f };
    float ambient_color_index { 0.0f };
    float diffuse_color_index { 1.0f };
    float specular_color_index { 1.0f };
};

}
