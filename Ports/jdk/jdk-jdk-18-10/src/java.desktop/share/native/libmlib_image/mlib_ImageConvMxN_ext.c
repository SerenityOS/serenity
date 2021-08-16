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
 *      mlib_ImageConvMxN - image convolution with edge condition
 *
 * SYNOPSIS
 *      mlib_status mlib_ImageConvMxN(mlib_image       *dst,
 *                                    const mlib_image *src,
 *                                    const mlib_s32   *kernel,
 *                                    mlib_s32         m,
 *                                    mlib_s32         n,
 *                                    mlib_s32         dm,
 *                                    mlib_s32         dn,
 *                                    mlib_s32         scale,
 *                                    mlib_s32         cmask,
 *                                    mlib_edge        edge)
 *
 * ARGUMENTS
 *      dst       Pointer to destination image.
 *      src       Pointer to source image.
 *      m         Kernel width (m must be not less than 1).
 *      n         Kernel height (n must be not less than 1).
 *      dm, dn    Position of key element in convolution kernel.
 *      kernel    Pointer to convolution kernel.
 *      scale     The scaling factor to convert the input integer
 *                coefficients into floating-point coefficients:
 *                floating-point coefficient = integer coefficient * 2^(-scale)
 *      cmask     Channel mask to indicate the channels to be convolved.
 *                Each bit of which represents a channel in the image. The
 *                channels corresponded to 1 bits are those to be processed.
 *      edge      Type of edge condition.
 *
 * DESCRIPTION
 *      2-D convolution, MxN kernel.
 *
 *      The center of the source image is mapped to the center of the
 *      destination image.
 *      The unselected channels are not overwritten. If both src and dst have
 *      just one channel, cmask is ignored.
 *
 *      The edge condition can be one of the following:
 *              MLIB_EDGE_DST_NO_WRITE  (default)
 *              MLIB_EDGE_DST_FILL_ZERO
 *              MLIB_EDGE_DST_COPY_SRC
 *              MLIB_EDGE_SRC_EXTEND
 *
 * RESTRICTION
 *      The src and the dst must be the same type and have same number
 *      of channels (1, 2, 3, or 4).
 *      m >= 1, n >= 1,
 *      0 <= dm < m, 0 <= dn < n.
 *      For data type MLIB_BYTE:   16 <= scale <= 31 (to be compatible with VIS version)
 *      For data type MLIB_USHORT: 17 <= scale <= 32 (to be compatible with VIS version)
 *      For data type MLIB_SHORT:  17 <= scale <= 32 (to be compatible with VIS version)
 *      For data type MLIB_INT:    scale >= 0
 */

#include "mlib_image.h"
#include "mlib_ImageConv.h"

/***************************************************************/
static void mlib_ImageConvMxNMulAdd_S32(mlib_d64       *dst,
                                        const mlib_s32 *src,
                                        const mlib_d64 *dkernel,
                                        mlib_s32       n,
                                        mlib_s32       m,
                                        mlib_s32       nch);

static void mlib_ImageConvMxNMedian_S32(mlib_s32 *dst,
                                        mlib_d64 *src,
                                        mlib_s32 n,
                                        mlib_s32 nch);

static void mlib_ImageConvMxNS322S32_ext(mlib_s32       *dst,
                                         const mlib_s32 *src,
                                         mlib_s32       n,
                                         mlib_s32       nch,
                                         mlib_s32       dx_l,
                                         mlib_s32       dx_r);

/***************************************************************/
#ifdef MLIB_USE_FTOI_CLAMPING

#define CLAMP_S32(dst, src)                                     \
  dst = (mlib_s32)(src)

#else

#define CLAMP_S32(dst, src) {                                   \
  mlib_d64 s0 = (mlib_d64)(src);                                \
  if (s0 > (mlib_d64)MLIB_S32_MAX) s0 = (mlib_d64)MLIB_S32_MAX; \
  if (s0 < (mlib_d64)MLIB_S32_MIN) s0 = (mlib_d64)MLIB_S32_MIN; \
  dst = (mlib_s32)s0;                                           \
}

#endif /* MLIB_USE_FTOI_CLAMPING */

/***************************************************************/
void mlib_ImageConvMxNMulAdd_S32(mlib_d64       *dst,
                                 const mlib_s32 *src,
                                 const mlib_d64 *dkernel,
                                 mlib_s32       n,
                                 mlib_s32       m,
                                 mlib_s32       nch)
{
  mlib_d64 *dst1 = dst + 1;
  mlib_s32 i, j;

  for (j = 0; j < m; j += 3, src += 3 * nch, dkernel += 3) {
    const mlib_s32 *src2 = src + 2 * nch;
    mlib_d64 hval0 = dkernel[0];
    mlib_d64 hval1 = dkernel[1];
    mlib_d64 hval2 = dkernel[2];
    mlib_d64 val0 = src[0];
    mlib_d64 val1 = src[nch];
    mlib_d64 dval = dst[0];

    if (j == m - 2) {
      hval2 = 0.f;
    }
    else if (j == m - 1) {
      hval1 = 0.f;
      hval2 = 0.f;
    }

    for (i = 0; i < n; i++) {
      mlib_d64 dval0 = val0 * hval0 + dval;
      mlib_d64 val2 = src2[i * nch];

      dval = dst1[i];
      dval0 += val1 * hval1;
      dval0 += val2 * hval2;
      val0 = val1;
      val1 = val2;

      dst[i] = dval0;
    }
  }
}

