/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @summary enumerate(list,n,recurse) may return 0 if the group is being destroyed,
 *          whereas it should never return a value < n. This lead to inconsistent
 *          results if ThreadGroup::enumerate is called concurrently at the same
 *          time that a child group is being destroyed. This is a race condition,
 *          and this test will not always fail without the fix, but it does fail
 *          often enough.
 * @bug 8219197
 *
 */
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Destroy {

    static final class Task implements Runnable {
        final Semaphore sem;
        final CountDownLatch count;

        public Task(Semaphore sem, CountDownLatch count) {
            this.sem = sem;
            this.count = count;
        }

        @Override
        public void run() {
            try {
                count.countDown();
                sem.acquire();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                System.out.println(Thread.currentThread().getName()
                        + " exiting");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        testDestroyChild();
    }

    public static void testDestroyChild() throws Exception {
        ThreadGroup root = new ThreadGroup("root");
        ThreadGroup parent = new ThreadGroup(root,"parent");
        ThreadGroup child1 = new ThreadGroup(parent, "child1");
        CountDownLatch count = new CountDownLatch(2);
        Semaphore sem1 = new Semaphore(1);
        Semaphore sem2 = new Semaphore(1);
        Thread t1 = new Thread(parent, new Task(sem1, count), "PT1");
        Thread t2 = new Thread(parent, new Task(sem2, count), "PT2");
        sem1.acquire();
        sem2.acquire();
        try {

            t1.start();
            t2.start();

            System.out.println("\nAwaiting parent threads...");
            count.await();
            Thread[] threads = new Thread[2];
            int nb = root.enumerate(threads, true);
            if (nb != 2) {
                throw new AssertionError("wrong number of threads: " + nb);
            }

            Thread t3 = new Thread(child1::destroy, "destroy");
            AtomicInteger nbr = new AtomicInteger();
            Thread t4 = new Thread("enumerate") {
                public void run() {
                    Thread[] threads = new Thread[42];
                    nbr.addAndGet(root.enumerate(threads, true));
                }
            };
            t4.start();
            t3.start();
            t4.join();
            t3.join();
            if (nbr.get() != nb) {
                throw new AssertionError("wrong number of threads: " + nbr.get());
            }

        } finally {
            sem1.release();
            sem2.release();
        }
        t1.join();
        t2.join();
    }
}
