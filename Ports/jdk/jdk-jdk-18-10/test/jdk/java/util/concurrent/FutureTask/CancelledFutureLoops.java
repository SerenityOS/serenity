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
 * @summary Checks for responsiveness of futures to cancellation.
 * Runs under the assumption that ITERS computations require more than
 * TIMEOUT msecs to complete.
 * @library /test/lib
 * @run main/timeout=2000 CancelledFutureLoops
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.SplittableRandom;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import jdk.test.lib.Utils;

public final class CancelledFutureLoops {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    static final ExecutorService pool = Executors.newCachedThreadPool();
    static final SplittableRandom rnd = new SplittableRandom();
    static boolean print = false;
    static final int ITERS = 1000000;
    static final long TIMEOUT = 100;

    public static void main(String[] args) throws Exception {
        int maxThreads = 5;
        if (args.length > 0)
            maxThreads = Integer.parseInt(args[0]);

        print = true;

        for (int i = 2; i <= maxThreads; i += (i+1) >>> 1) {
            System.out.print("Threads: " + i);
            try {
                new FutureLoop(i, rnd.split()).test();
            }
            catch (BrokenBarrierException bb) {
                // OK; ignore
            }
            catch (ExecutionException ee) {
                // OK; ignore
            }
            Thread.sleep(TIMEOUT);
        }
        pool.shutdown();
        if (! pool.awaitTermination(6 * LONG_DELAY_MS, MILLISECONDS))
            throw new Error();
    }

    static final class FutureLoop implements Callable {
        private final int nthreads;
        private final SplittableRandom rnd;
        private final ReentrantLock lock = new ReentrantLock();
        private final LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
        private final CyclicBarrier barrier;
        private int v;
        FutureLoop(int nthreads, SplittableRandom rnd) {
            this.nthreads = nthreads;
            this.rnd = rnd;
            barrier = new CyclicBarrier(nthreads+1, timer);
            v = rnd.nextInt();
        }

        final void test() throws Exception {
            Future[] futures = new Future[nthreads];
            for (int i = 0; i < nthreads; ++i)
                futures[i] = pool.submit(this);

            barrier.await();
            Thread.sleep(TIMEOUT);
            boolean tooLate = false;
            for (int i = 1; i < nthreads; ++i) {
                if (!futures[i].cancel(true))
                    tooLate = true;
                // Unbunch some of the cancels
                if ( (i & 3) == 0)
                    Thread.sleep(1 + rnd.nextInt(5));
            }

            Object f0 = futures[0].get();
            if (!tooLate) {
                for (int i = 1; i < nthreads; ++i) {
                    if (!futures[i].isDone() || !futures[i].isCancelled())
                        throw new Error("Only one thread should complete");
                }
            }
            else
                System.out.print("(cancelled too late) ");

            long endTime = System.nanoTime();
            long time = endTime - timer.startTime;
            if (print) {
                double secs = (double)time / 1000000000.0;
                System.out.println("\t " + secs + "s run time");
            }

        }

        public final Object call() throws Exception {
            barrier.await();
            int sum = v;
            int x = 0;
            int n = ITERS;
            while (n-- > 0) {
                lock.lockInterruptibly();
                try {
                    v = x = LoopHelpers.compute1(v);
                }
                finally {
                    lock.unlock();
                }
                sum += LoopHelpers.compute2(LoopHelpers.compute2(x));
            }
            return new Integer(sum);
        }
    }

}
