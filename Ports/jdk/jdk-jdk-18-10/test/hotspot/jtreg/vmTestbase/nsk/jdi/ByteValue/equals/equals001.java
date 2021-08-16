/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ByteValue.equals;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * ByteValue.                                                   <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ByteValue.equals()</code>                  <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The cases for testing are as follows :               <BR>
 *                                                      <BR>
 * when a gebuggee executes the following :             <BR>
 *      public static byte plus1_1 = +1;                <BR>
 *      public static byte plus1_2 = +1;                <BR>
 *      public static byte minus1  = -1;                <BR>
 *      public static short shortplus1 = +1;            <BR>
 *                                                      <BR>
 * which a debugger mirros as :                         <BR>
 *                                                      <BR>
 *      ByteValue bvplus1_1;                            <BR>
 *      ByteValue bvplus1_2;                            <BR>
 *      ByteValue bvminus1;                             <BR>
 *      ShortValue svplus1;                             <BR>
 *                                                      <BR>
 * the following is true:                               <BR>
 *                                                      <BR>
 *      bvplus1_1 == bvplus1_2                          <BR>
 *      bvplus1_1 != bvminus1                           <BR>
 *      bvplus1_1 != svplus1                            <BR>
 * <BR>
 */

public class equals001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ByteValue/equals/equals001",
    sHeader2 = "--> equals001: ",
    sHeader3 = "##> equals001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new equals001().runThis(argv, out);
    }

     //--------------------------------------------------   log procedures

    private static boolean verbMode = false;

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
        "nsk.jdi.ByteValue.equals.equals001a";

    //====================================================== test program

    static ArgumentHandler      argsHandler;
    static int                  testExitCode = PASSED;

    //------------------------------------------------------ common section

    private int runThis (String argv[], PrintStream out) {

        Debugee debugee;

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        if (argsHandler.verbose()) {
            debugee = binder.bindToDebugee(debuggeeName + " -vbs");  // *** tp
        } else {
            debugee = binder.bindToDebugee(debuggeeName);            // *** tp
        }

        IOPipe pipe     = new IOPipe(debugee);

        debugee.redirectStderr(out);
        log2("equals001a debugee launched");
        debugee.resume();

        String line = pipe.readln();
        if ((line == null) || !line.equals("ready")) {
            log3("signal received is not 'ready' but: " + line);
            return FAILED;
        } else {
            log2("'ready' recieved");
        }

        VirtualMachine vm = debugee.VM();

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

            List listOfDebuggeeExecClasses = vm.classesByName(debuggeeName);
            if (listOfDebuggeeExecClasses.size() != 1) {
                testExitCode = FAILED;
                log3("ERROR: listOfDebuggeeExecClasses.size() != 1");
                break ;
            }
            ReferenceType execClass =
                        (ReferenceType) listOfDebuggeeExecClasses.get(0);

            Field fbplus1_1 = execClass.fieldByName("plus1_1");
            Field fbplus1_2 = execClass.fieldByName("plus1_2");
            Field fbminus1  = execClass.fieldByName("minus1");
            Field fsplus1   = execClass.fieldByName("shortplus1");

            ByteValue bvplus1_1 = (ByteValue)  execClass.getValue(fbplus1_1);
            ByteValue bvplus1_2 = (ByteValue)  execClass.getValue(fbplus1_2);
            ByteValue bvminus1  = (ByteValue)  execClass.getValue(fbminus1);
            ShortValue svplus1  = (ShortValue) execClass.getValue(fsplus1);

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0: if (!bvplus1_1.equals(bvplus1_2))
                            expresult = 1;
                        break;

                case 1: if (bvplus1_1.equals(bvminus1))
                            expresult = 1;
                        break;

                case 2: if (bvplus1_1.equals(svplus1))
                            expresult = 1;
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
        log2("waiting for the debugee finish ...");
        debugee.waitFor();

        int status = debugee.getStatus();
        if (status != PASSED + PASS_BASE) {
            log3("debugee returned UNEXPECTED exit status: " +
                   status + " != PASS_BASE");
            testExitCode = FAILED;
        } else {
            log2("debugee returned expected exit status: " +
                   status + " == PASS_BASE");
        }

        if (testExitCode != PASSED) {
            logHandler.complain("TEST FAILED");
        }
        return testExitCode;
    }
}
