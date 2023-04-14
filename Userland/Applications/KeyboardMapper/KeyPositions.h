/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>

struct KeyPosition {
    u32 kernel_map_entry_index;
    int x;
    int y;
    int width;
    int height;
    bool enabled;
    int map_index;
    ByteString name;
};

#define KEY_COUNT 63

#if defined(AK_COMPILER_CLANG)
#    pragma clang diagnostic push
#    pragma clang diagnostic ignored "-Wc99-designator"
#endif

struct KeyPosition keys[KEY_COUNT] = {
    // clang-format off
    [ 0] = {     0,   0,  0,   0,   0, false,  0, ""},

    [ 1] = {  0x29,   0,  0,  50,  50,  true, 41, "`"},
    [ 2] = {  0x02,  51,  0,  50,  50,  true,  2, "1"},
    [ 3] = {  0x03, 102,  0,  50,  50,  true,  3, "2"},
    [ 4] = {  0x04, 153,  0,  50,  50,  true,  4, "3"},
    [ 5] = {  0x05, 204,  0,  50,  50,  true,  5, "4"},
    [ 6] = {  0x06, 255,  0,  50,  50,  true,  6, "5"},
    [ 7] = {  0x07, 306,  0,  50,  50,  true,  7, "6"},
    [ 8] = {  0x08, 357,  0,  50,  50,  true,  8, "7"},
    [ 9] = {  0x09, 408,  0,  50,  50,  true,  9, "8"},
    [10] = {  0x0A, 459,  0,  50,  50,  true, 10, "9"},
    [11] = {  0x0B, 510,  0,  50,  50,  true, 11, "0"},
    [12] = {  0x0C, 561,  0,  50,  50,  true, 12, "-"},
    [13] = {  0x0D, 612,  0,  50,  50,  true, 13, "="},
    [14] = {  0x0E, 663,  0, 100,  50, false,  0, "back space"},

    [15] = {  0x0F,   0,  52,  76,  50, false,  0, "tab"},
    [16] = {  0x10,  77,  52,  50,  50,  true, 16, "q"},
    [17] = {  0x11, 128,  52,  50,  50,  true, 17, "w"},
    [18] = {  0x12, 179,  52,  50,  50,  true, 18, "e"},
    [19] = {  0x13, 230,  52,  50,  50,  true, 19, "r"},
    [20] = {  0x14, 281,  52,  50,  50,  true, 20, "t"},
    [21] = {  0x15, 332,  52,  50,  50,  true, 21, "y"},
    [22] = {  0x16, 383,  52,  50,  50,  true, 22, "u"},
    [23] = {  0x17, 434,  52,  50,  50,  true, 23, "ı"},
    [24] = {  0x18, 485,  52,  50,  50,  true, 24, "o"},
    [25] = {  0x19, 536,  52,  50,  50,  true, 25, "p"},
    [26] = {  0x1A, 587,  52,  50,  50,  true, 26, "["},
    [27] = {  0x1B, 638,  52,  50,  50,  true, 27, "]"},
    [28] = {  0x1C, 689,  52,  74,  50, false,  0, "enter"},

    [29] = {  0x3A,   0, 104, 101,  50, false,  0, "caps lock"},
    [30] = {  0x1E, 103, 104,  50,  50,  true, 30, "a"},
    [31] = {  0x1F, 154, 104,  50,  50,  true, 31, "s"},
    [32] = {  0x20, 205, 104,  50,  50,  true, 32, "d"},
    [33] = {  0x21, 256, 104,  50,  50,  true, 33, "f"},
    [34] = {  0x22, 307, 104,  50,  50,  true, 34, "g"},
    [35] = {  0x23, 358, 104,  50,  50,  true, 35, "h"},
    [36] = {  0x24, 409, 104,  50,  50,  true, 36, "j"},
    [37] = {  0x25, 460, 104,  50,  50,  true, 37, "k"},
    [38] = {  0x26, 511, 104,  50,  50,  true, 38, "l"},
    [39] = {  0x27, 562, 104,  50,  50,  true, 39, ";"},
    [40] = {  0x28, 614, 104,  50,  50,  true, 40, "\""},
    [41] = {  0x2B, 665, 104,  50,  50,  true, 43, "\\"},

    [42] = {  0x2A,   0, 156,  76,  50, false,  0, "left shift"},
    [43] = {  0x56,  77, 156,  50,  50,  true, 86, "\\"},
    [44] = {  0x2C, 128, 156,  50,  50,  true, 44, "z"},
    [45] = {  0x2D, 179, 156,  50,  50,  true, 45, "x"},
    [46] = {  0x2E, 230, 156,  50,  50,  true, 46, "c"},
    [47] = {  0x2F, 281, 156,  50,  50,  true, 47, "v"},
    [48] = {  0x30, 332, 156,  50,  50,  true, 48, "b"},
    [49] = {  0x31, 383, 156,  50,  50,  true, 49, "n"},
    [50] = {  0x32, 434, 156,  50,  50,  true, 50, "m"},
    [51] = {  0x33, 485, 156,  50,  50,  true, 51, ","},
    [52] = {  0x34, 536, 156,  50,  50,  true, 52, "."},
    [53] = {  0x35, 587, 156,  50,  50,  true, 53, "/"},
    [54] = {  0x36, 638, 156, 125,  50, false,  0, "right shift"},

    [55] = {  0x1D,   0, 208,  76,  50, false,  0, "left ctrl"},
    [56] = {0xE05B,  77, 208,  50,  50, false,  0, "left\nsuper"},
    [57] = {  0x38, 128, 208,  50,  50, false,  0, "alt"},
    [58] = {  0x39, 179, 208, 356,  50, false,  0, "space"},
    [59] = {0xE038, 536, 208,  50,  50, false,  0, "alt gr"},
    [60] = {0xE05C, 587, 208,  50,  50, false,  0, "right\nsuper"},
    [61] = {0xE05D, 638, 208,  50,  50, false,  0, "menu"},
    [62] = {0xE01D, 689, 208,  74,  50, false,  0, "right ctrl"}
    // clang-format on
};

#if defined(AK_COMPILER_CLANG)
#    pragma clang diagnostic pop
#endif
