/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @summary SynchronizerDeadlock creates threads that are deadlocked
 *          waiting for JSR-166 synchronizers.
 * @author  Mandy Chung
 * @build Barrier
 */

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class SynchronizerDeadlock {

    private Lock a = new ReentrantLock();
    private Lock b = new ReentrantLock();
    private Lock c = new ReentrantLock();
    private final int EXPECTED_THREADS = 3;
    private Thread[] dThreads = new Thread[EXPECTED_THREADS];
    private Barrier go = new Barrier(1);
    private Barrier barr = new Barrier(EXPECTED_THREADS);

    public SynchronizerDeadlock() {
        dThreads[0] = new DeadlockingThread("Deadlock-Thread-1", a, b);
        dThreads[1] = new DeadlockingThread("Deadlock-Thread-2", b, c);
        dThreads[2] = new DeadlockingThread("Deadlock-Thread-3", c, a);

        // make them daemon threads so that the test will exit
        for (int i = 0; i < EXPECTED_THREADS; i++) {
            dThreads[i].setDaemon(true);
            dThreads[i].start();
        }
    }

    void goDeadlock() {
        // Wait until all threads have started
        barr.await();

        // reset for later signals
        barr.set(EXPECTED_THREADS);

        while (go.getWaiterCount() != EXPECTED_THREADS) {
            synchronized(this) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        // sleep a little so that all threads are blocked before notified.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        go.signal();

    }

    void waitUntilDeadlock() {
        barr.await();

        for (int i=0; i < 100; i++) {
            // sleep a little while to wait until threads are blocked.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
            boolean retry = false;
            for (Thread t: dThreads) {
                if (t.getState() == Thread.State.RUNNABLE) {
                    retry = true;
                    break;
                }
            }
            if (!retry) {
                break;
            }
        }
    }

    private class DeadlockingThread extends Thread {
        private final Lock lock1;
        private final Lock lock2;

        DeadlockingThread(String name, Lock lock1, Lock lock2) {
            super(name);
            this.lock1 = lock1;
            this.lock2 = lock2;
        }
        public void run() {
            f();
        }
        private void f() {
            lock1.lock();
            try {
                barr.signal();
                go.await();
                g();
            } finally {
                lock1.unlock();
            }
        }
        private void g() {
            barr.signal();
            lock2.lock();
            throw new RuntimeException("should not reach here.");
        }
    }

    void checkResult(long[] threads) {
        if (threads.length != EXPECTED_THREADS) {
            ThreadDump.threadDump();
            throw new RuntimeException("Expected to have " +
                EXPECTED_THREADS + " to be in the deadlock list");
        }
        boolean[] found = new boolean[EXPECTED_THREADS];
        for (int i = 0; i < threads.length; i++) {
            for (int j = 0; j < dThreads.length; j++) {
                if (dThreads[j].getId() == threads[i]) {
                    found[j] = true;
                }
            }
        }
        boolean ok = true;
        for (int j = 0; j < found.length; j++) {
            ok = ok && found[j];
        }

        if (!ok) {
            System.out.print("Returned result is [");
            for (int j = 0; j < threads.length; j++) {
                System.out.print(threads[j] + " ");
            }
            System.out.println("]");

            System.out.print("Expected result is [");
            for (int j = 0; j < threads.length; j++) {
                System.out.print(dThreads[j] + " ");
            }
            System.out.println("]");
            throw new RuntimeException("Unexpected result returned " +
                " by findMonitorDeadlockedThreads method.");
        }
    }
}
