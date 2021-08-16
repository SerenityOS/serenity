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

package nsk.jdi.VirtualMachine.mirrorOf_bool;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * VirtualMachine.                                              <BR>
 * <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.VirtualMachine.mirrorOf(boolean value)</code> <BR>
 * complies with the spec for                                   <BR>
 * <code>com.sun.jdi.BooleanValue</code> methods                <BR>
 * <BR>
 * The cases for testing are as follows <BR>
 * (prefix 'bv' means 'boolean_value') :<BR>
 *                                      <BR>
 * // BooleanValue bv.value() method    <BR>
 *                                      <BR>
 *      bvTrue.value()  == true         <BR>
 *      bvFalse.value() == false        <BR>
 *                                              <BR>
 * // BooleanValue bv.equals() method           <BR>
 *                                              <BR>
 *      bvTrue1.equals(bvTrue2)                 <BR>
 *      !bvTrue1.equals(bvFalse1)               <BR>
 *      bvFalse1.equals(bvFalse2)               <BR>
 *      !bvFalse1.equals(bvTrue1)               <BR>
 *                                              <BR>
 *      !bvTrue1.equals(IntegerValue i0)        <BR>
 *      !bvFalse1.equals(IntegerValue i1)       <BR>
 *                                                      <BR>
 * // BooleanValue bv.hashCode() method                 <BR>
 *                                                      <BR>
 *      bvTrue1.hashCode()  == bvTrue1.hashCode()       <BR>
 *      bvFalse1.hashCode() == bvFalse1.hashCode()      <BR>
 *      bvTrue1.hashCode()  == bvTrue2.hashCode()       <BR>
 *      bvFalse1.hashCode() == bvFalse2.hashCode()      <BR>
 *      bvTrue1.hashCode()  != bvFalse1.hashCode()      <BR>
 */

public class mirrorof_bool001 {

    //----------------------------------------------------- template section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- template parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/VirtualMachine/mirrorOf_bool/mirrorof_bool001",
    sHeader2 = "--> mirrorof001: ",
    sHeader3 = "##> mirrorof001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new mirrorof_bool001().runThis(argv, out);
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

    private String debugeeName =
        "nsk.jdi.VirtualMachine.mirrorOf_bool.mirrorof_bool001a";

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
            debugee = binder.bindToDebugee(debugeeName + " -vbs");  // *** tp
        } else {
            debugee = binder.bindToDebugee(debugeeName);            // *** tp
        }

        IOPipe pipe     = new IOPipe(debugee);

        debugee.redirectStderr(out);
        log2("mirrorof_bool001a debugee launched");
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
        log1("TESTING BEGINS");

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

        BooleanValue bvTrue1  = vm.mirrorOf(true);
        BooleanValue bvTrue2  = vm.mirrorOf(true);
        BooleanValue bvFalse1 = vm.mirrorOf(false);
        BooleanValue bvFalse2 = vm.mirrorOf(false);

        int i;

        for (i = 0; ; i++) {

            int expresult = 0;

            log2("new check: #" + i);

            switch (i) {

            case 0: if (bvTrue1.value() != true)
                        expresult = 1;
                    break;

            case 1: if (bvFalse1.value() != false)
                        expresult = 1;
                    break;


            case 2: if (!bvTrue1.equals(bvTrue2))
                        expresult = 1;
                    break;

            case 3: if (bvTrue1.equals(bvFalse1))
                        expresult = 1;
                    break;

            case 4: if (!bvFalse1.equals(bvFalse2))
                        expresult = 1;
                    break;

            case 5: if (bvFalse1.equals(bvTrue1))
                        expresult = 1;
                    break;

            case 6: IntegerValue i0 = vm.mirrorOf(0);
                    if (bvTrue1.equals(i0))
                        expresult = 1;
                    break;

            case 7: IntegerValue i1 = vm.mirrorOf(1);
                    if (bvFalse1.equals(i1))
                        expresult = 1;
                    break;


            case 8: if (bvTrue1.hashCode() != bvTrue1.hashCode())
                        expresult = 1;
                    break;

            case 9: if (bvFalse1.hashCode() != bvFalse1.hashCode())
                        expresult = 1;
                    break;

            case 10: if (bvTrue1.hashCode() != bvTrue2.hashCode())
                        expresult = 1;
                     break;

            case 11: if (bvFalse1.hashCode() != bvFalse2.hashCode())
                         expresult = 1;
                     break;

            case 12: if (bvTrue1.hashCode() == bvFalse1.hashCode())
                         expresult = 1;
                     break;



            default: expresult = 2;
                     break ;
            }

            if (expresult == 2) {
                log2("      test cases finished");
                break ;
            } else if (expresult == 1) {
                //      verbMode=true;
                log3("ERROR: expresult != true;  check # = " + i);
                testExitCode = FAILED;
            }

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }

        log1("TESTING ENDS");

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
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }
}
