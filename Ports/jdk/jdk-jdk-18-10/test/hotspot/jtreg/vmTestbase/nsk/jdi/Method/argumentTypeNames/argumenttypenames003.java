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

package nsk.jdi.Method.argumentTypeNames;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;

import java.io.*;
import java.util.*;

/**
 * The debugger application of the test.
 */
public class argumenttypenames003 {

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

    private final static String prefix = "nsk.jdi.Method.argumentTypeNames.";
    private final static String className = "argumenttypenames003";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields
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

    private static void failure(String msg) {
        complain(msg);
        exitStatus = Consts.TEST_FAILED;
    }

    public static int run(String argv[], PrintStream out) {
        exitStatus = Consts.TEST_PASSED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        debuggee = Debugee.prepareDebugee(argHandler, log, debuggeeName);
        debuggeeClass = debuggee.classByName(debuggeeName);
        if ( debuggeeClass == null ) {
            failure("Class '" + debuggeeName + "' not found.");
        }

        execTest();
        debuggee.quit();
        return exitStatus;
    }

    //------------------------------------------------------ mutable common method

    private static void execTest() {
        String expMethodName = "foo";
        String[][] expSignatures = { /* signature of method                                         varargs type name   */
                                      {"([I)Ljava/lang/String;",                                    "int[]"},
                                      {"([Ljava/lang/Integer;)Ljava/lang/String;",                  "java.lang.Integer[]"},
                                      {"(Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;", "java.lang.String[]"}
                                   };

        for (int i = 0; i < expSignatures.length; i++) {
            List<Method> methods = debuggeeClass.methodsByName(expMethodName);
            Method method = ((ClassType)debuggeeClass).concreteMethodByName(expMethodName, expSignatures[i][0]);
            if (method != null) {
                try {
                    List<String> typeNames = method.argumentTypeNames();
                    if (typeNames.contains(expSignatures[i][1])) {
                        display("Method.argumentTypeNames() returned expected varargs type name " + expSignatures[i][1]);
                    } else {
                        failure("Method.argumentTypeNames() did not return expected varargs type name " + expSignatures[i][1]);
                    }

                } catch (Exception e) {
                    failure("Unexpected exception: " + e);
                    e.printStackTrace(System.out);
                }
            } else {
                failure("Cannot find method with signature: " + expSignatures[i][0]);
            }
        }
        display("Checking completed!");
    }

    //--------------------------------------------------------- test specific methods
}
//--------------------------------------------------------- test specific classes
