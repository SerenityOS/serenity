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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

public class DelayQueueTest extends JSR166TestCase {

    public static class Generic extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new DelayQueue();
        }
        protected PDelay makeElement(int i) {
            return new PDelay(i);
        }
    }

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return DelayQueue.class; }
            public Collection emptyCollection() { return new DelayQueue(); }
            public Object makeElement(int i) { return new PDelay(i); }
            public boolean isConcurrent() { return true; }
            public boolean permitsNulls() { return false; }
        }
        return newTestSuite(DelayQueueTest.class,
                            new Generic().testSuite(),
                            CollectionTest.testSuite(new Implementation()));
    }

    /**
     * A fake Delayed implementation for testing.
     * Most tests use PDelays, where delays are all elapsed
     * (so, no blocking solely for delays) but are still ordered
     */
    static class PDelay implements Delayed {
        final int pseudodelay;
        PDelay(int pseudodelay) { this.pseudodelay = pseudodelay; }
        public int compareTo(Delayed y) {
            return Integer.compare(this.pseudodelay, ((PDelay)y).pseudodelay);
        }
        public boolean equals(Object other) {
            return (other instanceof PDelay) &&
                this.pseudodelay == ((PDelay)other).pseudodelay;
        }
        // suppress [overrides] javac warning
        public int hashCode() { return pseudodelay; }
        public long getDelay(TimeUnit ignore) {
            return (long) Integer.MIN_VALUE + pseudodelay;
        }
        public String toString() {
            return String.valueOf(pseudodelay);
        }
    }

    /**
     * Delayed implementation that actually delays
     */
    static class NanoDelay implements Delayed {
        final long trigger;
        NanoDelay(long i) {
            trigger = System.nanoTime() + i;
        }

        public int compareTo(Delayed y) {
            return Long.compare(trigger, ((NanoDelay)y).trigger);
        }

        public boolean equals(Object other) {
            return (other instanceof NanoDelay) &&
                this.trigger == ((NanoDelay)other).trigger;
        }

        // suppress [overrides] javac warning
        public int hashCode() { return (int) trigger; }

        public long getDelay(TimeUnit unit) {
            long n = trigger - System.nanoTime();
            return unit.convert(n, TimeUnit.NANOSECONDS);
        }

        public long getTriggerTime() {
            return trigger;
        }

        public String toString() {
            return String.valueOf(trigger);
        }
    }

    /**
     * Returns a new queue of given size containing consecutive
     * PDelays 0 ... n - 1.
     */
    private static DelayQueue<PDelay> populatedQueue(int n) {
        DelayQueue<PDelay> q = new DelayQueue<>();
        assertTrue(q.isEmpty());
        for (int i = n - 1; i >= 0; i -= 2)
            assertTrue(q.offer(new PDelay(i)));
        for (int i = (n & 1); i < n; i += 2)
            assertTrue(q.offer(new PDelay(i)));
        assertFalse(q.isEmpty());
        mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
        mustEqual(n, q.size());
        mustEqual(new PDelay(0), q.peek());
        return q;
    }

    /**
     * A new queue has unbounded capacity
     */
    public void testConstructor1() {
        mustEqual(Integer.MAX_VALUE, new DelayQueue<PDelay>().remainingCapacity());
    }

    /**
     * Initializing from null Collection throws NPE
     */
    public void testConstructor3() {
        try {
            new DelayQueue<PDelay>(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection of null elements throws NPE
     */
    public void testConstructor4() {
        try {
            new DelayQueue<PDelay>(Arrays.asList(new PDelay[SIZE]));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Initializing from Collection with some null elements throws NPE
     */
    public void testConstructor5() {
        PDelay[] a = new PDelay[SIZE];
        for (int i = 0; i < SIZE - 1; ++i)
            a[i] = new PDelay(i);
        try {
            new DelayQueue<PDelay>(Arrays.asList(a));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Queue contains all elements of collection used to initialize
     */
    public void testConstructor6() {
        PDelay[] items = new PDelay[SIZE];
        for (int i = 0; i < SIZE; ++i)
            items[i] = new PDelay(i);
        DelayQueue<PDelay> q = new DelayQueue<>(Arrays.asList(items));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * isEmpty is true before add, false after
     */
    public void testEmpty() {
        DelayQueue<PDelay> q = new DelayQueue<>();
        assertTrue(q.isEmpty());
        mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
        q.add(new PDelay(1));
        assertFalse(q.isEmpty());
        q.add(new PDelay(2));
        q.remove();
        q.remove();
        assertTrue(q.isEmpty());
    }

    /**
     * remainingCapacity() always returns Integer.MAX_VALUE
     */
    public void testRemainingCapacity() {
        BlockingQueue<PDelay> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
            mustEqual(SIZE - i, q.size());
            assertTrue(q.remove() instanceof PDelay);
        }
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
            mustEqual(i, q.size());
            assertTrue(q.add(new PDelay(i)));
        }
    }

    /**
     * offer non-null succeeds
     */
    public void testOffer() {
        DelayQueue<PDelay> q = new DelayQueue<>();
        assertTrue(q.offer(new PDelay(0)));
        assertTrue(q.offer(new PDelay(1)));
    }

    /**
     * add succeeds
     */
    public void testAdd() {
        DelayQueue<PDelay> q = new DelayQueue<>();
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(i, q.size());
            assertTrue(q.add(new PDelay(i)));
        }
    }

    /**
     * addAll(this) throws IllegalArgumentException
     */
    public void testAddAllSelf() {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
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
        DelayQueue<PDelay> q = new DelayQueue<>();
        PDelay[] a = new PDelay[SIZE];
        for (int i = 0; i < SIZE - 1; ++i)
            a[i] = new PDelay(i);
        try {
            q.addAll(Arrays.asList(a));
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Queue contains all elements of successful addAll
     */
    public void testAddAll5() {
        PDelay[] empty = new PDelay[0];
        PDelay[] items = new PDelay[SIZE];
        for (int i = SIZE - 1; i >= 0; --i)
            items[i] = new PDelay(i);
        DelayQueue<PDelay> q = new DelayQueue<>();
        assertFalse(q.addAll(Arrays.asList(empty)));
        assertTrue(q.addAll(Arrays.asList(items)));
        for (int i = 0; i < SIZE; ++i)
            mustEqual(items[i], q.poll());
    }

    /**
     * all elements successfully put are contained
     */
    public void testPut() {
        DelayQueue<PDelay> q = new DelayQueue<>();
        for (int i = 0; i < SIZE; ++i) {
            PDelay x = new PDelay(i);
            q.put(x);
            assertTrue(q.contains(x));
        }
        mustEqual(SIZE, q.size());
    }

    /**
     * put doesn't block waiting for take
     */
    public void testPutWithTake() throws InterruptedException {
        final DelayQueue<PDelay> q = new DelayQueue<>();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                q.put(new PDelay(0));
                q.put(new PDelay(0));
                q.put(new PDelay(0));
                q.put(new PDelay(0));
            }});

        awaitTermination(t);
        mustEqual(4, q.size());
    }

    /**
     * Queue is unbounded, so timed offer never times out
     */
    public void testTimedOffer() throws InterruptedException {
        final DelayQueue<PDelay> q = new DelayQueue<>();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.put(new PDelay(0));
                q.put(new PDelay(0));
                assertTrue(q.offer(new PDelay(0), SHORT_DELAY_MS, MILLISECONDS));
                assertTrue(q.offer(new PDelay(0), LONG_DELAY_MS, MILLISECONDS));
            }});

        awaitTermination(t);
    }

    /**
     * take retrieves elements in priority order
     */
    public void testTake() throws InterruptedException {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(new PDelay(i), q.take());
        }
    }

    /**
     * Take removes existing elements until empty, then blocks interruptibly
     */
    public void testBlockingTake() throws InterruptedException {
        final DelayQueue<PDelay> q = populatedQueue(SIZE);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; i++)
                    mustEqual(new PDelay(i), q.take());

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
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(new PDelay(i), q.poll());
        }
        assertNull(q.poll());
    }

    /**
     * timed poll with zero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll0() throws InterruptedException {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(new PDelay(i), q.poll(0, MILLISECONDS));
        }
        assertNull(q.poll(0, MILLISECONDS));
    }

    /**
     * timed poll with nonzero timeout succeeds when non-empty, else times out
     */
    public void testTimedPoll() throws InterruptedException {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            long startTime = System.nanoTime();
            mustEqual(new PDelay(i), q.poll(LONG_DELAY_MS, MILLISECONDS));
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
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        final DelayQueue<PDelay> q = populatedQueue(SIZE);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < SIZE; i++)
                    mustEqual(new PDelay(i),
                              q.poll(LONG_DELAY_MS, MILLISECONDS));

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
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(new PDelay(i), q.peek());
            mustEqual(new PDelay(i), q.poll());
            if (q.isEmpty())
                assertNull(q.peek());
            else
                assertFalse(new PDelay(i).equals(q.peek()));
        }
        assertNull(q.peek());
    }

    /**
     * element returns next element, or throws NSEE if empty
     */
    public void testElement() {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(new PDelay(i), q.element());
            q.poll();
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
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            mustEqual(new PDelay(i), q.remove());
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
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(q.contains(new PDelay(i)));
            q.poll();
            assertFalse(q.contains(new PDelay(i)));
        }
    }

    /**
     * clear removes all elements
     */
    public void testClear() {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        q.clear();
        assertTrue(q.isEmpty());
        mustEqual(0, q.size());
        mustEqual(Integer.MAX_VALUE, q.remainingCapacity());
        PDelay x = new PDelay(1);
        q.add(x);
        assertFalse(q.isEmpty());
        assertTrue(q.contains(x));
        q.clear();
        assertTrue(q.isEmpty());
    }

    /**
     * containsAll(c) is true when c contains a subset of elements
     */
    public void testContainsAll() {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        DelayQueue<PDelay> p = new DelayQueue<>();
        for (int i = 0; i < SIZE; ++i) {
            assertTrue(q.containsAll(p));
            assertFalse(p.containsAll(q));
            p.add(new PDelay(i));
        }
        assertTrue(p.containsAll(q));
    }

    /**
     * retainAll(c) retains only those elements of c and reports true if changed
     */
    public void testRetainAll() {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        DelayQueue<PDelay> p = populatedQueue(SIZE);
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
            DelayQueue<PDelay> q = populatedQueue(SIZE);
            DelayQueue<PDelay> p = populatedQueue(i);
            assertTrue(q.removeAll(p));
            mustEqual(SIZE - i, q.size());
            for (int j = 0; j < i; ++j) {
                assertFalse(q.contains(p.remove()));
            }
        }
    }

    /**
     * toArray contains all elements
     */
    public void testToArray() throws InterruptedException {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
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
    public void testToArray2() {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        PDelay[] items = new PDelay[SIZE];
        PDelay[] array = q.toArray(items);
        assertSame(items, array);
        Arrays.sort(items);
        for (PDelay o : items)
            assertSame(o, q.remove());
        assertTrue(q.isEmpty());
    }

    /**
     * toArray(incompatible array type) throws ArrayStoreException
     */
    @SuppressWarnings("CollectionToArraySafeParameter")
    public void testToArray_incompatibleArrayType() {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        try {
            q.toArray(new String[10]);
            shouldThrow();
        } catch (ArrayStoreException success) {}
    }

    /**
     * iterator iterates through all elements
     */
    public void testIterator() {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        int i = 0;
        Iterator<PDelay> it = q.iterator();
        while (it.hasNext()) {
            assertTrue(q.contains(it.next()));
            ++i;
        }
        mustEqual(i, SIZE);
        assertIteratorExhausted(it);
    }

    /**
     * iterator of empty collection has no elements
     */
    public void testEmptyIterator() {
        assertIteratorExhausted(new DelayQueue<PDelay>().iterator());
    }

    /**
     * iterator.remove removes current element
     */
    public void testIteratorRemove() {
        final DelayQueue<PDelay> q = new DelayQueue<>();
        q.add(new PDelay(2));
        q.add(new PDelay(1));
        q.add(new PDelay(3));
        Iterator<PDelay> it = q.iterator();
        it.next();
        it.remove();
        it = q.iterator();
        mustEqual(new PDelay(2), it.next());
        mustEqual(new PDelay(3), it.next());
        assertFalse(it.hasNext());
    }

    /**
     * toString contains toStrings of elements
     */
    public void testToString() {
        DelayQueue<PDelay> q = populatedQueue(SIZE);
        String s = q.toString();
        for (Object e : q)
            assertTrue(s.contains(e.toString()));
    }

    /**
     * timed poll transfers elements across Executor tasks
     */
    public void testPollInExecutor() {
        final DelayQueue<PDelay> q = new DelayQueue<>();
        final CheckedBarrier threadsStarted = new CheckedBarrier(2);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try (PoolCleaner cleaner = cleaner(executor)) {
            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertNull(q.poll());
                    threadsStarted.await();
                    assertNotNull(q.poll(LONG_DELAY_MS, MILLISECONDS));
                    checkEmpty(q);
                }});

            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadsStarted.await();
                    q.put(new PDelay(1));
                }});
        }
    }

    /**
     * Delayed actions do not occur until their delay elapses
     */
    public void testDelay() throws InterruptedException {
        DelayQueue<NanoDelay> q = new DelayQueue<>();
        for (int i = 0; i < SIZE; ++i)
            q.add(new NanoDelay(1000000L * (SIZE - i)));

        long last = 0;
        for (int i = 0; i < SIZE; ++i) {
            NanoDelay e = q.take();
            long tt = e.getTriggerTime();
            assertTrue(System.nanoTime() - tt >= 0);
            if (i != 0)
                assertTrue(tt >= last);
            last = tt;
        }
        assertTrue(q.isEmpty());
    }

    /**
     * peek of a non-empty queue returns non-null even if not expired
     */
    public void testPeekDelayed() {
        DelayQueue<NanoDelay> q = new DelayQueue<>();
        q.add(new NanoDelay(Long.MAX_VALUE));
        assertNotNull(q.peek());
    }

    /**
     * poll of a non-empty queue returns null if no expired elements.
     */
    public void testPollDelayed() {
        DelayQueue<NanoDelay> q = new DelayQueue<>();
        q.add(new NanoDelay(Long.MAX_VALUE));
        assertNull(q.poll());
    }

    /**
     * timed poll of a non-empty queue returns null if no expired elements.
     */
    public void testTimedPollDelayed() throws InterruptedException {
        DelayQueue<NanoDelay> q = new DelayQueue<>();
        q.add(new NanoDelay(LONG_DELAY_MS * 1000000L));
        long startTime = System.nanoTime();
        assertNull(q.poll(timeoutMillis(), MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
    }

    /**
     * drainTo(c) empties queue into another collection c
     */
    public void testDrainTo() {
        DelayQueue<PDelay> q = new DelayQueue<>();
        PDelay[] elems = new PDelay[SIZE];
        for (int i = 0; i < SIZE; ++i) {
            elems[i] = new PDelay(i);
            q.add(elems[i]);
        }
        ArrayList<PDelay> l = new ArrayList<>();
        q.drainTo(l);
        mustEqual(0, q.size());
        for (int i = 0; i < SIZE; ++i)
            mustEqual(elems[i], l.get(i));
        q.add(elems[0]);
        q.add(elems[1]);
        assertFalse(q.isEmpty());
        assertTrue(q.contains(elems[0]));
        assertTrue(q.contains(elems[1]));
        l.clear();
        q.drainTo(l);
        mustEqual(0, q.size());
        mustEqual(2, l.size());
        for (int i = 0; i < 2; ++i)
            mustEqual(elems[i], l.get(i));
    }

    /**
     * drainTo empties queue
     */
    public void testDrainToWithActivePut() throws InterruptedException {
        final DelayQueue<PDelay> q = populatedQueue(SIZE);
        Thread t = new Thread(new CheckedRunnable() {
            public void realRun() {
                q.put(new PDelay(SIZE + 1));
            }});

        t.start();
        ArrayList<PDelay> l = new ArrayList<>();
        q.drainTo(l);
        assertTrue(l.size() >= SIZE);
        t.join();
        assertTrue(q.size() + l.size() >= SIZE);
    }

    /**
     * drainTo(c, n) empties first min(n, size) elements of queue into c
     */
    public void testDrainToN() {
        for (int i = 0; i < SIZE + 2; ++i) {
            DelayQueue<PDelay> q = populatedQueue(SIZE);
            ArrayList<PDelay> l = new ArrayList<>();
            q.drainTo(l, i);
            int k = (i < SIZE) ? i : SIZE;
            mustEqual(SIZE - k, q.size());
            mustEqual(k, l.size());
        }
    }

    /**
     * remove(null), contains(null) always return false
     */
    public void testNeverContainsNull() {
        Collection<?> q = populatedQueue(SIZE);
        assertFalse(q.contains(null));
        assertFalse(q.remove(null));
    }
}
