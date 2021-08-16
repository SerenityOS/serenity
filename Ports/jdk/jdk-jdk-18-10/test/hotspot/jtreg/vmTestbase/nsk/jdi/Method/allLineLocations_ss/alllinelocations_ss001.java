/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.Method.allLineLocations_ss;

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
 * <code>com.sun.jdi.Method.allLineLocations(String,String)</code>      <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The case for testing includes only non-abstract, non-native  <BR>
 * method and defaultStratum.                                   <BR>
 * The test checks up that a list of locations for the method   <BR>
 *    - is not empty and                                        <BR>
 *    - is ordered in a proper, from low to high, sequence.     <BR>
 */

public class alllinelocations_ss001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/Method/allLineLocations_ss/alllinelocations_ss001 ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        return new alllinelocations_ss001().runThis(argv, out);
    }

    //--------------------------------------------------   log procedures

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
        "nsk.jdi.Method.allLineLocations_ss.alllinelocations_ss001a";

    String mName = "nsk.jdi.Method.allLineLocations_ss";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine vm = null;

    static int  testExitCode = PASSED;

    //------------------------------------------------------ methods

    private int runThis (String argv[], PrintStream out) {

        Debugee debuggee;

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        if (argsHandler.verbose()) {
            debuggee = binder.bindToDebugee(debuggeeName + " -vbs");
        } else {
            debuggee = binder.bindToDebugee(debuggeeName);
        }

        waitTime = argsHandler.getWaitTime();


        IOPipe pipe     = new IOPipe(debuggee);

        debuggee.redirectStderr(out);
        log2(debuggeeName + " debuggee launched");
        debuggee.resume();

        String line = pipe.readln();
        if ((line == null) || !line.equals("ready")) {
            log3("signal received is not 'ready' but: " + line);
            return FAILED;
        } else {
            log2("'ready' recieved");
        }

        vm = debuggee.VM();

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

            log1("new checkready: #" + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            List   methods   = null;
            Method m         = null;
            List   locations = null;


            log2("      getting: List classes = vm.classesByName(mName + '.TestClass');");
            List classes = vm.classesByName(mName + ".TestClass");

            if (classes.size() != 1) {
                testExitCode = FAILED;
                log3("ERROR: classes.size() != 1");
                break ;
            }

            log2("...... getting : String defaultStratum = vm.getDefaultStratum();");
            String defaultStratum = vm.getDefaultStratum();


            log2("      getting a tested Method object 'm'");
            methods = ((ReferenceType) classes.get(0)).methodsByName("primitiveargsmethod");
            m = (Method) methods.get(0);

            log2("......locations = m.allLineLocations(defaultStratum, null);");
            log2("                  no AbsentInformationException is expected");
            try {
                locations = m.allLineLocations(defaultStratum, null);
            } catch ( AbsentInformationException e ) {
                testExitCode = FAILED;
                log3("ERROR: AbsentInformationException");
                log3("       ATTENTION:     see the README file to this test");
                break ;
            }

            log2("......checking up on a value of locations.size(); 0 is not expected");
            if (locations.size() == 0) {
                testExitCode = FAILED;
                log3("ERROR: locations.size() == 0");
                break ;
            }

            log2("......checking up on element order in the List");

            ListIterator listIterator = locations.listIterator();

            Location location1 = null;
            Location location2 = null;

            int i2;

            for (i2 = 0; listIterator.hasNext(); i2++) {

                long codeIndex1 = 0l;
                long codeIndex2 = 0l;

                try {
                    location2 = (Location) listIterator.next();
                } catch ( ClassCastException e) {
                    testExitCode = FAILED;
                    log3("ERROR: ClassCastException");
                    break ;
                }
                codeIndex2 = location2.codeIndex();
                if (i2 == 0) {
                    continue;
                }

                if (codeIndex2 < codeIndex1) {
                    testExitCode = FAILED;
                    log3("ERROR: codeIndex disorder: codeIndex2 < codeIndex1");
                    break ;
                }

                codeIndex1 = codeIndex2;
            }

            log2("......compareing locations.size() to a processed number of elements in the list");
            if (locations.size() != i2) {
                testExitCode = FAILED;
                log3("ERROR: locations.size() != the processed number");
                break ;
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
