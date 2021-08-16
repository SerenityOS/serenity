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
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject;

import junit.framework.Test;
import junit.framework.TestSuite;

@SuppressWarnings("WaitNotInLoop") // we implement spurious-wakeup freedom
public class AbstractQueuedSynchronizerTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(AbstractQueuedSynchronizerTest.class);
    }

    /**
     * A simple mutex class, adapted from the class javadoc.  Exclusive
     * acquire tests exercise this as a sample user extension.  Other
     * methods/features of AbstractQueuedSynchronizer are tested via
     * other test classes, including those for ReentrantLock,
     * ReentrantReadWriteLock, and Semaphore.
     *
     * Unlike the javadoc sample, we don't track owner thread via
     * AbstractOwnableSynchronizer methods.
     */
    static class Mutex extends AbstractQueuedSynchronizer {
        /** An eccentric value for locked synchronizer state. */
        static final int LOCKED = (1 << 31) | (1 << 15);

        static final int UNLOCKED = 0;

        /** Owner thread is untracked, so this is really just isLocked(). */
        @Override public boolean isHeldExclusively() {
            int state = getState();
            assertTrue(state == UNLOCKED || state == LOCKED);
            return state == LOCKED;
        }

        @Override protected boolean tryAcquire(int acquires) {
            assertEquals(LOCKED, acquires);
            return compareAndSetState(UNLOCKED, LOCKED);
        }

        @Override protected boolean tryRelease(int releases) {
            if (getState() != LOCKED) throw new IllegalMonitorStateException();
            assertEquals(LOCKED, releases);
            setState(UNLOCKED);
            return true;
        }

        public boolean tryAcquireNanos(long nanos) throws InterruptedException {
            return tryAcquireNanos(LOCKED, nanos);
        }

        public boolean tryAcquire() {
            return tryAcquire(LOCKED);
        }

        public boolean tryRelease() {
            return tryRelease(LOCKED);
        }

        public void acquire() {
            acquire(LOCKED);
        }

        public void acquireInterruptibly() throws InterruptedException {
            acquireInterruptibly(LOCKED);
        }

        public void release() {
            release(LOCKED);
        }

        /** Faux-Implements Lock.newCondition(). */
        public ConditionObject newCondition() {
            return new ConditionObject();
        }
    }

    /**
     * A minimal latch class, to test shared mode.
     */
    static class BooleanLatch extends AbstractQueuedSynchronizer {
        public boolean isSignalled() { return getState() != 0; }

        public int tryAcquireShared(int ignore) {
            return isSignalled() ? 1 : -1;
        }

        public boolean tryReleaseShared(int ignore) {
            setState(1);
            return true;
        }
    }

    /**
     * A runnable calling acquireInterruptibly that does not expect to
     * be interrupted.
     */
    class InterruptibleSyncRunnable extends CheckedRunnable {
        final Mutex sync;
        InterruptibleSyncRunnable(Mutex sync) { this.sync = sync; }
        public void realRun() throws InterruptedException {
            sync.acquireInterruptibly();
        }
    }

    /**
     * A runnable calling acquireInterruptibly that expects to be
     * interrupted.
     */
    class InterruptedSyncRunnable extends CheckedInterruptedRunnable {
        final Mutex sync;
        InterruptedSyncRunnable(Mutex sync) { this.sync = sync; }
        public void realRun() throws InterruptedException {
            sync.acquireInterruptibly();
        }
    }

    /** A constant to clarify calls to checking methods below. */
    static final Thread[] NO_THREADS = new Thread[0];

    /**
     * Spin-waits until sync.isQueued(t) becomes true.
     */
    void waitForQueuedThread(AbstractQueuedSynchronizer sync, Thread t) {
        long startTime = System.nanoTime();
        while (!sync.isQueued(t)) {
            if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                throw new AssertionError("timed out");
            Thread.yield();
        }
        assertTrue(t.isAlive());
    }

    /**
     * Checks that sync has exactly the given queued threads.
     */
    void assertHasQueuedThreads(AbstractQueuedSynchronizer sync,
                                Thread... expected) {
        Collection<Thread> actual = sync.getQueuedThreads();
        assertEquals(expected.length > 0, sync.hasQueuedThreads());
        assertEquals(expected.length, sync.getQueueLength());
        assertEquals(expected.length, actual.size());
        assertEquals(expected.length == 0, actual.isEmpty());
        assertEquals(new HashSet<Thread>(actual),
                     new HashSet<Thread>(Arrays.asList(expected)));
    }

    /**
     * Checks that sync has exactly the given (exclusive) queued threads.
     */
    void assertHasExclusiveQueuedThreads(AbstractQueuedSynchronizer sync,
                                         Thread... expected) {
        assertHasQueuedThreads(sync, expected);
        assertEquals(new HashSet<Thread>(sync.getExclusiveQueuedThreads()),
                     new HashSet<Thread>(sync.getQueuedThreads()));
        assertEquals(0, sync.getSharedQueuedThreads().size());
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
    }

    /**
     * Checks that sync has exactly the given (shared) queued threads.
     */
    void assertHasSharedQueuedThreads(AbstractQueuedSynchronizer sync,
                                      Thread... expected) {
        assertHasQueuedThreads(sync, expected);
        assertEquals(new HashSet<Thread>(sync.getSharedQueuedThreads()),
                     new HashSet<Thread>(sync.getQueuedThreads()));
        assertEquals(0, sync.getExclusiveQueuedThreads().size());
        assertTrue(sync.getExclusiveQueuedThreads().isEmpty());
    }

    /**
     * Checks that condition c has exactly the given waiter threads,
     * after acquiring mutex.
     */
    void assertHasWaitersUnlocked(Mutex sync, ConditionObject c,
                                 Thread... threads) {
        sync.acquire();
        assertHasWaitersLocked(sync, c, threads);
        sync.release();
    }

    /**
     * Checks that condition c has exactly the given waiter threads.
     */
    void assertHasWaitersLocked(Mutex sync, ConditionObject c,
                                Thread... threads) {
        assertEquals(threads.length > 0, sync.hasWaiters(c));
        assertEquals(threads.length, sync.getWaitQueueLength(c));
        assertEquals(threads.length == 0, sync.getWaitingThreads(c).isEmpty());
        assertEquals(threads.length, sync.getWaitingThreads(c).size());
        assertEquals(new HashSet<Thread>(sync.getWaitingThreads(c)),
                     new HashSet<Thread>(Arrays.asList(threads)));
    }

    enum AwaitMethod { await, awaitTimed, awaitNanos, awaitUntil }

    /**
     * Awaits condition using the specified AwaitMethod.
     */
    void await(ConditionObject c, AwaitMethod awaitMethod)
            throws InterruptedException {
        long timeoutMillis = 2 * LONG_DELAY_MS;
        switch (awaitMethod) {
        case await:
            c.await();
            break;
        case awaitTimed:
            assertTrue(c.await(timeoutMillis, MILLISECONDS));
            break;
        case awaitNanos:
            long timeoutNanos = MILLISECONDS.toNanos(timeoutMillis);
            long nanosRemaining = c.awaitNanos(timeoutNanos);
            assertTrue(nanosRemaining > 0);
            break;
        case awaitUntil:
            assertTrue(c.awaitUntil(delayedDate(timeoutMillis)));
            break;
        default:
            throw new AssertionError();
        }
    }

    /**
     * Checks that awaiting the given condition times out (using the
     * default timeout duration).
     */
    void assertAwaitTimesOut(ConditionObject c, AwaitMethod awaitMethod) {
        final long timeoutMillis = timeoutMillis();
        final long startTime;
        try {
            switch (awaitMethod) {
            case awaitTimed:
                startTime = System.nanoTime();
                assertFalse(c.await(timeoutMillis, MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
                break;
            case awaitNanos:
                startTime = System.nanoTime();
                long timeoutNanos = MILLISECONDS.toNanos(timeoutMillis);
                long nanosRemaining = c.awaitNanos(timeoutNanos);
                assertTrue(nanosRemaining <= 0);
                assertTrue(nanosRemaining > -MILLISECONDS.toNanos(LONG_DELAY_MS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
                break;
            case awaitUntil:
                // We shouldn't assume that nanoTime and currentTimeMillis
                // use the same time source, so don't use nanoTime here.
                java.util.Date delayedDate = delayedDate(timeoutMillis);
                assertFalse(c.awaitUntil(delayedDate(timeoutMillis)));
                assertTrue(new java.util.Date().getTime() >= delayedDate.getTime());
                break;
            default:
                throw new UnsupportedOperationException();
            }
        } catch (InterruptedException ie) { threadUnexpectedException(ie); }
    }

    /**
     * isHeldExclusively is false upon construction
     */
    public void testIsHeldExclusively() {
        Mutex sync = new Mutex();
        assertFalse(sync.isHeldExclusively());
    }

    /**
     * acquiring released sync succeeds
     */
    public void testAcquire() {
        Mutex sync = new Mutex();
        sync.acquire();
        assertTrue(sync.isHeldExclusively());
        sync.release();
        assertFalse(sync.isHeldExclusively());
    }

    /**
     * tryAcquire on a released sync succeeds
     */
    public void testTryAcquire() {
        Mutex sync = new Mutex();
        assertTrue(sync.tryAcquire());
        assertTrue(sync.isHeldExclusively());
        sync.release();
        assertFalse(sync.isHeldExclusively());
    }

    /**
     * hasQueuedThreads reports whether there are waiting threads
     */
    public void testHasQueuedThreads() {
        final Mutex sync = new Mutex();
        assertFalse(sync.hasQueuedThreads());
        sync.acquire();
        Thread t1 = newStartedThread(new InterruptedSyncRunnable(sync));
        waitForQueuedThread(sync, t1);
        assertTrue(sync.hasQueuedThreads());
        Thread t2 = newStartedThread(new InterruptibleSyncRunnable(sync));
        waitForQueuedThread(sync, t2);
        assertTrue(sync.hasQueuedThreads());
        t1.interrupt();
        awaitTermination(t1);
        assertTrue(sync.hasQueuedThreads());
        sync.release();
        awaitTermination(t2);
        assertFalse(sync.hasQueuedThreads());
    }

    /**
     * isQueued(null) throws NullPointerException
     */
    public void testIsQueuedNPE() {
        final Mutex sync = new Mutex();
        try {
            sync.isQueued(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * isQueued reports whether a thread is queued
     */
    public void testIsQueued() {
        final Mutex sync = new Mutex();
        Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
        Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
        assertFalse(sync.isQueued(t1));
        assertFalse(sync.isQueued(t2));
        sync.acquire();
        t1.start();
        waitForQueuedThread(sync, t1);
        assertTrue(sync.isQueued(t1));
        assertFalse(sync.isQueued(t2));
        t2.start();
        waitForQueuedThread(sync, t2);
        assertTrue(sync.isQueued(t1));
        assertTrue(sync.isQueued(t2));
        t1.interrupt();
        awaitTermination(t1);
        assertFalse(sync.isQueued(t1));
        assertTrue(sync.isQueued(t2));
        sync.release();
        awaitTermination(t2);
        assertFalse(sync.isQueued(t1));
        assertFalse(sync.isQueued(t2));
    }

    /**
     * getFirstQueuedThread returns first waiting thread or null if none
     */
    public void testGetFirstQueuedThread() {
        final Mutex sync = new Mutex();
        assertNull(sync.getFirstQueuedThread());
        sync.acquire();
        Thread t1 = newStartedThread(new InterruptedSyncRunnable(sync));
        waitForQueuedThread(sync, t1);
        assertEquals(t1, sync.getFirstQueuedThread());
        Thread t2 = newStartedThread(new InterruptibleSyncRunnable(sync));
        waitForQueuedThread(sync, t2);
        assertEquals(t1, sync.getFirstQueuedThread());
        t1.interrupt();
        awaitTermination(t1);
        assertEquals(t2, sync.getFirstQueuedThread());
        sync.release();
        awaitTermination(t2);
        assertNull(sync.getFirstQueuedThread());
    }

    /**
     * hasContended reports false if no thread has ever blocked, else true
     */
    public void testHasContended() {
        final Mutex sync = new Mutex();
        assertFalse(sync.hasContended());
        sync.acquire();
        assertFalse(sync.hasContended());
        Thread t1 = newStartedThread(new InterruptedSyncRunnable(sync));
        waitForQueuedThread(sync, t1);
        assertTrue(sync.hasContended());
        Thread t2 = newStartedThread(new InterruptibleSyncRunnable(sync));
        waitForQueuedThread(sync, t2);
        assertTrue(sync.hasContended());
        t1.interrupt();
        awaitTermination(t1);
        assertTrue(sync.hasContended());
        sync.release();
        awaitTermination(t2);
        assertTrue(sync.hasContended());
    }

    /**
     * getQueuedThreads returns all waiting threads
     */
    public void testGetQueuedThreads() {
        final Mutex sync = new Mutex();
        Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
        Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
        sync.acquire();
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
        t1.start();
        waitForQueuedThread(sync, t1);
        assertHasExclusiveQueuedThreads(sync, t1);
        assertTrue(sync.getQueuedThreads().contains(t1));
        assertFalse(sync.getQueuedThreads().contains(t2));
        t2.start();
        waitForQueuedThread(sync, t2);
        assertHasExclusiveQueuedThreads(sync, t1, t2);
        assertTrue(sync.getQueuedThreads().contains(t1));
        assertTrue(sync.getQueuedThreads().contains(t2));
        t1.interrupt();
        awaitTermination(t1);
        assertHasExclusiveQueuedThreads(sync, t2);
        sync.release();
        awaitTermination(t2);
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
    }

    /**
     * getExclusiveQueuedThreads returns all exclusive waiting threads
     */
    public void testGetExclusiveQueuedThreads() {
        final Mutex sync = new Mutex();
        Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
        Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
        sync.acquire();
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
        t1.start();
        waitForQueuedThread(sync, t1);
        assertHasExclusiveQueuedThreads(sync, t1);
        assertTrue(sync.getExclusiveQueuedThreads().contains(t1));
        assertFalse(sync.getExclusiveQueuedThreads().contains(t2));
        t2.start();
        waitForQueuedThread(sync, t2);
        assertHasExclusiveQueuedThreads(sync, t1, t2);
        assertTrue(sync.getExclusiveQueuedThreads().contains(t1));
        assertTrue(sync.getExclusiveQueuedThreads().contains(t2));
        t1.interrupt();
        awaitTermination(t1);
        assertHasExclusiveQueuedThreads(sync, t2);
        sync.release();
        awaitTermination(t2);
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
    }

    /**
     * getSharedQueuedThreads does not include exclusively waiting threads
     */
    public void testGetSharedQueuedThreads_Exclusive() {
        final Mutex sync = new Mutex();
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        sync.acquire();
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        Thread t1 = newStartedThread(new InterruptedSyncRunnable(sync));
        waitForQueuedThread(sync, t1);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        Thread t2 = newStartedThread(new InterruptibleSyncRunnable(sync));
        waitForQueuedThread(sync, t2);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        t1.interrupt();
        awaitTermination(t1);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        sync.release();
        awaitTermination(t2);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
    }

    /**
     * getSharedQueuedThreads returns all shared waiting threads
     */
    public void testGetSharedQueuedThreads_Shared() {
        final BooleanLatch l = new BooleanLatch();
        assertHasSharedQueuedThreads(l, NO_THREADS);
        Thread t1 = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                l.acquireSharedInterruptibly(0);
            }});
        waitForQueuedThread(l, t1);
        assertHasSharedQueuedThreads(l, t1);
        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                l.acquireSharedInterruptibly(0);
            }});
        waitForQueuedThread(l, t2);
        assertHasSharedQueuedThreads(l, t1, t2);
        t1.interrupt();
        awaitTermination(t1);
        assertHasSharedQueuedThreads(l, t2);
        assertTrue(l.releaseShared(0));
        awaitTermination(t2);
        assertHasSharedQueuedThreads(l, NO_THREADS);
    }

    /**
     * tryAcquireNanos is interruptible
     */
    public void testTryAcquireNanos_Interruptible() {
        final Mutex sync = new Mutex();
        sync.acquire();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                sync.tryAcquireNanos(MILLISECONDS.toNanos(2 * LONG_DELAY_MS));
            }});

        waitForQueuedThread(sync, t);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * tryAcquire on exclusively held sync fails
     */
    public void testTryAcquireWhenSynced() {
        final Mutex sync = new Mutex();
        sync.acquire();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertFalse(sync.tryAcquire());
            }});

        awaitTermination(t);
        sync.release();
    }

    /**
     * tryAcquireNanos on an exclusively held sync times out
     */
    public void testAcquireNanos_Timeout() {
        final Mutex sync = new Mutex();
        sync.acquire();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                long nanos = MILLISECONDS.toNanos(timeoutMillis());
                assertFalse(sync.tryAcquireNanos(nanos));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
            }});

        awaitTermination(t);
        sync.release();
    }

    /**
     * getState is true when acquired and false when not
     */
    public void testGetState() {
        final Mutex sync = new Mutex();
        sync.acquire();
        assertTrue(sync.isHeldExclusively());
        sync.release();
        assertFalse(sync.isHeldExclusively());

        final BooleanLatch acquired = new BooleanLatch();
        final BooleanLatch done = new BooleanLatch();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                sync.acquire();
                assertTrue(acquired.releaseShared(0));
                done.acquireShared(0);
                sync.release();
            }});

        acquired.acquireShared(0);
        assertTrue(sync.isHeldExclusively());
        assertTrue(done.releaseShared(0));
        awaitTermination(t);
        assertFalse(sync.isHeldExclusively());
    }

    /**
     * acquireInterruptibly succeeds when released, else is interruptible
     */
    public void testAcquireInterruptibly() throws InterruptedException {
        final Mutex sync = new Mutex();
        final BooleanLatch threadStarted = new BooleanLatch();
        sync.acquireInterruptibly();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                assertTrue(threadStarted.releaseShared(0));
                sync.acquireInterruptibly();
            }});

        threadStarted.acquireShared(0);
        waitForQueuedThread(sync, t);
        t.interrupt();
        awaitTermination(t);
        assertTrue(sync.isHeldExclusively());
    }

    /**
     * owns is true for a condition created by sync else false
     */
    public void testOwns() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        final Mutex sync2 = new Mutex();
        assertTrue(sync.owns(c));
        assertFalse(sync2.owns(c));
    }

    /**
     * Calling await without holding sync throws IllegalMonitorStateException
     */
    public void testAwait_IMSE() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        for (AwaitMethod awaitMethod : AwaitMethod.values()) {
            long startTime = System.nanoTime();
            try {
                await(c, awaitMethod);
                shouldThrow();
            } catch (IllegalMonitorStateException success) {
            } catch (InterruptedException e) { threadUnexpectedException(e); }
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
    }

    /**
     * Calling signal without holding sync throws IllegalMonitorStateException
     */
    public void testSignal_IMSE() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        try {
            c.signal();
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
        assertHasWaitersUnlocked(sync, c, NO_THREADS);
    }

    /**
     * Calling signalAll without holding sync throws IllegalMonitorStateException
     */
    public void testSignalAll_IMSE() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        try {
            c.signalAll();
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * await/awaitNanos/awaitUntil without a signal times out
     */
    public void testAwaitTimed_Timeout() { testAwait_Timeout(AwaitMethod.awaitTimed); }
    public void testAwaitNanos_Timeout() { testAwait_Timeout(AwaitMethod.awaitNanos); }
    public void testAwaitUntil_Timeout() { testAwait_Timeout(AwaitMethod.awaitUntil); }
    public void testAwait_Timeout(AwaitMethod awaitMethod) {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        sync.acquire();
        assertAwaitTimesOut(c, awaitMethod);
        sync.release();
    }

    /**
     * await/awaitNanos/awaitUntil returns when signalled
     */
    public void testSignal_await()      { testSignal(AwaitMethod.await); }
    public void testSignal_awaitTimed() { testSignal(AwaitMethod.awaitTimed); }
    public void testSignal_awaitNanos() { testSignal(AwaitMethod.awaitNanos); }
    public void testSignal_awaitUntil() { testSignal(AwaitMethod.awaitUntil); }
    public void testSignal(final AwaitMethod awaitMethod) {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        final BooleanLatch acquired = new BooleanLatch();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                sync.acquire();
                assertTrue(acquired.releaseShared(0));
                await(c, awaitMethod);
                sync.release();
            }});

        acquired.acquireShared(0);
        sync.acquire();
        assertHasWaitersLocked(sync, c, t);
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
        c.signal();
        assertHasWaitersLocked(sync, c, NO_THREADS);
        assertHasExclusiveQueuedThreads(sync, t);
        sync.release();
        awaitTermination(t);
    }

    /**
     * hasWaiters(null) throws NullPointerException
     */
    public void testHasWaitersNPE() {
        final Mutex sync = new Mutex();
        try {
            sync.hasWaiters(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * getWaitQueueLength(null) throws NullPointerException
     */
    public void testGetWaitQueueLengthNPE() {
        final Mutex sync = new Mutex();
        try {
            sync.getWaitQueueLength(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * getWaitingThreads(null) throws NullPointerException
     */
    public void testGetWaitingThreadsNPE() {
        final Mutex sync = new Mutex();
        try {
            sync.getWaitingThreads(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * hasWaiters throws IllegalArgumentException if not owned
     */
    public void testHasWaitersIAE() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        final Mutex sync2 = new Mutex();
        try {
            sync2.hasWaiters(c);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
        assertHasWaitersUnlocked(sync, c, NO_THREADS);
    }

    /**
     * hasWaiters throws IllegalMonitorStateException if not synced
     */
    public void testHasWaitersIMSE() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        try {
            sync.hasWaiters(c);
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
        assertHasWaitersUnlocked(sync, c, NO_THREADS);
    }

    /**
     * getWaitQueueLength throws IllegalArgumentException if not owned
     */
    public void testGetWaitQueueLengthIAE() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        final Mutex sync2 = new Mutex();
        try {
            sync2.getWaitQueueLength(c);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
        assertHasWaitersUnlocked(sync, c, NO_THREADS);
    }

    /**
     * getWaitQueueLength throws IllegalMonitorStateException if not synced
     */
    public void testGetWaitQueueLengthIMSE() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        try {
            sync.getWaitQueueLength(c);
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
        assertHasWaitersUnlocked(sync, c, NO_THREADS);
    }

    /**
     * getWaitingThreads throws IllegalArgumentException if not owned
     */
    public void testGetWaitingThreadsIAE() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        final Mutex sync2 = new Mutex();
        try {
            sync2.getWaitingThreads(c);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
        assertHasWaitersUnlocked(sync, c, NO_THREADS);
    }

    /**
     * getWaitingThreads throws IllegalMonitorStateException if not synced
     */
    public void testGetWaitingThreadsIMSE() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        try {
            sync.getWaitingThreads(c);
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
        assertHasWaitersUnlocked(sync, c, NO_THREADS);
    }

    /**
     * hasWaiters returns true when a thread is waiting, else false
     */
    public void testHasWaiters() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        final BooleanLatch acquired = new BooleanLatch();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                sync.acquire();
                assertHasWaitersLocked(sync, c, NO_THREADS);
                assertFalse(sync.hasWaiters(c));
                assertTrue(acquired.releaseShared(0));
                c.await();
                sync.release();
            }});

        acquired.acquireShared(0);
        sync.acquire();
        assertHasWaitersLocked(sync, c, t);
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
        assertTrue(sync.hasWaiters(c));
        c.signal();
        assertHasWaitersLocked(sync, c, NO_THREADS);
        assertHasExclusiveQueuedThreads(sync, t);
        assertFalse(sync.hasWaiters(c));
        sync.release();

        awaitTermination(t);
        assertHasWaitersUnlocked(sync, c, NO_THREADS);
    }

    /**
     * getWaitQueueLength returns number of waiting threads
     */
    public void testGetWaitQueueLength() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        final BooleanLatch acquired1 = new BooleanLatch();
        final BooleanLatch acquired2 = new BooleanLatch();
        final Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                sync.acquire();
                assertHasWaitersLocked(sync, c, NO_THREADS);
                assertEquals(0, sync.getWaitQueueLength(c));
                assertTrue(acquired1.releaseShared(0));
                c.await();
                sync.release();
            }});
        acquired1.acquireShared(0);
        sync.acquire();
        assertHasWaitersLocked(sync, c, t1);
        assertEquals(1, sync.getWaitQueueLength(c));
        sync.release();

        final Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                sync.acquire();
                assertHasWaitersLocked(sync, c, t1);
                assertEquals(1, sync.getWaitQueueLength(c));
                assertTrue(acquired2.releaseShared(0));
                c.await();
                sync.release();
            }});
        acquired2.acquireShared(0);
        sync.acquire();
        assertHasWaitersLocked(sync, c, t1, t2);
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
        assertEquals(2, sync.getWaitQueueLength(c));
        c.signalAll();
        assertHasWaitersLocked(sync, c, NO_THREADS);
        assertHasExclusiveQueuedThreads(sync, t1, t2);
        assertEquals(0, sync.getWaitQueueLength(c));
        sync.release();

        awaitTermination(t1);
        awaitTermination(t2);
        assertHasWaitersUnlocked(sync, c, NO_THREADS);
    }

    /**
     * getWaitingThreads returns only and all waiting threads
     */
    public void testGetWaitingThreads() {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        final BooleanLatch acquired1 = new BooleanLatch();
        final BooleanLatch acquired2 = new BooleanLatch();
        final Thread t1 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                sync.acquire();
                assertHasWaitersLocked(sync, c, NO_THREADS);
                assertTrue(sync.getWaitingThreads(c).isEmpty());
                assertTrue(acquired1.releaseShared(0));
                c.await();
                sync.release();
            }});

        final Thread t2 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                sync.acquire();
                assertHasWaitersLocked(sync, c, t1);
                assertTrue(sync.getWaitingThreads(c).contains(t1));
                assertFalse(sync.getWaitingThreads(c).isEmpty());
                assertEquals(1, sync.getWaitingThreads(c).size());
                assertTrue(acquired2.releaseShared(0));
                c.await();
                sync.release();
            }});

        sync.acquire();
        assertHasWaitersLocked(sync, c, NO_THREADS);
        assertFalse(sync.getWaitingThreads(c).contains(t1));
        assertFalse(sync.getWaitingThreads(c).contains(t2));
        assertTrue(sync.getWaitingThreads(c).isEmpty());
        assertEquals(0, sync.getWaitingThreads(c).size());
        sync.release();

        t1.start();
        acquired1.acquireShared(0);
        sync.acquire();
        assertHasWaitersLocked(sync, c, t1);
        assertTrue(sync.getWaitingThreads(c).contains(t1));
        assertFalse(sync.getWaitingThreads(c).contains(t2));
        assertFalse(sync.getWaitingThreads(c).isEmpty());
        assertEquals(1, sync.getWaitingThreads(c).size());
        sync.release();

        t2.start();
        acquired2.acquireShared(0);
        sync.acquire();
        assertHasWaitersLocked(sync, c, t1, t2);
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
        assertTrue(sync.getWaitingThreads(c).contains(t1));
        assertTrue(sync.getWaitingThreads(c).contains(t2));
        assertFalse(sync.getWaitingThreads(c).isEmpty());
        assertEquals(2, sync.getWaitingThreads(c).size());
        c.signalAll();
        assertHasWaitersLocked(sync, c, NO_THREADS);
        assertHasExclusiveQueuedThreads(sync, t1, t2);
        assertFalse(sync.getWaitingThreads(c).contains(t1));
        assertFalse(sync.getWaitingThreads(c).contains(t2));
        assertTrue(sync.getWaitingThreads(c).isEmpty());
        assertEquals(0, sync.getWaitingThreads(c).size());
        sync.release();

        awaitTermination(t1);
        awaitTermination(t2);
        assertHasWaitersUnlocked(sync, c, NO_THREADS);
    }

    /**
     * awaitUninterruptibly is uninterruptible
     */
    public void testAwaitUninterruptibly() {
        final Mutex sync = new Mutex();
        final ConditionObject condition = sync.newCondition();
        final BooleanLatch pleaseInterrupt = new BooleanLatch();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                sync.acquire();
                assertTrue(pleaseInterrupt.releaseShared(0));
                condition.awaitUninterruptibly();
                assertTrue(Thread.interrupted());
                assertHasWaitersLocked(sync, condition, NO_THREADS);
                sync.release();
            }});

        pleaseInterrupt.acquireShared(0);
        sync.acquire();
        assertHasWaitersLocked(sync, condition, t);
        sync.release();
        t.interrupt();
        assertHasWaitersUnlocked(sync, condition, t);
        assertThreadBlocks(t, Thread.State.WAITING);
        sync.acquire();
        assertHasWaitersLocked(sync, condition, t);
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
        condition.signal();
        assertHasWaitersLocked(sync, condition, NO_THREADS);
        assertHasExclusiveQueuedThreads(sync, t);
        sync.release();
        awaitTermination(t);
    }

    /**
     * await/awaitNanos/awaitUntil is interruptible
     */
    public void testInterruptible_await()      { testInterruptible(AwaitMethod.await); }
    public void testInterruptible_awaitTimed() { testInterruptible(AwaitMethod.awaitTimed); }
    public void testInterruptible_awaitNanos() { testInterruptible(AwaitMethod.awaitNanos); }
    public void testInterruptible_awaitUntil() { testInterruptible(AwaitMethod.awaitUntil); }
    public void testInterruptible(final AwaitMethod awaitMethod) {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        final BooleanLatch pleaseInterrupt = new BooleanLatch();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                sync.acquire();
                assertTrue(pleaseInterrupt.releaseShared(0));
                await(c, awaitMethod);
            }});

        pleaseInterrupt.acquireShared(0);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * signalAll wakes up all threads
     */
    public void testSignalAll_await()      { testSignalAll(AwaitMethod.await); }
    public void testSignalAll_awaitTimed() { testSignalAll(AwaitMethod.awaitTimed); }
    public void testSignalAll_awaitNanos() { testSignalAll(AwaitMethod.awaitNanos); }
    public void testSignalAll_awaitUntil() { testSignalAll(AwaitMethod.awaitUntil); }
    public void testSignalAll(final AwaitMethod awaitMethod) {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        final BooleanLatch acquired1 = new BooleanLatch();
        final BooleanLatch acquired2 = new BooleanLatch();
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                sync.acquire();
                acquired1.releaseShared(0);
                await(c, awaitMethod);
                sync.release();
            }});

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                sync.acquire();
                acquired2.releaseShared(0);
                await(c, awaitMethod);
                sync.release();
            }});

        acquired1.acquireShared(0);
        acquired2.acquireShared(0);
        sync.acquire();
        assertHasWaitersLocked(sync, c, t1, t2);
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
        c.signalAll();
        assertHasWaitersLocked(sync, c, NO_THREADS);
        assertHasExclusiveQueuedThreads(sync, t1, t2);
        sync.release();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * toString indicates current state
     */
    public void testToString() {
        Mutex sync = new Mutex();
        assertTrue(sync.toString().contains("State = " + Mutex.UNLOCKED));
        sync.acquire();
        assertTrue(sync.toString().contains("State = " + Mutex.LOCKED));
    }

    /**
     * A serialized AQS deserializes with current state, but no queued threads
     */
    public void testSerialization() {
        Mutex sync = new Mutex();
        assertFalse(serialClone(sync).isHeldExclusively());
        sync.acquire();
        Thread t = newStartedThread(new InterruptedSyncRunnable(sync));
        waitForQueuedThread(sync, t);
        assertTrue(sync.isHeldExclusively());

        Mutex clone = serialClone(sync);
        assertTrue(clone.isHeldExclusively());
        assertHasExclusiveQueuedThreads(sync, t);
        assertHasExclusiveQueuedThreads(clone, NO_THREADS);
        t.interrupt();
        awaitTermination(t);
        sync.release();
        assertFalse(sync.isHeldExclusively());
        assertTrue(clone.isHeldExclusively());
        assertHasExclusiveQueuedThreads(sync, NO_THREADS);
        assertHasExclusiveQueuedThreads(clone, NO_THREADS);
    }

    /**
     * tryReleaseShared setting state changes getState
     */
    public void testGetStateWithReleaseShared() {
        final BooleanLatch l = new BooleanLatch();
        assertFalse(l.isSignalled());
        assertTrue(l.releaseShared(0));
        assertTrue(l.isSignalled());
    }

    /**
     * releaseShared has no effect when already signalled
     */
    public void testReleaseShared() {
        final BooleanLatch l = new BooleanLatch();
        assertFalse(l.isSignalled());
        assertTrue(l.releaseShared(0));
        assertTrue(l.isSignalled());
        assertTrue(l.releaseShared(0));
        assertTrue(l.isSignalled());
    }

    /**
     * acquireSharedInterruptibly returns after release, but not before
     */
    public void testAcquireSharedInterruptibly() {
        final BooleanLatch l = new BooleanLatch();

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertFalse(l.isSignalled());
                l.acquireSharedInterruptibly(0);
                assertTrue(l.isSignalled());
                l.acquireSharedInterruptibly(0);
                assertTrue(l.isSignalled());
            }});

        waitForQueuedThread(l, t);
        assertFalse(l.isSignalled());
        assertThreadBlocks(t, Thread.State.WAITING);
        assertHasSharedQueuedThreads(l, t);
        assertTrue(l.releaseShared(0));
        assertTrue(l.isSignalled());
        awaitTermination(t);
    }

    /**
     * tryAcquireSharedNanos returns after release, but not before
     */
    public void testTryAcquireSharedNanos() {
        final BooleanLatch l = new BooleanLatch();

        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertFalse(l.isSignalled());
                long nanos = MILLISECONDS.toNanos(2 * LONG_DELAY_MS);
                assertTrue(l.tryAcquireSharedNanos(0, nanos));
                assertTrue(l.isSignalled());
                assertTrue(l.tryAcquireSharedNanos(0, nanos));
                assertTrue(l.isSignalled());
            }});

        waitForQueuedThread(l, t);
        assertFalse(l.isSignalled());
        assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        assertTrue(l.releaseShared(0));
        assertTrue(l.isSignalled());
        awaitTermination(t);
    }

    /**
     * acquireSharedInterruptibly is interruptible
     */
    public void testAcquireSharedInterruptibly_Interruptible() {
        final BooleanLatch l = new BooleanLatch();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                assertFalse(l.isSignalled());
                l.acquireSharedInterruptibly(0);
            }});

        waitForQueuedThread(l, t);
        assertFalse(l.isSignalled());
        t.interrupt();
        awaitTermination(t);
        assertFalse(l.isSignalled());
    }

    /**
     * tryAcquireSharedNanos is interruptible
     */
    public void testTryAcquireSharedNanos_Interruptible() {
        final BooleanLatch l = new BooleanLatch();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                assertFalse(l.isSignalled());
                long nanos = MILLISECONDS.toNanos(2 * LONG_DELAY_MS);
                l.tryAcquireSharedNanos(0, nanos);
            }});

        waitForQueuedThread(l, t);
        assertFalse(l.isSignalled());
        t.interrupt();
        awaitTermination(t);
        assertFalse(l.isSignalled());
    }

    /**
     * tryAcquireSharedNanos times out if not released before timeout
     */
    public void testTryAcquireSharedNanos_Timeout() {
        final BooleanLatch l = new BooleanLatch();
        final BooleanLatch observedQueued = new BooleanLatch();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertFalse(l.isSignalled());
                for (long millis = timeoutMillis();
                     !observedQueued.isSignalled();
                     millis *= 2) {
                    long nanos = MILLISECONDS.toNanos(millis);
                    long startTime = System.nanoTime();
                    assertFalse(l.tryAcquireSharedNanos(0, nanos));
                    assertTrue(millisElapsedSince(startTime) >= millis);
                }
                assertFalse(l.isSignalled());
            }});

        waitForQueuedThread(l, t);
        observedQueued.releaseShared(0);
        assertFalse(l.isSignalled());
        awaitTermination(t);
        assertFalse(l.isSignalled());
    }

    /**
     * awaitNanos/timed await with 0 wait times out immediately
     */
    public void testAwait_Zero() throws InterruptedException {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        sync.acquire();
        assertTrue(c.awaitNanos(0L) <= 0);
        assertFalse(c.await(0L, NANOSECONDS));
        sync.release();
    }

    /**
     * awaitNanos/timed await with maximum negative wait times does not underflow
     */
    public void testAwait_NegativeInfinity() throws InterruptedException {
        final Mutex sync = new Mutex();
        final ConditionObject c = sync.newCondition();
        sync.acquire();
        assertTrue(c.awaitNanos(Long.MIN_VALUE) <= 0);
        assertFalse(c.await(Long.MIN_VALUE, NANOSECONDS));
        sync.release();
    }

    /**
     * JDK-8191483: AbstractQueuedSynchronizer cancel/cancel race
     * ant -Djsr166.tckTestClass=AbstractQueuedSynchronizerTest -Djsr166.methodFilter=testCancelCancelRace -Djsr166.runsPerTest=100 tck
     */
    public void testCancelCancelRace() throws InterruptedException {
        class Sync extends AbstractQueuedSynchronizer {
            protected boolean tryAcquire(int acquires) {
                return !hasQueuedPredecessors() && compareAndSetState(0, 1);
            }
            protected boolean tryRelease(int releases) {
                return compareAndSetState(1, 0);
            }
        }

        Sync s = new Sync();
        s.acquire(1);           // acquire to force other threads to enqueue

        // try to trigger double cancel race with two background threads
        ArrayList<Thread> threads = new ArrayList<>();
        Runnable failedAcquire = () -> {
            try {
                s.acquireInterruptibly(1);
                shouldThrow();
            } catch (InterruptedException success) {}
        };
        for (int i = 0; i < 2; i++) {
            Thread thread = new Thread(failedAcquire);
            thread.start();
            threads.add(thread);
        }
        Thread.sleep(100);
        for (Thread thread : threads) thread.interrupt();
        for (Thread thread : threads) awaitTermination(thread);

        s.release(1);

        // no one holds lock now, we should be able to acquire
        if (!s.tryAcquire(1))
            throw new RuntimeException(
                String.format(
                    "Broken: hasQueuedPredecessors=%s hasQueuedThreads=%s queueLength=%d firstQueuedThread=%s",
                    s.hasQueuedPredecessors(),
                    s.hasQueuedThreads(),
                    s.getQueueLength(),
                    s.getFirstQueuedThread()));
    }

    /**
     * Tests scenario for
     * JDK-8191937: Lost interrupt in AbstractQueuedSynchronizer when tryAcquire methods throw
     * ant -Djsr166.tckTestClass=AbstractQueuedSynchronizerTest -Djsr166.methodFilter=testInterruptedFailingAcquire -Djsr166.runsPerTest=10000 tck
     */
    public void testInterruptedFailingAcquire() throws Throwable {
        class PleaseThrow extends RuntimeException {}
        final PleaseThrow ex = new PleaseThrow();
        final AtomicBoolean thrown = new AtomicBoolean();

        // A synchronizer only offering a choice of failure modes
        class Sync extends AbstractQueuedSynchronizer {
            volatile boolean pleaseThrow;
            void maybeThrow() {
                if (pleaseThrow) {
                    // assert: tryAcquire methods can throw at most once
                    if (! thrown.compareAndSet(false, true))
                        throw new AssertionError();
                    throw ex;
                }
            }

            @Override protected boolean tryAcquire(int ignored) {
                maybeThrow();
                return false;
            }
            @Override protected int tryAcquireShared(int ignored) {
                maybeThrow();
                return -1;
            }
            @Override protected boolean tryRelease(int ignored) {
                return true;
            }
            @Override protected boolean tryReleaseShared(int ignored) {
                return true;
            }
        }

        final Sync s = new Sync();
        final boolean acquireInterruptibly = randomBoolean();
        final Action[] uninterruptibleAcquireActions = {
            () -> s.acquire(1),
            () -> s.acquireShared(1),
        };
        final long nanosTimeout = MILLISECONDS.toNanos(2 * LONG_DELAY_MS);
        final Action[] interruptibleAcquireActions = {
            () -> s.acquireInterruptibly(1),
            () -> s.acquireSharedInterruptibly(1),
            () -> s.tryAcquireNanos(1, nanosTimeout),
            () -> s.tryAcquireSharedNanos(1, nanosTimeout),
        };
        final Action[] releaseActions = {
            () -> s.release(1),
            () -> s.releaseShared(1),
        };
        final Action acquireAction = acquireInterruptibly
            ? chooseRandomly(interruptibleAcquireActions)
            : chooseRandomly(uninterruptibleAcquireActions);
        final Action releaseAction
            = chooseRandomly(releaseActions);

        // From os_posix.cpp:
        //
        // NOTE that since there is no "lock" around the interrupt and
        // is_interrupted operations, there is the possibility that the
        // interrupted flag (in osThread) will be "false" but that the
        // low-level events will be in the signaled state. This is
        // intentional. The effect of this is that Object.wait() and
        // LockSupport.park() will appear to have a spurious wakeup, which
        // is allowed and not harmful, and the possibility is so rare that
        // it is not worth the added complexity to add yet another lock.
        final Thread thread = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Throwable {
                try {
                    acquireAction.run();
                    shouldThrow();
                } catch (InterruptedException possible) {
                    assertTrue(acquireInterruptibly);
                    assertFalse(Thread.interrupted());
                } catch (PleaseThrow possible) {
                    awaitInterrupted();
                }
            }});
        for (long startTime = 0L;; ) {
            waitForThreadToEnterWaitState(thread);
            if (s.getFirstQueuedThread() == thread
                && s.hasQueuedPredecessors()
                && s.hasQueuedThreads()
                && s.getQueueLength() == 1
                && s.hasContended())
                break;
            if (startTime == 0L)
                startTime = System.nanoTime();
            else if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                fail("timed out waiting for AQS state: "
                     + "thread state=" + thread.getState()
                     + ", queued threads=" + s.getQueuedThreads());
            Thread.yield();
        }

        s.pleaseThrow = true;
        // release and interrupt, in random order
        if (randomBoolean()) {
            thread.interrupt();
            releaseAction.run();
        } else {
            releaseAction.run();
            thread.interrupt();
        }
        awaitTermination(thread);

        if (! acquireInterruptibly)
            assertTrue(thrown.get());

        assertNull(s.getFirstQueuedThread());
        assertFalse(s.hasQueuedPredecessors());
        assertFalse(s.hasQueuedThreads());
        assertEquals(0, s.getQueueLength());
        assertTrue(s.getQueuedThreads().isEmpty());
        assertTrue(s.hasContended());
    }

}
