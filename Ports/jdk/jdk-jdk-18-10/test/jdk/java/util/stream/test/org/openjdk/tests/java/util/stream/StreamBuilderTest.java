/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LambdaTestHelpers;
import java.util.stream.LongStream;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.TestData;

import static java.util.stream.Collectors.toList;
import static java.util.stream.ThrowableHelper.checkISE;

@Test
public class StreamBuilderTest extends OpTestCase {

    List<Integer> sizes = Arrays.asList(0, 1, 4, 16, 256,
                                        1023, 1024, 1025,
                                        2047, 2048, 2049,
                                        1024 * 32 - 1, 1024 * 32, 1024 * 32 + 1);

    @DataProvider(name = "sizes")
    public Object[][] createStreamBuilders() {
        return sizes.stream().map(i -> new Object[] { i }).toArray(Object[][]::new);
    }


    @Test
    public void testOfNullableWithNonNull() {
        TestData.OfRef<Integer> data = TestData.Factory.ofSupplier("{1}",
                                                                   () -> Stream.ofNullable(1));

        withData(data).
                stream(s -> s).
                expectedResult(Collections.singletonList(1)).
                exercise();
    }

    @Test
    public void testOfNullableWithNull() {
        TestData.OfRef<Integer> data = TestData.Factory.ofSupplier("{null})",
                                                                   () -> Stream.ofNullable(null));

        withData(data).
                stream(s -> s).
                expectedResult(Collections.emptyList()).
                exercise();
    }

    @Test
    public void testSingleton() {
        TestData.OfRef<Integer> data = TestData.Factory.ofSupplier("{1}",
                                                                   () -> Stream.of(1));

        withData(data).
                stream(s -> s).
                expectedResult(Collections.singletonList(1)).
                exercise();

        withData(data).
                stream(s -> s.map(LambdaTestHelpers.identity())).
                expectedResult(Collections.singletonList(1)).
                exercise();
    }

    @Test(dataProvider = "sizes")
    public void testAfterBuilding(int size) {
        Stream.Builder<Integer> sb = Stream.builder();
        IntStream.range(0, size).boxed().forEach(sb);
        sb.build();

        checkISE(() -> sb.accept(1));
        checkISE(() -> sb.add(1));
        checkISE(() -> sb.build());
    }

    @Test(dataProvider = "sizes", groups = { "serialization-hostile" })
    public void testStreamBuilder(int size) {
        testStreamBuilder(size, (s) -> {
            Stream.Builder<Integer> sb = Stream.builder();
            IntStream.range(0, s).boxed().forEach(sb);
            return sb.build();
        });

        testStreamBuilder(size, (s) -> {
            Stream.Builder<Integer> sb = Stream.builder();
            IntStream.range(0, s).boxed().forEach(i -> {
                Stream.Builder<Integer> _sb = sb.add(i);
                assertTrue(sb == _sb);
            });
            return sb.build();
        });
    }

    private void testStreamBuilder(int size, Function<Integer, Stream<Integer>> supplier) {
        TestData.OfRef<Integer> data = TestData.Factory.ofSupplier(String.format("[0, %d)", size),
                                                                   () -> supplier.apply(size));

        withData(data).
                stream(s -> s).
                expectedResult(IntStream.range(0, size).boxed().collect(toList())).
                exercise();

        withData(data).
                stream(s -> s.map(LambdaTestHelpers.identity())).
                expectedResult(IntStream.range(0, size).boxed().collect(toList())).
                exercise();
    }

    //

    @Test
    public void testIntSingleton() {
        TestData.OfInt data = TestData.Factory.ofIntSupplier("{1}",
                                                             () -> IntStream.of(1));

        withData(data).
                stream(s -> s).
                expectedResult(Collections.singletonList(1)).
                exercise();

        withData(data).
                stream(s -> s.map(i -> i)).
                expectedResult(Collections.singletonList(1)).
                exercise();
    }

    @Test(dataProvider = "sizes")
    public void testIntAfterBuilding(int size) {
        IntStream.Builder sb = IntStream.builder();
        IntStream.range(0, size).forEach(sb);
        sb.build();

        checkISE(() -> sb.accept(1));
        checkISE(() -> sb.add(1));
        checkISE(() -> sb.build());
    }

    @Test(dataProvider = "sizes", groups = { "serialization-hostile" })
    public void testIntStreamBuilder(int size) {
        testIntStreamBuilder(size, (s) -> {
            IntStream.Builder sb = IntStream.builder();
            IntStream.range(0, s).forEach(sb);
            return sb.build();
        });

        testIntStreamBuilder(size, (s) -> {
            IntStream.Builder sb = IntStream.builder();
            IntStream.range(0, s).forEach(i -> {
                IntStream.Builder _sb = sb.add(i);
                assertTrue(sb == _sb);
            });
            return sb.build();
        });
    }

