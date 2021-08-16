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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;

/*
 * @test
 * @bug 8023463
 * @summary Test the case where a bin is treeified and vice verser
 * @run testng MapBinToFromTreeTest
 */

@Test
public class MapBinToFromTreeTest {

    // Initial capacity of map
    // Should be >= the map capacity for treeifiying, see HashMap/ConcurrentMap.MIN_TREEIFY_CAPACITY
    static final int INITIAL_CAPACITY = 64;

    // Maximum size of map
    // Should be > the treeify threshold, see HashMap/ConcurrentMap.TREEIFY_THRESHOLD
    // Should be > INITIAL_CAPACITY to ensure resize occurs
    static final int SIZE = 256;

    // Load factor of map
    // A value 1.0 will ensure that a new threshold == capacity
    static final float LOAD_FACTOR = 1.0f;

    @DataProvider(name = "maps")
    static Object[][] mapProvider() {
        return new Object[][] {
                // Pass in the class name as a description for test reporting
                // purposes
                { HashMap.class.getName(), new HashMap(INITIAL_CAPACITY, LOAD_FACTOR) },
                { LinkedHashMap.class.getName(), new LinkedHashMap(INITIAL_CAPACITY, LOAD_FACTOR) },
                { ConcurrentHashMap.class.getName(), new ConcurrentHashMap(INITIAL_CAPACITY, LOAD_FACTOR) },
        };
    }

    @Test(dataProvider = "maps")
    public void testPutThenGet(String d, Map<HashCodeInteger, Integer> m) {
        put(SIZE, m, (i, s) -> {
            for (int j = 0; j < s; j++) {
                assertEquals(m.get(new HashCodeInteger(j)).intValue(), j,
                             String.format("Map.get(%d)", j));
            }
        });
    }

    @Test(dataProvider = "maps")
    public void testPutThenTraverse(String d, Map<HashCodeInteger, Integer> m) {
        Collector<Integer, ?, ? extends Collection<Integer>> c = getCollector(m);

        put(SIZE, m, (i, s) -> {
            // Note that it is OK to collect to a Set (HashSet) as long as
            // integer values are used since these tests only check for
            // collisions and other tests will verify more general functionality
            Collection<Integer> actual = m.keySet().stream().map(e -> e.value).collect(c);
            Collection<Integer> expected = IntStream.range(0, s).boxed().collect(c);
            assertEquals(actual, expected, "Map.keySet()");
        });
    }

    @Test(dataProvider = "maps")
    public void testRemoveThenGet(String d, Map<HashCodeInteger, Integer> m) {
        put(SIZE, m, (i, s) -> { });

        remove(m, (i, s) -> {
            for (int j = i + 1; j < SIZE; j++) {
                assertEquals(m.get(new HashCodeInteger(j)).intValue(), j,
                             String.format("Map.get(%d)", j));
            }
        });
    }

    @Test(dataProvider = "maps")
    public void testRemoveThenTraverse(String d, Map<HashCodeInteger, Integer> m) {
        put(SIZE, m, (i, s) -> { });

        Collector<Integer, ?, ? extends Collection<Integer>> c = getCollector(m);

        remove(m, (i, s) -> {
            Collection<Integer> actual = m.keySet().stream().map(e -> e.value).collect(c);
            Collection<Integer> expected = IntStream.range(i + 1, SIZE).boxed().collect(c);
            assertEquals(actual, expected, "Map.keySet()");
        });
    }

    @Test(dataProvider = "maps")
    public void testUntreeifyOnResizeWithGet(String d, Map<HashCodeInteger, Integer> m) {
        // Fill the map with 64 entries grouped into 4 buckets
        put(INITIAL_CAPACITY, m, (i, s) -> { });

        for (int i = INITIAL_CAPACITY; i < SIZE; i++) {
            // Add further entries in the 0'th bucket so as not to disturb
            // other buckets, entries of which may be distributed and/or
            // the bucket untreeified on resize
            m.put(new HashCodeInteger(i, 0), i);

            for (int j = 0; j < INITIAL_CAPACITY; j++) {
                assertEquals(m.get(new HashCodeInteger(j)).intValue(), j,
                             String.format("Map.get(%d) < INITIAL_CAPACITY", j));
            }
            for (int j = INITIAL_CAPACITY; j <= i; j++) {
                assertEquals(m.get(new HashCodeInteger(j, 0)).intValue(), j,
                             String.format("Map.get(%d) >= INITIAL_CAPACITY", j));
            }
        }
    }

    @Test(dataProvider = "maps")
    public void testUntreeifyOnResizeWithTraverse(String d, Map<HashCodeInteger, Integer> m) {
        // Fill the map with 64 entries grouped into 4 buckets
        put(INITIAL_CAPACITY, m, (i, s) -> { });

        Collector<Integer, ?, ? extends Collection<Integer>> c = getCollector(m);

        for (int i = INITIAL_CAPACITY; i < SIZE; i++) {
            // Add further entries in the 0'th bucket so as not to disturb
            // other buckets, entries of which may be distributed and/or
            // the bucket untreeified on resize
            m.put(new HashCodeInteger(i, 0), i);

            Collection<Integer> actual = m.keySet().stream().map(e -> e.value).collect(c);
            Collection<Integer> expected = IntStream.rangeClosed(0, i).boxed().collect(c);
            assertEquals(actual, expected, "Key set");
        }
    }

    Collector<Integer, ?, ? extends Collection<Integer>> getCollector(Map<?, ?> m) {
        Collector<Integer, ?, ? extends Collection<Integer>> collector = m instanceof LinkedHashMap
                                                                         ? Collectors.toList()
                                                                         : Collectors.toSet();
        return collector;
    }

    void put(int size, Map<HashCodeInteger, Integer> m, BiConsumer<Integer, Integer> c) {
        for (int i = 0; i < size; i++) {
            m.put(new HashCodeInteger(i), i);

            c.accept(i, m.size());
        }
    }

    void remove(Map<HashCodeInteger, Integer> m, BiConsumer<Integer, Integer> c) {
        int size = m.size();
        // Remove all elements thus ensuring at some point trees will be
        // converting back to bins
        for (int i = 0; i < size; i++) {
            m.remove(new HashCodeInteger(i));

            c.accept(i, m.size());
        }
    }

    static final class HashCodeInteger implements Comparable<HashCodeInteger> {
        final int value;

        final int hashcode;

        HashCodeInteger(int value) {
            this(value, hash(value));
        }

        HashCodeInteger(int value, int hashcode) {
            this.value = value;
            this.hashcode = hashcode;
        }

        static int hash(int i) {
            // Assuming 64 entries with keys from 0 to 63 then a map:
            // - of capacity 64 will have 4 buckets with 16 entries per-bucket
            // - of capacity 128 will have 8 buckets with 8 entries per-bucket
            // - of capacity 256 will have 16 buckets with 4 entries per-bucket
            //
            // Re-sizing will result in re-distribution, doubling the buckets
            // and reducing the entries by half. This will result in
            // untreeifying when the number of entries is less than untreeify
            // threshold (see HashMap/ConcurrentMap.UNTREEIFY_THRESHOLD)
            return (i % 4) + (i / 4) * INITIAL_CAPACITY;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof HashCodeInteger) {
                HashCodeInteger other = (HashCodeInteger) obj;
                return other.value == value;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hashcode;
        }

        @Override
        public int compareTo(HashCodeInteger o) {
            return value - o.value;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }
}
