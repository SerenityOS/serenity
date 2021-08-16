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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Spliterators;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.DoubleStreamTestDataProvider;
import java.util.stream.IntStream;
import java.util.stream.IntStreamTestDataProvider;
import java.util.stream.LongStream;
import java.util.stream.LongStreamTestDataProvider;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.stream.StreamTestDataProvider;
import java.util.stream.TestData;

import org.testng.annotations.Test;

import static java.util.stream.LambdaTestHelpers.countTo;
import static java.util.stream.LambdaTestHelpers.dpEven;
import static java.util.stream.LambdaTestHelpers.dpFalse;
import static java.util.stream.LambdaTestHelpers.dpOdd;
import static java.util.stream.LambdaTestHelpers.dpTrue;
import static java.util.stream.LambdaTestHelpers.ipEven;
import static java.util.stream.LambdaTestHelpers.ipFalse;
import static java.util.stream.LambdaTestHelpers.ipOdd;
import static java.util.stream.LambdaTestHelpers.ipTrue;
import static java.util.stream.LambdaTestHelpers.lpEven;
import static java.util.stream.LambdaTestHelpers.lpFalse;
import static java.util.stream.LambdaTestHelpers.lpOdd;
import static java.util.stream.LambdaTestHelpers.lpTrue;
import static java.util.stream.LambdaTestHelpers.pEven;
import static java.util.stream.LambdaTestHelpers.pFalse;
import static java.util.stream.LambdaTestHelpers.pOdd;
import static java.util.stream.LambdaTestHelpers.pTrue;

/**
 * MatchOpTest
 *
 * @author Brian Goetz
 */
@Test
public class MatchOpTest extends OpTestCase {
    private enum Kind { ANY, ALL, NONE }

    @SuppressWarnings("unchecked")
    private static final Predicate<Integer>[] INTEGER_PREDICATES
            = (Predicate<Integer>[]) new Predicate<?>[]{pTrue, pFalse, pEven, pOdd};

    @SuppressWarnings({"serial", "rawtypes"})
    private final Map kinds
            = new HashMap<Kind, Function<Predicate<Integer>, Function<Stream<Integer>, Boolean>>>() {{
        put(Kind.ANY, p -> s -> s.anyMatch(p));
        put(Kind.ALL, p -> s -> s.allMatch(p));
        put(Kind.NONE, p -> s -> s.noneMatch(p));
    }};

    @SuppressWarnings("unchecked")
    private <T> Map<Kind, Function<Predicate<T>, Function<Stream<T>, Boolean>>> kinds() {
        return (Map<Kind, Function<Predicate<T>, Function<Stream<T>, Boolean>>>) kinds;
    }

    private <T> void assertPredicates(List<T> source, Kind kind, Predicate<T>[] predicates, boolean... answers) {
        for (int i = 0; i < predicates.length; i++) {
            setContext("i", i);
            boolean match = this.<T>kinds().get(kind).apply(predicates[i]).apply(source.stream());
            assertEquals(answers[i], match, kind.toString() + predicates[i].toString());
        }
    }

    public void testStreamMatches() {
        assertPredicates(countTo(0), Kind.ANY, INTEGER_PREDICATES, false, false, false, false);
        assertPredicates(countTo(0), Kind.ALL, INTEGER_PREDICATES, true, true, true, true);
        assertPredicates(countTo(0), Kind.NONE, INTEGER_PREDICATES, true, true, true, true);

        assertPredicates(countTo(1), Kind.ANY, INTEGER_PREDICATES, true, false, false, true);
        assertPredicates(countTo(1), Kind.ALL, INTEGER_PREDICATES, true, false, false, true);
        assertPredicates(countTo(1), Kind.NONE, INTEGER_PREDICATES, false, true, true, false);

        assertPredicates(countTo(5), Kind.ANY, INTEGER_PREDICATES, true, false, true, true);
        assertPredicates(countTo(5), Kind.ALL, INTEGER_PREDICATES, true, false, false, false);
        assertPredicates(countTo(5), Kind.NONE, INTEGER_PREDICATES, false, true, false, false);
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testStream(String name, TestData.OfRef<Integer> data) {
        for (Predicate<Integer> p : INTEGER_PREDICATES) {
            setContext("p", p);
            for (Kind kind : Kind.values()) {
                setContext("kind", kind);
                exerciseTerminalOps(data, this.<Integer>kinds().get(kind).apply(p));
                exerciseTerminalOps(data, s -> s.filter(pFalse), this.<Integer>kinds().get(kind).apply(p));
                exerciseTerminalOps(data, s -> s.filter(pEven), this.<Integer>kinds().get(kind).apply(p));
            }
        }
    }

    public void testInfinite() {
        class CycleIterator implements Iterator<Integer> {
            final Supplier<Iterator<Integer>> source;
            Iterator<Integer> i = null;

            CycleIterator(Supplier<Iterator<Integer>> source) {
                this.source = source;
            }

            @Override
            public Integer next() {
                if (i == null || !i.hasNext()) {
                    i = source.get();
                }
                return i.next();
            }

            @Override
            public boolean hasNext() {
                if (i == null || !i.hasNext()) {
                    i = source.get();
                }
                return i.hasNext();
            }
        }

        Supplier<Iterator<Integer>> source = () -> Arrays.asList(1, 2, 3, 4).iterator();
        Supplier<Stream<Integer>> s = () -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(new CycleIterator(source), 0), false);

        assertFalse(s.get().allMatch(i -> i > 3));
        assertTrue(s.get().anyMatch(i -> i > 3));
        assertFalse(s.get().noneMatch(i -> i > 3));
        assertFalse(s.get().parallel().allMatch(i -> i > 3));
        assertTrue(s.get().parallel().anyMatch(i -> i > 3));
        assertFalse(s.get().parallel().noneMatch(i -> i > 3));
    }

