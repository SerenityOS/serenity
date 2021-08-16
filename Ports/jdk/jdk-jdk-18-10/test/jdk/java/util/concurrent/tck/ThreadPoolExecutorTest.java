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
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ThreadPoolExecutorTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ThreadPoolExecutorTest.class);
    }

    static class ExtendedTPE extends ThreadPoolExecutor {
        final CountDownLatch beforeCalled = new CountDownLatch(1);
        final CountDownLatch afterCalled = new CountDownLatch(1);
        final CountDownLatch terminatedCalled = new CountDownLatch(1);

        public ExtendedTPE() {
            super(1, 1, LONG_DELAY_MS, MILLISECONDS, new SynchronousQueue<Runnable>());
        }
        protected void beforeExecute(Thread t, Runnable r) {
            beforeCalled.countDown();
        }
        protected void afterExecute(Runnable r, Throwable t) {
            afterCalled.countDown();
        }
        protected void terminated() {
            terminatedCalled.countDown();
        }

        public boolean beforeCalled() {
            return beforeCalled.getCount() == 0;
        }
        public boolean afterCalled() {
            return afterCalled.getCount() == 0;
        }
        public boolean terminatedCalled() {
            return terminatedCalled.getCount() == 0;
        }
    }

    static class FailingThreadFactory implements ThreadFactory {
        int calls = 0;
        public Thread newThread(Runnable r) {
            if (++calls > 1) return null;
            return new Thread(r);
        }
    }

    /**
     * execute successfully executes a runnable
     */
    public void testExecute() throws InterruptedException {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch done = new CountDownLatch(1);
            final Runnable task = new CheckedRunnable() {
                public void realRun() { done.countDown(); }};
            p.execute(task);
            await(done);
        }
    }

    /**
     * getActiveCount increases but doesn't overestimate, when a
     * thread becomes active
     */
    public void testGetActiveCount() throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p, done)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            assertEquals(0, p.getActiveCount());
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadStarted.countDown();
                    assertEquals(1, p.getActiveCount());
                    await(done);
                }});
            await(threadStarted);
            assertEquals(1, p.getActiveCount());
        }
    }

    /**
     * prestartCoreThread starts a thread if under corePoolSize, else doesn't
     */
    public void testPrestartCoreThread() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(2, 6,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(0, p.getPoolSize());
            assertTrue(p.prestartCoreThread());
            assertEquals(1, p.getPoolSize());
            assertTrue(p.prestartCoreThread());
            assertEquals(2, p.getPoolSize());
            assertFalse(p.prestartCoreThread());
            assertEquals(2, p.getPoolSize());
            p.setCorePoolSize(4);
            assertTrue(p.prestartCoreThread());
            assertEquals(3, p.getPoolSize());
            assertTrue(p.prestartCoreThread());
            assertEquals(4, p.getPoolSize());
            assertFalse(p.prestartCoreThread());
            assertEquals(4, p.getPoolSize());
        }
    }

    /**
     * prestartAllCoreThreads starts all corePoolSize threads
     */
    public void testPrestartAllCoreThreads() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(2, 6,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(0, p.getPoolSize());
            p.prestartAllCoreThreads();
            assertEquals(2, p.getPoolSize());
            p.prestartAllCoreThreads();
            assertEquals(2, p.getPoolSize());
            p.setCorePoolSize(4);
            p.prestartAllCoreThreads();
            assertEquals(4, p.getPoolSize());
            p.prestartAllCoreThreads();
            assertEquals(4, p.getPoolSize());
        }
    }

    /**
     * getCompletedTaskCount increases, but doesn't overestimate,
     * when tasks complete
     */
    public void testGetCompletedTaskCount() throws InterruptedException {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch threadProceed = new CountDownLatch(1);
            final CountDownLatch threadDone = new CountDownLatch(1);
            assertEquals(0, p.getCompletedTaskCount());
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadStarted.countDown();
                    assertEquals(0, p.getCompletedTaskCount());
                    await(threadProceed);
                    threadDone.countDown();
                }});
            await(threadStarted);
            assertEquals(0, p.getCompletedTaskCount());
            threadProceed.countDown();
            await(threadDone);
            long startTime = System.nanoTime();
            while (p.getCompletedTaskCount() != 1) {
                if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                    fail("timed out");
                Thread.yield();
            }
        }
    }

    /**
     * getCorePoolSize returns size given in constructor if not otherwise set
     */
    public void testGetCorePoolSize() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(1, p.getCorePoolSize());
        }
    }

    /**
     * getKeepAliveTime returns value given in constructor if not otherwise set
     */
    public void testGetKeepAliveTime() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(2, 2,
                                   1000, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(1, p.getKeepAliveTime(SECONDS));
        }
    }

    /**
     * getThreadFactory returns factory in constructor if not set
     */
    public void testGetThreadFactory() {
        ThreadFactory threadFactory = new SimpleThreadFactory();
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   threadFactory,
                                   new NoOpREHandler());
        try (PoolCleaner cleaner = cleaner(p)) {
            assertSame(threadFactory, p.getThreadFactory());
        }
    }

    /**
     * setThreadFactory sets the thread factory returned by getThreadFactory
     */
    public void testSetThreadFactory() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            ThreadFactory threadFactory = new SimpleThreadFactory();
            p.setThreadFactory(threadFactory);
            assertSame(threadFactory, p.getThreadFactory());
        }
    }

    /**
     * setThreadFactory(null) throws NPE
     */
    public void testSetThreadFactoryNull() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            try {
                p.setThreadFactory(null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * The default rejected execution handler is AbortPolicy.
     */
    public void testDefaultRejectedExecutionHandler() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertTrue(p.getRejectedExecutionHandler() instanceof AbortPolicy);
        }
    }

    /**
     * getRejectedExecutionHandler returns handler in constructor if not set
     */
    public void testGetRejectedExecutionHandler() {
        final RejectedExecutionHandler handler = new NoOpREHandler();
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   handler);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertSame(handler, p.getRejectedExecutionHandler());
        }
    }

    /**
     * setRejectedExecutionHandler sets the handler returned by
     * getRejectedExecutionHandler
     */
    public void testSetRejectedExecutionHandler() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            RejectedExecutionHandler handler = new NoOpREHandler();
            p.setRejectedExecutionHandler(handler);
            assertSame(handler, p.getRejectedExecutionHandler());
        }
    }

    /**
     * setRejectedExecutionHandler(null) throws NPE
     */
    public void testSetRejectedExecutionHandlerNull() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            try {
                p.setRejectedExecutionHandler(null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * getLargestPoolSize increases, but doesn't overestimate, when
     * multiple threads active
     */
    public void testGetLargestPoolSize() throws InterruptedException {
        final int THREADS = 3;
        final CountDownLatch done = new CountDownLatch(1);
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(THREADS, THREADS,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p, done)) {
            assertEquals(0, p.getLargestPoolSize());
            final CountDownLatch threadsStarted = new CountDownLatch(THREADS);
            for (int i = 0; i < THREADS; i++)
                p.execute(new CheckedRunnable() {
                    public void realRun() throws InterruptedException {
                        threadsStarted.countDown();
                        await(done);
                        assertEquals(THREADS, p.getLargestPoolSize());
                    }});
            await(threadsStarted);
            assertEquals(THREADS, p.getLargestPoolSize());
        }
        assertEquals(THREADS, p.getLargestPoolSize());
    }

    /**
     * getMaximumPoolSize returns value given in constructor if not
     * otherwise set
     */
    public void testGetMaximumPoolSize() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(2, 3,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(3, p.getMaximumPoolSize());
            p.setMaximumPoolSize(5);
            assertEquals(5, p.getMaximumPoolSize());
            p.setMaximumPoolSize(4);
            assertEquals(4, p.getMaximumPoolSize());
        }
    }

    /**
     * getPoolSize increases, but doesn't overestimate, when threads
     * become active
     */
    public void testGetPoolSize() throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p, done)) {
            assertEquals(0, p.getPoolSize());
            final CountDownLatch threadStarted = new CountDownLatch(1);
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadStarted.countDown();
                    assertEquals(1, p.getPoolSize());
                    await(done);
                }});
            await(threadStarted);
            assertEquals(1, p.getPoolSize());
        }
    }

    /**
     * getTaskCount increases, but doesn't overestimate, when tasks submitted
     */
    public void testGetTaskCount() throws InterruptedException {
        final int TASKS = 3;
        final CountDownLatch done = new CountDownLatch(1);
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p, done)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            assertEquals(0, p.getTaskCount());
            assertEquals(0, p.getCompletedTaskCount());
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadStarted.countDown();
                    await(done);
                }});
            await(threadStarted);
            assertEquals(1, p.getTaskCount());
            assertEquals(0, p.getCompletedTaskCount());
            for (int i = 0; i < TASKS; i++) {
                assertEquals(1 + i, p.getTaskCount());
                p.execute(new CheckedRunnable() {
                    public void realRun() throws InterruptedException {
                        threadStarted.countDown();
                        assertEquals(1 + TASKS, p.getTaskCount());
                        await(done);
                    }});
            }
            assertEquals(1 + TASKS, p.getTaskCount());
            assertEquals(0, p.getCompletedTaskCount());
        }
        assertEquals(1 + TASKS, p.getTaskCount());
        assertEquals(1 + TASKS, p.getCompletedTaskCount());
    }

    /**
     * isShutdown is false before shutdown, true after
     */
    public void testIsShutdown() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertFalse(p.isShutdown());
            try { p.shutdown(); } catch (SecurityException ok) { return; }
            assertTrue(p.isShutdown());
        }
    }

    /**
     * awaitTermination on a non-shutdown pool times out
     */
    public void testAwaitTermination_timesOut() throws InterruptedException {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
            try { p.shutdown(); } catch (SecurityException ok) { return; }
            assertTrue(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(p.isTerminated());
        }
    }

    /**
     * isTerminated is false before termination, true after
     */
    public void testIsTerminated() throws InterruptedException {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(1);
            assertFalse(p.isTerminating());
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertFalse(p.isTerminating());
                    threadStarted.countDown();
                    await(done);
                }});
            await(threadStarted);
            assertFalse(p.isTerminating());
            done.countDown();
            try { p.shutdown(); } catch (SecurityException ok) { return; }
            assertTrue(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(p.isTerminated());
            assertFalse(p.isTerminating());
        }
    }

    /**
     * isTerminating is not true when running or when terminated
     */
    public void testIsTerminating() throws InterruptedException {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(1);
            assertFalse(p.isTerminating());
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertFalse(p.isTerminating());
                    threadStarted.countDown();
                    await(done);
                }});
            await(threadStarted);
            assertFalse(p.isTerminating());
            done.countDown();
            try { p.shutdown(); } catch (SecurityException ok) { return; }
            assertTrue(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(p.isTerminated());
            assertFalse(p.isTerminating());
        }
    }

    /**
     * getQueue returns the work queue, which contains queued tasks
     */
    public void testGetQueue() throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        final BlockingQueue<Runnable> q = new ArrayBlockingQueue<>(10);
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   q);
        try (PoolCleaner cleaner = cleaner(p, done)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            FutureTask[] rtasks = new FutureTask[5];
            @SuppressWarnings("unchecked")
            FutureTask<Boolean>[] tasks = (FutureTask<Boolean>[])rtasks;
            for (int i = 0; i < tasks.length; i++) {
                Callable<Boolean> task = new CheckedCallable<>() {
                    public Boolean realCall() throws InterruptedException {
                        threadStarted.countDown();
                        assertSame(q, p.getQueue());
                        await(done);
                        return Boolean.TRUE;
                    }};
                tasks[i] = new FutureTask<>(task);
                p.execute(tasks[i]);
            }
            await(threadStarted);
            assertSame(q, p.getQueue());
            assertFalse(q.contains(tasks[0]));
            assertTrue(q.contains(tasks[tasks.length - 1]));
            assertEquals(tasks.length - 1, q.size());
        }
    }

    /**
     * remove(task) removes queued task, and fails to remove active task
     */
    public void testRemove() throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        BlockingQueue<Runnable> q = new ArrayBlockingQueue<>(10);
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   q);
        try (PoolCleaner cleaner = cleaner(p, done)) {
            Runnable[] tasks = new Runnable[6];
            final CountDownLatch threadStarted = new CountDownLatch(1);
            for (int i = 0; i < tasks.length; i++) {
                tasks[i] = new CheckedRunnable() {
                    public void realRun() throws InterruptedException {
                        threadStarted.countDown();
                        await(done);
                    }};
                p.execute(tasks[i]);
            }
            await(threadStarted);
            assertFalse(p.remove(tasks[0]));
            assertTrue(q.contains(tasks[4]));
            assertTrue(q.contains(tasks[3]));
            assertTrue(p.remove(tasks[4]));
            assertFalse(p.remove(tasks[4]));
            assertFalse(q.contains(tasks[4]));
            assertTrue(q.contains(tasks[3]));
            assertTrue(p.remove(tasks[3]));
            assertFalse(q.contains(tasks[3]));
        }
    }

    /**
     * purge removes cancelled tasks from the queue
     */
    public void testPurge() throws InterruptedException {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);
        final BlockingQueue<Runnable> q = new ArrayBlockingQueue<>(10);
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   q);
        try (PoolCleaner cleaner = cleaner(p, done)) {
            FutureTask[] rtasks = new FutureTask[5];
            @SuppressWarnings("unchecked")
            FutureTask<Boolean>[] tasks = (FutureTask<Boolean>[])rtasks;
            for (int i = 0; i < tasks.length; i++) {
                Callable<Boolean> task = new CheckedCallable<>() {
                    public Boolean realCall() throws InterruptedException {
                        threadStarted.countDown();
                        await(done);
                        return Boolean.TRUE;
                    }};
                tasks[i] = new FutureTask<>(task);
                p.execute(tasks[i]);
            }
            await(threadStarted);
            assertEquals(tasks.length, p.getTaskCount());
            assertEquals(tasks.length - 1, q.size());
            assertEquals(1L, p.getActiveCount());
            assertEquals(0L, p.getCompletedTaskCount());
            tasks[4].cancel(true);
            tasks[3].cancel(false);
            p.purge();
            assertEquals(tasks.length - 3, q.size());
            assertEquals(tasks.length - 2, p.getTaskCount());
            p.purge();         // Nothing to do
            assertEquals(tasks.length - 3, q.size());
            assertEquals(tasks.length - 2, p.getTaskCount());
        }
    }

    /**
     * shutdownNow returns a list containing tasks that were not run,
     * and those tasks are drained from the queue
     */
    public void testShutdownNow() throws InterruptedException {
        final int poolSize = 2;
        final int count = 5;
        final AtomicInteger ran = new AtomicInteger(0);
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(poolSize, poolSize,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        final CountDownLatch threadsStarted = new CountDownLatch(poolSize);
        Runnable waiter = new CheckedRunnable() { public void realRun() {
            threadsStarted.countDown();
            try {
                MILLISECONDS.sleep(LONGER_DELAY_MS);
            } catch (InterruptedException success) {}
            ran.getAndIncrement();
        }};
        for (int i = 0; i < count; i++)
            p.execute(waiter);
        await(threadsStarted);
        assertEquals(poolSize, p.getActiveCount());
        assertEquals(0, p.getCompletedTaskCount());
        final List<Runnable> queuedTasks;
        try {
            queuedTasks = p.shutdownNow();
        } catch (SecurityException ok) {
            return; // Allowed in case test doesn't have privs
        }
        assertTrue(p.isShutdown());
        assertTrue(p.getQueue().isEmpty());
        assertEquals(count - poolSize, queuedTasks.size());
        assertTrue(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
        assertTrue(p.isTerminated());
        assertEquals(poolSize, ran.get());
        assertEquals(poolSize, p.getCompletedTaskCount());
    }

    // Exception Tests

    /**
     * Constructor throws if corePoolSize argument is less than zero
     */
    public void testConstructor1() {
        try {
            new ThreadPoolExecutor(-1, 1, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is less than zero
     */
    public void testConstructor2() {
        try {
            new ThreadPoolExecutor(1, -1, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is equal to zero
     */
    public void testConstructor3() {
        try {
            new ThreadPoolExecutor(1, 0, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if keepAliveTime is less than zero
     */
    public void testConstructor4() {
        try {
            new ThreadPoolExecutor(1, 2, -1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if corePoolSize is greater than the maximumPoolSize
     */
    public void testConstructor5() {
        try {
            new ThreadPoolExecutor(2, 1, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if workQueue is set to null
     */
    public void testConstructorNullPointerException() {
        try {
            new ThreadPoolExecutor(1, 2, 1L, SECONDS,
                                   (BlockingQueue<Runnable>) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if corePoolSize argument is less than zero
     */
    public void testConstructor6() {
        try {
            new ThreadPoolExecutor(-1, 1, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new SimpleThreadFactory());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is less than zero
     */
    public void testConstructor7() {
        try {
            new ThreadPoolExecutor(1, -1, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new SimpleThreadFactory());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is equal to zero
     */
    public void testConstructor8() {
        try {
            new ThreadPoolExecutor(1, 0, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new SimpleThreadFactory());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if keepAliveTime is less than zero
     */
    public void testConstructor9() {
        try {
            new ThreadPoolExecutor(1, 2, -1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new SimpleThreadFactory());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if corePoolSize is greater than the maximumPoolSize
     */
    public void testConstructor10() {
        try {
            new ThreadPoolExecutor(2, 1, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new SimpleThreadFactory());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if workQueue is set to null
     */
    public void testConstructorNullPointerException2() {
        try {
            new ThreadPoolExecutor(1, 2, 1L, SECONDS,
                                   (BlockingQueue<Runnable>) null,
                                   new SimpleThreadFactory());
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if threadFactory is set to null
     */
    public void testConstructorNullPointerException3() {
        try {
            new ThreadPoolExecutor(1, 2, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   (ThreadFactory) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if corePoolSize argument is less than zero
     */
    public void testConstructor11() {
        try {
            new ThreadPoolExecutor(-1, 1, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is less than zero
     */
    public void testConstructor12() {
        try {
            new ThreadPoolExecutor(1, -1, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is equal to zero
     */
    public void testConstructor13() {
        try {
            new ThreadPoolExecutor(1, 0, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if keepAliveTime is less than zero
     */
    public void testConstructor14() {
        try {
            new ThreadPoolExecutor(1, 2, -1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if corePoolSize is greater than the maximumPoolSize
     */
    public void testConstructor15() {
        try {
            new ThreadPoolExecutor(2, 1, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if workQueue is set to null
     */
    public void testConstructorNullPointerException4() {
        try {
            new ThreadPoolExecutor(1, 2, 1L, SECONDS,
                                   (BlockingQueue<Runnable>) null,
                                   new NoOpREHandler());
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if handler is set to null
     */
    public void testConstructorNullPointerException5() {
        try {
            new ThreadPoolExecutor(1, 2, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   (RejectedExecutionHandler) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if corePoolSize argument is less than zero
     */
    public void testConstructor16() {
        try {
            new ThreadPoolExecutor(-1, 1, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new SimpleThreadFactory(),
                                   new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is less than zero
     */
    public void testConstructor17() {
        try {
            new ThreadPoolExecutor(1, -1, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new SimpleThreadFactory(),
                                   new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is equal to zero
     */
    public void testConstructor18() {
        try {
            new ThreadPoolExecutor(1, 0, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new SimpleThreadFactory(),
                                   new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if keepAliveTime is less than zero
     */
    public void testConstructor19() {
        try {
            new ThreadPoolExecutor(1, 2, -1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new SimpleThreadFactory(),
                                   new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if corePoolSize is greater than the maximumPoolSize
     */
    public void testConstructor20() {
        try {
            new ThreadPoolExecutor(2, 1, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new SimpleThreadFactory(),
                                   new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if workQueue is null
     */
    public void testConstructorNullPointerException6() {
        try {
            new ThreadPoolExecutor(1, 2, 1L, SECONDS,
                                   (BlockingQueue<Runnable>) null,
                                   new SimpleThreadFactory(),
                                   new NoOpREHandler());
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if handler is null
     */
    public void testConstructorNullPointerException7() {
        try {
            new ThreadPoolExecutor(1, 2, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   new SimpleThreadFactory(),
                                   (RejectedExecutionHandler) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if ThreadFactory is null
     */
    public void testConstructorNullPointerException8() {
        try {
            new ThreadPoolExecutor(1, 2, 1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10),
                                   (ThreadFactory) null,
                                   new NoOpREHandler());
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * get of submitted callable throws InterruptedException if interrupted
     */
    public void testInterruptedSubmit() throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   60, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));

        try (PoolCleaner cleaner = cleaner(p, done)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            Thread t = newStartedThread(new CheckedInterruptedRunnable() {
                public void realRun() throws Exception {
                    Callable<Boolean> task = new CheckedCallable<>() {
                        public Boolean realCall() throws InterruptedException {
                            threadStarted.countDown();
                            await(done);
                            return Boolean.TRUE;
                        }};
                    p.submit(task).get();
                }});

            await(threadStarted); // ensure quiescence
            t.interrupt();
            awaitTermination(t);
        }
    }

    /**
     * Submitted tasks are rejected when saturated or shutdown
     */
    public void testSubmittedTasksRejectedWhenSaturatedOrShutdown() throws InterruptedException {
        final ThreadPoolExecutor p = new ThreadPoolExecutor(
            1, 1, 1, SECONDS, new ArrayBlockingQueue<Runnable>(1));
        final int saturatedSize = saturatedSize(p);
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final CountDownLatch threadsStarted = new CountDownLatch(p.getMaximumPoolSize());
        final CountDownLatch done = new CountDownLatch(1);
        final Runnable r = () -> {
            threadsStarted.countDown();
            for (;;) {
                try {
                    done.await();
                    return;
                } catch (InterruptedException shutdownNowDeliberatelyIgnored) {}
            }};
        final Callable<Boolean> c = () -> {
            threadsStarted.countDown();
            for (;;) {
                try {
                    done.await();
                    return Boolean.TRUE;
                } catch (InterruptedException shutdownNowDeliberatelyIgnored) {}
            }};
        final boolean shutdownNow = rnd.nextBoolean();

        try (PoolCleaner cleaner = cleaner(p, done)) {
            // saturate
            for (int i = saturatedSize; i--> 0; ) {
                switch (rnd.nextInt(4)) {
                case 0: p.execute(r); break;
                case 1: assertFalse(p.submit(r).isDone()); break;
                case 2: assertFalse(p.submit(r, Boolean.TRUE).isDone()); break;
                case 3: assertFalse(p.submit(c).isDone()); break;
                }
            }

            await(threadsStarted);
            assertTaskSubmissionsAreRejected(p);

            if (shutdownNow)
                p.shutdownNow();
            else
                p.shutdown();
            // Pool is shutdown, but not yet terminated
            assertTaskSubmissionsAreRejected(p);
            assertFalse(p.isTerminated());

            done.countDown();   // release blocking tasks
            assertTrue(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));

            assertTaskSubmissionsAreRejected(p);
        }
        assertEquals(saturatedSize(p)
                     - (shutdownNow ? p.getQueue().remainingCapacity() : 0),
                     p.getCompletedTaskCount());
    }

    /**
     * executor using DiscardOldestPolicy drops oldest task if saturated.
     */
    public void testSaturatedExecute_DiscardOldestPolicy() {
        final CountDownLatch done = new CountDownLatch(1);
        LatchAwaiter r1 = awaiter(done);
        LatchAwaiter r2 = awaiter(done);
        LatchAwaiter r3 = awaiter(done);
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(1),
                                   new DiscardOldestPolicy());
        try (PoolCleaner cleaner = cleaner(p, done)) {
            assertEquals(LatchAwaiter.NEW, r1.state);
            assertEquals(LatchAwaiter.NEW, r2.state);
            assertEquals(LatchAwaiter.NEW, r3.state);
            p.execute(r1);
            p.execute(r2);
            assertTrue(p.getQueue().contains(r2));
            p.execute(r3);
            assertFalse(p.getQueue().contains(r2));
            assertTrue(p.getQueue().contains(r3));
        }
        assertEquals(LatchAwaiter.DONE, r1.state);
        assertEquals(LatchAwaiter.NEW, r2.state);
        assertEquals(LatchAwaiter.DONE, r3.state);
    }

    /**
     * execute using DiscardOldestPolicy drops task on shutdown
     */
    public void testDiscardOldestOnShutdown() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(1),
                                   new DiscardOldestPolicy());

        try { p.shutdown(); } catch (SecurityException ok) { return; }
        try (PoolCleaner cleaner = cleaner(p)) {
            TrackedNoOpRunnable r = new TrackedNoOpRunnable();
            p.execute(r);
            assertFalse(r.done);
        }
    }

    /**
     * Submitting null tasks throws NullPointerException
     */
    public void testNullTaskSubmission() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 2,
                                   1L, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertNullTaskSubmissionThrowsNullPointerException(p);
        }
    }

    /**
     * setCorePoolSize of negative value throws IllegalArgumentException
     */
    public void testCorePoolSizeIllegalArgumentException() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            try {
                p.setCorePoolSize(-1);
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * setMaximumPoolSize(int) throws IllegalArgumentException if
     * given a value less the core pool size
     */
    public void testMaximumPoolSizeIllegalArgumentException() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(2, 3,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            try {
                p.setMaximumPoolSize(1);
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * setMaximumPoolSize throws IllegalArgumentException
     * if given a negative value
     */
    public void testMaximumPoolSizeIllegalArgumentException2() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(2, 3,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            try {
                p.setMaximumPoolSize(-1);
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * Configuration changes that allow core pool size greater than
     * max pool size result in IllegalArgumentException.
     */
    public void testPoolSizeInvariants() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            for (int s = 1; s < 5; s++) {
                p.setMaximumPoolSize(s);
                p.setCorePoolSize(s);
                try {
                    p.setMaximumPoolSize(s - 1);
                    shouldThrow();
                } catch (IllegalArgumentException success) {}
                assertEquals(s, p.getCorePoolSize());
                assertEquals(s, p.getMaximumPoolSize());
                try {
                    p.setCorePoolSize(s + 1);
                    shouldThrow();
                } catch (IllegalArgumentException success) {}
                assertEquals(s, p.getCorePoolSize());
                assertEquals(s, p.getMaximumPoolSize());
            }
        }
    }

    /**
     * setKeepAliveTime throws IllegalArgumentException
     * when given a negative value
     */
    public void testKeepAliveTimeIllegalArgumentException() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(2, 3,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            try {
                p.setKeepAliveTime(-1, MILLISECONDS);
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * terminated() is called on termination
     */
    public void testTerminated() {
        ExtendedTPE p = new ExtendedTPE();
        try (PoolCleaner cleaner = cleaner(p)) {
            try { p.shutdown(); } catch (SecurityException ok) { return; }
            assertTrue(p.terminatedCalled());
            assertTrue(p.isShutdown());
        }
    }

    /**
     * beforeExecute and afterExecute are called when executing task
     */
    public void testBeforeAfter() throws InterruptedException {
        ExtendedTPE p = new ExtendedTPE();
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch done = new CountDownLatch(1);
            p.execute(new CheckedRunnable() {
                public void realRun() {
                    done.countDown();
                }});
            await(p.afterCalled);
            assertEquals(0, done.getCount());
            assertTrue(p.afterCalled());
            assertTrue(p.beforeCalled());
        }
    }

    /**
     * completed submit of callable returns result
     */
    public void testSubmitCallable() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(e)) {
            Future<String> future = e.submit(new StringTask());
            String result = future.get();
            assertSame(TEST_STRING, result);
        }
    }

    /**
     * completed submit of runnable returns successfully
     */
    public void testSubmitRunnable() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(e)) {
            Future<?> future = e.submit(new NoOpRunnable());
            future.get();
            assertTrue(future.isDone());
        }
    }

    /**
     * completed submit of (runnable, result) returns result
     */
    public void testSubmitRunnable2() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(e)) {
            Future<String> future = e.submit(new NoOpRunnable(), TEST_STRING);
            String result = future.get();
            assertSame(TEST_STRING, result);
        }
    }

    /**
     * invokeAny(null) throws NPE
     */
    public void testInvokeAny1() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
    public void testInvokeAny2() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(new ArrayList<Callable<String>>());
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * invokeAny(c) throws NPE if c has null elements
     */
    public void testInvokeAny3() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
     * invokeAny(c) throws ExecutionException if no task completes
     */
    public void testInvokeAny4() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
     * invokeAny(c) returns result of some task
     */
    public void testInvokeAny5() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            String result = e.invokeAny(l);
            assertSame(TEST_STRING, result);
        }
    }

    /**
     * invokeAll(null) throws NPE
     */
    public void testInvokeAll1() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        final Collection<Callable<String>> emptyCollection
            = Collections.emptyList();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Future<String>> r = e.invokeAll(emptyCollection);
            assertTrue(r.isEmpty());
        }
    }

    /**
     * invokeAll(c) throws NPE if c has null elements
     */
    public void testInvokeAll3() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
     * get of element of invokeAll(c) throws exception on failed task
     */
    public void testInvokeAll4() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
     * invokeAll(c) returns results of all completed tasks
     */
    public void testInvokeAll5() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
     * timed invokeAny(null) throws NPE
     */
    public void testTimedInvokeAny1() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(null, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAny(,,null) throws NPE
     */
    public void testTimedInvokeAnyNullTimeUnit() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
    public void testTimedInvokeAny2() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
    public void testTimedInvokeAny3() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
    public void testTimedInvokeAny4() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
     * timed invokeAny(c) returns result of some task
     */
    public void testTimedInvokeAny5() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
     * timed invokeAll(null) throws NPE
     */
    public void testTimedInvokeAll1() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAll(null, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAll(,,null) throws NPE
     */
    public void testTimedInvokeAllNullTimeUnit() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        final Collection<Callable<String>> emptyCollection
            = Collections.emptyList();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Future<String>> r =
                e.invokeAll(emptyCollection, randomTimeout(), randomTimeUnit());
            assertTrue(r.isEmpty());
        }
    }

    /**
     * timed invokeAll(c) throws NPE if c has null elements
     */
    public void testTimedInvokeAll3() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
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
     * get of element of invokeAll(c) throws exception on failed task
     */
    public void testTimedInvokeAll4() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            List<Future<String>> futures =
                e.invokeAll(l, LONG_DELAY_MS, MILLISECONDS);
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
     * timed invokeAll(c) returns results of all completed tasks
     */
    public void testTimedInvokeAll5() throws Exception {
        final ExecutorService e =
            new ThreadPoolExecutor(2, 2,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            List<Future<String>> futures =
                e.invokeAll(l, LONG_DELAY_MS, MILLISECONDS);
            assertEquals(2, futures.size());
            for (Future<String> future : futures)
                assertSame(TEST_STRING, future.get());
        }
    }

    /**
     * timed invokeAll(c) cancels tasks not completed by timeout
     */
    public void testTimedInvokeAll6() throws Exception {
        for (long timeout = timeoutMillis();;) {
            final CountDownLatch done = new CountDownLatch(1);
            final Callable<String> waiter = new CheckedCallable<>() {
                public String realCall() {
                    try { done.await(LONG_DELAY_MS, MILLISECONDS); }
                    catch (InterruptedException ok) {}
                    return "1"; }};
            final ExecutorService p =
                new ThreadPoolExecutor(2, 2,
                                       LONG_DELAY_MS, MILLISECONDS,
                                       new ArrayBlockingQueue<Runnable>(10));
            try (PoolCleaner cleaner = cleaner(p, done)) {
                List<Callable<String>> tasks = new ArrayList<>();
                tasks.add(new StringTask("0"));
                tasks.add(waiter);
                tasks.add(new StringTask("2"));
                long startTime = System.nanoTime();
                List<Future<String>> futures =
                    p.invokeAll(tasks, timeout, MILLISECONDS);
                assertEquals(tasks.size(), futures.size());
                assertTrue(millisElapsedSince(startTime) >= timeout);
                for (Future<?> future : futures)
                    assertTrue(future.isDone());
                assertTrue(futures.get(1).isCancelled());
                try {
                    assertEquals("0", futures.get(0).get());
                    assertEquals("2", futures.get(2).get());
                    break;
                } catch (CancellationException retryWithLongerTimeout) {
                    timeout *= 2;
                    if (timeout >= LONG_DELAY_MS / 2)
                        fail("expected exactly one task to be cancelled");
                }
            }
        }
    }

    /**
     * Execution continues if there is at least one thread even if
     * thread factory fails to create more
     */
    public void testFailingThreadFactory() throws InterruptedException {
        final ExecutorService e =
            new ThreadPoolExecutor(100, 100,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new LinkedBlockingQueue<Runnable>(),
                                   new FailingThreadFactory());
        try (PoolCleaner cleaner = cleaner(e)) {
            final int TASKS = 100;
            final CountDownLatch done = new CountDownLatch(TASKS);
            for (int k = 0; k < TASKS; ++k)
                e.execute(new CheckedRunnable() {
                    public void realRun() {
                        done.countDown();
                    }});
            await(done);
        }
    }

    /**
     * allowsCoreThreadTimeOut is by default false.
     */
    public void testAllowsCoreThreadTimeOut() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(2, 2,
                                   1000, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertFalse(p.allowsCoreThreadTimeOut());
        }
    }

    /**
     * allowCoreThreadTimeOut(true) causes idle threads to time out
     */
    public void testAllowCoreThreadTimeOut_true() throws Exception {
        long keepAliveTime = timeoutMillis();
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(2, 10,
                                   keepAliveTime, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            p.allowCoreThreadTimeOut(true);
            p.execute(new CheckedRunnable() {
                public void realRun() {
                    threadStarted.countDown();
                    assertEquals(1, p.getPoolSize());
                }});
            await(threadStarted);
            delay(keepAliveTime);
            long startTime = System.nanoTime();
            while (p.getPoolSize() > 0
                   && millisElapsedSince(startTime) < LONG_DELAY_MS)
                Thread.yield();
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
            assertEquals(0, p.getPoolSize());
        }
    }

    /**
     * allowCoreThreadTimeOut(false) causes idle threads not to time out
     */
    public void testAllowCoreThreadTimeOut_false() throws Exception {
        long keepAliveTime = timeoutMillis();
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(2, 10,
                                   keepAliveTime, MILLISECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            p.allowCoreThreadTimeOut(false);
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadStarted.countDown();
                    assertTrue(p.getPoolSize() >= 1);
                }});
            delay(2 * keepAliveTime);
            assertTrue(p.getPoolSize() >= 1);
        }
    }

    /**
     * execute allows the same task to be submitted multiple times, even
     * if rejected
     */
    public void testRejectedRecycledTask() throws InterruptedException {
        final int nTasks = 1000;
        final CountDownLatch done = new CountDownLatch(nTasks);
        final Runnable recycledTask = new Runnable() {
            public void run() {
                done.countDown();
            }};
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 30,
                                   60, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(30));
        try (PoolCleaner cleaner = cleaner(p)) {
            for (int i = 0; i < nTasks; ++i) {
                for (;;) {
                    try {
                        p.execute(recycledTask);
                        break;
                    }
                    catch (RejectedExecutionException ignore) {}
                }
            }
            // enough time to run all tasks
            await(done, nTasks * SHORT_DELAY_MS);
        }
    }

    /**
     * get(cancelled task) throws CancellationException
     */
    public void testGet_cancelled() throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final ExecutorService e =
            new ThreadPoolExecutor(1, 1,
                                   LONG_DELAY_MS, MILLISECONDS,
                                   new LinkedBlockingQueue<Runnable>());
        try (PoolCleaner cleaner = cleaner(e, done)) {
            final CountDownLatch blockerStarted = new CountDownLatch(1);
            final List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                Runnable r = new CheckedRunnable() { public void realRun()
                                                         throws Throwable {
                    blockerStarted.countDown();
                    assertTrue(done.await(2 * LONG_DELAY_MS, MILLISECONDS));
                }};
                futures.add(e.submit(r));
            }
            await(blockerStarted);
            for (Future<?> future : futures) future.cancel(false);
            for (Future<?> future : futures) {
                try {
                    future.get();
                    shouldThrow();
                } catch (CancellationException success) {}
                try {
                    future.get(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (CancellationException success) {}
                assertTrue(future.isCancelled());
                assertTrue(future.isDone());
            }
        }
    }

    /** Directly test simple ThreadPoolExecutor RejectedExecutionHandlers. */
    public void testStandardRejectedExecutionHandlers() {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1, 1, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(1));
        final AtomicReference<Thread> thread = new AtomicReference<>();
        final Runnable r = new Runnable() { public void run() {
            thread.set(Thread.currentThread()); }};

        try {
            new AbortPolicy().rejectedExecution(r, p);
            shouldThrow();
        } catch (RejectedExecutionException success) {}
        assertNull(thread.get());

        new DiscardPolicy().rejectedExecution(r, p);
        assertNull(thread.get());

        new CallerRunsPolicy().rejectedExecution(r, p);
        assertSame(Thread.currentThread(), thread.get());

        // check that pool was not perturbed by handlers
        assertTrue(p.getRejectedExecutionHandler() instanceof AbortPolicy);
        assertEquals(0, p.getTaskCount());
        assertTrue(p.getQueue().isEmpty());
    }

    public void testThreadFactoryReturnsTerminatedThread_shouldThrow() {
        if (!testImplementationDetails)
            return;

        ThreadFactory returnsTerminatedThread = runnableIgnored -> {
            Thread thread = new Thread(() -> {});
            thread.start();
            try { thread.join(); }
            catch (InterruptedException ex) { throw new Error(ex); }
            return thread;
        };
        ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1, 1, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(1),
                                   returnsTerminatedThread);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertThrows(IllegalThreadStateException.class,
                         () -> p.execute(() -> {}));
        }
    }

    public void testThreadFactoryReturnsStartedThread_shouldThrow() {
        if (!testImplementationDetails)
            return;

        CountDownLatch latch = new CountDownLatch(1);
        Runnable awaitLatch = () -> {
            try { latch.await(); }
            catch (InterruptedException ex) { throw new Error(ex); }};
        ThreadFactory returnsStartedThread = runnable -> {
            Thread thread = new Thread(awaitLatch);
            thread.start();
            return thread;
        };
        ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1, 1, SECONDS,
                                   new ArrayBlockingQueue<Runnable>(1),
                                   returnsStartedThread);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertThrows(IllegalThreadStateException.class,
                         () -> p.execute(() -> {}));
            latch.countDown();
        }
    }

}
