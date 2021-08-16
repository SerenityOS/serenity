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
package nsk.jdb.kill.kill002;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class kill002a {
    public static void main(String args[]) {
       kill002a _kill002a = new kill002a();
       System.exit(kill002.JCK_STATUS_BASE + _kill002a.runIt(args, System.out));
    }

    static void breakHere () {}

    static final String MYTHREAD         = "MyThread";
    static final int numThreads          = 5;   // number of threads
    static Object waitnotify             = new Object();
    public static volatile int notKilled = 0;
    static final String message          = "kill002a's Exception";

    static JdbArgumentHandler argumentHandler;
    static Log log;

    static final Exception[] exceptions = {
                    new InterruptedException(message),
                    new NullPointerException(message),
                    new SecurityException(message),
                    new com.sun.jdi.IncompatibleThreadStateException(message),
                    new MyException(message)
                                          };

    public int runIt(String args[], PrintStream out) {

        argumentHandler = new JdbArgumentHandler(args);
        log = new Log(out, argumentHandler);

        int i;
        Thread[] holder = new Thread[numThreads];
        Object[] locks = new Object[numThreads];

        for (i = 0; i < numThreads ; i++) {
            locks[i] = new Object();
            holder[i] = new MyThread(locks[i], MYTHREAD + "-" + i);
        }

        synchronized (waitnotify) {
            for (i = 0; i < numThreads ; i++) {
                holder[i].start();
                try {
                    waitnotify.wait();
                } catch (InterruptedException e) {
                    log.complain("Main thread was interrupted while waiting for start of " + MYTHREAD + "-" + i);
                    return kill002.FAILED;
                }

                synchronized (locks[i]) {  // holder[i] must wait on its lock[i] at this moment.
                    log.display("Thread " + MYTHREAD + "-" + i + " is waiting");
                }
            }
        }
        breakHere();  // a break to get thread ids and then to kill MyThreads.

        // waits on all MyThreads completion in case they were not killed
        for (i = 0; i < numThreads ; i++) {
            synchronized (locks[i]) {
                locks[i].notifyAll();
            }
            if (holder[i].isAlive() && !holder[i].interrupted()) {
                try {
                    holder[i].join();
                } catch (InterruptedException e) {
                    log.display("Main thread was interrupted while waiting for finish of " + MYTHREAD + "-" + i);
                }
            }
        }
        breakHere(); // a break to check if MyThreads were killed

        log.display("notKilled == " + notKilled);
        log.display("Debuggee PASSED");
        return kill002.PASSED;
    }

    static class MyException extends Exception {
        MyException (String message) {
            super(message);
        }
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
        final String ThreadFinished = "WARNING: Thread finished: " + this.name;
        String ThreadInterrupted = "WARNING: Thread was interrupted while waiting for killing: " + this.name;
        kill002a.log.display("Thread started: " + this.name);

        synchronized (lock) {
            synchronized (kill002a.waitnotify) {
                kill002a.waitnotify.notify();
            }

            try {
                lock.wait();
                kill002a.notKilled++;
                kill002a.log.display(ThreadFinished);
            } catch (Exception e) {
                kill002a.log.display(ThreadInterrupted);
                e.printStackTrace(kill002a.log.getOutStream());
            }
        }
    }
}
