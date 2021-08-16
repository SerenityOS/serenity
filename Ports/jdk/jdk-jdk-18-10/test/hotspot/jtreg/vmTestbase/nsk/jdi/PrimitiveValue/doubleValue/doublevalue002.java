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

package nsk.jdi.PrimitiveValue.doubleValue;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * PrimitiveValue.                                              <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.PrimitiveValue.doubleValue()</code>        <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The case for testing is as follows.                  <BR>
 *                                                      <BR>
 * when a gebugger mirrors debuggee's field:            <BR>
 *   static double  db1 = Double.MAX_VALUE;             <BR>
 * as PrimitiveValue pvdb1;                             <BR>
 *                                                      <BR>
 * each of the following is true:                       <BR>
 *                                                      <BR>
 *  pvdb1.byteValue()   == (byte)   Double.MAX_VALUE    <BR>
 *  pvdb1.charValue()   == (char)   Double.MAX_VALUE    <BR>
 *  pvdb1.doubleValue() == (double) Double.MAX_VALUE    <BR>
 *  pvdb1.floatValue()  == (float)  Double.MAX_VALUE    <BR>
 *  pvdb1.intValue()    == (int)    Double.MAX_VALUE    <BR>
 *  pvdb1.longValue()   == (long)   Double.MAX_VALUE    <BR>
 *  pvdb1.shortValue()  == (short)  Double.MAX_VALUE    <BR>
 * <BR>
 */

public class doublevalue002 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/PrimitiveValue/doubleValue/doublevalue002  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new doublevalue002().runThis(argv, out);
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
        "nsk.jdi.PrimitiveValue.doubleValue.doublevalue002a";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine vm = null;

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

            List listOfDebuggeeExecClasses = vm.classesByName(debuggeeName);
            if (listOfDebuggeeExecClasses.size() != 1) {
                testExitCode = FAILED;
                log3("ERROR: listOfDebuggeeExecClasses.size() != 1");
                break ;
            }
            ReferenceType execClass =
                        (ReferenceType) listOfDebuggeeExecClasses.get(0);

            log2("......Field fsdb1 = execClass.fieldByName('db1');");
            Field fsdb1 = execClass.fieldByName("db1");

            log2("......PrimitiveValue pvdb1 = (PrimitiveValue) execClass.getValue(fsdb1);");
            PrimitiveValue pvdb1 = (PrimitiveValue) execClass.getValue(fsdb1);


            log2("......check: pvdb1.byteValue() == (byte) Double.MAX_VALUE");
            if (pvdb1.byteValue() != (byte) Double.MAX_VALUE) {
                testExitCode = FAILED;
                log3("ERROR: pvdb1.byteValue() != (byte) Double.MAX_VALUE");
                log2("       pvdb1.byteValue()       == " + pvdb1.byteValue() );
                log2("       (byte) Double.MAX_VALUE == " + ((byte) Double.MAX_VALUE) );
            }

            log2("......check: pvdb1.charValue() == (char) Double.MAX_VALUE");
            if (pvdb1.charValue() != (char) Double.MAX_VALUE) {
                testExitCode = FAILED;
                log3("ERROR: pvdb1.charValue() != (char) Double.MAX_VALUE");
                log2("       pvdb1.charValue()       == " + pvdb1.charValue() );
                log2("       (char) Double.MAX_VALUE == " + ((char) Double.MAX_VALUE) );
            }

            log2("......check: pvdb1.doubleValue() == (double) Double.MAX_VALUE");
            if (pvdb1.doubleValue() != (double) Double.MAX_VALUE) {
                testExitCode = FAILED;
                log3("ERROR: pvdb1.doubleValue() != (double) Double.MAX_VALUE");
                log2("       pvdb1.doubleValue()       == " + pvdb1.doubleValue() );
                log2("       (double) Double.MAX_VALUE == " + ((double) Double.MAX_VALUE) );
            }

            log2("......check: pvdb1.floatValue() == (float) Double.MAX_VALUE");
            if (pvdb1.floatValue() != (float) Double.MAX_VALUE) {
                testExitCode = FAILED;
                log3("ERROR: pvdb1.floatValue() != (float) Double.MAX_VALUE");
                log2("       pvdb1.floatValue()       == " + pvdb1.floatValue() );
                log2("       (float) Double.MAX_VALUE == " + ((float) Double.MAX_VALUE) );
            }

            log2("......check: pvdb1.intValue() == (int) Double.MAX_VALUE");
            if (pvdb1.intValue() != (int) Double.MAX_VALUE) {
                testExitCode = FAILED;
                log3("ERROR: pvdb1.intValue() != (int) Double.MAX_VALUE");
                log2("       pvdb1.intValue()       == " + pvdb1.intValue() );
                log2("       (int) Double.MAX_VALUE == " + ((int) Double.MAX_VALUE) );
            }

            log2("......check: pvdb1.longValue() == (long) Double.MAX_VALUE");
            if (pvdb1.longValue() != (long) Double.MAX_VALUE) {
                testExitCode = FAILED;
                log3("ERROR: pvdb1.longValue() != (long) Double.MAX_VALUE");
                log2("       pvdb1.longValue()       == " + pvdb1.longValue() );
                log2("       (long) Double.MAX_VALUE == " + ((long) Double.MAX_VALUE) );
            }

            log2("......check: pvdb1.shortValue() == (short) Double.MAX_VALUE");
            if (pvdb1.shortValue() != (short) Double.MAX_VALUE) {
                testExitCode = FAILED;
                log3("ERROR: pvdb1.shortValue() != (short) Double.MAX_VALUE");
                log2("       pvdb1.shortValue()       == " + pvdb1.shortValue() );
                log2("       (short) Double.MAX_VALUE == " + ((short) Double.MAX_VALUE) );
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
