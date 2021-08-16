/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.StackFrame.hashCode;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;
import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;

/**
 * The debugger application of the test.
 */
public class hashcode001 {

    //------------------------------------------------------- immutable common fields

    final static String SIGNAL_READY = "ready";
    final static String SIGNAL_GO    = "go";
    final static String SIGNAL_QUIT  = "quit";

    private static int waitTime;
    private static int exitStatus;
    private static ArgumentHandler     argHandler;
    private static Log                 log;
    private static Debugee             debuggee;
    private static ReferenceType       debuggeeClass;

    //------------------------------------------------------- mutable common fields

    private final static String prefix = "nsk.jdi.StackFrame.hashCode.";
    private final static String className = "hashcode001";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields

    /** debuggee's methods for check **/
    private final static String checkedMethods[] = {
        "Mv" ,  "MvS",  "MvI",  "MvY",  "MvU",  "MvN",
        "MvR",  "MvP",  "MvSM", "MvIM", "MvYM", "MvPM", "MvNP"
                                                   };

    //------------------------------------------------------- immutable common methods

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    private static void display(String msg) {
        log.display("debugger > " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE > " + msg);
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        waitTime = argHandler.getWaitTime() * 60000;

        debuggee = Debugee.prepareDebugee(argHandler, log, debuggeeName);

        debuggeeClass = debuggee.classByName(debuggeeName);
        if ( debuggeeClass == null ) {
            complain("Class '" + debuggeeName + "' not found.");
            exitStatus = Consts.TEST_FAILED;
        }

        execTest();

        debuggee.quit();

        return exitStatus;
    }

    //------------------------------------------------------ mutable common method

    private static void execTest() {

        BreakpointRequest brkp = debuggee.setBreakpoint(debuggeeClass,
                                                    hashcode001a.brkpMethodName,
                                                    hashcode001a.brkpLineNumber);
        debuggee.resume();

        debuggee.sendSignal(SIGNAL_GO);
        Event event = null;

        // waiting the breakpoint event
        try {
            event = debuggee.waitingEvent(brkp, waitTime);
        } catch (InterruptedException e) {
            throw new Failure("unexpected InterruptedException while waiting for Breakpoint event");
        }
        if (!(event instanceof BreakpointEvent)) {
            debuggee.resume();
            throw new Failure("BreakpointEvent didn't arrive");
        }

        ThreadReference thread = ((BreakpointEvent)event).thread();
        List frames = null;
        try {
            frames = thread.frames();
        } catch(IncompatibleThreadStateException e) {
            throw new Failure("Unexpected IncompatibleThreadStateException when getting list of frames");
        }

        display("Checking hashCode() method for debuggee's stack frames...");

        // Check all methods from debuggee
        for (int i = 0; i < frames.size(); i++) {

            StackFrame stackFrame = null;
            try {
                stackFrame = (StackFrame)frames.get(i);
                int hCode = stackFrame.hashCode();
                if (hCode == 0) {
                    complain("hashCode() returns 0 for stack frame #" + i);
                    exitStatus = Consts.TEST_FAILED;
                }

                int hCode1 = stackFrame.hashCode();
                if (hCode != hCode1) {
                    complain("hashCode() is not consistent for stack frame #" + i +
                        "\n\t first value :" + hCode + " ; second value : " + hCode1);
                    exitStatus = Consts.TEST_FAILED;
                }

                // get new reference to the same stack frame and get hash code.
                hCode1 = ((StackFrame)frames.get(i)).hashCode();
                if (hCode != stackFrame.hashCode()) {
                    complain("hashCode() does not return same value for stack frame equal to one #" + i +
                        "\n\t first value :" + hCode + " ; second value : " + hCode1);
                    exitStatus = Consts.TEST_FAILED;
                }

                display("hashCode() returns for stack frame #" + i + " : " + hCode);

            } catch(Exception e) {
                complain("Unexpected " + e + " when getting StackFrame for stack frame #" + i);
                exitStatus = Consts.TEST_FAILED;
            }

        }

        display("Checking completed!");
        debuggee.resume();
    }

    //--------------------------------------------------------- test specific methods

}
//--------------------------------------------------------- test specific classes