    private void testIntStreamBuilder(int size, Function<Integer, IntStream> supplier) {
        TestData.OfInt data = TestData.Factory.ofIntSupplier(String.format("[0, %d)", size),
                                                             () -> supplier.apply(size));

        withData(data).
                stream(s -> s).
                expectedResult(IntStream.range(0, size).toArray()).
                exercise();

        withData(data).
                stream(s -> s.map(i -> i)).
                expectedResult(IntStream.range(0, size).toArray()).
                exercise();
    }

    //

    @Test
    public void testLongSingleton() {
        TestData.OfLong data = TestData.Factory.ofLongSupplier("{1}",
                                                               () -> LongStream.of(1));

        withData(data).
                stream(s -> s).
                expectedResult(Collections.singletonList(1L)).
                exercise();

        withData(data).
                stream(s -> s.map(i -> i)).
                expectedResult(Collections.singletonList(1L)).
                exercise();
    }

    @Test(dataProvider = "sizes")
    public void testLongAfterBuilding(int size) {
        LongStream.Builder sb = LongStream.builder();
        LongStream.range(0, size).forEach(sb);
        sb.build();

        checkISE(() -> sb.accept(1));
        checkISE(() -> sb.add(1));
        checkISE(() -> sb.build());
    }

    @Test(dataProvider = "sizes", groups = { "serialization-hostile" })
    public void testLongStreamBuilder(int size) {
        testLongStreamBuilder(size, (s) -> {
            LongStream.Builder sb = LongStream.builder();
            LongStream.range(0, s).forEach(sb);
            return sb.build();
        });

        testLongStreamBuilder(size, (s) -> {
            LongStream.Builder sb = LongStream.builder();
            LongStream.range(0, s).forEach(i -> {
                LongStream.Builder _sb = sb.add(i);
                assertTrue(sb == _sb);
            });
            return sb.build();
        });
    }

    private void testLongStreamBuilder(int size, Function<Integer, LongStream> supplier) {
        TestData.OfLong data = TestData.Factory.ofLongSupplier(String.format("[0, %d)", size),
                                                               () -> supplier.apply(size));

        withData(data).
                stream(s -> s).
                expectedResult(LongStream.range(0, size).toArray()).
                exercise();

        withData(data).
                stream(s -> s.map(i -> i)).
                expectedResult(LongStream.range(0, size).toArray()).
                exercise();
    }

    //

    @Test
    public void testDoubleSingleton() {
        TestData.OfDouble data = TestData.Factory.ofDoubleSupplier("{1}", () -> DoubleStream.of(1));

        withData(data).
                stream(s -> s).
                expectedResult(Collections.singletonList(1.0)).
                exercise();

        withData(data).
                stream(s -> s.map(i -> i)).
                expectedResult(Collections.singletonList(1.0)).
                exercise();
    }

    @Test(dataProvider = "sizes")
    public void testDoubleAfterBuilding(int size) {
        DoubleStream.Builder sb = DoubleStream.builder();
        IntStream.range(0, size).asDoubleStream().forEach(sb);
        sb.build();

        checkISE(() -> sb.accept(1));
        checkISE(() -> sb.add(1));
        checkISE(() -> sb.build());
    }

    @Test(dataProvider = "sizes", groups = { "serialization-hostile" })
    public void testDoubleStreamBuilder(int size) {
        testDoubleStreamBuilder(size, (s) -> {
            DoubleStream.Builder sb = DoubleStream.builder();
            IntStream.range(0, s).asDoubleStream().forEach(sb);
            return sb.build();
        });

        testDoubleStreamBuilder(size, (s) -> {
            DoubleStream.Builder sb = DoubleStream.builder();
            IntStream.range(0, s).asDoubleStream().forEach(i -> {
                DoubleStream.Builder _sb = sb.add(i);
                assertTrue(sb == _sb);
            });
            return sb.build();
        });
    }

    private void testDoubleStreamBuilder(int size, Function<Integer, DoubleStream> supplier) {
        TestData.OfDouble data = TestData.Factory.ofDoubleSupplier(String.format("[0, %d)", size),
                                                                   () -> supplier.apply(size));

        withData(data).
                stream(s -> s).
                expectedResult(IntStream.range(0, size).asDoubleStream().toArray()).
                exercise();

        withData(data).
                stream(s -> s.map(i -> i)).
                expectedResult(IntStream.range(0, size).asDoubleStream().toArray()).
                exercise();
    }

}
