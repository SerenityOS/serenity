/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A transliteration of the "Freely Distributable Math Library"
 * algorithms from C into Java. That is, this port of the algorithms
 * is as close to the C originals as possible while still being
 * readable legal Java.
 */
public class FdlibmTranslit {
    private FdlibmTranslit() {
        throw new UnsupportedOperationException("No FdLibmTranslit instances for you.");
    }

    /**
     * Return the low-order 32 bits of the double argument as an int.
     */
    private static int __LO(double x) {
        long transducer = Double.doubleToRawLongBits(x);
        return (int)transducer;
    }

    /**
     * Return a double with its low-order bits of the second argument
     * and the high-order bits of the first argument..
     */
    private static double __LO(double x, int low) {
        long transX = Double.doubleToRawLongBits(x);
        return Double.longBitsToDouble((transX & 0xFFFF_FFFF_0000_0000L) |
                                       (low    & 0x0000_0000_FFFF_FFFFL));
    }

    /**
     * Return the high-order 32 bits of the double argument as an int.
     */
    private static int __HI(double x) {
        long transducer = Double.doubleToRawLongBits(x);
        return (int)(transducer >> 32);
    }

    /**
     * Return a double with its high-order bits of the second argument
     * and the low-order bits of the first argument..
     */
    private static double __HI(double x, int high) {
        long transX = Double.doubleToRawLongBits(x);
        return Double.longBitsToDouble((transX & 0x0000_0000_FFFF_FFFFL) |
                                       ( ((long)high)) << 32 );
    }

    public static double hypot(double x, double y) {
        return Hypot.compute(x, y);
    }

    /**
     * cbrt(x)
     * Return cube root of x
     */
    public static class Cbrt {
        // unsigned
        private static final int B1 = 715094163; /* B1 = (682-0.03306235651)*2**20 */
        private static final int B2 = 696219795; /* B2 = (664-0.03306235651)*2**20 */

        private static final double C =  5.42857142857142815906e-01; /* 19/35     = 0x3FE15F15, 0xF15F15F1 */
        private static final double D = -7.05306122448979611050e-01; /* -864/1225 = 0xBFE691DE, 0x2532C834 */
        private static final double E =  1.41428571428571436819e+00; /* 99/70     = 0x3FF6A0EA, 0x0EA0EA0F */
        private static final double F =  1.60714285714285720630e+00; /* 45/28     = 0x3FF9B6DB, 0x6DB6DB6E */
        private static final double G =  3.57142857142857150787e-01; /* 5/14      = 0x3FD6DB6D, 0xB6DB6DB7 */

        public static strictfp double compute(double x) {
            int     hx;
            double  r, s, t=0.0, w;
            int sign; // unsigned

            hx = __HI(x);           // high word of x
            sign = hx & 0x80000000;             // sign= sign(x)
            hx  ^= sign;
            if (hx >= 0x7ff00000)
                return (x+x); // cbrt(NaN,INF) is itself
            if ((hx | __LO(x)) == 0)
                return(x);          // cbrt(0) is itself

            x = __HI(x, hx);   // x <- |x|
            // rough cbrt to 5 bits
            if (hx < 0x00100000) {               // subnormal number
                t = __HI(t, 0x43500000);          // set t= 2**54
                t *= x;
                t = __HI(t, __HI(t)/3+B2);
            } else {
                t = __HI(t, hx/3+B1);
            }

            // new cbrt to 23 bits, may be implemented in single precision
            r = t * t/x;
            s = C + r*t;
            t *= G + F/(s + E + D/s);

            // chopped to 20 bits and make it larger than cbrt(x)
            t = __LO(t, 0);
            t = __HI(t, __HI(t)+0x00000001);


            // one step newton iteration to 53 bits with error less than 0.667 ulps
            s = t * t;          // t*t is exact
            r = x / s;
            w = t + t;
            r= (r - t)/(w + r);  // r-s is exact
            t= t + t*r;

            // retore the sign bit
            t = __HI(t, __HI(t) | sign);
            return(t);
        }
    }

