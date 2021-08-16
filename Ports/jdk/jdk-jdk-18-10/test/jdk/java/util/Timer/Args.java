/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6571655 6571881 6574585 6571297
 * @summary Test various args to task scheduling methods
 */

import java.util.*;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.*;

public class Args {
    static final long DELAY_MS = 30 * 1000L;

    void schedule(final Timer t, final TimerTask task, final Date d) {
        t.schedule(task, d);
        assertThrows
            (IllegalStateException.class,
             () -> t.schedule(task, d));
    }

    void schedule(final Timer t, final TimerTask task, final Date d, final
long period) {
        t.schedule(task, d, period);
        assertThrows
            (IllegalStateException.class,
             () -> t.schedule(task, d, period));
    }

    void scheduleAtFixedRate(final Timer t, final TimerTask task, final
Date d, final long period) {
        t.scheduleAtFixedRate(task, d, period);
        assertThrows
            (IllegalStateException.class,
             () -> t.scheduleAtFixedRate(task, d, period));
    }

    TimerTask counter(final CountDownLatch latch) {
        return new TimerTask() { public void run() {
            if (latch.getCount() == 0)
                fail(String.format("Latch counted down too many times: " +
latch));
            latch.countDown();
        }};
    }

    TimerTask nop() {
        return new TimerTask() { public void run() { }};
    }

    void test(String[] args) throws Throwable {
        final Timer t = new Timer();
        try {
            test(t);
        } finally {
            // Ensure this test doesn't interfere with subsequent
            // tests even in case of failure.
            t.cancel();
        }

        // Attempts to schedule tasks on a cancelled Timer result in ISE.

        final Date past = new Date(System.currentTimeMillis() - DELAY_MS);
        final Date future = new Date(System.currentTimeMillis() + DELAY_MS);
        assertThrows
            (IllegalStateException.class,
             () -> t.schedule(nop(), 42),
             () -> t.schedule(nop(), 42),
             () -> t.schedule(nop(), past),
             () -> t.schedule(nop(), 42, 42),
             () -> t.schedule(nop(), past, 42),
             () -> t.scheduleAtFixedRate(nop(), 42, 42),
             () -> t.scheduleAtFixedRate(nop(), past, 42),
             () -> t.scheduleAtFixedRate(nop(), future, 42));
    }

    void test(Timer t) throws Throwable {
        final TimerTask x = new TimerTask() { public void run() {}};
        assertThrows
            (IllegalArgumentException.class,
             () -> t.schedule(x, -42),
             () -> t.schedule(x, new Date(-42)),

             () -> t.schedule(x, Long.MAX_VALUE),
             () -> t.schedule(x, -42, 42),
             () -> t.schedule(x, new Date(-42), 42),
             () -> t.schedule(x, Long.MAX_VALUE, 42),
             () -> t.schedule(x, 42, 0),
             () -> t.schedule(x, new Date(42), 0),
             () -> t.schedule(x, 42, -42),
             () -> t.schedule(x, new Date(42), -42),

             () -> t.scheduleAtFixedRate(x, -42, 42),
             () -> t.scheduleAtFixedRate(x, new Date(-42), 42),
             () -> t.scheduleAtFixedRate(x, Long.MAX_VALUE, 42),
             () -> t.scheduleAtFixedRate(x, 42, 0),
             () -> t.scheduleAtFixedRate(x, new Date(42), 0),
             () -> t.scheduleAtFixedRate(x, 42, -42),
             () -> t.scheduleAtFixedRate(x, new Date(42), -42));

        assertThrows
            (NullPointerException.class,
             () -> t.schedule(null, 42),
             () -> t.schedule(x, (Date)null),

             () -> t.schedule(null, 42, 42),
             () -> t.schedule(x, (Date)null, 42),

             () -> t.scheduleAtFixedRate(null, 42, 42),
             () -> t.scheduleAtFixedRate(x, (Date)null, 42));

        // Create local classes for clearer diagnostics in case of failure
        class OneShotLatch extends CountDownLatch {
            OneShotLatch() { super(1); }
        }
        class FixedDelayLatch extends CountDownLatch {
            FixedDelayLatch() { super(1); }
        }
        class FixedRateLatch extends CountDownLatch {
            FixedRateLatch() { super(11); }
        }
        final CountDownLatch y1 = new OneShotLatch();
        final CountDownLatch y2 = new FixedDelayLatch();
        final CountDownLatch y3 = new FixedRateLatch();

        final long start = System.currentTimeMillis();
        final Date past = new Date(start - (10 * DELAY_MS + DELAY_MS / 2));

        schedule(           t, counter(y1), past);
        schedule(           t, counter(y2), past, DELAY_MS);
        scheduleAtFixedRate(t, counter(y3), past, DELAY_MS);

        check(y1.await(DELAY_MS / 4, MILLISECONDS));
        check(y2.await(DELAY_MS / 4, MILLISECONDS));
        check(y3.await(DELAY_MS / 4, MILLISECONDS));

        final long elapsed = System.currentTimeMillis() - start;
        if (elapsed >= DELAY_MS / 2)
            fail(String.format("Test took too long: elapsed=%d%n",
elapsed));
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
        new Args().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    interface F { void f() throws Throwable; }
    void assertThrows(Class<? extends Throwable> k, F... fs) {
        for (F f : fs)
            try {f.f(); fail("Expected " + k.getName() + " not thrown");}
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}}
}

