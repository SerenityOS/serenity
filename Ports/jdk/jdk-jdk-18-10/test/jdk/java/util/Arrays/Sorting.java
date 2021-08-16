/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @compile/module=java.base java/util/SortingHelper.java
 * @bug 6880672 6896573 6899694 6976036 7013585 7018258 8003981 8226297
 * @build Sorting
 * @run main Sorting -shortrun
 * @summary Exercise Arrays.sort, Arrays.parallelSort
 *
 * @author Vladimir Yaroslavskiy
 * @author Jon Bentley
 * @author Josh Bloch
 */

import java.io.PrintStream;
import java.util.Comparator;
import java.util.Random;
import java.util.SortingHelper;

public class Sorting {

    private static final PrintStream out = System.out;
    private static final PrintStream err = System.err;

    // Array lengths used in a long run (default)
    private static final int[] LONG_RUN_LENGTHS = {
        1, 3, 8, 21, 55, 100, 1_000, 10_000, 100_000 };

    // Array lengths used in a short run
    private static final int[] SHORT_RUN_LENGTHS = {
        1, 8, 55, 100, 10_000 };

    // Random initial values used in a long run (default)
    private static final TestRandom[] LONG_RUN_RANDOMS = {
        TestRandom.BABA, TestRandom.DEDA, TestRandom.C0FFEE };

    // Random initial values used in a short run
    private static final TestRandom[] SHORT_RUN_RANDOMS = {
        TestRandom.C0FFEE };

    // Constants used in subarray sorting
    private static final int A380 = 0xA380;
    private static final int B747 = 0xB747;

    private final SortingHelper sortingHelper;
    private final TestRandom[] randoms;
    private final int[] lengths;
    private Object[] gold;
    private Object[] test;

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        boolean shortRun = args.length > 0 && args[0].equals("-shortrun");

        int[] lengths = shortRun ? SHORT_RUN_LENGTHS : LONG_RUN_LENGTHS;
        TestRandom[] randoms = shortRun ? SHORT_RUN_RANDOMS : LONG_RUN_RANDOMS;

        new Sorting(SortingHelper.DUAL_PIVOT_QUICKSORT, randoms, lengths).testCore();
        new Sorting(SortingHelper.PARALLEL_SORT, randoms, lengths).testCore();
        new Sorting(SortingHelper.HEAP_SORT, randoms, lengths).testBasic();
        new Sorting(SortingHelper.ARRAYS_SORT, randoms, lengths).testAll();
        new Sorting(SortingHelper.ARRAYS_PARALLEL_SORT, randoms, lengths).testAll();

