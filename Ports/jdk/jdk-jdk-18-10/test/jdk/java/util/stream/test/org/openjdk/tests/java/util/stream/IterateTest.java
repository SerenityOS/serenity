/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8072727
 */

package org.openjdk.tests.java.util.stream;

import java.util.List;
import java.util.Objects;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.TestData;
import java.util.stream.TestData.Factory;

import static java.util.stream.ThrowableHelper.checkNPE;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class IterateTest extends OpTestCase {

    @DataProvider(name = "IterateStreamsData")
    public static Object[][] makeIterateStreamsTestData() {
        Object[][] data = {
            {List.of(),
                Factory.ofSupplier("ref.empty", () -> Stream.iterate(1, x -> x < 0, x -> x * 2))},
            {List.of(1),
                Factory.ofSupplier("ref.one", () -> Stream.iterate(1, x -> x < 2, x -> x * 2))},
            {List.of(1, 2, 4, 8, 16, 32, 64, 128, 256, 512),
                Factory.ofSupplier("ref.ten", () -> Stream.iterate(1, x -> x < 1000, x -> x * 2))},
            {List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0),
                Factory.ofSupplier("ref.nullCheck", () -> Stream.iterate(10, Objects::nonNull, x -> x > 0 ? x - 1 : null))},
            {List.of(),
                Factory.ofIntSupplier("int.empty", () -> IntStream.iterate(1, x -> x < 0, x -> x + 1))},
            {List.of(1),
                Factory.ofIntSupplier("int.one", () -> IntStream.iterate(1, x -> x < 2, x -> x + 1))},
            {List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                Factory.ofIntSupplier("int.ten", () -> IntStream.iterate(1, x -> x <= 10, x -> x + 1))},
            {List.of(5, 4, 3, 2, 1),
                Factory.ofIntSupplier("int.divZero", () -> IntStream.iterate(5, x -> x != 0, x -> x - 1/x/2 - 1))},
            {List.of(),
                Factory.ofLongSupplier("long.empty", () -> LongStream.iterate(1L, x -> x < 0, x -> x + 1))},
            {List.of(1L),
                Factory.ofLongSupplier("long.one", () -> LongStream.iterate(1L, x -> x < 2, x -> x + 1))},
            {List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L),
                Factory.ofLongSupplier("long.ten", () -> LongStream.iterate(1L, x -> x <= 10, x -> x + 1))},
            {List.of(),
                Factory.ofDoubleSupplier("double.empty", () -> DoubleStream.iterate(1.0, x -> x < 0, x -> x + 1))},
            {List.of(1.0),
                Factory.ofDoubleSupplier("double.one", () -> DoubleStream.iterate(1.0, x -> x < 2, x -> x + 1))},
            {List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0),
                Factory.ofDoubleSupplier("double.ten", () -> DoubleStream.iterate(1.0, x -> x <= 10, x -> x + 1))}
        };
        return data;
    }

    @Test(dataProvider = "IterateStreamsData")
    public <T> void testIterate(List<T> expected, TestData<T, ?> data) {
        withData(data).stream(s -> s).expectedResult(expected).exercise();
    }

    @Test
    public void testNPE() {
        checkNPE(() -> Stream.iterate("", null, x -> x + "a"));
        checkNPE(() -> Stream.iterate("", String::isEmpty, null));
        checkNPE(() -> IntStream.iterate(0, null, x -> x + 1));
        checkNPE(() -> IntStream.iterate(0, x -> x < 10, null));
        checkNPE(() -> LongStream.iterate(0, null, x -> x + 1));
        checkNPE(() -> LongStream.iterate(0, x -> x < 10, null));
        checkNPE(() -> DoubleStream.iterate(0, null, x -> x + 1));
        checkNPE(() -> DoubleStream.iterate(0, x -> x < 10, null));
    }
}
