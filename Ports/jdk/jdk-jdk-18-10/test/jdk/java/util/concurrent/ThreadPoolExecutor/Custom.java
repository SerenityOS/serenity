/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6277663
 * @summary Test TPE extensibility framework
 * @library /test/lib
 * @author Martin Buchholz
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import jdk.test.lib.Utils;

public class Custom {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    static volatile int passed = 0, failed = 0;
    static void pass() { passed++; }
    static void fail() { failed++; Thread.dumpStack(); }
    static void unexpected(Throwable t) { failed++; t.printStackTrace(); }
    static void check(boolean cond) { if (cond) pass(); else fail(); }
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else {System.out.println(x + " not equal to " + y); fail(); }}

    private static class CustomTask<V> extends FutureTask<V> {
        public static final AtomicInteger births = new AtomicInteger(0);
        CustomTask(Callable<V> c) { super(c); births.getAndIncrement(); }
        CustomTask(Runnable r, V v) { super(r, v); births.getAndIncrement(); }
    }

    private static class CustomTPE extends ThreadPoolExecutor {
        CustomTPE() {
            super(threadCount, threadCount,
                  30, TimeUnit.MILLISECONDS,
                  new ArrayBlockingQueue<Runnable>(2*threadCount));
        }
        protected <V> RunnableFuture<V> newTaskFor(Callable<V> c) {
            return new CustomTask<V>(c);
        }
        protected <V> RunnableFuture<V> newTaskFor(Runnable r, V v) {
            return new CustomTask<V>(r, v);
        }
    }

    private static class CustomSTPE extends ScheduledThreadPoolExecutor {
        public static final AtomicInteger decorations = new AtomicInteger(0);
        CustomSTPE() {
            super(threadCount);
        }
        protected <V> RunnableScheduledFuture<V> decorateTask(
            Runnable r, RunnableScheduledFuture<V> task) {
            decorations.getAndIncrement();
            return task;
        }
        protected <V> RunnableScheduledFuture<V> decorateTask(
            Callable<V> c, RunnableScheduledFuture<V> task) {
            decorations.getAndIncrement();
            return task;
        }
    }

    static int countExecutorThreads() {
        Thread[] threads = new Thread[Thread.activeCount()+100];
        Thread.enumerate(threads);
        int count = 0;
        for (Thread t : threads)
            if (t != null && t.getName().matches("pool-[0-9]+-thread-[0-9]+"))
                count++;
        return count;
    }

    private static final int threadCount = 10;

    static long millisElapsedSince(long startTime) {
        return (System.nanoTime() - startTime) / (1000L * 1000L);
    }

    static void spinWaitUntil(BooleanSupplier predicate, long timeoutMillis) {
        long startTime = -1L;
        while (!predicate.getAsBoolean()) {
            if (startTime == -1L)
                startTime = System.nanoTime();
            else if (millisElapsedSince(startTime) > timeoutMillis)
                throw new AssertionError(
                    String.format("timed out after %s ms", timeoutMillis));
            Thread.yield();
        }
    }

    public static void main(String[] args) throws Throwable {
        CustomTPE tpe = new CustomTPE();
        equal(tpe.getCorePoolSize(), threadCount);
        equal(countExecutorThreads(), 0);
        for (int i = 0; i < threadCount; i++)
            tpe.submit(new Runnable() { public void run() {}});
        equal(countExecutorThreads(), threadCount);
        equal(CustomTask.births.get(), threadCount);
        tpe.shutdown();
        tpe.awaitTermination(LONG_DELAY_MS, MILLISECONDS);
        spinWaitUntil(() -> countExecutorThreads() == 0, LONG_DELAY_MS);

        CustomSTPE stpe = new CustomSTPE();
        for (int i = 0; i < threadCount; i++)
            stpe.submit(new Runnable() { public void run() {}});
        equal(CustomSTPE.decorations.get(), threadCount);
        equal(countExecutorThreads(), threadCount);
        stpe.shutdown();
        stpe.awaitTermination(LONG_DELAY_MS, MILLISECONDS);
        spinWaitUntil(() -> countExecutorThreads() == 0, LONG_DELAY_MS);

        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new Exception("Some tests failed");
    }
}
