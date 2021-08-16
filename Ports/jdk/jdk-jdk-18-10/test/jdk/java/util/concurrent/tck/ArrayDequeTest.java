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
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import junit.framework.Test;

public class ArrayDequeTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return ArrayDeque.class; }
            public Collection emptyCollection() { return populatedDeque(0); }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return false; }
            public boolean permitsNulls() { return false; }
        }
        return newTestSuite(ArrayDequeTest.class,
                            CollectionTest.testSuite(new Implementation()));
    }

    /**
     * Returns a new deque of given size containing consecutive
     * Items 0 ... n - 1.
     */
    private static ArrayDeque<Item> populatedDeque(int n) {
        // Randomize various aspects of memory layout, including
        // capacity slop and wraparound.
        final ArrayDeque<Item> q;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        switch (rnd.nextInt(6)) {
        case 0: q = new ArrayDeque<>();      break;
        case 1: q = new ArrayDeque<>(0);     break;
        case 2: q = new ArrayDeque<>(1);     break;
        case 3: q = new ArrayDeque<>(Math.max(0, n - 1)); break;
        case 4: q = new ArrayDeque<>(n);     break;
        case 5: q = new ArrayDeque<>(n + 1); break;
        default: throw new AssertionError();
        }
        switch (rnd.nextInt(3)) {
        case 0:
            q.addFirst(fortytwo);
            mustEqual(42, q.removeLast());
            break;
        case 1:
            q.addLast(fortytwo);
            mustEqual(42, q.removeFirst());
            break;
        case 2: /* do nothing */ break;
        default: throw new AssertionError();
        }
        assertTrue(q.isEmpty());
        if (rnd.nextBoolean())
            for (int i = 0; i < n; i++)
                assertTrue(q.offerLast(itemFor(i)));
        else
            for (int i = n; --i >= 0; )
                q.addFirst(itemFor(i));
        mustEqual(n, q.size());
        if (n > 0) {
            assertFalse(q.isEmpty());
            mustEqual(0, q.peekFirst());
            mustEqual((n - 1), q.peekLast());
        }
        return q;
    }

    /**
     * new deque is empty
     */
    public void testConstructor1() {
        mustEqual(0, new ArrayDeque<Item>().size());
    }

    /**
     * Initializing from null Collection throws NPE
     */
    public void testConstructor3() {
        try {
            new ArrayDeque<Object>((Collection<Object>)null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection of null elements throws NPE
     */
    public void testConstructor4() {
        try {
            new ArrayDeque<Item>(Arrays.asList(new Item[SIZE]));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection with some null elements throws NPE
     */
    public void testConstructor5() {
        Item[] items = new Item[2]; items[0] = zero;
        try {
            new ArrayDeque<Item>(Arrays.asList(items));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Deque contains all elements of collection used to initialize
     */
    public void testConstructor6() {
        Item[] items = defaultItems;
        ArrayDeque<Item> q = new ArrayDeque<>(Arrays.asList(items));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.pollFirst());
    }

    /**
     * isEmpty is true before add, false after
     */
    public void testEmpty() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        assertTrue(q.isEmpty());
        q.add(one);
        assertFalse(q.isEmpty());
        q.add(two);
        q.removeFirst();
        q.removeFirst();
        assertTrue(q.isEmpty());
    }

    /**
     * size changes when elements added and removed
     */
    public void testSize() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(SIZE - i, q.size());
            q.removeFirst();
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
        ArrayDeque<Item> q = new ArrayDeque<>(1);
        try {
            q.push(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * peekFirst() returns element inserted with push
     */
    public void testPush() {
        ArrayDeque<Item> q = populatedDeque(3);
        q.pollLast();
        q.push(four);
        assertSame(four, q.peekFirst());
    }

    /**
     * pop() removes next element, or throws NSEE if empty
     */
    public void testPop() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
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
        ArrayDeque<Item> q = new ArrayDeque<>();
        try {
            q.offer(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * offerFirst(null) throws NPE
     */
    public void testOfferFirstNull() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        try {
            q.offerFirst(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * offerLast(null) throws NPE
     */
    public void testOfferLastNull() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        try {
            q.offerLast(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * offer(x) succeeds
     */
    public void testOffer() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        assertTrue(q.offer(zero));
        assertTrue(q.offer(one));
        assertSame(zero, q.peekFirst());
        assertSame(one, q.peekLast());
    }

    /**
     * offerFirst(x) succeeds
     */
    public void testOfferFirst() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        assertTrue(q.offerFirst(zero));
        assertTrue(q.offerFirst(one));
        assertSame(one, q.peekFirst());
        assertSame(zero, q.peekLast());
    }

    /**
     * offerLast(x) succeeds
     */
    public void testOfferLast() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        assertTrue(q.offerLast(zero));
        assertTrue(q.offerLast(one));
        assertSame(zero, q.peekFirst());
        assertSame(one, q.peekLast());
    }

    /**
     * add(null) throws NPE
     */
    public void testAddNull() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        try {
            q.add(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addFirst(null) throws NPE
     */
    public void testAddFirstNull() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        try {
            q.addFirst(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addLast(null) throws NPE
     */
    public void testAddLastNull() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        try {
            q.addLast(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * add(x) succeeds
     */
    public void testAdd() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        assertTrue(q.add(zero));
        assertTrue(q.add(one));
        assertSame(zero, q.peekFirst());
        assertSame(one, q.peekLast());
    }

    /**
     * addFirst(x) succeeds
     */
    public void testAddFirst() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        q.addFirst(zero);
        q.addFirst(one);
        assertSame(one, q.peekFirst());
        assertSame(zero, q.peekLast());
    }

    /**
     * addLast(x) succeeds
     */
    public void testAddLast() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        q.addLast(zero);
        q.addLast(one);
        assertSame(zero, q.peekFirst());
        assertSame(one, q.peekLast());
    }

    /**
     * addAll(null) throws NPE
     */
    public void testAddAll1() {
        ArrayDeque<Item> q = new ArrayDeque<>();
        try {
            q.addAll(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addAll of a collection with null elements throws NPE
     */
    public void testAddAll2() {
        ArrayDeque<Item> q = new ArrayDeque<>();
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
        ArrayDeque<Item> q = new ArrayDeque<>();
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
        ArrayDeque<Item> q = new ArrayDeque<>();
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.pollFirst());
    }

    /**
     * pollFirst() succeeds unless empty
     */
    public void testPollFirst() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.pollFirst());
        }
        assertNull(q.pollFirst());
    }

    /**
     * pollLast() succeeds unless empty
     */
    public void testPollLast() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            mustEqual(i, q.pollLast());
        }
        assertNull(q.pollLast());
    }

    /**
     * poll() succeeds unless empty
     */
    public void testPoll() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * remove() removes next element, or throws NSEE if empty
     */
    public void testRemove() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
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
        ArrayDeque<Item> q = populatedDeque(SIZE);
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
        ArrayDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.peekFirst());
            mustEqual(i, q.pollFirst());
            assertTrue(q.peekFirst() == null ||
                       !q.peekFirst().equals(i));
        }
        assertNull(q.peekFirst());
    }

    /**
     * peek() returns next element, or null if empty
     */
    public void testPeek() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.peek());
            mustEqual(i, q.poll());
            assertTrue(q.peek() == null ||
                       !q.peek().equals(i));
        }
        assertNull(q.peek());
    }

    /**
     * peekLast() returns next element, or null if empty
     */
    public void testPeekLast() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            mustEqual(i, q.peekLast());
            mustEqual(i, q.pollLast());
            assertTrue(q.peekLast() == null ||
                       !q.peekLast().equals(i));
        }
        assertNull(q.peekLast());
    }

    /**
     * element() returns first element, or throws NSEE if empty
     */
    public void testElement() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
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
     * getFirst() returns first element, or throws NSEE if empty
     */
    public void testFirstElement() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
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
        ArrayDeque<Item> q = populatedDeque(SIZE);
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
        ArrayDeque<Item> q = populatedDeque(SIZE);
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
        ArrayDeque<Item> q = populatedDeque(SIZE);
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
        Deque<Item> q = populatedDeque(SIZE);
        assertFalse(q.removeFirstOccurrence(null));
        for (int i = 1; i < SIZE; i += 2) {
            assertTrue(q.removeFirstOccurrence(itemFor(i)));
            mustNotContain(q, i);
        }
        for (int i = 0; i < SIZE; i += 2) {
            assertTrue(q.removeFirstOccurrence(itemFor(i)));
            assertFalse(q.removeFirstOccurrence(itemFor(i + 1)));
            mustNotContain(q, i);
            mustNotContain(q, i + 1);
        }
        assertTrue(q.isEmpty());
        assertFalse(q.removeFirstOccurrence(null));
        assertFalse(q.removeFirstOccurrence(fortytwo));
        q = new ArrayDeque<>();
        assertFalse(q.removeFirstOccurrence(null));
        assertFalse(q.removeFirstOccurrence(fortytwo));
    }

    /**
     * removeLastOccurrence(x) removes x and returns true if present
     */
    public void testRemoveLastOccurrence() {
        Deque<Item> q = populatedDeque(SIZE);
        assertFalse(q.removeLastOccurrence(null));
        for (int i = 1; i < SIZE; i += 2) {
            assertTrue(q.removeLastOccurrence(itemFor(i)));
            mustNotContain(q, i);
        }
        for (int i = 0; i < SIZE; i += 2) {
            assertTrue(q.removeLastOccurrence(itemFor(i)));
            assertFalse(q.removeLastOccurrence(itemFor(i + 1)));
            mustNotContain(q, i);
            mustNotContain(q, i + 1);
        }
        assertTrue(q.isEmpty());
        assertFalse(q.removeLastOccurrence(null));
        assertFalse(q.removeLastOccurrence(fortytwo));
        q = new ArrayDeque<>();
        assertFalse(q.removeLastOccurrence(null));
        assertFalse(q.removeLastOccurrence(fortytwo));
    }

    /**
     * contains(x) reports true when elements added but not yet removed
     */
    public void testContains() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustContain(q, i);
            mustEqual(i, q.pollFirst());
            mustNotContain(q, i);
        }
    }

    /**
     * clear removes all elements
     */
    public void testClear() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
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
        ArrayDeque<Item> q = populatedDeque(SIZE);
        ArrayDeque<Item> p = new ArrayDeque<>();
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
        ArrayDeque<Item> q = populatedDeque(SIZE);
        ArrayDeque<Item> p = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            boolean changed = q.retainAll(p);
            mustEqual(changed, (i > 0));
            assertTrue(q.containsAll(p));
            mustEqual(SIZE - i, q.size());
            p.removeFirst();
        }
    }

    /**
     * removeAll(c) removes only those elements of c and reports true if changed
     */
    public void testRemoveAll() {
        for (int i = 1; i < SIZE; ++i) {
            ArrayDeque<Item> q = populatedDeque(SIZE);
            ArrayDeque<Item> p = populatedDeque(i);
            assertTrue(q.removeAll(p));
            mustEqual(SIZE - i, q.size());
            for (int j = 0; j < i; ++j) {
                mustNotContain(q, p.removeFirst());
            }
        }
    }

    void checkToArray(ArrayDeque<Item> q) {
        int size = q.size();
        Object[] a1 = q.toArray();
        mustEqual(size, a1.length);
        Item[] a2 = q.toArray(new Item[0]);
        mustEqual(size, a2.length);
        Item[] a3 = q.toArray(new Item[Math.max(0, size - 1)]);
        mustEqual(size, a3.length);
        Item[] a4 = new Item[size];
        assertSame(a4, q.toArray(a4));
        Item[] a5 = new Item[size + 1];
        Arrays.fill(a5, fortytwo);
        assertSame(a5, q.toArray(a5));
        Item[] a6 = new Item[size + 2];
        Arrays.fill(a6, fortytwo);
        assertSame(a6, q.toArray(a6));
        Object[][] as = { a1, a2, a3, a4, a5, a6 };
        for (Object[] a : as) {
            if (a.length > size) assertNull(a[size]);
            if (a.length > size + 1) mustEqual(fortytwo, a[size + 1]);
        }
        Iterator<? extends Item> it = q.iterator();
        Item s = q.peekFirst();
        for (int i = 0; i < size; i++) {
            Item x = (Item) it.next();
            mustEqual(s.value + i, x);
            for (Object[] a : as)
                assertSame(a[i], x);
        }
    }

    /**
     * toArray() and toArray(a) contain all elements in FIFO order
     */
    public void testToArray() {
        final int size = ThreadLocalRandom.current().nextInt(10);
        ArrayDeque<Item> q = new ArrayDeque<>(size);
        for (int i = 0; i < size; i++) {
            checkToArray(q);
            q.addLast(itemFor(i));
        }
        // Provoke wraparound
        int added = size * 2;
        for (int i = 0; i < added; i++) {
            checkToArray(q);
            mustEqual(i, q.poll());
            q.addLast(itemFor(size + i));
        }
        for (int i = 0; i < size; i++) {
            checkToArray(q);
            mustEqual((added + i), q.poll());
        }
    }

    /**
     * toArray(null) throws NullPointerException
     */
    public void testToArray_NullArg() {
        ArrayDeque<Item> l = new ArrayDeque<>();
        l.add(zero);
        try {
            l.toArray((Object[])null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * toArray(incompatible array type) throws ArrayStoreException
     */
    @SuppressWarnings("CollectionToArraySafeParameter")
    public void testToArray_incompatibleArrayType() {
        ArrayDeque<Item> l = new ArrayDeque<>();
        l.add(five);
        try {
            l.toArray(new String[10]);
            shouldThrow();
        } catch (ArrayStoreException success) {}
        try {
            l.toArray(new String[0]);
            shouldThrow();
        } catch (ArrayStoreException success) {}
    }

    /**
     * Iterator iterates through all elements
     */
    public void testIterator() {
        ArrayDeque<Item> q = populatedDeque(SIZE);
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
        Deque<Item> c = new ArrayDeque<>();
        assertIteratorExhausted(c.iterator());
        assertIteratorExhausted(c.descendingIterator());
    }

    /**
     * Iterator ordering is FIFO
     */
    public void testIteratorOrdering() {
        final ArrayDeque<Item> q = new ArrayDeque<>();
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
     * iterator.remove() removes current element
     */
    public void testIteratorRemove() {
        final ArrayDeque<Item> q = new ArrayDeque<>();
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
            mustEqual(it.next(), split + 1);
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
        ArrayDeque<Item> q = populatedDeque(SIZE);
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
        final ArrayDeque<Item> q = new ArrayDeque<>();
        for (int iters = 0; iters < 100; ++iters) {
            q.add(three);
            q.add(two);
            q.add(one);
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
        final ArrayDeque<Item> q = new ArrayDeque<>();
        final Random rng = new Random();
        for (int iters = 0; iters < 100; ++iters) {
            int max = rng.nextInt(5) + 2;
            int split = rng.nextInt(max - 1) + 1;
            for (int j = max; j >= 1; --j)
                q.add(itemFor(j));
            Iterator<? extends Item> it = q.descendingIterator();
            for (int j = 1; j <= split; ++j)
                mustEqual(it.next(), itemFor(j));
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
        ArrayDeque<Item> q = populatedDeque(SIZE);
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

        assertNotSame(y, x);
        mustEqual(x.size(), y.size());
        mustEqual(x.toString(), y.toString());
        mustEqual(Arrays.toString(x.toArray()), Arrays.toString(y.toArray()));
        assertTrue(Arrays.equals(x.toArray(), y.toArray()));
        while (!x.isEmpty()) {
            assertFalse(y.isEmpty());
            mustEqual(x.remove(), y.remove());
        }
        assertTrue(y.isEmpty());
    }

    /**
     * A cloned deque has same elements in same order
     */
    public void testClone() throws Exception {
        ArrayDeque<Item> x = populatedDeque(SIZE);
        ArrayDeque<Item> y = x.clone();

        assertNotSame(y, x);
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
        Deque<?>[] qs = {
            new ArrayDeque<>(),
            populatedDeque(2),
        };

        for (Deque<?> q : qs) {
            assertFalse(q.contains(null));
            assertFalse(q.remove(null));
            assertFalse(q.removeFirstOccurrence(null));
            assertFalse(q.removeLastOccurrence(null));
        }
    }

}
