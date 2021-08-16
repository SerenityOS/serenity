/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8014076 8025067
 * @summary unit test for Arrays.ParallelPrefix().
 * @author Tristan Yan
 * @modules java.management jdk.management
 * @run testng/othervm -Xms256m -Xmx1024m ParallelPrefix
 */

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import com.sun.management.OperatingSystemMXBean;
import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeSuite;

public class ParallelPrefix {
    //Array size less than MIN_PARTITION
    private static final int SMALL_ARRAY_SIZE = 1 << 3;

    //Array size equals MIN_PARTITION
    private static final int THRESHOLD_ARRAY_SIZE = 1 << 4;

    //Array size greater than MIN_PARTITION
    private static final int MEDIUM_ARRAY_SIZE = 1 << 8;

    //Array size much greater than MIN_PARTITION
    private static final int LARGE_ARRAY_SIZE = 1 << 14;

    private static int[] arraySizeCollection;

    @BeforeSuite
    public static void setup() {
        java.lang.management.OperatingSystemMXBean bean =
                ManagementFactory.getOperatingSystemMXBean();
        if (bean instanceof OperatingSystemMXBean) {
            OperatingSystemMXBean os = (OperatingSystemMXBean)bean;
            long physicalMemorySize = os.getTotalPhysicalMemorySize() / (1024 * 1024);
            System.out.println("System memory size: " + physicalMemorySize + "M");
            // when we can get system memory size, and it's larger than 2G,
            // then we enable large array size test below,
            // else disable large array size test below.
            if (physicalMemorySize > (2 * 1024)) {
                arraySizeCollection  = new int[]{
                        SMALL_ARRAY_SIZE,
                        THRESHOLD_ARRAY_SIZE,
                        MEDIUM_ARRAY_SIZE,
                        LARGE_ARRAY_SIZE
                    };
                System.out.println("System memory is large enough, add large array size test");
                return;
            }
        }
        arraySizeCollection  = new int[]{
                SMALL_ARRAY_SIZE,
                THRESHOLD_ARRAY_SIZE,
                MEDIUM_ARRAY_SIZE
            };
        System.out.println("System memory is not large enough, remove large array size test");
    }

    @DataProvider(name = "intSet")
    public static Object[][] intSet(){
        return genericData(size -> IntStream.range(0, size).toArray(),
                new IntBinaryOperator[]{
                    Integer::sum,
                    Integer::min});
    }

    @DataProvider(name = "longSet")
    public static Object[][] longSet(){
        return genericData(size -> LongStream.range(0, size).toArray(),
                new LongBinaryOperator[]{
                    Long::sum,
                    Long::min});
    }

    @DataProvider(name = "doubleSet")
    public static Object[][] doubleSet(){
        return genericData(size -> IntStream.range(0, size).mapToDouble(i -> (double)i).toArray(),
                new DoubleBinaryOperator[]{
                    Double::sum,
                    Double::min});
    }

    @DataProvider(name = "stringSet")
    public static Object[][] stringSet(){
        Function<Integer, String[]> stringsFunc = size ->
                IntStream.range(0, size).mapToObj(Integer::toString).toArray(String[]::new);
        BinaryOperator<String> concat = String::concat;
        return genericData(stringsFunc,
                (BinaryOperator<String>[]) new BinaryOperator[]{
                    concat });
    }

    private static <T, OPS> Object[][] genericData(Function<Integer, T> generateFunc, OPS[] ops) {
        //test arrays which size is equals n-1, n, n+1, test random data
        Object[][] data = new Object[arraySizeCollection.length * 3 * ops.length][4];
        for(int n = 0; n < arraySizeCollection.length; n++ ) {
            for(int testValue = -1 ; testValue <= 1; testValue++) {
                int array_size = arraySizeCollection[n] + testValue;
                for(int opsN = 0; opsN < ops.length; opsN++) {
                    int index = n * 3 * ops.length + (testValue + 1) * ops.length + opsN;
                    data[index][0] = generateFunc.apply(array_size);
                    data[index][1] = array_size / 3;
                    data[index][2] = 2 * array_size / 3;
                    data[index][3] = ops[opsN];
                }
            }
        }
        return data;
    }

