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

package nsk.jdi.PrimitiveValue.booleanValue;

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
 * <code>com.sun.jdi.PrimitiveValue.booleanValue()</code>       <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The cases for testing are as follows :               <BR>
 *                                                      <BR>
 * when a gebuggee executes the following :             <BR>
 *   static boolean bl1 = true;                         <BR>
 *   static boolean bl0 = false;                        <BR>
 *   static byte    bt1 = Byte.MAX_VALUE;               <BR>
 *   static byte    bt0 = 0;                            <BR>
 *   static char    ch1 = Character.MAX_VALUE;          <BR>
 *   static char    ch0 = 0;                            <BR>
 *   static double  db1 = Double.MAX_VALUE;             <BR>
 *   static double  db0 = 0.0d;                         <BR>
 *   static float   fl1 = Float.MAX_VALUE;              <BR>
 *   static float   fl0 = 0.0f;                         <BR>
 *   static int     in1 = Integer.MAX_VALUE;            <BR>
 *   static int     in0 = 0;                            <BR>
 *   static long    ln1 = Long.MAX_VALUE;               <BR>
 *   static long    ln0 = 0;                            <BR>
 *   static short   sh1 = Short.MAX_VALUE;              <BR>
 *   static short   sh0 = 0;                            <BR>
 *                                                      <BR>
 * which a debugger mirrors as :                        <BR>
 *                                                      <BR>
 *      PrimitiveValue pvbl1, pvbl0, pvbt1, pvbt0,      <BR>
 *                     pvch1, pvch0, pvdb1, pvdb0,      <BR>
 *                     pvfl1, pvfl0, pvin1, pvin0,      <BR>
 *                     pvln1, pvln0, pvsh1, pvsh0;      <BR>
 *                                                      <BR>
 * each of the following is true:                       <BR>
 *                                                      <BR>
 *      pvbl1.booleanValue(), !pvbl0.booleanValue(),    <BR>
 *      pvbt1.booleanValue(), !pvbt0.booleanValue(),    <BR>
 *      pvch1.booleanValue(), !pvch0.booleanValue(),    <BR>
 *      pvdb1.booleanValue(), !pvdb0.booleanValue(),    <BR>
 *      pvfl1.booleanValue(), !pvfl0.booleanValue(),    <BR>
 *      pvin1.booleanValue(), !pvin0.booleanValue(),    <BR>
 *      pvln1.booleanValue(), !pvln0.booleanValue(),    <BR>
 *      pvsh1.booleanValue(), !pvsh0.booleanValue()     <BR>
 * <BR>
 */

