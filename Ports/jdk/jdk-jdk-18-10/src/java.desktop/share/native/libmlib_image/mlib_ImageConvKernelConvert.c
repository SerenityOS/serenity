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
 *      mlib_ImageConvKernelConvert - Convert convolution kernel from
 *                                    floating point version to integer
 *                                    version.
 *
 * SYNOPSIS
 *      mlib_status mlib_ImageConvKernelConvert(mlib_s32       *ikernel,
 *                                              mlib_s32       *iscale,
 *                                              const mlib_d64 *fkernel,
 *                                              mlib_s32       m,
 *                                              mlib_s32       n,
 *                                              mlib_type      type);
 *
 * ARGUMENT
 *      ikernel  integer kernel
 *      iscale   scaling factor of the integer kernel
 *      fkernel  floating-point kernel
 *      m        width of the convolution kernel
 *      n        height of the convolution kernel
 *      type     image type
 *
 * DESCRIPTION
 *      Convert a floating point convolution kernel to integer kernel
 *      with scaling factor. The result integer kernel and scaling factor
 *      can be used in convolution functions directly without overflow.
 *
 * RESTRICTION
 *      The type can be MLIB_BYTE, MLIB_SHORT, MLIB_USHORT or MLIB_INT.
 */

#include <stdlib.h>
#include "mlib_image.h"
#include "mlib_SysMath.h"
#include "mlib_ImageConv.h"

/***************************************************************/

#define CLAMP_S32(dst, src) {                                   \
  mlib_d64 s0 = (mlib_d64)(src);                                \
  if (s0 > (mlib_d64)MLIB_S32_MAX) s0 = (mlib_d64)MLIB_S32_MAX; \
  if (s0 < (mlib_d64)MLIB_S32_MIN) s0 = (mlib_d64)MLIB_S32_MIN; \
  dst = (mlib_s32)s0;                                           \
}

/***************************************************************/
JNIEXPORT
mlib_status mlib_ImageConvKernelConvert(mlib_s32       *ikernel,
                                        mlib_s32       *iscale,
                                        const mlib_d64 *fkernel,
                                        mlib_s32       m,
                                        mlib_s32       n,
                                        mlib_type      type)
{
  mlib_d64 sum_pos, sum_neg, sum, norm, max, f;
  mlib_s32 isum_pos, isum_neg, isum, test;
  mlib_s32 i, scale, scale1, chk_flag;

  if (ikernel == NULL || iscale == NULL || fkernel == NULL || m < 1 || n < 1) {
    return MLIB_FAILURE;
  }

  if ((type == MLIB_BYTE) || (type == MLIB_SHORT) || (type == MLIB_USHORT)) {

    if (type != MLIB_SHORT) {               /* MLIB_BYTE, MLIB_USHORT */
      sum_pos = 0;
      sum_neg = 0;

      for (i = 0; i < m * n; i++) {
        if (fkernel[i] > 0)
          sum_pos += fkernel[i];
        else
          sum_neg -= fkernel[i];
      }

      sum = (sum_pos > sum_neg) ? sum_pos : sum_neg;
      scale = mlib_ilogb(sum);
      scale++;

      scale = 31 - scale;
    }
    else {                                  /* MLIB_SHORT */
      sum = 0;
      max = 0;

      for (i = 0; i < m * n; i++) {
        f = mlib_fabs(fkernel[i]);
        sum += f;
        max = (max > f) ? max : f;
      }

      scale1 = mlib_ilogb(max) + 1;
      scale = mlib_ilogb(sum);
      scale = (scale > scale1) ? scale : scale1;
      scale++;

      scale = 32 - scale;
    }

    if (scale <= 16)
      return MLIB_FAILURE;
    if (scale > 31)
      scale = 31;

    *iscale = scale;

    chk_flag = mlib_ImageConvVersion(m, n, scale, type);

    if (!chk_flag) {
      norm = (1u << scale);
      for (i = 0; i < m * n; i++) {
        CLAMP_S32(ikernel[i], fkernel[i] * norm);
      }

      return MLIB_SUCCESS;
    }

    /* try to round coefficients */
    if (chk_flag == 3)
      scale1 = 16;                          /* MMX */
    else
      scale1 = (type == MLIB_BYTE) ? 8 : 16;
    norm = (1u << (scale - scale1));

    for (i = 0; i < m * n; i++) {
      if (fkernel[i] > 0)
        ikernel[i] = (mlib_s32) (fkernel[i] * norm + 0.5);
      else
        ikernel[i] = (mlib_s32) (fkernel[i] * norm - 0.5);
    }

    isum_pos = 0;
    isum_neg = 0;
    test = 0;

    for (i = 0; i < m * n; i++) {
      if (ikernel[i] > 0)
        isum_pos += ikernel[i];
      else
        isum_neg -= ikernel[i];
    }

    if (type == MLIB_BYTE || type == MLIB_USHORT) {
      isum = (isum_pos > isum_neg) ? isum_pos : isum_neg;

      if (isum >= (1 << (31 - scale1)))
        test = 1;
    }
    else {
      isum = isum_pos + isum_neg;

      if (isum >= (1 << (32 - scale1)))
        test = 1;
      for (i = 0; i < m * n; i++) {
        if (abs(ikernel[i]) >= (1 << (31 - scale1)))
          test = 1;
      }
    }

    if (test == 1) {                        /* rounding according scale1 cause overflow, truncate instead of round */
      for (i = 0; i < m * n; i++)
        ikernel[i] = (mlib_s32) (fkernel[i] * norm) << scale1;
    }
    else {                                  /* rounding is Ok */
      for (i = 0; i < m * n; i++)
        ikernel[i] = ikernel[i] << scale1;
    }

    return MLIB_SUCCESS;
  }
  else if ((type == MLIB_INT) || (type == MLIB_BIT)) {
    max = 0;

    for (i = 0; i < m * n; i++) {
      f = mlib_fabs(fkernel[i]);
      max = (max > f) ? max : f;
    }

    scale = mlib_ilogb(max);

    if (scale > 29)
      return MLIB_FAILURE;

    if (scale < -100)
      scale = -100;

    *iscale = 29 - scale;
    scale = 29 - scale;

    norm = 1.0;
    while (scale > 30) {
      norm *= (1 << 30);
      scale -= 30;
    }

    norm *= (1 << scale);

    for (i = 0; i < m * n; i++) {
      if (fkernel[i] > 0) {
        CLAMP_S32(ikernel[i], fkernel[i] * norm + 0.5);
      }
      else {
        CLAMP_S32(ikernel[i], fkernel[i] * norm - 0.5);
      }
    }

    return MLIB_SUCCESS;
  }
  else {
    return MLIB_FAILURE;
  }
}

/***************************************************************/
