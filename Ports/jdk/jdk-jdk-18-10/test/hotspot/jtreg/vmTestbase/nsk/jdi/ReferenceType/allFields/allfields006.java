/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ReferenceType.allFields;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;

import java.io.*;
import java.util.*;

/**
 * The debugger application of the test.
 */
public class allfields006 {

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

    private final static String prefix = "nsk.jdi.ReferenceType.allFields.";
    private final static String className = "allfields006";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields

    private final static String expectedFieldName = "f1";
    private final static String[][] expectedEnumFields = {
        { "e1", debuggerName + "Enum1", null },
        { "e2", debuggerName + "Enum1", null },
        { "i1", debuggerName + "i1", null },
        { "i1", debuggerName + "i2", null }  };

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
        check();
        display("Checking completed!");
    }

    //--------------------------------------------------------- test specific methods

    private static void check () {
        try {
            ClassType checkedClass = (ClassType)debuggeeClass.fieldByName("f1").type();
            String className = checkedClass.name();

            List<Field> l = checkedClass.allFields();
            if (l.isEmpty()) {
                complain("\t ReferenceType.allFields() returned empty list for type: " + className);
                exitStatus = Consts.TEST_FAILED;
            } else {

                for (int i = 0; i < expectedEnumFields.length; i++) {
                    Iterator<Field> it = l.iterator();
                    while (it.hasNext()) {
                        Field checkedField = it.next();
                        if (expectedEnumFields[i][0].equals(checkedField.name()) &&
                            checkedField.declaringType().name().equals(expectedEnumFields[i][1]) )

                            expectedEnumFields[i][2] = "found";
                    }
                }

                for (int i = 0; i < expectedEnumFields.length; i++) {
                    if (expectedEnumFields[i][2] != null) {
                        display("enum " + className + " has field " + expectedEnumFields[i][0]);
                        display("\t of type " + expectedEnumFields[i][1]);
                    } else {
                        complain("enum " + className + " does not have field " + expectedEnumFields[i][0]);
                        complain("\t of type " + expectedEnumFields[i][1]);
                        exitStatus = Consts.TEST_FAILED;
                    }
                }
            }
        } catch (Exception e) {
            complain("Unexpected exception while checking of " + className + ": " + e);
            e.printStackTrace(System.out);
            exitStatus = Consts.TEST_FAILED;
        }
    }
}
//--------------------------------------------------------- test specific classes
