/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/SIMD.h>

namespace Gfx::PNG {

// https://www.w3.org/TR/PNG/#5PNG-file-signature
static constexpr Array<u8, 8> header = { 0x89, 'P', 'N', 'G', 13, 10, 26, 10 };

// https://www.w3.org/TR/PNG/#6Colour-values
enum class ColorType : u8 {
    Greyscale = 0,
    Truecolor = 2, // RGB
    IndexedColor = 3,
    GreyscaleWithAlpha = 4,
    TruecolorWithAlpha = 6,
};

// https://www.w3.org/TR/PNG/#9Filter-types
enum class FilterType : u8 {
    None,
    Sub,
    Up,
    Average,
    Paeth,
};

// https://www.w3.org/TR/PNG/#9Filter-type-4-Paeth
ALWAYS_INLINE u8 paeth_predictor(u8 a, u8 b, u8 c)
{
    int p = a + b - c;
    int pa = AK::abs(p - a);
    int pb = AK::abs(p - b);
    int pc = AK::abs(p - c);
    if (pa <= pb && pa <= pc)
        return a;
    if (pb <= pc)
        return b;
    return c;
}

ALWAYS_INLINE AK::SIMD::u8x4 paeth_predictor(AK::SIMD::u8x4 a, AK::SIMD::u8x4 b, AK::SIMD::u8x4 c)
{
    return AK::SIMD::u8x4 {
        paeth_predictor(a[0], b[0], c[0]),
        paeth_predictor(a[1], b[1], c[1]),
        paeth_predictor(a[2], b[2], c[2]),
        paeth_predictor(a[3], b[3], c[3]),
    };
}

};
