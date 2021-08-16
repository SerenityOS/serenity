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

package nsk.jdwp.ClassType.InvokeMethod;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class invokemeth001a {

    // name of the tested thread
    public static final String THREAD_NAME = "testedThread";

    // line nunber for breakpoint
    public static final int BREAKPOINT_LINE_NUMBER = 86;

    // initial and final value of variable changed by the method invoked from debugger
    public static final int INITIAL_VALUE = 10;
    public static final int FINAL_VALUE = 1234;

    // scaffold objects
    private static volatile ArgumentHandler argumentHandler = null;
    private static volatile Log log = null;

    public static void main(String args[]) {
        invokemeth001a _invokemeth001a = new invokemeth001a();
        System.exit(invokemeth001.JCK_STATUS_BASE + _invokemeth001a.runIt(args, System.err));
    }

    public int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        // create tested of tested class
        log.display("Creating object of tested class");
        TestedObjectClass foo = new TestedObjectClass();
        log.display("  ... object created");

        // run method with breakpoint
        TestedObjectClass.run();

        log.display("Debugee PASSED");
        return invokemeth001.PASSED;
    }

    // tested object class
    public static class TestedObjectClass {
        // result of invoking tested mathod
        public static volatile int result = INITIAL_VALUE;

        // suspend current thread on breakpoint
        public static void run() {
            log.display("Tested thread: started");

            log.display("Breakpoint line reached");
            // next line is for breakpoint
            int foo = 0; // BREAKPOINT_LINE_NUMBER
            log.display("Breakpoint line passed");

            log.display("Tested thread: finished");
        }

        // tested method for invocation from debugger
        public static int testedMethod(int arg) {
            log.display("Tested method invoked with argument:" + arg);
            int old = result;
            result = arg;
            log.display("Tested method returned with result:" + old);
            return old;
        }
    }
}
