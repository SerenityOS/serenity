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

/* IEEE functions
 *      nextafter(x,y)
 *      return the next machine floating-point number of x in the
 *      direction toward y.
 *   Special cases:
 */

#include "fdlibm.h"

#ifdef __STDC__
        double nextafter(double x, double y)
#else
        double nextafter(x,y)
        double x,y;
#endif
{
        int     hx,hy,ix,iy;
        unsigned lx,ly;

        hx = __HI(x);           /* high word of x */
        lx = __LO(x);           /* low  word of x */
        hy = __HI(y);           /* high word of y */
        ly = __LO(y);           /* low  word of y */
        ix = hx&0x7fffffff;             /* |x| */
        iy = hy&0x7fffffff;             /* |y| */

        if(((ix>=0x7ff00000)&&((ix-0x7ff00000)|lx)!=0) ||   /* x is nan */
           ((iy>=0x7ff00000)&&((iy-0x7ff00000)|ly)!=0))     /* y is nan */
           return x+y;
        if(x==y) return x;              /* x=y, return x */
        if((ix|lx)==0) {                        /* x == 0 */
            __HI(x) = hy&0x80000000;    /* return +-minsubnormal */
            __LO(x) = 1;
            y = x*x;
            if(y==x) return y; else return x;   /* raise underflow flag */
        }
        if(hx>=0) {                             /* x > 0 */
            if(hx>hy||((hx==hy)&&(lx>ly))) {    /* x > y, x -= ulp */
                if(lx==0) hx -= 1;
                lx -= 1;
            } else {                            /* x < y, x += ulp */
                lx += 1;
                if(lx==0) hx += 1;
            }
        } else {                                /* x < 0 */
            if(hy>=0||hx>hy||((hx==hy)&&(lx>ly))){/* x < y, x -= ulp */
                if(lx==0) hx -= 1;
                lx -= 1;
            } else {                            /* x > y, x += ulp */
                lx += 1;
                if(lx==0) hx += 1;
            }
        }
        hy = hx&0x7ff00000;
        if(hy>=0x7ff00000) return x+x;  /* overflow  */
        if(hy<0x00100000) {             /* underflow */
            y = x*x;
            if(y!=x) {          /* raise underflow flag */
                __HI(y) = hx; __LO(y) = lx;
                return y;
            }
        }
        __HI(x) = hx; __LO(x) = lx;
        return x;
}
