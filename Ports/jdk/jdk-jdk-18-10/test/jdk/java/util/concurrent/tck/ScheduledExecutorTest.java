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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ScheduledExecutorTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ScheduledExecutorTest.class);
    }

    /**
     * execute successfully executes a runnable
     */
    public void testExecute() throws InterruptedException {
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch done = new CountDownLatch(1);
            final Runnable task = new CheckedRunnable() {
                public void realRun() { done.countDown(); }};
            p.execute(task);
            await(done);
        }
    }

    /**
     * delayed schedule of callable successfully executes after delay
     */
    public void testSchedule1() throws Exception {
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            final long startTime = System.nanoTime();
            final CountDownLatch done = new CountDownLatch(1);
            Callable<Boolean> task = new CheckedCallable<>() {
                public Boolean realCall() {
                    done.countDown();
                    assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
                    return Boolean.TRUE;
                }};
            Future<Boolean> f = p.schedule(task, timeoutMillis(), MILLISECONDS);
            assertSame(Boolean.TRUE, f.get());
            assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
            assertEquals(0L, done.getCount());
        }
    }

    /**
     * delayed schedule of runnable successfully executes after delay
     */
    public void testSchedule3() throws Exception {
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            final long startTime = System.nanoTime();
            final CountDownLatch done = new CountDownLatch(1);
            Runnable task = new CheckedRunnable() {
                public void realRun() {
                    done.countDown();
                    assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
                }};
            Future<?> f = p.schedule(task, timeoutMillis(), MILLISECONDS);
            await(done);
            assertNull(f.get(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        }
    }

    /**
     * scheduleAtFixedRate executes runnable after given initial delay
     */
    public void testSchedule4() throws Exception {
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            final long startTime = System.nanoTime();
            final CountDownLatch done = new CountDownLatch(1);
            Runnable task = new CheckedRunnable() {
                public void realRun() {
                    done.countDown();
                    assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
                }};
            ScheduledFuture<?> f =
                p.scheduleAtFixedRate(task, timeoutMillis(),
                                      LONG_DELAY_MS, MILLISECONDS);
            await(done);
            assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
            f.cancel(true);
        }
    }

    /**
     * scheduleWithFixedDelay executes runnable after given initial delay
     */
    public void testSchedule5() throws Exception {
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            final long startTime = System.nanoTime();
            final CountDownLatch done = new CountDownLatch(1);
            Runnable task = new CheckedRunnable() {
                public void realRun() {
                    done.countDown();
                    assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
                }};
            ScheduledFuture<?> f =
                p.scheduleWithFixedDelay(task, timeoutMillis(),
                                         LONG_DELAY_MS, MILLISECONDS);
            await(done);
            assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
            f.cancel(true);
        }
    }

    static class RunnableCounter implements Runnable {
        AtomicInteger count = new AtomicInteger(0);
        public void run() { count.getAndIncrement(); }
    }

    /**
     * scheduleAtFixedRate executes series of tasks at given rate.
     * Eventually, it must hold that:
     *   cycles - 1 <= elapsedMillis/delay < cycles
     */
    public void testFixedRateSequence() throws InterruptedException {
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            for (int delay = 1; delay <= LONG_DELAY_MS; delay *= 3) {
                final long startTime = System.nanoTime();
                final int cycles = 8;
                final CountDownLatch done = new CountDownLatch(cycles);
                final Runnable task = new CheckedRunnable() {
                    public void realRun() { done.countDown(); }};
                final ScheduledFuture<?> periodicTask =
                    p.scheduleAtFixedRate(task, 0, delay, MILLISECONDS);
                final int totalDelayMillis = (cycles - 1) * delay;
                await(done, totalDelayMillis + LONG_DELAY_MS);
                periodicTask.cancel(true);
                final long elapsedMillis = millisElapsedSince(startTime);
                assertTrue(elapsedMillis >= totalDelayMillis);
                if (elapsedMillis <= cycles * delay)
                    return;
                // else retry with longer delay
            }
            fail("unexpected execution rate");
        }
    }

    /**
     * scheduleWithFixedDelay executes series of tasks with given period.
     * Eventually, it must hold that each task starts at least delay and at
     * most 2 * delay after the termination of the previous task.
     */
    public void testFixedDelaySequence() throws InterruptedException {
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            for (int delay = 1; delay <= LONG_DELAY_MS; delay *= 3) {
                final long startTime = System.nanoTime();
                final AtomicLong previous = new AtomicLong(startTime);
                final AtomicBoolean tryLongerDelay = new AtomicBoolean(false);
                final int cycles = 8;
                final CountDownLatch done = new CountDownLatch(cycles);
                final int d = delay;
                final Runnable task = new CheckedRunnable() {
                    public void realRun() {
                        long now = System.nanoTime();
                        long elapsedMillis
                            = NANOSECONDS.toMillis(now - previous.get());
                        if (done.getCount() == cycles) { // first execution
                            if (elapsedMillis >= d)
                                tryLongerDelay.set(true);
                        } else {
                            assertTrue(elapsedMillis >= d);
                            if (elapsedMillis >= 2 * d)
                                tryLongerDelay.set(true);
                        }
                        previous.set(now);
                        done.countDown();
                    }};
                final ScheduledFuture<?> periodicTask =
                    p.scheduleWithFixedDelay(task, 0, delay, MILLISECONDS);
                final int totalDelayMillis = (cycles - 1) * delay;
                await(done, totalDelayMillis + cycles * LONG_DELAY_MS);
                periodicTask.cancel(true);
                final long elapsedMillis = millisElapsedSince(startTime);
                assertTrue(elapsedMillis >= totalDelayMillis);
                if (!tryLongerDelay.get())
                    return;
                // else retry with longer delay
            }
            fail("unexpected execution rate");
        }
    }

    /**
     * Submitting null tasks throws NullPointerException
     */
    public void testNullTaskSubmission() {
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertNullTaskSubmissionThrowsNullPointerException(p);
        }
    }

    /**
     * Submitted tasks are rejected when shutdown
     */
    public void testSubmittedTasksRejectedWhenShutdown() throws InterruptedException {
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final CountDownLatch threadsStarted = new CountDownLatch(p.getCorePoolSize());
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

        try (PoolCleaner cleaner = cleaner(p, done)) {
            for (int i = p.getCorePoolSize(); i--> 0; ) {
                switch (rnd.nextInt(4)) {
                case 0: p.execute(r); break;
                case 1: assertFalse(p.submit(r).isDone()); break;
                case 2: assertFalse(p.submit(r, Boolean.TRUE).isDone()); break;
                case 3: assertFalse(p.submit(c).isDone()); break;
                }
            }

            // ScheduledThreadPoolExecutor has an unbounded queue, so never saturated.
            await(threadsStarted);

            if (rnd.nextBoolean())
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
        assertEquals(p.getCorePoolSize(), p.getCompletedTaskCount());
    }

    /**
     * getActiveCount increases but doesn't overestimate, when a
     * thread becomes active
     */
    public void testGetActiveCount() throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(2);
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
     * getCompletedTaskCount increases, but doesn't overestimate,
     * when tasks complete
     */
    public void testGetCompletedTaskCount() throws InterruptedException {
        final ThreadPoolExecutor p = new ScheduledThreadPoolExecutor(2);
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
    public void testGetCorePoolSize() throws InterruptedException {
        ThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(1, p.getCorePoolSize());
        }
    }

    /**
     * getLargestPoolSize increases, but doesn't overestimate, when
     * multiple threads active
     */
    public void testGetLargestPoolSize() throws InterruptedException {
        final int THREADS = 3;
        final ThreadPoolExecutor p = new ScheduledThreadPoolExecutor(THREADS);
        final CountDownLatch threadsStarted = new CountDownLatch(THREADS);
        final CountDownLatch done = new CountDownLatch(1);
        try (PoolCleaner cleaner = cleaner(p, done)) {
            assertEquals(0, p.getLargestPoolSize());
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
     * getPoolSize increases, but doesn't overestimate, when threads
     * become active
     */
    public void testGetPoolSize() throws InterruptedException {
        final ThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);
        try (PoolCleaner cleaner = cleaner(p, done)) {
            assertEquals(0, p.getPoolSize());
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
     * getTaskCount increases, but doesn't overestimate, when tasks
     * submitted
     */
    public void testGetTaskCount() throws InterruptedException {
        final int TASKS = 3;
        final CountDownLatch done = new CountDownLatch(1);
        final ThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
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
     * getThreadFactory returns factory in constructor if not set
     */
    public void testGetThreadFactory() throws InterruptedException {
        final ThreadFactory threadFactory = new SimpleThreadFactory();
        final ScheduledThreadPoolExecutor p =
            new ScheduledThreadPoolExecutor(1, threadFactory);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertSame(threadFactory, p.getThreadFactory());
        }
    }

    /**
     * setThreadFactory sets the thread factory returned by getThreadFactory
     */
    public void testSetThreadFactory() throws InterruptedException {
        ThreadFactory threadFactory = new SimpleThreadFactory();
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            p.setThreadFactory(threadFactory);
            assertSame(threadFactory, p.getThreadFactory());
        }
    }

    /**
     * setThreadFactory(null) throws NPE
     */
    public void testSetThreadFactoryNull() throws InterruptedException {
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
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
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertTrue(p.getRejectedExecutionHandler()
                       instanceof ThreadPoolExecutor.AbortPolicy);
        }
    }

    /**
     * isShutdown is false before shutdown, true after
     */
    public void testIsShutdown() {
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        assertFalse(p.isShutdown());
        try (PoolCleaner cleaner = cleaner(p)) {
            try {
                p.shutdown();
                assertTrue(p.isShutdown());
            } catch (SecurityException ok) {}
        }
    }

    /**
     * isTerminated is false before termination, true after
     */
    public void testIsTerminated() throws InterruptedException {
        final ThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(1);
            assertFalse(p.isTerminated());
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertFalse(p.isTerminated());
                    threadStarted.countDown();
                    await(done);
                }});
            await(threadStarted);
            assertFalse(p.isTerminating());
            done.countDown();
            try { p.shutdown(); } catch (SecurityException ok) { return; }
            assertTrue(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(p.isTerminated());
        }
    }

    /**
     * isTerminating is not true when running or when terminated
     */
    public void testIsTerminating() throws InterruptedException {
        final ThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);
        try (PoolCleaner cleaner = cleaner(p)) {
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
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p, done)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            @SuppressWarnings("unchecked")
            ScheduledFuture<?>[] tasks = (ScheduledFuture<?>[])new ScheduledFuture[5];
            for (int i = 0; i < tasks.length; i++) {
                Runnable r = new CheckedRunnable() {
                    public void realRun() throws InterruptedException {
                        threadStarted.countDown();
                        await(done);
                    }};
                tasks[i] = p.schedule(r, 1, MILLISECONDS);
            }
            await(threadStarted);
            BlockingQueue<Runnable> q = p.getQueue();
            assertTrue(q.contains(tasks[tasks.length - 1]));
            assertFalse(q.contains(tasks[0]));
        }
    }

    /**
     * remove(task) removes queued task, and fails to remove active task
     */
    public void testRemove() throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(1);
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p, done)) {
            @SuppressWarnings("unchecked")
            ScheduledFuture<?>[] tasks = (ScheduledFuture<?>[])new ScheduledFuture[5];
            final CountDownLatch threadStarted = new CountDownLatch(1);
            for (int i = 0; i < tasks.length; i++) {
                Runnable r = new CheckedRunnable() {
                    public void realRun() throws InterruptedException {
                        threadStarted.countDown();
                        await(done);
                    }};
                tasks[i] = p.schedule(r, 1, MILLISECONDS);
            }
            await(threadStarted);
            BlockingQueue<Runnable> q = p.getQueue();
            assertFalse(p.remove((Runnable)tasks[0]));
            assertTrue(q.contains((Runnable)tasks[4]));
            assertTrue(q.contains((Runnable)tasks[3]));
            assertTrue(p.remove((Runnable)tasks[4]));
            assertFalse(p.remove((Runnable)tasks[4]));
            assertFalse(q.contains((Runnable)tasks[4]));
            assertTrue(q.contains((Runnable)tasks[3]));
            assertTrue(p.remove((Runnable)tasks[3]));
            assertFalse(q.contains((Runnable)tasks[3]));
        }
    }

    /**
     * purge eventually removes cancelled tasks from the queue
     */
    public void testPurge() throws InterruptedException {
        @SuppressWarnings("unchecked")
        ScheduledFuture<?>[] tasks = (ScheduledFuture<?>[])new ScheduledFuture[5];
        final Runnable releaser = new Runnable() { public void run() {
            for (ScheduledFuture<?> task : tasks)
                if (task != null) task.cancel(true); }};
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p, releaser)) {
            for (int i = 0; i < tasks.length; i++)
                tasks[i] = p.schedule(possiblyInterruptedRunnable(SMALL_DELAY_MS),
                                      LONG_DELAY_MS, MILLISECONDS);
            int max = tasks.length;
            if (tasks[4].cancel(true)) --max;
            if (tasks[3].cancel(true)) --max;
            // There must eventually be an interference-free point at
            // which purge will not fail. (At worst, when queue is empty.)
            long startTime = System.nanoTime();
            do {
                p.purge();
                long count = p.getTaskCount();
                if (count == max)
                    return;
            } while (millisElapsedSince(startTime) < LONG_DELAY_MS);
            fail("Purge failed to remove cancelled tasks");
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
        final ScheduledThreadPoolExecutor p =
            new ScheduledThreadPoolExecutor(poolSize);
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

    /**
     * shutdownNow returns a list containing tasks that were not run,
     * and those tasks are drained from the queue
     */
    public void testShutdownNow_delayedTasks() throws InterruptedException {
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        List<ScheduledFuture<?>> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Runnable r = new NoOpRunnable();
            tasks.add(p.schedule(r, 9, SECONDS));
            tasks.add(p.scheduleAtFixedRate(r, 9, 9, SECONDS));
            tasks.add(p.scheduleWithFixedDelay(r, 9, 9, SECONDS));
        }
        if (testImplementationDetails)
            assertEquals(new HashSet<Object>(tasks), new HashSet<Object>(p.getQueue()));
        final List<Runnable> queuedTasks;
        try {
            queuedTasks = p.shutdownNow();
        } catch (SecurityException ok) {
            return; // Allowed in case test doesn't have privs
        }
        assertTrue(p.isShutdown());
        assertTrue(p.getQueue().isEmpty());
        if (testImplementationDetails)
            assertEquals(new HashSet<Object>(tasks), new HashSet<Object>(queuedTasks));
        assertEquals(tasks.size(), queuedTasks.size());
        for (ScheduledFuture<?> task : tasks) {
            assertFalse(task.isDone());
            assertFalse(task.isCancelled());
        }
        assertTrue(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
        assertTrue(p.isTerminated());
    }

    /**
     * By default, periodic tasks are cancelled at shutdown.
     * By default, delayed tasks keep running after shutdown.
     * Check that changing the default values work:
     * - setExecuteExistingDelayedTasksAfterShutdownPolicy
     * - setContinueExistingPeriodicTasksAfterShutdownPolicy
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public void testShutdown_cancellation() throws Exception {
        final int poolSize = 4;
        final ScheduledThreadPoolExecutor p
            = new ScheduledThreadPoolExecutor(poolSize);
        final BlockingQueue<Runnable> q = p.getQueue();
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final long delay = rnd.nextInt(2);
        final int rounds = rnd.nextInt(1, 3);
        final boolean effectiveDelayedPolicy;
        final boolean effectivePeriodicPolicy;
        final boolean effectiveRemovePolicy;

        if (rnd.nextBoolean())
            p.setExecuteExistingDelayedTasksAfterShutdownPolicy(
                effectiveDelayedPolicy = rnd.nextBoolean());
        else
            effectiveDelayedPolicy = true;
        assertEquals(effectiveDelayedPolicy,
                     p.getExecuteExistingDelayedTasksAfterShutdownPolicy());

        if (rnd.nextBoolean())
            p.setContinueExistingPeriodicTasksAfterShutdownPolicy(
                effectivePeriodicPolicy = rnd.nextBoolean());
        else
            effectivePeriodicPolicy = false;
        assertEquals(effectivePeriodicPolicy,
                     p.getContinueExistingPeriodicTasksAfterShutdownPolicy());

        if (rnd.nextBoolean())
            p.setRemoveOnCancelPolicy(
                effectiveRemovePolicy = rnd.nextBoolean());
        else
            effectiveRemovePolicy = false;
        assertEquals(effectiveRemovePolicy,
                     p.getRemoveOnCancelPolicy());

        final boolean periodicTasksContinue = effectivePeriodicPolicy && rnd.nextBoolean();

        // Strategy: Wedge the pool with one wave of "blocker" tasks,
        // then add a second wave that waits in the queue until unblocked.
        final AtomicInteger ran = new AtomicInteger(0);
        final CountDownLatch poolBlocked = new CountDownLatch(poolSize);
        final CountDownLatch unblock = new CountDownLatch(1);
        final RuntimeException exception = new RuntimeException();

        class Task implements Runnable {
            public void run() {
                try {
                    ran.getAndIncrement();
                    poolBlocked.countDown();
                    await(unblock);
                } catch (Throwable fail) { threadUnexpectedException(fail); }
            }
        }

        class PeriodicTask extends Task {
            PeriodicTask(int rounds) { this.rounds = rounds; }
            int rounds;
            public void run() {
                if (--rounds == 0) super.run();
                // throw exception to surely terminate this periodic task,
                // but in a separate execution and in a detectable way.
                if (rounds == -1) throw exception;
            }
        }

        Runnable task = new Task();

        List<Future<?>> immediates = new ArrayList<>();
        List<Future<?>> delayeds   = new ArrayList<>();
        List<Future<?>> periodics  = new ArrayList<>();

        immediates.add(p.submit(task));
        delayeds.add(p.schedule(task, delay, MILLISECONDS));
        periodics.add(p.scheduleAtFixedRate(
                          new PeriodicTask(rounds), delay, 1, MILLISECONDS));
        periodics.add(p.scheduleWithFixedDelay(
                          new PeriodicTask(rounds), delay, 1, MILLISECONDS));

        await(poolBlocked);

        assertEquals(poolSize, ran.get());
        assertEquals(poolSize, p.getActiveCount());
        assertTrue(q.isEmpty());

        // Add second wave of tasks.
        immediates.add(p.submit(task));
        delayeds.add(p.schedule(task, effectiveDelayedPolicy ? delay : LONG_DELAY_MS, MILLISECONDS));
        periodics.add(p.scheduleAtFixedRate(
                          new PeriodicTask(rounds), delay, 1, MILLISECONDS));
        periodics.add(p.scheduleWithFixedDelay(
                          new PeriodicTask(rounds), delay, 1, MILLISECONDS));

        assertEquals(poolSize, q.size());
        assertEquals(poolSize, ran.get());

        immediates.forEach(
            f -> assertTrue(((ScheduledFuture)f).getDelay(NANOSECONDS) <= 0L));

        Stream.of(immediates, delayeds, periodics).flatMap(Collection::stream)
            .forEach(f -> assertFalse(f.isDone()));

        try { p.shutdown(); } catch (SecurityException ok) { return; }
        assertTrue(p.isShutdown());
        assertTrue(p.isTerminating());
        assertFalse(p.isTerminated());

        if (rnd.nextBoolean())
            assertThrows(
                RejectedExecutionException.class,
                () -> p.submit(task),
                () -> p.schedule(task, 1, SECONDS),
                () -> p.scheduleAtFixedRate(
                    new PeriodicTask(1), 1, 1, SECONDS),
                () -> p.scheduleWithFixedDelay(
                    new PeriodicTask(2), 1, 1, SECONDS));

        assertTrue(q.contains(immediates.get(1)));
        assertTrue(!effectiveDelayedPolicy
                   ^ q.contains(delayeds.get(1)));
        assertTrue(!effectivePeriodicPolicy
                   ^ q.containsAll(periodics.subList(2, 4)));

        immediates.forEach(f -> assertFalse(f.isDone()));

        assertFalse(delayeds.get(0).isDone());
        if (effectiveDelayedPolicy)
            assertFalse(delayeds.get(1).isDone());
        else
            assertTrue(delayeds.get(1).isCancelled());

        if (effectivePeriodicPolicy)
            periodics.forEach(
                f -> {
                    assertFalse(f.isDone());
                    if (!periodicTasksContinue) {
                        assertTrue(f.cancel(false));
                        assertTrue(f.isCancelled());
                    }
                });
        else {
            periodics.subList(0, 2).forEach(f -> assertFalse(f.isDone()));
            periodics.subList(2, 4).forEach(f -> assertTrue(f.isCancelled()));
        }

        unblock.countDown();    // Release all pool threads

        assertTrue(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
        assertFalse(p.isTerminating());
        assertTrue(p.isTerminated());

        assertTrue(q.isEmpty());

        Stream.of(immediates, delayeds, periodics).flatMap(Collection::stream)
            .forEach(f -> assertTrue(f.isDone()));

        for (Future<?> f : immediates) assertNull(f.get());

        assertNull(delayeds.get(0).get());
        if (effectiveDelayedPolicy)
            assertNull(delayeds.get(1).get());
        else
            assertTrue(delayeds.get(1).isCancelled());

        if (periodicTasksContinue)
            periodics.forEach(
                f -> {
                    try { f.get(); }
                    catch (ExecutionException success) {
                        assertSame(exception, success.getCause());
                    }
                    catch (Throwable fail) { threadUnexpectedException(fail); }
                });
        else
            periodics.forEach(f -> assertTrue(f.isCancelled()));

        assertEquals(poolSize + 1
                     + (effectiveDelayedPolicy ? 1 : 0)
                     + (periodicTasksContinue ? 2 : 0),
                     ran.get());
    }

    /**
     * completed submit of callable returns result
     */
    public void testSubmitCallable() throws Exception {
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
        try (PoolCleaner cleaner = cleaner(e)) {
            Future<String> future = e.submit(new NoOpRunnable(), TEST_STRING);
            String result = future.get();
            assertSame(TEST_STRING, result);
        }
    }

    /**
     * invokeAny(null) throws NullPointerException
     */
    public void testInvokeAny1() throws Exception {
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(new ArrayList<Callable<String>>());
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * invokeAny(c) throws NullPointerException if c has null elements
     */
    public void testInvokeAny3() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
    public void testInvokeAll2() throws Exception {
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
     * get of invokeAll(c) throws exception on failed task
     */
    public void testInvokeAll4() throws Exception {
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(null, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAny(,,null) throws NullPointerException
     */
    public void testTimedInvokeAnyNullTimeUnit() throws Exception {
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
        final Collection<Callable<String>> emptyCollection
            = Collections.emptyList();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(emptyCollection, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * timed invokeAny(c) throws NPE if c has null elements
     */
    public void testTimedInvokeAny3() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
    public void testTimedInvokeAll2() throws Exception {
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
        final ExecutorService e = new ScheduledThreadPoolExecutor(2);
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
            final ExecutorService p = new ScheduledThreadPoolExecutor(2);
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
     * A fixed delay task with overflowing period should not prevent a
     * one-shot task from executing.
     * https://bugs.openjdk.java.net/browse/JDK-8051859
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public void testScheduleWithFixedDelay_overflow() throws Exception {
        final CountDownLatch delayedDone = new CountDownLatch(1);
        final CountDownLatch immediateDone = new CountDownLatch(1);
        final ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(1);
        try (PoolCleaner cleaner = cleaner(p)) {
            final Runnable delayed = () -> {
                delayedDone.countDown();
                p.submit(() -> immediateDone.countDown());
            };
            p.scheduleWithFixedDelay(delayed, 0L, Long.MAX_VALUE, SECONDS);
            await(delayedDone);
            await(immediateDone);
        }
    }

}
