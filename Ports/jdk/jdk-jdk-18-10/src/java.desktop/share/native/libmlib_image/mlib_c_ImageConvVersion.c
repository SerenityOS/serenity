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
 *      mlib_ImageConvVersion - Get Conv* funtions version
 *      0  - "C" version
 *      1  - "VIS" version
 *      2  - "i386" version
 *      3  - "MMX" version
 *
 * SYNOPSIS
 *      mlib_s32 mlib_ImageConvVersion(mlib_s32 m,
 *                                     mlib_s32 n,
 *                                     mlib_s32 scale,
 *                                     mlib_type type)
 *
 */

#include "mlib_image.h"

#define MAX_U8   8
#define MAX_S16 32

/***************************************************************/
mlib_s32 mlib_ImageConvVersion(mlib_s32 m,
                               mlib_s32 n,
                               mlib_s32 scale,
                               mlib_type type)
{
  mlib_d64 dscale = 1.0 / (1 << scale); /* 16 < scale <= 31 */

  if (type == MLIB_BYTE) {
    if ((m * n * dscale * 32768.0) > MAX_U8)
      return 0;
    return 2;
  }
  else if ((type == MLIB_USHORT) || (type == MLIB_SHORT)) {

    if ((m * n * dscale * 32768.0 * 32768.0) > MAX_S16)
      return 0;
    return 2;
  }
  else
    return 0;
}

/***************************************************************/
