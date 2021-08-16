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

package nsk.jdb.kill.kill001;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;
import java.util.*;

/* This is debuggee aplication */
public class kill001a {
    public static void main(String args[]) {
       kill001a _kill001a = new kill001a();
       System.exit(kill001.JCK_STATUS_BASE + _kill001a.runIt(args, System.out));
    }

    static void breakHere () {}

    static final String MYTHREAD         = "MyThread";
    static final int numThreads          = 5;   // number of threads. one lock per thread.
    static Object lock                   = new Object();
    static Object waitnotify             = new Object();
    public static volatile int notKilled = 0;
    static final String message          = "kill001a's Exception";
    static int waitTime;

    static JdbArgumentHandler argumentHandler;
    static Log log;

    static final Throwable[] exceptions = {
                    new ThreadDeath(),
                    new NullPointerException(message),
                    new SecurityException(message),
                    new com.sun.jdi.IncompatibleThreadStateException(message),
                    new MyException(message)
                                          };


    public int runIt(String args[], PrintStream out) {

        argumentHandler = new JdbArgumentHandler(args);
        log = new Log(out, argumentHandler);
        waitTime = argumentHandler.getWaitTime() * 60 * 1000;

        int i;
        Thread holder [] = new Thread[numThreads];

        for (i = 0; i < numThreads ; i++) {
            holder[i] = new MyThread(MYTHREAD + "-" + i);
        }

        // lock monitor to prevent threads from finishing after they started
        synchronized (lock) {
            synchronized (waitnotify) {
                for (i = 0; i < numThreads ; i++) {
                    holder[i].start();
                    try {
                        waitnotify.wait();
                    } catch (InterruptedException e) {
                        log.complain("Main thread was interrupted while waiting for start of " + MYTHREAD + "-" + i);
                        return kill001.FAILED;
                    }
                }
            }
            breakHere();  // a break to get thread ids and then to kill MyThreads.
        }

        // wait during watTime until all MyThreads will be killed
        long oldTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - oldTime) <= kill001a.waitTime) {
            boolean waited = false;
            for (i = 0; i < numThreads ; i++) {
                if (holder[i].isAlive()) {
                    waited = true;
                    try {
                        synchronized(waitnotify) {
                            waitnotify.wait(1000);
                        }
                    } catch (InterruptedException e) {
                        log.display("Main thread was interrupted while waiting for killing of " + MYTHREAD + "-" + i);
                    }
                }
            }
            if (!waited) {
                break;
            }
        }
        breakHere(); // a break to check if MyThreads were killed
        log.display("notKilled == " + notKilled);

        for (i = 0; i < numThreads ; i++) {
            if (holder[i].isAlive()) {
                log.display("Debuggee FAILED");
                return kill001.FAILED;
            }
        }

        log.display("Debuggee PASSED");
        return kill001.PASSED;
    }

    static class MyException extends Exception {
        MyException (String message) {
            super(message);
        }
    }
}


class MyThread extends Thread {
    String name;

    public MyThread (String n) {
        name = n;
    }

    public void run() {
        // Concatenate strings in advance to avoid lambda calculations later
        String ThreadFinished = "WARNING: Thread finished: " + this.name;
        String ThreadInterrupted = "WARNING: Thread was interrupted while waiting for killing: " + this.name;
        kill001a.log.display("Thread started: " + this.name);

        synchronized (kill001a.waitnotify) {
            kill001a.waitnotify.notify();
        }
        // prevent thread from early finish
        synchronized (kill001a.lock) {}

        // sleep during waitTime to give debugger a chance to kill debugee's thread
        try {
            Thread.currentThread().sleep(kill001a.waitTime);
        } catch (InterruptedException e) {
            kill001a.log.display(ThreadInterrupted);
            e.printStackTrace(kill001a.log.getOutStream());
        }

        kill001a.notKilled++;
        kill001a.log.display(ThreadFinished);
    }
}
