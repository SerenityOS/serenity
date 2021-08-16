/*
 * Copyright (c) 2006, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6362121
 * @summary Test one ScheduledThreadPoolExecutor extension scenario
 * @author Martin Buchholz
 */

// based on a test kindly provided by Holger Hoffstaette <holger@wizards.de>

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScheduledTickleService {

    // We get intermittent ClassCastException if greater than 1
    // because of calls to compareTo
    private static final int concurrency = 2;

    // Record when tasks are done
    public static final CountDownLatch done = new CountDownLatch(concurrency);

    public static void realMain(String... args) throws InterruptedException {
        // our tickle service
        ScheduledExecutorService tickleService =
            new ScheduledThreadPoolExecutor(concurrency) {
                // We override decorateTask() to return a custom
                // RunnableScheduledFuture which explicitly removes
                // itself from the queue after cancellation.
                protected <V> RunnableScheduledFuture<V>
                    decorateTask(Runnable runnable,
                                 RunnableScheduledFuture<V> task) {
                    final ScheduledThreadPoolExecutor exec = this;
                    return new CustomRunnableScheduledFuture<V>(task) {
                        // delegate to wrapped task, except for:
                        public boolean cancel(boolean b) {
                            // cancel wrapped task & remove myself from the queue
                            return (task().cancel(b)
                                    && exec.remove(this));}};}};

        for (int i = 0; i < concurrency; i++)
            new ScheduledTickle(i, tickleService)
                .setUpdateInterval(25, MILLISECONDS);

        done.await();
        tickleService.shutdown();
        pass();
    }

    // our Runnable
    static class ScheduledTickle implements Runnable {
        public volatile int failures = 0;

        // my tickle service
        private final ScheduledExecutorService service;

        // remember my own scheduled ticket
        private ScheduledFuture ticket = null;

        // remember the number of times I've been tickled
        private int numTickled = 0;

        // my private name
        private final String name;

        public ScheduledTickle(int i, ScheduledExecutorService service) {
            super();
            this.name = "Tickler-"+i;
            this.service = service;
        }

        // set my tickle interval; 0 to disable further tickling.
        public synchronized void setUpdateInterval(long interval,
                                                   TimeUnit unit) {
            // cancel & remove previously created ticket
            if (ticket != null) {
                ticket.cancel(false);
                ticket = null;
            }

            if (interval > 0 && ! service.isShutdown()) {
                // requeue with new interval
                ticket = service.scheduleAtFixedRate(this, interval,
                                                     interval, unit);
            }
        }

        public synchronized void run() {
            try {
                check(numTickled < 6);
                numTickled++;
                System.out.println(name + ": Run " + numTickled);

                // tickle 3 times and then slow down
                if (numTickled == 3) {
                    System.out.println(name + ": slower please!");
                    this.setUpdateInterval(100, MILLISECONDS);
                }
                // ..but only 5 times max.
                else if (numTickled == 5) {
                    System.out.println(name + ": OK that's enough.");
                    this.setUpdateInterval(0, MILLISECONDS);
                    ScheduledTickleService.done.countDown();
                }
            } catch (Throwable t) { unexpected(t); }
        }
    }

    // This is just a generic wrapper to make up for the private ScheduledFutureTask
    static class CustomRunnableScheduledFuture<V>
        implements RunnableScheduledFuture<V> {
        // the wrapped future
        private RunnableScheduledFuture<V> task;

        public CustomRunnableScheduledFuture(RunnableScheduledFuture<V> task) {
            super();
            this.task = task;
        }

        public RunnableScheduledFuture<V> task() { return task; }

        // Forwarding methods
        public boolean isPeriodic()         { return task.isPeriodic(); }
        public boolean isCancelled()        { return task.isCancelled(); }
        public boolean isDone()             { return task.isDone(); }
        public boolean cancel(boolean b)    { return task.cancel(b); }
        public long getDelay(TimeUnit unit) { return task.getDelay(unit); }
        public void run()                   {        task.run(); }

        public V get()
            throws InterruptedException, ExecutionException {
            return task.get();
        }

        public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            return task.get(timeout, unit);
        }

        public int compareTo(Delayed other) {
            if (this == other)
                return 0;
            else if (other instanceof CustomRunnableScheduledFuture)
                return task.compareTo(((CustomRunnableScheduledFuture)other).task());
            else
                return task.compareTo(other);
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
