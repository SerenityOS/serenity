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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import junit.framework.Test;

public class ConcurrentLinkedQueueTest extends JSR166TestCase {

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return ConcurrentLinkedQueue.class; }
            public Collection emptyCollection() { return new ConcurrentLinkedQueue(); }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return true; }
            public boolean permitsNulls() { return false; }
        }
        return newTestSuite(ConcurrentLinkedQueueTest.class,
                            CollectionTest.testSuite(new Implementation()));
    }

    /**
     * Returns a new queue of given size containing consecutive
     * Items 0 ... n - 1.
     */
    private static ConcurrentLinkedQueue<Item> populatedQueue(int n) {
        ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
        assertTrue(q.isEmpty());
        for (int i = 0; i < n; ++i)
            mustOffer(q, i);
        assertFalse(q.isEmpty());
        mustEqual(n, q.size());
        mustEqual(0, q.peek());
        return q;
    }

    /**
     * new queue is empty
     */
    public void testConstructor1() {
        mustEqual(0, new ConcurrentLinkedQueue<Item>().size());
    }

    /**
     * Initializing from null Collection throws NPE
     */
    public void testConstructor3() {
        try {
            new ConcurrentLinkedQueue<Item>((Collection<Item>)null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection of null elements throws NPE
     */
    public void testConstructor4() {
        try {
            new ConcurrentLinkedQueue<Item>(Arrays.asList(new Item[SIZE]));
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
            new ConcurrentLinkedQueue<Item>(Arrays.asList(items));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Queue contains all elements of collection used to initialize
     */
    public void testConstructor6() {
        Item[] items = defaultItems;
        ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>(Arrays.asList(items));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * isEmpty is true before add, false after
     */
    public void testEmpty() {
        ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
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
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
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
        ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
        try {
            q.offer(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * add(null) throws NPE
     */
    public void testAddNull() {
        ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
        try {
            q.add(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Offer returns true
     */
    public void testOffer() {
        ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
        assertTrue(q.offer(zero));
        assertTrue(q.offer(one));
    }

    /**
     * add returns true
     */
    public void testAdd() {
        ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.size());
            mustAdd(q, i);
        }
    }

    /**
     * addAll(null) throws NullPointerException
     */
    public void testAddAll1() {
        ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
        try {
            q.addAll(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addAll(this) throws IllegalArgumentException
     */
    public void testAddAllSelf() {
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
        try {
            q.addAll(q);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * addAll of a collection with null elements throws NullPointerException
     */
    public void testAddAll2() {
        ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
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
        ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
        Item[] items = new Item[2];
        items[0] = zero;
        try {
            q.addAll(Arrays.asList(items));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Queue contains all elements, in traversal order, of successful addAll
     */
    public void testAddAll5() {
        Item[] empty = new Item[0];
        Item[] items = defaultItems;
        ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * poll succeeds unless empty
     */
    public void testPoll() {
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * peek returns next element, or null if empty
     */
    public void testPeek() {
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
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
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
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
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
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
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
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
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
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
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
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
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
        ConcurrentLinkedQueue<Item> p = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(q.containsAll(p));
            assertFalse(p.containsAll(q));
            mustAdd(p, i);
        }
        assertTrue(p.containsAll(q));
    }

    /**
     * retainAll(c) retains only those elements of c and reports true if change
     */
    public void testRetainAll() {
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
        ConcurrentLinkedQueue<Item> p = populatedQueue(SIZE);
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
            ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
            ConcurrentLinkedQueue<Item> p = populatedQueue(i);
            assertTrue(q.removeAll(p));
            mustEqual(SIZE - i, q.size());
            for (int j = 0; j < i; ++j) {
                Item x = p.remove();
                assertFalse(q.contains(x));
            }
        }
    }

    /**
     * toArray contains all elements in FIFO order
     */
    public void testToArray() {
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
        Object[] a = q.toArray();
        assertSame(Object[].class, a.getClass());
        for (Object o : a)
            assertSame(o, q.poll());
        assertTrue(q.isEmpty());
    }

    /**
     * toArray(a) contains all elements in FIFO order
     */
    public void testToArray2() {
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
        Item[] items = new Item[SIZE];
        Item[] array = q.toArray(items);
        assertSame(items, array);
        for (Item o : items)
            assertSame(o, q.poll());
        assertTrue(q.isEmpty());
    }

    /**
     * toArray(null) throws NullPointerException
     */
    public void testToArray_NullArg() {
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
        try {
            q.toArray((Object[])null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * toArray(incompatible array type) throws ArrayStoreException
     */
    @SuppressWarnings("CollectionToArraySafeParameter")
    public void testToArray_incompatibleArrayType() {
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
        try {
            q.toArray(new String[10]);
            shouldThrow();
        } catch (ArrayStoreException success) {}
    }

    /**
     * iterator iterates through all elements
     */
    public void testIterator() {
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
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
        assertIteratorExhausted(new ConcurrentLinkedQueue<>().iterator());
    }

    /**
     * iterator ordering is FIFO
     */
    public void testIteratorOrdering() {
        final ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
        q.add(one);
        q.add(two);
        q.add(three);

        int k = 0;
        for (Iterator<? extends Item> it = q.iterator(); it.hasNext();) {
            mustEqual(++k, it.next());
        }

        mustEqual(3, k);
    }

    /**
     * Modifications do not cause iterators to fail
     */
    public void testWeaklyConsistentIteration() {
        final ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
        q.add(one);
        q.add(two);
        q.add(three);

        for (Iterator<? extends Item> it = q.iterator(); it.hasNext();) {
            q.remove();
            it.next();
        }

        mustEqual(0, q.size());
    }

    /**
     * iterator.remove removes current element
     */
    public void testIteratorRemove() {
        final ConcurrentLinkedQueue<Item> q = new ConcurrentLinkedQueue<>();
        q.add(one);
        q.add(two);
        q.add(three);
        Iterator<? extends Item> it = q.iterator();
        it.next();
        it.remove();
        it = q.iterator();
        assertSame(it.next(), two);
        assertSame(it.next(), three);
        assertFalse(it.hasNext());
    }

    /**
     * toString contains toStrings of elements
     */
    public void testToString() {
        ConcurrentLinkedQueue<Item> q = populatedQueue(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /**
     * A deserialized/reserialized queue has same elements in same order
     */
    public void testSerialization() throws Exception {
        Queue<Item> x = populatedQueue(SIZE);
        Queue<Item> y = serialClone(x);

        assertNotSame(x, y);
        mustEqual(x.size(), y.size());
        mustEqual(x.toString(), y.toString());
        assertTrue(Arrays.equals(x.toArray(), y.toArray()));
        while (!x.isEmpty()) {
            assertFalse(y.isEmpty());
            mustEqual(x.remove(), y.remove());
        }
        assertTrue(y.isEmpty());
    }

    /**
     * remove(null), contains(null) always return false
     */
    public void testNeverContainsNull() {
        Collection<?>[] qs = {
            new ConcurrentLinkedQueue<>(),
            populatedQueue(2),
        };

        for (Collection<?> q : qs) {
            assertFalse(q.contains(null));
            assertFalse(q.remove(null));
        }
    }
}
