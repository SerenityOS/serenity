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
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;

import junit.framework.Test;

public class PriorityQueueTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return PriorityQueue.class; }
            public Collection emptyCollection() { return new PriorityQueue(); }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return false; }
            public boolean permitsNulls() { return false; }
        }
        class ComparatorImplementation implements CollectionImplementation {
            public Class<?> klazz() { return PriorityQueue.class; }
            @SuppressWarnings("unchecked")
            public Collection emptyCollection() {
                return new PriorityQueue(new MyReverseComparator());
            }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return false; }
            public boolean permitsNulls() { return false; }
        }
        return newTestSuite(
            PriorityQueueTest.class,
            CollectionTest.testSuite(new Implementation()),
            CollectionTest.testSuite(new ComparatorImplementation()));
    }

    static class MyReverseComparator implements Comparator, java.io.Serializable {
        @SuppressWarnings("unchecked")
        public int compare(Object x, Object y) {
            return ((Comparable)y).compareTo(x);
        }
    }

    /**
     * Returns a new queue of given size containing consecutive
     * Items 0 ... n - 1.
     */
    private static PriorityQueue<Item> populatedQueue(int n) {
        PriorityQueue<Item> q = new PriorityQueue<>(n);
        assertTrue(q.isEmpty());
        for (int i = n - 1; i >= 0; i -= 2)
            mustOffer(q, i);
        for (int i = (n & 1); i < n; i += 2)
            mustOffer(q, i);
        assertFalse(q.isEmpty());
        mustEqual(n, q.size());
        mustEqual(0, q.peek());
        return q;
    }

    /**
     * A new queue has unbounded capacity
     */
    public void testConstructor1() {
        mustEqual(0, new PriorityQueue<Item>(SIZE).size());
    }

    /**
     * Constructor throws IllegalArgumentException if capacity argument nonpositive
     */
    public void testConstructor2() {
        try {
            new PriorityQueue<Item>(0);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Initializing from null Collection throws NPE
     */
    public void testConstructor3() {
        try {
            new PriorityQueue<Item>((Collection<Item>)null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection of null elements throws NPE
     */
    public void testConstructor4() {
        try {
            new PriorityQueue<Item>(Arrays.asList(new Item[SIZE]));
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
            new PriorityQueue<Item>(Arrays.asList(items));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Queue contains all elements of collection used to initialize
     */
    public void testConstructor6() {
        Item[] items = defaultItems;
        PriorityQueue<Item> q = new PriorityQueue<>(Arrays.asList(items));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * The comparator used in constructor is used
     */
    public void testConstructor7() {
        MyReverseComparator cmp = new MyReverseComparator();
        @SuppressWarnings("unchecked")
        PriorityQueue<Item> q = new PriorityQueue<>(SIZE, cmp);
        assertEquals(cmp, q.comparator());
        Item[] items = seqItems(SIZE);
        q.addAll(Arrays.asList(items));
        for (int i = SIZE - 1; i >= 0; --i)
            mustEqual(items[i], q.poll());
    }

    /**
     * isEmpty is true before add, false after
     */
    public void testEmpty() {
        PriorityQueue<Item> q = new PriorityQueue<>(2);
        assertTrue(q.isEmpty());
        q.add(one);
        assertFalse(q.isEmpty());
        q.add(two);
        q.remove();
        q.remove();
        assertTrue(q.isEmpty());
    }

    /**
     * size changes when elements added and removed
     */
    public void testSize() {
        PriorityQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(SIZE - i, q.size());
            q.remove();
        }
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.size());
            mustAdd(q, i);
        }
    }

    /**
     * offer(null) throws NPE
     */
    public void testOfferNull() {
        PriorityQueue<Item> q = new PriorityQueue<>(1);
        try {
            q.offer(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * add(null) throws NPE
     */
    public void testAddNull() {
        PriorityQueue<Item> q = new PriorityQueue<>(1);
        try {
            q.add(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Offer of comparable element succeeds
     */
    public void testOffer() {
        PriorityQueue<Item> q = new PriorityQueue<>(1);
        assertTrue(q.offer(zero));
        assertTrue(q.offer(one));
    }

    /**
     * Offer of non-Comparable throws CCE
     */
    public void testOfferNonComparable() {
        PriorityQueue<Object> q = new PriorityQueue<>(1);
        try {
            q.offer(new Object());
            shouldThrow();
        } catch (ClassCastException success) {
            assertTrue(q.isEmpty());
            mustEqual(0, q.size());
            assertNull(q.poll());
        }
    }

    /**
     * add of comparable succeeds
     */
    public void testAdd() {
        PriorityQueue<Item> q = new PriorityQueue<>(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.size());
            mustAdd(q, i);
        }
    }

    /**
     * addAll(null) throws NPE
     */
    public void testAddAll1() {
        PriorityQueue<Item> q = new PriorityQueue<>(1);
        try {
            q.addAll(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addAll of a collection with null elements throws NPE
     */
    public void testAddAll2() {
        PriorityQueue<Item> q = new PriorityQueue<>(SIZE);
        try {
            q.addAll(Arrays.asList(new Item[SIZE]));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addAll of a collection with any null elements throws NPE after
     * possibly adding some elements
     */
    public void testAddAll3() {
        PriorityQueue<Item> q = new PriorityQueue<>(SIZE);
        Item[] items = new Item[2];
        items[0] = zero;
        try {
            q.addAll(Arrays.asList(items));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Queue contains all elements of successful addAll
     */
    public void testAddAll5() {
        Item[] empty = new Item[0];
        Item[] items = new Item[SIZE];
        for (int i = 0; i < SIZE; ++i)
            items[i] = itemFor(SIZE - 1 - i);
        PriorityQueue<Item> q = new PriorityQueue<>(SIZE);
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(i, q.poll());
    }

    /**
     * poll succeeds unless empty
     */
    public void testPoll() {
        PriorityQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * peek returns next element, or null if empty
     */
    public void testPeek() {
        PriorityQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.peek());
            mustEqual(i, q.poll());
            assertTrue(q.peek() == null ||
                       !q.peek().equals(i));
        }
        assertNull(q.peek());
    }

    /**
     * element returns next element, or throws NSEE if empty
     */
    public void testElement() {
        PriorityQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.element());
            mustEqual(i, q.poll());
        }
        try {
            q.element();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * remove removes next element, or throws NSEE if empty
     */
    public void testRemove() {
        PriorityQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.remove());
        }
        try {
            q.remove();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * remove(x) removes x and returns true if present
     */
    public void testRemoveElement() {
        PriorityQueue<Item> q = populatedQueue(SIZE);
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
        PriorityQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustContain(q, i);
            q.poll();
            mustNotContain(q, i);
        }
    }

    /**
     * clear removes all elements
     */
    public void testClear() {
        PriorityQueue<Item> q = populatedQueue(SIZE);
        q.clear();
        assertTrue(q.isEmpty());
        mustEqual(0, q.size());
        q.add(one);
        assertFalse(q.isEmpty());
        q.clear();
        assertTrue(q.isEmpty());
    }

    /**
     * containsAll(c) is true when c contains a subset of elements
     */
    public void testContainsAll() {
        PriorityQueue<Item> q = populatedQueue(SIZE);
        PriorityQueue<Item> p = new PriorityQueue<>(SIZE);
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
        PriorityQueue<Item> q = populatedQueue(SIZE);
        PriorityQueue<Item> p = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            boolean changed = q.retainAll(p);
            if (i == 0)
                assertFalse(changed);
            else
                assertTrue(changed);

            assertTrue(q.containsAll(p));
            mustEqual(SIZE - i, q.size());
            p.remove();
        }
    }

    /**
     * removeAll(c) removes only those elements of c and reports true if changed
     */
    public void testRemoveAll() {
        for (int i = 1; i < SIZE; ++i) {
            PriorityQueue<Item> q = populatedQueue(SIZE);
            PriorityQueue<Item> p = populatedQueue(i);
            assertTrue(q.removeAll(p));
            mustEqual(SIZE - i, q.size());
            for (int j = 0; j < i; ++j) {
                mustNotContain(q, p.remove());
            }
        }
    }

    /**
     * toArray contains all elements
     */
    public void testToArray() {
        PriorityQueue<Item> q = populatedQueue(SIZE);
        Object[] a = q.toArray();
        assertSame(Object[].class, a.getClass());
        Arrays.sort(a);
        for (Object o : a)
            assertSame(o, q.poll());
        assertTrue(q.isEmpty());
    }

    /**
     * toArray(a) contains all elements
     */
    public void testToArray2() {
        PriorityQueue<Item> q = populatedQueue(SIZE);
        Item[] items = new Item[SIZE];
        Item[] array = q.toArray(items);
        assertSame(items, array);
        Arrays.sort(items);
        for (Item o : items)
            assertSame(o, q.poll());
        assertTrue(q.isEmpty());
    }

    /**
     * iterator iterates through all elements
     */
    public void testIterator() {
        PriorityQueue<Item> q = populatedQueue(SIZE);
        Iterator<? extends Item> it = q.iterator();
        int i;
        for (i = 0; it.hasNext(); i++)
            mustContain(q, it.next());
        mustEqual(i, SIZE);
        assertIteratorExhausted(it);
    }

    /**
     * iterator of empty collection has no elements
     */
    public void testEmptyIterator() {
        assertIteratorExhausted(new PriorityQueue<Item>().iterator());
    }

    /**
     * iterator.remove removes current element
     */
    public void testIteratorRemove() {
        final PriorityQueue<Item> q = new PriorityQueue<>(3);
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
        PriorityQueue<Item> q = populatedQueue(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /**
     * A deserialized/reserialized queue has same elements
     */
    public void testSerialization() throws Exception {
        Queue<Item> x = populatedQueue(SIZE);
        Queue<Item> y = serialClone(x);

        assertNotSame(x, y);
        mustEqual(x.size(), y.size());
        while (!x.isEmpty()) {
            assertFalse(y.isEmpty());
            mustEqual(x.remove(), y.remove());
        }
        assertTrue(y.isEmpty());
    }
}
