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

package nsk.jdi.ArrayType.newInstance;

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
 * <code>com.sun.jdi.ArrayType.newInstance()</code>             <BR>
 * complies with its spec when a type is one of primitive types.<BR>
 * <BR>
 * The cases for testing are as follows.        <BR>
 *                                              <BR>
 * When a gebuggee executes the following :     <BR>
 *   static boolean bl[] = {true};              <BR>
 *   static byte    bt[] = {1};                 <BR>
 *   static char    ch[] = {1};                 <BR>
 *   static double  db[] = {1.0d};              <BR>
 *   static float   fl[] = {1.0f};              <BR>
 *   static int     in[] = {1};                 <BR>
 *   static long    ln[] = {1};                 <BR>
 *   static short   sh[] = {1};                 <BR>
 *                                                      <BR>
 * for all of the above primitive type variables,       <BR>
 * a debugger creates new instances of                  <BR>
 * corresponding ReferenceArray objects and checks up   <BR>
 * that (1) length of newly created arrays is equal to  <BR>
 * a method invocation parameter and (2) initial values <BR>
 * of arrays components are as they are defined in      <BR>
 * JLS, section 4.5.5.                                  <BR>
 */

public class newinstance001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ArrayType/newInstance/newinstance001",
    sHeader2 = "--> newinstance001: ",
    sHeader3 = "##> newinstance001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new newinstance001().runThis(argv, out);
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
        "nsk.jdi.ArrayType.newInstance.newinstance001a";

    //String mName = "nsk.jdi.ArrayType.newInstance";

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
        log2("newinstance001a debuggee launched");
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

            // There are potentially other non-test Java threads allocating objects and triggering
            // GC's so we need to suspend the target VM to avoid the objects created in the test
            // from being accidentally GC'ed. However, we need the target VM temporary resumed
            // while reading its response. Below we resume the target VM (if required) and suspend
            // it only after pipe.readln() returns.

            // On the first iteration the target VM is not suspended yet.
            if (i > 0) {
                debuggee.resume();
            }
            line = pipe.readln();

            // Suspending target VM to prevent other non-test Java threads from triggering GCs.
            debuggee.suspend();

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

            final int arraylength = 2;

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0:                 // boolean[]
                        ArrayType blArray =
                            (ArrayType) execClass.getValue(fsbl).type();
                        ArrayReference newBlArray = blArray.newInstance(arraylength);

                        if (newBlArray.length() != arraylength) {
                            log3("ERROR : newBlArray.length() != arraylength   " + newBlArray.length());
                            expresult = 1;
                            break;
                        }
                        if ( ((BooleanValue) newBlArray.getValue(0)).value() != false ||
                             ((BooleanValue) newBlArray.getValue(1)).value() != false ) {
                            log3("ERROR : newBlArray components != false");
                            expresult = 1;
                            break;
                        }

                        break;

                case 1:                 // byte[]
                        ArrayType btArray =
                            (ArrayType) execClass.getValue(fsbt).type();
                        ArrayReference newBtArray = btArray.newInstance(arraylength);

                        if (newBtArray.length() != arraylength) {
                            log3("ERROR : newBtArray.length() != arraylength   " + newBtArray.length());
                            expresult = 1;
                            break;
                        }
                        if ( ((ByteValue) newBtArray.getValue(0)).value() != (byte) 0 ||
                             ((ByteValue) newBtArray.getValue(1)).value() != (byte) 0 ) {
                            log3("ERROR : newBtArray components != (byte) 0");
                            expresult = 1;
                            break;
                        }
                        break;

                case 2:                 // char[]
                        ArrayType chArray =
                            (ArrayType) execClass.getValue(fsch).type();
                        ArrayReference newChArray = chArray.newInstance(arraylength);

                        if (newChArray.length() != arraylength) {
                            log3("ERROR : newChArray.length() != arraylength   " + newChArray.length());
                            expresult = 1;
                            break;
                        }
                        if ( ((CharValue) newChArray.getValue(0)).value() != '\u0000' ||
                             ((CharValue) newChArray.getValue(1)).value() != '\u0000' ) {
                            log3("ERROR : newChArray components != '\u0000' ");
                            expresult = 1;
                            break;
                        }
                        break;

                case 3:                 // double[]
                        ArrayType dbArray =
                            (ArrayType) execClass.getValue(fsdb).type();
                        ArrayReference newDbArray = dbArray.newInstance(arraylength);

                        if (newDbArray.length() != arraylength) {
                            log3("ERROR : newDBArray.length() != arraylength   " + newDbArray.length());
                            expresult = 1;
                            break;
                        }
                        if ( ((DoubleValue) newDbArray.getValue(0)).value() != 0.0d ||
                             ((DoubleValue) newDbArray.getValue(1)).value() != 0.0d ) {
                            log3("ERROR : newDbArray components != 0.0d");
                            expresult = 1;
                            break;
                        }
                        break;

                case 4:                 // float[]
                        ArrayType flArray =
                            (ArrayType) execClass.getValue(fsfl).type();
                        ArrayReference newFlArray = flArray.newInstance(arraylength);

                        if (newFlArray.length() != arraylength) {
                            log3("ERROR : newFlArray.length() != arraylength   " + newFlArray.length());
                            expresult = 1;
                            break;
                        }
                        if ( ((FloatValue) newFlArray.getValue(0)).value() != 0.0f ||
                             ((FloatValue) newFlArray.getValue(1)).value() != 0.0f ) {
                            log3("ERROR : newFlArray components != 0.0f");
                            expresult = 1;
                            break;
                        }
                        break;

                case 5:                 // int[]
                        ArrayType inArray =
                            (ArrayType) execClass.getValue(fsin).type();
                        ArrayReference newInArray = inArray.newInstance(arraylength);

                        if (newInArray.length() != arraylength) {
                            log3("ERROR : newInArray.length() != arraylength   " + newInArray.length());
                            expresult = 1;
                            break;
                        }
                        if ( ((IntegerValue) newInArray.getValue(0)).value() != 0 ||
                             ((IntegerValue) newInArray.getValue(1)).value() != 0 ) {
                            log3("ERROR : newInArray components != 0");
                            expresult = 1;
                            break;
                        }
                        break;

                case 6:                 // long[]
                        ArrayType lnArray =
                            (ArrayType) execClass.getValue(fsln).type();
                        ArrayReference newLnArray = lnArray.newInstance(arraylength);

                        if (newLnArray.length() != arraylength) {
                            log3("ERROR : newLnArray.length() != arraylength   " + newLnArray.length());
                            expresult = 1;
                            break;
                        }
                        if ( ((LongValue) newLnArray.getValue(0)).value() != 0L ||
                             ((LongValue) newLnArray.getValue(1)).value() != 0L) {
                            log3("ERROR : newLnArray components != 0L");
                            expresult = 1;
                            break;
                        }
                        break;

                case 7:                 // short[]
                        ArrayType shArray =
                            (ArrayType) execClass.getValue(fssh).type();
                        ArrayReference newShArray = shArray.newInstance(arraylength);

                        if (newShArray.length() != arraylength) {
                            log3("ERROR : newShArray.length() != arraylength   " + newShArray.length());
                            expresult = 1;
                            break;
                        }
                        if ( ((ShortValue) newShArray.getValue(0)).value() != (short) 0 ||
                             ((ShortValue) newShArray.getValue(1)).value() != (short) 0 ) {
                            log3("ERROR : newShArray components != (short) 0");
                            expresult = 1;
                            break;
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

        debuggee.resume();
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
