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

package nsk.jdi.EventRequest._bounds_;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 *
 * The test checks up the methods of <code>com.sun.jdi.EventRequest</code>: <br>
 *     1. <code>putProperty(Object, Object)</code>                          <br>
 *     2. <code>getProperty(Object)</code>                                  <br>
 *     3. <code>setSuspendPolicy(int)</code>                                <br>
 * to correctly work for boundary values of argument.                       <br>
 *
 * The test performs the next testcases:                                    <br>
 *     1. <code>putProperty(null, String)</code> - in this case it is
 *        expected property <code>null</code> with value of
 *        <code>String</code> will be created.                              <br>
 *        No exception must be thrown.
 *     2. <code>getProperty(null)</code> - value of <code>String</code> from
 *        the previous case is expected as return value. No exception must
 *        be thrown.                                                        <br>
 *     3. <code>setSuspendPolicy(int)</code> is checked for the next
 *        parameters: <code>Integer.MIN_VALUE</code>, <code>-1</code>,
 *        <code>Integer.MAX_VALUE</code>. IllegalArgumentException is expected. <br>
 */

public class eventrequest001 {

    private final static String prefix = "nsk.jdi.EventRequest._bounds_.";
    private final static String debuggerName = prefix + "eventrequest001";
    private final static String debugeeName = debuggerName + "a";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";

    public static int exitStatus;
    public static Log log;
    public static Debugee debugee;

    private static String propertyValue = "something";
    private static int policies[] = {
                                        Integer.MIN_VALUE,
                                        -1,
                                        Integer.MAX_VALUE
    };

//----------------------------------------------------

    public static void display(String msg) {
        log.display(msg);
    }

    public static void complain(String msg) {
        log.complain("debugger FAILURE> " + msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        eventrequest001 thisTest = new eventrequest001();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        thisTest.execTest();
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() {

        debugee.VM().suspend();
        ReferenceType testedClass = debugee.classByName(debugeeName);

        BreakpointRequest brkp = debugee.setBreakpoint(testedClass,
                                                    eventrequest001a.brkpMethodName,
                                                    eventrequest001a.brkpLineNumber);

        display("\nTEST BEGINS");
        display("===========");

        try {
            display("setting property <null>: value <" + propertyValue + ">");
            brkp.putProperty(null, propertyValue);
        } catch (Exception e) {
            complain("Unexpected: " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        Object obj = null;
        try {
            display("reading property <null>:");
            obj = brkp.getProperty(null);
            display("\t\t value : <" + obj + ">");
        } catch (Exception e) {
            complain("Unexpected: " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        if (!obj.equals(propertyValue)) {
            complain("Unexpected property value: " + obj);
            exitStatus = Consts.TEST_FAILED;
        }

        display("");
        for (int i = 0; i < policies.length; i++) {
            display("invoking brkp.setSuspendPolicy(" + policies[i] + ")");
            brkp.disable();
            try {
                brkp.setSuspendPolicy(policies[i]);

                complain("No exception was thrown for argument: " + policies[i]);
                exitStatus = Consts.TEST_FAILED;
            } catch (IllegalArgumentException e1) {
                display("Expected IllegalArgumentException was thrown for argument: " + policies[i]);
            } catch (Exception e) {
                complain("Unexpected exception was thrown for argument: " + policies[i] + " :\n\t" + e );
                exitStatus = Consts.TEST_FAILED;
            }
            brkp.enable();
            display("");
        }

        // breakpoint should be disabled before test finishing
        brkp.disable();

        display("=============");
        display("TEST FINISHES\n");

        debugee.resume();
        debugee.quit();
    }
}
