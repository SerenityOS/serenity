/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.tests.java.util;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.DoubleStreamTestScenario;
import java.util.stream.IntStream;
import java.util.stream.IntStreamTestScenario;
import java.util.stream.LongStream;
import java.util.stream.LongStreamTestScenario;
import java.util.stream.OpTestCase;
import java.util.stream.StreamSupport;
import java.util.stream.TestData;

@Test
public class SplittableRandomTest extends OpTestCase {

    static class RandomBoxedSpliterator<T> implements Spliterator<T> {
        final SplittableRandom rng;
        long index;
        final long fence;
        final Function<SplittableRandom, T> rngF;

        RandomBoxedSpliterator(SplittableRandom rng, long index, long fence, Function<SplittableRandom, T> rngF) {
            this.rng = rng;
            this.index = index;
            this.fence = fence;
            this.rngF = rngF;
        }

        public RandomBoxedSpliterator<T> trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m <= i) ? null :
                   new RandomBoxedSpliterator<>(rng.split(), i, index = m, rngF);
        }

        public long estimateSize() {
            return fence - index;
        }

        public int characteristics() {
            return (Spliterator.SIZED | Spliterator.SUBSIZED |
                    Spliterator.NONNULL | Spliterator.IMMUTABLE);
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                consumer.accept(rngF.apply(rng));
                index = i + 1;
                return true;
            }
            return false;
        }
    }

    static final int SIZE = 1 << 16;

    // Ensure there is a range of a power of 2
    static final int[] BOUNDS = {256};
    static final int[] ORIGINS = {-16, 0, 16};

    static <T extends Comparable<T>> ResultAsserter<Iterable<T>> randomAsserter(int size, T origin, T bound) {
        return (act, exp, ord, par) -> {
            int count = 0;
            Set<Comparable<T>> values = new HashSet<>();
            for (Comparable<T> t : act) {
                if (origin.compareTo(bound) < 0) {
                    assertTrue(t.compareTo(origin) >= 0);
                    assertTrue(t.compareTo(bound) < 0);
                }
                values.add(t);
                count++;
            }
            assertEquals(count, size);
            // Assert that at least one different result is produced
            // For the size of the data it is highly improbable that this
            // will cause a false negative (i.e. a false failure)
            assertTrue(values.size() > 1);
        };
    }

    @DataProvider(name = "ints")
    public static Object[][] intsDataProvider() {
        List<Object[]> data = new ArrayList<>();

        // Function to create a stream using a RandomBoxedSpliterator

        Function<Function<SplittableRandom, Integer>, IntStream> rbsf =
                sf -> StreamSupport.stream(new RandomBoxedSpliterator<>(new SplittableRandom(), 0, SIZE, sf), false).
                        mapToInt(i -> i);

        // Unbounded

        data.add(new Object[]{
                TestData.Factory.ofIntSupplier(
                        String.format("new SplittableRandom().ints().limit(%d)", SIZE),
                        () -> new SplittableRandom().ints().limit(SIZE)),
                randomAsserter(SIZE, Integer.MAX_VALUE, 0)
        });

        data.add(new Object[]{
                TestData.Factory.ofIntSupplier(
                        String.format("new SplittableRandom().ints(%d)", SIZE),
                        () -> new SplittableRandom().ints(SIZE)),
                randomAsserter(SIZE, Integer.MAX_VALUE, 0)
        });

        data.add(new Object[]{
                TestData.Factory.ofIntSupplier(
                        String.format("new RandomBoxedSpliterator(0, %d, sr -> sr.nextInt())", SIZE),
                        () -> rbsf.apply(sr -> sr.nextInt())),
                randomAsserter(SIZE, Integer.MAX_VALUE, 0)
        });

        // Bounded

        for (int b : BOUNDS) {
            for (int o : ORIGINS) {
                final int origin = o;
                final int bound = b;

                data.add(new Object[]{
                        TestData.Factory.ofIntSupplier(
                                String.format("new SplittableRandom().ints(%d, %d).limit(%d)", origin, bound, SIZE),
                                () -> new SplittableRandom().ints(origin, bound).limit(SIZE)),
                        randomAsserter(SIZE, origin, bound)
                });

                data.add(new Object[]{
                        TestData.Factory.ofIntSupplier(
                                String.format("new SplittableRandom().ints(%d, %d, %d)", SIZE, origin, bound),
                                () -> new SplittableRandom().ints(SIZE, origin, bound)),
                        randomAsserter(SIZE, origin, bound)
                });

                if (origin == 0) {
                    data.add(new Object[]{
                            TestData.Factory.ofIntSupplier(
                                    String.format("new RandomBoxedSpliterator(0, %d, sr -> sr.nextInt(%d))", SIZE, bound),
                                    () -> rbsf.apply(sr -> sr.nextInt(bound))),
                            randomAsserter(SIZE, origin, bound)
                    });
                }

                data.add(new Object[]{
                        TestData.Factory.ofIntSupplier(
                                String.format("new RandomBoxedSpliterator(0, %d, sr -> sr.nextInt(%d, %d))", SIZE, origin, bound),
                                () -> rbsf.apply(sr -> sr.nextInt(origin, bound))),
                        randomAsserter(SIZE, origin, bound)
                });
            }
        }

        return data.toArray(new Object[0][]);
    }

    @Test(dataProvider = "ints")
    public void testInts(TestData.OfInt data, ResultAsserter<Iterable<Integer>> ra) {
        withData(data).
                stream(s -> s).
                without(IntStreamTestScenario.CLEAR_SIZED_SCENARIOS).
                resultAsserter(ra).
                exercise();
    }

    @DataProvider(name = "longs")
    public static Object[][] longsDataProvider() {
        List<Object[]> data = new ArrayList<>();

        // Function to create a stream using a RandomBoxedSpliterator

        Function<Function<SplittableRandom, Long>, LongStream> rbsf =
                sf -> StreamSupport.stream(new RandomBoxedSpliterator<>(new SplittableRandom(), 0, SIZE, sf), false).
                        mapToLong(i -> i);

        // Unbounded

        data.add(new Object[]{
                TestData.Factory.ofLongSupplier(
                        String.format("new SplittableRandom().longs().limit(%d)", SIZE),
                        () -> new SplittableRandom().longs().limit(SIZE)),
                randomAsserter(SIZE, Long.MAX_VALUE, 0L)
        });

        data.add(new Object[]{
                TestData.Factory.ofLongSupplier(
                        String.format("new SplittableRandom().longs(%d)", SIZE),
                        () -> new SplittableRandom().longs(SIZE)),
                randomAsserter(SIZE, Long.MAX_VALUE, 0L)
        });

        data.add(new Object[]{
                TestData.Factory.ofLongSupplier(
                        String.format("new RandomBoxedSpliterator(0, %d, sr -> sr.nextLong())", SIZE),
                        () -> rbsf.apply(sr -> sr.nextLong())),
                randomAsserter(SIZE, Long.MAX_VALUE, 0L)
        });

        // Bounded

        for (int b : BOUNDS) {
            for (int o : ORIGINS) {
                final long origin = o;
                final long bound = b;

                data.add(new Object[]{
                        TestData.Factory.ofLongSupplier(
                                String.format("new SplittableRandom().longs(%d, %d).limit(%d)", origin, bound, SIZE),
                                () -> new SplittableRandom().longs(origin, bound).limit(SIZE)),
                        randomAsserter(SIZE, origin, bound)
                });

                data.add(new Object[]{
                        TestData.Factory.ofLongSupplier(
                                String.format("new SplittableRandom().longs(%d, %d, %d)", SIZE, origin, bound),
                                () -> new SplittableRandom().longs(SIZE, origin, bound)),
                        randomAsserter(SIZE, origin, bound)
                });

                if (origin == 0) {
                    data.add(new Object[]{
                            TestData.Factory.ofLongSupplier(
                                    String.format("new RandomBoxedSpliterator(0, %d, sr -> sr.nextLong(%d))", SIZE, bound),
                                    () -> rbsf.apply(sr -> sr.nextLong(bound))),
                            randomAsserter(SIZE, origin, bound)
                    });
                }

                data.add(new Object[]{
                        TestData.Factory.ofLongSupplier(
                                String.format("new RandomBoxedSpliterator(0, %d, sr -> sr.nextLong(%d, %d))", SIZE, origin, bound),
                                () -> rbsf.apply(sr -> sr.nextLong(origin, bound))),
                        randomAsserter(SIZE, origin, bound)
                });
            }
        }

        return data.toArray(new Object[0][]);
    }

    @Test(dataProvider = "longs")
    public void testLongs(TestData.OfLong data, ResultAsserter<Iterable<Long>> ra) {
        withData(data).
                stream(s -> s).
                without(LongStreamTestScenario.CLEAR_SIZED_SCENARIOS).
                resultAsserter(ra).
                exercise();
    }

    @DataProvider(name = "doubles")
    public static Object[][] doublesDataProvider() {
        List<Object[]> data = new ArrayList<>();

        // Function to create a stream using a RandomBoxedSpliterator

        Function<Function<SplittableRandom, Double>, DoubleStream> rbsf =
                sf -> StreamSupport.stream(new RandomBoxedSpliterator<>(new SplittableRandom(), 0, SIZE, sf), false).
                        mapToDouble(i -> i);

        // Unbounded

        data.add(new Object[]{
                TestData.Factory.ofDoubleSupplier(
                        String.format("new SplittableRandom().doubles().limit(%d)", SIZE),
                        () -> new SplittableRandom().doubles().limit(SIZE)),
                randomAsserter(SIZE, Double.MAX_VALUE, 0d)
        });

        data.add(new Object[]{
                TestData.Factory.ofDoubleSupplier(
                        String.format("new SplittableRandom().doubles(%d)", SIZE),
                        () -> new SplittableRandom().doubles(SIZE)),
                randomAsserter(SIZE, Double.MAX_VALUE, 0d)
        });

        data.add(new Object[]{
                TestData.Factory.ofDoubleSupplier(
                        String.format("new RandomBoxedSpliterator(0, %d, sr -> sr.nextDouble())", SIZE),
                        () -> rbsf.apply(sr -> sr.nextDouble())),
                randomAsserter(SIZE, Double.MAX_VALUE, 0d)
        });

        // Bounded

        for (int b : BOUNDS) {
            for (int o : ORIGINS) {
                final double origin = o;
                final double bound = b;

                data.add(new Object[]{
                        TestData.Factory.ofDoubleSupplier(
                                String.format("new SplittableRandom().doubles(%f, %f).limit(%d)", origin, bound, SIZE),
                                () -> new SplittableRandom().doubles(origin, bound).limit(SIZE)),
                        randomAsserter(SIZE, origin, bound)
                });

                data.add(new Object[]{
                        TestData.Factory.ofDoubleSupplier(
                                String.format("new SplittableRandom().doubles(%d, %f, %f)", SIZE, origin, bound),
                                () -> new SplittableRandom().doubles(SIZE, origin, bound)),
                        randomAsserter(SIZE, origin, bound)
                });

                if (origin == 0) {
                    data.add(new Object[]{
                            TestData.Factory.ofDoubleSupplier(
                                    String.format("new RandomBoxedSpliterator(0, %d, sr -> sr.nextDouble(%f))", SIZE, bound),
                                    () -> rbsf.apply(sr -> sr.nextDouble(bound))),
                            randomAsserter(SIZE, origin, bound)
                    });
                }

                data.add(new Object[]{
                        TestData.Factory.ofDoubleSupplier(
                                String.format("new RandomBoxedSpliterator(0, %d, sr -> sr.nextDouble(%f, %f))", SIZE, origin, bound),
                                () -> rbsf.apply(sr -> sr.nextDouble(origin, bound))),
                        randomAsserter(SIZE, origin, bound)
                });
            }
        }

        return data.toArray(new Object[0][]);
    }

    @Test(dataProvider = "doubles")
    public void testDoubles(TestData.OfDouble data, ResultAsserter<Iterable<Double>> ra) {
        withData(data).
                stream(s -> s).
                without(DoubleStreamTestScenario.CLEAR_SIZED_SCENARIOS).
                resultAsserter(ra).
                exercise();
    }
}
