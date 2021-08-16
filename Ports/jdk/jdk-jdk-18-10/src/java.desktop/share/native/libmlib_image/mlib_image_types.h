/*
 * Copyright (c) 1997, 2003, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


#ifndef MLIB_IMAGE_TYPES_H
#define MLIB_IMAGE_TYPES_H

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
  MLIB_BIT    = 0,      /* 1-bit data                   */
  MLIB_BYTE   = 1,      /* 8-bit unsigned integer data  */
  MLIB_SHORT  = 2,      /* 16-bit signed integer data   */
  MLIB_INT    = 3,      /* 32-bit signed integer data   */
  MLIB_FLOAT  = 4,      /* 32-bit floating-point data   */
  MLIB_DOUBLE = 5,      /* 64-bit floating-point data   */
  MLIB_USHORT = 6       /* 16-bit unsigned integer data */
} mlib_type;

typedef enum {
  MLIB_NEAREST  = 0,    /* nearest neighbor filter      */
  MLIB_BILINEAR = 1,    /* bilinear filter              */
  MLIB_BICUBIC  = 2,    /* bicubic filter               */
  MLIB_BICUBIC2 = 3     /* bicubic2 filter              */
} mlib_filter;

typedef enum {
  MLIB_EDGE_DST_NO_WRITE      = 0,      /* no write to dst edge */
  MLIB_EDGE_DST_FILL_ZERO     = 1,      /* set dst edge to zero */
  MLIB_EDGE_DST_COPY_SRC      = 2,      /* copy src edge to dst edge */
  MLIB_EDGE_OP_NEAREST        = 3,      /* use nearest neighbor interpolation
                                           for edge pixels */
  MLIB_EDGE_OP_DEGRADED       = 4,      /* use degraded interpolation for
                                           edge pixels, i.e., bicubic ->
                                           bilinear -> nearest neighbor */
  MLIB_EDGE_SRC_EXTEND        = 5,      /* extend src edge by replication */
  MLIB_EDGE_SRC_EXTEND_ZERO   = 6,      /* extend src edge with zeros */
  MLIB_EDGE_SRC_EXTEND_MIRROR = 7,      /* extend src edge with mirrored data */
  MLIB_EDGE_SRC_PADDED        = 8       /* use borders specified in mlib_image structure */
} mlib_edge;

typedef enum {
  MLIB_BLEND_ZERO                = 0,
  MLIB_BLEND_ONE                 = 1,
  MLIB_BLEND_DST_COLOR           = 2,
  MLIB_BLEND_SRC_COLOR           = 3,
  MLIB_BLEND_ONE_MINUS_DST_COLOR = 4,
  MLIB_BLEND_ONE_MINUS_SRC_COLOR = 5,
  MLIB_BLEND_DST_ALPHA           = 6,
  MLIB_BLEND_SRC_ALPHA           = 7,
  MLIB_BLEND_ONE_MINUS_DST_ALPHA = 8,
  MLIB_BLEND_ONE_MINUS_SRC_ALPHA = 9,
  MLIB_BLEND_SRC_ALPHA_SATURATE  = 10
} mlib_blend;

typedef enum {
  MLIB_DFT_SCALE_NONE     = 0,  /* forward transform without scaling */
  MLIB_DFT_SCALE_MXN      = 1,  /* forward transform with scaling of
                                   1/(M*N) */
  MLIB_DFT_SCALE_SQRT     = 2,  /* forward transform with scaling of
                                   1/sqrt(M*N) */
  MLIB_IDFT_SCALE_NONE    = 3,  /* inverse transform without scaling */
  MLIB_IDFT_SCALE_MXN     = 4,  /* inverse transform with scaling of
                                   1/(M*N) */
  MLIB_IDFT_SCALE_SQRT    = 5   /* inverse transform with scaling of
                                   1/sqrt(M*N) */
} mlib_fourier_mode;

