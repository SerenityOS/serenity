/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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


#ifndef MLIB_SYSMATH_H
#define MLIB_SYSMATH_H

#include <math.h>
#ifdef _MSC_VER
#define M_PI            3.14159265358979323846
#define M_1_PI          0.31830988618379067154
#endif /* _MSC_VER */

#define mlib_acos       acos
#define mlib_sin        sin
#define mlib_cos        cos
#define mlib_fabs       fabs
#define mlib_ceil       ceil

#ifdef MLIB_LIBCAFEMATH

#include <stdlib.h>

#define mlib_sqrt       mlib_sqrt_cafe
#define mlib_sinf       sinf
#define mlib_cosf       cosf
void mlib_sincosf (float x, float *s, float *c);
#define mlib_sqrtf      mlib_sqrtf_cafe
#define mlib_fabsf      fabsf

double mlib_sqrt_cafe  (double x);
float  mlib_sqrtf_cafe (float  x);

#else

#define mlib_sqrt       sqrt

#define mlib_sinf       (float) sin
#define mlib_cosf       (float) cos
void mlib_sincosf (float x, float *s, float *c);
#define mlib_sqrtf      (float) sqrt
#define mlib_fabsf      (float) fabs

#endif  /* MLIB_LIBCAFEMATH */


  /* internal mathematical functions */

double mlib_sincospi(double x, double *co);
double mlib_atan2i (int y, int x);
int    mlib_ilogb (double x);

#endif /* MLIB_SYSMATH_H */
