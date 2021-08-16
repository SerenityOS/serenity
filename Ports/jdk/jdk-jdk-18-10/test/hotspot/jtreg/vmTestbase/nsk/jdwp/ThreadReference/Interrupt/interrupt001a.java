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

package nsk.jdwp.ThreadReference.Interrupt;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class interrupt001a {

    // name for the tested thread
    public static final String THREAD_NAME = "TestedThreadName";
    public static final String FIELD_NAME = "thread";

    public static volatile boolean interrupted = false;

    // notification object to notify debuggee that thread is started
    private static Object threadStarting = new Object();
    // object which thread will wait for before being interruted
    private static Object threadWaiting = new Object();

    // scaffold objects
    private static volatile ArgumentHandler argumentHandler = null;
    private static volatile Log log = null;

    public static void main(String args[]) {
        interrupt001a _interrupt001a = new interrupt001a();
        System.exit(interrupt001.JCK_STATUS_BASE + _interrupt001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);
        long timeout = argumentHandler.getWaitTime() * 60 * 1000; // milliseconds

        // make communication pipe to debugger
        log.display("Creating pipe");
        IOPipe pipe = argumentHandler.createDebugeeIOPipe(log);

        // load tested class and create tested thread
        log.display("Creating object of tested class");
        TestedClass.thread = new TestedClass(THREAD_NAME);

        // start the thread and wait for notification from it
        synchronized (threadStarting) {
            TestedClass.thread.start();
            try {
                threadStarting.wait();
            } catch (InterruptedException e) {
                log.complain("Interruption while waiting for thread started:\n\t" + e);
                pipe.println(interrupt001.ERROR);
                log.display("Debugee FAILED");
                return interrupt001.FAILED;
            }

            // send debugger signal READY
            log.display("Sending signal to debugger: " + interrupt001.READY);
            pipe.println(interrupt001.READY);
        }

        // wait for signal RUN from debugeer
        log.display("Waiting for signal from debugger: " + interrupt001.RUN);
        String signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        // check received signal
        if (signal == null || !signal.equals(interrupt001.RUN)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + interrupt001.RUN + ")");
            log.display("Debugee FAILED");
            return interrupt001.FAILED;
        }

        // wait for thread finished in a waittime interval
        if (TestedClass.thread.isAlive()) {
            log.display("Waiting for tested thread finished for timeout: " + timeout);
            try {
                TestedClass.thread.join(timeout);
            } catch (InterruptedException e) {
                log.complain("Interruption while waiting for tested thread finished:\n\t" + e);
                pipe.println(interrupt001.ERROR);
                log.display("Debugee FAILED");
                return interrupt001.FAILED;
            }
        }

        // test if thread was interrupted by debugger
        if (interrupt001a.interrupted) {
            log.display("Sending signal to debugger: " + interrupt001.INTERRUPTED_TRUE);
            pipe.println(interrupt001.INTERRUPTED_TRUE);
        } else {
            log.display("Sending signal to debugger: " + interrupt001.INTERRUPTED_FALSE);
            pipe.println(interrupt001.INTERRUPTED_FALSE);
        }

        // wait for signal QUIT from debugeer
        log.display("Waiting for signal from debugger: " + interrupt001.QUIT);
        signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        // check received signal
        if (signal == null || !signal.equals(interrupt001.QUIT)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + interrupt001.QUIT + ")");
            log.display("Debugee FAILED");
            return interrupt001.FAILED;
        }

        // exit debugee
        log.display("Debugee PASSED");
        return interrupt001.PASSED;
    }

    // tested thread class
    public static class TestedClass extends Thread {

        // field with the tested Thread value
        public static volatile TestedClass thread = null;

        TestedClass(String name) {
            super(name);
        }

        // start the thread and recursive invoke makeFrames()
        public void run() {
            log.display("Tested thread started");

            synchronized (threadWaiting) {

                // notify debuggee that thread started
                synchronized (threadStarting) {
                    threadStarting.notifyAll();
                }

                // wait infinitely for notification object
                try {
                    threadWaiting.wait();
                    log.complain("Tested thread NOT interrupted");
                } catch (InterruptedException e) {
                    log.display("Tested thread interrupted");
                    interrupt001a.interrupted = true;
                }
            }

            log.display("Tested thread finished");
        }

    }

}
