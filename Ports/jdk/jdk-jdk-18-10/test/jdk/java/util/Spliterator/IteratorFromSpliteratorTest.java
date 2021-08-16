/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterators;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.fail;

/**
 * @test
 * @summary Spliterator.iterator traversing tests
 * @library /lib/testlibrary/bootlib
 * @run testng IteratorFromSpliteratorTest
 * @bug 8267452
 */
public class IteratorFromSpliteratorTest {
    @Test
    public void testIteratorFromSpliterator() {
        List<Integer> input = List.of(1, 2, 3, 4, 5);
        for (int i = 0; i < input.size(); i++) {
            Iterator<Integer> iterator = Spliterators.iterator(input.spliterator());
            List<Integer> result = new ArrayList<>();
            int j = i;
            while (j++ < input.size() && iterator.hasNext()) {
                result.add(iterator.next());
            }
            // While SpliteratorTraversingAndSplittingTest tests some scenarios with Spliterators.iterator
            // it always wraps the resulting iterator into spliterator again, and this limits the use patterns.
            // In particular, calling hasNext() right before forEachRemaining() is not tested.
            // Here we cover such a scenario.
            assertEquals(iterator.hasNext(), result.size() < input.size());
            iterator.forEachRemaining(result::add);
            iterator.forEachRemaining(x -> fail("Should not be called"));
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
            iterator.forEachRemaining(x -> fail("Should not be called"));
            assertEquals(result, input);
        }
    }

    @Test
    public void testIteratorFromSpliteratorInt() {
        int[] input = {1, 2, 3, 4, 5};
        for (int i = 0; i < input.length; i++) {
            PrimitiveIterator.OfInt iterator = Spliterators.iterator(Arrays.spliterator(input));
            List<Integer> result = new ArrayList<>();
            int j = i;
            while (j++ < input.length && iterator.hasNext()) {
                result.add(iterator.nextInt());
            }
            assertEquals(iterator.hasNext(), result.size() < input.length);
            iterator.forEachRemaining((IntConsumer) result::add);
            iterator.forEachRemaining((IntConsumer) (x -> fail("Should not be called")));
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
            iterator.forEachRemaining((IntConsumer) (x -> fail("Should not be called")));
            assertEquals(result.stream().mapToInt(x -> x).toArray(), input);
        }
    }

    @Test
    public void testIteratorFromSpliteratorLong() {
        long[] input = {1, 2, 3, 4, 5};
        for (int i = 0; i < input.length; i++) {
            PrimitiveIterator.OfLong iterator = Spliterators.iterator(Arrays.spliterator(input));
            List<Long> result = new ArrayList<>();
            int j = i;
            while (j++ < input.length && iterator.hasNext()) {
                result.add(iterator.nextLong());
            }
            assertEquals(iterator.hasNext(), result.size() < input.length);
            iterator.forEachRemaining((LongConsumer) result::add);
            iterator.forEachRemaining((LongConsumer) (x -> fail("Should not be called")));
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
            iterator.forEachRemaining((LongConsumer) (x -> fail("Should not be called")));
            assertEquals(result.stream().mapToLong(x -> x).toArray(), input);
        }
    }

    @Test
    public void testIteratorFromSpliteratorDouble() {
        double[] input = {1, 2, 3, 4, 5};
        for (int i = 0; i < input.length; i++) {
            PrimitiveIterator.OfDouble iterator = Spliterators.iterator(Arrays.spliterator(input));
            List<Double> result = new ArrayList<>();
            int j = i;
            while (j++ < input.length && iterator.hasNext()) {
                result.add(iterator.nextDouble());
            }
            assertEquals(iterator.hasNext(), result.size() < input.length);
            iterator.forEachRemaining((DoubleConsumer) result::add);
            iterator.forEachRemaining((DoubleConsumer) (x -> fail("Should not be called")));
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
            iterator.forEachRemaining((DoubleConsumer) (x -> fail("Should not be called")));
            assertEquals(result.stream().mapToDouble(x -> x).toArray(), input);
        }
    }

    @Test
    public void testIteratorFromSpliteratorEmpty() {
        Iterator<?>[] iterators = {
            Spliterators.iterator(Spliterators.emptySpliterator()),
            Spliterators.iterator(Spliterators.emptyIntSpliterator()),
            Spliterators.iterator(Spliterators.emptyLongSpliterator()),
            Spliterators.iterator(Spliterators.emptyDoubleSpliterator())
        };
        for (Iterator<?> iterator : iterators) {
            iterator.forEachRemaining(x -> fail("Should not be called"));
            assertFalse(iterator.hasNext());
            iterator.forEachRemaining(x -> fail("Should not be called"));
            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }
}
