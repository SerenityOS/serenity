/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * FUNCTION
 *      mlib_ImageAffine - image affine transformation with edge condition
 *
 * SYNOPSIS
 *      mlib_status mlib_ImageAffine(mlib_image       *dst,
 *                                   const mlib_image *src,
 *                                   const mlib_d64   *mtx,
 *                                   mlib_filter      filter,
 *                                   mlib_edge        edge)
 *
 * ARGUMENTS
 *      dst       Pointer to destination image
 *      src       Pointer to source image
 *      mtx       Transformation matrix, where
 *                  mtx[0] holds a;  mtx[1] holds b;
 *                  mtx[2] holds tx; mtx[3] holds c;
 *                  mtx[4] holds d;  mtx[5] holds ty.
 *      filter    Type of resampling filter.
 *      edge      Type of edge condition.
 *
 * DESCRIPTION
 *                      xd = a*xs + b*ys + tx
 *                      yd = c*xs + d*ys + ty
 *
 *  The upper-left corner pixel of an image is located at (0.5, 0.5).
 *
 *  The resampling filter can be one of the following:
 *      MLIB_NEAREST
 *      MLIB_BILINEAR
 *      MLIB_BICUBIC
 *      MLIB_BICUBIC2
 *
 *  The edge condition can be one of the following:
 *      MLIB_EDGE_DST_NO_WRITE  (default)
 *      MLIB_EDGE_DST_FILL_ZERO
 *      MLIB_EDGE_OP_NEAREST
 *      MLIB_EDGE_SRC_EXTEND
 *      MLIB_EDGE_SRC_PADDED
 *
 * RESTRICTION
 *      src and dst must be the same type and the same number of channels.
 *      They can have 1, 2, 3 or 4 channels. They can be in MLIB_BIT, MLIB_BYTE,
 *      MLIB_SHORT, MLIB_USHORT or MLIB_INT data type.
 *
 *      src image can not have width or height larger than 32767.
 */

#include "mlib_ImageCheck.h"
#include "mlib_ImageAffine.h"


/***************************************************************/
#define BUFF_SIZE  600

/***************************************************************/
const type_affine_fun mlib_AffineFunArr_nn[] = {
  mlib_ImageAffine_u8_1ch_nn,  mlib_ImageAffine_u8_2ch_nn,
  mlib_ImageAffine_u8_3ch_nn,  mlib_ImageAffine_u8_4ch_nn,
  mlib_ImageAffine_s16_1ch_nn, mlib_ImageAffine_s16_2ch_nn,
  mlib_ImageAffine_s16_3ch_nn, mlib_ImageAffine_s16_4ch_nn,
  mlib_ImageAffine_s32_1ch_nn, mlib_ImageAffine_s32_2ch_nn,
  mlib_ImageAffine_s32_3ch_nn, mlib_ImageAffine_s32_4ch_nn,
  mlib_ImageAffine_d64_1ch_nn, mlib_ImageAffine_d64_2ch_nn,
  mlib_ImageAffine_d64_3ch_nn, mlib_ImageAffine_d64_4ch_nn,
};

/***************************************************************/
const type_affine_fun mlib_AffineFunArr_bl[] = {
  mlib_ImageAffine_u8_1ch_bl,  mlib_ImageAffine_u8_2ch_bl,
  mlib_ImageAffine_u8_3ch_bl,  mlib_ImageAffine_u8_4ch_bl,
  mlib_ImageAffine_s16_1ch_bl, mlib_ImageAffine_s16_2ch_bl,
  mlib_ImageAffine_s16_3ch_bl, mlib_ImageAffine_s16_4ch_bl,
  mlib_ImageAffine_s32_1ch_bl, mlib_ImageAffine_s32_2ch_bl,
  mlib_ImageAffine_s32_3ch_bl, mlib_ImageAffine_s32_4ch_bl,
  mlib_ImageAffine_u16_1ch_bl, mlib_ImageAffine_u16_2ch_bl,
  mlib_ImageAffine_u16_3ch_bl, mlib_ImageAffine_u16_4ch_bl,
  mlib_ImageAffine_f32_1ch_bl, mlib_ImageAffine_f32_2ch_bl,
  mlib_ImageAffine_f32_3ch_bl, mlib_ImageAffine_f32_4ch_bl,
  mlib_ImageAffine_d64_1ch_bl, mlib_ImageAffine_d64_2ch_bl,
  mlib_ImageAffine_d64_3ch_bl, mlib_ImageAffine_d64_4ch_bl
};

