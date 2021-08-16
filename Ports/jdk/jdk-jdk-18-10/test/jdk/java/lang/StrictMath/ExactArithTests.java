/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;

/**
 * @test Test for StrictMath.*Exact integer and long methods.
 * @bug 6708398
 * @summary Basic tests for StrictMath exact arithmetic operations.
 *
 * @author Roger Riggs
 */
public class ExactArithTests {

    /**
     * The count of test errors.
     */
    private static int errors = 0;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        testIntegerExact();
        testLongExact();

        if (errors > 0) {
            throw new RuntimeException(errors + " errors found in ExactArithTests.");
        }
    }

    static void fail(String message) {
        errors++;
        System.err.println(message);
    }

    /**
     * Test StrictMath.addExact, multiplyExact, subtractExact, incrementExact,
     * decrementExact, negateExact methods with {@code int} arguments.
     */
    static void testIntegerExact() {
        testIntegerExact(0, 0);
        testIntegerExact(1, 1);
        testIntegerExact(1, -1);
        testIntegerExact(-1, 1);
        testIntegerExact(1000, 2000);

        testIntegerExact(Integer.MIN_VALUE, Integer.MIN_VALUE);
        testIntegerExact(Integer.MAX_VALUE, Integer.MAX_VALUE);
        testIntegerExact(Integer.MIN_VALUE, 1);
        testIntegerExact(Integer.MAX_VALUE, 1);
        testIntegerExact(Integer.MIN_VALUE, 2);
        testIntegerExact(Integer.MAX_VALUE, 2);
        testIntegerExact(Integer.MIN_VALUE, -1);
        testIntegerExact(Integer.MAX_VALUE, -1);
        testIntegerExact(Integer.MIN_VALUE, -2);
        testIntegerExact(Integer.MAX_VALUE, -2);
    }

    /**
     * Test exact arithmetic by comparing with the same operations using long
     * and checking that the result is the same as the integer truncation.
     * Errors are reported with {@link fail}.
     *
     * @param x first parameter
     * @param y second parameter
     */
    static void testIntegerExact(int x, int y) {
        try {
            // Test addExact
            int sum = StrictMath.addExact(x, y);
            long sum2 = (long) x + (long) y;
            if ((int) sum2 != sum2) {
                fail("FAIL: int StrictMath.addExact(" + x + " + " + y + ") = " + sum + "; expected Arithmetic exception");
            } else if (sum != sum2) {
                fail("FAIL: long StrictMath.addExact(" + x + " + " + y + ") = " + sum + "; expected: " + sum2);
            }
        } catch (ArithmeticException ex) {
            long sum2 = (long) x + (long) y;
            if ((int) sum2 == sum2) {
                fail("FAIL: int StrictMath.addExact(" + x + " + " + y + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test subtractExact
            int diff = StrictMath.subtractExact(x, y);
            long diff2 = (long) x - (long) y;
            if ((int) diff2 != diff2) {
                fail("FAIL: int StrictMath.subtractExact(" + x + " - " + y + ") = " + diff + "; expected: " + diff2);
            }

        } catch (ArithmeticException ex) {
            long diff2 = (long) x - (long) y;
            if ((int) diff2 == diff2) {
                fail("FAIL: int StrictMath.subtractExact(" + x + " - " + y + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test multiplyExact
            int product = StrictMath.multiplyExact(x, y);
            long m2 = (long) x * (long) y;
            if ((int) m2 != m2) {
                fail("FAIL: int StrictMath.multiplyExact(" + x + " * " + y + ") = " + product + "; expected: " + m2);
            }
        } catch (ArithmeticException ex) {
            long m2 = (long) x * (long) y;
            if ((int) m2 == m2) {
                fail("FAIL: int StrictMath.multiplyExact(" + x + " * " + y + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test incrementExact
            int inc = StrictMath.incrementExact(x);
            long inc2 = (long) x + 1L;
            if ((int) inc2 != inc2) {
                fail("FAIL: int StrictMath.incrementExact(" + x + ") = " + inc + "; expected Arithmetic exception");
            } else if (inc != inc2) {
                fail("FAIL: long StrictMath.incrementExact(" + x + ") = " + inc + "; expected: " + inc2);
            }
        } catch (ArithmeticException ex) {
            long inc2 = (long) x + 1L;
            if ((int) inc2 == inc2) {
                fail("FAIL: int StrictMath.incrementExact(" + x + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test decrementExact
            int dec = StrictMath.decrementExact(x);
            long dec2 = (long) x - 1L;
            if ((int) dec2 != dec2) {
                fail("FAIL: int StrictMath.decrementExact(" + x + ") = " + dec + "; expected Arithmetic exception");
            } else if (dec != dec2) {
                fail("FAIL: long StrictMath.decrementExact(" + x + ") = " + dec + "; expected: " + dec2);
            }
        } catch (ArithmeticException ex) {
            long dec2 = (long) x - 1L;
            if ((int) dec2 == dec2) {
                fail("FAIL: int StrictMath.decrementExact(" + x + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test negateExact
            int neg = StrictMath.negateExact(x);
            long neg2 = -((long)x);
            if ((int) neg2 != neg2) {
                fail("FAIL: int StrictMath.negateExact(" + x + ") = " + neg + "; expected Arithmetic exception");
            } else if (neg != neg2) {
                fail("FAIL: long StrictMath.negateExact(" + x + ") = " + neg + "; expected: " + neg2);
            }
        } catch (ArithmeticException ex) {
            long neg2 = -((long)x);
            if ((int) neg2 == neg2) {
                fail("FAIL: int StrictMath.negateExact(" + x + ")" + "; Unexpected exception: " + ex);
            }
        }
    }

    /**
     * Test StrictMath.addExact, multiplyExact, subtractExact, incrementExact,
     * decrementExact, negateExact, toIntExact methods with {@code long} arguments.
     */
    static void testLongExact() {
        testLongExactTwice(0, 0);
        testLongExactTwice(1, 1);
        testLongExactTwice(1, -1);
        testLongExactTwice(1000, 2000);

        testLongExactTwice(Long.MIN_VALUE, Long.MIN_VALUE);
        testLongExactTwice(Long.MAX_VALUE, Long.MAX_VALUE);
        testLongExactTwice(Long.MIN_VALUE, 1);
        testLongExactTwice(Long.MAX_VALUE, 1);
        testLongExactTwice(Long.MIN_VALUE, 2);
        testLongExactTwice(Long.MAX_VALUE, 2);
        testLongExactTwice(Long.MIN_VALUE, -1);
        testLongExactTwice(Long.MAX_VALUE, -1);
        testLongExactTwice(Long.MIN_VALUE, -2);
        testLongExactTwice(Long.MAX_VALUE, -2);
        testLongExactTwice(Long.MIN_VALUE/2, 2);
        testLongExactTwice(Long.MAX_VALUE, 2);
        testLongExactTwice(Integer.MAX_VALUE, Integer.MAX_VALUE);
        testLongExactTwice(Integer.MAX_VALUE, -Integer.MAX_VALUE);
        testLongExactTwice(Integer.MAX_VALUE+1, Integer.MAX_VALUE+1);
        testLongExactTwice(Integer.MAX_VALUE+1, -Integer.MAX_VALUE+1);
        testLongExactTwice(Integer.MIN_VALUE-1, Integer.MIN_VALUE-1);
        testLongExactTwice(Integer.MIN_VALUE-1, -Integer.MIN_VALUE-1);
        testLongExactTwice(Integer.MIN_VALUE/2, 2);
    }

    /**
     * Test each of the exact operations with the arguments and
     * with the arguments reversed.
     * @param x
     * @param y
     */
    static void testLongExactTwice(long x, long y) {
        testLongExact(x, y);
        testLongExact(y, x);
    }


    /**
     * Test long exact arithmetic by comparing with the same operations using BigInteger
     * and checking that the result is the same as the long truncation.
     * Errors are reported with {@link fail}.
     *
     * @param x first parameter
     * @param y second parameter
     */
    static void testLongExact(long x, long y) {
        BigInteger resultBig = null;
        final BigInteger xBig = BigInteger.valueOf(x);
        final BigInteger yBig = BigInteger.valueOf(y);
        try {
            // Test addExact
            resultBig = xBig.add(yBig);
            long sum = StrictMath.addExact(x, y);
            checkResult("long StrictMath.addExact", x, y, sum, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long StrictMath.addExact(" + x + " + " + y + "); Unexpected exception: " + ex);
            }
        }

        try {
            // Test subtractExact
            resultBig = xBig.subtract(yBig);
            long diff = StrictMath.subtractExact(x, y);
            checkResult("long StrictMath.subtractExact", x, y, diff, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long StrictMath.subtractExact(" + x + " - " + y + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test multiplyExact
            resultBig = xBig.multiply(yBig);
            long product = StrictMath.multiplyExact(x, y);
            checkResult("long StrictMath.multiplyExact", x, y, product, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long StrictMath.multiplyExact(" + x + " * " + y + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test incrementExact
            resultBig = xBig.add(BigInteger.ONE);
            long inc = StrictMath.incrementExact(x);
            checkResult("long Math.incrementExact", x, 1L, inc, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long Math.incrementExact(" + x + "); Unexpected exception: " + ex);
            }
        }

        try {
            // Test decrementExact
            resultBig = xBig.subtract(BigInteger.ONE);
            long dec = StrictMath.decrementExact(x);
            checkResult("long Math.decrementExact", x, 1L, dec, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long Math.decrementExact(" + x + "); Unexpected exception: " + ex);
            }
        }

        try {
            // Test negateExact
            resultBig = xBig.negate();
            long dec = StrictMath.negateExact(x);
            checkResult("long Math.negateExact", x, 0L, dec, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long Math.negateExact(" + x + "); Unexpected exception: " + ex);
            }
        }

        try {
            // Test toIntExact
            int value = StrictMath.toIntExact(x);
            if ((long)value != x) {
                fail("FAIL: " + "long StrictMath.toIntExact" + "(" + x + ") = " + value + "; expected an arithmetic exception: ");
            }
        } catch (ArithmeticException ex) {
            if (resultBig.bitLength() <= 32) {
                fail("FAIL: long StrictMath.toIntExact(" + x + ")" + "; Unexpected exception: " + ex);
            }
        }
    }

    /**
     * Compare the expected and actual results.
     * @param message message for the error
     * @param x first argument
     * @param y second argument
     * @param result actual result value
     * @param expected expected result value
     */
    static void checkResult(String message, long x, long y, long result, BigInteger expected) {
        BigInteger resultBig = BigInteger.valueOf(result);
        if (!inLongRange(expected)) {
            fail("FAIL: " + message + "(" + x + ", " + y + ") = " + result + "; expected an arithmetic exception: ");
        } else if (!resultBig.equals(expected)) {
            fail("FAIL: " + message + "(" + x + ", " + y + ") = " + result + "; expected " + expected);
        }
    }

    /**
     * Check if the value fits in 64 bits (a long).
     * @param value
     * @return true if the value fits in 64 bits (including the sign).
     */
    static boolean inLongRange(BigInteger value) {
        return value.bitLength() <= 63;
    }
}
