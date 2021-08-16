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
 * @bug 8008378
 * @summary Basic checks for parallelism 0, and null returning factory
 * @run main/othervm -Djava.util.concurrent.ForkJoinPool.common.parallelism=0 ThreadLessCommon
 * @run main/othervm -Djava.util.concurrent.ForkJoinPool.common.threadFactory=ThreadLessCommon$NullForkJoinWorkerThreadFactory ThreadLessCommon
 * @author Chris Hegarty
 */

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.RecursiveTask;

public class ThreadLessCommon {

    static final int THRESHOLD = 1000;
    static final boolean debug = true;

    private static void realMain(String[] args) throws Throwable {
        if (debug) {
            String pp = System.getProperty(
                    "java.util.concurrent.ForkJoinPool.common.parallelism");
            System.out.println(
                    "java.util.concurrent.ForkJoinPool.common.parallelism:" + pp);
            String tf = System.getProperty(
                    "java.util.concurrent.ForkJoinPool.common.threadFactory");
            System.out.println(
                    "java.util.concurrent.ForkJoinPool.common.threadFactory:" + tf);
        }

        long from = 0, to = 50000;
        RecursiveTask<Long> task = new SumTask(from, to, Thread.currentThread());
        long sum = task.invoke();
        System.out.printf("%nSum: from [%d] to [%d] = [%d]%n", from, to, sum);

        task.fork();
        sum = task.join();
        System.out.printf("%nSum: from [%d] to [%d] = [%d]%n", from, to, sum);

        sum = ForkJoinPool.commonPool().invoke(task.fork());
        System.out.printf("%nSum: from [%d] to [%d] = [%d]%n", from, to, sum);
    }

    static class SumTask extends RecursiveTask<Long> {
        final Thread expectedThread;
        final long from;
        final long to;
        SumTask(long from, long to, Thread thread) {
            this.from = from; this.to = to; expectedThread = thread;
        }

        @Override
        public Long compute() {
            check(Thread.currentThread() == expectedThread,
                  "Expected " + expectedThread + ", got " + Thread.currentThread());
            long range = to - from;
            if (range < THRESHOLD) {
                long acc = 0;
                for (long i = from; i <= to; i++)
                    acc = acc + i;
                return acc;
            } else {
                long half = from + range / 2;
                SumTask t1 = new SumTask(from, half ,expectedThread);
                SumTask t2 = new SumTask(half+1, to ,expectedThread);
                if (half % 2 == 0) {
                    t1.fork();
                    return t2.compute() + t1.join();
                } else {
                    invokeAll(t1, t2);
                    try { return t1.get() + t2.get(); }
                    catch (Exception x) { unexpected(x); return 0L;}
                }
            }
        }
    }

    public static class NullForkJoinWorkerThreadFactory
        implements ForkJoinWorkerThreadFactory
    {
        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            return null;
        }
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; /*Thread.dumpStack();*/}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond, String msg) {if (cond) pass(); else fail(msg);}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
