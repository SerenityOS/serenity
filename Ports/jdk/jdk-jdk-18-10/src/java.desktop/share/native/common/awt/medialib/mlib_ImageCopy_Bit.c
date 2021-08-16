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
 *      mlib_ImageCopy_bit_na     - BIT, non-aligned
 *      mlib_ImageCopy_bit_na_r   - BIT, non-aligned, reverse
 *
 * SYNOPSIS
 *
 *      void mlib_ImageCopy_bit_na(const mlib_u8 *sa,
 *                                 mlib_u8       *da,
 *                                 mlib_s32      size,
 *                                 mlib_s32      s_offset,
 *                                 mlib_s32      d_offset);
 *      void mlib_ImageCopy_bit_na_r(const mlib_u8 *sa,
 *                                   mlib_u8       *da,
 *                                   mlib_s32      size,
 *                                   mlib_s32      s_offset,
 *                                   mlib_s32      d_offset);
 * ARGUMENT
 *      sp       pointer to source image data
 *      dp       pointer to destination image data
 *      size     size in 8-bytes, bytes, or SHORTs
 *      width    image width in 8-bytes
 *      height   image height in lines
 *      stride   source image line stride in 8-bytes
 *      dstride  destination image line stride in 8-bytes
 *      s_offset source image line bit offset
 *      d_offset destination image line bit offset
 *
 * DESCRIPTION
 *      Direct copy from one image to another -- C version low level
 *      functions.
 */

#include <stdlib.h>
#include "mlib_image.h"
#include "mlib_ImageCopy.h"

/***************************************************************/
/*
 * Bit offsets of source and distination are not the same
 */

