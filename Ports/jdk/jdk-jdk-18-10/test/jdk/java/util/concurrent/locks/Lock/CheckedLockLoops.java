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
 * @summary basic safety and liveness of ReentrantLocks, and other locks based on them
 * @library /test/lib
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import jdk.test.lib.Utils;

public final class CheckedLockLoops {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    static ExecutorService pool;

    public static void main(String[] args) throws Exception {
        final int maxThreads = (args.length > 0)
            ? Integer.parseInt(args[0])
            : 5;
        int iters = 3000;

        pool = Executors.newCachedThreadPool();
        for (int i = 1; i <= maxThreads; i += (i+1) >>> 1) {
            oneTest(i, iters / i);
        }
        pool.shutdown();
        if (! pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS))
            throw new Error();
        pool = null;
    }

    static void oneTest(int nthreads, int iters) throws Exception {
        System.out.println("Threads: " + nthreads);
        int v = ThreadLocalRandom.current().nextInt();
        System.out.print("builtin lock          ");
        new BuiltinLockLoop().test(v, nthreads, iters);

        System.out.print("ReentrantLock         ");
        new ReentrantLockLoop().test(v, nthreads, iters);

        System.out.print("Mutex                 ");
        new MutexLoop().test(v, nthreads, iters);

        System.out.print("ReentrantWriteLock    ");
        new ReentrantWriteLockLoop().test(v, nthreads, iters);

        System.out.print("ReentrantReadWriteLock");
        new ReentrantReadWriteLockLoop().test(v, nthreads, iters);

        System.out.print("Semaphore             ");
        new SemaphoreLoop().test(v, nthreads, iters);

        System.out.print("fair Semaphore        ");
        new FairSemaphoreLoop().test(v, nthreads, iters);

        System.out.print("FairReentrantLock     ");
        new FairReentrantLockLoop().test(v, nthreads, iters);

        System.out.print("FairRWriteLock         ");
        new FairReentrantWriteLockLoop().test(v, nthreads, iters);

        System.out.print("FairRReadWriteLock     ");
        new FairReentrantReadWriteLockLoop().test(v, nthreads, iters);
    }

    abstract static class LockLoop implements Runnable {
        int value;
        int checkValue;
        int iters;
        volatile int result;
        final LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
        CyclicBarrier barrier;

        final int setValue(int v) {
            checkValue = v ^ 0x55555555;
            value = v;
            return v;
        }

        final int getValue() {
            int v = value;
            if (checkValue != ~(v ^ 0xAAAAAAAA))
                throw new Error("lock protection failure");
            return v;
        }

        final void test(int initialValue, int nthreads, int iters) throws Exception {
            setValue(initialValue);
            this.iters = iters;
            barrier = new CyclicBarrier(nthreads+1, timer);
            for (int i = 0; i < nthreads; ++i)
                pool.execute(this);
            barrier.await();
            barrier.await();
            long time = timer.getTime();
            long tpi = time / (iters * nthreads);
            System.out.print("\t" + LoopHelpers.rightJustify(tpi) + " ns per update");
            //                double secs = (double)time / 1000000000.0;
            //                System.out.print("\t " + secs + "s run time");
            System.out.println();

            if (result == 0) // avoid overoptimization
                System.out.println("useless result: " + result);
        }
        abstract int loop(int n);
        public final void run() {
            try {
                barrier.await();
                result += loop(iters);
                barrier.await();
            }
            catch (Exception ex) {
                return;
            }
        }

    }

    private static class BuiltinLockLoop extends LockLoop {
        final int loop(int n) {
            int sum = 0;
            int x = 0;
            while (n-- > 0) {
                synchronized (this) {
                    x = setValue(LoopHelpers.compute1(getValue()));
                }
                sum += LoopHelpers.compute2(x);
            }
            return sum;
        }
    }

    private static class ReentrantLockLoop extends LockLoop {
        private final ReentrantLock lock = new ReentrantLock();
        final int loop(int n) {
            final ReentrantLock lock = this.lock;
            int sum = 0;
            int x = 0;
            while (n-- > 0) {
                lock.lock();
                try {
                    x = setValue(LoopHelpers.compute1(getValue()));
                }
                finally {
                    lock.unlock();
                }
                sum += LoopHelpers.compute2(x);
            }
            return sum;
        }
    }

    private static class MutexLoop extends LockLoop {
        private final Mutex lock = new Mutex();
        final int loop(int n) {
            final Mutex lock = this.lock;
            int sum = 0;
            int x = 0;
            while (n-- > 0) {
                lock.lock();
                try {
                    x = setValue(LoopHelpers.compute1(getValue()));
                }
                finally {
                    lock.unlock();
                }
                sum += LoopHelpers.compute2(x);
            }
            return sum;
        }
    }

    private static class FairReentrantLockLoop extends LockLoop {
        private final ReentrantLock lock = new ReentrantLock(true);
        final int loop(int n) {
            final ReentrantLock lock = this.lock;
            int sum = 0;
            int x = 0;
            while (n-- > 0) {
                lock.lock();
                try {
                    x = setValue(LoopHelpers.compute1(getValue()));
                }
                finally {
                    lock.unlock();
                }
                sum += LoopHelpers.compute2(x);
            }
            return sum;
        }
    }

    private static class ReentrantWriteLockLoop extends LockLoop {
        private final Lock lock = new ReentrantReadWriteLock().writeLock();
        final int loop(int n) {
            final Lock lock = this.lock;
            int sum = 0;
            int x = 0;
            while (n-- > 0) {
                lock.lock();
                try {
                    x = setValue(LoopHelpers.compute1(getValue()));
                }
                finally {
                    lock.unlock();
                }
                sum += LoopHelpers.compute2(x);
            }
            return sum;
        }
    }

    private static class FairReentrantWriteLockLoop extends LockLoop {
        final Lock lock = new ReentrantReadWriteLock(true).writeLock();
        final int loop(int n) {
            final Lock lock = this.lock;
            int sum = 0;
            int x = 0;
            while (n-- > 0) {
                lock.lock();
                try {
                    x = setValue(LoopHelpers.compute1(getValue()));
                }
                finally {
                    lock.unlock();
                }
                sum += LoopHelpers.compute2(x);
            }
            return sum;
        }
    }

    private static class SemaphoreLoop extends LockLoop {
        private final Semaphore sem = new Semaphore(1, false);
        final int loop(int n) {
            final Semaphore sem = this.sem;
            int sum = 0;
            int x = 0;
            while (n-- > 0) {
                sem.acquireUninterruptibly();
                try {
                    x = setValue(LoopHelpers.compute1(getValue()));
                }
                finally {
                    sem.release();
                }
                sum += LoopHelpers.compute2(x);
            }
            return sum;
        }
    }

    private static class FairSemaphoreLoop extends LockLoop {
        private final Semaphore sem = new Semaphore(1, true);
        final int loop(int n) {
            final Semaphore sem = this.sem;
            int sum = 0;
            int x = 0;
            while (n-- > 0) {
                sem.acquireUninterruptibly();
                try {
                    x = setValue(LoopHelpers.compute1(getValue()));
                }
                finally {
                    sem.release();
                }
                sum += LoopHelpers.compute2(x);
            }
            return sum;
        }
    }

    private static class ReentrantReadWriteLockLoop extends LockLoop {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        final int loop(int n) {
            final Lock rlock = lock.readLock();
            final Lock wlock = lock.writeLock();
            int sum = 0;
            int x = 0;
            while (n-- > 0) {
                if ((n & 16) != 0) {
                    rlock.lock();
                    try {
                        x = LoopHelpers.compute1(getValue());
                        x = LoopHelpers.compute2(x);
                    }
                    finally {
                        rlock.unlock();
                    }
                }
                else {
                    wlock.lock();
                    try {
                        setValue(x);
                    }
                    finally {
                        wlock.unlock();
                    }
                    sum += LoopHelpers.compute2(x);
                }
            }
            return sum;
        }
    }

    private static class FairReentrantReadWriteLockLoop extends LockLoop {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
        final int loop(int n) {
            final Lock rlock = lock.readLock();
            final Lock wlock = lock.writeLock();
            int sum = 0;
            int x = 0;
            while (n-- > 0) {
                if ((n & 16) != 0) {
                    rlock.lock();
                    try {
                        x = LoopHelpers.compute1(getValue());
                        x = LoopHelpers.compute2(x);
                    }
                    finally {
                        rlock.unlock();
                    }
                }
                else {
                    wlock.lock();
                    try {
                        setValue(x);
                    }
                    finally {
                        wlock.unlock();
                    }
                    sum += LoopHelpers.compute2(x);
                }
            }
            return sum;
        }
    }
}