typedef enum {
  MLIB_MEDIAN_MASK_RECT             = 0, /* Rectangle shaped mask */
  MLIB_MEDIAN_MASK_PLUS             = 1, /* Plus shaped mask */
  MLIB_MEDIAN_MASK_X                = 2, /* X shaped mask */
  MLIB_MEDIAN_MASK_RECT_SEPARABLE   = 3  /* Separable rectangle mask */
} mlib_median_mask;

typedef enum { /* constants used for pixel format */
  MLIB_FORMAT_UNKNOWN         =  0,
  MLIB_FORMAT_INDEXED         =  1,
  MLIB_FORMAT_GRAYSCALE       =  2,
  MLIB_FORMAT_RGB             =  3,
  MLIB_FORMAT_BGR             =  4,
  MLIB_FORMAT_ARGB            =  5,
  MLIB_FORMAT_ABGR            =  6,
  MLIB_FORMAT_PACKED_ARGB     =  7,
  MLIB_FORMAT_PACKED_ABGR     =  8,
  MLIB_FORMAT_GRAYSCALE_ALPHA =  9,
  MLIB_FORMAT_RGBA            = 10
} mlib_format;

typedef struct {
  mlib_type   type;        /* data type of image                       */
  mlib_s32    channels;    /* number of channels                       */
  mlib_s32    width;       /* width of image in pixels, x dimension    */
  mlib_s32    height;      /* height of image in pixels, y dimension   */
  mlib_s32    stride;      /* linestride = bytes to next row           */
  mlib_s32    flags;       /* collection of helpful hints              */
  void        *data;       /* pointer to first data pixel              */
  void        *state;      /* internal state structure                 */
  mlib_u8     paddings[4]; /* left, top, right, bottom                 */
  mlib_s32    bitoffset;   /* the offset in bits from the beginning    */
                           /* of the data buffer to the first pixel    */
  mlib_format format;      /* pixels format                            */
  mlib_s32    reserved[7 - 2*sizeof(void*)/4];
                           /* Reserved for future use. Also makes      */
                           /* size of this structure = 64 bytes, which */
                           /* is the size of the cache line.           */
} mlib_image;

/*
 * Flags or hints are contained in a 32-bit integer. The bit structure is
 * shown below:
 *
 *      3                   2                   1
 *    1 0 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0 9 8 7 6 5 4 3 2 1 0
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   |S|                 |U|V| shint | hhint | whint |     dhint     |
 *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 *      S = 0   - attributes have been set (attribute field >= 0)
 *          1   - attributes have not been set (attribute field < 0)
 *
 *      U = 0   - mediaLib allocated data space
 *          1   - user allocated data space
 *
 *      V = 0   - stride == width => 1-D vector
 *          1   - stride != width
 *
 *      shint   - last 4 bits of stride
 *
 *      hhint   - last 4 bits of height
 *
 *      whint   - last 4 bits of width
 *
 *      dhint   - last 8 bits of data address
 */

enum {
  MLIB_IMAGE_ALIGNED64     = 0x3f,
  MLIB_IMAGE_ALIGNED8      = 0x7,
  MLIB_IMAGE_ALIGNED4      = 0x3,
  MLIB_IMAGE_ALIGNED2      = 0x1,
  MLIB_IMAGE_WIDTH8X       = 0x700,
  MLIB_IMAGE_WIDTH4X       = 0x300,
  MLIB_IMAGE_WIDTH2X       = 0x100,
  MLIB_IMAGE_HEIGHT8X      = 0x7000,
  MLIB_IMAGE_HEIGHT4X      = 0x3000,
  MLIB_IMAGE_HEIGHT2X      = 0x1000,
  MLIB_IMAGE_STRIDE8X      = 0x70000,
  MLIB_IMAGE_ONEDVECTOR    = 0x100000,
  MLIB_IMAGE_USERALLOCATED = 0x200000,
  MLIB_IMAGE_ATTRIBUTESET  = 0x7fffffff
};

#ifdef __cplusplus
}
#endif

#endif  /* MLIB_IMAGE_TYPES_H */
