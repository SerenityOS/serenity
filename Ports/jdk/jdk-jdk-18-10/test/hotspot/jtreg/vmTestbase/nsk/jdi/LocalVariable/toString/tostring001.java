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

package nsk.jdi.LocalVariable.toString;

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
public class tostring001 {

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

    private final static String prefix = "nsk.jdi.LocalVariable.toString.";
    private final static String className = "tostring001";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields

    /** debuggee's local variables for check **/
    private final static String checkedVars[] = {
        "z0", "z1", "z2",
        "b0", "b1", "b2",
        "c0", "c1", "c2",
        "d0", "d1", "d2",
        "f0", "f1", "f2",
        "i0", "i1", "i2",
        "l0", "l1", "l2",
        "r0", "r1", "r2",
        "Z0", "Z1", "Z2",
        "B0", "B1", "B2",
        "C0", "C1", "C2",
        "D0", "D1", "D2",
        "F0", "F1", "F2",
        "I0", "I1", "I2",
        "L0", "L1", "L2",
        "R0", "R1", "R2",
        "s0", "s1", "s2",
        "o0", "o1", "o2",
        "p0", "p1", "p2",
        "m0", "m1", "m2"
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

        // Finding of debuggee's main method
        Method mainMethod = methodByName(debuggeeClass, "main");

        display("Checking toString() method for local variables of debuggee's main method...");

        // Check all methods from debuggee
        for (int i = 0; i < checkedVars.length; i++) {

            LocalVariable localVar = variableByName(mainMethod, checkedVars[i]);

            try {
                String str = localVar.toString();
                if (str == null) {
                    complain("toString() returns null for LocalVariable of debuggee's local variable: " + checkedVars[i]);
                    exitStatus = Consts.TEST_FAILED;
                } else if (str.length() == 0) {
                    complain("toString() returns empty string for LocalVariable of debuggee's local variable: " + checkedVars[i]);
                    exitStatus = Consts.TEST_FAILED;
                } else {
                    display("toString() returns for debuggee's local variable " + checkedVars[i] + " : " + str);
                }
            } catch(Exception e) {
                complain("Unexpected " + e + " when getting LocalVariable of debuggee's local variable: " + checkedVars[i]);
                exitStatus = Consts.TEST_FAILED;
            }

        }

        display("Checking completed!");
    }

    //--------------------------------------------------------- test specific methods

    private static Method methodByName(ReferenceType refType, String methodName) {
        List methodList = refType.methodsByName(methodName);
        if (methodList == null) {
            throw new Failure("Can not find method: " + methodName);
        }
        if (methodList.size() > 1) {
            throw new Failure("Found more than one method with name : " + methodName);
        }

        Method method = (Method) methodList.get(0);
        return method;
    }

    private static LocalVariable variableByName(Method method, String varName) {
        List varList = null;
        try {
            varList = method.variablesByName(varName);
        } catch (AbsentInformationException e) {
            throw new Failure("Unexpected AbsentInformationException while getting variable: " + varName);
        }
        if (varList == null) {
            throw new Failure("Can not find variable: " + varName);
        }
        if (varList.size() > 1) {
            throw new Failure("Found more than one variable with name : " + varName);
        }

        LocalVariable var = (LocalVariable) varList.get(0);
        return var;
    }
}
//--------------------------------------------------------- test specific classes
