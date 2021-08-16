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
 * @bug 4486658 6785442
 * @summary Checks that a set of threads can repeatedly get and modify items
 * @library /test/lib
 * @run main ConcurrentQueueLoops 8 123456
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.test.lib.Utils;

public class ConcurrentQueueLoops {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    ExecutorService pool;
    AtomicInteger totalItems;
    boolean print;

    // Suitable for benchmarking.  Overridden by args[0] for testing.
    int maxStages = 20;

    // Suitable for benchmarking.  Overridden by args[1] for testing.
    int items = 1024 * 1024;

    Collection<Queue<Integer>> concurrentQueues() {
        List<Queue<Integer>> queues = new ArrayList<>();
        queues.add(new ConcurrentLinkedDeque<Integer>());
        queues.add(new ConcurrentLinkedQueue<Integer>());
        queues.add(new ArrayBlockingQueue<Integer>(items, false));
        //queues.add(new ArrayBlockingQueue<Integer>(count, true));
        queues.add(new LinkedBlockingQueue<Integer>());
        queues.add(new LinkedBlockingDeque<Integer>());
        queues.add(new LinkedTransferQueue<Integer>());

        // Following additional implementations are available from:
        // http://gee.cs.oswego.edu/dl/concurrency-interest/index.html
        // queues.add(new SynchronizedLinkedListQueue<Integer>());

        // Avoid "first fast, second slow" benchmark effect.
        Collections.shuffle(queues);
        return queues;
    }

    void test(String[] args) throws Throwable {
        if (args.length > 0)
            maxStages = Integer.parseInt(args[0]);
        if (args.length > 1)
            items = Integer.parseInt(args[1]);

        for (Queue<Integer> queue : concurrentQueues())
            test(queue);
    }

    void test(final Queue<Integer> q) throws Throwable {
        System.out.println(q.getClass().getSimpleName());
        pool = Executors.newCachedThreadPool();
        print = false;

        print = false;
        System.out.println("Warmup...");
        oneRun(1, items, q);
        oneRun(3, items, q);
        print = true;

        for (int i = 1; i <= maxStages; i += (i+1) >>> 1) {
            oneRun(i, items, q);
        }
        pool.shutdown();
        check(pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
   }

    class Stage implements Callable<Integer> {
        final Queue<Integer> queue;
        final CyclicBarrier barrier;
        int items;
        Stage(Queue<Integer> q, CyclicBarrier b, int items) {
            queue = q;
            barrier = b;
            this.items = items;
        }

        public Integer call() {
            // Repeatedly take something from queue if possible,
            // transform it, and put back in.
            try {
                barrier.await();
                int l = 4321;
                int takes = 0;
                for (;;) {
                    Integer item = queue.poll();
                    if (item != null) {
                        ++takes;
                        l = LoopHelpers.compute2(item.intValue());
                    }
                    else if (takes != 0) {
                        totalItems.getAndAdd(-takes);
                        takes = 0;
                    }
                    else if (totalItems.get() <= 0)
                        break;
                    l = LoopHelpers.compute1(l);
                    if (items > 0) {
                        --items;
                        queue.offer(new Integer(l));
                    }
                    else if ( (l & (3 << 5)) == 0) // spinwait
                        Thread.sleep(1);
                }
                return new Integer(l);
            }
            catch (Throwable t) { unexpected(t); return null; }
        }
    }

    void oneRun(int n, int items, final Queue<Integer> q) throws Exception {
        LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
        CyclicBarrier barrier = new CyclicBarrier(n + 1, timer);
        totalItems = new AtomicInteger(n * items);
        ArrayList<Future<Integer>> results = new ArrayList<>(n);
        for (int i = 0; i < n; ++i)
            results.add(pool.submit(new Stage(q, barrier, items)));

        if (print)
            System.out.print("Threads: " + n + "\t:");
        barrier.await();
        int total = 0;
        for (int i = 0; i < n; ++i) {
            Future<Integer> f = results.get(i);
            Integer r = f.get();
            total += r.intValue();
        }
        long endTime = System.nanoTime();
        long time = endTime - timer.startTime;
        if (print)
            System.out.println(LoopHelpers.rightJustify(time / (items * n)) + " ns per item");
        if (total == 0) // avoid overoptimization
            System.out.println("useless result: " + total);
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
        new ConcurrentQueueLoops().instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
