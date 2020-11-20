/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "Color.h"
#include <math.h>
#include <xmmintrin.h>

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

#ifndef NO_FPU

#    ifdef __SSE__

// A vector of 4 floats, aligned for SSE instructions
typedef float v4sf __attribute__((vector_size(16)));

// Transform v4sf from gamma2.2 space to linear space
// Assumes x is in range [0, 1]
constexpr v4sf gamma_to_linear4(v4sf x)
{
    return (0.8f + 0.2f * x) * x * x;
}

inline v4sf linear_to_gamma4(v4sf x)
{
    // Source for approximation: https://mimosa-pudica.net/fast-gamma/
    constexpr float a = 0.00279491f;
    constexpr float b = 1.15907984f;
    float c = (b / sqrt(1 + a)) - 1;
    return ((b * __builtin_ia32_rsqrtps(x + a)) - c) * x;
}

// Linearize v1 and v2, lerp them by mix factor, then convert back.
// The output is entirely v1 when mix = 0 and entirely v2 when mix = 1
inline v4sf gamma_accurate_lerp4(v4sf v1, v4sf v2, float mix)
{
    return linear_to_gamma4(gamma_to_linear4(v1) * (1 - mix) + gamma_to_linear4(v2) * mix);
}

// Convert a and b to linear space, blend them by mix factor, then convert back using sse1.
// The output is entirely a when mix = 0 and entirely b when mix = 1
inline Color gamma_accurate_blend4(Color a, Color b, float mix)
{
    v4sf ac = {
        (float)a.red(),
        (float)a.green(),
        (float)a.blue(),
        0.f,
    };
    v4sf bc = {
        (float)b.red(),
        (float)b.green(),
        (float)b.blue(),
        0.f,
    };
    v4sf out = 255.f * gamma_accurate_lerp4(ac / 255.f, bc / 255.f, mix);
    return Color(out[0], out[1], out[2]);
}

#    endif

// Transform scalar from gamma2.2 space to linear space
// Assumes x is in range [0, 1]
constexpr float gamma_to_linear(float x)
{
#    ifdef ACCURATE_GAMMA_ADJUSTMENT
    // Slower, but more accurate
    return pow(x, GAMMA);
#    else
    return (0.8 + 0.2 * x) * x * x;
#    endif
}

// Transform scalar from linear space to gamma2.2 space
// Assumes x is in range [0, 1]
inline float linear_to_gamma(float x)
{
#    ifdef ACCURATE_GAMMA_ADJUSTMENT
    // Slower, but more accurate
    return pow(x, 1. / GAMMA);
#    else
    // Source for approximation: https://mimosa-pudica.net/fast-gamma/
    constexpr float a = 0.00279491;
    constexpr float b = 1.15907984;
    float c = (b / sqrt(1 + a)) - 1;
    return ((b / __builtin_sqrt(x + a)) - c) * x;
#    endif
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
#    ifdef __SSE__
    return gamma_accurate_blend4(a, b, mix);
#    else
    return {
        static_cast<u8>(255. * gamma_accurate_lerp(a.red() / 255., b.red() / 255., mix)),
        static_cast<u8>(255. * gamma_accurate_lerp(a.green() / 255., b.green() / 255., mix)),
        static_cast<u8>(255. * gamma_accurate_lerp(a.blue() / 255., b.blue() / 255., mix)),
    };
#    endif
}

#endif

}
