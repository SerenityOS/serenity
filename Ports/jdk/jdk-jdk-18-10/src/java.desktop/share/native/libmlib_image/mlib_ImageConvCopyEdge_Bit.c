/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 *      mlib_ImageConvCopyEdge_Bit  - Copy src edges  to dst edges
 *
 *
 * SYNOPSIS
 *      mlib_status mlib_ImageConvCopyEdge_Bit(mlib_image      *dst,
 *                                            const mlib_image *src,
 *                                            mlib_s32         dx_l,
 *                                            mlib_32          dx_r,
 *                                            mlib_s32         dy_t,
 *                                            mlib_32          dy_b,
 *                                            mlib_s32         cmask);
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
 *      The src and the dst must be the MLIB_BIT type, same width, same height and have same number
 *      of channels (1). The unselected channels are not
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
mlib_status mlib_ImageConvCopyEdge_Bit(mlib_image       *dst,
                                       const mlib_image *src,
                                       mlib_s32         dx_l,
                                       mlib_s32         dx_r,
                                       mlib_s32         dy_t,
                                       mlib_s32         dy_b,
                                       mlib_s32         cmask)
{
  mlib_u8  *pdst = mlib_ImageGetData(dst), *pd;
  mlib_u8  *psrc = mlib_ImageGetData(src), *ps;
  mlib_s32 img_height = mlib_ImageGetHeight(dst);
  mlib_s32 img_width  = mlib_ImageGetWidth(dst);
  mlib_s32 img_strided = mlib_ImageGetStride(dst);
  mlib_s32 img_strides = mlib_ImageGetStride(src);
  mlib_s32 bitoffd = mlib_ImageGetBitOffset(dst);
  mlib_s32 bitoffs = mlib_ImageGetBitOffset(src);
  mlib_s32 bitoff_end, test, shift1, shift2;
  mlib_u32 s0, s1, tmp;
  mlib_u8  mask, mask_end;
  mlib_u8  tmp_start, tmp_end;
  mlib_s32 i, j, amount;

  if (bitoffd == bitoffs) {
    pd = pdst;
    ps = psrc;

    if (dx_l > 0) {
      if (bitoffd + dx_l <= 8) {
        mask = (0xFF >> bitoffd) & (0xFF << ((8 - (bitoffd + dx_l)) & 7));

        for (i = dy_t; i < (img_height - dy_b); i++) {
          pd[i*img_strided] = (pd[i*img_strided] & ~mask) | (ps[i*img_strides] & mask);
        }

      } else {
        mask = (0xFF >> bitoffd);

        for (i = dy_t; i < (img_height - dy_b); i++) {
          pd[i*img_strided] = (pd[i*img_strided] & ~mask) | (ps[i*img_strides] & mask);
        }

        amount = (bitoffd + dx_l + 7) >> 3;
        mask = (0xFF << ((8 - (bitoffd + dx_l)) & 7));

        for (j = 1; j < amount - 1; j++) {
          for (i = dy_t; i < (img_height - dy_b); i++) {
            pd[i*img_strided + j] = ps[i*img_strides + j];
          }
        }

        for (i = dy_t; i < (img_height - dy_b); i++) {
          pd[i*img_strided + amount - 1] = (pd[i*img_strided + amount - 1] & ~mask) |
                                           (ps[i*img_strides + amount - 1] & mask);
        }
      }
    }

    if (dx_r > 0) {
      pd = pdst + (img_width + bitoffd - dx_r) / 8;
      ps = psrc + (img_width + bitoffd - dx_r) / 8;
      bitoffd = (img_width + bitoffd - dx_r) & 7;

      if (bitoffd + dx_r <= 8) {
        mask = (0xFF >> bitoffd) & (0xFF << ((8 - (bitoffd + dx_r)) & 7));

        for (i = dy_t; i < (img_height - dy_b); i++) {
          pd[i*img_strided] = (pd[i*img_strided] & ~mask) | (ps[i*img_strides] & mask);
        }

      } else {
        mask = (0xFF >> bitoffd);

        for (i = dy_t; i < (img_height - dy_b); i++) {
          pd[i*img_strided] = (pd[i*img_strided] & ~mask) | (ps[i*img_strides] & mask);
        }

        amount = (bitoffd + dx_r + 7) >> 3;
        mask = (0xFF << ((8 - (bitoffd + dx_r)) & 7));

        for (j = 1; j < amount - 1; j++) {
          for (i = dy_t; i < (img_height - dy_b); i++) {
            pd[i*img_strided + j] = ps[i*img_strides + j];
          }
        }

        for (i = dy_t; i < (img_height - dy_b); i++) {
          pd[i*img_strided + amount - 1] = (pd[i*img_strided + amount - 1] & ~mask) |
                                           (ps[i*img_strides + amount - 1] & mask);
        }
      }
    }

    bitoffd = mlib_ImageGetBitOffset(dst);
    bitoff_end = (bitoffd + img_width) & 7;
    amount = (bitoffd + img_width + 7) >> 3;
    mask = (0xFF >> bitoffd);
    mask_end = (0xFF << ((8 - bitoff_end) & 7));

    pd = pdst;
    ps = psrc;

    for (i = 0; i < dy_t; i++) {
      tmp_start = pd[i*img_strided];
      tmp_end = pd[i*img_strided+amount-1];
      for (j = 0; j < amount; j++) {
        pd[i*img_strided + j] = ps[i*img_strides + j];
      }

      pd[i*img_strided] = (tmp_start & (~mask)) | (pd[i*img_strided] & mask);
      pd[i*img_strided+amount-1] = (tmp_end & (~mask_end)) |
                                  (pd[i*img_strided+amount-1] & mask_end);
    }

    pd = pdst + (img_height-1)*img_strided;
    ps = psrc + (img_height-1)*img_strides;

    for (i = 0; i < dy_b; i++) {
      tmp_start = pd[-i*img_strided];
      tmp_end = pd[-i*img_strided+amount-1];
      for (j = 0; j < amount; j++) {
       pd[-i*img_strided + j] = ps[-i*img_strides + j];
      }

      pd[-i*img_strided] = (tmp_start & (~mask)) | (pd[-i*img_strided] & mask);
      pd[-i*img_strided+amount-1] = (tmp_end & (~mask_end)) |
                                   (pd[-i*img_strided+amount-1] & mask_end);
    }

  } else {
    pd = pdst;

    if (bitoffs > bitoffd) {
      ps = psrc;
      shift2 = (8 - (bitoffs - bitoffd));
      test = 0;
    } else {
      test = 1;
      ps = psrc - 1;
      shift2 = bitoffd - bitoffs;
    }

    shift1 = 8 - shift2;

    if (dx_l > 0) {
      if (bitoffd + dx_l <= 8) {
        mask = (0xFF >> bitoffd) & (0xFF << ((8 - (bitoffd + dx_l)) & 7));

        for (i = dy_t; i < (img_height - dy_b); i++) {
          s0 = ps[i*img_strides];
          s1 = ps[i*img_strides+1];
          tmp = (s0 << shift1) | (s1 >> shift2);
          pd[i*img_strided] = (pd[i*img_strided] & ~mask) | (tmp & mask);
        }

      } else {
        mask = (0xFF >> bitoffd);

        for (i = dy_t; i < (img_height - dy_b); i++) {
          s0 = ps[i*img_strides];
          s1 = ps[i*img_strides+1];
          tmp = (s0 << shift1) | (s1 >> shift2);
          pd[i*img_strided] = (pd[i*img_strided] & ~mask) | (tmp & mask);
        }

        amount = (bitoffd + dx_l + 7) >> 3;
        mask = (0xFF << ((8 - (bitoffd + dx_l)) & 7));

        for (j = 1; j < amount - 1; j++) {
          for (i = dy_t; i < (img_height - dy_b); i++) {
            s0 = ps[i*img_strides+j];
            s1 = ps[i*img_strides+j+1];
            pd[i*img_strided + j] = (s0 << shift1) | (s1 >> shift2);
            s0 = s1;
          }
        }

        for (i = dy_t; i < (img_height - dy_b); i++) {
          s0 = ps[i*img_strides+amount-1];
          s1 = ps[i*img_strides+amount];
          tmp = (s0 << shift1) | (s1 >> shift2);
          pd[i*img_strided + amount - 1] = (pd[i*img_strided + amount - 1] & ~mask) |
                                           (tmp & mask);
        }
      }
    }

    if (dx_r > 0) {
      pd = pdst + (img_width + bitoffd - dx_r) / 8;
      ps = psrc + (img_width + bitoffd - dx_r) / 8;
      bitoffd = (img_width + bitoffd - dx_r) & 7;
      ps -= test;

      if (bitoffd + dx_r <= 8) {
        mask = (0xFF >> bitoffd) & (0xFF << ((8 - (bitoffd + dx_r)) & 7));

        for (i = dy_t; i < (img_height - dy_b); i++) {
          s0 = ps[i*img_strides];
          s1 = ps[i*img_strides+1];
          tmp = (s0 << shift1) | (s1 >> shift2);
          pd[i*img_strided] = (pd[i*img_strided] & ~mask) | (tmp & mask);
        }

      } else {
        mask = (0xFF >> bitoffd);

        for (i = dy_t; i < (img_height - dy_b); i++) {
          s0 = ps[i*img_strides];
          s1 = ps[i*img_strides+1];
          tmp = (s0 << shift1) | (s1 >> shift2);
          pd[i*img_strided] = (pd[i*img_strided] & ~mask) | (tmp & mask);
        }

        amount = (bitoffd + dx_r + 7) >> 3;
        mask = (0xFF << ((8 - (bitoffd + dx_r)) & 7));

        for (j = 1; j < amount - 1; j++) {
          for (i = dy_t; i < (img_height - dy_b); i++) {
            s0 = ps[i*img_strides+j];
            s1 = ps[i*img_strides+j+1];
            pd[i*img_strided + j] = (s0 << shift1) | (s1 >> shift2);
          }
        }

        for (i = dy_t; i < (img_height - dy_b); i++) {
          s0 = ps[i*img_strides+amount-1];
          s1 = ps[i*img_strides+amount];
          tmp = (s0 << shift1) | (s1 >> shift2);
          pd[i*img_strided + amount - 1] = (pd[i*img_strided + amount - 1] & ~mask) |
                                           (tmp & mask);
        }
      }
    }

    bitoffd = mlib_ImageGetBitOffset(dst);
    bitoff_end = (bitoffd + img_width) & 7;
    amount = (bitoffd + img_width + 7) >> 3;
    mask = (0xFF >> bitoffd);
    mask_end = (0xFF << ((8 - bitoff_end) & 7));

    pd = pdst;
    ps = psrc-test;

    for (i = 0; i < dy_t; i++) {
      tmp_start = pd[i*img_strided];
      tmp_end = pd[i*img_strided+amount-1];
      s0 = ps[i*img_strides];
      for (j = 0; j < amount; j++) {
        s1 = ps[i*img_strides+j+1];
        pd[i*img_strided + j] = (s0 << shift1) | (s1 >> shift2);
        s0 = s1;
      }

      pd[i*img_strided] = (tmp_start & (~mask)) | (pd[i*img_strided] & mask);
      pd[i*img_strided+amount-1] = (tmp_end & (~mask_end)) |
                                   (pd[i*img_strided+amount-1] & mask_end);
    }

    pd = pdst + (img_height-1)*img_strided;
    ps = psrc + (img_height-1)*img_strides - test;

    for (i = 0; i < dy_b; i++) {
      tmp_start = pd[-i*img_strided];
      tmp_end = pd[-i*img_strided+amount-1];
      s0 = ps[-i*img_strides];
      for (j = 0; j < amount; j++) {
       s1 = ps[-i*img_strides+j+1];
       pd[-i*img_strided + j] = (s0 << shift1) | (s1 >> shift2);
       s0 = s1;
      }

      pd[-i*img_strided] = (tmp_start & (~mask)) | (pd[-i*img_strided] & mask);
      pd[-i*img_strided+amount-1] = (tmp_end & (~mask_end)) |
                                   (pd[-i*img_strided+amount-1] & mask_end);
    }
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
