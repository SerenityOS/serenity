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
 * FUNCTIONS
 *      mlib_ImageCopy - Direct copy from one image to another.
 *
 * SYNOPSIS
 *      mlib_status mlib_ImageCopy(mlib_image       *dst,
 *                                 const mlib_image *src);
 *
 * ARGUMENT
 *      dst     pointer to output or destination image
 *      src     pointer to input or source image
 *
 * RESTRICTION
 *      src and dst must have the same size, type and number of channels.
 *      They can have 1, 2, 3 or 4 channels of MLIB_BIT, MLIB_BYTE, MLIB_SHORT,
 *      MLIB_USHORT, MLIB_INT, MLIB_FLOAT or MLIB_DOUBLE data type.
 *
 * DESCRIPTION
 *      Direct copy from one image to another
 */

#include <stdlib.h>
#include "mlib_image.h"
#include "mlib_ImageCheck.h"
#include "mlib_ImageCopy.h"

/***************************************************************/
#ifdef _MSC_VER
#pragma optimize("", off)                   /* Fix bug 4195132 */
#endif /* _MSC_VER */

/***************************************************************/
/* do not perform the coping by mlib_d64 data type for x86 */
#ifdef i386

typedef struct {
  mlib_s32 int0, int1;
} two_int;

#define TYPE_64BIT two_int

#else /* i386 */

#define TYPE_64BIT mlib_d64
#endif /* i386 */

/***************************************************************/
static void mlib_c_ImageCopy_u8(const mlib_image *src,
                                mlib_image       *dst);
static void mlib_c_ImageCopy_s16(const mlib_image *src,
                                 mlib_image       *dst);
static void mlib_c_ImageCopy_s32(const mlib_image *src,
                                 mlib_image       *dst);
static void mlib_c_ImageCopy_d64(const mlib_image *src,
                                 mlib_image       *dst);
static void mlib_c_ImageCopy_a1(const TYPE_64BIT *sp,
                                TYPE_64BIT       *dp,
                                mlib_s32         size);

/***************************************************************/
mlib_status mlib_ImageCopy(mlib_image       *dst,
                           const mlib_image *src)
{
  mlib_s32 s_offset, d_offset;
  mlib_s32 size, s_stride, d_stride;
  mlib_s32 width;                                     /* width in bytes of src and dst */
  mlib_s32 height;                                    /* height in lines of src and dst */
  mlib_u8 *sa, *da;
  mlib_s32 j;

  MLIB_IMAGE_CHECK(src);
  MLIB_IMAGE_CHECK(dst);
  MLIB_IMAGE_TYPE_EQUAL(src, dst);
  MLIB_IMAGE_CHAN_EQUAL(src, dst);
  MLIB_IMAGE_SIZE_EQUAL(src, dst);

  switch (mlib_ImageGetType(dst)) {
    case MLIB_BIT:
      width = mlib_ImageGetWidth(dst) * mlib_ImageGetChannels(dst); /* size in bits */
      height = mlib_ImageGetHeight(src);
      sa = (mlib_u8 *) mlib_ImageGetData(src);
      da = (mlib_u8 *) mlib_ImageGetData(dst);

      if (!mlib_ImageIsNotOneDvector(src) && !mlib_ImageIsNotOneDvector(dst)) {
        size = height * (width >> 3);
        if (!mlib_ImageIsNotAligned8(src) && !mlib_ImageIsNotAligned8(dst) && ((size & 7) == 0)) {

          mlib_c_ImageCopy_a1((TYPE_64BIT *) sa, (TYPE_64BIT *) da, size >> 3);
        }
        else {

          mlib_ImageCopy_na(sa, da, size);
        }
      }
      else {
        s_stride = mlib_ImageGetStride(src);
        d_stride = mlib_ImageGetStride(dst);
        s_offset = mlib_ImageGetBitOffset(src); /* in bits */
        d_offset = mlib_ImageGetBitOffset(dst); /* in bits */
        if (s_offset == d_offset) {
          for (j = 0; j < height; j++) {
            mlib_ImageCopy_bit_al(sa, da, width, s_offset);
            sa += s_stride;
            da += d_stride;
          }
        }
        else {
          for (j = 0; j < height; j++) {
            mlib_ImageCopy_bit_na(sa, da, width, s_offset, d_offset);
            sa += s_stride;
            da += d_stride;
          }
        }
      }

      break;
    case MLIB_BYTE:
      mlib_c_ImageCopy_u8(src, dst);
      break;
    case MLIB_SHORT:
    case MLIB_USHORT:
      mlib_c_ImageCopy_s16(src, dst);
      break;
    case MLIB_INT:
    case MLIB_FLOAT:
      mlib_c_ImageCopy_s32(src, dst);
      break;
    case MLIB_DOUBLE:
      mlib_c_ImageCopy_d64(src, dst);
      break;
    default:
      return MLIB_FAILURE;                  /* MLIB_BIT is not supported here */
  }

  return MLIB_SUCCESS;
}

