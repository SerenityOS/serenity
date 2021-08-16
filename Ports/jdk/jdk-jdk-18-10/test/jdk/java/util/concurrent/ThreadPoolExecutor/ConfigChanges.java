/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6450200
 * @summary Test proper handling of pool state changes
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main/othervm -Djava.security.manager=allow ConfigChanges
 * @key randomness
 * @author Martin Buchholz
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.security.Permission;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import jdk.test.lib.RandomFactory;

public class ConfigChanges {
    static final ThreadGroup tg = new ThreadGroup("pool");

    static final Random rnd = RandomFactory.getRandom();

    static void report(ThreadPoolExecutor tpe) {
        try {
            System.out.printf(
                "active=%d submitted=%d completed=%d queued=%d sizes=%d/%d/%d%n",
                tg.activeCount(),
                tpe.getTaskCount(),
                tpe.getCompletedTaskCount(),
                tpe.getQueue().size(),
                tpe.getPoolSize(),
                tpe.getCorePoolSize(),
                tpe.getMaximumPoolSize());
        } catch (Throwable t) { unexpected(t); }
    }

    static void report(String label, ThreadPoolExecutor tpe) {
        System.out.printf("%10s ", label);
        report(tpe);
    }

    static class PermissiveSecurityManger extends SecurityManager {
        public void checkPermission(Permission p) { /* bien sur, Monsieur */ }
    }

    static void checkShutdown(final ExecutorService es) {
        final Runnable nop = new Runnable() {public void run() {}};
        try {
            if (new Random().nextBoolean()) {
                check(es.isShutdown());
                if (es instanceof ThreadPoolExecutor)
                    check(((ThreadPoolExecutor) es).isTerminating()
                          || es.isTerminated());
                THROWS(RejectedExecutionException.class,
                       () -> es.execute(nop));
            }
        } catch (Throwable t) { unexpected(t); }
    }

    static void checkTerminated(final ThreadPoolExecutor tpe) {
        try {
            checkShutdown(tpe);
            check(tpe.getQueue().isEmpty());
            check(tpe.isTerminated());
            check(! tpe.isTerminating());
            equal(0, tpe.getActiveCount());
            equal(0, tpe.getPoolSize());
            equal(tpe.getTaskCount(), tpe.getCompletedTaskCount());
            check(tpe.awaitTermination(0L, MINUTES));
        } catch (Throwable t) { unexpected(t); }
    }

    static Runnable waiter(final CyclicBarrier barrier) {
        return new Runnable() { public void run() {
            try { barrier.await(); barrier.await(); }
            catch (Throwable t) { unexpected(t); }}};
    }

    static volatile Runnable runnableDuJour;

    static void awaitIdleness(ThreadPoolExecutor tpe, long taskCount) {
        restart: for (;;) {
            // check twice to make chance of race vanishingly small
            for (int i = 0; i < 2; i++) {
                if (tpe.getQueue().size() != 0 ||
                    tpe.getActiveCount() != 0 ||
                    tpe.getCompletedTaskCount() != taskCount) {
                    Thread.yield();
                    continue restart;
                }
            }
            return;
        }
    }

    /**
     * Waits for condition to become true, first spin-polling, then sleep-polling.
     */
    static void spinAwait(Supplier<Boolean> waitingForGodot) {
        for (int spins = 0; !waitingForGodot.get(); ) {
            if ((spins = (spins + 1) & 3) > 0) {
                Thread.yield();
            } else {
                try { Thread.sleep(4); }
                catch (InterruptedException unexpected) {
                    throw new AssertionError(unexpected);
                }
            }
        }
    }

