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
 *      mlib_ImageConvMxN_Fp - image convolution with edge condition
 *
 * SYNOPSIS
 *      mlib_status mlib_ImageConvMxN_Fp(mlib_image       *dst,
 *                                       const mlib_image *src,
 *                                       const mlib_d64   *kernel,
 *                                       mlib_s32         m,
 *                                       mlib_s32         n,
 *                                       mlib_s32         dm,
 *                                       mlib_s32         dn,
 *                                       mlib_s32         cmask,
 *                                       mlib_edge        edge)
 *
 * ARGUMENTS
 *      dst       Pointer to destination image.
 *      src       Pointer to source image.
 *      m         Kernel width (m must be not less than 1).
 *      n         Kernel height (n must be not less than 1).
 *      dm, dn    Position of key element in convolution kernel.
 *      kernel    Pointer to convolution kernel.
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
 */

#include "mlib_image.h"
#include "mlib_ImageCheck.h"
#include "mlib_SysMath.h"
#include "mlib_ImageConv.h"

/***************************************************************/
static void mlib_ImageConvMxNMulAdd_F32(mlib_f32       *dst,
                                        const mlib_f32 *src,
                                        const mlib_d64 *kernel,
                                        mlib_s32       n,
                                        mlib_s32       m,
                                        mlib_s32       nch,
                                        mlib_s32       dnch);

static void mlib_ImageConvMxNF322F32_ext(mlib_f32       *dst,
                                         const mlib_f32 *src,
                                         mlib_s32       n,
                                         mlib_s32       nch,
                                         mlib_s32       dx_l,
                                         mlib_s32       dx_r);

static void mlib_ImageConvMxNMulAdd_D64(mlib_d64       *dst,
                                        const mlib_d64 *src,
                                        const mlib_d64 *kernel,
                                        mlib_s32       n,
                                        mlib_s32       m,
                                        mlib_s32       nch,
                                        mlib_s32       dnch);

static void mlib_ImageConvMxND642D64_ext(mlib_d64       *dst,
                                         const mlib_d64 *src,
                                         mlib_s32       n,
                                         mlib_s32       nch,
                                         mlib_s32       dx_l,
                                         mlib_s32       dx_r);

/***************************************************************/
#if 0
static void mlib_ImageConvMxNMulAdd2_F32(mlib_f32       *hdst,
                                         mlib_f32       *vdst,
                                         const mlib_f32 *src,
                                         const mlib_d64 *hfilter,
                                         const mlib_d64 *vfilter,
                                         mlib_s32       n,
                                         mlib_s32       m,
                                         mlib_s32       nch,
                                         mlib_s32       dnch);

static void mlib_ImageConvMxNMulAdd2_D64(mlib_d64       *hdst,
                                         mlib_d64       *vdst,
                                         const mlib_d64 *src,
                                         const mlib_d64 *hfilter,
                                         const mlib_d64 *vfilter,
                                         mlib_s32       n,
                                         mlib_s32       m,
                                         mlib_s32       nch,
                                         mlib_s32       dnch);
#endif /* 0 */

/***************************************************************/
mlib_status mlib_ImageConvMxN_Fp(mlib_image       *dst,
                                 const mlib_image *src,
                                 const mlib_d64   *kernel,
                                 mlib_s32         m,
                                 mlib_s32         n,
                                 mlib_s32         dm,
                                 mlib_s32         dn,
                                 mlib_s32         cmask,
                                 mlib_edge        edge)
{
  mlib_type type;

  MLIB_IMAGE_CHECK(dst);
  type = mlib_ImageGetType(dst);

  if (type != MLIB_FLOAT && type != MLIB_DOUBLE)
    return MLIB_FAILURE;

  return mlib_ImageConvMxN_f(dst, src, kernel, m, n, dm, dn, 0, cmask, edge);
}

