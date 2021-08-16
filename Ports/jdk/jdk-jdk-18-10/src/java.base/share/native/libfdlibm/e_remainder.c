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

/* __ieee754_remainder(x,p)
 * Return :
 *      returns  x REM p  =  x - [x/p]*p as if in infinite
 *      precise arithmetic, where [x/p] is the (infinite bit)
 *      integer nearest x/p (in half way case choose the even one).
 * Method :
 *      Based on fmod() return x-[x/p]chopped*p exactlp.
 */

#include "fdlibm.h"

#ifdef __STDC__
static const double zero = 0.0;
#else
static double zero = 0.0;
#endif


#ifdef __STDC__
        double __ieee754_remainder(double x, double p)
#else
        double __ieee754_remainder(x,p)
        double x,p;
#endif
{
        int hx,hp;
        unsigned sx,lx,lp;
        double p_half;

        hx = __HI(x);           /* high word of x */
        lx = __LO(x);           /* low  word of x */
        hp = __HI(p);           /* high word of p */
        lp = __LO(p);           /* low  word of p */
        sx = hx&0x80000000;
        hp &= 0x7fffffff;
        hx &= 0x7fffffff;

    /* purge off exception values */
        if((hp|lp)==0) return (x*p)/(x*p);      /* p = 0 */
        if((hx>=0x7ff00000)||                   /* x not finite */
          ((hp>=0x7ff00000)&&                   /* p is NaN */
          (((hp-0x7ff00000)|lp)!=0)))
            return (x*p)/(x*p);


        if (hp<=0x7fdfffff) x = __ieee754_fmod(x,p+p);  /* now x < 2p */
        if (((hx-hp)|(lx-lp))==0) return zero*x;
        x  = fabs(x);
        p  = fabs(p);
        if (hp<0x00200000) {
            if(x+x>p) {
                x-=p;
                if(x+x>=p) x -= p;
            }
        } else {
            p_half = 0.5*p;
            if(x>p_half) {
                x-=p;
                if(x>=p_half) x -= p;
            }
        }
        __HI(x) ^= sx;
        return x;
}
