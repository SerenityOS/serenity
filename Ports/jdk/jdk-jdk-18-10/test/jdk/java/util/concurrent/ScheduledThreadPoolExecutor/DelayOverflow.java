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

/*
 * @test
 * @bug 6725789
 * @summary Check for long overflow in task time comparison.
 * @library /test/lib
 */

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import jdk.test.lib.Utils;

public class DelayOverflow {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    static void waitForNanoTimeTick() {
        for (long t0 = System.nanoTime(); t0 == System.nanoTime(); )
            ;
    }

    void scheduleNow(ScheduledThreadPoolExecutor pool,
                     Runnable r, int how) {
        switch (how) {
        case 0:
            pool.schedule(r, 0, MILLISECONDS);
            break;
        case 1:
            pool.schedule(Executors.callable(r), 0, DAYS);
            break;
        case 2:
            pool.scheduleWithFixedDelay(r, 0, 1000, NANOSECONDS);
            break;
        case 3:
            pool.scheduleAtFixedRate(r, 0, 1000, MILLISECONDS);
            break;
        default:
            fail(String.valueOf(how));
        }
    }

    void scheduleAtTheEndOfTime(ScheduledThreadPoolExecutor pool,
                                Runnable r, int how) {
        switch (how) {
        case 0:
            pool.schedule(r, Long.MAX_VALUE, MILLISECONDS);
            break;
        case 1:
            pool.schedule(Executors.callable(r), Long.MAX_VALUE, DAYS);
            break;
        case 2:
            pool.scheduleWithFixedDelay(r, Long.MAX_VALUE, 1000, NANOSECONDS);
            break;
        case 3:
            pool.scheduleAtFixedRate(r, Long.MAX_VALUE, 1000, MILLISECONDS);
            break;
        default:
            fail(String.valueOf(how));
        }
    }

    /**
     * Attempts to test exhaustively and deterministically, all 20
     * possible ways that one task can be scheduled in the maximal
     * distant future, while at the same time an existing task's time
     * has already expired.
     */
    void test(String[] args) throws Throwable {
        for (int nowHow = 0; nowHow < 4; nowHow++) {
            for (int thenHow = 0; thenHow < 4; thenHow++) {

                final ScheduledThreadPoolExecutor pool
                    = new ScheduledThreadPoolExecutor(1);
                final CountDownLatch runLatch     = new CountDownLatch(1);
                final CountDownLatch busyLatch    = new CountDownLatch(1);
                final CountDownLatch proceedLatch = new CountDownLatch(1);
                final Runnable notifier = new Runnable() {
                        public void run() { runLatch.countDown(); }};
                final Runnable neverRuns = new Runnable() {
                        public void run() { fail(); }};
                final Runnable keepPoolBusy = new Runnable() {
                        public void run() {
                            try {
                                busyLatch.countDown();
                                proceedLatch.await();
                            } catch (Throwable t) { unexpected(t); }
                        }};
                pool.schedule(keepPoolBusy, 0, DAYS);
                busyLatch.await();
                scheduleNow(pool, notifier, nowHow);
                waitForNanoTimeTick();
                scheduleAtTheEndOfTime(pool, neverRuns, thenHow);
                proceedLatch.countDown();

                check(runLatch.await(LONG_DELAY_MS, MILLISECONDS));
                equal(runLatch.getCount(), 0L);

                pool.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
                pool.shutdown();
            }

            final int nowHowCopy = nowHow;
            final ScheduledThreadPoolExecutor pool
                = new ScheduledThreadPoolExecutor(1);
            final CountDownLatch runLatch = new CountDownLatch(1);
            final Runnable notifier = new Runnable() {
                    public void run() { runLatch.countDown(); }};
            final Runnable scheduleNowScheduler = new Runnable() {
                    public void run() {
                        try {
                            scheduleNow(pool, notifier, nowHowCopy);
                            waitForNanoTimeTick();
                        } catch (Throwable t) { unexpected(t); }
                    }};
            pool.scheduleWithFixedDelay(scheduleNowScheduler,
                                        0, Long.MAX_VALUE, NANOSECONDS);

            check(runLatch.await(LONG_DELAY_MS, MILLISECONDS));
            equal(runLatch.getCount(), 0L);

            pool.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            pool.shutdown();
        }
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
        new DelayOverflow().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
