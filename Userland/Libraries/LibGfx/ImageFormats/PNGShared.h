/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Error.h>
#include <AK/SIMD.h>
#include <AK/SIMDMath.h>

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

inline ErrorOr<FilterType> filter_type(u8 byte)
{
    if (byte <= 4)
        return static_cast<FilterType>(byte);
    return Error::from_string_literal("PNGImageDecoderPlugin: Invalid PNG filter");
}

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
    using namespace AK::SIMD;
    auto a16 = simd_cast<i16x4>(a);
    auto b16 = simd_cast<i16x4>(b);
    auto c16 = simd_cast<i16x4>(c);

    auto p16 = a16 + b16 - c16;
    auto pa16 = abs(p16 - a16);
    auto pb16 = abs(p16 - b16);
    auto pc16 = abs(p16 - c16);

    auto mask_a = simd_cast<u8x4>((pa16 <= pb16) & (pa16 <= pc16));
    auto mask_b = ~mask_a & simd_cast<u8x4>(pb16 <= pc16);
    auto mask_c = ~(mask_a | mask_b);

    return (a & mask_a) | (b & mask_b) | (c & mask_c);
}

};
