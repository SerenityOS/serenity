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

package nsk.jdi.Method.hashCode;

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
 * <code>com.sun.jdi.Method.hashCode()</code>                   <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The cases for testing are as follows:                        <BR>
 *    two objects mirror the same method on the same VM         <BR>
 *    two objects mirror different methods on the same VM       <BR>
 * <BR>
 */

public class hashcode001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/Method/hashCode/hashcode001",
    sHeader2 = "--> hashcode001: ",
    sHeader3 = "##> hashcode001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new hashcode001().runThis(argv, out);
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
        "nsk.jdi.Method.hashCode.hashcode001a";

    String mName = "nsk.jdi.Method.hashCode";

    //====================================================== test program

    static ArgumentHandler      argsHandler;
    static int                  testExitCode = PASSED;

    //------------------------------------------------------ common section

    private int runThis (String argv[], PrintStream out) {

        Debugee debuggee1;
//        Debugee debuggee2;

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        if (argsHandler.verbose()) {
            debuggee1 = binder.bindToDebugee(debuggeeName + " -vbs");  // *** tp
//            debuggee2 = binder.bindToDebugee(debuggeeName + " -vbs");  // *** tp
        } else {
            debuggee1 = binder.bindToDebugee(debuggeeName);            // *** tp
//            debuggee2 = binder.bindToDebugee(debuggeeName);            // *** tp
        }

        IOPipe pipe1    = new IOPipe(debuggee1);
//        IOPipe pipe2  = new IOPipe(debuggee2);

        debuggee1.redirectStderr(out);
//        debuggee2.redirectStderr(out);
        log2("equals001a debuggees launched");
        debuggee1.resume();
//        debuggee2.resume();
//        log2("equals001a debuggees resumed");

        String line1 = pipe1.readln();
//        String line2 = pipe2.readln();


        if ((line1 == null) || !line1.equals("ready")) {
            log3("line1: signal received is not 'ready' but: " + line1);
            return FAILED;
        } else {
            log2("line1: 'ready' recieved");
        }
/*
        if ((line2 == null) || !line2.equals("ready")) {
            log3("line2: signal received is not 'ready' but: " + line2);
            return FAILED;
        } else {
            log2("line2: 'ready' recieved");
        }
*/
        VirtualMachine vm1 = debuggee1.VM();
//        VirtualMachine vm2 = debuggee2.VM();

    //------------------------------------------------------  testing section
        log1("      TESTING BEGINS");

//      for (int i = 0; ; i++) {

   check:
        {
            pipe1.println("newcheck");
            log2("line1: 'newcheck' sent");
            line1 = pipe1.readln();

//            if (line1.equals("checkend")) {
//                log2("     : line1: returned string is 'checkend'");
//            } else {

            if (!line1.equals("checkready")) {
                log3("ERROR: line1: returned string is not 'checkready'");
                testExitCode = FAILED;
                break check;
            }

/*
            pipe2.println("newcheck");
            log2("line2: 'newcheck' sent");
            line2 = pipe2.readln();

//            if (line2.equals("checkend")) {
//                log2("     : line2: returned string is 'checkend'");
//                break ;
//            } else {

            if (!line2.equals("checkready")) {
                log3("ERROR: line2: returned string is not 'checkready'");
                testExitCode = FAILED;
                break check;
            }
*/


            log1("check begins: ");

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            List listOfDebuggeeClasses1 = vm1.classesByName(mName + ".TestClass1");
                if (listOfDebuggeeClasses1.size() != 1) {
                    testExitCode = FAILED;
                    log3("ERROR: listOfDebuggeeClasses1.size() != 1");
                    break check;
                }
            List listOfDebuggeeClasses2 = vm1.classesByName(mName + ".TestClass2");
                if (listOfDebuggeeClasses2.size() != 1) {
                    testExitCode = FAILED;
                    log3("ERROR: listOfDebuggeeClasses2.size() != 1");
                    break check;
                }
/*
            List listOfDebuggeeClasses3 = vm2.classesByName(mName + ".TestClass1");
                if (listOfDebuggeeClasses3.size() != 1) {
                    testExitCode = FAILED;
                    log3("ERROR: listOfDebuggeeClasses3.size() != 1");
                    break check;
                }
*/
            List   methods  = null;
            Method m1       = null;
            Method m2       = null;
            Method m3       = null;
//          Method m4       = null;

            methods = ((ReferenceType) listOfDebuggeeClasses1.get(0)).
                methodsByName("primitiveargsmethod");
            m1 = (Method) methods.get(0);

            methods = ((ReferenceType) listOfDebuggeeClasses1.get(0)).
                methodsByName("primitiveargsmethod");
            m2 = (Method) methods.get(0);

            methods = ((ReferenceType) listOfDebuggeeClasses2.get(0)).
                methodsByName("arrayargmethod");
            m3 = (Method) methods.get(0);

/*
            methods = ((ReferenceType) listOfDebuggeeClasses3.get(0)).
                methodsByName("primitiveargsmethod");
            m4 = (Method) methods.get(0);
*/
            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0:                 // both m1 and m2 mirror the same method

                        if ( m1.hashCode() != m2.hashCode() ) {
                            log3("ERROR: m1.hashCode() != m2.hashCode()");
                            expresult = 1;
                            break;
                        }
                        break;

                case 1:                 // different classes on the same VM

                        if ( m1.hashCode() == m3.hashCode() ) {
                            log3("ERROR: m1.hashCode() == m3.hashCode()");
                            expresult = 1;
                            break;
                        }
                        break;
/*
                case 2:                 // different VMs

                        if ( m1.hashCode() != m4.hashCode() ) {
                            log3("ERROR: m1.hashCode() != m4.hashCode()");
                            expresult = 1;
                            break;
                        }
                        break;
*/

                default: expresult = 2;
                         break ;
                }

                if (expresult == 2) {
                    log2("      test cases finished");
                    break ;
                } else if (expresult == 1) {
                    log3("ERROR: expresult != true; " );
                    testExitCode = FAILED;
                }
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        }  //  check:

        log1("      TESTING ENDS");

    //--------------------------------------------------   test summary section
    //-------------------------------------------------    standard end section

        if (line1.equals("checkready")) {
            pipe1.println("quit");
            log2("waiting for the debuggee1 to finish ...");
            debuggee1.waitFor();

            int status1 = debuggee1.getStatus();
            if (status1 != PASSED + PASS_BASE) {
                log3("debuggee1 returned UNEXPECTED exit status: " +
                    status1 + " != PASS_BASE");
                testExitCode = FAILED;
            } else {
                log2("debuggee1 returned expected exit status: " +
                    status1 + " == PASS_BASE");
            }
        }
/*
        else {
            if (line2.equals("checkready")) {
                pipe2.println("quit");
                log2("waiting for the debuggee2 to finish ...");
                debuggee2.waitFor();

                int status2 = debuggee2.getStatus();
                if (status2 != PASSED + PASS_BASE) {
                    log3("debuggee2 returned UNEXPECTED exit status: " +
                        status2 + " != PASS_BASE");
                    testExitCode = FAILED;
                } else {
                    log2("debuggee2 returned expected exit status: " +
                        status2 + " == PASS_BASE");
                }
            }
        }
*/

        if (testExitCode != PASSED) {
            logHandler.complain("TEST FAILED");
        }
        return testExitCode;
    }
}
