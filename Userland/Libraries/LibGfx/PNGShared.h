/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Gfx::PNG {

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

};
