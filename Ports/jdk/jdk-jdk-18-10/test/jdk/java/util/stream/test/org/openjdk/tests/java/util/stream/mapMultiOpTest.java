/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test mapMulti(BiConsumer) and primitive stream operations
 */

package org.openjdk.tests.java.util.stream;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.DefaultMethodStreams;
import java.util.stream.DoubleStream;
import java.util.stream.DoubleStreamTestDataProvider;
import java.util.stream.IntStream;
import java.util.stream.IntStreamTestDataProvider;
import java.util.stream.LongStream;
import java.util.stream.LongStreamTestDataProvider;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.StreamTestDataProvider;
import java.util.stream.TestData;

import static java.util.stream.DefaultMethodStreams.delegateTo;
import static java.util.stream.LambdaTestHelpers.LONG_STRING;
import static java.util.stream.LambdaTestHelpers.assertConcat;
import static java.util.stream.LambdaTestHelpers.assertContents;
import static java.util.stream.LambdaTestHelpers.assertCountSum;
import static java.util.stream.LambdaTestHelpers.countTo;
import static java.util.stream.LambdaTestHelpers.flattenChars;
import static java.util.stream.LambdaTestHelpers.mfId;
import static java.util.stream.LambdaTestHelpers.mfLt;
import static java.util.stream.LambdaTestHelpers.mfNull;
import static java.util.stream.ThrowableHelper.checkNPE;

@Test
public class mapMultiOpTest extends OpTestCase {

    BiConsumer<Integer, Consumer<Integer>> nullConsumer =
            (e, sink) -> mfNull.apply(e).forEach(sink);
    BiConsumer<Integer, Consumer<Integer>> idConsumer =
            (e, sink) -> mfId.apply(e).forEach(sink);
    BiConsumer<Integer, Consumer<Integer>> listConsumer =
            (e, sink) -> mfLt.apply(e).forEach(sink);
    BiConsumer<String, Consumer<Character>> charConsumer =
            (e, sink) -> flattenChars.apply(e).forEach(sink);
    BiConsumer<Integer, Consumer<Integer>> emptyStreamConsumer =
            (e, sink) -> Stream.empty().forEach(i -> sink.accept((Integer) i));
    BiConsumer<Integer, Consumer<Integer>> intRangeConsumer =
            (e, sink) -> IntStream.range(0, e).boxed().forEach(sink);
    BiConsumer<Integer, Consumer<Integer>> rangeConsumerWithLimit =
            (e, sink) -> IntStream.range(0, e).boxed().limit(10).forEach(sink);

    @DataProvider(name = "Stream<Integer>")
    public Object[][] streamProvider() {
        return new Object[][]{
                {Stream.of(0, 1, 2)},
                {DefaultMethodStreams.delegateTo(Stream.of(0, 1, 2))}
        };
    }

    @Test(dataProvider = "Stream<Integer>")
    public void testNullMapper(Stream<Integer> s) {
        checkNPE(() -> s.mapMulti(null));
        checkNPE(() -> s.mapMultiToInt(null));
        checkNPE(() -> s.mapMultiToDouble(null));
        checkNPE(() -> s.mapMultiToLong(null));
    }

    @Test
    public void testMapMulti() {
        String[] stringsArray = {"hello", "there", "", "yada"};
        Stream<String> strings = Arrays.asList(stringsArray).stream();

        assertConcat(strings.mapMulti(charConsumer)
                .iterator(), "hellothereyada");
        assertCountSum((countTo(10).stream().mapMulti(idConsumer)),
                10, 55);
        assertCountSum(countTo(10).stream().mapMulti(nullConsumer),
                0, 0);
        assertCountSum(countTo(3).stream().mapMulti(listConsumer),
                6, 4);

        exerciseOps(TestData.Factory.ofArray("stringsArray",
                stringsArray), s -> s.mapMulti(charConsumer));
        exerciseOps(TestData.Factory.ofArray("LONG_STRING",
                new String[]{LONG_STRING}), s -> s.mapMulti(charConsumer));
    }

    @Test
    public void testDefaultMapMulti() {
        String[] stringsArray = {"hello", "there", "", "yada"};
        Stream<String> strings = Arrays.stream(stringsArray);

        assertConcat(delegateTo(strings)
                .mapMulti(charConsumer).iterator(), "hellothereyada");
        assertCountSum(delegateTo(countTo(10).stream())
                .mapMulti(idConsumer), 10, 55);
        assertCountSum(delegateTo(countTo(10).stream())
                .mapMulti(nullConsumer), 0, 0);
        assertCountSum(delegateTo(countTo(3).stream())
                .mapMulti(listConsumer), 6, 4);

        exerciseOps(TestData.Factory.ofArray("stringsArray",
                stringsArray), s -> delegateTo(s).mapMulti(charConsumer));
        exerciseOps(TestData.Factory.ofArray("LONG_STRING",
                new String[]{LONG_STRING}), s -> delegateTo(s).mapMulti(charConsumer));
    }

