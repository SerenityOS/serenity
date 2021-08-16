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

package nsk.jdi.ThreadGroupReference.toString;

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

    private final static String prefix = "nsk.jdi.ThreadGroupReference.toString.";
    private final static String className = "tostring001";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields

    private final static String testedClassName = prefix + "tostring001aThread";
    private final static String mainGroup = "mainGroup";
    private final static String thread2Group = "thread2Group";

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

        checkToString (debuggeeName, mainGroup);

        checkToString (testedClassName, thread2Group);

        display("Checking completed!");
    }

    //--------------------------------------------------------- test specific methods

    private static Method methodByName (ReferenceType refType, String methodName) {
        List methodList = refType.methodsByName(methodName);
        if (methodList == null) return null;

        Method method = (Method) methodList.get(0);
        return method;
    }

    private static void checkToString (String className, String fieldName) {
        display("Checking toString() method for " +
           "\n\tclass: " + className +
           "\n\tthread group: " + fieldName );

        ReferenceType testedClass = debuggee.classByName(className);

        ThreadGroupReference threadGroupRef = null;
        try {
            threadGroupRef = (ThreadGroupReference)testedClass.getValue(testedClass.fieldByName(fieldName));
        } catch (Exception e) {
            throw new Failure("Unexpected exception while getting ThreadGroupReference for " + fieldName + " : " + e.getMessage() );
        }

        String str = threadGroupRef.toString();
        if (str == null) {
            complain("toString() returns null for " + threadGroupRef.name());
            exitStatus = Consts.TEST_FAILED;
        } else if (str.length() == 0) {
            complain("toString() returns empty string for for " + threadGroupRef.name());
            exitStatus = Consts.TEST_FAILED;
        } else {
            display("toString() returns for " + threadGroupRef.name() + " : " + str);
        }
    }
}
//--------------------------------------------------------- test specific classes
