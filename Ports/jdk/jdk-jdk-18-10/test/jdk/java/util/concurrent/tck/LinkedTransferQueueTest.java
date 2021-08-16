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
 * Other contributors include John Vint
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedTransferQueue;

import junit.framework.Test;

@SuppressWarnings({"unchecked", "rawtypes"})
public class LinkedTransferQueueTest extends JSR166TestCase {
    public static class Generic extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new LinkedTransferQueue();
        }
    }

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return LinkedTransferQueue.class; }
            public Collection emptyCollection() { return new LinkedTransferQueue(); }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return true; }
            public boolean permitsNulls() { return false; }
        }
        return newTestSuite(LinkedTransferQueueTest.class,
                            new Generic().testSuite(),
                            CollectionTest.testSuite(new Implementation()));
    }

    /**
     * Constructor builds new queue with size being zero and empty
     * being true
     */
    public void testConstructor1() {
        mustEqual(0, new LinkedTransferQueue().size());
        assertTrue(new LinkedTransferQueue().isEmpty());
    }

    /**
     * Initializing constructor with null collection throws
     * NullPointerException
     */
    public void testConstructor2() {
        try {
            new LinkedTransferQueue(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection of null elements throws
     * NullPointerException
     */
    public void testConstructor3() {
        Collection<Item> elements = Arrays.asList(new Item[SIZE]);
        try {
            new LinkedTransferQueue(elements);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing constructor with a collection containing some null elements
     * throws NullPointerException
     */
    public void testConstructor4() {
        Item[] items = new Item[2];
        items[0] = zero;
        Collection<Item> elements = Arrays.asList(items);
        try {
            new LinkedTransferQueue(elements);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Queue contains all elements of the collection it is initialized by
     */
    public void testConstructor5() {
        Item[] items = defaultItems;
        List<Item> intList = Arrays.asList(items);
        LinkedTransferQueue<Item> q = new LinkedTransferQueue<>(intList);
        mustEqual(q.size(), intList.size());
        mustEqual(q.toString(), intList.toString());
        assertTrue(Arrays.equals(q.toArray(),
                                     intList.toArray()));
        assertTrue(Arrays.equals(q.toArray(new Object[0]),
                                 intList.toArray(new Object[0])));
        assertTrue(Arrays.equals(q.toArray(new Object[SIZE]),
                                 intList.toArray(new Object[SIZE])));
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(items[i], q.poll());
        }
    }

    /**
     * remainingCapacity() always returns Integer.MAX_VALUE
     */
    public void testRemainingCapacity() {
        BlockingQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
            mustEqual(SIZE - i, q.size());
            mustEqual(i, q.remove());
        }
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
            mustEqual(i, q.size());
            mustAdd(q, i);
        }
    }

    /**
     * addAll(this) throws IllegalArgumentException
     */
    public void testAddAllSelf() {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        try {
            q.addAll(q);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * addAll of a collection with any null elements throws
     * NullPointerException after possibly adding some elements
     */
    public void testAddAll3() {
        LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        Item[] items = new Item[2]; items[0] = zero;
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
        LinkedTransferQueue q = new LinkedTransferQueue();
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(items[i], q.poll());
        }
    }

    /**
     * all elements successfully put are contained
     */
    public void testPut() {
        LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        Item[] items = defaultItems;
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.size());
            q.put(items[i]);
            mustContain(q, items[i]);
        }
    }

    /**
     * take retrieves elements in FIFO order
     */
    public void testTake() throws InterruptedException {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.take());
        }
    }

    /**
     * take removes existing elements until empty, then blocks interruptibly
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
    public void testPoll() throws InterruptedException {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll());
        }
        assertNull(q.poll());
        checkEmpty(q);
    }

    /**
     * timed poll with zero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll0() throws InterruptedException {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll(0, MILLISECONDS));
        }
        assertNull(q.poll(0, MILLISECONDS));
        checkEmpty(q);
    }

    /**
     * timed poll with nonzero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll() throws InterruptedException {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        long startTime = System.nanoTime();
        for (int i = 0; i < SIZE; ++i)
            mustEqual(i, q.poll(LONG_DELAY_MS, MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);

        startTime = System.nanoTime();
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
     * timed poll after thread interrupted throws InterruptedException
     * instead of returning timeout status
     */
    public void testTimedPollAfterInterrupt() throws InterruptedException {
        final BlockingQueue<Item> q = populatedQueue(SIZE);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                Thread.currentThread().interrupt();
                for (int i = 0; i < SIZE; ++i)
                    mustEqual(i, q.poll(randomTimeout(), randomTimeUnit()));
                try {
                    q.poll(randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        awaitTermination(t);
        checkEmpty(q);
    }

    /**
     * peek returns next element, or null if empty
     */
    public void testPeek() throws InterruptedException {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.peek());
            mustEqual(i, q.poll());
            assertTrue(q.peek() == null ||
                       i != q.peek().value);
        }
        assertNull(q.peek());
        checkEmpty(q);
    }

    /**
     * element returns next element, or throws NoSuchElementException if empty
     */
    public void testElement() throws InterruptedException {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.element());
            mustEqual(i, q.poll());
        }
        try {
            q.element();
            shouldThrow();
        } catch (NoSuchElementException success) {}
        checkEmpty(q);
    }

    /**
     * remove removes next element, or throws NoSuchElementException if empty
     */
    public void testRemove() throws InterruptedException {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.remove());
        }
        try {
            q.remove();
            shouldThrow();
        } catch (NoSuchElementException success) {}
        checkEmpty(q);
    }

    /**
     * An add following remove(x) succeeds
     */
    public void testRemoveElementAndAdd() throws InterruptedException {
        LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        mustAdd(q, one);
        mustAdd(q, two);
        mustRemove(q, one);
        mustRemove(q, two);
        mustAdd(q, three);
        mustEqual(q.take(), three);
    }

    /**
     * contains(x) reports true when elements added but not yet removed
     */
    public void testContains() {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustContain(q, i);
            mustEqual(i, q.poll());
            mustNotContain(q, i);
        }
    }

    /**
     * clear removes all elements
     */
    public void testClear() throws InterruptedException {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        q.clear();
        checkEmpty(q);
        mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
        q.add(one);
        assertFalse(q.isEmpty());
        mustEqual(1, q.size());
        mustContain(q, one);
        q.clear();
        checkEmpty(q);
    }

    /**
     * containsAll(c) is true when c contains a subset of elements
     */
    public void testContainsAll() {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        LinkedTransferQueue<Item> p = new LinkedTransferQueue<>();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(q.containsAll(p));
            assertFalse(p.containsAll(q));
            mustAdd(p, i);
        }
        assertTrue(p.containsAll(q));
    }

    /**
     * retainAll(c) retains only those elements of c and reports true
     * if changed
     */
    public void testRetainAll() {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        LinkedTransferQueue<Item> p = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            boolean changed = q.retainAll(p);
            if (i == 0) {
                assertFalse(changed);
            } else {
                assertTrue(changed);
            }
            assertTrue(q.containsAll(p));
            mustEqual(SIZE - i, q.size());
            p.remove();
        }
    }

    /**
     * removeAll(c) removes only those elements of c and reports true
     * if changed
     */
    public void testRemoveAll() {
        for (int i = 1; i < SIZE; ++i) {
            LinkedTransferQueue<Item> q = populatedQueue(SIZE);
            LinkedTransferQueue<Item> p = populatedQueue(i);
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
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
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
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
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
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        try {
            q.toArray(new String[10]);
            shouldThrow();
        } catch (ArrayStoreException success) {}
    }

    /**
     * iterator iterates through all elements
     */
    public void testIterator() throws InterruptedException {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
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
        assertIteratorExhausted(new LinkedTransferQueue().iterator());
    }

    /**
     * iterator.remove() removes current element
     */
    public void testIteratorRemove() {
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
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
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
        q.add(one);
        q.add(two);
        q.add(three);
        mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
        int k = 0;
        for (Item n : q) {
            mustEqual(++k, n);
        }
        mustEqual(3, k);
    }

    /**
     * Modifications do not cause iterators to fail
     */
    public void testWeaklyConsistentIteration() {
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
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
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /**
     * offer transfers elements across Executor tasks
     */
    public void testOfferInExecutor() {
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        final CheckedBarrier threadsStarted = new CheckedBarrier(2);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try (PoolCleaner cleaner = cleaner(executor)) {

            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadsStarted.await();
                    long startTime = System.nanoTime();
                    assertTrue(q.offer(one, LONG_DELAY_MS, MILLISECONDS));
                    assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
                }});

            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadsStarted.await();
                    assertSame(one, q.take());
                    checkEmpty(q);
                }});
        }
    }

    /**
     * timed poll retrieves elements across Executor threads
     */
    public void testPollInExecutor() {
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        final CheckedBarrier threadsStarted = new CheckedBarrier(2);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try (PoolCleaner cleaner = cleaner(executor)) {

            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertNull(q.poll());
                    threadsStarted.await();
                    long startTime = System.nanoTime();
                    assertSame(one, q.poll(LONG_DELAY_MS, MILLISECONDS));
                    assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
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
     * drainTo(c) empties queue into another collection c
     */
    public void testDrainTo() {
        LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        ArrayList l = new ArrayList();
        q.drainTo(l);
        mustEqual(0, q.size());
        mustEqual(SIZE, l.size());
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, l.get(i));
        }
        q.add(zero);
        q.add(one);
        assertFalse(q.isEmpty());
        mustContain(q, zero);
        mustContain(q, one);
        l.clear();
        q.drainTo(l);
        mustEqual(0, q.size());
        mustEqual(2, l.size());
        for (int i = 0; i < 2; ++i) {
            mustEqual(i, l.get(i));
        }
    }

    /**
     * drainTo(c) empties full queue, unblocking a waiting put.
     */
    public void testDrainToWithActivePut() throws InterruptedException {
        final LinkedTransferQueue<Item> q = populatedQueue(SIZE);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                q.put(new Item(SIZE + 1));
            }});
        ArrayList l = new ArrayList();
        q.drainTo(l);
        assertTrue(l.size() >= SIZE);
        for (int i = 0; i < SIZE; ++i)
            mustEqual(i, l.get(i));
        awaitTermination(t);
        assertTrue(q.size() + l.size() >= SIZE);
    }

    /**
     * drainTo(c, n) empties first min(n, size) elements of queue into c
     */
    public void testDrainToN() {
        LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        for (int i = 0; i < SIZE + 2; ++i) {
            for (int j = 0; j < SIZE; j++) {
                mustOffer(q, j);
            }
            ArrayList l = new ArrayList();
            q.drainTo(l, i);
            int k = (i < SIZE) ? i : SIZE;
            mustEqual(k, l.size());
            mustEqual(SIZE - k, q.size());
            for (int j = 0; j < k; ++j)
                mustEqual(j, l.get(j));
            do {} while (q.poll() != null);
        }
    }

    /**
     * timed poll() or take() increments the waiting consumer count;
     * offer(e) decrements the waiting consumer count
     */
    public void testWaitingConsumer() throws InterruptedException {
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        mustEqual(0, q.getWaitingConsumerCount());
        assertFalse(q.hasWaitingConsumer());
        final CountDownLatch threadStarted = new CountDownLatch(1);

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                threadStarted.countDown();
                long startTime = System.nanoTime();
                assertSame(one, q.poll(LONG_DELAY_MS, MILLISECONDS));
                mustEqual(0, q.getWaitingConsumerCount());
                assertFalse(q.hasWaitingConsumer());
                assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
            }});

        threadStarted.await();
        Callable<Boolean> oneConsumer = new Callable<>() {
            public Boolean call() {
                return q.hasWaitingConsumer()
                && q.getWaitingConsumerCount() == 1; }};
        waitForThreadToEnterWaitState(t, oneConsumer);

        assertTrue(q.offer(one));
        mustEqual(0, q.getWaitingConsumerCount());
        assertFalse(q.hasWaitingConsumer());

        awaitTermination(t);
    }

    /**
     * transfer(null) throws NullPointerException
     */
    public void testTransfer1() throws InterruptedException {
        try {
            LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
            q.transfer(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * transfer waits until a poll occurs. The transferred element
     * is returned by the associated poll.
     */
    public void testTransfer2() throws InterruptedException {
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        final CountDownLatch threadStarted = new CountDownLatch(1);

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                threadStarted.countDown();
                q.transfer(five);
                checkEmpty(q);
            }});

        threadStarted.await();
        Callable<Boolean> oneElement = new Callable<>() {
            public Boolean call() {
                return !q.isEmpty() && q.size() == 1; }};
        waitForThreadToEnterWaitState(t, oneElement);

        assertSame(five, q.poll());
        checkEmpty(q);
        awaitTermination(t);
    }

    /**
     * transfer waits until a poll occurs, and then transfers in fifo order
     */
    public void testTransfer3() throws InterruptedException {
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();

        Thread first = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.transfer(four);
                mustNotContain(q, four);
                mustEqual(1, q.size());
            }});

        Thread interruptedThread = newStartedThread(
            new CheckedInterruptedRunnable() {
                public void realRun() throws InterruptedException {
                    while (q.isEmpty())
                        Thread.yield();
                    q.transfer(five);
                }});

        while (q.size() < 2)
            Thread.yield();
        mustEqual(2, q.size());
        assertSame(four, q.poll());
        first.join();
        mustEqual(1, q.size());
        interruptedThread.interrupt();
        interruptedThread.join();
        checkEmpty(q);
    }

    /**
     * transfer waits until a poll occurs, at which point the polling
     * thread returns the element
     */
    public void testTransfer4() throws InterruptedException {
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.transfer(four);
                mustNotContain(q, four);
                assertSame(three, q.poll());
            }});

        while (q.isEmpty())
            Thread.yield();
        assertFalse(q.isEmpty());
        mustEqual(1, q.size());
        assertTrue(q.offer(three));
        assertSame(four, q.poll());
        awaitTermination(t);
    }

    /**
     * transfer waits until a take occurs. The transferred element
     * is returned by the associated take.
     */
    public void testTransfer5() throws InterruptedException {
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.transfer(four);
                checkEmpty(q);
            }});

        while (q.isEmpty())
            Thread.yield();
        assertFalse(q.isEmpty());
        mustEqual(1, q.size());
        assertSame(four, q.take());
        checkEmpty(q);
        awaitTermination(t);
    }

    /**
     * tryTransfer(null) throws NullPointerException
     */
    public void testTryTransfer1() {
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        try {
            q.tryTransfer(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * tryTransfer returns false and does not enqueue if there are no
     * consumers waiting to poll or take.
     */
    public void testTryTransfer2() throws InterruptedException {
        final LinkedTransferQueue<Object> q = new LinkedTransferQueue<>();
        assertFalse(q.tryTransfer(new Object()));
        assertFalse(q.hasWaitingConsumer());
        checkEmpty(q);
    }

    /**
     * If there is a consumer waiting in timed poll, tryTransfer
     * returns true while successfully transfering object.
     */
    public void testTryTransfer3() throws InterruptedException {
        final Object hotPotato = new Object();
        final LinkedTransferQueue<Object> q = new LinkedTransferQueue<>();

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                while (! q.hasWaitingConsumer())
                    Thread.yield();
                assertTrue(q.hasWaitingConsumer());
                checkEmpty(q);
                assertTrue(q.tryTransfer(hotPotato));
            }});

        long startTime = System.nanoTime();
        assertSame(hotPotato, q.poll(LONG_DELAY_MS, MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        checkEmpty(q);
        awaitTermination(t);
    }

    /**
     * If there is a consumer waiting in take, tryTransfer returns
     * true while successfully transfering object.
     */
    public void testTryTransfer4() throws InterruptedException {
        final Object hotPotato = new Object();
        final LinkedTransferQueue<Object> q = new LinkedTransferQueue<>();

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                while (! q.hasWaitingConsumer())
                    Thread.yield();
                assertTrue(q.hasWaitingConsumer());
                checkEmpty(q);
                assertTrue(q.tryTransfer(hotPotato));
            }});

        assertSame(q.take(), hotPotato);
        checkEmpty(q);
        awaitTermination(t);
    }

    /**
     * tryTransfer blocks interruptibly if no takers
     */
    public void testTryTransfer5() throws InterruptedException {
        final LinkedTransferQueue<Object> q = new LinkedTransferQueue<>();
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        assertTrue(q.isEmpty());

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                Thread.currentThread().interrupt();
                try {
                    q.tryTransfer(new Object(), randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.tryTransfer(new Object(), LONGER_DELAY_MS, MILLISECONDS);
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
     * tryTransfer gives up after the timeout and returns false
     */
    public void testTryTransfer6() throws InterruptedException {
        final LinkedTransferQueue<Object> q = new LinkedTransferQueue<>();

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertFalse(q.tryTransfer(new Object(),
                                          timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
                checkEmpty(q);
            }});

        awaitTermination(t);
        checkEmpty(q);
    }

    /**
     * tryTransfer waits for any elements previously in to be removed
     * before transfering to a poll or take
     */
    public void testTryTransfer7() throws InterruptedException {
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        assertTrue(q.offer(four));

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertTrue(q.tryTransfer(five, LONG_DELAY_MS, MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
                checkEmpty(q);
            }});

        while (q.size() != 2)
            Thread.yield();
        mustEqual(2, q.size());
        assertSame(four, q.poll());
        assertSame(five, q.poll());
        checkEmpty(q);
        awaitTermination(t);
    }

    /**
     * tryTransfer attempts to enqueue into the queue and fails
     * returning false not enqueueing and the successive poll is null
     */
    public void testTryTransfer8() throws InterruptedException {
        final LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        assertTrue(q.offer(four));
        mustEqual(1, q.size());
        long startTime = System.nanoTime();
        assertFalse(q.tryTransfer(five, timeoutMillis(), MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        mustEqual(1, q.size());
        assertSame(four, q.poll());
        assertNull(q.poll());
        checkEmpty(q);
    }

    private LinkedTransferQueue<Item> populatedQueue(int n) {
        LinkedTransferQueue<Item> q = new LinkedTransferQueue<>();
        checkEmpty(q);
        for (int i = 0; i < n; i++) {
            mustEqual(i, q.size());
            mustOffer(q, i);
            mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
        }
        assertFalse(q.isEmpty());
        return q;
    }

    /**
     * remove(null), contains(null) always return false
     */
    public void testNeverContainsNull() {
        Collection<?>[] qs = {
            new LinkedTransferQueue<>(),
            populatedQueue(2),
        };

        for (Collection<?> q : qs) {
            assertFalse(q.contains(null));
            assertFalse(q.remove(null));
        }
    }
}
