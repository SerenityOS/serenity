/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8148250 8265029
 */

package org.openjdk.tests.java.util.stream;

import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LambdaTestHelpers;
import java.util.stream.LongStream;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.stream.StreamTestDataProvider;
import java.util.stream.TestData;

import static java.util.stream.LambdaTestHelpers.*;

/**
 * SliceOpTest
 *
 * @author Brian Goetz
 */
@Test
public class SliceOpTest extends OpTestCase {

    public void testSkip() {
        assertCountSum(countTo(0).stream().skip(0), 0, 0);
        assertCountSum(countTo(0).stream().skip(4), 0, 0);
        assertCountSum(countTo(4).stream().skip(4), 0, 0);
        assertCountSum(countTo(4).stream().skip(2), 2, 7);
        assertCountSum(countTo(4).stream().skip(0), 4, 10);

        assertCountSum(countTo(0).parallelStream().skip(0), 0, 0);
        assertCountSum(countTo(0).parallelStream().skip(4), 0, 0);
        assertCountSum(countTo(4).parallelStream().skip(4), 0, 0);
        assertCountSum(countTo(4).parallelStream().skip(2), 2, 7);
        assertCountSum(countTo(4).parallelStream().skip(0), 4, 10);

        exerciseOps(Collections.emptyList(), s -> s.skip(0), Collections.emptyList());
        exerciseOps(Collections.emptyList(), s -> s.skip(10), Collections.emptyList());

        exerciseOps(countTo(1), s -> s.skip(0), countTo(1));
        exerciseOps(countTo(1), s -> s.skip(1), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(0), countTo(100));
        exerciseOps(countTo(100), s -> s.skip(10), range(11, 100));
        exerciseOps(countTo(100), s -> s.skip(100), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(200), Collections.emptyList());
    }

    public void testLimit() {
        assertCountSum(countTo(0).stream().limit(4), 0, 0);
        assertCountSum(countTo(2).stream().limit(4), 2, 3);
        assertCountSum(countTo(4).stream().limit(4), 4, 10);
        assertCountSum(countTo(8).stream().limit(4), 4, 10);

        assertCountSum(countTo(0).parallelStream().limit(4), 0, 0);
        assertCountSum(countTo(2).parallelStream().limit(4), 2, 3);
        assertCountSum(countTo(4).parallelStream().limit(4), 4, 10);
        assertCountSum(countTo(8).parallelStream().limit(4), 4, 10);

        exerciseOps(Collections.emptyList(), s -> s.limit(0), Collections.emptyList());
        exerciseOps(Collections.emptyList(), s -> s.limit(10), Collections.emptyList());
        exerciseOps(countTo(1), s -> s.limit(0), Collections.emptyList());
        exerciseOps(countTo(1), s -> s.limit(1), countTo(1));
        exerciseOps(countTo(100), s -> s.limit(0), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.limit(10), countTo(10));
        exerciseOps(countTo(100), s -> s.limit(10).limit(10), countTo(10));
        exerciseOps(countTo(100), s -> s.limit(100), countTo(100));
        exerciseOps(countTo(100), s -> s.limit(100).limit(10), countTo(10));
        exerciseOps(countTo(100), s -> s.limit(200), countTo(100));
    }

    public void testSkipLimit() {
        exerciseOps(Collections.emptyList(), s -> s.skip(0).limit(0), Collections.emptyList());
        exerciseOps(Collections.emptyList(), s -> s.skip(0).limit(10), Collections.emptyList());
        exerciseOps(Collections.emptyList(), s -> s.skip(10).limit(0), Collections.emptyList());
        exerciseOps(Collections.emptyList(), s -> s.skip(10).limit(10), Collections.emptyList());

        exerciseOps(countTo(100), s -> s.skip(0).limit(100), countTo(100));
        exerciseOps(countTo(100), s -> s.skip(0).limit(10), countTo(10));
        exerciseOps(countTo(100), s -> s.skip(0).limit(0), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(10).limit(100), range(11, 100));
        exerciseOps(countTo(100), s -> s.skip(10).limit(10), range(11, 20));
        exerciseOps(countTo(100), s -> s.skip(10).limit(0), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(100).limit(100), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(100).limit(10), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(100).limit(0), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(200).limit(100), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(200).limit(10), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(200).limit(0), Collections.emptyList());
    }

