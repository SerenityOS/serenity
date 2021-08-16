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
 * @summary checks to make sure a pipeline of exchangers passes data.
 * @library /test/lib
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jdk.test.lib.Utils;

public class ExchangeLoops {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    static final ExecutorService pool = Executors.newCachedThreadPool();
    static boolean print = false;

    static class Int {
        public int value;
        Int(int i) { value = i; }
    }

    public static void main(String[] args) throws Exception {
        int maxStages = 5;
        int iters = 2000;

        if (args.length > 0)
            maxStages = Integer.parseInt(args[0]);

        print = false;
        System.out.println("Warmup...");
        oneRun(2, iters);
        print = true;

        for (int i = 2; i <= maxStages; i += (i+1) >>> 1) {
            System.out.print("Threads: " + i + "\t: ");
            oneRun(i, iters);
        }
        pool.shutdown();
        if (! pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS))
            throw new Error();
   }

    static class Stage implements Runnable {
        final int iters;
        final Exchanger<Int> left;
        final Exchanger<Int> right;
        final CyclicBarrier barrier;
        volatile int result;
        Stage(Exchanger<Int> left,
              Exchanger<Int> right,
              CyclicBarrier b, int iters) {
            this.left = left;
            this.right = right;
            barrier = b;
            this.iters = iters;
        }

        public void run() {
            try {
                barrier.await();
                Int item = new Int(hashCode());
                for (int i = 0; i < iters; ++i) {
                    if (left != null) {
                        item.value = LoopHelpers.compute1(item.value);
                        Int other = left.exchange(item);
                        if (other == item || other == null)
                            throw new Error("Failed Exchange");
                        item = other;

                    }
                    if (right != null) {
                        item.value = LoopHelpers.compute2(item.value);
                        Int other = right.exchange(item);
                        if (other == item || other == null)
                            throw new Error("Failed Exchange");
                        item = other;
                    }
                }
                barrier.await();

            }
            catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
        }
    }

    static void oneRun(int nthreads, int iters) throws Exception {
        LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
        CyclicBarrier barrier = new CyclicBarrier(nthreads + 1, timer);
        Exchanger<Int> l = null;
        Exchanger<Int> r = new Exchanger<>();
        for (int i = 0; i < nthreads; ++i) {
            pool.execute(new Stage(l, r, barrier, iters));
            l = r;
            r = (i+2 < nthreads) ? new Exchanger<Int>() : null;
        }
        barrier.await();
        barrier.await();
        long time = timer.getTime();
        if (print)
            System.out.println(LoopHelpers.rightJustify(time / (iters * nthreads + iters * (nthreads-2))) + " ns per transfer");
    }

}
