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
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;

import junit.framework.Test;

public class SynchronousQueueTest extends JSR166TestCase {

    public static class Fair extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new SynchronousQueue(true);
        }
    }

    public static class NonFair extends BlockingQueueTest {
        protected BlockingQueue emptyCollection() {
            return new SynchronousQueue(false);
        }
    }

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return newTestSuite(SynchronousQueueTest.class,
                            new Fair().testSuite(),
                            new NonFair().testSuite());
    }

    /**
     * Any SynchronousQueue is both empty and full
     */
    public void testEmptyFull()      { testEmptyFull(false); }
    public void testEmptyFull_fair() { testEmptyFull(true); }
    public void testEmptyFull(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        assertTrue(q.isEmpty());
        mustEqual(0, q.size());
        mustEqual(0, q.remainingCapacity());
        assertFalse(q.offer(zero));
    }

    /**
     * offer fails if no active taker
     */
    public void testOffer()      { testOffer(false); }
    public void testOffer_fair() { testOffer(true); }
    public void testOffer(boolean fair) {
        SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        assertFalse(q.offer(one));
    }

    /**
     * add throws IllegalStateException if no active taker
     */
    public void testAdd()      { testAdd(false); }
    public void testAdd_fair() { testAdd(true); }
    public void testAdd(boolean fair) {
        SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        mustEqual(0, q.remainingCapacity());
        try {
            q.add(one);
            shouldThrow();
        } catch (IllegalStateException success) {}
    }

    /**
     * addAll(this) throws IllegalArgumentException
     */
    public void testAddAll_self()      { testAddAll_self(false); }
    public void testAddAll_self_fair() { testAddAll_self(true); }
    public void testAddAll_self(boolean fair) {
        SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        try {
            q.addAll(q);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**S
     * addAll throws IllegalStateException if no active taker
     */
    public void testAddAll_ISE()      { testAddAll_ISE(false); }
    public void testAddAll_ISE_fair() { testAddAll_ISE(true); }
    public void testAddAll_ISE(boolean fair) {
        SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        Item[] items = seqItems(1);
        Collection<Item> coll = Arrays.asList(items);
        try {
            q.addAll(coll);
            shouldThrow();
        } catch (IllegalStateException success) {}
    }

    /**
     * put blocks interruptibly if no active taker
     */
    public void testBlockingPut()      { testBlockingPut(false); }
    public void testBlockingPut_fair() { testBlockingPut(true); }
    public void testBlockingPut(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
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
        mustEqual(0, q.remainingCapacity());
    }

    /**
     * put blocks interruptibly waiting for take
     */
    public void testPutWithTake()      { testPutWithTake(false); }
    public void testPutWithTake_fair() { testPutWithTake(true); }
    public void testPutWithTake(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        final CountDownLatch pleaseTake = new CountDownLatch(1);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                pleaseTake.countDown();
                q.put(one);

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
        try { assertSame(one, q.take()); }
        catch (InterruptedException e) { threadUnexpectedException(e); }

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
        mustEqual(0, q.remainingCapacity());
    }

    /**
     * timed offer times out if elements not taken
     */
    public void testTimedOffer() {
        final boolean fair = randomBoolean();
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();

                assertFalse(q.offer(zero, timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());

                Thread.currentThread().interrupt();
                try {
                    q.offer(one, randomTimeout(), randomTimeUnit());
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
     * poll return null if no active putter
     */
    public void testPoll()      { testPoll(false); }
    public void testPoll_fair() { testPoll(true); }
    public void testPoll(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        assertNull(q.poll());
    }

    /**
     * timed poll with zero timeout times out if no active putter
     */
    public void testTimedPoll0()      { testTimedPoll0(false); }
    public void testTimedPoll0_fair() { testTimedPoll0(true); }
    public void testTimedPoll0(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        try { assertNull(q.poll(0, MILLISECONDS)); }
        catch (InterruptedException e) { threadUnexpectedException(e); }
    }

    /**
     * timed poll with nonzero timeout times out if no active putter
     */
    public void testTimedPoll() {
        final boolean fair = randomBoolean();
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        final long startTime = System.nanoTime();
        try { assertNull(q.poll(timeoutMillis(), MILLISECONDS)); }
        catch (InterruptedException e) { threadUnexpectedException(e); }
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
    }

    /**
     * timed poll before a delayed offer times out, returning null;
     * after offer succeeds; on interruption throws
     */
    public void testTimedPollWithOffer() {
        final boolean fair = randomBoolean();
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        final CountDownLatch pleaseOffer = new CountDownLatch(1);
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertNull(q.poll(timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());

                pleaseOffer.countDown();
                startTime = System.nanoTime();
                assertSame(zero, q.poll(LONG_DELAY_MS, MILLISECONDS));

                Thread.currentThread().interrupt();
                try {
                    q.poll(randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    q.poll(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
            }});

        await(pleaseOffer);
        long startTime = System.nanoTime();
        try { assertTrue(q.offer(zero, LONG_DELAY_MS, MILLISECONDS)); }
        catch (InterruptedException e) { threadUnexpectedException(e); }
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * peek() returns null if no active putter
     */
    public void testPeek()      { testPeek(false); }
    public void testPeek_fair() { testPeek(true); }
    public void testPeek(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        assertNull(q.peek());
    }

    /**
     * element() throws NoSuchElementException if no active putter
     */
    public void testElement()      { testElement(false); }
    public void testElement_fair() { testElement(true); }
    public void testElement(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        try {
            q.element();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * remove() throws NoSuchElementException if no active putter
     */
    public void testRemove()      { testRemove(false); }
    public void testRemove_fair() { testRemove(true); }
    public void testRemove(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        try {
            q.remove();
            shouldThrow();
        } catch (NoSuchElementException success) {}
    }

    /**
     * contains returns false
     */
    public void testContains()      { testContains(false); }
    public void testContains_fair() { testContains(true); }
    public void testContains(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        assertFalse(q.contains(zero));
    }

    /**
     * clear ensures isEmpty
     */
    public void testClear()      { testClear(false); }
    public void testClear_fair() { testClear(true); }
    public void testClear(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        q.clear();
        assertTrue(q.isEmpty());
    }

    /**
     * containsAll returns false unless empty
     */
    public void testContainsAll()      { testContainsAll(false); }
    public void testContainsAll_fair() { testContainsAll(true); }
    public void testContainsAll(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        Item[] empty = new Item[0];
        assertTrue(q.containsAll(Arrays.asList(empty)));
        Item[] items = new Item[1]; items[0] = zero;
        assertFalse(q.containsAll(Arrays.asList(items)));
    }

    /**
     * retainAll returns false
     */
    public void testRetainAll()      { testRetainAll(false); }
    public void testRetainAll_fair() { testRetainAll(true); }
    public void testRetainAll(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        Item[] empty = new Item[0];
        assertFalse(q.retainAll(Arrays.asList(empty)));
        Item[] items = new Item[1]; items[0] = zero;
        assertFalse(q.retainAll(Arrays.asList(items)));
    }

    /**
     * removeAll returns false
     */
    public void testRemoveAll()      { testRemoveAll(false); }
    public void testRemoveAll_fair() { testRemoveAll(true); }
    public void testRemoveAll(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        Item[] empty = new Item[0];
        assertFalse(q.removeAll(Arrays.asList(empty)));
        Item[] items = new Item[1]; items[0] = zero;
        assertFalse(q.containsAll(Arrays.asList(items)));
    }

    /**
     * toArray is empty
     */
    public void testToArray()      { testToArray(false); }
    public void testToArray_fair() { testToArray(true); }
    public void testToArray(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        Object[] o = q.toArray();
        mustEqual(0, o.length);
    }

    /**
     * toArray(Item array) returns its argument with the first
     * element (if present) nulled out
     */
    public void testToArray2()      { testToArray2(false); }
    public void testToArray2_fair() { testToArray2(true); }
    public void testToArray2(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        Item[] a;

        a = new Item[0];
        assertSame(a, q.toArray(a));

        a = new Item[3];
        Arrays.fill(a, fortytwo);
        assertSame(a, q.toArray(a));
        assertNull(a[0]);
        for (int i = 1; i < a.length; i++)
            mustEqual(42, a[i]);
    }

    /**
     * toArray(null) throws NPE
     */
    public void testToArray_null()      { testToArray_null(false); }
    public void testToArray_null_fair() { testToArray_null(true); }
    public void testToArray_null(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        try {
            Object[] unused = q.toArray((Object[])null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * iterator does not traverse any elements
     */
    public void testIterator()      { testIterator(false); }
    public void testIterator_fair() { testIterator(true); }
    public void testIterator(boolean fair) {
        assertIteratorExhausted(new SynchronousQueue<Item>(fair).iterator());
    }

    /**
     * iterator remove throws IllegalStateException
     */
    public void testIteratorRemove()      { testIteratorRemove(false); }
    public void testIteratorRemove_fair() { testIteratorRemove(true); }
    public void testIteratorRemove(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        Iterator<? extends Item> it = q.iterator();
        try {
            it.remove();
            shouldThrow();
        } catch (IllegalStateException success) {}
    }

    /**
     * toString returns a non-null string
     */
    public void testToString()      { testToString(false); }
    public void testToString_fair() { testToString(true); }
    public void testToString(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        String s = q.toString();
        assertNotNull(s);
    }

    /**
     * offer transfers elements across Executor tasks
     */
    public void testOfferInExecutor()      { testOfferInExecutor(false); }
    public void testOfferInExecutor_fair() { testOfferInExecutor(true); }
    public void testOfferInExecutor(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        final CheckedBarrier threadsStarted = new CheckedBarrier(2);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try (PoolCleaner cleaner = cleaner(executor)) {

            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertFalse(q.offer(one));
                    threadsStarted.await();
                    assertTrue(q.offer(one, LONG_DELAY_MS, MILLISECONDS));
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
    public void testPollInExecutor()      { testPollInExecutor(false); }
    public void testPollInExecutor_fair() { testPollInExecutor(true); }
    public void testPollInExecutor(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        final CheckedBarrier threadsStarted = new CheckedBarrier(2);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try (PoolCleaner cleaner = cleaner(executor)) {
            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertNull(q.poll());
                    threadsStarted.await();
                    assertSame(one, q.poll(LONG_DELAY_MS, MILLISECONDS));
                    assertTrue(q.isEmpty());
                }});

            executor.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadsStarted.await();
                    q.put(one);
                }});
        }
    }

    /**
     * a deserialized/reserialized queue is usable
     */
    public void testSerialization() {
        final SynchronousQueue<Item> x = new SynchronousQueue<>();
        final SynchronousQueue<Item> y = new SynchronousQueue<>(false);
        final SynchronousQueue<Item> z = new SynchronousQueue<>(true);
        assertSerialEquals(x, y);
        assertNotSerialEquals(x, z);
        SynchronousQueue[] rqs = { x, y, z };
        @SuppressWarnings("unchecked")
        SynchronousQueue<Item>[] qs = (SynchronousQueue<Item>[])rqs;
        for (SynchronousQueue<Item> q : qs) {
            SynchronousQueue<Item> clone = serialClone(q);
            assertNotSame(q, clone);
            assertSerialEquals(q, clone);
            assertTrue(clone.isEmpty());
            mustEqual(0, clone.size());
            mustEqual(0, clone.remainingCapacity());
            assertFalse(clone.offer(zero));
        }
    }

    /**
     * drainTo(c) of empty queue doesn't transfer elements
     */
    public void testDrainTo()      { testDrainTo(false); }
    public void testDrainTo_fair() { testDrainTo(true); }
    public void testDrainTo(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        ArrayList<Item> l = new ArrayList<>();
        q.drainTo(l);
        mustEqual(0, q.size());
        mustEqual(0, l.size());
    }

    /**
     * drainTo empties queue, unblocking a waiting put.
     */
    public void testDrainToWithActivePut()      { testDrainToWithActivePut(false); }
    public void testDrainToWithActivePut_fair() { testDrainToWithActivePut(true); }
    public void testDrainToWithActivePut(boolean fair) {
        final SynchronousQueue<Item> q = new SynchronousQueue<>(fair);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.put(one);
            }});

        ArrayList<Item> l = new ArrayList<>();
        long startTime = System.nanoTime();
        while (l.isEmpty()) {
            q.drainTo(l);
            if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                fail("timed out");
            Thread.yield();
        }
        mustEqual(1, l.size());
        assertSame(one, l.get(0));
        awaitTermination(t);
    }

    /**
     * drainTo(c, n) empties up to n elements of queue into c
     */
    public void testDrainToN() throws InterruptedException {
        final SynchronousQueue<Item> q = new SynchronousQueue<>();
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.put(one);
            }});

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                q.put(two);
            }});

        ArrayList<Item> l = new ArrayList<>();
        int drained;
        while ((drained = q.drainTo(l, 1)) == 0) Thread.yield();
        mustEqual(1, drained);
        mustEqual(1, l.size());
        while ((drained = q.drainTo(l, 1)) == 0) Thread.yield();
        mustEqual(1, drained);
        mustEqual(2, l.size());
        assertTrue(l.contains(one));
        assertTrue(l.contains(two));
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * remove(null), contains(null) always return false
     */
    public void testNeverContainsNull() {
        Collection<?> q = new SynchronousQueue<>();
        assertFalse(q.contains(null));
        assertFalse(q.remove(null));
    }

}
