/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.Test;

public class TreeMapTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        class Implementation implements MapImplementation {
            public Class<?> klazz() { return TreeMap.class; }
            public Map emptyMap() { return new TreeMap(); }
            public boolean isConcurrent() { return false; }
            public boolean permitsNullKeys() { return false; }
            public boolean permitsNullValues() { return true; }
            public boolean supportsSetValue() { return true; }
        }
        return newTestSuite(
            TreeMapTest.class,
            MapTest.testSuite(new Implementation()));
    }

    /**
     * Returns a new map from Items 1-5 to Strings "A"-"E".
     */
    private static TreeMap<Item,String> map5() {
        TreeMap<Item,String> map = new TreeMap<>();
        assertTrue(map.isEmpty());
        map.put(one, "A");
        map.put(five, "E");
        map.put(three, "C");
        map.put(two, "B");
        map.put(four, "D");
        assertFalse(map.isEmpty());
        mustEqual(5, map.size());
        return map;
    }

    /**
     * clear removes all pairs
     */
    public void testClear() {
        TreeMap<Item,String> map = map5();
        map.clear();
        mustEqual(0, map.size());
    }

    /**
     * copy constructor creates map equal to source map
     */
    public void testConstructFromSorted() {
        TreeMap<Item,String> map = map5();
        TreeMap<Item,String> map2 = new TreeMap<>(map);
        mustEqual(map, map2);
    }

    /**
     * Maps with same contents are equal
     */
    public void testEquals() {
        TreeMap<Item,String> map1 = map5();
        TreeMap<Item,String> map2 = map5();
        mustEqual(map1, map2);
        mustEqual(map2, map1);
        map1.clear();
        assertFalse(map1.equals(map2));
        assertFalse(map2.equals(map1));
    }

    /**
     * containsKey returns true for contained key
     */
    public void testContainsKey() {
        TreeMap<Item,String> map = map5();
        assertTrue(map.containsKey(one));
        assertFalse(map.containsKey(zero));
    }

    /**
     * containsValue returns true for held values
     */
    public void testContainsValue() {
        TreeMap<Item,String> map = map5();
        assertTrue(map.containsValue("A"));
        assertFalse(map.containsValue("Z"));
    }

    /**
     * get returns the correct element at the given key,
     * or null if not present
     */
    public void testGet() {
        TreeMap<Item,String> map = map5();
        mustEqual("A", map.get(one));
        TreeMap<Item,String> empty = new TreeMap<>();
        assertNull(empty.get(one));
    }

    /**
     * isEmpty is true of empty map and false for non-empty
     */
    public void testIsEmpty() {
        TreeMap<Item,String> empty = new TreeMap<>();
        TreeMap<Item,String> map = map5();
        assertTrue(empty.isEmpty());
        assertFalse(map.isEmpty());
    }

    /**
     * firstKey returns first key
     */
    public void testFirstKey() {
        TreeMap<Item,String> map = map5();
        mustEqual(one, map.firstKey());
    }

    /**
     * lastKey returns last key
     */
    public void testLastKey() {
        TreeMap<Item,String> map = map5();
        mustEqual(five, map.lastKey());
    }

    /**
     * keySet.toArray returns contains all keys
     */
    public void testKeySetToArray() {
        TreeMap<Item,String> map = map5();
        Set<Item> s = map.keySet();
        Object[] ar = s.toArray();
        assertTrue(s.containsAll(Arrays.asList(ar)));
        mustEqual(5, ar.length);
        ar[0] = minusTen;
        assertFalse(s.containsAll(Arrays.asList(ar)));
    }

    /**
     * descendingkeySet.toArray returns contains all keys
     */
    public void testDescendingKeySetToArray() {
        TreeMap<Item,String> map = map5();
        Set<Item> s = map.descendingKeySet();
        Object[] ar = s.toArray();
        mustEqual(5, ar.length);
        assertTrue(s.containsAll(Arrays.asList(ar)));
        ar[0] = minusTen;
        assertFalse(s.containsAll(Arrays.asList(ar)));
    }

    /**
     * keySet returns a Set containing all the keys
     */
    public void testKeySet() {
        TreeMap<Item,String> map = map5();
        Set<Item> s = map.keySet();
        mustEqual(5, s.size());
        mustContain(s, one);
        mustContain(s, two);
        mustContain(s, three);
        mustContain(s, four);
        mustContain(s, five);
    }

    /**
     * keySet is ordered
     */
    public void testKeySetOrder() {
        TreeMap<Item,String> map = map5();
        Set<Item> s = map.keySet();
        Iterator<? extends Item> i = s.iterator();
        Item last = i.next();
        mustEqual(last, one);
        int count = 1;
        while (i.hasNext()) {
            Item k = i.next();
            assertTrue(last.compareTo(k) < 0);
            last = k;
            ++count;
        }
        mustEqual(5, count);
    }

    /**
     * descending iterator of key set is inverse ordered
     */
    public void testKeySetDescendingIteratorOrder() {
        TreeMap<Item,String> map = map5();
        NavigableSet<Item> s = map.navigableKeySet();
        Iterator<? extends Item> i = s.descendingIterator();
        Item last = (Item)i.next();
        mustEqual(last, five);
        int count = 1;
        while (i.hasNext()) {
            Item k = (Item)i.next();
            assertTrue(last.compareTo(k) > 0);
            last = k;
            ++count;
        }
        mustEqual(5, count);
    }

    /**
     * descendingKeySet is ordered
     */
    public void testDescendingKeySetOrder() {
        TreeMap<Item,String> map = map5();
        Set<Item> s = map.descendingKeySet();
        Iterator<? extends Item> i = s.iterator();
        Item last = (Item)i.next();
        mustEqual(last, five);
        int count = 1;
        while (i.hasNext()) {
            Item k = (Item)i.next();
            assertTrue(last.compareTo(k) > 0);
            last = k;
            ++count;
        }
        mustEqual(5, count);
    }

    /**
     * descending iterator of descendingKeySet is ordered
     */
    public void testDescendingKeySetDescendingIteratorOrder() {
        TreeMap<Item,String> map = map5();
        NavigableSet<Item> s = map.descendingKeySet();
        Iterator<? extends Item> i = s.descendingIterator();
        Item last = (Item)i.next();
        mustEqual(last, one);
        int count = 1;
        while (i.hasNext()) {
            Item k = (Item)i.next();
            assertTrue(last.compareTo(k) < 0);
            last = k;
            ++count;
        }
        mustEqual(5, count);
    }

    /**
     * values collection contains all values
     */
    public void testValues() {
        TreeMap<Item,String> map = map5();
        Collection<String> s = map.values();
        mustEqual(5, s.size());
        assertTrue(s.contains("A"));
        assertTrue(s.contains("B"));
        assertTrue(s.contains("C"));
        assertTrue(s.contains("D"));
        assertTrue(s.contains("E"));
    }

    /**
     * entrySet contains all pairs
     */
    public void testEntrySet() {
        TreeMap<Item,String> map = map5();
        Set<Map.Entry<Item,String>> s = map.entrySet();
        mustEqual(5, s.size());
        Iterator<Map.Entry<Item,String>> it = s.iterator();
        while (it.hasNext()) {
            Map.Entry<Item,String> e = it.next();
            assertTrue(
                       (e.getKey().equals(one) && e.getValue().equals("A")) ||
                       (e.getKey().equals(two) && e.getValue().equals("B")) ||
                       (e.getKey().equals(three) && e.getValue().equals("C")) ||
                       (e.getKey().equals(four) && e.getValue().equals("D")) ||
                       (e.getKey().equals(five) && e.getValue().equals("E")));
        }
    }

    /**
     * descendingEntrySet contains all pairs
     */
    public void testDescendingEntrySet() {
        TreeMap<Item,String> map = map5();
        Set<Map.Entry<Item,String>> s = map.descendingMap().entrySet();
        mustEqual(5, s.size());
        Iterator<Map.Entry<Item,String>> it = s.iterator();
        while (it.hasNext()) {
            Map.Entry<Item,String> e = it.next();
            assertTrue(
                       (e.getKey().equals(one) && e.getValue().equals("A")) ||
                       (e.getKey().equals(two) && e.getValue().equals("B")) ||
                       (e.getKey().equals(three) && e.getValue().equals("C")) ||
                       (e.getKey().equals(four) && e.getValue().equals("D")) ||
                       (e.getKey().equals(five) && e.getValue().equals("E")));
        }
    }

    /**
     * entrySet.toArray contains all entries
     */
    public void testEntrySetToArray() {
        TreeMap<Item,String> map = map5();
        Set<Map.Entry<Item,String>> s = map.entrySet();
        Object[] ar = s.toArray();
        mustEqual(5, ar.length);
        for (int i = 0; i < 5; ++i) {
            assertTrue(map.containsKey(((Map.Entry)(ar[i])).getKey()));
            assertTrue(map.containsValue(((Map.Entry)(ar[i])).getValue()));
        }
    }

    /**
     * descendingEntrySet.toArray contains all entries
     */
    public void testDescendingEntrySetToArray() {
        TreeMap<Item,String> map = map5();
        Set<Map.Entry<Item,String>> s = map.descendingMap().entrySet();
        Object[] ar = s.toArray();
        mustEqual(5, ar.length);
        for (int i = 0; i < 5; ++i) {
            assertTrue(map.containsKey(((Map.Entry)(ar[i])).getKey()));
            assertTrue(map.containsValue(((Map.Entry)(ar[i])).getValue()));
        }
    }

    /**
     * putAll adds all key-value pairs from the given map
     */
    public void testPutAll() {
        TreeMap<Item,String> empty = new TreeMap<>();
        TreeMap<Item,String> map = map5();
        empty.putAll(map);
        mustEqual(5, empty.size());
        assertTrue(empty.containsKey(one));
        assertTrue(empty.containsKey(two));
        assertTrue(empty.containsKey(three));
        assertTrue(empty.containsKey(four));
        assertTrue(empty.containsKey(five));
    }

    /**
     * remove removes the correct key-value pair from the map
     */
    public void testRemove() {
        TreeMap<Item,String> map = map5();
        map.remove(five);
        mustEqual(4, map.size());
        assertFalse(map.containsKey(five));
    }

    /**
     * lowerEntry returns preceding entry.
     */
    public void testLowerEntry() {
        TreeMap<Item,String> map = map5();
        Map.Entry<Item,String> e1 = map.lowerEntry(three);
        mustEqual(two, e1.getKey());

        Map.Entry<Item,String> e2 = map.lowerEntry(six);
        mustEqual(five, e2.getKey());

        Map.Entry<Item,String> e3 = map.lowerEntry(one);
        assertNull(e3);

        Map.Entry<Item,String> e4 = map.lowerEntry(zero);
        assertNull(e4);
    }

    /**
     * higherEntry returns next entry.
     */
    public void testHigherEntry() {
        TreeMap<Item,String> map = map5();
        Map.Entry<Item,String> e1 = map.higherEntry(three);
        mustEqual(four, e1.getKey());

        Map.Entry<Item,String> e2 = map.higherEntry(zero);
        mustEqual(one, e2.getKey());

        Map.Entry<Item,String> e3 = map.higherEntry(five);
        assertNull(e3);

        Map.Entry<Item,String> e4 = map.higherEntry(six);
        assertNull(e4);
    }

    /**
     * floorEntry returns preceding entry.
     */
    public void testFloorEntry() {
        TreeMap<Item,String> map = map5();
        Map.Entry<Item,String> e1 = map.floorEntry(three);
        mustEqual(three, e1.getKey());

        Map.Entry<Item,String> e2 = map.floorEntry(six);
        mustEqual(five, e2.getKey());

        Map.Entry<Item,String> e3 = map.floorEntry(one);
        mustEqual(one, e3.getKey());

        Map.Entry<Item,String> e4 = map.floorEntry(zero);
        assertNull(e4);
    }

    /**
     * ceilingEntry returns next entry.
     */
    public void testCeilingEntry() {
        TreeMap<Item,String> map = map5();
        Map.Entry<Item,String> e1 = map.ceilingEntry(three);
        mustEqual(three, e1.getKey());

        Map.Entry<Item,String> e2 = map.ceilingEntry(zero);
        mustEqual(one, e2.getKey());

        Map.Entry<Item,String> e3 = map.ceilingEntry(five);
        mustEqual(five, e3.getKey());

        Map.Entry<Item,String> e4 = map.ceilingEntry(six);
        assertNull(e4);
    }

    /**
     * lowerKey returns preceding element
     */
    public void testLowerKey() {
        TreeMap<Item,String> q = map5();
        Object e1 = q.lowerKey(three);
        mustEqual(two, e1);

        Object e2 = q.lowerKey(six);
        mustEqual(five, e2);

        Object e3 = q.lowerKey(one);
        assertNull(e3);

        Object e4 = q.lowerKey(zero);
        assertNull(e4);
    }

    /**
     * higherKey returns next element
     */
    public void testHigherKey() {
        TreeMap<Item,String> q = map5();
        Object e1 = q.higherKey(three);
        mustEqual(four, e1);

        Object e2 = q.higherKey(zero);
        mustEqual(one, e2);

        Object e3 = q.higherKey(five);
        assertNull(e3);

        Object e4 = q.higherKey(six);
        assertNull(e4);
    }

    /**
     * floorKey returns preceding element
     */
    public void testFloorKey() {
        TreeMap<Item,String> q = map5();
        Object e1 = q.floorKey(three);
        mustEqual(three, e1);

        Object e2 = q.floorKey(six);
        mustEqual(five, e2);

        Object e3 = q.floorKey(one);
        mustEqual(one, e3);

        Object e4 = q.floorKey(zero);
        assertNull(e4);
    }

    /**
     * ceilingKey returns next element
     */
    public void testCeilingKey() {
        TreeMap<Item,String> q = map5();
        Object e1 = q.ceilingKey(three);
        mustEqual(three, e1);

        Object e2 = q.ceilingKey(zero);
        mustEqual(one, e2);

        Object e3 = q.ceilingKey(five);
        mustEqual(five, e3);

        Object e4 = q.ceilingKey(six);
        assertNull(e4);
    }

    /**
     * pollFirstEntry returns entries in order
     */
    public void testPollFirstEntry() {
        TreeMap<Item,String> map = map5();
        Map.Entry<Item,String> e = map.pollFirstEntry();
        mustEqual(one, e.getKey());
        mustEqual("A", e.getValue());
        e = map.pollFirstEntry();
        mustEqual(two, e.getKey());
        map.put(one, "A");
        e = map.pollFirstEntry();
        mustEqual(one, e.getKey());
        mustEqual("A", e.getValue());
        e = map.pollFirstEntry();
        mustEqual(three, e.getKey());
        map.remove(four);
        e = map.pollFirstEntry();
        mustEqual(five, e.getKey());
        try {
            e.setValue("A");
            shouldThrow();
        } catch (UnsupportedOperationException success) {}
        e = map.pollFirstEntry();
        assertNull(e);
    }

    /**
     * pollLastEntry returns entries in order
     */
    public void testPollLastEntry() {
        TreeMap<Item,String> map = map5();
        Map.Entry<Item,String> e = map.pollLastEntry();
        mustEqual(five, e.getKey());
        mustEqual("E", e.getValue());
        e = map.pollLastEntry();
        mustEqual(four, e.getKey());
        map.put(five, "E");
        e = map.pollLastEntry();
        mustEqual(five, e.getKey());
        mustEqual("E", e.getValue());
        e = map.pollLastEntry();
        mustEqual(three, e.getKey());
        map.remove(two);
        e = map.pollLastEntry();
        mustEqual(one, e.getKey());
        try {
            e.setValue("E");
            shouldThrow();
        } catch (UnsupportedOperationException success) {}
        e = map.pollLastEntry();
        assertNull(e);
    }

    /**
     * size returns the correct values
     */
    public void testSize() {
        TreeMap<Item,String> map = map5();
        TreeMap<Item,String> empty = new TreeMap<>();
        mustEqual(0, empty.size());
        mustEqual(5, map.size());
    }

    /**
     * toString contains toString of elements
     */
    public void testToString() {
        TreeMap<Item,String> map = map5();
        String s = map.toString();
        for (int i = 1; i <= 5; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    // Exception tests

    /**
     * get(null) of nonempty map throws NPE
     */
    public void testGet_NullPointerException() {
        TreeMap<Item,String> c = map5();
        try {
            c.get(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * containsKey(null) of nonempty map throws NPE
     */
    public void testContainsKey_NullPointerException() {
        TreeMap<Item,String> c = map5();
        try {
            c.containsKey(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * remove(null) throws NPE for nonempty map
     */
    public void testRemove1_NullPointerException() {
        TreeMap<Item,String> c = new TreeMap<>();
        c.put(one, "asdads");
        try {
            c.remove(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * A deserialized/reserialized map equals original
     */
    public void testSerialization() throws Exception {
        NavigableMap<Item,String> x = map5();
        NavigableMap<Item,String> y = serialClone(x);

        assertNotSame(x, y);
        mustEqual(x.size(), y.size());
        mustEqual(x.toString(), y.toString());
        mustEqual(x, y);
        mustEqual(y, x);
    }

    /**
     * subMap returns map with keys in requested range
     */
    public void testSubMapContents() {
        TreeMap<Item,String> map = map5();
        NavigableMap<Item,String> sm = map.subMap(two, true, four, false);
        mustEqual(two, sm.firstKey());
        mustEqual(three, sm.lastKey());
        mustEqual(2, sm.size());
        assertFalse(sm.containsKey(one));
        assertTrue(sm.containsKey(two));
        assertTrue(sm.containsKey(three));
        assertFalse(sm.containsKey(four));
        assertFalse(sm.containsKey(five));
        Iterator<? extends Item> i = sm.keySet().iterator();
        Item k;
        k = (Item)(i.next());
        mustEqual(two, k);
        k = (Item)(i.next());
        mustEqual(three, k);
        assertFalse(i.hasNext());
        Iterator<? extends Item> r = sm.descendingKeySet().iterator();
        k = (Item)(r.next());
        mustEqual(three, k);
        k = (Item)(r.next());
        mustEqual(two, k);
        assertFalse(r.hasNext());

        Iterator<? extends Item> j = sm.keySet().iterator();
        j.next();
        j.remove();
        assertFalse(map.containsKey(two));
        mustEqual(4, map.size());
        mustEqual(1, sm.size());
        mustEqual(three, sm.firstKey());
        mustEqual(three, sm.lastKey());
        mustEqual("C", sm.remove(three));
        assertTrue(sm.isEmpty());
        mustEqual(3, map.size());
    }

    public void testSubMapContents2() {
        TreeMap<Item,String> map = map5();
        NavigableMap<Item,String> sm = map.subMap(two, true, three, false);
        mustEqual(1, sm.size());
        mustEqual(two, sm.firstKey());
        mustEqual(two, sm.lastKey());
        assertFalse(sm.containsKey(one));
        assertTrue(sm.containsKey(two));
        assertFalse(sm.containsKey(three));
        assertFalse(sm.containsKey(four));
        assertFalse(sm.containsKey(five));
        Iterator<? extends Item> i = sm.keySet().iterator();
        Item k;
        k = (Item)(i.next());
        mustEqual(two, k);
        assertFalse(i.hasNext());
        Iterator<? extends Item> r = sm.descendingKeySet().iterator();
        k = (Item)(r.next());
        mustEqual(two, k);
        assertFalse(r.hasNext());

        Iterator<? extends Item> j = sm.keySet().iterator();
        j.next();
        j.remove();
        assertFalse(map.containsKey(two));
        mustEqual(4, map.size());
        mustEqual(0, sm.size());
        assertTrue(sm.isEmpty());
        assertSame(sm.remove(three), null);
        mustEqual(4, map.size());
    }

    /**
     * headMap returns map with keys in requested range
     */
    public void testHeadMapContents() {
        TreeMap<Item,String> map = map5();
        NavigableMap<Item, String> sm = map.headMap(four, false);
        assertTrue(sm.containsKey(one));
        assertTrue(sm.containsKey(two));
        assertTrue(sm.containsKey(three));
        assertFalse(sm.containsKey(four));
        assertFalse(sm.containsKey(five));
        Iterator<? extends Item> i = sm.keySet().iterator();
        Item k;
        k = (Item)(i.next());
        mustEqual(one, k);
        k = (Item)(i.next());
        mustEqual(two, k);
        k = (Item)(i.next());
        mustEqual(three, k);
        assertFalse(i.hasNext());
        sm.clear();
        assertTrue(sm.isEmpty());
        mustEqual(2, map.size());
        mustEqual(four, map.firstKey());
    }

    /**
     * headMap returns map with keys in requested range
     */
    public void testTailMapContents() {
        TreeMap<Item,String> map = map5();
        NavigableMap<Item,String> sm = map.tailMap(two, true);
        assertFalse(sm.containsKey(one));
        assertTrue(sm.containsKey(two));
        assertTrue(sm.containsKey(three));
        assertTrue(sm.containsKey(four));
        assertTrue(sm.containsKey(five));
        Iterator<? extends Item> i = sm.keySet().iterator();
        Item k = (i.next());
        mustEqual(two, k);
        k = (i.next());
        mustEqual(three, k);
        k = (i.next());
        mustEqual(four, k);
        k = (i.next());
        mustEqual(five, k);
        assertFalse(i.hasNext());
        Iterator<? extends Item> r = sm.descendingKeySet().iterator();
        k = (r.next());
        mustEqual(five, k);
        k = (r.next());
        mustEqual(four, k);
        k = (r.next());
        mustEqual(three, k);
        k = (r.next());
        mustEqual(two, k);
        assertFalse(r.hasNext());

        Iterator<Map.Entry<Item,String>> ei = sm.entrySet().iterator();
        Map.Entry<Item,String> e;
        e = (ei.next());
        mustEqual(two, e.getKey());
        mustEqual("B", e.getValue());
        e = (ei.next());
        mustEqual(three, e.getKey());
        mustEqual("C", e.getValue());
        e = (ei.next());
        mustEqual(four, e.getKey());
        mustEqual("D", e.getValue());
        e = (ei.next());
        mustEqual(five, e.getKey());
        mustEqual("E", e.getValue());
        assertFalse(i.hasNext());

        NavigableMap<Item,String> ssm = sm.tailMap(four, true);
        mustEqual(four, ssm.firstKey());
        mustEqual(five, ssm.lastKey());
        mustEqual("D", ssm.remove(four));
        mustEqual(1, ssm.size());
        mustEqual(3, sm.size());
        mustEqual(4, map.size());
    }

    Random rnd = new Random(666);
    BitSet bs;

    /**
     * Submaps of submaps subdivide correctly
     */
    public void testRecursiveSubMaps() throws Exception {
        int mapSize = expensiveTests ? 1000 : 100;
        Class<?> cl = TreeMap.class;
        NavigableMap<Item, Item> map = newMap(cl);
        bs = new BitSet(mapSize);

        populate(map, mapSize);
        check(map,                 0, mapSize - 1, true);
        check(map.descendingMap(), 0, mapSize - 1, false);

        mutateMap(map, 0, mapSize - 1);
        check(map,                 0, mapSize - 1, true);
        check(map.descendingMap(), 0, mapSize - 1, false);

        bashSubMap(map.subMap(zero, true, itemFor(mapSize), false),
                   0, mapSize - 1, true);
    }

    static NavigableMap<Item, Item> newMap(Class<?> cl) throws Exception {
        @SuppressWarnings("unchecked")
        NavigableMap<Item, Item> result
            = (NavigableMap<Item, Item>) cl.getConstructor().newInstance();
        mustEqual(0, result.size());
        assertFalse(result.keySet().iterator().hasNext());
        return result;
    }

    void populate(NavigableMap<Item, Item> map, int limit) {
        for (int i = 0, n = 2 * limit / 3; i < n; i++) {
            int key = rnd.nextInt(limit);
            put(map, key);
        }
    }

    void mutateMap(NavigableMap<Item, Item> map, int min, int max) {
        int size = map.size();
        int rangeSize = max - min + 1;

        // Remove a bunch of entries directly
        for (int i = 0, n = rangeSize / 2; i < n; i++) {
            remove(map, min - 5 + rnd.nextInt(rangeSize + 10));
        }

        // Remove a bunch of entries with iterator
        for (Iterator<Item> it = map.keySet().iterator(); it.hasNext(); ) {
            if (rnd.nextBoolean()) {
                bs.clear(it.next().value);
                it.remove();
            }
        }

        // Add entries till we're back to original size
        while (map.size() < size) {
            int key = min + rnd.nextInt(rangeSize);
            assertTrue(key >= min && key <= max);
            put(map, key);
        }
    }

    void mutateSubMap(NavigableMap<Item, Item> map, int min, int max) {
        int size = map.size();
        int rangeSize = max - min + 1;

        // Remove a bunch of entries directly
        for (int i = 0, n = rangeSize / 2; i < n; i++) {
            remove(map, min - 5 + rnd.nextInt(rangeSize + 10));
        }

        // Remove a bunch of entries with iterator
        for (Iterator<Item> it = map.keySet().iterator(); it.hasNext(); ) {
            if (rnd.nextBoolean()) {
                bs.clear(it.next().value);
                it.remove();
            }
        }

        // Add entries till we're back to original size
        while (map.size() < size) {
            int key = min - 5 + rnd.nextInt(rangeSize + 10);
            if (key >= min && key <= max) {
                put(map, key);
            } else {
                try {
                    map.put(itemFor(key), itemFor(2 * key));
                    shouldThrow();
                } catch (IllegalArgumentException success) {}
            }
        }
    }

    void put(NavigableMap<Item, Item> map, int key) {
        if (map.put(itemFor(key), itemFor(2 * key)) == null)
            bs.set(key);
    }

    void remove(NavigableMap<Item, Item> map, int key) {
        if (map.remove(itemFor(key)) != null)
            bs.clear(key);
    }

    void bashSubMap(NavigableMap<Item, Item> map,
                    int min, int max, boolean ascending) {
        check(map, min, max, ascending);
        check(map.descendingMap(), min, max, !ascending);

        mutateSubMap(map, min, max);
        check(map, min, max, ascending);
        check(map.descendingMap(), min, max, !ascending);

        // Recurse
        if (max - min < 2)
            return;
        int midPoint = (min + max) / 2;

        // headMap - pick direction and endpoint inclusion randomly
        boolean incl = rnd.nextBoolean();
        NavigableMap<Item,Item> hm = map.headMap(itemFor(midPoint), incl);
        if (ascending) {
            if (rnd.nextBoolean())
                bashSubMap(hm, min, midPoint - (incl ? 0 : 1), true);
            else
                bashSubMap(hm.descendingMap(), min, midPoint - (incl ? 0 : 1),
                           false);
        } else {
            if (rnd.nextBoolean())
                bashSubMap(hm, midPoint + (incl ? 0 : 1), max, false);
            else
                bashSubMap(hm.descendingMap(), midPoint + (incl ? 0 : 1), max,
                           true);
        }

        // tailMap - pick direction and endpoint inclusion randomly
        incl = rnd.nextBoolean();
        NavigableMap<Item,Item> tm = map.tailMap(itemFor(midPoint),incl);
        if (ascending) {
            if (rnd.nextBoolean())
                bashSubMap(tm, midPoint + (incl ? 0 : 1), max, true);
            else
                bashSubMap(tm.descendingMap(), midPoint + (incl ? 0 : 1), max,
                           false);
        } else {
            if (rnd.nextBoolean()) {
                bashSubMap(tm, min, midPoint - (incl ? 0 : 1), false);
            } else {
                bashSubMap(tm.descendingMap(), min, midPoint - (incl ? 0 : 1),
                           true);
            }
        }

        // subMap - pick direction and endpoint inclusion randomly
        int rangeSize = max - min + 1;
        int[] endpoints = new int[2];
        endpoints[0] = min + rnd.nextInt(rangeSize);
        endpoints[1] = min + rnd.nextInt(rangeSize);
        Arrays.sort(endpoints);
        boolean lowIncl = rnd.nextBoolean();
        boolean highIncl = rnd.nextBoolean();
        if (ascending) {
            NavigableMap<Item,Item> sm = map.subMap(
                itemFor(endpoints[0]), lowIncl, itemFor(endpoints[1]), highIncl);
            if (rnd.nextBoolean())
                bashSubMap(sm, endpoints[0] + (lowIncl ? 0 : 1),
                           endpoints[1] - (highIncl ? 0 : 1), true);
            else
                bashSubMap(sm.descendingMap(), endpoints[0] + (lowIncl ? 0 : 1),
                           endpoints[1] - (highIncl ? 0 : 1), false);
        } else {
            NavigableMap<Item,Item> sm = map.subMap(
                itemFor(endpoints[1]), highIncl, itemFor(endpoints[0]), lowIncl);
            if (rnd.nextBoolean())
                bashSubMap(sm, endpoints[0] + (lowIncl ? 0 : 1),
                           endpoints[1] - (highIncl ? 0 : 1), false);
            else
                bashSubMap(sm.descendingMap(), endpoints[0] + (lowIncl ? 0 : 1),
                           endpoints[1] - (highIncl ? 0 : 1), true);
        }
    }

    /**
     * min and max are both inclusive.  If max < min, interval is empty.
     */
    void check(NavigableMap<Item, Item> map,
                      final int min, final int max, final boolean ascending) {
        class ReferenceSet {
            int lower(int key) {
                return ascending ? lowerAscending(key) : higherAscending(key);
            }
            int floor(int key) {
                return ascending ? floorAscending(key) : ceilingAscending(key);
            }
            int ceiling(int key) {
                return ascending ? ceilingAscending(key) : floorAscending(key);
            }
            int higher(int key) {
                return ascending ? higherAscending(key) : lowerAscending(key);
            }
            int first() {
                return ascending ? firstAscending() : lastAscending();
            }
            int last() {
                return ascending ? lastAscending() : firstAscending();
            }
            int lowerAscending(int key) {
                return floorAscending(key - 1);
            }
            int floorAscending(int key) {
                if (key < min)
                    return -1;
                else if (key > max)
                    key = max;

                // BitSet should support this! Test would run much faster
                while (key >= min) {
                    if (bs.get(key))
                        return key;
                    key--;
                }
                return -1;
            }
            int ceilingAscending(int key) {
                if (key < min)
                    key = min;
                else if (key > max)
                    return -1;
                int result = bs.nextSetBit(key);
                return result > max ? -1 : result;
            }
            int higherAscending(int key) {
                return ceilingAscending(key + 1);
            }
            private int firstAscending() {
                int result = ceilingAscending(min);
                return result > max ? -1 : result;
            }
            private int lastAscending() {
                int result = floorAscending(max);
                return result < min ? -1 : result;
            }
        }
        ReferenceSet rs = new ReferenceSet();

        // Test contents using containsKey
        int size = 0;
        for (int i = min; i <= max; i++) {
            boolean bsContainsI = bs.get(i);
            mustEqual(bsContainsI, map.containsKey(itemFor(i)));
            if (bsContainsI)
                size++;
        }
        mustEqual(size, map.size());

        // Test contents using contains keySet iterator
        int size2 = 0;
        int previousKey = -1;
        for (Item key : map.keySet()) {
            assertTrue(bs.get(key.value));
            size2++;
            assertTrue(previousKey < 0 ||
                (ascending ? key.value - previousKey > 0 : key.value - previousKey < 0));
            previousKey = key.value;
        }
        mustEqual(size2, size);

        // Test navigation ops
        for (int key = min - 1; key <= max + 1; key++) {
            Item k = itemFor(key);
            assertEq(map.lowerKey(k), rs.lower(key));
            assertEq(map.floorKey(k), rs.floor(key));
            assertEq(map.higherKey(k), rs.higher(key));
            assertEq(map.ceilingKey(k), rs.ceiling(key));
        }

        // Test extrema
        if (map.size() != 0) {
            assertEq(map.firstKey(), rs.first());
            assertEq(map.lastKey(), rs.last());
        } else {
            mustEqual(rs.first(), -1);
            mustEqual(rs.last(),  -1);
            try {
                map.firstKey();
                shouldThrow();
            } catch (NoSuchElementException success) {}
            try {
                map.lastKey();
                shouldThrow();
            } catch (NoSuchElementException success) {}
        }
    }

    static void assertEq(Item i, int j) {
        if (i == null)
            mustEqual(j, -1);
        else
            mustEqual(i, j);
    }

    static boolean eq(Item i, int j) {
        return i == null ? j == -1 : i.value == j;
    }

}
