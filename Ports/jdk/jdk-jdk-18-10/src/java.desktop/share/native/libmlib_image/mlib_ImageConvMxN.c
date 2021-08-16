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
 *      of channels (1, 2, 3, or 4). They can be in MLIB_BIT, MLIB_BYTE,
 *      MLIB_SHORT, MLIB_USHORT or MLIB_INT data type.
 *      m >= 1, n >= 1,
 *      0 <= dm < m, 0 <= dn < n.
 *      For data type MLIB_BYTE:   16 <= scale <= 31 (to be compatible with VIS version)
 *      For data type MLIB_SHORT:  17 <= scale <= 32 (to be compatible with VIS version)
 *      For data type MLIB_USHORT: 17 <= scale <= 32 (to be compatible with VIS version)
 *      For data type MLIB_INT:    scale >= 0
 */

#include "mlib_image.h"
#include "mlib_ImageCheck.h"
#include "mlib_ImageConv.h"
#include "mlib_ImageCreate.h"
#include "mlib_c_ImageConv.h"
#include "mlib_ImageClipping.h"
#include "mlib_ImageConvEdge.h"

/***************************************************************/
JNIEXPORT
mlib_status mlib_ImageConvMxN(mlib_image       *dst,
                              const mlib_image *src,
                              const mlib_s32   *kernel,
                              mlib_s32         m,
                              mlib_s32         n,
                              mlib_s32         dm,
                              mlib_s32         dn,
                              mlib_s32         scale,
                              mlib_s32         cmask,
                              mlib_edge        edge)
{
  MLIB_IMAGE_CHECK(dst);

  switch (mlib_ImageGetType(dst)) {
    case MLIB_BYTE:

      if (scale < 16 || scale > 31)
        return MLIB_FAILURE;
      break;
    case MLIB_SHORT:
    case MLIB_USHORT:

      if (scale < 17 || scale > 32)
        return MLIB_FAILURE;
      break;
    case MLIB_INT:

      if (scale < 0)
        return MLIB_FAILURE;
      break;
    default:
      return MLIB_FAILURE;
  }

  return mlib_ImageConvMxN_f(dst, src, kernel, m, n, dm, dn, scale, cmask, edge);
}

