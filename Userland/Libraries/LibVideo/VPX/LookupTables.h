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

#include "Enums.h"

namespace Video {

static constexpr InterpolationFilter literal_to_type[4] = { EIGHTTAP_SMOOTH, EIGHTTAP, EIGHTTAP_SHARP, BILINEAR };
static constexpr TXSize tx_mode_to_biggest_tx_size[TX_MODES] = { TX4x4, TX8x8, TX16x16, TX32x32, TX32x32 };
static constexpr u8 num_8x8_blocks_wide_lookup[BLOCK_SIZES] = { 1, 1, 1, 1, 1, 2, 2, 2, 4, 4, 4, 8, 8 };
static constexpr BlockSubsize subsize_lookup[PARTITION_TYPES][BLOCK_SIZES] = {
    {  // PARTITION_NONE
        Block_4x4,   Block_4x8,   Block_8x4,
        Block_8x8,   Block_8x16,  Block_16x8,
        Block_16x16, Block_16x32, Block_32x16,
        Block_32x32, Block_32x64, Block_64x32,
        Block_64x64,
    }, {  // PARTITION_HORZ
        Block_Invalid, Block_Invalid, Block_Invalid,
        Block_8x4,     Block_Invalid, Block_Invalid,
        Block_16x8,    Block_Invalid, Block_Invalid,
        Block_32x16,   Block_Invalid, Block_Invalid,
        Block_64x32,
    }, {  // PARTITION_VERT
        Block_Invalid, Block_Invalid, Block_Invalid,
        Block_4x8,     Block_Invalid, Block_Invalid,
        Block_8x16,    Block_Invalid, Block_Invalid,
        Block_16x32,   Block_Invalid, Block_Invalid,
        Block_32x64,
    }, {  // PARTITION_SPLIT
        Block_Invalid, Block_Invalid, Block_Invalid,
        Block_4x4,     Block_Invalid, Block_Invalid,
        Block_8x8,     Block_Invalid, Block_Invalid,
        Block_16x16,   Block_Invalid, Block_Invalid,
        Block_32x32,
    }
};

}
