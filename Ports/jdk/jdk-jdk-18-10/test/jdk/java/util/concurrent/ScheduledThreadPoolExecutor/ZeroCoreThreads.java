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
 * Written by Martin Buchholz with assistance from members of JCP
 * JSR-166 Expert Group and released to the public domain, as
 * explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 8022642 8065320 8129861
 * @summary Ensure relative sanity when zero core threads
 * @library /test/lib
 * @modules java.base/java.util.concurrent:open
 */

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.lang.reflect.Field;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import jdk.test.lib.Utils;

public class ZeroCoreThreads {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

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

    static boolean hasWaiters(ReentrantLock lock, Condition condition) {
        lock.lock();
        try {
            return lock.hasWaiters(condition);
        } finally {
            lock.unlock();
        }
    }

    static void awaitHasWaiters(ReentrantLock lock, Condition condition,
                                long timeoutMillis) {
        spinWaitUntil(() -> hasWaiters(lock, condition), timeoutMillis);
    }

    static <T> T getField(Object x, String fieldName) {
        try {
            Field field = x.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(x);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    void test(String[] args) throws Throwable {
        ScheduledThreadPoolExecutor p = new ScheduledThreadPoolExecutor(0);
        try {
            test(p);
        } finally {
            p.shutdownNow();
            check(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
        }
    }

    void test(ScheduledThreadPoolExecutor p) throws Throwable {
        Runnable dummy = new Runnable() { public void run() {
            throw new AssertionError("shouldn't get here"); }};
        BlockingQueue q = p.getQueue();
        ReentrantLock lock = getField(q, "lock");
        Condition available = getField(q, "available");

        equal(0, p.getPoolSize());
        equal(0, p.getLargestPoolSize());
        equal(0L, p.getTaskCount());
        equal(0L, p.getCompletedTaskCount());
        p.schedule(dummy, 1L, HOURS);
        // Ensure one pool thread actually waits in timed queue poll
        awaitHasWaiters(lock, available, LONG_DELAY_MS);
        equal(1, p.getPoolSize());
        equal(1, p.getLargestPoolSize());
        equal(1L, p.getTaskCount());
        equal(0L, p.getCompletedTaskCount());
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
        new ZeroCoreThreads().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