/***************************************************************/
#define PREPAREVARS(type)                                        \
  type *psrc = (type *) mlib_ImageGetData(src);                  \
  type *pdst = (type *) mlib_ImageGetData(dst);                  \
  mlib_s32 src_height = mlib_ImageGetHeight(src);                \
  mlib_s32 src_width  = mlib_ImageGetWidth(src);                 \
  mlib_s32 src_stride = mlib_ImageGetStride(src) / sizeof(type); \
  mlib_s32 dst_stride = mlib_ImageGetStride(dst) / sizeof(type); \
  mlib_s32 chan = mlib_ImageGetChannels(dst);                    \
  mlib_s32 i, j;                                                 \
                                                                 \
  src_width *= chan;                                             \
  if (src_width == src_stride && src_width == dst_stride) {      \
    src_width *= src_height;                                     \
    src_height = 1;                                              \
  }

/***************************************************************/
#define STRIP(pd, ps, w, h, data_type) {                        \
  data_type s0, s1;                                             \
  for ( i = 0; i < h; i++ ) {                                   \
    if ((j = (w & 1)))                                          \
      pd[i * dst_stride] = ps[i * src_stride];                  \
    for (; j < w; j += 2) {                                     \
      s0 = ps[i * src_stride + j];                              \
      s1 = ps[i * src_stride + j + 1];                          \
      pd[i * dst_stride + j]   = s0;                            \
      pd[i * dst_stride + j + 1] = s1;                          \
    }                                                           \
  }                                                             \
}

/***************************************************************/
/*
 * Both bit offsets of source and distination are the same
 */

void mlib_ImageCopy_bit_al(const mlib_u8 *sa,
                           mlib_u8       *da,
                           mlib_s32      size,
                           mlib_s32      offset)
{
  mlib_s32 b_size, i, j;
  TYPE_64BIT *sp, *dp;
  mlib_u8 mask0 = 0xFF;
  mlib_u8 src, mask;

  if (size <= 0) return;

  if (size <= (8 - offset)) {
    mask = mask0 << (8 - size);
    mask >>= offset;
    src = da[0];
    da[0] = (src & (~mask)) | (sa[0] & mask);
    return;
  }

  mask = mask0 >> offset;
  src = da[0];
  da[0] = (src & (~mask)) | (sa[0] & mask);
  da++;
  sa++;
  size = size - 8 + offset;
  b_size = size >> 3;                       /* size in bytes */

  for (j = 0; (j < b_size) && (((mlib_addr) da & 7) != 0); j++)
    *da++ = *sa++;

  if ((((mlib_addr) sa ^ (mlib_addr) da) & 7) == 0) {
    sp = (TYPE_64BIT *) sa;
    dp = (TYPE_64BIT *) da;
    for (i = 0; j <= (b_size - 8); j += 8, i++) {
      dp[i] = sp[i];
    }

    sa += i << 3;
    da += i << 3;
  }
  else {
#ifdef _NO_LONGLONG
    if ((((mlib_addr) sa ^ (mlib_addr) da) & 3) == 0) {
      mlib_u32 *pws, *pwd;

      pws = (mlib_u32 *) sa;
      pwd = (mlib_u32 *) da;
      for (i = 0; j <= (b_size - 4); j += 4, i++) {
        pwd[i] = pws[i];
      }

      sa += i << 2;
      da += i << 2;
    }
    else {
      mlib_u32 *pws, *pwd, src0, src1;
      mlib_s32 lshift = (mlib_addr) sa & 3, rshift;

      pwd = (mlib_u32 *) da;
      pws = (mlib_u32 *) (sa - lshift);
      lshift <<= 3;
      rshift = 32 - lshift;

      src1 = pws[0];
      for (i = 0; j <= (b_size - 4); j += 4, i++) {
        src0 = src1;
        src1 = pws[i + 1];
#ifdef _LITTLE_ENDIAN
        pwd[i] = (src0 >> lshift) | (src1 << rshift);
#else
        pwd[i] = (src0 << lshift) | (src1 >> rshift);
#endif /* _LITTLE_ENDIAN */
      }

      sa += i << 2;
      da += i << 2;
    }

#else
    mlib_u64 *pws, *pwd, src0, src1;
    mlib_s32 lshift = (mlib_s32) ((mlib_addr) sa & 7), rshift;

    pwd = (mlib_u64 *) da;
    pws = (mlib_u64 *) (sa - lshift);
    lshift <<= 3;
    rshift = 64 - lshift;

    src1 = pws[0];
    for (i = 0; j <= (b_size - 8); j += 8, i++) {
      src0 = src1;
      src1 = pws[i + 1];
      pwd[i] = (src0 << lshift) | (src1 >> rshift);
    }

    sa += i << 3;
    da += i << 3;
#endif /* _NO_LONGLONG */
  }

  for (; j < b_size; j++)
    *da++ = *sa++;

  j = size & 7;

  if (j > 0) {
    mask = mask0 << (8 - j);
    src = da[0];
    da[0] = (src & (~mask)) | (sa[0] & mask);
  }
}