/***************************************************************/
void mlib_ImageConvMxNMedian_S32(mlib_s32 *dst,
                                 mlib_d64 *src,
                                 mlib_s32 n,
                                 mlib_s32 nch)
{
  mlib_s32 i;

  for (i = 0; i < n; i++) {
    mlib_s32 res;

    CLAMP_S32(res, src[i]);
    src[i] = 0.5;
    dst[i * nch] = res;
  }
}

/***************************************************************/
void mlib_ImageConvMxNS322S32_ext(mlib_s32       *dst,
                                  const mlib_s32 *src,
                                  mlib_s32       n,
                                  mlib_s32       nch,
                                  mlib_s32       dx_l,
                                  mlib_s32       dx_r)
{
  mlib_s32 i;
  mlib_d64 val = src[0];

  for (i = 0; i < dx_l; i++)
    dst[i] = (mlib_s32) val;
  for (; i < n - dx_r; i++)
    dst[i] = src[nch * (i - dx_l)];
  val = dst[n - dx_r - 1];
  for (; i < n; i++)
    dst[i] = (mlib_s32) val;
}

/***************************************************************/
mlib_status mlib_convMxNext_s32(mlib_image       *dst,
                                const mlib_image *src,
                                const mlib_s32   *kernel,
                                mlib_s32         m,
                                mlib_s32         n,
                                mlib_s32         dx_l,
                                mlib_s32         dx_r,
                                mlib_s32         dy_t,
                                mlib_s32         dy_b,
                                mlib_s32         scale,
                                mlib_s32         cmask)
{
  mlib_d64 dspace[1024], *dsa = dspace;
  mlib_d64 akernel[256], *dkernel = akernel, fscale = 1.0;
  mlib_s32 wid_e = mlib_ImageGetWidth(src);
  mlib_d64 *dsh, *dsv;
  mlib_s32 *isa;
  mlib_s32 *da = mlib_ImageGetData(dst);
  mlib_s32 *sa = mlib_ImageGetData(src);
  mlib_s32 dlb = mlib_ImageGetStride(dst) >> 2;
  mlib_s32 slb = mlib_ImageGetStride(src) >> 2;
  mlib_s32 dw = mlib_ImageGetWidth(dst);
  mlib_s32 dh = mlib_ImageGetHeight(dst);
  mlib_s32 nch = mlib_ImageGetChannels(dst);
  mlib_s32 i, j, j1, k, mn;

  /* internal buffer */

  if (3 * wid_e + m > 1024) {
    dsa = mlib_malloc((3 * wid_e + m) * sizeof(mlib_d64));

    if (dsa == NULL)
      return MLIB_FAILURE;
  }

  isa = (mlib_s32 *) dsa;

  /* load kernel */
  mn = m * n;

  if (mn > 256) {
    dkernel = mlib_malloc(mn * sizeof(mlib_d64));

    if (dkernel == NULL) {
      if (dsa != dspace) mlib_free(dsa);
      return MLIB_FAILURE;
    }
  }

  while (scale > 30) {
    fscale /= (1 << 30);
    scale -= 30;
  }

  fscale /= (1 << scale);

  for (i = 0; i < mn; i++) {
    dkernel[i] = ((mlib_s32 *) kernel)[i] * fscale;
  }

  dsh = dsa + dw + m;
  dsv = dsh + dw;

  for (i = 0; i < dw; i++) {
    dsh[i] = 0.5;
    dsv[i] = 0.5;
  }

  for (j = 0; j < dh; j++, da += dlb) {
    for (k = 0; k < nch; k++)
      if (cmask & (1 << (nch - 1 - k))) {
        mlib_s32 *sa1 = sa + k;
        mlib_d64 *dkernel1 = dkernel;

        for (j1 = 0; j1 < n; j1++, dkernel1 += m) {
          mlib_ImageConvMxNS322S32_ext(isa, sa1, dw + m - 1, nch, dx_l, dx_r);
          mlib_ImageConvMxNMulAdd_S32(dsh, isa, dkernel1, dw, m, 1);

          if ((j + j1 >= dy_t) && (j + j1 < dh + n - dy_b - 2))
            sa1 += slb;
        }

        mlib_ImageConvMxNMedian_S32(da + k, dsh, dw, nch);
      }

    if ((j >= dy_t) && (j < dh + n - dy_b - 2))
      sa += slb;
  }

  if (dkernel != akernel)
    mlib_free(dkernel);
  if (dsa != dspace)
    mlib_free(dsa);
  return MLIB_SUCCESS;
}

/***************************************************************/
