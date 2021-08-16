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

package nsk.jdb.untrace.untrace001;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class untrace001a {

    public static void main(String args[]) {
       untrace001a _untrace001a = new untrace001a();
       System.exit(untrace001.JCK_STATUS_BASE + _untrace001a.runIt(args, System.out));
    }

    static void breakHere() {}

    static final String MYTHREAD  = "MyThread";
    static final int numThreads   = 1;   // number of threads.

    static JdbArgumentHandler argumentHandler;
    static Log log;

    static Object mainThreadLock0 = new Object();
    static Object mainThreadLock1 = new Object();
    static volatile boolean mainThreadRunning;
    static volatile boolean[] flags = new boolean[numThreads];

    public int runIt(String args[], PrintStream out) {

        argumentHandler = new JdbArgumentHandler(args);
        log = new Log(out, argumentHandler);

        int i;
        Thread holder [] = new Thread[numThreads];
        Object[] locks = new Object[numThreads];

        for (i = 0; i < numThreads ; i++) {
            locks[i]  = new Object();
            holder[i] = new MyThread(locks[i], i, MYTHREAD + "-" + i);
        }

        synchronized (mainThreadLock0) {
            synchronized (mainThreadLock1) {
                for (i = 0; i < numThreads ; i++) {
                    holder[i].start();
                    try {
                         mainThreadRunning = false;
                         while (!mainThreadRunning) {
                             mainThreadLock1.wait();
                         }
                    } catch (InterruptedException e) {
                        log.complain("Main thread was interrupted while waiting for start of " + MYTHREAD + "-" + i);
                        return untrace001.FAILED;
                    }

                    synchronized (locks[i]) {  // holder[i] must wait on its lock[i] at this moment.
                        log.display("Thread " + MYTHREAD + "-" + i + " is waiting");
                    }
                }
            }
            breakHere();  // a break to get thread ids and then to turn on tracing.

            // waits on all MyThreads completion
            for (i = 0; i < numThreads ; i++) {
                synchronized (locks[i]) {
                    flags[i] = true;
                    locks[i].notifyAll();
                }

                try {
                     mainThreadRunning = false;
                     while (!mainThreadRunning) {
                         mainThreadLock0.wait();
                     }
                } catch (InterruptedException e) {
                    log.complain("Main thread was interrupted while waiting for " + MYTHREAD + "-" + i);
                    return untrace001.FAILED;
                }

                breakHere();  // a break to turn off tracing.

                synchronized (locks[i]) {
                    flags[i] = true;
                    locks[i].notifyAll();
                }

                if (holder[i].isAlive() && !holder[i].interrupted()) {
                    try {
                        holder[i].join();
                    } catch (InterruptedException e) {
                        log.complain("Main thread was interrupted while waiting for finish of " + MYTHREAD + "-" + i);
                        return untrace001.FAILED;
                    }
                }
            }
        }

        log.display("Debuggee PASSED");
        return untrace001.PASSED;
    }
}


class MyThread extends Thread {
    Object lock;
    int ind;
    String name;

    public MyThread (Object l, int i, String n) {
        lock = l;
        ind = i;
        name = n;
    }

    public void run() {
        // Concatenate strings in advance to avoid lambda calculations later
        final String ThreadFinished = "Thread finished: " + this.name;
        final String ThreadInterrupted = "Thread was interrupted: " + this.name;
        untrace001a.log.display("Thread started: " + this.name);

        synchronized (lock) {
            synchronized (untrace001a.mainThreadLock1) {
                untrace001a.mainThreadRunning = true;
                untrace001a.mainThreadLock1.notify();
            }

            try {
                untrace001a.flags[ind] = false;
                while (!untrace001a.flags[ind]) {
                    lock.wait();
                }
                int square = func1(2);

                synchronized (untrace001a.mainThreadLock0) {
                    untrace001a.mainThreadRunning = true;
                    untrace001a.mainThreadLock0.notify();
                }

                untrace001a.flags[ind] = false;
                while (!untrace001a.flags[ind]) {
                    lock.wait();
                }
                square = func1(3);

                untrace001a.log.display(ThreadFinished);
            } catch (InterruptedException e) {
                untrace001a.log.display(ThreadInterrupted);
            }
        }

        untrace001a.log.display(ThreadFinished);
    }

    public int func1(int i) {
        return func2(i);
    }

    public int func2(int i) {
        return func3(i);
    }

    public int func3(int i) {
        return i*i;
    }
}
