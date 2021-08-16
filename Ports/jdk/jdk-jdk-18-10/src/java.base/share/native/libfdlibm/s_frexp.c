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

/*
 * for non-zero x
 *      x = frexp(arg,&exp);
 * return a double fp quantity x such that 0.5 <= |x| <1.0
 * and the corresponding binary exponent "exp". That is
 *      arg = x*2^exp.
 * If arg is inf, 0.0, or NaN, then frexp(arg,&exp) returns arg
 * with *exp=0.
 */

#include "fdlibm.h"

#ifdef __STDC__
static const double
#else
static double
#endif
two54 =  1.80143985094819840000e+16; /* 0x43500000, 0x00000000 */

#ifdef __STDC__
        double frexp(double x, int *eptr)
#else
        double frexp(x, eptr)
        double x; int *eptr;
#endif
{
        int  hx, ix, lx;
        hx = __HI(x);
        ix = 0x7fffffff&hx;
        lx = __LO(x);
        *eptr = 0;
        if(ix>=0x7ff00000||((ix|lx)==0)) return x;      /* 0,inf,nan */
        if (ix<0x00100000) {            /* subnormal */
            x *= two54;
            hx = __HI(x);
            ix = hx&0x7fffffff;
            *eptr = -54;
        }
        *eptr += (ix>>20)-1022;
        hx = (hx&0x800fffff)|0x3fe00000;
        __HI(x) = hx;
        return x;
}
