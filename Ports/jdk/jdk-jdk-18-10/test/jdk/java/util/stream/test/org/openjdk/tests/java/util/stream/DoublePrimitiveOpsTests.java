/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Random;
import java.util.Spliterator;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @test
 * @bug 8153293
 */
@Test
public class DoublePrimitiveOpsTests {

    // @@@ tests for double are fragile if relying on equality when accumulating and multiplying values

    public void testUnBox() {
        double sum = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0).stream().mapToDouble(i -> i).reduce(0.0, Double::sum);
        assertEquals(sum, 1.0 + 2.0 + 3.0 + 4.0 + 5.0);
    }

    public void testFlags() {
        assertTrue(LongStream.range(1, 10).asDoubleStream().boxed().spliterator()
                      .hasCharacteristics(Spliterator.SORTED));
        assertFalse(DoubleStream.of(1, 10).boxed().spliterator()
                      .hasCharacteristics(Spliterator.SORTED));
    }

    public void testToArray() {
        {
            double[] array =  LongStream.range(1, 10).asDoubleStream().map(i -> i * 2).toArray();
            assertEquals(array, new double[]{2, 4, 6, 8, 10, 12, 14, 16, 18});
        }

        {
            double[] array =  LongStream.range(1, 10).parallel().asDoubleStream().map(i -> i * 2).toArray();
            assertEquals(array, new double[]{2, 4, 6, 8, 10, 12, 14, 16, 18});
        }
    }

    public void testSort() {
        Random r = new Random();

        double[] content = DoubleStream.generate(() -> r.nextDouble()).limit(10).toArray();
        double[] sortedContent = content.clone();
        Arrays.sort(sortedContent);

        {
            double[] array =  Arrays.stream(content).sorted().toArray();
            assertEquals(array, sortedContent);
        }

        {
            double[] array =  Arrays.stream(content).parallel().sorted().toArray();
            assertEquals(array, sortedContent);
        }
    }

    public void testSortDistinct() {
        {
            double[] range = LongStream.range(0, 10).asDoubleStream().toArray();

            assertEquals(LongStream.range(0, 10).asDoubleStream().sorted().distinct().toArray(), range);
            assertEquals(LongStream.range(0, 10).asDoubleStream().parallel().sorted().distinct().toArray(), range);

            double[] data = {5, 3, 1, 1, 5, Double.NaN, 3, 9, Double.POSITIVE_INFINITY,
                             Double.NEGATIVE_INFINITY, 2, 9, 1, 0, 8, Double.NaN, -0.0};
            double[] expected = {Double.NEGATIVE_INFINITY, -0.0, 0, 1, 2, 3, 5, 8, 9,
                                 Double.POSITIVE_INFINITY, Double.NaN};
            assertEquals(DoubleStream.of(data).sorted().distinct().toArray(), expected);
            assertEquals(DoubleStream.of(data).parallel().sorted().distinct().toArray(), expected);
        }
    }

    public void testSortSort() {
        Random r = new Random();

        double[] content = DoubleStream.generate(() -> r.nextDouble()).limit(10).toArray();
        double[] sortedContent = content.clone();
        Arrays.sort(sortedContent);

        {
            double[] array =  Arrays.stream(content).sorted().sorted().toArray();
            assertEquals(array, sortedContent);
        }

        {
            double[] array =  Arrays.stream(content).parallel().sorted().sorted().toArray();
            assertEquals(array, sortedContent);
        }
    }

    public void testLimit() {
        double[] expected = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };

        {
            double[] actual = DoubleStream.iterate(1.0, i -> i + 1.0).limit(9).toArray();
            Assert.assertTrue(Arrays.equals(expected, actual));
        }

        {
            double[] actual = LongStream.range(1, 100).parallel().asDoubleStream().limit(9).toArray();
            Assert.assertTrue(Arrays.equals(expected, actual));
        }
    }
}
