/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Spice {

enum class ImageType {
    Bitmap,
    Quic,
    Reserved,
    LzPLT = 100,
    LzRGB,
    GLzRGB,
    FromCache,
    Surface,
    JPEG,
    FromCacheLossless,
    ZlibGLzRGB,
    JPEGAlpha
};

enum class BitmapFormat {
    FmtInvalid,
    Fmt1BitLE,
    Fmt1BitBE,
    Fmt4BitLE,
    Fmt4BitBE,
    Fmt8Bit,
    Fmt16Bit,
    Fmt24Bit,
    Fmt32Bit,
    FmtRGBA
};

enum class SurfaceFormat {
    FmtInvalid,
    Fmt1A,
    Fmt8A = 8,
    Fmt16_555 = 16,
    Fmt32_xRGB = 32,
    Fmt16_565 = 80,
    Fmt32_ARGB = 96
};

enum class ClipType {
    None,
    Rects
};

}
