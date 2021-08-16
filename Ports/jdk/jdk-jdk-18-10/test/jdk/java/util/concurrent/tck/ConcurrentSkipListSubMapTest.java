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
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ConcurrentSkipListSubMapTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ConcurrentSkipListSubMapTest.class);
    }

    /**
     * Returns a new map from Items 1-5 to Strings "A"-"E".
     */
    private static ConcurrentNavigableMap<Item,String> map5() {
        ConcurrentSkipListMap<Item,String>map = new ConcurrentSkipListMap<>();
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

    /**
     * Returns a new map from Items -5 to -1 to Strings "A"-"E".
     */
    private static ConcurrentNavigableMap<Item,String> dmap5() {
        ConcurrentSkipListMap<Item,String>map = new ConcurrentSkipListMap<>();
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

    private static ConcurrentNavigableMap<Item,String> map0() {
        ConcurrentSkipListMap<Item,String>map = new ConcurrentSkipListMap<>();
        assertTrue(map.isEmpty());
        return map.tailMap(one, true);
    }

    private static ConcurrentNavigableMap<Item,String> dmap0() {
        ConcurrentSkipListMap<Item,String>map = new ConcurrentSkipListMap<>();
        assertTrue(map.isEmpty());
        return map;
    }

    /**
     * clear removes all pairs
     */
    public void testClear() {
        ConcurrentNavigableMap<Item,String> map = map5();
        map.clear();
        mustEqual(0, map.size());
    }

    /**
     * Maps with same contents are equal
     */
    public void testEquals() {
        ConcurrentNavigableMap<Item,String> map1 = map5();
        ConcurrentNavigableMap<Item,String> map2 = map5();
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
        ConcurrentNavigableMap<Item,String> map = map5();
        assertTrue(map.containsKey(one));
        assertFalse(map.containsKey(zero));
    }

    /**
     * containsValue returns true for held values
     */
    public void testContainsValue() {
        ConcurrentNavigableMap<Item,String> map = map5();
        assertTrue(map.containsValue("A"));
        assertFalse(map.containsValue("Z"));
    }

    /**
     * get returns the correct element at the given key,
     * or null if not present
     */
    public void testGet() {
        ConcurrentNavigableMap<Item,String> map = map5();
        mustEqual("A", map.get(one));
        ConcurrentNavigableMap<Item,String> empty = map0();
        assertNull(empty.get(one));
    }

    /**
     * isEmpty is true of empty map and false for non-empty
     */
    public void testIsEmpty() {
        ConcurrentNavigableMap<Item,String> empty = map0();
        ConcurrentNavigableMap<Item,String> map = map5();
        assertTrue(empty.isEmpty());
        assertFalse(map.isEmpty());
    }

    /**
     * firstKey returns first key
     */
    public void testFirstKey() {
        ConcurrentNavigableMap<Item,String> map = map5();
        mustEqual(one, map.firstKey());
    }

    /**
     * lastKey returns last key
     */
    public void testLastKey() {
        ConcurrentNavigableMap<Item,String> map = map5();
        mustEqual(five, map.lastKey());
    }

    /**
     * keySet returns a Set containing all the keys
     */
    public void testKeySet() {
        ConcurrentNavigableMap<Item,String> map = map5();
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
        ConcurrentNavigableMap<Item,String> map = map5();
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
        ConcurrentNavigableMap<Item,String> map = map5();
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
    public void testKeySetToArray() {
        ConcurrentNavigableMap<Item,String> map = map5();
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
        ConcurrentNavigableMap<Item,String> map = map5();
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
    public void testValuesToArray() {
        ConcurrentNavigableMap<Item,String> map = map5();
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
    public void testEntrySet() {
        ConcurrentNavigableMap<Item,String> map = map5();
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
     * putAll adds all key-value pairs from the given map
     */
    public void testPutAll() {
        ConcurrentNavigableMap<Item,String> empty = map0();
        ConcurrentNavigableMap<Item,String> map = map5();
        empty.putAll(map);
        mustEqual(5, empty.size());
        assertTrue(empty.containsKey(one));
        assertTrue(empty.containsKey(two));
        assertTrue(empty.containsKey(three));
        assertTrue(empty.containsKey(four));
        assertTrue(empty.containsKey(five));
    }

    /**
     * putIfAbsent works when the given key is not present
     */
    public void testPutIfAbsent() {
        ConcurrentNavigableMap<Item,String> map = map5();
        map.putIfAbsent(six, "Z");
        assertTrue(map.containsKey(six));
    }

    /**
     * putIfAbsent does not add the pair if the key is already present
     */
    public void testPutIfAbsent2() {
        ConcurrentNavigableMap<Item,String> map = map5();
        mustEqual("A", map.putIfAbsent(one, "Z"));
    }

    /**
     * replace fails when the given key is not present
     */
    public void testReplace() {
        ConcurrentNavigableMap<Item,String> map = map5();
        assertNull(map.replace(six, "Z"));
        assertFalse(map.containsKey(six));
    }

    /**
     * replace succeeds if the key is already present
     */
    public void testReplace2() {
        ConcurrentNavigableMap<Item,String> map = map5();
        assertNotNull(map.replace(one, "Z"));
        mustEqual("Z", map.get(one));
    }

    /**
     * replace value fails when the given key not mapped to expected value
     */
    public void testReplaceValue() {
        ConcurrentNavigableMap<Item,String> map = map5();
        mustEqual("A", map.get(one));
        assertFalse(map.replace(one, "Z", "Z"));
        mustEqual("A", map.get(one));
    }

    /**
     * replace value succeeds when the given key mapped to expected value
     */
    public void testReplaceValue2() {
        ConcurrentNavigableMap<Item,String> map = map5();
        mustEqual("A", map.get(one));
        assertTrue(map.replace(one, "A", "Z"));
        mustEqual("Z", map.get(one));
    }

    /**
     * remove removes the correct key-value pair from the map
     */
    public void testRemove() {
        ConcurrentNavigableMap<Item,String> map = map5();
        map.remove(five);
        mustEqual(4, map.size());
        assertFalse(map.containsKey(five));
    }

    /**
     * remove(key,value) removes only if pair present
     */
    public void testRemove2() {
        ConcurrentNavigableMap<Item,String> map = map5();
        assertTrue(map.containsKey(five));
        mustEqual("E", map.get(five));
        map.remove(five, "E");
        mustEqual(4, map.size());
        assertFalse(map.containsKey(five));
        map.remove(four, "A");
        mustEqual(4, map.size());
        assertTrue(map.containsKey(four));
    }

    /**
     * lowerEntry returns preceding entry.
     */
    public void testLowerEntry() {
        ConcurrentNavigableMap<Item,String> map = map5();
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
        ConcurrentNavigableMap<Item,String> map = map5();
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
        ConcurrentNavigableMap<Item,String> map = map5();
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
        ConcurrentNavigableMap<Item,String> map = map5();
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
        ConcurrentNavigableMap<Item,String> map = map5();
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
        ConcurrentNavigableMap<Item,String> map = map5();
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
        ConcurrentNavigableMap<Item,String> map = map5();
        ConcurrentNavigableMap<Item,String> empty = map0();
        mustEqual(0, empty.size());
        mustEqual(5, map.size());
    }

    /**
     * toString contains toString of elements
     */
    public void testToString() {
        ConcurrentNavigableMap<Item,String> map = map5();
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
        try {
            ConcurrentNavigableMap<Item,String> c = map5();
            c.get(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * containsKey(null) of nonempty map throws NPE
     */
    public void testContainsKey_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = map5();
            c.containsKey(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * containsValue(null) throws NPE
     */
    public void testContainsValue_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = map0();
            c.containsValue(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * put(null,x) throws NPE
     */
    public void testPut1_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = map5();
            c.put(null, "whatever");
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * putIfAbsent(null, x) throws NPE
     */
    public void testPutIfAbsent1_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = map5();
            c.putIfAbsent(null, "whatever");
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * replace(null, x) throws NPE
     */
    public void testReplace_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = map5();
            c.replace(null, "A");
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * replace(null, x, y) throws NPE
     */
    public void testReplaceValue_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = map5();
            c.replace(null, "A", "B");
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * remove(null) throws NPE
     */
    public void testRemove1_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = map5();
            c.remove(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * remove(null, x) throws NPE
     */
    public void testRemove2_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = map5();
            c.remove(null, "whatever");
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
        ConcurrentNavigableMap<Item,String> map = map5();
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
        ConcurrentNavigableMap<Item,String> map = map5();
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
        Item k;
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
        assertSame(sm.remove(three), null);
        mustEqual(4, map.size());
    }

    /**
     * headMap returns map with keys in requested range
     */
    public void testHeadMapContents() {
        ConcurrentNavigableMap<Item,String> map = map5();
        SortedMap<Item,String> sm = map.headMap(four);
        assertTrue(sm.containsKey(one));
        assertTrue(sm.containsKey(two));
        assertTrue(sm.containsKey(three));
        assertFalse(sm.containsKey(four));
        assertFalse(sm.containsKey(five));
        Iterator<? extends Item> i = sm.keySet().iterator();
        Object k;
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
        ConcurrentNavigableMap<Item,String> map = map5();
        SortedMap<Item,String> sm = map.tailMap(two);
        assertFalse(sm.containsKey(one));
        assertTrue(sm.containsKey(two));
        assertTrue(sm.containsKey(three));
        assertTrue(sm.containsKey(four));
        assertTrue(sm.containsKey(five));
        Iterator<Item> i = sm.keySet().iterator();
        Item k = i.next();
        mustEqual(two, k);
        k = i.next();
        mustEqual(three, k);
        k = i.next();
        mustEqual(four, k);
        k = i.next();
        mustEqual(five, k);
        assertFalse(i.hasNext());

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
        ConcurrentNavigableMap<Item,String> map = dmap5();
        map.clear();
        mustEqual(0, map.size());
    }

    /**
     * Maps with same contents are equal
     */
    public void testDescendingEquals() {
        ConcurrentNavigableMap<Item,String> map1 = dmap5();
        ConcurrentNavigableMap<Item,String> map2 = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
        assertTrue(map.containsKey(minusOne));
        assertFalse(map.containsKey(zero));
    }

    /**
     * containsValue returns true for held values
     */
    public void testDescendingContainsValue() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        assertTrue(map.containsValue("A"));
        assertFalse(map.containsValue("Z"));
    }

    /**
     * get returns the correct element at the given key,
     * or null if not present
     */
    public void testDescendingGet() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        mustEqual("A", map.get(minusOne));
        ConcurrentNavigableMap<Item,String> empty = dmap0();
        assertNull(empty.get(minusOne));
    }

    /**
     * isEmpty is true of empty map and false for non-empty
     */
    public void testDescendingIsEmpty() {
        ConcurrentNavigableMap<Item,String> empty = dmap0();
        ConcurrentNavigableMap<Item,String> map = dmap5();
        assertTrue(empty.isEmpty());
        assertFalse(map.isEmpty());
    }

    /**
     * firstKey returns first key
     */
    public void testDescendingFirstKey() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        mustEqual(minusOne, map.firstKey());
    }

    /**
     * lastKey returns last key
     */
    public void testDescendingLastKey() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        mustEqual(minusFive, map.lastKey());
    }

    /**
     * keySet returns a Set containing all the keys
     */
    public void testDescendingKeySet() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
        Set<Item> s = map.keySet();
        Iterator<? extends Item> i = s.iterator();
        Item last = i.next();
        mustEqual(last, minusOne);
        while (i.hasNext()) {
            Item k = i.next();
            assertTrue(last.compareTo(k) > 0);
            last = k;
        }
    }

    /**
     * values collection contains all values
     */
    public void testDescendingValues() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> empty = dmap0();
        ConcurrentNavigableMap<Item,String> map = dmap5();
        empty.putAll(map);
        mustEqual(5, empty.size());
        assertTrue(empty.containsKey(minusOne));
        assertTrue(empty.containsKey(minusTwo));
        assertTrue(empty.containsKey(minusThree));
        assertTrue(empty.containsKey(minusFour));
        assertTrue(empty.containsKey(minusFive));
    }

    /**
     * putIfAbsent works when the given key is not present
     */
    public void testDescendingPutIfAbsent() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        map.putIfAbsent(six, "Z");
        assertTrue(map.containsKey(six));
    }

    /**
     * putIfAbsent does not add the pair if the key is already present
     */
    public void testDescendingPutIfAbsent2() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        mustEqual("A", map.putIfAbsent(minusOne, "Z"));
    }

    /**
     * replace fails when the given key is not present
     */
    public void testDescendingReplace() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        assertNull(map.replace(six, "Z"));
        assertFalse(map.containsKey(six));
    }

    /**
     * replace succeeds if the key is already present
     */
    public void testDescendingReplace2() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        assertNotNull(map.replace(minusOne, "Z"));
        mustEqual("Z", map.get(minusOne));
    }

    /**
     * replace value fails when the given key not mapped to expected value
     */
    public void testDescendingReplaceValue() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        mustEqual("A", map.get(minusOne));
        assertFalse(map.replace(minusOne, "Z", "Z"));
        mustEqual("A", map.get(minusOne));
    }

    /**
     * replace value succeeds when the given key mapped to expected value
     */
    public void testDescendingReplaceValue2() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        mustEqual("A", map.get(minusOne));
        assertTrue(map.replace(minusOne, "A", "Z"));
        mustEqual("Z", map.get(minusOne));
    }

    /**
     * remove removes the correct key-value pair from the map
     */
    public void testDescendingRemove() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        map.remove(minusFive);
        mustEqual(4, map.size());
        assertFalse(map.containsKey(minusFive));
    }

    /**
     * remove(key,value) removes only if pair present
     */
    public void testDescendingRemove2() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        assertTrue(map.containsKey(minusFive));
        mustEqual("E", map.get(minusFive));
        map.remove(minusFive, "E");
        mustEqual(4, map.size());
        assertFalse(map.containsKey(minusFive));
        map.remove(minusFour, "A");
        mustEqual(4, map.size());
        assertTrue(map.containsKey(minusFour));
    }

    /**
     * lowerEntry returns preceding entry.
     */
    public void testDescendingLowerEntry() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
        ConcurrentNavigableMap<Item,String> empty = dmap0();
        mustEqual(0, empty.size());
        mustEqual(5, map.size());
    }

    /**
     * toString contains toString of elements
     */
    public void testDescendingToString() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
        String s = map.toString();
        for (int i = 1; i <= 5; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    // Exception testDescendings

    /**
     * get(null) of empty map throws NPE
     */
    public void testDescendingGet_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = dmap5();
            c.get(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * containsKey(null) of empty map throws NPE
     */
    public void testDescendingContainsKey_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = dmap5();
            c.containsKey(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * containsValue(null) throws NPE
     */
    public void testDescendingContainsValue_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = dmap0();
            c.containsValue(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * put(null,x) throws NPE
     */
    public void testDescendingPut1_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = dmap5();
            c.put(null, "whatever");
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * putIfAbsent(null, x) throws NPE
     */
    public void testDescendingPutIfAbsent1_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = dmap5();
            c.putIfAbsent(null, "whatever");
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * replace(null, x) throws NPE
     */
    public void testDescendingReplace_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = dmap5();
            c.replace(null, "whatever");
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * replace(null, x, y) throws NPE
     */
    public void testDescendingReplaceValue_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = dmap5();
            c.replace(null, "A", "B");
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * remove(null) throws NPE
     */
    public void testDescendingRemove1_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = dmap5();
            c.remove(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * remove(null, x) throws NPE
     */
    public void testDescendingRemove2_NullPointerException() {
        try {
            ConcurrentNavigableMap<Item,String> c = dmap5();
            c.remove(null, "whatever");
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        assertSame(sm.remove(minusThree), null);
        mustEqual(4, map.size());
    }

    /**
     * headMap returns map with keys in requested range
     */
    public void testDescendingHeadMapContents() {
        ConcurrentNavigableMap<Item,String> map = dmap5();
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
        ConcurrentNavigableMap<Item,String> map = dmap5();
        SortedMap<Item,String> sm = map.tailMap(minusTwo);
        assertFalse(sm.containsKey(minusOne));
        assertTrue(sm.containsKey(minusTwo));
        assertTrue(sm.containsKey(minusThree));
        assertTrue(sm.containsKey(minusFour));
        assertTrue(sm.containsKey(minusFive));
        Iterator<? extends Item> i = sm.keySet().iterator();
        Item k = i.next();
        mustEqual(minusTwo, k);
        k = (i.next());
        mustEqual(minusThree, k);
        k = (i.next());
        mustEqual(minusFour, k);
        k = (i.next());
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
