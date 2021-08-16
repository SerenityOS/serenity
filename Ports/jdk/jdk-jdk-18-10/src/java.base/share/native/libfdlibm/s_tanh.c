/*
 * Copyright (c) 1998, 2001, Oracle and/or its affiliates. All rights reserved.
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

/* Tanh(x)
 * Return the Hyperbolic Tangent of x
 *
 * Method :
 *                                     x    -x
 *                                    e  - e
 *      0. tanh(x) is defined to be -----------
 *                                     x    -x
 *                                    e  + e
 *      1. reduce x to non-negative by tanh(-x) = -tanh(x).
 *      2.  0      <= x <= 2**-55 : tanh(x) := x*(one+x)
 *                                              -t
 *          2**-55 <  x <=  1     : tanh(x) := -----; t = expm1(-2x)
 *                                             t + 2
 *                                                   2
 *          1      <= x <=  22.0  : tanh(x) := 1-  ----- ; t=expm1(2x)
 *                                                 t + 2
 *          22.0   <  x <= INF    : tanh(x) := 1.
 *
 * Special cases:
 *      tanh(NaN) is NaN;
 *      only tanh(0)=0 is exact for finite argument.
 */

#include "fdlibm.h"

#ifdef __STDC__
static const double one=1.0, two=2.0, tiny = 1.0e-300;
#else
static double one=1.0, two=2.0, tiny = 1.0e-300;
#endif

#ifdef __STDC__
        double tanh(double x)
#else
        double tanh(x)
        double x;
#endif
{
        double t,z;
        int jx,ix;

    /* High word of |x|. */
        jx = __HI(x);
        ix = jx&0x7fffffff;

    /* x is INF or NaN */
        if(ix>=0x7ff00000) {
            if (jx>=0) return one/x+one;    /* tanh(+-inf)=+-1 */
            else       return one/x-one;    /* tanh(NaN) = NaN */
        }

    /* |x| < 22 */
        if (ix < 0x40360000) {          /* |x|<22 */
            if (ix<0x3c800000)          /* |x|<2**-55 */
                return x*(one+x);       /* tanh(small) = small */
            if (ix>=0x3ff00000) {       /* |x|>=1  */
                t = expm1(two*fabs(x));
                z = one - two/(t+two);
            } else {
                t = expm1(-two*fabs(x));
                z= -t/(t+two);
            }
    /* |x| > 22, return +-1 */
        } else {
            z = one - tiny;             /* raised inexact flag */
        }
        return (jx>=0)? z: -z;
}