    @Test(dataProvider = "StreamTestData<Integer>",
            dataProviderClass = StreamTestDataProvider.class)
    public void testOps(String name, TestData.OfRef<Integer> data) {
        testOps(name, data, s -> s);
        testOps(name, data, s -> delegateTo(s));
    }

    private void testOps(String name,
                         TestData.OfRef<Integer> data,
                         Function<Stream<Integer>, Stream<Integer>> sf) {
        Collection<Integer> result;
        result = exerciseOps(data, s -> sf.apply(s).mapMulti(idConsumer));
        assertEquals(data.size(), result.size());

        result = exerciseOps(data, s -> sf.apply(s).mapMulti(nullConsumer));
        assertEquals(0, result.size());

        result = exerciseOps(data, s -> sf.apply(s).mapMulti(emptyStreamConsumer));
        assertEquals(0, result.size());
    }

    @Test(dataProvider = "StreamTestData<Integer>.small",
            dataProviderClass = StreamTestDataProvider.class)
    public void testOpsX(String name, TestData.OfRef<Integer> data) {
        exerciseOps(data, s -> s.mapMulti(listConsumer));
        exerciseOps(data, s -> s.mapMulti(intRangeConsumer));
        exerciseOps(data, s -> s.mapMulti(rangeConsumerWithLimit));
    }

    @Test(dataProvider = "StreamTestData<Integer>.small",
            dataProviderClass = StreamTestDataProvider.class)
    public void testDefaultOpsX(String name, TestData.OfRef<Integer> data) {
        exerciseOps(data, s -> delegateTo(s).mapMulti(listConsumer));
        exerciseOps(data, s -> delegateTo(s).mapMulti(intRangeConsumer));
        exerciseOps(data, s -> delegateTo(s).mapMulti(rangeConsumerWithLimit));
    }

    // Int

    @DataProvider(name = "IntStream")
    public Object[][] intStreamProvider() {
        return new Object[][]{
                {IntStream.of(0, 1, 2)},
                {DefaultMethodStreams.delegateTo(IntStream.of(0, 1, 2))}
        };
    }

