/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * Port of the "Freely Distributable Math Library", version 5.3, from
 * C to Java.
 *
 * <p>The C version of fdlibm relied on the idiom of pointer aliasing
 * a 64-bit double floating-point value as a two-element array of
 * 32-bit integers and reading and writing the two halves of the
 * double independently. This coding pattern was problematic to C
 * optimizers and not directly expressible in Java. Therefore, rather
 * than a memory level overlay, if portions of a double need to be
 * operated on as integer values, the standard library methods for
 * bitwise floating-point to integer conversion,
 * Double.longBitsToDouble and Double.doubleToRawLongBits, are directly
 * or indirectly used.
 *
 * <p>The C version of fdlibm also took some pains to signal the
 * correct IEEE 754 exceptional conditions divide by zero, invalid,
 * overflow and underflow. For example, overflow would be signaled by
 * {@code huge * huge} where {@code huge} was a large constant that
 * would overflow when squared. Since IEEE floating-point exceptional
 * handling is not supported natively in the JVM, such coding patterns
 * have been omitted from this port. For example, rather than {@code
 * return huge * huge}, this port will use {@code return INFINITY}.
 *
 * <p>Various comparison and arithmetic operations in fdlibm could be
 * done either based on the integer view of a value or directly on the
 * floating-point representation. Which idiom is faster may depend on
 * platform specific factors. However, for code clarity if no other
 * reason, this port will favor expressing the semantics of those
 * operations in terms of floating-point operations when convenient to
 * do so.
 */
class FdLibm {
    // Constants used by multiple algorithms
    private static final double INFINITY = Double.POSITIVE_INFINITY;

