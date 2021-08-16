/*
 * Copyright (c) 1998, 2004, Oracle and/or its affiliates. All rights reserved.
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

/* __ieee754_atan2(y,x)
 * Method :
 *      1. Reduce y to positive by atan2(y,x)=-atan2(-y,x).
 *      2. Reduce x to positive by (if x and y are unexceptional):
 *              ARG (x+iy) = arctan(y/x)           ... if x > 0,
 *              ARG (x+iy) = pi - arctan[y/(-x)]   ... if x < 0,
 *
 * Special cases:
 *
 *      ATAN2((anything), NaN ) is NaN;
 *      ATAN2(NAN , (anything) ) is NaN;
 *      ATAN2(+-0, +(anything but NaN)) is +-0  ;
 *      ATAN2(+-0, -(anything but NaN)) is +-pi ;
 *      ATAN2(+-(anything but 0 and NaN), 0) is +-pi/2;
 *      ATAN2(+-(anything but INF and NaN), +INF) is +-0 ;
 *      ATAN2(+-(anything but INF and NaN), -INF) is +-pi;
 *      ATAN2(+-INF,+INF ) is +-pi/4 ;
 *      ATAN2(+-INF,-INF ) is +-3pi/4;
 *      ATAN2(+-INF, (anything but,0,NaN, and INF)) is +-pi/2;
 *
 * Constants:
 * The hexadecimal values are the intended ones for the following
 * constants. The decimal values may be used, provided that the
 * compiler will convert from decimal to binary accurately enough
 * to produce the hexadecimal values shown.
 */

#include "fdlibm.h"

#ifdef __STDC__
static const double
#else
static double
#endif
tiny  = 1.0e-300,
zero  = 0.0,
pi_o_4  = 7.8539816339744827900E-01, /* 0x3FE921FB, 0x54442D18 */
pi_o_2  = 1.5707963267948965580E+00, /* 0x3FF921FB, 0x54442D18 */
pi      = 3.1415926535897931160E+00, /* 0x400921FB, 0x54442D18 */
pi_lo   = 1.2246467991473531772E-16; /* 0x3CA1A626, 0x33145C07 */

#ifdef __STDC__
        double __ieee754_atan2(double y, double x)
#else
        double __ieee754_atan2(y,x)
        double  y,x;
#endif
{
        double z;
        int k,m,hx,hy,ix,iy;
        unsigned lx,ly;

        hx = __HI(x); ix = hx&0x7fffffff;
        lx = __LO(x);
        hy = __HI(y); iy = hy&0x7fffffff;
        ly = __LO(y);
        if(((ix|((lx|-lx)>>31))>0x7ff00000)||
           ((iy|((ly|-ly)>>31))>0x7ff00000))    /* x or y is NaN */
           return x+y;
        if(((hx-0x3ff00000)|lx)==0) return atan(y);   /* x=1.0 */
        m = ((hy>>31)&1)|((hx>>30)&2);  /* 2*sign(x)+sign(y) */

    /* when y = 0 */
        if((iy|ly)==0) {
            switch(m) {
                case 0:
                case 1: return y;       /* atan(+-0,+anything)=+-0 */
                case 2: return  pi+tiny;/* atan(+0,-anything) = pi */
                case 3: return -pi-tiny;/* atan(-0,-anything) =-pi */
            }
        }
    /* when x = 0 */
        if((ix|lx)==0) return (hy<0)?  -pi_o_2-tiny: pi_o_2+tiny;

    /* when x is INF */
        if(ix==0x7ff00000) {
            if(iy==0x7ff00000) {
                switch(m) {
                    case 0: return  pi_o_4+tiny;/* atan(+INF,+INF) */
                    case 1: return -pi_o_4-tiny;/* atan(-INF,+INF) */
                    case 2: return  3.0*pi_o_4+tiny;/*atan(+INF,-INF)*/
                    case 3: return -3.0*pi_o_4-tiny;/*atan(-INF,-INF)*/
                }
            } else {
                switch(m) {
                    case 0: return  zero  ;     /* atan(+...,+INF) */
                    case 1: return -1.0*zero  ; /* atan(-...,+INF) */
                    case 2: return  pi+tiny  ;  /* atan(+...,-INF) */
                    case 3: return -pi-tiny  ;  /* atan(-...,-INF) */
                }
            }
        }
    /* when y is INF */
        if(iy==0x7ff00000) return (hy<0)? -pi_o_2-tiny: pi_o_2+tiny;

    /* compute y/x */
        k = (iy-ix)>>20;
        if(k > 60) z=pi_o_2+0.5*pi_lo;  /* |y/x| >  2**60 */
        else if(hx<0&&k<-60) z=0.0;     /* |y|/x < -2**60 */
        else z=atan(fabs(y/x));         /* safe to do y/x */
        switch (m) {
            case 0: return       z  ;   /* atan(+,+) */
            case 1: __HI(z) ^= 0x80000000;
                    return       z  ;   /* atan(-,+) */
            case 2: return  pi-(z-pi_lo);/* atan(+,-) */
            default: /* case 3 */
                    return  (z-pi_lo)-pi;/* atan(-,-) */
        }
}
