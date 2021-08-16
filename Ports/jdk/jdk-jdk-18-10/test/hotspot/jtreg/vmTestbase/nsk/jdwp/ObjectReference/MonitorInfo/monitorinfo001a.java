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

package nsk.jdwp.ObjectReference.MonitorInfo;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class monitorinfo001a {

    // static field names for the tested objects
    public static final String OBJECT_FIELD_NAME = "object";
    public static final String MONITOR_OWNER_FIELD_NAME = "monitorOwner";
    public static final String MONITOR_WAITER_FIELD_NAMES[] =
                            { "monitorWaiter1", "monitorWaiter2" };

    // lock object to prevent thread from exit
    private static Object threadExiting = new Object();

    // scaffold objects
    private static volatile ArgumentHandler argumentHandler = null;
    private static volatile Log log = null;

    public static void main(String args[]) {
        monitorinfo001a _monitorinfo001a = new monitorinfo001a();
        System.exit(monitorinfo001.JCK_STATUS_BASE + _monitorinfo001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        // make log for debugee messages
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        // make communication pipe to debugger
        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);
        long timeout = argumentHandler.getWaitTime() * 60 * 1000;

        boolean success = true;

        // lock the object to prevent threads from exit
        synchronized (threadExiting) {

            // load tested class and create tested object and threads
            log.display("Creating object of tested class and threads");
            TestedClass.object = new TestedClass();
            TestedClass.monitorOwner = new MonitorOwnerThread("MonitorOwnerThread");
            TestedClass.monitorWaiter1 = new MonitorWaiterThread("MonitorWaiterThread1");
            TestedClass.monitorWaiter2 = new MonitorWaiterThread("MonitorWaiterThread2");

            // start MonitorWaiterThreads and wait for them become ready
            if (success) {
                if (!startMonitorWaiterThread(TestedClass.monitorWaiter1))
                    success = false;
            }
            if (success) {
                if (!startMonitorWaiterThread(TestedClass.monitorWaiter2))
                    success = false;
            }

            // start MonitorOwnerThread and wait for it becomes ready
            if (success) {
                if (!startMonitorOwnerThread(TestedClass.monitorOwner))
                    success = false;
            }

            // notify debugger if all threads started
            if (success) {
                log.display("Send signal to debugger: " + monitorinfo001.READY);
                pipe.println(monitorinfo001.READY);
            } else {
                log.complain("Send signal to debugger: " + monitorinfo001.ERROR);
                pipe.println(monitorinfo001.ERROR);
            }

            if (success) {
                // wait for signal QUIT from debugeer
                log.display("Waiting for signal from debugger: " + monitorinfo001.QUIT);
                String signal = pipe.readln();
                log.display("Received signal from debugger: " + signal);

                // check received signal
                if (signal == null || !signal.equals(monitorinfo001.QUIT)) {
                    log.complain("Unexpected communication signal from debugee: " + signal
                                + " (expected: " + monitorinfo001.QUIT + ")");
                    success = false;
                }
            }

            // allow started threads to exit
        }

        // finish monitor owner thread
        finishThread(TestedClass.monitorOwner, timeout);

        // notify monitor waiting threads
        synchronized (TestedClass.object) {
            TestedClass.object.notifyAll();
        }

        // finish monitor waiting threads
        finishThread(TestedClass.monitorWaiter2, timeout);
        finishThread(TestedClass.monitorWaiter1, timeout);

        // exit debugee
        if (!success) {
            log.display("Debugee FAILED");
            return monitorinfo001.FAILED;
        }

        log.display("Debugee PASSED");
        return monitorinfo001.PASSED;
    }

    // start MonitorWaiterThread and waits for it becomes ready
    private boolean startMonitorWaiterThread(MonitorWaiterThread thread) {
        // start thread and wait for it becomes ready
        synchronized (thread.ready) {
            thread.start();
            try {
                thread.ready.wait();
            } catch (InterruptedException e) {
                log.complain("Interruption while waiting for MonitorWaiterThread started:\n\t"
                            + e);
                return false;
            }
        }

        // ensure that thread becomes waiting for tested object
        synchronized (TestedClass.object) {
            return true;
        }
    }

    // start MonitorOwnrerThread and waits for it becomes ready
    private boolean startMonitorOwnerThread(MonitorOwnerThread thread) {
        // start thread an wait for it becomes ready
        synchronized (thread.ready) {
            thread.start();
            try {
                thread.ready.wait();
            } catch (InterruptedException e) {
                log.complain("Interruption while waiting for MonitorOwnerThread started:\n\t"
                            + e);
                return false;
            }
        }

        return true;
    }

    // interrupt thread if it is alive
    private void finishThread(Thread thread, long timeout) {
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure("Interruption while waiting for thread to finish:\n\t" + e);
        }

        if (thread.isAlive()) {
            log.display("Interrupting alive thread: " + thread.getName());
            thread.interrupt();
        }
    }

    /////////////////// Tested class and threads ////////////////

    // tested thread class
    public static class TestedClass {

        // field with the tested object value
        public static volatile TestedClass object = null;

        // field with the thread which ownes monitor of the tested object
        public static MonitorOwnerThread monitorOwner = null;

        // fields with the threads which wait for monitor of the tested object
        public static MonitorWaiterThread monitorWaiter1 = null;
        public static MonitorWaiterThread monitorWaiter2 = null;

        // internal field
        private int foo = 0;

        // constructor
        public TestedClass() {
            foo = 100;
        }

    } // TestedClass class

    // thread which will owns monitor of the tested object
    public static class MonitorOwnerThread extends Thread {

        public Object ready = new Object();

        public MonitorOwnerThread(String name) {
            super(name);
        }

        // start the thread and recursive invoke makeFrames()
        public void run() {
            log.display(getName() + " started");

            // get ownership for monitor of the tested object
            synchronized (TestedClass.object) {
                log.display(getName() + " owns monitor of the tested object");

                // notify debuggee that thread is ready
                synchronized (ready) {
                    ready.notifyAll();
                }

                // wait for debuggee releses object to finish threads
                synchronized (threadExiting) {
                    log.display(getName() + " finished");
                }

            }

        }

    } // MonitorOwnerThreadClass

    // thread which will wait for monitor of the tested object
    public static class MonitorWaiterThread extends Thread {

        public Object ready = new Object();

        public MonitorWaiterThread(String name) {
            super(name);
        }

        // start the thread and recursive invoke makeFrames()
        public void run() {
            log.display(getName() + " started");

            // get ownership for object monitor for futher waiting
            synchronized (TestedClass.object) {

                // notify debuggee that thread is ready
                synchronized (ready) {
                    ready.notifyAll();
                }

                // go to the waiting state for monitor of the tested object
                log.display(getName() + " waits for monitor of the tested object");
                try {
                    TestedClass.object.wait();
                } catch (InterruptedException e) {
                    log.display(getName() + " is interrupted while waiting for tested object");
                }
            }

            // wait for debuggee releses object to finish threads
            synchronized (threadExiting) {
                log.display(getName() + " finished");
            }

        }

    } // MonitorWaiterThread class

}
