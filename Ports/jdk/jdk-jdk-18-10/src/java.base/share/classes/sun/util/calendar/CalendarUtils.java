/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

package sun.util.calendar;

import java.util.HashMap;
import java.util.Map;

public class CalendarUtils {

    /**
     * Returns whether the specified year is a leap year in the Gregorian
     * calendar system.
     *
     * @param gregorianYear a Gregorian calendar year
     * @return true if the given year is a leap year in the Gregorian
     * calendar system.
     * @see CalendarDate#isLeapYear
     */
    public static final boolean isGregorianLeapYear(int gregorianYear) {
        return (((gregorianYear % 4) == 0)
                && (((gregorianYear % 100) != 0) || ((gregorianYear % 400) == 0)));
    }

    /**
     * Returns whether the specified year is a leap year in the Julian
     * calendar system. The year number must be a normalized one
     * (e.g., 45 B.C.E. is 1-45).
     *
     * @param normalizedJulianYear a normalized Julian calendar year
     * @return true if the given year is a leap year in the Julian
     * calendar system.
     * @see CalendarDate#isLeapYear
     */
    public static final boolean isJulianLeapYear(int normalizedJulianYear) {
        return (normalizedJulianYear % 4) == 0;
    }

    /**
     * Divides two integers and returns the floor of the quotient.
     * For example, <code>floorDivide(-1, 4)</code> returns -1 while
     * -1/4 is 0.
     *
     * @param n the numerator
     * @param d a divisor that must be greater than 0
     * @return the floor of the quotient
     */
    public static final long floorDivide(long n, long d) {
        return ((n >= 0) ?
                (n / d) : (((n + 1L) / d) - 1L));
    }

    /**
     * Divides two integers and returns the floor of the quotient.
     * For example, <code>floorDivide(-1, 4)</code> returns -1 while
     * -1/4 is 0.
     *
     * @param n the numerator
     * @param d a divisor that must be greater than 0
     * @return the floor of the quotient
     */
    public static final int floorDivide(int n, int d) {
        return ((n >= 0) ?
                (n / d) : (((n + 1) / d) - 1));
    }

    /**
     * Divides two integers and returns the floor of the quotient and
     * the modulus remainder.  For example,
     * <code>floorDivide(-1,4)</code> returns <code>-1</code> with
     * <code>3</code> as its remainder, while <code>-1/4</code> is
     * <code>0</code> and <code>-1%4</code> is <code>-1</code>.
     *
     * @param n the numerator
     * @param d a divisor which must be {@literal > 0}
     * @param r an array of at least one element in which the value
     * <code>mod(n, d)</code> is returned.
     * @return the floor of the quotient.
     */
    public static final int floorDivide(int n, int d, int[] r) {
        if (n >= 0) {
            r[0] = n % d;
            return n / d;
        }
        int q = ((n + 1) / d) - 1;
        r[0] = n - (q * d);
        return q;
    }

    /**
     * Divides two integers and returns the floor of the quotient and
     * the modulus remainder.  For example,
     * <code>floorDivide(-1,4)</code> returns <code>-1</code> with
     * <code>3</code> as its remainder, while <code>-1/4</code> is
     * <code>0</code> and <code>-1%4</code> is <code>-1</code>.
     *
     * @param n the numerator
     * @param d a divisor which must be {@literal > 0}
     * @param r an array of at least one element in which the value
     * <code>mod(n, d)</code> is returned.
     * @return the floor of the quotient.
     */
    public static final int floorDivide(long n, int d, int[] r) {
        if (n >= 0) {
            r[0] = (int)(n % d);
            return (int)(n / d);
        }
        int q = (int)(((n + 1) / d) - 1);
        r[0] = (int)(n - (q * d));
        return q;
    }

    public static final long mod(long x, long y) {
        return (x - y * floorDivide(x, y));
    }

    public static final int mod(int x, int y) {
        return (x - y * floorDivide(x, y));
    }

    public static final int amod(int x, int y) {
        int z = mod(x, y);
        return (z == 0) ? y : z;
    }

    public static final long amod(long x, long y) {
        long z = mod(x, y);
        return (z == 0) ? y : z;
    }

    /**
     * Mimics sprintf(buf, "%0*d", decaimal, width).
     */
    public static final StringBuilder sprintf0d(StringBuilder sb, int value, int width) {
        long d = value;
        if (d < 0) {
            sb.append('-');
            d = -d;
            --width;
        }
        int n = 10;
        for (int i = 2; i < width; i++) {
            n *= 10;
        }
        for (int i = 1; i < width && d < n; i++) {
            sb.append('0');
            n /= 10;
        }
        sb.append(d);
        return sb;
    }

    public static final StringBuffer sprintf0d(StringBuffer sb, int value, int width) {
        long d = value;
        if (d < 0) {
            sb.append('-');
            d = -d;
            --width;
        }
        int n = 10;
        for (int i = 2; i < width; i++) {
            n *= 10;
        }
        for (int i = 1; i < width && d < n; i++) {
            sb.append('0');
            n /= 10;
        }
        sb.append(d);
        return sb;
    }
}
