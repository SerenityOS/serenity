/*
 * Copyright (c) 2004, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4992438 6633113
 * @summary Checks that fairness setting is respected.
 */

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Fairness {
    private static void testFairness(boolean fair,
                                     final BlockingQueue<Integer> q)
        throws Throwable
    {
        final ReentrantLock lock = new ReentrantLock();
        final Condition ready = lock.newCondition();
        final int threadCount = 10;
        final Throwable[] badness = new Throwable[1];
        lock.lock();
        for (int i = 0; i < threadCount; i++) {
            final Integer I = i;
            Thread t = new Thread() { public void run() {
                try {
                    lock.lock();
                    ready.signal();
                    lock.unlock();
                    q.put(I);
                } catch (Throwable t) { badness[0] = t; }}};
            t.start();
            ready.await();
            // Probably unnecessary, but should be bullet-proof
            while (t.getState() == Thread.State.RUNNABLE)
                Thread.yield();
        }
        for (int i = 0; i < threadCount; i++) {
            int j = q.take();
            // Non-fair queues are lifo in our implementation
            if (fair ? j != i : j != threadCount - 1 - i)
                throw new Error(String.format("fair=%b i=%d j=%d%n",
                                              fair, i, j));
        }
        if (badness[0] != null) throw new Error(badness[0]);
    }

    public static void main(String[] args) throws Throwable {
        testFairness(false, new SynchronousQueue<Integer>());
        testFairness(false, new SynchronousQueue<Integer>(false));
        testFairness(true,  new SynchronousQueue<Integer>(true));
    }
}
