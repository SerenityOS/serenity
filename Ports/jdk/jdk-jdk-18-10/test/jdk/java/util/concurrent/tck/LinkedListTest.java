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
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

import junit.framework.Test;

public class LinkedListTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return LinkedList.class; }
            public List emptyCollection() { return new LinkedList(); }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return false; }
            public boolean permitsNulls() { return true; }
        }
        class SubListImplementation extends Implementation {
            @SuppressWarnings("unchecked")
            public List emptyCollection() {
                List list = super.emptyCollection();
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                if (rnd.nextBoolean())
                    list.add(makeElement(rnd.nextInt()));
                int i = rnd.nextInt(list.size() + 1);
                return list.subList(i, i);
            }
        }
        return newTestSuite(
                LinkedListTest.class,
                CollectionTest.testSuite(new Implementation()),
                CollectionTest.testSuite(new SubListImplementation()));
    }

    /**
     * Returns a new queue of given size containing consecutive
     * Items 0 ... n - 1.
     */
    private static LinkedList<Item> populatedQueue(int n) {
        LinkedList<Item> q = new LinkedList<>();
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
     * new queue is empty
     */
    public void testConstructor1() {
        mustEqual(0, new LinkedList<Item>().size());
    }

    /**
     * Initializing from null Collection throws NPE
     */
    public void testConstructor3() {
        try {
            new LinkedList<Item>((Collection<Item>)null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Queue contains all elements of collection used to initialize
     */
    public void testConstructor6() {
        Item[] items = defaultItems;
        LinkedList<Item> q = new LinkedList<>(Arrays.asList(items));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * isEmpty is true before add, false after
     */
    public void testEmpty() {
        LinkedList<Item> q = new LinkedList<>();
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
        LinkedList<Item> q = populatedQueue(SIZE);
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
     * offer(null) succeeds
     */
    public void testOfferNull() {
        LinkedList<Item> q = new LinkedList<>();
        q.offer(null);
        assertNull(q.get(0));
        assertTrue(q.contains(null));
    }

    /**
     * Offer succeeds
     */
    public void testOffer() {
        LinkedList<Item> q = new LinkedList<>();
        mustOffer(q, zero);
        mustOffer(q, one);
    }

    /**
     * add succeeds
     */
    public void testAdd() {
        LinkedList<Item> q = new LinkedList<>();
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.size());
            mustAdd(q, i);
        }
    }

    /**
     * addAll(null) throws NPE
     */
    public void testAddAll1() {
        LinkedList<Item> q = new LinkedList<>();
        try {
            q.addAll(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Queue contains all elements, in traversal order, of successful addAll
     */
    public void testAddAll5() {
        Item[] empty = new Item[0];
        Item[] items = defaultItems;
        LinkedList<Item> q = new LinkedList<>();
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * addAll with too large an index throws IOOBE
     */
    public void testAddAll2_IndexOutOfBoundsException() {
        LinkedList<Item> l = new LinkedList<>();
        l.add(zero);
        LinkedList<Item> m = new LinkedList<>();
        m.add(one);
        try {
            l.addAll(4,m);
            shouldThrow();
        } catch (IndexOutOfBoundsException success) {}
    }

    /**
     * addAll with negative index throws IOOBE
     */
    public void testAddAll4_BadIndex() {
        LinkedList<Item> l = new LinkedList<>();
        l.add(zero);
        LinkedList<Item> m = new LinkedList<>();
        m.add(one);
        try {
            l.addAll(-1,m);
            shouldThrow();
        } catch (IndexOutOfBoundsException success) {}
    }

    /**
     * poll succeeds unless empty
     */
    public void testPoll() {
        LinkedList<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * peek returns next element, or null if empty
     */
    public void testPeek() {
        LinkedList<Item> q = populatedQueue(SIZE);
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
        LinkedList<Item> q = populatedQueue(SIZE);
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
        LinkedList<Item> q = populatedQueue(SIZE);
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
        LinkedList<Item> q = populatedQueue(SIZE);
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
        LinkedList<Item> q = populatedQueue(SIZE);
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
        LinkedList<Item> q = populatedQueue(SIZE);
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
        LinkedList<Item> q = populatedQueue(SIZE);
        LinkedList<Item> p = new LinkedList<>();
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
        LinkedList<Item> q = populatedQueue(SIZE);
        LinkedList<Item> p = populatedQueue(SIZE);
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
            LinkedList<Item> q = populatedQueue(SIZE);
            LinkedList<Item> p = populatedQueue(i);
            assertTrue(q.removeAll(p));
            mustEqual(SIZE - i, q.size());
            for (int j = 0; j < i; ++j) {
                mustNotContain(q, p.remove());
            }
        }
    }

    /**
     * toArray contains all elements in FIFO order
     */
    public void testToArray() {
        LinkedList<Item> q = populatedQueue(SIZE);
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
        LinkedList<Item> q = populatedQueue(SIZE);
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
        LinkedList<Item> l = new LinkedList<>();
        l.add(zero);
        try {
            l.toArray((Item[])null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * toArray(incompatible array type) throws ArrayStoreException
     */
    @SuppressWarnings("CollectionToArraySafeParameter")
    public void testToArray_incompatibleArrayType() {
        LinkedList<Item> l = new LinkedList<>();
        l.add(five);
        try {
            l.toArray(new String[10]);
            shouldThrow();
        } catch (ArrayStoreException success) {}
    }

    /**
     * iterator iterates through all elements
     */
    public void testIterator() {
        LinkedList<Item> q = populatedQueue(SIZE);
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
        assertIteratorExhausted(new LinkedList<>().iterator());
    }

    /**
     * iterator ordering is FIFO
     */
    public void testIteratorOrdering() {
        final LinkedList<Item> q = new LinkedList<>();
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
     * iterator.remove removes current element
     */
    public void testIteratorRemove() {
        final LinkedList<Item> q = new LinkedList<>();
        q.add(one);
        q.add(two);
        q.add(three);
        Iterator<? extends Item> it = q.iterator();
        mustEqual(1, it.next());
        it.remove();
        it = q.iterator();
        mustEqual(2, it.next());
        mustEqual(3, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Descending iterator iterates through all elements
     */
    public void testDescendingIterator() {
        LinkedList<Item> q = populatedQueue(SIZE);
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
        final LinkedList<Item> q = new LinkedList<>();
        q.add(three);
        q.add(two);
        q.add(one);
        int k = 0;
        for (Iterator<? extends Item> it = q.descendingIterator(); it.hasNext();) {
            mustEqual(++k, it.next());
        }

        mustEqual(3, k);
    }

    /**
     * descendingIterator.remove removes current element
     */
    public void testDescendingIteratorRemove() {
        final LinkedList<Item> q = new LinkedList<>();
        q.add(three);
        q.add(two);
        q.add(one);
        Iterator<? extends Item> it = q.descendingIterator();
        it.next();
        it.remove();
        it = q.descendingIterator();
        assertSame(it.next(), two);
        assertSame(it.next(), three);
        assertFalse(it.hasNext());
    }

    /**
     * toString contains toStrings of elements
     */
    public void testToString() {
        LinkedList<Item> q = populatedQueue(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /**
     * peek returns element inserted with addFirst
     */
    public void testAddFirst() {
        LinkedList<Item> q = populatedQueue(3);
        q.addFirst(four);
        assertSame(four, q.peek());
    }

    /**
     * peekFirst returns element inserted with push
     */
    public void testPush() {
        LinkedList<Item> q = populatedQueue(3);
        q.push(four);
        assertSame(four, q.peekFirst());
    }

    /**
     * pop removes next element, or throws NSEE if empty
     */
    public void testPop() {
        LinkedList<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.pop());
        }
        try {
            q.pop();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * OfferFirst succeeds
     */
    public void testOfferFirst() {
        LinkedList<Item> q = new LinkedList<>();
        assertTrue(q.offerFirst(zero));
        assertTrue(q.offerFirst(one));
    }

    /**
     * OfferLast succeeds
     */
    public void testOfferLast() {
        LinkedList<Item> q = new LinkedList<>();
        assertTrue(q.offerLast(zero));
        assertTrue(q.offerLast(one));
    }

    /**
     * pollLast succeeds unless empty
     */
    public void testPollLast() {
        LinkedList<Item> q = populatedQueue(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            mustEqual(i, q.pollLast());
        }
        assertNull(q.pollLast());
    }

    /**
     * peekFirst returns next element, or null if empty
     */
    public void testPeekFirst() {
        LinkedList<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.peekFirst());
            mustEqual(i, q.pollFirst());
            assertTrue(q.peekFirst() == null ||
                       !q.peekFirst().equals(i));
        }
        assertNull(q.peekFirst());
    }

    /**
     * peekLast returns next element, or null if empty
     */
    public void testPeekLast() {
        LinkedList<Item> q = populatedQueue(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            mustEqual(i, q.peekLast());
            mustEqual(i, q.pollLast());
            assertTrue(q.peekLast() == null ||
                       !q.peekLast().equals(i));
        }
        assertNull(q.peekLast());
    }

    public void testFirstElement() {
        LinkedList<Item> q = populatedQueue(SIZE);
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
     * getLast returns next element, or throws NSEE if empty
     */
    public void testLastElement() {
        LinkedList<Item> q = populatedQueue(SIZE);
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
     * removeFirstOccurrence(x) removes x and returns true if present
     */
    public void testRemoveFirstOccurrence() {
        LinkedList<Item> q = populatedQueue(SIZE);
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
        LinkedList<Item> q = populatedQueue(SIZE);
        for (int i = 1; i < SIZE; i += 2) {
            assertTrue(q.removeLastOccurrence(itemFor(i)));
        }
        for (int i = 0; i < SIZE; i += 2) {
            assertTrue(q.removeLastOccurrence(itemFor(i)));
            assertFalse(q.removeLastOccurrence(itemFor(i + 1)));
        }
        assertTrue(q.isEmpty());
    }

}