/***************************************************************/
void mlib_ImageConvMxNMulAdd_F32(mlib_f32       *dst,
                                 const mlib_f32 *src,
                                 const mlib_d64 *kernel,
                                 mlib_s32       n,
                                 mlib_s32       m,
                                 mlib_s32       nch,
                                 mlib_s32       dnch)
{
  mlib_f32 *hdst1 = dst + dnch;
  mlib_s32 i, j;

  for (j = 0; j < m - 2; j += 3, src += 3 * nch, kernel += 3) {
    const mlib_f32 *src2 = src + 2 * nch;
    mlib_f32 hval0 = (mlib_f32) kernel[0];
    mlib_f32 hval1 = (mlib_f32) kernel[1];
    mlib_f32 hval2 = (mlib_f32) kernel[2];
    mlib_f32 val0 = src[0];
    mlib_f32 val1 = src[nch];
    mlib_f32 hdvl = dst[0];

    for (i = 0; i < n; i++) {
      mlib_f32 hdvl0 = val0 * hval0 + hdvl;
      mlib_f32 val2 = src2[i * nch];

      hdvl = hdst1[i * dnch];
      hdvl0 += val1 * hval1;
      hdvl0 += val2 * hval2;
      val0 = val1;
      val1 = val2;

      dst[i * dnch] = hdvl0;
    }
  }

  if (j < m - 1) {
    const mlib_f32 *src2 = src + 2 * nch;
    mlib_f32 hval0 = (mlib_f32) kernel[0];
    mlib_f32 hval1 = (mlib_f32) kernel[1];
    mlib_f32 val0 = src[0];
    mlib_f32 val1 = src[nch];
    mlib_f32 hdvl = dst[0];
    for (i = 0; i < n; i++) {
      mlib_f32 hdvl0 = val0 * hval0 + hdvl;
      mlib_f32 val2 = src2[i * nch];

      hdvl = hdst1[i * dnch];
      hdvl0 += val1 * hval1;
      val0 = val1;
      val1 = val2;

      dst[i * dnch] = hdvl0;
    }

  }
  else if (j < m) {
    const mlib_f32 *src2 = src + 2 * nch;
    mlib_f32 hval0 = (mlib_f32) kernel[0];
    mlib_f32 val0 = src[0];
    mlib_f32 val1 = src[nch];
    mlib_f32 hdvl = dst[0];

    for (i = 0; i < n; i++) {
      mlib_f32 hdvl0 = val0 * hval0 + hdvl;
      mlib_f32 val2 = src2[i * nch];

      hdvl = hdst1[i * dnch];
      val0 = val1;
      val1 = val2;

      dst[i * dnch] = hdvl0;
    }
  }
}

/***************************************************************/
void mlib_ImageConvMxNF322F32_ext(mlib_f32       *dst,
                                  const mlib_f32 *src,
                                  mlib_s32       n,
                                  mlib_s32       nch,
                                  mlib_s32       dx_l,
                                  mlib_s32       dx_r)
{
  mlib_s32 i;
  mlib_f32 val = src[0];

  for (i = 0; i < dx_l; i++)
    dst[i] = val;
  for (; i < n - dx_r; i++)
    dst[i] = src[nch * (i - dx_l)];
  val = dst[n - dx_r - 1];
  for (; i < n; i++)
    dst[i] = val;
}

/***************************************************************/
mlib_status mlib_convMxNext_f32(mlib_image       *dst,
                                const mlib_image *src,
                                const mlib_d64   *kernel,
                                mlib_s32         m,
                                mlib_s32         n,
                                mlib_s32         dx_l,
                                mlib_s32         dx_r,
                                mlib_s32         dy_t,
                                mlib_s32         dy_b,
                                mlib_s32         cmask)
{
  mlib_d64 dspace[1024], *dsa = dspace;
  mlib_s32 wid_e = mlib_ImageGetWidth(src);
  mlib_f32 *fsa;
  mlib_f32 *da = mlib_ImageGetData(dst);
  mlib_f32 *sa = mlib_ImageGetData(src);
  mlib_s32 dlb = mlib_ImageGetStride(dst) >> 2;
  mlib_s32 slb = mlib_ImageGetStride(src) >> 2;
  mlib_s32 dw = mlib_ImageGetWidth(dst);
  mlib_s32 dh = mlib_ImageGetHeight(dst);
  mlib_s32 nch = mlib_ImageGetChannels(dst);
  mlib_s32 i, j, j1, k;

  if (3 * wid_e + m > 1024) {
    dsa = mlib_malloc((3 * wid_e + m) * sizeof(mlib_d64));

    if (dsa == NULL)
      return MLIB_FAILURE;
  }

  fsa = (mlib_f32 *) dsa;

  for (j = 0; j < dh; j++, da += dlb) {
    for (k = 0; k < nch; k++)
      if (cmask & (1 << (nch - 1 - k))) {
        const mlib_f32 *sa1 = sa + k;
        mlib_f32 *da1 = da + k;
        const mlib_d64 *kernel1 = kernel;

        for (i = 0; i < dw; i++)
          da1[i * nch] = 0.f;
        for (j1 = 0; j1 < n; j1++, kernel1 += m) {
          mlib_ImageConvMxNF322F32_ext(fsa, sa1, dw + m - 1, nch, dx_l, dx_r);
          mlib_ImageConvMxNMulAdd_F32(da1, fsa, kernel1, dw, m, 1, nch);

          if ((j + j1 >= dy_t) && (j + j1 < dh + n - dy_b - 2))
            sa1 += slb;
        }
      }

    if ((j >= dy_t) && (j < dh + n - dy_b - 2))
      sa += slb;
  }

  if (dsa != dspace)
    mlib_free(dsa);
  return MLIB_SUCCESS;
}

