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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.Test;

public class LinkedBlockingQueueTest extends JSR166TestCase {

    public static class Unbounded extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new LinkedBlockingQueue();
        }
    }

    public static class Bounded extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new LinkedBlockingQueue(SIZE);
        }
    }

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return LinkedBlockingQueue.class; }
            public Collection emptyCollection() { return new LinkedBlockingQueue(); }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return true; }
            public boolean permitsNulls() { return false; }
        }
        return newTestSuite(LinkedBlockingQueueTest.class,
                            new Unbounded().testSuite(),
                            new Bounded().testSuite(),
                            CollectionTest.testSuite(new Implementation()));
    }

    /**
     * Returns a new queue of given size containing consecutive
     * Items 0 ... n - 1.
     */
    private static LinkedBlockingQueue<Item> populatedQueue(int n) {
        LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(n);
        assertTrue(q.isEmpty());
        for (int i = 0; i < n; i++)
            mustOffer(q, i);
        assertFalse(q.isEmpty());
        mustEqual(0, q.remainingCapacity());
        mustEqual(n, q.size());
        mustEqual(0, q.peek());
        return q;
    }

    /**
     * A new queue has the indicated capacity, or Integer.MAX_VALUE if
     * none given
     */
    public void testConstructor1() {
        mustEqual(SIZE, new LinkedBlockingQueue<Item>(SIZE).remainingCapacity());
        mustEqual(Integer.MAX_VALUE, new LinkedBlockingQueue<Item>().remainingCapacity());
    }

    /**
     * Constructor throws IllegalArgumentException if capacity argument nonpositive
     */
    public void testConstructor2() {
        try {
            new LinkedBlockingQueue<Item>(0);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Initializing from null Collection throws NullPointerException
     */
    public void testConstructor3() {
        try {
            new LinkedBlockingQueue<Item>(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection of null elements throws NullPointerException
     */
    public void testConstructor4() {
        Collection<Item> elements = Arrays.asList(new Item[SIZE]);
        try {
            new LinkedBlockingQueue<Item>(elements);
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
            new LinkedBlockingQueue<Item>(elements);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Queue contains all elements of collection used to initialize
     */
    public void testConstructor6() {
        Item[] items = defaultItems;
        LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(Arrays.asList(items));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * Queue transitions from empty to full when elements added
     */
    public void testEmptyFull() {
        LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(2);
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
        BlockingQueue<Item> q = populatedQueue(SIZE);
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
     * Offer succeeds if not full; fails if full
     */
    public void testOffer() {
        LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(1);
        assertTrue(q.offer(zero));
        assertFalse(q.offer(one));
    }

    /**
     * add succeeds if not full; throws IllegalStateException if full
     */
    public void testAdd() {
        LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(SIZE);
        for (int i = 0; i < SIZE; ++i)
            mustAdd(q, i);
        mustEqual(0, q.remainingCapacity());
        try {
            q.add(new Item(SIZE));
            shouldThrow();
        } catch (IllegalStateException success) {}
    }

    /**
     * addAll(this) throws IllegalArgumentException
     */
    public void testAddAllSelf() {
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
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
        LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(SIZE);
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
        LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(SIZE - 1);
        Item[] items = defaultItems;
        Collection<Item> elements = Arrays.asList(items);
        try {
            q.addAll(elements);
            shouldThrow();
        } catch (IllegalStateException success) {}
    }

    /**
     * Queue contains all elements, in traversal order, of successful addAll
     */
    public void testAddAll5() {
        Item[] empty = new Item[0];
        Item[] items = defaultItems;
        LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(SIZE);
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * all elements successfully put are contained
     */
    public void testPut() throws InterruptedException {
        LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(SIZE);
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
        final LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(SIZE);
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
        final LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(2);
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
        final LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(2);
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
                    q.offer(two, LONGER_DELAY_MS, MILLISECONDS);
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
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.take());
        }
    }

    /**
     * Take removes existing elements until empty, then blocks interruptibly
     */
    public void testBlockingTake() throws InterruptedException {
        final BlockingQueue<Item> q = populatedQueue(SIZE);
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
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * timed poll with zero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll0() throws InterruptedException {
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll(0, MILLISECONDS));
        }
        assertNull(q.poll(0, MILLISECONDS));
    }

    /**
     * timed poll with nonzero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll() throws InterruptedException {
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
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
        final BlockingQueue<Item> q = populatedQueue(SIZE);
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
     * peek returns next element, or null if empty
     */
    public void testPeek() {
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
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
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
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
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.remove());
        }
        try {
            q.remove();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * An add following remove(x) succeeds
     */
    public void testRemoveElementAndAdd() throws InterruptedException {
        LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>();
        assertTrue(q.add(one));
        assertTrue(q.add(two));
        mustRemove(q, one);
        mustRemove(q, two);
        mustAdd(q, three);
        assertNotNull(q.take());
    }

    /**
     * contains(x) reports true when elements added but not yet removed
     */
    public void testContains() {
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
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
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
        q.clear();
        assertTrue(q.isEmpty());
        mustEqual(0, q.size());
        mustEqual(SIZE, q.remainingCapacity());
        q.add(one);
        assertFalse(q.isEmpty());
        assertTrue(q.contains(one));
        q.clear();
        assertTrue(q.isEmpty());
    }

    /**
     * containsAll(c) is true when c contains a subset of elements
     */
    public void testContainsAll() {
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
        LinkedBlockingQueue<Item> p = new LinkedBlockingQueue<>(SIZE);
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
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
        LinkedBlockingQueue<Item> p = populatedQueue(SIZE);
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
            LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
            LinkedBlockingQueue<Item> p = populatedQueue(i);
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
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
        Object[] a = q.toArray();
        assertSame(Object[].class, a.getClass());
        for (Object o : a)
            assertSame(o, q.poll());
        assertTrue(q.isEmpty());
    }

    /**
     * toArray(a) contains all elements in FIFO order
     */
    public void testToArray2() throws InterruptedException {
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
        Item[] items = new Item[SIZE];
        Item[] array = q.toArray(items);
        assertSame(items, array);
        for (Item o : items)
            assertSame(o, q.poll());
        assertTrue(q.isEmpty());
    }

    /**
     * toArray(incompatible array type) throws ArrayStoreException
     */
    @SuppressWarnings("CollectionToArraySafeParameter")
    public void testToArray_incompatibleArrayType() {
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
        try {
            q.toArray(new String[10]);
            shouldThrow();
        } catch (ArrayStoreException success) {}
    }

    /**
     * iterator iterates through all elements
     */
    public void testIterator() throws InterruptedException {
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
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
        assertIteratorExhausted(new LinkedBlockingQueue<Item>().iterator());
    }

    /**
     * iterator.remove removes current element
     */
    public void testIteratorRemove() {
        final LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(3);
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
        final LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(3);
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
        final LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(3);
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
     * toString contains toStrings of elements
     */
    public void testToString() {
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /**
     * offer transfers elements across Executor tasks
     */
    public void testOfferInExecutor() {
        final LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(2);
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
        final LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>(2);
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
     * drainTo(c) empties queue into another collection c
     */
    public void testDrainTo() {
        LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
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
     * drainTo empties full queue, unblocking a waiting put.
     */
    public void testDrainToWithActivePut() throws InterruptedException {
        final LinkedBlockingQueue<Item> q = populatedQueue(SIZE);
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
        LinkedBlockingQueue<Item> q = new LinkedBlockingQueue<>();
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
        Collection<?>[] qs = {
            new LinkedBlockingQueue<>(),
            populatedQueue(2),
        };

        for (Collection<?> q : qs) {
            assertFalse(q.contains(null));
            assertFalse(q.remove(null));
        }
    }

}
