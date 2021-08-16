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

package nsk.jdi.Method.argumentTypeNames;

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
 * <code>com.sun.jdi.Method.argumentTypeNames()</code>          <BR>
 * complies with its spec when a type is one of PrimitiveTypes. <BR>
 * <BR>
 * The cases for testing are as follows.                <BR>
 *                                                      <BR>
 * When a gebuggee creates an object of                 <BR>
 * the following class type:                            <BR>
 *    class TestClass {                                 <BR>
 *        public void primitiveargsmethod (             <BR>
 *            boolean bl,                               <BR>
 *            byte    bt,                               <BR>
 *            char    ch,                               <BR>
 *            double  db,                               <BR>
 *            float   fl,                               <BR>
 *            int     in,                               <BR>
 *            long    l,                                <BR>
 *            short   sh ) {                            <BR>
 *                  return ;                            <BR>
 *        }                                             <BR>
 * }                                                    <BR>
 * for all of the above primitive type arguments,       <BR>
 * a debugger forms text strings with corresponding     <BR>
 * types, that is 'boolean', 'byte', 'char', and etc.   <BR>
 * <BR>
 */

public class argumenttypenames001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/Method/argumentTypeNames/argumenttypenames001",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new argumenttypenames001().runThis(argv, out);
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
        "nsk.jdi.Method.argumentTypeNames.argumenttypenames001a";

    String mName = "nsk.jdi.Method.argumentTypeNames";

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
        log2("argumenttypenames001a debuggee launched");
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

            List listOfDebuggeeClasses = vm.classesByName(mName + ".argumenttypenames001aTestClass");
                if (listOfDebuggeeClasses.size() != 1) {
                    testExitCode = FAILED;
                    log3("ERROR: listOfDebuggeeClasses.size() != 1");
                    break ;
                }

            List   methods  = null;
            Method m        = null;

            List   argTypeNames = null;


            methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("primitiveargsmethod");
            m = (Method) methods.get(0);
            argTypeNames = m.argumentTypeNames();

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0:                 // boolean arg

                        if (!argTypeNames.contains("boolean")) {
                            log3("ERROR: !argTypes.contains('boolean')");
                            expresult = 1;
                            break;
                        }
                        break;

                case 1:                 // byte arg

                        if (!argTypeNames.contains("byte")) {
                            log3("ERROR: !argTypes.contains('byte')");
                            expresult = 1;
                            break;
                        }
                        break;

                case 2:                 // char arg

                        if (!argTypeNames.contains("char")) {
                            log3("ERROR: !argTypes.contains('char')");
                            expresult = 1;
                            break;
                        }
                        break;

                case 3:                 // double arg

                        if (!argTypeNames.contains("double")) {
                            log3("ERROR: !argTypes.contains('double')");
                            expresult = 1;
                            break;
                        }
                        break;

                case 4:                 // float arg

                        if (!argTypeNames.contains("float")) {
                            log3("ERROR: !argTypes.contains('float')");
                            expresult = 1;
                            break;
                        }
                        break;
                case 5:                 // int arg

                        if (!argTypeNames.contains("int")) {
                            log3("ERROR: !argTypes.contains('int')");
                            expresult = 1;
                            break;
                        }
                        break;

                case 6:                 // long arg

                        if (!argTypeNames.contains("long")) {
                            log3("ERROR: !argTypes.contains('long')");
                            expresult = 1;
                            break;
                        }
                        break;

                case 7:                 // short arg

                        if (!argTypeNames.contains("short")) {
                            log3("ERROR: !argTypes.contains('short')");
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
