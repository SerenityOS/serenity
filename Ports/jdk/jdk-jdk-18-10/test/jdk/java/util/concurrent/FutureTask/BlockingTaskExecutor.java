/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6431315
 * @summary ExecutorService.invokeAll might hang
 * @author Martin Buchholz
 * @library /test/lib
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import jdk.test.lib.Utils;

/**
 * Adapted from Doug Lea, which was...
 * adapted from a posting by Tom Sugden tom at epcc.ed.ac.uk
 */
public class BlockingTaskExecutor {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    static void realMain(String[] args) throws Throwable {
        for (int i = 1; i <= 100; i++) {
            System.out.print(".");
            test();
        }
    }

    static void test() throws Throwable {
        final ExecutorService executor = Executors.newCachedThreadPool();

        final NotificationReceiver notifiee1 = new NotificationReceiver();
        final NotificationReceiver notifiee2 = new NotificationReceiver();

        final Collection<Callable<Object>> tasks = new ArrayList<>();
        tasks.add(new BlockingTask(notifiee1));
        tasks.add(new BlockingTask(notifiee2));
        tasks.add(new NonBlockingTask());

        // start a thread to invoke the tasks
        Thread thread = new Thread() { public void run() {
            try { executor.invokeAll(tasks); }
            catch (RejectedExecutionException t) {/* OK */}
            catch (Throwable t) { unexpected(t); }}};
        thread.start();

        // Wait until tasks begin execution
        notifiee1.waitForNotification();
        notifiee2.waitForNotification();

        // Now try to shutdown the executor service while tasks
        // are blocked.  This should cause the tasks to be
        // interrupted.
        executor.shutdownNow();
        if (! executor.awaitTermination(LONG_DELAY_MS, MILLISECONDS))
            throw new Error(
                String.format("Executor termination timed out after %d ms",
                              LONG_DELAY_MS));

        // Wait for the invocation thread to complete.
        thread.join(LONG_DELAY_MS);
        if (thread.isAlive()) {
            thread.interrupt();
            thread.join(LONG_DELAY_MS);
            throw new Error(
                String.format("invokeAll timed out after %d ms",
                              LONG_DELAY_MS));
        }
    }

    /**
     * A helper class with a method to wait for a notification.
     *
     * The notification is received via the
     * {@code sendNotification} method.
     */
    static class NotificationReceiver {
        /** Has the notifiee been notified? */
        boolean notified;

        /**
         * Notify the notification receiver.
         */
        public synchronized void sendNotification() {
            notified = true;
            notifyAll();
        }

        /**
         * Waits until a notification has been received.
         *
         * @throws InterruptedException if the wait is interrupted
         */
        public synchronized void waitForNotification()
            throws InterruptedException {
            while (! notified)
                wait();
        }
    }

    /**
     * A callable task that blocks until it is interrupted.
     * This task sends a notification to a notification receiver when
     * it is first called.
     */
    static class BlockingTask implements Callable<Object> {
        private final NotificationReceiver notifiee;

        BlockingTask(NotificationReceiver notifiee) {
            this.notifiee = notifiee;
        }

        public Object call() throws InterruptedException {
            notifiee.sendNotification();

            // wait indefinitely until task is interrupted
            while (true) {
                synchronized (this) {
                    wait();
                }
            }
        }
    }

    /**
     * A callable task that simply returns a string result.
     */
    static class NonBlockingTask implements Callable<Object> {
        public Object call() {
            return "NonBlockingTaskResult";
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
