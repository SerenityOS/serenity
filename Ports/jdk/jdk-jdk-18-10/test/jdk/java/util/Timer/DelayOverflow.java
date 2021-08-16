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
 * @bug 6730507
 * @summary java.util.Timer schedule delay Long.MAX_VALUE causes task to execute multiple times
 * @author Chris Hegarty
 * @author Martin Buchholz
 * @key randomness
 */

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DelayOverflow
{
    void scheduleNow(Timer timer, TimerTask task, int how) {
        switch (how) {
            case 0 :
                timer.schedule(task, new Date(), Long.MAX_VALUE);
                break;
            case 1:
                timer.schedule(task, 0L, Long.MAX_VALUE);
                break;
            case 2:
                timer.scheduleAtFixedRate(task, new Date(), Long.MAX_VALUE);
                break;
            case 3:
                timer.scheduleAtFixedRate(task, 0L, Long.MAX_VALUE);
                break;
            default:
                fail(String.valueOf(how));
        }
    }

    void sleep(long millis) {
        try { Thread.sleep(millis); }
        catch (Throwable t) { unexpected(t); }
    }

    /** Checks that scheduledExecutionTime returns a "recent" time. */
    void checkScheduledExecutionTime(TimerTask task) {
        long t = System.currentTimeMillis()
            - task.scheduledExecutionTime();
        check(t >= 0 && t < 1000 * 600);
    }

    void test(String[] args) throws Throwable {
        for (int how=0; how<4; how++) {
            final CountDownLatch done = new CountDownLatch(1);
            final AtomicInteger count = new AtomicInteger(0);
            final Timer timer = new Timer();
            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    checkScheduledExecutionTime(this);
                    count.incrementAndGet();
                    done.countDown();
                }};

            scheduleNow(timer, task, how);
            done.await();
            equal(count.get(), 1);
            checkScheduledExecutionTime(task);
            if (new java.util.Random().nextBoolean())
                sleep(10);
            check(task.cancel());
            timer.cancel();
            checkScheduledExecutionTime(task);
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
        Class<?> k = new Object(){}.getClass().getEnclosingClass();
        try {k.getMethod("instanceMain",String[].class)
                .invoke( k.newInstance(), (Object) args);}
        catch (Throwable e) {throw e.getCause();}}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
