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

import java.util.Collection;
import java.util.stream.*;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.LambdaTestHelpers.assertCountSum;

/**
 * SliceOpTest
 *
 * @author Brian Goetz
 */
@Test
public class IntSliceOpTest extends OpTestCase {

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    public void testSkip() {
        assertCountSum(IntStream.range(0, 0).skip(0).boxed(), 0, 0);
        assertCountSum(IntStream.range(0, 0).skip(4).boxed(), 0, 0);
        assertCountSum(IntStream.range(1, 5).skip(4).boxed(), 0, 0);
        assertCountSum(IntStream.range(1, 5).skip(2).boxed(), 2, 7);
        assertCountSum(IntStream.range(1, 5).skip(0).boxed(), 4, 10);

        assertCountSum(IntStream.range(0, 0).parallel().skip(0).boxed(), 0, 0);
        assertCountSum(IntStream.range(0, 0).parallel().skip(4).boxed(), 0, 0);
        assertCountSum(IntStream.range(1, 5).parallel().skip(4).boxed(), 0, 0);
        assertCountSum(IntStream.range(1, 5).parallel().skip(2).boxed(), 2, 7);
        assertCountSum(IntStream.range(1, 5).parallel().skip(0).boxed(), 4, 10);

        exerciseOps(EMPTY_INT_ARRAY, s -> s.skip(0), EMPTY_INT_ARRAY);
        exerciseOps(EMPTY_INT_ARRAY, s -> s.skip(10), EMPTY_INT_ARRAY);

        exerciseOps(IntStream.range(1, 2).toArray(), s -> s.skip(0), IntStream.range(1, 2).toArray());
        exerciseOps(IntStream.range(1, 2).toArray(), s -> s.skip(1), EMPTY_INT_ARRAY);
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(0), IntStream.range(1, 101).toArray());
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(10), IntStream.range(11, 101).toArray());
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(100), EMPTY_INT_ARRAY);
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(200), EMPTY_INT_ARRAY);
    }

    public void testLimit() {
        assertCountSum(IntStream.range(0, 0).limit(4).boxed(), 0, 0);
        assertCountSum(IntStream.range(1, 3).limit(4).boxed(), 2, 3);
        assertCountSum(IntStream.range(1, 5).limit(4).boxed(), 4, 10);
        assertCountSum(IntStream.range(1, 9).limit(4).boxed(), 4, 10);

        assertCountSum(IntStream.range(0, 0).parallel().limit(4).boxed(), 0, 0);
        assertCountSum(IntStream.range(1, 3).parallel().limit(4).boxed(), 2, 3);
        assertCountSum(IntStream.range(1, 5).parallel().limit(4).boxed(), 4, 10);
        assertCountSum(IntStream.range(1, 9).parallel().limit(4).boxed(), 4, 10);

        exerciseOps(EMPTY_INT_ARRAY, s -> s.limit(0), EMPTY_INT_ARRAY);
        exerciseOps(EMPTY_INT_ARRAY, s -> s.limit(10), EMPTY_INT_ARRAY);

        exerciseOps(IntStream.range(1, 2).toArray(), s -> s.limit(0), EMPTY_INT_ARRAY);
        exerciseOps(IntStream.range(1, 2).toArray(), s -> s.limit(1), IntStream.range(1, 2).toArray());
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.limit(0), EMPTY_INT_ARRAY);
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.limit(10), IntStream.range(1, 11).toArray());
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.limit(10).limit(10), IntStream.range(1, 11).toArray());
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.limit(100), IntStream.range(1, 101).toArray());
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.limit(100).limit(10), IntStream.range(1, 11).toArray());
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.limit(200), IntStream.range(1, 101).toArray());
    }

    public void testSkipLimit() {
        exerciseOps(EMPTY_INT_ARRAY, s -> s.skip(0).limit(0), EMPTY_INT_ARRAY);
        exerciseOps(EMPTY_INT_ARRAY, s -> s.skip(0).limit(10), EMPTY_INT_ARRAY);
        exerciseOps(EMPTY_INT_ARRAY, s -> s.skip(10).limit(0), EMPTY_INT_ARRAY);
        exerciseOps(EMPTY_INT_ARRAY, s -> s.skip(10).limit(10), EMPTY_INT_ARRAY);

        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(0).limit(100), IntStream.range(1, 101).toArray());
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(0).limit(10), IntStream.range(1, 11).toArray());
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(0).limit(0), EMPTY_INT_ARRAY);
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(10).limit(100), IntStream.range(11, 101).toArray());
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(10).limit(10), IntStream.range(11, 21).toArray());
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(10).limit(0), EMPTY_INT_ARRAY);
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(100).limit(100), EMPTY_INT_ARRAY);
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(100).limit(10), EMPTY_INT_ARRAY);
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(100).limit(0), EMPTY_INT_ARRAY);
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(200).limit(100), EMPTY_INT_ARRAY);
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(200).limit(10), EMPTY_INT_ARRAY);
        exerciseOps(IntStream.range(1, 101).toArray(), s -> s.skip(200).limit(0), EMPTY_INT_ARRAY);
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

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testSkipOps(String name, TestData.OfInt data) {
        List<Integer> skips = sizes(data.size());

        for (int s : skips) {
            setContext("skip", s);
            Collection<Integer> sr = exerciseOps(data, st -> st.skip(s));
            assertEquals(sr.size(), sliceSize(data.size(), s));

            sr = exerciseOps(data, st -> st.skip(s).skip(s / 2));
            assertEquals(sr.size(), sliceSize(sliceSize(data.size(), s), s / 2));
        }
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testSkipLimitOps(String name, TestData.OfInt data) {
        List<Integer> skips = sizes(data.size());
        List<Integer> limits = skips;

        for (int s : skips) {
            setContext("skip", s);
            for (int limit : limits) {
                setContext("limit", limit);
                Collection<Integer> sr = exerciseOps(data, st -> st.skip(s).limit(limit));
                assertEquals(sr.size(), sliceSize(sliceSize(data.size(), s), 0, limit));

                sr = exerciseOps(data, st -> st.skip(s).limit(limit));
                assertEquals(sr.size(), sliceSize(data.size(), s, limit));
            }
        }
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testLimitOps(String name, TestData.OfInt data) {
        List<Integer> limits = sizes(data.size());

        for (int limit : limits) {
            setContext("limit", limit);
            Collection<Integer> sr = exerciseOps(data, st -> st.limit(limit));
            assertEquals(sr.size(), sliceSize(data.size(), 0, limit));

            sr = exerciseOps(data, st -> st.limit(limit).limit(limit / 2));
            assertEquals(sr.size(), sliceSize(sliceSize(data.size(), 0, limit), 0, limit / 2));
        }
    }

    public void testLimitSort() {
        exerciseOps(IntStream.range(1, 101).map(i -> 101 - i).toArray(), s -> s.limit(10).sorted());
    }

    @Test(groups = { "serialization-hostile" })
    public void testLimitShortCircuit() {
        for (int l : Arrays.asList(0, 10)) {
            setContext("limit", l);
            AtomicInteger ai = new AtomicInteger();
            IntStream.range(1, 101)
                    .peek(i -> ai.getAndIncrement())
                    .limit(l).toArray();
            // For the case of a zero limit, one element will get pushed through the sink chain
            assertEquals(ai.get(), l, "tee block was called too many times");
        }
    }

    public void testSkipParallel() {
        int[] l = IntStream.range(1, 1001).parallel().skip(200).limit(200).sequential().toArray();
        assertEquals(l.length, 200);
        assertEquals(l[l.length - 1], 400);
    }

    public void testLimitParallel() {
        int[] l = IntStream.range(1, 1001).parallel().limit(500).sequential().toArray();
        assertEquals(l.length, 500);
        assertEquals(l[l.length - 1], 500);
    }

    private List<Integer> sizes(int size) {
        if (size < 4) {
            return Arrays.asList(0, 1, 2, 3, 4, 6);
        }
        else {
            return Arrays.asList(0, 1, size / 2, size - 1, size, size + 1, 2 * size);
        }
    }
}