void mlib_ImageCopy_bit_na(const mlib_u8 *sa,
                           mlib_u8       *da,
                           mlib_s32      size,
                           mlib_s32      s_offset,
                           mlib_s32      d_offset)
{
#ifdef _NO_LONGLONG

  mlib_u32 *dp;          /* 4-byte aligned start points in dst */
  mlib_u32 *sp;          /* 4-byte aligned start point in src */
  mlib_s32 j;            /* offset of address in dst */
  mlib_u32 mask0 = 0xFFFFFFFF;
  mlib_u32 dmask;
  mlib_u32 src, src0, src1, dst;
  mlib_s32 ls_offset, ld_offset, shift;

  if (size <= 0) return;

  /* prepare the destination addresses */
  dp = (mlib_u32 *)((mlib_addr)da & (~3));
  sp = (mlib_u32 *)((mlib_addr)sa & (~3));
  ld_offset = (((mlib_addr)da & 3) << 3) + d_offset;     /* bit d_offset to first mlib_s32 */
  ls_offset = (((mlib_addr)sa & 3) << 3) + s_offset;     /* bit d_offset to first mlib_s32 */

  if (ld_offset > ls_offset) {
    src0 = sp[0];
    dst = dp[0];
    if (ld_offset + size < 32) {
      dmask = (mask0 << (32 - size)) >> ld_offset;
#ifdef _LITTLE_ENDIAN
      src0 = (src0 << 24) | ((src0 & 0xFF00) << 8) | ((src0 >> 8) & 0xFF00) | (src0 >> 24);
      src = (src0 >> (ld_offset - ls_offset));
      dst = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
      dst = (dst & (~dmask)) | (src & dmask);
      dp[0] = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
#else
      src = (src0 >> (ld_offset - ls_offset));
      dp[0] = (dst & (~dmask)) | (src & dmask);
#endif /* _LITTLE_ENDIAN */
      return;
    }

    dmask = mask0 >> ld_offset;
#ifdef _LITTLE_ENDIAN
    src0 = (src0 << 24) | ((src0 & 0xFF00) << 8) | ((src0 >> 8) & 0xFF00) | (src0 >> 24);
    src = (src0 >> (ld_offset - ls_offset));
    dst = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
    dst = (dst & ~dmask) | (src & dmask);
    dp[0] = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
#else
    src = (src0 >> (ld_offset - ls_offset));
    dp[0] = (dst & ~dmask) | (src & dmask);
#endif /* _LITTLE_ENDIAN */
    j = 32 - ld_offset;
    dp++;
    ls_offset += j;
  } else {

    shift = ls_offset - ld_offset;
    src0 = sp[0];
    if (ls_offset + size > 32) src1 = sp[1];
    dst = dp[0];

    if (ld_offset + size < 32) {
      dmask = (mask0 << (32 - size)) >> ld_offset;
#ifdef _LITTLE_ENDIAN
      src0 = (src0 << 24) | ((src0 & 0xFF00) << 8) | ((src0 >> 8) & 0xFF00) | (src0 >> 24);
      src1 = (src1 << 24) | ((src1 & 0xFF00) << 8) | ((src1 >> 8) & 0xFF00) | (src1 >> 24);
      src = (src0 << shift) | (src1 >> (32 - shift));
      dst = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
      dst = (dst & ~dmask) | (src & dmask);
      dp[0] = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
#else
      src = (src0 << shift) | (src1 >> (32 - shift));
      dp[0] = (dst & ~dmask) | (src & dmask);
#endif /* _LITTLE_ENDIAN */
      return;
    }

    dmask = mask0 >> ld_offset;
#ifdef _LITTLE_ENDIAN
    src0 = (src0 << 24) | ((src0 & 0xFF00) << 8) | ((src0 >> 8) & 0xFF00) | (src0 >> 24);
    src1 = (src1 << 24) | ((src1 & 0xFF00) << 8) | ((src1 >> 8) & 0xFF00) | (src1 >> 24);
    src = (src0 << shift) | (src1 >> (32 - shift));
    dst = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
    dst = (dst & ~dmask) | (src & dmask);
    dp[0] = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
#else
    src = (src0 << shift) | (src1 >> (32 - shift));
    dp[0] = (dst & ~dmask) | (src & dmask);
#endif /* _LITTLE_ENDIAN */
    j = 32 - ld_offset;
    dp++;
    sp++;
    ls_offset = ls_offset + j - 32;
  }

  if (j < size) src1 = sp[0];
#ifdef _LITTLE_ENDIAN
  src1 = (src1 << 24) | ((src1 & 0xFF00) << 8) | ((src1 >> 8) & 0xFF00) | (src1 >> 24);
#endif /* _LITTLE_ENDIAN */
  for (; j <= size - 32; j += 32) {
    src0 = src1;
    src1 = sp[1];
#ifdef _LITTLE_ENDIAN
    src1 = (src1 << 24) | ((src1 & 0xFF00) << 8) | ((src1 >> 8) & 0xFF00) | (src1 >> 24);
    src = (src0 << ls_offset) | (src1 >> (32 - ls_offset));
    dp[0] = (src << 24) | ((src & 0xFF00) << 8) | ((src >> 8) & 0xFF00) | (src >> 24);
#else
    dp[0] = (src0 << ls_offset) | (src1 >> (32 - ls_offset));
#endif /* _LITTLE_ENDIAN */
    sp++;
    dp++;
  }

  if (j < size) {
    j = size - j;
    src0 = src1;
    if (ls_offset + j > 32) src1 = sp[1];
    dst = dp[0];
    dmask = mask0 << (32 - j);
#ifdef _LITTLE_ENDIAN
    src1 = (src1 << 24) | ((src1 & 0xFF00) << 8) | ((src1 >> 8) & 0xFF00) | (src1 >> 24);
    src = (src0 << ls_offset) | (src1 >> (32 - ls_offset));
    dst = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
    dst = (dst & ~dmask) | (src & dmask);
    dp[0] = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
#else
    src = (src0 << ls_offset) | (src1 >> (32 - ls_offset));
    dp[0] = (dst & ~dmask) | (src & dmask);
#endif /* _LITTLE_ENDIAN */
  }

#else /* _LONGLONG */

  mlib_u64 *dp;          /* 8-byte aligned start points in dst */
  mlib_u64 *sp;          /* 8-byte aligned start point in src */
  mlib_s32 j;            /* offset of address in dst */
  mlib_u64 lmask0 = 0xFFFFFFFFFFFFFFFFULL;
  mlib_u64 dmask;
  mlib_u64 lsrc, lsrc0, lsrc1 = 0ULL, ldst;
  mlib_s32 ls_offset, ld_offset, shift;

  if (size <= 0) return;

  /* prepare the destination addresses */
  dp = (mlib_u64 *)((mlib_addr)da & (~7));
  sp = (mlib_u64 *)((mlib_addr)sa & (~7));
  /* we can explicitly cast ro mlib_s32 here because value is in [0,64] range */
  ld_offset = (((mlib_s32) ((mlib_addr)da & 7)) << 3) + d_offset;     /* bit d_offset to first mlib_d64 */
  ls_offset = (((mlib_s32) ((mlib_addr)sa & 7)) << 3) + s_offset;     /* bit d_offset to first mlib_d64 */

  if (ld_offset > ls_offset) {
    lsrc0 = sp[0];
    ldst = dp[0];
    lsrc = (lsrc0 >> (ld_offset - ls_offset));
    if (ld_offset + size < 64) {
      dmask = (lmask0 << (64 - size)) >> ld_offset;
      dp[0] = (ldst & (~dmask)) | (lsrc & dmask);
      return;
    }

    dmask = lmask0 >> ld_offset;
    dp[0] = (ldst & ~dmask) | (lsrc & dmask);
    j = 64 - ld_offset;
    dp++;
    ls_offset += j;
  } else {

    shift = ls_offset - ld_offset;
    lsrc0 = sp[0];
    if (ls_offset + size > 64) lsrc1 = sp[1];
    ldst = dp[0];
    lsrc = (lsrc0 << shift) | (lsrc1 >> (64 - shift));

    if (ld_offset + size < 64) {
      dmask = (lmask0 << (64 - size)) >> ld_offset;
      dp[0] = (ldst & ~dmask) | (lsrc & dmask);
      return;
    }

    dmask = lmask0 >> ld_offset;
    dp[0] = (ldst & ~dmask) | (lsrc & dmask);
    j = 64 - ld_offset;
    dp++;
    sp++;
    ls_offset = ls_offset + j - 64;
  }

  if (j < size) lsrc1 = sp[0];
  for (; j <= size - 64; j += 64) {
    lsrc0 = lsrc1;
    lsrc1 = sp[1];
    lsrc = (lsrc0 << ls_offset) | (lsrc1 >> (64 - ls_offset));
    dp[0] = lsrc;
    sp++;
    dp++;
  }

  if (j < size) {
    j = size - j;
    lsrc0 = lsrc1;
    if (ls_offset + j > 64) lsrc1 = sp[1];
    ldst = dp[0];
    dmask = lmask0 << (64 - j);
    lsrc = (lsrc0 << ls_offset) | (lsrc1 >> (64 - ls_offset));
    dp[0] = (ldst & ~dmask) | (lsrc & dmask);
  }
#endif /* _NO_LONGLONG */
}

