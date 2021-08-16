/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.DefaultMethodStreams;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

/*
 * @test
 * @bug 8071597
 */
@Test
public class WhileOpStatefulTest extends OpTestCase {
    static final long COUNT_PERIOD = 100;

    static final long EXECUTION_TIME_LIMIT = TimeUnit.SECONDS.toMillis(10);

    static final long TAKE_WHILE_COUNT_LIMIT = 100_000;

    static final int DROP_SOURCE_SIZE = 10_000;

    static final long DROP_WHILE_COUNT_LIMIT = 5000;

    @Test
    public void testTimedTakeWithCount() {
        testTakeWhileMulti(
                s -> {
                    BooleanSupplier isWithinTakePeriod =
                            within(System.currentTimeMillis(), COUNT_PERIOD);
                    s.takeWhile(e -> isWithinTakePeriod.getAsBoolean())
                            .mapToLong(e -> 1).reduce(0, Long::sum);
                },
                s -> {
                    BooleanSupplier isWithinTakePeriod =
                            within(System.currentTimeMillis(), COUNT_PERIOD);
                    s.takeWhile(e -> isWithinTakePeriod.getAsBoolean())
                            .mapToLong(e -> 1).reduce(0, Long::sum);
                },
                s -> {
                    BooleanSupplier isWithinTakePeriod =
                            within(System.currentTimeMillis(), COUNT_PERIOD);
                    s.takeWhile(e -> isWithinTakePeriod.getAsBoolean())
                            .map(e -> 1).reduce(0, Long::sum);
                },
                s -> {
                    BooleanSupplier isWithinTakePeriod =
                            within(System.currentTimeMillis(), COUNT_PERIOD);
                    s.takeWhile(e -> isWithinTakePeriod.getAsBoolean())
                            .mapToLong(e -> 1).reduce(0, Long::sum);
                });
    }

    @Test(groups = { "serialization-hostile" })
    public void testCountTakeWithCount() {
        testTakeWhileMulti(
                s -> {
                    AtomicLong c = new AtomicLong();
                    long rc = s.takeWhile(e -> c.getAndIncrement() < TAKE_WHILE_COUNT_LIMIT)
                            .mapToLong(e -> 1).reduce(0, Long::sum);
                    assertTrue(rc <= c.get());
                },
                s -> {
                    AtomicLong c = new AtomicLong();
                    long rc = s.takeWhile(e -> c.getAndIncrement() < TAKE_WHILE_COUNT_LIMIT)
                            .mapToLong(e -> 1).reduce(0, Long::sum);
                    assertTrue(rc <= c.get());
                },
                s -> {
                    AtomicLong c = new AtomicLong();
                    long rc = s.takeWhile(e -> c.getAndIncrement() < TAKE_WHILE_COUNT_LIMIT)
                            .map(e -> 1).reduce(0, Long::sum);
                    assertTrue(rc <= c.get());
                },
                s -> {
                    AtomicLong c = new AtomicLong();
                    long rc = s.takeWhile(e -> c.getAndIncrement() < TAKE_WHILE_COUNT_LIMIT)
                            .mapToLong(e -> 1).reduce(0, Long::sum);
                    assertTrue(rc <= c.get());
                });
    }

    @Test(groups = { "serialization-hostile" })
    public void testCountTakeWithToArray() {
        testTakeWhileMulti(
                s -> {
                    AtomicLong c = new AtomicLong();
                    Object[] ra = s.takeWhile(e -> c.getAndIncrement() < TAKE_WHILE_COUNT_LIMIT)
                            .toArray();
                    assertTrue(ra.length <= c.get());
                },
                s -> {
                    AtomicLong c = new AtomicLong();
                    int[] ra = s.takeWhile(e -> c.getAndIncrement() < TAKE_WHILE_COUNT_LIMIT)
                            .toArray();
                    assertTrue(ra.length <= c.get());
                },
                s -> {
                    AtomicLong c = new AtomicLong();
                    long[] ra = s.takeWhile(e -> c.getAndIncrement() < TAKE_WHILE_COUNT_LIMIT)
                            .toArray();
                    assertTrue(ra.length <= c.get());
                },
                s -> {
                    AtomicLong c = new AtomicLong();
                    double[] ra = s.takeWhile(e -> c.getAndIncrement() < TAKE_WHILE_COUNT_LIMIT)
                            .toArray();
                    assertTrue(ra.length <= c.get());
                });
    }


