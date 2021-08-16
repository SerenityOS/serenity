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

//    THIS TEST IS LINE NUMBER SENSITIVE

package nsk.jdwp.Event.SINGLE_STEP;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class singlestep003a {

    static final int BREAKPOINT_LINE = 101;
    static final int SINGLE_STEP_LINE = 92;

    static ArgumentHandler argumentHandler = null;
    static Log log = null;

    public static void main(String args[]) {
        singlestep003a _singlestep003a = new singlestep003a();
        System.exit(singlestep003.JCK_STATUS_BASE + _singlestep003a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        // create tested thread
        log.display("Creating tested thread");
        TestedClass thread = new TestedClass(singlestep003.TESTED_THREAD_NAME);
        log.display("  ... thread created");

        // start tested thread
        log.display("Starting tested thread");
        thread.start();
        log.display("  ... thread started");

        // wait for thread finished
        try {
            log.display("Waiting for tested thread finished");
            thread.join();
            log.display("  ... thread finished");
        } catch (InterruptedException e) {
            log.complain("Interruption while waiting for tested thread finished");
            return singlestep003.FAILED;
        }

        // exit debugee
        log.display("Debugee PASSED");
        return singlestep003.PASSED;
    }

    // tested class
    public static class TestedClass extends Thread {
        public TestedClass(String name) {
            super(name);
        }

        public void run() {
            log.display("Tested thread: started");

            log.display("Invoking tested method");
            methodForBreakpoint();
            // next line is for out step event
            int foo = 0; // SINGLE_STEP_LINE
            log.display("  ... tested method invoked");

            log.display("Tested thread: finished");
        }

        public void methodForBreakpoint() {
            log.display("Breakpoint line reached");
            // next line is for breakpoint before out step event request
            int foo = 100; // BREAKPOINT_LINE
            foo = foo + 100;
            foo = foo + 100;
            foo = foo + 100;
            foo = foo + 100;
            log.display("Returned from tested method");
        }
    }
}
