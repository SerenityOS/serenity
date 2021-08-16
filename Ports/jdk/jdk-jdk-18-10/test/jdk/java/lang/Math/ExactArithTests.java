/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test Test for Math.*Exact integer and long methods.
 * @bug 6708398 8075806
 * @summary Basic tests for Math exact arithmetic operations.
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
        testLongIntExact();

        if (errors > 0) {
            throw new RuntimeException(errors + " errors found in ExactArithTests.");
        }
    }

    static void fail(String message) {
        errors++;
        System.err.println(message);
    }

    /**
     * Test Math.addExact, multiplyExact, divideExact, subtractExact,
     * incrementExact, decrementExact, negateExact methods with
     * {@code int} arguments.
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
            int sum = Math.addExact(x, y);
            long sum2 = (long) x + (long) y;
            if ((int) sum2 != sum2) {
                fail("FAIL: int Math.addExact(" + x + " + " + y + ") = " + sum + "; expected Arithmetic exception");
            } else if (sum != sum2) {
                fail("FAIL: long Math.addExact(" + x + " + " + y + ") = " + sum + "; expected: " + sum2);
            }
        } catch (ArithmeticException ex) {
            long sum2 = (long) x + (long) y;
            if ((int) sum2 == sum2) {
                fail("FAIL: int Math.addExact(" + x + " + " + y + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test subtractExact
            int diff = Math.subtractExact(x, y);
            long diff2 = (long) x - (long) y;
            if ((int) diff2 != diff2) {
                fail("FAIL: int Math.subtractExact(" + x + " - " + y + ") = " + diff + "; expected: " + diff2);
            }

        } catch (ArithmeticException ex) {
            long diff2 = (long) x - (long) y;
            if ((int) diff2 == diff2) {
                fail("FAIL: int Math.subtractExact(" + x + " - " + y + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test multiplyExact
            int product = Math.multiplyExact(x, y);
            long m2 = (long) x * (long) y;
            if ((int) m2 != m2) {
                fail("FAIL: int Math.multiplyExact(" + x + " * " + y + ") = " + product + "; expected: " + m2);
            }
        } catch (ArithmeticException ex) {
            long m2 = (long) x * (long) y;
            if ((int) m2 == m2) {
                fail("FAIL: int Math.multiplyExact(" + x + " * " + y + ")" + "; Unexpected exception: " + ex);
            }
        }

        boolean exceptionExpected = false;
        try {
            // Test divideExact
            BigInteger q = null;
            try {
                q = BigInteger.valueOf(x).divide(BigInteger.valueOf(y));
            } catch (ArithmeticException e) {
                exceptionExpected = true;
            }
            int quotient = 0;
            if (q != null) {
                try {
                    quotient = q.intValueExact();
                } catch (ArithmeticException e) {
                    exceptionExpected = true;
                }
            }
            int z = Math.divideExact(x, y);
            if (exceptionExpected) {
                fail("FAIL: int Math.divideExact(" + x + " / " + y + ")" +
                    "; expected ArithmeticException not thrown");
            }
            if (z != quotient) {
                fail("FAIL: int Math.divideExact(" + x + " / " + y + ") = " +
                    z + "; expected: " + quotient);
            }
        } catch (ArithmeticException ex) {
            if (!exceptionExpected) {
                fail("FAIL: int Math.divideExact(" + x + " / " + y + ")" +
                    "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test incrementExact
            int inc = Math.incrementExact(x);
            long inc2 = (long) x + 1L;
            if ((int) inc2 != inc2) {
                fail("FAIL: int Math.incrementExact(" + x + ") = " + inc + "; expected Arithmetic exception");
            } else if (inc != inc2) {
                fail("FAIL: long Math.incrementExact(" + x + ") = " + inc + "; expected: " + inc2);
            }
        } catch (ArithmeticException ex) {
            long inc2 = (long) x + 1L;
            if ((int) inc2 == inc2) {
                fail("FAIL: int Math.incrementExact(" + x + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test decrementExact
            int dec = Math.decrementExact(x);
            long dec2 = (long) x - 1L;
            if ((int) dec2 != dec2) {
                fail("FAIL: int Math.decrementExact(" + x + ") = " + dec + "; expected Arithmetic exception");
            } else if (dec != dec2) {
                fail("FAIL: long Math.decrementExact(" + x + ") = " + dec + "; expected: " + dec2);
            }
        } catch (ArithmeticException ex) {
            long dec2 = (long) x - 1L;
            if ((int) dec2 == dec2) {
                fail("FAIL: int Math.decrementExact(" + x + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test negateExact
            int neg = Math.negateExact(x);
            long neg2 = -((long)x);
            if ((int) neg2 != neg2) {
                fail("FAIL: int Math.negateExact(" + x + ") = " + neg + "; expected Arithmetic exception");
            } else if (neg != neg2) {
                fail("FAIL: long Math.negateExact(" + x + ") = " + neg + "; expected: " + neg2);
            }
        } catch (ArithmeticException ex) {
            long neg2 = -((long)x);
            if ((int) neg2 == neg2) {
                fail("FAIL: int Math.negateExact(" + x + ")" + "; Unexpected exception: " + ex);
            }
        }
    }

    /**
     * Test Math.addExact, multiplyExact, divideExact, subtractExact,
     * incrementExact, decrementExact, negateExact, toIntExact methods
     * with {@code long} arguments.
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
            long sum = Math.addExact(x, y);
            checkResult("long Math.addExact", x, y, sum, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long Math.addExact(" + x + " + " + y + "); Unexpected exception: " + ex);
            }
        }

        try {
            // Test subtractExact
            resultBig = xBig.subtract(yBig);
            long diff = Math.subtractExact(x, y);
            checkResult("long Math.subtractExact", x, y, diff, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long Math.subtractExact(" + x + " - " + y + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test multiplyExact
            resultBig = xBig.multiply(yBig);
            long product = Math.multiplyExact(x, y);
            checkResult("long Math.multiplyExact", x, y, product, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long Math.multiplyExact(" + x + " * " + y + ")" + "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test divideExact
            resultBig = null;
            try {
                resultBig = xBig.divide(yBig);
            } catch (ArithmeticException ex) {
            }
            long quotient = Math.divideExact(x, y);
            if (resultBig == null) {
                fail("FAIL: long Math.divideExact(" + x + " / " + y + ")" +
                    "; expected ArithmeticException not thrown");
            }
            checkResult("long Math.divideExact", x, y, quotient, resultBig);
        } catch (ArithmeticException ex) {
            if (resultBig != null && inLongRange(resultBig)) {
                fail("FAIL: long Math.divideExact(" + x + " / " + y + ")" +
                    "; Unexpected exception: " + ex);
            }
        }

        try {
            // Test incrementExact
            resultBig = xBig.add(BigInteger.ONE);
            long inc = Math.incrementExact(x);
            checkResult("long Math.incrementExact", x, 1L, inc, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long Math.incrementExact(" + x + "); Unexpected exception: " + ex);
            }
        }

        try {
            // Test decrementExact
            resultBig = xBig.subtract(BigInteger.ONE);
            long dec = Math.decrementExact(x);
            checkResult("long Math.decrementExact", x, 1L, dec, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long Math.decrementExact(" + x + "); Unexpected exception: " + ex);
            }
        }

        try {
            // Test negateExact
            resultBig = xBig.negate();
            long dec = Math.negateExact(x);
            checkResult("long Math.negateExact", x, 0L, dec, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long Math.negateExact(" + x + "); Unexpected exception: " + ex);
            }
        }

        try {
            // Test toIntExact
            int value = Math.toIntExact(x);
            if ((long)value != x) {
                fail("FAIL: " + "long Math.toIntExact" + "(" + x + ") = " + value + "; expected an arithmetic exception: ");
            }
        } catch (ArithmeticException ex) {
            if (resultBig.bitLength() <= 32) {
                fail("FAIL: long Math.toIntExact(" + x + ")" + "; Unexpected exception: " + ex);
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

    /**
     * Test Math.multiplyExact method with {@code long} and {@code int}
     * arguments.
     */
    static void testLongIntExact() {
        testLongIntExact(0, 0);
        testLongIntExact(1, 1);
        testLongIntExact(1, -1);
        testLongIntExact(1000, 2000);

        testLongIntExact(Long.MIN_VALUE, Integer.MIN_VALUE);
        testLongIntExact(Long.MAX_VALUE, Integer.MAX_VALUE);
        testLongIntExact(Long.MIN_VALUE, 1);
        testLongIntExact(Long.MAX_VALUE, 1);
        testLongIntExact(Long.MIN_VALUE, 2);
        testLongIntExact(Long.MAX_VALUE, 2);
        testLongIntExact(Long.MIN_VALUE, -1);
        testLongIntExact(Long.MAX_VALUE, -1);
        testLongIntExact(Long.MIN_VALUE, -2);
        testLongIntExact(Long.MAX_VALUE, -2);
        testLongIntExact(Long.MIN_VALUE/2, 2);
        testLongIntExact(Long.MAX_VALUE, 2);
        testLongIntExact(Integer.MAX_VALUE, Integer.MAX_VALUE);
        testLongIntExact(Integer.MAX_VALUE, -Integer.MAX_VALUE);
        testLongIntExact((long)Integer.MAX_VALUE+1L, Integer.MAX_VALUE);
        testLongIntExact((long)Integer.MAX_VALUE+1L, -Integer.MAX_VALUE+1);
        testLongIntExact((long)Integer.MIN_VALUE-1L, Integer.MIN_VALUE);
        testLongIntExact((long)Integer.MIN_VALUE-1, Integer.MAX_VALUE);
        testLongIntExact(Integer.MIN_VALUE/2, 2);
    }

    /**
     * Test long-int exact arithmetic by comparing with the same operations using BigInteger
     * and checking that the result is the same as the long truncation.
     * Errors are reported with {@link fail}.
     *
     * @param x first parameter
     * @param y second parameter
     */
    static void testLongIntExact(long x, int y) {
        BigInteger resultBig = null;
        final BigInteger xBig = BigInteger.valueOf(x);
        final BigInteger yBig = BigInteger.valueOf(y);

        try {
            // Test multiplyExact
            resultBig = xBig.multiply(yBig);
            long product = Math.multiplyExact(x, y);
            checkResult("long Math.multiplyExact", x, y, product, resultBig);
        } catch (ArithmeticException ex) {
            if (inLongRange(resultBig)) {
                fail("FAIL: long Math.multiplyExact(" + x + " * " + y + ")" + "; Unexpected exception: " + ex);
            }
        }
    }
}
