/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8148115
 */

package org.openjdk.tests.java.util.stream;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.*;

import org.testng.annotations.Test;

import java.util.function.Function;

import static java.util.stream.LambdaTestHelpers.*;


/**
 * FindAnyOpTest
 */
@Test
public class FindAnyOpTest extends OpTestCase {

    public void testFindAny() {
        assertFalse(Collections.emptySet().stream().findAny().isPresent(), "no result");
        assertFalse(countTo(10).stream().filter(x -> x > 10).findAny().isPresent(), "no result");
        assertTrue(countTo(10).stream().filter(pEven).findAny().isPresent(), "with result");
    }

    public void testFindAnyParallel() {
        assertFalse(Collections.emptySet().parallelStream().findAny().isPresent(), "no result");
        assertFalse(countTo(1000).parallelStream().filter(x -> x > 1000).findAny().isPresent(), "no result");
        assertTrue(countTo(1000).parallelStream().filter(pEven).findAny().isPresent(), "with result");
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testStream(String name, TestData.OfRef<Integer> data) {
        exerciseStream(data, s -> s);
        exerciseStream(data, s -> s.filter(pTrue));
        exerciseStream(data, s -> s.filter(pFalse));
        exerciseStream(data, s -> s.filter(pEven));
    }

    void exerciseStream(TestData.OfRef<Integer> data, Function<Stream<Integer>, Stream<Integer>> fs) {
        Optional<Integer> or = withData(data).terminal(fs, s -> s.findAny()).equalator(VALID_ANSWER).exercise();
        assertContains(or, fs.apply(data.stream()).iterator());
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntStream(String name, TestData.OfInt data) {
        exerciseIntStream(data, s -> s);
        exerciseIntStream(data, s -> s.filter(ipTrue));
        exerciseIntStream(data, s -> s.filter(ipFalse));
        exerciseIntStream(data, s -> s.filter(ipEven));
    }

    void exerciseIntStream(TestData.OfInt data, Function<IntStream, IntStream> fs) {
        OptionalInt or = withData(data).terminal(fs, s -> s.findAny()).equalator(INT_VALID_ANSWER).exercise();
        if (or.isPresent()) {
            int r = or.getAsInt();
            PrimitiveIterator.OfInt it = fs.apply(data.stream()).iterator();
            boolean contained = false;
            while (!contained && it.hasNext()) {
                contained = r == it.nextInt();
            }
            assertTrue(contained);
        }
        else {
            assertFalse(fs.apply(data.stream()).iterator().hasNext());
        }
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongStream(String name, TestData.OfLong data) {
        exerciseLongStream(data, s -> s);
        exerciseLongStream(data, s -> s.filter(lpTrue));
        exerciseLongStream(data, s -> s.filter(lpFalse));
        exerciseLongStream(data, s -> s.filter(lpEven));
    }

    void exerciseLongStream(TestData.OfLong data, Function<LongStream, LongStream> fs) {
        OptionalLong or = withData(data).terminal(fs, s -> s.findAny()).equalator(LONG_VALID_ANSWER).exercise();
        if (or.isPresent()) {
            long r = or.getAsLong();
            PrimitiveIterator.OfLong it = fs.apply(data.stream()).iterator();
            boolean contained = false;
            while (!contained && it.hasNext()) {
                contained = r == it.nextLong();
            }
            assertTrue(contained);
        }
        else {
            assertFalse(fs.apply(data.stream()).iterator().hasNext());
        }
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleStream(String name, TestData.OfDouble data) {
        exerciseDoubleStream(data, s -> s);
        exerciseDoubleStream(data, s -> s.filter(dpTrue));
        exerciseDoubleStream(data, s -> s.filter(dpEven));
        exerciseDoubleStream(data, s -> s.filter(dpFalse));
    }

    void exerciseDoubleStream(TestData.OfDouble data, Function<DoubleStream, DoubleStream> fs) {
        OptionalDouble or = withData(data).terminal(fs, s -> s.findAny()).equalator(DOUBLE_VALID_ANSWER).exercise();
        if (or.isPresent()) {
            double r = or.getAsDouble();
            PrimitiveIterator.OfDouble it = fs.apply(data.stream()).iterator();
            boolean contained = false;
            while (!contained && it.hasNext()) {
                contained = r == it.nextDouble();
            }
            assertTrue(contained);
        }
        else {
            assertFalse(fs.apply(data.stream()).iterator().hasNext());
        }
    }

    static final BiConsumer<Optional<Integer>, Optional<Integer>> VALID_ANSWER = (a, b) -> assertEquals(a.isPresent(), b.isPresent());

    static final BiConsumer<OptionalInt, OptionalInt> INT_VALID_ANSWER = (a, b) -> assertEquals(a.isPresent(), b.isPresent());

    static final BiConsumer<OptionalLong, OptionalLong> LONG_VALID_ANSWER = (a, b) -> assertEquals(a.isPresent(), b.isPresent());

    static final BiConsumer<OptionalDouble, OptionalDouble> DOUBLE_VALID_ANSWER = (a, b) -> assertEquals(a.isPresent(), b.isPresent());
}
