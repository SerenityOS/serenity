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
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

import junit.framework.Test;

public class PriorityBlockingQueueTest extends JSR166TestCase {

    public static class Generic extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new PriorityBlockingQueue();
        }
    }

    public static class InitialCapacity extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            int initialCapacity = rnd.nextInt(1, SIZE);
            return new PriorityBlockingQueue(initialCapacity);
        }
    }

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return PriorityBlockingQueue.class; }
            public Collection emptyCollection() {
                return new PriorityBlockingQueue();
            }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return true; }
            public boolean permitsNulls() { return false; }
        }
        class ComparatorImplementation implements CollectionImplementation {
            public Class<?> klazz() { return PriorityBlockingQueue.class; }
            @SuppressWarnings("unchecked")
            public Collection emptyCollection() {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                int initialCapacity = rnd.nextInt(1, 10);
                return new PriorityBlockingQueue(
                    initialCapacity, new MyReverseComparator());
            }
            public Object makeElement(int i) { return JSR166TestCase.itemFor(i); }
            public boolean isConcurrent() { return true; }
            public boolean permitsNulls() { return false; }
        }
        return newTestSuite(
            PriorityBlockingQueueTest.class,
            new Generic().testSuite(),
            new InitialCapacity().testSuite(),
            CollectionTest.testSuite(new Implementation()),
            CollectionTest.testSuite(new ComparatorImplementation()));
    }

    /** Sample Comparator */
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
    private static PriorityBlockingQueue<Item> populatedQueue(int n) {
        PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(n);
        assertTrue(q.isEmpty());
        for (int i = n - 1; i >= 0; i -= 2)
            mustOffer(q, i);
        for (int i = (n & 1); i < n; i += 2)
            mustOffer(q, i);
        assertFalse(q.isEmpty());
        mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
        mustEqual(n, q.size());
        mustEqual(0, q.peek());
        return q;
    }

    /**
     * A new queue has unbounded capacity
     */
    public void testConstructor1() {
        mustEqual(Integer.MAX_VALUE,
                     new PriorityBlockingQueue<Item>(SIZE).remainingCapacity());
    }

    /**
     * Constructor throws IllegalArgumentException if capacity argument nonpositive
     */
    public void testConstructor2() {
        try {
            new PriorityBlockingQueue<Item>(0);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Initializing from null Collection throws NPE
     */
    public void testConstructor3() {
        try {
            new PriorityBlockingQueue<Item>(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection of null elements throws NPE
     */
    public void testConstructor4() {
        Collection<Item> elements = Arrays.asList(new Item[SIZE]);
        try {
            new PriorityBlockingQueue<Item>(elements);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection with some null elements throws NPE
     */
    public void testConstructor5() {
        Item[] items = new Item[2]; items[0] = zero;
        Collection<Item> elements = Arrays.asList(items);
        try {
            new PriorityBlockingQueue<Item>(elements);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Queue contains all elements of collection used to initialize
     */
    public void testConstructor6() {
        Item[] items = defaultItems;
        PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(Arrays.asList(items));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * The comparator used in constructor is used
     */
    public void testConstructor7() {
        MyReverseComparator cmp = new MyReverseComparator();
        @SuppressWarnings("unchecked")
        PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(SIZE, cmp);
        mustEqual(cmp, q.comparator());
        Item[] items = defaultItems;
        q.addAll(Arrays.asList(items));
        for (int i = SIZE - 1; i >= 0; --i)
            mustEqual(items[i], q.poll());
    }

    /**
     * isEmpty is true before add, false after
     */
    public void testEmpty() {
        PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(2);
        assertTrue(q.isEmpty());
        mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
        q.add(one);
        assertFalse(q.isEmpty());
        q.add(two);
        q.remove();
        q.remove();
        assertTrue(q.isEmpty());
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
     * Offer of comparable element succeeds
     */
    public void testOffer() {
        PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(1);
        assertTrue(q.offer(zero));
        assertTrue(q.offer(one));
    }

    /**
     * Offer of non-Comparable throws CCE
     */
    public void testOfferNonComparable() {
        PriorityBlockingQueue<Object> q = new PriorityBlockingQueue<>(1);
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
        PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.size());
            mustAdd(q, i);
        }
    }

    /**
     * addAll(this) throws IllegalArgumentException
     */
    public void testAddAllSelf() {
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
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
        PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(SIZE);
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
        Item[] items = defaultItems;
        PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(SIZE);
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * all elements successfully put are contained
     */
    public void testPut() {
        PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            Item x = itemFor(i);
            q.put(x);
            mustContain(q, x);
        }
        mustEqual(SIZE, q.size());
    }

    /**
     * put doesn't block waiting for take
     */
    public void testPutWithTake() throws InterruptedException {
        final PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(2);
        final int size = 4;
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                for (int i = 0; i < size; i++)
                    q.put(zero);
            }});

        awaitTermination(t);
        mustEqual(size, q.size());
        q.take();
    }

    /**
     * Queue is unbounded, so timed offer never times out
     */
    public void testTimedOffer() {
        final PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(2);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                q.put(one);
                q.put(two);
                assertTrue(q.offer(zero, SHORT_DELAY_MS, MILLISECONDS));
                assertTrue(q.offer(zero, LONG_DELAY_MS, MILLISECONDS));
            }});

        awaitTermination(t);
    }

    /**
     * take retrieves elements in priority order
     */
    public void testTake() throws InterruptedException {
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.take());
        }
    }

    /**
     * Take removes existing elements until empty, then blocks interruptibly
     */
    public void testBlockingTake() throws InterruptedException {
        final PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
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
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * timed poll with zero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll0() throws InterruptedException {
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.poll(0, MILLISECONDS));
        }
        assertNull(q.poll(0, MILLISECONDS));
    }

    /**
     * timed poll with nonzero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll() throws InterruptedException {
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
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
    }

    /**
     * peek returns next element, or null if empty
     */
    public void testPeek() {
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
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
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
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
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.remove());
        }
        try {
            q.remove();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * contains(x) reports true when elements added but not yet removed
     */
    public void testContains() {
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
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
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        q.clear();
        assertTrue(q.isEmpty());
        mustEqual(0, q.size());
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
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        PriorityBlockingQueue<Item> p = new PriorityBlockingQueue<>(SIZE);
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
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        PriorityBlockingQueue<Item> p = populatedQueue(SIZE);
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
            PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
            PriorityBlockingQueue<Item> p = populatedQueue(i);
            assertTrue(q.removeAll(p));
            mustEqual(SIZE - i, q.size());
            for (int j = 0; j < i; ++j) {
                Item x = p.remove();
                mustNotContain(q, x);
            }
        }
    }

    /**
     * toArray contains all elements
     */
    public void testToArray() throws InterruptedException {
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        Object[] a = q.toArray();
        assertSame(Object[].class, a.getClass());
        Arrays.sort(a);
        for (Object o : a)
            assertSame(o, q.take());
        assertTrue(q.isEmpty());
    }

    /**
     * toArray(a) contains all elements
     */
    public void testToArray2() throws InterruptedException {
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        Item[] items = new Item[SIZE];
        Item[] array = q.toArray(items);
        assertSame(items, array);
        Arrays.sort(items);
        for (Item o : items)
            assertSame(o, q.take());
        assertTrue(q.isEmpty());
    }

    /**
     * toArray(incompatible array type) throws ArrayStoreException
     */
    @SuppressWarnings("CollectionToArraySafeParameter")
    public void testToArray_incompatibleArrayType() {
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        try {
            q.toArray(new String[10]);
            shouldThrow();
        } catch (ArrayStoreException success) {}
    }

    /**
     * iterator iterates through all elements
     */
    public void testIterator() {
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
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
        assertIteratorExhausted(new PriorityBlockingQueue<>().iterator());
    }

    /**
     * iterator.remove removes current element
     */
    public void testIteratorRemove() {
        final PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(3);
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
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        String s = q.toString();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(s.contains(String.valueOf(i)));
        }
    }

    /**
     * timed poll transfers elements across Executor tasks
     */
    public void testPollInExecutor() {
        final PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(2);
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

    /**
     * drainTo(c) empties queue into another collection c
     */
    public void testDrainTo() {
        PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        ArrayList<Item> l = new ArrayList<>();
        q.drainTo(l);
        mustEqual(0, q.size());
        mustEqual(SIZE, l.size());
        for (int i = 0; i < SIZE; ++i)
            mustEqual(l.get(i), i);
        q.add(zero);
        q.add(one);
        assertFalse(q.isEmpty());
        mustContain(q, one);
        l.clear();
        q.drainTo(l);
        mustEqual(0, q.size());
        mustEqual(2, l.size());
        for (int i = 0; i < 2; ++i)
            mustEqual(l.get(i), i);
    }

    /**
     * drainTo empties queue
     */
    public void testDrainToWithActivePut() throws InterruptedException {
        final PriorityBlockingQueue<Item> q = populatedQueue(SIZE);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
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
        PriorityBlockingQueue<Item> q = new PriorityBlockingQueue<>(SIZE * 2);
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
            new PriorityBlockingQueue<>(),
            populatedQueue(2),
        };

        for (Collection<?> q : qs) {
            assertFalse(q.contains(null));
            assertFalse(q.remove(null));
        }
    }

}
