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

/* __ieee754_atanh(x)
 * Method :
 *    1.Reduced x to positive by atanh(-x) = -atanh(x)
 *    2.For x>=0.5
 *                  1              2x                          x
 *      atanh(x) = --- * log(1 + -------) = 0.5 * log1p(2 * --------)
 *                  2             1 - x                      1 - x
 *
 *      For x<0.5
 *      atanh(x) = 0.5*log1p(2x+2x*x/(1-x))
 *
 * Special cases:
 *      atanh(x) is NaN if |x| > 1 with signal;
 *      atanh(NaN) is that NaN with no signal;
 *      atanh(+-1) is +-INF with signal.
 *
 */

#include "fdlibm.h"

#ifdef __STDC__
static const double one = 1.0, huge = 1e300;
#else
static double one = 1.0, huge = 1e300;
#endif

static double zero = 0.0;

#ifdef __STDC__
        double __ieee754_atanh(double x)
#else
        double __ieee754_atanh(x)
        double x;
#endif
{
        double t;
        int hx,ix;
        unsigned lx;
        hx = __HI(x);           /* high word */
        lx = __LO(x);           /* low word */
        ix = hx&0x7fffffff;
        if ((ix|((lx|(-lx))>>31))>0x3ff00000) /* |x|>1 */
            return (x-x)/(x-x);
        if(ix==0x3ff00000)
            return x/zero;
        if(ix<0x3e300000&&(huge+x)>zero) return x;      /* x<2**-28 */
        __HI(x) = ix;           /* x <- |x| */
        if(ix<0x3fe00000) {             /* x < 0.5 */
            t = x+x;
            t = 0.5*log1p(t+t*x/(one-x));
        } else
            t = 0.5*log1p((x+x)/(one-x));
        if(hx>=0) return t; else return -t;
}
