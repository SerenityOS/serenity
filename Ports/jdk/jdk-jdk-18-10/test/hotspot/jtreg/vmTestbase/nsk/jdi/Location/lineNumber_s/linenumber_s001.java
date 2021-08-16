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

package nsk.jdi.Location.lineNumber_s;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * Location.                                                    <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.Location.lineNumber(String stratum)</code> <BR>
 * complies with its spec.                                      <BR>
 * Case for testing includes only a List of locations returned  <BR>
 * by the method ReferenceType.allLineLocations() applied to    <BR>
 * a class with the "Java" stratum which is the default one.    <BR>
 */

public class linenumber_s001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/Location/lineNumber_s/linenumber_s001 ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        return new linenumber_s001().runThis(argv, out);
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
        "nsk.jdi.Location.lineNumber_s.linenumber_s001a";

    String mName = "nsk.jdi.Location.lineNumber_s";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine vm = null;;

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

            String className = mName + ".TestClass";

            log2("      getting: List of loaded 'TestClass' classes; only one element is expected");
            List listOfLoadedClasses = vm.classesByName(className);

            int size = listOfLoadedClasses.size();
            if (size != 1) {
                log3("ERROR: listOfLoadedClasses.size() != 1 : " + size);
                testExitCode = FAILED;
                break ;
            }

            ReferenceType testedClass = (ReferenceType) listOfLoadedClasses.get(0);

            log2("      getting: List of TestClass lineLocations; AbsentInformationException is not expected");
            List lineLocations = null;
            try {
                lineLocations = testedClass.allLineLocations();
            } catch ( AbsentInformationException e) {
                log3("ERROR: AbsentInformationException");
                testExitCode = FAILED;
                break;
            }
            size = lineLocations.size();
            if (size == 0) {
                log3("ERROR: lineLocations.size() == 0 : " + size);
                testExitCode = FAILED;
                break ;
            }

            log2("      getting: min and max line numbers of TestClass");
            int minLine;
            int maxLine;
            try {
                minLine = ( (IntegerValue) testedClass.getValue(testedClass.fieldByName("minLine")) ).value();
                maxLine = ( (IntegerValue) testedClass.getValue(testedClass.fieldByName("maxLine")) ).value();
            } catch ( ClassCastException e ) {
                log3("ERROR: ClassCastException");
                testExitCode = FAILED;
                break;
            }

            String defaultStratum = vm.getDefaultStratum();

            log2("      loop for checking up lineNumbers in each element in lineLocations");
            ListIterator li = lineLocations.listIterator();

            for (int ifor = 0; li.hasNext(); ifor++) {
                int lineNumber = ((Location) li.next()).lineNumber(defaultStratum);
                if ( lineNumber < minLine || lineNumber > maxLine ) {
                    log3("ERROR: lineNumber  is out of range minLine-maxLine: ");
                    log2("         minLine == " + minLine + "; lineNumber == " + lineNumber + "; maxLine == " + maxLine);
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
