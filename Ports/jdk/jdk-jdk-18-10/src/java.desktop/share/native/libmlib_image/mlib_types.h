/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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


#ifndef MLIB_TYPES_H
#define MLIB_TYPES_H

#include <limits.h>
#if defined(_MSC_VER)
#include <float.h>                      /* for FLT_MAX and DBL_MAX */
#endif

#ifndef DBL_MAX
#define DBL_MAX 1.7976931348623157E+308 /* max decimal value of a "double" */
#endif

#ifndef FLT_MAX
#define FLT_MAX 3.402823466E+38F        /* max decimal value of a "float" */
#endif

#ifndef FLT_MIN
#define FLT_MIN 1.175494351e-38F        /* min normalised value of a "float" */
#endif

#ifdef  __cplusplus
extern "C" {
#endif

typedef char               mlib_s8;
typedef unsigned char      mlib_u8;
typedef short              mlib_s16;
typedef unsigned short     mlib_u16;
typedef int                mlib_s32;
typedef unsigned int       mlib_u32;
typedef float              mlib_f32;
typedef double             mlib_d64;

#if defined(__GNUC__) || defined(_AIX)

#include <stdint.h>
#include <stddef.h>

#if defined(MLIB_OS64BIT) || (defined(MACOSX) && defined(_LP64))

typedef long               mlib_s64;
typedef unsigned long      mlib_u64;

#define MLIB_S64_MIN       LONG_MIN
#define MLIB_S64_MAX       LONG_MAX

#define MLIB_S64_CONST(x)  x##L
#define MLIB_U64_CONST(x)  x##UL

#elif (__STDC__ - 0 == 0) || defined(__GNUC__)

#if defined(_NO_LONGLONG)

typedef union {
  mlib_d64 d64;
  mlib_s32 s32[2];
} mlib_s64;

typedef union {
  mlib_d64 d64;
  mlib_u32 u32[2];
} mlib_u64;

#else

typedef long long          mlib_s64;
typedef unsigned long long mlib_u64;

#define MLIB_S64_MIN       LLONG_MIN
#define MLIB_S64_MAX       LLONG_MAX

#define MLIB_S64_CONST(x)  x##LL
#define MLIB_U64_CONST(x)  x##ULL

#endif /* !defined(_NO_LONGLONG) */

#endif  /* MLIB_OS64BIT */

#elif defined(_MSC_VER)

#if defined(_NO_LONGLONG)

typedef union {
  mlib_d64 d64;
  mlib_s32 s32[2];
} mlib_s64;

typedef union {
  mlib_d64 d64;
  mlib_u32 u32[2];
} mlib_u64;

#else

typedef __int64            mlib_s64;
typedef unsigned __int64   mlib_u64;

#define MLIB_S64_MIN       _I64_MIN
#define MLIB_S64_MAX       _I64_MAX

#define MLIB_S64_CONST(x)  x##I64
#define MLIB_U64_CONST(x)  x##UI64

#endif /* !defined(_NO_LONGLONG) */

#include <stddef.h>
#if !defined(_WIN64)
typedef int                intptr_t;
typedef unsigned int       uintptr_t;
#endif  /* _WIN64 */

#else

#error  "unknown platform"

#endif

typedef uintptr_t          mlib_addr;
typedef void*              mlib_ras;

#define MLIB_S8_MIN        SCHAR_MIN
#define MLIB_S8_MAX        SCHAR_MAX
#define MLIB_U8_MIN        0
#define MLIB_U8_MAX        UCHAR_MAX
#define MLIB_S16_MIN       SHRT_MIN
#define MLIB_S16_MAX       SHRT_MAX
#define MLIB_U16_MIN       0
#define MLIB_U16_MAX       USHRT_MAX
#define MLIB_S32_MIN       INT_MIN
#define MLIB_S32_MAX       INT_MAX
#define MLIB_U32_MIN       0
#define MLIB_U32_MAX       UINT_MAX
#define MLIB_F32_MIN      -FLT_MAX
#define MLIB_F32_MAX       FLT_MAX
#define MLIB_D64_MIN      -DBL_MAX
#define MLIB_D64_MAX       DBL_MAX

#ifdef  __cplusplus
}
#endif

#endif  /* MLIB_TYPES_H */
