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


#include <stdlib.h>
#include <string.h>
#ifdef MACOSX
#include <unistd.h>
#include <sys/param.h>
#else
#include <malloc.h>
#endif
#include <mlib_types.h>
#include <mlib_sys_proto.h>
#include "mlib_SysMath.h"

/***************************************************************/

#if ! defined ( __MEDIALIB_OLD_NAMES )
#if defined ( __GNUC__ )

  __typeof__ ( __mlib_memmove) mlib_memmove
    __attribute__ ((weak,alias("__mlib_memmove")));
  __typeof__ ( __mlib_malloc) mlib_malloc
    __attribute__ ((weak,alias("__mlib_malloc")));
  __typeof__ ( __mlib_realloc) mlib_realloc
    __attribute__ ((weak,alias("__mlib_realloc")));
  __typeof__ ( __mlib_free) mlib_free
    __attribute__ ((weak,alias("__mlib_free")));
  __typeof__ ( __mlib_memset) mlib_memset
    __attribute__ ((weak,alias("__mlib_memset")));
  __typeof__ ( __mlib_memcpy) mlib_memcpy
    __attribute__ ((weak,alias("__mlib_memcpy")));

void __mlib_sincosf (float x, float *s, float *c);

__typeof__ ( __mlib_sincosf) mlib_sincosf
    __attribute__ ((weak,alias("__mlib_sincosf")));

#else /* defined ( __GNUC__ ) */

#error  "unknown platform"

#endif /* defined ( __GNUC__ ) */
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */

/***************************************************************/

void *__mlib_malloc(mlib_u32 size)
{
#if defined(_MSC_VER) || defined(AIX)
  /*
   * Currently, all MS C compilers for Win32 platforms default to 8 byte
   * alignment. -- from stdlib.h of MS VC++5.0.
   *
   * On AIX, the malloc subroutine returns a pointer to space suitably
   * aligned for the storage of any type of object (see 'man malloc').
   */
  return (void *) malloc(size);
#elif defined(MACOSX)
  return valloc(size);
#else
  return (void *) memalign(8, size);
#endif /* _MSC_VER */
}

void *__mlib_realloc(void *ptr, mlib_u32 size)
{
  return realloc(ptr, size);
}

void __mlib_free(void *ptr)
{
  free(ptr);
}

void *__mlib_memset(void *s, mlib_s32 c, mlib_u32 n)
{
  return memset(s, c, n);
}

void *__mlib_memcpy(void *s1, void *s2, mlib_u32 n)
{
  return memcpy(s1, s2, n);
}

void *__mlib_memmove(void *s1, void *s2, mlib_u32 n)
{
  return memmove(s1, s2, n);
}

void __mlib_sincosf (mlib_f32 x, mlib_f32 *s, mlib_f32 *c)
{
  *s = (mlib_f32)sin(x);
  *c = (mlib_f32)cos(x);
}