    @Test(dataProvider="intSet")
    public void testParallelPrefixForInt(int[] data, int fromIndex, int toIndex, IntBinaryOperator op) {
        int[] sequentialResult = data.clone();
        for (int index = fromIndex + 1; index < toIndex; index++) {
            sequentialResult[index ] = op.applyAsInt(sequentialResult[index  - 1], sequentialResult[index]);
        }

        int[] parallelResult = data.clone();
        Arrays.parallelPrefix(parallelResult, fromIndex, toIndex, op);
        assertArraysEqual(parallelResult, sequentialResult);

        int[] parallelRangeResult = Arrays.copyOfRange(data, fromIndex, toIndex);
        Arrays.parallelPrefix(parallelRangeResult, op);
        assertArraysEqual(parallelRangeResult, Arrays.copyOfRange(sequentialResult, fromIndex, toIndex));
    }

    @Test(dataProvider="longSet")
    public void testParallelPrefixForLong(long[] data, int fromIndex, int toIndex, LongBinaryOperator op) {
        long[] sequentialResult = data.clone();
        for (int index = fromIndex + 1; index < toIndex; index++) {
            sequentialResult[index ] = op.applyAsLong(sequentialResult[index  - 1], sequentialResult[index]);
        }

        long[] parallelResult = data.clone();
        Arrays.parallelPrefix(parallelResult, fromIndex, toIndex, op);
        assertArraysEqual(parallelResult, sequentialResult);

        long[] parallelRangeResult = Arrays.copyOfRange(data, fromIndex, toIndex);
        Arrays.parallelPrefix(parallelRangeResult, op);
        assertArraysEqual(parallelRangeResult, Arrays.copyOfRange(sequentialResult, fromIndex, toIndex));
    }

    @Test(dataProvider="doubleSet")
    public void testParallelPrefixForDouble(double[] data, int fromIndex, int toIndex, DoubleBinaryOperator op) {
        double[] sequentialResult = data.clone();
        for (int index = fromIndex + 1; index < toIndex; index++) {
            sequentialResult[index ] = op.applyAsDouble(sequentialResult[index  - 1], sequentialResult[index]);
        }

        double[] parallelResult = data.clone();
        Arrays.parallelPrefix(parallelResult, fromIndex, toIndex, op);
        assertArraysEqual(parallelResult, sequentialResult);

        double[] parallelRangeResult = Arrays.copyOfRange(data, fromIndex, toIndex);
        Arrays.parallelPrefix(parallelRangeResult, op);
        assertArraysEqual(parallelRangeResult, Arrays.copyOfRange(sequentialResult, fromIndex, toIndex));
    }

    @Test(dataProvider="stringSet")
    public void testParallelPrefixForStringr(String[] data , int fromIndex, int toIndex, BinaryOperator<String> op) {
        String[] sequentialResult = data.clone();
        for (int index = fromIndex + 1; index < toIndex; index++) {
            sequentialResult[index ] = op.apply(sequentialResult[index  - 1], sequentialResult[index]);
        }

        String[] parallelResult = data.clone();
        Arrays.parallelPrefix(parallelResult, fromIndex, toIndex, op);
        assertArraysEqual(parallelResult, sequentialResult);

        String[] parallelRangeResult = Arrays.copyOfRange(data, fromIndex, toIndex);
        Arrays.parallelPrefix(parallelRangeResult, op);
        assertArraysEqual(parallelRangeResult, Arrays.copyOfRange(sequentialResult, fromIndex, toIndex));
    }

