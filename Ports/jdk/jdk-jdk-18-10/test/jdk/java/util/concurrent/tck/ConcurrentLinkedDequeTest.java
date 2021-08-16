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
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;

import junit.framework.Test;

public class ConcurrentLinkedDequeTest extends JSR166TestCase {

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return ConcurrentLinkedDeque.class; }
            public Collection emptyCollection() { return new ConcurrentLinkedDeque(); }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return true; }
            public boolean permitsNulls() { return false; }
        }
        return newTestSuite(ConcurrentLinkedDequeTest.class,
                            CollectionTest.testSuite(new Implementation()));
    }

    /**
     * Returns a new deque of given size containing consecutive
     * Items 0 ... n - 1.
     */
    private static ConcurrentLinkedDeque<Item> populatedDeque(int n) {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        assertTrue(q.isEmpty());
        for (int i = 0; i < n; ++i)
            mustOffer(q, i);
        assertFalse(q.isEmpty());
        mustEqual(n, q.size());
        mustEqual(0, q.peekFirst());
        mustEqual((n - 1), q.peekLast());
        return q;
    }

    /**
     * new deque is empty
     */
    public void testConstructor1() {
        assertTrue(new ConcurrentLinkedDeque<Item>().isEmpty());
        mustEqual(0, new ConcurrentLinkedDeque<Item>().size());
    }

    /**
     * Initializing from null Collection throws NPE
     */
    public void testConstructor3() {
        try {
            new ConcurrentLinkedDeque<Item>((Collection<Item>)null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection of null elements throws NPE
     */
    public void testConstructor4() {
        try {
            new ConcurrentLinkedDeque<Item>(Arrays.asList(new Item[SIZE]));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection with some null elements throws NPE
     */
    public void testConstructor5() {
        Item[] items = new Item[2]; items[0] = zero;
        try {
            new ConcurrentLinkedDeque<Item>(Arrays.asList(items));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Deque contains all elements of collection used to initialize
     */
    public void testConstructor6() {
        Item[] items = defaultItems;
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>(Arrays.asList(items));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * isEmpty is true before add, false after
     */
    public void testEmpty() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        assertTrue(q.isEmpty());
        q.add(one);
        assertFalse(q.isEmpty());
        q.add(two);
        q.remove();
        q.remove();
        assertTrue(q.isEmpty());
    }

    /**
     * size() changes when elements added and removed
     */
    public void testSize() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
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
     * push(null) throws NPE
     */
    public void testPushNull() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        try {
            q.push(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * peekFirst() returns element inserted with push
     */
    public void testPush() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(3);
        q.pollLast();
        q.push(four);
        assertSame(four, q.peekFirst());
    }

    /**
     * pop() removes first element, or throws NSEE if empty
     */
    public void testPop() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.pop());
        }
        try {
            q.pop();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * offer(null) throws NPE
     */
    public void testOfferNull() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        try {
            q.offer(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * offerFirst(null) throws NPE
     */
    public void testOfferFirstNull() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        try {
            q.offerFirst(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * offerLast(null) throws NPE
     */
    public void testOfferLastNull() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        try {
            q.offerLast(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * offer(x) succeeds
     */
    public void testOffer() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        assertTrue(q.offer(zero));
        assertTrue(q.offer(one));
        assertSame(zero, q.peekFirst());
        assertSame(one, q.peekLast());
    }

    /**
     * offerFirst(x) succeeds
     */
    public void testOfferFirst() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        assertTrue(q.offerFirst(zero));
        assertTrue(q.offerFirst(one));
        assertSame(one, q.peekFirst());
        assertSame(zero, q.peekLast());
    }

    /**
     * offerLast(x) succeeds
     */
    public void testOfferLast() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        assertTrue(q.offerLast(zero));
        assertTrue(q.offerLast(one));
        assertSame(zero, q.peekFirst());
        assertSame(one, q.peekLast());
    }

    /**
     * add(null) throws NPE
     */
    public void testAddNull() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        try {
            q.add(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addFirst(null) throws NPE
     */
    public void testAddFirstNull() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        try {
            q.addFirst(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addLast(null) throws NPE
     */
    public void testAddLastNull() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        try {
            q.addLast(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * add(x) succeeds
     */
    public void testAdd() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        assertTrue(q.add(zero));
        assertTrue(q.add(one));
        assertSame(zero, q.peekFirst());
        assertSame(one, q.peekLast());
    }

    /**
     * addFirst(x) succeeds
     */
    public void testAddFirst() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        q.addFirst(zero);
        q.addFirst(one);
        assertSame(one, q.peekFirst());
        assertSame(zero, q.peekLast());
    }

    /**
     * addLast(x) succeeds
     */
    public void testAddLast() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        q.addLast(zero);
        q.addLast(one);
        assertSame(zero, q.peekFirst());
        assertSame(one, q.peekLast());
    }

    /**
     * addAll(null) throws NPE
     */
    public void testAddAll1() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        try {
            q.addAll(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addAll(this) throws IllegalArgumentException
     */
    public void testAddAllSelf() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        try {
            q.addAll(q);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * addAll of a collection with null elements throws NPE
     */
    public void testAddAll2() {
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
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
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        Item[] items = new Item[2]; items[0] = zero;
        try {
            q.addAll(Arrays.asList(items));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Deque contains all elements, in traversal order, of successful addAll
     */
    public void testAddAll5() {
        Item[] empty = new Item[0];
        Item[] items = defaultItems;
        ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * pollFirst() succeeds unless empty
     */
    public void testPollFirst() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.pollFirst());
        }
        assertNull(q.pollFirst());
    }

    /**
     * pollLast() succeeds unless empty
     */
    public void testPollLast() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            mustEqual(i, q.pollLast());
        }
        assertNull(q.pollLast());
    }

    /**
     * poll() succeeds unless empty
     */
    public void testPoll() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * peek() returns next element, or null if empty
     */
    public void testPeek() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.peek());
            mustEqual(i, q.poll());
            assertTrue(q.peek() == null ||
                       !q.peek().equals(i));
        }
        assertNull(q.peek());
    }

    /**
     * element() returns first element, or throws NSEE if empty
     */
    public void testElement() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
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
     * remove() removes next element, or throws NSEE if empty
     */
    public void testRemove() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
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
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
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
     * peekFirst() returns next element, or null if empty
     */
    public void testPeekFirst() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.peekFirst());
            mustEqual(i, q.pollFirst());
            assertTrue(q.peekFirst() == null ||
                       !q.peekFirst().equals(i));
        }
        assertNull(q.peekFirst());
    }

    /**
     * peekLast() returns next element, or null if empty
     */
    public void testPeekLast() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            mustEqual(i, q.peekLast());
            mustEqual(i, q.pollLast());
            assertTrue(q.peekLast() == null ||
                       !q.peekLast().equals(i));
        }
        assertNull(q.peekLast());
    }

    /**
     * getFirst() returns first element, or throws NSEE if empty
     */
    public void testFirstElement() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.getFirst());
            mustEqual(i, q.pollFirst());
        }
        try {
            q.getFirst();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * getLast() returns last element, or throws NSEE if empty
     */
    public void testLastElement() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            mustEqual(i, q.getLast());
            mustEqual(i, q.pollLast());
        }
        try {
            q.getLast();
            shouldThrow();
        } catch (NoSuchElementException success) {}
        assertNull(q.peekLast());
    }

    /**
     * removeFirst() removes first element, or throws NSEE if empty
     */
    public void testRemoveFirst() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.removeFirst());
        }
        try {
            q.removeFirst();
            shouldThrow();
        } catch (NoSuchElementException success) {}
        assertNull(q.peekFirst());
    }

    /**
     * removeLast() removes last element, or throws NSEE if empty
     */
    public void testRemoveLast() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            mustEqual(i, q.removeLast());
        }
        try {
            q.removeLast();
            shouldThrow();
        } catch (NoSuchElementException success) {}
        assertNull(q.peekLast());
    }

    /**
     * removeFirstOccurrence(x) removes x and returns true if present
     */
    public void testRemoveFirstOccurrence() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = 1; i < SIZE; i += 2) {
            assertTrue(q.removeFirstOccurrence(itemFor(i)));
        }
        for (int i = 0; i < SIZE; i += 2) {
            assertTrue(q.removeFirstOccurrence(itemFor(i)));
            assertFalse(q.removeFirstOccurrence(itemFor(i + 1)));
        }
        assertTrue(q.isEmpty());
    }

    /**
     * removeLastOccurrence(x) removes x and returns true if present
     */
    public void testRemoveLastOccurrence() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = 1; i < SIZE; i += 2) {
            assertTrue(q.removeLastOccurrence(itemFor(i)));
        }
        for (int i = 0; i < SIZE; i += 2) {
            assertTrue(q.removeLastOccurrence(itemFor(i)));
            assertFalse(q.removeLastOccurrence(itemFor(i + 1)));
        }
        assertTrue(q.isEmpty());
    }

    /**
     * contains(x) reports true when elements added but not yet removed
     */
    public void testContains() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustContain(q, i);
            q.poll();
            mustNotContain(q, i);
        }
    }

    /**
     * clear() removes all elements
     */
    public void testClear() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
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
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        ConcurrentLinkedDeque<Item> p = new ConcurrentLinkedDeque<>();
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
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        ConcurrentLinkedDeque<Item> p = populatedDeque(SIZE);
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
            ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
            ConcurrentLinkedDeque<Item> p = populatedDeque(i);
            assertTrue(q.removeAll(p));
            mustEqual(SIZE - i, q.size());
            for (int j = 0; j < i; ++j) {
                mustNotContain(q, p.remove());
            }
        }
    }

    /**
     * toArray() contains all elements in FIFO order
     */
    public void testToArray() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
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
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
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
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
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
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        try {
            q.toArray(new String[10]);
            shouldThrow();
        } catch (ArrayStoreException success) {}
    }

    /**
     * Iterator iterates through all elements
     */
    public void testIterator() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        Iterator<Item> it = q.iterator();
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
        Deque<Item> c = new ConcurrentLinkedDeque<>();
        assertIteratorExhausted(c.iterator());
        assertIteratorExhausted(c.descendingIterator());
    }

    /**
     * Iterator ordering is FIFO
     */
    public void testIteratorOrdering() {
        final ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
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
        final ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
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
     * iterator.remove() removes current element
     */
    public void testIteratorRemove() {
        final ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        final Random rng = new Random();
        for (int iters = 0; iters < 100; ++iters) {
            int max = rng.nextInt(5) + 2;
            int split = rng.nextInt(max - 1) + 1;
            for (int j = 1; j <= max; ++j)
                mustAdd(q, j);
            Iterator<? extends Item> it = q.iterator();
            for (int j = 1; j <= split; ++j)
                mustEqual(it.next(), j);
            it.remove();
            mustEqual(it.next(), itemFor(split + 1));
            for (int j = 1; j <= split; ++j)
                q.remove(itemFor(j));
            it = q.iterator();
            for (int j = split + 1; j <= max; ++j) {
                mustEqual(it.next(), j);
                it.remove();
            }
            assertFalse(it.hasNext());
            assertTrue(q.isEmpty());
        }
    }

    /**
     * Descending iterator iterates through all elements
     */
    public void testDescendingIterator() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        int i = 0;
        Iterator<? extends Item> it = q.descendingIterator();
        while (it.hasNext()) {
            mustContain(q, it.next());
            ++i;
        }
        mustEqual(i, SIZE);
        assertFalse(it.hasNext());
        try {
            it.next();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * Descending iterator ordering is reverse FIFO
     */
    public void testDescendingIteratorOrdering() {
        final ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        for (int iters = 0; iters < 100; ++iters) {
            mustAdd(q, three);
            mustAdd(q, two);
            mustAdd(q, one);
            int k = 0;
            for (Iterator<? extends Item> it = q.descendingIterator(); it.hasNext();) {
                mustEqual(++k, it.next());
            }

            mustEqual(3, k);
            q.remove();
            q.remove();
            q.remove();
        }
    }

    /**
     * descendingIterator.remove() removes current element
     */
    public void testDescendingIteratorRemove() {
        final ConcurrentLinkedDeque<Item> q = new ConcurrentLinkedDeque<>();
        final Random rng = new Random();
        for (int iters = 0; iters < 100; ++iters) {
            int max = rng.nextInt(5) + 2;
            int split = rng.nextInt(max - 1) + 1;
            for (int j = max; j >= 1; --j)
                mustAdd(q, j);
            Iterator<? extends Item> it = q.descendingIterator();
            for (int j = 1; j <= split; ++j)
                mustEqual(it.next(), j);
            it.remove();
            mustEqual(it.next(), itemFor(split + 1));
            for (int j = 1; j <= split; ++j)
                q.remove(itemFor(j));
            it = q.descendingIterator();
            for (int j = split + 1; j <= max; ++j) {
                mustEqual(it.next(), j);
                it.remove();
            }
            assertFalse(it.hasNext());
            assertTrue(q.isEmpty());
        }
    }

    /**
     * toString() contains toStrings of elements
     */
    public void testToString() {
        ConcurrentLinkedDeque<Item> q = populatedDeque(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /**
     * A deserialized/reserialized deque has same elements in same order
     */
    public void testSerialization() throws Exception {
        Queue<Item> x = populatedDeque(SIZE);
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
     * contains(null) always return false.
     * remove(null) always throws NullPointerException.
     */
    public void testNeverContainsNull() {
        Deque<?>[] qs = {
            new ConcurrentLinkedDeque<>(),
            populatedDeque(2),
        };

        for (Deque<?> q : qs) {
            assertFalse(q.contains(null));
            try {
                assertFalse(q.remove(null));
                shouldThrow();
            } catch (NullPointerException success) {}
            try {
                assertFalse(q.removeFirstOccurrence(null));
                shouldThrow();
            } catch (NullPointerException success) {}
            try {
                assertFalse(q.removeLastOccurrence(null));
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    void runAsync(Runnable r1, Runnable r2) {
        boolean b = randomBoolean();
        CompletableFuture<Void> f1 = CompletableFuture.runAsync(b ? r1 : r2);
        CompletableFuture<Void> f2 = CompletableFuture.runAsync(b ? r2 : r1);
        f1.join();
        f2.join();
    }

    /**
     * Non-traversing Deque operations are linearizable.
     * https://bugs.openjdk.java.net/browse/JDK-8188900
     * ant -Djsr166.expensiveTests=true -Djsr166.tckTestClass=ConcurrentLinkedDequeTest -Djsr166.methodFilter=testBug8188900 tck
     */
    public void testBug8188900() {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final LongAdder nulls = new LongAdder(), zeros = new LongAdder();
        for (int n = expensiveTests ? 100_000 : 10; n--> 0; ) {
            ConcurrentLinkedDeque<Item> d = new ConcurrentLinkedDeque<>();

            boolean peek = rnd.nextBoolean();
            Runnable getter = () -> {
                Item x = peek ? d.peekFirst() : d.pollFirst();
                if (x == null) nulls.increment();
                else if (x.value == 0) zeros.increment();
                else
                    throw new AssertionError(
                        String.format(
                            "unexpected value %s after %d nulls and %d zeros",
                            x, nulls.sum(), zeros.sum()));
            };

            Runnable adder = () -> { d.addFirst(zero); d.addLast(fortytwo); };

            runAsync(getter, adder);
        }
    }

    /**
     * Reverse direction variant of testBug8188900
     */
    public void testBug8188900_reverse() {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final LongAdder nulls = new LongAdder(), zeros = new LongAdder();
        for (int n = expensiveTests ? 100_000 : 10; n--> 0; ) {
            ConcurrentLinkedDeque<Item> d = new ConcurrentLinkedDeque<>();

            boolean peek = rnd.nextBoolean();
            Runnable getter = () -> {
                Item x = peek ? d.peekLast() : d.pollLast();
                if (x == null) nulls.increment();
                else if (x.value == 0) zeros.increment();
                else
                    throw new AssertionError(
                        String.format(
                            "unexpected value %s after %d nulls and %d zeros",
                            x, nulls.sum(), zeros.sum()));
            };

            Runnable adder = () -> { d.addLast(zero); d.addFirst(fortytwo); };

            runAsync(getter, adder);
        }
    }

    /**
     * Non-traversing Deque operations (that return null) are linearizable.
     * Don't return null when the deque is observably never empty.
     * https://bugs.openjdk.java.net/browse/JDK-8189387
     * ant -Djsr166.expensiveTests=true -Djsr166.tckTestClass=ConcurrentLinkedDequeTest -Djsr166.methodFilter=testBug8189387 tck
     */
    public void testBug8189387() {
        Object x = new Object();
        for (int n = expensiveTests ? 100_000 : 10; n--> 0; ) {
            ConcurrentLinkedDeque<Object> d = new ConcurrentLinkedDeque<>();
            Runnable add = chooseRandomly(
                () -> d.addFirst(x),
                () -> d.offerFirst(x),
                () -> d.addLast(x),
                () -> d.offerLast(x));

            Runnable get = chooseRandomly(
                () -> assertFalse(d.isEmpty()),
                () -> assertSame(x, d.peekFirst()),
                () -> assertSame(x, d.peekLast()),
                () -> assertSame(x, d.pollFirst()),
                () -> assertSame(x, d.pollLast()));

            Runnable addRemove = chooseRandomly(
                () -> { d.addFirst(x); d.pollLast(); },
                () -> { d.offerFirst(x); d.removeFirst(); },
                () -> { d.offerLast(x); d.removeLast(); },
                () -> { d.addLast(x); d.pollFirst(); });

            add.run();
            runAsync(get, addRemove);
        }
    }
}
