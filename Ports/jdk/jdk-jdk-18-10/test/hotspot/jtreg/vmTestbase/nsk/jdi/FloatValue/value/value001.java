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

package nsk.jdi.FloatValue.value;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * FloatValue.                                                  <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.FloatValue.value()</code>                  <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The cases for testing are as follows :               <BR>
 *                                                      <BR>
 * when a gebuggee executes the following :                             <BR>
 *      public static float pos_inf      =  Float.POSITIVE_INFINITY;    <BR>
 *      public static float pos_zero     =  0.0d;                       <BR>
 *      public static float neg_zero     = -0.0d;                       <BR>
 *      public static float neg_inf      =  Float.NEGATIVE_INFINITY;    <BR>
 *      public static float pos_largest  =  Float.MAX_VALUE;            <BR>
 *      public static float pos_smallest =  Float.MIN_VALUE;            <BR>
 *      public static float neg_largest  = -Float.MAX_VALUE;            <BR>
 *      public static float neg_smallest = -Float.MIN_VALUE;            <BR>
 *      public static float float_nan    =  Float.NaN;                  <BR>
 *                                                                      <BR>
 * which a debugger mirros as :                                 <BR>
 *                                                              <BR>
 *      FloatValue fvpos_inf;                                   <BR>
 *      FloatValue fvpos_zero;                                  <BR>
 *      FloatValue fvneg_zero;                                  <BR>
 *      FloatValue fvneg_inf;                                   <BR>
 *      FloatValue fvpos_largest;                               <BR>
 *      FloatValue fvpos_smallest;                              <BR>
 *      FloatValue fvneg_largest;                               <BR>
 *      FloatValue fvneg_smallest;                              <BR>
 *      FloatValue fvdouble_nan;                                <BR>
 *                                                              <BR>
 * the following is true:                                       <BR>
 *                                                              <BR>
 *      fvneg_inf.value()      == Floate.NEGATIVE_INFINITY      <BR>
 *      fvneg_largest.value()  == -Float.MAX_VALUE              <BR>
 *      fvneg_smallest.value() == -Double.MIN_VALUE             <BR>
 *      fvneg_zero.value()     == -0.0d                         <BR>
 *      fvpos_zero.value()     ==  0.0d                         <BR>
 *      fvpos_smallest.value() == Float.MIN_VALUE               <BR>
 *      fvpos_largest.value()  == Float.MAX_VALUE               <BR>
 *      fvpos_inf.value()      == Float.POSITIVE_INFINITY       <BR>
 *      fvfloat_nan.value()    != fvfloat_nan.value()           <BR>
 * <BR>
 */

public class value001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/FloatValue/value/value001",
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
        "nsk.jdi.FloatValue.value.value001a";

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

            Field ffneg_inf      = execClass.fieldByName("neg_inf");
            Field ffneg_largest  = execClass.fieldByName("neg_largest");
            Field ffneg_smallest = execClass.fieldByName("neg_smallest");
            Field ffneg_zero     = execClass.fieldByName("neg_zero");
            Field ffpos_zero     = execClass.fieldByName("pos_zero");
            Field ffpos_smallest = execClass.fieldByName("pos_smallest");
            Field ffpos_largest  = execClass.fieldByName("pos_largest");
            Field ffpos_inf      = execClass.fieldByName("pos_inf");
            Field ffloat_nan     = execClass.fieldByName("float_nan");

            FloatValue fvneg_inf      = (FloatValue) execClass.getValue(ffneg_inf);
            FloatValue fvneg_largest  = (FloatValue) execClass.getValue(ffneg_largest);
            FloatValue fvneg_smallest = (FloatValue) execClass.getValue(ffneg_smallest);
            FloatValue fvneg_zero     = (FloatValue) execClass.getValue(ffneg_zero);
            FloatValue fvpos_zero     = (FloatValue) execClass.getValue(ffpos_zero);
            FloatValue fvpos_smallest = (FloatValue) execClass.getValue(ffpos_smallest);
            FloatValue fvpos_largest  = (FloatValue) execClass.getValue(ffpos_largest);
            FloatValue fvpos_inf      = (FloatValue) execClass.getValue(ffpos_inf);
            FloatValue fvfloat_nan    = (FloatValue) execClass.getValue(ffloat_nan);

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0: if (fvneg_inf.value() != Float.NEGATIVE_INFINITY)
                            expresult = 1;
                        break;

                case 1: if (fvneg_largest.value() != -Float.MAX_VALUE)
                            expresult = 1;
                        break;

                case 2: if (fvneg_smallest.value() != -Float.MIN_VALUE)
                            expresult = 1;
                        break;

                case 3: if (fvneg_zero.value() != -0.0d)
                            expresult = 1;
                        break;

                case 4: if (fvpos_zero.value() != 0.0d)
                            expresult = 1;
                        break;

                case 5: if (fvpos_smallest.value() != Float.MIN_VALUE)
                            expresult = 1;
                        break;

                case 6: if (fvpos_largest.value() != Float.MAX_VALUE)
                            expresult = 1;
                        break;

                case 7: if (fvpos_inf.value() != Float.POSITIVE_INFINITY)
                            expresult = 1;
                        break;

                case 8: if ( !(fvfloat_nan.value() != fvfloat_nan.value()) )
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
