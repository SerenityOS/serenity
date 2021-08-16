/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.DoubleStreamTestScenario;
import java.util.stream.IntStream;
import java.util.stream.IntStreamTestScenario;
import java.util.stream.LambdaTestHelpers;
import java.util.stream.LongStream;
import java.util.stream.LongStreamTestScenario;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.stream.StreamTestScenario;
import java.util.stream.TestData;

import static java.util.stream.LambdaTestHelpers.assertUnique;


@Test
public class InfiniteStreamWithLimitOpTest extends OpTestCase {

    private static final long SKIP_LIMIT_SIZE = 1 << 16;

    @DataProvider(name = "Stream.limit")
    @SuppressWarnings("rawtypes")
    public static Object[][] sliceFunctionsDataProvider() {
        Function<String, String> f = s -> String.format(s, SKIP_LIMIT_SIZE);

        List<Object[]> data = new ArrayList<>();

        data.add(new Object[]{f.apply("Stream.limit(%d)"),
                (UnaryOperator<Stream>) s -> s.limit(SKIP_LIMIT_SIZE)});
        data.add(new Object[]{f.apply("Stream.skip(%1$d).limit(%1$d)"),
                (UnaryOperator<Stream>) s -> s.skip(SKIP_LIMIT_SIZE).limit(SKIP_LIMIT_SIZE)});

        return data.toArray(new Object[0][]);
    }

    @DataProvider(name = "IntStream.limit")
    public static Object[][] intSliceFunctionsDataProvider() {
        Function<String, String> f = s -> String.format(s, SKIP_LIMIT_SIZE);

        List<Object[]> data = new ArrayList<>();

        data.add(new Object[]{f.apply("IntStream.limit(%d)"),
                (UnaryOperator<IntStream>) s -> s.limit(SKIP_LIMIT_SIZE)});
        data.add(new Object[]{f.apply("IntStream.skip(%1$d).limit(%1$d)"),
                (UnaryOperator<IntStream>) s -> s.skip(SKIP_LIMIT_SIZE).limit(SKIP_LIMIT_SIZE)});

        return data.toArray(new Object[0][]);
    }

    @DataProvider(name = "LongStream.limit")
    public static Object[][] longSliceFunctionsDataProvider() {
        Function<String, String> f = s -> String.format(s, SKIP_LIMIT_SIZE);

        List<Object[]> data = new ArrayList<>();

        data.add(new Object[]{f.apply("LongStream.limit(%d)"),
                (UnaryOperator<LongStream>) s -> s.limit(SKIP_LIMIT_SIZE)});
        data.add(new Object[]{f.apply("LongStream.skip(%1$d).limit(%1$d)"),
                (UnaryOperator<LongStream>) s -> s.skip(SKIP_LIMIT_SIZE).limit(SKIP_LIMIT_SIZE)});

        return data.toArray(new Object[0][]);
    }

    @DataProvider(name = "DoubleStream.limit")
    public static Object[][] doubleSliceFunctionsDataProvider() {
        Function<String, String> f = s -> String.format(s, SKIP_LIMIT_SIZE);

        List<Object[]> data = new ArrayList<>();

        data.add(new Object[]{f.apply("DoubleStream.limit(%d)"),
                (UnaryOperator<DoubleStream>) s -> s.limit(SKIP_LIMIT_SIZE)});
        data.add(new Object[]{f.apply("DoubleStream.skip(%1$d).limit(%1$d)"),
                (UnaryOperator<DoubleStream>) s -> s.skip(SKIP_LIMIT_SIZE).limit(SKIP_LIMIT_SIZE)});

        return data.toArray(new Object[0][]);
    }

    private <T> ResultAsserter<Iterable<T>> unorderedAsserter() {
        return (act, exp, ord, par) -> {
            if (par & !ord) {
                // Can only assert that all elements of the actual result
                // are distinct and that the count is the limit size
                // any element within the range [0, Long.MAX_VALUE) may be
                // present
                assertUnique(act);
                long count = 0;
                for (T l : act) {
                    count++;
                }
                assertEquals(count, SKIP_LIMIT_SIZE, "size not equal");
            }
            else {
                LambdaTestHelpers.assertContents(act, exp);
            }
        };
    }

