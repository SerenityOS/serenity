/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SIMD.h>
#include <LibGfx/Vector2.h>
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Config.h>

namespace SoftGPU {

struct PixelQuad final {
    Vector2<AK::SIMD::i32x4> screen_coordinates;
    Vector3<AK::SIMD::f32x4> barycentrics;
    AK::SIMD::f32x4 depth;
    Vector4<AK::SIMD::f32x4> vertex_color;
    Array<Vector4<AK::SIMD::f32x4>, NUM_SAMPLERS> texture_coordinates;
    Vector4<AK::SIMD::f32x4> out_color;
    AK::SIMD::f32x4 fog_depth;
    AK::SIMD::i32x4 mask;
};

}
