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
 * Written by Martin Buchholz with assistance from members of JCP
 * JSR-166 Expert Group and released to the public domain, as
 * explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 8074773
 * @summary Stress test looks for lost unparks
 */

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.SplittableRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.LockSupport;

public final class ParkLoops {
    static final int THREADS = 4;
    static final int ITERS = 30_000;

    static class Parker implements Runnable {
        static {
            // Reduce the risk of rare disastrous classloading in first call to
            // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
            Class<?> ensureLoaded = LockSupport.class;
        }

        private final AtomicReferenceArray<Thread> threads;
        private final CountDownLatch done;
        private final SplittableRandom rnd;

        Parker(AtomicReferenceArray<Thread> threads,
                 CountDownLatch done,
                 SplittableRandom rnd) {
            this.threads = threads; this.done = done; this.rnd = rnd;
        }

        public void run() {
            final Thread current = Thread.currentThread();
            for (int k = ITERS, j; k > 0; k--) {
                do {
                    j = rnd.nextInt(THREADS);
                } while (!threads.compareAndSet(j, null, current));
                do {                    // handle spurious wakeups
                    LockSupport.park();
                } while (threads.get(j) == current);
            }
            done.countDown();
        }
    }

    static class Unparker implements Runnable {
        static {
            // Reduce the risk of rare disastrous classloading in first call to
            // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
            Class<?> ensureLoaded = LockSupport.class;
        }

        private final AtomicReferenceArray<Thread> threads;
        private final CountDownLatch done;
        private final SplittableRandom rnd;

        Unparker(AtomicReferenceArray<Thread> threads,
                 CountDownLatch done,
                 SplittableRandom rnd) {
            this.threads = threads; this.done = done; this.rnd = rnd;
        }

        public void run() {
            for (int n = 0; (n++ & 0xff) != 0 || done.getCount() > 0;) {
                int j = rnd.nextInt(THREADS);
                Thread parker = threads.get(j);
                if (parker != null &&
                    threads.compareAndSet(j, parker, null)) {
                    LockSupport.unpark(parker);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        final SplittableRandom rnd = new SplittableRandom();
        final ExecutorService pool = Executors.newCachedThreadPool();
        final AtomicReferenceArray<Thread> threads
            = new AtomicReferenceArray<>(THREADS);
        final CountDownLatch done = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            pool.submit(new Parker(threads, done, rnd.split()));
            pool.submit(new Unparker(threads, done, rnd.split()));
        }
        // Let test harness handle timeout
        done.await();
        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, SECONDS);
    }
}