/***************************************************************/
/*
 * Bit offsets of source and distination are not the same
 * This function is both for C and VIS version (LONGLONG case)
 */

void mlib_ImageCopy_bit_na_r(const mlib_u8 *sa,
                             mlib_u8       *da,
                             mlib_s32      size,
                             mlib_s32      s_offset,
                             mlib_s32      d_offset)
{
#ifdef _NO_LONGLONG

  mlib_u32 *dp;          /* 4-byte aligned start points in dst */
  mlib_u32 *sp;          /* 4-byte aligned start point in src */
  mlib_s32 j;            /* offset of address in dst */
  mlib_u32 lmask0 = 0xFFFFFFFF;
  mlib_u32 dmask;
  mlib_u32 src, src0, src1, dst;
  mlib_s32 ls_offset, ld_offset, shift;

  if (size <= 0) return;

  /* prepare the destination addresses */
  dp = (mlib_u32 *)((mlib_addr)da & (~3));
  sp = (mlib_u32 *)((mlib_addr)sa & (~3));
  ld_offset = (((mlib_addr)da & 3) << 3) + d_offset;     /* bit d_offset to first mlib_s32 */
  ls_offset = (((mlib_addr)sa & 3) << 3) + s_offset;     /* bit d_offset to first mlib_s32 */

  if (ld_offset < ls_offset) {
    src0 = sp[0];
    dst = dp[0];
    if (ld_offset >= size) {
      dmask = (lmask0 << (32 - size)) >> (ld_offset - size);
#ifdef _LITTLE_ENDIAN
      src0 = (src0 << 24) | ((src0 & 0xFF00) << 8) | ((src0 >> 8) & 0xFF00) | (src0 >> 24);
      src = (src0 << (ls_offset - ld_offset));
      dst = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
      dst = (dst & (~dmask)) | (src & dmask);
      dp[0] = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
#else
      src = (src0 << (ls_offset - ld_offset));
      dp[0] = (dst & (~dmask)) | (src & dmask);
#endif /* _LITTLE_ENDIAN */
      return;
    }

    dmask = lmask0 << (32 - ld_offset);
#ifdef _LITTLE_ENDIAN
    src0 = (src0 << 24) | ((src0 & 0xFF00) << 8) | ((src0 >> 8) & 0xFF00) | (src0 >> 24);
    src = (src0 << (ls_offset - ld_offset));
    dst = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
    dst = (dst & ~dmask) | (src & dmask);
    dp[0] = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
#else
    src = (src0 << (ls_offset - ld_offset));
    dp[0] = (dst & ~dmask) | (src & dmask);
#endif /* _LITTLE_ENDIAN */
    j = ld_offset;
    dp--;
    ls_offset -= j;
  } else {

    shift = ld_offset - ls_offset;
    src0 = sp[0];
    if (ls_offset < size) src1 = sp[-1];
    dst = dp[0];

    if (ld_offset >= size) {
      dmask = (lmask0 << (32 - size)) >> (ld_offset - size);
#ifdef _LITTLE_ENDIAN
      src0 = (src0 << 24) | ((src0 & 0xFF00) << 8) | ((src0 >> 8) & 0xFF00) | (src0 >> 24);
      src1 = (src1 << 24) | ((src1 & 0xFF00) << 8) | ((src1 >> 8) & 0xFF00) | (src1 >> 24);
      src = (src0 >> shift) | (src1 << (32 - shift));
      dst = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
      dst = (dst & ~dmask) | (src & dmask);
      dp[0] = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
#else
      src = (src0 >> shift) | (src1 << (32 - shift));
      dp[0] = (dst & ~dmask) | (src & dmask);
#endif /* _LITTLE_ENDIAN */
      return;
    }

    dmask = lmask0 << (32 - ld_offset);
#ifdef _LITTLE_ENDIAN
    src0 = (src0 << 24) | ((src0 & 0xFF00) << 8) | ((src0 >> 8) & 0xFF00) | (src0 >> 24);
    src1 = (src1 << 24) | ((src1 & 0xFF00) << 8) | ((src1 >> 8) & 0xFF00) | (src1 >> 24);
    src = (src0 >> shift) | (src1 << (32 - shift));
    dst = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
    dst = (dst & ~dmask) | (src & dmask);
    dp[0] = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
#else
    src = (src0 >> shift) | (src1 << (32 - shift));
    dp[0] = (dst & ~dmask) | (src & dmask);
#endif /* _LITTLE_ENDIAN */
    j = ld_offset;
    dp--;
    sp--;
    ls_offset = ls_offset - j + 32;
  }

  if (j < size) src1 = sp[0];
#ifdef _LITTLE_ENDIAN
  src1 = (src1 << 24) | ((src1 & 0xFF00) << 8) | ((src1 >> 8) & 0xFF00) | (src1 >> 24);
#endif /* _LITTLE_ENDIAN */
  for (; j <= size - 32; j += 32) {
    src0 = src1;
    src1 = sp[-1];
#ifdef _LITTLE_ENDIAN
    src1 = (src1 << 24) | ((src1 & 0xFF00) << 8) | ((src1 >> 8) & 0xFF00) | (src1 >> 24);
    src = (src0 >> (32 - ls_offset)) | (src1 << ls_offset);
    dp[0] = (src << 24) | ((src & 0xFF00) << 8) | ((src >> 8) & 0xFF00) | (src >> 24);
#else
    dp[0] = (src0 >> (32 - ls_offset)) | (src1 << ls_offset);
#endif /* _LITTLE_ENDIAN */
    sp--;
    dp--;
  }

  if (j < size) {
    j = size - j;
    src0 = src1;
    if (ls_offset < j) src1 = sp[-1];
    dst = dp[0];
    dmask = lmask0 >> (32 - j);
#ifdef _LITTLE_ENDIAN
    src1 = (src1 << 24) | ((src1 & 0xFF00) << 8) | ((src1 >> 8) & 0xFF00) | (src1 >> 24);
    src = (src0 >> (32 - ls_offset)) | (src1 << ls_offset);
    dst = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
    dst = (dst & ~dmask) | (src & dmask);
    dp[0] = (dst << 24) | ((dst & 0xFF00) << 8) | ((dst >> 8) & 0xFF00) | (dst >> 24);
#else
    src = (src0 >> (32 - ls_offset)) | (src1 << ls_offset);
    dp[0] = (dst & ~dmask) | (src & dmask);
#endif /* _LITTLE_ENDIAN */
  }

#else  /* _LONGLONG */

  mlib_u64 *dp;          /* 8-byte aligned start points in dst */
  mlib_u64 *sp;          /* 8-byte aligned start point in src */
  mlib_s32 j;            /* offset of address in dst */
  mlib_u64 lmask0 = 0xFFFFFFFFFFFFFFFFULL;
  mlib_u64 dmask;
  mlib_u64 lsrc, lsrc0, lsrc1 = 0ULL, ldst;
  mlib_s32 ls_offset, ld_offset, shift;

  if (size <= 0) return;

  /* prepare the destination addresses */
  dp = (mlib_u64 *)((mlib_addr)da & (~7));
  sp = (mlib_u64 *)((mlib_addr)sa & (~7));
  /* we can explicitly cast ro mlib_s32 here because value is in [0,64] range */
  ld_offset = (((mlib_s32) ((mlib_addr)da & 7)) << 3) + d_offset;     /* bit d_offset to first mlib_d64 */
  ls_offset = (((mlib_s32) ((mlib_addr)sa & 7)) << 3) + s_offset;     /* bit d_offset to first mlib_d64 */

  if (ld_offset < ls_offset) {
    lsrc0 = sp[0];
    ldst = dp[0];
    lsrc = (lsrc0 << (ls_offset - ld_offset));
    if (ld_offset >= size) {
      dmask = (lmask0 << (64 - size)) >> (ld_offset - size);
      dp[0] = (ldst & (~dmask)) | (lsrc & dmask);
      return;
    }

    dmask = lmask0 << (64 - ld_offset);
    dp[0] = (ldst & ~dmask) | (lsrc & dmask);
    j = ld_offset;
    dp--;
    ls_offset -= j;
  } else {

    shift = ld_offset - ls_offset;
    lsrc0 = sp[0];
    if (ls_offset < size) lsrc1 = sp[-1];
    ldst = dp[0];
    lsrc = (lsrc0 >> shift) | (lsrc1 << (64 - shift));
    if (ld_offset >= size) {
      dmask = (lmask0 << (64 - size)) >> (ld_offset - size);
      dp[0] = (ldst & ~dmask) | (lsrc & dmask);
      return;
    }

    dmask = lmask0 << (64 - ld_offset);
    dp[0] = (ldst & ~dmask) | (lsrc & dmask);
    j = ld_offset;
    dp--;
    sp--;
    ls_offset = ls_offset - j + 64;
  }

  if (j < size) lsrc1 = sp[0];
  for (; j <= size - 64; j += 64) {
    lsrc0 = lsrc1;
    lsrc1 = sp[-1];
    dp[0] = (lsrc0 >> (64 - ls_offset)) | (lsrc1 << ls_offset);
    sp--;
    dp--;
  }

  if (j < size) {
    j = size - j;
    lsrc0 = lsrc1;
    if (ls_offset < j) lsrc1 = sp[-1];
    ldst = dp[0];
    dmask = lmask0 >> (64 - j);
    lsrc = (lsrc0 >> (64 - ls_offset)) | (lsrc1 << ls_offset);
    dp[0] = (ldst & ~dmask) | (lsrc & dmask);
  }
#endif /* _NO_LONGLONG */
}

/***************************************************************/
