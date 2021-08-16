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
import java.util.stream.*;

import org.testng.annotations.Test;

import java.util.function.Function;

import static java.util.stream.LambdaTestHelpers.*;


/**
 * FindFirstOpTest
 */
@Test
public class FindFirstOpTest extends OpTestCase {

    public void testFindFirst() {
        assertFalse(Collections.emptySet().stream().findFirst().isPresent(), "no result");
        assertFalse(countTo(10).stream().filter(x -> x > 10).findFirst().isPresent(), "no result");

        exerciseOps(countTo(1000), s -> Arrays.asList(new Integer[]{s.filter(pEven).findFirst().get()}).stream(), Arrays.asList(2));
        exerciseOps(countTo(1000), s -> Arrays.asList(new Integer[]{s.findFirst().get()}).stream(), Arrays.asList(1));
        exerciseOps(countTo(1000), s -> Arrays.asList(new Integer[]{s.filter(e -> e == 499).findFirst().get()}).stream(), Arrays.asList(499));
        exerciseOps(countTo(1000), s -> Arrays.asList(new Integer[]{s.filter(e -> e == 999).findFirst().get()}).stream(), Arrays.asList(999));
        exerciseOps(countTo(0), s -> Arrays.asList(new Integer[]{s.findFirst().orElse(-1)}).stream(), Arrays.asList(-1));
        exerciseOps(countTo(1000), s -> Arrays.asList(new Integer[]{s.filter(e -> e == 1499).findFirst().orElse(-1)}).stream(), Arrays.asList(-1));
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testStream(String name, TestData.OfRef<Integer> data) {
        exerciseStream(data, s -> s);
        exerciseStream(data, s -> s.filter(pTrue));
        exerciseStream(data, s -> s.filter(pFalse));
        exerciseStream(data, s -> s.filter(pEven));
    }

    void exerciseStream(TestData.OfRef<Integer> data, Function<Stream<Integer>, Stream<Integer>> fs) {
        Iterator<Integer> i = fs.apply(data.stream()).iterator();
        Optional<Integer> expected = i.hasNext() ? Optional.of(i.next()) : Optional.empty();
        withData(data).terminal(fs, s -> s.findFirst())
                      .expectedResult(expected)
                      .resultAsserter((act, exp, ord, par) -> {
                          if (par & !ord) {
                              assertContains(act, fs.apply(data.stream()).iterator());
                          }
                          else {
                              assertEquals(act, exp);
                          }
                      })
                      .exercise();
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntStream(String name, TestData.OfInt data) {
        exerciseIntStream(data, s -> s);
        exerciseIntStream(data, s -> s.filter(ipTrue));
        exerciseIntStream(data, s -> s.filter(ipFalse));
        exerciseIntStream(data, s -> s.filter(ipEven));
    }

    void exerciseIntStream(TestData.OfInt data, Function<IntStream, IntStream> fs) {
        OptionalInt r = exerciseTerminalOps(data, fs, s -> s.findFirst());
        if (r.isPresent()) {
            PrimitiveIterator.OfInt i = fs.apply(data.stream()).iterator();
            assertTrue(i.hasNext());
            assertEquals(i.nextInt(), r.getAsInt());
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
        OptionalLong r = exerciseTerminalOps(data, fs, s -> s.findFirst());
        if (r.isPresent()) {
            PrimitiveIterator.OfLong i = fs.apply(data.stream()).iterator();
            assertTrue(i.hasNext());
            assertEquals(i.nextLong(), r.getAsLong());
        }
        else {
            assertFalse(fs.apply(data.stream()).iterator().hasNext());
        }
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleStream(String name, TestData.OfDouble data) {
        exerciseDoubleStream(data, s -> s);
        exerciseDoubleStream(data, s -> s.filter(dpTrue));
        exerciseDoubleStream(data, s -> s.filter(dpFalse));
        exerciseDoubleStream(data, s -> s.filter(dpEven));
    }

    void exerciseDoubleStream(TestData.OfDouble data, Function<DoubleStream, DoubleStream> fs) {
        OptionalDouble r = exerciseTerminalOps(data, fs, s -> s.findFirst());
        if (r.isPresent()) {
            PrimitiveIterator.OfDouble i = fs.apply(data.stream()).iterator();
            assertTrue(i.hasNext());
            assertEquals(i.nextDouble(), r.getAsDouble());
        }
        else {
            assertFalse(fs.apply(data.stream()).iterator().hasNext());
        }
    }
}
