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

package nsk.jdi.EventRequestManager._bounds_;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.*;

/**
 * The test checks up the                                       <br>
 *  a) <code>createStepRequest(ThreadReference, int, int)</code> <br>
 *     this method is invoked 3 times with different arguments: <br>
 *       1. (null, StepRequest.STEP_LINE, StepRequest.STEP_OVER)<br>
 *          in this case NullPointerException is expected       <br>
 *       2. (thread, Integer.MAX_VALUE, StepRequest.STEP_OVER)  <br>
 *       3. (null, StepRequest.STEP_LINE, Integer.MAX_VALUE)    <br>
 *          in 2, 3 cases no exceptions are expected            <br>
 *  b) <code>createBreakpointRequest(Location)</code>            <br>
 *  c) <code>createAccessWatchpointRequest(Field)</code>         <br>
 *  d) <code>createModificationWatchpointRequest(Field)</code>   <br>
 *  f) <code>deleteEventRequest(EventRequest)</code>             <br>
 *  g) <code>deleteEventRequests(List)</code>                    <br>
 * In b)-g) cases <code>NullPointerException</code> is expected.
 */
public class requests001 {

    final static String prefix = "nsk.jdi.EventRequestManager._bounds_.";
    private final static String className = "requests001";
    private final static String debuggerName = prefix + className;
    private final static String debugeeName = debuggerName + "a";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;

    private static void display(String msg) {
        log.display(msg);
    }

    private static void complain(String msg) {
        log.complain("FAILURE> " + msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        requests001 thisTest = new requests001();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        thisTest.execTest();
        display("execTest finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() {

        EventRequest request;
        EventRequestManager evm = debugee.getEventRequestManager();
        ThreadReference thread = debugee.threadByName("main");

        display("");
        display("...calling createStepRequest(null, StepRequest.STEP_LINE, "
                            + "StepRequest.STEP_OVER)");
        display("                             ^^^^");
        try {
            request = evm.createStepRequest(null, StepRequest.STEP_LINE,
                                                        StepRequest.STEP_OVER);
            exitStatus = Consts.TEST_FAILED;
            complain("NullPointerException is not throw");
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        }

        display("");
        display("...calling createStepRequest(thread, Integer.MAX_VALUE, "
                    + "StepRequest.STEP_OVER)");
        display("                                     ^^^^^^^^^^^^^^^^^");
        try {
            request = evm.createStepRequest(thread, Integer.MAX_VALUE,
                                                        StepRequest.STEP_OVER);
            exitStatus = Consts.TEST_FAILED;
            complain("IllegalArgumentException is not throw");
        } catch(IllegalArgumentException e) {
            display("!!!expected IllegalArgumentException");
        } catch(Exception e) {
            exitStatus = Consts.TEST_FAILED;
            complain("***unexpected " + e);
        }

        display("");
        display("...calling createStepRequest(null, StepRequest.STEP_LINE, "
                        + "Integer.MAX_VALUE)");
        display("                                                          "
                        + "^^^^^^^^^^^^^^^^^");
        try {
            request = evm.createStepRequest(thread, StepRequest.STEP_LINE,
                                                        Integer.MAX_VALUE);
            exitStatus = Consts.TEST_FAILED;
            complain("IllegalArgumentException is not throw");
        } catch(IllegalArgumentException e) {
            display("!!!expected IllegalArgumentException");
        } catch(Exception e) {
            exitStatus = Consts.TEST_FAILED;
            complain("***unexpected " + e);
        }

        display("");
        display("...calling createBreakpointRequest(null)");
        try {
            request = evm.createBreakpointRequest(null);
            exitStatus = Consts.TEST_FAILED;
            complain("NullPointerException is not throw");
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        }

        display("");
        display("...calling createAccessWatchpointRequest(null)");
        try {
            request = evm.createAccessWatchpointRequest(null);
            exitStatus = Consts.TEST_FAILED;
            complain("NullPointerException is not throw");
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        }

        display("");
        display("...calling createModificationWatchpointRequest(null)");
        try {
            request = evm.createModificationWatchpointRequest(null);
            exitStatus = Consts.TEST_FAILED;
            complain("NullPointerException is not throw");
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        }

        display("");
        display("...calling deleteEventRequest(null)");
        try {
            evm.deleteEventRequest(null);
            exitStatus = Consts.TEST_FAILED;
            complain("NullPointerException is not throw");
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        }

        display("");
        display("...calling deleteEventRequests(null)");
        try {
            evm.deleteEventRequests(null);
            exitStatus = Consts.TEST_FAILED;
            complain("NullPointerException is not throw");
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        }

        display("");
        display("...calling deleteEventRequests(list of <null> values)");
        Vector<EventRequest> list = new Vector<EventRequest>();
        list.add(null);
        list.add(null);
        list.add(null);
        list.add(null);
        list.add(null);
        list.add(null);
        try {
            evm.deleteEventRequests(list);
            exitStatus = Consts.TEST_FAILED;
            complain("NullPointerException is not throw");
        } catch(NullPointerException e) {
            display("!!!expected NullPointerException");
        }

        display("");
        debugee.quit();
    }
}
