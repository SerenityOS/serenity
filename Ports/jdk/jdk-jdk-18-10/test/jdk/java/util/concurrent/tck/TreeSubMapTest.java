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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TreeSubMapTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(TreeSubMapTest.class);
    }

    /**
     * Returns a new map from Items 1-5 to Strings "A"-"E".
     */
    private static NavigableMap<Item,String> map5() {
        TreeMap<Item,String> map = new TreeMap<>();
        assertTrue(map.isEmpty());
        map.put(zero, "Z");
        map.put(one, "A");
        map.put(five, "E");
        map.put(three, "C");
        map.put(two, "B");
        map.put(four, "D");
        map.put(seven, "F");
        assertFalse(map.isEmpty());
        mustEqual(7, map.size());
        return map.subMap(one, true, seven, false);
    }

    private static NavigableMap<Item,String> map0() {
        TreeMap<Item,String> map = new TreeMap<>();
        assertTrue(map.isEmpty());
        return map.tailMap(one, true);
    }

    /**
     * Returns a new map from Items -5 to -1 to Strings "A"-"E".
     */
    private static NavigableMap<Item,String> dmap5() {
        TreeMap<Item,String> map = new TreeMap<>();
        assertTrue(map.isEmpty());
        map.put(minusOne, "A");
        map.put(minusFive, "E");
        map.put(minusThree, "C");
        map.put(minusTwo, "B");
        map.put(minusFour, "D");
        assertFalse(map.isEmpty());
        mustEqual(5, map.size());
        return map.descendingMap();
    }

    private static NavigableMap<Item,String> dmap0() {
        TreeMap<Item,String> map = new TreeMap<>();
        assertTrue(map.isEmpty());
        return map;
    }

    /**
     * clear removes all pairs
     */
    public void testClear() {
        NavigableMap<Item,String> map = map5();
        map.clear();
        mustEqual(0, map.size());
    }

    /**
     * Maps with same contents are equal
     */
    public void testEquals() {
        NavigableMap<Item,String> map1 = map5();
        NavigableMap<Item,String> map2 = map5();
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
        NavigableMap<Item,String> map = map5();
        assertTrue(map.containsKey(one));
        assertFalse(map.containsKey(zero));
    }

    /**
     * containsValue returns true for held values
     */
    public void testContainsValue() {
        NavigableMap<Item,String> map = map5();
        assertTrue(map.containsValue("A"));
        assertFalse(map.containsValue("Z"));
    }

    /**
     * get returns the correct element at the given key,
     * or null if not present
     */
    public void testGet() {
        NavigableMap<Item,String> map = map5();
        mustEqual("A", map.get(one));
        NavigableMap<Item,String> empty = map0();
        assertNull(empty.get(one));
    }

    /**
     * isEmpty is true of empty map and false for non-empty
     */
    public void testIsEmpty() {
        NavigableMap<Item,String> empty = map0();
        NavigableMap<Item,String> map = map5();
        assertTrue(empty.isEmpty());
        assertFalse(map.isEmpty());
    }

    /**
     * firstKey returns first key
     */
    public void testFirstKey() {
        NavigableMap<Item,String> map = map5();
        mustEqual(one, map.firstKey());
    }

    /**
     * lastKey returns last key
     */
    public void testLastKey() {
        NavigableMap<Item,String> map = map5();
        mustEqual(five, map.lastKey());
    }

    /**
     * keySet returns a Set containing all the keys
     */
    public void testKeySet() {
        NavigableMap<Item,String> map = map5();
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
        NavigableMap<Item,String> map = map5();
        Set<Item> s = map.keySet();
        Iterator<? extends Item> i = s.iterator();
        Item last = i.next();
        mustEqual(last, one);
        while (i.hasNext()) {
            Item k = i.next();
            assertTrue(last.compareTo(k) < 0);
            last = k;
        }
    }

    /**
     * values collection contains all values
     */
    public void testValues() {
        NavigableMap<Item,String> map = map5();
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
        NavigableMap<Item,String> map = map5();
        Set<Map.Entry<Item,String>> s = map.entrySet();
        mustEqual(5, s.size());
        Iterator<? extends Map.Entry<Item,String>> it = s.iterator();
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
     * putAll adds all key-value pairs from the given map
     */
    public void testPutAll() {
        NavigableMap<Item,String> empty = map0();
        NavigableMap<Item,String> map = map5();
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
        NavigableMap<Item,String> map = map5();
        map.remove(five);
        mustEqual(4, map.size());
        assertFalse(map.containsKey(five));
    }

    /**
     * lowerEntry returns preceding entry.
     */
    public void testLowerEntry() {
        NavigableMap<Item,String> map = map5();
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
        NavigableMap<Item,String> map = map5();
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
        NavigableMap<Item,String> map = map5();
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
        NavigableMap<Item,String> map = map5();
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
     * pollFirstEntry returns entries in order
     */
    public void testPollFirstEntry() {
        NavigableMap<Item,String> map = map5();
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
        assertTrue(map.isEmpty());
        Map.Entry<Item,String> f = map.firstEntry();
        assertNull(f);
        e = map.pollFirstEntry();
        assertNull(e);
    }

    /**
     * pollLastEntry returns entries in order
     */
    public void testPollLastEntry() {
        NavigableMap<Item,String> map = map5();
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
        NavigableMap<Item,String> map = map5();
        NavigableMap<Item,String> empty = map0();
        mustEqual(0, empty.size());
        mustEqual(5, map.size());
    }

    /**
     * toString contains toString of elements
     */
    public void testToString() {
        NavigableMap<Item,String> map = map5();
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
        NavigableMap<Item,String> c = map5();
        try {
            c.get(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * containsKey(null) of nonempty map throws NPE
     */
    public void testContainsKey_NullPointerException() {
        NavigableMap<Item,String> c = map5();
        try {
            c.containsKey(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * put(null,x) throws NPE
     */
    public void testPut1_NullPointerException() {
        NavigableMap<Item,String> c = map5();
        try {
            c.put(null, "whatever");
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * remove(null) throws NPE
     */
    public void testRemove1_NullPointerException() {
        NavigableMap<Item,String> c = map5();
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
        NavigableMap<Item,String> map = map5();
        SortedMap<Item,String> sm = map.subMap(two, four);
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
        NavigableMap<Item,String> map = map5();
        SortedMap<Item,String> sm = map.subMap(two, three);
        mustEqual(1, sm.size());
        mustEqual(two, sm.firstKey());
        mustEqual(two, sm.lastKey());
        assertFalse(sm.containsKey(one));
        assertTrue(sm.containsKey(two));
        assertFalse(sm.containsKey(three));
        assertFalse(sm.containsKey(four));
        assertFalse(sm.containsKey(five));
        Iterator<? extends Item> i = sm.keySet().iterator();
        Object k;
        k = (Item)(i.next());
        mustEqual(two, k);
        assertFalse(i.hasNext());
        Iterator<? extends Item> j = sm.keySet().iterator();
        j.next();
        j.remove();
        assertFalse(map.containsKey(two));
        mustEqual(4, map.size());
        mustEqual(0, sm.size());
        assertTrue(sm.isEmpty());
        assertNull(sm.remove(three));
        mustEqual(4, map.size());
    }

    /**
     * headMap returns map with keys in requested range
     */
    public void testHeadMapContents() {
        NavigableMap<Item,String> map = map5();
        SortedMap<Item,String> sm = map.headMap(four);
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
        NavigableMap<Item,String> map = map5();
        SortedMap<Item,String> sm = map.tailMap(two);
        assertFalse(sm.containsKey(one));
        assertTrue(sm.containsKey(two));
        assertTrue(sm.containsKey(three));
        assertTrue(sm.containsKey(four));
        assertTrue(sm.containsKey(five));
        Iterator<? extends Item> i = sm.keySet().iterator();
        Object k;
        k = (Item)(i.next());
        mustEqual(two, k);
        k = (Item)(i.next());
        mustEqual(three, k);
        k = (Item)(i.next());
        mustEqual(four, k);
        k = (Item)(i.next());
        mustEqual(five, k);
        assertFalse(i.hasNext());

        Iterator<Map.Entry<Item,String>> ei = sm.entrySet().iterator();
        Map.Entry<Item,String> e;
        e = ei.next();
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

        SortedMap<Item,String> ssm = sm.tailMap(four);
        mustEqual(four, ssm.firstKey());
        mustEqual(five, ssm.lastKey());
        mustEqual("D", ssm.remove(four));
        mustEqual(1, ssm.size());
        mustEqual(3, sm.size());
        mustEqual(4, map.size());
    }

    /**
     * clear removes all pairs
     */
    public void testDescendingClear() {
        NavigableMap<Item,String> map = dmap5();
        map.clear();
        mustEqual(0, map.size());
    }

    /**
     * Maps with same contents are equal
     */
    public void testDescendingEquals() {
        NavigableMap<Item,String> map1 = dmap5();
        NavigableMap<Item,String> map2 = dmap5();
        mustEqual(map1, map2);
        mustEqual(map2, map1);
        map1.clear();
        assertFalse(map1.equals(map2));
        assertFalse(map2.equals(map1));
    }

    /**
     * containsKey returns true for contained key
     */
    public void testDescendingContainsKey() {
        NavigableMap<Item,String> map = dmap5();
        assertTrue(map.containsKey(minusOne));
        assertFalse(map.containsKey(zero));
    }

    /**
     * containsValue returns true for held values
     */
    public void testDescendingContainsValue() {
        NavigableMap<Item,String> map = dmap5();
        assertTrue(map.containsValue("A"));
        assertFalse(map.containsValue("Z"));
    }

    /**
     * get returns the correct element at the given key,
     * or null if not present
     */
    public void testDescendingGet() {
        NavigableMap<Item,String> map = dmap5();
        mustEqual("A", map.get(minusOne));
        NavigableMap<Item,String> empty = dmap0();
        assertNull(empty.get(minusOne));
    }

    /**
     * isEmpty is true of empty map and false for non-empty
     */
    public void testDescendingIsEmpty() {
        NavigableMap<Item,String> empty = dmap0();
        NavigableMap<Item,String> map = dmap5();
        assertTrue(empty.isEmpty());
        assertFalse(map.isEmpty());
    }

    /**
     * firstKey returns first key
     */
    public void testDescendingFirstKey() {
        NavigableMap<Item,String> map = dmap5();
        mustEqual(minusOne, map.firstKey());
    }

    /**
     * lastKey returns last key
     */
    public void testDescendingLastKey() {
        NavigableMap<Item,String> map = dmap5();
        mustEqual(minusFive, map.lastKey());
    }

    /**
     * keySet returns a Set containing all the keys
     */
    public void testDescendingKeySet() {
        NavigableMap<Item,String> map = dmap5();
        Set<Item> s = map.keySet();
        mustEqual(5, s.size());
        mustContain(s, minusOne);
        mustContain(s, minusTwo);
        mustContain(s, minusThree);
        mustContain(s, minusFour);
        mustContain(s, minusFive);
    }

    /**
     * keySet is ordered
     */
    public void testDescendingKeySetOrder() {
        NavigableMap<Item,String> map = dmap5();
        Set<Item> s = map.keySet();
        Iterator<? extends Item> i = s.iterator();
        Item last = (Item)i.next();
        mustEqual(last, minusOne);
        while (i.hasNext()) {
            Item k = (Item)i.next();
            assertTrue(last.compareTo(k) > 0);
            last = k;
        }
    }

    /**
     * values collection contains all values
     */
    public void testDescendingValues() {
        NavigableMap<Item,String> map = dmap5();
        Collection<String> s = map.values();
        mustEqual(5, s.size());
        assertTrue(s.contains("A"));
        assertTrue(s.contains("B"));
        assertTrue(s.contains("C"));
        assertTrue(s.contains("D"));
        assertTrue(s.contains("E"));
    }

    /**
     * keySet.toArray returns contains all keys
     */
    public void testDescendingAscendingKeySetToArray() {
        NavigableMap<Item,String> map = dmap5();
        Set<Item> s = map.keySet();
        Item[] ar = s.toArray(new Item[0]);
        assertTrue(s.containsAll(Arrays.asList(ar)));
        mustEqual(5, ar.length);
        ar[0] = minusTen;
        assertFalse(s.containsAll(Arrays.asList(ar)));
    }

    /**
     * descendingkeySet.toArray returns contains all keys
     */
    public void testDescendingDescendingKeySetToArray() {
        NavigableMap<Item,String> map = dmap5();
        Set<Item> s = map.descendingKeySet();
        Item[] ar = s.toArray(new Item[0]);
        mustEqual(5, ar.length);
        assertTrue(s.containsAll(Arrays.asList(ar)));
        ar[0] = minusTen;
        assertFalse(s.containsAll(Arrays.asList(ar)));
    }

    /**
     * Values.toArray contains all values
     */
    public void testDescendingValuesToArray() {
        NavigableMap<Item,String> map = dmap5();
        Collection<String> v = map.values();
        String[] ar = v.toArray(new String[0]);
        ArrayList<String> s = new ArrayList<>(Arrays.asList(ar));
        mustEqual(5, ar.length);
        assertTrue(s.contains("A"));
        assertTrue(s.contains("B"));
        assertTrue(s.contains("C"));
        assertTrue(s.contains("D"));
        assertTrue(s.contains("E"));
    }

    /**
     * entrySet contains all pairs
     */
    public void testDescendingEntrySet() {
        NavigableMap<Item,String> map = dmap5();
        Set<Map.Entry<Item,String>> s = map.entrySet();
        mustEqual(5, s.size());
        Iterator<Map.Entry<Item,String>> it = s.iterator();
        while (it.hasNext()) {
            Map.Entry<Item,String> e = it.next();
            assertTrue(
                       (e.getKey().equals(minusOne) && e.getValue().equals("A")) ||
                       (e.getKey().equals(minusTwo) && e.getValue().equals("B")) ||
                       (e.getKey().equals(minusThree) && e.getValue().equals("C")) ||
                       (e.getKey().equals(minusFour) && e.getValue().equals("D")) ||
                       (e.getKey().equals(minusFive) && e.getValue().equals("E")));
        }
    }

    /**
     * putAll adds all key-value pairs from the given map
     */
    public void testDescendingPutAll() {
        NavigableMap<Item,String> empty = dmap0();
        NavigableMap<Item,String> map = dmap5();
        empty.putAll(map);
        mustEqual(5, empty.size());
        assertTrue(empty.containsKey(minusOne));
        assertTrue(empty.containsKey(minusTwo));
        assertTrue(empty.containsKey(minusThree));
        assertTrue(empty.containsKey(minusFour));
        assertTrue(empty.containsKey(minusFive));
    }

    /**
     * remove removes the correct key-value pair from the map
     */
    public void testDescendingRemove() {
        NavigableMap<Item,String> map = dmap5();
        map.remove(minusFive);
        mustEqual(4, map.size());
        assertFalse(map.containsKey(minusFive));
    }

    /**
     * lowerEntry returns preceding entry.
     */
    public void testDescendingLowerEntry() {
        NavigableMap<Item,String> map = dmap5();
        Map.Entry<Item,String> e1 = map.lowerEntry(minusThree);
        mustEqual(minusTwo, e1.getKey());

        Map.Entry<Item,String> e2 = map.lowerEntry(minusSix);
        mustEqual(minusFive, e2.getKey());

        Map.Entry<Item,String> e3 = map.lowerEntry(minusOne);
        assertNull(e3);

        Map.Entry<Item,String> e4 = map.lowerEntry(zero);
        assertNull(e4);
    }

    /**
     * higherEntry returns next entry.
     */
    public void testDescendingHigherEntry() {
        NavigableMap<Item,String> map = dmap5();
        Map.Entry<Item,String> e1 = map.higherEntry(minusThree);
        mustEqual(minusFour, e1.getKey());

        Map.Entry<Item,String> e2 = map.higherEntry(zero);
        mustEqual(minusOne, e2.getKey());

        Map.Entry<Item,String> e3 = map.higherEntry(minusFive);
        assertNull(e3);

        Map.Entry<Item,String> e4 = map.higherEntry(minusSix);
        assertNull(e4);
    }

    /**
     * floorEntry returns preceding entry.
     */
    public void testDescendingFloorEntry() {
        NavigableMap<Item,String> map = dmap5();
        Map.Entry<Item,String> e1 = map.floorEntry(minusThree);
        mustEqual(minusThree, e1.getKey());

        Map.Entry<Item,String> e2 = map.floorEntry(minusSix);
        mustEqual(minusFive, e2.getKey());

        Map.Entry<Item,String> e3 = map.floorEntry(minusOne);
        mustEqual(minusOne, e3.getKey());

        Map.Entry<Item,String> e4 = map.floorEntry(zero);
        assertNull(e4);
    }

    /**
     * ceilingEntry returns next entry.
     */
    public void testDescendingCeilingEntry() {
        NavigableMap<Item,String> map = dmap5();
        Map.Entry<Item,String> e1 = map.ceilingEntry(minusThree);
        mustEqual(minusThree, e1.getKey());

        Map.Entry<Item,String> e2 = map.ceilingEntry(zero);
        mustEqual(minusOne, e2.getKey());

        Map.Entry<Item,String> e3 = map.ceilingEntry(minusFive);
        mustEqual(minusFive, e3.getKey());

        Map.Entry<Item,String> e4 = map.ceilingEntry(minusSix);
        assertNull(e4);
    }

    /**
     * pollFirstEntry returns entries in order
     */
    public void testDescendingPollFirstEntry() {
        NavigableMap<Item,String> map = dmap5();
        Map.Entry<Item,String> e = map.pollFirstEntry();
        mustEqual(minusOne, e.getKey());
        mustEqual("A", e.getValue());
        e = map.pollFirstEntry();
        mustEqual(minusTwo, e.getKey());
        map.put(minusOne, "A");
        e = map.pollFirstEntry();
        mustEqual(minusOne, e.getKey());
        mustEqual("A", e.getValue());
        e = map.pollFirstEntry();
        mustEqual(minusThree, e.getKey());
        map.remove(minusFour);
        e = map.pollFirstEntry();
        mustEqual(minusFive, e.getKey());
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
    public void testDescendingPollLastEntry() {
        NavigableMap<Item,String> map = dmap5();
        Map.Entry<Item,String> e = map.pollLastEntry();
        mustEqual(minusFive, e.getKey());
        mustEqual("E", e.getValue());
        e = map.pollLastEntry();
        mustEqual(minusFour, e.getKey());
        map.put(minusFive, "E");
        e = map.pollLastEntry();
        mustEqual(minusFive, e.getKey());
        mustEqual("E", e.getValue());
        e = map.pollLastEntry();
        mustEqual(minusThree, e.getKey());
        map.remove(minusTwo);
        e = map.pollLastEntry();
        mustEqual(minusOne, e.getKey());
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
    public void testDescendingSize() {
        NavigableMap<Item,String> map = dmap5();
        NavigableMap<Item,String> empty = dmap0();
        mustEqual(0, empty.size());
        mustEqual(5, map.size());
    }

    /**
     * toString contains toString of elements
     */
    public void testDescendingToString() {
        NavigableMap<Item,String> map = dmap5();
        String s = map.toString();
        for (int i = 1; i <= 5; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    // Exception testDescendings

    /**
     * get(null) of nonempty map throws NPE
     */
    public void testDescendingGet_NullPointerException() {
        NavigableMap<Item,String> c = dmap5();
        try {
            c.get(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * put(null,x) throws NPE
     */
    public void testDescendingPut1_NullPointerException() {
        NavigableMap<Item,String> c = dmap5();
        try {
            c.put(null, "whatever");
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * A deserialized/reserialized map equals original
     */
    public void testDescendingSerialization() throws Exception {
        NavigableMap<Item,String> x = dmap5();
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
    public void testDescendingSubMapContents() {
        NavigableMap<Item,String> map = dmap5();
        SortedMap<Item,String> sm = map.subMap(minusTwo, minusFour);
        mustEqual(minusTwo, sm.firstKey());
        mustEqual(minusThree, sm.lastKey());
        mustEqual(2, sm.size());
        assertFalse(sm.containsKey(minusOne));
        assertTrue(sm.containsKey(minusTwo));
        assertTrue(sm.containsKey(minusThree));
        assertFalse(sm.containsKey(minusFour));
        assertFalse(sm.containsKey(minusFive));
        Iterator<? extends Item> i = sm.keySet().iterator();
        Item k;
        k = (Item)(i.next());
        mustEqual(minusTwo, k);
        k = (Item)(i.next());
        mustEqual(minusThree, k);
        assertFalse(i.hasNext());
        Iterator<? extends Item> j = sm.keySet().iterator();
        j.next();
        j.remove();
        assertFalse(map.containsKey(minusTwo));
        mustEqual(4, map.size());
        mustEqual(1, sm.size());
        mustEqual(minusThree, sm.firstKey());
        mustEqual(minusThree, sm.lastKey());
        mustEqual("C", sm.remove(minusThree));
        assertTrue(sm.isEmpty());
        mustEqual(3, map.size());
    }

    public void testDescendingSubMapContents2() {
        NavigableMap<Item,String> map = dmap5();
        SortedMap<Item,String> sm = map.subMap(minusTwo, minusThree);
        mustEqual(1, sm.size());
        mustEqual(minusTwo, sm.firstKey());
        mustEqual(minusTwo, sm.lastKey());
        assertFalse(sm.containsKey(minusOne));
        assertTrue(sm.containsKey(minusTwo));
        assertFalse(sm.containsKey(minusThree));
        assertFalse(sm.containsKey(minusFour));
        assertFalse(sm.containsKey(minusFive));
        Iterator<? extends Item> i = sm.keySet().iterator();
        Item k;
        k = (Item)(i.next());
        mustEqual(minusTwo, k);
        assertFalse(i.hasNext());
        Iterator<? extends Item> j = sm.keySet().iterator();
        j.next();
        j.remove();
        assertFalse(map.containsKey(minusTwo));
        mustEqual(4, map.size());
        mustEqual(0, sm.size());
        assertTrue(sm.isEmpty());
        assertNull(sm.remove(minusThree));
        mustEqual(4, map.size());
    }

    /**
     * headMap returns map with keys in requested range
     */
    public void testDescendingHeadMapContents() {
        NavigableMap<Item,String> map = dmap5();
        SortedMap<Item,String> sm = map.headMap(minusFour);
        assertTrue(sm.containsKey(minusOne));
        assertTrue(sm.containsKey(minusTwo));
        assertTrue(sm.containsKey(minusThree));
        assertFalse(sm.containsKey(minusFour));
        assertFalse(sm.containsKey(minusFive));
        Iterator<? extends Item> i = sm.keySet().iterator();
        Item k;
        k = (Item)(i.next());
        mustEqual(minusOne, k);
        k = (Item)(i.next());
        mustEqual(minusTwo, k);
        k = (Item)(i.next());
        mustEqual(minusThree, k);
        assertFalse(i.hasNext());
        sm.clear();
        assertTrue(sm.isEmpty());
        mustEqual(2, map.size());
        mustEqual(minusFour, map.firstKey());
    }

    /**
     * headMap returns map with keys in requested range
     */
    public void testDescendingTailMapContents() {
        NavigableMap<Item,String> map = dmap5();
        SortedMap<Item,String> sm = map.tailMap(minusTwo);
        assertFalse(sm.containsKey(minusOne));
        assertTrue(sm.containsKey(minusTwo));
        assertTrue(sm.containsKey(minusThree));
        assertTrue(sm.containsKey(minusFour));
        assertTrue(sm.containsKey(minusFive));
        Iterator<? extends Item> i = sm.keySet().iterator();
        Item k;
        k = (Item)(i.next());
        mustEqual(minusTwo, k);
        k = (Item)(i.next());
        mustEqual(minusThree, k);
        k = (Item)(i.next());
        mustEqual(minusFour, k);
        k = (Item)(i.next());
        mustEqual(minusFive, k);
        assertFalse(i.hasNext());

        Iterator<Map.Entry<Item,String>> ei = sm.entrySet().iterator();
        Map.Entry<Item,String> e;
        e = (ei.next());
        mustEqual(minusTwo, e.getKey());
        mustEqual("B", e.getValue());
        e = (ei.next());
        mustEqual(minusThree, e.getKey());
        mustEqual("C", e.getValue());
        e = (ei.next());
        mustEqual(minusFour, e.getKey());
        mustEqual("D", e.getValue());
        e = (ei.next());
        mustEqual(minusFive, e.getKey());
        mustEqual("E", e.getValue());
        assertFalse(i.hasNext());

        SortedMap<Item,String> ssm = sm.tailMap(minusFour);
        mustEqual(minusFour, ssm.firstKey());
        mustEqual(minusFive, ssm.lastKey());
        mustEqual("D", ssm.remove(minusFour));
        mustEqual(1, ssm.size());
        mustEqual(3, sm.size());
        mustEqual(4, map.size());
    }

}
