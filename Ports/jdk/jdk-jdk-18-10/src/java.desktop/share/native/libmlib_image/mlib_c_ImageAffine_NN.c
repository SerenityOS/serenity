/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *      mlib_ImageAffine_u8_1ch_nn
 *      mlib_ImageAffine_u8_2ch_nn
 *      mlib_ImageAffine_u8_3ch_nn
 *      mlib_ImageAffine_u8_4ch_nn
 *      mlib_ImageAffine_s16_1ch_nn
 *      mlib_ImageAffine_s16_2ch_nn
 *      mlib_ImageAffine_s16_3ch_nn
 *      mlib_ImageAffine_s16_4ch_nn
 *        - image affine transformation with Nearest Neighbor filtering
 * SYNOPSIS
 *      mlib_status mlib_ImageAffine_[u8|s16]_?ch_nn(mlib_s32 *leftEdges,
 *                                                   mlib_s32 *rightEdges,
 *                                                   mlib_s32 *xStarts,
 *                                                   mlib_s32 *yStarts,
 *                                                   mlib_s32 *sides,
 *                                                   mlib_u8  *dstData,
 *                                                   mlib_u8  **lineAddr,
 *                                                   mlib_s32 dstYStride,
 *                                                   mlib_s32 is_affine)
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
 *
 * DESCRIPTION
 *      The functions step along the lines from xLeft to xRight and get the
 *      nearest pixel values as being with the following coordinates
 *      ((xStart - (i - xLeft) * dx) >> 16, (yStart - (i - xLeft) * dy) >> 16)
 *
 */

#include "mlib_ImageAffine.h"

/***************************************************************/
#undef  DTYPE
#define DTYPE mlib_u8

mlib_status mlib_ImageAffine_u8_1ch_nn(mlib_affine_param *param)
{
  DECLAREVAR_NN();
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    DTYPE pix0;

    CLIP(1);
    dstLineEnd = (DTYPE *) dstData + xRight;

    for (; dstPixelPtr <= dstLineEnd; dstPixelPtr++) {
      ySrc = MLIB_POINTER_SHIFT(Y);
      Y += dY;
      srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc);
      xSrc = X >> MLIB_SHIFT;
      X += dX;
      pix0 = srcPixelPtr[xSrc];
      dstPixelPtr[0] = pix0;
    }
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageAffine_u8_2ch_nn(mlib_affine_param *param)
{
  DECLAREVAR_NN();
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    DTYPE pix0, pix1;

    CLIP(2);
    dstLineEnd = (DTYPE *) dstData + 2 * xRight;

    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;
    srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc) + 2 * xSrc;
    pix0 = srcPixelPtr[0];
    pix1 = srcPixelPtr[1];
    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;
    for (; dstPixelPtr < dstLineEnd; dstPixelPtr += 2) {
      srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc) + 2 * xSrc;
      ySrc = MLIB_POINTER_SHIFT(Y);
      Y += dY;
      xSrc = X >> MLIB_SHIFT;
      X += dX;
      dstPixelPtr[0] = pix0;
      dstPixelPtr[1] = pix1;
      pix0 = srcPixelPtr[0];
      pix1 = srcPixelPtr[1];
    }

    dstPixelPtr[0] = pix0;
    dstPixelPtr[1] = pix1;
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageAffine_u8_3ch_nn(mlib_affine_param *param)
{
  DECLAREVAR_NN();
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    DTYPE pix0, pix1, pix2;

    CLIP(3);
    dstLineEnd = (DTYPE *) dstData + 3 * xRight;

    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;
    srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc) + 3 * xSrc;
    pix0 = srcPixelPtr[0];
    pix1 = srcPixelPtr[1];
    pix2 = srcPixelPtr[2];
    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;
    for (; dstPixelPtr < dstLineEnd; dstPixelPtr += 3) {
      srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc) + 3 * xSrc;
      ySrc = MLIB_POINTER_SHIFT(Y);
      Y += dY;
      xSrc = X >> MLIB_SHIFT;
      X += dX;
      dstPixelPtr[0] = pix0;
      dstPixelPtr[1] = pix1;
      dstPixelPtr[2] = pix2;
      pix0 = srcPixelPtr[0];
      pix1 = srcPixelPtr[1];
      pix2 = srcPixelPtr[2];
    }

    dstPixelPtr[0] = pix0;
    dstPixelPtr[1] = pix1;
    dstPixelPtr[2] = pix2;
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageAffine_u8_4ch_nn(mlib_affine_param *param)
{
  DECLAREVAR_NN();
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    DTYPE pix0, pix1, pix2, pix3;
    CLIP(4);
    dstLineEnd = (DTYPE *) dstData + 4 * xRight;

    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;
    srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc) + 4 * xSrc;
    pix0 = srcPixelPtr[0];
    pix1 = srcPixelPtr[1];
    pix2 = srcPixelPtr[2];
    pix3 = srcPixelPtr[3];
    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;
    for (; dstPixelPtr < dstLineEnd; dstPixelPtr += 4) {
      srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc) + 4 * xSrc;
      ySrc = MLIB_POINTER_SHIFT(Y);
      Y += dY;
      xSrc = X >> MLIB_SHIFT;
      X += dX;
      dstPixelPtr[0] = pix0;
      dstPixelPtr[1] = pix1;
      dstPixelPtr[2] = pix2;
      dstPixelPtr[3] = pix3;
      pix0 = srcPixelPtr[0];
      pix1 = srcPixelPtr[1];
      pix2 = srcPixelPtr[2];
      pix3 = srcPixelPtr[3];
    }

    dstPixelPtr[0] = pix0;
    dstPixelPtr[1] = pix1;
    dstPixelPtr[2] = pix2;
    dstPixelPtr[3] = pix3;
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
#undef  DTYPE
#define DTYPE mlib_u16

