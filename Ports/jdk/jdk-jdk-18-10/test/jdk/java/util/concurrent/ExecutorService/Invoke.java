/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug     6267833
 * @summary Tests for invokeAny, invokeAll
 * @author  Martin Buchholz
 */

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class Invoke {
    static volatile int passed = 0, failed = 0;

    static void fail(String msg) {
        failed++;
        new AssertionError(msg).printStackTrace();
    }

    static void pass() {
        passed++;
    }

    static void unexpected(Throwable t) {
        failed++;
        t.printStackTrace();
    }

    static void check(boolean condition, String msg) {
        if (condition) pass(); else fail(msg);
    }

    static void check(boolean condition) {
        check(condition, "Assertion failure");
    }

    static long secondsElapsedSince(long startTime) {
        return NANOSECONDS.toSeconds(System.nanoTime() - startTime);
    }

    static void awaitInterrupt(long timeoutSeconds) {
        long startTime = System.nanoTime();
        try {
            Thread.sleep(SECONDS.toMillis(timeoutSeconds));
            fail("timed out waiting for interrupt");
        } catch (InterruptedException expected) {
            check(secondsElapsedSince(startTime) < timeoutSeconds);
        }
    }

    public static void main(String[] args) {
        try {
            for (int nThreads = 1; nThreads <= 6; ++nThreads) {
                // untimed
                testInvokeAll(nThreads, false);
                testInvokeAny(nThreads, false);
                testInvokeAny_cancellationInterrupt(nThreads, false);
                // timed
                testInvokeAll(nThreads, true);
                testInvokeAny(nThreads, true);
                testInvokeAny_cancellationInterrupt(nThreads, true);
            }
        } catch (Throwable t) {  unexpected(t); }

        if (failed > 0)
            throw new Error(
                    String.format("Passed = %d, failed = %d", passed, failed));
    }

    static final long timeoutSeconds = 10L;

    static void testInvokeAll(int nThreads, boolean timed) throws Throwable {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        final AtomicLong count = new AtomicLong(0);
        class Task implements Callable<Long> {
            public Long call() throws Exception {
                return count.incrementAndGet();
            }
        }

        try {
            final List<Task> tasks =
                IntStream.range(0, nThreads)
                .mapToObj(i -> new Task())
                .collect(Collectors.toList());

            List<Future<Long>> futures;
            if (timed) {
                long startTime = System.nanoTime();
                futures = pool.invokeAll(tasks, timeoutSeconds, SECONDS);
                check(secondsElapsedSince(startTime) < timeoutSeconds);
            }
            else
                futures = pool.invokeAll(tasks);
            check(futures.size() == tasks.size());
            check(count.get() == tasks.size());

            long gauss = 0;
            for (Future<Long> future : futures) gauss += future.get();
            check(gauss == (tasks.size()+1)*tasks.size()/2);

            pool.shutdown();
            check(pool.awaitTermination(10L, SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    static void testInvokeAny(int nThreads, boolean timed) throws Throwable {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        final AtomicLong count = new AtomicLong(0);
        final CountDownLatch invokeAnyDone = new CountDownLatch(1);
        class Task implements Callable<Long> {
            public Long call() throws Exception {
                long x = count.incrementAndGet();
                if (x > 1) {
                    // wait for main thread to interrupt us ...
                    awaitInterrupt(timeoutSeconds);
                    // ... and then for invokeAny to return
                    check(invokeAnyDone.await(timeoutSeconds, SECONDS));
                }
                return x;
            }
        }

        try {
            final List<Task> tasks =
                IntStream.range(0, rnd.nextInt(1, 7))
                .mapToObj(i -> new Task())
                .collect(Collectors.toList());

            long val;
            if (timed) {
                long startTime = System.nanoTime();
                val = pool.invokeAny(tasks, timeoutSeconds, SECONDS);
                check(secondsElapsedSince(startTime) < timeoutSeconds);
            }
            else
                val = pool.invokeAny(tasks);
            check(val == 1);
            invokeAnyDone.countDown();

            pool.shutdown();
            check(pool.awaitTermination(timeoutSeconds, SECONDS));

            long c = count.get();
            check(c >= 1 && c <= tasks.size());

        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Every remaining running task is sent an interrupt for cancellation.
     */
    static void testInvokeAny_cancellationInterrupt(int nThreads, boolean timed) throws Throwable {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        final AtomicLong count = new AtomicLong(0);
        final AtomicLong interruptedCount = new AtomicLong(0);
        final CyclicBarrier allStarted = new CyclicBarrier(nThreads);
        class Task implements Callable<Long> {
            public Long call() throws Exception {
                long x = count.incrementAndGet();
                allStarted.await();
                if (x > 1)
                    // main thread will interrupt us
                    awaitInterrupt(timeoutSeconds);
                return x;
            }
        }

        try {
            final List<Task> tasks =
                IntStream.range(0, nThreads)
                .mapToObj(i -> new Task())
                .collect(Collectors.toList());

            long val;
            if (timed) {
                long startTime = System.nanoTime();
                val = pool.invokeAny(tasks, timeoutSeconds, SECONDS);
                check(secondsElapsedSince(startTime) < timeoutSeconds);
            }
            else
                val = pool.invokeAny(tasks);
            check(val == 1);

            pool.shutdown();
            check(pool.awaitTermination(timeoutSeconds, SECONDS));

            // Check after shutdown to avoid race
            check(count.get() == nThreads);
        } finally {
            pool.shutdownNow();
        }
    }
}
