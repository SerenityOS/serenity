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

package nsk.jdb.suspend.suspend001;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class suspend001a {
    static suspend001a _suspend001a = new suspend001a();

    static JdbArgumentHandler argumentHandler;
    static Log log;

    public static void main(String args[]) {
       System.exit(suspend001.JCK_STATUS_BASE + _suspend001a.runIt(args, System.out));
    }

    static void breakHere () {}

    static Object lock                   = new Object();
    static Object waitnotify             = new Object();
    public static volatile int notSuspended = 0;

    public int runIt(String args[], PrintStream out) {
        argumentHandler = new JdbArgumentHandler(args);
        log = new Log(out, argumentHandler);

        Thread suspended = new Suspended("Suspended");
        Thread myThread = new MyThread("MyThread");

        // lock monitor to prevent threads from finishing after they started
        synchronized (lock) {
            synchronized (waitnotify) {
                    suspended.start();
                    try {
                        waitnotify.wait();
                    } catch (InterruptedException e) {
                        log.complain("Main thread was interrupted while waiting for start of Suspended thread");
                        return suspend001.FAILED;
                    }

                    myThread.start();
                    try {
                        waitnotify.wait();
                    } catch (InterruptedException e) {
                        log.complain("Main thread was interrupted while waiting for start of MyThread thread");
                        return suspend001.FAILED;
                    }
            }
            breakHere();  // a break to get thread ids and then to suspend Suspended thread.
        }

        // wait for MyThread completion
        if (myThread.isAlive()) {
            try {
                myThread.join();
            } catch (InterruptedException e) {
                log.display("Main thread was interrupted while waiting for finish of MyThread thread");
            }
        }

        // give 3 seconds for suspended thread to finish (if it is not really suspended).
        try {
            Thread.currentThread().sleep(3 * 1000);
        } catch (InterruptedException e) {
            log.display("Main thread was interrupted while sleeping");
        }

        breakHere(); // a break to check if Suspended was suspended

        log.display("notSuspended == " + notSuspended);
        log.display("Debuggee PASSED");
        return suspend001.PASSED;
    }

    public static int getResult() {
        return notSuspended;
    }
}

class Suspended extends Thread {
    String name;

    public Suspended (String n) {
        name = n;
    }

    public void run() {
        // Concatenate strings in advance to avoid lambda calculations later
        final String ThreadFinished = "Thread finished: " + this.name;
        suspend001a.log.display("Thread started: " + this.name);

        synchronized (suspend001a.waitnotify) {
            suspend001a.waitnotify.notify();
        }
        // prevent thread from early finish
        synchronized (suspend001a.lock) {}

        suspend001a.notSuspended++;

        suspend001a.log.display(ThreadFinished);
    }
}

class MyThread extends Thread {
    String name;

    public MyThread (String n) {
        name = n;
    }

    public void run() {
        // Concatenate strings in advance to avoid lambda calculations later
        final String ThreadFinished = "Thread finished: " + this.name;
        suspend001a.log.display("Thread started: " + this.name);

        synchronized (suspend001a.waitnotify) {
            suspend001a.waitnotify.notify();
        }
        // prevent thread from early finish
        synchronized (suspend001a.lock) {}

        suspend001a.notSuspended++;

        suspend001a.log.display(ThreadFinished);
    }
}
