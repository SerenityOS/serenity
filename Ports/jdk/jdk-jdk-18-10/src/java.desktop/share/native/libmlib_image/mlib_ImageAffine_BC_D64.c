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
 * FUNCTION
 *      Image affine transformation with Bicubic filtering
 * SYNOPSIS
 *      mlib_status mlib_ImageAffine_[s32|f32|d64]_?ch_bc(mlib_s32 *leftEdges,
 *                                                        mlib_s32 *rightEdges,
 *                                                        mlib_s32 *xStarts,
 *                                                        mlib_s32 *yStarts,
 *                                                        mlib_s32 *sides,
 *                                                        mlib_u8  *dstData,
 *                                                        mlib_u8  **lineAddr,
 *                                                        mlib_s32 dstYStride,
 *                                                        mlib_s32 is_affine,
 *                                                        mlib_s32 srcYStride,
 *                                                        mlib_filter filter)
 *
 *
 * ARGUMENTS
 *      leftEdges  array[dstHeight] of xLeft coordinates
 *      RightEdges array[dstHeight] of xRight coordinates
 *      xStarts    array[dstHeight] of xStart * 65536 coordinates
 *      yStarts    array[dstHeight] of yStart * 65536 coordinates
 *      sides      output array[4]. sides[0] is yStart, sides[1] is yFinish,
 *                 sides[2] is dx * 65536, sides[3] is dy * 65536
 *      dstData    pointer to the first pixel on (yStart - 1) line
 *      lineAddr   array[srcHeight] of pointers to the first pixel on
 *                 the corresponding lines
 *      dstYStride stride of destination image
 *      is_affine  indicator (Affine - GridWarp)
 *      srcYStride stride of source image
 *      filter     type of resampling filter
 *
 * DESCRIPTION
 *      The functions step along the lines from xLeft to xRight and apply
 *      the Bicubic and Bicubic2 filtering.
 *
 */

#include "mlib_ImageAffine.h"

#define IMG_TYPE  5

/***************************************************************/
#if IMG_TYPE == 3

#define DTYPE  mlib_s32
#define FTYPE  mlib_d64

#define FUN_NAME(CHAN) mlib_ImageAffine_s32_##CHAN##_bc

#define STORE(res, x) SAT32(res)

#elif IMG_TYPE == 4

#define DTYPE  mlib_f32
#define FTYPE  DTYPE

#define FUN_NAME(CHAN) mlib_ImageAffine_f32_##CHAN##_bc

#define STORE(res, x) res = (x)

#elif IMG_TYPE == 5

#define DTYPE  mlib_d64
#define FTYPE  DTYPE

#define FUN_NAME(CHAN) mlib_ImageAffine_d64_##CHAN##_bc

#define STORE(res, x) res = (x)

#endif /* IMG_TYPE == 3 */

/***************************************************************/
#define CREATE_COEF_BICUBIC( X, Y, OPERATOR )                   \
  dx = (X & MLIB_MASK) * scale;                                 \
  dy = (Y & MLIB_MASK) * scale;                                 \
  dx_2  = ((FTYPE)0.5)  * dx;                                   \
  dy_2  = ((FTYPE)0.5)  * dy;                                   \
  dx2   = dx   * dx;    dy2   = dy   * dy;                      \
  dx3_2 = dx_2 * dx2;   dy3_2 = dy_2 * dy2;                     \
  dx3_3 = ((FTYPE)3.0)  * dx3_2;                                \
  dy3_3 = ((FTYPE)3.0)  * dy3_2;                                \
                                                                \
  xf0 = dx2 - dx3_2 - dx_2;                                     \
  xf1 = dx3_3 - ((FTYPE)2.5) * dx2 + ((FTYPE)1.0);              \
  xf2 = ((FTYPE)2.0) * dx2 - dx3_3 + dx_2;                      \
  xf3 = dx3_2 - ((FTYPE)0.5) * dx2;                             \
                                                                \
  OPERATOR;                                                     \
                                                                \
  yf0 = dy2 - dy3_2 - dy_2;                                     \
  yf1 = dy3_3 - ((FTYPE)2.5) * dy2 + ((FTYPE)1.0);              \
  yf2 = ((FTYPE)2.0) * dy2 - dy3_3 + dy_2;                      \
  yf3 = dy3_2 - ((FTYPE)0.5) * dy2

