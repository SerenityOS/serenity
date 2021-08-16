/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, BELLSOFT. All rights reserved.
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
 * @summary Test compiler intrinsics for signum
 * @library /test/lib
 *
 * @run main/othervm
 *      -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *      -XX:+UseSignumIntrinsic
 *      compiler.intrinsics.math.TestSignumIntrinsic
 * @run main/othervm
 *      -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *      -XX:-UseSignumIntrinsic -XX:+UseCopySignIntrinsic
 *      compiler.intrinsics.math.TestSignumIntrinsic
 */

package compiler.intrinsics.math;

import jdk.test.lib.Asserts;

public class TestSignumIntrinsic {

    private static final float[][] float_cases = {
        {123.4f,                       1.0f},
        {-56.7f,                      -1.0f},
        {7e30f,                        1.0f},
        {-0.3e30f,                    -1.0f},
        {Float.MAX_VALUE,              1.0f},
        {-Float.MAX_VALUE,            -1.0f},
        {Float.MIN_VALUE,              1.0f},
        {-Float.MIN_VALUE,            -1.0f},
        {0.0f,                         0.0f},
        {-0.0f,                       -0.0f},
        {Float.POSITIVE_INFINITY,      1.0f},
        {Float.NEGATIVE_INFINITY,     -1.0f},
        {Float.NaN,               Float.NaN},
        {Float.MIN_NORMAL,             1.0f},
        {-Float.MIN_NORMAL,           -1.0f},
        {0x0.0002P-126f,               1.0f},
        {-0x0.0002P-126f,             -1.0f}
    };

    private static final double[][] double_cases = {
        {123.4d,                         1.0d},
        {-56.7d,                        -1.0d},
        {7e30d,                          1.0d},
        {-0.3e30d,                      -1.0d},
        {Double.MAX_VALUE,               1.0d},
        {-Double.MAX_VALUE,             -1.0d},
        {Double.MIN_VALUE,               1.0d},
        {-Double.MIN_VALUE,             -1.0d},
        {0.0d,                           0.0d},
        {-0.0d,                         -0.0d},
        {Double.POSITIVE_INFINITY,       1.0d},
        {Double.NEGATIVE_INFINITY,      -1.0d},
        {Double.NaN,               Double.NaN},
        {Double.MIN_NORMAL,              1.0d},
        {-Double.MIN_NORMAL,            -1.0d},
        {0x0.00000001P-1022,             1.0d},
        {-0x0.00000001P-1022,           -1.0d}
    };

    public static void main(String[] args) throws Exception {
        float fAccum = 0.0f;
        double dAccum = 0.0d;
        for (int i = 0; i < 100_000; i++) {
            fAccum += floatTest();
            dAccum += doubleTest();
        }
        System.out.println("SUCCESS. Accum values: " + fAccum + " and " + dAccum);
    }

    private static float floatTest() {
        float accum = 0.0f;
        for (float[] fcase : float_cases) {
            float arg = fcase[0];
            float expected = fcase[1];
            float calculated = Math.signum(arg);
            Asserts.assertEQ(expected, calculated, "Unexpected float result from " + arg);
            accum += calculated;
        }
        return accum;
    }

    private static double doubleTest() {
        double accum = 0.0d;
        for (double[] dcase : double_cases) {
            double arg = dcase[0];
            double expected = dcase[1];
            double calculated = Math.signum(arg);
            Asserts.assertEQ(expected, calculated, "Unexpected double result from " + arg);
            accum += calculated;
        }
        return accum;
    }
}
