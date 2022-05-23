/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifndef __SSE__
#    include <AK/Math.h>
#endif
#include <AK/SIMD.h>
#include <AK/SIMDExtras.h>
#include <math.h>

// Functions returning vectors or accepting vector arguments have different calling conventions
// depending on whether the target architecture supports SSE or not. GCC generates warning "psabi"
// when compiling for non-SSE architectures. We disable this warning because these functions
// are static and should never be visible from outside the translation unit that includes this header.
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wpsabi"

namespace AK::SIMD {

// Functions ending in "_int_range" only accept arguments within range [INT_MIN, INT_MAX].
// Other inputs will generate unexpected results.

ALWAYS_INLINE static f32x4 truncate_int_range(f32x4 v)
{
    return to_f32x4(to_i32x4(v));
}

ALWAYS_INLINE static f32x4 floor_int_range(f32x4 v)
{
    auto t = truncate_int_range(v);
    return t > v ? t - 1.0f : t;
}

ALWAYS_INLINE static f32x4 ceil_int_range(f32x4 v)
{
    auto t = truncate_int_range(v);
    return t < v ? t + 1.0f : t;
}

ALWAYS_INLINE static f32x4 frac_int_range(f32x4 v)
{
    return v - floor_int_range(v);
}

ALWAYS_INLINE static f32x4 clamp(f32x4 v, f32x4 min, f32x4 max)
{
    return v < min ? min : (v > max ? max : v);
}

ALWAYS_INLINE static f32x4 clamp(f32x4 v, float min, float max)
{
    return v < min ? min : (v > max ? max : v);
}

ALWAYS_INLINE static f32x4 exp(f32x4 v)
{
    // FIXME: This should be replaced with a vectorized algorithm instead of calling the scalar expf 4 times
    return f32x4 {
        expf(v[0]),
        expf(v[1]),
        expf(v[2]),
        expf(v[3]),
    };
}

ALWAYS_INLINE static f32x4 sqrt(f32x4 v)
{
#ifdef __SSE__
    return __builtin_ia32_sqrtps(v);
#else
    return f32x4 {
        AK::sqrt(v[0]),
        AK::sqrt(v[1]),
        AK::sqrt(v[2]),
        AK::sqrt(v[3]),
    };
#endif
}

ALWAYS_INLINE static f32x4 rsqrt(f32x4 v)
{
#ifdef __SSE__
    return __builtin_ia32_rsqrtps(v);
#else
    return f32x4 {
        1.f / AK::sqrt(v[0]),
        1.f / AK::sqrt(v[1]),
        1.f / AK::sqrt(v[2]),
        1.f / AK::sqrt(v[3]),
    };
#endif
}

}

#pragma GCC diagnostic pop
