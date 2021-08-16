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

package nsk.jdi.ClassType.concreteMethodByName;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;

import java.io.*;

/**
 *  The test checks up the method                                               <br>
 *      <code>com.sun.jdi.ClassType.concreteMethodByName(String, String)</code> <br>
 *  for boundary value of parameters.                                           <br>
 *
 *  In any cases it is expected, this method will return either the <code>Method</code>
 *  that matches the given name and signature or <code>null</code> if there is
 *  no match. No exception is expected.
 *  All variants of argument values are placed in <code>parameters</code> array.
 *  Also, this array contains an expected return value for every pair of
 *  arguments values: <br>
 *      <code>null</code> - return value of <code>concreteMethodByName()</code><br> is
 *  <code>null</code>
 *      <code>"T"</code> - return value of <code>concreteMethodByName()</code><br> is not
 *  <code>null</code>
 */

public class method002 {

    private final static String prefix = "nsk.jdi.ClassType.concreteMethodByName.";
    private final static String debuggerName = prefix + "method002";
    private final static String debugeeName = debuggerName + "a";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;

    private ClassType testedClass;

    private String [][] parameters = {
//                  methodName            signature      result
                    {"justMethod",          "(I)I",     "T"},
                    {"justMethod",          null,       null},
                    {null,                  "(I)I",     null},
                    {null,                  null,       null},
                    {"",                    "",         null}
    };

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

        method002 thisTest = new method002();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        thisTest.execTest();
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() {

        testedClass = (ClassType )debugee.classByName(debugeeName);

        display("\nTEST BEGINS");
        display("===========");

        Method method;
        for (int i = 0; i < parameters.length; i ++) {
            display("invoking concreteMethodByName(\"" + parameters[i][0]
                        + "\",\"" + parameters[i][1] + "\")");
            try {
                method = testedClass.concreteMethodByName(parameters[i][0],
                                                                    parameters[i][1]);
                if (parameters[i][2] != null) {
                    if (method == null) {
                        complain("Method : " + method);
                        exitStatus = Consts.TEST_FAILED;
                    } else {
                        display("Method : " + method);
                    }
                } else {
                    if (method == null) {
                        display("Method : " + method);
                    } else {
                        complain("Method : " + method);
                        exitStatus = Consts.TEST_FAILED;
                    }
                }
            } catch(NullPointerException e) {
                display("!!!expected NullPointerException");
            } catch(Exception e) {
                complain("Unexpected " + e);
                exitStatus = Consts.TEST_FAILED;
            }
            display("");
        }

        display("");
        display("=============");
        display("TEST FINISHES\n");

        debugee.resume();
        debugee.quit();
    }
}
