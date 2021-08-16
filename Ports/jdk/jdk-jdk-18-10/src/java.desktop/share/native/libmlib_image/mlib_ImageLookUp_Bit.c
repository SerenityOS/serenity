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
 *      mlib_ImageLookUp_Bit_U8 - table lookup
 *
 * SYNOPSIS
 *      void mlib_ImageLookUp_Bit_U8(src, slb,
 *                                   dst, dlb,
 *                                   xsize, ysize,
 *                                   csize, table)
 *
 * ARGUMENT
 *      src     pointer to input image (BIT)
 *      slb     stride of input image (in pixels)
 *      dst     pointer to output image (BYTE)
 *      dlb     stride of output image (in pixels)
 *      xsize   image width
 *      ysize   image height
 *      csize   number of channels
 *      table   lookup table
 *
 * DESCRIPTION
 *      dst = table[src] (c, vis version)
 */

#include "mlib_image.h"
#include "mlib_ImageLookUp.h"

/***************************************************************/
#define MAX_WIDTH  512

/***************************************************************/
#ifdef i386 /* do not copy by double data type for x86 */

typedef struct {
  mlib_u32 int0, int1;
} two_uint;

#define TYPE_64BIT two_uint
#define TYPE_32BIT mlib_u32
#define DTYPE      two_uint

#elif defined(_NO_LONGLONG)

#define TYPE_64BIT mlib_d64
#define TYPE_32BIT mlib_f32
#define DTYPE      mlib_d64

#else

#define TYPE_64BIT mlib_d64
#define TYPE_32BIT mlib_f32
#define DTYPE      mlib_u64

#endif /* i386 ( do not copy by double data type for x86 ) */

/***************************************************************/
typedef union {
  TYPE_64BIT d64;
  struct {
    TYPE_32BIT f0, f1;
  } f32s;
} d64_2_f32;

/***************************************************************/
#ifdef _LITTLE_ENDIAN

static const mlib_u32 mlib_bit_mask[16] = {
  0x00000000u, 0xFF000000u, 0x00FF0000u, 0xFFFF0000u,
  0x0000FF00u, 0xFF00FF00u, 0x00FFFF00u, 0xFFFFFF00u,
  0x000000FFu, 0xFF0000FFu, 0x00FF00FFu, 0xFFFF00FFu,
  0x0000FFFFu, 0xFF00FFFFu, 0x00FFFFFFu, 0xFFFFFFFFu
};

static const mlib_u32 mlib_bit_mask_2[4] = {
  0x00000000u, 0xFFFF0000u, 0x0000FFFFu, 0xFFFFFFFFu
};

static const mlib_u32 mlib_bit_mask_3[3*4] = {
  0x00000000u, 0xFF000000u, 0x00FFFFFFu, 0xFFFFFFFFu,
  0x00000000u, 0xFFFF0000u, 0x0000FFFFu, 0xFFFFFFFFu,
  0x00000000u, 0xFFFFFF00u, 0x000000FFu, 0xFFFFFFFFu
};

#else

static const mlib_u32 mlib_bit_mask[16] = {
  0x00000000u, 0x000000FFu, 0x0000FF00u, 0x0000FFFFu,
  0x00FF0000u, 0x00FF00FFu, 0x00FFFF00u, 0x00FFFFFFu,
  0xFF000000u, 0xFF0000FFu, 0xFF00FF00u, 0xFF00FFFFu,
  0xFFFF0000u, 0xFFFF00FFu, 0xFFFFFF00u, 0xFFFFFFFFu
};

static const mlib_u32 mlib_bit_mask_2[4] = {
  0x00000000u, 0x0000FFFFu, 0xFFFF0000u, 0xFFFFFFFFu
};

static const mlib_u32 mlib_bit_mask_3[3*4] = {
  0x00000000u, 0x000000FFu, 0xFFFFFF00u, 0xFFFFFFFFu,
  0x00000000u, 0x0000FFFFu, 0xFFFF0000u, 0xFFFFFFFFu,
  0x00000000u, 0x00FFFFFFu, 0xFF000000u, 0xFFFFFFFFu
};

