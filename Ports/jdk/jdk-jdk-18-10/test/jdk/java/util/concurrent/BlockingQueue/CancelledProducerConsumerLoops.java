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
 * @bug 4486658
 * @summary Checks for responsiveness of blocking queues to cancellation.
 * @library /test/lib
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import jdk.test.lib.Utils;

public class CancelledProducerConsumerLoops {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    static ExecutorService pool;

    public static void main(String[] args) throws Exception {
        final int maxPairs = (args.length > 0) ? Integer.parseInt(args[0]) : 5;

        pool = Executors.newCachedThreadPool();
        for (int i = 1; i <= maxPairs; i += (i+1) >>> 1) {
            final List<BlockingQueue<Integer>> queues = new ArrayList<>();
            queues.add(new ArrayBlockingQueue<Integer>(100));
            queues.add(new LinkedBlockingQueue<Integer>(100));
            queues.add(new LinkedBlockingDeque<Integer>(100));
            queues.add(new SynchronousQueue<Integer>());
            // unbounded queue implementations are prone to OOME:
            // PriorityBlockingQueue, LinkedTransferQueue
            for (BlockingQueue<Integer> queue : queues)
                new CancelledProducerConsumerLoops(i, queue).run();
        }
        pool.shutdown();
        if (! pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS))
            throw new AssertionError("timed out");
        pool = null;
    }

    final int npairs;
    final BlockingQueue<Integer> queue;
    final CountDownLatch producersInterrupted;
    final CountDownLatch consumersInterrupted;
    final LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
    final CyclicBarrier barrier;
    volatile boolean done = false;

    CancelledProducerConsumerLoops(int npairs, BlockingQueue<Integer> queue) {
        this.npairs = npairs;
        this.queue = queue;
        this.producersInterrupted = new CountDownLatch(npairs - 1);
        this.consumersInterrupted = new CountDownLatch(npairs - 1);
        this.barrier = new CyclicBarrier(npairs * 2 + 1, timer);
    }

    void run() throws Exception {
        Future<?>[] prods = new Future<?>[npairs];
        Future<?>[] cons  = new Future<?>[npairs];

        for (int i = 0; i < npairs; i++) {
            prods[i] = pool.submit(new Producer());
            cons[i] = pool.submit(new Consumer());
        }
        barrier.await();
        Thread.sleep(ThreadLocalRandom.current().nextInt(5));

        for (int i = 1; i < npairs; i++) {
            if (!prods[i].cancel(true) ||
                !cons[i].cancel(true))
                throw new AssertionError("completed before done");
        }

        for (int i = 1; i < npairs; i++) {
            assertCancelled(prods[i]);
            assertCancelled(cons[i]);
        }

        if (!producersInterrupted.await(LONG_DELAY_MS, MILLISECONDS))
            throw new AssertionError("timed out");
        if (!consumersInterrupted.await(LONG_DELAY_MS, MILLISECONDS))
            throw new AssertionError("timed out");
        if (prods[0].isDone() || prods[0].isCancelled())
            throw new AssertionError("completed too early");

        done = true;

        if (! (prods[0].get(LONG_DELAY_MS, MILLISECONDS) instanceof Integer))
            throw new AssertionError("expected Integer");
        if (! (cons[0].get(LONG_DELAY_MS, MILLISECONDS) instanceof Integer))
            throw new AssertionError("expected Integer");
    }

    void assertCancelled(Future<?> future) throws Exception {
        if (!future.isDone())
            throw new AssertionError("not done");
        if (!future.isCancelled())
            throw new AssertionError("not cancelled");
        try {
            future.get(LONG_DELAY_MS, MILLISECONDS);
            throw new AssertionError("should throw CancellationException");
        } catch (CancellationException success) {}
    }

    class Producer implements Callable<Integer> {
        public Integer call() throws Exception {
            barrier.await();
            int sum = 0;
            try {
                int x = 4321;
                while (!done) {
                    if (Thread.interrupted()) throw new InterruptedException();
                    x = LoopHelpers.compute1(x);
                    sum += LoopHelpers.compute2(x);
                    queue.offer(new Integer(x), 1, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException cancelled) {
                producersInterrupted.countDown();
            }
            return sum;
        }
    }

    class Consumer implements Callable<Integer> {
        public Integer call() throws Exception {
            barrier.await();
            int sum = 0;
            try {
                while (!done) {
                    Integer x = queue.poll(1, TimeUnit.MILLISECONDS);
                    if (x != null)
                        sum += LoopHelpers.compute1(x.intValue());
                }
            } catch (InterruptedException cancelled) {
                consumersInterrupted.countDown();
            }
            return sum;
        }
    }
}
