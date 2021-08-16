/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Spliterator;
import java.util.SpliteratorOfIntDataBuilder;
import java.util.SpliteratorTestHelper;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.util.stream.Collectors.toList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

/**
 * @test
 * @summary test BitSet stream
 * @bug 8012645 8076442
 * @requires os.maxMemory >= 2g
 * @library /lib/testlibrary/bootlib
 * @build java.base/java.util.SpliteratorTestHelper
 *        java.base/java.util.SpliteratorOfIntDataBuilder
 * @run testng/othervm -Xms512m -Xmx1024m BitSetStreamTest
 */
public class BitSetStreamTest extends SpliteratorTestHelper {
    static class Fibs implements IntSupplier {
        private int n1 = 0;
        private int n2 = 1;

        static int fibs(int n) {
            Fibs f = new Fibs();
            while (n-- > 0) f.getAsInt();
            return f.getAsInt();
        }

        public int getAsInt() { int s = n1; n1 = n2; n2 = s + n1; return s; }
    }

    @Test
    public void testFibs() {
        Fibs f = new Fibs();
        assertEquals(0, f.getAsInt());
        assertEquals(1, f.getAsInt());
        assertEquals(1, f.getAsInt());
        assertEquals(2, f.getAsInt());
        assertEquals(3, f.getAsInt());
        assertEquals(5, f.getAsInt());
        assertEquals(8, f.getAsInt());
        assertEquals(13, f.getAsInt());
        assertEquals(987, Fibs.fibs(16));
    }


    @DataProvider(name = "cases")
    public static Object[][] produceCases() {
        return new Object[][] {
                { "none", IntStream.empty() },
                { "index 0", IntStream.of(0) },
                { "index 255", IntStream.of(255) },
                { "index 0 and 255", IntStream.of(0, 255) },
                { "index Integer.MAX_VALUE", IntStream.of(Integer.MAX_VALUE) },
                { "index Integer.MAX_VALUE - 1", IntStream.of(Integer.MAX_VALUE - 1) },
                { "index 0 and Integer.MAX_VALUE", IntStream.of(0, Integer.MAX_VALUE) },
                { "every bit", IntStream.range(0, 255) },
                { "step 2", IntStream.range(0, 255).map(f -> f * 2) },
                { "step 3", IntStream.range(0, 255).map(f -> f * 3) },
                { "step 5", IntStream.range(0, 255).map(f -> f * 5) },
                { "step 7", IntStream.range(0, 255).map(f -> f * 7) },
                { "1, 10, 100, 1000", IntStream.of(1, 10, 100, 1000) },
                { "25 fibs", IntStream.generate(new Fibs()).limit(25) }
        };
    }

