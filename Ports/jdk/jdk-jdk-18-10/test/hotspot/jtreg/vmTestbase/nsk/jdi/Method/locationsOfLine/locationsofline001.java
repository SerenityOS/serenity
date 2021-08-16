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

package nsk.jdi.Method.locationsOfLine;

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
 * <code>com.sun.jdi.Method.locationsOfLine()</code>            <BR>
 * complies with its spec for non-abstract, non-native method.  <BR>
 * The test checks up that for each line in the List returned   <BR>
 * by the method Method.allLineLocations(), call to the method  <BR>
 * Method.locationsOfLine(lineNumber) returns                   <BR>
 *      non-empty List object in which                          <BR>
 *      each object is a Location object.                       <BR>
 * Not throwing  AbsentInformationException or                  <BR>
 *               InvalidLineNumberException                     <BR>
 * is checked up as well.                                       <BR>
 */

public class locationsofline001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/Method/locationsOfLine/locationsofline001  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new locationsofline001().runThis(argv, out);
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
        "nsk.jdi.Method.locationsOfLine.locationsofline001a";

    String mName = "nsk.jdi.Method.locationsOfLine";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine  vm = null;

    static int  testExitCode = PASSED;

    static final int returnCode0 = 0;
    static final int returnCode1 = 1;
    static final int returnCode2 = 2;
    static final int returnCode3 = 3;
    static final int returnCode4 = 4;

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

            List     methods   = null;
            Method   m         = null;
            List     locations = null;


            log2("      getting: List classes = vm.classesByName(mName + '.TestClass');");
            List classes = vm.classesByName(mName + ".TestClass");

            if (classes.size() != 1) {
                testExitCode = FAILED;
                log3("ERROR: classes.size() != 1");
                break ;
            }

            log2("      getting a tested method object 'm'");
            methods = ((ReferenceType) classes.get(0)).methodsByName("primitiveargsmethod");
            m = (Method) methods.get(0);


            log2("......locations = m.allLineLocations(); no AbsentInformationException is expected");
            try {
                locations = m.allLineLocations();
            } catch ( AbsentInformationException e ) {
                testExitCode = FAILED;
                log3("ERROR: AbsentInformationException");
                log3("       ATTENTION:     see the README file to this test");
                break ;
            }

            log2("......checking up on a value of locations.size(); 0 is not expected");
            int size = locations.size();
            if (size == 0) {
                testExitCode = FAILED;
                log3("ERROR: locations.size() == 0");
                break ;
            }

            ListIterator li1 = locations.listIterator();

            log2("      the loop of checking locations in the returned list; neither AbsentInformationException no InvalidLineNumberException is expected");

            label1:
                for (int ifor1 = 0; ifor1 < size; ifor1++) {

                    int lineNumber = ((Location) li1.next()).lineNumber();

                    try {
                        log2("......List lineLocations = m.locationsOfLine(" + lineNumber + ");");
                        List lineLocations = m.locationsOfLine(lineNumber);
                        int size1 = lineLocations.size();
                        if (size1 == 0) {
                            testExitCode = FAILED;
                            log3("ERROR: lineLocations.size() == 0");
                            break label1;
                        }
                        ListIterator li2 = lineLocations.listIterator();
                        try {
                            for (; li2.hasNext(); ) {
                                Location loc = (Location) li2.next();
                            }
                        } catch ( ClassCastException e) {
                             testExitCode = FAILED;
                             log3("ERROR: ClassCastException");
                             break label1;
                        }

                    } catch ( InvalidLineNumberException e1 ) {
                        testExitCode = FAILED;
                        log3("ERROR: InvalidLineNumberException");
                        break label1;
                    } catch ( AbsentInformationException e2 ) {
                        testExitCode = FAILED;
                        log3("ERROR: AbsentInformationException");
                        log3("       ATTENTION:     see the README file to this test");
                        break label1;
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