        long end = System.currentTimeMillis();
        out.format("PASSED in %d sec.\n", (end - start) / 1000);
    }

    private Sorting(SortingHelper sortingHelper, TestRandom[] randoms, int[] lengths) {
        this.sortingHelper = sortingHelper;
        this.randoms = randoms;
        this.lengths = lengths;
    }

    private void testBasic() {
        testEmptyArray();

        for (int length : lengths) {
            createData(length);
            testBasic(length);
        }
    }

    private void testBasic(int length) {
        for (TestRandom random : randoms) {
            testWithInsertionSort(length, random);
            testWithCheckSum(length, random);
            testWithScrambling(length, random);
        }
    }

    private void testCore() {
        for (int length : lengths) {
            createData(length);
            testCore(length);
        }
    }

    private void testCore(int length) {
        testBasic(length);

        for (TestRandom random : randoms) {
            testMergingSort(length, random);
            testSubArray(length, random);
            testNegativeZero(length, random);
            testFloatingPointSorting(length, random);
        }
    }

    private void testAll() {
        for (int length : lengths) {
            createData(length);
            testAll(length);
        }
    }

    private void testAll(int length) {
        testCore(length);

        for (TestRandom random : randoms) {
            testRange(length, random);
            testStability(length, random);
        }
    }

    private void testEmptyArray() {
        testEmptyAndNullIntArray();
        testEmptyAndNullLongArray();
        testEmptyAndNullByteArray();
        testEmptyAndNullCharArray();
        testEmptyAndNullShortArray();
        testEmptyAndNullFloatArray();
        testEmptyAndNullDoubleArray();
    }

    private void testStability(int length, TestRandom random) {
        printTestName("Test stability", random, length);

        Pair[] a = build(length, random);
        sortingHelper.sort(a);
        checkSorted(a);
        checkStable(a);

        a = build(length, random);
        sortingHelper.sort(a, pairComparator);
        checkSorted(a);
        checkStable(a);

        out.println();
    }

    private void testEmptyAndNullIntArray() {
        sortingHelper.sort(new int[] {});
        sortingHelper.sort(new int[] {}, 0, 0);

        try {
            sortingHelper.sort(null);
        } catch (NullPointerException expected) {
            try {
                sortingHelper.sort(null, 0, 0);
            } catch (NullPointerException expected2) {
                return;
            }
            fail(sortingHelper + "(int[],fromIndex,toIndex) shouldn't " +
                "catch null array");
        }
        fail(sortingHelper + "(int[]) shouldn't catch null array");
    }

    private void testEmptyAndNullLongArray() {
        sortingHelper.sort(new long[] {});
        sortingHelper.sort(new long[] {}, 0, 0);

        try {
            sortingHelper.sort(null);
        } catch (NullPointerException expected) {
            try {
                sortingHelper.sort(null, 0, 0);
            } catch (NullPointerException expected2) {
                return;
            }
            fail(sortingHelper + "(long[],fromIndex,toIndex) shouldn't " +
                "catch null array");
        }
        fail(sortingHelper + "(long[]) shouldn't catch null array");
    }

    private void testEmptyAndNullByteArray() {
        sortingHelper.sort(new byte[] {});
        sortingHelper.sort(new byte[] {}, 0, 0);

        try {
            sortingHelper.sort(null);
        } catch (NullPointerException expected) {
            try {
                sortingHelper.sort(null, 0, 0);
            } catch (NullPointerException expected2) {
                return;
            }
            fail(sortingHelper + "(byte[],fromIndex,toIndex) shouldn't " +
                "catch null array");
        }
        fail(sortingHelper + "(byte[]) shouldn't catch null array");
    }

    private void testEmptyAndNullCharArray() {
        sortingHelper.sort(new char[] {});
        sortingHelper.sort(new char[] {}, 0, 0);

        try {
            sortingHelper.sort(null);
        } catch (NullPointerException expected) {
            try {
                sortingHelper.sort(null, 0, 0);
            } catch (NullPointerException expected2) {
                return;
            }
            fail(sortingHelper + "(char[],fromIndex,toIndex) shouldn't " +
                "catch null array");
        }
        fail(sortingHelper + "(char[]) shouldn't catch null array");
    }

    private void testEmptyAndNullShortArray() {
        sortingHelper.sort(new short[] {});
        sortingHelper.sort(new short[] {}, 0, 0);

        try {
            sortingHelper.sort(null);
        } catch (NullPointerException expected) {
            try {
                sortingHelper.sort(null, 0, 0);
            } catch (NullPointerException expected2) {
                return;
            }
            fail(sortingHelper + "(short[],fromIndex,toIndex) shouldn't " +
                "catch null array");
        }
        fail(sortingHelper + "(short[]) shouldn't catch null array");
    }

    private void testEmptyAndNullFloatArray() {
        sortingHelper.sort(new float[] {});
        sortingHelper.sort(new float[] {}, 0, 0);

        try {
            sortingHelper.sort(null);
        } catch (NullPointerException expected) {
            try {
                sortingHelper.sort(null, 0, 0);
            } catch (NullPointerException expected2) {
                return;
            }
            fail(sortingHelper + "(float[],fromIndex,toIndex) shouldn't " +
                "catch null array");
        }
        fail(sortingHelper + "(float[]) shouldn't catch null array");
    }

    private void testEmptyAndNullDoubleArray() {
        sortingHelper.sort(new double[] {});
        sortingHelper.sort(new double[] {}, 0, 0);

        try {
            sortingHelper.sort(null);
        } catch (NullPointerException expected) {
            try {
                sortingHelper.sort(null, 0, 0);
            } catch (NullPointerException expected2) {
                return;
            }
            fail(sortingHelper + "(double[],fromIndex,toIndex) shouldn't " +
                "catch null array");
        }
        fail(sortingHelper + "(double[]) shouldn't catch null array");
    }

    private void testSubArray(int length, TestRandom random) {
        if (length < 4) {
            return;
        }
        for (int m = 1; m < length / 2; m <<= 1) {
            int fromIndex = m;
            int toIndex = length - m;

            prepareSubArray((int[]) gold[0], fromIndex, toIndex);
            convertData(length);

            for (int i = 0; i < test.length; i++) {
                printTestName("Test subarray", random, length,
                    ", m = " + m + ", " + getType(i));
                sortingHelper.sort(test[i], fromIndex, toIndex);
                checkSubArray(test[i], fromIndex, toIndex);
            }
        }
        out.println();
    }

    private void testRange(int length, TestRandom random) {
        if (length < 2) {
            return;
        }
        for (int m = 1; m < length; m <<= 1) {
            for (int i = 1; i <= length; i++) {
                ((int[]) gold[0]) [i - 1] = i % m + m % i;
            }
            convertData(length);

            for (int i = 0; i < test.length; i++) {
                printTestName("Test range check", random, length,
                    ", m = " + m + ", " + getType(i));
                checkRange(test[i], m);
            }
        }
        out.println();
    }

    private void checkSorted(Pair[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i].getKey() > a[i + 1].getKey()) {
                fail("Array is not sorted at " + i + "-th position: " +
                    a[i].getKey() + " and " + a[i + 1].getKey());
            }
        }
    }

    private void checkStable(Pair[] a) {
        for (int i = 0; i < a.length / 4; ) {
            int key1 = a[i].getKey();
            int value1 = a[i++].getValue();
            int key2 = a[i].getKey();
            int value2 = a[i++].getValue();
            int key3 = a[i].getKey();
            int value3 = a[i++].getValue();
            int key4 = a[i].getKey();
            int value4 = a[i++].getValue();

            if (!(key1 == key2 && key2 == key3 && key3 == key4)) {
                fail("Keys are different " + key1 + ", " + key2 + ", " +
                    key3 + ", " + key4 + " at position " + i);
            }
            if (!(value1 < value2 && value2 < value3 && value3 < value4)) {
                fail("Sorting is not stable at position " + i +
                    ". Second values have been changed: " + value1 + ", " +
                    value2 + ", " + value3 + ", " + value4);
            }
        }
    }

    private Pair[] build(int length, Random random) {
        Pair[] a = new Pair[length * 4];

        for (int i = 0; i < a.length; ) {
            int key = random.nextInt();
            a[i++] = new Pair(key, 1);
            a[i++] = new Pair(key, 2);
            a[i++] = new Pair(key, 3);
            a[i++] = new Pair(key, 4);
        }
        return a;
    }

    private void testWithInsertionSort(int length, TestRandom random) {
        if (length > 1000) {
            return;
        }
        for (int m = 1; m <= length; m <<= 1) {
            for (UnsortedBuilder builder : UnsortedBuilder.values()) {
                builder.build((int[]) gold[0], m, random);
                convertData(length);

                for (int i = 0; i < test.length; i++) {
                    printTestName("Test with insertion sort", random, length,
                        ", m = " + m + ", " + getType(i) + " " + builder);
                    sortingHelper.sort(test[i]);
                    sortByInsertionSort(gold[i]);
                    compare(test[i], gold[i]);
                }
            }
        }
        out.println();
    }

    private void testMergingSort(int length, TestRandom random) {
        if (length < (4 << 10)) { // DualPivotQuicksort.MIN_TRY_MERGE_SIZE
            return;
        }
        final int PERIOD = 50;

        for (int m = PERIOD - 2; m <= PERIOD + 2; m++) {
            for (MergingBuilder builder : MergingBuilder.values()) {
                builder.build((int[]) gold[0], m);
                convertData(length);

                for (int i = 0; i < test.length; i++) {
                    printTestName("Test merging sort", random, length,
                        ", m = " + m + ", " +  getType(i) + " " + builder);
                    sortingHelper.sort(test[i]);
                    checkSorted(test[i]);
                }
            }
        }
        out.println();
    }

    private void testWithCheckSum(int length, TestRandom random) {
        for (int m = 1; m <= length; m <<= 1) {
            for (UnsortedBuilder builder : UnsortedBuilder.values()) {
                builder.build((int[]) gold[0], m, random);
                convertData(length);

                for (int i = 0; i < test.length; i++) {
                    printTestName("Test with check sum", random, length,
                        ", m = " + m + ", " + getType(i) + " " + builder);
                    sortingHelper.sort(test[i]);
                    checkWithCheckSum(test[i], gold[i]);
                }
            }
        }
        out.println();
    }

    private void testWithScrambling(int length, TestRandom random) {
        for (int m = 1; m <= length; m <<= 1) {
            for (SortedBuilder builder : SortedBuilder.values()) {
                builder.build((int[]) gold[0], m);
                convertData(length);

                for (int i = 0; i < test.length; i++) {
                    printTestName("Test with scrambling", random, length,
                        ", m = " + m + ", " + getType(i) + " " + builder);
                    scramble(test[i], random);
                    sortingHelper.sort(test[i]);
                    compare(test[i], gold[i]);
                }
            }
        }
        out.println();
    }

    private void testNegativeZero(int length, TestRandom random) {
        for (int i = 5; i < test.length; i++) {
            printTestName("Test negative zero -0.0", random, length, " " + getType(i));

            NegativeZeroBuilder builder = NegativeZeroBuilder.values() [i - 5];
            builder.build(test[i], random);

            sortingHelper.sort(test[i]);
            checkNegativeZero(test[i]);
        }
        out.println();
    }

    private void testFloatingPointSorting(int length, TestRandom random) {
        if (length < 2) {
            return;
        }
        final int MAX = 13;

        for (int a = 0; a < MAX; a++) {
            for (int g = 0; g < MAX; g++) {
                for (int z = 0; z < MAX; z++) {
                    for (int n = 0; n < MAX; n++) {
                        for (int p = 0; p < MAX; p++) {
                            if (a + g + z + n + p != length) {
                                continue;
                            }
                            for (int i = 5; i < test.length; i++) {
                                printTestName("Test float-pointing sorting", random, length,
                                    ", a = " + a + ", g = " + g + ", z = " + z +
                                    ", n = " + n + ", p = " + p + ", " + getType(i));
                                FloatingPointBuilder builder = FloatingPointBuilder.values()[i - 5];
                                builder.build(gold[i], a, g, z, n, p, random);
                                copy(test[i], gold[i]);
                                scramble(test[i], random);
                                sortingHelper.sort(test[i]);
                                compare(test[i], gold[i], a, n, g);
                            }
                        }
                    }
                }
            }
        }

        for (int m = 13; m > 4; m--) {
            int t = length / m;
            int g = t, z = t, n = t, p = t;
            int a = length - g - z - n - p;

            for (int i = 5; i < test.length; i++) {
                printTestName("Test float-pointing sorting", random, length,
                    ", a = " + a + ", g = " + g + ", z = " + z +
                    ", n = " + n + ", p = " + p + ", " + getType(i));
                FloatingPointBuilder builder = FloatingPointBuilder.values() [i - 5];
                builder.build(gold[i], a, g, z, n, p, random);
                copy(test[i], gold[i]);
                scramble(test[i], random);
                sortingHelper.sort(test[i]);
                compare(test[i], gold[i], a, n, g);
            }
        }
        out.println();
    }

    private void prepareSubArray(int[] a, int fromIndex, int toIndex) {
        for (int i = 0; i < fromIndex; i++) {
            a[i] = A380;
        }
        int middle = (fromIndex + toIndex) >>> 1;
        int k = 0;

        for (int i = fromIndex; i < middle; i++) {
            a[i] = k++;
        }

        for (int i = middle; i < toIndex; i++) {
            a[i] = k--;
        }

        for (int i = toIndex; i < a.length; i++) {
            a[i] = B747;
        }
    }

    private void scramble(Object a, Random random) {
        if (a instanceof int[]) {
            scramble((int[]) a, random);
        } else if (a instanceof long[]) {
            scramble((long[]) a, random);
        } else if (a instanceof byte[]) {
            scramble((byte[]) a, random);
        } else if (a instanceof char[]) {
            scramble((char[]) a, random);
        } else if (a instanceof short[]) {
            scramble((short[]) a, random);
        } else if (a instanceof float[]) {
            scramble((float[]) a, random);
        } else if (a instanceof double[]) {
            scramble((double[]) a, random);
        } else {
            fail("Unknown type of array: " + a.getClass().getName());
        }
    }

    private void scramble(int[] a, Random random) {
        for (int i = 0; i < a.length * 7; i++) {
            swap(a, random.nextInt(a.length), random.nextInt(a.length));
        }
    }

    private void scramble(long[] a, Random random) {
        for (int i = 0; i < a.length * 7; i++) {
            swap(a, random.nextInt(a.length), random.nextInt(a.length));
        }
    }

    private void scramble(byte[] a, Random random) {
        for (int i = 0; i < a.length * 7; i++) {
            swap(a, random.nextInt(a.length), random.nextInt(a.length));
        }
    }

    private void scramble(char[] a, Random random) {
        for (int i = 0; i < a.length * 7; i++) {
            swap(a, random.nextInt(a.length), random.nextInt(a.length));
        }
    }

    private void scramble(short[] a, Random random) {
        for (int i = 0; i < a.length * 7; i++) {
            swap(a, random.nextInt(a.length), random.nextInt(a.length));
        }
    }

    private void scramble(float[] a, Random random) {
        for (int i = 0; i < a.length * 7; i++) {
            swap(a, random.nextInt(a.length), random.nextInt(a.length));
        }
    }

    private void scramble(double[] a, Random random) {
        for (int i = 0; i < a.length * 7; i++) {
            swap(a, random.nextInt(a.length), random.nextInt(a.length));
        }
    }

    private void swap(int[] a, int i, int j) {
        int t = a[i]; a[i] = a[j]; a[j] = t;
    }

    private void swap(long[] a, int i, int j) {
        long t = a[i]; a[i] = a[j]; a[j] = t;
    }

    private void swap(byte[] a, int i, int j) {
        byte t = a[i]; a[i] = a[j]; a[j] = t;
    }

    private void swap(char[] a, int i, int j) {
        char t = a[i]; a[i] = a[j]; a[j] = t;
    }

    private void swap(short[] a, int i, int j) {
        short t = a[i]; a[i] = a[j]; a[j] = t;
    }

    private void swap(float[] a, int i, int j) {
        float t = a[i]; a[i] = a[j]; a[j] = t;
    }

    private void swap(double[] a, int i, int j) {
        double t = a[i]; a[i] = a[j]; a[j] = t;
    }

    private void checkWithCheckSum(Object test, Object gold) {
        checkSorted(test);
        checkCheckSum(test, gold);
    }

    private void fail(String message) {
        err.format("\n*** TEST FAILED ***\n\n%s\n\n", message);
        throw new RuntimeException("Test failed");
    }

    private void checkNegativeZero(Object a) {
        if (a instanceof float[]) {
            checkNegativeZero((float[]) a);
        } else if (a instanceof double[]) {
            checkNegativeZero((double[]) a);
        } else {
            fail("Unknown type of array: " + a.getClass().getName());
        }
    }

    private void checkNegativeZero(float[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            if (Float.floatToRawIntBits(a[i]) == 0 && Float.floatToRawIntBits(a[i + 1]) < 0) {
                fail(a[i] + " before " + a[i + 1] + " at position " + i);
            }
        }
    }

    private void checkNegativeZero(double[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            if (Double.doubleToRawLongBits(a[i]) == 0 && Double.doubleToRawLongBits(a[i + 1]) < 0) {
                fail(a[i] + " before " + a[i + 1] + " at position " + i);
            }
        }
    }

    private void compare(Object a, Object b, int numNaN, int numNeg, int numNegZero) {
        if (a instanceof float[]) {
            compare((float[]) a, (float[]) b, numNaN, numNeg, numNegZero);
        } else if (a instanceof double[]) {
            compare((double[]) a, (double[]) b, numNaN, numNeg, numNegZero);
        } else {
            fail("Unknown type of array: " + a.getClass().getName());
        }
    }

    private void compare(float[] a, float[] b, int numNaN, int numNeg, int numNegZero) {
        for (int i = a.length - numNaN; i < a.length; i++) {
            if (a[i] == a[i]) {
                fail("There must be NaN instead of " + a[i] + " at position " + i);
            }
        }
        final int NEGATIVE_ZERO = Float.floatToIntBits(-0.0f);

        for (int i = numNeg; i < numNeg + numNegZero; i++) {
            if (NEGATIVE_ZERO != Float.floatToIntBits(a[i])) {
                fail("There must be -0.0 instead of " + a[i] + " at position " + i);
            }
        }

        for (int i = 0; i < a.length - numNaN; i++) {
            if (a[i] != b[i]) {
                fail("There must be " + b[i] + " instead of " + a[i] + " at position " + i);
            }
        }
    }

    private void compare(double[] a, double[] b, int numNaN, int numNeg, int numNegZero) {
        for (int i = a.length - numNaN; i < a.length; i++) {
            if (a[i] == a[i]) {
                fail("There must be NaN instead of " + a[i] + " at position " + i);
            }
        }
        final long NEGATIVE_ZERO = Double.doubleToLongBits(-0.0d);

        for (int i = numNeg; i < numNeg + numNegZero; i++) {
            if (NEGATIVE_ZERO != Double.doubleToLongBits(a[i])) {
                fail("There must be -0.0 instead of " + a[i] + " at position " + i);
            }
        }

        for (int i = 0; i < a.length - numNaN; i++) {
            if (a[i] != b[i]) {
                fail("There must be " + b[i] + " instead of " + a[i] + " at position " + i);
            }
        }
    }

    private void compare(Object a, Object b) {
        if (a instanceof int[]) {
            compare((int[]) a, (int[]) b);
        } else if (a instanceof long[]) {
            compare((long[]) a, (long[]) b);
        } else if (a instanceof byte[]) {
            compare((byte[]) a, (byte[]) b);
        } else if (a instanceof char[]) {
            compare((char[]) a, (char[]) b);
        } else if (a instanceof short[]) {
            compare((short[]) a, (short[]) b);
        } else if (a instanceof float[]) {
            compare((float[]) a, (float[]) b);
        } else if (a instanceof double[]) {
            compare((double[]) a, (double[]) b);
        } else {
            fail("Unknown type of array: " + a.getClass().getName());
        }
    }

    private void compare(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                fail("There must be " + b[i] + " instead of " + a[i] + " at position " + i);
            }
        }
    }

    private void compare(long[] a, long[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                fail("There must be " + b[i] + " instead of " + a[i] + " at position " + i);
            }
        }
    }

    private void compare(byte[] a, byte[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                fail("There must be " + b[i] + " instead of " + a[i] + " at position " + i);
            }
        }
    }

    private void compare(char[] a, char[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                fail("There must be " + b[i] + " instead of " + a[i] + " at position " + i);
            }
        }
    }

    private void compare(short[] a, short[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                fail("There must be " + b[i] + " instead of " + a[i] + " at position " + i);
            }
        }
    }

    private void compare(float[] a, float[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                fail("There must be " + b[i] + " instead of " + a[i] + " at position " + i);
            }
        }
    }

    private void compare(double[] a, double[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                fail("There must be " + b[i] + " instead of " + a[i] + " at position " + i);
            }
        }
    }

    private String getType(int i) {
        Object a = test[i];

        if (a instanceof int[]) {
            return "INT   ";
        }
        if (a instanceof long[]) {
            return "LONG  ";
        }
        if (a instanceof byte[]) {
            return "BYTE  ";
        }
        if (a instanceof char[]) {
            return "CHAR  ";
        }
        if (a instanceof short[]) {
            return "SHORT ";
        }
        if (a instanceof float[]) {
            return "FLOAT ";
        }
        if (a instanceof double[]) {
            return "DOUBLE";
        }
        fail("Unknown type of array: " + a.getClass().getName());
        return null;
    }

    private void checkSorted(Object a) {
        if (a instanceof int[]) {
            checkSorted((int[]) a);
        } else if (a instanceof long[]) {
            checkSorted((long[]) a);
        } else if (a instanceof byte[]) {
            checkSorted((byte[]) a);
        } else if (a instanceof char[]) {
            checkSorted((char[]) a);
        } else if (a instanceof short[]) {
            checkSorted((short[]) a);
        } else if (a instanceof float[]) {
            checkSorted((float[]) a);
        } else if (a instanceof double[]) {
            checkSorted((double[]) a);
        } else {
            fail("Unknown type of array: " + a.getClass().getName());
        }
    }

    private void checkSorted(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }
    }

    private void checkSorted(long[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }
    }

    private void checkSorted(byte[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }
    }

    private void checkSorted(char[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }
    }

    private void checkSorted(short[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }
    }

    private void checkSorted(float[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }
    }

    private void checkSorted(double[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }
    }

    private void checkCheckSum(Object test, Object gold) {
        if (checkSumXor(test) != checkSumXor(gold)) {
            fail("Original and sorted arrays are not identical [^]");
        }
        if (checkSumPlus(test) != checkSumPlus(gold)) {
            fail("Original and sorted arrays are not identical [+]");
        }
    }

    private int checkSumXor(Object a) {
        if (a instanceof int[]) {
            return checkSumXor((int[]) a);
        }
        if (a instanceof long[]) {
            return checkSumXor((long[]) a);
        }
        if (a instanceof byte[]) {
            return checkSumXor((byte[]) a);
        }
        if (a instanceof char[]) {
            return checkSumXor((char[]) a);
        }
        if (a instanceof short[]) {
            return checkSumXor((short[]) a);
        }
        if (a instanceof float[]) {
            return checkSumXor((float[]) a);
        }
        if (a instanceof double[]) {
            return checkSumXor((double[]) a);
        }
        fail("Unknown type of array: " + a.getClass().getName());
        return -1;
    }

    private int checkSumXor(int[] a) {
        int checkSum = 0;

        for (int e : a) {
            checkSum ^= e;
        }
        return checkSum;
    }

    private int checkSumXor(long[] a) {
        long checkSum = 0;

        for (long e : a) {
            checkSum ^= e;
        }
        return (int) checkSum;
    }

    private int checkSumXor(byte[] a) {
        byte checkSum = 0;

        for (byte e : a) {
            checkSum ^= e;
        }
        return (int) checkSum;
    }

    private int checkSumXor(char[] a) {
        char checkSum = 0;

        for (char e : a) {
            checkSum ^= e;
        }
        return (int) checkSum;
    }

    private int checkSumXor(short[] a) {
        short checkSum = 0;

        for (short e : a) {
            checkSum ^= e;
        }
        return (int) checkSum;
    }

    private int checkSumXor(float[] a) {
        int checkSum = 0;

        for (float e : a) {
            checkSum ^= (int) e;
        }
        return checkSum;
    }

    private int checkSumXor(double[] a) {
        int checkSum = 0;

        for (double e : a) {
            checkSum ^= (int) e;
        }
        return checkSum;
    }

    private int checkSumPlus(Object a) {
        if (a instanceof int[]) {
            return checkSumPlus((int[]) a);
        }
        if (a instanceof long[]) {
            return checkSumPlus((long[]) a);
        }
        if (a instanceof byte[]) {
            return checkSumPlus((byte[]) a);
        }
        if (a instanceof char[]) {
            return checkSumPlus((char[]) a);
        }
        if (a instanceof short[]) {
            return checkSumPlus((short[]) a);
        }
        if (a instanceof float[]) {
            return checkSumPlus((float[]) a);
        }
        if (a instanceof double[]) {
            return checkSumPlus((double[]) a);
        }
        fail("Unknown type of array: " + a.getClass().getName());
        return -1;
    }

    private int checkSumPlus(int[] a) {
        int checkSum = 0;

        for (int e : a) {
            checkSum += e;
        }
        return checkSum;
    }

    private int checkSumPlus(long[] a) {
        long checkSum = 0;

        for (long e : a) {
            checkSum += e;
        }
        return (int) checkSum;
    }

    private int checkSumPlus(byte[] a) {
        byte checkSum = 0;

        for (byte e : a) {
            checkSum += e;
        }
        return (int) checkSum;
    }

    private int checkSumPlus(char[] a) {
        char checkSum = 0;

        for (char e : a) {
            checkSum += e;
        }
        return (int) checkSum;
    }

    private int checkSumPlus(short[] a) {
        short checkSum = 0;

        for (short e : a) {
            checkSum += e;
        }
        return (int) checkSum;
    }

    private int checkSumPlus(float[] a) {
        int checkSum = 0;

        for (float e : a) {
            checkSum += (int) e;
        }
        return checkSum;
    }

    private int checkSumPlus(double[] a) {
        int checkSum = 0;

        for (double e : a) {
            checkSum += (int) e;
        }
        return checkSum;
    }

    private void sortByInsertionSort(Object a) {
        if (a instanceof int[]) {
            sortByInsertionSort((int[]) a);
        } else if (a instanceof long[]) {
            sortByInsertionSort((long[]) a);
        } else if (a instanceof byte[]) {
            sortByInsertionSort((byte[]) a);
        } else if (a instanceof char[]) {
            sortByInsertionSort((char[]) a);
        } else if (a instanceof short[]) {
            sortByInsertionSort((short[]) a);
        } else if (a instanceof float[]) {
            sortByInsertionSort((float[]) a);
        } else if (a instanceof double[]) {
            sortByInsertionSort((double[]) a);
        } else {
            fail("Unknown type of array: " + a.getClass().getName());
        }
    }

    private void sortByInsertionSort(int[] a) {
        for (int j, i = 1; i < a.length; i++) {
            int ai = a[i];

            for (j = i - 1; j >= 0 && ai < a[j]; j--) {
                a[j + 1] = a[j];
            }
            a[j + 1] = ai;
        }
    }

    private void sortByInsertionSort(long[] a) {
        for (int j, i = 1; i < a.length; i++) {
            long ai = a[i];

            for (j = i - 1; j >= 0 && ai < a[j]; j--) {
                a[j + 1] = a[j];
            }
            a[j + 1] = ai;
        }
    }

    private void sortByInsertionSort(byte[] a) {
        for (int j, i = 1; i < a.length; i++) {
            byte ai = a[i];

            for (j = i - 1; j >= 0 && ai < a[j]; j--) {
                a[j + 1] = a[j];
            }
            a[j + 1] = ai;
        }
    }

    private void sortByInsertionSort(char[] a) {
        for (int j, i = 1; i < a.length; i++) {
            char ai = a[i];

            for (j = i - 1; j >= 0 && ai < a[j]; j--) {
                a[j + 1] = a[j];
            }
            a[j + 1] = ai;
        }
    }

    private void sortByInsertionSort(short[] a) {
        for (int j, i = 1; i < a.length; i++) {
            short ai = a[i];

            for (j = i - 1; j >= 0 && ai < a[j]; j--) {
                a[j + 1] = a[j];
            }
            a[j + 1] = ai;
        }
    }

    private void sortByInsertionSort(float[] a) {
        for (int j, i = 1; i < a.length; i++) {
            float ai = a[i];

            for (j = i - 1; j >= 0 && ai < a[j]; j--) {
                a[j + 1] = a[j];
            }
            a[j + 1] = ai;
        }
    }

    private void sortByInsertionSort(double[] a) {
        for (int j, i = 1; i < a.length; i++) {
            double ai = a[i];

            for (j = i - 1; j >= 0 && ai < a[j]; j--) {
                a[j + 1] = a[j];
            }
            a[j + 1] = ai;
        }
    }

    private void checkSubArray(Object a, int fromIndex, int toIndex) {
        if (a instanceof int[]) {
            checkSubArray((int[]) a, fromIndex, toIndex);
        } else if (a instanceof long[]) {
            checkSubArray((long[]) a, fromIndex, toIndex);
        } else if (a instanceof byte[]) {
            checkSubArray((byte[]) a, fromIndex, toIndex);
        } else if (a instanceof char[]) {
            checkSubArray((char[]) a, fromIndex, toIndex);
        } else if (a instanceof short[]) {
            checkSubArray((short[]) a, fromIndex, toIndex);
        } else if (a instanceof float[]) {
            checkSubArray((float[]) a, fromIndex, toIndex);
        } else if (a instanceof double[]) {
            checkSubArray((double[]) a, fromIndex, toIndex);
        } else {
            fail("Unknown type of array: " + a.getClass().getName());
        }
    }

    private void checkSubArray(int[] a, int fromIndex, int toIndex) {
        for (int i = 0; i < fromIndex; i++) {
            if (a[i] != A380) {
                fail("Range sort changes left element at position " + i + hex(a[i], A380));
            }
        }

        for (int i = fromIndex; i < toIndex - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }

        for (int i = toIndex; i < a.length; i++) {
            if (a[i] != B747) {
                fail("Range sort changes right element at position " + i + hex(a[i], B747));
            }
        }
    }

    private void checkSubArray(long[] a, int fromIndex, int toIndex) {
        for (int i = 0; i < fromIndex; i++) {
            if (a[i] != (long) A380) {
                fail("Range sort changes left element at position " + i + hex(a[i], A380));
            }
        }

        for (int i = fromIndex; i < toIndex - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }

        for (int i = toIndex; i < a.length; i++) {
            if (a[i] != (long) B747) {
                fail("Range sort changes right element at position " + i + hex(a[i], B747));
            }
        }
    }

    private void checkSubArray(byte[] a, int fromIndex, int toIndex) {
        for (int i = 0; i < fromIndex; i++) {
            if (a[i] != (byte) A380) {
                fail("Range sort changes left element at position " + i + hex(a[i], A380));
            }
        }

        for (int i = fromIndex; i < toIndex - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }

        for (int i = toIndex; i < a.length; i++) {
            if (a[i] != (byte) B747) {
                fail("Range sort changes right element at position " + i + hex(a[i], B747));
            }
        }
    }

    private void checkSubArray(char[] a, int fromIndex, int toIndex) {
        for (int i = 0; i < fromIndex; i++) {
            if (a[i] != (char) A380) {
                fail("Range sort changes left element at position " + i + hex(a[i], A380));
            }
        }

        for (int i = fromIndex; i < toIndex - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }

        for (int i = toIndex; i < a.length; i++) {
            if (a[i] != (char) B747) {
                fail("Range sort changes right element at position " + i + hex(a[i], B747));
            }
        }
    }

    private void checkSubArray(short[] a, int fromIndex, int toIndex) {
        for (int i = 0; i < fromIndex; i++) {
            if (a[i] != (short) A380) {
                fail("Range sort changes left element at position " + i + hex(a[i], A380));
            }
        }

        for (int i = fromIndex; i < toIndex - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }

        for (int i = toIndex; i < a.length; i++) {
            if (a[i] != (short) B747) {
                fail("Range sort changes right element at position " + i + hex(a[i], B747));
            }
        }
    }

    private void checkSubArray(float[] a, int fromIndex, int toIndex) {
        for (int i = 0; i < fromIndex; i++) {
            if (a[i] != (float) A380) {
                fail("Range sort changes left element at position " + i + hex((long) a[i], A380));
            }
        }

        for (int i = fromIndex; i < toIndex - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }

        for (int i = toIndex; i < a.length; i++) {
            if (a[i] != (float) B747) {
                fail("Range sort changes right element at position " + i + hex((long) a[i], B747));
            }
        }
    }

    private void checkSubArray(double[] a, int fromIndex, int toIndex) {
        for (int i = 0; i < fromIndex; i++) {
            if (a[i] != (double) A380) {
                fail("Range sort changes left element at position " + i + hex((long) a[i], A380));
            }
        }

        for (int i = fromIndex; i < toIndex - 1; i++) {
            if (a[i] > a[i + 1]) {
                fail("Array is not sorted at " + i + "-th position: " + a[i] + " and " + a[i + 1]);
            }
        }

        for (int i = toIndex; i < a.length; i++) {
            if (a[i] != (double) B747) {
                fail("Range sort changes right element at position " + i + hex((long) a[i], B747));
            }
        }
    }

    private void checkRange(Object a, int m) {
        if (a instanceof int[]) {
            checkRange((int[]) a, m);
        } else if (a instanceof long[]) {
            checkRange((long[]) a, m);
        } else if (a instanceof byte[]) {
            checkRange((byte[]) a, m);
        } else if (a instanceof char[]) {
            checkRange((char[]) a, m);
        } else if (a instanceof short[]) {
            checkRange((short[]) a, m);
        } else if (a instanceof float[]) {
            checkRange((float[]) a, m);
        } else if (a instanceof double[]) {
            checkRange((double[]) a, m);
        } else {
            fail("Unknown type of array: " + a.getClass().getName());
        }
    }

    private void checkRange(int[] a, int m) {
        try {
            sortingHelper.sort(a, m + 1, m);
            fail(sortingHelper + " does not throw IllegalArgumentException " +
                "as expected: fromIndex = " + (m + 1) + " toIndex = " + m);
        } catch (IllegalArgumentException iae) {
            try {
                sortingHelper.sort(a, -m, a.length);
                fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                    "as expected: fromIndex = " + (-m));
            } catch (ArrayIndexOutOfBoundsException aoe) {
                try {
                    sortingHelper.sort(a, 0, a.length + m);
                    fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                        "as expected: toIndex = " + (a.length + m));
                } catch (ArrayIndexOutOfBoundsException expected) {}
            }
        }
    }

    private void checkRange(long[] a, int m) {
        try {
            sortingHelper.sort(a, m + 1, m);
            fail(sortingHelper + " does not throw IllegalArgumentException " +
                "as expected: fromIndex = " + (m + 1) + " toIndex = " + m);
        } catch (IllegalArgumentException iae) {
            try {
                sortingHelper.sort(a, -m, a.length);
                fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                    "as expected: fromIndex = " + (-m));
            } catch (ArrayIndexOutOfBoundsException aoe) {
                try {
                    sortingHelper.sort(a, 0, a.length + m);
                    fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                        "as expected: toIndex = " + (a.length + m));
                } catch (ArrayIndexOutOfBoundsException expected) {}
            }
        }
    }

    private void checkRange(byte[] a, int m) {
        try {
            sortingHelper.sort(a, m + 1, m);
            fail(sortingHelper + " does not throw IllegalArgumentException " +
                "as expected: fromIndex = " + (m + 1) + " toIndex = " + m);
        } catch (IllegalArgumentException iae) {
            try {
                sortingHelper.sort(a, -m, a.length);
                fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                    "as expected: fromIndex = " + (-m));
            } catch (ArrayIndexOutOfBoundsException aoe) {
                try {
                    sortingHelper.sort(a, 0, a.length + m);
                    fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                        "as expected: toIndex = " + (a.length + m));
                } catch (ArrayIndexOutOfBoundsException expected) {}
            }
        }
    }

    private void checkRange(char[] a, int m) {
        try {
            sortingHelper.sort(a, m + 1, m);
            fail(sortingHelper + " does not throw IllegalArgumentException " +
                "as expected: fromIndex = " + (m + 1) + " toIndex = " + m);
        } catch (IllegalArgumentException iae) {
            try {
                sortingHelper.sort(a, -m, a.length);
                fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                    "as expected: fromIndex = " + (-m));
            } catch (ArrayIndexOutOfBoundsException aoe) {
                try {
                    sortingHelper.sort(a, 0, a.length + m);
                    fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                        "as expected: toIndex = " + (a.length + m));
                } catch (ArrayIndexOutOfBoundsException expected) {}
            }
        }
    }

    private void checkRange(short[] a, int m) {
        try {
            sortingHelper.sort(a, m + 1, m);
            fail(sortingHelper + " does not throw IllegalArgumentException " +
                "as expected: fromIndex = " + (m + 1) + " toIndex = " + m);
        } catch (IllegalArgumentException iae) {
            try {
                sortingHelper.sort(a, -m, a.length);
                fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                    "as expected: fromIndex = " + (-m));
            } catch (ArrayIndexOutOfBoundsException aoe) {
                try {
                    sortingHelper.sort(a, 0, a.length + m);
                    fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                        "as expected: toIndex = " + (a.length + m));
                } catch (ArrayIndexOutOfBoundsException expected) {}
            }
        }
    }

    private void checkRange(float[] a, int m) {
        try {
            sortingHelper.sort(a, m + 1, m);
            fail(sortingHelper + " does not throw IllegalArgumentException " +
                "as expected: fromIndex = " + (m + 1) + " toIndex = " + m);
        } catch (IllegalArgumentException iae) {
            try {
                sortingHelper.sort(a, -m, a.length);
                fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                    "as expected: fromIndex = " + (-m));
            } catch (ArrayIndexOutOfBoundsException aoe) {
                try {
                    sortingHelper.sort(a, 0, a.length + m);
                    fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                        "as expected: toIndex = " + (a.length + m));
                } catch (ArrayIndexOutOfBoundsException expected) {}
            }
        }
    }

    private void checkRange(double[] a, int m) {
        try {
            sortingHelper.sort(a, m + 1, m);
            fail(sortingHelper + " does not throw IllegalArgumentException " +
                "as expected: fromIndex = " + (m + 1) + " toIndex = " + m);
        } catch (IllegalArgumentException iae) {
            try {
                sortingHelper.sort(a, -m, a.length);
                fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                    "as expected: fromIndex = " + (-m));
            } catch (ArrayIndexOutOfBoundsException aoe) {
                try {
                    sortingHelper.sort(a, 0, a.length + m);
                    fail(sortingHelper + " does not throw ArrayIndexOutOfBoundsException " +
                        "as expected: toIndex = " + (a.length + m));
                } catch (ArrayIndexOutOfBoundsException expected) {}
            }
        }
    }

    private void copy(Object dst, Object src) {
        if (src instanceof float[]) {
            copy((float[]) dst, (float[]) src);
        } else if (src instanceof double[]) {
            copy((double[]) dst, (double[]) src);
        } else {
            fail("Unknown type of array: " + src.getClass().getName());
        }
    }

    private void copy(float[] dst, float[] src) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    private void copy(double[] dst, double[] src) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    private void printTestName(String test, TestRandom random, int length) {
        printTestName(test, random, length, "");
    }

    private void createData(int length) {
        gold = new Object[] {
            new int[length], new long[length],
            new byte[length], new char[length], new short[length],
            new float[length], new double[length]
        };

        test = new Object[] {
            new int[length], new long[length],
            new byte[length], new char[length], new short[length],
            new float[length], new double[length]
        };
    }

    private void convertData(int length) {
        for (int i = 1; i < gold.length; i++) {
            TypeConverter converter = TypeConverter.values()[i - 1];
            converter.convert((int[])gold[0], gold[i]);
        }

        for (int i = 0; i < gold.length; i++) {
            System.arraycopy(gold[i], 0, test[i], 0, length);
        }
    }

    private String hex(long a, int b) {
        return ": " + Long.toHexString(a) + ", must be " + Integer.toHexString(b);
    }

    private void printTestName(String test, TestRandom random, int length, String message) {
        out.println( "[" + sortingHelper + "] '" + test +
            "' length = " + length + ", random = " + random + message);
    }

    private static enum TypeConverter {
        LONG {
            void convert(int[] src, Object dst) {
                long[] b = (long[]) dst;

                for (int i = 0; i < src.length; i++) {
                    b[i] = (long) src[i];
                }
            }
        },

        BYTE {
            void convert(int[] src, Object dst) {
                byte[] b = (byte[]) dst;

                for (int i = 0; i < src.length; i++) {
                    b[i] = (byte) src[i];
                }
            }
        },

        CHAR {
            void convert(int[] src, Object dst) {
                char[] b = (char[]) dst;

                for (int i = 0; i < src.length; i++) {
                    b[i] = (char) src[i];
                }
            }
        },

        SHORT {
            void convert(int[] src, Object dst) {
                short[] b = (short[]) dst;

                for (int i = 0; i < src.length; i++) {
                    b[i] = (short) src[i];
                }
            }
        },

        FLOAT {
            void convert(int[] src, Object dst) {
                float[] b = (float[]) dst;

                for (int i = 0; i < src.length; i++) {
                    b[i] = (float) src[i];
                }
            }
        },

        DOUBLE {
            void convert(int[] src, Object dst) {
                double[] b = (double[]) dst;

                for (int i = 0; i < src.length; i++) {
                    b[i] = (double) src[i];
                }
            }
        };

        abstract void convert(int[] src, Object dst);
    }

    private static enum SortedBuilder {
        STEPS {
            void build(int[] a, int m) {
                for (int i = 0; i < m; i++) {
                    a[i] = 0;
                }

                for (int i = m; i < a.length; i++) {
                    a[i] = 1;
                }
            }
        };

        abstract void build(int[] a, int m);
    }

    private static enum UnsortedBuilder {
        RANDOM {
            void build(int[] a, int m, Random random) {
                for (int i = 0; i < a.length; i++) {
                    a[i] = random.nextInt();
                }
            }
        },

        ASCENDING {
            void build(int[] a, int m, Random random) {
                for (int i = 0; i < a.length; i++) {
                    a[i] = m + i;
                }
            }
        },

        DESCENDING {
            void build(int[] a, int m, Random random) {
                for (int i = 0; i < a.length; i++) {
                    a[i] = a.length - m - i;
                }
            }
        },

        EQUAL {
            void build(int[] a, int m, Random random) {
                for (int i = 0; i < a.length; i++) {
                    a[i] = m;
                }
            }
        },

        SAW {
            void build(int[] a, int m, Random random) {
                int incCount = 1;
                int decCount = a.length;
                int i = 0;
                int period = m--;

                while (true) {
                    for (int k = 1; k <= period; k++) {
                        if (i >= a.length) {
                            return;
                        }
                        a[i++] = incCount++;
                    }
                    period += m;

                    for (int k = 1; k <= period; k++) {
                        if (i >= a.length) {
                            return;
                        }
                        a[i++] = decCount--;
                    }
                    period += m;
                }
            }
        },

        REPEATED {
            void build(int[] a, int m, Random random) {
                for (int i = 0; i < a.length; i++) {
                    a[i] = i % m;
                }
            }
        },

        DUPLICATED {
            void build(int[] a, int m, Random random) {
                for (int i = 0; i < a.length; i++) {
                    a[i] = random.nextInt(m);
                }
            }
        },

        ORGAN_PIPES {
            void build(int[] a, int m, Random random) {
                int middle = a.length / (m + 1);

                for (int i = 0; i < middle; i++) {
                    a[i] = i;
                }

                for (int i = middle; i < a.length; i++) {
                    a[i] = a.length - i - 1;
                }
            }
        },

        STAGGER {
            void build(int[] a, int m, Random random) {
                for (int i = 0; i < a.length; i++) {
                    a[i] = (i * m + i) % a.length;
                }
            }
        },

        PLATEAU {
            void build(int[] a, int m, Random random) {
                for (int i = 0; i < a.length; i++) {
                    a[i] = Math.min(i, m);
                }
            }
        },

        SHUFFLE {
            void build(int[] a, int m, Random random) {
                int x = 0, y = 0;

                for (int i = 0; i < a.length; i++) {
                    a[i] = random.nextBoolean() ? (x += 2) : (y += 2);
                }
            }
        },

        LATCH {
            void build(int[] a, int m, Random random) {
                int max = a.length / m;
                max = max < 2 ? 2 : max;

                for (int i = 0; i < a.length; i++) {
                    a[i] = i % max;
                }
            }
        };

        abstract void build(int[] a, int m, Random random);
    }

    private static enum MergingBuilder {
        ASCENDING {
            void build(int[] a, int m) {
                int period = a.length / m;
                int v = 1, i = 0;

                for (int k = 0; k < m; k++) {
                    v = 1;

                    for (int p = 0; p < period; p++) {
                        a[i++] = v++;
                    }
                }

                for (int j = i; j < a.length - 1; j++) {
                    a[j] = v++;
                }

                a[a.length - 1] = 0;
            }
        },

        DESCENDING {
            void build(int[] a, int m) {
                int period = a.length / m;
                int v = -1, i = 0;

                for (int k = 0; k < m; k++) {
                    v = -1;

                    for (int p = 0; p < period; p++) {
                        a[i++] = v--;
                    }
                }

                for (int j = i; j < a.length - 1; j++) {
                    a[j] = v--;
                }

                a[a.length - 1] = 0;
            }
        },

        POINT {
            void build(int[] a, int m) {
                for (int i = 0; i < a.length; i++) {
                    a[i] = 0;
                }
                a[a.length / 2] = m;
            }
        },

        LINE {
            void build(int[] a, int m) {
                for (int i = 0; i < a.length; i++) {
                    a[i] = i;
                }
                reverse(a, 0, a.length - 1);
            }
        },

        PEARL {
            void build(int[] a, int m) {
                for (int i = 0; i < a.length; i++) {
                    a[i] = i;
                }
                reverse(a, 0, 2);
            }
        },

        RING {
            void build(int[] a, int m) {
                int k1 = a.length / 3;
                int k2 = a.length / 3 * 2;
                int level = a.length / 3;

                for (int i = 0, k = level; i < k1; i++) {
                    a[i] = k--;
                }

                for (int i = k1; i < k2; i++) {
                    a[i] = 0;
                }

                for (int i = k2, k = level; i < a.length; i++) {
                    a[i] = k--;
                }
            }
        };

        abstract void build(int[] a, int m);

        private static void reverse(int[] a, int lo, int hi) {
            for (--hi; lo < hi; ) {
                int tmp = a[lo];
                a[lo++] = a[hi];
                a[hi--] = tmp;
            }
        }
    }

    private static enum NegativeZeroBuilder {
        FLOAT {
            void build(Object o, Random random) {
                float[] a = (float[]) o;

                for (int i = 0; i < a.length; i++) {
                    a[i] = random.nextBoolean() ? -0.0f : 0.0f;
                }
            }
        },

        DOUBLE {
            void build(Object o, Random random) {
                double[] a = (double[]) o;

                for (int i = 0; i < a.length; i++) {
                    a[i] = random.nextBoolean() ? -0.0d : 0.0d;
                }
            }
        };

        abstract void build(Object o, Random random);
    }

    private static enum FloatingPointBuilder {
        FLOAT {
            void build(Object o, int a, int g, int z, int n, int p, Random random) {
                float negativeValue = -random.nextFloat();
                float positiveValue =  random.nextFloat();
                float[] x = (float[]) o;
                int fromIndex = 0;

                writeValue(x, negativeValue, fromIndex, n);
                fromIndex += n;

                writeValue(x, -0.0f, fromIndex, g);
                fromIndex += g;

                writeValue(x, 0.0f, fromIndex, z);
                fromIndex += z;

                writeValue(x, positiveValue, fromIndex, p);
                fromIndex += p;

                writeValue(x, Float.NaN, fromIndex, a);
            }
        },

        DOUBLE {
            void build(Object o, int a, int g, int z, int n, int p, Random random) {
                double negativeValue = -random.nextFloat();
                double positiveValue =  random.nextFloat();
                double[] x = (double[]) o;
                int fromIndex = 0;

                writeValue(x, negativeValue, fromIndex, n);
                fromIndex += n;

                writeValue(x, -0.0d, fromIndex, g);
                fromIndex += g;

                writeValue(x, 0.0d, fromIndex, z);
                fromIndex += z;

                writeValue(x, positiveValue, fromIndex, p);
                fromIndex += p;

                writeValue(x, Double.NaN, fromIndex, a);
            }
        };

        abstract void build(Object o, int a, int g, int z, int n, int p, Random random);

        private static void writeValue(float[] a, float value, int fromIndex, int count) {
            for (int i = fromIndex; i < fromIndex + count; i++) {
                a[i] = value;
            }
        }

        private static void writeValue(double[] a, double value, int fromIndex, int count) {
            for (int i = fromIndex; i < fromIndex + count; i++) {
                a[i] = value;
            }
        }
    }

    private static Comparator<Pair> pairComparator = new Comparator<Pair>() {

        @Override
        public int compare(Pair p1, Pair p2) {
            return p1.compareTo(p2);
        }
    };

    private static class Pair implements Comparable<Pair> {

        private Pair(int key, int value) {
            this.key = key;
            this.value = value;
        }

        int getKey() {
            return key;
        }

        int getValue() {
            return value;
        }

        @Override
        public int compareTo(Pair pair) {
            return Integer.compare(key, pair.key);
        }

        @Override
        public String toString() {
            return "(" + key + ", " + value + ")";
        }

        private int key;
        private int value;
    }

    private static class TestRandom extends Random {

        private static final TestRandom BABA = new TestRandom(0xBABA);
        private static final TestRandom DEDA = new TestRandom(0xDEDA);
        private static final TestRandom C0FFEE = new TestRandom(0xC0FFEE);

        private TestRandom(long seed) {
            super(seed);
            this.seed = Long.toHexString(seed).toUpperCase();
        }

        @Override
        public String toString() {
            return seed;
        }

        private String seed;
    }
}
