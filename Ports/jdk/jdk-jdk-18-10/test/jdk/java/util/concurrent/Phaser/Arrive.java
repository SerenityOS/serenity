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
 * @bug 6445158
 * @summary tests for Phaser.arrive()
 */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Arrive {
    void test(String[] args) throws Throwable {
        for (int i = 0; i < 100; ++i)
            doTest(args);
    }
    void doTest(String[] args) throws Throwable {
        final int n = ThreadLocalRandom.current().nextInt(1, 10);
        final Phaser startingGate = new Phaser(n);
        final Phaser phaser = new Phaser(n);
        final List<Thread> threads = new ArrayList<>();
        final AtomicInteger count0 = new AtomicInteger(0);
        final AtomicInteger count1 = new AtomicInteger(0);
        final Runnable task = new Runnable() { public void run() {
            equal(startingGate.getPhase(), 0);
            startingGate.arriveAndAwaitAdvance();
            equal(startingGate.getPhase(), 1);
            int phase = phaser.arrive();
            if (phase == 0)
                count0.getAndIncrement();
            else if (phase == 1)
                count1.getAndIncrement();
            else
                fail();
        }};
        for (int i = 0; i < n; i++)
            threads.add(new Thread(task));
        for (Thread thread : threads)
            thread.start();
        for (Thread thread : threads)
            thread.join();
        equal(count0.get(), n);
        equal(count1.get(), 0);
        equal(phaser.getPhase(), 1);
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
        new Arrive().instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
