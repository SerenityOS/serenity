/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng AsyncShutdownNowInvokeAnyRace
 * @summary A variant of AsyncShutdownNow useful for race bug hunting
 */

// TODO: reorganize all of the AsyncShutdown tests

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.lang.management.ManagementFactory;
import java.lang.management.LockInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class AsyncShutdownNowInvokeAnyRace {

    // TODO: even more jitter-inducing parallelism?

//         int nThreads = ThreadLocalRandom.current().nextInt(1, 50);
//         ExecutorService pool = Executors.newCachedThreadPool();
//         Callable<Void> task = () -> { testInvokeAny_1(); return null; };
//         List<Callable<Void>> tasks = Collections.nCopies(nThreads, task);
//         try {
//             for (Future<Void> future : pool.invokeAll(tasks)) {
//                 future.get();
//             }
//         } finally {
//             pool.shutdown();
//         }
//     }

//     public void testInvokeAny_1() throws Exception {

    /**
     * Test shutdownNow with thread blocked in invokeAny.
     */
    @Test
    public void testInvokeAny() throws Exception {
        final int reps = 30_000;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int falseAlarms = 0;
        for (int rep = 1; rep < reps; rep++) {
            ForkJoinPool pool = new ForkJoinPool(1);
            CountDownLatch pleaseShutdownNow = new CountDownLatch(1);
            int nTasks = rnd.nextInt(2, 5);
            AtomicInteger threadsStarted = new AtomicInteger(0);
            AtomicReference<String> poolAtShutdownNow = new AtomicReference<>();
            Callable<Void> blockPool = () -> {
                threadsStarted.getAndIncrement();
                // await submission quiescence; may false-alarm
                // TODO: consider re-checking to reduce false alarms
                while (threadsStarted.get() + pool.getQueuedSubmissionCount() < nTasks)
                    Thread.yield();
                pleaseShutdownNow.countDown();
                Thread.sleep(86400_000);
                return null;
            };
            List<Callable<Void>> tasks = Collections.nCopies(nTasks, blockPool);
            Runnable shutdown = () -> {
                try {
                    pleaseShutdownNow.await();
                    poolAtShutdownNow.set(pool.toString());
                    pool.shutdownNow();
                } catch (Throwable t) { throw new AssertionError(t); }
            };
            Future<Void> shutdownResult = CompletableFuture.runAsync(shutdown);
            try {
                try {
                    if (rnd.nextBoolean())
                        pool.invokeAny(tasks, 10L, SECONDS);
                    else
                        pool.invokeAny(tasks);
                    throw new AssertionError("should throw");
                } catch (RejectedExecutionException re) {
                    falseAlarms++;
                } catch (CancellationException re) {
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (!(cause instanceof InterruptedException) &&
                        !(cause instanceof CancellationException))
                        throw ex;
                } catch (TimeoutException ex) {
                    dumpTestThreads();
                    int i = rep;
                    String detail = String.format(
                        "invokeAny timed out, "
                        + "nTasks=%d rep=%d threadsStarted=%d%n"
                        + "poolAtShutdownNow=%s%n"
                        + "poolAtTimeout=%s%n"
                        + "queuedTaskCount=%d queuedSubmissionCount=%d",
                        nTasks, i, threadsStarted.get(),
                        poolAtShutdownNow,
                        pool,
                        pool.getQueuedTaskCount(),
                        pool.getQueuedSubmissionCount());
                    throw new AssertionError(detail, ex);
                }
            } finally {
                pool.shutdown();
            }
            if (falseAlarms != 0)
                System.out.println("Premature shutdowns = " + falseAlarms);
            shutdownResult.get();
        }
    }

    //--- thread stack dumping (from JSR166TestCase.java) ---

    private static final ThreadMXBean THREAD_MXBEAN
        = ManagementFactory.getThreadMXBean();

    /** Returns true if thread info might be useful in a thread dump. */
    static boolean threadOfInterest(ThreadInfo info) {
        final String name = info.getThreadName();
        String lockName;
        if (name == null)
            return true;
        if (name.equals("Signal Dispatcher")
            || name.equals("WedgedTestDetector"))
            return false;
        if (name.equals("Reference Handler")) {
            // Reference Handler stacktrace changed in JDK-8156500
            StackTraceElement[] stackTrace; String methodName;
            if ((stackTrace = info.getStackTrace()) != null
                && stackTrace.length > 0
                && (methodName = stackTrace[0].getMethodName()) != null
                && methodName.equals("waitForReferencePendingList"))
                return false;
            // jdk8 Reference Handler stacktrace
            if ((lockName = info.getLockName()) != null
                && lockName.startsWith("java.lang.ref"))
                return false;
        }
        if ((name.equals("Finalizer") || name.equals("Common-Cleaner"))
            && (lockName = info.getLockName()) != null
            && lockName.startsWith("java.lang.ref"))
            return false;
        if (name.startsWith("ForkJoinPool.commonPool-worker")
            && (lockName = info.getLockName()) != null
            && lockName.startsWith("java.util.concurrent.ForkJoinPool"))
            return false;
        return true;
    }

    /**
     * A debugging tool to print stack traces of most threads, as jstack does.
     * Uninteresting threads are filtered out.
     */
    static void dumpTestThreads() {
        System.err.println("------ stacktrace dump start ------");
        for (ThreadInfo info : THREAD_MXBEAN.dumpAllThreads(true, true))
            if (threadOfInterest(info))
                System.err.print(info);
        System.err.println("------ stacktrace dump end ------");
    }

}
