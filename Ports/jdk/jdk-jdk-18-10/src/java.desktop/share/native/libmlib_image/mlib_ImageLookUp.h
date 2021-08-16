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


#ifndef __MLIB_IMAGE_LOOKUP_FUNC_INTENAL_H
#define __MLIB_IMAGE_LOOKUP_FUNC_INTENAL_H

#include "mlib_ImageCopy.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#ifdef _MSC_VER
/* Microsoft VC 6.0 compiler assumes that pointer fit into long
   and therefore array's index may not exceed MAX_INT/sizeof(data_type).

   TABLE_SHIFT_32 is used as index in arrays of types up to mlib_d64
   (see mlib_ImageLookUp_S32_D64 for instance) and therefore must not
   exceed ((2^33/sizeof(mlib_d64)) - 1) */
#define TABLE_SHIFT_S32         (mlib_u32) 536870911
#else
#define TABLE_SHIFT_S32         536870911u
#endif /* _MSC_VER */


/* mlib_ImageLookUp_64.c */

void mlib_ImageLookUp_U8_D64(const mlib_u8  *src,
                             mlib_s32       slb,
                             mlib_d64       *dst,
                             mlib_s32       dlb,
                             mlib_s32       xsize,
                             mlib_s32       ysize,
                             mlib_s32       csize,
                             const mlib_d64 **table);

void mlib_ImageLookUp_S16_D64(const mlib_s16 *src,
                              mlib_s32       slb,
                              mlib_d64       *dst,
                              mlib_s32       dlb,
                              mlib_s32       xsize,
                              mlib_s32       ysize,
                              mlib_s32       csize,
                              const mlib_d64 **table);

void mlib_ImageLookUp_U16_D64(const mlib_u16 *src,
                              mlib_s32       slb,
                              mlib_d64       *dst,
                              mlib_s32       dlb,
                              mlib_s32       xsize,
                              mlib_s32       ysize,
                              mlib_s32       csize,
                              const mlib_d64 **table);

void mlib_ImageLookUp_S32_D64(const mlib_s32 *src,
                              mlib_s32       slb,
                              mlib_d64       *dst,
                              mlib_s32       dlb,
                              mlib_s32       xsize,
                              mlib_s32       ysize,
                              mlib_s32       csize,
                              const mlib_d64 **table);

void mlib_ImageLookUpSI_U8_D64(const mlib_u8  *src,
                               mlib_s32       slb,
                               mlib_d64       *dst,
                               mlib_s32       dlb,
                               mlib_s32       xsize,
                               mlib_s32       ysize,
                               mlib_s32       csize,
                               const mlib_d64 **table);

void mlib_ImageLookUpSI_S16_D64(const mlib_s16 *src,
                                mlib_s32       slb,
                                mlib_d64       *dst,
                                mlib_s32       dlb,
                                mlib_s32       xsize,
                                mlib_s32       ysize,
                                mlib_s32       csize,
                                const mlib_d64 **table);

void mlib_ImageLookUpSI_U16_D64(const mlib_u16 *src,
                                mlib_s32       slb,
                                mlib_d64       *dst,
                                mlib_s32       dlb,
                                mlib_s32       xsize,
                                mlib_s32       ysize,
                                mlib_s32       csize,
                                const mlib_d64 **table);

void mlib_ImageLookUpSI_S32_D64(const mlib_s32 *src,
                                mlib_s32       slb,
                                mlib_d64       *dst,
                                mlib_s32       dlb,
                                mlib_s32       xsize,
                                mlib_s32       ysize,
                                mlib_s32       csize,
                                const mlib_d64 **table);

/* mlib_ImageLookUp_Bit.c */

mlib_status mlib_ImageLookUp_Bit_U8_1(const mlib_u8 *src,
                                      mlib_s32      slb,
                                      mlib_u8       *dst,
                                      mlib_s32      dlb,
                                      mlib_s32      xsize,
                                      mlib_s32      ysize,
                                      mlib_s32      nchan,
                                      mlib_s32      bitoff,
                                      const mlib_u8 **table);

mlib_status mlib_ImageLookUp_Bit_U8_2(const mlib_u8 *src,
                                      mlib_s32      slb,
                                      mlib_u8       *dst,
                                      mlib_s32      dlb,
                                      mlib_s32      xsize,
                                      mlib_s32      ysize,
                                      mlib_s32      nchan,
                                      mlib_s32      bitoff,
                                      const mlib_u8 **table);

mlib_status mlib_ImageLookUp_Bit_U8_3(const mlib_u8 *src,
                                      mlib_s32      slb,
                                      mlib_u8       *dst,
                                      mlib_s32      dlb,
                                      mlib_s32      xsize,
                                      mlib_s32      ysize,
                                      mlib_s32      nchan,
                                      mlib_s32      bitoff,
                                      const mlib_u8 **table);

mlib_status mlib_ImageLookUp_Bit_U8_4(const mlib_u8 *src,
                                      mlib_s32      slb,
                                      mlib_u8       *dst,
                                      mlib_s32      dlb,
                                      mlib_s32      xsize,
                                      mlib_s32      ysize,
                                      mlib_s32      nchan,
                                      mlib_s32      bitoff,
                                      const mlib_u8 **table);

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __MLIB_IMAGE_LOOKUP_FUNC_INTENAL_H */