    public void testSlice() {
        exerciseOps(Collections.emptyList(), s -> s.skip(0).limit(0), Collections.emptyList());
        exerciseOps(Collections.emptyList(), s -> s.skip(0).limit(10), Collections.emptyList());
        exerciseOps(Collections.emptyList(), s -> s.skip(10).limit(10), Collections.emptyList());
        exerciseOps(Collections.emptyList(), s -> s.skip(10).limit(20), Collections.emptyList());

        exerciseOps(countTo(100), s -> s.skip(0).limit(100), countTo(100));
        exerciseOps(countTo(100), s -> s.skip(0).limit(10), countTo(10));
        exerciseOps(countTo(100), s -> s.skip(0).limit(0), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(10).limit(100), range(11, 100));
        exerciseOps(countTo(100), s -> s.skip(10).limit(10), range(11, 20));
        exerciseOps(countTo(100), s -> s.skip(10).limit(0), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(100).limit(100), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(100).limit(10), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(100).limit(0), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(200).limit(100), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(200).limit(10), Collections.emptyList());
        exerciseOps(countTo(100), s -> s.skip(200).limit(0), Collections.emptyList());
    }

    private int sliceSize(int dataSize, int skip, int limit) {
        int size = Math.max(0, dataSize - skip);
        if (limit >= 0)
            size = Math.min(size, limit);
        return size;
    }

    private int sliceSize(int dataSize, int skip) {
        return Math.max(0, dataSize - skip);
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class,
          groups = { "serialization-hostile" })
    public void testSkipOps(String name, TestData.OfRef<Integer> data) {
        List<Integer> skips = sizes(data.size());

        for (int s : skips) {
            setContext("skip", s);
            testSliceMulti(data,
                           sliceSize(data.size(), s),
                           st -> st.skip(s),
                           st -> st.skip(s),
                           st -> st.skip(s),
                           st -> st.skip(s));

            testSliceMulti(data,
                           sliceSize(sliceSize(data.size(), s), s/2),
                           st -> st.skip(s).skip(s / 2),
                           st -> st.skip(s).skip(s / 2),
                           st -> st.skip(s).skip(s / 2),
                           st -> st.skip(s).skip(s / 2));
        }
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class,
          groups = { "serialization-hostile" })
    public void testSkipLimitOps(String name, TestData.OfRef<Integer> data) {
        List<Integer> skips = sizes(data.size());
        List<Integer> limits = skips;

        for (int s : skips) {
            setContext("skip", s);
            for (int l : limits) {
                setContext("limit", l);
                testSliceMulti(data,
                               sliceSize(sliceSize(data.size(), s), 0, l),
                               st -> st.skip(s).limit(l),
                               st -> st.skip(s).limit(l),
                               st -> st.skip(s).limit(l),
                               st -> st.skip(s).limit(l));
            }
        }
    }

    @Test(groups = { "serialization-hostile" })
    public void testSkipLimitOpsWithNonSplittingSpliterator() {
        class NonSplittingNotSubsizedOrderedSpliterator<T> implements Spliterator<T> {
            Spliterator<T> s;

            NonSplittingNotSubsizedOrderedSpliterator(Spliterator<T> s) {
                assert s.hasCharacteristics(Spliterator.ORDERED);
                this.s = s;
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                return s.tryAdvance(action);
            }

            @Override
            public void forEachRemaining(Consumer<? super T> action) {
                s.forEachRemaining(action);
            }

            @Override
            public Spliterator<T> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return s.estimateSize();
            }

            @Override
            public int characteristics() {
                return s.characteristics() & ~(Spliterator.SUBSIZED);
            }

            @Override
            public Comparator<? super T> getComparator() {
                return s.getComparator();
            }
        }
        List<Integer> list = IntStream.range(0, 100).boxed().collect(Collectors.toList());
        TestData.OfRef<Integer> data = TestData.Factory.ofSupplier(
                "Non splitting, not SUBSIZED, ORDERED, stream",
                () -> StreamSupport.stream(new NonSplittingNotSubsizedOrderedSpliterator<>(list.spliterator()), false));