/***************************************************************/
#define CREATE_COEF_BICUBIC_2( X, Y, OPERATOR )                 \
  dx = (X & MLIB_MASK) * scale;                                 \
  dy = (Y & MLIB_MASK) * scale;                                 \
  dx2   = dx  * dx;    dy2   = dy  * dy;                        \
  dx3_2 = dx  * dx2;   dy3_2 = dy  * dy2;                       \
  dx3_3 = ((FTYPE)2.0) * dx2;                                   \
  dy3_3 = ((FTYPE)2.0) * dy2;                                   \
                                                                \
  xf0 = dx3_3 - dx3_2 - dx;                                     \
  xf1 = dx3_2 - dx3_3 + ((FTYPE)1.0);                           \
  xf2 = dx2   - dx3_2   + dx;                                   \
  xf3 = dx3_2 - dx2;                                            \
                                                                \
  OPERATOR;                                                     \
                                                                \
  yf0 = dy3_3 - dy3_2 - dy;                                     \
  yf1 = dy3_2 - dy3_3 + ((FTYPE)1.0);                           \
  yf2 = dy2   - dy3_2   + dy;                                   \
  yf3 = dy3_2 - dy2

/***************************************************************/
mlib_status FUN_NAME(1ch)(mlib_affine_param *param)
{
  DECLAREVAR_BC();
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    FTYPE xf0, xf1, xf2, xf3;
    FTYPE yf0, yf1, yf2, yf3;
    FTYPE dx, dx_2, dx2, dx3_2, dx3_3;
    FTYPE dy, dy_2, dy2, dy3_2, dy3_3;
    FTYPE c0, c1, c2, c3, val0;
    FTYPE scale = 1 / 65536.f;
    FTYPE s0, s1, s2, s3;
    FTYPE s4, s5, s6, s7;

    CLIP(1);
    dstLineEnd = (DTYPE *) dstData + xRight;

    if (filter == MLIB_BICUBIC) {
      CREATE_COEF_BICUBIC(X, Y,;);
    }
    else {
      CREATE_COEF_BICUBIC_2(X, Y,;);
    }

    xSrc = (X >> MLIB_SHIFT) - 1;
    ySrc = (Y >> MLIB_SHIFT) - 1;

    srcPixelPtr = ((DTYPE **) lineAddr)[ySrc] + xSrc;
    s0 = srcPixelPtr[0];
    s1 = srcPixelPtr[1];
    s2 = srcPixelPtr[2];
    s3 = srcPixelPtr[3];

    srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
    s4 = srcPixelPtr[0];
    s5 = srcPixelPtr[1];
    s6 = srcPixelPtr[2];
    s7 = srcPixelPtr[3];

    if (filter == MLIB_BICUBIC) {
      for (; dstPixelPtr <= (dstLineEnd - 1); dstPixelPtr++) {
        X += dX;
        Y += dY;

        c0 = (s0 * xf0 + s1 * xf1 + s2 * xf2 + s3 * xf3);
        c1 = (s4 * xf0 + s5 * xf1 + s6 * xf2 + s7 * xf3);
        srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
        c2 = (srcPixelPtr[0] * xf0 + srcPixelPtr[1] * xf1 +
              srcPixelPtr[2] * xf2 + srcPixelPtr[3] * xf3);
        srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
        c3 = (srcPixelPtr[0] * xf0 + srcPixelPtr[1] * xf1 +
              srcPixelPtr[2] * xf2 + srcPixelPtr[3] * xf3);

        CREATE_COEF_BICUBIC(X, Y, val0 = (c0 * yf0 + c1 * yf1 + c2 * yf2 + c3 * yf3));

        STORE(dstPixelPtr[0], val0);

        xSrc = (X >> MLIB_SHIFT) - 1;
        ySrc = (Y >> MLIB_SHIFT) - 1;

        srcPixelPtr = ((DTYPE **) lineAddr)[ySrc] + xSrc;
        s0 = srcPixelPtr[0];
        s1 = srcPixelPtr[1];
        s2 = srcPixelPtr[2];
        s3 = srcPixelPtr[3];

        srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
        s4 = srcPixelPtr[0];
        s5 = srcPixelPtr[1];
        s6 = srcPixelPtr[2];
        s7 = srcPixelPtr[3];
      }

    }
    else {
      for (; dstPixelPtr <= (dstLineEnd - 1); dstPixelPtr++) {
        X += dX;
        Y += dY;

        c0 = (s0 * xf0 + s1 * xf1 + s2 * xf2 + s3 * xf3);
        c1 = (s4 * xf0 + s5 * xf1 + s6 * xf2 + s7 * xf3);
        srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
        c2 = (srcPixelPtr[0] * xf0 + srcPixelPtr[1] * xf1 +
              srcPixelPtr[2] * xf2 + srcPixelPtr[3] * xf3);
        srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
        c3 = (srcPixelPtr[0] * xf0 + srcPixelPtr[1] * xf1 +
              srcPixelPtr[2] * xf2 + srcPixelPtr[3] * xf3);

        CREATE_COEF_BICUBIC_2(X, Y, val0 = (c0 * yf0 + c1 * yf1 + c2 * yf2 + c3 * yf3));

        STORE(dstPixelPtr[0], val0);

        xSrc = (X >> MLIB_SHIFT) - 1;
        ySrc = (Y >> MLIB_SHIFT) - 1;

        srcPixelPtr = ((DTYPE **) lineAddr)[ySrc] + xSrc;
        s0 = srcPixelPtr[0];
        s1 = srcPixelPtr[1];
        s2 = srcPixelPtr[2];
        s3 = srcPixelPtr[3];

        srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
        s4 = srcPixelPtr[0];
        s5 = srcPixelPtr[1];
        s6 = srcPixelPtr[2];
        s7 = srcPixelPtr[3];
      }
    }

    c0 = (s0 * xf0 + s1 * xf1 + s2 * xf2 + s3 * xf3);
    c1 = (s4 * xf0 + s5 * xf1 + s6 * xf2 + s7 * xf3);
    srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
    c2 = (srcPixelPtr[0] * xf0 + srcPixelPtr[1] * xf1 +
          srcPixelPtr[2] * xf2 + srcPixelPtr[3] * xf3);
    srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
    c3 = (srcPixelPtr[0] * xf0 + srcPixelPtr[1] * xf1 +
          srcPixelPtr[2] * xf2 + srcPixelPtr[3] * xf3);

    val0 = (c0 * yf0 + c1 * yf1 + c2 * yf2 + c3 * yf3);
    STORE(dstPixelPtr[0], val0);
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status FUN_NAME(2ch)(mlib_affine_param *param)
{
  DECLAREVAR_BC();
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    FTYPE xf0, xf1, xf2, xf3;
    FTYPE yf0, yf1, yf2, yf3;
    FTYPE dx, dx_2, dx2, dx3_2, dx3_3;
    FTYPE dy, dy_2, dy2, dy3_2, dy3_3;
    FTYPE c0, c1, c2, c3, val0;
    FTYPE scale = 1 / 65536.f;
    FTYPE s0, s1, s2, s3;
    FTYPE s4, s5, s6, s7;
    mlib_s32 k;

    CLIP(2);
    dstLineEnd = (DTYPE *) dstData + 2 * xRight;

    for (k = 0; k < 2; k++) {
      mlib_s32 X1 = X;
      mlib_s32 Y1 = Y;
      DTYPE *dPtr = dstPixelPtr + k;

      if (filter == MLIB_BICUBIC) {
        CREATE_COEF_BICUBIC(X1, Y1,;);
      }
      else {
        CREATE_COEF_BICUBIC_2(X1, Y1,;);
      }

      xSrc = (X1 >> MLIB_SHIFT) - 1;
      ySrc = (Y1 >> MLIB_SHIFT) - 1;

      srcPixelPtr = ((DTYPE **) lineAddr)[ySrc] + 2 * xSrc + k;
      s0 = srcPixelPtr[0];
      s1 = srcPixelPtr[2];
      s2 = srcPixelPtr[4];
      s3 = srcPixelPtr[6];

      srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
      s4 = srcPixelPtr[0];
      s5 = srcPixelPtr[2];
      s6 = srcPixelPtr[4];
      s7 = srcPixelPtr[6];

      if (filter == MLIB_BICUBIC) {
        for (; dPtr <= (dstLineEnd - 1); dPtr += 2) {
          X1 += dX;
          Y1 += dY;

          c0 = (s0 * xf0 + s1 * xf1 + s2 * xf2 + s3 * xf3);
          c1 = (s4 * xf0 + s5 * xf1 + s6 * xf2 + s7 * xf3);
          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          c2 = (srcPixelPtr[0] * xf0 + srcPixelPtr[2] * xf1 +
                srcPixelPtr[4] * xf2 + srcPixelPtr[6] * xf3);
          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          c3 = (srcPixelPtr[0] * xf0 + srcPixelPtr[2] * xf1 +
                srcPixelPtr[4] * xf2 + srcPixelPtr[6] * xf3);

          CREATE_COEF_BICUBIC(X1, Y1, val0 = (c0 * yf0 + c1 * yf1 + c2 * yf2 + c3 * yf3));

          STORE(dPtr[0], val0);

          xSrc = (X1 >> MLIB_SHIFT) - 1;
          ySrc = (Y1 >> MLIB_SHIFT) - 1;

          srcPixelPtr = ((DTYPE **) lineAddr)[ySrc] + 2 * xSrc + k;
          s0 = srcPixelPtr[0];
          s1 = srcPixelPtr[2];
          s2 = srcPixelPtr[4];
          s3 = srcPixelPtr[6];

          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          s4 = srcPixelPtr[0];
          s5 = srcPixelPtr[2];
          s6 = srcPixelPtr[4];
          s7 = srcPixelPtr[6];
        }

      }
      else {
        for (; dPtr <= (dstLineEnd - 1); dPtr += 2) {
          X1 += dX;
          Y1 += dY;

          c0 = (s0 * xf0 + s1 * xf1 + s2 * xf2 + s3 * xf3);
          c1 = (s4 * xf0 + s5 * xf1 + s6 * xf2 + s7 * xf3);
          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          c2 = (srcPixelPtr[0] * xf0 + srcPixelPtr[2] * xf1 +
                srcPixelPtr[4] * xf2 + srcPixelPtr[6] * xf3);
          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          c3 = (srcPixelPtr[0] * xf0 + srcPixelPtr[2] * xf1 +
                srcPixelPtr[4] * xf2 + srcPixelPtr[6] * xf3);

          CREATE_COEF_BICUBIC_2(X1, Y1, val0 = (c0 * yf0 + c1 * yf1 + c2 * yf2 + c3 * yf3));

            STORE(dPtr[0], val0);

          xSrc = (X1 >> MLIB_SHIFT) - 1;
          ySrc = (Y1 >> MLIB_SHIFT) - 1;

          srcPixelPtr = ((DTYPE **) lineAddr)[ySrc] + 2 * xSrc + k;
          s0 = srcPixelPtr[0];
          s1 = srcPixelPtr[2];
          s2 = srcPixelPtr[4];
          s3 = srcPixelPtr[6];

          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          s4 = srcPixelPtr[0];
          s5 = srcPixelPtr[2];
          s6 = srcPixelPtr[4];
          s7 = srcPixelPtr[6];
        }
      }

      c0 = (s0 * xf0 + s1 * xf1 + s2 * xf2 + s3 * xf3);
      c1 = (s4 * xf0 + s5 * xf1 + s6 * xf2 + s7 * xf3);
      srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
      c2 = (srcPixelPtr[0] * xf0 + srcPixelPtr[2] * xf1 +
            srcPixelPtr[4] * xf2 + srcPixelPtr[6] * xf3);
      srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
      c3 = (srcPixelPtr[0] * xf0 + srcPixelPtr[2] * xf1 +
            srcPixelPtr[4] * xf2 + srcPixelPtr[6] * xf3);

      val0 = (c0 * yf0 + c1 * yf1 + c2 * yf2 + c3 * yf3);
      STORE(dPtr[0], val0);
    }
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status FUN_NAME(3ch)(mlib_affine_param *param)
{
  DECLAREVAR_BC();
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    FTYPE xf0, xf1, xf2, xf3;
    FTYPE yf0, yf1, yf2, yf3;
    FTYPE dx, dx_2, dx2, dx3_2, dx3_3;
    FTYPE dy, dy_2, dy2, dy3_2, dy3_3;
    FTYPE c0, c1, c2, c3, val0;
    FTYPE scale = 1 / 65536.f;
    FTYPE s0, s1, s2, s3;
    FTYPE s4, s5, s6, s7;
    mlib_s32 k;

    CLIP(3);
    dstLineEnd = (DTYPE *) dstData + 3 * xRight;

    for (k = 0; k < 3; k++) {
      mlib_s32 X1 = X;
      mlib_s32 Y1 = Y;
      DTYPE *dPtr = dstPixelPtr + k;

      if (filter == MLIB_BICUBIC) {
        CREATE_COEF_BICUBIC(X1, Y1,;);
      }
      else {
        CREATE_COEF_BICUBIC_2(X1, Y1,;);
      }

      xSrc = (X1 >> MLIB_SHIFT) - 1;
      ySrc = (Y1 >> MLIB_SHIFT) - 1;

      srcPixelPtr = ((DTYPE **) lineAddr)[ySrc] + 3 * xSrc + k;
      s0 = srcPixelPtr[0];
      s1 = srcPixelPtr[3];
      s2 = srcPixelPtr[6];
      s3 = srcPixelPtr[9];

      srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
      s4 = srcPixelPtr[0];
      s5 = srcPixelPtr[3];
      s6 = srcPixelPtr[6];
      s7 = srcPixelPtr[9];

      if (filter == MLIB_BICUBIC) {
        for (; dPtr <= (dstLineEnd - 1); dPtr += 3) {
          X1 += dX;
          Y1 += dY;

          c0 = (s0 * xf0 + s1 * xf1 + s2 * xf2 + s3 * xf3);
          c1 = (s4 * xf0 + s5 * xf1 + s6 * xf2 + s7 * xf3);
          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          c2 = (srcPixelPtr[0] * xf0 + srcPixelPtr[3] * xf1 +
                srcPixelPtr[6] * xf2 + srcPixelPtr[9] * xf3);
          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          c3 = (srcPixelPtr[0] * xf0 + srcPixelPtr[3] * xf1 +
                srcPixelPtr[6] * xf2 + srcPixelPtr[9] * xf3);

          CREATE_COEF_BICUBIC(X1, Y1, val0 = (c0 * yf0 + c1 * yf1 + c2 * yf2 + c3 * yf3));

          STORE(dPtr[0], val0);

          xSrc = (X1 >> MLIB_SHIFT) - 1;
          ySrc = (Y1 >> MLIB_SHIFT) - 1;

          srcPixelPtr = ((DTYPE **) lineAddr)[ySrc] + 3 * xSrc + k;
          s0 = srcPixelPtr[0];
          s1 = srcPixelPtr[3];
          s2 = srcPixelPtr[6];
          s3 = srcPixelPtr[9];

          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          s4 = srcPixelPtr[0];
          s5 = srcPixelPtr[3];
          s6 = srcPixelPtr[6];
          s7 = srcPixelPtr[9];
        }

      }
      else {
        for (; dPtr <= (dstLineEnd - 1); dPtr += 3) {
          X1 += dX;
          Y1 += dY;

          c0 = (s0 * xf0 + s1 * xf1 + s2 * xf2 + s3 * xf3);
          c1 = (s4 * xf0 + s5 * xf1 + s6 * xf2 + s7 * xf3);
          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          c2 = (srcPixelPtr[0] * xf0 + srcPixelPtr[3] * xf1 +
                srcPixelPtr[6] * xf2 + srcPixelPtr[9] * xf3);
          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          c3 = (srcPixelPtr[0] * xf0 + srcPixelPtr[3] * xf1 +
                srcPixelPtr[6] * xf2 + srcPixelPtr[9] * xf3);

          CREATE_COEF_BICUBIC_2(X1, Y1, val0 = (c0 * yf0 + c1 * yf1 + c2 * yf2 + c3 * yf3));

            STORE(dPtr[0], val0);

          xSrc = (X1 >> MLIB_SHIFT) - 1;
          ySrc = (Y1 >> MLIB_SHIFT) - 1;

          srcPixelPtr = ((DTYPE **) lineAddr)[ySrc] + 3 * xSrc + k;
          s0 = srcPixelPtr[0];
          s1 = srcPixelPtr[3];
          s2 = srcPixelPtr[6];
          s3 = srcPixelPtr[9];

          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          s4 = srcPixelPtr[0];
          s5 = srcPixelPtr[3];
          s6 = srcPixelPtr[6];
          s7 = srcPixelPtr[9];
        }
      }

      c0 = (s0 * xf0 + s1 * xf1 + s2 * xf2 + s3 * xf3);
      c1 = (s4 * xf0 + s5 * xf1 + s6 * xf2 + s7 * xf3);
      srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
      c2 = (srcPixelPtr[0] * xf0 + srcPixelPtr[3] * xf1 +
            srcPixelPtr[6] * xf2 + srcPixelPtr[9] * xf3);
      srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
      c3 = (srcPixelPtr[0] * xf0 + srcPixelPtr[3] * xf1 +
            srcPixelPtr[6] * xf2 + srcPixelPtr[9] * xf3);

      val0 = (c0 * yf0 + c1 * yf1 + c2 * yf2 + c3 * yf3);
      STORE(dPtr[0], val0);
    }
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status FUN_NAME(4ch)(mlib_affine_param *param)
{
  DECLAREVAR_BC();
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    FTYPE xf0, xf1, xf2, xf3;
    FTYPE yf0, yf1, yf2, yf3;
    FTYPE dx, dx_2, dx2, dx3_2, dx3_3;
    FTYPE dy, dy_2, dy2, dy3_2, dy3_3;
    FTYPE c0, c1, c2, c3, val0;
    FTYPE scale = 1 / 65536.f;
    FTYPE s0, s1, s2, s3;
    FTYPE s4, s5, s6, s7;
    mlib_s32 k;

    CLIP(4);
    dstLineEnd = (DTYPE *) dstData + 4 * xRight;

    for (k = 0; k < 4; k++) {
      mlib_s32 X1 = X;
      mlib_s32 Y1 = Y;
      DTYPE *dPtr = dstPixelPtr + k;

      if (filter == MLIB_BICUBIC) {
        CREATE_COEF_BICUBIC(X1, Y1,;);
      }
      else {
        CREATE_COEF_BICUBIC_2(X1, Y1,;);
      }

      xSrc = (X1 >> MLIB_SHIFT) - 1;
      ySrc = (Y1 >> MLIB_SHIFT) - 1;

      srcPixelPtr = ((DTYPE **) lineAddr)[ySrc] + 4 * xSrc + k;
      s0 = srcPixelPtr[0];
      s1 = srcPixelPtr[4];
      s2 = srcPixelPtr[8];
      s3 = srcPixelPtr[12];

      srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
      s4 = srcPixelPtr[0];
      s5 = srcPixelPtr[4];
      s6 = srcPixelPtr[8];
      s7 = srcPixelPtr[12];

      if (filter == MLIB_BICUBIC) {
        for (; dPtr <= (dstLineEnd - 1); dPtr += 4) {

          X1 += dX;
          Y1 += dY;

          c0 = (s0 * xf0 + s1 * xf1 + s2 * xf2 + s3 * xf3);
          c1 = (s4 * xf0 + s5 * xf1 + s6 * xf2 + s7 * xf3);
          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          c2 = (srcPixelPtr[0] * xf0 + srcPixelPtr[4] * xf1 +
                srcPixelPtr[8] * xf2 + srcPixelPtr[12] * xf3);
          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          c3 = (srcPixelPtr[0] * xf0 + srcPixelPtr[4] * xf1 +
                srcPixelPtr[8] * xf2 + srcPixelPtr[12] * xf3);

          CREATE_COEF_BICUBIC(X1, Y1, val0 = (c0 * yf0 + c1 * yf1 + c2 * yf2 + c3 * yf3));

          STORE(dPtr[0], val0);

          xSrc = (X1 >> MLIB_SHIFT) - 1;
          ySrc = (Y1 >> MLIB_SHIFT) - 1;

          srcPixelPtr = ((DTYPE **) lineAddr)[ySrc] + 4 * xSrc + k;
          s0 = srcPixelPtr[0];
          s1 = srcPixelPtr[4];
          s2 = srcPixelPtr[8];
          s3 = srcPixelPtr[12];

          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          s4 = srcPixelPtr[0];
          s5 = srcPixelPtr[4];
          s6 = srcPixelPtr[8];
          s7 = srcPixelPtr[12];
        }

      }
      else {
        for (; dPtr <= (dstLineEnd - 1); dPtr += 4) {

          X1 += dX;
          Y1 += dY;

          c0 = (s0 * xf0 + s1 * xf1 + s2 * xf2 + s3 * xf3);
          c1 = (s4 * xf0 + s5 * xf1 + s6 * xf2 + s7 * xf3);
          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          c2 = (srcPixelPtr[0] * xf0 + srcPixelPtr[4] * xf1 +
                srcPixelPtr[8] * xf2 + srcPixelPtr[12] * xf3);
          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          c3 = (srcPixelPtr[0] * xf0 + srcPixelPtr[4] * xf1 +
                srcPixelPtr[8] * xf2 + srcPixelPtr[12] * xf3);

          CREATE_COEF_BICUBIC_2(X1, Y1, val0 = (c0 * yf0 + c1 * yf1 + c2 * yf2 + c3 * yf3));

            STORE(dPtr[0], val0);

          xSrc = (X1 >> MLIB_SHIFT) - 1;
          ySrc = (Y1 >> MLIB_SHIFT) - 1;

          srcPixelPtr = ((DTYPE **) lineAddr)[ySrc] + 4 * xSrc + k;
          s0 = srcPixelPtr[0];
          s1 = srcPixelPtr[4];
          s2 = srcPixelPtr[8];
          s3 = srcPixelPtr[12];

          srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
          s4 = srcPixelPtr[0];
          s5 = srcPixelPtr[4];
          s6 = srcPixelPtr[8];
          s7 = srcPixelPtr[12];
        }
      }

      c0 = (s0 * xf0 + s1 * xf1 + s2 * xf2 + s3 * xf3);
      c1 = (s4 * xf0 + s5 * xf1 + s6 * xf2 + s7 * xf3);
      srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
      c2 = (srcPixelPtr[0] * xf0 + srcPixelPtr[4] * xf1 +
            srcPixelPtr[8] * xf2 + srcPixelPtr[12] * xf3);
      srcPixelPtr = (DTYPE *) ((mlib_u8 *) srcPixelPtr + srcYStride);
      c3 = (srcPixelPtr[0] * xf0 + srcPixelPtr[4] * xf1 +
            srcPixelPtr[8] * xf2 + srcPixelPtr[12] * xf3);

      val0 = (c0 * yf0 + c1 * yf1 + c2 * yf2 + c3 * yf3);
      STORE(dPtr[0], val0);
    }
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
