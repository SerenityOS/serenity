/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Symbols.h"

namespace Video::VP9 {

enum FrameType {
    KeyFrame,
    NonKeyFrame
};

enum ColorSpace : u8 {
    Unknown = 0,
    Bt601 = 1,
    Bt709 = 2,
    Smpte170 = 3,
    Smpte240 = 4,
    Bt2020 = 5,
    Reserved = 6,
    RGB = 7
};

enum ColorRange {
    StudioSwing,
    FullSwing
};

enum InterpolationFilter {
    EightTap = 0,
    EightTapSmooth = 1,
    EightTapSharp = 2,
    Bilinear = 3,
    Switchable = 4
};

enum ReferenceFrame {
    IntraFrame = 0,
    LastFrame = 1,
    GoldenFrame = 2,
    AltRefFrame = 3,
};

enum TXMode {
    Only4x4 = 0,
    Allow8x8 = 1,
    Allow16x16 = 2,
    Allow32x32 = 3,
    TXModeSelect = 4,
};

enum TXSize {
    TX4x4 = 0,
    TX8x8 = 1,
    TX16x16 = 2,
    TX32x32 = 3,
};

enum ReferenceMode {
    SingleReference = 0,
    CompoundReference = 1,
    ReferenceModeSelect = 2,
};

enum BlockSubsize : u8 {
    Block_4x4 = 0,
    Block_4x8 = 1,
    Block_8x4 = 2,
    Block_8x8 = 3,
    Block_8x16 = 4,
    Block_16x8 = 5,
    Block_16x16 = 6,
    Block_16x32 = 7,
    Block_32x16 = 8,
    Block_32x32 = 9,
    Block_32x64 = 10,
    Block_64x32 = 11,
    Block_64x64 = 12,
    Block_Invalid = BLOCK_INVALID
};

}