/***************************************************************/
const type_affine_fun mlib_AffineFunArr_bc[] = {
  mlib_ImageAffine_u8_1ch_bc,  mlib_ImageAffine_u8_2ch_bc,
  mlib_ImageAffine_u8_3ch_bc,  mlib_ImageAffine_u8_4ch_bc,
  mlib_ImageAffine_s16_1ch_bc, mlib_ImageAffine_s16_2ch_bc,
  mlib_ImageAffine_s16_3ch_bc, mlib_ImageAffine_s16_4ch_bc,
  mlib_ImageAffine_s32_1ch_bc, mlib_ImageAffine_s32_2ch_bc,
  mlib_ImageAffine_s32_3ch_bc, mlib_ImageAffine_s32_4ch_bc,
  mlib_ImageAffine_u16_1ch_bc, mlib_ImageAffine_u16_2ch_bc,
  mlib_ImageAffine_u16_3ch_bc, mlib_ImageAffine_u16_4ch_bc,
  mlib_ImageAffine_f32_1ch_bc, mlib_ImageAffine_f32_2ch_bc,
  mlib_ImageAffine_f32_3ch_bc, mlib_ImageAffine_f32_4ch_bc,
  mlib_ImageAffine_d64_1ch_bc, mlib_ImageAffine_d64_2ch_bc,
  mlib_ImageAffine_d64_3ch_bc, mlib_ImageAffine_d64_4ch_bc
};

/***************************************************************/
#ifdef i386 /* do not perform the coping by mlib_d64 data type for x86 */
#define MAX_T_IND  2
#else
#define MAX_T_IND  3
#endif /* i386 ( do not perform the coping by mlib_d64 data type for x86 ) */

