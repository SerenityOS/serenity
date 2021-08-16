/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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


#ifndef __MLIB_IMAGEAFFINE_H
#define __MLIB_IMAGEAFFINE_H

#include "mlib_image.h"
#include "mlib_ImageDivTables.h"
#include "mlib_ImageFilters.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/*
 * DESCRIPTION
 *   Internal macro for mlib_ImageAffine.
 *
 *   DTYPE define must be set to data type of image.
 *   FTYPE define must be set to type of floating-point operations.
 */

/***************************************************************/
typedef struct {
  mlib_image *src;
  mlib_image *dst;
  mlib_u8  *buff_malloc;
  mlib_u8  **lineAddr;
  mlib_u8  *dstData;
  mlib_s32 *leftEdges;
  mlib_s32 *rightEdges;
  mlib_s32 *xStarts;
  mlib_s32 *yStarts;
  mlib_s32 yStart;
  mlib_s32 yFinish;
  mlib_s32 dX;
  mlib_s32 dY;
  mlib_s32 max_xsize;
  mlib_s32 srcYStride;
  mlib_s32 dstYStride;
  mlib_s32 *warp_tbl;
  mlib_filter filter;
} mlib_affine_param;

/***************************************************************/

#define LOAD_PARAM(param, x)  x = param->x
#define STORE_PARAM(param, x) param->x=x

/***************************************************************/
mlib_status mlib_AffineEdges(mlib_affine_param *param,
                             const mlib_image  *dst,
                             const mlib_image  *src,
                             void              *buff_lcl,
                             mlib_s32          buff_size,
                             mlib_s32          kw,
                             mlib_s32          kh,
                             mlib_s32          kw1,
                             mlib_s32          kh1,
                             mlib_edge         edge,
                             const mlib_d64    *mtx,
                             mlib_s32          shiftx,
                             mlib_s32          shifty);

/***************************************************************/
typedef mlib_status (*type_affine_fun)(mlib_affine_param *param);

/***************************************************************/
void mlib_ImageAffine_bit_1ch_nn(mlib_affine_param *param,
                                 mlib_s32          s_bitoff,
                                 mlib_s32          d_bitoff);

mlib_status mlib_ImageAffine_u8_1ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u8_2ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u8_3ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u8_4ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s16_1ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s16_2ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s16_3ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s16_4ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s32_1ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s32_2ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s32_3ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s32_4ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_d64_1ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_d64_2ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_d64_3ch_nn(mlib_affine_param *param);
mlib_status mlib_ImageAffine_d64_4ch_nn(mlib_affine_param *param);

mlib_status mlib_ImageAffine_u8_1ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u8_2ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u8_3ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u8_4ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s16_1ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s16_2ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s16_3ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s16_4ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u16_1ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u16_2ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u16_3ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u16_4ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s32_1ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s32_2ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s32_3ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s32_4ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_f32_1ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_f32_2ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_f32_3ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_f32_4ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_d64_1ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_d64_2ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_d64_3ch_bl(mlib_affine_param *param);
mlib_status mlib_ImageAffine_d64_4ch_bl(mlib_affine_param *param);

mlib_status mlib_ImageAffine_u8_1ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u8_2ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u8_3ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u8_4ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s16_1ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s16_2ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s16_3ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s16_4ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u16_1ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u16_2ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u16_3ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_u16_4ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s32_1ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s32_2ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s32_3ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_s32_4ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_f32_1ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_f32_2ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_f32_3ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_f32_4ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_d64_1ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_d64_2ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_d64_3ch_bc(mlib_affine_param *param);
mlib_status mlib_ImageAffine_d64_4ch_bc(mlib_affine_param *param);

/***************************************************************/
void mlib_ImageAffineEdgeZero(mlib_affine_param *param,
                              mlib_affine_param *param_e);

void mlib_ImageAffineEdgeNearest(mlib_affine_param *param,
                                 mlib_affine_param *param_e);

mlib_status mlib_ImageAffineEdgeExtend_BL(mlib_affine_param *param,
                                          mlib_affine_param *param_e);

mlib_status mlib_ImageAffineEdgeExtend_BC(mlib_affine_param *param,
                                          mlib_affine_param *param_e);

/***************************************************************/
mlib_status mlib_ImageAffine_alltypes(mlib_image       *dst,
                                      const mlib_image *src,
                                      const mlib_d64   *mtx,
                                      mlib_filter      filter,
                                      mlib_edge        edge);

/***************************************************************/
extern const type_affine_fun mlib_AffineFunArr_nn[];
extern const type_affine_fun mlib_AffineFunArr_bl[];
extern const type_affine_fun mlib_AffineFunArr_bc[];

/***************************************************************/
typedef union {
  mlib_d64 d64;
  struct {
    mlib_f32 f0;
    mlib_f32 f1;
  } f32s;
} d64_2x32;

/***************************************************************/
#define MLIB_SHIFT  16
#define MLIB_PREC   (1 << MLIB_SHIFT)
#define MLIB_MASK   (MLIB_PREC - 1)

/***************************************************************/
#define ONE  (FTYPE)1.0

/***************************************************************/
#ifdef MLIB_USE_FTOI_CLAMPING

#define SAT_32(DST, SRC)                                              \
  DST = (mlib_s32) SRC

#else

