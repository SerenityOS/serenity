/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * Shared static test methods for numerical tests.  Sharing these
 * helper test methods avoids repeated functions in the various test
 * programs.  The test methods return 1 for a test failure and 0 for
 * success.  The order of arguments to the test methods is generally
 * the test name, followed by the test arguments, the computed result,
 * and finally the expected result.
 */

public class Tests {
    private Tests(){}; // do not instantiate

    public static String toHexString(float f) {
        if (!Float.isNaN(f))
            return Float.toHexString(f);
        else
            return "NaN(0x" + Integer.toHexString(Float.floatToRawIntBits(f)) + ")";
    }

    public static String toHexString(double d) {
        if (!Double.isNaN(d))
            return Double.toHexString(d);
        else
            return "NaN(0x" + Long.toHexString(Double.doubleToRawLongBits(d)) + ")";
    }

    /**
     * Return the floating-point value next larger in magnitude.
     */
    public static double nextOut(double d) {
        if (d > 0.0)
            return Math.nextUp(d);
        else
            return -Math.nextUp(-d);
    }

    /**
     * Returns unbiased exponent of a {@code float}; for
     * subnormal values, the number is treated as if it were
     * normalized.  That is for all finite, non-zero, positive numbers
     * <i>x</i>, <code>scalb(<i>x</i>, -ilogb(<i>x</i>))</code> is
     * always in the range [1, 2).
     * <p>
     * Special cases:
     * <ul>
     * <li> If the argument is NaN, then the result is 2<sup>30</sup>.
     * <li> If the argument is infinite, then the result is 2<sup>28</sup>.
     * <li> If the argument is zero, then the result is -(2<sup>28</sup>).
     * </ul>
     *
     * @param f floating-point number whose exponent is to be extracted
     * @return unbiased exponent of the argument.
     */
    public static int ilogb(double d) {
        int exponent = Math.getExponent(d);

        switch (exponent) {
        case Double.MAX_EXPONENT+1:       // NaN or infinity
            if( Double.isNaN(d) )
                return (1<<30);         // 2^30
            else // infinite value
                return (1<<28);         // 2^28

        case Double.MIN_EXPONENT-1:       // zero or subnormal
            if(d == 0.0) {
                return -(1<<28);        // -(2^28)
            }
            else {
                long transducer = Double.doubleToRawLongBits(d);

                /*
                 * To avoid causing slow arithmetic on subnormals,
                 * the scaling to determine when d's significand
                 * is normalized is done in integer arithmetic.
                 * (there must be at least one "1" bit in the
                 * significand since zero has been screened out.
                 */

                // isolate significand bits
                transducer &= DoubleConsts.SIGNIF_BIT_MASK;
                assert(transducer != 0L);

                // This loop is simple and functional. We might be
                // able to do something more clever that was faster;
                // e.g. number of leading zero detection on
                // (transducer << (# exponent and sign bits).
                while (transducer <
                       (1L << (DoubleConsts.SIGNIFICAND_WIDTH - 1))) {
                    transducer *= 2;
                    exponent--;
                }
                exponent++;
                assert( exponent >=
                        Double.MIN_EXPONENT - (DoubleConsts.SIGNIFICAND_WIDTH-1) &&
                        exponent < Double.MIN_EXPONENT);
                return exponent;
            }

        default:
            assert( exponent >= Double.MIN_EXPONENT &&
                    exponent <= Double.MAX_EXPONENT);
            return exponent;
        }
    }

    /**
     * Returns unbiased exponent of a {@code float}; for
     * subnormal values, the number is treated as if it were
     * normalized.  That is for all finite, non-zero, positive numbers
     * <i>x</i>, <code>scalb(<i>x</i>, -ilogb(<i>x</i>))</code> is
     * always in the range [1, 2).
     * <p>
     * Special cases:
     * <ul>
     * <li> If the argument is NaN, then the result is 2<sup>30</sup>.
     * <li> If the argument is infinite, then the result is 2<sup>28</sup>.
     * <li> If the argument is zero, then the result is -(2<sup>28</sup>).
     * </ul>
     *
     * @param f floating-point number whose exponent is to be extracted
     * @return unbiased exponent of the argument.
     */
     public static int ilogb(float f) {
        int exponent = Math.getExponent(f);

        switch (exponent) {
        case Float.MAX_EXPONENT+1:        // NaN or infinity
            if( Float.isNaN(f) )
                return (1<<30);         // 2^30
            else // infinite value
                return (1<<28);         // 2^28

        case Float.MIN_EXPONENT-1:        // zero or subnormal
            if(f == 0.0f) {
                return -(1<<28);        // -(2^28)
            }
            else {
                int transducer = Float.floatToRawIntBits(f);

                /*
                 * To avoid causing slow arithmetic on subnormals,
                 * the scaling to determine when f's significand
                 * is normalized is done in integer arithmetic.
                 * (there must be at least one "1" bit in the
                 * significand since zero has been screened out.
                 */

                // isolate significand bits
                transducer &= FloatConsts.SIGNIF_BIT_MASK;
                assert(transducer != 0);

                // This loop is simple and functional. We might be
                // able to do something more clever that was faster;
                // e.g. number of leading zero detection on
                // (transducer << (# exponent and sign bits).
                while (transducer <
                       (1 << (FloatConsts.SIGNIFICAND_WIDTH - 1))) {
                    transducer *= 2;
                    exponent--;
                }
                exponent++;
                assert( exponent >=
                        Float.MIN_EXPONENT - (FloatConsts.SIGNIFICAND_WIDTH-1) &&
                        exponent < Float.MIN_EXPONENT);
                return exponent;
            }

        default:
            assert( exponent >= Float.MIN_EXPONENT &&
                    exponent <= Float.MAX_EXPONENT);
            return exponent;
        }
    }

