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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @test
 * @bug 8153293
 */
@Test
public class IntPrimitiveOpsTests {

    public void testSum() {
        long sum = IntStream.range(1, 10).filter(i -> i % 2 == 0).sum();
        assertEquals(sum, 20);
    }

    public void testMap() {
        long sum = IntStream.range(1, 10).filter(i -> i % 2 == 0).map(i -> i * 2).sum();
        assertEquals(sum, 40);
    }

    public void testParSum() {
        long sum = IntStream.range(1, 10).parallel().filter(i -> i % 2 == 0).sum();
        assertEquals(sum, 20);
    }

    @Test(groups = { "serialization-hostile" })
    public void testTee() {
        int[] teeSum = new int[1];
        long sum = IntStream.range(1, 10).filter(i -> i % 2 == 0).peek(i -> { teeSum[0] = teeSum[0] + i; }).sum();
        assertEquals(teeSum[0], sum);
    }

    @Test(groups = { "serialization-hostile" })
    public void testForEach() {
        int[] sum = new int[1];
        IntStream.range(1, 10).filter(i -> i % 2 == 0).forEach(i -> { sum[0] = sum[0] + i; });
        assertEquals(sum[0], 20);
    }

    @Test(groups = { "serialization-hostile" })
    public void testParForEach() {
        AtomicInteger ai = new AtomicInteger(0);
        IntStream.range(1, 10).parallel().filter(i -> i % 2 == 0).forEach(ai::addAndGet);
        assertEquals(ai.get(), 20);
    }

    public void testBox() {
        List<Integer> l = IntStream.range(1, 10).parallel().boxed().collect(Collectors.toList());
        int sum = l.stream().reduce(0, (a, b) -> a + b);
        assertEquals(sum, 45);
    }

    public void testUnBox() {
        long sum = Arrays.asList(1, 2, 3, 4, 5).stream().mapToInt(i -> (int) i).sum();
        assertEquals(sum, 15);
    }

    public void testFlags() {
        assertTrue(IntStream.range(1, 10).boxed().spliterator()
                      .hasCharacteristics(Spliterator.SORTED | Spliterator.DISTINCT));
        assertFalse(IntStream.of(1, 10).boxed().spliterator()
                      .hasCharacteristics(Spliterator.SORTED));
        assertFalse(IntStream.of(1, 10).boxed().spliterator()
                      .hasCharacteristics(Spliterator.DISTINCT));

        assertTrue(IntStream.range(1, 10).asLongStream().spliterator()
                      .hasCharacteristics(Spliterator.SORTED | Spliterator.DISTINCT));
        assertFalse(IntStream.of(1, 10).asLongStream().spliterator()
                      .hasCharacteristics(Spliterator.SORTED));
        assertFalse(IntStream.of(1, 10).asLongStream().spliterator()
                      .hasCharacteristics(Spliterator.DISTINCT));

        assertTrue(IntStream.range(1, 10).asDoubleStream().spliterator()
                      .hasCharacteristics(Spliterator.SORTED | Spliterator.DISTINCT));
        assertFalse(IntStream.of(1, 10).asDoubleStream().spliterator()
                      .hasCharacteristics(Spliterator.SORTED));
        assertFalse(IntStream.of(1, 10).asDoubleStream().spliterator()
                      .hasCharacteristics(Spliterator.DISTINCT));
    }

    public void testToArray() {
        {
            int[] array =  IntStream.range(1, 10).map(i -> i * 2).toArray();
            assertEquals(array, new int[]{2, 4, 6, 8, 10, 12, 14, 16, 18});
        }

        {
            int[] array =  IntStream.range(1, 10).parallel().map(i -> i * 2).toArray();
            assertEquals(array, new int[]{2, 4, 6, 8, 10, 12, 14, 16, 18});
        }
    }

    public void testSort() {
        Random r = new Random();

        int[] content = IntStream.generate(() -> r.nextInt(100)).limit(10).toArray();
        int[] sortedContent = content.clone();
        Arrays.sort(sortedContent);

        {
            int[] array =  Arrays.stream(content).sorted().toArray();
            assertEquals(array, sortedContent);
        }

        {
            int[] array =  Arrays.stream(content).parallel().sorted().toArray();
            assertEquals(array, sortedContent);
        }
    }

    public void testSortDistinct() {
        {
            int[] range = IntStream.range(0, 10).toArray();

            assertEquals(IntStream.range(0, 10).sorted().distinct().toArray(), range);
            assertEquals(IntStream.range(0, 10).parallel().sorted().distinct().toArray(), range);

            int[] data = {5, 3, 1, 1, 5, 3, 9, 2, 9, 1, 0, 8};
            int[] expected = {0, 1, 2, 3, 5, 8, 9};
            assertEquals(IntStream.of(data).sorted().distinct().toArray(), expected);
            assertEquals(IntStream.of(data).parallel().sorted().distinct().toArray(), expected);
        }

        {
            int[] input = new Random().ints(100, -10, 10).map(x -> x+Integer.MAX_VALUE).toArray();
            TreeSet<Long> longs = new TreeSet<>();
            for(int i : input) longs.add((long)i);
            long[] expectedLongs = longs.stream().mapToLong(Long::longValue).toArray();
            assertEquals(IntStream.of(input).sorted().asLongStream().sorted().distinct().toArray(),
                         expectedLongs);

            TreeSet<Double> doubles = new TreeSet<>();
            for(int i : input) doubles.add((double)i);
            double[] expectedDoubles = doubles.stream().mapToDouble(Double::doubleValue).toArray();
            assertEquals(IntStream.of(input).sorted().distinct().asDoubleStream()
                         .sorted().distinct().toArray(), expectedDoubles);
        }
    }

    public void testSortSort() {
        Random r = new Random();

        int[] content = IntStream.generate(() -> r.nextInt(100)).limit(10).toArray();
        int[] sortedContent = content.clone();
        Arrays.sort(sortedContent);

        {
            int[] array =  Arrays.stream(content).sorted().sorted().toArray();
            assertEquals(array, sortedContent);
        }

        {
            int[] array =  Arrays.stream(content).parallel().sorted().sorted().toArray();
            assertEquals(array, sortedContent);
        }
    }

    public void testSequential() {

        int[] expected = IntStream.range(1, 1000).toArray();

        class AssertingConsumer implements IntConsumer {
            private final int[] array;
            int offset;

            AssertingConsumer(int[] array) {
                this.array = array;
            }

            @Override
            public void accept(int value) {
                assertEquals(array[offset++], value);
            }

            public int getCount() { return offset; }
        }

        {
            AssertingConsumer consumer = new AssertingConsumer(expected);
            IntStream.range(1, 1000).sequential().forEach(consumer);
            assertEquals(expected.length, consumer.getCount());
        }

        {
            AssertingConsumer consumer = new AssertingConsumer(expected);
            IntStream.range(1, 1000).parallel().sequential().forEach(consumer);
            assertEquals(expected.length, consumer.getCount());
        }
    }

    public void testLimit() {
        int[] expected = IntStream.range(1, 10).toArray();

        {
            int[] actual = IntStream.iterate(1, i -> i + 1).limit(9).toArray();
            Assert.assertTrue(Arrays.equals(expected, actual));
        }

        {
            int[] actual = IntStream.range(1, 100).parallel().limit(9).toArray();
            Assert.assertTrue(Arrays.equals(expected, actual));
        }
    }

}
