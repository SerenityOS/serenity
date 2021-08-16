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

package nsk.jdi.StepEvent._itself_;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


// This class is the debugged application in the test

public class stepevent002a {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    // commands
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT  = "quit";
    static final String COMMAND_GO    = "go";
    static final String COMMAND_DONE  = "done";

    // monitors
    public static Object threadStarted  = new Object();
    public static Object threadExecuted = new Object();
    public static Object threadFinished = new Object();

    // checked thread
    private static stepevent002aThread threadForEvent;

    // support classes
    private static ArgumentHandler argHandler;
    private static IOPipe pipe;
    public static Log log;

    public static void main(String args[]) {
        stepevent002a _stepevent002a = new stepevent002a();
        System.exit(JCK_STATUS_BASE + _stepevent002a.runIt(args, System.err));
    }

    int runIt( String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        pipe = argHandler.createDebugeeIOPipe();

        // create checked thread
        threadForEvent = new stepevent002aThread("threadForEvent");

        // lock monitor threadFinished to prevent checked thread
        // from exiting after it executed
        synchronized (threadFinished) {

            // lock monitor threadExecuted to prevent checked method invokation
            // before <GO> command received
            synchronized (threadExecuted) {

                // start checked thread and wait for it actually started
                synchronized (threadStarted) {
                    log.display("Starting checked thread");
                    threadForEvent.start();
                    try {
                        threadStarted.wait();
                    } catch (InterruptedException e) {
                        log.complain("TEST BUG: Debugee: InterruptedException caught: " + e);
                        return FAILED;
                    }
                }

                // thread started; notify debugger and wait for command <GO>
                pipe.println(COMMAND_READY);
                String command = pipe.readln();
                if (!command.equals(COMMAND_GO)) {
                    log.complain("TEST BUG: Debugee: unknown command: " + command);
                    return FAILED;
                }

                // unlock threadExecuted monitor to permit checked thread to execute
                // and wait for checked thread finishes invoking method
                try {
                    log.display("Waiting for checked thread executed");
                    threadExecuted.wait();
                } catch (InterruptedException e) {
                    log.complain("TEST BUG: Debugee: InterruptedException caught: " + e);
                    return FAILED;
                }
            }

            // checked method invoked (but thread not finished yet)
            // notify debugger
            pipe.println(COMMAND_DONE);

            // wait for command <QUIT> from debugger and exit debuggee
            String command = pipe.readln();
            if (!command.equals(COMMAND_QUIT)) {
                log.complain("TEST BUG: Debugee: unknown command: " + command);
                return FAILED;
            }

            // unlock monitor threadFinished to permit checked thread to finish
        }

        return PASSED;
    }

    // checked method (recursive for 10 times)
    static void foo(int i) { if (i-- > 1) { foo(i); } }
}

// checked thread class
class stepevent002aThread extends Thread {
    static private int counter;

    stepevent002aThread (String name) {
        super(name);
    }

    public void run() {

        // notify main thread that checked thread started
        synchronized (stepevent002a.threadStarted) {
            stepevent002a.log.display("Checked thread started");
            stepevent002a.threadStarted.notify();
        }

        // wait for main thread releases monitor to permit checked thread to execute
        synchronized (stepevent002a.threadExecuted) {
            counter = stepevent002.EXPECTED_EVENTS_COUNT;
            stepevent002a.log.display("Invoking checked method from checked thread");
            stepevent002a.foo(counter);
            stepevent002a.log.display("Checked method invoked");

            // notify main thread that checked thread executed
            stepevent002a.threadExecuted.notify();
        }

        // wait for main thread releases monitor to permit thread to finish
        synchronized (stepevent002a.threadFinished) {
            stepevent002a.log.display("Checked thread finished");
        }
    }
}
