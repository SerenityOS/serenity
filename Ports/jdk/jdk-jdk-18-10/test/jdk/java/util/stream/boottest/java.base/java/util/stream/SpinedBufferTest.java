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
package java.util.stream;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

@Test
public class SpinedBufferTest {

    // Create sizes around the boundary of spines
    static List<Integer> sizes;
    static {
        try {
            sizes = IntStream.range(0, 15)
                             .map(i -> 1 << i)
                             .flatMap(i -> Arrays.stream(new int[] { i-2, i-1, i, i+1, i+2 }))
                             .filter(i -> i >= 0)
                             .boxed()
                             .distinct()
                             .collect(Collectors.toList());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final int TEST_SIZE = 5000;

    // SpinedBuffer

    @DataProvider(name = "SpinedBuffer")
    public Object[][] createSpinedBuffer() {
        List<Object[]> params = new ArrayList<>();

        for (int size : sizes) {
            int[] array = IntStream.range(0, size).toArray();

            SpinedBuffer<Integer> sb = new SpinedBuffer<>();
            Arrays.stream(array).boxed().forEach(sb);
            params.add(new Object[]{array, sb});

            sb = new SpinedBuffer<>(size / 2);
            Arrays.stream(array).boxed().forEach(sb);
            params.add(new Object[]{array, sb});

            sb = new SpinedBuffer<>(size);
            Arrays.stream(array).boxed().forEach(sb);
            params.add(new Object[]{array, sb});

            sb = new SpinedBuffer<>(size * 2);
            Arrays.stream(array).boxed().forEach(sb);
            params.add(new Object[]{array, sb});
        }

        return params.toArray(new Object[0][]);
    }

    @Test(dataProvider = "SpinedBuffer")
    public void testSpliterator(int[] array, SpinedBuffer<Integer> sb) {
        assertEquals(sb.count(), array.length);
        assertEquals(sb.count(), sb.spliterator().getExactSizeIfKnown());

        SpliteratorTestHelper.testSpliterator(sb::spliterator);
    }

    @Test(dataProvider = "SpinedBuffer", groups = { "serialization-hostile" })
    public void testLastSplit(int[] array, SpinedBuffer<Integer> sb) {
        Spliterator<Integer> spliterator = sb.spliterator();
        Spliterator<Integer> split = spliterator.trySplit();
        long splitSizes = (split == null) ? 0 : split.getExactSizeIfKnown();
        long lastSplitSize = spliterator.getExactSizeIfKnown();
        splitSizes += lastSplitSize;

        assertEquals(splitSizes, array.length);

        List<Integer> contentOfLastSplit = new ArrayList<>();
        spliterator.forEachRemaining(contentOfLastSplit::add);

        assertEquals(contentOfLastSplit.size(), lastSplitSize);

        List<Integer> end = Arrays.stream(array)
                .boxed()
                .skip(array.length - lastSplitSize)
                .collect(Collectors.toList());
        assertEquals(contentOfLastSplit, end);
    }

    @Test(groups = { "serialization-hostile" })
    public void testSpinedBuffer() {
        List<Integer> list1 = new ArrayList<>();
        List<Integer> list2 = new ArrayList<>();
        SpinedBuffer<Integer> sb = new SpinedBuffer<>();
        for (int i = 0; i < TEST_SIZE; i++) {
            list1.add(i);
            sb.accept(i);
        }
        Iterator<Integer> it = sb.iterator();
        for (int i = 0; i < TEST_SIZE; i++)
            list2.add(it.next());
        assertFalse(it.hasNext());
        assertEquals(list1, list2);

        for (int i = 0; i < TEST_SIZE; i++)
            assertEquals(sb.get(i), (Integer) i, Integer.toString(i));

        list2.clear();
        sb.forEach(list2::add);
        assertEquals(list1, list2);
        Integer[] array = sb.asArray(LambdaTestHelpers.integerArrayGenerator);
        list2.clear();
        for (Integer i : array)
            list2.add(i);
        assertEquals(list1, list2);
    }

    // IntSpinedBuffer

    @DataProvider(name = "IntSpinedBuffer")
    public Object[][] createIntSpinedBuffer() {
        List<Object[]> params = new ArrayList<>();

        for (int size : sizes) {
            int[] array = IntStream.range(0, size).toArray();
            SpinedBuffer.OfInt sb = new SpinedBuffer.OfInt();
            Arrays.stream(array).forEach(sb);

            params.add(new Object[]{array, sb});
        }

        return params.toArray(new Object[0][]);
    }

    @Test(dataProvider = "IntSpinedBuffer")
    public void testIntSpliterator(int[] array, SpinedBuffer.OfInt sb) {
        assertEquals(sb.count(), array.length);
        assertEquals(sb.count(), sb.spliterator().getExactSizeIfKnown());

        SpliteratorTestHelper.testIntSpliterator(sb::spliterator);
    }

    @Test(dataProvider = "IntSpinedBuffer", groups = { "serialization-hostile" })
    public void testIntLastSplit(int[] array, SpinedBuffer.OfInt sb) {
        Spliterator.OfInt spliterator = sb.spliterator();
        Spliterator.OfInt split = spliterator.trySplit();
        long splitSizes = (split == null) ? 0 : split.getExactSizeIfKnown();
        long lastSplitSize = spliterator.getExactSizeIfKnown();
        splitSizes += lastSplitSize;

        assertEquals(splitSizes, array.length);

        List<Integer> contentOfLastSplit = new ArrayList<>();
        spliterator.forEachRemaining((IntConsumer) contentOfLastSplit::add);

        assertEquals(contentOfLastSplit.size(), lastSplitSize);

        List<Integer> end = Arrays.stream(array)
                .boxed()
                .skip(array.length - lastSplitSize)
                .collect(Collectors.toList());
        assertEquals(contentOfLastSplit, end);
    }

    @Test(groups = { "serialization-hostile" })
    public void testIntSpinedBuffer() {
        List<Integer> list1 = new ArrayList<>();
        List<Integer> list2 = new ArrayList<>();
        SpinedBuffer.OfInt sb = new SpinedBuffer.OfInt();
        for (int i = 0; i < TEST_SIZE; i++) {
            list1.add(i);
            sb.accept(i);
        }
        PrimitiveIterator.OfInt it = sb.iterator();
        for (int i = 0; i < TEST_SIZE; i++)
            list2.add(it.nextInt());
        assertFalse(it.hasNext());
        assertEquals(list1, list2);

        for (int i = 0; i < TEST_SIZE; i++)
            assertEquals(sb.get(i), i, Integer.toString(i));

        list2.clear();
        sb.forEach((int i) -> list2.add(i));
        assertEquals(list1, list2);
        int[] array = sb.asPrimitiveArray();
        list2.clear();
        for (int i : array)
            list2.add(i);
        assertEquals(list1, list2);
    }

    // LongSpinedBuffer

    @DataProvider(name = "LongSpinedBuffer")
    public Object[][] createLongSpinedBuffer() {
        List<Object[]> params = new ArrayList<>();

        for (int size : sizes) {
            long[] array = LongStream.range(0, size).toArray();
            SpinedBuffer.OfLong sb = new SpinedBuffer.OfLong();
            Arrays.stream(array).forEach(sb);

            params.add(new Object[]{array, sb});
        }

        return params.toArray(new Object[0][]);
    }

    @Test(dataProvider = "LongSpinedBuffer")
    public void testLongSpliterator(long[] array, SpinedBuffer.OfLong sb) {
        assertEquals(sb.count(), array.length);
        assertEquals(sb.count(), sb.spliterator().getExactSizeIfKnown());

        SpliteratorTestHelper.testLongSpliterator(sb::spliterator);
    }

    @Test(dataProvider = "LongSpinedBuffer", groups = { "serialization-hostile" })
    public void testLongLastSplit(long[] array, SpinedBuffer.OfLong sb) {
        Spliterator.OfLong spliterator = sb.spliterator();
        Spliterator.OfLong split = spliterator.trySplit();
        long splitSizes = (split == null) ? 0 : split.getExactSizeIfKnown();
        long lastSplitSize = spliterator.getExactSizeIfKnown();
        splitSizes += lastSplitSize;

        assertEquals(splitSizes, array.length);

        List<Long> contentOfLastSplit = new ArrayList<>();
        spliterator.forEachRemaining((LongConsumer) contentOfLastSplit::add);

        assertEquals(contentOfLastSplit.size(), lastSplitSize);

        List<Long> end = Arrays.stream(array)
                .boxed()
                .skip(array.length - lastSplitSize)
                .collect(Collectors.toList());
        assertEquals(contentOfLastSplit, end);
    }

    @Test(groups = { "serialization-hostile" })
    public void testLongSpinedBuffer() {
        List<Long> list1 = new ArrayList<>();
        List<Long> list2 = new ArrayList<>();
        SpinedBuffer.OfLong sb = new SpinedBuffer.OfLong();
        for (long i = 0; i < TEST_SIZE; i++) {
            list1.add(i);
            sb.accept(i);
        }
        PrimitiveIterator.OfLong it = sb.iterator();
        for (int i = 0; i < TEST_SIZE; i++)
            list2.add(it.nextLong());
        assertFalse(it.hasNext());
        assertEquals(list1, list2);

        for (int i = 0; i < TEST_SIZE; i++)
            assertEquals(sb.get(i), i, Long.toString(i));

        list2.clear();
        sb.forEach((long i) -> list2.add(i));
        assertEquals(list1, list2);
        long[] array = sb.asPrimitiveArray();
        list2.clear();
        for (long i : array)
            list2.add(i);
        assertEquals(list1, list2);
    }

    // DoubleSpinedBuffer

    @DataProvider(name = "DoubleSpinedBuffer")
    public Object[][] createDoubleSpinedBuffer() {
        List<Object[]> params = new ArrayList<>();

        for (int size : sizes) {
            // @@@ replace with double range when implemented
            double[] array = LongStream.range(0, size).asDoubleStream().toArray();
            SpinedBuffer.OfDouble sb = new SpinedBuffer.OfDouble();
            Arrays.stream(array).forEach(sb);

            params.add(new Object[]{array, sb});
        }

        return params.toArray(new Object[0][]);
    }

    @Test(dataProvider = "DoubleSpinedBuffer")
    public void testDoubleSpliterator(double[] array, SpinedBuffer.OfDouble sb) {
        assertEquals(sb.count(), array.length);
        assertEquals(sb.count(), sb.spliterator().getExactSizeIfKnown());

        SpliteratorTestHelper.testDoubleSpliterator(sb::spliterator);
    }

    @Test(dataProvider = "DoubleSpinedBuffer", groups = { "serialization-hostile" })
    public void testLongLastSplit(double[] array, SpinedBuffer.OfDouble sb) {
        Spliterator.OfDouble spliterator = sb.spliterator();
        Spliterator.OfDouble split = spliterator.trySplit();
        long splitSizes = (split == null) ? 0 : split.getExactSizeIfKnown();
        long lastSplitSize = spliterator.getExactSizeIfKnown();
        splitSizes += lastSplitSize;

        assertEquals(splitSizes, array.length);

        List<Double> contentOfLastSplit = new ArrayList<>();
        spliterator.forEachRemaining((DoubleConsumer) contentOfLastSplit::add);

        assertEquals(contentOfLastSplit.size(), lastSplitSize);

        List<Double> end = Arrays.stream(array)
                .boxed()
                .skip(array.length - lastSplitSize)
                .collect(Collectors.toList());
        assertEquals(contentOfLastSplit, end);
    }

    @Test(groups = { "serialization-hostile" })
    public void testDoubleSpinedBuffer() {
        List<Double> list1 = new ArrayList<>();
        List<Double> list2 = new ArrayList<>();
        SpinedBuffer.OfDouble sb = new SpinedBuffer.OfDouble();
        for (long i = 0; i < TEST_SIZE; i++) {
            list1.add((double) i);
            sb.accept((double) i);
        }
        PrimitiveIterator.OfDouble it = sb.iterator();
        for (int i = 0; i < TEST_SIZE; i++)
            list2.add(it.nextDouble());
        assertFalse(it.hasNext());
        assertEquals(list1, list2);

        for (int i = 0; i < TEST_SIZE; i++)
            assertEquals(sb.get(i), (double) i, Double.toString(i));

        list2.clear();
        sb.forEach((double i) -> list2.add(i));
        assertEquals(list1, list2);
        double[] array = sb.asPrimitiveArray();
        list2.clear();
        for (double i : array)
            list2.add(i);
        assertEquals(list1, list2);
    }
}