#define SAT_32(DST, SRC)                                              \
  if (SRC >= MLIB_S32_MAX)                                     \
    SRC = MLIB_S32_MAX;                                        \
  if (SRC <= MLIB_S32_MIN)                                     \
    SRC = MLIB_S32_MIN;                                        \
  DST = (mlib_s32) SRC

#endif /* MLIB_USE_FTOI_CLAMPING */

//we still need this for mlib_ImageAffine_BC_S32.c
#define SAT32(DST) SAT_32(DST, val0)

/***************************************************************/
#if defined(MLIB_OS64BIT) || (defined(MACOSX) && defined(_LP64))
#define PBITS  3
#define MLIB_POINTER_SHIFT(P)  (((P) >> (MLIB_SHIFT - 3)) &~ 7)
#define MLIB_POINTER_GET(A, P) (*(DTYPE**)((mlib_u8*)(A) + (P)))
#else
#define PBITS  2
#define MLIB_POINTER_SHIFT(P)  (((P) >> (MLIB_SHIFT - 2)) &~ 3)
#define MLIB_POINTER_GET(A, P) (*(DTYPE**)((mlib_addr)(A) + (P)))
#endif /* MLIB_OS64BIT */

#define PTR_SHIFT MLIB_POINTER_SHIFT

/***************************************************************/
#define SHIFT(X, SH, LO_BITS)                                   \
  (((X) >> (SH - LO_BITS)) & ((1 << (15 + LO_BITS)) - (1 << LO_BITS)))

/***************************************************************/
#define S_PTRl(Y, SH)                                           \
  (*(DTYPE**)((mlib_u8*)lineAddr + SHIFT(Y, SH, PBITS)))

#define S_PTR(Y) S_PTRl(Y, 16)

/***************************************************************/
#define AL_ADDR(sp, ind) (mlib_d64*)((mlib_addr)(sp + ind) &~ 7)

/***************************************************************/
#define FILTER_ELEM_BITS  3

/***************************************************************/
#define FILTER_SHIFT (MLIB_SHIFT - FILTER_BITS - FILTER_ELEM_BITS)
#define FILTER_SIZE  (1 << FILTER_BITS)
#define FILTER_MASK  ((FILTER_SIZE - 1) << FILTER_ELEM_BITS)

/***************************************************************/
#define DECLAREVAR0()                                           \
  mlib_s32  *leftEdges  = param -> leftEdges;                   \
  mlib_s32  *rightEdges = param -> rightEdges;                  \
  mlib_s32  *xStarts    = param -> xStarts;                     \
  mlib_s32  *yStarts    = param -> yStarts;                     \
  mlib_u8   *dstData    = param -> dstData;                     \
  mlib_u8   **lineAddr  = param -> lineAddr;                    \
  mlib_s32  dstYStride  = param -> dstYStride;                  \
  mlib_s32  xLeft, xRight, X, Y;                                \
  mlib_s32  yStart  = param -> yStart;                          \
  mlib_s32  yFinish = param -> yFinish;                         \
  mlib_s32  dX = param -> dX;                                   \
  mlib_s32  dY = param -> dY;                                   \
  mlib_s32  j

/***************************************************************/
#define DECLAREVAR()                                            \
  DECLAREVAR0();                                                \
  mlib_s32 *warp_tbl   = param -> warp_tbl;                     \
  DTYPE    *dstPixelPtr

/***************************************************************/
#define DECLAREVAR_NN()                                         \
  DECLAREVAR();                                                 \
  DTYPE    *srcPixelPtr;                                        \
  mlib_s32 xSrc, ySrc

/***************************************************************/
#define DECLAREVAR_BL()                                         \
  DECLAREVAR_NN();                                              \
  mlib_s32 srcYStride = param -> srcYStride

/***************************************************************/
#define DECLAREVAR_BC()                                         \
  DECLAREVAR_BL();                                              \
  mlib_filter filter = param -> filter

/***************************************************************/
#define PREPARE_DELTAS                                          \
  if (warp_tbl != NULL) {                                       \
    dX = warp_tbl[2*j];                                         \
    dY = warp_tbl[2*j + 1];                                     \
  }

/***************************************************************/
#define CLIP(N)                                                 \
  dstData += dstYStride;                                        \
  xLeft  = leftEdges[j];                                        \
  xRight = rightEdges[j];                                       \
  X = xStarts[j];                                               \
  Y = yStarts[j];                                               \
  PREPARE_DELTAS;                                               \
  if (xLeft > xRight) continue;                                 \
  dstPixelPtr = (DTYPE*)dstData + N * xLeft

/***************************************************************/
#define NEW_LINE(NCHAN)                                         \
  dstData += dstYStride;                                        \
  xLeft  = leftEdges[j];                                        \
  xRight = rightEdges[j];                                       \
  X = xStarts[j];                                               \
  Y = yStarts[j];                                               \
  PREPARE_DELTAS                                                \
  dl = (void*)((DTYPE*)dstData + NCHAN*xLeft);                  \
  size = xRight - xLeft + 1;                                    \
  if (size <= 0) continue

/***************************************************************/

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __MLIB_IMAGEAFFINE_H */