/***************************************************************/
void mlib_c_ImageCopy_u8(const mlib_image *src,
                         mlib_image       *dst)
{
  PREPAREVARS(mlib_u8);
  if (src_width < 16) {
    STRIP(pdst, psrc, src_width, src_height, mlib_u8);
    return;
  }

  for (i = 0; i < src_height; i++) {
    mlib_u8 *psrc_row = psrc + i * src_stride, *pdst_row = pdst + i * dst_stride;

    if (!(((mlib_addr) psrc_row ^ (mlib_addr) pdst_row) & 7)) {
      for (j = 0; j < (mlib_s32) ((8 - (mlib_addr) psrc_row) & 7); j++) {
        pdst_row[j] = psrc_row[j];
      }

      for (; j <= (src_width - 8); j += 8) {
        TYPE_64BIT dsrc0 = *((TYPE_64BIT *) (psrc_row + j));

        *((TYPE_64BIT *) (pdst_row + j)) = dsrc0;
      }
    }
    else {

#ifdef _NO_LONGLONG

      for (j = 0; j < (mlib_s32) ((4 - (mlib_addr) pdst_row) & 3); j++) {
        pdst_row[j] = psrc_row[j];
      }

      if (!(((mlib_addr) psrc_row ^ (mlib_addr) pdst_row) & 3)) {
        for (; j <= (src_width - 4); j += 4) {
          *((mlib_s32 *) (pdst_row + j)) = *((mlib_s32 *) (psrc_row + j));
        }
      }
      else {
        mlib_u32 *ps, shl, shr, src0, src1;

        ps = (mlib_u32 *) (psrc_row + j);
        shl = (mlib_addr) ps & 3;
        ps = (mlib_u32 *) ((mlib_addr) ps - shl);
        shl <<= 3;
        shr = 32 - shl;

        src1 = ps[0];
        for (; j <= (src_width - 4); j += 4) {
          src0 = src1;
          src1 = ps[1];
#ifdef _LITTLE_ENDIAN
          *((mlib_s32 *) (pdst_row + j)) = (src0 >> shl) | (src1 << shr);
#else
          *((mlib_s32 *) (pdst_row + j)) = (src0 << shl) | (src1 >> shr);
#endif /* _LITTLE_ENDIAN */
          ps++;
        }
      }

#else

      for (j = 0; j < (mlib_s32) ((8 - (mlib_addr) pdst_row) & 7); j++) {
        pdst_row[j] = psrc_row[j];
      }

      {
        mlib_s32 shl, shr;
        mlib_u64 *ps, src0, src1;

        ps = (mlib_u64 *) (psrc_row + j);
        /* shl and shr are in range [0, 64] */
        shl = (mlib_s32) ((mlib_addr) ps & 7);
        ps = (mlib_u64 *) ((mlib_addr) ps - shl);
        shl <<= 3;
        shr = 64 - shl;

        src1 = ps[0];
        for (; j <= (src_width - 8); j += 8) {
          src0 = src1;
          src1 = ps[1];
#ifdef _LITTLE_ENDIAN
          *((mlib_s64 *) (pdst_row + j)) = (src0 >> shl) | (src1 << shr);
#else
          *((mlib_s64 *) (pdst_row + j)) = (src0 << shl) | (src1 >> shr);
#endif /* _LITTLE_ENDIAN */
          ps++;
        }
      }
#endif /* _NO_LONGLONG */
    }

    for (; j < src_width; j++)
      pdst_row[j] = psrc_row[j];
  }
}

