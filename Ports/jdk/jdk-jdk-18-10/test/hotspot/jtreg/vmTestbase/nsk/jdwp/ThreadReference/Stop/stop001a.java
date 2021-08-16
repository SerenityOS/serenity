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

package nsk.jdwp.ThreadReference.Stop;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class stop001a {

    // name for the tested thread
    public static final String THREAD_NAME = "TestedThreadName";
    public static final String THREAD_FIELD_NAME = "thread";
    public static final String THROWABLE_FIELD_NAME = "throwable";

    // frames count for tested thread in recursive method invokation
    public static final int FRAMES_COUNT = 10;

    // notification object to notify debuggee that thread is started
    private static Object threadStarting = new Object();
    // object which thread will wait for before being interruted
    private static Object threadWaiting = new Object();

    // scaffold objects
    private static volatile ArgumentHandler argumentHandler = null;
    private static volatile Log log = null;

    public static void main(String args[]) {
        stop001a _stop001a = new stop001a();
        System.exit(stop001.JCK_STATUS_BASE + _stop001a.runIt(args, System.err));
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
                pipe.println(stop001.ERROR);
                log.display("Debugee FAILED");
                return stop001.FAILED;
            }

            // ensure that tested thread is waiting for object
            synchronized (threadWaiting) {
                // send debugger signal READY
                log.display("Sending signal to debugger: " + stop001.READY);
                pipe.println(stop001.READY);
            }
        }

        // wait for signal QUIT from debugeer
        log.display("Waiting for signal from debugger: " + stop001.RUN);
        String signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        // check received signal
        if (signal == null || !signal.equals(stop001.RUN)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + stop001.RUN + ")");
            log.display("Debugee FAILED");
            return stop001.FAILED;
        }

        // wait for thread finished in a waittime interval
        log.display("Waiting for tested thread finished for timeout: " + timeout);
        try {
            TestedClass.thread.join(timeout);
        } catch (InterruptedException e) {
            log.complain("Interruption while waiting for tested thread finished:\n\t" + e);
            pipe.println(stop001.ERROR);
            log.display("Debugee FAILED");
            return stop001.FAILED;
        }

        // test if thread was interrupted by debugger
        if (TestedClass.thread.isAlive()) {
            log.display("Sending signal to debugger: " + stop001.NOT_STOPPED);
            pipe.println(stop001.NOT_STOPPED);
            // interrupt thread to allow it to finish
            TestedClass.thread.interrupt();
        } else {
            log.display("Sending signal to debugger: " + stop001.STOPPED);
            pipe.println(stop001.STOPPED);
        }

        // wait for signal QUIT from debugeer
        log.display("Waiting for signal from debugger: " + stop001.QUIT);
        signal = pipe.readln();
        log.display("Received signal from debugger: " + signal);

        // check received signal
        if (signal == null || !signal.equals(stop001.QUIT)) {
            log.complain("Unexpected communication signal from debugee: " + signal
                        + " (expected: " + stop001.QUIT + ")");
            log.display("Debugee FAILED");
            return stop001.FAILED;
        }

        // exit debugee
        log.display("Debugee PASSED");
        return stop001.PASSED;
    }

    // tested thread class
    public static class TestedClass extends Thread {

        // field with the tested Thread value
        public static volatile TestedClass thread = null;

        // field with the tested Throwable value
        public static volatile Throwable throwable = new Throwable("Tested throwable");

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
                }
            }

            log.display("Tested thread finished");
        }

    }

}
