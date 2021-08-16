/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ThreadStartEvent.thread;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugged application in the test

class thread001a {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final int THREADS_COUNT = 2;

    public static Log log;

    public static Object lock = new Object();
    public static Object waitnotify = new Object();

    private static Thread threads[] = new Thread[THREADS_COUNT];

    public static void main(String args[]) {
        thread001a _thread001a = new thread001a();
        System.exit(JCK_STATUS_BASE + _thread001a.runIt(args, System.err));
    }

    int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        // creating threads
        threads[0] = new thread001aThread("Thread1");
        threads[1] = new thread001aThread("Thread2");
        threads[1].setDaemon(true);

        // lock monitor to prevent threads from finishing after they started
        synchronized (lock) {

            synchronized (waitnotify) {
                // start all threads
                for (int i = 0; i < THREADS_COUNT; i++) {
                    threads[i].start();
                    try {
                        waitnotify.wait();
                    } catch ( Exception e ) {
                        System.err.println("TEST INCOMPLETE: caught InterruptedException while sleeping");
                        pipe.println("ERROR");
                        return FAILED;
                    }
                }
            }

            // waiting for command <QUIT> to finish threads and exit
            String command = pipe.readln();
            if (command.equals(thread001.COMMAND_QUIT)) {
                System.err.println("'quit' received");
            } else {
                System.err.println("TEST BUG: Unexpected debugger's command: " + command);
                return FAILED;
            }
        }

        for (int i = 0; i < THREADS_COUNT; i++) {
            if (threads[i].isAlive()) {
                try {
                    threads[i].join();
                } catch (InterruptedException e ) {
                    System.err.println("TEST INCOMPLETE: caught InterruptedException while waiting for aux thread join");
                    pipe.println("ERROR");
                }
            }
        }
        System.err.println("Debuggee exited");
        return PASSED;
    }
}

class thread001aThread extends Thread {
    thread001aThread (String name) {
        super(name);
    }
    public void run() {
        System.err.println("Thread started: " + this.getName());

        synchronized (thread001a.waitnotify) {
            thread001a.waitnotify.notify();
        }
        // prevent thread for early finish
        synchronized (thread001a.lock) {
        }
        System.err.println("Thread completed: "+ this.getName());
    }
}
