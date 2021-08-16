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
 * @bug 8073704
 * @summary Checks that once isDone() returns true,
 * get() never throws InterruptedException or TimeoutException
 * @library /test/lib
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import jdk.test.lib.Utils;

public class DoneMeansDone {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);

    public static void main(String[] args) throws Throwable {
        final int iters = 1000;
        final int nThreads = 2;
        final AtomicBoolean done = new AtomicBoolean(false);
        final AtomicReference<FutureTask<Boolean>> a = new AtomicReference<>();
        final CountDownLatch threadsStarted = new CountDownLatch(nThreads);
        final Callable<Boolean> alwaysTrue = new Callable<>() {
            public Boolean call() {
                return true;
            }};

        final Runnable observer = new Runnable() { public void run() {
            threadsStarted.countDown();
            final ThreadLocalRandom rnd = ThreadLocalRandom.current();
            try {
                for (FutureTask<Boolean> f; !done.get();) {
                    f = a.get();
                    if (f != null) {
                        do {} while (!f.isDone());
                        Thread.currentThread().interrupt();
                        if (!(rnd.nextBoolean()
                              ? f.get()
                              : f.get(-1L, TimeUnit.DAYS)))
                            throw new AssertionError();
                    }
                }
            } catch (Exception t) { throw new AssertionError(t); }
        }};

        final ArrayList<Future<?>> futures = new ArrayList<>(nThreads);
        final ExecutorService pool = Executors.newCachedThreadPool();
        for (int i = 0; i < nThreads; i++)
            futures.add(pool.submit(observer));
        threadsStarted.await();
        for (int i = 0; i < iters; i++) {
            FutureTask<Boolean> f = new FutureTask<>(alwaysTrue);
            a.set(f);
            f.run();
        }
        done.set(true);
        pool.shutdown();
        if (!pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS))
            throw new AssertionError();
        for (Future<?> future : futures)
            future.get();
    }
}
