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
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ForkJoinPoolTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(ForkJoinPoolTest.class);
    }

    /*
     * Testing coverage notes:
     *
     * 1. shutdown and related methods are tested via super.joinPool.
     *
     * 2. newTaskFor and adapters are tested in submit/invoke tests
     *
     * 3. We cannot portably test monitoring methods such as
     * getStealCount() since they rely ultimately on random task
     * stealing that may cause tasks not to be stolen/propagated
     * across threads, especially on uniprocessors.
     *
     * 4. There are no independently testable ForkJoinWorkerThread
     * methods, but they are covered here and in task tests.
     */

    // Some classes to test extension and factory methods

    static class MyError extends Error {}

    // to test handlers
    static class FailingFJWSubclass extends ForkJoinWorkerThread {
        public FailingFJWSubclass(ForkJoinPool p) { super(p) ; }
        protected void onStart() { super.onStart(); throw new MyError(); }
    }

    static class FailingThreadFactory
            implements ForkJoinPool.ForkJoinWorkerThreadFactory {
        final AtomicInteger calls = new AtomicInteger(0);
        public ForkJoinWorkerThread newThread(ForkJoinPool p) {
            if (calls.incrementAndGet() > 1) return null;
            return new FailingFJWSubclass(p);
        }
    }

    static class SubFJP extends ForkJoinPool { // to expose protected
        SubFJP() { super(1); }
        public int drainTasksTo(Collection<? super ForkJoinTask<?>> c) {
            return super.drainTasksTo(c);
        }
        public ForkJoinTask<?> pollSubmission() {
            return super.pollSubmission();
        }
    }

    static class ManagedLocker implements ForkJoinPool.ManagedBlocker {
        final ReentrantLock lock;
        boolean hasLock = false;
        ManagedLocker(ReentrantLock lock) { this.lock = lock; }
        public boolean block() {
            if (!hasLock)
                lock.lock();
            return true;
        }
        public boolean isReleasable() {
            return hasLock || (hasLock = lock.tryLock());
        }
    }

    // A simple recursive task for testing
    static final class FibTask extends RecursiveTask<Integer> {
        final int number;
        FibTask(int n) { number = n; }
        protected Integer compute() {
            int n = number;
            if (n <= 1)
                return n;
            FibTask f1 = new FibTask(n - 1);
            f1.fork();
            return new FibTask(n - 2).compute() + f1.join();
        }
    }

    // A failing task for testing
    static final class FailingTask extends ForkJoinTask<Void> {
        public final Void getRawResult() { return null; }
        protected final void setRawResult(Void mustBeNull) { }
        protected final boolean exec() { throw new Error(); }
        FailingTask() {}
    }

    // Fib needlessly using locking to test ManagedBlockers
    static final class LockingFibTask extends RecursiveTask<Integer> {
        final int number;
        final ManagedLocker locker;
        final ReentrantLock lock;
        LockingFibTask(int n, ManagedLocker locker, ReentrantLock lock) {
            number = n;
            this.locker = locker;
            this.lock = lock;
        }
        protected Integer compute() {
            int n;
            LockingFibTask f1 = null;
            LockingFibTask f2 = null;
            locker.block();
            n = number;
            if (n > 1) {
                f1 = new LockingFibTask(n - 1, locker, lock);
                f2 = new LockingFibTask(n - 2, locker, lock);
            }
            lock.unlock();
            if (n <= 1)
                return n;
            else {
                f1.fork();
                return f2.compute() + f1.join();
            }
        }
    }

    /**
     * Successfully constructed pool reports default factory,
     * parallelism and async mode policies, no active threads or
     * tasks, and quiescent running state.
     */
    public void testDefaultInitialState() {
        ForkJoinPool p = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertSame(ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                       p.getFactory());
            assertFalse(p.getAsyncMode());
            assertEquals(0, p.getActiveThreadCount());
            assertEquals(0, p.getStealCount());
            assertEquals(0, p.getQueuedTaskCount());
            assertEquals(0, p.getQueuedSubmissionCount());
            assertFalse(p.hasQueuedSubmissions());
            assertFalse(p.isShutdown());
            assertFalse(p.isTerminating());
            assertFalse(p.isTerminated());
        }
    }

    /**
     * Constructor throws if size argument is less than zero
     */
    public void testConstructor1() {
        try {
            new ForkJoinPool(-1);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if factory argument is null
     */
    public void testConstructor2() {
        try {
            new ForkJoinPool(1, null, null, false);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * getParallelism returns size set in constructor
     */
    public void testGetParallelism() {
        ForkJoinPool p = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(1, p.getParallelism());
        }
    }

    /**
     * getPoolSize returns number of started workers.
     */
    public void testGetPoolSize() {
        final CountDownLatch taskStarted = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);
        final ForkJoinPool p = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(0, p.getActiveThreadCount());
            final Runnable task = new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    taskStarted.countDown();
                    assertEquals(1, p.getPoolSize());
                    assertEquals(1, p.getActiveThreadCount());
                    await(done);
                }};
            Future<?> future = p.submit(task);
            await(taskStarted);
            assertEquals(1, p.getPoolSize());
            assertEquals(1, p.getActiveThreadCount());
            done.countDown();
        }
        assertEquals(0, p.getPoolSize());
        assertEquals(0, p.getActiveThreadCount());
    }

    /**
     * awaitTermination on a non-shutdown pool times out
     */
    public void testAwaitTermination_timesOut() throws InterruptedException {
        ForkJoinPool p = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertFalse(p.isTerminated());
            assertFalse(p.awaitTermination(Long.MIN_VALUE, NANOSECONDS));
            assertFalse(p.awaitTermination(Long.MIN_VALUE, MILLISECONDS));
            assertFalse(p.awaitTermination(-1L, NANOSECONDS));
            assertFalse(p.awaitTermination(-1L, MILLISECONDS));
            assertFalse(p.awaitTermination(randomExpiredTimeout(),
                                           randomTimeUnit()));
            long timeoutNanos = 999999L;
            long startTime = System.nanoTime();
            assertFalse(p.awaitTermination(timeoutNanos, NANOSECONDS));
            assertTrue(System.nanoTime() - startTime >= timeoutNanos);
            assertFalse(p.isTerminated());
            startTime = System.nanoTime();
            long timeoutMillis = timeoutMillis();
            assertFalse(p.awaitTermination(timeoutMillis, MILLISECONDS));
            assertTrue(millisElapsedSince(startTime) >= timeoutMillis);
            assertFalse(p.isTerminated());
            p.shutdown();
            assertTrue(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(p.isTerminated());
        }
    }

    /**
     * setUncaughtExceptionHandler changes handler for uncaught exceptions.
     *
     * Additionally tests: Overriding ForkJoinWorkerThread.onStart
     * performs its defined action
     */
    public void testSetUncaughtExceptionHandler() throws InterruptedException {
        final CountDownLatch uehInvoked = new CountDownLatch(1);
        final Thread.UncaughtExceptionHandler ueh =
            new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    threadAssertTrue(e instanceof MyError);
                    threadAssertTrue(t instanceof FailingFJWSubclass);
                    uehInvoked.countDown();
                }};
        ForkJoinPool p = new ForkJoinPool(1, new FailingThreadFactory(),
                                          ueh, false);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertSame(ueh, p.getUncaughtExceptionHandler());
            try {
                p.execute(new FibTask(8));
                await(uehInvoked);
            } finally {
                p.shutdownNow(); // failure might have prevented processing task
            }
        }
    }

    /**
     * After invoking a single task, isQuiescent eventually becomes
     * true, at which time queues are empty, threads are not active,
     * the task has completed successfully, and construction
     * parameters continue to hold
     */
    public void testIsQuiescent() throws Exception {
        ForkJoinPool p = new ForkJoinPool(2);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertTrue(p.isQuiescent());
            long startTime = System.nanoTime();
            FibTask f = new FibTask(20);
            p.invoke(f);
            assertSame(ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                       p.getFactory());
            while (! p.isQuiescent()) {
                if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                    throw new AssertionError("timed out");
                assertFalse(p.getAsyncMode());
                assertFalse(p.isShutdown());
                assertFalse(p.isTerminating());
                assertFalse(p.isTerminated());
                Thread.yield();
            }

            assertTrue(p.isQuiescent());
            assertFalse(p.getAsyncMode());
            assertEquals(0, p.getQueuedTaskCount());
            assertEquals(0, p.getQueuedSubmissionCount());
            assertFalse(p.hasQueuedSubmissions());
            while (p.getActiveThreadCount() != 0
                   && millisElapsedSince(startTime) < LONG_DELAY_MS)
                Thread.yield();
            assertFalse(p.isShutdown());
            assertFalse(p.isTerminating());
            assertFalse(p.isTerminated());
            assertTrue(f.isDone());
            assertEquals(6765, (int) f.get());
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
    }

    /**
     * Completed submit(ForkJoinTask) returns result
     */
    public void testSubmitForkJoinTask() throws Throwable {
        ForkJoinPool p = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            ForkJoinTask<Integer> f = p.submit(new FibTask(8));
            assertEquals(21, (int) f.get());
        }
    }

    /**
     * A task submitted after shutdown is rejected
     */
    public void testSubmitAfterShutdown() {
        ForkJoinPool p = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            p.shutdown();
            assertTrue(p.isShutdown());
            try {
                ForkJoinTask<Integer> unused = p.submit(new FibTask(8));
                shouldThrow();
            } catch (RejectedExecutionException success) {}
        }
    }

    /**
     * Pool maintains parallelism when using ManagedBlocker
     */
    public void testBlockingForkJoinTask() throws Throwable {
        ForkJoinPool p = new ForkJoinPool(4);
        try {
            ReentrantLock lock = new ReentrantLock();
            ManagedLocker locker = new ManagedLocker(lock);
            ForkJoinTask<Integer> f = new LockingFibTask(20, locker, lock);
            p.execute(f);
            assertEquals(6765, (int) f.get());
        } finally {
            p.shutdownNow(); // don't wait out shutdown
        }
    }

    /**
     * pollSubmission returns unexecuted submitted task, if present
     */
    public void testPollSubmission() {
        final CountDownLatch done = new CountDownLatch(1);
        SubFJP p = new SubFJP();
        try (PoolCleaner cleaner = cleaner(p)) {
            ForkJoinTask<?> a = p.submit(awaiter(done));
            ForkJoinTask<?> b = p.submit(awaiter(done));
            ForkJoinTask<?> c = p.submit(awaiter(done));
            ForkJoinTask<?> r = p.pollSubmission();
            assertTrue(r == a || r == b || r == c);
            assertFalse(r.isDone());
            done.countDown();
        }
    }

    /**
     * drainTasksTo transfers unexecuted submitted tasks, if present
     */
    public void testDrainTasksTo() {
        final CountDownLatch done = new CountDownLatch(1);
        SubFJP p = new SubFJP();
        try (PoolCleaner cleaner = cleaner(p)) {
            ForkJoinTask<?> a = p.submit(awaiter(done));
            ForkJoinTask<?> b = p.submit(awaiter(done));
            ForkJoinTask<?> c = p.submit(awaiter(done));
            ArrayList<ForkJoinTask<?>> al = new ArrayList<>();
            p.drainTasksTo(al);
            assertTrue(al.size() > 0);
            for (ForkJoinTask<?> r : al) {
                assertTrue(r == a || r == b || r == c);
                assertFalse(r.isDone());
            }
            done.countDown();
        }
    }

    // FJ Versions of AbstractExecutorService tests

    /**
     * execute(runnable) runs it to completion
     */
    public void testExecuteRunnable() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            final AtomicBoolean done = new AtomicBoolean(false);
            Future<?> future = e.submit(new CheckedRunnable() {
                public void realRun() {
                    done.set(true);
                }});
            assertNull(future.get());
            assertNull(future.get(randomExpiredTimeout(), randomTimeUnit()));
            assertTrue(done.get());
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
        }
    }

    /**
     * Completed submit(callable) returns result
     */
    public void testSubmitCallable() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            Future<String> future = e.submit(new StringTask());
            assertSame(TEST_STRING, future.get());
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
        }
    }

    /**
     * Completed submit(runnable) returns successfully
     */
    public void testSubmitRunnable() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            Future<?> future = e.submit(new NoOpRunnable());
            assertNull(future.get());
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
        }
    }

    /**
     * Completed submit(runnable, result) returns result
     */
    public void testSubmitRunnable2() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            Future<String> future = e.submit(new NoOpRunnable(), TEST_STRING);
            assertSame(TEST_STRING, future.get());
            assertTrue(future.isDone());
            assertFalse(future.isCancelled());
        }
    }

    /**
     * A submitted privileged action runs to completion
     */
    public void testSubmitPrivilegedAction() throws Exception {
        final Callable<Object> callable = Executors.callable(new PrivilegedAction<Object>() {
                public Object run() { return TEST_STRING; }});
        Runnable r = new CheckedRunnable() {
        public void realRun() throws Exception {
            ExecutorService e = new ForkJoinPool(1);
            try (PoolCleaner cleaner = cleaner(e)) {
                Future<Object> future = e.submit(callable);
                assertSame(TEST_STRING, future.get());
            }
        }};

        runWithPermissions(r, new RuntimePermission("modifyThread"));
    }

    /**
     * A submitted privileged exception action runs to completion
     */
    public void testSubmitPrivilegedExceptionAction() throws Exception {
        final Callable<Object> callable =
            Executors.callable(new PrivilegedExceptionAction<Object>() {
                public Object run() { return TEST_STRING; }});
        Runnable r = new CheckedRunnable() {
        public void realRun() throws Exception {
            ExecutorService e = new ForkJoinPool(1);
            try (PoolCleaner cleaner = cleaner(e)) {
                Future<Object> future = e.submit(callable);
                assertSame(TEST_STRING, future.get());
            }
        }};

        runWithPermissions(r, new RuntimePermission("modifyThread"));
    }

    /**
     * A submitted failed privileged exception action reports exception
     */
    public void testSubmitFailedPrivilegedExceptionAction() throws Exception {
        final Callable<Object> callable =
            Executors.callable(new PrivilegedExceptionAction<Object>() {
                public Object run() { throw new IndexOutOfBoundsException(); }});
        Runnable r = new CheckedRunnable() {
        public void realRun() throws Exception {
            ExecutorService e = new ForkJoinPool(1);
            try (PoolCleaner cleaner = cleaner(e)) {
                Future<Object> future = e.submit(callable);
                try {
                    future.get();
                    shouldThrow();
                } catch (ExecutionException success) {
                    assertTrue(success.getCause() instanceof IndexOutOfBoundsException);
                }
            }
        }};

        runWithPermissions(r, new RuntimePermission("modifyThread"));
    }

    /**
     * execute(null runnable) throws NullPointerException
     */
    public void testExecuteNullRunnable() {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                Future<?> unused = e.submit((Runnable) null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * submit(null callable) throws NullPointerException
     */
    public void testSubmitNullCallable() {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                Future<String> unused = e.submit((Callable<String>) null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * submit(callable).get() throws InterruptedException if interrupted
     */
    public void testInterruptedSubmit() throws InterruptedException {
        final CountDownLatch submitted    = new CountDownLatch(1);
        final CountDownLatch quittingTime = new CountDownLatch(1);
        final Callable<Void> awaiter = new CheckedCallable<>() {
            public Void realCall() throws InterruptedException {
                assertTrue(quittingTime.await(2*LONG_DELAY_MS, MILLISECONDS));
                return null;
            }};
        final ExecutorService p = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(p, quittingTime)) {
            Thread t = new Thread(new CheckedInterruptedRunnable() {
                public void realRun() throws Exception {
                    Future<Void> future = p.submit(awaiter);
                    submitted.countDown();
                    future.get();
                }});
            t.start();
            await(submitted);
            t.interrupt();
            awaitTermination(t);
        }
    }

    /**
     * get of submit(callable) throws ExecutionException if callable
     * throws exception
     */
    public void testSubmitEE() throws Throwable {
        ForkJoinPool p = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            try {
                p.submit(new Callable<Object>() {
                        public Object call() { throw new ArithmeticException(); }})
                    .get();
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof ArithmeticException);
            }
        }
    }

    /**
     * invokeAny(null) throws NullPointerException
     */
    public void testInvokeAny1() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * invokeAny(empty collection) throws IllegalArgumentException
     */
    public void testInvokeAny2() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(new ArrayList<Callable<String>>());
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * invokeAny(c) throws NullPointerException if c has a single null element
     */
    public void testInvokeAny3() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(null);
            try {
                e.invokeAny(l);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * invokeAny(c) throws NullPointerException if c has null elements
     */
    public void testInvokeAny4() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(latchAwaitingStringTask(latch));
            l.add(null);
            try {
                e.invokeAny(l);
                shouldThrow();
            } catch (NullPointerException success) {}
            latch.countDown();
        }
    }

    /**
     * invokeAny(c) throws ExecutionException if no task in c completes
     */
    public void testInvokeAny5() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            try {
                e.invokeAny(l);
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof NullPointerException);
            }
        }
    }

    /**
     * invokeAny(c) returns result of some task in c if at least one completes
     */
    public void testInvokeAny6() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            String result = e.invokeAny(l);
            assertSame(TEST_STRING, result);
        }
    }

    /**
     * invokeAll(null) throws NullPointerException
     */
    public void testInvokeAll1() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAll(null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * invokeAll(empty collection) returns empty list
     */
    public void testInvokeAll2() throws InterruptedException {
        ExecutorService e = new ForkJoinPool(1);
        final Collection<Callable<String>> emptyCollection
            = Collections.emptyList();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Future<String>> r = e.invokeAll(emptyCollection);
            assertTrue(r.isEmpty());
        }
    }

    /**
     * invokeAll(c) throws NullPointerException if c has null elements
     */
    public void testInvokeAll3() throws InterruptedException {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(null);
            try {
                e.invokeAll(l);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * get of returned element of invokeAll(c) throws
     * ExecutionException on failed task
     */
    public void testInvokeAll4() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            List<Future<String>> futures = e.invokeAll(l);
            assertEquals(1, futures.size());
            try {
                futures.get(0).get();
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof NullPointerException);
            }
        }
    }

    /**
     * invokeAll(c) returns results of all completed tasks in c
     */
    public void testInvokeAll5() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            List<Future<String>> futures = e.invokeAll(l);
            assertEquals(2, futures.size());
            for (Future<String> future : futures)
                assertSame(TEST_STRING, future.get());
        }
    }

    /**
     * timed invokeAny(null) throws NullPointerException
     */
    public void testTimedInvokeAny1() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(null, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAny(null time unit) throws NullPointerException
     */
    public void testTimedInvokeAnyNullTimeUnit() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            try {
                e.invokeAny(l, randomTimeout(), null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAny(empty collection) throws IllegalArgumentException
     */
    public void testTimedInvokeAny2() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(new ArrayList<Callable<String>>(),
                            randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * timed invokeAny(c) throws NullPointerException if c has null elements
     */
    public void testTimedInvokeAny3() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(latchAwaitingStringTask(latch));
            l.add(null);
            try {
                e.invokeAny(l, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (NullPointerException success) {}
            latch.countDown();
        }
    }

    /**
     * timed invokeAny(c) throws ExecutionException if no task completes
     */
    public void testTimedInvokeAny4() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            long startTime = System.nanoTime();
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            try {
                e.invokeAny(l, LONG_DELAY_MS, MILLISECONDS);
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof NullPointerException);
            }
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
    }

    /**
     * timed invokeAny(c) returns result of some task in c
     */
    public void testTimedInvokeAny5() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            long startTime = System.nanoTime();
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            String result = e.invokeAny(l, LONG_DELAY_MS, MILLISECONDS);
            assertSame(TEST_STRING, result);
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
    }

    /**
     * timed invokeAll(null) throws NullPointerException
     */
    public void testTimedInvokeAll1() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAll(null, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAll(null time unit) throws NullPointerException
     */
    public void testTimedInvokeAllNullTimeUnit() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            try {
                e.invokeAll(l, randomTimeout(), null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAll(empty collection) returns empty list
     */
    public void testTimedInvokeAll2() throws InterruptedException {
        ExecutorService e = new ForkJoinPool(1);
        final Collection<Callable<String>> emptyCollection
            = Collections.emptyList();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Future<String>> r
                = e.invokeAll(emptyCollection,
                              randomTimeout(), randomTimeUnit());
            assertTrue(r.isEmpty());
        }
    }

    /**
     * timed invokeAll(c) throws NullPointerException if c has null elements
     */
    public void testTimedInvokeAll3() throws InterruptedException {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(null);
            try {
                e.invokeAll(l, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * get of returned element of invokeAll(c) throws exception on failed task
     */
    public void testTimedInvokeAll4() throws Throwable {
        ExecutorService e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            List<Future<String>> futures
                = e.invokeAll(l, LONG_DELAY_MS, MILLISECONDS);
            assertEquals(1, futures.size());
            try {
                futures.get(0).get();
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof NullPointerException);
            }
        }
    }

    /**
     * timed invokeAll(c) returns results of all completed tasks in c
     */
    public void testTimedInvokeAll5() throws Throwable {
        ForkJoinPool e = new ForkJoinPool(1);
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            List<Future<String>> futures
                = e.invokeAll(l, LONG_DELAY_MS, MILLISECONDS);
            assertEquals(2, futures.size());
            for (Future<String> future : futures)
                assertSame(TEST_STRING, future.get());
        }
    }

}
