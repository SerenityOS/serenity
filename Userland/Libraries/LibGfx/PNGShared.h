/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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
    int pa = abs(p - a);
    int pb = abs(p - b);
    int pc = abs(p - c);
    if (pa <= pb && pa <= pc)
        return a;
    if (pb <= pc)
        return b;
    return c;
}

};
