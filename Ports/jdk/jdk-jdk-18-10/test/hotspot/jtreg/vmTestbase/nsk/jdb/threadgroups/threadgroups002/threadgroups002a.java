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

package nsk.jdb.threadgroups.threadgroups002;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdb.*;

import java.io.*;

/* This is debuggee aplication */
public class threadgroups002a {
    public static void main(String args[]) {
        threadgroups002a _threadgroups002a = new threadgroups002a();
        System.exit(threadgroups002.JCK_STATUS_BASE + _threadgroups002a.runIt(args, System.out));
    }

    static void lastBreak () {}

    static int numThreadGroups = 3;
    static int numThreads      = 15;
    static Object waitnotify   = new Object();
    final static String THREADGROUP_NAME = "MyThreadGroup#";

    public int runIt(String args[], PrintStream out) {
        JdbArgumentHandler argumentHandler = new JdbArgumentHandler(args);
        Log log = new Log(out, argumentHandler);

        ThreadGroup tgHolder[] = new ThreadGroup[numThreadGroups];
        Thread holder [] = new Thread[numThreads];
        Lock lock = new Lock();

        for (int i = 0; i < numThreadGroups ; i++ )
            tgHolder[i] = new ThreadGroup(THREADGROUP_NAME + i);

        try {
            lock.setLock();
            int factor = numThreads / numThreadGroups;
            int k;
            for (int i = 0; i < numThreadGroups ; i++) {
                for (int j = 0; j < factor ; j++) {
                    k = i * factor + j;
                    holder[k] = new MyThread(lock, tgHolder[i], "MyThread#");
                    synchronized (waitnotify) {
                        holder[k].start();
                        waitnotify.wait();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("TEST ERROR: Caught unexpected Exception while waiting in main thread: " +
                e.getMessage());
            System.exit(threadgroups002.FAILED);
        }

        lastBreak();   // When jdb stops here, there should be 5 running MyThreads.
        lock.releaseLock();

        for (int i = 0; i < numThreads ; i++) {
            if (holder[i].isAlive()) {
                try {
                    holder[i].join(argumentHandler.getWaitTime() * 60000);
                } catch (InterruptedException e) {
                    throw new Failure("Unexpected InterruptedException catched while waiting for join of: " + holder[i]);
                }
            }
        }

        log.display("Debuggee PASSED");
        return threadgroups002.PASSED;
    }

}

class Lock {
    boolean lockSet;

    synchronized void setLock() throws InterruptedException {
        while (lockSet == true)
            wait();
        lockSet = true;
    }

    synchronized void releaseLock() {
        if (lockSet == true) {
            lockSet = false;
            notify();
        }
    }
}

class MyThread extends Thread {

    Lock lock;
    MyThread (Lock l, ThreadGroup group, String name) {
        super(group, name);
        this.lock = l;
    }

    public void run() {
        synchronized (threadgroups002a.waitnotify) {
            threadgroups002a.waitnotify.notifyAll();
        }
        try {
            lock.setLock();
        } catch(Exception e) {
            System.err.println("TEST ERROR: Caught unexpected Exception while waiting in MyThread: " +
                e.getMessage());
            System.exit(threadgroups002.FAILED);
        }
        lock.releaseLock();
    }
}