mlib_status mlib_ImageAffine_s16_1ch_nn(mlib_affine_param *param)
{
  DECLAREVAR_NN();
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    mlib_s32 pix0;

    CLIP(1);
    dstLineEnd = (DTYPE *) dstData + xRight;

    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;
    srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc);
    pix0 = srcPixelPtr[xSrc];
    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    for (; dstPixelPtr < dstLineEnd; dstPixelPtr++) {
      xSrc = X >> MLIB_SHIFT;
      X += dX;
      srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc);
      dstPixelPtr[0] = pix0;
      ySrc = MLIB_POINTER_SHIFT(Y);
      Y += dY;
      pix0 = srcPixelPtr[xSrc];
    }

    dstPixelPtr[0] = pix0;
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageAffine_s16_2ch_nn(mlib_affine_param *param)
{
  DECLAREVAR_NN();
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    mlib_s32 pix0, pix1;

    CLIP(2);
    dstLineEnd = (DTYPE *) dstData + 2 * xRight;

    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;
    srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc) + 2 * xSrc;
    pix0 = srcPixelPtr[0];
    pix1 = srcPixelPtr[1];
    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;

    for (; dstPixelPtr < dstLineEnd; dstPixelPtr += 2) {
      srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc) + 2 * xSrc;
      ySrc = MLIB_POINTER_SHIFT(Y);
      Y += dY;
      xSrc = X >> MLIB_SHIFT;
      X += dX;
      dstPixelPtr[0] = pix0;
      dstPixelPtr[1] = pix1;
      pix0 = srcPixelPtr[0];
      pix1 = srcPixelPtr[1];
    }

    dstPixelPtr[0] = pix0;
    dstPixelPtr[1] = pix1;
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageAffine_s16_3ch_nn(mlib_affine_param *param)
{
  DECLAREVAR_NN();
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    mlib_s32 pix0, pix1, pix2;

    CLIP(3);
    dstLineEnd = (DTYPE *) dstData + 3 * xRight;

    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;
    srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc) + 3 * xSrc;
    pix0 = srcPixelPtr[0];
    pix1 = srcPixelPtr[1];
    pix2 = srcPixelPtr[2];
    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;
    for (; dstPixelPtr < dstLineEnd; dstPixelPtr += 3) {
      srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc) + 3 * xSrc;
      ySrc = MLIB_POINTER_SHIFT(Y);
      Y += dY;
      xSrc = X >> MLIB_SHIFT;
      X += dX;
      dstPixelPtr[0] = pix0;
      dstPixelPtr[1] = pix1;
      dstPixelPtr[2] = pix2;
      pix0 = srcPixelPtr[0];
      pix1 = srcPixelPtr[1];
      pix2 = srcPixelPtr[2];
    }

    dstPixelPtr[0] = pix0;
    dstPixelPtr[1] = pix1;
    dstPixelPtr[2] = pix2;
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageAffine_s16_4ch_nn(mlib_affine_param *param)
{
  DECLAREVAR_NN();
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    mlib_s32 pix0, pix1, pix2, pix3;
    CLIP(4);
    dstLineEnd = (DTYPE *) dstData + 4 * xRight;

    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;
    srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc) + 4 * xSrc;
    pix0 = srcPixelPtr[0];
    pix1 = srcPixelPtr[1];
    pix2 = srcPixelPtr[2];
    pix3 = srcPixelPtr[3];
    ySrc = MLIB_POINTER_SHIFT(Y);
    Y += dY;
    xSrc = X >> MLIB_SHIFT;
    X += dX;
    for (; dstPixelPtr < dstLineEnd; dstPixelPtr += 4) {
      srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc) + 4 * xSrc;
      ySrc = MLIB_POINTER_SHIFT(Y);
      Y += dY;
      xSrc = X >> MLIB_SHIFT;
      X += dX;
      dstPixelPtr[0] = pix0;
      dstPixelPtr[1] = pix1;
      dstPixelPtr[2] = pix2;
      dstPixelPtr[3] = pix3;
      pix0 = srcPixelPtr[0];
      pix1 = srcPixelPtr[1];
      pix2 = srcPixelPtr[2];
      pix3 = srcPixelPtr[3];
    }

    dstPixelPtr[0] = pix0;
    dstPixelPtr[1] = pix1;
    dstPixelPtr[2] = pix2;
    dstPixelPtr[3] = pix3;
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
