/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static java.math.BigInteger.ONE;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @test
 * @bug 7131192
 * @summary This test ensures that BigInteger.floatValue() and
 *          BigInteger.doubleValue() behave correctly.
 * @author Louis Wasserman
 */
public class PrimitiveConversionTests {
    static final List<BigInteger> ALL_BIGINTEGER_CANDIDATES;

    static {
        List<BigInteger> samples = new ArrayList<>();
        // Now add values near 2^N for lots of values of N.
        for (int exponent : Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 31, 32, 33,
                34, 62, 63, 64, 65, 71, 72, 73, 79, 80, 81, 255, 256, 257, 511,
                512, 513, Double.MAX_EXPONENT - 1, Double.MAX_EXPONENT,
                Double.MAX_EXPONENT + 1, 2000, 2001, 2002)) {
            BigInteger x = ONE.shiftLeft(exponent);
            for (BigInteger y : Arrays.asList(x, x.add(ONE), x.subtract(ONE))) {
                samples.add(y);
                samples.add(y.negate());
            }
        }

        Random rng = new Random(1234567);
        for (int i = 0; i < 2000; i++) {
            samples.add(new BigInteger(rng.nextInt(2000), rng));
        }

        ALL_BIGINTEGER_CANDIDATES = Collections.unmodifiableList(samples);
    }

    public static int testDoubleValue() {
        System.out.println("--- testDoubleValue ---");
        int failures = 0;
        for (BigInteger big : ALL_BIGINTEGER_CANDIDATES) {
            double expected = Double.parseDouble(big.toString());
            double actual = big.doubleValue();

            // should be bitwise identical
            if (Double.doubleToRawLongBits(expected) != Double
                    .doubleToRawLongBits(actual)) {
                System.out.format("big: %s, expected: %f, actual: %f%n",
                    big, expected, actual);
                failures++;
            }
        }
        return failures;
    }

    public static int testFloatValue() {
        System.out.println("--- testFloatValue ---");
        int failures = 0;
        for (BigInteger big : ALL_BIGINTEGER_CANDIDATES) {
            float expected = Float.parseFloat(big.toString());
            float actual = big.floatValue();

            // should be bitwise identical
            if (Float.floatToRawIntBits(expected) != Float
                    .floatToRawIntBits(actual)) {
                System.out.format("big: %s, expected: %f, actual: %f%n",
                    big, expected, actual);
                failures++;
            }
        }
        return failures;
    }

    public static void main(String[] args) {
        int failures = testDoubleValue();
        failures += testFloatValue();
        if (failures > 0) {
            throw new RuntimeException("Incurred " + failures
                    + " failures while testing primitive conversions.");
        }
    }
}