/***************************************************************/
mlib_status mlib_ImageConvMxN_f(mlib_image       *dst,
                                const mlib_image *src,
                                const void       *kernel,
                                mlib_s32         m,
                                mlib_s32         n,
                                mlib_s32         dm,
                                mlib_s32         dn,
                                mlib_s32         scale,
                                mlib_s32         cmask,
                                mlib_edge        edge)
{
  mlib_image dst_i[1], src_i[1], dst_e[1], src_e[1];
  mlib_type type;
  mlib_s32 nchan, dx_l, dx_r, dy_t, dy_b;
  mlib_s32 edg_sizes[8];
  mlib_status ret;

  if (m < 1 || n < 1 || dm < 0 || dm > m - 1 || dn < 0 || dn > n - 1)
    return MLIB_FAILURE;

  if (kernel == NULL)
    return MLIB_NULLPOINTER;

  ret =
    mlib_ImageClippingMxN(dst_i, src_i, dst_e, src_e, edg_sizes, dst, src, m, n, dm, dn);

  if (ret != MLIB_SUCCESS)
    return ret;

  nchan = mlib_ImageGetChannels(dst);
  type = mlib_ImageGetType(dst);

  if (nchan == 1)
    cmask = 1;

  if ((cmask & ((1 << nchan) - 1)) == 0)
    return MLIB_SUCCESS;

  dx_l = edg_sizes[0];
  dx_r = edg_sizes[1];
  dy_t = edg_sizes[2];
  dy_b = edg_sizes[3];

  if (dx_l + dx_r + dy_t + dy_b == 0)
    edge = MLIB_EDGE_DST_NO_WRITE;

  if (edge != MLIB_EDGE_SRC_EXTEND) {
    if (mlib_ImageGetWidth(dst_i) >= m && mlib_ImageGetHeight(dst_i) >= n) {
      switch (type) {
        case MLIB_BYTE:
          ret = mlib_convMxNnw_u8(dst_i, src_i, kernel, m, n, dm, dn, scale, cmask);
          break;
        case MLIB_SHORT:
          if (mlib_ImageConvVersion(m, n, scale, type) == 0)
            ret = mlib_convMxNnw_s16(dst_i, src_i, kernel, m, n, dm, dn, scale, cmask);
          else
            ret = mlib_i_convMxNnw_s16(dst_i, src_i, kernel, m, n, dm, dn, scale, cmask);
          break;
        case MLIB_USHORT:
          if (mlib_ImageConvVersion(m, n, scale, type) == 0)
            ret = mlib_convMxNnw_u16(dst_i, src_i, kernel, m, n, dm, dn, scale, cmask);
          else
            ret = mlib_i_convMxNnw_u16(dst_i, src_i, kernel, m, n, dm, dn, scale, cmask);
          break;
        case MLIB_INT:
          ret = mlib_convMxNnw_s32(dst_i, src_i, kernel, m, n, dm, dn, scale, cmask);
          break;
        case MLIB_FLOAT:
          ret = mlib_convMxNnw_f32(dst_i, src_i, kernel, m, n, dm, dn, cmask);
          break;
        case MLIB_DOUBLE:
          ret = mlib_convMxNnw_d64(dst_i, src_i, kernel, m, n, dm, dn, cmask);
          break;

      default:
        /* For some reasons, there is no convolution routine for type MLIB_BIT.
         * For now, we silently ignore it (because this image type is not used by java),
         * but probably we have to report an error.
         */
        break;
      }
    }

    switch (edge) {
      case MLIB_EDGE_DST_FILL_ZERO:
        mlib_ImageConvZeroEdge(dst_e, dx_l, dx_r, dy_t, dy_b, cmask);
        break;
      case MLIB_EDGE_DST_COPY_SRC:
        mlib_ImageConvCopyEdge(dst_e, src_e, dx_l, dx_r, dy_t, dy_b, cmask);
        break;
    default:
      /* Other edge conditions do not need additional handling.
       *  Note also that they are not exposed in public Java API
       */
      break;
    }
  }
  else {                                    /* MLIB_EDGE_SRC_EXTEND */
    /* adjust src_e image */
    mlib_ImageSetSubimage(src_e, src_e, dx_l - dm, dy_t - dn,
                          mlib_ImageGetWidth(src_e), mlib_ImageGetHeight(src_e));

    switch (type) {
      case MLIB_BYTE:
        ret =
          mlib_convMxNext_u8(dst_e, src_e, kernel, m, n, dx_l, dx_r, dy_t, dy_b, scale,
                             cmask);
        break;
      case MLIB_SHORT:
        if (mlib_ImageConvVersion(m, n, scale, type) == 0)
          ret =
            mlib_convMxNext_s16(dst_e, src_e, kernel, m, n, dx_l, dx_r, dy_t, dy_b, scale,
                                cmask);
        else
          ret =
            mlib_i_convMxNext_s16(dst_e, src_e, kernel, m, n, dx_l, dx_r, dy_t, dy_b,
                                  scale, cmask);
        break;
      case MLIB_USHORT:
        if (mlib_ImageConvVersion(m, n, scale, type) == 0)
          ret =
            mlib_convMxNext_u16(dst_e, src_e, kernel, m, n, dx_l, dx_r, dy_t, dy_b, scale,
                                cmask);
        else
          ret =
            mlib_i_convMxNext_u16(dst_e, src_e, kernel, m, n, dx_l, dx_r, dy_t, dy_b,
                                  scale, cmask);
        break;
      case MLIB_INT:
        ret =
          mlib_convMxNext_s32(dst_e, src_e, kernel, m, n, dx_l, dx_r, dy_t, dy_b, scale,
                              cmask);
        break;
      case MLIB_FLOAT:
        mlib_convMxNext_f32(dst_e, src_e, kernel, m, n, dx_l, dx_r, dy_t, dy_b, cmask);
        break;
      case MLIB_DOUBLE:
        mlib_convMxNext_d64(dst_e, src_e, kernel, m, n, dx_l, dx_r, dy_t, dy_b, cmask);
        break;
    default:
      /* For some reasons, there is no convolution routine for type MLIB_BIT.
       * For now, we silently ignore it (because this image type is not used by java),
       * but probably we have to report an error.
       */
      break;
    }
  }

  return ret;
}

/***************************************************************/