    /**
     * hypot(x,y)
     *
     * Method :
     *      If (assume round-to-nearest) z = x*x + y*y
     *      has error less than sqrt(2)/2 ulp, than
     *      sqrt(z) has error less than 1 ulp (exercise).
     *
     *      So, compute sqrt(x*x + y*y) with some care as
     *      follows to get the error below 1 ulp:
     *
     *      Assume x > y > 0;
     *      (if possible, set rounding to round-to-nearest)
     *      1. if x > 2y  use
     *              x1*x1 + (y*y + (x2*(x + x1))) for x*x + y*y
     *      where x1 = x with lower 32 bits cleared, x2 = x - x1; else
     *      2. if x <= 2y use
     *              t1*y1 + ((x-y) * (x-y) + (t1*y2 + t2*y))
     *      where t1 = 2x with lower 32 bits cleared, t2 = 2x - t1,
     *      y1= y with lower 32 bits chopped, y2 = y - y1.
     *
     *      NOTE: scaling may be necessary if some argument is too
     *            large or too tiny
     *
     * Special cases:
     *      hypot(x,y) is INF if x or y is +INF or -INF; else
     *      hypot(x,y) is NAN if x or y is NAN.
     *
     * Accuracy:
     *      hypot(x,y) returns sqrt(x^2 + y^2) with error less
     *      than 1 ulps (units in the last place)
     */
    static class Hypot {
        public static double compute(double x, double y) {
            double a = x;
            double b = y;
            double t1, t2, y1, y2, w;
            int j, k, ha, hb;

            ha = __HI(x) & 0x7fffffff;        // high word of  x
            hb = __HI(y) & 0x7fffffff;        // high word of  y
            if(hb > ha) {
                a = y;
                b = x;
                j = ha;
                ha = hb;
                hb = j;
            } else {
                a = x;
                b = y;
            }
            a = __HI(a, ha);   // a <- |a|
            b = __HI(b, hb);   // b <- |b|
            if ((ha - hb) > 0x3c00000) {
                return a + b;  // x / y > 2**60
            }
            k=0;
            if (ha > 0x5f300000) {   // a>2**500
                if (ha >= 0x7ff00000) {       // Inf or NaN
                    w = a + b;                // for sNaN
                    if (((ha & 0xfffff) | __LO(a)) == 0)
                        w = a;
                    if (((hb ^ 0x7ff00000) | __LO(b)) == 0)
                        w = b;
                    return w;
                }
                // scale a and b by 2**-600
                ha -= 0x25800000;
                hb -= 0x25800000;
                k += 600;
                a = __HI(a, ha);
                b = __HI(b, hb);
            }
            if (hb < 0x20b00000) {   // b < 2**-500
                if (hb <= 0x000fffff) {      // subnormal b or 0 */
                    if ((hb | (__LO(b))) == 0)
                        return a;
                    t1 = 0;
                    t1 = __HI(t1, 0x7fd00000);  // t1=2^1022
                    b *= t1;
                    a *= t1;
                    k -= 1022;
                } else {            // scale a and b by 2^600
                    ha += 0x25800000;       // a *= 2^600
                    hb += 0x25800000;       // b *= 2^600
                    k -= 600;
                    a = __HI(a, ha);
                    b = __HI(b, hb);
                }
            }
            // medium size a and b
            w = a - b;
            if (w > b) {
                t1 = 0;
                t1 = __HI(t1, ha);
                t2 = a - t1;
                w  = Math.sqrt(t1*t1 - (b*(-b) - t2 * (a + t1)));
            } else {
                a  = a + a;
                y1 = 0;
                y1 = __HI(y1, hb);
                y2 = b - y1;
                t1 = 0;
                t1 = __HI(t1, ha + 0x00100000);
                t2 = a - t1;
                w  = Math.sqrt(t1*y1 - (w*(-w) - (t1*y2 + t2*b)));
            }
            if (k != 0) {
                t1 = 1.0;
                int t1_hi = __HI(t1);
                t1_hi += (k << 20);
                t1 = __HI(t1, t1_hi);
                return t1 * w;
            } else
                return w;
        }
    }