    @Test(groups = { "serialization-hostile" })
    public void testCountDropWithCount() {
        testDropWhileMulti(
                s -> {
                    AtomicLong c = new AtomicLong();
                    long rc = s.dropWhile(e -> c.getAndIncrement() < DROP_WHILE_COUNT_LIMIT)
                            .mapToLong(e -> 1).reduce(0, Long::sum);
                    assertTrue(c.get() >= DROP_WHILE_COUNT_LIMIT);
                    assertTrue(rc <= DROP_SOURCE_SIZE);
                },
                s -> {
                    AtomicLong c = new AtomicLong();
                    long rc = s.dropWhile(e -> c.getAndIncrement() < DROP_WHILE_COUNT_LIMIT)
                            .mapToLong(e -> 1).reduce(0, Long::sum);
                    assertTrue(c.get() >= DROP_WHILE_COUNT_LIMIT);
                    assertTrue(rc <= DROP_SOURCE_SIZE);
                },
                s -> {
                    AtomicLong c = new AtomicLong();
                    long rc = s.dropWhile(e -> c.getAndIncrement() < DROP_WHILE_COUNT_LIMIT)
                            .map(e -> 1).reduce(0, Long::sum);
                    assertTrue(c.get() >= DROP_WHILE_COUNT_LIMIT);
                    assertTrue(rc <= DROP_SOURCE_SIZE);
                },
                s -> {
                    AtomicLong c = new AtomicLong();
                    long rc = s.dropWhile(e -> c.getAndIncrement() < DROP_WHILE_COUNT_LIMIT)
                            .mapToLong(e -> 1).reduce(0, Long::sum);
                    assertTrue(c.get() >= DROP_WHILE_COUNT_LIMIT);
                    assertTrue(rc <= DROP_SOURCE_SIZE);
                });
    }

    @Test(groups = { "serialization-hostile" })
    public void testCountDropWithToArray() {
        testDropWhileMulti(
                s -> {
                    AtomicLong c = new AtomicLong();
                    Object[] ra = s.dropWhile(e -> c.getAndIncrement() < DROP_WHILE_COUNT_LIMIT)
                            .toArray();
                    assertTrue(c.get() >= DROP_WHILE_COUNT_LIMIT);
                    assertTrue(ra.length <= DROP_SOURCE_SIZE);
                },
                s -> {
                    AtomicLong c = new AtomicLong();
                    int[] ra = s.dropWhile(e -> c.getAndIncrement() < DROP_WHILE_COUNT_LIMIT)
                            .toArray();
                    assertTrue(c.get() >= DROP_WHILE_COUNT_LIMIT);
                    assertTrue(ra.length <= DROP_SOURCE_SIZE);
                },
                s -> {
                    AtomicLong c = new AtomicLong();
                    long[] ra = s.dropWhile(e -> c.getAndIncrement() < DROP_WHILE_COUNT_LIMIT)
                            .toArray();
                    assertTrue(c.get() >= DROP_WHILE_COUNT_LIMIT);
                    assertTrue(ra.length <= DROP_SOURCE_SIZE);
                },
                s -> {
                    AtomicLong c = new AtomicLong();
                    double[] ra = s.dropWhile(e -> c.getAndIncrement() < DROP_WHILE_COUNT_LIMIT)
                            .toArray();
                    assertTrue(c.get() >= DROP_WHILE_COUNT_LIMIT);
                    assertTrue(ra.length <= DROP_SOURCE_SIZE);
                });
    }


    private void testTakeWhileMulti(Consumer<Stream<Integer>> mRef,
                                    Consumer<IntStream> mInt,
                                    Consumer<LongStream> mLong,
                                    Consumer<DoubleStream> mDouble) {
        Map<String, Supplier<Stream<Integer>>> sources = new HashMap<>();
        sources.put("Stream.generate()", () -> Stream.generate(() -> 1));
        sources.put("Stream.iterate()", () -> Stream.iterate(1, x -> 1));
        sources.put("Stream.iterate().unordered()", () -> Stream.iterate(1, x -> 1));
        testWhileMulti(sources, mRef, mInt, mLong, mDouble);
    }

