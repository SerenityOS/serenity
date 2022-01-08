/*
 * Copyright (c) 2022, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Vector4.h>

namespace SoftGPU {

struct Material {
    Vector4<float> ambient { 0.2f, 0.2f, 0.2f, 1.0f };
    Vector4<float> diffuse { 0.8f, 0.8f, 0.8f, 1.0f };
    Vector4<float> specular { 0.0f, 0.0f, 0.0f, 1.0f };
    Vector4<float> emissive { 0.0f, 0.0f, 0.0f, 1.0f };
    float shininess { 0.0f };
    float specular_exponent { 0.0f };
    float ambient_color_index { 0.0f };
    float diffuse_color_index = { 1.0f };
    float specular_color_index = { 1.0f };
};

}