    /**
     * Returns {@code true} if the unordered relation holds
     * between the two arguments.  When two floating-point values are
     * unordered, one value is neither less than, equal to, nor
     * greater than the other.  For the unordered relation to be true,
     * at least one argument must be a {@code NaN}.
     *
     * @param arg1      the first argument
     * @param arg2      the second argument
     * @return {@code true} if at least one argument is a NaN,
     * {@code false} otherwise.
     */
     public static boolean isUnordered(float arg1, float arg2) {
        return Float.isNaN(arg1) || Float.isNaN(arg2);
    }

    /**
     * Returns {@code true} if the unordered relation holds
     * between the two arguments.  When two floating-point values are
     * unordered, one value is neither less than, equal to, nor
     * greater than the other.  For the unordered relation to be true,
     * at least one argument must be a {@code NaN}.
     *
     * @param arg1      the first argument
     * @param arg2      the second argument
     * @return {@code true} if at least one argument is a NaN,
     * {@code false} otherwise.
     */
    public static boolean isUnordered(double arg1, double arg2) {
        return Double.isNaN(arg1) || Double.isNaN(arg2);
    }

    public static int test(String testName, float input,
                           boolean result, boolean expected) {
        if (expected != result) {
            System.err.println("Failure for " + testName + ":\n" +
                               "\tFor input " + input    + "\t(" + toHexString(input) + ")\n" +
                               "\texpected  " + expected + "\n"  +
                               "\tgot       " + result   + ").");
            return 1;
        }
        else
            return 0;
    }

    public static int test(String testName, double input,
                           boolean result, boolean expected) {
        if (expected != result) {
            System.err.println("Failure for " + testName + ":\n" +
                               "\tFor input " + input    + "\t(" + toHexString(input) + ")\n" +
                               "\texpected  " + expected + "\n"  +
                               "\tgot       " + result   + ").");
            return 1;
        }
        else
            return 0;
    }

    public static int test(String testName, float input1, float input2,
                           boolean result, boolean expected) {
        if (expected != result) {
            System.err.println("Failure for "  + testName + ":\n" +
                               "\tFor inputs " + input1   + "\t(" + toHexString(input1) + ") and "
                                               + input2   + "\t(" + toHexString(input2) + ")\n" +
                               "\texpected  "  + expected + "\n"  +
                               "\tgot       "  + result   + ").");
            return 1;
        }
        return 0;
    }

    public static int test(String testName, double input1, double input2,
                           boolean result, boolean expected) {
        if (expected != result) {
            System.err.println("Failure for "  + testName + ":\n" +
                               "\tFor inputs " + input1   + "\t(" + toHexString(input1) + ") and "
                                               + input2   + "\t(" + toHexString(input2) + ")\n" +
                               "\texpected  "  + expected + "\n"  +
                               "\tgot       "  + result   + ").");
            return 1;
        }
        return 0;
    }

    public static int test(String testName, float input,
                           int result, int expected) {
        if (expected != result) {
            System.err.println("Failure for " + testName + ":\n" +
                               "\tFor input " + input    + "\t(" + toHexString(input) + ")\n" +
                               "\texpected  " + expected + "\n" +
                               "\tgot       " + result    + ").");
            return 1;
        }
        return 0;
    }

    public  static int test(String testName, double input,
                            int result, int expected) {
        if (expected != result) {
            System.err.println("Failure for " + testName + ":\n" +
                               "\tFor input " + input    + "\t(" + toHexString(input) + ")\n" +
                               "\texpected  " + expected + "\n"  +
                               "\tgot       " + result   + ").");
            return 1;
        }
        else
            return 0;
    }

