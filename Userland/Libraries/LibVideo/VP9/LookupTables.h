/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Enums.h"
#include "Symbols.h"

namespace Video::VP9 {

static constexpr InterpolationFilter literal_to_type[4] = { EightTapSmooth, EightTap, EightTapSharp, Bilinear };
static constexpr TXSize tx_mode_to_biggest_tx_size[TX_MODES] = { TX_4x4, TX_8x8, TX_16x16, TX_32x32, TX_32x32 };
static constexpr u8 segmentation_feature_bits[SEG_LVL_MAX] = { 8, 6, 2, 0 };
static constexpr bool segmentation_feature_signed[SEG_LVL_MAX] = { true, true, false, false };
static constexpr u8 inv_map_table[MAX_PROB] = {
    7, 20, 33, 46, 59, 72, 85, 98, 111, 124, 137, 150, 163, 176, 189, 202, 215, 228, 241, 254,
    1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 26, 27,
    28, 29, 30, 31, 32, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 47, 48, 49, 50, 51, 52,
    53, 54, 55, 56, 57, 58, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 73, 74, 75, 76, 77,
    78, 79, 80, 81, 82, 83, 84, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 99, 100, 101, 102,
    103, 104, 105, 106, 107, 108, 109, 110, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122,
    123, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 138, 139, 140, 141, 142, 143,
    144, 145, 146, 147, 148, 149, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 164,
    165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 177, 178, 179, 180, 181, 182, 183, 184,
    185, 186, 187, 188, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 203, 204, 205,
    206, 207, 208, 209, 210, 211, 212, 213, 214, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225,
    226, 227, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 242, 243, 244, 245, 246,
    247, 248, 249, 250, 251, 252, 253, 253
};
static constexpr BlockSubsize subsize_lookup[PARTITION_TYPES][BLOCK_SIZES] = {
    {
        // PARTITION_NONE
        Block_4x4,
        Block_4x8,
        Block_8x4,
        Block_8x8,
        Block_8x16,
        Block_16x8,
        Block_16x16,
        Block_16x32,
        Block_32x16,
        Block_32x32,
        Block_32x64,
        Block_64x32,
        Block_64x64,
    },
    {
        // PARTITION_HORZ
        Block_Invalid,
        Block_Invalid,
        Block_Invalid,
        Block_8x4,
        Block_Invalid,
        Block_Invalid,
        Block_16x8,
        Block_Invalid,
        Block_Invalid,
        Block_32x16,
        Block_Invalid,
        Block_Invalid,
        Block_64x32,
    },
    {
        // PARTITION_VERT
        Block_Invalid,
        Block_Invalid,
        Block_Invalid,
        Block_4x8,
        Block_Invalid,
        Block_Invalid,
        Block_8x16,
        Block_Invalid,
        Block_Invalid,
        Block_16x32,
        Block_Invalid,
        Block_Invalid,
        Block_32x64,
    },
    {
        // PARTITION_SPLIT
        Block_Invalid,
        Block_Invalid,
        Block_Invalid,
        Block_4x4,
        Block_Invalid,
        Block_Invalid,
        Block_8x8,
        Block_Invalid,
        Block_Invalid,
        Block_16x16,
        Block_Invalid,
        Block_Invalid,
        Block_32x32,
    }
};

static constexpr int partition_tree[6] = {
    -PartitionNone, 2,
    -PartitionHorizontal, 4,
    -PartitionVertical, -PartitionSplit
};
static constexpr int cols_partition_tree[2] = { -PartitionHorizontal, -PartitionSplit };
static constexpr int rows_partition_tree[2] = { -PartitionVertical, -PartitionSplit };
static constexpr int intra_mode_tree[18] = {
    -DcPred, 2,
    -TmPred, 4,
    -VPred, 6,
    8, 12,
    -HPred, 10,
    -D135Pred, -D117Pred,
    -D45Pred, 14,
    -D63Pred, 16,
    -D153Pred, -D207Pred
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
    -(ZeroMv - NearestMv), 2,
    -(NearestMv - NearestMv), 4,
    -(NearMv - NearestMv), -(NewMv - NearestMv)
};
static constexpr int interp_filter_tree[4] = {
    -EightTap, 2,
    -EightTapSmooth, -EightTapSharp
};
static constexpr int mv_joint_tree[6] = {
    -MvJointZero, 2,
    -MvJointHnzvz, 4,
    -MvJointHzvnz, -MvJointHnzvnz
};
static constexpr int mv_class_tree[20] = {
    -MvClass0, 2,
    -MvClass1, 4,
    6, 8,
    -MvClass2, -MvClass3,
    10, 12,
    -MvClass4, -MvClass5,
    -MvClass6, 14,
    16, 18,
    -MvClass7, -MvClass8,
    -MvClass9, -MvClass10
};
static constexpr int mv_fr_tree[6] = {
    -0, 2,
    -1, 4,
    -2, -3
};
static constexpr int token_tree[20] = {
    -ZeroToken, 2,
    -OneToken, 4,
    6, 10,
    -TwoToken, 8,
    -ThreeToken, -FourToken,
    12, 14,
    -DctValCat1, -DctValCat2,
    16, 18,
    -DctValCat3, -DctValCat4,
    -DctValCat5, -DctValCat6
};

static constexpr u8 b_width_log2_lookup[BLOCK_SIZES] = { 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4 };
static constexpr u8 b_height_log2_lookup[BLOCK_SIZES] = { 0, 1, 0, 1, 2, 1, 2, 3, 2, 3, 4, 3, 4 };
static constexpr u8 num_4x4_blocks_wide_lookup[BLOCK_SIZES] = { 1, 1, 2, 2, 2, 4, 4, 4, 8, 8, 8, 16, 16 };
static constexpr u8 num_4x4_blocks_high_lookup[BLOCK_SIZES] = { 1, 2, 1, 2, 4, 2, 4, 8, 4, 8, 16, 8, 16 };
static constexpr u8 mi_width_log2_lookup[BLOCK_SIZES] = { 0, 0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3 };
static constexpr u8 num_8x8_blocks_wide_lookup[BLOCK_SIZES] = { 1, 1, 1, 1, 1, 2, 2, 2, 4, 4, 4, 8, 8 };
static constexpr u8 mi_height_log2_lookup[BLOCK_SIZES] = { 0, 0, 0, 0, 1, 0, 1, 2, 1, 2, 3, 2, 3 };
static constexpr u8 num_8x8_blocks_high_lookup[BLOCK_SIZES] = { 1, 1, 1, 1, 2, 1, 2, 4, 2, 4, 8, 4, 8 };
static constexpr u8 size_group_lookup[BLOCK_SIZES] = { 0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3 };

static constexpr TXSize max_txsize_lookup[BLOCK_SIZES] = {
    TX_4x4,
    TX_4x4,
    TX_4x4,
    TX_8x8,
    TX_8x8,
    TX_8x8,
    TX_16x16,
    TX_16x16,
    TX_16x16,
    TX_32x32,
    TX_32x32,
    TX_32x32,
    TX_32x32,
};

static constexpr BlockSubsize ss_size_lookup[BLOCK_SIZES][2][2] = {
    { { Block_4x4, Block_Invalid }, { Block_Invalid, Block_Invalid } },
    { { Block_4x8, Block_4x4 }, { Block_Invalid, Block_Invalid } },
    { { Block_8x4, Block_Invalid }, { Block_4x4, Block_Invalid } },
    { { Block_8x8, Block_8x4 }, { Block_4x8, Block_4x4 } },
    { { Block_8x16, Block_8x8 }, { Block_Invalid, Block_4x8 } },
    { { Block_16x8, Block_Invalid }, { Block_8x8, Block_8x4 } },
    { { Block_16x16, Block_16x8 }, { Block_8x16, Block_8x8 } },
    { { Block_16x32, Block_16x16 }, { Block_Invalid, Block_8x16 } },
    { { Block_32x16, Block_Invalid }, { Block_16x16, Block_16x8 } },
    { { Block_32x32, Block_32x16 }, { Block_16x32, Block_16x16 } },
    { { Block_32x64, Block_32x32 }, { Block_Invalid, Block_16x32 } },
    { { Block_64x32, Block_Invalid }, { Block_32x32, Block_32x16 } },
    { { Block_64x64, Block_64x32 }, { Block_32x64, Block_32x32 } },
};

}
