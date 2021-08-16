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

/* __ieee754_acos(x)
 * Method :
 *      acos(x)  = pi/2 - asin(x)
 *      acos(-x) = pi/2 + asin(x)
 * For |x|<=0.5
 *      acos(x) = pi/2 - (x + x*x^2*R(x^2))     (see asin.c)
 * For x>0.5
 *      acos(x) = pi/2 - (pi/2 - 2asin(sqrt((1-x)/2)))
 *              = 2asin(sqrt((1-x)/2))
 *              = 2s + 2s*z*R(z)        ...z=(1-x)/2, s=sqrt(z)
 *              = 2f + (2c + 2s*z*R(z))
 *     where f=hi part of s, and c = (z-f*f)/(s+f) is the correction term
 *     for f so that f+c ~ sqrt(z).
 * For x<-0.5
 *      acos(x) = pi - 2asin(sqrt((1-|x|)/2))
 *              = pi - 0.5*(s+s*z*R(z)), where z=(1-|x|)/2,s=sqrt(z)
 *
 * Special cases:
 *      if x is NaN, return x itself;
 *      if |x|>1, return NaN with invalid signal.
 *
 * Function needed: sqrt
 */

#include "fdlibm.h"

#ifdef __STDC__
static const double
#else
static double
#endif
one=  1.00000000000000000000e+00, /* 0x3FF00000, 0x00000000 */
pi =  3.14159265358979311600e+00, /* 0x400921FB, 0x54442D18 */
pio2_hi =  1.57079632679489655800e+00, /* 0x3FF921FB, 0x54442D18 */
pio2_lo =  6.12323399573676603587e-17, /* 0x3C91A626, 0x33145C07 */
pS0 =  1.66666666666666657415e-01, /* 0x3FC55555, 0x55555555 */
pS1 = -3.25565818622400915405e-01, /* 0xBFD4D612, 0x03EB6F7D */
pS2 =  2.01212532134862925881e-01, /* 0x3FC9C155, 0x0E884455 */
pS3 = -4.00555345006794114027e-02, /* 0xBFA48228, 0xB5688F3B */
pS4 =  7.91534994289814532176e-04, /* 0x3F49EFE0, 0x7501B288 */
pS5 =  3.47933107596021167570e-05, /* 0x3F023DE1, 0x0DFDF709 */
qS1 = -2.40339491173441421878e+00, /* 0xC0033A27, 0x1C8A2D4B */
qS2 =  2.02094576023350569471e+00, /* 0x40002AE5, 0x9C598AC8 */
qS3 = -6.88283971605453293030e-01, /* 0xBFE6066C, 0x1B8D0159 */
qS4 =  7.70381505559019352791e-02; /* 0x3FB3B8C5, 0xB12E9282 */

#ifdef __STDC__
        double __ieee754_acos(double x)
#else
        double __ieee754_acos(x)
        double x;
#endif
{
        double z,p,q,r,w,s,c,df;
        int hx,ix;
        hx = __HI(x);
        ix = hx&0x7fffffff;
        if(ix>=0x3ff00000) {    /* |x| >= 1 */
            if(((ix-0x3ff00000)|__LO(x))==0) {  /* |x|==1 */
                if(hx>0) return 0.0;            /* acos(1) = 0  */
                else return pi+2.0*pio2_lo;     /* acos(-1)= pi */
            }
            return (x-x)/(x-x);         /* acos(|x|>1) is NaN */
        }
        if(ix<0x3fe00000) {     /* |x| < 0.5 */
            if(ix<=0x3c600000) return pio2_hi+pio2_lo;/*if|x|<2**-57*/
            z = x*x;
            p = z*(pS0+z*(pS1+z*(pS2+z*(pS3+z*(pS4+z*pS5)))));
            q = one+z*(qS1+z*(qS2+z*(qS3+z*qS4)));
            r = p/q;
            return pio2_hi - (x - (pio2_lo-x*r));
        } else  if (hx<0) {             /* x < -0.5 */
            z = (one+x)*0.5;
            p = z*(pS0+z*(pS1+z*(pS2+z*(pS3+z*(pS4+z*pS5)))));
            q = one+z*(qS1+z*(qS2+z*(qS3+z*qS4)));
            s = sqrt(z);
            r = p/q;
            w = r*s-pio2_lo;
            return pi - 2.0*(s+w);
        } else {                        /* x > 0.5 */
            z = (one-x)*0.5;
            s = sqrt(z);
            df = s;
            __LO(df) = 0;
            c  = (z-df*df)/(s+df);
            p = z*(pS0+z*(pS1+z*(pS2+z*(pS3+z*(pS4+z*pS5)))));
            q = one+z*(qS1+z*(qS2+z*(qS3+z*qS4)));
            r = p/q;
            w = r*s+c;
            return 2.0*(df+w);
        }
}
