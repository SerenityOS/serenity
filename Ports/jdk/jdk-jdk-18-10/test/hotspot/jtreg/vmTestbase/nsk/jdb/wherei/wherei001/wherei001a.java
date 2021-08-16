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

package nsk.jdb.wherei.wherei001;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class wherei001a {
    public static void main(String args[]) {
       wherei001a _wherei001a = new wherei001a();
       System.exit(wherei001.JCK_STATUS_BASE + _wherei001a.runIt(args, System.out));
    }

    static void lastBreak () {}

    static int numThreads = 5;   // number of threads. one lock per thread.
    static Object lock = new Object();
    static Object waitnotify = new Object();

    static JdbArgumentHandler argumentHandler;
    static Log log;

    public int runIt(String args[], PrintStream out) {

        argumentHandler = new JdbArgumentHandler(args);
        log = new Log(out, argumentHandler);

        int i;
        Thread holder [] = new Thread[numThreads];
        Lock locks[] = new Lock[numThreads];

        for (i = 0; i < numThreads ; i++) {
            locks[i] = new Lock();
            holder[i] = new MyThread(locks[i],"MyThread-" + i);
        }

        // lock monitor to prevent threads from finishing after they started
        synchronized (lock) {
            synchronized (waitnotify) {
                for (i = 0; i < numThreads ; i++) {
                    holder[i].start();
                    try {
                        waitnotify.wait();
                    } catch ( Exception e ) {
                        log.complain("TEST ERROR: caught Exception while waiting: " + e);
                        e.printStackTrace();
                    }
                }
            }
            lastBreak();
        }

        log.display("Debuggee PASSED");
        return wherei001.PASSED;
    }
}


class MyThread extends Thread {
    Lock lock;
    String name;
    // Concatenate strings in advance to avoid lambda calculations later
    final String ThreadFinished = "Thread finished: " + this.name;

    public MyThread (Lock l, String n) {
        this.lock = l;
        name = n;
    }

    public void run() {
        int square = func1(100);
        wherei001a.log.display(name + " returns " + square);
        lock.releaseLock();
    }

    public int func1(int i) {
        char x1 = 'x';
        String s1 = "hello world";
        return func2(i);
    }

    public int func2(int i) {
        char x2 = 'x';
        String s2 = "hello world";
        return func3(i);
    }

    public int func3(int i) {
        char x3 = 'x';
        String s3 = "hello world";
        return func4(i);
    }

    public int func4(int i) {
        char x4 = 'x';
        String s4 = "hello world";
        return func5(i);
    }

    public int func5(int i) {
        char x5 = 'x';
        String s5 = "hello world";
        synchronized (wherei001a.waitnotify) {
            wherei001a.waitnotify.notify();
        }
        // prevent thread for early finish
        synchronized (wherei001a.lock) {
            wherei001a.log.display(ThreadFinished);
        }
        return i*i;
    }
}

class Lock {
    boolean lockSet;

    synchronized void setLock() throws InterruptedException {
        while (lockSet == true ) {
            wait();
        }
        lockSet = true;
    }

    synchronized void releaseLock() {
        if (lockSet == true) {
            lockSet = false;
            notify();
        }
    }
}
