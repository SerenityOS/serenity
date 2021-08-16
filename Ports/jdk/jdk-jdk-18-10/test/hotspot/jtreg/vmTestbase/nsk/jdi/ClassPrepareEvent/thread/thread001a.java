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

package nsk.jdi.ClassPrepareEvent.thread;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import java.io.*;

// This class is the debugged application in the test

class thread001a {
    public static Object threadExitLock = new Object();

    public static void main(String args[]) {
        thread001a _thread001a = new thread001a();
        System.exit(thread001.JCK_STATUS_BASE + _thread001a.runIt(args, System.err));
    }

    int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        final Log log = new Log(out, argHandler);

//        final long threadStartTimeout = 10 * 100; // milliseconds

        // notify debugger that debugge started
        pipe.println(thread001.COMMAND_READY);

        // wait for command RUN from debugger to create another thread
        String command = pipe.readln();
        if (!command.equals(thread001.COMMAND_RUN)) {
            log.complain("TEST BUG: Debugee: unknown command: " + command);
            return thread001.FAILED;
        }

        class InnerThread extends Thread {
            public volatile boolean started = false;
            public volatile Object startedNotification = new Object();

            InnerThread (String name) {
                super(name);
            }

            public void run() {
                ClassForInnerThread a = new ClassForInnerThread();

                // start outer thread from inner thread and wait tor it started
                OuterThread outerThread = new OuterThread("outerThread");
                synchronized (outerThread.startedNotification) {
                    outerThread.start();
                    try {
                        outerThread.startedNotification.wait();
                    } catch (InterruptedException e) {
                        log.complain("Unexpected InterruptedException while waiting for outer thread started: " + e);
                        return;
                    }
                }

                // check if outer thread actually started
                if (!outerThread.started) {
                    log.complain("Outer thread NOT started from inner thread in debuggee: "
                                    + outerThread.getName());
                    outerThread.interrupt();
                    return;
                }

                // notify main thread that both threads started
                synchronized (startedNotification) {
                    started = true;
                    startedNotification.notify();
                }

                // wait for main thread releases mionitor to permint this thread to finish
                synchronized (thread001a.threadExitLock) {
                }

            }
        }

        // prevent further started thread from exit
        synchronized (threadExitLock) {

            // start an inner thread from main thread and wait for it started
            InnerThread innerThread = new InnerThread("innerThread");
            synchronized (innerThread.startedNotification) {
                innerThread.start();
                try {
                    innerThread.startedNotification.wait();
                } catch (InterruptedException e) {
                    log.complain("Unexpected InterruptedException while waiting for inner thread started: " + e);
                    return thread001.FAILED;
                }
            }

            // check if threads really started and notify debugger
            if (!innerThread.started) {
                log.complain("Inner thread NOT started from main thread in debuggee: "
                                + innerThread.getName());
                innerThread.interrupt();
                pipe.println(thread001.COMMAND_ERROR);
            } else {
                log.display("All threads started in debuggee");
                pipe.println(thread001.COMMAND_DONE);
            }

            // wait for command QUIT from debugger to release started threads and exit
            command = pipe.readln();
            if (!command.equals(thread001.COMMAND_QUIT)) {
                log.complain("TEST BUG: Debugee: unknown command: " + command);
                return thread001.FAILED;
            }

            // release monitor to permit started threads to finish
        }

        return thread001.PASSED;
    }
}

class OuterThread extends Thread {
    public volatile boolean started = false;
    public volatile Object startedNotification = new Object();

    OuterThread (String name) {
        super(name);
    }

    public void run() {
        ClassForOuterThread a = new ClassForOuterThread();

        // notify main thread that this thread started
        synchronized (startedNotification) {
            started = true;
            startedNotification.notify();
        }

        // wait for main thread releases mionitor to permint this thread to finish
        synchronized (thread001a.threadExitLock) {
        }
    }
}

class ClassForInnerThread {}

class ClassForOuterThread {}
