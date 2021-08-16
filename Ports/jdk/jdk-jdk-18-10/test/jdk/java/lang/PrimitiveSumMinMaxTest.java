/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import java.util.Comparator;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;

/**
 * @test
 * @run testng PrimitiveSumMinMaxTest
 * @summary test conversion of primitive wrapper sum, min, max, and compareTo methods to functional interfaces
 *
 * @author Brian Goetz
 */
@Test
public class PrimitiveSumMinMaxTest {

    public void testBooleanMethods() {
        BinaryOperator<Boolean> and = Boolean::logicalAnd;
        BinaryOperator<Boolean> or = Boolean::logicalOr;
        BinaryOperator<Boolean> xor = Boolean::logicalXor;
        Comparator<Boolean> cmp = Boolean::compare;

        assertTrue(and.apply(true, true));
        assertFalse(and.apply(true, false));
        assertFalse(and.apply(false, true));
        assertFalse(and.apply(false, false));

        assertTrue(or.apply(true, true));
        assertTrue(or.apply(true, false));
        assertTrue(or.apply(false, true));
        assertFalse(or.apply(false, false));

        assertFalse(xor.apply(true, true));
        assertTrue(xor.apply(true, false));
        assertTrue(xor.apply(false, true));
        assertFalse(xor.apply(false, false));

        assertEquals(Boolean.TRUE.compareTo(Boolean.TRUE), cmp.compare(true, true));
        assertEquals(Boolean.TRUE.compareTo(Boolean.FALSE), cmp.compare(true, false));
        assertEquals(Boolean.FALSE.compareTo(Boolean.TRUE), cmp.compare(false, true));
        assertEquals(Boolean.FALSE.compareTo(Boolean.FALSE), cmp.compare(false, false));
    };

    public void testIntMethods() {
        BinaryOperator<Integer> sum1 = Integer::sum;
        IntBinaryOperator sum2 = Integer::sum;
        BinaryOperator<Integer> max1 = Integer::max;
        IntBinaryOperator max2 = Integer::max;
        BinaryOperator<Integer> min1 = Integer::min;
        IntBinaryOperator min2 = Integer::min;
        Comparator<Integer> cmp = Integer::compare;

        int[] numbers = { -1, 0, 1, 100, Integer.MAX_VALUE, Integer.MIN_VALUE };
        for (int i : numbers) {
            for (int j : numbers) {
                assertEquals(i+j, (int) sum1.apply(i, j));
                assertEquals(i+j, sum2.applyAsInt(i, j));
                assertEquals(Math.max(i,j), (int) max1.apply(i, j));
                assertEquals(Math.max(i,j), max2.applyAsInt(i, j));
                assertEquals(Math.min(i,j), (int) min1.apply(i, j));
                assertEquals(Math.min(i,j), min2.applyAsInt(i, j));
                assertEquals(((Integer) i).compareTo(j), cmp.compare(i, j));
            }
        }
    }

    public void testLongMethods() {
        BinaryOperator<Long> sum1 = Long::sum;
        LongBinaryOperator sum2 = Long::sum;
        BinaryOperator<Long> max1 = Long::max;
        LongBinaryOperator max2 = Long::max;
        BinaryOperator<Long> min1 = Long::min;
        LongBinaryOperator min2 = Long::min;
        Comparator<Long> cmp = Long::compare;

        long[] numbers = { -1, 0, 1, 100, Long.MAX_VALUE, Long.MIN_VALUE };
        for (long i : numbers) {
            for (long j : numbers) {
                assertEquals(i+j, (long) sum1.apply(i, j));
                assertEquals(i+j, sum2.applyAsLong(i, j));
                assertEquals(Math.max(i,j), (long) max1.apply(i, j));
                assertEquals(Math.max(i,j), max2.applyAsLong(i, j));
                assertEquals(Math.min(i,j), (long) min1.apply(i, j));
                assertEquals(Math.min(i,j), min2.applyAsLong(i, j));
                assertEquals(((Long) i).compareTo(j), cmp.compare(i, j));
            }
        }
    }

    public void testFloatMethods() {
        BinaryOperator<Float> sum1 = Float::sum;
        BinaryOperator<Float> max1 = Float::max;
        BinaryOperator<Float> min1 = Float::min;
        Comparator<Float> cmp = Float::compare;

        float[] numbers = { -1, 0, 1, 100, Float.MAX_VALUE, Float.MIN_VALUE };
        for (float i : numbers) {
            for (float j : numbers) {
                assertEquals(i+j, (float) sum1.apply(i, j));
                assertEquals(Math.max(i,j), (float) max1.apply(i, j));
                assertEquals(Math.min(i,j), (float) min1.apply(i, j));
                assertEquals(((Float) i).compareTo(j), cmp.compare(i, j));
            }
        }
    }

    public void testDoubleMethods() {
        BinaryOperator<Double> sum1 = Double::sum;
        DoubleBinaryOperator sum2 = Double::sum;
        BinaryOperator<Double> max1 = Double::max;
        DoubleBinaryOperator max2 = Double::max;
        BinaryOperator<Double> min1 = Double::min;
        DoubleBinaryOperator min2 = Double::min;
        Comparator<Double> cmp = Double::compare;

        double[] numbers = { -1, 0, 1, 100, Double.MAX_VALUE, Double.MIN_VALUE };
        for (double i : numbers) {
            for (double j : numbers) {
                assertEquals(i+j, (double) sum1.apply(i, j));
                assertEquals(i+j, sum2.applyAsDouble(i, j));
                assertEquals(Math.max(i,j), (double) max1.apply(i, j));
                assertEquals(Math.max(i,j), max2.applyAsDouble(i, j));
                assertEquals(Math.min(i,j), (double) min1.apply(i, j));
                assertEquals(Math.min(i,j), min2.applyAsDouble(i, j));
                assertEquals(((Double) i).compareTo(j), cmp.compare(i, j));
            }
        }
    }

}
