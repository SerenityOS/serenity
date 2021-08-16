/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6602600
 * @run main/othervm -Xmx8m BasicCancelTest
 * @summary Check effectiveness of RemoveOnCancelPolicy
 */

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Simple timer cancellation test. Submits tasks to a scheduled executor
 * service and immediately cancels them.
 */
public class BasicCancelTest {

    void checkShutdown(final ExecutorService es) {
        final Runnable nop = new Runnable() {public void run() {}};
        try {
            if (new Random().nextBoolean()) {
                check(es.isShutdown());
                if (es instanceof ThreadPoolExecutor)
                    check(((ThreadPoolExecutor) es).isTerminating()
                          || es.isTerminated());
                THROWS(RejectedExecutionException.class,
                       new F(){void f(){es.execute(nop);}});
            }
        } catch (Throwable t) { unexpected(t); }
    }

    void checkTerminated(final ThreadPoolExecutor tpe) {
        try {
            checkShutdown(tpe);
            check(tpe.getQueue().isEmpty());
            check(tpe.isTerminated());
            check(! tpe.isTerminating());
            equal(tpe.getActiveCount(), 0);
            equal(tpe.getPoolSize(), 0);
            equal(tpe.getTaskCount(), tpe.getCompletedTaskCount());
            check(tpe.awaitTermination(0L, TimeUnit.SECONDS));
        } catch (Throwable t) { unexpected(t); }
    }

    void test(String[] args) throws Throwable {

        final ScheduledThreadPoolExecutor pool =
            new ScheduledThreadPoolExecutor(1);

        // Needed to avoid OOME
        pool.setRemoveOnCancelPolicy(true);

        final long moreThanYouCanChew = Runtime.getRuntime().freeMemory() / 4;
        System.out.printf("moreThanYouCanChew=%d%n", moreThanYouCanChew);

        Runnable noopTask = new Runnable() { public void run() {}};

        for (long i = 0; i < moreThanYouCanChew; i++)
            pool.schedule(noopTask, 10, TimeUnit.MINUTES).cancel(true);

        pool.shutdown();
        check(pool.awaitTermination(1L, TimeUnit.DAYS));
        checkTerminated(pool);
        equal(pool.getTaskCount(), 0L);
        equal(pool.getCompletedTaskCount(), 0L);
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
        new BasicCancelTest().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    abstract class F {abstract void f() throws Throwable;}
    void THROWS(Class<? extends Throwable> k, F... fs) {
        for (F f : fs)
            try {f.f(); fail("Expected " + k.getName() + " not thrown");}
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}}
}
