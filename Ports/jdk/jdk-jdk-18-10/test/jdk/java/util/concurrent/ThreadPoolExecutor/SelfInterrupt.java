/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6576792
 * @summary non-idle worker threads should not be interrupted
 * @library /test/lib
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jdk.test.lib.Utils;

public class SelfInterrupt {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    void test(String[] args) throws Throwable {
        final int n = 100;
        final ThreadPoolExecutor pool =
            new ThreadPoolExecutor(n, n, 1L, TimeUnit.NANOSECONDS,
                                   new SynchronousQueue<Runnable>());
        final CountDownLatch startingGate = new CountDownLatch(n);
        final CountDownLatch finishLine = new CountDownLatch(n);
        equal(pool.getCorePoolSize(), n);
        equal(pool.getPoolSize(), 0);
        for (int i = 0; i < n; i++)
            pool.execute(new Runnable() { public void run() {
                try {
                    startingGate.countDown();
                    startingGate.await();
                    equal(pool.getPoolSize(), n);
                    pool.setCorePoolSize(n);
                    pool.setCorePoolSize(1);
                    check(! Thread.interrupted());
                    equal(pool.getPoolSize(), n);
                    finishLine.countDown();
                    finishLine.await();
                    check(! Thread.interrupted());
                } catch (Throwable t) { unexpected(t); }}});
        finishLine.await();
        pool.shutdown();
        check(pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
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
        new SelfInterrupt().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
