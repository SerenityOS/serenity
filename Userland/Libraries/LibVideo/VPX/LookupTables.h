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
static constexpr TXSize tx_mode_to_biggest_tx_size[TX_MODES] = { TX_4x4, TX_8x8, TX_16x16, TX_32x32, TX_32x32 };
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

static constexpr int partition_tree[6] = {
    -PARTITION_NONE, 2,
    -PARTITION_HORZ, 4,
    -PARTITION_VERT, -PARTITION_SPLIT
};
static constexpr int cols_partition_tree[2] = { -PARTITION_HORZ, -PARTITION_SPLIT };
static constexpr int rows_partition_tree[2] = { -PARTITION_VERT, -PARTITION_SPLIT };
static constexpr int intra_mode_tree[18] = {
    -DC_PRED, 2,
    -TM_PRED, 4,
    -V_PRED, 6,
    8, 12,
    -H_PRED, 10,
    -D135_PRED, -D117_PRED,
    -D45_PRED, 14,
    -D63_PRED, 16,
    -D153_PRED, -D207_PRED
};
static constexpr int segment_tree[14] = {
    2, 4, 6, 8, 10, 12,
    0, -1, -2, -3, -4, -5, -6, -7
};
static constexpr int binary_tree[2] = { 0, -1 };
static constexpr int tx_size_32_tree[6] = {
    -TX_4x4, 2,
    -TX_8x8, 4,
    -TX_16x16, -TX_32x32
};
static constexpr int tx_size_16_tree[4] = {
    -TX_4x4, 2,
    -TX_8x8, -TX_16x16
};
static constexpr int tx_size_8_tree[2] = { -TX_4x4, -TX_8x8 };
static constexpr int inter_mode_tree[6] = {
    -(ZERO_MV - NEAREST_MV), 2,
    -(NEAREST_MV - NEAREST_MV), 4,
    -(NEAR_MV - NEAREST_MV), -(NEW_MV - NEAREST_MV)
};
static constexpr int interp_filter_tree[4] = {
    -EIGHTTAP, 2,
    -EIGHTTAP_SMOOTH, -EIGHTTAP_SHARP
};
static constexpr int mv_joint_tree[6] = {
    -MV_JOINT_ZERO, 2,
    -MV_JOINT_HNZVZ, 4,
    -MV_JOINT_HZVNZ, -MV_JOINT_HNZVNZ
};
static constexpr int mv_class_tree[20] = {
    -MV_CLASS_0, 2,
    -MV_CLASS_1, 4,
    6, 8,
    -MV_CLASS_2, -MV_CLASS_3,
    10, 12,
    -MV_CLASS_4, -MV_CLASS_5,
    -MV_CLASS_6, 14,
    16, 18,
    -MV_CLASS_7, -MV_CLASS_8,
    -MV_CLASS_9, -MV_CLASS_10
};
static constexpr int mv_fr_tree[6] = {
    -0, 2,
    -1, 4,
    -2, -3
};
static constexpr int token_tree[20] = {
    -ZERO_TOKEN, 2,
    -ONE_TOKEN, 4,
    6, 10,
    -TWO_TOKEN, 8,
    -THREE_TOKEN, -FOUR_TOKEN,
    12, 14,
    -DCT_VAL_CAT1, -DCT_VAL_CAT2,
    16, 18,
    -DCT_VAL_CAT3, -DCT_VAL_CAT4,
    -DCT_VAL_CAT5, -DCT_VAL_CAT6
};

static constexpr u8 b_width_log2_lookup[BLOCK_SIZES] = { 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4 };
static constexpr u8 b_height_log2_lookup[BLOCK_SIZES] = {0, 1, 0, 1, 2, 1, 2, 3, 2, 3, 4, 3, 4 };
static constexpr u8 num_4x4_blocks_wide_lookup[BLOCK_SIZES] = {1, 1, 2, 2, 2, 4, 4, 4, 8, 8, 8, 16, 16 };
static constexpr u8 num_4x4_blocks_high_lookup[BLOCK_SIZES] = {1, 2, 1, 2, 4, 2, 4, 8, 4, 8, 16, 8, 16 };
static constexpr u8 mi_width_log2_lookup[BLOCK_SIZES] = {0, 0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3 };
static constexpr u8 num_8x8_blocks_wide_lookup[BLOCK_SIZES] = {1, 1, 1, 1, 1, 2, 2, 2, 4, 4, 4, 8, 8 };
static constexpr u8 mi_height_log2_lookup[BLOCK_SIZES] = {0, 0, 0, 0, 1, 0, 1, 2, 1, 2, 3, 2, 3 };
static constexpr u8 num_8x8_blocks_high_lookup[BLOCK_SIZES] = { 1, 1, 1, 1, 2, 1, 2, 4, 2, 4, 8, 4, 8 };
static constexpr u8 size_group_lookup[BLOCK_SIZES] = { 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3 };

}
