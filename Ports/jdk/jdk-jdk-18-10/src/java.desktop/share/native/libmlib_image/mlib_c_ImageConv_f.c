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


#include "mlib_image.h"
#include "mlib_ImageConv.h"
#include "mlib_c_ImageConv.h"

/***************************************************************/
#define MLIB_PARAMS_CONV_MN_NW                                  \
  mlib_image *dst,                                              \
  const mlib_image *src,                                        \
  const mlib_s32   *kern,                                       \
  mlib_s32         m,                                           \
  mlib_s32         n,                                           \
  mlib_s32         dm,                                          \
  mlib_s32         dn,                                          \
  mlib_s32         scale,                                       \
  mlib_s32         cmask

/***************************************************************/
#define MLIB_CALL_PARAMS_CONV_MN_NW                             \
  dst, src, kern, m, n, dm, dn, scale, cmask

/***************************************************************/
#define MLIB_PARAMS_CONV_MN_EXT                                 \
  mlib_image       *dst,                                        \
  const mlib_image *src,                                        \
  const mlib_s32   *kern,                                       \
  mlib_s32         m,                                           \
  mlib_s32         n,                                           \
  mlib_s32         dx_l,                                        \
  mlib_s32         dx_r,                                        \
  mlib_s32         dy_t,                                        \
  mlib_s32         dy_b,                                        \
  mlib_s32         scale,                                       \
  mlib_s32         cmask

/***************************************************************/
#define MLIB_CALL_PARAMS_CONV_MN_EXT                            \
  dst, src, kern, m, n, dx_l, dx_r, dy_t, dy_b, scale, cmask


/***************************************************************/
mlib_status mlib_convMxNnw_u8(MLIB_PARAMS_CONV_MN_NW)
{
  if (mlib_ImageConvVersion(m, n, scale, MLIB_BYTE) == 0)
    return mlib_c_convMxNnw_u8(MLIB_CALL_PARAMS_CONV_MN_NW);
  else
    return mlib_i_convMxNnw_u8(MLIB_CALL_PARAMS_CONV_MN_NW);
}

/***************************************************************/
mlib_status mlib_convMxNext_u8(MLIB_PARAMS_CONV_MN_EXT)
{
  if (mlib_ImageConvVersion(m, n, scale, MLIB_BYTE) == 0)
    return mlib_c_convMxNext_u8(MLIB_CALL_PARAMS_CONV_MN_EXT);
  else
    return mlib_i_convMxNext_u8(MLIB_CALL_PARAMS_CONV_MN_EXT);
}

/***************************************************************/
