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
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ConcurrentSkipListSetTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ConcurrentSkipListSetTest.class);
    }

    static class MyReverseComparator implements Comparator {
        @SuppressWarnings("unchecked")
        public int compare(Object x, Object y) {
            return ((Comparable)y).compareTo(x);
        }
    }

    /**
     * Returns a new set of given size containing consecutive
     * Items 0 ... n - 1.
     */
    private static ConcurrentSkipListSet<Item> populatedSet(int n) {
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        assertTrue(q.isEmpty());
        for (int i = n - 1; i >= 0; i -= 2)
            mustAdd(q, i);
        for (int i = (n & 1); i < n; i += 2)
            mustAdd(q, i);
        assertFalse(q.isEmpty());
        mustEqual(n, q.size());
        return q;
    }

    /**
     * Returns a new set of first 5 ints.
     */
    private static ConcurrentSkipListSet<Item> set5() {
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        assertTrue(q.isEmpty());
        q.add(one);
        q.add(two);
        q.add(three);
        q.add(four);
        q.add(five);
        mustEqual(5, q.size());
        return q;
    }

    /**
     * A new set has unbounded capacity
     */
    public void testConstructor1() {
        mustEqual(0, new ConcurrentSkipListSet<Item>().size());
    }

    /**
     * Initializing from null Collection throws NPE
     */
    public void testConstructor3() {
        try {
            new ConcurrentSkipListSet<Item>((Collection<Item>)null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection of null elements throws NPE
     */
    public void testConstructor4() {
        try {
            new ConcurrentSkipListSet<Item>(Arrays.asList(new Item[SIZE]));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection with some null elements throws NPE
     */
    public void testConstructor5() {
        Item[] items = new Item[2];
        items[0] = zero;
        try {
            new ConcurrentSkipListSet<Item>(Arrays.asList(items));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Set contains all elements of collection used to initialize
     */
    public void testConstructor6() {
        Item[] items = defaultItems;
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>(Arrays.asList(items));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.pollFirst());
    }

    /**
     * The comparator used in constructor is used
     */
    public void testConstructor7() {
        MyReverseComparator cmp = new MyReverseComparator();
        @SuppressWarnings("unchecked")
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>(cmp);
        mustEqual(cmp, q.comparator());
        Item[] items = defaultItems;
        q.addAll(Arrays.asList(items));
        for (int i = SIZE - 1; i >= 0; --i)
            mustEqual(items[i], q.pollFirst());
    }

    /**
     * isEmpty is true before add, false after
     */
    public void testEmpty() {
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        assertTrue(q.isEmpty());
        mustAdd(q, one);
        assertFalse(q.isEmpty());
        mustAdd(q, two);
        q.pollFirst();
        q.pollFirst();
        assertTrue(q.isEmpty());
    }

    /**
     * size changes when elements added and removed
     */
    public void testSize() {
        ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(SIZE - i, q.size());
            q.pollFirst();
        }
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.size());
            mustAdd(q, i);
        }
    }

    /**
     * add(null) throws NPE
     */
    public void testAddNull() {
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        try {
            q.add(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Add of comparable element succeeds
     */
    public void testAdd() {
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        assertTrue(q.add(zero));
        assertTrue(q.add(one));
    }

    /**
     * Add of duplicate element fails
     */
    public void testAddDup() {
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        assertTrue(q.add(zero));
        assertFalse(q.add(zero));
    }

    /**
     * Add of non-Comparable throws CCE
     */
    public void testAddNonComparable() {
        ConcurrentSkipListSet<Object> q = new ConcurrentSkipListSet<>();
        try {
            q.add(new Object());
            q.add(new Object());
            shouldThrow();
        } catch (ClassCastException success) {
            assertTrue(q.size() < 2);
            for (int i = 0, size = q.size(); i < size; i++)
                assertSame(Object.class, q.pollFirst().getClass());
            assertNull(q.pollFirst());
            assertTrue(q.isEmpty());
            mustEqual(0, q.size());
        }
    }

    /**
     * addAll(null) throws NPE
     */
    public void testAddAll1() {
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        try {
            q.addAll(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addAll of a collection with null elements throws NPE
     */
    public void testAddAll2() {
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        Item[] items = new Item[SIZE];
        try {
            q.addAll(Arrays.asList(items));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addAll of a collection with any null elements throws NPE after
     * possibly adding some elements
     */
    public void testAddAll3() {
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        Item[] items = new Item[2]; items[0] = zero;
        try {
            q.addAll(Arrays.asList(items));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Set contains all elements of successful addAll
     */
    public void testAddAll5() {
        Item[] empty = new Item[0];
        Item[] items = defaultItems;
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(i, q.pollFirst());
    }

    /**
     * pollFirst succeeds unless empty
     */
    public void testPollFirst() {
        ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.pollFirst());
        }
        assertNull(q.pollFirst());
    }

    /**
     * pollLast succeeds unless empty
     */
    public void testPollLast() {
        ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            mustEqual(i, q.pollLast());
        }
        assertNull(q.pollFirst());
    }

    /**
     * remove(x) removes x and returns true if present
     */
    public void testRemoveElement() {
        ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
        for (int i = 1; i < SIZE; i += 2) {
            mustContain(q, i);
            mustRemove(q, i);
            mustNotContain(q, i);
            mustContain(q, i - 1);
        }
        for (int i = 0; i < SIZE; i += 2) {
            mustContain(q, i);
            mustRemove(q, i);
            mustNotContain(q, i);
            mustNotRemove(q, i + 1);
            mustNotContain(q, i + 1);
        }
        assertTrue(q.isEmpty());
    }

    /**
     * contains(x) reports true when elements added but not yet removed
     */
    public void testContains() {
        ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustContain(q, i);
            q.pollFirst();
            mustNotContain(q, i);
        }
    }

    /**
     * clear removes all elements
     */
    public void testClear() {
        ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
        q.clear();
        assertTrue(q.isEmpty());
        mustEqual(0, q.size());
        mustAdd(q, one);
        assertFalse(q.isEmpty());
        q.clear();
        assertTrue(q.isEmpty());
    }

    /**
     * containsAll(c) is true when c contains a subset of elements
     */
    public void testContainsAll() {
        ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
        ConcurrentSkipListSet<Item> p = new ConcurrentSkipListSet<>();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(q.containsAll(p));
            assertFalse(p.containsAll(q));
            mustAdd(p, i);
        }
        assertTrue(p.containsAll(q));
    }

    /**
     * retainAll(c) retains only those elements of c and reports true if changed
     */
    public void testRetainAll() {
        ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
        ConcurrentSkipListSet<Item> p = populatedSet(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            boolean changed = q.retainAll(p);
            if (i == 0)
                assertFalse(changed);
            else
                assertTrue(changed);

            assertTrue(q.containsAll(p));
            mustEqual(SIZE - i, q.size());
            p.pollFirst();
        }
    }

    /**
     * removeAll(c) removes only those elements of c and reports true if changed
     */
    public void testRemoveAll() {
        for (int i = 1; i < SIZE; ++i) {
            ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
            ConcurrentSkipListSet<Item> p = populatedSet(i);
            assertTrue(q.removeAll(p));
            mustEqual(SIZE - i, q.size());
            for (int j = 0; j < i; ++j) {
                mustNotContain(q, p.pollFirst());
            }
        }
    }

    /**
     * lower returns preceding element
     */
    public void testLower() {
        ConcurrentSkipListSet<Item> q = set5();
        Object e1 = q.lower(three);
        mustEqual(two, e1);

        Object e2 = q.lower(six);
        mustEqual(five, e2);

        Object e3 = q.lower(one);
        assertNull(e3);

        Object e4 = q.lower(zero);
        assertNull(e4);
    }

    /**
     * higher returns next element
     */
    public void testHigher() {
        ConcurrentSkipListSet<Item> q = set5();
        Object e1 = q.higher(three);
        mustEqual(four, e1);

        Object e2 = q.higher(zero);
        mustEqual(one, e2);

        Object e3 = q.higher(five);
        assertNull(e3);

        Object e4 = q.higher(six);
        assertNull(e4);
    }

    /**
     * floor returns preceding element
     */
    public void testFloor() {
        ConcurrentSkipListSet<Item> q = set5();
        Object e1 = q.floor(three);
        mustEqual(three, e1);

        Object e2 = q.floor(six);
        mustEqual(five, e2);

        Object e3 = q.floor(one);
        mustEqual(one, e3);

        Object e4 = q.floor(zero);
        assertNull(e4);
    }

    /**
     * ceiling returns next element
     */
    public void testCeiling() {
        ConcurrentSkipListSet<Item> q = set5();
        Object e1 = q.ceiling(three);
        mustEqual(three, e1);

        Object e2 = q.ceiling(zero);
        mustEqual(one, e2);

        Object e3 = q.ceiling(five);
        mustEqual(five, e3);

        Object e4 = q.ceiling(six);
        assertNull(e4);
    }

    /**
     * toArray contains all elements in sorted order
     */
    public void testToArray() {
        ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
        Object[] a = q.toArray();
        assertSame(Object[].class, a.getClass());
        for (Object o : a)
            assertSame(o, q.pollFirst());
        assertTrue(q.isEmpty());
    }

    /**
     * toArray(a) contains all elements in sorted order
     */
    public void testToArray2() {
        ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
        Item[] items = new Item[SIZE];
        assertSame(items, q.toArray(items));
        for (Item o : items)
            assertSame(o, q.pollFirst());
        assertTrue(q.isEmpty());
    }

    /**
     * iterator iterates through all elements
     */
    public void testIterator() {
        ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
        Iterator<? extends Item> it = q.iterator();
        int i;
        for (i = 0; it.hasNext(); i++)
            mustContain(q, it.next());
        mustEqual(i, SIZE);
        assertIteratorExhausted(it);
    }

    /**
     * iterator of empty set has no elements
     */
    public void testEmptyIterator() {
        NavigableSet<Item> s = new ConcurrentSkipListSet<>();
        assertIteratorExhausted(s.iterator());
        assertIteratorExhausted(s.descendingSet().iterator());
    }

    /**
     * iterator.remove removes current element
     */
    public void testIteratorRemove() {
        final ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        q.add(two);
        q.add(one);
        q.add(three);

        Iterator<? extends Item> it = q.iterator();
        it.next();
        it.remove();

        it = q.iterator();
        mustEqual(it.next(), two);
        mustEqual(it.next(), three);
        assertFalse(it.hasNext());
    }

    /**
     * toString contains toStrings of elements
     */
    public void testToString() {
        ConcurrentSkipListSet<Item> q = populatedSet(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /**
     * A cloned set equals original
     */
    public void testClone() {
        ConcurrentSkipListSet<Item> x = populatedSet(SIZE);
        ConcurrentSkipListSet<Item> y = x.clone();

        assertNotSame(x, y);
        mustEqual(x.size(), y.size());
        mustEqual(x, y);
        mustEqual(y, x);
        while (!x.isEmpty()) {
            assertFalse(y.isEmpty());
            mustEqual(x.pollFirst(), y.pollFirst());
        }
        assertTrue(y.isEmpty());
    }

    /**
     * A deserialized/reserialized set equals original
     */
    public void testSerialization() throws Exception {
        NavigableSet<Item> x = populatedSet(SIZE);
        NavigableSet<Item> y = serialClone(x);

        assertNotSame(x, y);
        mustEqual(x.size(), y.size());
        mustEqual(x, y);
        mustEqual(y, x);
        while (!x.isEmpty()) {
            assertFalse(y.isEmpty());
            mustEqual(x.pollFirst(), y.pollFirst());
        }
        assertTrue(y.isEmpty());
    }

    /**
     * subSet returns set with keys in requested range
     */
    public void testSubSetContents() {
        ConcurrentSkipListSet<Item> set = set5();
        SortedSet<Item> sm = set.subSet(two, four);
        mustEqual(two, sm.first());
        mustEqual(three, sm.last());
        mustEqual(2, sm.size());
        mustNotContain(sm, one);
        mustContain(sm, two);
        mustContain(sm, three);
        mustNotContain(sm, four);
        mustNotContain(sm, five);
        Iterator<? extends Item> i = sm.iterator();
        Item k = i.next();
        mustEqual(two, k);
        k = i.next();
        mustEqual(three, k);
        assertFalse(i.hasNext());
        Iterator<? extends Item> j = sm.iterator();
        j.next();
        j.remove();
        mustNotContain(set, two);
        mustEqual(4, set.size());
        mustEqual(1, sm.size());
        mustEqual(three, sm.first());
        mustEqual(three, sm.last());
        mustRemove(sm, three);
        assertTrue(sm.isEmpty());
        mustEqual(3, set.size());
    }

    public void testSubSetContents2() {
        ConcurrentSkipListSet<Item> set = set5();
        SortedSet<Item> sm = set.subSet(two, three);
        mustEqual(1, sm.size());
        mustEqual(two, sm.first());
        mustEqual(two, sm.last());
        mustNotContain(sm, one);
        mustContain(sm, two);
        mustNotContain(sm, three);
        mustNotContain(sm, four);
        mustNotContain(sm, five);
        Iterator<? extends Item> i = sm.iterator();
        Item k = i.next();
        mustEqual(two, k);
        assertFalse(i.hasNext());
        Iterator<? extends Item> j = sm.iterator();
        j.next();
        j.remove();
        mustNotContain(set, two);
        mustEqual(4, set.size());
        mustEqual(0, sm.size());
        assertTrue(sm.isEmpty());
        mustNotRemove(sm, three);
        mustEqual(4, set.size());
    }

    /**
     * headSet returns set with keys in requested range
     */
    public void testHeadSetContents() {
        ConcurrentSkipListSet<Item> set = set5();
        SortedSet<Item> sm = set.headSet(four);
        mustContain(sm, one);
        mustContain(sm, two);
        mustContain(sm, three);
        mustNotContain(sm, four);
        mustNotContain(sm, five);
        Iterator<? extends Item> i = sm.iterator();
        Item k = i.next();
        mustEqual(one, k);
        k = i.next();
        mustEqual(two, k);
        k = i.next();
        mustEqual(three, k);
        assertFalse(i.hasNext());
        sm.clear();
        assertTrue(sm.isEmpty());
        mustEqual(2, set.size());
        mustEqual(four, set.first());
    }

    /**
     * tailSet returns set with keys in requested range
     */
    public void testTailSetContents() {
        ConcurrentSkipListSet<Item> set = set5();
        SortedSet<Item> sm = set.tailSet(two);
        mustNotContain(sm, one);
        mustContain(sm, two);
        mustContain(sm, three);
        mustContain(sm, four);
        mustContain(sm, five);
        mustContain(sm, two);
        Iterator<? extends Item> i = sm.iterator();
        Item k = i.next();
        mustEqual(two, k);
        k = i.next();
        mustEqual(three, k);
        k = i.next();
        mustEqual(four, k);
        k = i.next();
        mustEqual(five, k);
        assertFalse(i.hasNext());

        SortedSet<Item> ssm = sm.tailSet(four);
        mustEqual(four, ssm.first());
        mustEqual(five, ssm.last());
        mustRemove(ssm, four);
        mustEqual(1, ssm.size());
        mustEqual(3, sm.size());
        mustEqual(4, set.size());
    }

    Random rnd = new Random(666);

    /**
     * Subsets of subsets subdivide correctly
     */
    public void testRecursiveSubSets() throws Exception {
        int setSize = expensiveTests ? 1000 : 100;
        Class<?> cl = ConcurrentSkipListSet.class;

        NavigableSet<Item> set = newSet(cl);
        BitSet bs = new BitSet(setSize);

        populate(set, setSize, bs);
        check(set,                 0, setSize - 1, true, bs);
        check(set.descendingSet(), 0, setSize - 1, false, bs);

        mutateSet(set, 0, setSize - 1, bs);
        check(set,                 0, setSize - 1, true, bs);
        check(set.descendingSet(), 0, setSize - 1, false, bs);

        bashSubSet(set.subSet(zero, true, itemFor(setSize), false),
                   0, setSize - 1, true, bs);
    }

    /**
     * addAll is idempotent
     */
    public void testAddAll_idempotent() throws Exception {
        Set<Item> x = populatedSet(SIZE);
        Set<Item> y = new ConcurrentSkipListSet<>(x);
        y.addAll(x);
        mustEqual(x, y);
        mustEqual(y, x);
    }

    static NavigableSet<Item> newSet(Class<?> cl) throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Item> result =
            (NavigableSet<Item>) cl.getConstructor().newInstance();
        mustEqual(0, result.size());
        assertFalse(result.iterator().hasNext());
        return result;
    }

    void populate(NavigableSet<Item> set, int limit, BitSet bs) {
        for (int i = 0, n = 2 * limit / 3; i < n; i++) {
            int element = rnd.nextInt(limit);
            put(set, element, bs);
        }
    }

    void mutateSet(NavigableSet<Item> set, int min, int max, BitSet bs) {
        int size = set.size();
        int rangeSize = max - min + 1;

        // Remove a bunch of entries directly
        for (int i = 0, n = rangeSize / 2; i < n; i++) {
            remove(set, min - 5 + rnd.nextInt(rangeSize + 10), bs);
        }

        // Remove a bunch of entries with iterator
        for (Iterator<Item> it = set.iterator(); it.hasNext(); ) {
            if (rnd.nextBoolean()) {
                bs.clear(it.next().value);
                it.remove();
            }
        }

        // Add entries till we're back to original size
        while (set.size() < size) {
            int element = min + rnd.nextInt(rangeSize);
            assertTrue(element >= min && element <= max);
            put(set, element, bs);
        }
    }

    void mutateSubSet(NavigableSet<Item> set, int min, int max,
                      BitSet bs) {
        int size = set.size();
        int rangeSize = max - min + 1;

        // Remove a bunch of entries directly
        for (int i = 0, n = rangeSize / 2; i < n; i++) {
            remove(set, min - 5 + rnd.nextInt(rangeSize + 10), bs);
        }

        // Remove a bunch of entries with iterator
        for (Iterator<Item> it = set.iterator(); it.hasNext(); ) {
            if (rnd.nextBoolean()) {
                bs.clear(it.next().value);
                it.remove();
            }
        }

        // Add entries till we're back to original size
        while (set.size() < size) {
            int element = min - 5 + rnd.nextInt(rangeSize + 10);
            if (element >= min && element <= max) {
                put(set, element, bs);
            } else {
                try {
                    set.add(itemFor(element));
                    shouldThrow();
                } catch (IllegalArgumentException success) {}
            }
        }
    }

    void put(NavigableSet<Item> set, int element, BitSet bs) {
        if (set.add(itemFor(element)))
            bs.set(element);
    }

    void remove(NavigableSet<Item> set, int element, BitSet bs) {
        if (set.remove(itemFor(element)))
            bs.clear(element);
    }

    void bashSubSet(NavigableSet<Item> set,
                    int min, int max, boolean ascending,
                    BitSet bs) {
        check(set, min, max, ascending, bs);
        check(set.descendingSet(), min, max, !ascending, bs);

        mutateSubSet(set, min, max, bs);
        check(set, min, max, ascending, bs);
        check(set.descendingSet(), min, max, !ascending, bs);

        // Recurse
        if (max - min < 2)
            return;
        int midPoint = (min + max) / 2;

        // headSet - pick direction and endpoint inclusion randomly
        boolean incl = rnd.nextBoolean();
        NavigableSet<Item> hm = set.headSet(itemFor(midPoint), incl);
        if (ascending) {
            if (rnd.nextBoolean())
                bashSubSet(hm, min, midPoint - (incl ? 0 : 1), true, bs);
            else
                bashSubSet(hm.descendingSet(), min, midPoint - (incl ? 0 : 1),
                           false, bs);
        } else {
            if (rnd.nextBoolean())
                bashSubSet(hm, midPoint + (incl ? 0 : 1), max, false, bs);
            else
                bashSubSet(hm.descendingSet(), midPoint + (incl ? 0 : 1), max,
                           true, bs);
        }

        // tailSet - pick direction and endpoint inclusion randomly
        incl = rnd.nextBoolean();
        NavigableSet<Item> tm = set.tailSet(itemFor(midPoint),incl);
        if (ascending) {
            if (rnd.nextBoolean())
                bashSubSet(tm, midPoint + (incl ? 0 : 1), max, true, bs);
            else
                bashSubSet(tm.descendingSet(), midPoint + (incl ? 0 : 1), max,
                           false, bs);
        } else {
            if (rnd.nextBoolean()) {
                bashSubSet(tm, min, midPoint - (incl ? 0 : 1), false, bs);
            } else {
                bashSubSet(tm.descendingSet(), min, midPoint - (incl ? 0 : 1),
                           true, bs);
            }
        }

        // subSet - pick direction and endpoint inclusion randomly
        int rangeSize = max - min + 1;
        int[] endpoints = new int[2];
        endpoints[0] = min + rnd.nextInt(rangeSize);
        endpoints[1] = min + rnd.nextInt(rangeSize);
        Arrays.sort(endpoints);
        boolean lowIncl = rnd.nextBoolean();
        boolean highIncl = rnd.nextBoolean();
        if (ascending) {
            NavigableSet<Item> sm = set.subSet(
                itemFor(endpoints[0]), lowIncl, itemFor(endpoints[1]), highIncl);
            if (rnd.nextBoolean())
                bashSubSet(sm, endpoints[0] + (lowIncl ? 0 : 1),
                           endpoints[1] - (highIncl ? 0 : 1), true, bs);
            else
                bashSubSet(sm.descendingSet(), endpoints[0] + (lowIncl ? 0 : 1),
                           endpoints[1] - (highIncl ? 0 : 1), false, bs);
        } else {
            NavigableSet<Item> sm = set.subSet(
                itemFor(endpoints[1]), highIncl, itemFor(endpoints[0]), lowIncl);
            if (rnd.nextBoolean())
                bashSubSet(sm, endpoints[0] + (lowIncl ? 0 : 1),
                           endpoints[1] - (highIncl ? 0 : 1), false, bs);
            else
                bashSubSet(sm.descendingSet(), endpoints[0] + (lowIncl ? 0 : 1),
                           endpoints[1] - (highIncl ? 0 : 1), true, bs);
        }
    }

    /**
     * min and max are both inclusive.  If max < min, interval is empty.
     */
    void check(NavigableSet<Item> set,
               final int min, final int max, final boolean ascending,
               final BitSet bs) {
        class ReferenceSet {
            int lower(int element) {
                return ascending ?
                    lowerAscending(element) : higherAscending(element);
            }
            int floor(int element) {
                return ascending ?
                    floorAscending(element) : ceilingAscending(element);
            }
            int ceiling(int element) {
                return ascending ?
                    ceilingAscending(element) : floorAscending(element);
            }
            int higher(int element) {
                return ascending ?
                    higherAscending(element) : lowerAscending(element);
            }
            int first() {
                return ascending ? firstAscending() : lastAscending();
            }
            int last() {
                return ascending ? lastAscending() : firstAscending();
            }
            int lowerAscending(int element) {
                return floorAscending(element - 1);
            }
            int floorAscending(int element) {
                if (element < min)
                    return -1;
                else if (element > max)
                    element = max;

                // BitSet should support this! Test would run much faster
                while (element >= min) {
                    if (bs.get(element))
                        return element;
                    element--;
                }
                return -1;
            }
            int ceilingAscending(int element) {
                if (element < min)
                    element = min;
                else if (element > max)
                    return -1;
                int result = bs.nextSetBit(element);
                return result > max ? -1 : result;
            }
            int higherAscending(int element) {
                return ceilingAscending(element + 1);
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

        // Test contents using containsElement
        int size = 0;
        for (int i = min; i <= max; i++) {
            boolean bsContainsI = bs.get(i);
            mustEqual(bsContainsI, set.contains(itemFor(i)));
            if (bsContainsI)
                size++;
        }
        mustEqual(size, set.size());

        // Test contents using contains elementSet iterator
        int size2 = 0;
        int previousElement = -1;
        for (Item element : set) {
            assertTrue(bs.get(element.value));
            size2++;
            assertTrue(previousElement < 0 || (ascending ?
                element.value - previousElement > 0 : element.value - previousElement < 0));
            previousElement = element.value;
        }
        mustEqual(size2, size);

        // Test navigation ops
        for (int element = min - 1; element <= max + 1; element++) {
            Item e = itemFor(element);
            assertEq(set.lower(e), rs.lower(element));
            assertEq(set.floor(e), rs.floor(element));
            assertEq(set.higher(e), rs.higher(element));
            assertEq(set.ceiling(e), rs.ceiling(element));
        }

        // Test extrema
        if (set.size() != 0) {
            assertEq(set.first(), rs.first());
            assertEq(set.last(), rs.last());
        } else {
            mustEqual(rs.first(), -1);
            mustEqual(rs.last(),  -1);
            try {
                set.first();
                shouldThrow();
            } catch (NoSuchElementException success) {}
            try {
                set.last();
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
        return (i == null) ? j == -1 : i.value == j;
    }

}
