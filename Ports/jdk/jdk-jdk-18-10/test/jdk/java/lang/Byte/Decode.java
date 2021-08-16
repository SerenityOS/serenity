/*
 * Copyright (c) 1999, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4242173 5017980 6576055
 * @summary Test Byte.decode method
 * @author madbot
 * @author Joseph D. Darcy
 */

/**
 * There are six methods in java.lang.Byte which transform strings
 * into a byte or Byte value:
 *
 * public Byte(String s)
 * public static Byte decode(String nm)
 * public static byte parseByte(String s, int radix)
 * public static byte parseByte(String s)
 * public static Byte valueOf(String s, int radix)
 * public static Byte valueOf(String s)
 *
 * However, of these only decode has a nontrivial implementation
 * in that class.
 */
public class Decode {

    private static void check(String val, byte expected) {
        byte n = (Byte.decode(val)).byteValue();
        if (n != expected)
            throw new RuntimeException("Byte.decode failed. String:" +
                                                val + " Result:" + n);
    }

    private static void checkFailure(String val, String message) {
        try {
            byte n = (Byte.decode(val)).byteValue();
            throw new RuntimeException(message);
        } catch (NumberFormatException e) { /* Okay */}
    }

    public static void main(String[] args) throws Exception {
        check(new String(""+Byte.MIN_VALUE), Byte.MIN_VALUE);
        check(new String(""+Byte.MAX_VALUE), Byte.MAX_VALUE);

        check("10",   (byte)10);
        check("0x10", (byte)16);
        check("0X10", (byte)16);
        check("010",  (byte)8);
        check("#10",  (byte)16);

        check("+10",   (byte)10);
        check("+0x10", (byte)16);
        check("+0X10", (byte)16);
        check("+010",  (byte)8);
        check("+#10",  (byte)16);

        check("-10",   (byte)-10);
        check("-0x10", (byte)-16);
        check("-0X10", (byte)-16);
        check("-010",  (byte)-8);
        check("-#10",  (byte)-16);

        check(Integer.toString((int)Byte.MIN_VALUE), Byte.MIN_VALUE);
        check(Integer.toString((int)Byte.MAX_VALUE), Byte.MAX_VALUE);

        checkFailure("0x-10",   "Byte.decode allows negative sign in wrong position.");
        checkFailure("0x+10",   "Byte.decode allows positive sign in wrong position.");

        checkFailure("+",       "Raw plus sign allowed.");
        checkFailure("-",       "Raw minus sign allowed.");

        checkFailure(Integer.toString((int)Byte.MIN_VALUE - 1), "Out of range");
        checkFailure(Integer.toString((int)Byte.MAX_VALUE + 1), "Out of range");

        checkFailure("", "Empty String");
    }
}
