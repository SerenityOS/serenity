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

package nsk.jdi.ArrayType.componentSignature;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * ArrayType.                                                   <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ArrayType.componentSignature()</code>      <BR>
 * complies with its spec when a type is one of primitive types.<BR>
 * <BR>
 * The cases for testing are as follows.                <BR>
 *                                                      <BR>
 * When a gebuggee executes the following :             <BR>
 *   static boolean bl[] = {true, false};               <BR>
 *   static byte    bt[] = {0, 1};                      <BR>
 *   static char    ch[] = {0, 1};                      <BR>
 *   static double  db[] = {0.0d, 1.0d};                <BR>
 *   static float   fl[] = {0.0f, 1.0f};                <BR>
 *   static int     in[] = {0, 1};                      <BR>
 *   static long    ln[] = {0, 1};                      <BR>
 *   static short   sh[] = {0, 1};                      <BR>
 *                                                      <BR>
 * for all of the above primitive type variables,       <BR>
 * a debugger forms their corresponding Type objects    <BR>
 * from which it forms text representations of          <BR>
 * signatures in String variables                       <BR>
 * named blSignature, btSignature, and etc.             <BR>
 *                                                      <BR>
 * Then the debugger checks up that     <BR>
 * each of the following is true :      <BR>
 *                                      <BR>
 *      blSignature.equals("Z")         <BR>
 *      btSignature.equals("B")         <BR>
 *      chSignature.equals("C")         <BR>
 *      dbSignature.equals("D")         <BR>
 *      flSignature.equals("F")         <BR>
 *      inSignature.equals("I")         <BR>
 *      lnSignature.equals("J")         <BR>
 *      shSignature.equals("S")         <BR>
 */

public class componentsignature001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ArrayType/componentSignature/componentsignature001",
    sHeader2 = "--> componentsignature001: ",
    sHeader3 = "##> componentsignature001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new componentsignature001().runThis(argv, out);
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
        "nsk.jdi.ArrayType.componentSignature.componentsignature001a";

    //String mName = "nsk.jdi.ArrayType.componentSignature";

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
        log2("componentsignature001a debuggee launched");
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

            List listOfDebuggeeExecClasses = vm.classesByName(debuggeeName);
            if (listOfDebuggeeExecClasses.size() != 1) {
                testExitCode = FAILED;
                log3("ERROR: listOfDebuggeeExecClasses.size() != 1");
                break ;
            }
            ReferenceType execClass =
                        (ReferenceType) listOfDebuggeeExecClasses.get(0);

            Field fsbl = execClass.fieldByName("bl");
            Field fsbt = execClass.fieldByName("bt");
            Field fsch = execClass.fieldByName("ch");
            Field fsdb = execClass.fieldByName("db");
            Field fsfl = execClass.fieldByName("fl");
            Field fsin = execClass.fieldByName("in");
            Field fsln = execClass.fieldByName("ln");
            Field fssh = execClass.fieldByName("sh");

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0:                 // boolean[]
                        String blSignature =
                            ((ArrayType) execClass.getValue(fsbl).type()).componentSignature();
                        if (!blSignature.equals("Z")) {
                            expresult = 1;
                            log3("ERROR: !blSignature.equals('[Z')   "  + blSignature);
                        }
                        break;

                case 1:                 // byte[]
                        String btSignature =
                            ((ArrayType) execClass.getValue(fsbt).type()).componentSignature();
                        if (!btSignature.equals("B")) {
                            expresult = 1;
                            log3("ERROR: !btSignature.equals('B')");
                        }
                        break;

                case 2:                 // char[]
                        String chSignature =
                            ((ArrayType) execClass.getValue(fsch).type()).componentSignature();
                        if (!chSignature.equals("C")) {
                            expresult = 1;
                            log3("ERROR: !chSignature.equals('C')");
                        }
                        break;

                case 3:                 // double[]
                        String dbSignature =
                            ((ArrayType) execClass.getValue(fsdb).type()).componentSignature();
                        if (!dbSignature.equals("D")) {
                            expresult = 1;
                            log3("ERROR: !dbSignature.equals('D')");
                        }
                        break;

                case 4:                 // float[]
                        String flSignature =
                            ((ArrayType) execClass.getValue(fsfl).type()).componentSignature();
                        if (!flSignature.equals("F")) {
                            expresult = 1;
                            log3("ERROR: !flSignature.equals('F')");
                        }
                        break;

                case 5:                 // int[]
                        String inSignature =
                            ((ArrayType) execClass.getValue(fsin).type()).componentSignature();
                        if (!inSignature.equals("I")) {
                            expresult = 1;
                            log3("ERROR: !inSignature.equals('I')");
                        }
                        break;

                case 6:                 // long[]
                        String lnSignature =
                            ((ArrayType) execClass.getValue(fsln).type()).componentSignature();
                        if (!lnSignature.equals("J")) {
                            expresult = 1;
                            log3("ERROR: !lnSignature.equals('J')");
                        }
                        break;

                case 7:                 // short[]
                        String shSignature =
                            ((ArrayType) execClass.getValue(fssh).type()).componentSignature();
                        if (!shSignature.equals("S")) {
                            expresult = 1;
                            log3("ERROR: !shSignature.equals('S')");
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
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }
}
