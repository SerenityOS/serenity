/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SIMDExtras.h>
#include <AK/SIMDMath.h>
#include <LibGfx/Vector2.h>
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>

namespace SoftGPU {

ALWAYS_INLINE static constexpr Vector2<AK::SIMD::f32x4> expand4(Vector2<float> const& v)
{
    return Vector2<AK::SIMD::f32x4> {
        AK::SIMD::expand4(v.x()),
        AK::SIMD::expand4(v.y()),
    };
}

ALWAYS_INLINE static constexpr Vector3<AK::SIMD::f32x4> expand4(Vector3<float> const& v)
{
    return Vector3<AK::SIMD::f32x4> {
        AK::SIMD::expand4(v.x()),
        AK::SIMD::expand4(v.y()),
        AK::SIMD::expand4(v.z()),
    };
}

ALWAYS_INLINE static constexpr Vector4<AK::SIMD::f32x4> expand4(Vector4<float> const& v)
{
    return Vector4<AK::SIMD::f32x4> {
        AK::SIMD::expand4(v.x()),
        AK::SIMD::expand4(v.y()),
        AK::SIMD::expand4(v.z()),
        AK::SIMD::expand4(v.w()),
    };
}

ALWAYS_INLINE static constexpr Vector2<AK::SIMD::i32x4> expand4(Vector2<int> const& v)
{
    return Vector2<AK::SIMD::i32x4> {
        AK::SIMD::expand4(v.x()),
        AK::SIMD::expand4(v.y()),
    };
}

ALWAYS_INLINE static constexpr Vector3<AK::SIMD::i32x4> expand4(Vector3<int> const& v)
{
    return Vector3<AK::SIMD::i32x4> {
        AK::SIMD::expand4(v.x()),
        AK::SIMD::expand4(v.y()),
        AK::SIMD::expand4(v.z()),
    };
}

ALWAYS_INLINE static constexpr Vector4<AK::SIMD::i32x4> expand4(Vector4<int> const& v)
{
    return Vector4<AK::SIMD::i32x4> {
        AK::SIMD::expand4(v.x()),
        AK::SIMD::expand4(v.y()),
        AK::SIMD::expand4(v.z()),
        AK::SIMD::expand4(v.w()),
    };
}

ALWAYS_INLINE static AK::SIMD::f32x4 ddx(AK::SIMD::f32x4 v)
{
    return AK::SIMD::f32x4 {
        v[1] - v[0],
        v[1] - v[0],
        v[3] - v[2],
        v[3] - v[2],
    };
}

ALWAYS_INLINE static AK::SIMD::f32x4 ddy(AK::SIMD::f32x4 v)
{
    return AK::SIMD::f32x4 {
        v[2] - v[0],
        v[3] - v[1],
        v[2] - v[0],
        v[3] - v[1],
    };
}

ALWAYS_INLINE static Vector2<AK::SIMD::f32x4> ddx(Vector2<AK::SIMD::f32x4> const& v)
{
    return {
        ddx(v.x()),
        ddx(v.y()),
    };
}

ALWAYS_INLINE static Vector2<AK::SIMD::f32x4> ddy(Vector2<AK::SIMD::f32x4> const& v)
{
    return {
        ddy(v.x()),
        ddy(v.y()),
    };
}

ALWAYS_INLINE static AK::SIMD::f32x4 length(Vector2<AK::SIMD::f32x4> const& v)
{
    return AK::SIMD::sqrt(v.dot(v));
}

// Calculates a quadratic approximation of log2, exploiting the fact that IEEE754 floats are represented as mantissa * 2^exponent.
// See https://stackoverflow.com/questions/9411823/fast-log2float-x-implementation-c
ALWAYS_INLINE static AK::SIMD::f32x4 log2_approximate(AK::SIMD::f32x4 v)
{
    union {
        AK::SIMD::f32x4 float_val;
        AK::SIMD::i32x4 int_val;
    } u { v };

    // Extract just the exponent minus 1, giving a lower integral bound for log2.
    auto log = AK::SIMD::simd_cast<AK::SIMD::f32x4>(((u.int_val >> 23) & 255) - 128);

    // Replace the exponent with 0, giving a value between 1 and 2.
    u.int_val &= ~(255 << 23);
    u.int_val |= 127 << 23;

    // Approximate log2 by adding a quadratic function of u to the integral part.
    log += (-0.34484843f * u.float_val + 2.02466578f) * u.float_val - 0.67487759f;
    return log;
}

ALWAYS_INLINE static Vector2<AK::SIMD::f32x4> to_vec2_f32x4(Vector2<AK::SIMD::i32x4> const& v)
{
    return {
        AK::SIMD::simd_cast<AK::SIMD::f32x4>(v.x()),
        AK::SIMD::simd_cast<AK::SIMD::f32x4>(v.y()),
    };
}

ALWAYS_INLINE static constexpr Vector4<AK::SIMD::f32x4> to_vec4(AK::SIMD::f32x4 v)
{
    return { v, v, v, v };
}

}