/***************************************************************/
mlib_status mlib_ImageAffine_alltypes(mlib_image       *dst,
                                      const mlib_image *src,
                                      const mlib_d64   *mtx,
                                      mlib_filter      filter,
                                      mlib_edge        edge)
{
  mlib_affine_param param[1];
  mlib_status res;
  mlib_type type;
  mlib_s32 nchan, t_ind, kw, kw1;
  mlib_addr align;
  mlib_d64 buff_lcl[BUFF_SIZE / 8];
  mlib_u8 **lineAddr = NULL;

  /* check for obvious errors */
  MLIB_IMAGE_TYPE_EQUAL(src, dst);
  MLIB_IMAGE_CHAN_EQUAL(src, dst);

  type = mlib_ImageGetType(dst);
  nchan = mlib_ImageGetChannels(dst);

  switch (filter) {
    case MLIB_NEAREST:
      kw = 1;
      kw1 = 0;
      break;

    case MLIB_BILINEAR:
      kw = 2;
      kw1 = 0;
      break;

    case MLIB_BICUBIC:
    case MLIB_BICUBIC2:
      kw = 4;
      kw1 = 1;
      break;

    default:
      return MLIB_FAILURE;
  }

  STORE_PARAM(param, lineAddr);
  STORE_PARAM(param, filter);

  res = mlib_AffineEdges(param, dst, src, buff_lcl, BUFF_SIZE,
                         kw, kw, kw1, kw1, edge, mtx, MLIB_SHIFT, MLIB_SHIFT);

  if (res != MLIB_SUCCESS)
    return res;

  lineAddr = param->lineAddr;

  if (type == MLIB_BYTE)
    t_ind = 0;
  else if (type == MLIB_SHORT)
    t_ind = 1;
  else if (type == MLIB_INT)
    t_ind = 2;
  else if (type == MLIB_USHORT)
    t_ind = 3;
  else if (type == MLIB_FLOAT)
    t_ind = 4;
  else if (type == MLIB_DOUBLE)
    t_ind = 5;
  else
    return MLIB_FAILURE; /* unknown image type */

  if (type == MLIB_BIT) {
    mlib_s32 s_bitoff = mlib_ImageGetBitOffset(src);
    mlib_s32 d_bitoff = mlib_ImageGetBitOffset(dst);

    if (nchan != 1 || filter != MLIB_NEAREST)
      return MLIB_FAILURE;
    mlib_ImageAffine_bit_1ch_nn(param, s_bitoff, d_bitoff);
  }
  else {
    switch (filter) {
      case MLIB_NEAREST:

        if (t_ind >= 3)
          t_ind -= 2;                                      /* correct types USHORT, FLOAT, DOUBLE; new values: 1, 2, 3 */

        /* two channels as one channel of next type */
        align = (mlib_addr) (param->dstData) | (mlib_addr) lineAddr[0];
        align |= param->dstYStride | param->srcYStride;
        while (((nchan | (align >> t_ind)) & 1) == 0 && t_ind < MAX_T_IND) {
          nchan >>= 1;
          t_ind++;
        }

        res = mlib_AffineFunArr_nn[4 * t_ind + (nchan - 1)] (param);
        break;

      case MLIB_BILINEAR:

        res = mlib_AffineFunArr_bl[4 * t_ind + (nchan - 1)] (param);
        break;

      case MLIB_BICUBIC:
      case MLIB_BICUBIC2:

        res = mlib_AffineFunArr_bc[4 * t_ind + (nchan - 1)] (param);
        break;
    }

    if (res != MLIB_SUCCESS) {
      if (param->buff_malloc != NULL)
        mlib_free(param->buff_malloc);
      return res;
    }
  }

  if (edge == MLIB_EDGE_SRC_PADDED)
    edge = MLIB_EDGE_DST_NO_WRITE;

  if (filter != MLIB_NEAREST && edge != MLIB_EDGE_DST_NO_WRITE) {
    mlib_affine_param param_e[1];
    mlib_d64 buff_lcl1[BUFF_SIZE / 8];

    STORE_PARAM(param_e, lineAddr);
    STORE_PARAM(param_e, filter);

    res = mlib_AffineEdges(param_e, dst, src, buff_lcl1, BUFF_SIZE,
                           kw, kw, kw1, kw1, -1, mtx, MLIB_SHIFT, MLIB_SHIFT);

    if (res != MLIB_SUCCESS) {
      if (param->buff_malloc != NULL)
        mlib_free(param->buff_malloc);
      return res;
    }

    switch (edge) {
      case MLIB_EDGE_DST_FILL_ZERO:
        mlib_ImageAffineEdgeZero(param, param_e);
        break;

      case MLIB_EDGE_OP_NEAREST:
        mlib_ImageAffineEdgeNearest(param, param_e);
        break;

      case MLIB_EDGE_SRC_EXTEND:

        if (filter == MLIB_BILINEAR) {
          res = mlib_ImageAffineEdgeExtend_BL(param, param_e);
        }
        else {
          res = mlib_ImageAffineEdgeExtend_BC(param, param_e);
        }

        break;

    default:
      /* nothing to do for other edge types. */
      break;
    }

    if (param_e->buff_malloc != NULL)
      mlib_free(param_e->buff_malloc);
  }

  if (param->buff_malloc != NULL)
    mlib_free(param->buff_malloc);

  return res;
}

/***************************************************************/
JNIEXPORT
mlib_status mlib_ImageAffine(mlib_image       *dst,
                             const mlib_image *src,
                             const mlib_d64   *mtx,
                             mlib_filter      filter,
                             mlib_edge        edge)
{
  mlib_type type;

  MLIB_IMAGE_CHECK(src);
  MLIB_IMAGE_CHECK(dst);

  type = mlib_ImageGetType(dst);

  if (type != MLIB_BIT && type != MLIB_BYTE &&
      type != MLIB_SHORT && type != MLIB_USHORT && type != MLIB_INT) {
    return MLIB_FAILURE;
  }

  return mlib_ImageAffine_alltypes(dst, src, mtx, filter, edge);
}

/***************************************************************/
