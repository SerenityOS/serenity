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

package nsk.jdwp.StackFrame.PopFrames;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class popframes001a {

    // name of the tested thread
    public static final String THREAD_NAME = "testedThread";

    // line nunber for breakpoint
    public static final int BREAKPOINT_LINE_NUMBER = 113;

    // scaffold objects
    private static volatile ArgumentHandler argumentHandler = null;
    private static volatile Log log = null;

    public static void main(String args[]) {
        popframes001a _popframes001a = new popframes001a();
        System.exit(popframes001.JCK_STATUS_BASE + _popframes001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        // create tested thread
        log.display("Creating testing thread");
        TestedThreadClass thread = new TestedThreadClass(THREAD_NAME);
        log.display("  ... thread created");

        // start tested thread
        log.display("Starting tested thread");
        thread.start();
        log.display("  ... thread started");

        // wait for tested thread finished
        try {
            log.display("Waiting for tested thread finished");
            thread.join();
            log.display("  ... thread finished");
        } catch(InterruptedException e) {
            log.complain("Interruption while waiting for tested thread finished:\n\t" + e);
            log.display("Debugee FAILED");
            return popframes001.FAILED;
        }

        log.display("Debugee PASSED");
        return popframes001.PASSED;
    }

    // tested thread class
    public static class TestedThreadClass extends Thread {

        // number of invokations of tested method
        public static volatile int invokations = 0;

        public TestedThreadClass(String name) {
            super(name);
        }

        // invoke tested method
        public void run() {
            log.display("Tested thread: started");

            // invoke tested method
            int foo = 100;
            foo = testedMethod(foo);

            log.display("Tested thread: finished");
        }

        // tested method to pop frames
        public int testedMethod(int arg) {
            invokations++;
            log.display("Tested method invoked " + invokations + " time");
            int boo = 0;

            log.display("Breakpoint line reached");
            // next line is for breakpoint
            boo = arg * 2; // BREAKPOINT_LINE_NUMBER
            log.display("Breakpoint line passed");

            return boo;
        }
    }
}
