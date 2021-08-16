/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdb.interrupt.interrupt001;

import nsk.share.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/* This is debuggee aplication */
public class interrupt001a {
    private class MyThread extends Thread {
        final Object lock;
        int ind;
        String name;

        public MyThread (Object l, int i, String n) {
            lock = l;
            ind = i;
            name = n;
        }

        public void run() {
            synchronized (lock) {
                synchronized (waitnotify) {
                    threadRunning = true;
                    waitnotify.notify();
                }

                try {
                    flags[ind] = false;
                    while (!flags[ind]) {
                        lock.wait();
                    }
                } catch (InterruptedException e) {
                    notInterrupted.decrementAndGet();
                    synchronized (waitnotify) {
                        waitnotify.notify();
                    }
                }
            }
        }
    }

    public static void main(String args[]) {
       interrupt001a _interrupt001a = new interrupt001a();
       System.exit(interrupt001.JCK_STATUS_BASE + _interrupt001a.runIt(args, System.out));
    }

    static void breakHere () {}

    static final String MYTHREAD        = "MyThread";
    static final int numThreads         = 5;   // number of threads
    static volatile boolean allWorkersAreWaiting = false;

    private final Object waitnotify            = new Object();
    private volatile boolean threadRunning;
    private volatile boolean[] flags     = new boolean[numThreads];

    private JdbArgumentHandler argumentHandler;
    private Log log;

    public static final AtomicInteger notInterrupted = new AtomicInteger(numThreads);

    public int runIt(String args[], PrintStream out) {

        argumentHandler = new JdbArgumentHandler(args);
        log = new Log(out, argumentHandler);

        int i;
        Thread[] holder = new Thread[numThreads];
        Object[] locks = new Object[numThreads];

        for (i = 0; i < numThreads ; i++) {
            locks[i] = new Object();
            holder[i] = new MyThread(locks[i], i, MYTHREAD + "-" + i);
        }

        synchronized (waitnotify) {
            for (i = 0; i < numThreads ; i++) {
                holder[i].start();
                try {
                     threadRunning = false;
                     while (!threadRunning) {
                         waitnotify.wait();
                     }
                } catch (InterruptedException e) {
                    log.complain("Main thread was interrupted while waiting for start of " + MYTHREAD + "-" + i);
                    return interrupt001.FAILED;
                }
            }
        }

        // allWorkersAreWaiting will be set to true by the debugger thread
        // when it sees all of the worker treads are waiting.
        do {
            breakHere();  // a break to get thread ids and then to interrupt MyThreads.
        } while (!allWorkersAreWaiting);

        long waitTime = argumentHandler.getWaitTime() * 60 * 1000;
        long startTime = System.currentTimeMillis();
        while (notInterrupted.get() > 0 && System.currentTimeMillis() - startTime <= waitTime) {
            synchronized (waitnotify) {
                try {
                    waitnotify.wait(waitTime);
                } catch (InterruptedException e) {
                    log.display("Main thread was interrupted while waiting");
                }
            }
        }
        for (i = 0; i < numThreads ; i++) {
            if (holder[i].isAlive()) {
                synchronized (locks[i]) {
                    flags[i] = true;
                    locks[i].notifyAll();
                }
            }
        }
        breakHere(); // a break to check if MyThreads were interrupted

        log.display("Debuggee PASSED");
        return interrupt001.PASSED;
    }
}
