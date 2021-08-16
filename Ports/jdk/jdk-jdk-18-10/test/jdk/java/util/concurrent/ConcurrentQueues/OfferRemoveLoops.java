/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6316155 6595669 6871697 6868712
 * @summary Test concurrent offer vs. remove
 * @library /test/lib
 * @run main OfferRemoveLoops 100
 * @author Martin Buchholz
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Arrays;
import java.util.Queue;
import java.util.SplittableRandom;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import jdk.test.lib.Utils;

@SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
public class OfferRemoveLoops {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    final long testDurationMillisDefault = 10_000L;
    final long testDurationMillis;

    OfferRemoveLoops(String[] args) {
        testDurationMillis = (args.length > 0) ?
            Long.valueOf(args[0]) : testDurationMillisDefault;
    }

    void checkNotContainsNull(Iterable it) {
        for (Object x : it)
            check(x != null);
    }

    void test(String[] args) throws Throwable {
        testQueue(new LinkedBlockingQueue(10));
        testQueue(new LinkedBlockingQueue());
        testQueue(new LinkedBlockingDeque(10));
        testQueue(new LinkedBlockingDeque());
        testQueue(new ArrayBlockingQueue(10));
        testQueue(new PriorityBlockingQueue(10));
        testQueue(new ConcurrentLinkedDeque());
        testQueue(new ConcurrentLinkedQueue());
        testQueue(new LinkedTransferQueue());
    }

    void testQueue(final Queue q) throws Throwable {
        System.err.println(q.getClass().getSimpleName());
        final long testDurationNanos = testDurationMillis * 1000L * 1000L;
        final long quittingTimeNanos = System.nanoTime() + testDurationNanos;
        final int maxChunkSize = 1042;
        final int maxQueueSize = 10 * maxChunkSize;
        final CountDownLatch done = new CountDownLatch(3);
        final SplittableRandom rnd = new SplittableRandom();

        // Poor man's bounded buffer; prevents unbounded queue expansion.
        final Semaphore offers = new Semaphore(maxQueueSize);

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
            protected abstract void realRun() throws Exception;
            public void run() {
                try { realRun(); } catch (Throwable t) { unexpected(t); }
            }
        }

        Thread offerer = new CheckedThread("offerer", rnd.split()) {
            protected void realRun() throws InterruptedException {
                final int chunkSize = rnd.nextInt(maxChunkSize) + 20;
                long c = 0;
                while (! quittingTime()) {
                    if (q.offer(Long.valueOf(c))) {
                        if ((++c % chunkSize) == 0) {
                            offers.acquire(chunkSize);
                        }
                    } else {
                        Thread.yield();
                    }
                }
                done.countDown();
            }};

        Thread remover = new CheckedThread("remover", rnd.split()) {
            protected void realRun() {
                final int chunkSize = rnd.nextInt(maxChunkSize) + 20;
                long c = 0;
                while (! quittingTime()) {
                    if (q.remove(Long.valueOf(c))) {
                        if ((++c % chunkSize) == 0) {
                            offers.release(chunkSize);
                        }
                    } else {
                        Thread.yield();
                    }
                }
                q.clear();
                offers.release(1<<30);  // Releases waiting offerer thread
                done.countDown();
            }};

        Thread scanner = new CheckedThread("scanner", rnd.split()) {
            protected void realRun() {
                while (! quittingTime()) {
                    switch (rnd.nextInt(3)) {
                    case 0: checkNotContainsNull(q); break;
                    case 1: q.size(); break;
                    case 2: checkNotContainsNull
                            (Arrays.asList(q.toArray(new Long[0])));
                        break;
                    }
                    Thread.yield();
                }
                done.countDown();
            }};

        if (! done.await(LONG_DELAY_MS + testDurationMillis, MILLISECONDS)) {
            for (Thread thread : new Thread[] { offerer, remover, scanner }) {
                if (thread.isAlive()) {
                    System.err.printf("Hung thread: %s%n", thread.getName());
                    failed++;
                    for (StackTraceElement e : thread.getStackTrace())
                        System.err.println(e);
                    thread.interrupt();
                }
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
        new OfferRemoveLoops(args).instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
