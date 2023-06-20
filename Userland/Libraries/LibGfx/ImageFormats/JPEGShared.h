/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// These names are defined in B.1.1.3 - Marker assignments

#define JPEG_APPN0 0XFFE0
#define JPEG_APPN1 0XFFE1
#define JPEG_APPN2 0XFFE2
#define JPEG_APPN3 0XFFE3
#define JPEG_APPN4 0XFFE4
#define JPEG_APPN5 0XFFE5
#define JPEG_APPN6 0XFFE6
#define JPEG_APPN7 0XFFE7
#define JPEG_APPN8 0XFFE8
#define JPEG_APPN9 0XFFE9
#define JPEG_APPN10 0XFFEA
#define JPEG_APPN11 0XFFEB
#define JPEG_APPN12 0XFFEC
#define JPEG_APPN13 0XFFED
#define JPEG_APPN14 0xFFEE
#define JPEG_APPN15 0xFFEF

#define JPEG_RESERVED1 0xFFF1
#define JPEG_RESERVED2 0xFFF2
#define JPEG_RESERVED3 0xFFF3
#define JPEG_RESERVED4 0xFFF4
#define JPEG_RESERVED5 0xFFF5
#define JPEG_RESERVED6 0xFFF6
#define JPEG_RESERVED7 0xFFF7
#define JPEG_RESERVED8 0xFFF8
#define JPEG_RESERVED9 0xFFF9
#define JPEG_RESERVEDA 0xFFFA
#define JPEG_RESERVEDB 0xFFFB
#define JPEG_RESERVEDC 0xFFFC
#define JPEG_RESERVEDD 0xFFFD

#define JPEG_RST0 0xFFD0
#define JPEG_RST1 0xFFD1
#define JPEG_RST2 0xFFD2
#define JPEG_RST3 0xFFD3
#define JPEG_RST4 0xFFD4
#define JPEG_RST5 0xFFD5
#define JPEG_RST6 0xFFD6
#define JPEG_RST7 0xFFD7

#define JPEG_ZRL 0xF0

#define JPEG_DHP 0xFFDE
#define JPEG_EXP 0xFFDF

#define JPEG_DAC 0XFFCC
#define JPEG_DHT 0XFFC4
#define JPEG_DQT 0XFFDB
#define JPEG_EOI 0xFFD9
#define JPEG_DRI 0XFFDD
#define JPEG_SOF0 0XFFC0
#define JPEG_SOF1 0XFFC1
#define JPEG_SOF2 0xFFC2
#define JPEG_SOF15 0xFFCF
#define JPEG_SOI 0XFFD8
#define JPEG_SOS 0XFFDA
#define JPEG_COM 0xFFFE

namespace Gfx {

using Marker = u16;

constexpr static u8 zigzag_map[64] {
    0, 1, 8, 16, 9, 2, 3, 10,
    17, 24, 32, 25, 18, 11, 4, 5,
    12, 19, 26, 33, 40, 48, 41, 34,
    27, 20, 13, 6, 7, 14, 21, 28,
    35, 42, 49, 56, 57, 50, 43, 36,
    29, 22, 15, 23, 30, 37, 44, 51,
    58, 59, 52, 45, 38, 31, 39, 46,
    53, 60, 61, 54, 47, 55, 62, 63
};

/**
 * MCU means group of data units that are coded together. A data unit is an 8x8
 * block of component data. In interleaved scans, number of non-interleaved data
 * units of a component C is Ch * Cv, where Ch and Cv represent the horizontal &
 * vertical subsampling factors of the component, respectively. A MacroBlock is
 * an 8x8 block of RGB values before encoding, and 8x8 block of YCbCr values when
 * we're done decoding the huffman stream.
 */
struct Macroblock {
    union {
        i16 y[64] = { 0 };
        i16 r[64];
    };

    union {
        i16 cb[64] = { 0 };
        i16 g[64];
    };

    union {
        i16 cr[64] = { 0 };
        i16 b[64];
    };

    i16 k[64] = { 0 };
};

}