    @Test(dataProvider = "IntStream")
    public void testIntNullMapper(IntStream s) {
        checkNPE(() -> s.mapMulti(null));
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntOps(String name, TestData.OfInt data) {
        testIntOps(name, data, s -> s);
        testIntOps(name, data, s -> delegateTo(s));
    }

    private void testIntOps(String name,
                            TestData.OfInt data,
                            Function<IntStream, IntStream> sf) {
        Collection<Integer> result = exerciseOps(data, s -> sf.apply(s).mapMulti((i, sink) -> IntStream.of(i).forEach(sink)));
        assertEquals(data.size(), result.size());
        assertContents(data, result);

        result = exerciseOps(data, s -> sf.apply(s).boxed().mapMultiToInt((i, sink) -> IntStream.of(i).forEach(sink)));
        assertEquals(data.size(), result.size());
        assertContents(data, result);

        result = exerciseOps(data, s -> sf.apply(s).mapMulti((i, sink) -> IntStream.empty().forEach(sink)));
        assertEquals(0, result.size());
    }

    @Test(dataProvider = "IntStreamTestData.small", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntOpsX(String name, TestData.OfInt data) {
        exerciseOps(data, s -> s.mapMulti((e, sink) -> IntStream.range(0, e).forEach(sink)));
        exerciseOps(data, s -> s.mapMulti((e, sink) -> IntStream.range(0, e).limit(10).forEach(sink)));

        exerciseOps(data, s -> s.boxed().mapMultiToInt((e, sink) -> IntStream.range(0, e).forEach(sink)));
        exerciseOps(data, s -> s.boxed().mapMultiToInt((e, sink) -> IntStream.range(0, e).limit(10).forEach(sink)));
    }

    @Test(dataProvider = "IntStreamTestData.small", dataProviderClass = IntStreamTestDataProvider.class)
    public void testDefaultIntOpsX(String name, TestData.OfInt data) {
        exerciseOps(data, s -> delegateTo(s).mapMulti((e, sink) -> IntStream.range(0, e).forEach(sink)));
        exerciseOps(data, s -> delegateTo(s).mapMulti((e, sink) -> IntStream.range(0, e).limit(10).forEach(sink)));

        exerciseOps(data, s -> delegateTo(s).boxed().mapMultiToInt((e, sink) -> IntStream.range(0, e).forEach(sink)));
        exerciseOps(data, s -> delegateTo(s).boxed().mapMultiToInt((e, sink) -> IntStream.range(0, e).limit(10).forEach(sink)));
    }

    // Double

    @DataProvider(name = "DoubleStream")
    public Object[][] doubleStreamProvider() {
        return new Object[][]{
                {DoubleStream.of(0, 1, 2)},
                {DefaultMethodStreams.delegateTo(DoubleStream.of(0, 1, 2))}
        };
    }

    @Test(dataProvider = "DoubleStream")
    public void testDoubleNullMapper(DoubleStream s) {
        checkNPE(() -> s.mapMulti(null));
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleOps(String name, TestData.OfDouble data) {
        testDoubleOps(name, data, s -> s);
        testDoubleOps(name, data, s -> delegateTo(s));
    }

    private void testDoubleOps(String name,
                               TestData.OfDouble data,
                               Function<DoubleStream, DoubleStream> sf) {
        Collection<Double> result = exerciseOps(data, s -> sf.apply(s).mapMulti((i, sink) -> DoubleStream.of(i).forEach(sink)));
        assertEquals(data.size(), result.size());
        assertContents(data, result);

        result = exerciseOps(data, s -> sf.apply(s).boxed().mapMultiToDouble((i, sink) -> DoubleStream.of(i).forEach(sink)));
        assertEquals(data.size(), result.size());
        assertContents(data, result);

        result = exerciseOps(data, s -> sf.apply(s).mapMulti((i, sink) -> DoubleStream.empty().forEach(sink)));
        assertEquals(0, result.size());
    }

    @Test(dataProvider = "DoubleStreamTestData.small", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleOpsX(String name, TestData.OfDouble data) {
        exerciseOps(data, s -> s.mapMulti((e, sink) -> IntStream.range(0, (int) e).asDoubleStream().forEach(sink)));
        exerciseOps(data, s -> s.mapMulti((e, sink) -> IntStream.range(0, (int) e).limit(10).asDoubleStream().forEach(sink)));
    }

    @Test(dataProvider = "DoubleStreamTestData.small", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDefaultDoubleOpsX(String name, TestData.OfDouble data) {
        exerciseOps(data, s -> delegateTo(s).mapMulti((e, sink) -> IntStream.range(0, (int) e).asDoubleStream().forEach(sink)));
        exerciseOps(data, s -> delegateTo(s).mapMulti((e, sink) -> IntStream.range(0, (int) e).limit(10).asDoubleStream().forEach(sink)));
    }

    // Long

    @DataProvider(name = "LongStream")
    public Object[][] longStreamProvider() {
        return new Object[][]{
                {LongStream.of(0, 1, 2)},
                {DefaultMethodStreams.delegateTo(LongStream.of(0, 1, 2))}
        };
    }

    @Test(dataProvider = "LongStream")
    public void testLongNullMapper(LongStream s) {
        checkNPE(() -> s.mapMulti(null));
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongOps(String name, TestData.OfLong data) {
        testLongOps(name, data, s -> s);
        testLongOps(name, data, s -> delegateTo(s));
    }

    private void testLongOps(String name,
                             TestData.OfLong data,
                             Function<LongStream, LongStream> sf) {
        Collection<Long> result = exerciseOps(data, s -> sf.apply(s).mapMulti((i, sink) -> LongStream.of(i).forEach(sink)));
        assertEquals(data.size(), result.size());
        assertContents(data, result);

        result = exerciseOps(data, s -> sf.apply(s).boxed().mapMultiToLong((i, sink) -> LongStream.of(i).forEach(sink)));
        assertEquals(data.size(), result.size());
        assertContents(data, result);

        result = exerciseOps(data, s -> sf.apply(s).mapMulti((i, sink) -> LongStream.empty().forEach(sink)));
        assertEquals(0, result.size());
    }

    @Test(dataProvider = "LongStreamTestData.small", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongOpsX(String name, TestData.OfLong data) {
        exerciseOps(data, s -> s.mapMulti((e, sink) -> LongStream.range(0, e).forEach(sink)));
        exerciseOps(data, s -> s.mapMulti((e, sink) -> LongStream.range(0, e).limit(10).forEach(sink)));
    }

    @Test(dataProvider = "LongStreamTestData.small", dataProviderClass = LongStreamTestDataProvider.class)
    public void testDefaultLongOpsX(String name, TestData.OfLong data) {
        exerciseOps(data, s -> delegateTo(s).mapMulti((e, sink) -> LongStream.range(0, e).forEach(sink)));
        exerciseOps(data, s -> delegateTo(s).mapMulti((e, sink) -> LongStream.range(0, e).limit(10).forEach(sink)));
    }
}