public class booleanvalue001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/PrimitiveValue/booleanValue/booleanvalue001",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new booleanvalue001().runThis(argv, out);
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
        "nsk.jdi.PrimitiveValue.booleanValue.booleanvalue001a";

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
        log2("booleanvalue001a debuggee launched");
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

            Field fsbl1 = execClass.fieldByName("bl1");
            Field fsbl0 = execClass.fieldByName("bl0");
            Field fsbt1 = execClass.fieldByName("bt1");
            Field fsbt0 = execClass.fieldByName("bt0");
            Field fsch1 = execClass.fieldByName("ch1");
            Field fsch0 = execClass.fieldByName("ch0");
            Field fsdb1 = execClass.fieldByName("db1");
            Field fsdb0 = execClass.fieldByName("db0");
            Field fsfl1 = execClass.fieldByName("fl1");
            Field fsfl0 = execClass.fieldByName("fl0");
            Field fsin1 = execClass.fieldByName("in1");
            Field fsin0 = execClass.fieldByName("in0");
            Field fsln1 = execClass.fieldByName("ln1");
            Field fsln0 = execClass.fieldByName("ln0");
            Field fssh1 = execClass.fieldByName("sh1");
            Field fssh0 = execClass.fieldByName("sh0");

            PrimitiveValue pvbl1 = (PrimitiveValue) execClass.getValue(fsbl1);
            PrimitiveValue pvbl0 = (PrimitiveValue) execClass.getValue(fsbl0);
            PrimitiveValue pvbt1 = (PrimitiveValue) execClass.getValue(fsbt1);
            PrimitiveValue pvbt0 = (PrimitiveValue) execClass.getValue(fsbt0);
            PrimitiveValue pvch1 = (PrimitiveValue) execClass.getValue(fsch1);
            PrimitiveValue pvch0 = (PrimitiveValue) execClass.getValue(fsch0);
            PrimitiveValue pvdb1 = (PrimitiveValue) execClass.getValue(fsdb1);
            PrimitiveValue pvdb0 = (PrimitiveValue) execClass.getValue(fsdb0);
            PrimitiveValue pvfl1 = (PrimitiveValue) execClass.getValue(fsfl1);
            PrimitiveValue pvfl0 = (PrimitiveValue) execClass.getValue(fsfl0);
            PrimitiveValue pvin1 = (PrimitiveValue) execClass.getValue(fsin1);
            PrimitiveValue pvin0 = (PrimitiveValue) execClass.getValue(fsin0);
            PrimitiveValue pvln1 = (PrimitiveValue) execClass.getValue(fsln1);
            PrimitiveValue pvln0 = (PrimitiveValue) execClass.getValue(fsln0);
            PrimitiveValue pvsh1 = (PrimitiveValue) execClass.getValue(fssh1);
            PrimitiveValue pvsh0 = (PrimitiveValue) execClass.getValue(fssh0);

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0:
                        log2("...... checks for: pvbl1.booleanValue()  and  pvbl0.booleanValue()");
                        if (!pvbl1.booleanValue() || pvbl0.booleanValue()) {
                            log3("ERROR: !pvbl1.booleanValue() || pvbl0.booleanValue()");
                            expresult = 1;
                        }
                        break;

                case 1:
                        log2("...... checks for: pvbt1.booleanValue()  and  pvbt0.booleanValue()");
                        if (!pvbt1.booleanValue() || pvbt0.booleanValue()) {
                            log3("ERROR: !pvbt1.booleanValue() || pvbt0.booleanValue()");
                            expresult = 1;
                        }
                        break;

                case 2:
                        log2("...... checks for: pvch1.booleanValue()  and  pvch0.booleanValue()");
                        if (!pvch1.booleanValue() || pvch0.booleanValue()) {
                            log3("ERROR: !pvch1.booleanValue() || pvch0.booleanValue()");
                            expresult = 1;
                        }
                        break;

                case 3:
                        log2("...... checks for: pvdb1.booleanValue()  and  pvdb0.booleanValue()");
                        if (!pvdb1.booleanValue() || pvdb0.booleanValue()) {
                            log3("ERROR: !pvdb1.booleanValue() || pvdb0.booleanValue()");
                            expresult = 1;
                        }
                        break;

                case 4:
                        log2("...... checks for: pvfl1.booleanValue()  and  pvfl0.booleanValue()");
                        if (!pvfl1.booleanValue() || pvfl0.booleanValue()) {
                            log3("ERROR: !pvfl1.booleanValue() || pvfl0.booleanValue()");
                            expresult = 1;
                        }
                        break;

                case 5:
                        log2("...... checks for: pvin1.booleanValue()  and  pvin0.booleanValue()");
                        if (!pvin1.booleanValue() || pvin0.booleanValue()) {
                            log3("ERROR: !pvin1.booleanValue() || pvin0.booleanValue()");
                            expresult = 1;
                        }
                        break;

                case 6:
                        log2("...... checks for: pvln1.booleanValue()  and  pvln0.booleanValue()");
                        if (!pvln1.booleanValue() || pvln0.booleanValue()) {
                            log3("ERROR: !pvln1.booleanValue() || pvln0.booleanValue()");
                            expresult = 1;
                        }
                        break;

                case 7:
                        log2("...... checks for: pvsh1.booleanValue()  and  pvsh0.booleanValue()");
                        if (!pvsh1.booleanValue() || pvsh0.booleanValue()) {
                            log3("ERROR: !pvsh1.booleanValue() || pvsh0.booleanValue()");
                            expresult = 1;
                        }
                        break;


                default: expresult = 2;
                         break ;
                }

                if (expresult == 2) {
                    log2("      test cases finished");
                    break ;
                } else if (expresult == 1) {
//                  log3("ERROR: expresult != true;  check # = " + i);
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