#endif /* _LITTLE_ENDIAN */

/***************************************************************/
mlib_status mlib_ImageLookUp_Bit_U8_1(const mlib_u8 *src,
                                      mlib_s32      slb,
                                      mlib_u8       *dst,
                                      mlib_s32      dlb,
                                      mlib_s32      xsize,
                                      mlib_s32      ysize,
                                      mlib_s32      nchan,
                                      mlib_s32      bitoff,
                                      const mlib_u8 **table)
{
  mlib_s32 i, j, n;
  TYPE_64BIT dd_array[256];
  mlib_u8  buff_lcl[MAX_WIDTH/8];
  mlib_u8  *buff = (mlib_u8*)buff_lcl;
  mlib_u32 val0, val1, *p_dd = (mlib_u32*)dd_array;

  if (xsize > MAX_WIDTH) {
    buff = mlib_malloc((xsize + 7)/8);

    if (buff == NULL) return MLIB_FAILURE;
  }

  val0 = table[0][0];
  val1 = table[0][1];
  val0 |= (val0 << 8);
  val1 |= (val1 << 8);
  val0 |= (val0 << 16);
  val1 |= (val1 << 16);

  /* calculate lookup table */
  for (i = 0; i < 16; i++) {
    mlib_u32 v, mask = mlib_bit_mask[i];

    v = (val0 &~ mask) | (val1 & mask);

    for (j = 0; j < 16; j++) {
      p_dd[2*(16*i + j)] = v;
    }

    for (j = 0; j < 16; j++) {
      p_dd[2*(i + 16*j) + 1] = v;
    }
  }

  for (j = 0; j < ysize; j++) {
    mlib_s32 s0, size = xsize;
    mlib_u8  *dp = dst;
    mlib_u8  *sp = (void *)src;
    mlib_u8  *sa;
    TYPE_64BIT *da;
    mlib_s32 doff, boff = bitoff;

    if ((mlib_addr)dp & 7) {

      /* result of (dp & 7) certainly fits into mlib_s32 */
      doff = 8 - ((mlib_s32) ((mlib_addr)dp & 7));

      if (doff > xsize) doff = xsize;

      for (n = 0; n < doff; n++) {
        dp[n] = table[0][(sp[0] >> (7 - boff)) & 0x1];
        boff++;

        if (boff >= 8) {
          sp++;
          boff -= 8;
        }

        size--;
      }

      dp += doff;
    }

    if (boff) {
      mlib_ImageCopy_bit_na(sp, buff, size, boff, 0);
      sp = buff;
    }

    sa = (mlib_u8*)sp;
    da = (TYPE_64BIT*)dp;
    i  = 0;

    if ((mlib_addr)sa & 1 && size >= 8) {
      *da++ = dd_array[*sa++];
      i += 8;
    }

    for (; i <= (size - 16); i += 16) {
      s0 = *(mlib_u16*)sa;
#ifdef _LITTLE_ENDIAN
      *da++ = dd_array[s0 & 0xFF];
      *da++ = dd_array[s0 >> 8];
#else
      *da++ = dd_array[s0 >> 8];
      *da++ = dd_array[s0 & 0xFF];
#endif /* _LITTLE_ENDIAN */
      sa += 2;
    }

    if (i <= (size - 8)) {
      *da++ = dd_array[*sa++];
      i += 8;
    }

    if (i < size) {

#ifdef _NO_LONGLONG

      mlib_u32 emask;
      val0 = sa[0];
      val1 = p_dd[2*val0];

      if (i < (size - 4)) {
        ((mlib_u32*)da)[0] = val1;
        da = (TYPE_64BIT *) ((mlib_u8 *)da + 4);
        i += 4;
        val1 = p_dd[2*val0+1];
      }

#ifdef _LITTLE_ENDIAN
      emask = (~(mlib_u32)0) >> ((4 - (size - i)) * 8);
#else
      emask = (~(mlib_u32)0) << ((4 - (size - i)) * 8);
#endif /* _LITTLE_ENDIAN */
      ((mlib_u32*)da)[0] = (val1 & emask) | (((mlib_u32*)da)[0] &~ emask);

#else /* _NO_LONGLONG */

#ifdef _LITTLE_ENDIAN
      mlib_u64 emask = (~(mlib_u64)0) >> ((8 - (size - i)) * 8);
#else
      mlib_u64 emask = (~(mlib_u64)0) << ((8 - (size - i)) * 8);
#endif /* _LITTLE_ENDIAN */

      ((mlib_u64*)da)[0] = (((mlib_u64*)dd_array)[sa[0]] & emask) | (((mlib_u64*)da)[0] &~ emask);

#endif /* _NO_LONGLONG */
    }

    src += slb;
    dst += dlb;
  }

  if (buff != (mlib_u8*)buff_lcl) mlib_free(buff);

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageLookUp_Bit_U8_2(const mlib_u8 *src,
                                      mlib_s32      slb,
                                      mlib_u8       *dst,
                                      mlib_s32      dlb,
                                      mlib_s32      xsize,
                                      mlib_s32      ysize,
                                      mlib_s32      nchan,
                                      mlib_s32      bitoff,
                                      const mlib_u8 **table)
{
  mlib_s32 i, j;
  mlib_s32 s0, size;
#ifdef _NO_LONGLONG
  mlib_u32 emask, dd1, dd2;
#else /* _NO_LONGLONG */
  mlib_u64 emask, dd;
#endif /* _NO_LONGLONG */
  DTYPE    dd_array[16];
  mlib_u32 *p_dd = (mlib_u32*)dd_array;
  mlib_d64 buff_lcl[(MAX_WIDTH + MAX_WIDTH/8)/8];
  mlib_u8  *buff = (mlib_u8*)buff_lcl, *buffs;
  mlib_u32 val0, val1;

  size = xsize * 2;

  if (size > MAX_WIDTH) {
    buff = mlib_malloc(size + (size + 7)/8);

    if (buff == NULL) return MLIB_FAILURE;
  }

  buffs = buff + size;

  val0 = table[0][0];
  val1 = table[0][1];
#ifdef _LITTLE_ENDIAN
  val0 = val0 | (table[1][0] << 8);
  val1 = val1 | (table[1][1] << 8);
#else
  val0 = (val0 << 8) | table[1][0];
  val1 = (val1 << 8) | table[1][1];
#endif /* _LITTLE_ENDIAN */
  val0 |= (val0 << 16);
  val1 |= (val1 << 16);

  /* calculate lookup table */
  for (i = 0; i < 4; i++) {
    mlib_u32 v, mask = mlib_bit_mask_2[i];

    v = (val0 &~ mask) | (val1 & mask);

    for (j = 0; j < 4; j++) {
      p_dd[2*(4*i + j)] = v;
      p_dd[2*(i + 4*j) + 1] = v;
    }
  }

  for (j = 0; j < ysize; j++) {
    mlib_u8  *dp = dst;
    mlib_u8  *sp = (void *)src;
    mlib_u8  *sa;
    DTYPE    *da;

    if ((mlib_addr)dp & 7) dp = buff;

    if (bitoff) {
      mlib_ImageCopy_bit_na(sp, buffs, size, bitoff, 0);
      sp = buffs;
    }

    sa = (mlib_u8*)sp;
    da = (DTYPE*)dp;

    for (i = 0; i <= (size - 16); i += 16) {
      s0 = *sa++;
      *da++ = dd_array[s0 >> 4];
      *da++ = dd_array[s0 & 0xF];
    }

    if (i < size) {
      s0 = *sa++;

#ifdef _NO_LONGLONG

      dd1 = p_dd[2*(s0 >> 4)];
      dd2 = p_dd[2*(s0 >> 4)+1];

      if (i < (size - 8)) {
        ((mlib_u32*)da)[0] = dd1;
        ((mlib_u32*)da)[1] = dd2;
        da++;
        i += 8;
        dd1 = p_dd[2*(s0 & 0xf)];
        dd2 = p_dd[2*(s0 & 0xf)+1];
      }

      if (i < (size - 4)) {
        ((mlib_u32*)da)[0] = dd1;
        da = (DTYPE *) ((mlib_u8 *)da + 4);
        i += 4;
        dd1 = dd2;
      }

#ifdef _LITTLE_ENDIAN
      emask = (~(mlib_u32)0) >> ((4 - (size - i)) * 8);
#else
      emask = (~(mlib_u32)0) << ((4 - (size - i)) * 8);
#endif /* _LITTLE_ENDIAN */
      ((mlib_u32*)da)[0] = (dd1 & emask) | (((mlib_u32*)da)[0] &~ emask);

#else /* _NO_LONGLONG */

      dd = ((mlib_u64*)dd_array)[s0 >> 4];

      if (i < (size - 8)) {
        ((mlib_u64*)da)[0] = dd;
        da++;
        i += 8;
        dd = ((mlib_u64*)dd_array)[s0 & 0xf];
      }

#ifdef _LITTLE_ENDIAN
      emask = (~(mlib_u64)0) >> ((8 - (size - i)) * 8);
#else
      emask = (~(mlib_u64)0) << ((8 - (size - i)) * 8);
#endif /* _LITTLE_ENDIAN */
      ((mlib_u64*)da)[0] = (dd & emask) | (((mlib_u64*)da)[0] &~ emask);

#endif /* _NO_LONGLONG */
    }

    if (dp != dst) mlib_ImageCopy_na(dp, dst, size);

    src += slb;
    dst += dlb;
  }

  if (buff != (mlib_u8*)buff_lcl) mlib_free(buff);

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageLookUp_Bit_U8_3(const mlib_u8 *src,
                                      mlib_s32      slb,
                                      mlib_u8       *dst,
                                      mlib_s32      dlb,
                                      mlib_s32      xsize,
                                      mlib_s32      ysize,
                                      mlib_s32      nchan,
                                      mlib_s32      bitoff,
                                      const mlib_u8 **table)
{
  mlib_s32 i, j;
  mlib_s32 s0, size;
  mlib_u32 emask, dd;
  TYPE_64BIT d_array01[16], d_array12[16];
  TYPE_64BIT buff_lcl[(MAX_WIDTH + MAX_WIDTH/8)/8];
  mlib_u8  *buff = (mlib_u8*)buff_lcl, *buffs;
  mlib_u32 l0, h0, v0, l1, h1, v1, l2, h2, v2;

  size = 3 * xsize;

  if (size > MAX_WIDTH) {
    buff = mlib_malloc(size + (size + 7)/8);

    if (buff == NULL) return MLIB_FAILURE;
  }

  buffs = buff + size;

#ifdef _LITTLE_ENDIAN
  l0 = (table[0][0] << 24) | (table[2][0] << 16) | (table[1][0] << 8) | (table[0][0]);
  h0 = (table[0][1] << 24) | (table[2][1] << 16) | (table[1][1] << 8) | (table[0][1]);
  l1 = (l0 >> 8); l1 |= (l1 << 24);
  h1 = (h0 >> 8); h1 |= (h1 << 24);
  l2 = (l1 >> 8); l2 |= (l2 << 24);
  h2 = (h1 >> 8); h2 |= (h2 << 24);
#else
  l0 = (table[0][0] << 24) | (table[1][0] << 16) | (table[2][0] << 8) | (table[0][0]);
  h0 = (table[0][1] << 24) | (table[1][1] << 16) | (table[2][1] << 8) | (table[0][1]);
  l1 = (l0 << 8); l1 |= (l1 >> 24);
  h1 = (h0 << 8); h1 |= (h1 >> 24);
  l2 = (l1 << 8); l2 |= (l2 >> 24);
  h2 = (h1 << 8); h2 |= (h2 >> 24);
#endif /* _LITTLE_ENDIAN */

  /* calculate lookup table */
  for (i = 0; i < 16; i++) {
    mlib_u32 mask0 = mlib_bit_mask_3[i >> 2];
    mlib_u32 mask1 = mlib_bit_mask_3[4 + ((i >> 1) & 3)];
    mlib_u32 mask2 = mlib_bit_mask_3[8 + (i & 3)];

    v0 = (l0 &~ mask0) | (h0 & mask0);
    v1 = (l1 &~ mask1) | (h1 & mask1);
    v2 = (l2 &~ mask2) | (h2 & mask2);

    ((mlib_u32*)d_array01)[2*i    ] = v0;
    ((mlib_u32*)d_array01)[2*i + 1] = v1;
    ((mlib_u32*)d_array12)[2*i    ] = v1;
    ((mlib_u32*)d_array12)[2*i + 1] = v2;
  }

  for (j = 0; j < ysize; j++) {
    mlib_u8  *dp = dst;
    mlib_u8  *sp = (void *)src;
    mlib_u8  *sa;
    mlib_u32 *da;

    if ((mlib_addr)dp & 7) dp = buff;

    if (bitoff) {
      mlib_ImageCopy_bit_na(sp, buffs, size, bitoff, 0);
      sp = buffs;
    }

    sa = (mlib_u8*)sp;
    da = (mlib_u32*)dp;

    for (i = 0; i <= (size - 24); i += 24) {
      d64_2_f32 dd;
      s0 = *sa++;

      ((TYPE_64BIT*)da)[0] = *(d_array01 + (s0 >> 4));

      dd.f32s.f0 = ((TYPE_32BIT*)(d_array12 + (s0 >> 4)))[1];
      dd.f32s.f1 = ((TYPE_32BIT*)(d_array01 + (s0 & 0xF)))[0];
      ((TYPE_64BIT*)da)[1] = dd.d64;
      ((TYPE_64BIT*)da)[2] = *(d_array12 + (s0 & 0xF));

      da += 6;
    }

    if (i < size) {
      s0 = *sa++;
      dd = ((mlib_u32*)(d_array01 + (s0 >> 4)))[0];

      if (i < (size - 4)) {
        *da++ = dd;
        i += 4;
        dd = ((mlib_u32*)(d_array12 + (s0 >> 4)))[0];
      }

      if (i < (size - 4)) {
        *da++ = dd;
        i += 4;
        dd = ((mlib_u32*)(d_array12 + (s0 >> 4)))[1];
      }

      if (i < (size - 4)) {
        *da++ = dd;
        i += 4;
        dd = ((mlib_u32*)(d_array01 + (s0 & 0xF)))[0];
      }

      if (i < (size - 4)) {
        *da++ = dd;
        i += 4;
        dd = ((mlib_u32*)(d_array12 + (s0 & 0xF)))[0];
      }

      if (i < (size - 4)) {
        *da++ = dd;
        i += 4;
        dd = ((mlib_u32*)(d_array12 + (s0 & 0xF)))[1];
      }

#ifdef _LITTLE_ENDIAN
      emask = (~(mlib_u32)0) >> ((4 - (size - i)) * 8);
#else
      emask = (~(mlib_u32)0) << ((4 - (size - i)) * 8);
#endif /* _LITTLE_ENDIAN */
      da[0] = (dd & emask) | (da[0] &~ emask);
    }

    if (dp != dst) mlib_ImageCopy_na(dp, dst, size);

    src += slb;
    dst += dlb;
  }

  if (buff != (mlib_u8*)buff_lcl) mlib_free(buff);

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageLookUp_Bit_U8_4(const mlib_u8 *src,
                                      mlib_s32      slb,
                                      mlib_u8       *dst,
                                      mlib_s32      dlb,
                                      mlib_s32      xsize,
                                      mlib_s32      ysize,
                                      mlib_s32      nchan,
                                      mlib_s32      bitoff,
                                      const mlib_u8 **table)
{
  mlib_s32 i, j;
  mlib_s32 s0, size;
  DTYPE    dd_array0[16], dd_array1[16], lh[4], dd;
  mlib_d64 buff_lcl[(MAX_WIDTH + MAX_WIDTH/8)/8];
  mlib_u8  *buff = (mlib_u8*)buff_lcl, *buffs;
  mlib_u32 l, h;

  size = xsize * 4;

  if (size > MAX_WIDTH) {
    buff = mlib_malloc(size + (size + 7)/8);

    if (buff == NULL) return MLIB_FAILURE;
  }

  buffs = buff + size;

#ifdef _LITTLE_ENDIAN
  l = (table[3][0] << 24) | (table[2][0] << 16) | (table[1][0] << 8) | (table[0][0]);
  h = (table[3][1] << 24) | (table[2][1] << 16) | (table[1][1] << 8) | (table[0][1]);
#else
  l = (table[0][0] << 24) | (table[1][0] << 16) | (table[2][0] << 8) | (table[3][0]);
  h = (table[0][1] << 24) | (table[1][1] << 16) | (table[2][1] << 8) | (table[3][1]);
#endif /* _LITTLE_ENDIAN */

  ((mlib_u32*)lh)[0] = l;  ((mlib_u32*)lh)[1] = l;
  ((mlib_u32*)lh)[2] = l;  ((mlib_u32*)lh)[3] = h;
  ((mlib_u32*)lh)[4] = h;  ((mlib_u32*)lh)[5] = l;
  ((mlib_u32*)lh)[6] = h;  ((mlib_u32*)lh)[7] = h;

  /* calculate lookup table */
  dd_array0[ 0] = lh[0];  dd_array1[ 0] = lh[0];
  dd_array0[ 1] = lh[0];  dd_array1[ 1] = lh[1];
  dd_array0[ 2] = lh[0];  dd_array1[ 2] = lh[2];
  dd_array0[ 3] = lh[0];  dd_array1[ 3] = lh[3];
  dd_array0[ 4] = lh[1];  dd_array1[ 4] = lh[0];
  dd_array0[ 5] = lh[1];  dd_array1[ 5] = lh[1];
  dd_array0[ 6] = lh[1];  dd_array1[ 6] = lh[2];
  dd_array0[ 7] = lh[1];  dd_array1[ 7] = lh[3];
  dd_array0[ 8] = lh[2];  dd_array1[ 8] = lh[0];
  dd_array0[ 9] = lh[2];  dd_array1[ 9] = lh[1];
  dd_array0[10] = lh[2];  dd_array1[10] = lh[2];
  dd_array0[11] = lh[2];  dd_array1[11] = lh[3];
  dd_array0[12] = lh[3];  dd_array1[12] = lh[0];
  dd_array0[13] = lh[3];  dd_array1[13] = lh[1];
  dd_array0[14] = lh[3];  dd_array1[14] = lh[2];
  dd_array0[15] = lh[3];  dd_array1[15] = lh[3];

  for (j = 0; j < ysize; j++) {
    mlib_u8  *dp = dst;
    mlib_u8  *sp = (void *)src;
    mlib_u8  *sa;
    DTYPE    *da;

    if ((mlib_addr)dp & 7) dp = buff;

    if (bitoff) {
      mlib_ImageCopy_bit_na(sp, buffs, size, bitoff, 0);
      sp = buffs;
    }

    sa = (mlib_u8*)sp;
    da = (DTYPE*)dp;

    for (i = 0; i <= (size - 32); i += 32) {
      s0 = *sa++;
      *da++ = dd_array0[s0 >> 4];
      *da++ = dd_array1[s0 >> 4];
      *da++ = dd_array0[s0 & 0xF];
      *da++ = dd_array1[s0 & 0xF];
    }

    if (i < size) {
      s0 = *sa++;
      dd = dd_array0[s0 >> 4];

      if (i <= (size - 8)) {
        *da++ = dd;
        i += 8;
        dd = dd_array1[s0 >> 4];
      }

      if (i <= (size - 8)) {
        *da++ = dd;
        i += 8;
        dd = dd_array0[s0 & 0xF];
      }

      if (i <= (size - 8)) {
        *da++ = dd;
        i += 8;
        dd = dd_array1[s0 & 0xF];
      }

      if (i < size) {
        *(mlib_u32*)da = *(mlib_u32*) & dd;
      }
    }

    if (dp != dst) mlib_ImageCopy_na(dp, dst, size);

    src += slb;
    dst += dlb;
  }

  if (buff != (mlib_u8*)buff_lcl) mlib_free(buff);

  return MLIB_SUCCESS;
}

/***************************************************************/
