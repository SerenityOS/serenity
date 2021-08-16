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

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import junit.framework.Test;
import junit.framework.TestSuite;

public class SemaphoreTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(SemaphoreTest.class);
    }

    /**
     * Subclass to expose protected methods
     */
    static class PublicSemaphore extends Semaphore {
        PublicSemaphore(int permits) { super(permits); }
        PublicSemaphore(int permits, boolean fair) { super(permits, fair); }
        public Collection<Thread> getQueuedThreads() {
            return super.getQueuedThreads();
        }
        public boolean hasQueuedThread(Thread t) {
            return super.getQueuedThreads().contains(t);
        }
        public void reducePermits(int reduction) {
            super.reducePermits(reduction);
        }
    }

    /**
     * A runnable calling acquire
     */
    class InterruptibleLockRunnable extends CheckedRunnable {
        final Semaphore lock;
        InterruptibleLockRunnable(Semaphore s) { lock = s; }
        public void realRun() {
            try {
                lock.acquire();
            }
            catch (InterruptedException ignored) {}
        }
    }

    /**
     * A runnable calling acquire that expects to be interrupted
     */
    class InterruptedLockRunnable extends CheckedInterruptedRunnable {
        final Semaphore lock;
        InterruptedLockRunnable(Semaphore s) { lock = s; }
        public void realRun() throws InterruptedException {
            lock.acquire();
        }
    }

    /**
     * Spin-waits until s.hasQueuedThread(t) becomes true.
     */
    void waitForQueuedThread(PublicSemaphore s, Thread t) {
        long startTime = System.nanoTime();
        while (!s.hasQueuedThread(t)) {
            if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                throw new AssertionError("timed out");
            Thread.yield();
        }
        assertTrue(s.hasQueuedThreads());
        assertTrue(t.isAlive());
    }

    /**
     * Spin-waits until s.hasQueuedThreads() becomes true.
     */
    void waitForQueuedThreads(Semaphore s) {
        long startTime = System.nanoTime();
        while (!s.hasQueuedThreads()) {
            if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                throw new AssertionError("timed out");
            Thread.yield();
        }
    }

    enum AcquireMethod {
        acquire() {
            void acquire(Semaphore s) throws InterruptedException {
                s.acquire();
            }
        },
        acquireN() {
            void acquire(Semaphore s, int permits) throws InterruptedException {
                s.acquire(permits);
            }
        },
        acquireUninterruptibly() {
            void acquire(Semaphore s) {
                s.acquireUninterruptibly();
            }
        },
        acquireUninterruptiblyN() {
            void acquire(Semaphore s, int permits) {
                s.acquireUninterruptibly(permits);
            }
        },
        tryAcquire() {
            void acquire(Semaphore s) {
                assertTrue(s.tryAcquire());
            }
        },
        tryAcquireN() {
            void acquire(Semaphore s, int permits) {
                assertTrue(s.tryAcquire(permits));
            }
        },
        tryAcquireTimed() {
            void acquire(Semaphore s) throws InterruptedException {
                assertTrue(s.tryAcquire(2 * LONG_DELAY_MS, MILLISECONDS));
            }
            Thread.State parkedState() { return Thread.State.TIMED_WAITING; }
        },
        tryAcquireTimedN {
            void acquire(Semaphore s, int permits) throws InterruptedException {
                assertTrue(s.tryAcquire(permits, 2 * LONG_DELAY_MS, MILLISECONDS));
            }
            Thread.State parkedState() { return Thread.State.TIMED_WAITING; }
        };

        // Intentionally meta-circular

        /** Acquires 1 permit. */
        void acquire(Semaphore s) throws InterruptedException {
            acquire(s, 1);
        }
        /** Acquires the given number of permits. */
        void acquire(Semaphore s, int permits) throws InterruptedException {
            for (int i = 0; i < permits; i++)
                acquire(s);
        }
        Thread.State parkedState() { return Thread.State.WAITING; }
    }

    /**
     * Zero, negative, and positive initial values are allowed in constructor
     */
    public void testConstructor()      { testConstructor(false); }
    public void testConstructor_fair() { testConstructor(true); }
    public void testConstructor(boolean fair) {
        for (int permits : new int[] { -42, -1, 0, 1, 42 }) {
            Semaphore s = new Semaphore(permits, fair);
            assertEquals(permits, s.availablePermits());
            assertEquals(fair, s.isFair());
        }
    }

    /**
     * Constructor without fairness argument behaves as nonfair
     */
    public void testConstructorDefaultsToNonFair() {
        for (int permits : new int[] { -42, -1, 0, 1, 42 }) {
            Semaphore s = new Semaphore(permits);
            assertEquals(permits, s.availablePermits());
            assertFalse(s.isFair());
        }
    }

    /**
     * tryAcquire succeeds when sufficient permits, else fails
     */
    public void testTryAcquireInSameThread()      { testTryAcquireInSameThread(false); }
    public void testTryAcquireInSameThread_fair() { testTryAcquireInSameThread(true); }
    public void testTryAcquireInSameThread(boolean fair) {
        Semaphore s = new Semaphore(2, fair);
        assertEquals(2, s.availablePermits());
        assertTrue(s.tryAcquire());
        assertTrue(s.tryAcquire());
        assertEquals(0, s.availablePermits());
        assertFalse(s.tryAcquire());
        assertFalse(s.tryAcquire());
        assertEquals(0, s.availablePermits());
    }

    /**
     * timed tryAcquire times out
     */
    public void testTryAcquire_timeout() throws InterruptedException {
        final boolean fair = randomBoolean();
        final Semaphore s = new Semaphore(0, fair);
        final long startTime = System.nanoTime();
        assertFalse(s.tryAcquire(timeoutMillis(), MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
    }

    /**
     * timed tryAcquire(N) times out
     */
    public void testTryAcquireN_timeout() throws InterruptedException {
        final boolean fair = randomBoolean();
        final Semaphore s = new Semaphore(2, fair);
        final long startTime = System.nanoTime();
        assertFalse(s.tryAcquire(3, timeoutMillis(), MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
    }

    /**
     * acquire(), acquire(N), timed tryAcquired, timed tryAcquire(N)
     * are interruptible
     */
    public void testInterruptible_acquire()               { testInterruptible(false, AcquireMethod.acquire); }
    public void testInterruptible_acquire_fair()          { testInterruptible(true,  AcquireMethod.acquire); }
    public void testInterruptible_acquireN()              { testInterruptible(false, AcquireMethod.acquireN); }
    public void testInterruptible_acquireN_fair()         { testInterruptible(true,  AcquireMethod.acquireN); }
    public void testInterruptible_tryAcquireTimed()       { testInterruptible(false, AcquireMethod.tryAcquireTimed); }
    public void testInterruptible_tryAcquireTimed_fair()  { testInterruptible(true,  AcquireMethod.tryAcquireTimed); }
    public void testInterruptible_tryAcquireTimedN()      { testInterruptible(false, AcquireMethod.tryAcquireTimedN); }
    public void testInterruptible_tryAcquireTimedN_fair() { testInterruptible(true,  AcquireMethod.tryAcquireTimedN); }
    public void testInterruptible(boolean fair, final AcquireMethod acquirer) {
        final PublicSemaphore s = new PublicSemaphore(0, fair);
        final java.util.concurrent.CyclicBarrier pleaseInterrupt
            = new java.util.concurrent.CyclicBarrier(2);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                // Interrupt before acquire
                Thread.currentThread().interrupt();
                try {
                    acquirer.acquire(s);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                // Interrupt before acquire(N)
                Thread.currentThread().interrupt();
                try {
                    acquirer.acquire(s, 3);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                // Interrupt during acquire
                await(pleaseInterrupt);
                try {
                    acquirer.acquire(s);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                // Interrupt during acquire(N)
                await(pleaseInterrupt);
                try {
                    acquirer.acquire(s, 3);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        for (int n = 2; n-->0; ) {
            await(pleaseInterrupt);
            assertThreadBlocks(t, acquirer.parkedState());
            t.interrupt();
        }

        awaitTermination(t);
    }

    /**
     * acquireUninterruptibly(), acquireUninterruptibly(N) are
     * uninterruptible
     */
    public void testUninterruptible_acquireUninterruptibly()       { testUninterruptible(false, AcquireMethod.acquireUninterruptibly); }
    public void testUninterruptible_acquireUninterruptibly_fair()  { testUninterruptible(true,  AcquireMethod.acquireUninterruptibly); }
    public void testUninterruptible_acquireUninterruptiblyN()      { testUninterruptible(false, AcquireMethod.acquireUninterruptiblyN); }
    public void testUninterruptible_acquireUninterruptiblyN_fair() { testUninterruptible(true,  AcquireMethod.acquireUninterruptiblyN); }
    public void testUninterruptible(boolean fair, final AcquireMethod acquirer) {
        final PublicSemaphore s = new PublicSemaphore(0, fair);
        final Semaphore pleaseInterrupt = new Semaphore(-1, fair);

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                // Interrupt before acquire
                pleaseInterrupt.release();
                Thread.currentThread().interrupt();
                acquirer.acquire(s);
                assertTrue(Thread.interrupted());
            }});

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                // Interrupt during acquire
                pleaseInterrupt.release();
                acquirer.acquire(s);
                assertTrue(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        waitForQueuedThread(s, t1);
        waitForQueuedThread(s, t2);
        t2.interrupt();

        assertThreadBlocks(t1, Thread.State.WAITING);
        assertThreadBlocks(t2, Thread.State.WAITING);

        s.release(2);

        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * hasQueuedThreads reports whether there are waiting threads
     */
    public void testHasQueuedThreads()      { testHasQueuedThreads(false); }
    public void testHasQueuedThreads_fair() { testHasQueuedThreads(true); }
    public void testHasQueuedThreads(boolean fair) {
        final PublicSemaphore lock = new PublicSemaphore(1, fair);
        assertFalse(lock.hasQueuedThreads());
        lock.acquireUninterruptibly();
        Thread t1 = newStartedThread(new InterruptedLockRunnable(lock));
        waitForQueuedThread(lock, t1);
        assertTrue(lock.hasQueuedThreads());
        Thread t2 = newStartedThread(new InterruptibleLockRunnable(lock));
        waitForQueuedThread(lock, t2);
        assertTrue(lock.hasQueuedThreads());
        t1.interrupt();
        awaitTermination(t1);
        assertTrue(lock.hasQueuedThreads());
        lock.release();
        awaitTermination(t2);
        assertFalse(lock.hasQueuedThreads());
    }

    /**
     * getQueueLength reports number of waiting threads
     */
    public void testGetQueueLength()      { testGetQueueLength(false); }
    public void testGetQueueLength_fair() { testGetQueueLength(true); }
    public void testGetQueueLength(boolean fair) {
        final PublicSemaphore lock = new PublicSemaphore(1, fair);
        assertEquals(0, lock.getQueueLength());
        lock.acquireUninterruptibly();
        Thread t1 = newStartedThread(new InterruptedLockRunnable(lock));
        waitForQueuedThread(lock, t1);
        assertEquals(1, lock.getQueueLength());
        Thread t2 = newStartedThread(new InterruptibleLockRunnable(lock));
        waitForQueuedThread(lock, t2);
        assertEquals(2, lock.getQueueLength());
        t1.interrupt();
        awaitTermination(t1);
        assertEquals(1, lock.getQueueLength());
        lock.release();
        awaitTermination(t2);
        assertEquals(0, lock.getQueueLength());
    }

    /**
     * getQueuedThreads includes waiting threads
     */
    public void testGetQueuedThreads()      { testGetQueuedThreads(false); }
    public void testGetQueuedThreads_fair() { testGetQueuedThreads(true); }
    public void testGetQueuedThreads(boolean fair) {
        final PublicSemaphore lock = new PublicSemaphore(1, fair);
        assertTrue(lock.getQueuedThreads().isEmpty());
        lock.acquireUninterruptibly();
        assertTrue(lock.getQueuedThreads().isEmpty());
        Thread t1 = newStartedThread(new InterruptedLockRunnable(lock));
        waitForQueuedThread(lock, t1);
        assertTrue(lock.getQueuedThreads().contains(t1));
        Thread t2 = newStartedThread(new InterruptibleLockRunnable(lock));
        waitForQueuedThread(lock, t2);
        assertTrue(lock.getQueuedThreads().contains(t1));
        assertTrue(lock.getQueuedThreads().contains(t2));
        t1.interrupt();
        awaitTermination(t1);
        assertFalse(lock.getQueuedThreads().contains(t1));
        assertTrue(lock.getQueuedThreads().contains(t2));
        lock.release();
        awaitTermination(t2);
        assertTrue(lock.getQueuedThreads().isEmpty());
    }

    /**
     * drainPermits reports and removes given number of permits
     */
    public void testDrainPermits()      { testDrainPermits(false); }
    public void testDrainPermits_fair() { testDrainPermits(true); }
    public void testDrainPermits(boolean fair) {
        Semaphore s = new Semaphore(0, fair);
        assertEquals(0, s.availablePermits());
        assertEquals(0, s.drainPermits());
        s.release(10);
        assertEquals(10, s.availablePermits());
        assertEquals(10, s.drainPermits());
        assertEquals(0, s.availablePermits());
        assertEquals(0, s.drainPermits());
    }

    /**
     * release(-N) throws IllegalArgumentException
     */
    public void testReleaseIAE()      { testReleaseIAE(false); }
    public void testReleaseIAE_fair() { testReleaseIAE(true); }
    public void testReleaseIAE(boolean fair) {
        Semaphore s = new Semaphore(10, fair);
        try {
            s.release(-1);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * reducePermits(-N) throws IllegalArgumentException
     */
    public void testReducePermitsIAE()      { testReducePermitsIAE(false); }
    public void testReducePermitsIAE_fair() { testReducePermitsIAE(true); }
    public void testReducePermitsIAE(boolean fair) {
        PublicSemaphore s = new PublicSemaphore(10, fair);
        try {
            s.reducePermits(-1);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * reducePermits reduces number of permits
     */
    public void testReducePermits()      { testReducePermits(false); }
    public void testReducePermits_fair() { testReducePermits(true); }
    public void testReducePermits(boolean fair) {
        PublicSemaphore s = new PublicSemaphore(10, fair);
        assertEquals(10, s.availablePermits());
        s.reducePermits(0);
        assertEquals(10, s.availablePermits());
        s.reducePermits(1);
        assertEquals(9, s.availablePermits());
        s.reducePermits(10);
        assertEquals(-1, s.availablePermits());
        s.reducePermits(10);
        assertEquals(-11, s.availablePermits());
        s.reducePermits(0);
        assertEquals(-11, s.availablePermits());
    }

    /**
     * a reserialized semaphore has same number of permits and
     * fairness, but no queued threads
     */
    public void testSerialization()      { testSerialization(false); }
    public void testSerialization_fair() { testSerialization(true); }
    public void testSerialization(boolean fair) {
        try {
            Semaphore s = new Semaphore(3, fair);
            s.acquire();
            s.acquire();
            s.release();

            Semaphore clone = serialClone(s);
            assertEquals(fair, s.isFair());
            assertEquals(fair, clone.isFair());
            assertEquals(2, s.availablePermits());
            assertEquals(2, clone.availablePermits());
            clone.acquire();
            clone.acquire();
            clone.release();
            assertEquals(2, s.availablePermits());
            assertEquals(1, clone.availablePermits());
            assertFalse(s.hasQueuedThreads());
            assertFalse(clone.hasQueuedThreads());
        } catch (InterruptedException e) { threadUnexpectedException(e); }

        {
            PublicSemaphore s = new PublicSemaphore(0, fair);
            Thread t = newStartedThread(new InterruptibleLockRunnable(s));
            // waitForQueuedThreads(s); // suffers from "flicker", so ...
            waitForQueuedThread(s, t);  // ... we use this instead
            PublicSemaphore clone = serialClone(s);
            assertEquals(fair, s.isFair());
            assertEquals(fair, clone.isFair());
            assertEquals(0, s.availablePermits());
            assertEquals(0, clone.availablePermits());
            assertTrue(s.hasQueuedThreads());
            assertFalse(clone.hasQueuedThreads());
            s.release();
            awaitTermination(t);
            assertFalse(s.hasQueuedThreads());
            assertFalse(clone.hasQueuedThreads());
        }
    }

    /**
     * tryAcquire(n) succeeds when sufficient permits, else fails
     */
    public void testTryAcquireNInSameThread()      { testTryAcquireNInSameThread(false); }
    public void testTryAcquireNInSameThread_fair() { testTryAcquireNInSameThread(true); }
    public void testTryAcquireNInSameThread(boolean fair) {
        Semaphore s = new Semaphore(2, fair);
        assertEquals(2, s.availablePermits());
        assertFalse(s.tryAcquire(3));
        assertEquals(2, s.availablePermits());
        assertTrue(s.tryAcquire(2));
        assertEquals(0, s.availablePermits());
        assertFalse(s.tryAcquire(1));
        assertFalse(s.tryAcquire(2));
        assertEquals(0, s.availablePermits());
    }

    /**
     * acquire succeeds if permits available
     */
    public void testReleaseAcquireSameThread_acquire()       { testReleaseAcquireSameThread(false, AcquireMethod.acquire); }
    public void testReleaseAcquireSameThread_acquire_fair()  { testReleaseAcquireSameThread(true, AcquireMethod.acquire); }
    public void testReleaseAcquireSameThread_acquireN()      { testReleaseAcquireSameThread(false, AcquireMethod.acquireN); }
    public void testReleaseAcquireSameThread_acquireN_fair() { testReleaseAcquireSameThread(true, AcquireMethod.acquireN); }
    public void testReleaseAcquireSameThread_acquireUninterruptibly()       { testReleaseAcquireSameThread(false, AcquireMethod.acquireUninterruptibly); }
    public void testReleaseAcquireSameThread_acquireUninterruptibly_fair()  { testReleaseAcquireSameThread(true, AcquireMethod.acquireUninterruptibly); }
    public void testReleaseAcquireSameThread_acquireUninterruptiblyN()      { testReleaseAcquireSameThread(false, AcquireMethod.acquireUninterruptibly); }
    public void testReleaseAcquireSameThread_acquireUninterruptiblyN_fair() { testReleaseAcquireSameThread(true, AcquireMethod.acquireUninterruptibly); }
    public void testReleaseAcquireSameThread_tryAcquire()       { testReleaseAcquireSameThread(false, AcquireMethod.tryAcquire); }
    public void testReleaseAcquireSameThread_tryAcquire_fair()  { testReleaseAcquireSameThread(true, AcquireMethod.tryAcquire); }
    public void testReleaseAcquireSameThread_tryAcquireN()      { testReleaseAcquireSameThread(false, AcquireMethod.tryAcquireN); }
    public void testReleaseAcquireSameThread_tryAcquireN_fair() { testReleaseAcquireSameThread(true, AcquireMethod.tryAcquireN); }
    public void testReleaseAcquireSameThread_tryAcquireTimed()       { testReleaseAcquireSameThread(false, AcquireMethod.tryAcquireTimed); }
    public void testReleaseAcquireSameThread_tryAcquireTimed_fair()  { testReleaseAcquireSameThread(true, AcquireMethod.tryAcquireTimed); }
    public void testReleaseAcquireSameThread_tryAcquireTimedN()      { testReleaseAcquireSameThread(false, AcquireMethod.tryAcquireTimedN); }
    public void testReleaseAcquireSameThread_tryAcquireTimedN_fair() { testReleaseAcquireSameThread(true, AcquireMethod.tryAcquireTimedN); }
    public void testReleaseAcquireSameThread(boolean fair,
                                             final AcquireMethod acquirer) {
        Semaphore s = new Semaphore(1, fair);
        for (int i = 1; i < 6; i++) {
            s.release(i);
            assertEquals(1 + i, s.availablePermits());
            try {
                acquirer.acquire(s, i);
            } catch (InterruptedException e) { threadUnexpectedException(e); }
            assertEquals(1, s.availablePermits());
        }
    }

    /**
     * release in one thread enables acquire in another thread
     */
    public void testReleaseAcquireDifferentThreads_acquire()       { testReleaseAcquireDifferentThreads(false, AcquireMethod.acquire); }
    public void testReleaseAcquireDifferentThreads_acquire_fair()  { testReleaseAcquireDifferentThreads(true, AcquireMethod.acquire); }
    public void testReleaseAcquireDifferentThreads_acquireN()      { testReleaseAcquireDifferentThreads(false, AcquireMethod.acquireN); }
    public void testReleaseAcquireDifferentThreads_acquireN_fair() { testReleaseAcquireDifferentThreads(true, AcquireMethod.acquireN); }
    public void testReleaseAcquireDifferentThreads_acquireUninterruptibly()       { testReleaseAcquireDifferentThreads(false, AcquireMethod.acquireUninterruptibly); }
    public void testReleaseAcquireDifferentThreads_acquireUninterruptibly_fair()  { testReleaseAcquireDifferentThreads(true, AcquireMethod.acquireUninterruptibly); }
    public void testReleaseAcquireDifferentThreads_acquireUninterruptiblyN()      { testReleaseAcquireDifferentThreads(false, AcquireMethod.acquireUninterruptibly); }
    public void testReleaseAcquireDifferentThreads_acquireUninterruptiblyN_fair() { testReleaseAcquireDifferentThreads(true, AcquireMethod.acquireUninterruptibly); }
    public void testReleaseAcquireDifferentThreads_tryAcquireTimed()       { testReleaseAcquireDifferentThreads(false, AcquireMethod.tryAcquireTimed); }
    public void testReleaseAcquireDifferentThreads_tryAcquireTimed_fair()  { testReleaseAcquireDifferentThreads(true, AcquireMethod.tryAcquireTimed); }
    public void testReleaseAcquireDifferentThreads_tryAcquireTimedN()      { testReleaseAcquireDifferentThreads(false, AcquireMethod.tryAcquireTimedN); }
    public void testReleaseAcquireDifferentThreads_tryAcquireTimedN_fair() { testReleaseAcquireDifferentThreads(true, AcquireMethod.tryAcquireTimedN); }
    public void testReleaseAcquireDifferentThreads(boolean fair,
                                                   final AcquireMethod acquirer) {
        final Semaphore s = new Semaphore(0, fair);
        final int rounds = 4;
        long startTime = System.nanoTime();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                for (int i = 0; i < rounds; i++) {
                    assertFalse(s.hasQueuedThreads());
                    if (i % 2 == 0)
                        acquirer.acquire(s);
                    else
                        acquirer.acquire(s, 3);
                }}});

        for (int i = 0; i < rounds; i++) {
            while (! (s.availablePermits() == 0 && s.hasQueuedThreads()))
                Thread.yield();
            assertTrue(t.isAlive());
            if (i % 2 == 0)
                s.release();
            else
                s.release(3);
        }
        awaitTermination(t);
        assertEquals(0, s.availablePermits());
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
    }

    /**
     * fair locks are strictly FIFO
     */
    public void testFairLocksFifo() {
        final PublicSemaphore s = new PublicSemaphore(1, true);
        final CountDownLatch pleaseRelease = new CountDownLatch(1);
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                // Will block; permits are available, but not three
                s.acquire(3);
            }});

        waitForQueuedThread(s, t1);

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                // Will fail, even though 1 permit is available
                assertFalse(
                    s.tryAcquire(randomExpiredTimeout(), randomTimeUnit()));
                assertFalse(
                    s.tryAcquire(1, randomExpiredTimeout(), randomTimeUnit()));

                // untimed tryAcquire will barge and succeed
                assertTrue(s.tryAcquire());
                s.release(2);
                assertTrue(s.tryAcquire(2));
                s.release();

                pleaseRelease.countDown();
                // Will queue up behind t1, even though 1 permit is available
                s.acquire();
            }});

        await(pleaseRelease);
        waitForQueuedThread(s, t2);
        s.release(2);
        awaitTermination(t1);
        assertTrue(t2.isAlive());
        s.release();
        awaitTermination(t2);
    }

    /**
     * toString indicates current number of permits
     */
    public void testToString()      { testToString(false); }
    public void testToString_fair() { testToString(true); }
    public void testToString(boolean fair) {
        PublicSemaphore s = new PublicSemaphore(0, fair);
        assertTrue(s.toString().contains("Permits = 0"));
        s.release();
        assertTrue(s.toString().contains("Permits = 1"));
        s.release(2);
        assertTrue(s.toString().contains("Permits = 3"));
        s.reducePermits(5);
        assertTrue(s.toString().contains("Permits = -2"));
    }

}
