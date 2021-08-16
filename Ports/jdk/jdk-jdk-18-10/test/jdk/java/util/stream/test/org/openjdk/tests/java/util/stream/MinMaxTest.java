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

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.*;

import org.testng.annotations.Test;

import static java.util.stream.LambdaTestHelpers.countTo;

/**
 * MinMaxTest
 *
 * @author Brian Goetz
 */
@Test
public class MinMaxTest extends OpTestCase {
    public void testMinMax() {
        assertTrue(!countTo(0).stream().min(Integer::compare).isPresent());
        assertTrue(!countTo(0).stream().max(Integer::compare).isPresent());
        assertEquals(1, (int) countTo(1000).stream().min(Integer::compare).get());
        assertEquals(1000, (int) countTo(1000).stream().max(Integer::compare).get());
    }

    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOps(String name, TestData.OfRef<Integer> data) {
        exerciseTerminalOps(data, s -> s.min(Integer::compare));
        exerciseTerminalOps(data, s -> s.max(Integer::compare));
    }

    public void testIntMinMax() {
        assertEquals(IntStream.empty().min(), OptionalInt.empty());
        assertEquals(IntStream.empty().max(), OptionalInt.empty());
        assertEquals(1, IntStream.range(1, 1001).min().getAsInt());
        assertEquals(1000, IntStream.range(1, 1001).max().getAsInt());
    }

    @Test(dataProvider = "IntStreamTestData", dataProviderClass = IntStreamTestDataProvider.class)
    public void testIntOps(String name, TestData.OfInt data) {
        exerciseTerminalOps(data, s -> s.min());
        exerciseTerminalOps(data, s -> s.max());
    }

    public void testLongMinMax() {
        assertEquals(LongStream.empty().min(), OptionalLong.empty());
        assertEquals(LongStream.empty().max(), OptionalLong.empty());
        assertEquals(1, LongStream.range(1, 1001).min().getAsLong());
        assertEquals(1000, LongStream.range(1, 1001).max().getAsLong());
    }

    @Test(dataProvider = "LongStreamTestData", dataProviderClass = LongStreamTestDataProvider.class)
    public void testLongOps(String name, TestData.OfLong data) {
        exerciseTerminalOps(data, s -> s.min());
        exerciseTerminalOps(data, s -> s.max());
    }

    public void testDoubleMinMax() {
        assertEquals(DoubleStream.empty().min(), OptionalDouble.empty());
        assertEquals(DoubleStream.empty().max(), OptionalDouble.empty());
        assertEquals(1.0, LongStream.range(1, 1001).asDoubleStream().min().getAsDouble());
        assertEquals(1000.0, LongStream.range(1, 1001).asDoubleStream().max().getAsDouble());
    }

    @Test(dataProvider = "DoubleStreamTestData", dataProviderClass = DoubleStreamTestDataProvider.class)
    public void testDoubleOps(String name, TestData.OfDouble data) {
        exerciseTerminalOps(data, s -> s.min());
        exerciseTerminalOps(data, s -> s.max());
    }
}