    @Test
    public void testNPEs() {
        // null array
        assertThrowsNPE(() -> Arrays.parallelPrefix((int[]) null, Integer::max));
        assertThrowsNPE(() -> Arrays.parallelPrefix((long []) null, Long::max));
        assertThrowsNPE(() -> Arrays.parallelPrefix((double []) null, Double::max));
        assertThrowsNPE(() -> Arrays.parallelPrefix((String []) null, String::concat));

        // null array w/ range
        assertThrowsNPE(() -> Arrays.parallelPrefix((int[]) null, 0, 0, Integer::max));
        assertThrowsNPE(() -> Arrays.parallelPrefix((long []) null, 0, 0, Long::max));
        assertThrowsNPE(() -> Arrays.parallelPrefix((double []) null, 0, 0, Double::max));
        assertThrowsNPE(() -> Arrays.parallelPrefix((String []) null, 0, 0, String::concat));

        // null op
        assertThrowsNPE(() -> Arrays.parallelPrefix(new int[] {}, null));
        assertThrowsNPE(() -> Arrays.parallelPrefix(new long[] {}, null));
        assertThrowsNPE(() -> Arrays.parallelPrefix(new double[] {}, null));
        assertThrowsNPE(() -> Arrays.parallelPrefix(new String[] {}, null));

        // null op w/ range
        assertThrowsNPE(() -> Arrays.parallelPrefix(new int[] {}, 0, 0, null));
        assertThrowsNPE(() -> Arrays.parallelPrefix(new long[] {}, 0, 0, null));
        assertThrowsNPE(() -> Arrays.parallelPrefix(new double[] {}, 0, 0, null));
        assertThrowsNPE(() -> Arrays.parallelPrefix(new String[] {}, 0, 0, null));
    }

    @Test
    public void testIAEs() {
        assertThrowsIAE(() -> Arrays.parallelPrefix(new int[] {}, 1, 0, Integer::max));
        assertThrowsIAE(() -> Arrays.parallelPrefix(new long[] {}, 1, 0, Long::max));
        assertThrowsIAE(() -> Arrays.parallelPrefix(new double[] {}, 1, 0, Double::max));
        assertThrowsIAE(() -> Arrays.parallelPrefix(new String[] {}, 1, 0, String::concat));
    }

    @Test
    public void testAIOOBEs() {
        // bad "fromIndex"
        assertThrowsAIOOB(() -> Arrays.parallelPrefix(new int[] {}, -1, 0, Integer::max));
        assertThrowsAIOOB(() -> Arrays.parallelPrefix(new long[] {}, -1, 0, Long::max));
        assertThrowsAIOOB(() -> Arrays.parallelPrefix(new double[] {}, -1, 0, Double::max));
        assertThrowsAIOOB(() -> Arrays.parallelPrefix(new String[] {}, -1, 0, String::concat));

        // bad "toIndex"
        assertThrowsAIOOB(() -> Arrays.parallelPrefix(new int[] {}, 0, 1, Integer::max));
        assertThrowsAIOOB(() -> Arrays.parallelPrefix(new long[] {}, 0, 1, Long::max));
        assertThrowsAIOOB(() -> Arrays.parallelPrefix(new double[] {}, 0, 1, Double::max));
        assertThrowsAIOOB(() -> Arrays.parallelPrefix(new String[] {}, 0, 1, String::concat));
    }

    // "library" code

    private void assertThrowsNPE(ThrowingRunnable r) {
        assertThrows(NullPointerException.class, r);
    }

    private void assertThrowsIAE(ThrowingRunnable r) {
        assertThrows(IllegalArgumentException.class, r);
    }

    private void assertThrowsAIOOB(ThrowingRunnable r) {
        assertThrows(ArrayIndexOutOfBoundsException.class, r);
    }

    static void assertArraysEqual(int[] actual, int[] expected) {
        try {
            assertEquals(actual, expected, "");
        } catch (AssertionError x) {
            throw new AssertionError(String.format("Expected:%s, actual:%s",
                    Arrays.toString(expected), Arrays.toString(actual)), x);
        }
    }

    static void assertArraysEqual(long[] actual, long[] expected) {
        try {
            assertEquals(actual, expected, "");
        } catch (AssertionError x) {
            throw new AssertionError(String.format("Expected:%s, actual:%s",
                    Arrays.toString(expected), Arrays.toString(actual)), x);
        }
    }

    static void assertArraysEqual(double[] actual, double[] expected) {
        try {
            assertEquals(actual, expected, "");
        } catch (AssertionError x) {
            throw new AssertionError(String.format("Expected:%s, actual:%s",
                    Arrays.toString(expected), Arrays.toString(actual)), x);
        }
    }

    static void assertArraysEqual(String[] actual, String[] expected) {
        try {
            assertEquals(actual, expected, "");
        } catch (AssertionError x) {
            throw new AssertionError(String.format("Expected:%s, actual:%s",
                    Arrays.toString(expected), Arrays.toString(actual)), x);
        }
    }
}

