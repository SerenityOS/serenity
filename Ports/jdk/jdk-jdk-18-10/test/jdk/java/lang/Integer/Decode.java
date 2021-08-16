/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test Integer.decode method
 * @author madbot
 * @author Joseph D. Darcy
 */

/**
 * There are six methods in java.lang.Integer which transform strings
 * into an int or Integer value:
 *
 * public Integer(String s)
 * public static Integer decode(String nm)
 * public static int parseInteger(String s, int radix)
 * public static int parseInteger(String s)
 * public static Integer valueOf(String s, int radix)
 * public static Integer valueOf(String s)
 *
 * The other five methods are tested elsewhere.
 */
public class Decode {

    private static void check(String val, int expected) {
        int n = (Integer.decode(val)).intValue();
        if (n != expected)
            throw new RuntimeException("Integer.decode failed. String:" +
                                                val + " Result:" + n);
    }

    private static void checkFailure(String val, String message) {
        try {
            int n = (Integer.decode(val)).intValue();
            throw new RuntimeException(message);
        } catch (NumberFormatException e) { /* Okay */}
    }

    public static void main(String[] args) throws Exception {
        check(new String(""+Integer.MIN_VALUE), Integer.MIN_VALUE);
        check(new String(""+Integer.MAX_VALUE), Integer.MAX_VALUE);

        check("10",   10);
        check("0x10", 16);
        check("0X10", 16);
        check("010",  8);
        check("#10",  16);

        check("+10",   10);
        check("+0x10", 16);
        check("+0X10", 16);
        check("+010",  8);
        check("+#10",  16);

        check("-10",   -10);
        check("-0x10", -16);
        check("-0X10", -16);
        check("-010",  -8);
        check("-#10",  -16);

        check(Long.toString(Integer.MIN_VALUE), Integer.MIN_VALUE);
        check(Long.toString(Integer.MAX_VALUE), Integer.MAX_VALUE);

        checkFailure("0x-10",   "Integer.decode allows negative sign in wrong position.");
        checkFailure("0x+10",   "Integer.decode allows positive sign in wrong position.");

        checkFailure("+",       "Raw plus sign allowed.");
        checkFailure("-",       "Raw minus sign allowed.");

        checkFailure(Long.toString((long)Integer.MIN_VALUE - 1L), "Out of range");
        checkFailure(Long.toString((long)Integer.MAX_VALUE + 1L), "Out of range");

        checkFailure("", "Empty String");

        try {
            Integer.decode(null);
            throw new RuntimeException("Integer.decode(null) expected to throw NPE");
        } catch (NullPointerException npe) {/* Okay */}
    }
}