    private FdLibm() {
        throw new UnsupportedOperationException("No FdLibm instances for you.");
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

    /**
     * cbrt(x)
     * Return cube root of x
     */
    public static class Cbrt {
        // unsigned
        private static final int B1 = 715094163; /* B1 = (682-0.03306235651)*2**20 */
        private static final int B2 = 696219795; /* B2 = (664-0.03306235651)*2**20 */

        private static final double C =  0x1.15f15f15f15f1p-1; //   19/35   ~= 5.42857142857142815906e-01
        private static final double D = -0x1.691de2532c834p-1; // -864/1225 ~= 7.05306122448979611050e-01
        private static final double E =  0x1.6a0ea0ea0ea0fp0;  //   99/70   ~= 1.41428571428571436819e+00
        private static final double F =  0x1.9b6db6db6db6ep0;  //   45/28   ~= 1.60714285714285720630e+00
        private static final double G =  0x1.6db6db6db6db7p-2; //    5/14   ~= 3.57142857142857150787e-01

        private Cbrt() {
            throw new UnsupportedOperationException();
        }

        public static double compute(double x) {
            double  t = 0.0;
            double sign;

            if (x == 0.0 || !Double.isFinite(x))
                return x; // Handles signed zeros properly

            sign = (x < 0.0) ? -1.0:  1.0;

            x = Math.abs(x);   // x <- |x|

            // Rough cbrt to 5 bits
            if (x < 0x1.0p-1022) {     // subnormal number
                t = 0x1.0p54;          // set t= 2**54
                t *= x;
                t = __HI(t, __HI(t)/3 + B2);
            } else {
                int hx = __HI(x);           // high word of x
                t = __HI(t, hx/3 + B1);
            }

            // New cbrt to 23 bits, may be implemented in single precision
            double  r, s, w;
            r = t * t/x;
            s = C + r*t;
            t *= G + F/(s + E + D/s);

            // Chopped to 20 bits and make it larger than cbrt(x)
            t = __LO(t, 0);
            t = __HI(t, __HI(t) + 0x00000001);

            // One step newton iteration to 53 bits with error less than 0.667 ulps
            s = t * t;          // t*t is exact
            r = x / s;
            w = t + t;
            r = (r - t)/(w + r);  // r-s is exact
            t = t + t*r;

            // Restore the original sign bit
            return sign * t;
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
     *      than 1 ulp (unit in the last place)
     */
    public static class Hypot {
        public static final double TWO_MINUS_600 = 0x1.0p-600;
        public static final double TWO_PLUS_600  = 0x1.0p+600;

        private Hypot() {
            throw new UnsupportedOperationException();
        }

        public static double compute(double x, double y) {
            double a = Math.abs(x);
            double b = Math.abs(y);

            if (!Double.isFinite(a) || !Double.isFinite(b)) {
                if (a == INFINITY || b == INFINITY)
                    return INFINITY;
                else
                    return a + b; // Propagate NaN significand bits
            }

            if (b > a) {
                double tmp = a;
                a = b;
                b = tmp;
            }
            assert a >= b;

            // Doing bitwise conversion after screening for NaN allows
            // the code to not worry about the possibility of
            // "negative" NaN values.

            // Note: the ha and hb variables are the high-order
            // 32-bits of a and b stored as integer values. The ha and
            // hb values are used first for a rough magnitude
            // comparison of a and b and second for simulating higher
            // precision by allowing a and b, respectively, to be
            // decomposed into non-overlapping portions. Both of these
            // uses could be eliminated. The magnitude comparison
            // could be eliminated by extracting and comparing the
            // exponents of a and b or just be performing a
            // floating-point divide.  Splitting a floating-point
            // number into non-overlapping portions can be
            // accomplished by judicious use of multiplies and
            // additions. For details see T. J. Dekker, A Floating-Point
            // Technique for Extending the Available Precision,
            // Numerische Mathematik, vol. 18, 1971, pp.224-242 and
            // subsequent work.

            int ha = __HI(a);        // high word of a
            int hb = __HI(b);        // high word of b

            if ((ha - hb) > 0x3c00000) {
                return a + b;  // x / y > 2**60
            }

            int k = 0;
            if (a > 0x1.00000_ffff_ffffp500) {   // a > ~2**500
                // scale a and b by 2**-600
                ha -= 0x25800000;
                hb -= 0x25800000;
                a = a * TWO_MINUS_600;
                b = b * TWO_MINUS_600;
                k += 600;
            }
            double t1, t2;
            if (b < 0x1.0p-500) {   // b < 2**-500
                if (b < Double.MIN_NORMAL) {      // subnormal b or 0 */
                    if (b == 0.0)
                        return a;
                    t1 = 0x1.0p1022;   // t1 = 2^1022
                    b *= t1;
                    a *= t1;
                    k -= 1022;
                } else {            // scale a and b by 2^600
                    ha += 0x25800000;       // a *= 2^600
                    hb += 0x25800000;       // b *= 2^600
                    a = a * TWO_PLUS_600;
                    b = b * TWO_PLUS_600;
                    k -= 600;
                }
            }
            // medium size a and b
            double w = a - b;
            if (w > b) {
                t1 = 0;
                t1 = __HI(t1, ha);
                t2 = a - t1;
                w  = Math.sqrt(t1*t1 - (b*(-b) - t2 * (a + t1)));
            } else {
                double y1, y2;
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
                return Math.powerOfTwoD(k) * w;
            } else
                return w;
        }
    }

    /**
     * Compute x**y
     *                    n
     * Method:  Let x =  2   * (1+f)
     *      1. Compute and return log2(x) in two pieces:
     *              log2(x) = w1 + w2,
     *         where w1 has 53 - 24 = 29 bit trailing zeros.
     *      2. Perform y*log2(x) = n+y' by simulating multi-precision
     *         arithmetic, where |y'| <= 0.5.
     *      3. Return x**y = 2**n*exp(y'*log2)
     *
     * Special cases:
     *      1.  (anything) ** 0  is 1
     *      2.  (anything) ** 1  is itself
     *      3.  (anything) ** NAN is NAN
     *      4.  NAN ** (anything except 0) is NAN
     *      5.  +-(|x| > 1) **  +INF is +INF
     *      6.  +-(|x| > 1) **  -INF is +0
     *      7.  +-(|x| < 1) **  +INF is +0
     *      8.  +-(|x| < 1) **  -INF is +INF
     *      9.  +-1         ** +-INF is NAN
     *      10. +0 ** (+anything except 0, NAN)               is +0
     *      11. -0 ** (+anything except 0, NAN, odd integer)  is +0
     *      12. +0 ** (-anything except 0, NAN)               is +INF
     *      13. -0 ** (-anything except 0, NAN, odd integer)  is +INF
     *      14. -0 ** (odd integer) = -( +0 ** (odd integer) )
     *      15. +INF ** (+anything except 0,NAN) is +INF
     *      16. +INF ** (-anything except 0,NAN) is +0
     *      17. -INF ** (anything)  = -0 ** (-anything)
     *      18. (-anything) ** (integer) is (-1)**(integer)*(+anything**integer)
     *      19. (-anything except 0 and inf) ** (non-integer) is NAN
     *
     * Accuracy:
     *      pow(x,y) returns x**y nearly rounded. In particular
     *                      pow(integer,integer)
     *      always returns the correct integer provided it is
     *      representable.
     */
    public static class Pow {
        private Pow() {
            throw new UnsupportedOperationException();
        }

        public static double compute(final double x, final double y) {
            double z;
            double r, s, t, u, v, w;
            int i, j, k, n;

            // y == zero: x**0 = 1
            if (y == 0.0)
                return 1.0;

            // +/-NaN return x + y to propagate NaN significands
            if (Double.isNaN(x) || Double.isNaN(y))
                return x + y;

            final double y_abs = Math.abs(y);
            double x_abs   = Math.abs(x);
            // Special values of y
            if (y == 2.0) {
                return x * x;
            } else if (y == 0.5) {
                if (x >= -Double.MAX_VALUE) // Handle x == -infinity later
                    return Math.sqrt(x + 0.0); // Add 0.0 to properly handle x == -0.0
            } else if (y_abs == 1.0) {        // y is  +/-1
                return (y == 1.0) ? x : 1.0 / x;
            } else if (y_abs == INFINITY) {       // y is +/-infinity
                if (x_abs == 1.0)
                    return  y - y;         // inf**+/-1 is NaN
                else if (x_abs > 1.0) // (|x| > 1)**+/-inf = inf, 0
                    return (y >= 0) ? y : 0.0;
                else                       // (|x| < 1)**-/+inf = inf, 0
                    return (y < 0) ? -y : 0.0;
            }

            final int hx = __HI(x);
            int ix = hx & 0x7fffffff;

            /*
             * When x < 0, determine if y is an odd integer:
             * y_is_int = 0       ... y is not an integer
             * y_is_int = 1       ... y is an odd int
             * y_is_int = 2       ... y is an even int
             */
            int y_is_int  = 0;
            if (hx < 0) {
                if (y_abs >= 0x1.0p53)   // |y| >= 2^53 = 9.007199254740992E15
                    y_is_int = 2; // y is an even integer since ulp(2^53) = 2.0
                else if (y_abs >= 1.0) { // |y| >= 1.0
                    long y_abs_as_long = (long) y_abs;
                    if ( ((double) y_abs_as_long) == y_abs) {
                        y_is_int = 2 -  (int)(y_abs_as_long & 0x1L);
                    }
                }
            }

            // Special value of x
            if (x_abs == 0.0 ||
                x_abs == INFINITY ||
                x_abs == 1.0) {
                z = x_abs;                 // x is +/-0, +/-inf, +/-1
                if (y < 0.0)
                    z = 1.0/z;     // z = (1/|x|)
                if (hx < 0) {
                    if (((ix - 0x3ff00000) | y_is_int) == 0) {
                        z = (z-z)/(z-z); // (-1)**non-int is NaN
                    } else if (y_is_int == 1)
                        z = -1.0 * z;             // (x < 0)**odd = -(|x|**odd)
                }
                return z;
            }

            n = (hx >> 31) + 1;

            // (x < 0)**(non-int) is NaN
            if ((n | y_is_int) == 0)
                return (x-x)/(x-x);

            s = 1.0; // s (sign of result -ve**odd) = -1 else = 1
            if ( (n | (y_is_int - 1)) == 0)
                s = -1.0; // (-ve)**(odd int)

            double p_h, p_l, t1, t2;
            // |y| is huge
            if (y_abs > 0x1.00000_ffff_ffffp31) { // if |y| > ~2**31
                final double INV_LN2   =  0x1.7154_7652_b82fep0;   //  1.44269504088896338700e+00 = 1/ln2
                final double INV_LN2_H =  0x1.715476p0;            //  1.44269502162933349609e+00 = 24 bits of 1/ln2
                final double INV_LN2_L =  0x1.4ae0_bf85_ddf44p-26; //  1.92596299112661746887e-08 = 1/ln2 tail

                // Over/underflow if x is not close to one
                if (x_abs < 0x1.fffff_0000_0000p-1) // |x| < ~0.9999995231628418
                    return (y < 0.0) ? s * INFINITY : s * 0.0;
                if (x_abs > 0x1.00000_ffff_ffffp0)         // |x| > ~1.0
                    return (y > 0.0) ? s * INFINITY : s * 0.0;
                /*
                 * now |1-x| is tiny <= 2**-20, sufficient to compute
                 * log(x) by x - x^2/2 + x^3/3 - x^4/4
                 */
                t = x_abs - 1.0;        // t has 20 trailing zeros
                w = (t * t) * (0.5 - t * (0.3333333333333333333333 - t * 0.25));
                u = INV_LN2_H * t;      // INV_LN2_H has 21 sig. bits
                v =  t * INV_LN2_L - w * INV_LN2;
                t1 = u + v;
                t1 =__LO(t1, 0);
                t2 = v - (t1 - u);
            } else {
                final double CP      =  0x1.ec70_9dc3_a03fdp-1;  //  9.61796693925975554329e-01 = 2/(3ln2)
                final double CP_H    =  0x1.ec709ep-1;           //  9.61796700954437255859e-01 = (float)cp
                final double CP_L    = -0x1.e2fe_0145_b01f5p-28; // -7.02846165095275826516e-09 = tail of CP_H

                double z_h, z_l, ss, s2, s_h, s_l, t_h, t_l;
                n = 0;
                // Take care of subnormal numbers
                if (ix < 0x00100000) {
                    x_abs *= 0x1.0p53; // 2^53 = 9007199254740992.0
                    n -= 53;
                    ix = __HI(x_abs);
                }
                n  += ((ix) >> 20) - 0x3ff;
                j  = ix & 0x000fffff;
                // Determine interval
                ix = j | 0x3ff00000;          // Normalize ix
                if (j <= 0x3988E)
                    k = 0;         // |x| <sqrt(3/2)
                else if (j < 0xBB67A)
                    k = 1;         // |x| <sqrt(3)
                else {
                    k = 0;
                    n += 1;
                    ix -= 0x00100000;
                }
                x_abs = __HI(x_abs, ix);

                // Compute ss = s_h + s_l = (x-1)/(x+1) or (x-1.5)/(x+1.5)

                final double BP[]    = {1.0,
                                       1.5};
                final double DP_H[]  = {0.0,
                                        0x1.2b80_34p-1};        // 5.84962487220764160156e-01
                final double DP_L[]  = {0.0,
                                        0x1.cfde_b43c_fd006p-27};// 1.35003920212974897128e-08

                // Poly coefs for (3/2)*(log(x)-2s-2/3*s**3
                final double L1      =  0x1.3333_3333_33303p-1;  //  5.99999999999994648725e-01
                final double L2      =  0x1.b6db_6db6_fabffp-2;  //  4.28571428578550184252e-01
                final double L3      =  0x1.5555_5518_f264dp-2;  //  3.33333329818377432918e-01
                final double L4      =  0x1.1746_0a91_d4101p-2;  //  2.72728123808534006489e-01
                final double L5      =  0x1.d864_a93c_9db65p-3;  //  2.30660745775561754067e-01
                final double L6      =  0x1.a7e2_84a4_54eefp-3;  //  2.06975017800338417784e-01
                u = x_abs - BP[k];               // BP[0]=1.0, BP[1]=1.5
                v = 1.0 / (x_abs + BP[k]);
                ss = u * v;
                s_h = ss;
                s_h = __LO(s_h, 0);
                // t_h=x_abs + BP[k] High
                t_h = 0.0;
                t_h = __HI(t_h, ((ix >> 1) | 0x20000000) + 0x00080000 + (k << 18) );
                t_l = x_abs - (t_h - BP[k]);
                s_l = v * ((u - s_h * t_h) - s_h * t_l);
                // Compute log(x_abs)
                s2 = ss * ss;
                r = s2 * s2* (L1 + s2 * (L2 + s2 * (L3 + s2 * (L4 + s2 * (L5 + s2 * L6)))));
                r += s_l * (s_h + ss);
                s2  = s_h * s_h;
                t_h = 3.0 + s2 + r;
                t_h = __LO(t_h, 0);
                t_l = r - ((t_h - 3.0) - s2);
                // u+v = ss*(1+...)
                u = s_h * t_h;
                v = s_l * t_h + t_l * ss;
                // 2/(3log2)*(ss + ...)
                p_h = u + v;
                p_h = __LO(p_h, 0);
                p_l = v - (p_h - u);
                z_h = CP_H * p_h;             // CP_H + CP_L = 2/(3*log2)
                z_l = CP_L * p_h + p_l * CP + DP_L[k];
                // log2(x_abs) = (ss + ..)*2/(3*log2) = n + DP_H + z_h + z_l
                t = (double)n;
                t1 = (((z_h + z_l) + DP_H[k]) + t);
                t1 = __LO(t1, 0);
                t2 = z_l - (((t1 - t) - DP_H[k]) - z_h);
            }

            // Split up y into (y1 + y2) and compute (y1 + y2) * (t1 + t2)
            double y1  = y;
            y1 = __LO(y1, 0);
            p_l = (y - y1) * t1 + y * t2;
            p_h = y1 * t1;
            z = p_l + p_h;
            j = __HI(z);
            i = __LO(z);
            if (j >= 0x40900000) {                           // z >= 1024
                if (((j - 0x40900000) | i)!=0)               // if z > 1024
                    return s * INFINITY;                     // Overflow
                else {
                    final double OVT     =  8.0085662595372944372e-0017; // -(1024-log2(ovfl+.5ulp))
                    if (p_l + OVT > z - p_h)
                        return s * INFINITY;   // Overflow
                }
            } else if ((j & 0x7fffffff) >= 0x4090cc00 ) {        // z <= -1075
                if (((j - 0xc090cc00) | i)!=0)           // z < -1075
                    return s * 0.0;           // Underflow
                else {
                    if (p_l <= z - p_h)
                        return s * 0.0;      // Underflow
                }
            }
            /*
             * Compute 2**(p_h+p_l)
             */
            // Poly coefs for (3/2)*(log(x)-2s-2/3*s**3
            final double P1      =  0x1.5555_5555_5553ep-3;  //  1.66666666666666019037e-01
            final double P2      = -0x1.6c16_c16b_ebd93p-9;  // -2.77777777770155933842e-03
            final double P3      =  0x1.1566_aaf2_5de2cp-14; //  6.61375632143793436117e-05
            final double P4      = -0x1.bbd4_1c5d_26bf1p-20; // -1.65339022054652515390e-06
            final double P5      =  0x1.6376_972b_ea4d0p-25; //  4.13813679705723846039e-08
            final double LG2     =  0x1.62e4_2fef_a39efp-1;  //  6.93147180559945286227e-01
            final double LG2_H   =  0x1.62e43p-1;            //  6.93147182464599609375e-01
            final double LG2_L   = -0x1.05c6_10ca_86c39p-29; // -1.90465429995776804525e-09
            i = j & 0x7fffffff;
            k = (i >> 20) - 0x3ff;
            n = 0;
            if (i > 0x3fe00000) {              // if |z| > 0.5, set n = [z + 0.5]
                n = j + (0x00100000 >> (k + 1));
                k = ((n & 0x7fffffff) >> 20) - 0x3ff;     // new k for n
                t = 0.0;
                t = __HI(t, (n & ~(0x000fffff >> k)) );
                n = ((n & 0x000fffff) | 0x00100000) >> (20 - k);
                if (j < 0)
                    n = -n;
                p_h -= t;
            }
            t = p_l + p_h;
            t = __LO(t, 0);
            u = t * LG2_H;
            v = (p_l - (t - p_h)) * LG2 + t * LG2_L;
            z = u + v;
            w = v - (z - u);
            t  = z * z;
            t1  = z - t * (P1 + t * (P2 + t * (P3 + t * (P4 + t * P5))));
            r  = (z * t1)/(t1 - 2.0) - (w + z * w);
            z  = 1.0 - (r - z);
            j  = __HI(z);
            j += (n << 20);
            if ((j >> 20) <= 0)
                z = Math.scalb(z, n); // subnormal output
            else {
                int z_hi = __HI(z);
                z_hi += (n << 20);
                z = __HI(z, z_hi);
            }
            return s * z;
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
        private static final double[] half = {0.5, -0.5,};
        private static final double huge    = 1.0e+300;
        private static final double twom1000=     0x1.0p-1000;             //  9.33263618503218878990e-302 = 2^-1000
        private static final double o_threshold=  0x1.62e42fefa39efp9;     //  7.09782712893383973096e+02
        private static final double u_threshold= -0x1.74910d52d3051p9;     // -7.45133219101941108420e+02;
        private static final double[] ln2HI   ={  0x1.62e42feep-1,         //  6.93147180369123816490e-01
                                                 -0x1.62e42feep-1};        // -6.93147180369123816490e-01
        private static final double[] ln2LO   ={  0x1.a39ef35793c76p-33,   //  1.90821492927058770002e-10
                                                 -0x1.a39ef35793c76p-33};  // -1.90821492927058770002e-10
        private static final double invln2 =      0x1.71547652b82fep0;     //  1.44269504088896338700e+00

        private static final double P1   =  0x1.555555555553ep-3;  //  1.66666666666666019037e-01
        private static final double P2   = -0x1.6c16c16bebd93p-9;  // -2.77777777770155933842e-03
        private static final double P3   =  0x1.1566aaf25de2cp-14; //  6.61375632143793436117e-05
        private static final double P4   = -0x1.bbd41c5d26bf1p-20; // -1.65339022054652515390e-06
        private static final double P5   =  0x1.6376972bea4d0p-25; //  4.13813679705723846039e-08

        private Exp() {
            throw new UnsupportedOperationException();
        }

        public static double compute(double x) {
            double y;
            double hi = 0.0;
            double lo = 0.0;
            double c;
            double t;
            int k = 0;
            int xsb;
            /*unsigned*/ int hx;

            hx  = __HI(x);  /* high word of x */
            xsb = (hx >> 31) & 1;               /* sign bit of x */
            hx &= 0x7fffffff;               /* high word of |x| */

            /* filter out non-finite argument */
            if (hx >= 0x40862E42) {                  /* if |x| >= 709.78... */
                if (hx >= 0x7ff00000) {
                    if (((hx & 0xfffff) | __LO(x)) != 0)
                        return x + x;                /* NaN */
                    else
                        return (xsb == 0) ? x : 0.0;    /* exp(+-inf) = {inf, 0} */
                }
                if (x > o_threshold)
                    return huge * huge; /* overflow */
                if (x < u_threshold) // unsigned compare needed here?
                    return twom1000 * twom1000; /* underflow */
            }

            /* argument reduction */
            if (hx > 0x3fd62e42) {           /* if  |x| > 0.5 ln2 */
                if(hx < 0x3FF0A2B2) {       /* and |x| < 1.5 ln2 */
                    hi = x - ln2HI[xsb];
                    lo=ln2LO[xsb];
                    k = 1 - xsb - xsb;
                } else {
                    k  = (int)(invln2 * x + half[xsb]);
                    t  = k;
                    hi = x - t*ln2HI[0];    /* t*ln2HI is exact here */
                    lo = t*ln2LO[0];
                }
                x  = hi - lo;
            } else if (hx < 0x3e300000)  {     /* when |x|<2**-28 */
                if (huge + x > one)
                    return one + x; /* trigger inexact */
            } else {
                k = 0;
            }

            /* x is now in primary range */
            t  = x * x;
            c  = x - t*(P1 + t*(P2 + t*(P3 + t*(P4 + t*P5))));
            if (k == 0)
                return one - ((x*c)/(c - 2.0) - x);
            else
                y = one - ((lo - (x*c)/(2.0 - c)) - hi);

            if(k >= -1021) {
                y = __HI(y, __HI(y) + (k << 20)); /* add k to y's exponent */
                return y;
            } else {
                y = __HI(y, __HI(y) + ((k + 1000) << 20)); /* add k to y's exponent */
                return y * twom1000;
            }
        }
    }
}
