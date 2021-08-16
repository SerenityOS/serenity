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
 * @summary  multiple producers and single consumer using blocking queues
 * @library /test/lib
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.test.lib.Utils;

public class MultipleProducersSingleConsumerLoops {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    static ExecutorService pool;

    public static void main(String[] args) throws Exception {
        final int maxProducers = (args.length > 0)
            ? Integer.parseInt(args[0])
            : 5;

        pool = Executors.newCachedThreadPool();
        for (int i = 1; i <= maxProducers; i += (i+1) >>> 1) {
            // Adjust iterations to limit typical single runs to <= 10 ms;
            // Notably, fair queues get fewer iters.
            // Unbounded queues can legitimately OOME if iterations
            // high enough, but we have a sufficiently low limit here.
            run(new ArrayBlockingQueue<Integer>(100), i, 300);
            run(new LinkedBlockingQueue<Integer>(100), i, 700);
            run(new LinkedBlockingDeque<Integer>(100), i , 500);
            run(new LinkedTransferQueue<Integer>(), i, 1000);
            run(new PriorityBlockingQueue<Integer>(), i, 1000);
            run(new SynchronousQueue<Integer>(), i, 500);
            run(new SynchronousQueue<Integer>(true), i, 200);
            run(new ArrayBlockingQueue<Integer>(100, true), i, 100);
        }

        pool.shutdown();
        if (! pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS))
            throw new Error();
        pool = null;
    }

    static void run(BlockingQueue<Integer> queue, int nproducers, int iters) throws Exception {
        new MultipleProducersSingleConsumerLoops(queue, nproducers, iters).run();
    }

    final BlockingQueue<Integer> queue;
    final int nproducers;
    final int iters;
    final LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
    final CyclicBarrier barrier;
    final AtomicInteger checksum = new AtomicInteger(0);
    Throwable fail;

    MultipleProducersSingleConsumerLoops(BlockingQueue<Integer> queue, int nproducers, int iters) {
        this.queue = queue;
        this.nproducers = nproducers;
        this.iters = iters;
        this.barrier = new CyclicBarrier(nproducers + 2, timer);
    }

    void run() throws Exception {
        for (int i = 0; i < nproducers; i++)
            pool.execute(new Producer());
        pool.execute(new Consumer());
        barrier.await();
        barrier.await();
        System.out.printf("%s, nproducers=%d:  %d ms%n",
                          queue.getClass().getSimpleName(), nproducers,
                          NANOSECONDS.toMillis(timer.getTime()));
        if (checksum.get() != 0) throw new AssertionError("checksum mismatch");
        if (fail != null) throw new AssertionError(fail);
    }

    abstract class CheckedRunnable implements Runnable {
        abstract void realRun() throws Throwable;
        public final void run() {
            try {
                realRun();
            } catch (Throwable t) {
                fail = t;
                t.printStackTrace();
                throw new AssertionError(t);
            }
        }
    }

    class Producer extends CheckedRunnable {
        void realRun() throws Throwable {
            barrier.await();
            int s = 0;
            int l = hashCode();
            for (int i = 0; i < iters; i++) {
                l = LoopHelpers.compute1(l);
                l = LoopHelpers.compute2(l);
                queue.put(new Integer(l));
                s += l;
            }
            checksum.getAndAdd(s);
            barrier.await();
        }
    }

    class Consumer extends CheckedRunnable {
        void realRun() throws Throwable {
            barrier.await();
            int s = 0;
            for (int i = 0; i < nproducers * iters; i++) {
                s += queue.take().intValue();
            }
            checksum.getAndAdd(-s);
            barrier.await();
        }
    }
}
