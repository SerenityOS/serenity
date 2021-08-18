/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Color.h"
#include <AK/Math.h>
#include <xmmintrin.h>

#include <AK/SIMD.h>

#define GAMMA 2.2

// Most computer graphics are stored in the sRGB color space, which stores something close to
// the square root of the display intensity of each color channel. This is problematic for most
// operations that we want to perform on colors, since they typically assume that color scales
// linearly (e.g. rgb(127, 0, 0) is half as bright as rgb(255, 0, 0)). This causes incorrect
// results that look more gray than they should, to fix this we have to convert colors to the linear
// color space before performing these operations, then convert back before displaying.
//
// Conversion between linear and sRGB spaces are somewhat expensive to do on the CPU, so we instead
// interpret sRGB colors as gamma2.2 colors, which are close enough in most cases to be indistinguishable.
// Gamma 2.2 colors follow the simple rule of `display_intensity = pow(stored_intensity, 2.2)`.
// This module implements some fast color space transforms between the gamma2.2 and linear color spaces, plus
// some common primitive operations like blending.
//
// For a more in-depth overview of how gamma-adjustment works, check out:
// https://blog.johnnovak.net/2016/09/21/what-every-coder-should-know-about-gamma/

namespace Gfx {

using AK::SIMD::f32x4;

#ifdef __SSE__

// Transform f32x4 from gamma2.2 space to linear space
// Assumes x is in range [0, 1]
// FIXME: Remove this hack once clang-11 is available as the default in Github Actions.
//        This is apparently sometime mid-December. https://github.com/actions/virtual-environments/issues/2130
#    if !defined(__clang__) || __clang_major__ >= 11
constexpr f32x4 gamma_to_linear4(f32x4 x)
#    else
inline f32x4 gamma_to_linear4(f32x4 x)
#    endif
{
    return (0.8f + 0.2f * x) * x * x;
}

// Transform f32x4 from linear space to gamma2.2 space
// Assumes x is in range [0, 1]
inline f32x4 linear_to_gamma4(f32x4 x)
{
    // Source for approximation: https://mimosa-pudica.net/fast-gamma/
    constexpr float a = 0.00279491f;
    constexpr float b = 1.15907984f;
    float c = (b / AK::sqrt(1.0f + a)) - 1;
    return ((b * __builtin_ia32_rsqrtps(x + a)) - c) * x;
}

// Linearize v1 and v2, lerp them by mix factor, then convert back.
// The output is entirely v1 when mix = 0 and entirely v2 when mix = 1
inline f32x4 gamma_accurate_lerp4(f32x4 v1, f32x4 v2, float mix)
{
    return linear_to_gamma4(gamma_to_linear4(v1) * (1 - mix) + gamma_to_linear4(v2) * mix);
}

#endif

// Transform scalar from gamma2.2 space to linear space
// Assumes x is in range [0, 1]
constexpr float gamma_to_linear(float x)
{
    return (0.8f + 0.2f * x) * x * x;
}

// Transform scalar from linear space to gamma2.2 space
// Assumes x is in range [0, 1]
inline float linear_to_gamma(float x)
{
    // Source for approximation: https://mimosa-pudica.net/fast-gamma/
    constexpr float a = 0.00279491;
    constexpr float b = 1.15907984;
    float c = (b / AK::sqrt(1 + a)) - 1;
    return ((b / AK::sqrt(x + a)) - c) * x;
}

// Linearize v1 and v2, lerp them by mix factor, then convert back.
// The output is entirely v1 when mix = 0 and entirely v2 when mix = 1
inline float gamma_accurate_lerp(float v1, float v2, float mix)
{
    return linear_to_gamma(gamma_to_linear(v1) * (1 - mix) + gamma_to_linear(v2) * mix);
}

// Convert a and b to linear space, blend them by mix factor, then convert back.
// The output is entirely a when mix = 0 and entirely b when mix = 1
inline Color gamma_accurate_blend(Color a, Color b, float mix)
{
#ifdef __SSE__
    f32x4 ac = {
        (float)a.red(),
        (float)a.green(),
        (float)a.blue(),
    };
    f32x4 bc = {
        (float)b.red(),
        (float)b.green(),
        (float)b.blue(),
    };
    f32x4 out = 255.f * gamma_accurate_lerp4(ac * (1.f / 255.f), bc * (1.f / 255.f), mix);
    return Color(out[0], out[1], out[2]);
#else
    return {
        static_cast<u8>(255.f * gamma_accurate_lerp(a.red() / 255.f, b.red() / 255.f, mix)),
        static_cast<u8>(255.f * gamma_accurate_lerp(a.green() / 255.f, b.green() / 255.f, mix)),
        static_cast<u8>(255.f * gamma_accurate_lerp(a.blue() / 255.f, b.blue() / 255.f, mix)),
    };
#endif
}

}