    public static int test(String testName, float input,
                           float result, float expected) {
        if (Float.compare(expected, result) != 0 ) {
            System.err.println("Failure for " + testName + ":\n" +
                               "\tFor input " + input    + "\t(" + toHexString(input) + ")\n" +
                               "\texpected  " + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       " + result   + "\t(" + toHexString(result) + ").");
            return 1;
        }
        else
            return 0;
    }


    public static int test(String testName, double input,
                           double result, double expected) {
        if (Double.compare(expected, result ) != 0) {
            System.err.println("Failure for " + testName + ":\n" +
                               "\tFor input " + input    + "\t(" + toHexString(input) + ")\n" +
                               "\texpected  " + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       " + result   + "\t(" + toHexString(result) + ").");
            return 1;
        }
        else
            return 0;
    }

    public static int test(String testName,
                           float input1, double input2,
                           float result, float expected) {
        if (Float.compare(expected, result ) != 0) {
            System.err.println("Failure for "  + testName + ":\n" +
                               "\tFor inputs " + input1   + "\t(" + toHexString(input1) + ") and "
                                               + input2   + "\t(" + toHexString(input2) + ")\n" +
                               "\texpected  "  + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       "  + result   + "\t(" + toHexString(result) + ").");
            return 1;
        }
        else
            return 0;
    }

    public static int test(String testName,
                           double input1, double input2,
                           double result, double expected) {
        if (Double.compare(expected, result ) != 0) {
            System.err.println("Failure for "  + testName + ":\n" +
                               "\tFor inputs " + input1   + "\t(" + toHexString(input1) + ") and "
                                               + input2   + "\t(" + toHexString(input2) + ")\n" +
                               "\texpected  "  + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       "  + result   + "\t(" + toHexString(result) + ").");
            return 1;
        }
        else
            return 0;
    }

    public static int test(String testName,
                           float input1, int input2,
                           float result, float expected) {
        if (Float.compare(expected, result ) != 0) {
            System.err.println("Failure for "  + testName + ":\n" +
                               "\tFor inputs " + input1   + "\t(" + toHexString(input1) + ") and "
                                               + input2   + "\n"  +
                               "\texpected  "  + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       "  + result   + "\t(" + toHexString(result) + ").");
            return 1;
        }
        else
            return 0;
    }

    public static int test(String testName,
                           double input1, int input2,
                           double result, double expected) {
        if (Double.compare(expected, result ) != 0) {
            System.err.println("Failure for "  + testName + ":\n" +
                               "\tFor inputs " + input1   + "\t(" + toHexString(input1) + ") and "
                                               + input2   + "\n"  +
                               "\texpected  "  + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       "  + result   + "\t(" + toHexString(result) + ").");
            return 1;
        }
        else
            return 0;
    }

    public static int test(String testName,
                           float input1, float input2, float input3,
                           float result, float expected) {
        if (Float.compare(expected, result ) != 0) {
            System.err.println("Failure for "  + testName + ":\n" +
                               "\tFor inputs " + input1   + "\t(" + toHexString(input1) + ") and "
                                               + input2   + "\t(" + toHexString(input2) + ") and"
                                               + input3   + "\t(" + toHexString(input3) + ")\n"  +
                               "\texpected  "  + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       "  + result   + "\t(" + toHexString(result) + ").");
            return 1;
        }
        else
            return 0;
    }

    public static int test(String testName,
                           double input1, double input2, double input3,
                           double result, double expected) {
        if (Double.compare(expected, result ) != 0) {
            System.err.println("Failure for "  + testName + ":\n" +
                               "\tFor inputs " + input1   + "\t(" + toHexString(input1) + ") and "
                                               + input2   + "\t(" + toHexString(input2) + ") and"
                                               + input3   + "\t(" + toHexString(input3) + ")\n"  +
                               "\texpected  "  + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       "  + result   + "\t(" + toHexString(result) + ").");
            return 1;
        }
        else
            return 0;
    }

    static int testUlpCore(double result, double expected, double ulps) {
        // We assume we won't be unlucky and have an inexact expected
        // be nextDown(2^i) when 2^i would be the correctly rounded
        // answer.  This would cause the ulp size to be half as large
        // as it should be, doubling the measured error).

        if (Double.compare(expected, result) == 0) {
            return 0;   // result and expected are equivalent
        } else {
            if( ulps == 0.0) {
                // Equivalent results required but not found
                return 1;
            } else {
                double difference = expected - result;
                if (isUnordered(expected, result) ||
                    Double.isNaN(difference) ||
                    // fail if greater than or unordered
                    !(Math.abs( difference/Math.ulp(expected) ) <= Math.abs(ulps)) ) {
                    return 1;
                }
                else
                    return 0;
            }
        }
    }

    // One input argument.
    public static int testUlpDiff(String testName, double input,
                                  double result, double expected, double ulps) {
        int code = testUlpCore(result, expected, ulps);
        if (code == 1) {
            System.err.println("Failure for " + testName + ":\n" +
                               "\tFor input " + input    + "\t(" + toHexString(input) + ")\n" +
                               "\texpected  " + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       " + result   + "\t(" + toHexString(result) + ");\n" +
                               "\tdifference greater than ulp tolerance " + ulps);
        }
        return code;
    }

    // Two input arguments.
    public static int testUlpDiff(String testName, double input1, double input2,
                                  double result, double expected, double ulps) {
        int code = testUlpCore(result, expected, ulps);
        if (code == 1) {
            System.err.println("Failure for "  + testName + ":\n" +
                               "\tFor inputs " + input1   + "\t(" + toHexString(input1) + ") and "
                                               + input2   + "\t(" + toHexString(input2) + ")\n" +
                               "\texpected  "  + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       "  + result   + "\t(" + toHexString(result) + ");\n" +
                               "\tdifference greater than ulp tolerance " + ulps);
        }
        return code;
    }

    // For a successful test, the result must be within the ulp bound of
    // expected AND the result must have absolute value less than or
    // equal to absBound.
    public static int testUlpDiffWithAbsBound(String testName, double input,
                                              double result, double expected,
                                              double ulps, double absBound) {
        int code = 0;   // return code value

        if (!(StrictMath.abs(result) <= StrictMath.abs(absBound)) &&
            !Double.isNaN(expected)) {
            code = 1;
        } else
            code = testUlpCore(result, expected, ulps);

        if (code == 1) {
            System.err.println("Failure for " + testName + ":\n" +
                               "\tFor input " + input    + "\t(" + toHexString(input) + ")\n" +
                               "\texpected  " + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       " + result   + "\t(" + toHexString(result) + ");\n" +
                               "\tdifference greater than ulp tolerance " + ulps +
                               " or the result has larger magnitude than " + absBound);
        }
        return code;
    }

    // For a successful test, the result must be within the ulp bound of
    // expected AND the result must have absolute value greater than
    // or equal to the lowerBound.
    public static int testUlpDiffWithLowerBound(String testName, double input,
                                                double result, double expected,
                                                double ulps, double lowerBound) {
        int code = 0;   // return code value

        if (!(result >= lowerBound) && !Double.isNaN(expected)) {
            code = 1;
        } else
            code = testUlpCore(result, expected, ulps);

        if (code == 1) {
            System.err.println("Failure for " + testName +
                               ":\n" +
                               "\tFor input "   + input    + "\t(" + toHexString(input) + ")" +
                               "\n\texpected  " + expected + "\t(" + toHexString(expected) + ")" +
                               "\n\tgot       " + result   + "\t(" + toHexString(result) + ");" +
                               "\ndifference greater than ulp tolerance " + ulps +
                               " or result not greater than or equal to the bound " + lowerBound);
        }
        return code;
    }

    public static int testTolerance(String testName, double input,
                                    double result, double expected, double tolerance) {
        if (Double.compare(expected, result ) != 0) {
            double difference = expected - result;
            if (isUnordered(expected, result) ||
                Double.isNaN(difference) ||
                // fail if greater than or unordered
                !(Math.abs((difference)/expected) <= StrictMath.pow(10, -tolerance)) ) {
                System.err.println("Failure for " + testName + ":\n" +
                                   "\tFor input " + input    + "\t(" + toHexString(input) + ")\n" +
                                   "\texpected  " + expected + "\t(" + toHexString(expected) + ")\n" +
                                   "\tgot       " + result   + "\t(" + toHexString(result) + ");\n" +
                                   "\tdifference greater than tolerance 10^-" + tolerance);
                return 1;
            }
            return 0;
        }
        else
            return 0;
    }

    // For a successful test, the result must be within the upper and
    // lower bounds.
    public static int testBounds(String testName, double input, double result,
                                 double bound1, double bound2) {
        if ((result >= bound1 && result <= bound2) ||
            (result <= bound1 && result >= bound2))
            return 0;
        else {
            double lowerBound = Math.min(bound1, bound2);
            double upperBound = Math.max(bound1, bound2);
            System.err.println("Failure for " + testName + ":\n" +
                               "\tFor input " + input    + "\t(" + toHexString(input) + ")\n" +
                               "\tgot       " + result   + "\t(" + toHexString(result) + ");\n" +
                               "\toutside of range\n" +
                               "\t[" + lowerBound    + "\t(" + toHexString(lowerBound) + "), " +
                               upperBound    + "\t(" + toHexString(upperBound) + ")]");
            return 1;
        }
    }
}