/***************************************************************/
void mlib_c_ImageCopy_s16(const mlib_image       *src,
                          mlib_image *dst)
{
  PREPAREVARS(mlib_u16);
  if (src_width < 8) {
    STRIP(pdst, psrc, src_width, src_height, mlib_u16);
    return;
  }

  for (i = 0; i < src_height; i++) {
    mlib_u16 *psrc_row = psrc + i * src_stride, *pdst_row = pdst + i * dst_stride;

    if (!(((mlib_addr) psrc_row ^ (mlib_addr) pdst_row) & 7)) {
      for (j = 0; j < (mlib_s32) (((8 - (mlib_addr) psrc_row) & 7) >> 1); j++) {
        pdst_row[j] = psrc_row[j];
      }

      for (; j <= (src_width - 4); j += 4) {
        TYPE_64BIT dsrc0 = *((TYPE_64BIT *) (psrc_row + j));

        *((TYPE_64BIT *) (pdst_row + j)) = dsrc0;
      }
    }
    else {

#ifdef _NO_LONGLONG

      if (j = (((mlib_addr) pdst_row & 2) != 0)) {
        pdst_row[0] = psrc_row[0];
      }

      if (!(((mlib_addr) psrc_row ^ (mlib_addr) pdst_row) & 3)) {
        for (; j <= (src_width - 2); j += 2) {
          *((mlib_s32 *) (pdst_row + j)) = *((mlib_s32 *) (psrc_row + j));
        }
      }
      else {
        mlib_u32 *ps, src0, src1;

        ps = (mlib_u32 *) (psrc_row + j - 1);
        src1 = ps[0];
        for (; j <= (src_width - 2); j += 2) {
          src0 = src1;
          src1 = ps[1];
#ifdef _LITTLE_ENDIAN
          *((mlib_s32 *) (pdst_row + j)) = (src0 >> 16) | (src1 << 16);
#else
          *((mlib_s32 *) (pdst_row + j)) = (src0 << 16) | (src1 >> 16);
#endif /* _LITTLE_ENDIAN */
          ps++;
        }
      }

#else

      for (j = 0; j < (mlib_s32) (((8 - (mlib_addr) pdst_row) & 7) >> 1); j++) {
        pdst_row[j] = psrc_row[j];
      }

      {
        mlib_s32 shl, shr;
        mlib_u64 *ps, src0, src1;

        ps = (mlib_u64 *) (psrc_row + j);
        shl = (mlib_s32) ((mlib_addr) ps & 7);
        ps = (mlib_u64 *) ((mlib_addr) ps - shl);
        shl <<= 3;
        shr = 64 - shl;

        src1 = ps[0];
        for (; j <= (src_width - 4); j += 4) {
          src0 = src1;
          src1 = ps[1];
#ifdef _LITTLE_ENDIAN
          *((mlib_s64 *) (pdst_row + j)) = (src0 >> shl) | (src1 << shr);
#else
          *((mlib_s64 *) (pdst_row + j)) = (src0 << shl) | (src1 >> shr);
#endif /* _LITTLE_ENDIAN */
          ps++;
        }
      }
#endif /* _NO_LONGLONG */
    }

    for (; j < src_width; j++)
      pdst_row[j] = psrc_row[j];
  }
}

