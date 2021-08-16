/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6560953
 * @summary Test ScheduledThreadPoolExecutor.decorateTask
 */

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class DecorateTask {
    Runnable countDownTask(final CountDownLatch latch) {
        return new Runnable() { public void run() {
            latch.countDown();
            if (latch.getCount() <= 0)
                throw new RuntimeException("done");
        }};}

    void test(String[] args) throws Throwable {
        final int jobs = 100;
        final AtomicInteger decoratorCount = new AtomicInteger(0);
        final ScheduledThreadPoolExecutor pool =
            new ScheduledThreadPoolExecutor(10) {
                protected <V> RunnableScheduledFuture<V> decorateTask(
                    final Runnable runnable,
                    final RunnableScheduledFuture<V> task) {
                    return new RunnableScheduledFuture<V>() {
                        public void run() {
                            decoratorCount.incrementAndGet();
                            task.run();
                        }
                        public boolean isPeriodic() {
                            return task.isPeriodic();
                        }
                        public boolean cancel(boolean mayInterruptIfRunning) {
                            return task.cancel(mayInterruptIfRunning);
                        }
                        public boolean isCancelled() {
                            return task.isCancelled();
                        }
                        public boolean isDone() {
                            return task.isDone();
                        }
                        public V get()
                            throws InterruptedException, ExecutionException {
                            return task.get();
                        }
                        public V get(long timeout, TimeUnit unit)
                            throws InterruptedException, ExecutionException, TimeoutException {
                            return task.get(timeout, unit);
                        }
                        public long getDelay(TimeUnit unit) {
                            return task.getDelay(unit);
                        }
                        public int compareTo(Delayed o) {
                            return task.compareTo(o);
                        }};}};
        final CountDownLatch latch1 = new CountDownLatch(jobs);
        final CountDownLatch latch2 = new CountDownLatch(jobs);
        pool.scheduleAtFixedRate(countDownTask(latch1), 0L, 1L, TimeUnit.NANOSECONDS);
        pool.scheduleWithFixedDelay(countDownTask(latch2), 0L, 1L, TimeUnit.NANOSECONDS);
        latch1.await();
        latch2.await();
        pool.shutdown();
        pool.awaitTermination(1L, TimeUnit.MINUTES);
        equal(decoratorCount.get(), 2 * jobs);
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new DecorateTask().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
