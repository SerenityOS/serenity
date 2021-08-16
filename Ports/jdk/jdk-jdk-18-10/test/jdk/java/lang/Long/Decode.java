/*
 * Copyright (c) 1998, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * @test
 * @bug 4136371 5017980 6576055
 * @summary Test Long.decode method
 * @author madbot
 * @author Joseph D. Darcy
 */

import java.math.BigInteger;

/**
 * There are six methods in java.lang.Integer which transform strings
 * into a long or Long value:
 *
 * public Long(String s)
 * public static Long decode(String nm)
 * public static long parseLong(String s, int radix)
 * public static long parseLong(String s)
 * public static Long valueOf(String s, int radix)
 * public static Long valueOf(String s)
 *
 * The other five methods are tested elsewhere.
 */
public class Decode {

    private static void check(String val, long expected) {
        long n = (Long.decode(val)).longValue();
        if (n != expected)
            throw new RuntimeException("Long.decode failed. String:" +
                                                val + " Result:" + n);
    }

    private static void checkFailure(String val, String message) {
        try {
            long n = (Long.decode(val)).longValue();
            throw new RuntimeException(message);
        } catch (NumberFormatException e) { /* Okay */}
    }

    public static void main(String[] args) throws Exception {
        check(new String(""+Long.MIN_VALUE), Long.MIN_VALUE);
        check(new String(""+Long.MAX_VALUE), Long.MAX_VALUE);

        check("10",   10L);
        check("0x10", 16L);
        check("0X10", 16L);
        check("010",  8L);
        check("#10",  16L);

        check("+10",   10L);
        check("+0x10", 16L);
        check("+0X10", 16L);
        check("+010",  8L);
        check("+#10",  16L);

        check("-10",   -10L);
        check("-0x10", -16L);
        check("-0X10", -16L);
        check("-010",  -8L);
        check("-#10",  -16L);

        check(Long.toString(Long.MIN_VALUE), Long.MIN_VALUE);
        check(Long.toString(Long.MAX_VALUE), Long.MAX_VALUE);

        checkFailure("0x-10",   "Long.decode allows negative sign in wrong position.");
        checkFailure("0x+10",   "Long.decode allows positive sign in wrong position.");

        checkFailure("+",       "Raw plus sign allowed.");
        checkFailure("-",       "Raw minus sign allowed.");

        checkFailure(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE).toString(),
                     "Out of range");
        checkFailure(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE).toString(),
                     "Out of range");

        checkFailure("", "Empty String");

        try {
            Long.decode(null);
            throw new RuntimeException("Long.decode(null) expected to throw NPE");
        } catch (NullPointerException npe) {/* Okay */}
    }
}
