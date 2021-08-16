/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6853696
 * @summary ReferenceQueue#remove(timeout) should not return null before
 *          timeout is elapsed
 */

import java.lang.InterruptedException;
import java.lang.System;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * In order to demonstrate the issue we make several threads (two appears to be sufficient)
 * to block in ReferenceQueue#remove(timeout) at the same time.
 * Then, we force a reference to be enqueued by setting its referent to null and calling System.gc().
 * One of the threads gets the reference returned from the remove().
 * The other threads get null:
 * 1) with bug:  this may happen before the specified timeout is elapsed,
 * 2) without bug:  this can only happen after the timeout is fully elapsed.
 */

public class EarlyTimeout extends Thread {

    static final int THREADS_COUNT = 2;
    static final int TIMEOUT = 1000;

    static Object referent = new Object();
    static final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
    static final WeakReference<Object> weakReference = new WeakReference<Object>(referent, queue);
    static final CountDownLatch startedSignal = new CountDownLatch(THREADS_COUNT);

    long actual;
    Reference<?> reference;

    public static void main(String[] args) throws Exception {
        EarlyTimeout[] threads = new EarlyTimeout[THREADS_COUNT];
        for (int i = 0; i < THREADS_COUNT; ++i) {
            threads[i] = new EarlyTimeout();
            threads[i].start();
        }
        // The main thread waits until the threads has started and give it a chance
        // for the threads to block on the queue.remove(TIMEOUT) call
        startedSignal.await();
        Thread.sleep(TIMEOUT / 2);
        referent = null;
        System.gc();
        for (EarlyTimeout thread : threads) {
            thread.join();
        }
        if (weakReference.get() != null) {
            throw new RuntimeException("weakReference was not cleared");
        }
        int nonNullRefCount = 0;
        for (EarlyTimeout thread : threads) {
            if (thread.reference == null && thread.actual < TIMEOUT) {
                throw new RuntimeException("elapsed time " + thread.actual
                        + " is less than timeout " + TIMEOUT);
            }
            if (thread.reference != null && thread.reference == weakReference) {
                nonNullRefCount++;
            }
        }
        if (nonNullRefCount > 1) {
            throw new RuntimeException("more than one references were removed from queue");
        }
    }

    public void run() {
        try {
            startedSignal.countDown();
            long start = System.nanoTime();
            reference = queue.remove(TIMEOUT);
            actual = NANOSECONDS.toMillis(System.nanoTime() - start);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
