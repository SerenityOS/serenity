/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
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

#include "Constants.h"

namespace Video {

enum FrameType {
    KEY_FRAME,
    NON_KEY_FRAME
};

enum ColorSpace : u8 {
    CS_UNKNOWN = 0,
    CS_BT_601 = 1,
    CS_BT_709 = 2,
    CS_SMPTE_170 = 3,
    CS_SMPTE_240 = 4,
    CS_BT_2020 = 5,
    CS_RESERVED = 6,
    CS_RGB = 7
};

enum ColorRange {
    STUDIO_SWING,
    FULL_SWING
};

enum InterpolationFilter {
    EIGHTTAP = 0,
    EIGHTTAP_SMOOTH = 1,
    EIGHTTAP_SHARP = 2,
    BILINEAR = 3,
    SWITCHABLE = 4
};

enum ReferenceFrame {
    INTRA_FRAME = 0,
    LAST_FRAME = 1,
    GOLDEN_FRAME = 2,
    ALTREF_FRAME = 3,
    MAX_REF_FRAMES = 4
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
