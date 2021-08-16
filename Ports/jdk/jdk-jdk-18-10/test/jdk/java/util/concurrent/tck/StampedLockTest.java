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
 * Written by Doug Lea and Martin Buchholz
 * with assistance from members of JCP JSR-166 Expert Group and
 * released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static java.util.concurrent.locks.StampedLock.isLockStamp;
import static java.util.concurrent.locks.StampedLock.isOptimisticReadStamp;
import static java.util.concurrent.locks.StampedLock.isReadLockStamp;
import static java.util.concurrent.locks.StampedLock.isWriteLockStamp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import junit.framework.Test;
import junit.framework.TestSuite;

public class StampedLockTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(StampedLockTest.class);
    }

    /**
     * Releases write lock, checking isWriteLocked before and after
     */
    void releaseWriteLock(StampedLock lock, long stamp) {
        assertTrue(lock.isWriteLocked());
        assertValid(lock, stamp);
        lock.unlockWrite(stamp);
        assertFalse(lock.isWriteLocked());
        assertFalse(lock.validate(stamp));
    }

    /**
     * Releases read lock, checking isReadLocked before and after
     */
    void releaseReadLock(StampedLock lock, long stamp) {
        assertTrue(lock.isReadLocked());
        assertValid(lock, stamp);
        lock.unlockRead(stamp);
        assertFalse(lock.isReadLocked());
        assertTrue(lock.validate(stamp));
    }

    long assertNonZero(long v) {
        assertTrue(v != 0L);
        return v;
    }

    long assertValid(StampedLock lock, long stamp) {
        assertTrue(stamp != 0L);
        assertTrue(lock.validate(stamp));
        return stamp;
    }

    void assertUnlocked(StampedLock lock) {
        assertFalse(lock.isReadLocked());
        assertFalse(lock.isWriteLocked());
        assertEquals(0, lock.getReadLockCount());
        assertValid(lock, lock.tryOptimisticRead());
    }

    List<Action> lockLockers(Lock lock) {
        return List.of(
            () -> lock.lock(),
            () -> lock.lockInterruptibly(),
            () -> lock.tryLock(),
            () -> lock.tryLock(Long.MIN_VALUE, DAYS),
            () -> lock.tryLock(0L, DAYS),
            () -> lock.tryLock(Long.MAX_VALUE, DAYS));
    }

    List<Function<StampedLock, Long>> readLockers() {
        return List.of(
            sl -> sl.readLock(),
            sl -> sl.tryReadLock(),
            sl -> readLockInterruptiblyUninterrupted(sl),
            sl -> tryReadLockUninterrupted(sl, Long.MIN_VALUE, DAYS),
            sl -> tryReadLockUninterrupted(sl, 0L, DAYS),
            sl -> sl.tryConvertToReadLock(sl.tryOptimisticRead()));
    }

    List<BiConsumer<StampedLock, Long>> readUnlockers() {
        return List.of(
            (sl, stamp) -> sl.unlockRead(stamp),
            (sl, stamp) -> assertTrue(sl.tryUnlockRead()),
            (sl, stamp) -> sl.asReadLock().unlock(),
            (sl, stamp) -> sl.unlock(stamp),
            (sl, stamp) -> assertValid(sl, sl.tryConvertToOptimisticRead(stamp)));
    }

    List<Function<StampedLock, Long>> writeLockers() {
        return List.of(
            sl -> sl.writeLock(),
            sl -> sl.tryWriteLock(),
            sl -> writeLockInterruptiblyUninterrupted(sl),
            sl -> tryWriteLockUninterrupted(sl, Long.MIN_VALUE, DAYS),
            sl -> tryWriteLockUninterrupted(sl, 0L, DAYS),
            sl -> sl.tryConvertToWriteLock(sl.tryOptimisticRead()));
    }

    List<BiConsumer<StampedLock, Long>> writeUnlockers() {
        return List.of(
            (sl, stamp) -> sl.unlockWrite(stamp),
            (sl, stamp) -> assertTrue(sl.tryUnlockWrite()),
            (sl, stamp) -> sl.asWriteLock().unlock(),
            (sl, stamp) -> sl.unlock(stamp),
            (sl, stamp) -> assertValid(sl, sl.tryConvertToOptimisticRead(stamp)));
    }

    /**
     * Constructed StampedLock is in unlocked state
     */
    public void testConstructor() {
        assertUnlocked(new StampedLock());
    }

    /**
     * write-locking, then unlocking, an unlocked lock succeed
     */
    public void testWriteLock_lockUnlock() {
        StampedLock lock = new StampedLock();

        for (Function<StampedLock, Long> writeLocker : writeLockers())
        for (BiConsumer<StampedLock, Long> writeUnlocker : writeUnlockers()) {
            assertFalse(lock.isWriteLocked());
            assertFalse(lock.isReadLocked());
            assertEquals(0, lock.getReadLockCount());

            long s = writeLocker.apply(lock);
            assertValid(lock, s);
            assertTrue(lock.isWriteLocked());
            assertFalse(lock.isReadLocked());
            assertEquals(0, lock.getReadLockCount());
            writeUnlocker.accept(lock, s);
            assertUnlocked(lock);
        }
    }

    /**
     * read-locking, then unlocking, an unlocked lock succeed
     */
    public void testReadLock_lockUnlock() {
        StampedLock lock = new StampedLock();

        for (Function<StampedLock, Long> readLocker : readLockers())
        for (BiConsumer<StampedLock, Long> readUnlocker : readUnlockers()) {
            long s = 42;
            for (int i = 0; i < 2; i++) {
                s = assertValid(lock, readLocker.apply(lock));
                assertFalse(lock.isWriteLocked());
                assertTrue(lock.isReadLocked());
                assertEquals(i + 1, lock.getReadLockCount());
            }
            for (int i = 0; i < 2; i++) {
                assertFalse(lock.isWriteLocked());
                assertTrue(lock.isReadLocked());
                assertEquals(2 - i, lock.getReadLockCount());
                readUnlocker.accept(lock, s);
            }
            assertUnlocked(lock);
        }
    }

    /**
     * tryUnlockWrite fails if not write locked
     */
    public void testTryUnlockWrite_failure() {
        StampedLock lock = new StampedLock();
        assertFalse(lock.tryUnlockWrite());

        for (Function<StampedLock, Long> readLocker : readLockers())
        for (BiConsumer<StampedLock, Long> readUnlocker : readUnlockers()) {
            long s = assertValid(lock, readLocker.apply(lock));
            assertFalse(lock.tryUnlockWrite());
            assertTrue(lock.isReadLocked());
            readUnlocker.accept(lock, s);
            assertUnlocked(lock);
        }
    }

    /**
     * tryUnlockRead fails if not read locked
     */
    public void testTryUnlockRead_failure() {
        StampedLock lock = new StampedLock();
        assertFalse(lock.tryUnlockRead());

        for (Function<StampedLock, Long> writeLocker : writeLockers())
        for (BiConsumer<StampedLock, Long> writeUnlocker : writeUnlockers()) {
            long s = writeLocker.apply(lock);
            assertFalse(lock.tryUnlockRead());
            assertTrue(lock.isWriteLocked());
            writeUnlocker.accept(lock, s);
            assertUnlocked(lock);
        }
    }

    /**
     * validate(0L) fails
     */
    public void testValidate0() {
        StampedLock lock = new StampedLock();
        assertFalse(lock.validate(0L));
    }

    /**
     * A stamp obtained from a successful lock operation validates while the lock is held
     */
    public void testValidate() throws InterruptedException {
        StampedLock lock = new StampedLock();

        for (Function<StampedLock, Long> readLocker : readLockers())
        for (BiConsumer<StampedLock, Long> readUnlocker : readUnlockers()) {
            long s = assertNonZero(readLocker.apply(lock));
            assertTrue(lock.validate(s));
            readUnlocker.accept(lock, s);
        }

        for (Function<StampedLock, Long> writeLocker : writeLockers())
        for (BiConsumer<StampedLock, Long> writeUnlocker : writeUnlockers()) {
            long s = assertNonZero(writeLocker.apply(lock));
            assertTrue(lock.validate(s));
            writeUnlocker.accept(lock, s);
        }
    }

    /**
     * A stamp obtained from an unsuccessful lock operation does not validate
     */
    public void testValidate2() throws InterruptedException {
        StampedLock lock = new StampedLock();
        long s = assertNonZero(lock.writeLock());
        assertTrue(lock.validate(s));
        assertFalse(lock.validate(lock.tryWriteLock()));
        assertFalse(lock.validate(lock.tryWriteLock(randomExpiredTimeout(),
                                                    randomTimeUnit())));
        assertFalse(lock.validate(lock.tryReadLock()));
        assertFalse(lock.validate(lock.tryWriteLock(randomExpiredTimeout(),
                                                    randomTimeUnit())));
        assertFalse(lock.validate(lock.tryOptimisticRead()));
        lock.unlockWrite(s);
    }

    void assertThrowInterruptedExceptionWhenPreInterrupted(Action[] actions) {
        for (Action action : actions) {
            Thread.currentThread().interrupt();
            try {
                action.run();
                shouldThrow();
            }
            catch (InterruptedException success) {}
            catch (Throwable fail) { threadUnexpectedException(fail); }
            assertFalse(Thread.interrupted());
        }
    }

    /**
     * interruptible operations throw InterruptedException when pre-interrupted
     */
    public void testInterruptibleOperationsThrowInterruptedExceptionWhenPreInterrupted() {
        final StampedLock lock = new StampedLock();

        Action[] interruptibleLockActions = {
            () -> lock.writeLockInterruptibly(),
            () -> lock.tryWriteLock(Long.MIN_VALUE, DAYS),
            () -> lock.tryWriteLock(Long.MAX_VALUE, DAYS),
            () -> lock.readLockInterruptibly(),
            () -> lock.tryReadLock(Long.MIN_VALUE, DAYS),
            () -> lock.tryReadLock(Long.MAX_VALUE, DAYS),
            () -> lock.asWriteLock().lockInterruptibly(),
            () -> lock.asWriteLock().tryLock(0L, DAYS),
            () -> lock.asWriteLock().tryLock(Long.MAX_VALUE, DAYS),
            () -> lock.asReadLock().lockInterruptibly(),
            () -> lock.asReadLock().tryLock(0L, DAYS),
            () -> lock.asReadLock().tryLock(Long.MAX_VALUE, DAYS),
        };
        shuffle(interruptibleLockActions);

        assertThrowInterruptedExceptionWhenPreInterrupted(interruptibleLockActions);
        {
            long s = lock.writeLock();
            assertThrowInterruptedExceptionWhenPreInterrupted(interruptibleLockActions);
            lock.unlockWrite(s);
        }
        {
            long s = lock.readLock();
            assertThrowInterruptedExceptionWhenPreInterrupted(interruptibleLockActions);
            lock.unlockRead(s);
        }
    }

    void assertThrowInterruptedExceptionWhenInterrupted(Action[] actions) {
        int n = actions.length;
        Future<?>[] futures = new Future<?>[n];
        CountDownLatch threadsStarted = new CountDownLatch(n);
        CountDownLatch done = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            Action action = actions[i];
            futures[i] = cachedThreadPool.submit(new CheckedRunnable() {
                public void realRun() throws Throwable {
                    threadsStarted.countDown();
                    try {
                        action.run();
                        shouldThrow();
                    }
                    catch (InterruptedException success) {}
                    catch (Throwable fail) { threadUnexpectedException(fail); }
                    assertFalse(Thread.interrupted());
                    done.countDown();
                }});
        }

        await(threadsStarted);
        assertEquals(n, done.getCount());
        for (Future<?> future : futures) // Interrupt all the tasks
            future.cancel(true);
        await(done);
    }

    /**
     * interruptible operations throw InterruptedException when write locked and interrupted
     */
    public void testInterruptibleOperationsThrowInterruptedExceptionWriteLockedInterrupted() {
        final StampedLock lock = new StampedLock();
        long stamp = lock.writeLock();

        Action[] interruptibleLockBlockingActions = {
            () -> lock.writeLockInterruptibly(),
            () -> lock.tryWriteLock(Long.MAX_VALUE, DAYS),
            () -> lock.readLockInterruptibly(),
            () -> lock.tryReadLock(Long.MAX_VALUE, DAYS),
            () -> lock.asWriteLock().lockInterruptibly(),
            () -> lock.asWriteLock().tryLock(Long.MAX_VALUE, DAYS),
            () -> lock.asReadLock().lockInterruptibly(),
            () -> lock.asReadLock().tryLock(Long.MAX_VALUE, DAYS),
        };
        shuffle(interruptibleLockBlockingActions);

        assertThrowInterruptedExceptionWhenInterrupted(interruptibleLockBlockingActions);

        releaseWriteLock(lock, stamp);
    }

    /**
     * interruptible operations throw InterruptedException when read locked and interrupted
     */
    public void testInterruptibleOperationsThrowInterruptedExceptionReadLockedInterrupted() {
        final StampedLock lock = new StampedLock();
        long stamp = lock.readLock();

        Action[] interruptibleLockBlockingActions = {
            () -> lock.writeLockInterruptibly(),
            () -> lock.tryWriteLock(Long.MAX_VALUE, DAYS),
            () -> lock.asWriteLock().lockInterruptibly(),
            () -> lock.asWriteLock().tryLock(Long.MAX_VALUE, DAYS),
        };
        shuffle(interruptibleLockBlockingActions);

        assertThrowInterruptedExceptionWhenInterrupted(interruptibleLockBlockingActions);

        releaseReadLock(lock, stamp);
    }

    /**
     * Non-interruptible operations ignore and preserve interrupt status
     */
    public void testNonInterruptibleOperationsIgnoreInterrupts() {
        final StampedLock lock = new StampedLock();
        Thread.currentThread().interrupt();

        for (BiConsumer<StampedLock, Long> readUnlocker : readUnlockers()) {
            long s = assertValid(lock, lock.readLock());
            readUnlocker.accept(lock, s);
            s = assertValid(lock, lock.tryReadLock());
            readUnlocker.accept(lock, s);
        }

        lock.asReadLock().lock();
        lock.asReadLock().unlock();

        for (BiConsumer<StampedLock, Long> writeUnlocker : writeUnlockers()) {
            long s = assertValid(lock, lock.writeLock());
            writeUnlocker.accept(lock, s);
            s = assertValid(lock, lock.tryWriteLock());
            writeUnlocker.accept(lock, s);
        }

        lock.asWriteLock().lock();
        lock.asWriteLock().unlock();

        assertTrue(Thread.interrupted());
    }

    /**
     * tryWriteLock on an unlocked lock succeeds
     */
    public void testTryWriteLock() {
        final StampedLock lock = new StampedLock();
        long s = lock.tryWriteLock();
        assertTrue(s != 0L);
        assertTrue(lock.isWriteLocked());
        assertEquals(0L, lock.tryWriteLock());
        releaseWriteLock(lock, s);
    }

    /**
     * tryWriteLock fails if locked
     */
    public void testTryWriteLockWhenLocked() {
        final StampedLock lock = new StampedLock();
        long s = lock.writeLock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(0L, lock.tryWriteLock());
            }});

        assertEquals(0L, lock.tryWriteLock());
        awaitTermination(t);
        releaseWriteLock(lock, s);
    }

    /**
     * tryReadLock fails if write-locked
     */
    public void testTryReadLockWhenLocked() {
        final StampedLock lock = new StampedLock();
        long s = lock.writeLock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(0L, lock.tryReadLock());
            }});

        assertEquals(0L, lock.tryReadLock());
        awaitTermination(t);
        releaseWriteLock(lock, s);
    }

    /**
     * Multiple threads can hold a read lock when not write-locked
     */
    public void testMultipleReadLocks() {
        final StampedLock lock = new StampedLock();
        final long s = lock.readLock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long s2 = lock.tryReadLock();
                assertValid(lock, s2);
                lock.unlockRead(s2);
                long s3 = lock.tryReadLock(LONG_DELAY_MS, MILLISECONDS);
                assertValid(lock, s3);
                lock.unlockRead(s3);
                long s4 = lock.readLock();
                assertValid(lock, s4);
                lock.unlockRead(s4);
                lock.asReadLock().lock();
                lock.asReadLock().unlock();
                lock.asReadLock().lockInterruptibly();
                lock.asReadLock().unlock();
                lock.asReadLock().tryLock(Long.MIN_VALUE, DAYS);
                lock.asReadLock().unlock();
            }});

        awaitTermination(t);
        lock.unlockRead(s);
    }

    /**
     * writeLock() succeeds only after a reading thread unlocks
     */
    public void testWriteAfterReadLock() throws InterruptedException {
        final CountDownLatch aboutToLock = new CountDownLatch(1);
        final StampedLock lock = new StampedLock();
        long rs = lock.readLock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                aboutToLock.countDown();
                long s = lock.writeLock();
                assertTrue(lock.isWriteLocked());
                assertFalse(lock.isReadLocked());
                lock.unlockWrite(s);
            }});

        await(aboutToLock);
        assertThreadBlocks(t, Thread.State.WAITING);
        assertFalse(lock.isWriteLocked());
        assertTrue(lock.isReadLocked());
        lock.unlockRead(rs);
        awaitTermination(t);
        assertUnlocked(lock);
    }

    /**
     * writeLock() succeeds only after reading threads unlock
     */
    public void testWriteAfterMultipleReadLocks() {
        final StampedLock lock = new StampedLock();
        long s = lock.readLock();
        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                long rs = lock.readLock();
                lock.unlockRead(rs);
            }});

        awaitTermination(t1);

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                long ws = lock.writeLock();
                lock.unlockWrite(ws);
            }});

        assertTrue(lock.isReadLocked());
        assertFalse(lock.isWriteLocked());
        lock.unlockRead(s);
        awaitTermination(t2);
        assertUnlocked(lock);
    }

    /**
     * readLock() succeed only after a writing thread unlocks
     */
    public void testReadAfterWriteLock() {
        final StampedLock lock = new StampedLock();
        final CountDownLatch threadsStarted = new CountDownLatch(2);
        final long s = lock.writeLock();
        final Runnable acquireReleaseReadLock = new CheckedRunnable() {
            public void realRun() {
                threadsStarted.countDown();
                long rs = lock.readLock();
                assertTrue(lock.isReadLocked());
                assertFalse(lock.isWriteLocked());
                lock.unlockRead(rs);
            }};
        Thread t1 = newStartedThread(acquireReleaseReadLock);
        Thread t2 = newStartedThread(acquireReleaseReadLock);

        await(threadsStarted);
        assertThreadBlocks(t1, Thread.State.WAITING);
        assertThreadBlocks(t2, Thread.State.WAITING);
        assertTrue(lock.isWriteLocked());
        assertFalse(lock.isReadLocked());
        releaseWriteLock(lock, s);
        awaitTermination(t1);
        awaitTermination(t2);
        assertUnlocked(lock);
    }

    /**
     * tryReadLock succeeds if read locked but not write locked
     */
    public void testTryLockWhenReadLocked() {
        final StampedLock lock = new StampedLock();
        long s = lock.readLock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                long rs = lock.tryReadLock();
                assertValid(lock, rs);
                lock.unlockRead(rs);
            }});

        awaitTermination(t);
        lock.unlockRead(s);
    }

    /**
     * tryWriteLock fails when read locked
     */
    public void testTryWriteLockWhenReadLocked() {
        final StampedLock lock = new StampedLock();
        long s = lock.readLock();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                assertEquals(0L, lock.tryWriteLock());
            }});

        awaitTermination(t);
        lock.unlockRead(s);
    }

    /**
     * timed lock operations time out if lock not available
     */
    public void testTimedLock_Timeout() throws Exception {
        ArrayList<Future<?>> futures = new ArrayList<>();

        // Write locked
        final StampedLock lock = new StampedLock();
        long stamp = lock.writeLock();
        assertEquals(0L, lock.tryReadLock(0L, DAYS));
        assertEquals(0L, lock.tryReadLock(Long.MIN_VALUE, DAYS));
        assertFalse(lock.asReadLock().tryLock(0L, DAYS));
        assertFalse(lock.asReadLock().tryLock(Long.MIN_VALUE, DAYS));
        assertEquals(0L, lock.tryWriteLock(0L, DAYS));
        assertEquals(0L, lock.tryWriteLock(Long.MIN_VALUE, DAYS));
        assertFalse(lock.asWriteLock().tryLock(0L, DAYS));
        assertFalse(lock.asWriteLock().tryLock(Long.MIN_VALUE, DAYS));

        futures.add(cachedThreadPool.submit(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertEquals(0L, lock.tryWriteLock(timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
            }}));

        futures.add(cachedThreadPool.submit(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertEquals(0L, lock.tryReadLock(timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
            }}));

        // Read locked
        final StampedLock lock2 = new StampedLock();
        long stamp2 = lock2.readLock();
        assertEquals(0L, lock2.tryWriteLock(0L, DAYS));
        assertEquals(0L, lock2.tryWriteLock(Long.MIN_VALUE, DAYS));
        assertFalse(lock2.asWriteLock().tryLock(0L, DAYS));
        assertFalse(lock2.asWriteLock().tryLock(Long.MIN_VALUE, DAYS));

        futures.add(cachedThreadPool.submit(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertEquals(0L, lock2.tryWriteLock(timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
            }}));

        for (Future<?> future : futures)
            assertNull(future.get());

        releaseWriteLock(lock, stamp);
        releaseReadLock(lock2, stamp2);
    }

    /**
     * writeLockInterruptibly succeeds if unlocked
     */
    public void testWriteLockInterruptibly() throws InterruptedException {
        final StampedLock lock = new StampedLock();
        long s = lock.writeLockInterruptibly();
        assertTrue(lock.isWriteLocked());
        releaseWriteLock(lock, s);
    }

    /**
     * readLockInterruptibly succeeds if lock free
     */
    public void testReadLockInterruptibly() throws InterruptedException {
        final StampedLock lock = new StampedLock();

        long s = assertValid(lock, lock.readLockInterruptibly());
        assertTrue(lock.isReadLocked());
        lock.unlockRead(s);

        lock.asReadLock().lockInterruptibly();
        assertTrue(lock.isReadLocked());
        lock.asReadLock().unlock();
    }

    /**
     * A serialized lock deserializes as unlocked
     */
    public void testSerialization() {
        StampedLock lock = new StampedLock();
        lock.writeLock();
        StampedLock clone = serialClone(lock);
        assertTrue(lock.isWriteLocked());
        assertFalse(clone.isWriteLocked());
        long s = clone.writeLock();
        assertTrue(clone.isWriteLocked());
        clone.unlockWrite(s);
        assertFalse(clone.isWriteLocked());
    }

    /**
     * toString indicates current lock state
     */
    public void testToString() {
        StampedLock lock = new StampedLock();
        assertTrue(lock.toString().contains("Unlocked"));
        long s = lock.writeLock();
        assertTrue(lock.toString().contains("Write-locked"));
        lock.unlockWrite(s);
        s = lock.readLock();
        assertTrue(lock.toString().contains("Read-locks"));
        releaseReadLock(lock, s);
    }

    /**
     * tryOptimisticRead succeeds and validates if unlocked, fails if
     * exclusively locked
     */
    public void testValidateOptimistic() throws InterruptedException {
        StampedLock lock = new StampedLock();

        assertValid(lock, lock.tryOptimisticRead());

        for (Function<StampedLock, Long> writeLocker : writeLockers()) {
            long s = assertValid(lock, writeLocker.apply(lock));
            assertEquals(0L, lock.tryOptimisticRead());
            releaseWriteLock(lock, s);
        }

        for (Function<StampedLock, Long> readLocker : readLockers()) {
            long s = assertValid(lock, readLocker.apply(lock));
            long p = assertValid(lock, lock.tryOptimisticRead());
            releaseReadLock(lock, s);
            assertTrue(lock.validate(p));
        }

        assertValid(lock, lock.tryOptimisticRead());
    }

    /**
     * tryOptimisticRead stamp does not validate if a write lock intervenes
     */
    public void testValidateOptimisticWriteLocked() {
        final StampedLock lock = new StampedLock();
        final long p = assertValid(lock, lock.tryOptimisticRead());
        final long s = assertValid(lock, lock.writeLock());
        assertFalse(lock.validate(p));
        assertEquals(0L, lock.tryOptimisticRead());
        assertTrue(lock.validate(s));
        lock.unlockWrite(s);
    }

    /**
     * tryOptimisticRead stamp does not validate if a write lock
     * intervenes in another thread
     */
    public void testValidateOptimisticWriteLocked2()
            throws InterruptedException {
        final CountDownLatch locked = new CountDownLatch(1);
        final StampedLock lock = new StampedLock();
        final long p = assertValid(lock, lock.tryOptimisticRead());

        Thread t = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                lock.writeLockInterruptibly();
                locked.countDown();
                lock.writeLockInterruptibly();
            }});

        await(locked);
        assertFalse(lock.validate(p));
        assertEquals(0L, lock.tryOptimisticRead());
        assertThreadBlocks(t, Thread.State.WAITING);
        t.interrupt();
        awaitTermination(t);
        assertTrue(lock.isWriteLocked());
    }

    /**
     * tryConvertToOptimisticRead succeeds and validates if successfully locked
     */
    public void testTryConvertToOptimisticRead() throws InterruptedException {
        StampedLock lock = new StampedLock();
        long s, p, q;
        assertEquals(0L, lock.tryConvertToOptimisticRead(0L));

        s = assertValid(lock, lock.tryOptimisticRead());
        assertEquals(s, lock.tryConvertToOptimisticRead(s));
        assertTrue(lock.validate(s));

        for (Function<StampedLock, Long> writeLocker : writeLockers()) {
            s = assertValid(lock, writeLocker.apply(lock));
            p = assertValid(lock, lock.tryConvertToOptimisticRead(s));
            assertFalse(lock.validate(s));
            assertTrue(lock.validate(p));
            assertUnlocked(lock);
        }

        for (Function<StampedLock, Long> readLocker : readLockers()) {
            s = assertValid(lock, readLocker.apply(lock));
            q = assertValid(lock, lock.tryOptimisticRead());
            assertEquals(q, lock.tryConvertToOptimisticRead(q));
            assertTrue(lock.validate(q));
            assertTrue(lock.isReadLocked());
            p = assertValid(lock, lock.tryConvertToOptimisticRead(s));
            assertTrue(lock.validate(p));
            assertTrue(lock.validate(s));
            assertUnlocked(lock);
            assertEquals(q, lock.tryConvertToOptimisticRead(q));
            assertTrue(lock.validate(q));
        }
    }

    /**
     * tryConvertToReadLock succeeds for valid stamps
     */
    public void testTryConvertToReadLock() throws InterruptedException {
        StampedLock lock = new StampedLock();
        long s, p;

        assertEquals(0L, lock.tryConvertToReadLock(0L));

        s = assertValid(lock, lock.tryOptimisticRead());
        p = assertValid(lock, lock.tryConvertToReadLock(s));
        assertTrue(lock.isReadLocked());
        assertEquals(1, lock.getReadLockCount());
        assertTrue(lock.validate(s));
        lock.unlockRead(p);

        s = assertValid(lock, lock.tryOptimisticRead());
        lock.readLock();
        p = assertValid(lock, lock.tryConvertToReadLock(s));
        assertTrue(lock.isReadLocked());
        assertEquals(2, lock.getReadLockCount());
        lock.unlockRead(p);
        lock.unlockRead(p);
        assertUnlocked(lock);

        for (BiConsumer<StampedLock, Long> readUnlocker : readUnlockers()) {
            for (Function<StampedLock, Long> writeLocker : writeLockers()) {
                s = assertValid(lock, writeLocker.apply(lock));
                p = assertValid(lock, lock.tryConvertToReadLock(s));
                assertFalse(lock.validate(s));
                assertTrue(lock.isReadLocked());
                assertEquals(1, lock.getReadLockCount());
                readUnlocker.accept(lock, p);
            }

            for (Function<StampedLock, Long> readLocker : readLockers()) {
                s = assertValid(lock, readLocker.apply(lock));
                assertEquals(s, lock.tryConvertToReadLock(s));
                assertTrue(lock.validate(s));
                assertTrue(lock.isReadLocked());
                assertEquals(1, lock.getReadLockCount());
                readUnlocker.accept(lock, s);
            }
        }
    }

    /**
     * tryConvertToWriteLock succeeds if lock available; fails if multiply read locked
     */
    public void testTryConvertToWriteLock() throws InterruptedException {
        StampedLock lock = new StampedLock();
        long s, p;

        assertEquals(0L, lock.tryConvertToWriteLock(0L));

        assertTrue((s = lock.tryOptimisticRead()) != 0L);
        assertTrue((p = lock.tryConvertToWriteLock(s)) != 0L);
        assertTrue(lock.isWriteLocked());
        lock.unlockWrite(p);

        for (BiConsumer<StampedLock, Long> writeUnlocker : writeUnlockers()) {
            for (Function<StampedLock, Long> writeLocker : writeLockers()) {
                s = assertValid(lock, writeLocker.apply(lock));
                assertEquals(s, lock.tryConvertToWriteLock(s));
                assertTrue(lock.validate(s));
                assertTrue(lock.isWriteLocked());
                writeUnlocker.accept(lock, s);
            }

            for (Function<StampedLock, Long> readLocker : readLockers()) {
                s = assertValid(lock, readLocker.apply(lock));
                p = assertValid(lock, lock.tryConvertToWriteLock(s));
                assertFalse(lock.validate(s));
                assertTrue(lock.validate(p));
                assertTrue(lock.isWriteLocked());
                writeUnlocker.accept(lock, p);
            }
        }

        // failure if multiply read locked
        for (Function<StampedLock, Long> readLocker : readLockers()) {
            s = assertValid(lock, readLocker.apply(lock));
            p = assertValid(lock, readLocker.apply(lock));
            assertEquals(0L, lock.tryConvertToWriteLock(s));
            assertTrue(lock.validate(s));
            assertTrue(lock.validate(p));
            assertEquals(2, lock.getReadLockCount());
            lock.unlock(p);
            lock.unlock(s);
            assertUnlocked(lock);
        }
    }

    /**
     * asWriteLock can be locked and unlocked
     */
    public void testAsWriteLock() throws Throwable {
        StampedLock sl = new StampedLock();
        Lock lock = sl.asWriteLock();
        for (Action locker : lockLockers(lock)) {
            locker.run();
            assertTrue(sl.isWriteLocked());
            assertFalse(sl.isReadLocked());
            assertFalse(lock.tryLock());
            lock.unlock();
            assertUnlocked(sl);
        }
    }

    /**
     * asReadLock can be locked and unlocked
     */
    public void testAsReadLock() throws Throwable {
        StampedLock sl = new StampedLock();
        Lock lock = sl.asReadLock();
        for (Action locker : lockLockers(lock)) {
            locker.run();
            assertTrue(sl.isReadLocked());
            assertFalse(sl.isWriteLocked());
            assertEquals(1, sl.getReadLockCount());
            locker.run();
            assertTrue(sl.isReadLocked());
            assertEquals(2, sl.getReadLockCount());
            lock.unlock();
            lock.unlock();
            assertUnlocked(sl);
        }
    }

    /**
     * asReadWriteLock.writeLock can be locked and unlocked
     */
    public void testAsReadWriteLockWriteLock() throws Throwable {
        StampedLock sl = new StampedLock();
        Lock lock = sl.asReadWriteLock().writeLock();
        for (Action locker : lockLockers(lock)) {
            locker.run();
            assertTrue(sl.isWriteLocked());
            assertFalse(sl.isReadLocked());
            assertFalse(lock.tryLock());
            lock.unlock();
            assertUnlocked(sl);
        }
    }

    /**
     * asReadWriteLock.readLock can be locked and unlocked
     */
    public void testAsReadWriteLockReadLock() throws Throwable {
        StampedLock sl = new StampedLock();
        Lock lock = sl.asReadWriteLock().readLock();
        for (Action locker : lockLockers(lock)) {
            locker.run();
            assertTrue(sl.isReadLocked());
            assertFalse(sl.isWriteLocked());
            assertEquals(1, sl.getReadLockCount());
            locker.run();
            assertTrue(sl.isReadLocked());
            assertEquals(2, sl.getReadLockCount());
            lock.unlock();
            lock.unlock();
            assertUnlocked(sl);
        }
    }

    /**
     * Lock.newCondition throws UnsupportedOperationException
     */
    public void testLockViewsDoNotSupportConditions() {
        StampedLock sl = new StampedLock();
        assertThrows(UnsupportedOperationException.class,
                     () -> sl.asWriteLock().newCondition(),
                     () -> sl.asReadLock().newCondition(),
                     () -> sl.asReadWriteLock().writeLock().newCondition(),
                     () -> sl.asReadWriteLock().readLock().newCondition());
    }

    /**
     * Passing optimistic read stamps to unlock operations result in
     * IllegalMonitorStateException
     */
    public void testCannotUnlockOptimisticReadStamps() {
        {
            StampedLock sl = new StampedLock();
            long stamp = assertValid(sl, sl.tryOptimisticRead());
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlockRead(stamp));
        }
        {
            StampedLock sl = new StampedLock();
            long stamp = sl.tryOptimisticRead();
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlock(stamp));
        }

        {
            StampedLock sl = new StampedLock();
            long stamp = sl.tryOptimisticRead();
            sl.writeLock();
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlock(stamp));
        }
        {
            StampedLock sl = new StampedLock();
            sl.readLock();
            long stamp = assertValid(sl, sl.tryOptimisticRead());
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlockRead(stamp));
        }
        {
            StampedLock sl = new StampedLock();
            sl.readLock();
            long stamp = assertValid(sl, sl.tryOptimisticRead());
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlock(stamp));
        }

        {
            StampedLock sl = new StampedLock();
            long stamp = sl.tryConvertToOptimisticRead(sl.writeLock());
            assertValid(sl, stamp);
            sl.writeLock();
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlockWrite(stamp));
        }
        {
            StampedLock sl = new StampedLock();
            long stamp = sl.tryConvertToOptimisticRead(sl.writeLock());
            sl.writeLock();
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlock(stamp));
        }
        {
            StampedLock sl = new StampedLock();
            long stamp = sl.tryConvertToOptimisticRead(sl.writeLock());
            sl.readLock();
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlockRead(stamp));
        }
        {
            StampedLock sl = new StampedLock();
            long stamp = sl.tryConvertToOptimisticRead(sl.writeLock());
            sl.readLock();
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlock(stamp));
        }

        {
            StampedLock sl = new StampedLock();
            long stamp = sl.tryConvertToOptimisticRead(sl.readLock());
            assertValid(sl, stamp);
            sl.writeLock();
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlockWrite(stamp));
            }
        {
            StampedLock sl = new StampedLock();
            long stamp = sl.tryConvertToOptimisticRead(sl.readLock());
            sl.writeLock();
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlock(stamp));
        }
        {
            StampedLock sl = new StampedLock();
            long stamp = sl.tryConvertToOptimisticRead(sl.readLock());
            sl.readLock();
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlockRead(stamp));
        }
        {
            StampedLock sl = new StampedLock();
            sl.readLock();
            long stamp = sl.tryConvertToOptimisticRead(sl.readLock());
            assertValid(sl, stamp);
            sl.readLock();
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlockRead(stamp));
        }
        {
            StampedLock sl = new StampedLock();
            long stamp = sl.tryConvertToOptimisticRead(sl.readLock());
            sl.readLock();
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlock(stamp));
        }
        {
            StampedLock sl = new StampedLock();
            sl.readLock();
            long stamp = sl.tryConvertToOptimisticRead(sl.readLock());
            sl.readLock();
            assertThrows(IllegalMonitorStateException.class,
                () -> sl.unlock(stamp));
        }
    }

    static long writeLockInterruptiblyUninterrupted(StampedLock sl) {
        try { return sl.writeLockInterruptibly(); }
        catch (InterruptedException ex) { throw new AssertionError(ex); }
    }

    static long tryWriteLockUninterrupted(StampedLock sl, long time, TimeUnit unit) {
        try { return sl.tryWriteLock(time, unit); }
        catch (InterruptedException ex) { throw new AssertionError(ex); }
    }

    static long readLockInterruptiblyUninterrupted(StampedLock sl) {
        try { return sl.readLockInterruptibly(); }
        catch (InterruptedException ex) { throw new AssertionError(ex); }
    }

    static long tryReadLockUninterrupted(StampedLock sl, long time, TimeUnit unit) {
        try { return sl.tryReadLock(time, unit); }
        catch (InterruptedException ex) { throw new AssertionError(ex); }
    }

    /**
     * Invalid stamps result in IllegalMonitorStateException
     */
    public void testInvalidStampsThrowIllegalMonitorStateException() {
        final StampedLock sl = new StampedLock();

        assertThrows(IllegalMonitorStateException.class,
                     () -> sl.unlockWrite(0L),
                     () -> sl.unlockRead(0L),
                     () -> sl.unlock(0L));

        final long optimisticStamp = sl.tryOptimisticRead();
        final long readStamp = sl.readLock();
        sl.unlockRead(readStamp);
        final long writeStamp = sl.writeLock();
        sl.unlockWrite(writeStamp);
        assertTrue(optimisticStamp != 0L && readStamp != 0L && writeStamp != 0L);
        final long[] noLongerValidStamps = { optimisticStamp, readStamp, writeStamp };
        final Runnable assertNoLongerValidStampsThrow = () -> {
            for (long noLongerValidStamp : noLongerValidStamps)
                assertThrows(IllegalMonitorStateException.class,
                             () -> sl.unlockWrite(noLongerValidStamp),
                             () -> sl.unlockRead(noLongerValidStamp),
                             () -> sl.unlock(noLongerValidStamp));
        };
        assertNoLongerValidStampsThrow.run();

        for (Function<StampedLock, Long> readLocker : readLockers())
        for (BiConsumer<StampedLock, Long> readUnlocker : readUnlockers()) {
            final long stamp = readLocker.apply(sl);
            assertValid(sl, stamp);
            assertNoLongerValidStampsThrow.run();
            assertThrows(IllegalMonitorStateException.class,
                         () -> sl.unlockWrite(stamp),
                         () -> sl.unlockRead(sl.tryOptimisticRead()),
                         () -> sl.unlockRead(0L));
            readUnlocker.accept(sl, stamp);
            assertUnlocked(sl);
            assertNoLongerValidStampsThrow.run();
        }

        for (Function<StampedLock, Long> writeLocker : writeLockers())
        for (BiConsumer<StampedLock, Long> writeUnlocker : writeUnlockers()) {
            final long stamp = writeLocker.apply(sl);
            assertValid(sl, stamp);
            assertNoLongerValidStampsThrow.run();
            assertThrows(IllegalMonitorStateException.class,
                         () -> sl.unlockRead(stamp),
                         () -> sl.unlockWrite(0L));
            writeUnlocker.accept(sl, stamp);
            assertUnlocked(sl);
            assertNoLongerValidStampsThrow.run();
        }
    }

    /**
     * Read locks can be very deeply nested
     */
    public void testDeeplyNestedReadLocks() {
        final StampedLock lock = new StampedLock();
        final int depth = 300;
        final long[] stamps = new long[depth];
        final List<Function<StampedLock, Long>> readLockers = readLockers();
        final List<BiConsumer<StampedLock, Long>> readUnlockers = readUnlockers();
        for (int i = 0; i < depth; i++) {
            Function<StampedLock, Long> readLocker
                = readLockers.get(i % readLockers.size());
            long stamp = readLocker.apply(lock);
            assertEquals(i + 1, lock.getReadLockCount());
            assertTrue(lock.isReadLocked());
            stamps[i] = stamp;
        }
        for (int i = 0; i < depth; i++) {
            BiConsumer<StampedLock, Long> readUnlocker
                = readUnlockers.get(i % readUnlockers.size());
            assertEquals(depth - i, lock.getReadLockCount());
            assertTrue(lock.isReadLocked());
            readUnlocker.accept(lock, stamps[depth - 1 - i]);
        }
        assertUnlocked(lock);
    }

    /**
     * Stamped locks are not reentrant.
     */
    public void testNonReentrant() throws InterruptedException {
        final StampedLock lock = new StampedLock();
        long stamp;

        stamp = lock.writeLock();
        assertValid(lock, stamp);
        assertEquals(0L, lock.tryWriteLock(0L, DAYS));
        assertEquals(0L, lock.tryReadLock(0L, DAYS));
        assertValid(lock, stamp);
        lock.unlockWrite(stamp);

        stamp = lock.tryWriteLock(1L, DAYS);
        assertEquals(0L, lock.tryWriteLock(0L, DAYS));
        assertValid(lock, stamp);
        lock.unlockWrite(stamp);

        stamp = lock.readLock();
        assertEquals(0L, lock.tryWriteLock(0L, DAYS));
        assertValid(lock, stamp);
        lock.unlockRead(stamp);
    }

    /**
     * """StampedLocks have no notion of ownership. Locks acquired in
     * one thread can be released or converted in another."""
     */
    public void testNoOwnership() throws Throwable {
        ArrayList<Future<?>> futures = new ArrayList<>();
        for (Function<StampedLock, Long> writeLocker : writeLockers())
        for (BiConsumer<StampedLock, Long> writeUnlocker : writeUnlockers()) {
            StampedLock lock = new StampedLock();
            long stamp = writeLocker.apply(lock);
            futures.add(cachedThreadPool.submit(new CheckedRunnable() {
                public void realRun() {
                    writeUnlocker.accept(lock, stamp);
                    assertUnlocked(lock);
                    assertFalse(lock.validate(stamp));
                }}));
        }
        for (Future<?> future : futures)
            assertNull(future.get());
    }

    /** Tries out sample usage code from StampedLock javadoc. */
    public void testSampleUsage() throws Throwable {
        class Point {
            private double x, y;
            private final StampedLock sl = new StampedLock();

            void move(double deltaX, double deltaY) { // an exclusively locked method
                long stamp = sl.writeLock();
                try {
                    x += deltaX;
                    y += deltaY;
                } finally {
                    sl.unlockWrite(stamp);
                }
            }

            double distanceFromOrigin() { // A read-only method
                double currentX, currentY;
                long stamp = sl.tryOptimisticRead();
                do {
                    if (stamp == 0L)
                        stamp = sl.readLock();
                    try {
                        // possibly racy reads
                        currentX = x;
                        currentY = y;
                    } finally {
                        stamp = sl.tryConvertToOptimisticRead(stamp);
                    }
                } while (stamp == 0);
                return Math.hypot(currentX, currentY);
            }

            double distanceFromOrigin2() {
                long stamp = sl.tryOptimisticRead();
                try {
                    retryHoldingLock:
                    for (;; stamp = sl.readLock()) {
                        if (stamp == 0L)
                            continue retryHoldingLock;
                        // possibly racy reads
                        double currentX = x;
                        double currentY = y;
                        if (!sl.validate(stamp))
                            continue retryHoldingLock;
                        return Math.hypot(currentX, currentY);
                    }
                } finally {
                    if (StampedLock.isReadLockStamp(stamp))
                        sl.unlockRead(stamp);
                }
            }

            void moveIfAtOrigin(double newX, double newY) {
                long stamp = sl.readLock();
                try {
                    while (x == 0.0 && y == 0.0) {
                        long ws = sl.tryConvertToWriteLock(stamp);
                        if (ws != 0L) {
                            stamp = ws;
                            x = newX;
                            y = newY;
                            return;
                        }
                        else {
                            sl.unlockRead(stamp);
                            stamp = sl.writeLock();
                        }
                    }
                } finally {
                    sl.unlock(stamp);
                }
            }
        }

        Point p = new Point();
        p.move(3.0, 4.0);
        assertEquals(5.0, p.distanceFromOrigin());
        p.moveIfAtOrigin(5.0, 12.0);
        assertEquals(5.0, p.distanceFromOrigin2());
    }

    /**
     * Stamp inspection methods work as expected, and do not inspect
     * the state of the lock itself.
     */
    public void testStampStateInspectionMethods() {
        StampedLock lock = new StampedLock();

        assertFalse(isWriteLockStamp(0L));
        assertFalse(isReadLockStamp(0L));
        assertFalse(isLockStamp(0L));
        assertFalse(isOptimisticReadStamp(0L));

        {
            long stamp = lock.writeLock();
            for (int i = 0; i < 2; i++) {
                assertTrue(isWriteLockStamp(stamp));
                assertFalse(isReadLockStamp(stamp));
                assertTrue(isLockStamp(stamp));
                assertFalse(isOptimisticReadStamp(stamp));
                if (i == 0)
                    lock.unlockWrite(stamp);
            }
        }

        {
            long stamp = lock.readLock();
            for (int i = 0; i < 2; i++) {
                assertFalse(isWriteLockStamp(stamp));
                assertTrue(isReadLockStamp(stamp));
                assertTrue(isLockStamp(stamp));
                assertFalse(isOptimisticReadStamp(stamp));
                if (i == 0)
                    lock.unlockRead(stamp);
            }
        }

        {
            long optimisticStamp = lock.tryOptimisticRead();
            long readStamp = lock.tryConvertToReadLock(optimisticStamp);
            long writeStamp = lock.tryConvertToWriteLock(readStamp);
            for (int i = 0; i < 2; i++) {
                assertFalse(isWriteLockStamp(optimisticStamp));
                assertFalse(isReadLockStamp(optimisticStamp));
                assertFalse(isLockStamp(optimisticStamp));
                assertTrue(isOptimisticReadStamp(optimisticStamp));

                assertFalse(isWriteLockStamp(readStamp));
                assertTrue(isReadLockStamp(readStamp));
                assertTrue(isLockStamp(readStamp));
                assertFalse(isOptimisticReadStamp(readStamp));

                assertTrue(isWriteLockStamp(writeStamp));
                assertFalse(isReadLockStamp(writeStamp));
                assertTrue(isLockStamp(writeStamp));
                assertFalse(isOptimisticReadStamp(writeStamp));
                if (i == 0)
                    lock.unlockWrite(writeStamp);
            }
        }
    }

    /**
     * Multiple threads repeatedly contend for the same lock.
     */
    public void testConcurrentAccess() throws Exception {
        final StampedLock sl = new StampedLock();
        final Lock wl = sl.asWriteLock();
        final Lock rl = sl.asReadLock();
        final long testDurationMillis = expensiveTests ? 1000 : 2;
        final int nTasks = ThreadLocalRandom.current().nextInt(1, 10);
        final AtomicBoolean done = new AtomicBoolean(false);
        final List<CompletableFuture<?>> futures = new ArrayList<>();
        final List<Callable<Long>> stampedWriteLockers = List.of(
            () -> sl.writeLock(),
            () -> writeLockInterruptiblyUninterrupted(sl),
            () -> tryWriteLockUninterrupted(sl, LONG_DELAY_MS, MILLISECONDS),
            () -> {
                long stamp;
                do { stamp = sl.tryConvertToWriteLock(sl.tryOptimisticRead()); }
                while (stamp == 0L);
                return stamp;
            },
            () -> {
              long stamp;
              do { stamp = sl.tryWriteLock(); } while (stamp == 0L);
              return stamp;
            },
            () -> {
              long stamp;
              do { stamp = sl.tryWriteLock(0L, DAYS); } while (stamp == 0L);
              return stamp;
            });
        final List<Callable<Long>> stampedReadLockers = List.of(
            () -> sl.readLock(),
            () -> readLockInterruptiblyUninterrupted(sl),
            () -> tryReadLockUninterrupted(sl, LONG_DELAY_MS, MILLISECONDS),
            () -> {
                long stamp;
                do { stamp = sl.tryConvertToReadLock(sl.tryOptimisticRead()); }
                while (stamp == 0L);
                return stamp;
            },
            () -> {
              long stamp;
              do { stamp = sl.tryReadLock(); } while (stamp == 0L);
              return stamp;
            },
            () -> {
              long stamp;
              do { stamp = sl.tryReadLock(0L, DAYS); } while (stamp == 0L);
              return stamp;
            });
        final List<Consumer<Long>> stampedWriteUnlockers = List.of(
            stamp -> sl.unlockWrite(stamp),
            stamp -> sl.unlock(stamp),
            stamp -> assertTrue(sl.tryUnlockWrite()),
            stamp -> wl.unlock(),
            stamp -> sl.tryConvertToOptimisticRead(stamp));
        final List<Consumer<Long>> stampedReadUnlockers = List.of(
            stamp -> sl.unlockRead(stamp),
            stamp -> sl.unlock(stamp),
            stamp -> assertTrue(sl.tryUnlockRead()),
            stamp -> rl.unlock(),
            stamp -> sl.tryConvertToOptimisticRead(stamp));
        final Action writer = () -> {
            // repeatedly acquires write lock
            var locker = chooseRandomly(stampedWriteLockers);
            var unlocker = chooseRandomly(stampedWriteUnlockers);
            while (!done.getAcquire()) {
                long stamp = locker.call();
                try {
                    assertTrue(isWriteLockStamp(stamp));
                    assertTrue(sl.isWriteLocked());
                    assertFalse(isReadLockStamp(stamp));
                    assertFalse(sl.isReadLocked());
                    assertEquals(0, sl.getReadLockCount());
                    assertTrue(sl.validate(stamp));
                } finally {
                    unlocker.accept(stamp);
                }
            }
        };
        final Action reader = () -> {
            // repeatedly acquires read lock
            var locker = chooseRandomly(stampedReadLockers);
            var unlocker = chooseRandomly(stampedReadUnlockers);
            while (!done.getAcquire()) {
                long stamp = locker.call();
                try {
                    assertFalse(isWriteLockStamp(stamp));
                    assertFalse(sl.isWriteLocked());
                    assertTrue(isReadLockStamp(stamp));
                    assertTrue(sl.isReadLocked());
                    assertTrue(sl.getReadLockCount() > 0);
                    assertTrue(sl.validate(stamp));
                } finally {
                    unlocker.accept(stamp);
                }
            }
        };
        for (int i = nTasks; i--> 0; ) {
            Action task = chooseRandomly(writer, reader);
            futures.add(CompletableFuture.runAsync(checkedRunnable(task)));
        }
        Thread.sleep(testDurationMillis);
        done.setRelease(true);
        for (var future : futures)
            checkTimedGet(future, null);
    }

}
