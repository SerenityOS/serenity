/*
 * Copyright 2015 Goldman Sachs.
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8154049
 * @summary Tests the sorting of a large array of sorted primitive values,
 *          predominently for cases where the array is nearly sorted. This tests
 *          code that detects patterns in the array to determine if it is nearly
 *          sorted and if so employs and optimizes merge sort rather than a
 *          Dual-Pivot QuickSort.
 *
 * @run testng SortingNearlySortedPrimitive
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SortingNearlySortedPrimitive {

    static final int BASE = 3;
    static final int WIDTH = 4;
    // Should be > DualPivotQuicksort.QUICKSORT_THRESHOLD
    static final int PAD = 300;

    Stream<int[]> createCombinations() {
        // Create all combinations for the BASE value and double the WIDTH
        // elements
        // This is create various combinations of ascending, descending and
        // equal runs to exercise the nearly sorted code paths
        return IntStream.range(0, (int) Math.pow(BASE, 2 * WIDTH)).
                mapToObj(this::createArray);
    }

    // Create an array which at either end is filled with -ve and +ve elements
    // according to the base value and padded with zeros in between
    int[] createArray(int v) {
        int[] a = new int[WIDTH + PAD + WIDTH];

        // Fill head of array
        for (int j = 0; j < WIDTH; j++) {
            a[j] = (v % BASE) - (BASE / 2);
            v /= BASE;
        }
        // Fill tail of array
        for (int j = 0; j < WIDTH; j++) {
            a[WIDTH + PAD + j] = (v % BASE) - (BASE / 2);
            v /= BASE;
        }
        return a;
    }

    @Test
    public void testCombination() {
        createCombinations().forEach(a -> {
            try {
                // Clone source array to ensure it is not modified
                this.sortAndAssert(a.clone());
                this.sortAndAssert(floatCopyFromInt(a));
                this.sortAndAssert(doubleCopyFromInt(a));
                this.sortAndAssert(longCopyFromInt(a));
                this.sortAndAssert(shortCopyFromInt(a));
                this.sortAndAssert(charCopyFromInt(a));
            } catch (AssertionError sae) {
                AssertionError ae = new AssertionError("Sort failed for " + arrayToString(a));
                ae.addSuppressed(sae);
                throw ae;
            }
        });
    }

    String arrayToString(int[] a) {
        int[] l = Arrays.copyOfRange(a, 0, WIDTH + 2);
        int[] r = Arrays.copyOfRange(a, a.length - (WIDTH + 2), a.length);
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (int i : l) {
            sj.add(Integer.toString(i));
        }
        sj.add("...");
        for (int i : r) {
            sj.add(Integer.toString(i));
        }
        return sj.toString();
    }


    @DataProvider(name = "shapes")
    public Object[][] createShapes() {
        Stream<List<Object>> baseCases = Stream.of(
                List.of("hiZeroLowTest", (IntFunction<int[]>) this::hiZeroLowData),
                List.of("endLessThanTest", (IntFunction<int[]>) this::endLessThanData),
                List.of("highFlatLowTest", (IntFunction<int[]>) this::highFlatLowData),
                List.of("identicalTest", (IntFunction<int[]>) this::identicalData),
                List.of("sortedReversedSortedTest", (IntFunction<int[]>) this::sortedReversedSortedData),
                List.of("pairFlipTest", (IntFunction<int[]>) this::pairFlipData),
                List.of("zeroHiTest", (IntFunction<int[]>) this::zeroHiData)
        );

        // Ensure the following inequality holds for certain sizes
        // DualPivotQuicksort.QUICKSORT_THRESHOLD <= size - 1
        //   < DualPivotQuicksort.COUNTING_SORT_THRESHOLD_FOR_SHORT_OR_CHAR
        // This guarantees that code paths are taken for checking nearly sorted
        // arrays for all primitive types
        List<Integer> sizes = List.of(100, 1_000, 10_000, 1_000_000);
        return baseCases.
                flatMap(l -> sizes.stream().map(s -> append(l, s))).
                toArray(Object[][]::new);
    }

    Object[] append(List<Object> l, Object value) {
        List<Object> nl = new ArrayList<>(l);
        nl.add(value);
        return nl.toArray();
    }

    @Test(dataProvider = "shapes")
    public void testShapes(String testName, IntFunction<int[]> dataMethod, int size) {
        int[] intSourceArray = dataMethod.apply(size);

        // Clone source array to ensure it is not modified
        this.sortAndAssert(intSourceArray.clone());
        this.sortAndAssert(floatCopyFromInt(intSourceArray));
        this.sortAndAssert(doubleCopyFromInt(intSourceArray));
        this.sortAndAssert(longCopyFromInt(intSourceArray));
        this.sortAndAssert(shortCopyFromInt(intSourceArray));
        this.sortAndAssert(charCopyFromInt(intSourceArray));
    }

    private float[] floatCopyFromInt(int[] src) {
        float[] result = new float[src.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = src[i];
        }
        return result;
    }

    private double[] doubleCopyFromInt(int[] src) {
        double[] result = new double[src.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = src[i];
        }
        return result;
    }

    private long[] longCopyFromInt(int[] src) {
        long[] result = new long[src.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = src[i];
        }
        return result;
    }

    private short[] shortCopyFromInt(int[] src) {
        short[] result = new short[src.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (short) src[i];
        }
        return result;
    }

    private char[] charCopyFromInt(int[] src) {
        char[] result = new char[src.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (char) src[i];
        }
        return result;
    }

    private void sortAndAssert(int[] array) {
        Arrays.sort(array);
        for (int i = 1; i < array.length; i++) {
            if (array[i] < array[i - 1]) {
                throw new AssertionError("not sorted");
            }
        }
    }

    private void sortAndAssert(char[] array) {
        Arrays.sort(array);
        for (int i = 1; i < array.length; i++) {
            if (array[i] < array[i - 1]) {
                throw new AssertionError("not sorted");
            }
        }
    }

    private void sortAndAssert(short[] array) {
        Arrays.sort(array);
        for (int i = 1; i < array.length; i++) {
            if (array[i] < array[i - 1]) {
                throw new AssertionError("not sorted");
            }
        }
    }

    private void sortAndAssert(double[] array) {
        Arrays.sort(array);
        for (int i = 1; i < array.length; i++) {
            if (array[i] < array[i - 1]) {
                throw new AssertionError("not sorted");
            }
        }
    }

    private void sortAndAssert(float[] array) {
        Arrays.sort(array);
        for (int i = 1; i < array.length; i++) {
            if (array[i] < array[i - 1]) {
                throw new AssertionError("not sorted");
            }
        }
    }

    private void sortAndAssert(long[] array) {
        Arrays.sort(array);
        for (int i = 1; i < array.length; i++) {
            if (array[i] < array[i - 1]) {
                throw new AssertionError("not sorted");
            }
        }
    }

    private int[] zeroHiData(int size) {
        int[] array = new int[size];

        int threeQuarters = (int) (size * 0.75);
        for (int i = 0; i < threeQuarters; i++) {
            array[i] = 0;
        }
        int k = 1;
        for (int i = threeQuarters; i < size; i++) {
            array[i] = k;
            k++;
        }

        return array;
    }

    private int[] hiZeroLowData(int size) {
        int[] array = new int[size];

        int oneThird = size / 3;
        for (int i = 0; i < oneThird; i++) {
            array[i] = i;
        }
        int twoThirds = oneThird * 2;
        for (int i = oneThird; i < twoThirds; i++) {
            array[i] = 0;
        }
        for (int i = twoThirds; i < size; i++) {
            array[i] = oneThird - i + twoThirds;
        }
        return array;
    }

    private int[] highFlatLowData(int size) {
        int[] array = new int[size];

        int oneThird = size / 3;
        for (int i = 0; i < oneThird; i++) {
            array[i] = i;
        }
        int twoThirds = oneThird * 2;
        int constant = oneThird - 1;
        for (int i = oneThird; i < twoThirds; i++) {
            array[i] = constant;
        }
        for (int i = twoThirds; i < size; i++) {
            array[i] = constant - i + twoThirds;
        }

        return array;
    }

    private int[] identicalData(int size) {
        int[] array = new int[size];
        int listNumber = 24;

        for (int i = 0; i < size; i++) {
            array[i] = listNumber;
        }

        return array;
    }

    private int[] endLessThanData(int size) {
        int[] array = new int[size];

        for (int i = 0; i < size - 1; i++) {
            array[i] = 3;
        }
        array[size - 1] = 1;

        return array;
    }

    private int[] sortedReversedSortedData(int size) {
        int[] array = new int[size];

        for (int i = 0; i < size / 2; i++) {
            array[i] = i;
        }
        int num = 0;
        for (int i = size / 2; i < size; i++) {
            array[i] = size - num;
            num++;
        }

        return array;
    }

    private int[] pairFlipData(int size) {
        int[] array = new int[size];

        for (int i = 0; i < size; i++) {
            array[i] = i;
        }
        for (int i = 0; i < size; i += 2) {
            int temp = array[i];
            array[i] = array[i + 1];
            array[i + 1] = temp;
        }

        return array;
    }
}
