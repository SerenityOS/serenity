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

package nsk.jdi.StackFrame._bounds_;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 * Test checks up StackFrame methods for the following cases:  <br>
 *      - <code>getValue(null)</code>                               <br>
 *      - <code>getValues(null)</code>                              <br>
 *      - <code>getValues(list with size = 0)</code>                <br>
 *      - <code>setValue(null, null)</code>                         <br>
 *      - <code>setValue(field, null)</code>                        <br>
 *      - <code>visibleVariableByName(null)</code>                  <br>
 *      - <code>visibleVariableByName("")</code>                    <br>
 * <code>NullPointerException</code> is expected for every test case
 * except for the three last.
 */

public class bounds002 {

    private final static String prefix = "nsk.jdi.StackFrame._bounds_.";
    private final static String debuggerName = prefix + "bounds002";
    private final static String debugeeName = debuggerName + "a";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;

    private static void display(String msg) {
        log.display(msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE> " + msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        bounds002 thisTest = new bounds002();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        thisTest.execTest();
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() {

        debugee.VM().suspend();
        ThreadReference thread = debugee.threadByName("main");
        StackFrame stackFrame = null;
        try {
            for (int i = 0; i < thread.frameCount(); i++) {
                stackFrame = thread.frame(i);
                if (stackFrame.location().method().name().equals("main") ) {
                    break;
                }
            }
        } catch (IncompatibleThreadStateException e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
            return;
        }

        display("\nTEST BEGINS");
        display("===========");

        Location loc = stackFrame.location();
        try {
            display("StackFrame: " + loc.sourcePath());
            display("    method: " + loc.method().name());
        } catch (AbsentInformationException e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
            return;
        }
        display("");

        Value retValue;

        display("getValue(null)");
        try {
            retValue = stackFrame.getValue(null);
            complain("NullPointerException is not thrown");
            exitStatus = Consts.TEST_FAILED;
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        } catch(Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("getValues(null)");
        try {
            stackFrame.getValues(null);
            complain("NullPointerException is not thrown");
            exitStatus = Consts.TEST_FAILED;
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        } catch(Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        List<? extends com.sun.jdi.LocalVariable> lst = null;
        display("getValues(list with size = 0)");
        try {
            stackFrame.getValues(lst);
            complain("NullPointerException is not thrown");
            exitStatus = Consts.TEST_FAILED;
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        } catch(Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("setValue(null, null)");
        try {
            stackFrame.setValue(null, null);
            complain("NullPointerException is not thrown");
            exitStatus = Consts.TEST_FAILED;
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        } catch(Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("setValue(variable, null)");
        LocalVariable var = null;
        try {
            var = stackFrame.visibleVariableByName(bounds002a.testedFieldName);
        } catch (AbsentInformationException e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        try {
            stackFrame.setValue(var, null);
            display("OK");
        } catch(Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("visibleVariableByName(null)");
        try {
            var = stackFrame.visibleVariableByName(null);
            if (var != null ) {
                complain("Unexpected local variable <null>");
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("OK");
            }
        } catch (Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("visibleVariableByName(\"\")");
        try {
            var = stackFrame.visibleVariableByName("");
            if (var != null ) {
                complain("Unexpected local variable \"\"");
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("OK");
            }
        } catch (Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("=============");
        display("TEST FINISHES\n");

        debugee.resume();
        debugee.quit();
    }
}