    private TestData.OfRef<Long> refLongs() {
        return refLongRange(0, Long.MAX_VALUE);
    }

    private TestData.OfRef<Long> refLongRange(long l, long u) {
        return TestData.Factory.ofSupplier(
                String.format("[%d, %d)", l, u),
                () -> LongStream.range(l, u).boxed());
    }

    private TestData.OfInt ints() {
        return intRange(0, Integer.MAX_VALUE);
    }

    private TestData.OfInt intRange(int l, int u) {
        return TestData.Factory.ofIntSupplier(
                String.format("[%d, %d)", l, u),
                () -> IntStream.range(l, u));
    }

    private TestData.OfLong longs() {
        return longRange(0, Long.MAX_VALUE);
    }

    private TestData.OfLong longRange(long l, long u) {
        return TestData.Factory.ofLongSupplier(
                String.format("[%d, %d)", l, u),
                () -> LongStream.range(l, u));
    }

    private TestData.OfDouble doubles() {
        return doubleRange(0, 1L << 53);
    }

    private TestData.OfDouble doubleRange(long l, long u) {
        return TestData.Factory.ofDoubleSupplier(
                String.format("[%d, %d)", l, u),
                () -> LongStream.range(l, u).mapToDouble(i -> (double) i));
    }


    // Sized/subsized range

    @Test(dataProvider = "Stream.limit")
    public void testSubsizedWithRange(String description, UnaryOperator<Stream<Long>> fs) {
        // Range is [0, Long.MAX_VALUE), splits are SUBSIZED
        // Such a size will induce out of memory errors for incorrect
        // slice implementations
        withData(refLongs()).
                stream(s -> fs.apply(s)).
                without(StreamTestScenario.CLEAR_SIZED_SCENARIOS).
                exercise();
    }

    @Test(dataProvider = "IntStream.limit")
    public void testIntSubsizedWithRange(String description, UnaryOperator<IntStream> fs) {
        // Range is [0, Integer.MAX_VALUE), splits are SUBSIZED
        // Such a size will induce out of memory errors for incorrect
        // slice implementations
        withData(ints()).
                stream(s -> fs.apply(s)).
                without(IntStreamTestScenario.CLEAR_SIZED_SCENARIOS).
                exercise();
    }

    @Test(dataProvider = "LongStream.limit")
    public void testLongSubsizedWithRange(String description, UnaryOperator<LongStream> fs) {
        // Range is [0, Long.MAX_VALUE), splits are SUBSIZED
        // Such a size will induce out of memory errors for incorrect
        // slice implementations
        withData(longs()).
                stream(s -> fs.apply(s)).
                without(LongStreamTestScenario.CLEAR_SIZED_SCENARIOS).
                exercise();
    }

    @Test(dataProvider = "DoubleStream.limit")
    public void testDoubleSubsizedWithRange(String description, UnaryOperator<DoubleStream> fs) {
        // Range is [0, 2^53), splits are SUBSIZED
        // Such a size will induce out of memory errors for incorrect
        // slice implementations
        withData(doubles()).
                stream(s -> fs.apply(s)).
                without(DoubleStreamTestScenario.CLEAR_SIZED_SCENARIOS).
                exercise();
    }


    // Unordered finite not SIZED/SUBSIZED

    @Test(dataProvider = "Stream.limit")
    public void testUnorderedFinite(String description, UnaryOperator<Stream<Long>> fs) {
        // Range is [0, Long.MAX_VALUE), splits are SUBSIZED
        // Such a size will induce out of memory errors for incorrect
        // slice implementations
        withData(longs()).
                stream(s -> fs.apply(s.filter(i -> true).unordered().boxed())).
                resultAsserter(unorderedAsserter()).
                exercise();
    }

    @Test(dataProvider = "IntStream.limit")
    public void testIntUnorderedFinite(String description, UnaryOperator<IntStream> fs) {
        // Range is [0, Integer.MAX_VALUE), splits are SUBSIZED
        // Such a size will induce out of memory errors for incorrect
        // slice implementations
        withData(ints()).
                stream(s -> fs.apply(s.filter(i -> true).unordered())).
                resultAsserter(unorderedAsserter()).
                exercise();
    }

