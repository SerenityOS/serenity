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

import static java.util.Spliterator.CONCURRENT;
import static java.util.Spliterator.DISTINCT;
import static java.util.Spliterator.NONNULL;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiFunction;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ConcurrentHashMap8Test extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ConcurrentHashMap8Test.class);
    }

    /**
     * Returns a new map from Items 1-5 to Strings "A"-"E".
     */
    private static ConcurrentHashMap<Item,String> map5() {
        ConcurrentHashMap<Item,String> map = new ConcurrentHashMap<>(5);
        assertTrue(map.isEmpty());
        map.put(one, "A");
        map.put(two, "B");
        map.put(three, "C");
        map.put(four, "D");
        map.put(five, "E");
        assertFalse(map.isEmpty());
        mustEqual(5, map.size());
        return map;
    }

    /**
     * getOrDefault returns value if present, else default
     */
    public void testGetOrDefault() {
        ConcurrentHashMap<Item,String> map = map5();
        mustEqual(map.getOrDefault(one, "Z"), "A");
        mustEqual(map.getOrDefault(six, "Z"), "Z");
    }

    /**
     * computeIfAbsent adds when the given key is not present
     */
    public void testComputeIfAbsent() {
        ConcurrentHashMap<Item,String> map = map5();
        map.computeIfAbsent(six, x -> "Z");
        assertTrue(map.containsKey(six));
    }

    /**
     * computeIfAbsent does not replace if the key is already present
     */
    public void testComputeIfAbsent2() {
        ConcurrentHashMap<Item,String> map = map5();
        mustEqual("A", map.computeIfAbsent(one, x -> "Z"));
    }

    /**
     * computeIfAbsent does not add if function returns null
     */
    public void testComputeIfAbsent3() {
        ConcurrentHashMap<Item,String> map = map5();
        map.computeIfAbsent(six, x -> null);
        assertFalse(map.containsKey(six));
    }

    /**
     * computeIfPresent does not replace if the key is already present
     */
    public void testComputeIfPresent() {
        ConcurrentHashMap<Item,String> map = map5();
        map.computeIfPresent(six, (x, y) -> "Z");
        assertFalse(map.containsKey(six));
    }

    /**
     * computeIfPresent adds when the given key is not present
     */
    public void testComputeIfPresent2() {
        ConcurrentHashMap<Item,String> map = map5();
        mustEqual("Z", map.computeIfPresent(one, (x, y) -> "Z"));
    }

    /**
     * compute does not replace if the function returns null
     */
    public void testCompute() {
        ConcurrentHashMap<Item,String> map = map5();
        map.compute(six, (x, y) -> null);
        assertFalse(map.containsKey(six));
    }

    /**
     * compute adds when the given key is not present
     */
    public void testCompute2() {
        ConcurrentHashMap<Item,String> map = map5();
        mustEqual("Z", map.compute(six, (x, y) -> "Z"));
    }

    /**
     * compute replaces when the given key is present
     */
    public void testCompute3() {
        ConcurrentHashMap<Item,String> map = map5();
        mustEqual("Z", map.compute(one, (x, y) -> "Z"));
    }

    /**
     * compute removes when the given key is present and function returns null
     */
    public void testCompute4() {
        ConcurrentHashMap<Item,String> map = map5();
        map.compute(one, (x, y) -> null);
        assertFalse(map.containsKey(one));
    }

    /**
     * merge adds when the given key is not present
     */
    public void testMerge1() {
        ConcurrentHashMap<Item,String> map = map5();
        mustEqual("Y", map.merge(six, "Y", (x, y) -> "Z"));
    }

    /**
     * merge replaces when the given key is present
     */
    public void testMerge2() {
        ConcurrentHashMap<Item,String> map = map5();
        mustEqual("Z", map.merge(one, "Y", (x, y) -> "Z"));
    }

    /**
     * merge removes when the given key is present and function returns null
     */
    public void testMerge3() {
        ConcurrentHashMap<Item,String> map = map5();
        map.merge(one, "Y", (x, y) -> null);
        assertFalse(map.containsKey(one));
    }

    static Set<Item> populatedSet(int n) {
        Set<Item> a = ConcurrentHashMap.<Item>newKeySet();
        assertTrue(a.isEmpty());
        for (int i = 0; i < n; i++)
            mustAdd(a, i);
        mustEqual(n == 0, a.isEmpty());
        mustEqual(n, a.size());
        return a;
    }

    static Set<Item> populatedSet(Item[] elements) {
        Set<Item> a = ConcurrentHashMap.<Item>newKeySet();
        assertTrue(a.isEmpty());
        for (Item element : elements)
            assertTrue(a.add(element));
        assertFalse(a.isEmpty());
        mustEqual(elements.length, a.size());
        return a;
    }

    /**
     * replaceAll replaces all matching values.
     */
    public void testReplaceAll() {
        ConcurrentHashMap<Item, String> map = map5();
        map.replaceAll((x, y) -> (x.value > 3) ? "Z" : y);
        mustEqual("A", map.get(one));
        mustEqual("B", map.get(two));
        mustEqual("C", map.get(three));
        mustEqual("Z", map.get(four));
        mustEqual("Z", map.get(five));
    }

    /**
     * Default-constructed set is empty
     */
    public void testNewKeySet() {
        Set<Item> a = ConcurrentHashMap.<Item>newKeySet();
        assertTrue(a.isEmpty());
    }

    /**
     * keySet.add adds the key with the established value to the map;
     * remove removes it.
     */
    public void testKeySetAddRemove() {
        ConcurrentHashMap<Item,String> map = map5();
        Set<Item> set1 = map.keySet();
        Set<Item> set2 = map.keySet("added");
        set2.add(six);
        assertSame(map, ((ConcurrentHashMap.KeySetView)set2).getMap());
        assertSame(map, ((ConcurrentHashMap.KeySetView)set1).getMap());
        mustEqual(set2.size(), map.size());
        mustEqual(set1.size(), map.size());
        assertEquals(map.get(six), "added");
        mustContain(set1, six);
        mustContain(set2, six);
        mustRemove(set2, six);
        assertNull(map.get(six));
        mustNotContain(set1, six);
        mustNotContain(set2, six);
    }

    /**
     * keySet.addAll adds each element from the given collection
     */
    public void testAddAll() {
        Set<Item> full = populatedSet(3);
        assertTrue(full.addAll(Arrays.asList(three, four, five)));
        mustEqual(6, full.size());
        assertFalse(full.addAll(Arrays.asList(three, four, five)));
        mustEqual(6, full.size());
    }

    /**
     * keySet.addAll adds each element from the given collection that did not
     * already exist in the set
     */
    public void testAddAll2() {
        Set<Item> full = populatedSet(3);
        // "one" is duplicate and will not be added
        assertTrue(full.addAll(Arrays.asList(three, four, one)));
        mustEqual(5, full.size());
        assertFalse(full.addAll(Arrays.asList(three, four, one)));
        mustEqual(5, full.size());
    }

    /**
     * keySet.add will not add the element if it already exists in the set
     */
    public void testAdd2() {
        Set<Item> full = populatedSet(3);
        assertFalse(full.add(one));
        mustEqual(3, full.size());
    }

    /**
     * keySet.add adds the element when it does not exist in the set
     */
    public void testAdd3() {
        Set<Item> full = populatedSet(3);
        assertTrue(full.add(three));
        mustContain(full, three);
        assertFalse(full.add(three));
        mustContain(full, three);
    }

    /**
     * keySet.add throws UnsupportedOperationException if no default
     * mapped value
     */
    public void testAdd4() {
        Set<Item> full = map5().keySet();
        try {
            full.add(three);
            shouldThrow();
        } catch (UnsupportedOperationException success) {}
    }

    /**
     * keySet.add throws NullPointerException if the specified key is
     * null
     */
    public void testAdd5() {
        Set<Item> full = populatedSet(3);
        try {
            full.add(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * KeySetView.getMappedValue returns the map's mapped value
     */
    public void testGetMappedValue() {
        ConcurrentHashMap<Item,String> map = map5();
        assertNull(map.keySet().getMappedValue());
        String added = "added";
        try {
            map.keySet(null);
            shouldThrow();
        } catch (NullPointerException success) {}
        ConcurrentHashMap.KeySetView<Item,String> set = map.keySet(added);
        assertFalse(set.add(one));
        assertTrue(set.add(six));
        assertTrue(set.add(seven));
        assertSame(added, set.getMappedValue());
        assertNotSame(added, map.get(one));
        assertSame(added, map.get(six));
        assertSame(added, map.get(seven));
    }

    void checkSpliteratorCharacteristics(Spliterator<?> sp,
                                         int requiredCharacteristics) {
        mustEqual(requiredCharacteristics,
                     requiredCharacteristics & sp.characteristics());
    }

    /**
     * KeySetView.spliterator returns spliterator over the elements in this set
     */
    public void testKeySetSpliterator() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Item,String> map = map5();
        Set<Item> set = map.keySet();
        Spliterator<Item> sp = set.spliterator();
        checkSpliteratorCharacteristics(sp, CONCURRENT | DISTINCT | NONNULL);
        mustEqual(sp.estimateSize(), map.size());
        Spliterator<Item> sp2 = sp.trySplit();
        sp.forEachRemaining((Item x) -> adder.add(x.longValue()));
        long v = adder.sumThenReset();
        sp2.forEachRemaining((Item x) -> adder.add(x.longValue()));
        long v2 = adder.sum();
        mustEqual(v + v2, 15);
    }

    /**
     * keyset.clear removes all elements from the set
     */
    public void testClear() {
        Set<Item> full = populatedSet(3);
        full.clear();
        mustEqual(0, full.size());
    }

    /**
     * keyset.contains returns true for added elements
     */
    public void testContains() {
        Set<Item> full = populatedSet(3);
        mustContain(full, one);
        mustNotContain(full, five);
    }

    /**
     * KeySets with equal elements are equal
     */
    public void testEquals() {
        Set<Item> a = populatedSet(3);
        Set<Item> b = populatedSet(3);
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        mustEqual(a.hashCode(), b.hashCode());
        a.add(minusOne);
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
        b.add(minusOne);
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        mustEqual(a.hashCode(), b.hashCode());
    }

    /**
     * KeySet.containsAll returns true for collections with subset of elements
     */
    public void testContainsAll() {
        Collection<Item> full = populatedSet(3);
        assertTrue(full.containsAll(Arrays.asList()));
        assertTrue(full.containsAll(Arrays.asList(one)));
        assertTrue(full.containsAll(Arrays.asList(one, two)));
        assertFalse(full.containsAll(Arrays.asList(one, two, six)));
        assertFalse(full.containsAll(Arrays.asList(six)));
    }

    /**
     * KeySet.isEmpty is true when empty, else false
     */
    public void testIsEmpty() {
        assertTrue(populatedSet(0).isEmpty());
        assertFalse(populatedSet(3).isEmpty());
    }

    /**
     * KeySet.iterator() returns an iterator containing the elements of the
     * set
     */
    public void testIterator() {
        Collection<Item> empty = ConcurrentHashMap.<Item>newKeySet();
        int size = 20;
        assertFalse(empty.iterator().hasNext());
        try {
            empty.iterator().next();
            shouldThrow();
        } catch (NoSuchElementException success) {}

        Item[] elements = seqItems(size);
        shuffle(elements);
        Collection<Item> full = populatedSet(elements);

        Iterator<? extends Item> it = full.iterator();
        for (int j = 0; j < size; j++) {
            assertTrue(it.hasNext());
            it.next();
        }
        assertIteratorExhausted(it);
    }

    /**
     * iterator of empty collections has no elements
     */
    public void testEmptyIterator() {
        assertIteratorExhausted(ConcurrentHashMap.newKeySet().iterator());
        assertIteratorExhausted(new ConcurrentHashMap<Item,String>().entrySet().iterator());
        assertIteratorExhausted(new ConcurrentHashMap<Item,String>().values().iterator());
        assertIteratorExhausted(new ConcurrentHashMap<Item,String>().keySet().iterator());
    }

    /**
     * KeySet.iterator.remove removes current element
     */
    public void testIteratorRemove() {
        Set<Item> q = populatedSet(3);
        Iterator<Item> it = q.iterator();
        Object removed = it.next();
        it.remove();

        it = q.iterator();
        assertFalse(it.next().equals(removed));
        assertFalse(it.next().equals(removed));
        assertFalse(it.hasNext());
    }

    /**
     * KeySet.toString holds toString of elements
     */
    public void testToString() {
        mustEqual("[]", ConcurrentHashMap.newKeySet().toString());
        Set<Item> full = populatedSet(3);
        String s = full.toString();
        for (int i = 0; i < 3; ++i)
            assertTrue(s.contains(String.valueOf(i)));
    }

    /**
     * KeySet.removeAll removes all elements from the given collection
     */
    public void testRemoveAll() {
        Set<Item> full = populatedSet(3);
        assertTrue(full.removeAll(Arrays.asList(one, two)));
        mustEqual(1, full.size());
        assertFalse(full.removeAll(Arrays.asList(one, two)));
        mustEqual(1, full.size());
    }

    /**
     * KeySet.remove removes an element
     */
    public void testRemove() {
        Set<Item> full = populatedSet(3);
        full.remove(one);
        mustNotContain(full, one);
        mustEqual(2, full.size());
    }

    /**
     * keySet.size returns the number of elements
     */
    public void testSize() {
        Set<Item> empty = ConcurrentHashMap.newKeySet();
        Set<Item> full = populatedSet(3);
        mustEqual(3, full.size());
        mustEqual(0, empty.size());
    }

    /**
     * KeySet.toArray() returns an Object array containing all elements from
     * the set
     */
    public void testToArray() {
        Object[] a = ConcurrentHashMap.newKeySet().toArray();
        assertTrue(Arrays.equals(new Object[0], a));
        assertSame(Object[].class, a.getClass());
        int size = 20;
        Item[] elements = seqItems(size);
        shuffle(elements);
        Collection<Item> full = populatedSet(elements);

        assertTrue(Arrays.asList(elements).containsAll(Arrays.asList(full.toArray())));
        assertTrue(full.containsAll(Arrays.asList(full.toArray())));
        assertSame(Object[].class, full.toArray().getClass());
    }

    /**
     * toArray(Item array) returns an Item array containing all
     * elements from the set
     */
    public void testToArray2() {
        Collection<Item> empty = ConcurrentHashMap.<Item>newKeySet();
        Item[] a;
        int size = 20;

        a = new Item[0];
        assertSame(a, empty.toArray(a));

        a = new Item[size / 2];
        Arrays.fill(a, fortytwo);
        assertSame(a, empty.toArray(a));
        assertNull(a[0]);
        for (int i = 1; i < a.length; i++)
            mustEqual(42, a[i]);

        Item[] elements = seqItems(size);
        shuffle(elements);
        Collection<Item> full = populatedSet(elements);

        Arrays.fill(a, fortytwo);
        assertTrue(Arrays.asList(elements).containsAll(Arrays.asList(full.toArray(a))));
        for (int i = 0; i < a.length; i++)
            mustEqual(42, a[i]);
        assertSame(Item[].class, full.toArray(a).getClass());

        a = new Item[size];
        Arrays.fill(a, fortytwo);
        assertSame(a, full.toArray(a));
        assertTrue(Arrays.asList(elements).containsAll(Arrays.asList(full.toArray(a))));
    }

    /**
     * A deserialized/reserialized set equals original
     */
    public void testSerialization() throws Exception {
        int size = 20;
        Set<Item> x = populatedSet(size);
        Set<Item> y = serialClone(x);

        assertNotSame(x, y);
        mustEqual(x.size(), y.size());
        mustEqual(x, y);
        mustEqual(y, x);
    }

    static final int SIZE = 10000;
    static ConcurrentHashMap<Long, Long> longMap;

    static ConcurrentHashMap<Long, Long> longMap() {
        if (longMap == null) {
            longMap = new ConcurrentHashMap<>(SIZE);
            for (int i = 0; i < SIZE; ++i)
                longMap.put(Long.valueOf(i), Long.valueOf(2 *i));
        }
        return longMap;
    }

    // explicit function class to avoid type inference problems
    static class AddKeys implements BiFunction<Map.Entry<Long,Long>, Map.Entry<Long,Long>, Map.Entry<Long,Long>> {
        public Map.Entry<Long,Long> apply(Map.Entry<Long,Long> x, Map.Entry<Long,Long> y) {
            return new AbstractMap.SimpleEntry<Long,Long>
             (Long.valueOf(x.getKey().longValue() + y.getKey().longValue()),
              Long.valueOf(1L));
        }
    }

    /**
     * forEachKeySequentially traverses all keys
     */
    public void testForEachKeySequentially() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEachKey(Long.MAX_VALUE, (Long x) -> adder.add(x.longValue()));
        mustEqual(adder.sum(), SIZE * (SIZE - 1) / 2);
    }

    /**
     * forEachValueSequentially traverses all values
     */
    public void testForEachValueSequentially() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEachValue(Long.MAX_VALUE, (Long x) -> adder.add(x.longValue()));
        mustEqual(adder.sum(), SIZE * (SIZE - 1));
    }

    /**
     * forEachSequentially traverses all mappings
     */
    public void testForEachSequentially() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEach(Long.MAX_VALUE, (Long x, Long y) -> adder.add(x.longValue() + y.longValue()));
        mustEqual(adder.sum(), 3 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * forEachEntrySequentially traverses all entries
     */
    public void testForEachEntrySequentially() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEachEntry(Long.MAX_VALUE, (Map.Entry<Long,Long> e) -> adder.add(e.getKey().longValue() + e.getValue().longValue()));
        mustEqual(adder.sum(), 3 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * forEachKeyInParallel traverses all keys
     */
    public void testForEachKeyInParallel() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEachKey(1L, (Long x) -> adder.add(x.longValue()));
        mustEqual(adder.sum(), SIZE * (SIZE - 1) / 2);
    }

    /**
     * forEachValueInParallel traverses all values
     */
    public void testForEachValueInParallel() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEachValue(1L, (Long x) -> adder.add(x.longValue()));
        mustEqual(adder.sum(), SIZE * (SIZE - 1));
    }

    /**
     * forEachInParallel traverses all mappings
     */
    public void testForEachInParallel() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEach(1L, (Long x, Long y) -> adder.add(x.longValue() + y.longValue()));
        mustEqual(adder.sum(), 3 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * forEachEntryInParallel traverses all entries
     */
    public void testForEachEntryInParallel() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEachEntry(1L, (Map.Entry<Long,Long> e) -> adder.add(e.getKey().longValue() + e.getValue().longValue()));
        mustEqual(adder.sum(), 3 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * Mapped forEachKeySequentially traverses the given
     * transformations of all keys
     */
    public void testMappedForEachKeySequentially() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEachKey(Long.MAX_VALUE, (Long x) -> Long.valueOf(4 * x.longValue()),
                                 (Long x) -> adder.add(x.longValue()));
        mustEqual(adder.sum(), 4 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * Mapped forEachValueSequentially traverses the given
     * transformations of all values
     */
    public void testMappedForEachValueSequentially() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEachValue(Long.MAX_VALUE, (Long x) -> Long.valueOf(4 * x.longValue()),
                                   (Long x) -> adder.add(x.longValue()));
        mustEqual(adder.sum(), 4 * SIZE * (SIZE - 1));
    }

    /**
     * Mapped forEachSequentially traverses the given
     * transformations of all mappings
     */
    public void testMappedForEachSequentially() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEach(Long.MAX_VALUE, (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()),
                              (Long x) -> adder.add(x.longValue()));
        mustEqual(adder.sum(), 3 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * Mapped forEachEntrySequentially traverses the given
     * transformations of all entries
     */
    public void testMappedForEachEntrySequentially() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEachEntry(Long.MAX_VALUE, (Map.Entry<Long,Long> e) -> Long.valueOf(e.getKey().longValue() + e.getValue().longValue()),
                                   (Long x) -> adder.add(x.longValue()));
        mustEqual(adder.sum(), 3 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * Mapped forEachKeyInParallel traverses the given
     * transformations of all keys
     */
    public void testMappedForEachKeyInParallel() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEachKey(1L, (Long x) -> Long.valueOf(4 * x.longValue()),
                               (Long x) -> adder.add(x.longValue()));
        mustEqual(adder.sum(), 4 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * Mapped forEachValueInParallel traverses the given
     * transformations of all values
     */
    public void testMappedForEachValueInParallel() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEachValue(1L, (Long x) -> Long.valueOf(4 * x.longValue()),
                                 (Long x) -> adder.add(x.longValue()));
        mustEqual(adder.sum(), 4 * SIZE * (SIZE - 1));
    }

    /**
     * Mapped forEachInParallel traverses the given
     * transformations of all mappings
     */
    public void testMappedForEachInParallel() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEach(1L, (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()),
                            (Long x) -> adder.add(x.longValue()));
        mustEqual(adder.sum(), 3 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * Mapped forEachEntryInParallel traverses the given
     * transformations of all entries
     */
    public void testMappedForEachEntryInParallel() {
        LongAdder adder = new LongAdder();
        ConcurrentHashMap<Long, Long> m = longMap();
        m.forEachEntry(1L, (Map.Entry<Long,Long> e) -> Long.valueOf(e.getKey().longValue() + e.getValue().longValue()),
                                 (Long x) -> adder.add(x.longValue()));
        mustEqual(adder.sum(), 3 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * reduceKeysSequentially accumulates across all keys,
     */
    public void testReduceKeysSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.reduceKeys(Long.MAX_VALUE, (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()));
        mustEqual((long)r, (long)SIZE * (SIZE - 1) / 2);
    }

    /**
     * reduceValuesSequentially accumulates across all values
     */
    public void testReduceValuesSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.reduceKeys(Long.MAX_VALUE, (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()));
        mustEqual((long)r, (long)SIZE * (SIZE - 1) / 2);
    }

    /**
     * reduceEntriesSequentially accumulates across all entries
     */
    public void testReduceEntriesSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Map.Entry<Long,Long> r;
        r = m.reduceEntries(Long.MAX_VALUE, new AddKeys());
        mustEqual(r.getKey().longValue(), (long)SIZE * (SIZE - 1) / 2);
    }

    /**
     * reduceKeysInParallel accumulates across all keys
     */
    public void testReduceKeysInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.reduceKeys(1L, (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()));
        mustEqual((long)r, (long)SIZE * (SIZE - 1) / 2);
    }

    /**
     * reduceValuesInParallel accumulates across all values
     */
    public void testReduceValuesInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.reduceValues(1L, (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()));
        mustEqual((long)r, (long)SIZE * (SIZE - 1));
    }

    /**
     * reduceEntriesInParallel accumulate across all entries
     */
    public void testReduceEntriesInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Map.Entry<Long,Long> r;
        r = m.reduceEntries(1L, new AddKeys());
        mustEqual(r.getKey().longValue(), (long)SIZE * (SIZE - 1) / 2);
    }

    /**
     * Mapped reduceKeysSequentially accumulates mapped keys
     */
    public void testMapReduceKeysSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r = m.reduceKeys(Long.MAX_VALUE, (Long x) -> Long.valueOf(4 * x.longValue()),
                                     (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()));
        mustEqual((long)r, (long)4 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * Mapped reduceValuesSequentially accumulates mapped values
     */
    public void testMapReduceValuesSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r = m.reduceValues(Long.MAX_VALUE, (Long x) -> Long.valueOf(4 * x.longValue()),
                                       (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()));
        mustEqual((long)r, (long)4 * SIZE * (SIZE - 1));
    }

    /**
     * reduceSequentially accumulates across all transformed mappings
     */
    public void testMappedReduceSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r = m.reduce(Long.MAX_VALUE, (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()),
                                 (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()));

        mustEqual((long)r, (long)3 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * Mapped reduceKeysInParallel, accumulates mapped keys
     */
    public void testMapReduceKeysInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r = m.reduceKeys(1L, (Long x) -> Long.valueOf(4 * x.longValue()),
                                   (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()));
        mustEqual((long)r, (long)4 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * Mapped reduceValuesInParallel accumulates mapped values
     */
    public void testMapReduceValuesInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r = m.reduceValues(1L, (Long x) -> Long.valueOf(4 * x.longValue()),
                                     (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()));
        mustEqual((long)r, (long)4 * SIZE * (SIZE - 1));
    }

    /**
     * reduceInParallel accumulate across all transformed mappings
     */
    public void testMappedReduceInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.reduce(1L, (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()),
                               (Long x, Long y) -> Long.valueOf(x.longValue() + y.longValue()));
        mustEqual((long)r, (long)3 * SIZE * (SIZE - 1) / 2);
    }

    /**
     * reduceKeysToLongSequentially accumulates mapped keys
     */
    public void testReduceKeysToLongSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        long lr = m.reduceKeysToLong(Long.MAX_VALUE, (Long x) -> x.longValue(), 0L, Long::sum);
        mustEqual(lr, (long)SIZE * (SIZE - 1) / 2);
    }

    /**
     * reduceKeysToIntSequentially accumulates mapped keys
     */
    public void testReduceKeysToIntSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        int ir = m.reduceKeysToInt(Long.MAX_VALUE, (Long x) -> x.intValue(), 0, Integer::sum);
        mustEqual(ir, SIZE * (SIZE - 1) / 2);
    }

    /**
     * reduceKeysToDoubleSequentially accumulates mapped keys
     */
    public void testReduceKeysToDoubleSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        double dr = m.reduceKeysToDouble(Long.MAX_VALUE, (Long x) -> x.doubleValue(), 0.0, Double::sum);
        mustEqual(dr, (double)SIZE * (SIZE - 1) / 2);
    }

    /**
     * reduceValuesToLongSequentially accumulates mapped values
     */
    public void testReduceValuesToLongSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        long lr = m.reduceValuesToLong(Long.MAX_VALUE, (Long x) -> x.longValue(), 0L, Long::sum);
        mustEqual(lr, (long)SIZE * (SIZE - 1));
    }

    /**
     * reduceValuesToIntSequentially accumulates mapped values
     */
    public void testReduceValuesToIntSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        int ir = m.reduceValuesToInt(Long.MAX_VALUE, (Long x) -> x.intValue(), 0, Integer::sum);
        mustEqual(ir, SIZE * (SIZE - 1));
    }

    /**
     * reduceValuesToDoubleSequentially accumulates mapped values
     */
    public void testReduceValuesToDoubleSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        double dr = m.reduceValuesToDouble(Long.MAX_VALUE, (Long x) -> x.doubleValue(), 0.0, Double::sum);
        mustEqual(dr, (double)SIZE * (SIZE - 1));
    }

    /**
     * reduceKeysToLongInParallel accumulates mapped keys
     */
    public void testReduceKeysToLongInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        long lr = m.reduceKeysToLong(1L, (Long x) -> x.longValue(), 0L, Long::sum);
        mustEqual(lr, (long)SIZE * (SIZE - 1) / 2);
    }

    /**
     * reduceKeysToIntInParallel accumulates mapped keys
     */
    public void testReduceKeysToIntInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        int ir = m.reduceKeysToInt(1L, (Long x) -> x.intValue(), 0, Integer::sum);
        mustEqual(ir, SIZE * (SIZE - 1) / 2);
    }

    /**
     * reduceKeysToDoubleInParallel accumulates mapped values
     */
    public void testReduceKeysToDoubleInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        double dr = m.reduceKeysToDouble(1L, (Long x) -> x.doubleValue(), 0.0, Double::sum);
        mustEqual(dr, (double)SIZE * (SIZE - 1) / 2);
    }

    /**
     * reduceValuesToLongInParallel accumulates mapped values
     */
    public void testReduceValuesToLongInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        long lr = m.reduceValuesToLong(1L, (Long x) -> x.longValue(), 0L, Long::sum);
        mustEqual(lr, (long)SIZE * (SIZE - 1));
    }

    /**
     * reduceValuesToIntInParallel accumulates mapped values
     */
    public void testReduceValuesToIntInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        int ir = m.reduceValuesToInt(1L, (Long x) -> x.intValue(), 0, Integer::sum);
        mustEqual(ir, SIZE * (SIZE - 1));
    }

    /**
     * reduceValuesToDoubleInParallel accumulates mapped values
     */
    public void testReduceValuesToDoubleInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        double dr = m.reduceValuesToDouble(1L, (Long x) -> x.doubleValue(), 0.0, Double::sum);
        mustEqual(dr, (double)SIZE * (SIZE - 1));
    }

    /**
     * searchKeysSequentially returns a non-null result of search
     * function, or null if none
     */
    public void testSearchKeysSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.searchKeys(Long.MAX_VALUE, (Long x) -> x.longValue() == (long)(SIZE/2) ? x : null);
        mustEqual((long)r, (long)(SIZE/2));
        r = m.searchKeys(Long.MAX_VALUE, (Long x) -> x.longValue() < 0L ? x : null);
        assertNull(r);
    }

    /**
     * searchValuesSequentially returns a non-null result of search
     * function, or null if none
     */
    public void testSearchValuesSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.searchValues(Long.MAX_VALUE,
            (Long x) -> (x.longValue() == (long)(SIZE/2)) ? x : null);
        mustEqual((long)r, (long)(SIZE/2));
        r = m.searchValues(Long.MAX_VALUE,
            (Long x) -> (x.longValue() < 0L) ? x : null);
        assertNull(r);
    }

    /**
     * searchSequentially returns a non-null result of search
     * function, or null if none
     */
    public void testSearchSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.search(Long.MAX_VALUE, (Long x, Long y) -> x.longValue() == (long)(SIZE/2) ? x : null);
        mustEqual((long)r, (long)(SIZE/2));
        r = m.search(Long.MAX_VALUE, (Long x, Long y) -> x.longValue() < 0L ? x : null);
        assertNull(r);
    }

    /**
     * searchEntriesSequentially returns a non-null result of search
     * function, or null if none
     */
    public void testSearchEntriesSequentially() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.searchEntries(Long.MAX_VALUE, (Map.Entry<Long,Long> e) -> e.getKey().longValue() == (long)(SIZE/2) ? e.getKey() : null);
        mustEqual((long)r, (long)(SIZE/2));
        r = m.searchEntries(Long.MAX_VALUE, (Map.Entry<Long,Long> e) -> e.getKey().longValue() < 0L ? e.getKey() : null);
        assertNull(r);
    }

    /**
     * searchKeysInParallel returns a non-null result of search
     * function, or null if none
     */
    public void testSearchKeysInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.searchKeys(1L, (Long x) -> x.longValue() == (long)(SIZE/2) ? x : null);
        mustEqual((long)r, (long)(SIZE/2));
        r = m.searchKeys(1L, (Long x) -> x.longValue() < 0L ? x : null);
        assertNull(r);
    }

    /**
     * searchValuesInParallel returns a non-null result of search
     * function, or null if none
     */
    public void testSearchValuesInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.searchValues(1L, (Long x) -> x.longValue() == (long)(SIZE/2) ? x : null);
        mustEqual((long)r, (long)(SIZE/2));
        r = m.searchValues(1L, (Long x) -> x.longValue() < 0L ? x : null);
        assertNull(r);
    }

    /**
     * searchInParallel returns a non-null result of search function,
     * or null if none
     */
    public void testSearchInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.search(1L, (Long x, Long y) -> x.longValue() == (long)(SIZE/2) ? x : null);
        mustEqual((long)r, (long)(SIZE/2));
        r = m.search(1L, (Long x, Long y) -> x.longValue() < 0L ? x : null);
        assertNull(r);
    }

    /**
     * searchEntriesInParallel returns a non-null result of search
     * function, or null if none
     */
    public void testSearchEntriesInParallel() {
        ConcurrentHashMap<Long, Long> m = longMap();
        Long r;
        r = m.searchEntries(1L, (Map.Entry<Long,Long> e) -> e.getKey().longValue() == (long)(SIZE/2) ? e.getKey() : null);
        mustEqual((long)r, (long)(SIZE/2));
        r = m.searchEntries(1L, (Map.Entry<Long,Long> e) -> e.getKey().longValue() < 0L ? e.getKey() : null);
        assertNull(r);
    }

    /**
     * Tests performance of computeIfAbsent when the element is present.
     * See JDK-8161372
     * ant -Djsr166.tckTestClass=ConcurrentHashMapTest -Djsr166.methodFilter=testcomputeIfAbsent_performance -Djsr166.expensiveTests=true tck
     */
    public void testcomputeIfAbsent_performance() {
        final int mapSize = 20;
        final int iterations = expensiveTests ? (1 << 23) : mapSize * 2;
        final int threads = expensiveTests ? 10 : 2;
        final ConcurrentHashMap<Item, Item> map = new ConcurrentHashMap<>();
        for (int i = 0; i < mapSize; i++) {
            Item I = itemFor(i);
            map.put(I, I);
        }
        final ExecutorService pool = Executors.newFixedThreadPool(2);
        try (PoolCleaner cleaner = cleaner(pool)) {
            Runnable r = new CheckedRunnable() {
                public void realRun() {
                    int result = 0;
                    for (int i = 0; i < iterations; i++)
                        result += map.computeIfAbsent(itemFor(i % mapSize), k -> itemFor(k.value * 2)).value;
                    if (result == -42) throw new Error();
                }};
            for (int i = 0; i < threads; i++)
                pool.execute(r);
        }
    }

}
