/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SIMD.h>
#include <AK/SIMDExtras.h>
#include <LibGfx/Vector2.h>
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Config.h>

namespace SoftGPU {

using AK::SIMD::expand4;
using AK::SIMD::f32x4;
using AK::SIMD::i32x4;

struct PixelQuad final {
    void set_input(int index, f32x4 value) { inputs[index] = value; }
    f32x4 get_input_float(int index) const { return inputs[index]; }

    void set_input(int index, Vector4<f32x4> const& value)
    {
        inputs[index] = value.x();
        inputs[index + 1] = value.y();
        inputs[index + 2] = value.z();
        inputs[index + 3] = value.w();
    }
    Vector4<f32x4> get_input_vector4(int index) const
    {
        return Vector4<f32x4>(
            inputs[index],
            inputs[index + 1],
            inputs[index + 2],
            inputs[index + 3]);
    }

    Vector2<i32x4> screen_coordinates;
    Vector3<f32x4> barycentrics;
    f32x4 depth;
    Array<f32x4, NUM_SHADER_INPUTS> inputs;
    Vector4<f32x4> out_color;
    f32x4 fog_depth;
    i32x4 mask;
    f32x4 coverage { expand4(1.f) };
};

}