    @Test(dataProvider = "cases")
    public void testBitsetStream(String name, IntStream data) {
        BitSet bs = data.collect(BitSet::new, BitSet::set, BitSet::or);

        assertEquals(bs.cardinality(), bs.stream().count());

        int[] indexHolder = new int[] { -1 };
        bs.stream().forEach(i -> {
            int ei = indexHolder[0];
            indexHolder[0] = bs.nextSetBit(ei + 1);
            assertEquals(i, indexHolder[0]);
        });

        PrimitiveIterator.OfInt it = bs.stream().iterator();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            assertTrue(it.hasNext());
            assertEquals(it.nextInt(), i);
            if (i == Integer.MAX_VALUE)
                break; // or (i + 1) would overflow
        }
        assertFalse(it.hasNext());
    }

    static Object[][] spliteratorOfIntDataProvider;

    @DataProvider(name = "BitSet.stream.spliterator")
    public static Object[][] spliteratorOfIntDataProvider() {
        if (spliteratorOfIntDataProvider != null) {
            return spliteratorOfIntDataProvider;
        }

        List<Object[]> data = new ArrayList<>();

        Object[][] bitStreamTestcases = new Object[][] {
                { "none", IntStream.empty().toArray() },
                { "index 0", IntStream.of(0).toArray() },
                { "index 255", IntStream.of(255).toArray() },
                { "index 0 and 255", IntStream.of(0, 255).toArray() },
                { "index Integer.MAX_VALUE", IntStream.of(Integer.MAX_VALUE).toArray() },
                { "index Integer.MAX_VALUE - 1", IntStream.of(Integer.MAX_VALUE - 1).toArray() },
                { "index 0 and Integer.MAX_VALUE", IntStream.of(0, Integer.MAX_VALUE).toArray() },
                { "every bit", IntStream.range(0, 255).toArray() },
                { "step 2", IntStream.range(0, 255).map(f -> f * 2).toArray() },
                { "step 3", IntStream.range(0, 255).map(f -> f * 3).toArray() },
                { "step 5", IntStream.range(0, 255).map(f -> f * 5).toArray() },
                { "step 7", IntStream.range(0, 255).map(f -> f * 7).toArray() },
                { "1, 10, 100, 1000", IntStream.of(1, 10, 100, 1000).toArray() },
        };
        for (Object[] tc : bitStreamTestcases) {
            String description = (String)tc[0];
            int[] exp = (int[])tc[1];
            SpliteratorOfIntDataBuilder db = new SpliteratorOfIntDataBuilder(
                    data, IntStream.of(exp).boxed().collect(toList()));

            db.add("BitSet.stream.spliterator() {" + description + "}", () ->
                IntStream.of(exp).collect(BitSet::new, BitSet::set, BitSet::or).
                        stream().spliterator()
            );
        }
        return spliteratorOfIntDataProvider = data.toArray(new Object[0][]);
    }

    @Test(dataProvider = "BitSet.stream.spliterator")
    public void testIntNullPointerException(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        assertThrows(NullPointerException.class, () -> s.get().forEachRemaining((IntConsumer) null));
        assertThrows(NullPointerException.class, () -> s.get().tryAdvance((IntConsumer) null));
    }

    @Test(dataProvider = "BitSet.stream.spliterator")
    public void testIntForEach(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testForEach(exp, s, intBoxingConsumer());
    }

    @Test(dataProvider = "BitSet.stream.spliterator")
    public void testIntTryAdvance(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testTryAdvance(exp, s, intBoxingConsumer());
    }

    @Test(dataProvider = "BitSet.stream.spliterator")
    public void testIntMixedTryAdvanceForEach(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testMixedTryAdvanceForEach(exp, s, intBoxingConsumer());
    }

    @Test(dataProvider = "BitSet.stream.spliterator")
    public void testIntMixedTraverseAndSplit(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testMixedTraverseAndSplit(exp, s, intBoxingConsumer());
    }

    @Test(dataProvider = "BitSet.stream.spliterator")
    public void testIntSplitAfterFullTraversal(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testSplitAfterFullTraversal(s, intBoxingConsumer());
    }

    @Test(dataProvider = "BitSet.stream.spliterator")
    public void testIntSplitOnce(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testSplitOnce(exp, s, intBoxingConsumer());
    }

    @Test(dataProvider = "BitSet.stream.spliterator")
    public void testIntSplitSixDeep(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testSplitSixDeep(exp, s, intBoxingConsumer());
    }

    @Test(dataProvider = "BitSet.stream.spliterator")
    public void testIntSplitUntilNull(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testSplitUntilNull(exp, s, intBoxingConsumer());
    }

    @Test
    public void testRandomStream() {
        final int size = 1024 * 1024;
        final int[] seeds = {
                2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41,
                43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97};
        final byte[] bytes = new byte[size];
        for (int seed : seeds) {
            final Random random = new Random(seed);
            random.nextBytes(bytes);

            BitSet bitSet = BitSet.valueOf(bytes);
            testBitSetContents(bitSet, bitSet.stream().toArray());
            testBitSetContents(bitSet, bitSet.stream().parallel().toArray());
        }
    }

    void testBitSetContents(BitSet bitSet, int[] array) {
        int cardinality = bitSet.cardinality();
        assertEquals(array.length, cardinality);
        int nextSetBit = -1;
        for (int i = 0; i < cardinality; i++) {
            nextSetBit = bitSet.nextSetBit(nextSetBit + 1);
            assertEquals(array[i], nextSetBit);
        }
    }
}
