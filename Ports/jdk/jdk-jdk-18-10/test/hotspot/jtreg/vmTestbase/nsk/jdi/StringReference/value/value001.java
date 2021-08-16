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

package nsk.jdi.StringReference.value;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * StringReference.                                             <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.StringReference.value()</code>             <BR>
 * complies with its spec regarding the following requirement:  <BR>
 * "Returns the StringReference as a String.                    <BR>
 * The returned string is the equivalent of the mirrored string,<BR>
 * but is an entity in the client VM and can be manipulated     <BR>
 * like any other string."                                      <BR>
 * <BR>
 * The test checks up that a returned string is the equivalent  <BR>
 * of the mirrored string.                                      <BR>
 * <BR>
 */

public class value001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/StringReference/value/value001",
    sHeader2 = "--> value001: ",
    sHeader3 = "##> value001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new value001().runThis(argv, out);
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
        "nsk.jdi.StringReference.value.value001a";

    //====================================================== test program

    static ArgumentHandler      argsHandler;
    static int                  testExitCode = PASSED;

    static StringReference      str_ref = null;

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
        log2("value001a debuggee launched");
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

            String classForCheckName =
                                "nsk.jdi.StringReference.value.ClassForCheck";

            List listOfDebuggeeLoadedClasses =
                        vm.classesByName(classForCheckName);
/*
            if (listOfDebuggeeLoadedClasses.size() != 1) {
                testExitCode = FAILED;
                log3("ERROR: listOfDebuggeeLoadedClasses.size() != 1    " +
                        listOfDebuggeeLoadedClasses.size());
                break ;
            }
*/
            ReferenceType testClass =
                        (ReferenceType) listOfDebuggeeLoadedClasses.get(0);

            Field fstr = null;

            try {
                fstr = testClass.fieldByName("str");
            } catch ( ClassNotPreparedException e) {
                testExitCode = FAILED;
                log3("ERROR: 'fstr = testClass.fieldByName' throws " +
                      "ClassNotPreparedException");
                break ;
            } catch ( ObjectCollectedException e) {
                testExitCode = FAILED;
                log3("ERROR: 'fstr = testClass.fieldByName' throws " +
                      "ObjectCollectedException");
                break ;
            }

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0:
                        try {
                            str_ref = (StringReference) testClass.getValue(fstr);
                            log2("     : 1st str_ref = testClass.getValue(fstr)  " +
                                 "doesn't throws ObjectCollectedException");
                            if (str_ref.value().compareTo("abc") != 0) {
                                log3("ERROR: strings are not equal");
                                expresult = 1;
                                break ;
                            }
                        } catch ( ObjectCollectedException e ) {
                            log3("ERROR: 1st str_ref = testClass.getValue(fstr)  " +
                                 "does throws ObjectCollectedException");
                            expresult = 1;
                            break ;
                        }

                        pipe.println("continue");
                        line = pipe.readln();
                        if (!line.equals("docontinue")) {
                            log3("ERROR: debuggee's reply is not 'docontinue'");
                            expresult = 1;
                            break ;
                        }

                        try {
                            str_ref = (StringReference) testClass.getValue(fstr);
                            log2("     : 2nd str_ref = testClass.getValue(fstr)  " +
                                 "doesn't throws ObjectCollectedException");
                        } catch ( ObjectCollectedException e ) {
                            log3("ERROR: 2nd str_ref = testClass.getValue(fstr)  " +
                                 "does throws ObjectCollectedException");
                            expresult = 1;
                        }
                        break ;


                default: expresult = 2;
                         break ;
                }

                if (expresult == 2) {
                    log2("      test cases finished");
                    break ;
                } else if (expresult == 1) {
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
