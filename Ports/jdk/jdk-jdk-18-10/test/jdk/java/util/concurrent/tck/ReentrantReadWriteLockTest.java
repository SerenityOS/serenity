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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import junit.framework.Test;
import junit.framework.TestSuite;

@SuppressWarnings("WaitNotInLoop") // we implement spurious-wakeup freedom
public class ReentrantReadWriteLockTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ReentrantReadWriteLockTest.class);
    }

    /**
     * A runnable calling lockInterruptibly
     */
    class InterruptibleLockRunnable extends CheckedRunnable {
        final ReentrantReadWriteLock lock;
        InterruptibleLockRunnable(ReentrantReadWriteLock l) { lock = l; }
        public void realRun() throws InterruptedException {
            lock.writeLock().lockInterruptibly();
        }
    }

    /**
     * A runnable calling lockInterruptibly that expects to be
     * interrupted
     */
    class InterruptedLockRunnable extends CheckedInterruptedRunnable {
        final ReentrantReadWriteLock lock;
        InterruptedLockRunnable(ReentrantReadWriteLock l) { lock = l; }
        public void realRun() throws InterruptedException {
            lock.writeLock().lockInterruptibly();
        }
    }

    /**
     * Subclass to expose protected methods
     */
    static class PublicReentrantReadWriteLock extends ReentrantReadWriteLock {
        PublicReentrantReadWriteLock() { super(); }
        PublicReentrantReadWriteLock(boolean fair) { super(fair); }
        public Thread getOwner() {
            return super.getOwner();
        }
        public Collection<Thread> getQueuedThreads() {
            return super.getQueuedThreads();
        }
        public Collection<Thread> getWaitingThreads(Condition c) {
            return super.getWaitingThreads(c);
        }
    }

    /**
     * Releases write lock, checking that it had a hold count of 1.
     */
    void releaseWriteLock(PublicReentrantReadWriteLock lock) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        assertWriteLockedByMoi(lock);
        assertEquals(1, lock.getWriteHoldCount());
        writeLock.unlock();
        assertNotWriteLocked(lock);
    }

    /**
     * Spin-waits until lock.hasQueuedThread(t) becomes true.
     */
    void waitForQueuedThread(PublicReentrantReadWriteLock lock, Thread t) {
        long startTime = System.nanoTime();
        while (!lock.hasQueuedThread(t)) {
            if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                throw new AssertionError("timed out");
            Thread.yield();
        }
        assertTrue(t.isAlive());
        assertNotSame(t, lock.getOwner());
    }

    /**
     * Checks that lock is not write-locked.
     */
    void assertNotWriteLocked(PublicReentrantReadWriteLock lock) {
        assertFalse(lock.isWriteLocked());
        assertFalse(lock.isWriteLockedByCurrentThread());
        assertFalse(lock.writeLock().isHeldByCurrentThread());
        assertEquals(0, lock.getWriteHoldCount());
        assertEquals(0, lock.writeLock().getHoldCount());
        assertNull(lock.getOwner());
    }

    /**
     * Checks that lock is write-locked by the given thread.
     */
    void assertWriteLockedBy(PublicReentrantReadWriteLock lock, Thread t) {
        assertTrue(lock.isWriteLocked());
        assertSame(t, lock.getOwner());
        assertEquals(t == Thread.currentThread(),
                     lock.isWriteLockedByCurrentThread());
        assertEquals(t == Thread.currentThread(),
                     lock.writeLock().isHeldByCurrentThread());
        assertEquals(t == Thread.currentThread(),
                     lock.getWriteHoldCount() > 0);
        assertEquals(t == Thread.currentThread(),
                     lock.writeLock().getHoldCount() > 0);
        assertEquals(0, lock.getReadLockCount());
    }

    /**
     * Checks that lock is write-locked by the current thread.
     */
    void assertWriteLockedByMoi(PublicReentrantReadWriteLock lock) {
        assertWriteLockedBy(lock, Thread.currentThread());
    }

    /**
     * Checks that condition c has no waiters.
     */
    void assertHasNoWaiters(PublicReentrantReadWriteLock lock, Condition c) {
        assertHasWaiters(lock, c, new Thread[] {});
    }

    /**
     * Checks that condition c has exactly the given waiter threads.
     */
    void assertHasWaiters(PublicReentrantReadWriteLock lock, Condition c,
                          Thread... threads) {
        lock.writeLock().lock();
        assertEquals(threads.length > 0, lock.hasWaiters(c));
        assertEquals(threads.length, lock.getWaitQueueLength(c));
        assertEquals(threads.length == 0, lock.getWaitingThreads(c).isEmpty());
        assertEquals(threads.length, lock.getWaitingThreads(c).size());
        assertEquals(new HashSet<Thread>(lock.getWaitingThreads(c)),
                     new HashSet<Thread>(Arrays.asList(threads)));
        lock.writeLock().unlock();
    }

    enum AwaitMethod { await, awaitTimed, awaitNanos, awaitUntil }

    /**
     * Awaits condition "indefinitely" using the specified AwaitMethod.
     */
    void await(Condition c, AwaitMethod awaitMethod)
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
            assertTrue(nanosRemaining > timeoutNanos / 2);
            assertTrue(nanosRemaining <= timeoutNanos);
            break;
        case awaitUntil:
            assertTrue(c.awaitUntil(delayedDate(timeoutMillis)));
            break;
        default:
            throw new AssertionError();
        }
    }

    /**
     * Constructor sets given fairness, and is in unlocked state
     */
    public void testConstructor() {
        PublicReentrantReadWriteLock lock;

        lock = new PublicReentrantReadWriteLock();
        assertFalse(lock.isFair());
        assertNotWriteLocked(lock);
        assertEquals(0, lock.getReadLockCount());

        lock = new PublicReentrantReadWriteLock(true);
        assertTrue(lock.isFair());
        assertNotWriteLocked(lock);
        assertEquals(0, lock.getReadLockCount());

        lock = new PublicReentrantReadWriteLock(false);
        assertFalse(lock.isFair());
        assertNotWriteLocked(lock);
        assertEquals(0, lock.getReadLockCount());
    }

    /**
     * write-locking and read-locking an unlocked lock succeed
     */
    public void testLock()      { testLock(false); }
    public void testLock_fair() { testLock(true); }
    public void testLock(boolean fair) {
        PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        assertNotWriteLocked(lock);
        lock.writeLock().lock();
        assertWriteLockedByMoi(lock);
        lock.writeLock().unlock();
        assertNotWriteLocked(lock);
        assertEquals(0, lock.getReadLockCount());
        lock.readLock().lock();
        assertNotWriteLocked(lock);
        assertEquals(1, lock.getReadLockCount());
        lock.readLock().unlock();
        assertNotWriteLocked(lock);
        assertEquals(0, lock.getReadLockCount());
    }

    /**
     * getWriteHoldCount returns number of recursive holds
     */
    public void testGetWriteHoldCount()      { testGetWriteHoldCount(false); }
    public void testGetWriteHoldCount_fair() { testGetWriteHoldCount(true); }
    public void testGetWriteHoldCount(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        for (int i = 1; i <= SIZE; i++) {
            lock.writeLock().lock();
            assertEquals(i,lock.getWriteHoldCount());
        }
        for (int i = SIZE; i > 0; i--) {
            lock.writeLock().unlock();
            assertEquals(i - 1,lock.getWriteHoldCount());
        }
    }

    /**
     * writelock.getHoldCount returns number of recursive holds
     */
    public void testGetHoldCount()      { testGetHoldCount(false); }
    public void testGetHoldCount_fair() { testGetHoldCount(true); }
    public void testGetHoldCount(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        for (int i = 1; i <= SIZE; i++) {
            lock.writeLock().lock();
            assertEquals(i,lock.writeLock().getHoldCount());
        }
        for (int i = SIZE; i > 0; i--) {
            lock.writeLock().unlock();
            assertEquals(i - 1,lock.writeLock().getHoldCount());
        }
    }

    /**
     * getReadHoldCount returns number of recursive holds
     */
    public void testGetReadHoldCount()      { testGetReadHoldCount(false); }
    public void testGetReadHoldCount_fair() { testGetReadHoldCount(true); }
    public void testGetReadHoldCount(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        for (int i = 1; i <= SIZE; i++) {
            lock.readLock().lock();
            assertEquals(i,lock.getReadHoldCount());
        }
        for (int i = SIZE; i > 0; i--) {
            lock.readLock().unlock();
            assertEquals(i - 1,lock.getReadHoldCount());
        }
    }

    /**
     * write-unlocking an unlocked lock throws IllegalMonitorStateException
     */
    public void testWriteUnlock_IMSE()      { testWriteUnlock_IMSE(false); }
    public void testWriteUnlock_IMSE_fair() { testWriteUnlock_IMSE(true); }
    public void testWriteUnlock_IMSE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        try {
            lock.writeLock().unlock();
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * read-unlocking an unlocked lock throws IllegalMonitorStateException
     */
    public void testReadUnlock_IMSE()      { testReadUnlock_IMSE(false); }
    public void testReadUnlock_IMSE_fair() { testReadUnlock_IMSE(true); }
    public void testReadUnlock_IMSE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        try {
            lock.readLock().unlock();
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * write-lockInterruptibly is interruptible
     */
    public void testWriteLockInterruptibly_Interruptible()      { testWriteLockInterruptibly_Interruptible(false); }
    public void testWriteLockInterruptibly_Interruptible_fair() { testWriteLockInterruptibly_Interruptible(true); }
    public void testWriteLockInterruptibly_Interruptible(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lockInterruptibly();
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * timed write-tryLock is interruptible
     */
    public void testWriteTryLock_Interruptible()      { testWriteTryLock_Interruptible(false); }
    public void testWriteTryLock_Interruptible_fair() { testWriteTryLock_Interruptible(true); }
    public void testWriteTryLock_Interruptible(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().tryLock(2 * LONG_DELAY_MS, MILLISECONDS);
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * read-lockInterruptibly is interruptible
     */
    public void testReadLockInterruptibly_Interruptible()      { testReadLockInterruptibly_Interruptible(false); }
    public void testReadLockInterruptibly_Interruptible_fair() { testReadLockInterruptibly_Interruptible(true); }
    public void testReadLockInterruptibly_Interruptible(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.readLock().lockInterruptibly();
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * timed read-tryLock is interruptible
     */
    public void testReadTryLock_Interruptible()      { testReadTryLock_Interruptible(false); }
    public void testReadTryLock_Interruptible_fair() { testReadTryLock_Interruptible(true); }
    public void testReadTryLock_Interruptible(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.readLock().tryLock(2 * LONG_DELAY_MS, MILLISECONDS);
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * write-tryLock on an unlocked lock succeeds
     */
    public void testWriteTryLock()      { testWriteTryLock(false); }
    public void testWriteTryLock_fair() { testWriteTryLock(true); }
    public void testWriteTryLock(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        assertTrue(lock.writeLock().tryLock());
        assertWriteLockedByMoi(lock);
        assertTrue(lock.writeLock().tryLock());
        assertWriteLockedByMoi(lock);
        lock.writeLock().unlock();
        releaseWriteLock(lock);
    }

    /**
     * write-tryLock fails if locked
     */
    public void testWriteTryLockWhenLocked()      { testWriteTryLockWhenLocked(false); }
    public void testWriteTryLockWhenLocked_fair() { testWriteTryLockWhenLocked(true); }
    public void testWriteTryLockWhenLocked(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertFalse(lock.writeLock().tryLock());
            }});

        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * read-tryLock fails if locked
     */
    public void testReadTryLockWhenLocked()      { testReadTryLockWhenLocked(false); }
    public void testReadTryLockWhenLocked_fair() { testReadTryLockWhenLocked(true); }
    public void testReadTryLockWhenLocked(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertFalse(lock.readLock().tryLock());
            }});

        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * Multiple threads can hold a read lock when not write-locked
     */
    public void testMultipleReadLocks()      { testMultipleReadLocks(false); }
    public void testMultipleReadLocks_fair() { testMultipleReadLocks(true); }
    public void testMultipleReadLocks(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        lock.readLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                assertTrue(lock.readLock().tryLock());
                lock.readLock().unlock();
                assertTrue(lock.readLock().tryLock(LONG_DELAY_MS, MILLISECONDS));
                lock.readLock().unlock();
                lock.readLock().lock();
                lock.readLock().unlock();
            }});

        awaitTermination(t);
        lock.readLock().unlock();
    }

    /**
     * A writelock succeeds only after a reading thread unlocks
     */
    public void testWriteAfterReadLock()      { testWriteAfterReadLock(false); }
    public void testWriteAfterReadLock_fair() { testWriteAfterReadLock(true); }
    public void testWriteAfterReadLock(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.readLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(1, lock.getReadLockCount());
                lock.writeLock().lock();
                assertEquals(0, lock.getReadLockCount());
                lock.writeLock().unlock();
            }});
        waitForQueuedThread(lock, t);
        assertNotWriteLocked(lock);
        assertEquals(1, lock.getReadLockCount());
        lock.readLock().unlock();
        assertEquals(0, lock.getReadLockCount());
        awaitTermination(t);
        assertNotWriteLocked(lock);
    }

    /**
     * A writelock succeeds only after reading threads unlock
     */
    public void testWriteAfterMultipleReadLocks()      { testWriteAfterMultipleReadLocks(false); }
    public void testWriteAfterMultipleReadLocks_fair() { testWriteAfterMultipleReadLocks(true); }
    public void testWriteAfterMultipleReadLocks(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.readLock().lock();
        lock.readLock().lock();
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().lock();
                assertEquals(3, lock.getReadLockCount());
                lock.readLock().unlock();
            }});
        awaitTermination(t1);

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(2, lock.getReadLockCount());
                lock.writeLock().lock();
                assertEquals(0, lock.getReadLockCount());
                lock.writeLock().unlock();
            }});
        waitForQueuedThread(lock, t2);
        assertNotWriteLocked(lock);
        assertEquals(2, lock.getReadLockCount());
        lock.readLock().unlock();
        lock.readLock().unlock();
        assertEquals(0, lock.getReadLockCount());
        awaitTermination(t2);
        assertNotWriteLocked(lock);
    }

    /**
     * A thread that tries to acquire a fair read lock (non-reentrantly)
     * will block if there is a waiting writer thread
     */
    public void testReaderWriterReaderFairFifo() {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(true);
        final AtomicBoolean t1GotLock = new AtomicBoolean(false);

        lock.readLock().lock();
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(1, lock.getReadLockCount());
                lock.writeLock().lock();
                assertEquals(0, lock.getReadLockCount());
                t1GotLock.set(true);
                lock.writeLock().unlock();
            }});
        waitForQueuedThread(lock, t1);

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(1, lock.getReadLockCount());
                lock.readLock().lock();
                assertEquals(1, lock.getReadLockCount());
                assertTrue(t1GotLock.get());
                lock.readLock().unlock();
            }});
        waitForQueuedThread(lock, t2);
        assertTrue(t1.isAlive());
        assertNotWriteLocked(lock);
        assertEquals(1, lock.getReadLockCount());
        lock.readLock().unlock();
        awaitTermination(t1);
        awaitTermination(t2);
        assertNotWriteLocked(lock);
    }

    /**
     * Readlocks succeed only after a writing thread unlocks
     */
    public void testReadAfterWriteLock()      { testReadAfterWriteLock(false); }
    public void testReadAfterWriteLock_fair() { testReadAfterWriteLock(true); }
    public void testReadAfterWriteLock(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().lock();
                lock.readLock().unlock();
            }});
        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().lock();
                lock.readLock().unlock();
            }});

        waitForQueuedThread(lock, t1);
        waitForQueuedThread(lock, t2);
        releaseWriteLock(lock);
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * Read trylock succeeds if write locked by current thread
     */
    public void testReadHoldingWriteLock()      { testReadHoldingWriteLock(false); }
    public void testReadHoldingWriteLock_fair() { testReadHoldingWriteLock(true); }
    public void testReadHoldingWriteLock(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        assertTrue(lock.readLock().tryLock());
        lock.readLock().unlock();
        lock.writeLock().unlock();
    }

    /**
     * Read trylock succeeds (barging) even in the presence of waiting
     * readers and/or writers
     */
    public void testReadTryLockBarging()      { testReadTryLockBarging(false); }
    public void testReadTryLockBarging_fair() { testReadTryLockBarging(true); }
    public void testReadTryLockBarging(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.readLock().lock();

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.writeLock().lock();
                lock.writeLock().unlock();
            }});

        waitForQueuedThread(lock, t1);

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().lock();
                lock.readLock().unlock();
            }});

        if (fair)
            waitForQueuedThread(lock, t2);

        Thread t3 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().tryLock();
                lock.readLock().unlock();
            }});

        assertTrue(lock.getReadLockCount() > 0);
        awaitTermination(t3);
        assertTrue(t1.isAlive());
        if (fair) assertTrue(t2.isAlive());
        lock.readLock().unlock();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * Read lock succeeds if write locked by current thread even if
     * other threads are waiting for readlock
     */
    public void testReadHoldingWriteLock2()      { testReadHoldingWriteLock2(false); }
    public void testReadHoldingWriteLock2_fair() { testReadHoldingWriteLock2(true); }
    public void testReadHoldingWriteLock2(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        lock.readLock().lock();
        lock.readLock().unlock();

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().lock();
                lock.readLock().unlock();
            }});
        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.readLock().lock();
                lock.readLock().unlock();
            }});

        waitForQueuedThread(lock, t1);
        waitForQueuedThread(lock, t2);
        assertWriteLockedByMoi(lock);
        lock.readLock().lock();
        lock.readLock().unlock();
        releaseWriteLock(lock);
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * Read lock succeeds if write locked by current thread even if
     * other threads are waiting for writelock
     */
    public void testReadHoldingWriteLock3()      { testReadHoldingWriteLock3(false); }
    public void testReadHoldingWriteLock3_fair() { testReadHoldingWriteLock3(true); }
    public void testReadHoldingWriteLock3(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        lock.readLock().lock();
        lock.readLock().unlock();

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.writeLock().lock();
                lock.writeLock().unlock();
            }});
        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.writeLock().lock();
                lock.writeLock().unlock();
            }});

        waitForQueuedThread(lock, t1);
        waitForQueuedThread(lock, t2);
        assertWriteLockedByMoi(lock);
        lock.readLock().lock();
        lock.readLock().unlock();
        assertWriteLockedByMoi(lock);
        lock.writeLock().unlock();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * Write lock succeeds if write locked by current thread even if
     * other threads are waiting for writelock
     */
    public void testWriteHoldingWriteLock4()      { testWriteHoldingWriteLock4(false); }
    public void testWriteHoldingWriteLock4_fair() { testWriteHoldingWriteLock4(true); }
    public void testWriteHoldingWriteLock4(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        lock.writeLock().lock();
        lock.writeLock().unlock();

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.writeLock().lock();
                lock.writeLock().unlock();
            }});
        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                lock.writeLock().lock();
                lock.writeLock().unlock();
            }});

        waitForQueuedThread(lock, t1);
        waitForQueuedThread(lock, t2);
        assertWriteLockedByMoi(lock);
        assertEquals(1, lock.getWriteHoldCount());
        lock.writeLock().lock();
        assertWriteLockedByMoi(lock);
        assertEquals(2, lock.getWriteHoldCount());
        lock.writeLock().unlock();
        assertWriteLockedByMoi(lock);
        assertEquals(1, lock.getWriteHoldCount());
        lock.writeLock().unlock();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * Read tryLock succeeds if readlocked but not writelocked
     */
    public void testTryLockWhenReadLocked()      { testTryLockWhenReadLocked(false); }
    public void testTryLockWhenReadLocked_fair() { testTryLockWhenReadLocked(true); }
    public void testTryLockWhenReadLocked(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        lock.readLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertTrue(lock.readLock().tryLock());
                lock.readLock().unlock();
            }});

        awaitTermination(t);
        lock.readLock().unlock();
    }

    /**
     * write tryLock fails when readlocked
     */
    public void testWriteTryLockWhenReadLocked()      { testWriteTryLockWhenReadLocked(false); }
    public void testWriteTryLockWhenReadLocked_fair() { testWriteTryLockWhenReadLocked(true); }
    public void testWriteTryLockWhenReadLocked(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        lock.readLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertFalse(lock.writeLock().tryLock());
            }});

        awaitTermination(t);
        lock.readLock().unlock();
    }

    /**
     * write timed tryLock times out if locked
     */
    public void testWriteTryLock_Timeout()      { testWriteTryLock_Timeout(false); }
    public void testWriteTryLock_Timeout_fair() { testWriteTryLock_Timeout(true); }
    public void testWriteTryLock_Timeout(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final long timeoutMillis = timeoutMillis();
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertFalse(lock.writeLock().tryLock(timeoutMillis, MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
            }});

        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * read timed tryLock times out if write-locked
     */
    public void testReadTryLock_Timeout()      { testReadTryLock_Timeout(false); }
    public void testReadTryLock_Timeout_fair() { testReadTryLock_Timeout(true); }
    public void testReadTryLock_Timeout(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                long timeoutMillis = timeoutMillis();
                assertFalse(lock.readLock().tryLock(timeoutMillis, MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
            }});

        awaitTermination(t);
        assertTrue(lock.writeLock().isHeldByCurrentThread());
        lock.writeLock().unlock();
    }

    /**
     * write lockInterruptibly succeeds if unlocked, else is interruptible
     */
    public void testWriteLockInterruptibly()      { testWriteLockInterruptibly(false); }
    public void testWriteLockInterruptibly_fair() { testWriteLockInterruptibly(true); }
    public void testWriteLockInterruptibly(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        try {
            lock.writeLock().lockInterruptibly();
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lockInterruptibly();
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        assertTrue(lock.writeLock().isHeldByCurrentThread());
        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * read lockInterruptibly succeeds if lock free else is interruptible
     */
    public void testReadLockInterruptibly()      { testReadLockInterruptibly(false); }
    public void testReadLockInterruptibly_fair() { testReadLockInterruptibly(true); }
    public void testReadLockInterruptibly(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        try {
            lock.readLock().lockInterruptibly();
            lock.readLock().unlock();
            lock.writeLock().lockInterruptibly();
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.readLock().lockInterruptibly();
            }});

        waitForQueuedThread(lock, t);
        t.interrupt();
        awaitTermination(t);
        releaseWriteLock(lock);
    }

    /**
     * Calling await without holding lock throws IllegalMonitorStateException
     */
    public void testAwait_IMSE()      { testAwait_IMSE(false); }
    public void testAwait_IMSE_fair() { testAwait_IMSE(true); }
    public void testAwait_IMSE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        for (AwaitMethod awaitMethod : AwaitMethod.values()) {
            long startTime = System.nanoTime();
            try {
                await(c, awaitMethod);
                shouldThrow();
            } catch (IllegalMonitorStateException success) {
            } catch (InterruptedException fail) {
                threadUnexpectedException(fail);
            }
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
    }

    /**
     * Calling signal without holding lock throws IllegalMonitorStateException
     */
    public void testSignal_IMSE()      { testSignal_IMSE(false); }
    public void testSignal_IMSE_fair() { testSignal_IMSE(true); }
    public void testSignal_IMSE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        try {
            c.signal();
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * Calling signalAll without holding lock throws IllegalMonitorStateException
     */
    public void testSignalAll_IMSE()      { testSignalAll_IMSE(false); }
    public void testSignalAll_IMSE_fair() { testSignalAll_IMSE(true); }
    public void testSignalAll_IMSE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        try {
            c.signalAll();
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * awaitNanos without a signal times out
     */
    public void testAwaitNanos_Timeout()      { testAwaitNanos_Timeout(false); }
    public void testAwaitNanos_Timeout_fair() { testAwaitNanos_Timeout(true); }
    public void testAwaitNanos_Timeout(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final long timeoutMillis = timeoutMillis();
        lock.writeLock().lock();
        final long startTime = System.nanoTime();
        final long timeoutNanos = MILLISECONDS.toNanos(timeoutMillis);
        try {
            long nanosRemaining = c.awaitNanos(timeoutNanos);
            assertTrue(nanosRemaining <= 0);
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
        lock.writeLock().unlock();
    }

    /**
     * timed await without a signal times out
     */
    public void testAwait_Timeout()      { testAwait_Timeout(false); }
    public void testAwait_Timeout_fair() { testAwait_Timeout(true); }
    public void testAwait_Timeout(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final long timeoutMillis = timeoutMillis();
        lock.writeLock().lock();
        final long startTime = System.nanoTime();
        try {
            assertFalse(c.await(timeoutMillis, MILLISECONDS));
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
        lock.writeLock().unlock();
    }

    /**
     * awaitUntil without a signal times out
     */
    public void testAwaitUntil_Timeout()      { testAwaitUntil_Timeout(false); }
    public void testAwaitUntil_Timeout_fair() { testAwaitUntil_Timeout(true); }
    public void testAwaitUntil_Timeout(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        lock.writeLock().lock();
        // We shouldn't assume that nanoTime and currentTimeMillis
        // use the same time source, so don't use nanoTime here.
        final java.util.Date delayedDate = delayedDate(timeoutMillis());
        try {
            assertFalse(c.awaitUntil(delayedDate));
        } catch (InterruptedException fail) { threadUnexpectedException(fail); }
        assertTrue(new java.util.Date().getTime() >= delayedDate.getTime());
        lock.writeLock().unlock();
    }

    /**
     * await returns when signalled
     */
    public void testAwait()      { testAwait(false); }
    public void testAwait_fair() { testAwait(true); }
    public void testAwait(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                locked.countDown();
                c.await();
                lock.writeLock().unlock();
            }});

        await(locked);
        lock.writeLock().lock();
        assertHasWaiters(lock, c, t);
        c.signal();
        assertHasNoWaiters(lock, c);
        assertTrue(t.isAlive());
        lock.writeLock().unlock();
        awaitTermination(t);
    }

    /**
     * awaitUninterruptibly is uninterruptible
     */
    public void testAwaitUninterruptibly()      { testAwaitUninterruptibly(false); }
    public void testAwaitUninterruptibly_fair() { testAwaitUninterruptibly(true); }
    public void testAwaitUninterruptibly(boolean fair) {
        final Lock lock = new ReentrantReadWriteLock(fair).writeLock();
        final Condition condition = lock.newCondition();
        final CountDownLatch pleaseInterrupt = new CountDownLatch(2);

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                // Interrupt before awaitUninterruptibly
                lock.lock();
                pleaseInterrupt.countDown();
                Thread.currentThread().interrupt();
                condition.awaitUninterruptibly();
                assertTrue(Thread.interrupted());
                lock.unlock();
            }});

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                // Interrupt during awaitUninterruptibly
                lock.lock();
                pleaseInterrupt.countDown();
                condition.awaitUninterruptibly();
                assertTrue(Thread.interrupted());
                lock.unlock();
            }});

        await(pleaseInterrupt);
        t2.interrupt();
        lock.lock();
        lock.unlock();
        assertThreadBlocks(t1, Thread.State.WAITING);
        assertThreadBlocks(t2, Thread.State.WAITING);

        lock.lock();
        condition.signalAll();
        lock.unlock();

        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * await/awaitNanos/awaitUntil is interruptible
     */
    public void testInterruptible_await()           { testInterruptible(false, AwaitMethod.await); }
    public void testInterruptible_await_fair()      { testInterruptible(true,  AwaitMethod.await); }
    public void testInterruptible_awaitTimed()      { testInterruptible(false, AwaitMethod.awaitTimed); }
    public void testInterruptible_awaitTimed_fair() { testInterruptible(true,  AwaitMethod.awaitTimed); }
    public void testInterruptible_awaitNanos()      { testInterruptible(false, AwaitMethod.awaitNanos); }
    public void testInterruptible_awaitNanos_fair() { testInterruptible(true,  AwaitMethod.awaitNanos); }
    public void testInterruptible_awaitUntil()      { testInterruptible(false, AwaitMethod.awaitUntil); }
    public void testInterruptible_awaitUntil_fair() { testInterruptible(true,  AwaitMethod.awaitUntil); }
    public void testInterruptible(boolean fair, final AwaitMethod awaitMethod) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                assertWriteLockedByMoi(lock);
                assertHasNoWaiters(lock, c);
                locked.countDown();
                try {
                    await(c, awaitMethod);
                } finally {
                    assertWriteLockedByMoi(lock);
                    assertHasNoWaiters(lock, c);
                    lock.writeLock().unlock();
                    assertFalse(Thread.interrupted());
                }
            }});

        await(locked);
        assertHasWaiters(lock, c, t);
        t.interrupt();
        awaitTermination(t);
        assertNotWriteLocked(lock);
    }

    /**
     * signalAll wakes up all threads
     */
    public void testSignalAll_await()           { testSignalAll(false, AwaitMethod.await); }
    public void testSignalAll_await_fair()      { testSignalAll(true,  AwaitMethod.await); }
    public void testSignalAll_awaitTimed()      { testSignalAll(false, AwaitMethod.awaitTimed); }
    public void testSignalAll_awaitTimed_fair() { testSignalAll(true,  AwaitMethod.awaitTimed); }
    public void testSignalAll_awaitNanos()      { testSignalAll(false, AwaitMethod.awaitNanos); }
    public void testSignalAll_awaitNanos_fair() { testSignalAll(true,  AwaitMethod.awaitNanos); }
    public void testSignalAll_awaitUntil()      { testSignalAll(false, AwaitMethod.awaitUntil); }
    public void testSignalAll_awaitUntil_fair() { testSignalAll(true,  AwaitMethod.awaitUntil); }
    public void testSignalAll(boolean fair, final AwaitMethod awaitMethod) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked = new CountDownLatch(2);
        final Lock writeLock = lock.writeLock();
        class Awaiter extends CheckedRunnable {
            public void realRun() throws InterruptedException {
                writeLock.lock();
                locked.countDown();
                await(c, awaitMethod);
                writeLock.unlock();
            }
        }

        Thread t1 = newStartedThread(new Awaiter());
        Thread t2 = newStartedThread(new Awaiter());

        await(locked);
        writeLock.lock();
        assertHasWaiters(lock, c, t1, t2);
        c.signalAll();
        assertHasNoWaiters(lock, c);
        writeLock.unlock();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * signal wakes up waiting threads in FIFO order
     */
    public void testSignalWakesFifo()      { testSignalWakesFifo(false); }
    public void testSignalWakesFifo_fair() { testSignalWakesFifo(true); }
    public void testSignalWakesFifo(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked1 = new CountDownLatch(1);
        final CountDownLatch locked2 = new CountDownLatch(1);
        final Lock writeLock = lock.writeLock();
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                writeLock.lock();
                locked1.countDown();
                c.await();
                writeLock.unlock();
            }});

        await(locked1);

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                writeLock.lock();
                locked2.countDown();
                c.await();
                writeLock.unlock();
            }});

        await(locked2);

        writeLock.lock();
        assertHasWaiters(lock, c, t1, t2);
        assertFalse(lock.hasQueuedThreads());
        c.signal();
        assertHasWaiters(lock, c, t2);
        assertTrue(lock.hasQueuedThread(t1));
        assertFalse(lock.hasQueuedThread(t2));
        c.signal();
        assertHasNoWaiters(lock, c);
        assertTrue(lock.hasQueuedThread(t1));
        assertTrue(lock.hasQueuedThread(t2));
        writeLock.unlock();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * await after multiple reentrant locking preserves lock count
     */
    public void testAwaitLockCount()      { testAwaitLockCount(false); }
    public void testAwaitLockCount_fair() { testAwaitLockCount(true); }
    public void testAwaitLockCount(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked = new CountDownLatch(2);
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                assertWriteLockedByMoi(lock);
                assertEquals(1, lock.writeLock().getHoldCount());
                locked.countDown();
                c.await();
                assertWriteLockedByMoi(lock);
                assertEquals(1, lock.writeLock().getHoldCount());
                lock.writeLock().unlock();
            }});

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                lock.writeLock().lock();
                assertWriteLockedByMoi(lock);
                assertEquals(2, lock.writeLock().getHoldCount());
                locked.countDown();
                c.await();
                assertWriteLockedByMoi(lock);
                assertEquals(2, lock.writeLock().getHoldCount());
                lock.writeLock().unlock();
                lock.writeLock().unlock();
            }});

        await(locked);
        lock.writeLock().lock();
        assertHasWaiters(lock, c, t1, t2);
        c.signalAll();
        assertHasNoWaiters(lock, c);
        lock.writeLock().unlock();
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * A serialized lock deserializes as unlocked
     */
    public void testSerialization()      { testSerialization(false); }
    public void testSerialization_fair() { testSerialization(true); }
    public void testSerialization(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        lock.writeLock().lock();
        lock.readLock().lock();

        ReentrantReadWriteLock clone = serialClone(lock);
        assertEquals(lock.isFair(), clone.isFair());
        assertTrue(lock.isWriteLocked());
        assertFalse(clone.isWriteLocked());
        assertEquals(1, lock.getReadLockCount());
        assertEquals(0, clone.getReadLockCount());
        clone.writeLock().lock();
        clone.readLock().lock();
        assertTrue(clone.isWriteLocked());
        assertEquals(1, clone.getReadLockCount());
        clone.readLock().unlock();
        clone.writeLock().unlock();
        assertFalse(clone.isWriteLocked());
        assertEquals(1, lock.getReadLockCount());
        assertEquals(0, clone.getReadLockCount());
    }

    /**
     * hasQueuedThreads reports whether there are waiting threads
     */
    public void testHasQueuedThreads()      { testHasQueuedThreads(false); }
    public void testHasQueuedThreads_fair() { testHasQueuedThreads(true); }
    public void testHasQueuedThreads(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        assertFalse(lock.hasQueuedThreads());
        lock.writeLock().lock();
        assertFalse(lock.hasQueuedThreads());
        t1.start();
        waitForQueuedThread(lock, t1);
        assertTrue(lock.hasQueuedThreads());
        t2.start();
        waitForQueuedThread(lock, t2);
        assertTrue(lock.hasQueuedThreads());
        t1.interrupt();
        awaitTermination(t1);
        assertTrue(lock.hasQueuedThreads());
        lock.writeLock().unlock();
        awaitTermination(t2);
        assertFalse(lock.hasQueuedThreads());
    }

    /**
     * hasQueuedThread(null) throws NPE
     */
    public void testHasQueuedThreadNPE()      { testHasQueuedThreadNPE(false); }
    public void testHasQueuedThreadNPE_fair() { testHasQueuedThreadNPE(true); }
    public void testHasQueuedThreadNPE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        try {
            lock.hasQueuedThread(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * hasQueuedThread reports whether a thread is queued
     */
    public void testHasQueuedThread()      { testHasQueuedThread(false); }
    public void testHasQueuedThread_fair() { testHasQueuedThread(true); }
    public void testHasQueuedThread(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        assertFalse(lock.hasQueuedThread(t1));
        assertFalse(lock.hasQueuedThread(t2));
        lock.writeLock().lock();
        t1.start();
        waitForQueuedThread(lock, t1);
        assertTrue(lock.hasQueuedThread(t1));
        assertFalse(lock.hasQueuedThread(t2));
        t2.start();
        waitForQueuedThread(lock, t2);
        assertTrue(lock.hasQueuedThread(t1));
        assertTrue(lock.hasQueuedThread(t2));
        t1.interrupt();
        awaitTermination(t1);
        assertFalse(lock.hasQueuedThread(t1));
        assertTrue(lock.hasQueuedThread(t2));
        lock.writeLock().unlock();
        awaitTermination(t2);
        assertFalse(lock.hasQueuedThread(t1));
        assertFalse(lock.hasQueuedThread(t2));
    }

    /**
     * getQueueLength reports number of waiting threads
     */
    public void testGetQueueLength()      { testGetQueueLength(false); }
    public void testGetQueueLength_fair() { testGetQueueLength(true); }
    public void testGetQueueLength(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        assertEquals(0, lock.getQueueLength());
        lock.writeLock().lock();
        t1.start();
        waitForQueuedThread(lock, t1);
        assertEquals(1, lock.getQueueLength());
        t2.start();
        waitForQueuedThread(lock, t2);
        assertEquals(2, lock.getQueueLength());
        t1.interrupt();
        awaitTermination(t1);
        assertEquals(1, lock.getQueueLength());
        lock.writeLock().unlock();
        awaitTermination(t2);
        assertEquals(0, lock.getQueueLength());
    }

    /**
     * getQueuedThreads includes waiting threads
     */
    public void testGetQueuedThreads()      { testGetQueuedThreads(false); }
    public void testGetQueuedThreads_fair() { testGetQueuedThreads(true); }
    public void testGetQueuedThreads(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        Thread t1 = new Thread(new InterruptedLockRunnable(lock));
        Thread t2 = new Thread(new InterruptibleLockRunnable(lock));
        assertTrue(lock.getQueuedThreads().isEmpty());
        lock.writeLock().lock();
        assertTrue(lock.getQueuedThreads().isEmpty());
        t1.start();
        waitForQueuedThread(lock, t1);
        assertEquals(1, lock.getQueuedThreads().size());
        assertTrue(lock.getQueuedThreads().contains(t1));
        t2.start();
        waitForQueuedThread(lock, t2);
        assertEquals(2, lock.getQueuedThreads().size());
        assertTrue(lock.getQueuedThreads().contains(t1));
        assertTrue(lock.getQueuedThreads().contains(t2));
        t1.interrupt();
        awaitTermination(t1);
        assertFalse(lock.getQueuedThreads().contains(t1));
        assertTrue(lock.getQueuedThreads().contains(t2));
        assertEquals(1, lock.getQueuedThreads().size());
        lock.writeLock().unlock();
        awaitTermination(t2);
        assertTrue(lock.getQueuedThreads().isEmpty());
    }

    /**
     * hasWaiters throws NPE if null
     */
    public void testHasWaitersNPE()      { testHasWaitersNPE(false); }
    public void testHasWaitersNPE_fair() { testHasWaitersNPE(true); }
    public void testHasWaitersNPE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        try {
            lock.hasWaiters(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * getWaitQueueLength throws NPE if null
     */
    public void testGetWaitQueueLengthNPE()      { testGetWaitQueueLengthNPE(false); }
    public void testGetWaitQueueLengthNPE_fair() { testGetWaitQueueLengthNPE(true); }
    public void testGetWaitQueueLengthNPE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        try {
            lock.getWaitQueueLength(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * getWaitingThreads throws NPE if null
     */
    public void testGetWaitingThreadsNPE()      { testGetWaitingThreadsNPE(false); }
    public void testGetWaitingThreadsNPE_fair() { testGetWaitingThreadsNPE(true); }
    public void testGetWaitingThreadsNPE(boolean fair) {
        final PublicReentrantReadWriteLock lock = new PublicReentrantReadWriteLock(fair);
        try {
            lock.getWaitingThreads(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * hasWaiters throws IllegalArgumentException if not owned
     */
    public void testHasWaitersIAE()      { testHasWaitersIAE(false); }
    public void testHasWaitersIAE_fair() { testHasWaitersIAE(true); }
    public void testHasWaitersIAE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final ReentrantReadWriteLock lock2 = new ReentrantReadWriteLock(fair);
        try {
            lock2.hasWaiters(c);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * hasWaiters throws IllegalMonitorStateException if not locked
     */
    public void testHasWaitersIMSE()      { testHasWaitersIMSE(false); }
    public void testHasWaitersIMSE_fair() { testHasWaitersIMSE(true); }
    public void testHasWaitersIMSE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        try {
            lock.hasWaiters(c);
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * getWaitQueueLength throws IllegalArgumentException if not owned
     */
    public void testGetWaitQueueLengthIAE()      { testGetWaitQueueLengthIAE(false); }
    public void testGetWaitQueueLengthIAE_fair() { testGetWaitQueueLengthIAE(true); }
    public void testGetWaitQueueLengthIAE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final ReentrantReadWriteLock lock2 = new ReentrantReadWriteLock(fair);
        try {
            lock2.getWaitQueueLength(c);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * getWaitQueueLength throws IllegalMonitorStateException if not locked
     */
    public void testGetWaitQueueLengthIMSE()      { testGetWaitQueueLengthIMSE(false); }
    public void testGetWaitQueueLengthIMSE_fair() { testGetWaitQueueLengthIMSE(true); }
    public void testGetWaitQueueLengthIMSE(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        try {
            lock.getWaitQueueLength(c);
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * getWaitingThreads throws IllegalArgumentException if not owned
     */
    public void testGetWaitingThreadsIAE()      { testGetWaitingThreadsIAE(false); }
    public void testGetWaitingThreadsIAE_fair() { testGetWaitingThreadsIAE(true); }
    public void testGetWaitingThreadsIAE(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final PublicReentrantReadWriteLock lock2 =
            new PublicReentrantReadWriteLock(fair);
        try {
            lock2.getWaitingThreads(c);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * getWaitingThreads throws IllegalMonitorStateException if not locked
     */
    public void testGetWaitingThreadsIMSE()      { testGetWaitingThreadsIMSE(false); }
    public void testGetWaitingThreadsIMSE_fair() { testGetWaitingThreadsIMSE(true); }
    public void testGetWaitingThreadsIMSE(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        try {
            lock.getWaitingThreads(c);
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }

    /**
     * hasWaiters returns true when a thread is waiting, else false
     */
    public void testHasWaiters()      { testHasWaiters(false); }
    public void testHasWaiters_fair() { testHasWaiters(true); }
    public void testHasWaiters(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                assertHasNoWaiters(lock, c);
                assertFalse(lock.hasWaiters(c));
                locked.countDown();
                c.await();
                assertHasNoWaiters(lock, c);
                assertFalse(lock.hasWaiters(c));
                lock.writeLock().unlock();
            }});

        await(locked);
        lock.writeLock().lock();
        assertHasWaiters(lock, c, t);
        assertTrue(lock.hasWaiters(c));
        c.signal();
        assertHasNoWaiters(lock, c);
        assertFalse(lock.hasWaiters(c));
        lock.writeLock().unlock();
        awaitTermination(t);
        assertHasNoWaiters(lock, c);
    }

    /**
     * getWaitQueueLength returns number of waiting threads
     */
    public void testGetWaitQueueLength()      { testGetWaitQueueLength(false); }
    public void testGetWaitQueueLength_fair() { testGetWaitQueueLength(true); }
    public void testGetWaitQueueLength(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                assertEquals(0, lock.getWaitQueueLength(c));
                locked.countDown();
                c.await();
                lock.writeLock().unlock();
            }});

        await(locked);
        lock.writeLock().lock();
        assertHasWaiters(lock, c, t);
        assertEquals(1, lock.getWaitQueueLength(c));
        c.signal();
        assertHasNoWaiters(lock, c);
        assertEquals(0, lock.getWaitQueueLength(c));
        lock.writeLock().unlock();
        awaitTermination(t);
    }

    /**
     * getWaitingThreads returns only and all waiting threads
     */
    public void testGetWaitingThreads()      { testGetWaitingThreads(false); }
    public void testGetWaitingThreads_fair() { testGetWaitingThreads(true); }
    public void testGetWaitingThreads(boolean fair) {
        final PublicReentrantReadWriteLock lock =
            new PublicReentrantReadWriteLock(fair);
        final Condition c = lock.writeLock().newCondition();
        final CountDownLatch locked1 = new CountDownLatch(1);
        final CountDownLatch locked2 = new CountDownLatch(1);
        Thread t1 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                assertTrue(lock.getWaitingThreads(c).isEmpty());
                locked1.countDown();
                c.await();
                lock.writeLock().unlock();
            }});

        Thread t2 = new Thread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLock().lock();
                assertFalse(lock.getWaitingThreads(c).isEmpty());
                locked2.countDown();
                c.await();
                lock.writeLock().unlock();
            }});

        lock.writeLock().lock();
        assertTrue(lock.getWaitingThreads(c).isEmpty());
        lock.writeLock().unlock();

        t1.start();
        await(locked1);
        t2.start();
        await(locked2);

        lock.writeLock().lock();
        assertTrue(lock.hasWaiters(c));
        assertTrue(lock.getWaitingThreads(c).contains(t1));
        assertTrue(lock.getWaitingThreads(c).contains(t2));
        assertEquals(2, lock.getWaitingThreads(c).size());
        c.signalAll();
        assertHasNoWaiters(lock, c);
        lock.writeLock().unlock();

        awaitTermination(t1);
        awaitTermination(t2);

        assertHasNoWaiters(lock, c);
    }

    /**
     * toString indicates current lock state
     */
    public void testToString()      { testToString(false); }
    public void testToString_fair() { testToString(true); }
    public void testToString(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        assertTrue(lock.toString().contains("Write locks = 0"));
        assertTrue(lock.toString().contains("Read locks = 0"));
        lock.writeLock().lock();
        assertTrue(lock.toString().contains("Write locks = 1"));
        assertTrue(lock.toString().contains("Read locks = 0"));
        lock.writeLock().lock();
        assertTrue(lock.toString().contains("Write locks = 2"));
        assertTrue(lock.toString().contains("Read locks = 0"));
        lock.writeLock().unlock();
        lock.writeLock().unlock();
        lock.readLock().lock();
        assertTrue(lock.toString().contains("Write locks = 0"));
        assertTrue(lock.toString().contains("Read locks = 1"));
        lock.readLock().lock();
        assertTrue(lock.toString().contains("Write locks = 0"));
        assertTrue(lock.toString().contains("Read locks = 2"));
    }

    /**
     * readLock.toString indicates current lock state
     */
    public void testReadLockToString()      { testReadLockToString(false); }
    public void testReadLockToString_fair() { testReadLockToString(true); }
    public void testReadLockToString(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        assertTrue(lock.readLock().toString().contains("Read locks = 0"));
        lock.readLock().lock();
        assertTrue(lock.readLock().toString().contains("Read locks = 1"));
        lock.readLock().lock();
        assertTrue(lock.readLock().toString().contains("Read locks = 2"));
        lock.readLock().unlock();
        assertTrue(lock.readLock().toString().contains("Read locks = 1"));
        lock.readLock().unlock();
        assertTrue(lock.readLock().toString().contains("Read locks = 0"));
    }

    /**
     * writeLock.toString indicates current lock state
     */
    public void testWriteLockToString()      { testWriteLockToString(false); }
    public void testWriteLockToString_fair() { testWriteLockToString(true); }
    public void testWriteLockToString(boolean fair) {
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(fair);
        assertTrue(lock.writeLock().toString().contains("Unlocked"));
        lock.writeLock().lock();
        assertTrue(lock.writeLock().toString().contains("Locked by"));
        lock.writeLock().unlock();
        assertTrue(lock.writeLock().toString().contains("Unlocked"));
    }

    /**
     * ThreadMXBean reports the blockers that we expect.
     */
    public void testBlockers() {
        if (!testImplementationDetails) return;
        final boolean fair = randomBoolean();
        final boolean timedAcquire = randomBoolean();
        final boolean timedAwait = randomBoolean();
        final String syncClassName = fair
            ? "ReentrantReadWriteLock$FairSync"
            : "ReentrantReadWriteLock$NonfairSync";
        final String conditionClassName
            = "AbstractQueuedSynchronizer$ConditionObject";
        final Thread.State expectedAcquireState = timedAcquire
            ? Thread.State.TIMED_WAITING
            : Thread.State.WAITING;
        final Thread.State expectedAwaitState = timedAwait
            ? Thread.State.TIMED_WAITING
            : Thread.State.WAITING;
        final Lock lock = new ReentrantReadWriteLock(fair).writeLock();
        final Condition condition = lock.newCondition();
        final AtomicBoolean conditionSatisfied = new AtomicBoolean(false);
        lock.lock();
        final Thread thread = newStartedThread((Action) () -> {
            if (timedAcquire)
                lock.tryLock(LONGER_DELAY_MS, MILLISECONDS);
            else
                lock.lock();
            while (!conditionSatisfied.get())
                if (timedAwait)
                    condition.await(LONGER_DELAY_MS, MILLISECONDS);
                else
                    condition.await();
        });
        Callable<Boolean> waitingForLock = () -> {
            String className;
            return thread.getState() == expectedAcquireState
            && (className = blockerClassName(thread)) != null
            && className.endsWith(syncClassName);
        };
        waitForThreadToEnterWaitState(thread, waitingForLock);

        lock.unlock();
        Callable<Boolean> waitingForCondition = () -> {
            String className;
            return thread.getState() == expectedAwaitState
            && (className = blockerClassName(thread)) != null
            && className.endsWith(conditionClassName);
        };
        waitForThreadToEnterWaitState(thread, waitingForCondition);

        // politely release the waiter
        conditionSatisfied.set(true);
        lock.lock();
        try {
            condition.signal();
        } finally { lock.unlock(); }

        awaitTermination(thread);
    }
}
