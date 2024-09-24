/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math.h>
#include <AK/SIMD.h>
#include <AK/SIMDExtras.h>
#include <math.h>

namespace AK::SIMD {

// Functions ending in "_int_range" only accept arguments within range [INT_MIN, INT_MAX].
// Other inputs will generate unexpected results.

ALWAYS_INLINE static f32x4 truncate_int_range(f32x4 v)
{
    return simd_cast<f32x4>(simd_cast<i32x4>(v));
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

template<SIMDVector T>
ALWAYS_INLINE T bitselect(T v1, T v2, T control_mask)
{
    return (v1 & control_mask) | (v2 & ~control_mask);
}

template<SIMDVector T>
requires(IsIntegral<ElementOf<T>>)
ALWAYS_INLINE T abs(T x)
{
    return bitselect(x, -x, x > 0);
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

ALWAYS_INLINE static f32x4 exp_approximate(f32x4 v)
{
    static constexpr int number_of_iterations = 10;
    auto result = 1.f + v / (1 << number_of_iterations);
    for (int i = 0; i < number_of_iterations; ++i)
        result *= result;
    return result;
}

ALWAYS_INLINE static f32x4 sqrt(f32x4 v)
{
#if ARCH(X86_64)
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

}
