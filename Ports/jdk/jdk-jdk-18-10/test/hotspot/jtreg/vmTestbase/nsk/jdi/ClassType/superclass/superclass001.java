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

package nsk.jdi.ClassType.superclass;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * ClassType.                                                   <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ClassType.superclass()</code>              <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The cases for testing are as follows.        <BR>
 *                                              <BR>
 * 1)    Class2 extends Class1                  <BR>
 *       hence Class1 is superclass of Class2   <BR>
 * 2)    Class2 extends Class1                  <BR>
 *       Class3 extends Class2                  <BR>
 *       hence Class2 is superclass of Class3   <BR>
 * 3)    Class1                                 <BR>
 *       hence Object is superclass of Class1   <BR>
 * <BR>
 */

public class superclass001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ClassType/superclass/superclass001",
    sHeader2 = "--> superclass001: ",
    sHeader3 = "##> superclass001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new superclass001().runThis(argv, out);
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
        "nsk.jdi.ClassType.superclass.superclass001a";

    String mName = "nsk.jdi.ClassType.superclass";

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
        log2("superclass001a debuggee launched");
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

            List list1 = vm.classesByName(mName + ".Class1ForCheck");
            List list2 = vm.classesByName(mName + ".Class2ForCheck");
            List list3 = vm.classesByName(mName + ".Class3ForCheck");

            ClassType  superclass0   = null;
            ClassType  superclass1   = null;
            ClassType  superclass2   = null;

            ReferenceType classRefType = null;

            String name = null;

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0:         // Class2 extends Class1

                        classRefType = (ReferenceType) list2.get(0);
                        superclass1   = ((ClassType) classRefType).superclass();

                        if (superclass1 == null) {
                            log3("ERROR : superclass1 == null in case: Class2 extends Class1");
                            expresult = 1;
                            break;
                        }

                        name = superclass1.name();
                        if (!name.equals(mName + ".Class1ForCheck")) {
                            log3("ERROR : name != 'Class1ForCheck' in Class2 extends Class1");
                            expresult = 1;
                            break;
                        }

                        break;

                case 1:         // Class3 extends Class2

                        classRefType = (ReferenceType) list3.get(0);
                        superclass2   = ((ClassType) classRefType).superclass();

                        if (superclass2 == null) {
                            log3("ERROR : superclass2 == null in case: Class3 extends Class2");
                            expresult = 1;
                            break;
                        }

                        name = superclass2.name();
                        if (!name.equals(mName + ".Class2ForCheck")) {
                            log3("ERROR : name != 'Class2ForCheck' in Class3 extends Class2");
                            expresult = 1;
                            break;
                        }

                        break;

                case 2:         // Class1

                        classRefType = (ReferenceType) list1.get(0);

                        superclass0   = ((ClassType) classRefType).superclass();

                        if (superclass0 == null) {
                            log3("ERROR : superclass2 == null in case: Class1");
                            expresult = 1;
                            break;
                        }

                        name = superclass0.name();
                        if (!name.equals("java.lang.Object")) {
                            log3("ERROR : name != 'java.lang.Object' in Class1");
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
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }
}
