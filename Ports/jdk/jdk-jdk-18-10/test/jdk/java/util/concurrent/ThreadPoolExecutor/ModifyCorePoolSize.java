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
 * @bug 6522773
 * @summary Test changes to STPE core pool size
 * @author Martin Buchholz
 */

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ModifyCorePoolSize {
    static void awaitPoolSize(ThreadPoolExecutor pool, int n) {
        while (pool.getPoolSize() != n) Thread.yield();
        pass();
    }

    static void setCorePoolSize(ThreadPoolExecutor pool, int n) {
        pool.setCorePoolSize(n);
        equal(pool.getCorePoolSize(), n);
        awaitPoolSize(pool, n);
    }

    static void realMain(String[] args) throws Throwable {
        final int size = 10;
        final ScheduledThreadPoolExecutor pool
            = new ScheduledThreadPoolExecutor(size);
        final Runnable nop = new Runnable() { public void run() {}};

        for (int i = 0; i < size; i++)
            pool.scheduleAtFixedRate(nop, 100L * (i + 1),
                                     1000L, TimeUnit.MILLISECONDS);
        awaitPoolSize(pool, size);
        setCorePoolSize(pool, size - 3);
        setCorePoolSize(pool, size + 3);
        pool.shutdownNow();
        check(pool.awaitTermination(1L, TimeUnit.DAYS));
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