        testSkipLimitOps("testSkipLimitOpsWithNonSplittingSpliterator", data);
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class,
          groups = { "serialization-hostile" })
    public void testLimitOps(String name, TestData.OfRef<Integer> data) {
        List<Integer> limits = sizes(data.size());

        for (int l : limits) {
            setContext("limit", l);
            testSliceMulti(data,
                           sliceSize(data.size(), 0, l),
                           st -> st.limit(l),
                           st -> st.limit(l),
                           st -> st.limit(l),
                           st -> st.limit(l));
        }

        for (int l : limits) {
            setContext("limit", l);
            testSliceMulti(data,
                           sliceSize(sliceSize(data.size(), 0, l), 0, l / 2),
                           st -> st.limit(l).limit(l / 2),
                           st -> st.limit(l).limit(l / 2),
                           st -> st.limit(l).limit(l / 2),
                           st -> st.limit(l).limit(l / 2));
        }
    }

    private ResultAsserter<Iterable<Integer>> sliceResultAsserter(Iterable<Integer> data,
                                                                  int expectedSize) {
        return (act, exp, ord, par) -> {
            if (par & !ord) {
                List<Integer> expected = new ArrayList<>();
                data.forEach(expected::add);

                List<Integer> actual = new ArrayList<>();
                act.forEach(actual::add);

                assertEquals(actual.size(), expectedSize);
                assertTrue(expected.containsAll(actual));
            }
            else {
                LambdaTestHelpers.assertContents(act, exp);
            }
        };
    }

    private void testSliceMulti(TestData.OfRef<Integer> data,
                                int expectedSize,
                                Function<Stream<Integer>, Stream<Integer>> mRef,
                                Function<IntStream, IntStream> mInt,
                                Function<LongStream, LongStream> mLong,
                                Function<DoubleStream, DoubleStream> mDouble) {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        Function<Stream<Integer>, Stream<Integer>>[] ms = new Function[4];
        ms[0] = mRef;
        ms[1] = s -> mInt.apply(s.mapToInt(e -> e)).mapToObj(e -> e);
        ms[2] = s -> mLong.apply(s.mapToLong(e -> e)).mapToObj(e -> (int) e);
        ms[3] = s -> mDouble.apply(s.mapToDouble(e -> e)).mapToObj(e -> (int) e);
        testSliceMulti(data, expectedSize, ms);
    }

    @SafeVarargs
    private final void testSliceMulti(TestData.OfRef<Integer> data,
                                      int expectedSize,
                                      Function<Stream<Integer>, Stream<Integer>>... ms) {
        for (int i = 0; i < ms.length; i++) {
            setContext("mIndex", i);
            Function<Stream<Integer>, Stream<Integer>> m = ms[i];
            Collection<Integer> sr = withData(data)
                    .stream(m)
                    .resultAsserter(sliceResultAsserter(data, expectedSize))
                    .exercise();
            assertEquals(sr.size(), expectedSize);
        }
    }

    public void testLimitSort() {
        List<Integer> l = countTo(100);
        Collections.reverse(l);
        exerciseOps(l, s -> s.limit(10).sorted(Comparator.naturalOrder()));
    }

    @Test(groups = { "serialization-hostile" })
    public void testLimitShortCircuit() {
        for (int l : Arrays.asList(0, 10)) {
            setContext("l", l);
            AtomicInteger ai = new AtomicInteger();
            countTo(100).stream()
                    .peek(i -> ai.getAndIncrement())
                    .limit(l).toArray();
            // For the case of a zero limit, one element will get pushed through the sink chain
            assertEquals(ai.get(), l, "tee block was called too many times");
        }
    }

    private List<Integer> sizes(int size) {
        if (size < 4) {
            return Arrays.asList(0, 1, 2, 3, 4, 6);
        }
        else {
            return Arrays.asList(0, 1, size / 2, size - 1, size, size + 1, 2 * size);
        }
    }

    public void testLimitParallelHugeInput() {
        for (int n : new int[] {10, 100, 1000, 10000}) {
            long[] actual = LongStream.range(0, Long.MAX_VALUE)
                                  .parallel().filter(x -> true) // remove SIZED
                                  .limit(n).toArray();
            assertEquals(LongStream.range(0, n).toArray(), actual);
        }
    }

    public void testSliceOpsSpliteratorPreservesSized() {
        var parSpliterator = IntStream.range(0, 1000).parallel().skip(50).limit(800).spliterator();
        assertTrue(parSpliterator.hasCharacteristics(Spliterator.SIZED));
        assertTrue(parSpliterator.hasCharacteristics(Spliterator.SUBSIZED));
        assertEquals(parSpliterator.getExactSizeIfKnown(), 800);
        // Original spliterator is split to [0..499] and [500..999] parts
        // due to skip+limit, we have [50..499] and [500..849]
        var prefix = parSpliterator.trySplit();
        assertNotNull(prefix);
        assertTrue(parSpliterator.hasCharacteristics(Spliterator.SIZED));
        assertTrue(parSpliterator.hasCharacteristics(Spliterator.SUBSIZED));
        assertEquals(parSpliterator.getExactSizeIfKnown(), 350);
        assertTrue(prefix.hasCharacteristics(Spliterator.SIZED));
        assertTrue(prefix.hasCharacteristics(Spliterator.SUBSIZED));
        assertEquals(prefix.getExactSizeIfKnown(), 450);

        var seqSpliterator = IntStream.range(0, 1000).skip(50).limit(800).spliterator();
        assertTrue(seqSpliterator.hasCharacteristics(Spliterator.SIZED));
        assertTrue(seqSpliterator.hasCharacteristics(Spliterator.SUBSIZED));
        assertEquals(seqSpliterator.getExactSizeIfKnown(), 800);
    }
}
