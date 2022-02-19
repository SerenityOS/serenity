/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SIMDExtras.h>
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

}
