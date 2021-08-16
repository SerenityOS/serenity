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

package nsk.jdi.VirtualMachine.canGetSyntheticAttribute;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * VirtualMachine.                                              <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.VirtualMachine.canGetSyntheticAttribute()</code>   <BR>
 * complies with its spec.                                              <BR>
 * <BR>
 * The test works as follows.                                           <BR>
 * After being started up, a debuggee informs a debugger.               <BR>
 * Upon the receiption a message from the debuggee, the debugger        <BR>
 * - gets the Field flagField object                                    <BR>
 *   mirroring int variable "flag" in the debuggee,                     <BR>
 * - calls to the method VirtualMachine.canGetCurrentContendedMonitor() <BR>
 *   to get its returned value.                                         <BR>
 * Then if the value is true the debugger checks up that invoking       <BR>
 * the method Field.isSynthetic() on the flagField doesn't throw        <BR>
 *    UnsupportedOperationException.                                    <BR>
 * If the value is false the debugger checks up that invoking           <BR>
 * the method Field.isSynthetic() on the flagField does throw           <BR>
 *    UnsupportedOperationException.                                    <BR>
 */

public class cangetattr001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/VirtualMachine/canGetSyntheticAttribute/cangetattr001 ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new cangetattr001().runThis(argv, out);
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
        "nsk.jdi.VirtualMachine.canGetSyntheticAttribute.cangetattr001a";

    //String mName = "nsk.jdi.VirtualMachine.canGetSyntheticAttribute";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine vm  = null;

    ReferenceType testedclass  = null;

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

            int expresult = returnCode0;

            Field flagField = null;

            label0: {

                log2("......getting Field flagField object");
                testedclass = (ReferenceType) vm.classesByName(debuggeeName).get(0);
                flagField = testedclass.fieldByName("flag");


                if (vm.canGetSyntheticAttribute()) {

                    log2("......vm.canGetSyntheticAttribute() == true");
                    log2("       checking up on no UnsupportedOperationException trown");
                    try {
                        flagField.isSynthetic();
                        log2("        no Exception thrown");
                    } catch ( UnsupportedOperationException e1 ) {
                        log3("ERROR: UnsupportedOperationException");
                        expresult = returnCode1;
                    } catch ( Exception e2 ) {
                        expresult = returnCode1;
                        log3("ERROR: UNEXPECTED Exception is thrown : " + e2);
                    }

                } else {

                    log2(".......vm.canGetSyntheticAttribute() == false");
                    log2("       checking up on UnsupportedOperationException trown");
                    try {
                        flagField.isSynthetic();
                        log3("ERROR: no UnsupportedOperationException thrown");
                        expresult = returnCode1;
                    } catch ( UnsupportedOperationException e1 ) {
                        log2("        UnsupportedOperationException thrown");
                    } catch ( Exception e2 ) {
                        log3("ERROR: UNEXPECTED Exception is thrown : " + e2);
                        expresult = returnCode1;
                    }
                }
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            log2("     the end of testing");
            if (expresult != returnCode0)
                testExitCode = FAILED;

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
