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

package nsk.jdb.trace.trace001;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class trace001a {
    public static void main(String args[]) {
       trace001a _trace001a = new trace001a();
       System.exit(trace001.JCK_STATUS_BASE + _trace001a.runIt(args, System.out));
    }

    static void lastBreak () {}

    static final String MYTHREAD  = "MyThread";
    static final int numThreads   = 2;   // number of threads.

    static Object waitnotify = new Object();

    public int runIt(String args[], PrintStream out) {

        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        int i;
        Thread holder [] = new Thread[numThreads];
        Object[] locks = new Object[numThreads];

        for (i = 0; i < numThreads ; i++) {
            locks[i]  = new Object();
            holder[i] = new MyThread(locks[i],MYTHREAD + "-" + i);
        }

        synchronized (waitnotify) {
            for (i = 0; i < numThreads ; i++) {
                holder[i].start();
                try {
                    waitnotify.wait();
                } catch (InterruptedException e) {
                    System.out.println("Main thread was interrupted while waiting for start of " + MYTHREAD + "-" + i);
                    return trace001.FAILED;
                }

                synchronized (locks[i]) {  // holder[i] must wait on its lock[i] at this moment.
                    System.out.println("Thread " + MYTHREAD + "-" + i + " is waiting");
                }
            }
        }
        lastBreak();  // a break to get thread ids and then to turn on tracing.

        // waits on all MyThreads completion
        for (i = 0; i < numThreads ; i++) {
            synchronized (locks[i]) {
                locks[i].notifyAll();
            }
            if (holder[i].isAlive() && !holder[i].interrupted()) {
                try {
                    holder[i].join();
                } catch (InterruptedException e) {
                    System.out.println("Main thread was interrupted while waiting for finish of " + MYTHREAD + "-" + i);
                    return trace001.FAILED;
                }
            }
        }

        log.display("Debuggee PASSED");
        return trace001.PASSED;
    }
}


class MyThread extends Thread {
    Object lock;
    String name;

    public MyThread (Object l, String n) {
        lock = l;
        name = n;
    }

    public void run() {
        // Concatenate strings in advance to avoid lambda calculations later
        final String ThreadFinished = "Thread finished: " + this.name;
        final String ThreadInterrupted = "Thread was interrupted: " + this.name;
        System.out.println("Thread started: " + this.name);

        synchronized (lock) {
            synchronized (trace001a.waitnotify) {
                trace001a.waitnotify.notify();
            }

            try {
                lock.wait();
                int square = func1(100);
                System.out.println(ThreadFinished);
            } catch (InterruptedException e) {
                System.out.println(ThreadInterrupted);
                e.printStackTrace();
            }
        }

        System.out.println(ThreadFinished);
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