/***************************************************************/
void mlib_c_ImageCopy_s32(const mlib_image       *src,
                          mlib_image *dst)
{
  PREPAREVARS(mlib_u32);
  if (src_width < 4) {
    STRIP(pdst, psrc, src_width, src_height, mlib_u32);
    return;
  }

  for (i = 0; i < src_height; i++) {
    mlib_u32 *psrc_row = psrc + i * src_stride, *pdst_row = pdst + i * dst_stride;

    if (!(((mlib_addr) psrc_row ^ (mlib_addr) pdst_row) & 7)) {
      j = (mlib_s32) ((mlib_addr) psrc_row & 4) >> 2;
      if (j != 0) {
        pdst_row[0] = psrc_row[0];
      }

      for (; j <= (src_width - 2); j += 2) {
        TYPE_64BIT dsrc0 = *((TYPE_64BIT *) (psrc_row + j));

        *((TYPE_64BIT *) (pdst_row + j)) = dsrc0;
      }
    }
    else {

#ifdef _NO_LONGLONG

      for (j = 0; j <= (src_width - 1); j++) {
        *((mlib_s32 *) (pdst_row + j)) = *((mlib_s32 *) (psrc_row + j));
      }

#else

      {
        mlib_u64 *ps, src0, src1;

        j = (mlib_s32) ((mlib_addr) pdst_row & 4) >> 2;
        if (j != 0) {
          pdst_row[0] = psrc_row[0];
        }
        ps = (mlib_u64 *) (psrc_row + j - 1);
        src1 = ps[0];
        for (; j <= (src_width - 2); j += 2) {
          src0 = src1;
          src1 = ps[1];
#ifdef _LITTLE_ENDIAN
          *((mlib_s64 *) (pdst_row + j)) = (src0 >> 32) | (src1 << 32);
#else
          *((mlib_s64 *) (pdst_row + j)) = (src0 << 32) | (src1 >> 32);
#endif /* _LITTLE_ENDIAN */
          ps++;
        }
      }
#endif /* _NO_LONGLONG */
    }

    for (; j < src_width; j++)
      pdst_row[j] = psrc_row[j];
  }
}

/***************************************************************/
void mlib_c_ImageCopy_d64(const mlib_image       *src,
                          mlib_image *dst)
{
  PREPAREVARS(mlib_d64);
  for (i = 0; i < src_height; i++) {
    mlib_d64 *psrc_row = psrc + i * src_stride, *pdst_row = pdst + i * dst_stride;

    for (j = 0; j < src_width; j++)
      *((mlib_d64 *) (pdst_row + j)) = *((mlib_d64 *) (psrc_row + j));
  }
}

/***************************************************************/
/*
 * Both source and destination image data are 1 - d vectors and
 * 8 - byte aligned. And size is in 8 - bytes.
 */

void mlib_c_ImageCopy_a1(const TYPE_64BIT *sp,
                         TYPE_64BIT       *dp,
                         mlib_s32         size)
{
  mlib_s32 i;

  for (i = 0; i < size; i++) {
    *dp++ = *sp++;
  }
}

/***************************************************************/
#ifndef _NO_LONGLONG
#define TYPE    mlib_u64
#define BSIZE   64
#define SIZE    8
#else
#define TYPE    mlib_u32
#define BSIZE   32
#define SIZE    4
#endif /* _NO_LONGLONG */

/***************************************************************/
void mlib_ImageCopy_na(const mlib_u8 *sp,
                       mlib_u8       *dp,
                       mlib_s32      n)
{
  mlib_s32 shr, shl;
  TYPE *tmp, s0, s1;

  if (((mlib_addr) sp ^ (mlib_addr) dp) & 7) {

    for (; (n > 0) && (mlib_addr) dp & (SIZE - 1); n--)
      *dp++ = *sp++;

#ifdef _NO_LONGLONG

    if (((mlib_addr) sp & (SIZE - 1)) == 0) {
      for (; n > SIZE; n -= SIZE) {
        *(TYPE *) dp = *(TYPE *) sp;
        dp += SIZE;
        sp += SIZE;
      }
    }
    else
#endif /* _NO_LONGLONG */
    {
      tmp = (TYPE *) ((mlib_addr) sp & ~(SIZE - 1));
      /* shl and shr do not exceed 64 here */
      shl = (mlib_s32) (((mlib_addr) sp & (SIZE - 1)) << 3);
      shr = BSIZE - shl;
      s0 = *tmp++;

      for (; n > SIZE; n -= SIZE) {
        s1 = *tmp++;
#ifdef _LITTLE_ENDIAN
        *(TYPE *) dp = (s0 >> shl) | (s1 << shr);
#else
        *(TYPE *) dp = (s0 << shl) | (s1 >> shr);
#endif /* _LITTLE_ENDIAN */
        s0 = s1;
        dp += SIZE;
        sp += SIZE;
      }
    }
  }
  else {
    for (; (n > 0) && (mlib_addr) dp & 7; n--)
      *dp++ = *sp++;

    for (; n > 8; n -= 8) {
      *(TYPE_64BIT *) dp = *(TYPE_64BIT *) sp;
      dp += 8;
      sp += 8;
    }
  }

  for (; n > 0; n--)
    *dp++ = *sp++;
}

/***************************************************************/
#ifdef _MSC_VER
#pragma optimize("", on)
#endif /* _MSC_VER */

/***************************************************************/
