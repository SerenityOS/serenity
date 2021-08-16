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
 * @bug 4486658 5031862 8140471
 * @summary Checks for responsiveness of locks to timeouts.
 * @library /test/lib
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import jdk.test.lib.Utils;

public final class TimeoutLockLoops {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    static final ExecutorService pool = Executors.newCachedThreadPool();
    static boolean print = false;
    static final long TIMEOUT = 10;

    public static void main(String[] args) throws Exception {
        int maxThreads = 8;
        if (args.length > 0)
            maxThreads = Integer.parseInt(args[0]);

        print = true;

        for (int i = 1; i <= maxThreads; i += (i+1) >>> 1) {
            System.out.print("Threads: " + i);
            new ReentrantLockLoop(i).test();
        }
        pool.shutdown();
        if (! pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS))
            throw new Error();
    }

    static final class ReentrantLockLoop implements Runnable {
        private int v = ThreadLocalRandom.current().nextInt();
        private volatile int result = 17;
        private final ReentrantLock lock = new ReentrantLock();
        private final LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
        private final CyclicBarrier barrier;
        private final int nthreads;
        private volatile Throwable fail = null;
        ReentrantLockLoop(int nthreads) {
            this.nthreads = nthreads;
            barrier = new CyclicBarrier(nthreads+1, timer);
        }

        final void test() throws Exception {
            for (int i = 0; i < nthreads; ++i) {
                lock.lock();
                pool.execute(this);
                lock.unlock();
            }
            barrier.await();
            Thread.sleep(ThreadLocalRandom.current().nextInt(5));
            while (!lock.tryLock()); // Jam lock
            //            lock.lock();
            barrier.await();
            if (print) {
                long time = timer.getTime();
                double secs = (double)time / 1000000000.0;
                System.out.println("\t " + secs + "s run time");
            }

            int r = result;
            if (r == 0) // avoid overoptimization
                System.out.println("useless result: " + r);
            if (fail != null) throw new RuntimeException(fail);
        }

        public final void run() {
            try {
                barrier.await();
                int sum = v;
                int x = 17;
                final ReentrantLock lock = this.lock;
                while (lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS)) {
                    try {
                        v = x = LoopHelpers.compute1(v);
                    }
                    finally {
                        lock.unlock();
                    }
                    sum += LoopHelpers.compute2(x);
                }
                barrier.await();
                result += sum;
            }
            catch (Throwable ex) {
                fail = ex;
                throw new RuntimeException(ex);
            }
        }
    }
}