    private void testDropWhileMulti(Consumer<Stream<Integer>> mRef,
                                    Consumer<IntStream> mInt,
                                    Consumer<LongStream> mLong,
                                    Consumer<DoubleStream> mDouble) {
        Map<String, Supplier<Stream<Integer>>> sources = new HashMap<>();
        sources.put("IntStream.range().boxed()",
                    () -> IntStream.range(0, DROP_SOURCE_SIZE).boxed());
        sources.put("IntStream.range().boxed().unordered()",
                    () -> IntStream.range(0, DROP_SOURCE_SIZE).boxed().unordered());
        sources.put("LinkedList.stream()",
                    () -> IntStream.range(0, DROP_SOURCE_SIZE).boxed()
                            .collect(toCollection(LinkedList::new))
                            .stream());
        sources.put("LinkedList.stream().unordered()",
                    () -> IntStream.range(0, DROP_SOURCE_SIZE).boxed()
                            .collect(toCollection(LinkedList::new))
                            .stream()
                            .unordered());
        testWhileMulti(sources, mRef, mInt, mLong, mDouble);
    }

    private void testWhileMulti(Map<String, Supplier<Stream<Integer>>> sources,
                                Consumer<Stream<Integer>> mRef,
                                Consumer<IntStream> mInt,
                                Consumer<LongStream> mLong,
                                Consumer<DoubleStream> mDouble) {
        Map<String, Function<Stream<Integer>, Stream<Integer>>> transforms = new HashMap<>();
        transforms.put("Stream.sequential()", s -> {
            BooleanSupplier isWithinExecutionPeriod = within(System.currentTimeMillis(),
                                                             EXECUTION_TIME_LIMIT);
            return s.peek(e -> {
                if (!isWithinExecutionPeriod.getAsBoolean()) {
                    throw new RuntimeException();
                }
            });
        });
        transforms.put("Stream.parallel()", s -> {
            BooleanSupplier isWithinExecutionPeriod = within(System.currentTimeMillis(),
                                                             EXECUTION_TIME_LIMIT);
            return s.parallel()
                    .peek(e -> {
                        if (!isWithinExecutionPeriod.getAsBoolean()) {
                            throw new RuntimeException();
                        }
                    });
        });

        Map<String, Consumer<Stream<Integer>>> actions = new HashMap<>();
        actions.put("Ref", mRef);
        actions.put("Int", s -> mInt.accept(s.mapToInt(e -> e)));
        actions.put("Long", s -> mLong.accept(s.mapToLong(e -> e)));
        actions.put("Double", s -> mDouble.accept(s.mapToDouble(e -> e)));
        actions.put("Ref using defaults", s -> mRef.accept(DefaultMethodStreams.delegateTo(s)));
        actions.put("Int using defaults", s -> mInt.accept(DefaultMethodStreams.delegateTo(s.mapToInt(e -> e))));
        actions.put("Long using defaults", s -> mLong.accept(DefaultMethodStreams.delegateTo(s.mapToLong(e -> e))));
        actions.put("Double using defaults", s -> mDouble.accept(DefaultMethodStreams.delegateTo(s.mapToDouble(e -> e))));

        for (Map.Entry<String, Supplier<Stream<Integer>>> s : sources.entrySet()) {
            setContext("source", s.getKey());

            for (Map.Entry<String, Function<Stream<Integer>, Stream<Integer>>> t : transforms.entrySet()) {
                setContext("transform", t.getKey());

                for (Map.Entry<String, Consumer<Stream<Integer>>> a : actions.entrySet()) {
                    setContext("shape", a.getKey());

                    Stream<Integer> stream = s.getValue().get();
                    stream = t.getValue().apply(stream);
                    a.getValue().accept(stream);
                }
            }
        }
    }

    static BooleanSupplier within(long start, long durationInMillis) {
        return () -> (System.currentTimeMillis() - start) < durationInMillis;
    }
}
