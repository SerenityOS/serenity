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

import java.util.Spliterator;
import java.util.stream.BaseStream;
import java.util.stream.OpTestCase;
import java.util.stream.StreamTestDataProvider;

import org.testng.annotations.Test;

import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.DoubleStream;
import java.util.stream.TestData;

import static java.util.stream.LambdaTestHelpers.*;
import static org.testng.Assert.assertEquals;

/**
 * @test
 * @bug 8021863
 */
public class ConcatOpTest extends OpTestCase {

    // Sanity to make sure all type of stream source works
    @Test(dataProvider = "StreamTestData<Integer>", dataProviderClass = StreamTestDataProvider.class)
    public void testOps(String name, TestData.OfRef<Integer> data) {
        exerciseOpsInt(data,
                       s -> Stream.concat(s, data.stream()),
                       s -> IntStream.concat(s, data.stream().mapToInt(Integer::intValue)),
                       s -> LongStream.concat(s, data.stream().mapToLong(Integer::longValue)),
                       s -> DoubleStream.concat(s, data.stream().mapToDouble(Integer::doubleValue)));
    }

    public void testSize() {
        assertSized(Stream.concat(
                LongStream.range(0, Long.MAX_VALUE / 2).boxed(),
                LongStream.range(0, Long.MAX_VALUE / 2).boxed()));

        assertUnsized(Stream.concat(
                LongStream.range(0, Long.MAX_VALUE).boxed(),
                LongStream.range(0, Long.MAX_VALUE).boxed()));

        assertUnsized(Stream.concat(
                LongStream.range(0, Long.MAX_VALUE).boxed(),
                Stream.iterate(0, i -> i + 1)));

        assertUnsized(Stream.concat(
                Stream.iterate(0, i -> i + 1),
                LongStream.range(0, Long.MAX_VALUE).boxed()));
    }

    public void testLongSize() {
        assertSized(LongStream.concat(
                LongStream.range(0, Long.MAX_VALUE / 2),
                LongStream.range(0, Long.MAX_VALUE / 2)));

        assertUnsized(LongStream.concat(
                LongStream.range(0, Long.MAX_VALUE),
                LongStream.range(0, Long.MAX_VALUE)));

        assertUnsized(LongStream.concat(
                LongStream.range(0, Long.MAX_VALUE),
                LongStream.iterate(0, i -> i + 1)));

        assertUnsized(LongStream.concat(
                LongStream.iterate(0, i -> i + 1),
                LongStream.range(0, Long.MAX_VALUE)));
    }

    public void testIntSize() {
        assertSized(IntStream.concat(
                IntStream.range(0, Integer.MAX_VALUE),
                IntStream.range(0, Integer.MAX_VALUE)));

        assertUnsized(IntStream.concat(
                LongStream.range(0, Long.MAX_VALUE).mapToInt(i -> (int) i),
                LongStream.range(0, Long.MAX_VALUE).mapToInt(i -> (int) i)));

        assertUnsized(IntStream.concat(
                LongStream.range(0, Long.MAX_VALUE).mapToInt(i -> (int) i),
                IntStream.iterate(0, i -> i + 1)));

        assertUnsized(IntStream.concat(
                IntStream.iterate(0, i -> i + 1),
                LongStream.range(0, Long.MAX_VALUE).mapToInt(i -> (int) i)));
    }

    public void testDoubleSize() {
        assertSized(DoubleStream.concat(
                IntStream.range(0, Integer.MAX_VALUE).mapToDouble(i -> i),
                IntStream.range(0, Integer.MAX_VALUE).mapToDouble(i -> i)));

        assertUnsized(DoubleStream.concat(
                LongStream.range(0, Long.MAX_VALUE).mapToDouble(i -> i),
                LongStream.range(0, Long.MAX_VALUE).mapToDouble(i -> i)));

        assertUnsized(DoubleStream.concat(
                LongStream.range(0, Long.MAX_VALUE).mapToDouble(i -> i),
                DoubleStream.iterate(0, i -> i + 1)));

        assertUnsized(DoubleStream.concat(
                DoubleStream.iterate(0, i -> i + 1),
                LongStream.range(0, Long.MAX_VALUE).mapToDouble(i -> i)));
    }

    void assertUnsized(BaseStream<?, ?> s) {
        Spliterator<?> sp = s.spliterator();

        assertFalse(sp.hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED));
        assertEquals(sp.estimateSize(), Long.MAX_VALUE);
    }

    void assertSized(BaseStream<?, ?> s) {
        Spliterator<?> sp = s.spliterator();

        assertTrue(sp.hasCharacteristics(Spliterator.SIZED | Spliterator.SUBSIZED));
        assertTrue(sp.estimateSize() < Long.MAX_VALUE);
    }
}
