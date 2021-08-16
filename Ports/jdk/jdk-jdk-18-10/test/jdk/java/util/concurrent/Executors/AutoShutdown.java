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
 * @bug 6399443
 * @summary Check for auto-shutdown and gc of singleThreadExecutors
 * @library /test/lib
 * @run main/othervm/timeout=1000 AutoShutdown
 * @author Martin Buchholz
 */

import static java.util.concurrent.Executors.defaultThreadFactory;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import jdk.test.lib.Utils;

public class AutoShutdown {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(LONG_DELAY_MS, MILLISECONDS))
            throw new AssertionError("timed out waiting for latch");
    }

    private static void realMain(String[] args) throws Throwable {
        final Executor[] executors = {
            newSingleThreadExecutor(),
            newSingleThreadExecutor(defaultThreadFactory()),
            // TODO: should these executors also auto-shutdown?
            //newFixedThreadPool(1),
            //newSingleThreadScheduledExecutor(),
            //newSingleThreadScheduledExecutor(defaultThreadFactory()),
        };
        final ConcurrentLinkedQueue<WeakReference<Thread>> poolThreads
            = new ConcurrentLinkedQueue<>();
        final CountDownLatch threadStarted
            = new CountDownLatch(executors.length);
        final CountDownLatch pleaseProceed
            = new CountDownLatch(1);
        Runnable task = new Runnable() { public void run() {
            try {
                poolThreads.add(new WeakReference<>(Thread.currentThread()));
                threadStarted.countDown();
                await(pleaseProceed);
            } catch (Throwable t) { unexpected(t); }
        }};
        for (Executor executor : executors)
            executor.execute(task);
        await(threadStarted);
        pleaseProceed.countDown();
        Arrays.fill(executors, null);   // make executors unreachable
        boolean done = false;
        for (long timeout = 1L; !done && timeout <= 128L; timeout *= 2) {
            System.gc();
            done = true;
            for (WeakReference<Thread> ref : poolThreads) {
                Thread thread = ref.get();
                if (thread != null) {
                    TimeUnit.SECONDS.timedJoin(thread, timeout);
                    if (thread.isAlive())
                        done = false;
                }
            }
        }
        if (!done)
            throw new AssertionError("pool threads did not terminate");
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
