/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
#define DECLAREVAR_BIT()                                        \
  DECLAREVAR0();                                                \
  mlib_s32 ySrc;                                                \
  DTYPE *srcPixelPtr;                                           \
  DTYPE *srcPixelPtr0;                                          \
  DTYPE *srcPixelPtr1;                                          \
  DTYPE *srcPixelPtr2;                                          \
  DTYPE *srcPixelPtr3;                                          \
  DTYPE *srcPixelPtr4;                                          \
  DTYPE *srcPixelPtr5;                                          \
  DTYPE *srcPixelPtr6;                                          \
  DTYPE *srcPixelPtr7

/***************************************************************/
#define CLIP_BIT()                                              \
  dstData += dstYStride;                                        \
  xLeft  = leftEdges[j]  + d_bitoff;                            \
  xRight = rightEdges[j] + d_bitoff;                            \
  X = xStarts[j] + (s_bitoff << MLIB_SHIFT);                    \
  Y = yStarts[j];                                               \
  if (xLeft > xRight) continue

/***************************************************************/
#define DTYPE mlib_u8

void mlib_ImageAffine_bit_1ch_nn(mlib_affine_param *param,
                                 mlib_s32          s_bitoff,
                                 mlib_s32          d_bitoff)
{
  DECLAREVAR_BIT();
  mlib_s32 i, bit, res;

  for (j = yStart; j <= yFinish; j++) {

    CLIP_BIT();
    xRight++;

    i = xLeft;

    if (i & 7) {
      mlib_u8 *dp = dstData + (i >> 3);
      mlib_s32 res = dp[0];
      mlib_s32 i_end = i + (8 - (i & 7));

      if (i_end > xRight)
        i_end = xRight;

      for (; i < i_end; i++) {
        bit = 7 - (i & 7);
        ySrc = MLIB_POINTER_SHIFT(Y);
        srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc);

        res = (res & ~(1 << bit)) | (((srcPixelPtr[X >> (MLIB_SHIFT + 3)] >> (7 - ((X >> MLIB_SHIFT) & 7))) & 1) << bit);

        X += dX;
        Y += dY;
      }

      dp[0] = res;
    }

    for (; i <= (xRight - 8); i += 8) {
      srcPixelPtr0 = MLIB_POINTER_GET(lineAddr, MLIB_POINTER_SHIFT(Y));
      Y += dY;
      res = ((srcPixelPtr0[X >> (MLIB_SHIFT + 3)] << (((X >> MLIB_SHIFT)) & 7)) & 0x0080);
      X += dX;

      srcPixelPtr1 = MLIB_POINTER_GET(lineAddr, MLIB_POINTER_SHIFT(Y));
      Y += dY;
      res |= ((srcPixelPtr1[X >> (MLIB_SHIFT + 3)] << (((X >> MLIB_SHIFT) - 1) & 7)) & 0x4040);
      X += dX;

      srcPixelPtr2 = MLIB_POINTER_GET(lineAddr, MLIB_POINTER_SHIFT(Y));
      Y += dY;
      res |= ((srcPixelPtr2[X >> (MLIB_SHIFT + 3)] << (((X >> MLIB_SHIFT) - 2) & 7)) & 0x2020);
      X += dX;

      srcPixelPtr3 = MLIB_POINTER_GET(lineAddr, MLIB_POINTER_SHIFT(Y));
      Y += dY;
      res |= ((srcPixelPtr3[X >> (MLIB_SHIFT + 3)] << (((X >> MLIB_SHIFT) - 3) & 7)) & 0x1010);
      X += dX;

      srcPixelPtr4 = MLIB_POINTER_GET(lineAddr, MLIB_POINTER_SHIFT(Y));
      Y += dY;
      res |= ((srcPixelPtr4[X >> (MLIB_SHIFT + 3)] << (((X >> MLIB_SHIFT) - 4) & 7)) & 0x0808);
      X += dX;

      srcPixelPtr5 = MLIB_POINTER_GET(lineAddr, MLIB_POINTER_SHIFT(Y));
      Y += dY;
      res |= ((srcPixelPtr5[X >> (MLIB_SHIFT + 3)] << (((X >> MLIB_SHIFT) - 5) & 7)) & 0x0404);
      X += dX;

      srcPixelPtr6 = MLIB_POINTER_GET(lineAddr, MLIB_POINTER_SHIFT(Y));
      Y += dY;
      res |= ((srcPixelPtr6[X >> (MLIB_SHIFT + 3)] << (((X >> MLIB_SHIFT) - 6) & 7)) & 0x0202);
      X += dX;

      srcPixelPtr7 = MLIB_POINTER_GET(lineAddr, MLIB_POINTER_SHIFT(Y));
      Y += dY;
      res |= ((srcPixelPtr7[X >> (MLIB_SHIFT + 3)] >> (7 - ((X >> MLIB_SHIFT) & 7))) & 0x0001);
      X += dX;

      dstData[i >> 3] = res | (res >> 8);
    }

    if (i < xRight) {
      mlib_u8 *dp = dstData + (i >> 3);
      mlib_s32 res = dp[0];

      for (; i < xRight; i++) {
        bit = 7 - (i & 7);
        ySrc = MLIB_POINTER_SHIFT(Y);
        srcPixelPtr = MLIB_POINTER_GET(lineAddr, ySrc);

        res = (res & ~(1 << bit)) | (((srcPixelPtr[X >> (MLIB_SHIFT + 3)] >> (7 - ((X >> MLIB_SHIFT) & 7))) & 1) << bit);

        X += dX;
        Y += dY;
      }

      dp[0] = res;
    }
  }
}

/***************************************************************/