    /**
     * Returns the exponential of x.
     *
     * Method
     *   1. Argument reduction:
     *      Reduce x to an r so that |r| <= 0.5*ln2 ~ 0.34658.
     *      Given x, find r and integer k such that
     *
     *               x = k*ln2 + r,  |r| <= 0.5*ln2.
     *
     *      Here r will be represented as r = hi-lo for better
     *      accuracy.
     *
     *   2. Approximation of exp(r) by a special rational function on
     *      the interval [0,0.34658]:
     *      Write
     *          R(r**2) = r*(exp(r)+1)/(exp(r)-1) = 2 + r*r/6 - r**4/360 + ...
     *      We use a special Reme algorithm on [0,0.34658] to generate
     *      a polynomial of degree 5 to approximate R. The maximum error
     *      of this polynomial approximation is bounded by 2**-59. In
     *      other words,
     *          R(z) ~ 2.0 + P1*z + P2*z**2 + P3*z**3 + P4*z**4 + P5*z**5
     *      (where z=r*r, and the values of P1 to P5 are listed below)
     *      and
     *          |                  5          |     -59
     *          | 2.0+P1*z+...+P5*z   -  R(z) | <= 2
     *          |                             |
     *      The computation of exp(r) thus becomes
     *                             2*r
     *              exp(r) = 1 + -------
     *                            R - r
     *                                 r*R1(r)
     *                     = 1 + r + ----------- (for better accuracy)
     *                                2 - R1(r)
     *      where
     *                               2       4             10
     *              R1(r) = r - (P1*r  + P2*r  + ... + P5*r   ).
     *
     *   3. Scale back to obtain exp(x):
     *      From step 1, we have
     *         exp(x) = 2^k * exp(r)
     *
     * Special cases:
     *      exp(INF) is INF, exp(NaN) is NaN;
     *      exp(-INF) is 0, and
     *      for finite argument, only exp(0)=1 is exact.
     *
     * Accuracy:
     *      according to an error analysis, the error is always less than
     *      1 ulp (unit in the last place).
     *
     * Misc. info.
     *      For IEEE double
     *          if x >  7.09782712893383973096e+02 then exp(x) overflow
     *          if x < -7.45133219101941108420e+02 then exp(x) underflow
     *
     * Constants:
     * The hexadecimal values are the intended ones for the following
     * constants. The decimal values may be used, provided that the
     * compiler will convert from decimal to binary accurately enough
     * to produce the hexadecimal values shown.
     */
    static class Exp {
        private static final double one     = 1.0;
        private static final double[] halF = {0.5,-0.5,};
        private static final double huge    = 1.0e+300;
        private static final double twom1000= 9.33263618503218878990e-302;      /* 2**-1000=0x01700000,0*/
        private static final double o_threshold=  7.09782712893383973096e+02;   /* 0x40862E42, 0xFEFA39EF */
        private static final double u_threshold= -7.45133219101941108420e+02;   /* 0xc0874910, 0xD52D3051 */
        private static final double[] ln2HI   ={ 6.93147180369123816490e-01,    /* 0x3fe62e42, 0xfee00000 */
                                                 -6.93147180369123816490e-01};  /* 0xbfe62e42, 0xfee00000 */
        private static final double[] ln2LO   ={ 1.90821492927058770002e-10,    /* 0x3dea39ef, 0x35793c76 */
                                                 -1.90821492927058770002e-10,}; /* 0xbdea39ef, 0x35793c76 */
        private static final double invln2 =  1.44269504088896338700e+00;       /* 0x3ff71547, 0x652b82fe */
        private static final double P1   =  1.66666666666666019037e-01;         /* 0x3FC55555, 0x5555553E */
        private static final double P2   = -2.77777777770155933842e-03;         /* 0xBF66C16C, 0x16BEBD93 */
        private static final double P3   =  6.61375632143793436117e-05;         /* 0x3F11566A, 0xAF25DE2C */
        private static final double P4   = -1.65339022054652515390e-06;         /* 0xBEBBBD41, 0xC5D26BF1 */
        private static final double P5   =  4.13813679705723846039e-08;         /* 0x3E663769, 0x72BEA4D0 */

        public static strictfp double compute(double x) {
            double y,hi=0,lo=0,c,t;
            int k=0,xsb;
            /*unsigned*/ int hx;

            hx  = __HI(x);  /* high word of x */
            xsb = (hx>>31)&1;               /* sign bit of x */
            hx &= 0x7fffffff;               /* high word of |x| */

            /* filter out non-finite argument */
            if(hx >= 0x40862E42) {                  /* if |x|>=709.78... */
                if(hx>=0x7ff00000) {
                    if(((hx&0xfffff)|__LO(x))!=0)
                        return x+x;                /* NaN */
                    else return (xsb==0)? x:0.0;    /* exp(+-inf)={inf,0} */
                }
                if(x > o_threshold) return huge*huge; /* overflow */
                if(x < u_threshold) return twom1000*twom1000; /* underflow */
            }

            /* argument reduction */
            if(hx > 0x3fd62e42) {           /* if  |x| > 0.5 ln2 */
                if(hx < 0x3FF0A2B2) {       /* and |x| < 1.5 ln2 */
                    hi = x-ln2HI[xsb]; lo=ln2LO[xsb]; k = 1-xsb-xsb;
                } else {
                    k  = (int)(invln2*x+halF[xsb]);
                    t  = k;
                    hi = x - t*ln2HI[0];    /* t*ln2HI is exact here */
                    lo = t*ln2LO[0];
                }
                x  = hi - lo;
            }
            else if(hx < 0x3e300000)  {     /* when |x|<2**-28 */
                if(huge+x>one) return one+x;/* trigger inexact */
            }
            else k = 0;

            /* x is now in primary range */
            t  = x*x;
            c  = x - t*(P1+t*(P2+t*(P3+t*(P4+t*P5))));
            if(k==0)        return one-((x*c)/(c-2.0)-x);
            else            y = one-((lo-(x*c)/(2.0-c))-hi);
            if(k >= -1021) {
                y = __HI(y, __HI(y) + (k<<20)); /* add k to y's exponent */
                return y;
            } else {
                y = __HI(y, __HI(y) + ((k+1000)<<20));/* add k to y's exponent */
                return y*twom1000;
            }
        }
    }
}
