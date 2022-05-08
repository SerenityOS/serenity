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
    Vector2<i32x4> screen_coordinates;
    Vector3<f32x4> barycentrics;
    f32x4 depth;
    Vector4<f32x4> vertex_color;
    Array<Vector4<f32x4>, GPU::NUM_SAMPLERS> texture_coordinates;
    Vector4<f32x4> out_color;
    f32x4 fog_depth;
    i32x4 mask;
    f32x4 coverage { expand4(1.f) };
};

}