    @Test(dataProvider = "LongStream.limit")
    public void testLongUnorderedFinite(String description, UnaryOperator<LongStream> fs) {
        // Range is [0, Long.MAX_VALUE), splits are SUBSIZED
        // Such a size will induce out of memory errors for incorrect
        // slice implementations
        withData(longs()).
                stream(s -> fs.apply(s.filter(i -> true).unordered())).
                resultAsserter(unorderedAsserter()).
                exercise();
    }

    @Test(dataProvider = "DoubleStream.limit")
    public void testDoubleUnorderedFinite(String description, UnaryOperator<DoubleStream> fs) {
        // Range is [0, 1L << 53), splits are SUBSIZED
        // Such a size will induce out of memory errors for incorrect
        // slice implementations
        // Upper bound ensures values mapped to doubles will be unique
        withData(doubles()).
                stream(s -> fs.apply(s.filter(i -> true).unordered())).
                resultAsserter(unorderedAsserter()).
                exercise();
    }


    // Unordered finite not SUBSIZED

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Spliterator.OfLong proxyNotSubsized(Spliterator.OfLong s) {
        InvocationHandler ih = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                switch (method.getName()) {
                    case "characteristics": {
                        int c = (Integer) method.invoke(s, args);
                        return c & ~Spliterator.SUBSIZED;
                    }
                    case "hasCharacteristics": {
                        int c = (Integer) args[0];
                        boolean b = (Boolean) method.invoke(s, args);
                        return b & ((c & Spliterator.SUBSIZED) == 0);
                    }
                    default:
                        return method.invoke(s, args);
                }
            }
        };

        return (Spliterator.OfLong) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                           new Class[]{Spliterator.OfLong.class},
                                                           ih);
    }

    private TestData.OfLong proxiedLongRange(long l, long u) {
        return TestData.Factory.ofLongSupplier(
                String.format("[%d, %d)", l, u),
                () -> StreamSupport.longStream(proxyNotSubsized(LongStream.range(l, u).spliterator()), false));
    }

    @Test(dataProvider = "Stream.limit")
    public void testUnorderedSizedNotSubsizedFinite(String description, UnaryOperator<Stream<Long>> fs) {
        // Range is [0, Long.MAX_VALUE), splits are not SUBSIZED (proxy clears
        // the SUBSIZED characteristic)
        // Such a size will induce out of memory errors for incorrect
        // slice implementations
        withData(proxiedLongRange(0, Long.MAX_VALUE)).
                stream(s -> fs.apply(s.unordered().boxed())).
                resultAsserter(unorderedAsserter()).
                exercise();
    }

    @Test(dataProvider = "IntStream.limit")
    public void testIntUnorderedSizedNotSubsizedFinite(String description, UnaryOperator<IntStream> fs) {
        // Range is [0, Integer.MAX_VALUE), splits are not SUBSIZED (proxy clears
        // the SUBSIZED characteristic)
        // Such a size will induce out of memory errors for incorrect
        // slice implementations
        withData(proxiedLongRange(0, Integer.MAX_VALUE)).
                stream(s -> fs.apply(s.unordered().mapToInt(i -> (int) i))).
                resultAsserter(unorderedAsserter()).
                exercise();
    }

    @Test(dataProvider = "LongStream.limit")
    public void testLongUnorderedSizedNotSubsizedFinite(String description, UnaryOperator<LongStream> fs) {
        // Range is [0, Long.MAX_VALUE), splits are not SUBSIZED (proxy clears
        // the SUBSIZED characteristic)
        // Such a size will induce out of memory errors for incorrect
        // slice implementations
        withData(proxiedLongRange(0, Long.MAX_VALUE)).
                stream(s -> fs.apply(s.unordered())).
                resultAsserter(unorderedAsserter()).
                exercise();
    }

    @Test(dataProvider = "DoubleStream.limit")
    public void testDoubleUnorderedSizedNotSubsizedFinite(String description, UnaryOperator<DoubleStream> fs) {
        // Range is [0, Double.MAX_VALUE), splits are not SUBSIZED (proxy clears
        // the SUBSIZED characteristic)
        // Such a size will induce out of memory errors for incorrect
        // slice implementations
        withData(proxiedLongRange(0, 1L << 53)).
                stream(s -> fs.apply(s.unordered().mapToDouble(i -> (double) i))).
                resultAsserter(unorderedAsserter()).
                exercise();
    }


    // Unordered generation

    @Test(dataProvider = "Stream.limit")
    public void testUnorderedGenerator(String description, UnaryOperator<Stream<Long>> fs) {
        // Source is spliterator of infinite size
        TestData.OfRef<Long> generator = TestData.Factory.ofSupplier(
                "[1L, 1L, ...]", () -> Stream.generate(() -> 1L));

        withData(generator).
                stream(s -> fs.apply(s.filter(i -> true).unordered())).
                exercise();
    }

    @Test(dataProvider = "IntStream.limit")
    public void testIntUnorderedGenerator(String description, UnaryOperator<IntStream> fs) {
        // Source is spliterator of infinite size
        TestData.OfInt generator = TestData.Factory.ofIntSupplier(
                "[1, 1, ...]", () -> IntStream.generate(() -> 1));

        withData(generator).
                stream(s -> fs.apply(s.filter(i -> true).unordered())).
                exercise();
    }

    @Test(dataProvider = "LongStream.limit")
    public void testLongUnorderedGenerator(String description, UnaryOperator<LongStream> fs) {
        // Source is spliterator of infinite size
        TestData.OfLong generator = TestData.Factory.ofLongSupplier(
                "[1L, 1L, ...]", () -> LongStream.generate(() -> 1));

        withData(generator).
                stream(s -> fs.apply(s.filter(i -> true).unordered())).
                exercise();
    }

    @Test(dataProvider = "DoubleStream.limit")
    public void testDoubleUnorderedGenerator(String description, UnaryOperator<DoubleStream> fs) {
        // Source is spliterator of infinite size
        TestData.OfDouble generator = TestData.Factory.ofDoubleSupplier(
                "[1.0, 1.0, ...]", () -> DoubleStream.generate(() -> 1.0));

        withData(generator).
                stream(s -> fs.apply(s.filter(i -> true).unordered())).
                exercise();
    }


    // Unordered iteration

    @Test(dataProvider = "Stream.limit")
    public void testUnorderedIteration(String description, UnaryOperator<Stream<Long>> fs) {
        // Source is a right-balanced tree of infinite size
        TestData.OfRef<Long> iterator = TestData.Factory.ofSupplier(
                "[1L, 2L, 3L, ...]", () -> Stream.iterate(1L, i -> i + 1L));

        // Ref
        withData(iterator).
                stream(s -> fs.apply(s.unordered())).
                resultAsserter(unorderedAsserter()).
                exercise();
    }

    @Test(dataProvider = "IntStream.limit")
    public void testIntUnorderedIteration(String description, UnaryOperator<IntStream> fs) {
        // Source is a right-balanced tree of infinite size
        TestData.OfInt iterator = TestData.Factory.ofIntSupplier(
                "[1, 2, 3, ...]", () -> IntStream.iterate(1, i -> i + 1));

        // Ref
        withData(iterator).
                stream(s -> fs.apply(s.unordered())).
                resultAsserter(unorderedAsserter()).
                exercise();
    }

    @Test(dataProvider = "LongStream.limit")
    public void testLongUnorderedIteration(String description, UnaryOperator<LongStream> fs) {
        // Source is a right-balanced tree of infinite size
        TestData.OfLong iterator = TestData.Factory.ofLongSupplier(
                "[1L, 2L, 3L, ...]", () -> LongStream.iterate(1, i -> i + 1));

        // Ref
        withData(iterator).
                stream(s -> fs.apply(s.unordered())).
                resultAsserter(unorderedAsserter()).
                exercise();
    }

    @Test(dataProvider = "DoubleStream.limit")
    public void testDoubleUnorderedIteration(String description, UnaryOperator<DoubleStream> fs) {
        // Source is a right-balanced tree of infinite size
        TestData.OfDouble iterator = TestData.Factory.ofDoubleSupplier(
                "[1.0, 2.0, 3.0, ...]", () -> DoubleStream.iterate(1, i -> i + 1));

        // Ref
        withData(iterator).
                stream(s -> fs.apply(s.unordered())).
                resultAsserter(unorderedAsserter()).
                exercise();
    }
}
