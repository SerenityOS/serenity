/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.DoubleStream;
import jdk.internal.math.DoubleConsts;
import jdk.internal.math.FloatConsts;

/*
 * @test
 * @bug 6506405 8241374
 * @summary Test abs and absExact for Math and StrictMath
 * @modules java.base/jdk.internal.math
 */
public class AbsTests {
    private static final double GELFOND = Math.exp(Math.PI);
    private static final double TAU     = 2.0*Math.PI;

    // Values for testing float and double abs
    private static final double[] FLOATING_POINT_VALUES = new double[] {
        0.0,
        -0.0,
        +0.0,
        Double.MIN_VALUE,
        Double.MIN_NORMAL,
        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.NaN,
        Float.MIN_VALUE,
        Float.MIN_NORMAL,
        Float.NEGATIVE_INFINITY,
        Float.POSITIVE_INFINITY,
        Float.NaN,
        Double.longBitsToDouble((1 << DoubleConsts.SIGNIFICAND_WIDTH) |
           ((1 << DoubleConsts.SIGNIFICAND_WIDTH) - 1)),
        DoubleConsts.MAG_BIT_MASK >>> 1,
        Float.intBitsToFloat((1 << FloatConsts.SIGNIFICAND_WIDTH) |
           ((1 << FloatConsts.SIGNIFICAND_WIDTH) - 1)),
        FloatConsts.MAG_BIT_MASK >>> 1,
        Math.E,
        GELFOND,
        Math.PI,
        TAU
    };

    private static int errors = 0;

    public static void main(String... args) {
        errors += testInRangeIntAbs();
        errors += testIntMinValue();
        errors += testInRangeLongAbs();
        errors += testLongMinValue();
        errors += testFloatAbs();
        errors += testDoubleAbs();

        if (errors > 0) {
            throw new RuntimeException(errors + " errors found testing abs.");
        }
    }

    // --------------------------------------------------------------------

    private static int testInRangeIntAbs() {
        int errors = 0;
        int[][] testCases  = {
            // Argument to abs, expected result
            {+0, 0},
            {+1, 1},
            {-1, 1},
            {-2, 2},
            {+2, 2},
            {-Integer.MAX_VALUE, Integer.MAX_VALUE},
            {+Integer.MAX_VALUE, Integer.MAX_VALUE}
        };

        for(var testCase : testCases) {
            errors += testIntAbs(Math::abs,      testCase[0], testCase[1]);
            errors += testIntAbs(Math::absExact, testCase[0], testCase[1]);
        }
        return errors;
    }

    private static int testIntMinValue() {
        int errors = 0;
        // Strange but true
        errors += testIntAbs(Math::abs, Integer.MIN_VALUE, Integer.MIN_VALUE);

        // Test exceptional behavior for absExact
        try {
            int result = Math.absExact(Integer.MIN_VALUE);
            System.err.printf("Bad return value %d from Math.absExact(MIN_VALUE)%n",
                              result);
            errors++;
        } catch (ArithmeticException ae) {
            ; // Expected
        }
        return errors;
    }

    private static int testIntAbs(IntUnaryOperator absFunc,
                           int argument, int expected) {
        int result = absFunc.applyAsInt(argument);
        if (result != expected) {
            System.err.printf("Unexpected int abs result %d for argument %d%n",
                                result, argument);
            return 1;
        } else {
            return 0;
        }
    }

    // --------------------------------------------------------------------

    private static long testInRangeLongAbs() {
        int errors = 0;
        long[][] testCases  = {
            // Argument to abs, expected result
            {+0L, 0L},
            {+1L, 1L},
            {-1L, 1L},
            {-2L, 2L},
            {+2L, 2L},
            {-Integer.MAX_VALUE, Integer.MAX_VALUE},
            {+Integer.MAX_VALUE, Integer.MAX_VALUE},
            { Integer.MIN_VALUE, -((long)Integer.MIN_VALUE)},
            {-Long.MAX_VALUE, Long.MAX_VALUE},
        };

        for(var testCase : testCases) {
            errors += testLongAbs(Math::abs,      testCase[0], testCase[1]);
            errors += testLongAbs(Math::absExact, testCase[0], testCase[1]);
        }
        return errors;
    }

    private static int testLongMinValue() {
        int errors = 0;
        // Strange but true
        errors += testLongAbs(Math::abs, Long.MIN_VALUE, Long.MIN_VALUE);

        // Test exceptional behavior for absExact
        try {
            long result = Math.absExact(Long.MIN_VALUE);
            System.err.printf("Bad return value %d from Math.absExact(MIN_VALUE)%n",
                              result);
            errors++;
        } catch (ArithmeticException ae) {
            ; // Expected
        }
        return errors;
    }

    private static int testLongAbs(LongUnaryOperator absFunc,
                           long argument, long expected) {
        long result = absFunc.applyAsLong(argument);
        if (result != expected) {
            System.err.printf("Unexpected long abs result %d for argument %d%n",
                                result, argument);
            return 1;
        } else {
            return 0;
        }
    }

    // --------------------------------------------------------------------

    private static int testFloatAbs() {
        DoubleStream doubles = DoubleStream.of(FLOATING_POINT_VALUES);

        final AtomicInteger errors = new AtomicInteger();
        doubles.mapToObj(d -> (float)d).
            forEach(f -> {errors.addAndGet(testFloatAbs(f));});

        return errors.get();
    }

    private static int testFloatAbs(float f) {
        int errors  = testFloatAbs("Math.abs", Math::abs, f);
        errors     += testFloatAbs("Math.abs", Math::abs, -f);
        errors     += testFloatAbs("StrictMath.abs", StrictMath::abs, f);
        errors     += testFloatAbs("StrictMath.abs", StrictMath::abs, -f);
        return errors;
    }

    private static int testFloatAbs(String testName,
                                    UnaryOperator<Float> absFunc, float f) {
        float result = absFunc.apply(-f);
        if (Float.isNaN(f) && Float.isNaN(result)) {
            return 0;
        }

        float expected = f == -0.0F ? 0.0F : (f < 0.0F ? -f : f);
        return Tests.test(testName, f, result, expected);
    }

    // --------------------------------------------------------------------

    private static int testDoubleAbs() {
        DoubleStream doubles = DoubleStream.of(FLOATING_POINT_VALUES);

        final AtomicInteger errors = new AtomicInteger();
        doubles.forEach(d -> {errors.addAndGet(testDoubleAbs(d));});

        return errors.get();
    }

    private static int testDoubleAbs(double d) {
        int errors  = testDoubleAbs("Math.abs", Math::abs, d);
        errors     += testDoubleAbs("Math.abs", Math::abs, -d);
        errors     += testDoubleAbs("StrictMath.abs", StrictMath::abs, d);
        errors     += testDoubleAbs("StrictMath.abs", StrictMath::abs, -d);
        return errors;
    }

    private static int testDoubleAbs(String testName,
                                     DoubleUnaryOperator absFunc, double d) {
        double result = absFunc.applyAsDouble(-d);
        if (Double.isNaN(d) && Double.isNaN(result)) {
            return 0;
        }

        double expected = d == -0.0F ? 0.0F : (d < 0.0F ? -d : d);
        return Tests.test(testName, d, result, expected);
    }
}
