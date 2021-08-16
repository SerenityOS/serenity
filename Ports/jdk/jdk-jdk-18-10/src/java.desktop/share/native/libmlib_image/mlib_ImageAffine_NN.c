/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *      Internal functions for mlib_ImageAffine with Nearest Neighbor filtering.
 */

#include "mlib_ImageAffine.h"

/***************************************************************/
#define sp  srcPixelPtr
#define dp  dstPixelPtr

/***************************************************************/
#undef  DTYPE
#define DTYPE mlib_s32

#ifdef _MSC_VER
/* Workaround for MSC optimizer bug (known affected versions
   12.00.8168-12.00.8804). See bug 4893435 for details. */
#pragma optimize("gs", off)
#endif /* _MSC_VER */
#ifdef i386 /* do not perform the coping by mlib_d64 data type for x86 */

mlib_status mlib_ImageAffine_s32_1ch_nn(mlib_affine_param *param)
{
  DECLAREVAR();
  DTYPE *srcPixelPtr;
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    CLIP(1);
    dstLineEnd = (DTYPE *) dstData + xRight;

    for (; dp <= dstLineEnd; dp++) {
      sp = S_PTR(Y) + (X >> MLIB_SHIFT);
      dp[0] = sp[0];

      X += dX;
      Y += dY;
    }
  }

  return MLIB_SUCCESS;
}

#else

mlib_status mlib_ImageAffine_s32_1ch_nn(mlib_affine_param *param)
{
  DECLAREVAR();
  DTYPE *srcPixelPtr;
  mlib_s32 i, size;

  for (j = yStart; j <= yFinish; j++) {
    d64_2x32 dd;

    CLIP(1);
    size = xRight - xLeft + 1;

    if ((mlib_addr) dp & 7) {
      sp = S_PTR(Y);
      *dp++ = sp[X >> MLIB_SHIFT];
      X += dX;
      Y += dY;
      size--;
    }

    for (i = 0; i <= (size - 2); i += 2) {
      mlib_f32 *sp0, *sp1;

      sp0 = (mlib_f32 *) S_PTR(Y);
      sp1 = (mlib_f32 *) S_PTR(Y + dY);

      dd.f32s.f0 = sp0[X >> MLIB_SHIFT];
      dd.f32s.f1 = sp1[(X + dX) >> MLIB_SHIFT];

      *(mlib_d64 *) dp = dd.d64;

      dp += 2;
      X += 2 * dX;
      Y += 2 * dY;
    }

    if (size & 1) {
      sp = S_PTR(Y);
      *dp = sp[X >> MLIB_SHIFT];
    }
  }

  return MLIB_SUCCESS;
}

#endif /* i386 ( do not perform the coping by mlib_d64 data type for x86 ) */

/***************************************************************/
mlib_status mlib_ImageAffine_s32_2ch_nn(mlib_affine_param *param)
{
  DECLAREVAR();
  DTYPE *srcPixelPtr;
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    CLIP(2);
    dstLineEnd = (DTYPE *) dstData + 2 * xRight;

    for (; dp <= dstLineEnd; dp += 2) {
      sp = S_PTR(Y) + 2 * (X >> MLIB_SHIFT);
      dp[0] = sp[0];
      dp[1] = sp[1];

      X += dX;
      Y += dY;
    }
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageAffine_s32_3ch_nn(mlib_affine_param *param)
{
  DECLAREVAR();
  DTYPE *srcPixelPtr;
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    CLIP(3);
    dstLineEnd = (DTYPE *) dstData + 3 * xRight;

    for (; dp <= dstLineEnd; dp += 3) {
      sp = S_PTR(Y) + 3 * (X >> MLIB_SHIFT);
      dp[0] = sp[0];
      dp[1] = sp[1];
      dp[2] = sp[2];

      X += dX;
      Y += dY;
    }
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageAffine_s32_4ch_nn(mlib_affine_param *param)
{
  DECLAREVAR();
  DTYPE *srcPixelPtr;
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    CLIP(4);
    dstLineEnd = (DTYPE *) dstData + 4 * xRight;

    for (; dp <= dstLineEnd; dp += 4) {
      sp = S_PTR(Y) + 4 * (X >> MLIB_SHIFT);
      dp[0] = sp[0];
      dp[1] = sp[1];
      dp[2] = sp[2];
      dp[3] = sp[3];

      X += dX;
      Y += dY;
    }
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
#undef  DTYPE
#define DTYPE mlib_d64

mlib_status mlib_ImageAffine_d64_1ch_nn(mlib_affine_param *param)
{
  DECLAREVAR();
  DTYPE *srcPixelPtr;
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    CLIP(1);
    dstLineEnd = (DTYPE *) dstData + xRight;

    for (; dp <= dstLineEnd; dp++) {
      sp = S_PTR(Y);
      dp[0] = sp[X >> MLIB_SHIFT];

      X += dX;
      Y += dY;
    }
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageAffine_d64_2ch_nn(mlib_affine_param *param)
{
  DECLAREVAR();
  DTYPE *srcPixelPtr;
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    CLIP(2);
    dstLineEnd = (DTYPE *) dstData + 2 * xRight;

    for (; dp <= dstLineEnd; dp += 2) {
      sp = S_PTR(Y) + 2 * (X >> MLIB_SHIFT);
      dp[0] = sp[0];
      dp[1] = sp[1];

      X += dX;
      Y += dY;
    }
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageAffine_d64_3ch_nn(mlib_affine_param *param)
{
  DECLAREVAR();
  DTYPE *srcPixelPtr;
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    CLIP(3);
    dstLineEnd = (DTYPE *) dstData + 3 * xRight;

    for (; dp <= dstLineEnd; dp += 3) {
      sp = S_PTR(Y) + 3 * (X >> MLIB_SHIFT);
      dp[0] = sp[0];
      dp[1] = sp[1];
      dp[2] = sp[2];

      X += dX;
      Y += dY;
    }
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageAffine_d64_4ch_nn(mlib_affine_param *param)
{
  DECLAREVAR();
  DTYPE *srcPixelPtr;
  DTYPE *dstLineEnd;

  for (j = yStart; j <= yFinish; j++) {
    CLIP(4);
    dstLineEnd = (DTYPE *) dstData + 4 * xRight;

    for (; dp <= dstLineEnd; dp += 4) {
      sp = S_PTR(Y) + 4 * (X >> MLIB_SHIFT);
      dp[0] = sp[0];
      dp[1] = sp[1];
      dp[2] = sp[2];
      dp[3] = sp[3];

      X += dX;
      Y += dY;
    }
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
#ifdef _MSC_VER
/* Workaround for MSC optimizer bug (known affected versions
   12.00.8168-12.00.8804). See bug 4893435 for details. */
#pragma optimize("gs", on)
#endif /* _MSC_VER */
