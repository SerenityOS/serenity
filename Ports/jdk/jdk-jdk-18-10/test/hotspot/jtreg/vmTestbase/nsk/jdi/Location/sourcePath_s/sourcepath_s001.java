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

package nsk.jdi.Location.sourcePath_s;

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
 * <code>com.sun.jdi.Location.sourcePath(String stratum)</code> <BR>
 * complies with its specification.                             <BR>
 * <BR>
 * The test checks that                                         <BR>
 * - invocation of  the method doesn't throw                    <BR>
 *   an error or unspecified exception;                         <BR>
 * - returned value is debuggee's                               <BR>
 *   "unqualified name of the source file for this Location".   <BR>
 * <BR>
 */

public class sourcepath_s001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/Location/sourcePath_s/sourcepath_s001 ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        return new sourcepath_s001().runThis(argv, out);
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
        "nsk.jdi.Location.sourcePath_s.sourcepath_s001a";

    String mName = "nsk.jdi.Location.sourcePath_s";

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

            String testedclassName = mName + ".TestClass";

            log2("       getting: List classes = vm.classesByName(testedclassName); expected size == 1");
            List classes = vm.classesByName(testedclassName);
            int size = classes.size();
            if (size != 1) {
                log3("ERROR: classes.size() != 1 : " + size);
                testExitCode = FAILED;
                break ;
            }

            log2("      getting: TestClass.allLineLocations(); no AbsentInformationException expected");
            List lineLocations = null;
            try {
                lineLocations = ((ReferenceType) classes.get(0)).allLineLocations();
            } catch ( AbsentInformationException e) {
                log3("ERROR: AbsentInformationException");
                testExitCode = FAILED;
                break;
            }
            size = lineLocations.size();
            if (size == 0) {
                log3("ERROR: lineLocations.size() == 0");
                testExitCode = FAILED;
                break;
            }

            log2("...... getting : String defaultStratum = vm.getDefaultStratum();");
            String defaultStratum = vm.getDefaultStratum();

            log2("...... getting : ListIterator li = lineLocations.listIterator();");
            ListIterator li = lineLocations.listIterator();

            log2("...... getting : Location loc = (Location) li.next();");
            Location loc = (Location) li.next();

            String sep = System.getProperty("file.separator");
            String debuggeeUnqualifiedSourcePath = "nsk" + sep + "jdi" +sep + "Location" + sep +
                                                  "sourcePath_s" + sep + "sourcepath_s001a.java";

            log2("......getting: String str = loc.sourcePath(defaultStratum)");
            try {
                String str = loc.sourcePath(defaultStratum);

                log2("......compareing: loc.sourcePath(defaultStratum) to debuggeeUnqualifiedSourcePath");
                if (!str.equals(debuggeeUnqualifiedSourcePath)) {
                    log3("ERROR: loc.sourcePath(defaultStratum) != debuggeeUnqualifiedSourcePath");
                    log2("        loc.sourcePath(defaultStratum) == " + str);
                    log2("        debuggeeUnqualifiedSourcePath  == " + debuggeeUnqualifiedSourcePath);
                    testExitCode = FAILED;
                }
            } catch ( AbsentInformationException e ) {
                log3("ERROR: AbsentInformationException");
                testExitCode = FAILED;
            } catch ( Throwable err ) {
                log3("ERROR: Throwable : " + err);
                testExitCode = FAILED;
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
