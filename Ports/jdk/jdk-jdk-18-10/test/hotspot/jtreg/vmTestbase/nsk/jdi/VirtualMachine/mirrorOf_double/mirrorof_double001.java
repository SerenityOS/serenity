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

package nsk.jdi.VirtualMachine.mirrorOf_double;

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
 * <code>com.sun.jdi.VirtualMachine.mirrorOf_double()</code>    <BR>
 * complies with the spec for                                   <BR>
 * <code>com.sun.jdi.DoubleValue</code> methods                 <BR>
 * <BR>
 * The cases for testing are as follows         <BR>
 * ('val_i' means 'double_value_i') :           <BR>
 *                                                                         <BR>
 * // DoubleValue val.value() method                                       <BR>
 *                                                                         <BR>
 *      val_i.value(Double.NEGATIVE_INFINITY) ==  Double.NEGATIVE_INFINITY <BR>
 *      val_i.value(-Double.MAX_VALUE)        == -Double.MAX_VALUE         <BR>
 *      val_i.value(-Double.MIN_VALUE)        == -Double.MIN_VALUE         <BR>
 *      val_i.value(-0.0f)                    == -0.0f                     <BR>
 *      val_i.value(0.0f)                     ==  0.0f                     <BR>
 *      val_i.value(Double.MIN_VALUE)         ==  Double.MIN_VALUE         <BR>
 *      val_i.value(Double.MAX_VALUE)         ==  Double.MAX_VALUE         <BR>
 *      val_i.value(Double.POSITIVE_INFINITY) ==  Double.POSITIVE_INFINITY <BR>
 *      val_i.value(Double.NaN)               ==  Double.NaN               <BR>
 *                                                                         <BR>
 * // DoubleValue val.equals() method                   <BR>
 *                                                      <BR>
 *      val_i.value(+1.0f) == val_j.value(+1.0f)        <BR>
 *      val_i.value(+1.0f) != val_j.value(-1.0f)        <BR>
 *                                                      <BR>
 *      val_i.value(1.0) != doubleValue.value(1.0)      <BR>
 *                                                      <BR>
 * // DoubleValue val.hashCode() method                 <BR>
 *                                                      <BR>
 *      val_i.hashCode() == val_i.hashCode()            <BR>
 *                                                      <BR>
 *      if (val_i.value() == val_j.value()) {           <BR>
 *          val_i.hashCode() == val_j.hashCode()        <BR>
 *      }                                               <BR>
 * <BR>
 */

public class mirrorof_double001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/VirtualMachine/mirrorOf_double/mirrorof_double001",
    sHeader2 = "--> mirrorof_double001: ",
    sHeader3 = "##> mirrorof_double001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new mirrorof_double001().runThis(argv, out);
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
        "nsk.jdi.VirtualMachine.mirrorOf_double.mirrorof_double001a";

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
        log2("mirrorof_double001a debugee launched");
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

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

        double pos_inf  =  Double.POSITIVE_INFINITY;
        double pos_zero =  0.0d;
        double neg_zero = -0.0d;
        double neg_inf  =  Double.NEGATIVE_INFINITY;

        double pos_largest  =  Double.MAX_VALUE;
        double pos_smallest =  Double.MIN_VALUE;
        double neg_largest  = -Double.MAX_VALUE;
        double neg_smallest = -Double.MIN_VALUE;

        double double_nan  =  Double.NaN;

        double plus1    =  +1.0d;
        double minus1   =  -1.0d;

        DoubleValue val_1  = vm.mirrorOf(neg_inf);
        DoubleValue val_2  = vm.mirrorOf(neg_largest);
        DoubleValue val_3  = vm.mirrorOf(neg_smallest);
        DoubleValue val_4  = vm.mirrorOf(neg_zero);
        DoubleValue val_5  = vm.mirrorOf(pos_zero);
        DoubleValue val_6  = vm.mirrorOf(pos_smallest);
        DoubleValue val_7  = vm.mirrorOf(pos_largest);
        DoubleValue val_8  = vm.mirrorOf(pos_inf);

        DoubleValue val_9  = vm.mirrorOf(double_nan);

        DoubleValue val_10 = vm.mirrorOf(plus1);
        DoubleValue val_11 = vm.mirrorOf(plus1);
        DoubleValue val_12 = vm.mirrorOf(minus1);

        FloatValue val_13 = vm.mirrorOf((float)plus1);


        int i;

        for (i = 0; ; i++) {

            int expresult = 0;

            log2("     new check: #" + i);

            switch (i) {

            // tests for DoubleValue.value()

            case 0: if (val_1.value() != neg_inf)
                        expresult = 1;
                    break;

            case 1: if (val_2.value() != neg_largest)
                        expresult = 1;
                    break;

            case 2: if (val_3.value() != neg_smallest)
                        expresult = 1;
                    break;

            case 3: if (val_4.value() != neg_zero)
                        expresult = 1;
                    break;

            case 4: if (val_5.value() != pos_zero)
                        expresult = 1;
                    break;

            case 5: if (val_6.value() != pos_smallest)
                        expresult = 1;
                    break;

            case 6: if (val_7.value() != pos_largest)
                        expresult = 1;
                    break;

            case 7: if (val_8.value() != pos_inf)
                        expresult = 1;
                    break;

            case 8: if ( !(val_9.value() != val_9.value()) )
                        expresult = 1;
                    break;


            // tests for DoubleValue.equals()

            case 9: if (!val_10.equals(val_11))
                        expresult = 1;
                    break;

            case 10: if (val_10.equals(val_12))
                        expresult = 1;
                    break;

            case 11: if (val_10.equals(val_13))
                        expresult = 1;
                    break;


            // tests for DoubleValue.hashCode()

            case 12: if (val_7.hashCode() != val_7.hashCode())
                        expresult = 1;
                    break;

            case 13: if (val_10.hashCode() != val_11.hashCode())
                        expresult = 1;
                    break;


            default: expresult = 2;
                     break ;
            }

            if (expresult == 2) {
                log2("      test cases finished");
                break ;
            } else if (expresult == 1) {;
                log3("ERROR: expresult != true;  check # = " + i);
                testExitCode = FAILED;
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
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }
}
