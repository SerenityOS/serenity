/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6371401
 * @summary Tests of fooValueExact methods
 * @author Joseph D. Darcy
 */
import java.math.BigInteger;

public class TestValueExact {
    public static void main(String... args) {
        int errors = 0;

        errors += testLongValueExact();
        errors += testIntValueExact();
        errors += testShortValueExact();
        errors += testByteValueExact();

        if (errors > 0)
            throw new RuntimeException();
    }

    private static int testLongValueExact() {
        int errors = 0;
        BigInteger[] inRange = {
            BigInteger.valueOf(Long.MIN_VALUE),
            BigInteger.ZERO,
            BigInteger.valueOf(Long.MAX_VALUE)
        };

        BigInteger[] outOfRange = {
            BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE),
            BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)
        };

        for (BigInteger bi : inRange) {
            if (bi.longValueExact() != bi.longValue()) {
                System.err.println("Mismatching int conversion for " + bi);
                errors++;
            }
        }

        for (BigInteger bi : outOfRange) {
            try {
                long value = bi.longValueExact();
                System.err.println("Failed to get expected exception on " +
                                   bi + " got " + value);
                errors++;
            } catch(ArithmeticException ae) {
                ; // Expected
            }
        }
        return errors;
    }

    private static int testIntValueExact() {
        int errors = 0;
        BigInteger[] inRange = {
            BigInteger.valueOf(Integer.MIN_VALUE),
            BigInteger.ZERO,
            BigInteger.ONE,
            BigInteger.TEN,
            BigInteger.valueOf(Integer.MAX_VALUE)
        };

        BigInteger[] outOfRange = {
            BigInteger.valueOf((long)Integer.MIN_VALUE - 1),
            BigInteger.valueOf((long)Integer.MAX_VALUE + 1)
        };

        for (BigInteger bi : inRange) {
            if (bi.intValueExact() != bi.intValue()) {
                System.err.println("Mismatching int conversion for " + bi);
                errors++;
            }
        }

        for (BigInteger bi : outOfRange) {
            try {
                int value = bi.intValueExact();
                System.err.println("Failed to get expected exception on " +
                                   bi + " got " + value);
                errors++;
            } catch(ArithmeticException ae) {
                ; // Expected
            }
        }
        return errors;
    }

    private static int testShortValueExact() {
        int errors = 0;
        BigInteger[] inRange = {
            BigInteger.valueOf(Short.MIN_VALUE),
            BigInteger.ZERO,
            BigInteger.ONE,
            BigInteger.TEN,
            BigInteger.valueOf(Short.MAX_VALUE)
        };

        BigInteger[] outOfRange = {
            BigInteger.valueOf((long)Integer.MIN_VALUE - 1),
            BigInteger.valueOf((long)Integer.MIN_VALUE),
            BigInteger.valueOf(   (int)Short.MIN_VALUE - 1),
            BigInteger.valueOf(   (int)Short.MAX_VALUE + 1),
            BigInteger.valueOf((long)Integer.MAX_VALUE),
            BigInteger.valueOf((long)Integer.MAX_VALUE + 1)
        };

        for (BigInteger bi : inRange) {
            if (bi.shortValueExact() != bi.shortValue()) {
                System.err.println("Mismatching short  conversion for " + bi);
                errors++;
            }
        }

        for (BigInteger bi : outOfRange) {
            try {
                int value = bi.shortValueExact();
                System.err.println("Failed to get expected exception on " +
                                   bi + " got " + value);
                errors++;
            } catch(ArithmeticException ae) {
                ; // Expected
            }
        }
        return errors;
    }

    private static int testByteValueExact() {
        int errors = 0;
        BigInteger[] inRange = {
            BigInteger.valueOf(Byte.MIN_VALUE),
            BigInteger.valueOf(0),
            BigInteger.ONE,
            BigInteger.TEN,
            BigInteger.valueOf(Byte.MAX_VALUE)
        };

        BigInteger[] outOfRange = {
            BigInteger.valueOf((long)Integer.MIN_VALUE - 1),
            BigInteger.valueOf((long)Integer.MIN_VALUE),
            BigInteger.valueOf(   (int)Short.MIN_VALUE - 1),
            BigInteger.valueOf(   (int)Short.MIN_VALUE),
            BigInteger.valueOf(    (int)Byte.MIN_VALUE - 1),
            BigInteger.valueOf(    (int)Byte.MAX_VALUE + 1),
            BigInteger.valueOf(   (int)Short.MAX_VALUE + 1),
            BigInteger.valueOf(   (int)Short.MAX_VALUE),
            BigInteger.valueOf((long)Integer.MAX_VALUE),
            BigInteger.valueOf((long)Integer.MAX_VALUE + 1)
        };

        for (BigInteger bi : inRange) {
            if (bi.byteValueExact() != bi.byteValue()) {
                System.err.println("Mismatching byte conversion for " + bi);
                errors++;
            }
        }

        for (BigInteger bi : outOfRange) {
            try {
                int value = bi.byteValueExact();
                System.err.println("Failed to get expected exception on " +
                                   bi + " got " + value);
                errors++;
            } catch(ArithmeticException ae) {
                ; // Expected
            }
        }
        return errors;
    }
}
