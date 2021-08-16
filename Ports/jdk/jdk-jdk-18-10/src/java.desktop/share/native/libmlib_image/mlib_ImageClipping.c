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
 *      mlib_ImageClipping
 *      mlib_ImageClippingMxN
 *              Clipping for image processing in case of pixel-to-pixel
 *              square kernel filtering. Source and destination images can have
 *              different sizes, center of the source is mapped to the center of
 *              the destination image.
 *              Examples of this type of image processing are Convolve, Gradient,
 *              Dilate/Erode functions, etc.
 *
 * SYNOPSIS
 *      mlib_status mlib_ImageClipping(mlib_image       *dst_i,
 *                                     mlib_image       *src_i,
 *                                     mlib_image       *dst_e,
 *                                     mlib_image       *src_e,
 *                                     mlib_s32         *edg_sizes,
 *                                     const mlib_image *dst,
 *                                     const mlib_image *src,
 *                                     mlib_s32         ker_size)
 *
 *      mlib_status mlib_ImageClippingMxN(mlib_image       *dst_i,
 *                                        mlib_image       *src_i,
 *                                        mlib_image       *dst_e,
 *                                        mlib_image       *src_e,
 *                                        mlib_s32         *edg_sizes,
 *                                        const mlib_image *dst,
 *                                        const mlib_image *src,
 *                                        mlib_s32         kw,
 *                                        mlib_s32         kh,
 *                                        mlib_s32         kw1,
 *                                        mlib_s32         kh1)
 *
 * OUTPUT ARGUMENTS
 *      dst_i     Pointer to destination image of internal pixels
 *      src_i     Pointer to source image of internal pixels
 *      dst_e     Pointer to destination image for edge processing
 *      src_e     Pointer to source image for edge processing
 *      edg_sizes Array of edge sizes
 *
 * INPUT ARGUMENTS
 *      dst       Pointer to destination image.
 *      src       Pointer to source image.
 *      ksize     Size of kernel
 *
 * RESTRICTION
 *      The src and the dst must be images of the same type.
 *      The src and dst must have same number of channels.
 *
 */

#include "mlib_image.h"
#include "mlib_ImageCheck.h"
#include "mlib_ImageClipping.h"
#include "mlib_ImageCreate.h"

/***************************************************************/
mlib_status mlib_ImageClippingMxN(mlib_image       *dst_i,
                                  mlib_image       *src_i,
                                  mlib_image       *dst_e,
                                  mlib_image       *src_e,
                                  mlib_s32         *edg_sizes,
                                  const mlib_image *dst,
                                  const mlib_image *src,
                                  mlib_s32         kw,
                                  mlib_s32         kh,
                                  mlib_s32         kw1,
                                  mlib_s32         kh1)
{
  mlib_s32  kw2 = kw - 1 - kw1;
  mlib_s32  kh2 = kh - 1 - kh1;
  mlib_s32  src_wid, src_hgt, dst_wid, dst_hgt;
  mlib_s32  dx, dy, dxd, dxs, dyd, dys, wid_e, hgt_e;
  mlib_s32  dx_l, dx_r, dy_t, dy_b, wid_i, hgt_i;

  MLIB_IMAGE_CHECK(dst);
  MLIB_IMAGE_CHECK(src);
  MLIB_IMAGE_TYPE_EQUAL(dst, src);
  MLIB_IMAGE_CHAN_EQUAL(dst, src);

  dst_wid = mlib_ImageGetWidth(dst);
  dst_hgt = mlib_ImageGetHeight(dst);
  src_wid = mlib_ImageGetWidth(src);
  src_hgt = mlib_ImageGetHeight(src);

  /* X clipping */
  dx = src_wid - dst_wid;

  if (dx > 0) {
    dxs = (dx + 1) >> 1;
    dxd = 0;
  } else {
    dxs = 0;
    dxd = (-dx) >> 1;
  }

  dx_l = kw1 - dxs;
  dx_r = kw2 + dxs - dx;

  if (dx_l < 0) dx_l = 0;
  if (dx_r < 0) dx_r = 0;
  if (dx_r > kw2) dx_r = kw2;

  /* Y clipping */
  dy = src_hgt - dst_hgt;

  if (dy > 0) {
    dys = (dy + 1) >> 1;
    dyd = 0;
  } else {
    dys = 0;
    dyd = (-dy) >> 1;
  }

  dy_t = kh1 - dys;
  dy_b = kh2 + dys - dy;

  if (dy_t < 0) dy_t = 0;
  if (dy_b < 0) dy_b = 0;
  if (dy_b > kh2) dy_b = kh2;

  /* image sizes */
  wid_e = (src_wid < dst_wid) ? src_wid : dst_wid;
  hgt_e = (src_hgt < dst_hgt) ? src_hgt : dst_hgt;
  wid_i = wid_e + (kw1 - dx_l) + (kw2 - dx_r);
  hgt_i = hgt_e + (kh1 - dy_t) + (kh2 - dy_b);

  mlib_ImageSetSubimage(dst_i, dst, dxd - (kw1 - dx_l), dyd - (kh1 - dy_t), wid_i, hgt_i);
  mlib_ImageSetSubimage(src_i, src, dxs - (kw1 - dx_l), dys - (kh1 - dy_t), wid_i, hgt_i);

  if (dst_e != NULL && src_e != NULL) { /* images for edge processing */
    mlib_ImageSetSubimage(dst_e, dst, dxd, dyd, wid_e, hgt_e);
    mlib_ImageSetSubimage(src_e, src, dxs, dys, wid_e, hgt_e);
  }

  if (edg_sizes != NULL) { /* save edges */
    edg_sizes[0] = dx_l;
    edg_sizes[1] = dx_r;
    edg_sizes[2] = dy_t;
    edg_sizes[3] = dy_b;
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageClipping(mlib_image       *dst_i,
                               mlib_image       *src_i,
                               mlib_image       *dst_e,
                               mlib_image       *src_e,
                               mlib_s32         *edg_sizes,
                               const mlib_image *dst,
                               const mlib_image *src,
                               mlib_s32         ker_size)
{
  mlib_s32 kw1 = (ker_size - 1)/2;
  return mlib_ImageClippingMxN(dst_i, src_i, dst_e, src_e, edg_sizes,
                               dst, src, ker_size, ker_size, kw1, kw1);
}

/***************************************************************/
