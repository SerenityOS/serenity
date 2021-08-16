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

package nsk.jdi.Method.equals;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * Method.                                                      <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.Method.equals()</code>                     <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The cases for testing are as follows:                        <BR>
 *    two objects mirror the same method on the same VM         <BR>
 *    two objects mirror different methods on the same VM       <BR>
 * <BR>
 */

public class equals001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/Method/equals/equals001",
    sHeader2 = "--> equals001: ",
    sHeader3 = "##> equals001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new equals001().runThis(argv, out);
    }

     //--------------------------------------------------   log procedures

    //private static boolean verbMode = false;

    private static Log  logHandler;

    private static void log1(String message) {
        logHandler.display(sHeader1 + message);
    }
    private static void log2(String message) {
        logHandler.display(sHeader2 + message);
    }
    private static void log3(String message) {
        logHandler.complain(sHeader3 + message);
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.Method.equals.equals001a";

    String mName = "nsk.jdi.Method.equals";

    //====================================================== test program

    static ArgumentHandler      argsHandler;
    static int                  testExitCode = PASSED;

    //------------------------------------------------------ common section

    private int runThis (String argv[], PrintStream out) {

        Debugee debuggee;

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        if (argsHandler.verbose()) {
            debuggee = binder.bindToDebugee(debuggeeName + " -vbs");  // *** tp
        } else {
            debuggee = binder.bindToDebugee(debuggeeName);            // *** tp
        }

        IOPipe pipe     = new IOPipe(debuggee);

        debuggee.redirectStderr(out);
        log2("equals001a debuggees launched");
        debuggee.resume();
        log2("equals001a debuggees resumed");

        String line = pipe.readln();

        if ((line == null) || !line.equals("ready")) {
            log3("line: signal received is not 'ready' but: " + line);
            return FAILED;
        } else {
            log2("line: 'ready' recieved");
        }

        VirtualMachine vm = debuggee.VM();

    //------------------------------------------------------  testing section
        log1("      TESTING BEGINS");

        for (int i = 0; ; i++) {
        pipe.println("newcheck");
        log2("line: 'newcheck' sent");

            line = pipe.readln();
            if (line.equals("checkend")) {
                log2("     : line: returned string is 'checkend'");
                break ;
            } else if (!line.equals("checkready")) {
                log3("ERROR: line: returned string is not 'checkready'");
                testExitCode = FAILED;
//                break ;
            }

            log1("new check: #" + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            List listOfDebuggeeClasses1 = vm.classesByName(mName + ".TestClass1");
                if (listOfDebuggeeClasses1.size() != 1) {
                    testExitCode = FAILED;
                    log3("ERROR: listOfDebuggeeClasses1.size() != 1");
                    break ;
                }
            List listOfDebuggeeClasses2 = vm.classesByName(mName + ".TestClass2");
                if (listOfDebuggeeClasses2.size() != 1) {
                    testExitCode = FAILED;
                    log3("ERROR: listOfDebuggeeClasses2.size() != 1");
                    break ;
                }

            List   methods  = null;
            Method m1       = null;
            Method m2       = null;
            Method m3       = null;

            methods = ((ReferenceType) listOfDebuggeeClasses1.get(0)).
                methodsByName("primitiveargsmethod");
            m1 = (Method) methods.get(0);

            methods = ((ReferenceType) listOfDebuggeeClasses1.get(0)).
                methodsByName("primitiveargsmethod");
            m2 = (Method) methods.get(0);

            methods = ((ReferenceType) listOfDebuggeeClasses2.get(0)).
                methodsByName("arrayargmethod");
            m3 = (Method) methods.get(0);

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0:                 // both m1 and m2 mirror the same method

                        if (!m1.equals(m2)) {
                            log3("ERROR: !m1.equals(m2)");
                            expresult = 1;
                            break;
                        }
                        break;

                case 1:                 // different classes on the same VM

                        if (m1.equals(m3)) {
                            log3("ERROR: m1.equals(m3)");
                            expresult = 1;
                            break;
                        }
                        break;

                default: expresult = 2;
                         break ;
                }

                if (expresult == 2) {
                    log2("      test cases finished");
                    break ;
                } else if (expresult == 1) {
                    log3("ERROR: expresult != true;  check # = " + i);
                    testExitCode = FAILED;
                }
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        }
        log1("      TESTING ENDS");

    //--------------------------------------------------   test summary section
    //-------------------------------------------------    standard end section

        pipe.println("quit");
        log2("waiting for the debuggee to finish ...");
        debuggee.waitFor();

        int status = debuggee.getStatus();
        if (status != PASSED + PASS_BASE) {
            log3("debuggee returned UNEXPECTED exit status: " +
                    status + " != PASS_BASE");
            testExitCode = FAILED;
        } else {
            log2("debuggee returned expected exit status: " +
                    status + " == PASS_BASE");
        }

        if (testExitCode != PASSED) {
            logHandler.complain("TEST FAILED");
        }
        return testExitCode;
    }
}