/***************************************************************/
#if 0

void mlib_ImageConvMxNMulAdd2_F32(mlib_f32       *hdst,
                                  mlib_f32       *vdst,
                                  const mlib_f32 *src,
                                  const mlib_d64 *hfilter,
                                  const mlib_d64 *vfilter,
                                  mlib_s32       n,
                                  mlib_s32       m,
                                  mlib_s32       nch,
                                  mlib_s32       dnch)
{
  mlib_f32 *hdst1 = hdst + dnch, *vdst1 = vdst + dnch;
  mlib_s32 i, j;

  for (j = 0; j < m - 2; j += 3, src += 3 * nch, hfilter += 3, vfilter += 3) {
    mlib_f32 *src2 = src + 2 * nch;
    mlib_f32 hval0 = (mlib_f32) hfilter[0];
    mlib_f32 vval0 = (mlib_f32) vfilter[0];
    mlib_f32 hval1 = (mlib_f32) hfilter[1];
    mlib_f32 vval1 = (mlib_f32) vfilter[1];
    mlib_f32 hval2 = (mlib_f32) hfilter[2];
    mlib_f32 vval2 = (mlib_f32) vfilter[2];
    mlib_f32 val0 = src[0];
    mlib_f32 val1 = src[nch];
    mlib_f32 hdvl = hdst[0];
    mlib_f32 vdvl = vdst[0];

    for (i = 0; i < n; i++) {
      mlib_f32 hdvl0 = val0 * hval0 + hdvl;
      mlib_f32 vdvl0 = val0 * vval0 + vdvl;
      mlib_f32 val2 = src2[i * nch];

      hdvl = hdst1[i * dnch];
      vdvl = vdst1[i * dnch];
      hdvl0 += val1 * hval1;
      vdvl0 += val1 * vval1;
      hdvl0 += val2 * hval2;
      vdvl0 += val2 * vval2;
      val0 = val1;
      val1 = val2;

      hdst[i * dnch] = hdvl0;
      vdst[i * dnch] = vdvl0;
    }
  }

  if (j < m - 1) {
    mlib_f32 *src2 = src + 2 * nch;
    mlib_f32 hval0 = (mlib_f32) hfilter[0];
    mlib_f32 vval0 = (mlib_f32) vfilter[0];
    mlib_f32 hval1 = (mlib_f32) hfilter[1];
    mlib_f32 vval1 = (mlib_f32) vfilter[1];
    mlib_f32 val0 = src[0];
    mlib_f32 val1 = src[nch];
    mlib_f32 hdvl = hdst[0];
    mlib_f32 vdvl = vdst[0];

    for (i = 0; i < n; i++) {
      mlib_f32 hdvl0 = val0 * hval0 + hdvl;
      mlib_f32 vdvl0 = val0 * vval0 + vdvl;
      mlib_f32 val2 = src2[i * nch];

      hdvl = hdst1[i * dnch];
      vdvl = vdst1[i * dnch];
      hdvl0 += val1 * hval1;
      vdvl0 += val1 * vval1;
      val0 = val1;
      val1 = val2;

      hdst[i * dnch] = hdvl0;
      vdst[i * dnch] = vdvl0;
    }

  }
  else if (j < m) {
    mlib_f32 *src2 = src + 2 * nch;
    mlib_f32 hval0 = (mlib_f32) hfilter[0];
    mlib_f32 vval0 = (mlib_f32) vfilter[0];
    mlib_f32 val0 = src[0];
    mlib_f32 val1 = src[nch];
    mlib_f32 hdvl = hdst[0];
    mlib_f32 vdvl = vdst[0];

    for (i = 0; i < n; i++) {
      mlib_f32 hdvl0 = val0 * hval0 + hdvl;
      mlib_f32 vdvl0 = val0 * vval0 + vdvl;
      mlib_f32 val2 = src2[i * nch];

      hdvl = hdst1[i * dnch];
      vdvl = vdst1[i * dnch];
      val0 = val1;
      val1 = val2;

      hdst[i * dnch] = hdvl0;
      vdst[i * dnch] = vdvl0;
    }
  }
}

