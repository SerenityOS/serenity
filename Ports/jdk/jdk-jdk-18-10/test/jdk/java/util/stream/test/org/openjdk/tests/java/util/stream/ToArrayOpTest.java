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
package org.openjdk.tests.java.util.stream;

import org.testng.annotations.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

import static java.util.stream.LambdaTestHelpers.*;
import static org.testng.Assert.assertEquals;


/**
 * ToArrayOpTest
 *
 */
@Test
public class ToArrayOpTest extends OpTestCase {

    public void testToArray() {
        assertCountSum(Arrays.asList(countTo(0).stream().toArray()), 0, 0);
        assertCountSum(Arrays.asList(countTo(10).stream().toArray()), 10, 55);
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOps(String name, TestData.OfRef<Integer> data) {
        exerciseTerminalOps(data, s -> s.toArray());
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOpsWithMap(String name, TestData.OfRef<Integer> data) {
        // Retain the size of the source
        // This should kick in the parallel evaluation optimization for tasks stuffing elements into a shared array

        Object[] objects = exerciseTerminalOps(data, s -> s.map(i -> (Integer) (i + i)), s -> s.toArray());
        assertTrue(objects.length == data.size());
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOpsWithSorted(String name, TestData.OfRef<Integer> data) {
        // Retain the size of the source
        // This should kick in the parallel evaluation optimization for tasks stuffing elements into a shared array

        Object[] objects = exerciseTerminalOps(data, s -> s.sorted(), s -> s.toArray());
        assertTrue(objects.length == data.size());
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOpsWithFlatMap(String name, TestData.OfRef<Integer> data) {
        // Double the size of the source
        // Fixed size optimizations will not be used

        Object[] objects = exerciseTerminalOps(data,
                                               s -> s.flatMap(e -> Arrays.stream(new Object[] { e, e })),
                                               s -> s.toArray());
        assertTrue(objects.length == data.size() * 2);
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOpsWithFilter(String name, TestData.OfRef<Integer> data) {
        // Reduce the size of the source
        // Fixed size optimizations will not be used

        exerciseTerminalOps(data, s -> s.filter(LambdaTestHelpers.pEven), s -> s.toArray());
    }

    public void testAsArrayWithType() {
        exerciseTerminalOps(
                TestData.Factory.ofCollection("", Arrays.asList(1.1, 2.2, 3.4, 4.4)),
                s -> // First pipeline slice using Object[] with Double elements
                    s.sorted()
                    // Second pipeline slice using Integer[] with Integer elements
                    .map((Double d) -> Integer.valueOf(d.intValue())).sorted(),
                s -> s.toArray(Integer[]::new));
    }

    private List<Function<Stream<Integer>, Stream<Integer>>> uniqueAndSortedPermutations =
            LambdaTestHelpers.permuteStreamFunctions(Arrays.asList(
                    s -> s.distinct(),
                    s -> s.distinct(),
                    s -> s.sorted(),
                    s -> s.sorted()
            ));

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testDistinctAndSortedPermutations(String name, TestData.OfRef<Integer> data) {
        for (Function<Stream<Integer>, Stream<Integer>> f : uniqueAndSortedPermutations) {
            exerciseTerminalOps(data, f, s -> s.toArray());

            Integer[] is = exerciseTerminalOps(data, f, s -> s.toArray(Integer[]::new));
            assertEquals(is.getClass(), Integer[].class);

            Number[] ns = exerciseTerminalOps(data, f, s -> s.toArray(Number[]::new));
            assertEquals(ns.getClass(), Number[].class);

            if (data.size() > 0) {
                Exception caught = null;
                try {
                    exerciseTerminalOps(data, f, s -> s.toArray(String[]::new));
                } catch (Exception e) {
                    caught = e;
                }
                assertTrue(caught != null);
                assertEquals(caught.getClass(), ArrayStoreException.class);
            }
        }
    }

    private List<Function<Stream<Integer>, Stream<Integer>>> statefulOpPermutations =
            LambdaTestHelpers.permuteStreamFunctions(Arrays.asList(
                    s -> s.limit(10),
                    s -> s.distinct(),
                    s -> s.sorted()
            ));

    private <T extends Object> ResultAsserter<T[]> statefulOpResultAsserter(TestData.OfRef<Integer> data) {
        return (act, exp, ord, par) -> {
            if (par) {
                if (!data.isOrdered()) {
                    // Relax the checking if the data source is unordered
                    // It is not exactly possible to determine if the limit
                    // operation is present and if it is before or after
                    // the sorted operation
                    // If the limit operation is present and before the sorted
                    // operation then the sub-set output after limit is a
                    // non-deterministic sub-set of the source
                    List<Integer> expected = new ArrayList<>();
                    data.forEach(expected::add);

                    List<T> actual = Arrays.asList(act);

                    assertEquals(actual.size(), exp.length);
                    assertTrue(expected.containsAll(actual));
                    return;
                }
                else if (!ord) {
                    LambdaTestHelpers.assertContentsUnordered(Arrays.asList(act),
                                                              Arrays.asList(exp));
                    return;
                }
            }
            assertEquals(act, exp);
        };
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class,
          groups = { "serialization-hostile" })
    public void testStatefulOpPermutations(String name, TestData.OfRef<Integer> data) {
        for (Function<Stream<Integer>, Stream<Integer>> f : statefulOpPermutations) {
            withData(data).terminal(f, s -> s.toArray())
                    .resultAsserter(statefulOpResultAsserter(data))
                    .exercise();

            Integer[] is = withData(data).terminal(f, s -> s.toArray(Integer[]::new))
                    .resultAsserter(statefulOpResultAsserter(data))
                    .exercise();
            assertEquals(is.getClass(), Integer[].class);

            Number[] ns = withData(data).terminal(f, s -> s.toArray(Number[]::new))
                    .resultAsserter(statefulOpResultAsserter(data))
                    .exercise();
            assertEquals(ns.getClass(), Number[].class);

            if (data.size() > 0) {
                Exception caught = null;
                try {
                    exerciseTerminalOps(data, f, s -> s.toArray(String[]::new));
                } catch (Exception e) {
                    caught = e;
                }
                assertTrue(caught != null);
                assertEquals(caught.getClass(), ArrayStoreException.class);
            }
        }
    }

    //

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntOps(String name, TestData.OfInt data) {
        exerciseTerminalOps(data, s -> s.toArray());
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntOpsWithMap(String name, TestData.OfInt data) {
        // Retain the size of the source
        // This should kick in the parallel evaluation optimization for tasks stuffing elements into a shared array

        int[] ints = exerciseTerminalOps(data, s -> s.map(i -> i + i), s -> s.toArray());
        assertTrue(ints.length == data.size());
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntOpsWithSorted(String name, TestData.OfInt data) {
        // Retain the size of the source
        // This should kick in the parallel evaluation optimization for tasks stuffing elements into a shared array

        int[] ints = exerciseTerminalOps(data, s -> s.sorted(), (IntStream s) -> s.toArray());
        assertTrue(ints.length == data.size());
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntOpsWithFlatMap(String name, TestData.OfInt data) {
        // Int the size of the source
        // Fixed size optimizations will not be used

        int[] objects = exerciseTerminalOps(data,
                                               s -> s.flatMap(e -> Arrays.stream(new int[] { e, e })),
                                               s -> s.toArray());
        assertTrue(objects.length == data.size() * 2);
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntOpsWithFilter(String name, TestData.OfInt data) {
        // Reduce the size of the source
        // Fixed size optimizations will not be used

        exerciseTerminalOps(data, s -> s.filter(LambdaTestHelpers.ipEven), s -> s.toArray());
    }

    private List<Function<IntStream, IntStream>> intUniqueAndSortedPermutations =
            LambdaTestHelpers.permuteStreamFunctions(Arrays.asList(
                    s -> s.distinct(),
                    s -> s.distinct(),
                    s -> s.sorted(),
                    s -> s.sorted()
            ));

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntDistinctAndSortedPermutations(String name, TestData.OfInt data) {
        for (Function<IntStream, IntStream> f : intUniqueAndSortedPermutations) {
            exerciseTerminalOps(data, f, s -> s.toArray());
        }
    }

    private List<Function<IntStream, IntStream>> intStatefulOpPermutations =
            LambdaTestHelpers.permuteStreamFunctions(Arrays.asList(
                    s -> s.limit(10),
                    s -> s.distinct(),
                    s -> s.sorted()
            ));

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntStatefulOpPermutations(String name, TestData.OfInt data) {
        for (Function<IntStream, IntStream> f : intStatefulOpPermutations) {
            exerciseTerminalOps(data, f, s -> s.toArray());
        }
    }

    //

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongOps(String name, TestData.OfLong data) {
        exerciseTerminalOps(data, s -> s.toArray());
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongOpsWithMap(String name, TestData.OfLong data) {
        // Retain the size of the source
        // This should kick in the parallel evaluation optimization for tasks stuffing elements into a shared array

        long[] longs = exerciseTerminalOps(data, s -> s.map(i -> i + i), s -> s.toArray());
        assertTrue(longs.length == data.size());
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongOpsWithSorted(String name, TestData.OfLong data) {
        // Retain the size of the source
        // This should kick in the parallel evaluation optimization for tasks stuffing elements into a shared array

        long[] longs = exerciseTerminalOps(data, s -> s.sorted(), (LongStream s) -> s.toArray());
        assertTrue(longs.length == data.size());
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongOpsWithFlatMap(String name, TestData.OfLong data) {
        // Long the size of the source
        // Fixed size optimizations will not be used

        long[] objects = exerciseTerminalOps(data,
                                               s -> s.flatMap(e -> Arrays.stream(new long[] { e, e })),
                                               s -> s.toArray());
        assertTrue(objects.length == data.size() * 2);
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongOpsWithFilter(String name, TestData.OfLong data) {
        // Reduce the size of the source
        // Fixed size optimizations will not be used

        exerciseTerminalOps(data, s -> s.filter(LambdaTestHelpers.lpEven), s -> s.toArray());
    }

    private List<Function<LongStream, LongStream>> longUniqueAndSortedPermutations =
            LambdaTestHelpers.permuteStreamFunctions(Arrays.asList(
                    s -> s.distinct(),
                    s -> s.distinct(),
                    s -> s.sorted(),
                    s -> s.sorted()
            ));

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongDistinctAndSortedPermutations(String name, TestData.OfLong data) {
        for (Function<LongStream, LongStream> f : longUniqueAndSortedPermutations) {
            exerciseTerminalOps(data, f, s -> s.toArray());
        }
    }

    private List<Function<LongStream, LongStream>> longStatefulOpPermutations =
            LambdaTestHelpers.permuteStreamFunctions(Arrays.asList(
                    s -> s.limit(10),
                    s -> s.distinct(),
                    s -> s.sorted()
            ));

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongStatefulOpPermutations(String name, TestData.OfLong data) {
        for (Function<LongStream, LongStream> f : longStatefulOpPermutations) {
            exerciseTerminalOps(data, f, s -> s.toArray());
        }
    }

    //

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleOps(String name, TestData.OfDouble data) {
        exerciseTerminalOps(data, s -> s.toArray());
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleOpsWithMap(String name, TestData.OfDouble data) {
        // Retain the size of the source
        // This should kick in the parallel evaluation optimization for tasks stuffing elements into a shared array

        double[] doubles = exerciseTerminalOps(data, s -> s.map(i -> i + i), s -> s.toArray());
        assertTrue(doubles.length == data.size());
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleOpsWithSorted(String name, TestData.OfDouble data) {
        // Retain the size of the source
        // This should kick in the parallel evaluation optimization for tasks stuffing elements into a shared array

        double[] doubles = exerciseTerminalOps(data, s -> s.sorted(), (DoubleStream s) -> s.toArray());
        assertTrue(doubles.length == data.size());
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleOpsWithFlatMap(String name, TestData.OfDouble data) {
        // Double the size of the source
        // Fixed size optimizations will not be used

        double[] objects = exerciseTerminalOps(data,
                                               s -> s.flatMap(e -> Arrays.stream(new double[] { e, e })),
                                               s -> s.toArray());
        assertTrue(objects.length == data.size() * 2);
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleOpsWithFilter(String name, TestData.OfDouble data) {
        // Reduce the size of the source
        // Fixed size optimizations will not be used

        exerciseTerminalOps(data, s -> s.filter(LambdaTestHelpers.dpEven), s -> s.toArray());
    }

    private List<Function<DoubleStream, DoubleStream>> doubleUniqueAndSortedPermutations =
            LambdaTestHelpers.permuteStreamFunctions(Arrays.asList(
                    s -> s.distinct(),
                    s -> s.distinct(),
                    s -> s.sorted(),
                    s -> s.sorted()
            ));

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleDistinctAndSortedPermutations(String name, TestData.OfDouble data) {
        for (Function<DoubleStream, DoubleStream> f : doubleUniqueAndSortedPermutations) {
            exerciseTerminalOps(data, f, s -> s.toArray());
        }
    }

    private List<Function<DoubleStream, DoubleStream>> doubleStatefulOpPermutations =
            LambdaTestHelpers.permuteStreamFunctions(Arrays.asList(
                    s -> s.limit(10),
                    s -> s.distinct(),
                    s -> s.sorted()
            ));

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleStatefulOpPermutations(String name, TestData.OfDouble data) {
        for (Function<DoubleStream, DoubleStream> f : doubleStatefulOpPermutations) {
            exerciseTerminalOps(data, f, s -> s.toArray());
        }
    }
}
