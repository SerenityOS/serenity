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

package nsk.jdi.DoubleValue.value;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * DoubleValue.                                                 <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.DoubleValue.value()</code>                 <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The cases for testing are as follows :               <BR>
 *                                                      <BR>
 * when a gebuggee executes the following :                             <BR>
 *      public static double pos_inf      =  Double.POSITIVE_INFINITY;  <BR>
 *      public static double pos_zero     =  0.0d;                      <BR>
 *      public static double neg_zero     = -0.0d;                      <BR>
 *      public static double neg_inf      =  Double.NEGATIVE_INFINITY;  <BR>
 *      public static double pos_largest  =  Double.MAX_VALUE;          <BR>
 *      public static double pos_smallest =  Double.MIN_VALUE;          <BR>
 *      public static double neg_largest  = -Double.MAX_VALUE;          <BR>
 *      public static double neg_smallest = -Double.MIN_VALUE;          <BR>
 *      public static double double_nan   =  Double.NaN;                <BR>
 *                                                                      <BR>
 * which a debugger mirros as :                         <BR>
 *                                                      <BR>
 *      DoubleValue dvpos_inf;                          <BR>
 *      DoubleValue dvpos_zero;                         <BR>
 *      DoubleValue dvneg_zero;                         <BR>
 *      DoubleValue dvneg_inf;                          <BR>
 *      DoubleValue dvpos_largest;                      <BR>
 *      DoubleValue dvpos_smallest;                     <BR>
 *      DoubleValue dvneg_largest;                      <BR>
 *      DoubleValue dvneg_smallest;                     <BR>
 *      DoubleValue dvdouble_nan;                       <BR>
 *                                                      <BR>
 * the following is true:                               <BR>
 *                                                              <BR>
 *      dvneg_inf.value()      == Double.NEGATIVE_INFINITY      <BR>
 *      dvneg_largest.value()  == -Double.MAX_VALUE             <BR>
 *      dvneg_smallest.value() == -Double.MIN_VALUE             <BR>
 *      dvneg_zero.value()     == -0.0d                         <BR>
 *      dvpos_zero.value()     ==  0.0d                         <BR>
 *      dvpos_smallest.value() == Double.MIN_VALUE              <BR>
 *      dvpos_largest.value()  == Double.MAX_VALUE              <BR>
 *      dvpos_inf.value()      == Double.POSITIVE_INFINITY      <BR>
 *      dvdouble_nan.value()   != dvdouble_nan.value()          <BR>
 * <BR>
 */

public class value001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/DoubleValue/value/value001",
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
        "nsk.jdi.DoubleValue.value.value001a";

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

            List listOfDebuggeeExecClasses = vm.classesByName(debuggeeName);
            if (listOfDebuggeeExecClasses.size() != 1) {
                testExitCode = FAILED;
                log3("ERROR: listOfDebuggeeExecClasses.size() != 1");
                break ;
            }
            ReferenceType execClass =
                        (ReferenceType) listOfDebuggeeExecClasses.get(0);

            Field fdneg_inf      = execClass.fieldByName("neg_inf");
            Field fdneg_largest  = execClass.fieldByName("neg_largest");
            Field fdneg_smallest = execClass.fieldByName("neg_smallest");
            Field fdneg_zero     = execClass.fieldByName("neg_zero");
            Field fdpos_zero     = execClass.fieldByName("pos_zero");
            Field fdpos_smallest = execClass.fieldByName("pos_smallest");
            Field fdpos_largest  = execClass.fieldByName("pos_largest");
            Field fdpos_inf      = execClass.fieldByName("pos_inf");
            Field fddouble_nan   = execClass.fieldByName("double_nan");

            DoubleValue dvneg_inf      = (DoubleValue) execClass.getValue(fdneg_inf);
            DoubleValue dvneg_largest  = (DoubleValue) execClass.getValue(fdneg_largest);
            DoubleValue dvneg_smallest = (DoubleValue) execClass.getValue(fdneg_smallest);
            DoubleValue dvneg_zero     = (DoubleValue) execClass.getValue(fdneg_zero);
            DoubleValue dvpos_zero     = (DoubleValue) execClass.getValue(fdpos_zero);
            DoubleValue dvpos_smallest = (DoubleValue) execClass.getValue(fdpos_smallest);
            DoubleValue dvpos_largest  = (DoubleValue) execClass.getValue(fdpos_largest);
            DoubleValue dvpos_inf      = (DoubleValue) execClass.getValue(fdpos_inf);
            DoubleValue dvdouble_nan   = (DoubleValue) execClass.getValue(fddouble_nan);

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

            case 0: if (dvneg_inf.value() != Double.NEGATIVE_INFINITY)
                        expresult = 1;
                    break;

            case 1: if (dvneg_largest.value() != -Double.MAX_VALUE)
                        expresult = 1;
                    break;

            case 2: if (dvneg_smallest.value() != -Double.MIN_VALUE)
                        expresult = 1;
                    break;

            case 3: if (dvneg_zero.value() != -0.0d)
                        expresult = 1;
                    break;

            case 4: if (dvpos_zero.value() != 0.0d)
                        expresult = 1;
                    break;

            case 5: if (dvpos_smallest.value() != Double.MIN_VALUE)
                        expresult = 1;
                    break;

            case 6: if (dvpos_largest.value() != Double.MAX_VALUE)
                        expresult = 1;
                    break;

            case 7: if (dvpos_inf.value() != Double.POSITIVE_INFINITY)
                        expresult = 1;
                    break;

            case 8: if ( !(dvdouble_nan.value() != dvdouble_nan.value()) )
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
