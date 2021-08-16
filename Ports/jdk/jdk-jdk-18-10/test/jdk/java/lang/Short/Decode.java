/*
 * Copyright (c) 1998, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test Short.decode method
 * @author madbot
 * @author Joseph D. Darcy
 */

/**
 * There are six methods in java.lang.Short which transform strings
 * into a short or Short value:
 *
 * public Short(String s)
 * public static Short decode(String nm)
 * public static short parseShort(String s, int radix)
 * public static short parseShort(String s)
 * public static Short valueOf(String s, int radix)
 * public static Short valueOf(String s)
 *
 * However, of these only decode has a nontrivial implementation
 * in that class.
 */
public class Decode {

    private static void check(String ashort, short expected) {
        short sh = (Short.decode(ashort)).shortValue();
        if (sh != expected)
            throw new RuntimeException("Short.decode failed. String:" +
                                                ashort + " Result:" + sh);
    }

    private static void checkFailure(String val, String message) {
        try {
            short n = (Short.decode(val)).shortValue();
            throw new RuntimeException(message);
        } catch (NumberFormatException e) { /* Okay */}
    }

    public static void main(String[] args) throws Exception {
        check(new String(""+Short.MIN_VALUE), Short.MIN_VALUE);
        check(new String(""+Short.MAX_VALUE), Short.MAX_VALUE);

        check("10",   (short)10);
        check("0x10", (short)16);
        check("0X10", (short)16);
        check("010",  (short)8);
        check("#10",  (short)16);

        check("+10",   (short)10);
        check("+0x10", (short)16);
        check("+0X10", (short)16);
        check("+010",  (short)8);
        check("+#10",  (short)16);

        check("-10",   (short)-10);
        check("-0x10", (short)-16);
        check("-0X10", (short)-16);
        check("-010",  (short)-8);
        check("-#10",  (short)-16);

        check(Integer.toString((int)Short.MIN_VALUE), Short.MIN_VALUE);
        check(Integer.toString((int)Short.MAX_VALUE), Short.MAX_VALUE);

        checkFailure("0x-10",   "Short.decode allows negative sign in wrong position.");
        checkFailure("0x+10",   "Short.decode allows positive sign in wrong position.");

        checkFailure("+",       "Raw plus sign allowed.");
        checkFailure("-",       "Raw minus sign allowed.");

        checkFailure(Integer.toString((int)Short.MIN_VALUE - 1), "Out of range");
        checkFailure(Integer.toString((int)Short.MAX_VALUE + 1), "Out of range");

        checkFailure("", "Empty String");
    }
}
