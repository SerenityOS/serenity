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

package nsk.jdwp.ThreadReference.OwnedMonitors;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class ownmonitors001a {

    // name for the tested thread
    public static final String THREAD_NAME = "TestedThreadName";
    public static final String THREAD_FIELD_NAME = "thread";
    public static final String OWNED_MONITOR_FIELD_NAME = "ownedMonitor";
    public static final String NOT_OWNED_MONITOR_FIELD_NAME = "notOwnedMonitor";

    // notification object to notify debuggee that thread is ready
    private static Object threadReady = new Object();
    // lock object to prevent thread from exit
    private static Object threadLock = new Object();

    // scaffold objects
    private static volatile ArgumentHandler argumentHandler = null;
    private static volatile Log log = null;

    public static void main(String args[]) {
        ownmonitors001a _ownmonitors001a = new ownmonitors001a();
        System.exit(ownmonitors001.JCK_STATUS_BASE + _ownmonitors001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        // make communication pipe to debugger
        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);

        // lock the object to prevent thread from exit
        synchronized (threadLock) {

            // load tested class and create tested thread
            log.display("Creating object of tested class");
            TestedClass.thread = new TestedClass(THREAD_NAME);

            // start the thread and wait for notification from it
            synchronized (threadReady) {
                TestedClass.thread.start();
                try {
                    threadReady.wait();
                    // send debugger signal READY
                    log.display("Sending signal to debugger: " + ownmonitors001.READY);
                    pipe.println(ownmonitors001.READY);
                } catch (InterruptedException e) {
                    log.complain("Interruption while waiting for thread started: " + e);
                    pipe.println(ownmonitors001.ERROR);
                }
            }

            // wait for signal QUIT from debugeer
            log.display("Waiting for signal from debugger: " + ownmonitors001.QUIT);
            String signal = pipe.readln();
            log.display("Received signal from debugger: " + signal);

            // check received signal
            if (signal == null || !signal.equals(ownmonitors001.QUIT)) {
                log.complain("Unexpected communication signal from debugee: " + signal
                            + " (expected: " + ownmonitors001.QUIT + ")");
                log.display("Debugee FAILED");
                return ownmonitors001.FAILED;
            }

            // allow started thread to exit
        }

        // exit debugee
        log.display("Debugee PASSED");
        return ownmonitors001.PASSED;
    }

    // tested thread class
    public static class TestedClass extends Thread {

        // field with the tested Thread value
        public static volatile TestedClass thread = null;

        // field with object whose monitor the tested thread owns
        public static Object ownedMonitor = new Object();

        // field with object whose monitor the tested thread does not own
        public static Object notOwnedMonitor = new Object();

        TestedClass(String name) {
            super(name);
        }

        // start the thread and recursive invoke makeFrames()
        public void run() {
            log.display("Tested thread started");

            // get ownership for the tested monitors
            synchronized (ownedMonitor) {

                // notify debuggee that thread ready for testing
                synchronized (threadReady) {
                    threadReady.notifyAll();
                }

                // wait for lock object released
                synchronized (threadLock) {
                    log.display("Tested thread finished");
                }

            }

        }

    }

}
