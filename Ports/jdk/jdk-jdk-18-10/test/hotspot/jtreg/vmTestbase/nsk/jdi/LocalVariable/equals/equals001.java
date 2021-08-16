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

package nsk.jdi.LocalVariable.equals;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * LocalVariable.                                               <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.LocalVariable.equals()</code>              <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The cases for testing are as follows.                <BR>
 *                                                      <BR>
 * When a gebuggee creates an object containing two     <BR>
 * methods with equal sets of variables and arguments,  <BR>
 * a debugger creates following LocalVariable objects:  <BR>
 * - two for the same variable in the debuggee;         <BR>
 * - for "the same type - different name" variable in   <BR>
 *   the same method in the debuggee;                   <BR>
 * - for "same name-type - different methods" variable; <BR>
 *                                                      <BR>
 * and applies the method LocalVariable.equals()        <BR>
 * to pairs of the above Localvariables to              <BR>
 * check up that the method returns the follwing values:<BR>
 * - true if two LocalVariable mirror the same variable <BR>
 *   or argument in the same method in the debuggee;    <BR>
 * - false otherwise.                                   <BR>
 * <BR>
 */

public class equals001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/LocalVariable/equals/equals001  ",
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
        "nsk.jdi.LocalVariable.equals.equals001a";

    String mName = "nsk.jdi.LocalVariable.equals";

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
        log2("equals001a debuggee launched");
        debuggee.resume();

        String line = pipe.readln();
        if ((line == null) || !line.equals("ready")) {
            log3("signal received is not 'ready' but: " + line);
            return FAILED;
        } else {
            log2("'ready' recieved");
        }

        VirtualMachine vm = debuggee.VM();

    //------------------------------------------------------  testing section
        log1("      TESTING BEGINS");

        for (int i = 0; ; i++) {
        pipe.println("newcheck");
            line = pipe.readln();

            if (line.equals("checkend")) {
                log2("     : returned string is 'checkend'");
                break ;
            } else if (!line.equals("checkready")) {
                log3("ERROR: returned string is not 'checkready'");
                testExitCode = FAILED;
                break ;
            }

            log1("new check: #" + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            List listOfLoadedClasses = vm.classesByName(mName + ".TestClass");

            if (listOfLoadedClasses.size() != 1) {
                testExitCode = FAILED;
                log3("ERROR: listOfLoadedClasses.size() != 1   " +
                     listOfLoadedClasses.size());
                break ;
            }

            List methods1 =
                ((ReferenceType) listOfLoadedClasses.get(0)).
                                 methodsByName("testmethod1");
            List methods2 =
                ((ReferenceType) listOfLoadedClasses.get(0)).
                                 methodsByName("testmethod2");

            Method testMethod1 = (Method) methods1.get(0);
            Method testMethod2 = (Method) methods2.get(0);

            String names1[] = { "bl1", "bt1", "ch1", "db1",
                                "fl1", "in1", "ln1", "sh1",
                                "class2_1",   "iface_1", "cfc_1", "param1" };
            String names2[] = { "bl2", "bt2", "ch2", "db2",
                                "fl2", "in2", "ln2", "sh2",
                                "class2_2",   "iface_2", "cfc_2", "param2" };

            int i2;

            for (i2 = 0; i2 < names1.length; i2++) {

                log2("new check: #" + i2);

                List lVars1 = null;
                List lVars2 = null;
                List lVars3 = null;
                List lVars4 = null;

                try {
                    lVars1 = testMethod1.variablesByName(names1[i2]);
                    lVars2 = testMethod1.variablesByName(names1[i2]);
                    lVars3 = testMethod1.variablesByName(names2[i2]);
                    lVars4 = testMethod2.variablesByName(names1[i2]);
                } catch ( AbsentInformationException e ) {
                    log3("ERROR: AbsentInformationException for " +
                         "lVars = testMethod_i.variablesByName(names[i2])" );
                    testExitCode = FAILED;
                    continue;
                }
                if (lVars1.size() != 1 || lVars2.size() != 1 ||
                    lVars3.size() != 1 || lVars4.size() != 1 ) {
                    testExitCode = FAILED;
                    log3("ERROR: lVars-i.size() != 1 for i2= " + i2);
                    continue;
                }

                LocalVariable lVar1 = (LocalVariable) lVars1.get(0);
                LocalVariable lVar2 = (LocalVariable) lVars2.get(0);
                LocalVariable lVar3 = (LocalVariable) lVars3.get(0);
                LocalVariable lVar4 = (LocalVariable) lVars4.get(0);


                if (!lVar1.equals(lVar2)) {
                    testExitCode = FAILED;
                    log3("ERROR: !lVar1.equals(lvar2) for check# " + i2);
                }
                if (lVar1.equals(lVar3)) {
                    testExitCode = FAILED;
                    log3("ERROR: lVar1.equals(lvar3) for check# " + i2);
                    }
                if (lVar1.equals(lVar4)) {
                    testExitCode = FAILED;
                    log3("ERROR: lVar1.equals(lvar4) for check# " + i2);
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
