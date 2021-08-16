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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;

import junit.framework.Test;

public class LinkedBlockingDequeTest extends JSR166TestCase {

    public static class Unbounded extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new LinkedBlockingDeque();
        }
    }

    public static class Bounded extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new LinkedBlockingDeque(SIZE);
        }
    }

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return LinkedBlockingDeque.class; }
            public Collection emptyCollection() { return new LinkedBlockingDeque(); }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return true; }
            public boolean permitsNulls() { return false; }
        }
        return newTestSuite(LinkedBlockingDequeTest.class,
                            new Unbounded().testSuite(),
                            new Bounded().testSuite(),
                            CollectionTest.testSuite(new Implementation()));
    }

    /**
     * Returns a new deque of given size containing consecutive
     * Items 0 ... n - 1.
     */
    private static LinkedBlockingDeque<Item> populatedDeque(int n) {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(n);
        assertTrue(q.isEmpty());
        for (int i = 0; i < n; i++)
            mustOffer(q, i);
        assertFalse(q.isEmpty());
        mustEqual(0, q.remainingCapacity());
        mustEqual(n, q.size());
        mustEqual(0, q.peekFirst());
        mustEqual((n - 1), q.peekLast());
        return q;
    }

    /**
     * isEmpty is true before add, false after
     */
    public void testEmpty() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>();
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
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(SIZE - i, q.size());
            q.removeFirst();
        }
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.size());
            mustAdd(q, one);
        }
    }

    /**
     * offerFirst(null) throws NullPointerException
     */
    public void testOfferFirstNull() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>();
        try {
            q.offerFirst(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * offerLast(null) throws NullPointerException
     */
    public void testOfferLastNull() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>();
        try {
            q.offerLast(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * OfferFirst succeeds
     */
    public void testOfferFirst() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>();
        assertTrue(q.offerFirst(zero));
        assertTrue(q.offerFirst(two));
    }

    /**
     * OfferLast succeeds
     */
    public void testOfferLast() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>();
        assertTrue(q.offerLast(zero));
        assertTrue(q.offerLast(one));
    }

    /**
     * pollFirst succeeds unless empty
     */
    public void testPollFirst() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.pollFirst());
        }
        assertNull(q.pollFirst());
    }

    /**
     * pollLast succeeds unless empty
     */
    public void testPollLast() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = SIZE - 1; i >= 0; --i) {
            mustEqual(i, q.pollLast());
        }
        assertNull(q.pollLast());
    }

    /**
     * peekFirst returns next element, or null if empty
     */
    public void testPeekFirst() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.peekFirst());
            mustEqual(i, q.pollFirst());
            assertTrue(q.peekFirst() == null ||
                       !q.peekFirst().equals(i));
        }
        assertNull(q.peekFirst());
    }

    /**
     * peek returns next element, or null if empty
     */
    public void testPeek() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.peek());
            mustEqual(i, q.pollFirst());
            assertTrue(q.peek() == null ||
                       !q.peek().equals(i));
        }
        assertNull(q.peek());
    }

    /**
     * peekLast returns next element, or null if empty
     */
    public void testPeekLast() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
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
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.getFirst());
            mustEqual(i, q.pollFirst());
        }
        try {
            q.getFirst();
            shouldThrow();
        } catch (NoSuchElementException success) {}
        assertNull(q.peekFirst());
    }

    /**
     * getLast() returns last element, or throws NSEE if empty
     */
    public void testLastElement() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
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
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
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
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
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
     * remove removes next element, or throws NSEE if empty
     */
    public void testRemove() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.remove());
        }
        try {
            q.remove();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * removeFirstOccurrence(x) removes x and returns true if present
     */
    public void testRemoveFirstOccurrence() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
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
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
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
     * peekFirst returns element inserted with addFirst
     */
    public void testAddFirst() {
        LinkedBlockingDeque<Item> q = populatedDeque(3);
        q.pollLast();
        q.addFirst(four);
        assertSame(four, q.peekFirst());
    }

    /**
     * peekLast returns element inserted with addLast
     */
    public void testAddLast() {
        LinkedBlockingDeque<Item> q = populatedDeque(3);
        q.pollLast();
        q.addLast(four);
        assertSame(four, q.peekLast());
    }

    /**
     * A new deque has the indicated capacity, or Integer.MAX_VALUE if
     * none given
     */
    public void testConstructor1() {
        mustEqual(SIZE, new LinkedBlockingDeque<Item>(SIZE).remainingCapacity());
        mustEqual(Integer.MAX_VALUE, new LinkedBlockingDeque<Item>().remainingCapacity());
    }

    /**
     * Constructor throws IllegalArgumentException if capacity argument nonpositive
     */
    public void testConstructor2() {
        try {
            new LinkedBlockingDeque<Item>(0);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Initializing from null Collection throws NullPointerException
     */
    public void testConstructor3() {
        try {
            new LinkedBlockingDeque<Item>(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection of null elements throws NullPointerException
     */
    public void testConstructor4() {
        Collection<Item> elements = Arrays.asList(new Item[SIZE]);
        try {
            new LinkedBlockingDeque<Item>(elements);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection with some null elements throws
     * NullPointerException
     */
    public void testConstructor5() {
        Item[] items = new Item[2];
        items[0] = zero;
        Collection<Item> elements = Arrays.asList(items);
        try {
            new LinkedBlockingDeque<Item>(elements);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Deque contains all elements of collection used to initialize
     */
    public void testConstructor6() {
        Item[] items = defaultItems;
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(Arrays.asList(items));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * Deque transitions from empty to full when elements added
     */
    public void testEmptyFull() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(2);
        assertTrue(q.isEmpty());
        mustEqual(2, q.remainingCapacity());
        q.add(one);
        assertFalse(q.isEmpty());
        q.add(two);
        assertFalse(q.isEmpty());
        mustEqual(0, q.remainingCapacity());
        assertFalse(q.offer(three));
    }

    /**
     * remainingCapacity decreases on add, increases on remove
     */
    public void testRemainingCapacity() {
        BlockingQueue<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.remainingCapacity());
            mustEqual(SIZE, q.size() + q.remainingCapacity());
            mustEqual(i, q.remove());
        }
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(SIZE - i, q.remainingCapacity());
            mustEqual(SIZE, q.size() + q.remainingCapacity());
            mustAdd(q, i);
        }
    }

    /**
     * push(null) throws NPE
     */
    public void testPushNull() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(1);
        try {
            q.push(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * push succeeds if not full; throws IllegalStateException if full
     */
    public void testPush() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            Item x = itemFor(i);
            q.push(x);
            mustEqual(x, q.peek());
        }
        mustEqual(0, q.remainingCapacity());
        try {
            q.push(itemFor(SIZE));
            shouldThrow();
        } catch (IllegalStateException success) {}
    }

    /**
     * peekFirst returns element inserted with push
     */
    public void testPushWithPeek() {
        LinkedBlockingDeque<Item> q = populatedDeque(3);
        q.pollLast();
        q.push(four);
        assertSame(four, q.peekFirst());
    }

    /**
     * pop removes next element, or throws NSEE if empty
     */
    public void testPop() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.pop());
        }
        try {
            q.pop();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * Offer succeeds if not full; fails if full
     */
    public void testOffer() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(1);
        assertTrue(q.offer(zero));
        assertFalse(q.offer(one));
    }

    /**
     * add succeeds if not full; throws IllegalStateException if full
     */
    public void testAdd() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE);
        for (int i = 0; i < SIZE; ++i)
            mustAdd(q, i);
        mustEqual(0, q.remainingCapacity());
        try {
            q.add(itemFor(SIZE));
            shouldThrow();
        } catch (IllegalStateException success) {}
    }

    /**
     * addAll(this) throws IllegalArgumentException
     */
    public void testAddAllSelf() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        try {
            q.addAll(q);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * addAll of a collection with any null elements throws NPE after
     * possibly adding some elements
     */
    public void testAddAll3() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE);
        Item[] items = new Item[2]; items[0] = zero;
        Collection<Item> elements = Arrays.asList(items);
        try {
            q.addAll(elements);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * addAll throws IllegalStateException if not enough room
     */
    public void testAddAll4() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE - 1);
        Item[] items = defaultItems;
        Collection<Item> elements = Arrays.asList(items);
        try {
            q.addAll(elements);
            shouldThrow();
        } catch (IllegalStateException success) {}
    }

    /**
     * Deque contains all elements, in traversal order, of successful addAll
     */
    public void testAddAll5() {
        Item[] empty = new Item[0];
        Item[] items = defaultItems;
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE);
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * all elements successfully put are contained
     */
    public void testPut() throws InterruptedException {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            Item x = itemFor(i);
            q.put(x);
            mustContain(q, x);
        }
        mustEqual(0, q.remainingCapacity());
    }

    /**
     * put blocks interruptibly if full
     */
    public void testBlockingPut() throws InterruptedException {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i)
                    q.put(itemFor(i));
                mustEqual(SIZE, q.size());
                mustEqual(0, q.remainingCapacity());

                Thread.currentThread().interrupt();
                try {
                    q.put(ninetynine);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.put(ninetynine);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
        mustEqual(SIZE, q.size());
        mustEqual(0, q.remainingCapacity());
    }

    /**
     * put blocks interruptibly waiting for take when full
     */
    public void testPutWithTake() throws InterruptedException {
        final int capacity = 2;
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(capacity);
        final CountDownLatch pleaseTake = new CountDownLatch(1);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < capacity; i++)
                    q.put(itemFor(i));
                pleaseTake.countDown();
                q.put(eightysix);

                Thread.currentThread().interrupt();
                try {
                    q.put(ninetynine);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.put(ninetynine);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseTake);
        mustEqual(0, q.remainingCapacity());
        mustEqual(0, q.take());

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
        mustEqual(0, q.remainingCapacity());
    }

    /**
     * timed offer times out if full and elements not taken
     */
    public void testTimedOffer() {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(2);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.put(zero);
                q.put(one);
                long startTime = System.nanoTime();

                assertFalse(q.offer(two, timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());

                Thread.currentThread().interrupt();
                try {
                    q.offer(three, randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.offer(four, LONGER_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * take retrieves elements in FIFO order
     */
    public void testTake() throws InterruptedException {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.take());
        }
    }

    /**
     * take removes existing elements until empty, then blocks interruptibly
     */
    public void testBlockingTake() throws InterruptedException {
        final LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; i++) mustEqual(i, q.take());

                Thread.currentThread().interrupt();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * poll succeeds unless empty
     */
    public void testPoll() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * timed poll with zero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll0() throws InterruptedException {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll(0, MILLISECONDS));
        }
        assertNull(q.poll(0, MILLISECONDS));
    }

    /**
     * timed poll with nonzero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll() throws InterruptedException {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            long startTime = System.nanoTime();
            mustEqual(i, q.poll(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
        long startTime = System.nanoTime();
        assertNull(q.poll(timeoutMillis(), MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        checkEmpty(q);
    }

    /**
     * Interrupted timed poll throws InterruptedException instead of
     * returning timeout status
     */
    public void testInterruptedTimedPoll() throws InterruptedException {
        final BlockingQueue<Item> q = populatedDeque(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; i++)
                    mustEqual(i, q.poll(LONG_DELAY_MS, MILLISECONDS));

                Thread.currentThread().interrupt();
                try {
                    q.poll(randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.poll(LONGER_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
        checkEmpty(q);
    }

    /**
     * putFirst(null) throws NPE
     */
    public void testPutFirstNull() throws InterruptedException {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE);
        try {
            q.putFirst(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * all elements successfully putFirst are contained
     */
    public void testPutFirst() throws InterruptedException {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            Item x = itemFor(i);
            q.putFirst(x);
            mustContain(q, x);
        }
        mustEqual(0, q.remainingCapacity());
    }

    /**
     * putFirst blocks interruptibly if full
     */
    public void testBlockingPutFirst() throws InterruptedException {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i)
                    q.putFirst(itemFor(i));
                mustEqual(SIZE, q.size());
                mustEqual(0, q.remainingCapacity());

                Thread.currentThread().interrupt();
                try {
                    q.putFirst(ninetynine);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.putFirst(ninetynine);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
        mustEqual(SIZE, q.size());
        mustEqual(0, q.remainingCapacity());
    }

    /**
     * putFirst blocks interruptibly waiting for take when full
     */
    public void testPutFirstWithTake() throws InterruptedException {
        final int capacity = 2;
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(capacity);
        final CountDownLatch pleaseTake = new CountDownLatch(1);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < capacity; i++)
                    q.putFirst(itemFor(i));
                pleaseTake.countDown();
                q.putFirst(eightysix);

                pleaseInterrupt.countDown();
                try {
                    q.putFirst(ninetynine);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseTake);
        mustEqual(0, q.remainingCapacity());
        mustEqual(capacity - 1, q.take());

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
        mustEqual(0, q.remainingCapacity());
    }

    /**
     * timed offerFirst times out if full and elements not taken
     */
    public void testTimedOfferFirst() {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(2);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.putFirst(zero);
                q.putFirst(one);
                long startTime = System.nanoTime();

                assertFalse(q.offerFirst(two, timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());

                Thread.currentThread().interrupt();
                try {
                    q.offerFirst(three, randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.offerFirst(four, LONGER_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * take retrieves elements in FIFO order
     */
    public void testTakeFirst() throws InterruptedException {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.takeFirst());
        }
    }

    /**
     * takeFirst() blocks interruptibly when empty
     */
    public void testTakeFirstFromEmptyBlocksInterruptibly() {
        final BlockingDeque<Item> q = new LinkedBlockingDeque<>();
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                threadStarted.countDown();
                try {
                    q.takeFirst();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(threadStarted);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * takeFirst() throws InterruptedException immediately if interrupted
     * before waiting
     */
    public void testTakeFirstFromEmptyAfterInterrupt() {
        final BlockingDeque<Item> q = new LinkedBlockingDeque<>();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                Thread.currentThread().interrupt();
                try {
                    q.takeFirst();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        awaitTermination(t);
    }

    /**
     * takeLast() blocks interruptibly when empty
     */
    public void testTakeLastFromEmptyBlocksInterruptibly() {
        final BlockingDeque<Item> q = new LinkedBlockingDeque<>();
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                threadStarted.countDown();
                try {
                    q.takeLast();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(threadStarted);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * takeLast() throws InterruptedException immediately if interrupted
     * before waiting
     */
    public void testTakeLastFromEmptyAfterInterrupt() {
        final BlockingDeque<Item> q = new LinkedBlockingDeque<>();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                Thread.currentThread().interrupt();
                try {
                    q.takeLast();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        awaitTermination(t);
    }

    /**
     * takeFirst removes existing elements until empty, then blocks interruptibly
     */
    public void testBlockingTakeFirst() throws InterruptedException {
        final LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; i++) mustEqual(i, q.takeFirst());

                Thread.currentThread().interrupt();
                try {
                    q.takeFirst();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.takeFirst();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * timed pollFirst with zero timeout succeeds when non-empty, else times out
     */
    public void testTimedPollFirst0() throws InterruptedException {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.pollFirst(0, MILLISECONDS));
        }
        assertNull(q.pollFirst(0, MILLISECONDS));
    }

    /**
     * timed pollFirst with nonzero timeout succeeds when non-empty, else times out
     */
    public void testTimedPollFirst() throws InterruptedException {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            long startTime = System.nanoTime();
            mustEqual(i, q.pollFirst(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
        long startTime = System.nanoTime();
        assertNull(q.pollFirst(timeoutMillis(), MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        checkEmpty(q);
    }

    /**
     * Interrupted timed pollFirst throws InterruptedException instead of
     * returning timeout status
     */
    public void testInterruptedTimedPollFirst() throws InterruptedException {
        final LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; i++)
                    mustEqual(i, q.pollFirst(LONG_DELAY_MS, MILLISECONDS));

                Thread.currentThread().interrupt();
                try {
                    q.pollFirst(randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.pollFirst(LONGER_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * timed pollFirst before a delayed offerFirst fails; after offerFirst succeeds;
     * on interruption throws
     */
    public void testTimedPollFirstWithOfferFirst() throws InterruptedException {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(2);
        final CheckedBarrier barrier = new CheckedBarrier(2);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertNull(q.pollFirst(timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());

                barrier.await();

                assertSame(zero, q.pollFirst(LONG_DELAY_MS, MILLISECONDS));

                Thread.currentThread().interrupt();
                try {
                    q.pollFirst(randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}

                barrier.await();
                try {
                    q.pollFirst(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
            }});

        barrier.await();
        long startTime = System.nanoTime();
        assertTrue(q.offerFirst(zero, LONG_DELAY_MS, MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        barrier.await();
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * putLast(null) throws NPE
     */
    public void testPutLastNull() throws InterruptedException {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE);
        try {
            q.putLast(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * all elements successfully putLast are contained
     */
    public void testPutLast() throws InterruptedException {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            Item x = itemFor(i);
            q.putLast(x);
            mustContain(q, x);
        }
        mustEqual(0, q.remainingCapacity());
    }

    /**
     * putLast blocks interruptibly if full
     */
    public void testBlockingPutLast() throws InterruptedException {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; ++i)
                    q.putLast(itemFor(i));
                mustEqual(SIZE, q.size());
                mustEqual(0, q.remainingCapacity());

                Thread.currentThread().interrupt();
                try {
                    q.putLast(ninetynine);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.putLast(ninetynine);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
        mustEqual(SIZE, q.size());
        mustEqual(0, q.remainingCapacity());
    }

    /**
     * putLast blocks interruptibly waiting for take when full
     */
    public void testPutLastWithTake() throws InterruptedException {
        final int capacity = 2;
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(capacity);
        final CountDownLatch pleaseTake = new CountDownLatch(1);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < capacity; i++)
                    q.putLast(itemFor(i));
                pleaseTake.countDown();
                q.putLast(eightysix);

                Thread.currentThread().interrupt();
                try {
                    q.putLast(ninetynine);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.putLast(ninetynine);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseTake);
        mustEqual(0, q.remainingCapacity());
        mustEqual(0, q.take());

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
        mustEqual(0, q.remainingCapacity());
    }

    /**
     * timed offerLast times out if full and elements not taken
     */
    public void testTimedOfferLast() {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(2);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.putLast(zero);
                q.putLast(one);
                long startTime = System.nanoTime();

                assertFalse(q.offerLast(two, timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());

                Thread.currentThread().interrupt();
                try {
                    q.offerLast(three, randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}

                pleaseInterrupt.countDown();
                try {
                    q.offerLast(four, LONGER_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * takeLast retrieves elements in FIFO order
     */
    public void testTakeLast() throws InterruptedException {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(SIZE - i - 1, q.takeLast());
        }
    }

    /**
     * takeLast removes existing elements until empty, then blocks interruptibly
     */
    public void testBlockingTakeLast() throws InterruptedException {
        final LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; i++)
                    mustEqual(SIZE - i - 1, q.takeLast());

                Thread.currentThread().interrupt();
                try {
                    q.takeLast();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.takeLast();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * timed pollLast with zero timeout succeeds when non-empty, else times out
     */
    public void testTimedPollLast0() throws InterruptedException {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(SIZE - i - 1, q.pollLast(0, MILLISECONDS));
        }
        assertNull(q.pollLast(0, MILLISECONDS));
    }

    /**
     * timed pollLast with nonzero timeout succeeds when non-empty, else times out
     */
    public void testTimedPollLast() throws InterruptedException {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            long startTime = System.nanoTime();
            mustEqual(SIZE - i - 1, q.pollLast(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
        long startTime = System.nanoTime();
        assertNull(q.pollLast(timeoutMillis(), MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        checkEmpty(q);
    }

    /**
     * Interrupted timed pollLast throws InterruptedException instead of
     * returning timeout status
     */
    public void testInterruptedTimedPollLast() throws InterruptedException {
        final LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; i++)
                    mustEqual(SIZE - i - 1,
                                 q.pollLast(LONG_DELAY_MS, MILLISECONDS));

                Thread.currentThread().interrupt();
                try {
                    q.pollLast(randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.pollLast(LONGER_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
        checkEmpty(q);
    }

    /**
     * timed poll before a delayed offerLast fails; after offerLast succeeds;
     * on interruption throws
     */
    public void testTimedPollWithOfferLast() throws InterruptedException {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(2);
        final CheckedBarrier barrier = new CheckedBarrier(2);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertNull(q.poll(timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());

                barrier.await();

                assertSame(zero, q.poll(LONG_DELAY_MS, MILLISECONDS));

                Thread.currentThread().interrupt();
                try {
                    q.poll(randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                barrier.await();
                try {
                    q.poll(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
            }});

        barrier.await();
        long startTime = System.nanoTime();
        assertTrue(q.offerLast(zero, LONG_DELAY_MS, MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);

        barrier.await();
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * element returns next element, or throws NSEE if empty
     */
    public void testElement() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.element());
            q.poll();
        }
        try {
            q.element();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * contains(x) reports true when elements added but not yet removed
     */
    public void testContains() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
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
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        q.clear();
        assertTrue(q.isEmpty());
        mustEqual(0, q.size());
        mustEqual(SIZE, q.remainingCapacity());
        q.add(one);
        assertFalse(q.isEmpty());
        mustContain(q, one);
        q.clear();
        assertTrue(q.isEmpty());
    }

    /**
     * containsAll(c) is true when c contains a subset of elements
     */
    public void testContainsAll() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        LinkedBlockingDeque<Item> p = new LinkedBlockingDeque<>(SIZE);
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
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        LinkedBlockingDeque<Item> p = populatedDeque(SIZE);
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
            LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
            LinkedBlockingDeque<Item> p = populatedDeque(i);
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
    public void testToArray() throws InterruptedException {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
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
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        Item[] items = new Item[SIZE];
        Item[] array = q.toArray(items);
        assertSame(items, array);
        for (Item o : items)
            assertSame(o, q.remove());
        assertTrue(q.isEmpty());
    }

    /**
     * toArray(incompatible array type) throws ArrayStoreException
     */
    @SuppressWarnings("CollectionToArraySafeParameter")
    public void testToArray_incompatibleArrayType() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        try {
            q.toArray(new String[10]);
            shouldThrow();
        } catch (ArrayStoreException success) {}
    }

    /**
     * iterator iterates through all elements
     */
    public void testIterator() throws InterruptedException {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        Iterator<? extends Item> it = q.iterator();
        int i;
        for (i = 0; it.hasNext(); i++)
            mustContain(q, it.next());
        mustEqual(i, SIZE);
        assertIteratorExhausted(it);

        it = q.iterator();
        for (i = 0; it.hasNext(); i++)
            mustEqual(it.next(), q.take());
        mustEqual(i, SIZE);
        assertIteratorExhausted(it);
    }

    /**
     * iterator of empty collection has no elements
     */
    public void testEmptyIterator() {
        Deque<Item> c = new LinkedBlockingDeque<>();
        assertIteratorExhausted(c.iterator());
        assertIteratorExhausted(c.descendingIterator());
    }

    /**
     * iterator.remove removes current element
     */
    public void testIteratorRemove() {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(3);
        q.add(two);
        q.add(one);
        q.add(three);

        Iterator<? extends Item> it = q.iterator();
        it.next();
        it.remove();

        it = q.iterator();
        assertSame(it.next(), one);
        assertSame(it.next(), three);
        assertFalse(it.hasNext());
    }

    /**
     * iterator ordering is FIFO
     */
    public void testIteratorOrdering() {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(3);
        q.add(one);
        q.add(two);
        q.add(three);
        mustEqual(0, q.remainingCapacity());
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
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(3);
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
     * Descending iterator iterates through all elements
     */
    public void testDescendingIterator() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
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
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>();
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
     * descendingIterator.remove removes current element
     */
    public void testDescendingIteratorRemove() {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>();
        for (int iters = 0; iters < 100; ++iters) {
            mustAdd(q, three);
            mustAdd(q, two);
            mustAdd(q, one);
            Iterator<? extends Item> it = q.descendingIterator();
            mustEqual(it.next(), one);
            it.remove();
            mustEqual(it.next(), two);
            it = q.descendingIterator();
            mustEqual(it.next(), two);
            mustEqual(it.next(), three);
            it.remove();
            assertFalse(it.hasNext());
            q.remove();
        }
    }

    /**
     * toString contains toStrings of elements
     */
    public void testToString() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /**
     * offer transfers elements across Executor tasks
     */
    public void testOfferInExecutor() {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(2);
        q.add(one);
        q.add(two);
        final CheckedBarrier threadsStarted = new CheckedBarrier(2);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try (PoolCleaner cleaner = cleaner(executor)) {
            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertFalse(q.offer(three));
                    threadsStarted.await();
                    assertTrue(q.offer(three, LONG_DELAY_MS, MILLISECONDS));
                    mustEqual(0, q.remainingCapacity());
                }});

            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadsStarted.await();
                    assertSame(one, q.take());
                }});
        }
    }

    /**
     * timed poll retrieves elements across Executor threads
     */
    public void testPollInExecutor() {
        final LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>(2);
        final CheckedBarrier threadsStarted = new CheckedBarrier(2);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try (PoolCleaner cleaner = cleaner(executor)) {
            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertNull(q.poll());
                    threadsStarted.await();
                    assertSame(one, q.poll(LONG_DELAY_MS, MILLISECONDS));
                    checkEmpty(q);
                }});

            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadsStarted.await();
                    q.put(one);
                }});
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
        assertTrue(Arrays.equals(x.toArray(), y.toArray()));
        while (!x.isEmpty()) {
            assertFalse(y.isEmpty());
            mustEqual(x.remove(), y.remove());
        }
        assertTrue(y.isEmpty());
    }

    /**
     * drainTo(c) empties deque into another collection c
     */
    public void testDrainTo() {
        LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        ArrayList<Item> l = new ArrayList<>();
        q.drainTo(l);
        mustEqual(0, q.size());
        mustEqual(SIZE, l.size());
        for (int i = 0; i < SIZE; ++i)
            mustEqual(l.get(i), i);
        q.add(zero);
        q.add(one);
        assertFalse(q.isEmpty());
        mustContain(q, zero);
        mustContain(q, one);
        l.clear();
        q.drainTo(l);
        mustEqual(0, q.size());
        mustEqual(2, l.size());
        for (int i = 0; i < 2; ++i)
            mustEqual(l.get(i), i);
    }

    /**
     * drainTo empties full deque, unblocking a waiting put.
     */
    public void testDrainToWithActivePut() throws InterruptedException {
        final LinkedBlockingDeque<Item> q = populatedDeque(SIZE);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.put(new Item(SIZE + 1));
            }});

        t.start();
        ArrayList<Item> l = new ArrayList<>();
        q.drainTo(l);
        assertTrue(l.size() >= SIZE);
        for (int i = 0; i < SIZE; ++i)
            mustEqual(l.get(i), i);
        t.join();
        assertTrue(q.size() + l.size() >= SIZE);
    }

    /**
     * drainTo(c, n) empties first min(n, size) elements of queue into c
     */
    public void testDrainToN() {
        LinkedBlockingDeque<Item> q = new LinkedBlockingDeque<>();
        for (int i = 0; i < SIZE + 2; ++i) {
            for (int j = 0; j < SIZE; j++)
                mustOffer(q, j);
            ArrayList<Item> l = new ArrayList<>();
            q.drainTo(l, i);
            int k = (i < SIZE) ? i : SIZE;
            mustEqual(k, l.size());
            mustEqual(SIZE - k, q.size());
            for (int j = 0; j < k; ++j)
                mustEqual(l.get(j), j);
            do {} while (q.poll() != null);
        }
    }

    /**
     * remove(null), contains(null) always return false
     */
    public void testNeverContainsNull() {
        Deque<?>[] qs = {
            new LinkedBlockingDeque<>(),
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
