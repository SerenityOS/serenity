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


/*
 * FUNCTIONS
 *      mlib_ImageConvCopyEdge  - Copy src edges to dst edges
 *
 *
 * SYNOPSIS
 *      mlib_status mlib_ImageConvCopyEdge(mlib_image       *dst,
 *                                         const mlib_image *src,
 *                                         mlib_s32         dx_l,
 *                                         mlib_s32         dx_r,
 *                                         mlib_s32         dy_t,
 *                                         mlib_s32         dy_b,
 *                                         mlib_s32         cmask)
 *
 * ARGUMENT
 *      dst       Pointer to an dst image.
 *      src       Pointer to an src image.
 *      dx_l      Number of columns on the left side of the
 *                image to be copyed.
 *      dx_r      Number of columns on the right side of the
 *                image to be copyed.
 *      dy_t      Number of rows on the top edge of the
 *                image to be copyed.
 *      dy_b      Number of rows on the top edge of the
 *                image to be copyed.
 *      cmask     Channel mask to indicate the channels to be convolved.
 *                Each bit of which represents a channel in the image. The
 *                channels corresponded to 1 bits are those to be processed.
 *
 * RESTRICTION
 *      The src and the dst must be the same type, same width, same height and have same number
 *      of channels (1, 2, 3, or 4). The unselected channels are not
 *      overwritten. If both src and dst have just one channel,
 *      cmask is ignored.
 *
 * DESCRIPTION
 *      Copy src edges  to dst edges.
 *
 *      The unselected channels are not overwritten.
 *      If src and dst have just one channel,
 *      cmask is ignored.
 */

#include "mlib_image.h"
#include "mlib_ImageConvEdge.h"

/***************************************************************/
#define EDGES(chan, type, mask)                                              \
  {                                                                          \
    type *pdst = (type *) mlib_ImageGetData(dst);                            \
    type *psrc = (type *) mlib_ImageGetData(src);                            \
    mlib_s32 dst_stride = mlib_ImageGetStride(dst) / sizeof(type);           \
    mlib_s32 src_stride = mlib_ImageGetStride(src) / sizeof(type);           \
    mlib_s32 i, j, l;                                                        \
    mlib_s32 testchan;                                                       \
                                                                             \
    testchan = 1;                                                            \
    for (l = chan - 1; l >= 0; l--) {                                        \
      if ((mask & testchan) == 0) {                                          \
        testchan <<= 1;                                                      \
        continue;                                                            \
      }                                                                      \
      testchan <<= 1;                                                        \
      for (j = 0; j < dx_l; j++) {                                           \
        for (i = dy_t; i < (img_height - dy_b); i++) {                       \
          pdst[i*dst_stride + l + j*chan] = psrc[i*src_stride + l + j*chan]; \
        }                                                                    \
      }                                                                      \
      for (j = 0; j < dx_r; j++) {                                           \
        for (i = dy_t; i < (img_height - dy_b); i++) {                       \
          pdst[i*dst_stride + l+(img_width-1 - j)*chan] =                    \
          psrc[i*src_stride + l+(img_width-1 - j)*chan];                     \
        }                                                                    \
      }                                                                      \
      for (i = 0; i < dy_t; i++) {                                           \
        for (j = 0; j < img_width; j++) {                                    \
          pdst[i*dst_stride + l + j*chan] = psrc[i*src_stride + l + j*chan]; \
        }                                                                    \
      }                                                                      \
      for (i = 0; i < dy_b; i++) {                                           \
        for (j = 0; j < img_width; j++) {                                    \
          pdst[(img_height-1 - i)*dst_stride + l + j*chan] =                 \
          psrc[(img_height-1 - i)*src_stride + l + j*chan];                  \
        }                                                                    \
      }                                                                      \
    }                                                                        \
  }

/***************************************************************/
mlib_status mlib_ImageConvCopyEdge(mlib_image       *dst,
                                   const mlib_image *src,
                                   mlib_s32         dx_l,
                                   mlib_s32         dx_r,
                                   mlib_s32         dy_t,
                                   mlib_s32         dy_b,
                                   mlib_s32         cmask)
{
  mlib_s32 img_width = mlib_ImageGetWidth(dst);
  mlib_s32 img_height = mlib_ImageGetHeight(dst);
  mlib_s32 channel = mlib_ImageGetChannels(dst);

  if (dx_l + dx_r > img_width) {
    dx_l = img_width;
    dx_r = 0;
  }

  if (dy_t + dy_b > img_height) {
    dy_t = img_height;
    dy_b = 0;
  }

  if (channel == 1)
    cmask = 1;

  switch (mlib_ImageGetType(src)) {
    case MLIB_BIT:
      return mlib_ImageConvCopyEdge_Bit(dst, src, dx_l, dx_r, dy_t, dy_b, cmask);
    case MLIB_BYTE:
      EDGES(channel, mlib_u8, cmask)
        break;
    case MLIB_SHORT:
    case MLIB_USHORT:
      EDGES(channel, mlib_u16, cmask)
        break;
    case MLIB_INT:
    case MLIB_FLOAT:
      EDGES(channel, mlib_u32, cmask)
        break;
    case MLIB_DOUBLE:
      EDGES(channel, mlib_d64, cmask)
        break;
    default:
      return MLIB_FAILURE;
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
