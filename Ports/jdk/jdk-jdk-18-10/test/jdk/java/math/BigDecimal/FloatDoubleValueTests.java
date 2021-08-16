/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6274390 7082971
 * @summary Verify {float, double}Value methods work with condensed representation
 * @run main FloatDoubleValueTests
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+EliminateAutoBox -XX:AutoBoxCacheMax=20000 FloatDoubleValueTests
 */
import java.math.*;

public class FloatDoubleValueTests {
    private static final long two2the24 = 1L<<23;
    private static final long two2the53 = 1L<<52;

    // Largest long that fits exactly in a float
    private static final long maxFltLong = (long)(Integer.MAX_VALUE & ~(0xff));

    // Largest long that fits exactly in a double
    private static final long maxDblLong = Long.MAX_VALUE & ~(0x7ffL);

    static void testDoubleValue0(long i, BigDecimal bd) {
        if (bd.doubleValue() != i ||
            bd.longValue()   != i)
            throw new RuntimeException("Unexpected equality failure for " +
                                       i + "\t" + bd);
    }

    static void testFloatValue0(long i, BigDecimal bd) {
        if (bd.floatValue() != i ||
            bd.longValue()   != i)
            throw new RuntimeException("Unexpected equality failure for " +
                                       i + "\t" + bd);
    }

    static void checkFloat(BigDecimal bd, float f) {
        float fbd = bd.floatValue();
        if (f != fbd ) {
            String message = String.format("Bad conversion:"+
                                           "got %g (%a)\texpected %g (%a)",
                                           f, f, fbd, fbd);
            throw new RuntimeException(message);
        }
    }

    static void checkDouble(BigDecimal bd, double d) {
        double dbd = bd.doubleValue();

        if (d != dbd ) {
            String message = String.format("Bad conversion:"+
                                           "got %g (%a)\texpected %g (%a)",
                                           d, d, dbd, dbd);
            throw new RuntimeException(message);
        }
    }

    // Test integral values that will convert exactly to both float
    // and double.
    static void testFloatDoubleValue() {
        long longValues[] = {
            Long.MIN_VALUE, // -2^63
            0,
            1,
            2,

            two2the24-1,
            two2the24,
            two2the24+1,

            maxFltLong-1,
            maxFltLong,
            maxFltLong+1,
        };

        for(long i : longValues) {
            BigDecimal bd1 = new BigDecimal(i);
            BigDecimal bd2 = new BigDecimal(-i);

            testDoubleValue0( i, bd1);
            testDoubleValue0(-i, bd2);

            testFloatValue0( i, bd1);
            testFloatValue0(-i, bd2);
        }

    }

    static void testDoubleValue() {
        long longValues[] = {
            Integer.MAX_VALUE-1,
            Integer.MAX_VALUE,
            (long)Integer.MAX_VALUE+1,

            two2the53-1,
            two2the53,
            two2the53+1,

            maxDblLong,
        };

        // Test integral values that will convert exactly to double
        // but not float.
        for(long i : longValues) {
            BigDecimal bd1 = new BigDecimal(i);
            BigDecimal bd2 = new BigDecimal(-i);

            testDoubleValue0( i, bd1);
            testDoubleValue0(-i, bd2);

            checkFloat(bd1, (float)i);
            checkFloat(bd2, -(float)i);
        }

        // Now check values that should not convert the same in double
        for(long i = maxDblLong; i < Long.MAX_VALUE; i++) {
            BigDecimal bd1 = new BigDecimal(i);
            BigDecimal bd2 = new BigDecimal(-i);
            checkDouble(bd1, (double)i);
            checkDouble(bd2, -(double)i);

            checkFloat(bd1, (float)i);
            checkFloat(bd2, -(float)i);
        }

        checkDouble(new BigDecimal(Long.MIN_VALUE), (double)Long.MIN_VALUE);
        checkDouble(new BigDecimal(Long.MAX_VALUE), (double)Long.MAX_VALUE);
    }

    static void testFloatValue() {
        // Now check values that should not convert the same in float
        for(long i = maxFltLong; i <= Integer.MAX_VALUE; i++) {
            BigDecimal bd1 = new BigDecimal(i);
            BigDecimal bd2 = new BigDecimal(-i);
            checkFloat(bd1, (float)i);
            checkFloat(bd2, -(float)i);

            testDoubleValue0( i, bd1);
            testDoubleValue0(-i, bd2);
        }
    }

    static void testFloatValue1() {
        checkFloat(new BigDecimal("85070591730234615847396907784232501249"), 8.507059e+37f);
        checkFloat(new BigDecimal("7784232501249e12"), 7.7842326e24f);
        checkFloat(new BigDecimal("907784232501249e-12"),907.78424f);
        checkFloat(new BigDecimal("7784e8"),7.7839997e11f);
        checkFloat(new BigDecimal("9077e-8"),9.077e-5f);

    }

    static void testDoubleValue1() {
        checkDouble(new BigDecimal("85070591730234615847396907784232501249"), 8.507059173023462e37);
        checkDouble(new BigDecimal("7784232501249e12"), 7.784232501249e24);
        checkDouble(new BigDecimal("907784232501249e-12"), 907.784232501249);
        checkDouble(new BigDecimal("7784e8"), 7.784e11);
        checkDouble(new BigDecimal("9077e-8"), 9.077e-5);

    }

    public static void main(String[] args) throws Exception {
        testFloatDoubleValue();
        testDoubleValue();
        testFloatValue();
        testFloatValue1();
        testDoubleValue1();
    }
}
