/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5017980 6576055 8041972 8055251
 * @summary Test parsing methods
 * @author Joseph D. Darcy
 */

/**
 * There are seven methods in java.lang.Long which transform strings
 * into a long or Long value:
 *
 * public Long(String s)
 * public static Long decode(String nm)
 * public static long parseLong(CharSequence s, int radix, int beginIndex, int endIndex)
 * public static long parseLong(String s, int radix)
 * public static long parseLong(String s)
 * public static Long valueOf(String s, int radix)
 * public static Long valueOf(String s)
 *
 * Besides decode, all the methods and constructor call down into
 * parseLong(CharSequence, int, int, int) to do the actual work.  Therefore, the
 * behavior of parseLong(CharSequence, int, int, int) will be tested here.
 */

public class ParsingTest {

    public static void main(String... argv) {
        check(+100L, "+100");
        check(-100L, "-100");

        check(0L, "+0");
        check(0L, "-0");
        check(0L, "+00000");
        check(0L, "-00000");

        check(0L, "0");
        check(1L, "1");
        check(9L, "9");

        checkFailure("");
        checkFailure("\u0000");
        checkFailure("\u002f");
        checkFailure("+");
        checkFailure("-");
        checkFailure("++");
        checkFailure("+-");
        checkFailure("-+");
        checkFailure("--");
        checkFailure("++100");
        checkFailure("--100");
        checkFailure("+-6");
        checkFailure("-+6");
        checkFailure("*100");

        check(0L, "test-00000", 4, 10, 10);
        check(-12345L, "test-12345", 4, 10, 10);
        check(12345L, "xx12345yy", 2, 7, 10);
        check(123456789012345L, "xx123456789012345yy", 2, 17, 10);
        check(15L, "xxFyy", 2, 3, 16);

        checkNumberFormatException("", 0, 0, 10);
        checkNumberFormatException("+-6", 0, 3, 10);
        checkNumberFormatException("1000000", 7, 7, 10);
        checkNumberFormatException("1000000", 0, 2, Character.MAX_RADIX + 1);
        checkNumberFormatException("1000000", 0, 2, Character.MIN_RADIX - 1);

        checkIndexOutOfBoundsException("", 1, 1, 10);
        checkIndexOutOfBoundsException("1000000", 10, 4, 10);
        checkIndexOutOfBoundsException("1000000", 10, 2, Character.MAX_RADIX + 1);
        checkIndexOutOfBoundsException("1000000", 10, 2, Character.MIN_RADIX - 1);
        checkIndexOutOfBoundsException("1000000", -1, 2, Character.MAX_RADIX + 1);
        checkIndexOutOfBoundsException("1000000", -1, 2, Character.MIN_RADIX - 1);
        checkIndexOutOfBoundsException("-1", 0, 3, 10);
        checkIndexOutOfBoundsException("-1", 2, 3, 10);
        checkIndexOutOfBoundsException("-1", -1, 2, 10);

        checkNull(0, 1, 10);
        checkNull(-1, 0, 10);
        checkNull(0, 0, 10);
        checkNull(0, -1, 10);
        checkNull(-1, -1, -1);
    }

    private static void check(long expected, String val) {
        long n = Long.parseLong(val);
        if (n != expected)
            throw new RuntimeException("Long.parseLong failed. String:" +
                                       val + " Result:" + n);
    }

    private static void checkFailure(String val) {
        long n = 0L;
        try {
            n = Long.parseLong(val);
            System.err.println("parseLong(" + val + ") incorrectly returned " + n);
            throw new RuntimeException();
        } catch (NumberFormatException nfe) {
            ; // Expected
        }
    }

    private static void checkNumberFormatException(String val, int start, int end, int radix) {
        long n = 0;
        try {
            n = Long.parseLong(val, start, end, radix);
            System.err.println("parseLong(" + val + ", " + start + ", " + end + ", " + radix +
                    ") incorrectly returned " + n);
            throw new RuntimeException();
        } catch (NumberFormatException nfe) {
            ; // Expected
        }
    }

    private static void checkIndexOutOfBoundsException(String val, int start, int end, int radix) {
        long n = 0;
        try {
            n = Long.parseLong(val, start, end, radix);
            System.err.println("parseLong(" + val + ", " + start + ", " + end + ", " + radix +
                    ") incorrectly returned " + n);
            throw new RuntimeException();
        } catch (IndexOutOfBoundsException ioob) {
            ; // Expected
        }
    }

    private static void checkNull(int start, int end, int radix) {
        long n = 0;
        try {
            n = Long.parseLong(null, start, end, radix);
            System.err.println("parseLong(null, " + start + ", " + end + ", " + radix +
                    ") incorrectly returned " + n);
            throw new RuntimeException();
        } catch (NullPointerException npe) {
            ; // Expected
        }
    }

    private static void check(long expected, String val, int start, int end, int radix) {
        long n = Long.parseLong(val, start, end, radix);
        if (n != expected)
            throw new RuntimeException("Long.parseLong failed. Expexted: " + expected + " String: \"" +
                    val + "\", start: " + start + ", end: " + end + " radix: " + radix + " Result: " + n);
    }
}