    //

    private static final IntPredicate[] INT_PREDICATES
            = new IntPredicate[]{ipTrue, ipFalse, ipEven, ipOdd};

    @SuppressWarnings("serial")
    private final Map<Kind, Function<IntPredicate, Function<IntStream, Boolean>>> intKinds
            = new HashMap<Kind, Function<IntPredicate, Function<IntStream, Boolean>>>() {{
        put(Kind.ANY, p -> s -> s.anyMatch(p));
        put(Kind.ALL, p -> s -> s.allMatch(p));
        put(Kind.NONE, p -> s -> s.noneMatch(p));
    }};

    private void assertIntPredicates(Supplier<IntStream> source, Kind kind, IntPredicate[] predicates, boolean... answers) {
        for (int i = 0; i < predicates.length; i++) {
            setContext("i", i);
            boolean match = intKinds.get(kind).apply(predicates[i]).apply(source.get());
            assertEquals(answers[i], match, kind.toString() + predicates[i].toString());
        }
    }

    public void testIntStreamMatches() {
        assertIntPredicates(() -> IntStream.range(0, 0), Kind.ANY, INT_PREDICATES, false, false, false, false);
        assertIntPredicates(() -> IntStream.range(0, 0), Kind.ALL, INT_PREDICATES, true, true, true, true);
        assertIntPredicates(() -> IntStream.range(0, 0), Kind.NONE, INT_PREDICATES, true, true, true, true);

        assertIntPredicates(() -> IntStream.range(1, 2), Kind.ANY, INT_PREDICATES, true, false, false, true);
        assertIntPredicates(() -> IntStream.range(1, 2), Kind.ALL, INT_PREDICATES, true, false, false, true);
        assertIntPredicates(() -> IntStream.range(1, 2), Kind.NONE, INT_PREDICATES, false, true, true, false);

        assertIntPredicates(() -> IntStream.range(1, 6), Kind.ANY, INT_PREDICATES, true, false, true, true);
        assertIntPredicates(() -> IntStream.range(1, 6), Kind.ALL, INT_PREDICATES, true, false, false, false);
        assertIntPredicates(() -> IntStream.range(1, 6), Kind.NONE, INT_PREDICATES, false, true, false, false);
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntStream(String name, TestData.OfInt data) {
        for (IntPredicate p : INT_PREDICATES) {
            setContext("p", p);
            for (Kind kind : Kind.values()) {
                setContext("kind", kind);
                exerciseTerminalOps(data, intKinds.get(kind).apply(p));
                exerciseTerminalOps(data, s -> s.filter(ipFalse), intKinds.get(kind).apply(p));
                exerciseTerminalOps(data, s -> s.filter(ipEven), intKinds.get(kind).apply(p));
            }
        }
    }

    public void testIntInfinite() {
        class CycleIterator implements PrimitiveIterator.OfInt {
            final Supplier<PrimitiveIterator.OfInt> source;
            PrimitiveIterator.OfInt i = null;

            CycleIterator(Supplier<PrimitiveIterator.OfInt> source) {
                this.source = source;
            }

            @Override
            public int nextInt() {
                if (i == null || !i.hasNext()) {
                    i = source.get();
                }
                return i.nextInt();
            }

            @Override
            public boolean hasNext() {
                if (i == null || !i.hasNext()) {
                    i = source.get();
                }
                return i.hasNext();
            }
        }

        Supplier<PrimitiveIterator.OfInt> source = () -> Arrays.stream(new int[]{1, 2, 3, 4}).iterator();
        Supplier<IntStream> s = () -> StreamSupport.intStream(Spliterators.spliteratorUnknownSize(new CycleIterator(source), 0), false);

        assertFalse(s.get().allMatch(i -> i > 3));
        assertTrue(s.get().anyMatch(i -> i > 3));
        assertFalse(s.get().noneMatch(i -> i > 3));
        assertFalse(s.get().parallel().allMatch(i -> i > 3));
        assertTrue(s.get().parallel().anyMatch(i -> i > 3));
        assertFalse(s.get().parallel().noneMatch(i -> i > 3));
    }

    //

    private static final LongPredicate[] LONG_PREDICATES
            = new LongPredicate[]{lpTrue, lpFalse, lpEven, lpOdd};

    @SuppressWarnings("serial")
    private final Map<Kind, Function<LongPredicate, Function<LongStream, Boolean>>> longKinds
            = new HashMap<Kind, Function<LongPredicate, Function<LongStream, Boolean>>>() {{
        put(Kind.ANY, p -> s -> s.anyMatch(p));
        put(Kind.ALL, p -> s -> s.allMatch(p));
        put(Kind.NONE, p -> s -> s.noneMatch(p));
    }};

    private void assertLongPredicates(Supplier<LongStream> source, Kind kind, LongPredicate[] predicates, boolean... answers) {
        for (int i = 0; i < predicates.length; i++) {
            setContext("i", i);
            boolean match = longKinds.get(kind).apply(predicates[i]).apply(source.get());
            assertEquals(answers[i], match, kind.toString() + predicates[i].toString());
        }
    }

    public void testLongStreamMatches() {
        assertLongPredicates(() -> LongStream.range(0, 0), Kind.ANY, LONG_PREDICATES, false, false, false, false);
        assertLongPredicates(() -> LongStream.range(0, 0), Kind.ALL, LONG_PREDICATES, true, true, true, true);
        assertLongPredicates(() -> LongStream.range(0, 0), Kind.NONE, LONG_PREDICATES, true, true, true, true);

        assertLongPredicates(() -> LongStream.range(1, 2), Kind.ANY, LONG_PREDICATES, true, false, false, true);
        assertLongPredicates(() -> LongStream.range(1, 2), Kind.ALL, LONG_PREDICATES, true, false, false, true);
        assertLongPredicates(() -> LongStream.range(1, 2), Kind.NONE, LONG_PREDICATES, false, true, true, false);

        assertLongPredicates(() -> LongStream.range(1, 6), Kind.ANY, LONG_PREDICATES, true, false, true, true);
        assertLongPredicates(() -> LongStream.range(1, 6), Kind.ALL, LONG_PREDICATES, true, false, false, false);
        assertLongPredicates(() -> LongStream.range(1, 6), Kind.NONE, LONG_PREDICATES, false, true, false, false);
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongStream(String name, TestData.OfLong data) {
        for (LongPredicate p : LONG_PREDICATES) {
            setContext("p", p);
            for (Kind kind : Kind.values()) {
                setContext("kind", kind);
                exerciseTerminalOps(data, longKinds.get(kind).apply(p));
                exerciseTerminalOps(data, s -> s.filter(lpFalse), longKinds.get(kind).apply(p));
                exerciseTerminalOps(data, s -> s.filter(lpEven), longKinds.get(kind).apply(p));
            }
        }
    }

    public void testLongInfinite() {
        class CycleIterator implements PrimitiveIterator.OfLong {
            final Supplier<PrimitiveIterator.OfLong> source;
            PrimitiveIterator.OfLong i = null;

            CycleIterator(Supplier<PrimitiveIterator.OfLong> source) {
                this.source = source;
            }

            @Override
            public long nextLong() {
                if (i == null || !i.hasNext()) {
                    i = source.get();
                }
                return i.nextLong();
            }

            @Override
            public boolean hasNext() {
                if (i == null || !i.hasNext()) {
                    i = source.get();
                }
                return i.hasNext();
            }
        }

        Supplier<PrimitiveIterator.OfLong> source = () -> Arrays.stream(new long[]{1, 2, 3, 4}).iterator();
        Supplier<LongStream> s = () -> StreamSupport.longStream(Spliterators.spliteratorUnknownSize(new CycleIterator(source), 0), false);

        assertFalse(s.get().allMatch(i -> i > 3));
        assertTrue(s.get().anyMatch(i -> i > 3));
        assertFalse(s.get().noneMatch(i -> i > 3));
        assertFalse(s.get().parallel().allMatch(i -> i > 3));
        assertTrue(s.get().parallel().anyMatch(i -> i > 3));
        assertFalse(s.get().parallel().noneMatch(i -> i > 3));
    }

    //

    private static final DoublePredicate[] DOUBLE_PREDICATES
            = new DoublePredicate[]{dpTrue, dpFalse, dpEven, dpOdd};

    @SuppressWarnings("serial")
    private final Map<Kind, Function<DoublePredicate, Function<DoubleStream, Boolean>>> doubleKinds
            = new HashMap<Kind, Function<DoublePredicate, Function<DoubleStream, Boolean>>>() {{
        put(Kind.ANY, p -> s -> s.anyMatch(p));
        put(Kind.ALL, p -> s -> s.allMatch(p));
        put(Kind.NONE, p -> s -> s.noneMatch(p));
    }};

    private void assertDoublePredicates(Supplier<DoubleStream> source, Kind kind, DoublePredicate[] predicates, boolean... answers) {
        for (int i = 0; i < predicates.length; i++) {
            setContext("i", i);
            boolean match = doubleKinds.get(kind).apply(predicates[i]).apply(source.get());
            assertEquals(answers[i], match, kind.toString() + predicates[i].toString());
        }
    }

    public void testDoubleStreamMatches() {
        assertDoublePredicates(() -> LongStream.range(0, 0).asDoubleStream(), Kind.ANY, DOUBLE_PREDICATES, false, false, false, false);
        assertDoublePredicates(() -> LongStream.range(0, 0).asDoubleStream(), Kind.ALL, DOUBLE_PREDICATES, true, true, true, true);
        assertDoublePredicates(() -> LongStream.range(0, 0).asDoubleStream(), Kind.NONE, DOUBLE_PREDICATES, true, true, true, true);

        assertDoublePredicates(() -> LongStream.range(1, 2).asDoubleStream(), Kind.ANY, DOUBLE_PREDICATES, true, false, false, true);
        assertDoublePredicates(() -> LongStream.range(1, 2).asDoubleStream(), Kind.ALL, DOUBLE_PREDICATES, true, false, false, true);
        assertDoublePredicates(() -> LongStream.range(1, 2).asDoubleStream(), Kind.NONE, DOUBLE_PREDICATES, false, true, true, false);

        assertDoublePredicates(() -> LongStream.range(1, 6).asDoubleStream(), Kind.ANY, DOUBLE_PREDICATES, true, false, true, true);
        assertDoublePredicates(() -> LongStream.range(1, 6).asDoubleStream(), Kind.ALL, DOUBLE_PREDICATES, true, false, false, false);
        assertDoublePredicates(() -> LongStream.range(1, 6).asDoubleStream(), Kind.NONE, DOUBLE_PREDICATES, false, true, false, false);
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleStream(String name, TestData.OfDouble data) {
        for (DoublePredicate p : DOUBLE_PREDICATES) {
            setContext("p", p);
            for (Kind kind : Kind.values()) {
                setContext("kind", kind);
                exerciseTerminalOps(data, doubleKinds.get(kind).apply(p));
                exerciseTerminalOps(data, s -> s.filter(dpFalse), doubleKinds.get(kind).apply(p));
                exerciseTerminalOps(data, s -> s.filter(dpEven), doubleKinds.get(kind).apply(p));
            }
        }
    }

    public void testDoubleInfinite() {
        class CycleIterator implements PrimitiveIterator.OfDouble {
            final Supplier<PrimitiveIterator.OfDouble> source;
            PrimitiveIterator.OfDouble i = null;

            CycleIterator(Supplier<PrimitiveIterator.OfDouble> source) {
                this.source = source;
            }

            @Override
            public double nextDouble() {
                if (i == null || !i.hasNext()) {
                    i = source.get();
                }
                return i.nextDouble();
            }

            @Override
            public boolean hasNext() {
                if (i == null || !i.hasNext()) {
                    i = source.get();
                }
                return i.hasNext();
            }
        }

        Supplier<PrimitiveIterator.OfDouble> source = () -> Arrays.stream(new double[]{1, 2, 3, 4}).iterator();
        Supplier<DoubleStream> s = () -> StreamSupport.doubleStream(Spliterators.spliteratorUnknownSize(new CycleIterator(source), 0), false);

        assertFalse(s.get().allMatch(i -> i > 3));
        assertTrue(s.get().anyMatch(i -> i > 3));
        assertFalse(s.get().noneMatch(i -> i > 3));
        assertFalse(s.get().parallel().allMatch(i -> i > 3));
        assertTrue(s.get().parallel().anyMatch(i -> i > 3));
        assertFalse(s.get().parallel().noneMatch(i -> i > 3));
    }
}
