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
 * @summary stress test for arrivals in a tiered phaser
 * @run main TieredArriveLoops 300
 */

import java.util.concurrent.Phaser;

public class TieredArriveLoops {
    final long testDurationMillisDefault = 10_000L;
    final long testDurationMillis;
    final long quittingTimeNanos;

    TieredArriveLoops(String[] args) {
        testDurationMillis = (args.length > 0) ?
            Long.valueOf(args[0]) : testDurationMillisDefault;
        quittingTimeNanos = System.nanoTime() +
            testDurationMillis * 1000L * 1000L;
    }

    Runnable runner(final Phaser p) {
        return new CheckedRunnable() { public void realRun() {
            int prevPhase = p.register();
            while (!p.isTerminated()) {
                int phase = p.awaitAdvance(p.arrive());
                if (phase < 0)
                    return;
                equal(phase, (prevPhase + 1) & Integer.MAX_VALUE);
                int ph = p.getPhase();
                check(ph < 0 || ph == phase);
                prevPhase = phase;
            }
        }};
    }

    void test(String[] args) throws Throwable {
        final Phaser parent = new Phaser();
        final Phaser child1 = new Phaser(parent);
        final Phaser child2 = new Phaser(parent);

        Thread t1 = new Thread(runner(child1));
        Thread t2 = new Thread(runner(child2));
        t1.start();
        t2.start();

        for (int prevPhase = 0, phase; ; prevPhase = phase) {
            phase = child2.getPhase();
            check(phase >= prevPhase);
            if (System.nanoTime() - quittingTimeNanos > 0) {
                System.err.printf("phase=%d%n", phase);
                child1.forceTermination();
                break;
            }
        }

        t1.join();
        t2.join();
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
        new TieredArriveLoops(args).instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}

    abstract class CheckedRunnable implements Runnable {
        protected abstract void realRun() throws Throwable;

        public final void run() {
            try {realRun();} catch (Throwable t) {unexpected(t);}
        }
    }
}
