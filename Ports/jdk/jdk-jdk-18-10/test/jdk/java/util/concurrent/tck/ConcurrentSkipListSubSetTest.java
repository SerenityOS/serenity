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
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ConcurrentSkipListSubSetTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ConcurrentSkipListSubSetTest.class);
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
    private static NavigableSet<Item> populatedSet(int n) {
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        assertTrue(q.isEmpty());

        for (int i = n - 1; i >= 0; i -= 2)
            mustAdd(q, i);
        for (int i = (n & 1); i < n; i += 2)
            mustAdd(q, i);
        mustAdd(q, -n);
        mustAdd(q, n);
        NavigableSet<Item> s = q.subSet(itemFor(0), true, itemFor(n), false);
        assertFalse(s.isEmpty());
        mustEqual(n, s.size());
        return s;
    }

    /**
     * Returns a new set of first 5 ints.
     */
    private static NavigableSet<Item> set5() {
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        assertTrue(q.isEmpty());
        q.add(one);
        q.add(two);
        q.add(three);
        q.add(four);
        q.add(five);
        q.add(zero);
        q.add(seven);
        NavigableSet<Item> s = q.subSet(one, true, seven, false);
        mustEqual(5, s.size());
        return s;
    }

    /**
     * Returns a new set of first 5 negative ints.
     */
    private static NavigableSet<Item> dset5() {
        ConcurrentSkipListSet<Item> q = new ConcurrentSkipListSet<>();
        assertTrue(q.isEmpty());
        q.add(minusOne);
        q.add(minusTwo);
        q.add(minusThree);
        q.add(minusFour);
        q.add(minusFive);
        NavigableSet<Item> s = q.descendingSet();
        mustEqual(5, s.size());
        return s;
    }

    private static NavigableSet<Item> set0() {
        ConcurrentSkipListSet<Item> set = new ConcurrentSkipListSet<>();
        assertTrue(set.isEmpty());
        return set.tailSet(minusOne, true);
    }

    private static NavigableSet<Item> dset0() {
        ConcurrentSkipListSet<Item> set = new ConcurrentSkipListSet<>();
        assertTrue(set.isEmpty());
        return set;
    }

    /**
     * A new set has unbounded capacity
     */
    public void testConstructor1() {
        mustEqual(0, set0().size());
    }

    /**
     * isEmpty is true before add, false after
     */
    public void testEmpty() {
        NavigableSet<Item> q = set0();
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
        NavigableSet<Item> q = populatedSet(SIZE);
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
        NavigableSet<Item> q = set0();
        try {
            q.add(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Add of comparable element succeeds
     */
    public void testAdd() {
        NavigableSet<Item> q = set0();
        assertTrue(q.add(six));
    }

    /**
     * Add of duplicate element fails
     */
    public void testAddDup() {
        NavigableSet<Item> q = set0();
        assertTrue(q.add(six));
        assertFalse(q.add(six));
    }

    /**
     * Add of non-Comparable throws CCE
     */
    public void testAddNonComparable() {
        ConcurrentSkipListSet<Object> src = new ConcurrentSkipListSet<>();
        NavigableSet<Object> q = src.tailSet(minusOne, true);
        try {
            q.add(new Object());
            q.add(new Object());
            shouldThrow();
        } catch (ClassCastException success) {}
    }

    /**
     * addAll(null) throws NPE
     */
    public void testAddAll1() {
        NavigableSet<Item> q = set0();
        try {
            q.addAll(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addAll of a collection with null elements throws NPE
     */
    public void testAddAll2() {
        NavigableSet<Item> q = set0();
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
        NavigableSet<Item> q = set0();
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
        Item[] items = new Item[SIZE];
        for (int i = 0; i < SIZE; ++i)
            items[i] = itemFor(SIZE - 1 - i);
        NavigableSet<Item> q = set0();
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(i, q.pollFirst());
    }

    /**
     * poll succeeds unless empty
     */
    public void testPoll() {
        NavigableSet<Item> q = populatedSet(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.pollFirst());
        }
        assertNull(q.pollFirst());
    }

    /**
     * remove(x) removes x and returns true if present
     */
    public void testRemoveElement() {
        NavigableSet<Item> q = populatedSet(SIZE);
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
        NavigableSet<Item> q = populatedSet(SIZE);
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
        NavigableSet<Item> q = populatedSet(SIZE);
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
        NavigableSet<Item> q = populatedSet(SIZE);
        NavigableSet<Item> p = set0();
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
        NavigableSet<Item> q = populatedSet(SIZE);
        NavigableSet<Item> p = populatedSet(SIZE);
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
            NavigableSet<Item> q = populatedSet(SIZE);
            NavigableSet<Item> p = populatedSet(i);
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
        NavigableSet<Item> q = set5();
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
        NavigableSet<Item> q = set5();
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
        NavigableSet<Item> q = set5();
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
        NavigableSet<Item> q = set5();
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
        NavigableSet<Item> q = populatedSet(SIZE);
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
        NavigableSet<Item> q = populatedSet(SIZE);
        Item[] items = new Item[SIZE];
        Item[] array = q.toArray(items);
        assertSame(items, array);
        for (Item o : items)
            assertSame(o, q.pollFirst());
        assertTrue(q.isEmpty());
    }

    /**
     * iterator iterates through all elements
     */
    public void testIterator() {
        NavigableSet<Item> q = populatedSet(SIZE);
        Iterator<? extends Item> it = q.iterator();
        int i;
        for (i = 0; it.hasNext(); i++)
            assertTrue(q.contains(it.next()));
        mustEqual(i, SIZE);
        assertIteratorExhausted(it);
    }

    /**
     * iterator of empty set has no elements
     */
    public void testEmptyIterator() {
        assertIteratorExhausted(set0().iterator());
    }

    /**
     * iterator.remove removes current element
     */
    public void testIteratorRemove() {
        final NavigableSet<Item> q = set0();
        mustAdd(q, two);
        mustAdd(q, one);
        mustAdd(q, three);
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
        NavigableSet<Item> q = populatedSet(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /**
     * A deserialized/reserialized set equals original
     */
    public void testSerialization() throws Exception {
        NavigableSet<Item> x = populatedSet(SIZE);
        NavigableSet<Item> y = serialClone(x);

        assertNotSame(y, x);
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
        NavigableSet<Item> set = set5();
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
        NavigableSet<Item> set = set5();
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
        assertFalse(sm.remove(three));
        mustEqual(4, set.size());
    }

    /**
     * headSet returns set with keys in requested range
     */
    public void testHeadSetContents() {
        NavigableSet<Item> set = set5();
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
        NavigableSet<Item> set = set5();
        SortedSet<Item> sm = set.tailSet(two);
        mustNotContain(sm, one);
        mustContain(sm, two);
        mustContain(sm, three);
        mustContain(sm, four);
        mustContain(sm, five);
        assertFalse(sm.contains(one));
        assertTrue(sm.contains(two));
        assertTrue(sm.contains(three));
        assertTrue(sm.contains(four));
        assertTrue(sm.contains(five));
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

    /**
     * size changes when elements added and removed
     */
    public void testDescendingSize() {
        NavigableSet<Item> q = populatedSet(SIZE);
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
    public void testDescendingAddNull() {
        NavigableSet<Item> q = dset0();
        try {
            q.add(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Add of comparable element succeeds
     */
    public void testDescendingAdd() {
        NavigableSet<Item> q = dset0();
        assertTrue(q.add(minusSix));
    }

    /**
     * Add of duplicate element fails
     */
    public void testDescendingAddDup() {
        NavigableSet<Item> q = dset0();
        assertTrue(q.add(minusSix));
        assertFalse(q.add(minusSix));
    }

    /**
     * Add of non-Comparable throws CCE
     */
    public void testDescendingAddNonComparable() {
        NavigableSet<Object> q = new ConcurrentSkipListSet<>();
        try {
            q.add(new Object());
            q.add(new Object());
            shouldThrow();
        } catch (ClassCastException success) {}
    }

    /**
     * addAll(null) throws NPE
     */
    public void testDescendingAddAll1() {
        NavigableSet<Item> q = dset0();
        try {
            q.addAll(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addAll of a collection with null elements throws NPE
     */
    public void testDescendingAddAll2() {
        NavigableSet<Item> q = dset0();
        Item[] items = new Item[1];
        try {
            q.addAll(Arrays.asList(items));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addAll of a collection with any null elements throws NPE after
     * possibly adding some elements
     */
    public void testDescendingAddAll3() {
        NavigableSet<Item> q = dset0();
        Item[] items = new Item[2]; items[0] = zero;
        try {
            q.addAll(Arrays.asList(items));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Set contains all elements of successful addAll
     */
    public void testDescendingAddAll5() {
        Item[] empty = new Item[0];
        Item[] items = new Item[SIZE];
        for (int i = 0; i < SIZE; ++i)
            items[i] = itemFor(SIZE - 1 - i);
        NavigableSet<Item> q = dset0();
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(i, q.pollFirst());
    }

    /**
     * poll succeeds unless empty
     */
    public void testDescendingPoll() {
        NavigableSet<Item> q = populatedSet(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.pollFirst());
        }
        assertNull(q.pollFirst());
    }

    /**
     * remove(x) removes x and returns true if present
     */
    public void testDescendingRemoveElement() {
        NavigableSet<Item> q = populatedSet(SIZE);
        for (int i = 1; i < SIZE; i += 2) {
            mustRemove(q, i);
        }
        for (int i = 0; i < SIZE; i += 2 ) {
            mustRemove(q, i);
            mustNotRemove(q, i + 1);
        }
        assertTrue(q.isEmpty());
    }

    /**
     * contains(x) reports true when elements added but not yet removed
     */
    public void testDescendingContains() {
        NavigableSet<Item> q = populatedSet(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustContain(q, i);
            q.pollFirst();
            mustNotContain(q, i);
        }
    }

    /**
     * clear removes all elements
     */
    public void testDescendingClear() {
        NavigableSet<Item> q = populatedSet(SIZE);
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
    public void testDescendingContainsAll() {
        NavigableSet<Item> q = populatedSet(SIZE);
        NavigableSet<Item> p = dset0();
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
    public void testDescendingRetainAll() {
        NavigableSet<Item> q = populatedSet(SIZE);
        NavigableSet<Item> p = populatedSet(SIZE);
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
    public void testDescendingRemoveAll() {
        for (int i = 1; i < SIZE; ++i) {
            NavigableSet<Item> q = populatedSet(SIZE);
            NavigableSet<Item> p = populatedSet(i);
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
    public void testDescendingLower() {
        NavigableSet<Item> q = dset5();
        Object e1 = q.lower(minusThree);
        mustEqual(minusTwo, e1);

        Object e2 = q.lower(minusSix);
        mustEqual(minusFive, e2);

        Object e3 = q.lower(minusOne);
        assertNull(e3);

        Object e4 = q.lower(zero);
        assertNull(e4);
    }

    /**
     * higher returns next element
     */
    public void testDescendingHigher() {
        NavigableSet<Item> q = dset5();
        Object e1 = q.higher(minusThree);
        mustEqual(minusFour, e1);

        Object e2 = q.higher(zero);
        mustEqual(minusOne, e2);

        Object e3 = q.higher(minusFive);
        assertNull(e3);

        Object e4 = q.higher(minusSix);
        assertNull(e4);
    }

    /**
     * floor returns preceding element
     */
    public void testDescendingFloor() {
        NavigableSet<Item> q = dset5();
        Object e1 = q.floor(minusThree);
        mustEqual(minusThree, e1);

        Object e2 = q.floor(minusSix);
        mustEqual(minusFive, e2);

        Object e3 = q.floor(minusOne);
        mustEqual(minusOne, e3);

        Object e4 = q.floor(zero);
        assertNull(e4);
    }

    /**
     * ceiling returns next element
     */
    public void testDescendingCeiling() {
        NavigableSet<Item> q = dset5();
        Object e1 = q.ceiling(minusThree);
        mustEqual(minusThree, e1);

        Object e2 = q.ceiling(zero);
        mustEqual(minusOne, e2);

        Object e3 = q.ceiling(minusFive);
        mustEqual(minusFive, e3);

        Object e4 = q.ceiling(minusSix);
        assertNull(e4);
    }

    /**
     * toArray contains all elements
     */
    public void testDescendingToArray() {
        NavigableSet<Item> q = populatedSet(SIZE);
        Object[] o = q.toArray();
        Arrays.sort(o);
        for (int i = 0; i < o.length; i++)
            mustEqual(o[i], q.pollFirst());
    }

    /**
     * toArray(a) contains all elements
     */
    public void testDescendingToArray2() {
        NavigableSet<Item> q = populatedSet(SIZE);
        Item[] items = new Item[SIZE];
        assertSame(items, q.toArray(items));
        Arrays.sort(items);
        for (int i = 0; i < items.length; i++)
            mustEqual(items[i], q.pollFirst());
    }

    /**
     * iterator iterates through all elements
     */
    public void testDescendingIterator() {
        NavigableSet<Item> q = populatedSet(SIZE);
        int i = 0;
        Iterator<? extends Item> it = q.iterator();
        while (it.hasNext()) {
            mustContain(q, it.next());
            ++i;
        }
        mustEqual(i, SIZE);
    }

    /**
     * iterator of empty set has no elements
     */
    public void testDescendingEmptyIterator() {
        NavigableSet<Item> q = dset0();
        int i = 0;
        Iterator<? extends Item> it = q.iterator();
        while (it.hasNext()) {
            mustContain(q, it.next());
            ++i;
        }
        mustEqual(0, i);
    }

    /**
     * iterator.remove removes current element
     */
    public void testDescendingIteratorRemove() {
        final NavigableSet<Item> q = dset0();
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
    public void testDescendingToString() {
        NavigableSet<Item> q = populatedSet(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /**
     * A deserialized/reserialized set equals original
     */
    public void testDescendingSerialization() throws Exception {
        NavigableSet<Item> x = dset5();
        NavigableSet<Item> y = serialClone(x);

        assertNotSame(y, x);
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
    public void testDescendingSubSetContents() {
        NavigableSet<Item> set = dset5();
        SortedSet<Item> sm = set.subSet(minusTwo, minusFour);
        mustEqual(minusTwo, sm.first());
        mustEqual(minusThree, sm.last());
        mustEqual(2, sm.size());
        mustNotContain(sm, minusOne);
        mustContain(sm, minusTwo);
        mustContain(sm, minusThree);
        mustNotContain(sm, minusFour);
        mustNotContain(sm, minusFive);
        Iterator<? extends Item> i = sm.iterator();
        Item k = i.next();
        mustEqual(minusTwo, k);
        k = i.next();
        mustEqual(minusThree, k);
        assertFalse(i.hasNext());
        Iterator<? extends Item> j = sm.iterator();
        j.next();
        j.remove();
        mustNotContain(set, minusTwo);
        mustEqual(4, set.size());
        mustEqual(1, sm.size());
        mustEqual(minusThree, sm.first());
        mustEqual(minusThree, sm.last());
        mustRemove(sm, minusThree);
        assertTrue(sm.isEmpty());
        mustEqual(3, set.size());
    }

    public void testDescendingSubSetContents2() {
        NavigableSet<Item> set = dset5();
        SortedSet<Item> sm = set.subSet(minusTwo, minusThree);
        mustEqual(1, sm.size());
        mustEqual(minusTwo, sm.first());
        mustEqual(minusTwo, sm.last());
        mustNotContain(sm, minusOne);
        mustContain(sm, minusTwo);
        mustNotContain(sm, minusThree);
        mustNotContain(sm, minusFour);
        mustNotContain(sm, minusFive);
        Iterator<? extends Item> i = sm.iterator();
        Item k = i.next();
        mustEqual(minusTwo, k);
        assertFalse(i.hasNext());
        Iterator<? extends Item> j = sm.iterator();
        j.next();
        j.remove();
        mustNotContain(set, minusTwo);
        mustEqual(4, set.size());
        mustEqual(0, sm.size());
        assertTrue(sm.isEmpty());
        mustNotRemove(sm, minusThree);
        mustEqual(4, set.size());
    }

    /**
     * headSet returns set with keys in requested range
     */
    public void testDescendingHeadSetContents() {
        NavigableSet<Item> set = dset5();
        SortedSet<Item> sm = set.headSet(minusFour);
        mustContain(sm, minusOne);
        mustContain(sm, minusTwo);
        mustContain(sm, minusThree);
        mustNotContain(sm, minusFour);
        mustNotContain(sm, minusFive);
        Iterator<? extends Item> i = sm.iterator();
        Item k = i.next();
        mustEqual(minusOne, k);
        k = i.next();
        mustEqual(minusTwo, k);
        k = i.next();
        mustEqual(minusThree, k);
        assertFalse(i.hasNext());
        sm.clear();
        assertTrue(sm.isEmpty());
        mustEqual(2, set.size());
        mustEqual(minusFour, set.first());
    }

    /**
     * tailSet returns set with keys in requested range
     */
    public void testDescendingTailSetContents() {
        NavigableSet<Item> set = dset5();
        SortedSet<Item> sm = set.tailSet(minusTwo);
        mustNotContain(sm, minusOne);
        mustContain(sm, minusTwo);
        mustContain(sm, minusThree);
        mustContain(sm, minusFour);
        mustContain(sm, minusFive);
        Iterator<? extends Item> i = sm.iterator();
        Item k = i.next();
        mustEqual(minusTwo, k);
        k = i.next();
        mustEqual(minusThree, k);
        k = i.next();
        mustEqual(minusFour, k);
        k = i.next();
        mustEqual(minusFive, k);
        assertFalse(i.hasNext());

        SortedSet<Item> ssm = sm.tailSet(minusFour);
        mustEqual(minusFour, ssm.first());
        mustEqual(minusFive, ssm.last());
        mustRemove(ssm, minusFour);
        mustEqual(1, ssm.size());
        mustEqual(3, sm.size());
        mustEqual(4, set.size());
    }

}
