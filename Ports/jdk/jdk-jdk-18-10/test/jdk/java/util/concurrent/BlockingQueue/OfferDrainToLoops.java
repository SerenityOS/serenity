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
 * @bug 6805775 6815766
 * @library /test/lib
 * @run main OfferDrainToLoops 100
 * @summary Test concurrent offer vs. drainTo
 */

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicLong;
import jdk.test.lib.Utils;

@SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
public class OfferDrainToLoops {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    final long testDurationMillisDefault = 10_000L;
    final long testDurationMillis;

    OfferDrainToLoops(String[] args) {
        testDurationMillis = (args.length > 0) ?
            Long.valueOf(args[0]) : testDurationMillisDefault;
    }

    void checkNotContainsNull(Iterable it) {
        for (Object x : it)
            check(x != null);
    }

    void test(String[] args) throws Throwable {
        test(new LinkedBlockingQueue());
        test(new LinkedBlockingQueue(2000));
        test(new LinkedBlockingDeque());
        test(new LinkedBlockingDeque(2000));
        test(new ArrayBlockingQueue(2000));
        test(new LinkedTransferQueue());
    }

    void test(final BlockingQueue q) throws Throwable {
        System.out.println(q.getClass().getSimpleName());
        final long testDurationNanos = testDurationMillis * 1000L * 1000L;
        final long quittingTimeNanos = System.nanoTime() + testDurationNanos;
        final SplittableRandom rnd = new SplittableRandom();

        // Poor man's bounded buffer.
        final AtomicLong approximateCount = new AtomicLong(0L);

        abstract class CheckedThread extends Thread {
            final SplittableRandom rnd;

            CheckedThread(String name, SplittableRandom rnd) {
                super(name);
                this.rnd = rnd;
                setDaemon(true);
                start();
            }
            /** Polls for quitting time. */
            protected boolean quittingTime() {
                return System.nanoTime() - quittingTimeNanos > 0;
            }
            /** Polls occasionally for quitting time. */
            protected boolean quittingTime(long i) {
                return (i % 1024) == 0 && quittingTime();
            }
            protected abstract void realRun();
            public void run() {
                try { realRun(); } catch (Throwable t) { unexpected(t); }
            }
        }

        Thread offerer = new CheckedThread("offerer", rnd.split()) {
            protected void realRun() {
                long c = 0;
                for (long i = 0; ! quittingTime(i); i++) {
                    if (q.offer(c)) {
                        if ((++c % 1024) == 0) {
                            approximateCount.getAndAdd(1024);
                            while (approximateCount.get() > 10000)
                                Thread.yield();
                        }
                    } else {
                        Thread.yield();
                    }}}};

        Thread drainer = new CheckedThread("drainer", rnd.split()) {
            protected void realRun() {
                while (! quittingTime()) {
                    List list = new ArrayList();
                    int n = rnd.nextBoolean() ?
                        q.drainTo(list) :
                        q.drainTo(list, 100);
                    approximateCount.getAndAdd(-n);
                    equal(list.size(), n);
                    for (int j = 0; j < n - 1; j++)
                        equal((Long) list.get(j) + 1L, list.get(j + 1));
                    Thread.yield();
                }
                q.clear();
                approximateCount.set(0); // Releases waiting offerer thread
            }};

        Thread scanner = new CheckedThread("scanner", rnd.split()) {
            protected void realRun() {
                while (! quittingTime()) {
                    switch (rnd.nextInt(3)) {
                    case 0: checkNotContainsNull(q); break;
                    case 1: q.size(); break;
                    case 2:
                        Long[] a = (Long[]) q.toArray(new Long[0]);
                        int n = a.length;
                        for (int j = 0; j < n - 1; j++) {
                            check(a[j] < a[j+1]);
                            check(a[j] != null);
                        }
                        break;
                    }
                    Thread.yield();
                }}};

        for (Thread thread : new Thread[] { offerer, drainer, scanner }) {
            thread.join(LONG_DELAY_MS + testDurationMillis);
            if (thread.isAlive()) {
                System.err.printf("Hung thread: %s%n", thread.getName());
                failed++;
                for (StackTraceElement e : thread.getStackTrace())
                    System.err.println(e);
                thread.join(LONG_DELAY_MS);
            }
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
        new OfferDrainToLoops(args).instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
