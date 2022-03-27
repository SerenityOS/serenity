/*
 * Copyright (c) 2022, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>

namespace GPU {

struct Light {
    bool is_enabled { false };

    // According to the OpenGL 1.5 specification, page 56, all of the parameters
    // for the following data members (positions, colors, and reals) are all
    // floating point.
    Vector4<float> ambient_intensity { 0.0f, 0.0f, 0.0f, 1.0f };
    Vector4<float> diffuse_intensity { 0.0f, 0.0f, 0.0f, 1.0f };
    Vector4<float> specular_intensity { 0.0f, 0.0f, 0.0f, 1.0f };
    Vector4<float> position { 0.0f, 0.0f, 1.0f, 0.0f };
    Vector3<float> spotlight_direction { 0.0f, 0.0f, -1.0f };

    float spotlight_exponent { 0.0f };
    float spotlight_cutoff_angle { 180.0f };
    float constant_attenuation { 1.0f };  // This is referred to `k0i` in the OpenGL spec
    float linear_attenuation { 0.0f };    // This is referred to `k1i` in the OpenGL spec
    float quadratic_attenuation { 0.0f }; // This is referred to `k2i` in the OpenGL spec
};

}
