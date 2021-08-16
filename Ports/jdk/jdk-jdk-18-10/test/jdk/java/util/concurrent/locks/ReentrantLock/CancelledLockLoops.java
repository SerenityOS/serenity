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
 * @bug 4486658 8040928 8140468
 * @summary tests ReentrantLock.lockInterruptibly.
 * Checks for responsiveness of locks to interrupts.
 */

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

public final class CancelledLockLoops {
    public static void main(String[] args) throws Exception {
        final int maxThreads = (args.length > 0) ? Integer.parseInt(args[0]) : 5;
        final int reps = 1;     // increase for stress testing

        for (int j = 0; j < reps; j++) {
            for (int i = 2; i <= maxThreads; i += (i+1) >>> 1) {
                new Loops(i).test();
            }
        }
    }

    static final class Loops implements Runnable {
        private final boolean print = false;
        private volatile boolean done = false;
        private int v = ThreadLocalRandom.current().nextInt();
        private int completed = 0;
        private volatile int result = 17;
        private final ReentrantLock lock = new ReentrantLock();
        private final LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
        private final CyclicBarrier barrier;
        private final int nthreads;
        private volatile Throwable fail = null;
        Loops(int nthreads) {
            this.nthreads = nthreads;
            if (print) System.out.print("Threads: " + nthreads);
            barrier = new CyclicBarrier(nthreads+1, timer);
        }

        final void test() throws Exception {
            final ThreadLocalRandom rnd = ThreadLocalRandom.current();
            Thread[] threads = new Thread[nthreads];
            for (int i = 0; i < threads.length; ++i)
                threads[i] = new Thread(this);
            for (int i = 0; i < threads.length; ++i)
                threads[i].start();
            Thread[] cancels = threads.clone();
            barrier.await();
            Thread.sleep(rnd.nextInt(5));
            for (int i = 0; i < cancels.length-2; ++i) {
                cancels[i].interrupt();
                // make sure all OK even when cancellations spaced out
                if ( (i & 3) == 0)
                    Thread.sleep(1 + rnd.nextInt(5));
            }
            done = true;
            barrier.await();
            if (print) {
                long time = timer.getTime();
                double secs = (double)time / 1000000000.0;
                System.out.println("\t " + secs + "s run time");
            }

            int c;
            lock.lock();
            try {
                c = completed;
            }
            finally {
                lock.unlock();
            }
            if (c != 2)
                throw new Error("Completed == " + c + "; expected 2");
            int r = result;
            if (r == 0) // avoid overoptimization
                System.out.println("useless result: " + r);
            if (fail != null) throw new RuntimeException(fail);
        }

        public final void run() {
            try {
                barrier.await();
                boolean interrupted = false;
                long startTime = System.nanoTime();
                int sum = v;
                int x = 0;
                while (!done || Thread.currentThread().isInterrupted()) {
                    try {
                        lock.lockInterruptibly();
                    }
                    catch (InterruptedException ie) {
                        interrupted = true;
                        if (print)
                            System.out.printf("interrupted after %d millis%n",
                                              NANOSECONDS.toMillis(System.nanoTime() - startTime));
                        break;
                    }
                    try {
                        v = x = LoopHelpers.compute1(v);
                    }
                    finally {
                        lock.unlock();
                    }
                    sum += LoopHelpers.compute2(x);
                }
                if (!interrupted) {
                    if (print)
                        System.out.printf("completed after %d millis%n",
                                          NANOSECONDS.toMillis(System.nanoTime() - startTime));
                    lock.lock();
                    try {
                        ++completed;
                    }
                    finally {
                        lock.unlock();
                    }
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
