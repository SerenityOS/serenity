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
 *      mlib_ImageConvClearEdge_BIt  - Set edge of an bit type image to a specific
 *                                     color.
 *
 * SYNOPSIS
 *      mlib_status mlib_ImageConvClearEdge_Bit(mlib_image     *img,
 *                                              mlib_s32       dx_l,
 *                                              mlib_32        dx_r,
 *                                              mlib_s32       dy_t,
 *                                              mlib_32        dy_b,
 *                                              const mlib_s32 *color,
 *                                              mlib_s32       cmask);
 *
 * ARGUMENT
 *      img       Pointer to an image.
 *      dx_l      Number of columns on the left side of the
 *                image to be cleared.
 *      dx_r      Number of columns on the right side of the
 *                image to be cleared.
 *      dy_t      Number of rows on the top edge of the
 *                image to be cleared.
 *      dy_b      Number of rows on the top edge of the
 *                image to be cleared.
 *      color     Pointer to the color that the edges are set to.
 *      cmask     Channel mask to indicate the channels to be convolved.
 *                Each bit of which represents a channel in the image. The
 *                channels corresponded to 1 bits are those to be processed.
 *
 * RESTRICTION
 *      img can have 1 channels of MLIB_BIT data type.
 *
 * DESCRIPTION
 *      Set edge of an image to a specific color.
 *      The unselected channels are not overwritten.
 *      If src and dst have just one channel,
 *      cmask is ignored.
 */

#include "mlib_image.h"
#include "mlib_ImageConvEdge.h"

/***************************************************************/
mlib_status mlib_ImageConvClearEdge_Bit(mlib_image     *img,
                                        mlib_s32       dx_l,
                                        mlib_s32       dx_r,
                                        mlib_s32       dy_t,
                                        mlib_s32       dy_b,
                                        const mlib_s32 *color,
                                        mlib_s32       cmask)
{
  mlib_u8  *pimg = mlib_ImageGetData(img), *pd;
  mlib_s32 img_height = mlib_ImageGetHeight(img);
  mlib_s32 img_width  = mlib_ImageGetWidth(img);
  mlib_s32 img_stride = mlib_ImageGetStride(img);
  mlib_s32 bitoff = mlib_ImageGetBitOffset(img);
  mlib_s32 bitoff_end;
  mlib_u8  color_i, mask, mask_end, tmp_color;
  mlib_u8  tmp_start, tmp_end;
  mlib_s32 i, j, amount;

  if ((mlib_ImageGetType(img) != MLIB_BIT) || (mlib_ImageGetChannels(img) != 1))
    return MLIB_FAILURE;

  color_i = (mlib_u8)(color[0] & 1);
  color_i |= (color_i << 1);
  color_i |= (color_i << 2);
  color_i |= (color_i << 4);

  pd = pimg;

  if (dx_l > 0) {
    if (bitoff + dx_l <= 8) {
      mask = (0xFF >> bitoff) & (0xFF << ((8 - (bitoff + dx_l)) & 7));
      tmp_color = color_i & mask;
      mask = ~mask;

      for (i = dy_t; i < (img_height - dy_b); i++) {
        pd[i*img_stride] = (pd[i*img_stride] & mask) | tmp_color;
      }

    } else {
      mask = (0xFF >> bitoff);
      tmp_color = color_i & mask;
      mask = ~mask;

      for (i = dy_t; i < (img_height - dy_b); i++) {
        pd[i*img_stride] = (pd[i*img_stride] & mask) | tmp_color;
      }

      amount = (bitoff + dx_l + 7) >> 3;
      mask = (0xFF << ((8 - (bitoff + dx_l)) & 7));
      tmp_color = color_i & mask;
      mask = ~mask;

      for (j = 1; j < amount - 1; j++) {
        for (i = dy_t; i < (img_height - dy_b); i++) {
          pd[i*img_stride + j] = color_i;
        }
      }

      for (i = dy_t; i < (img_height - dy_b); i++) {
        pd[i*img_stride + amount - 1] = (pd[i*img_stride + amount - 1] & mask) | tmp_color;
      }
    }
  }

  if (dx_r > 0) {
    pd = pimg + (img_width + bitoff - dx_r) / 8;
    bitoff = (img_width + bitoff - dx_r) & 7;

    if (bitoff + dx_r <= 8) {
      mask = (0xFF >> bitoff) & (0xFF << ((8 - (bitoff + dx_r)) & 7));
      tmp_color = color_i & mask;
      mask = ~mask;

      for (i = dy_t; i < (img_height - dy_b); i++) {
        pd[i*img_stride] = (pd[i*img_stride] & mask) | tmp_color;
      }

    } else {
      mask = (0xFF >> bitoff);
      tmp_color = color_i & mask;
      mask = ~mask;

      for (i = dy_t; i < (img_height - dy_b); i++) {
        pd[i*img_stride] = (pd[i*img_stride] & mask) | tmp_color;
      }

      amount = (bitoff + dx_r + 7) >> 3;
      mask = (0xFF << ((8 - (bitoff + dx_r)) & 7));
      tmp_color = color_i & mask;
      mask = ~mask;

      for (j = 1; j < amount - 1; j++) {
        for (i = dy_t; i < (img_height - dy_b); i++) {
          pd[i*img_stride + j] = color_i;
        }
      }

      for (i = dy_t; i < (img_height - dy_b); i++) {
        pd[i*img_stride + amount - 1] = (pd[i*img_stride + amount - 1] & mask) | tmp_color;
      }
    }
  }

  bitoff = mlib_ImageGetBitOffset(img);
  bitoff_end = (bitoff + img_width) & 7;
  amount = (bitoff + img_width + 7) >> 3;
  mask = (0xFF >> bitoff);
  mask_end = (0xFF << ((8 - bitoff_end) & 7));

  pd = pimg;

  for (i = 0; i < dy_t; i++) {
    tmp_start = pd[i*img_stride];
    tmp_end = pd[i*img_stride+amount-1];
    for (j = 0; j < amount; j++) {
      pd[i*img_stride + j] = color_i;
    }

    pd[i*img_stride] = (tmp_start & (~mask)) | (pd[i*img_stride] & mask);
    pd[i*img_stride+amount-1] = (tmp_end & (~mask_end)) |
                                (pd[i*img_stride+amount-1] & mask_end);
  }

  pd = pimg + (img_height-1)*img_stride;

  for (i = 0; i < dy_b; i++) {
    tmp_start = pd[-i*img_stride];
    tmp_end = pd[-i*img_stride+amount-1];
    for (j = 0; j < amount; j++) {
     pd[-i*img_stride + j] = color_i;
    }

    pd[-i*img_stride] = (tmp_start & (~mask)) | (pd[-i*img_stride] & mask);
    pd[-i*img_stride+amount-1] = (tmp_end & (~mask_end)) |
                                 (pd[-i*img_stride+amount-1] & mask_end);
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
