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

/* __ieee754_sinh(x)
 * Method :
 * mathematically sinh(x) if defined to be (exp(x)-exp(-x))/2
 *      1. Replace x by |x| (sinh(-x) = -sinh(x)).
 *      2.
 *                                                  E + E/(E+1)
 *          0        <= x <= 22     :  sinh(x) := --------------, E=expm1(x)
 *                                                      2
 *
 *          22       <= x <= lnovft :  sinh(x) := exp(x)/2
 *          lnovft   <= x <= ln2ovft:  sinh(x) := exp(x/2)/2 * exp(x/2)
 *          ln2ovft  <  x           :  sinh(x) := x*shuge (overflow)
 *
 * Special cases:
 *      sinh(x) is |x| if x is +INF, -INF, or NaN.
 *      only sinh(0)=0 is exact for finite x.
 */

#include "fdlibm.h"

#ifdef __STDC__
static const double one = 1.0, shuge = 1.0e307;
#else
static double one = 1.0, shuge = 1.0e307;
#endif

#ifdef __STDC__
        double __ieee754_sinh(double x)
#else
        double __ieee754_sinh(x)
        double x;
#endif
{
        double t,w,h;
        int ix,jx;
        unsigned lx;

    /* High word of |x|. */
        jx = __HI(x);
        ix = jx&0x7fffffff;

    /* x is INF or NaN */
        if(ix>=0x7ff00000) return x+x;

        h = 0.5;
        if (jx<0) h = -h;
    /* |x| in [0,22], return sign(x)*0.5*(E+E/(E+1))) */
        if (ix < 0x40360000) {          /* |x|<22 */
            if (ix<0x3e300000)          /* |x|<2**-28 */
                if(shuge+x>one) return x;/* sinh(tiny) = tiny with inexact */
            t = expm1(fabs(x));
            if(ix<0x3ff00000) return h*(2.0*t-t*t/(t+one));
            return h*(t+t/(t+one));
        }

    /* |x| in [22, log(maxdouble)] return 0.5*exp(|x|) */
        if (ix < 0x40862E42)  return h*__ieee754_exp(fabs(x));

    /* |x| in [log(maxdouble), overflowthresold] */
        lx = *( (((*(unsigned*)&one)>>29)) + (unsigned*)&x);
        if (ix<0x408633CE || ((ix==0x408633ce)&&(lx<=(unsigned)0x8fb9f87d))) {
            w = __ieee754_exp(0.5*fabs(x));
            t = h*w;
            return t*w;
        }

    /* |x| > overflowthresold, sinh(x) overflow */
        return x*shuge;
}