/***************************************************************/
void mlib_ImageConvMxNMulAdd2_D64(mlib_d64       *hdst,
                                  mlib_d64       *vdst,
                                  const mlib_d64 *src,
                                  const mlib_d64 *hfilter,
                                  const mlib_d64 *vfilter,
                                  mlib_s32       n,
                                  mlib_s32       m,
                                  mlib_s32       nch,
                                  mlib_s32       dnch)
{
  mlib_d64 *hdst1 = hdst + dnch, *vdst1 = vdst + dnch;
  mlib_s32 i, j;

  for (j = 0; j < m - 2; j += 3, src += 3 * nch, hfilter += 3, vfilter += 3) {
    mlib_d64 *src2 = src + 2 * nch;
    mlib_d64 hval0 = hfilter[0];
    mlib_d64 vval0 = vfilter[0];
    mlib_d64 hval1 = hfilter[1];
    mlib_d64 vval1 = vfilter[1];
    mlib_d64 hval2 = hfilter[2];
    mlib_d64 vval2 = vfilter[2];
    mlib_d64 val0 = src[0];
    mlib_d64 val1 = src[nch];
    mlib_d64 hdvl = hdst[0];
    mlib_d64 vdvl = vdst[0];

    for (i = 0; i < n; i++) {
      mlib_d64 hdvl0 = val0 * hval0 + hdvl;
      mlib_d64 vdvl0 = val0 * vval0 + vdvl;
      mlib_d64 val2 = src2[i * nch];

      hdvl = hdst1[i * dnch];
      vdvl = vdst1[i * dnch];
      hdvl0 += val1 * hval1;
      vdvl0 += val1 * vval1;
      hdvl0 += val2 * hval2;
      vdvl0 += val2 * vval2;
      val0 = val1;
      val1 = val2;

      hdst[i * dnch] = hdvl0;
      vdst[i * dnch] = vdvl0;
    }
  }

  if (j < m - 1) {
    mlib_d64 *src2 = src + 2 * nch;
    mlib_d64 hval0 = hfilter[0];
    mlib_d64 vval0 = vfilter[0];
    mlib_d64 hval1 = hfilter[1];
    mlib_d64 vval1 = vfilter[1];
    mlib_d64 val0 = src[0];
    mlib_d64 val1 = src[nch];
    mlib_d64 hdvl = hdst[0];
    mlib_d64 vdvl = vdst[0];

    for (i = 0; i < n; i++) {
      mlib_d64 hdvl0 = val0 * hval0 + hdvl;
      mlib_d64 vdvl0 = val0 * vval0 + vdvl;
      mlib_d64 val2 = src2[i * nch];

      hdvl = hdst1[i * dnch];
      vdvl = vdst1[i * dnch];
      hdvl0 += val1 * hval1;
      vdvl0 += val1 * vval1;
      val0 = val1;
      val1 = val2;

      hdst[i * dnch] = hdvl0;
      vdst[i * dnch] = vdvl0;
    }

  }
  else if (j < m) {
    mlib_d64 *src2 = src + 2 * nch;
    mlib_d64 hval0 = hfilter[0];
    mlib_d64 vval0 = vfilter[0];
    mlib_d64 val0 = src[0];
    mlib_d64 val1 = src[nch];
    mlib_d64 hdvl = hdst[0];
    mlib_d64 vdvl = vdst[0];

    for (i = 0; i < n; i++) {
      mlib_d64 hdvl0 = val0 * hval0 + hdvl;
      mlib_d64 vdvl0 = val0 * vval0 + vdvl;
      mlib_d64 val2 = src2[i * nch];

      hdvl = hdst1[i * dnch];
      vdvl = vdst1[i * dnch];
      val0 = val1;
      val1 = val2;

      hdst[i * dnch] = hdvl0;
      vdst[i * dnch] = vdvl0;
    }
  }
}

#endif /* 0 */