    private static void realMain(String[] args) throws Throwable {
        if (rnd.nextBoolean())
            System.setSecurityManager(new PermissiveSecurityManger());

        final boolean prestart = rnd.nextBoolean();

        final Thread.UncaughtExceptionHandler handler
            = new Thread.UncaughtExceptionHandler() {
                    public void uncaughtException(Thread t, Throwable e) {
                        check(! Thread.currentThread().isInterrupted());
                        unexpected(e);
                    }};

        final int n = 3;
        final ThreadPoolExecutor tpe
            = new ThreadPoolExecutor(n, 3*n,
                                     3L, MINUTES,
                                     new ArrayBlockingQueue<Runnable>(3*n));
        tpe.setThreadFactory(new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(tg, r);
                    t.setUncaughtExceptionHandler(handler);
                    return t;
                }});

        if (prestart) {
            tpe.prestartAllCoreThreads();
            equal(n, tg.activeCount());
            equal(n, tpe.getCorePoolSize());
            equal(n, tpe.getLargestPoolSize());
        }

        final Runnable runRunnableDuJour =
            new Runnable() { public void run() {
                // Delay choice of action till last possible moment.
                runnableDuJour.run(); }};
        final CyclicBarrier pumpedUp = new CyclicBarrier(3*n + 1);
        runnableDuJour = waiter(pumpedUp);

        if (prestart) {
            for (int i = 0; i < 1*n; i++)
                tpe.execute(runRunnableDuJour);
            // Wait for prestarted threads to dequeue their initial tasks.
            while (! tpe.getQueue().isEmpty())
                Thread.sleep(1);
            for (int i = 0; i < 5*n; i++)
                tpe.execute(runRunnableDuJour);
        } else {
            for (int i = 0; i < 6*n; i++)
                tpe.execute(runRunnableDuJour);
        }

        //report("submitted", tpe);
        pumpedUp.await();
        equal(3*n, tg.activeCount());
        equal(3*n, tpe.getMaximumPoolSize());
        equal(3*n, tpe.getLargestPoolSize());
        equal(n, tpe.getCorePoolSize());
        equal(3*n, tpe.getActiveCount());
        equal(6L*n, tpe.getTaskCount());
        equal(0L, tpe.getCompletedTaskCount());

        //report("pumped up", tpe);
        tpe.setMaximumPoolSize(4*n);
        equal(4*n, tpe.getMaximumPoolSize());
        //report("pumped up2", tpe);
        final CyclicBarrier pumpedUp2 = new CyclicBarrier(n + 1);
        runnableDuJour = waiter(pumpedUp2);
        for (int i = 0; i < 1*n; i++)
            tpe.execute(runRunnableDuJour);
        pumpedUp2.await();
        equal(4*n, tg.activeCount());
        equal(4*n, tpe.getMaximumPoolSize());
        equal(4*n, tpe.getLargestPoolSize());
        equal(4*n, tpe.getActiveCount());
        equal(7L*n, tpe.getTaskCount());
        equal(0L, tpe.getCompletedTaskCount());
        //report("pumped up2", tpe);
        runnableDuJour = new Runnable() { public void run() {}};

        tpe.setMaximumPoolSize(2*n);
        //report("after setMaximumPoolSize", tpe);

        pumpedUp2.await();
        pumpedUp.await();

        spinAwait(() -> tg.activeCount() == 2*n);
        equal(2*n, tpe.getMaximumPoolSize());
        equal(4*n, tpe.getLargestPoolSize());

        //report("draining", tpe);
        awaitIdleness(tpe, 7L*n);

        equal(2*n, tg.activeCount());
        equal(2*n, tpe.getMaximumPoolSize());
        equal(4*n, tpe.getLargestPoolSize());

        equal(7L*n, tpe.getTaskCount());
        equal(7L*n, tpe.getCompletedTaskCount());
        equal(0, tpe.getActiveCount());

        equal(3L, tpe.getKeepAliveTime(MINUTES));
        long t0 = System.nanoTime();
        tpe.setKeepAliveTime(7L, MILLISECONDS);
        equal(7L, tpe.getKeepAliveTime(MILLISECONDS));
        spinAwait(() -> tg.activeCount() == n);
        check(System.nanoTime() - t0 >= tpe.getKeepAliveTime(NANOSECONDS));

        //report("idle", tpe);
        check(! tpe.allowsCoreThreadTimeOut());
        t0 = System.nanoTime();
        tpe.allowCoreThreadTimeOut(true);
        check(tpe.allowsCoreThreadTimeOut());
        spinAwait(() -> tg.activeCount() == 0);

        // The following assertion is almost always true, but may
        // exceptionally not be during a transition from core count
        // too high to allowCoreThreadTimeOut.  Users will never
        // notice, and we accept the small loss of testability.
        //
        // check(System.nanoTime() - t0 >= tpe.getKeepAliveTime(NANOSECONDS));

        //report("idle", tpe);

        tpe.shutdown();
        checkShutdown(tpe);
        check(tpe.awaitTermination(3L, MINUTES));
        checkTerminated(tpe);
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
    interface Fun {void f() throws Throwable;}
    static void THROWS(Class<? extends Throwable> k, Fun... fs) {
        for (Fun f : fs)
            try { f.f(); fail("Expected " + k.getName() + " not thrown"); }
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}}
}
