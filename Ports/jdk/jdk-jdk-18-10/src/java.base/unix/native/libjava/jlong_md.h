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

#ifndef _UNIX_JLONG_MD_H_
#define _UNIX_JLONG_MD_H_

/* Make sure ptrdiff_t is defined */
#include <stddef.h>
#include <stdint.h>  /* For uintptr_t */

#define jlong_high(a)   ((jint)((a)>>32))
#define jlong_low(a)    ((jint)(a))
#define jlong_add(a, b) ((a) + (b))
#define jlong_and(a, b) ((a) & (b))
#define jlong_div(a, b) ((a) / (b))
#define jlong_mul(a, b) ((a) * (b))
#define jlong_neg(a)    (-(a))
#define jlong_not(a)    (~(a))
#define jlong_or(a, b)  ((a) | (b))
#define jlong_shl(a, n) ((a) << (n))
#define jlong_shr(a, n) ((a) >> (n))
#define jlong_sub(a, b) ((a) - (b))
#define jlong_xor(a, b) ((a) ^ (b))
#define jlong_rem(a,b)  ((a) % (b))

/* comparison operators */
#define jlong_ltz(ll)   ((ll)<0)
#define jlong_gez(ll)   ((ll)>=0)
#define jlong_gtz(ll)   ((ll)>0)
#define jlong_eqz(a)    ((a) == 0)
#define jlong_eq(a, b)  ((a) == (b))
#define jlong_ne(a,b)   ((a) != (b))
#define jlong_ge(a,b)   ((a) >= (b))
#define jlong_le(a,b)   ((a) <= (b))
#define jlong_lt(a,b)   ((a) < (b))
#define jlong_gt(a,b)   ((a) > (b))

#define jlong_zero      ((jlong) 0)
#define jlong_one       ((jlong) 1)
#define jlong_minus_one ((jlong) -1)

/* For static variables initialized to zero */
#define jlong_zero_init  ((jlong) 0L)

#ifdef _LP64
  #ifndef jlong_to_ptr
    #define jlong_to_ptr(a) ((void*)(a))
  #endif
  #ifndef ptr_to_jlong
    #define ptr_to_jlong(a) ((jlong)(a))
  #endif
#else
  #ifndef jlong_to_ptr
    #define jlong_to_ptr(a) ((void*)(int)(a))
  #endif
  #ifndef ptr_to_jlong
    #define ptr_to_jlong(a) ((jlong)(int)(a))
  #endif
#endif

#define jint_to_jlong(a)        ((jlong)(a))
#define jlong_to_jint(a)        ((jint)(a))

/* Useful on machines where jlong and jdouble have different endianness. */
#define jlong_to_jdouble_bits(a)
#define jdouble_to_jlong_bits(a)

#define jlong_to_int(a)     ((int)(a))
#define int_to_jlong(a)     ((jlong)(a))
#define jlong_to_uint(a)    ((unsigned int)(a))
#define uint_to_jlong(a)    ((jlong)(a))
#define jlong_to_ptrdiff(a) ((ptrdiff_t)(a))
#define ptrdiff_to_jlong(a) ((jlong)(a))
#define jlong_to_size(a)    ((size_t)(a))
#define size_to_jlong(a)    ((jlong)(a))
#define long_to_jlong(a)    ((jlong)(a))

#endif /* !_UNIX_JLONG_MD_H_ */