/***************************************************************/
void mlib_ImageConvMxNMulAdd_D64(mlib_d64       *dst,
                                 const mlib_d64 *src,
                                 const mlib_d64 *kernel,
                                 mlib_s32       n,
                                 mlib_s32       m,
                                 mlib_s32       nch,
                                 mlib_s32       dnch)
{
  mlib_d64 *hdst1 = dst + dnch;
  mlib_s32 i, j;

  for (j = 0; j < m - 2; j += 3, src += 3 * nch, kernel += 3) {
    const mlib_d64 *src2 = src + 2 * nch;
    mlib_d64 hval0 = kernel[0];
    mlib_d64 hval1 = kernel[1];
    mlib_d64 hval2 = kernel[2];
    mlib_d64 val0 = src[0];
    mlib_d64 val1 = src[nch];
    mlib_d64 hdvl = dst[0];

    for (i = 0; i < n; i++) {
      mlib_d64 hdvl0 = val0 * hval0 + hdvl;
      mlib_d64 val2 = src2[i * nch];

      hdvl = hdst1[i * dnch];
      hdvl0 += val1 * hval1;
      hdvl0 += val2 * hval2;
      val0 = val1;
      val1 = val2;

      dst[i * dnch] = hdvl0;
    }
  }

  if (j < m - 1) {
    const mlib_d64 *src2 = src + 2 * nch;
    mlib_d64 hval0 = kernel[0];
    mlib_d64 hval1 = kernel[1];
    mlib_d64 val0 = src[0];
    mlib_d64 val1 = src[nch];
    mlib_d64 hdvl = dst[0];

    for (i = 0; i < n; i++) {
      mlib_d64 hdvl0 = val0 * hval0 + hdvl;
      mlib_d64 val2 = src2[i * nch];

      hdvl = hdst1[i * dnch];
      hdvl0 += val1 * hval1;
      val0 = val1;
      val1 = val2;

      dst[i * dnch] = hdvl0;
    }

  }
  else if (j < m) {
    const mlib_d64 *src2 = src + 2 * nch;
    mlib_d64 hval0 = kernel[0];
    mlib_d64 val0 = src[0];
    mlib_d64 val1 = src[nch];
    mlib_d64 hdvl = dst[0];

    for (i = 0; i < n; i++) {
      mlib_d64 hdvl0 = val0 * hval0 + hdvl;
      mlib_d64 val2 = src2[i * nch];

      hdvl = hdst1[i * dnch];
      val0 = val1;
      val1 = val2;

      dst[i * dnch] = hdvl0;
    }
  }
}

/***************************************************************/
void mlib_ImageConvMxND642D64_ext(mlib_d64       *dst,
                                  const mlib_d64 *src,
                                  mlib_s32       n,
                                  mlib_s32       nch,
                                  mlib_s32       dx_l,
                                  mlib_s32       dx_r)
{
  mlib_s32 i;
  mlib_d64 val = src[0];

  for (i = 0; i < dx_l; i++)
    dst[i] = val;
  for (; i < n - dx_r; i++)
    dst[i] = src[nch * (i - dx_l)];
  val = dst[n - dx_r - 1];
  for (; i < n; i++)
    dst[i] = val;
}

/***************************************************************/
mlib_status mlib_convMxNext_d64(mlib_image       *dst,
                                const mlib_image *src,
                                const mlib_d64   *kernel,
                                mlib_s32         m,
                                mlib_s32         n,
                                mlib_s32         dx_l,
                                mlib_s32         dx_r,
                                mlib_s32         dy_t,
                                mlib_s32         dy_b,
                                mlib_s32         cmask)
{
  mlib_d64 dspace[1024], *dsa = dspace;
  mlib_s32 wid_e = mlib_ImageGetWidth(src);
  mlib_d64 *da = mlib_ImageGetData(dst);
  mlib_d64 *sa = mlib_ImageGetData(src);
  mlib_s32 dlb = mlib_ImageGetStride(dst) >> 3;
  mlib_s32 slb = mlib_ImageGetStride(src) >> 3;
  mlib_s32 dw = mlib_ImageGetWidth(dst);
  mlib_s32 dh = mlib_ImageGetHeight(dst);
  mlib_s32 nch = mlib_ImageGetChannels(dst);
  mlib_s32 i, j, j1, k;

  if (3 * wid_e + m > 1024) {
    dsa = mlib_malloc((3 * wid_e + m) * sizeof(mlib_d64));

    if (dsa == NULL)
      return MLIB_FAILURE;
  }

  for (j = 0; j < dh; j++, da += dlb) {
    for (k = 0; k < nch; k++)
      if (cmask & (1 << (nch - 1 - k))) {
        mlib_d64 *sa1 = sa + k;
        mlib_d64 *da1 = da + k;
        const mlib_d64 *kernel1 = kernel;

        for (i = 0; i < dw; i++)
          da1[i * nch] = 0.;
        for (j1 = 0; j1 < n; j1++, kernel1 += m) {
          mlib_ImageConvMxND642D64_ext(dsa, sa1, dw + m - 1, nch, dx_l, dx_r);
          mlib_ImageConvMxNMulAdd_D64(da1, dsa, kernel1, dw, m, 1, nch);

          if ((j + j1 >= dy_t) && (j + j1 < dh + n - dy_b - 2))
            sa1 += slb;
        }
      }

    if ((j >= dy_t) && (j < dh + n - dy_b - 2))
      sa += slb;
  }

  if (dsa != dspace)
    mlib_free(dsa);
  return MLIB_SUCCESS;
}

/***************************************************************/
