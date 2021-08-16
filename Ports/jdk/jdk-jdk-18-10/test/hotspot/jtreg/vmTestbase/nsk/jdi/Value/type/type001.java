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

package nsk.jdi.Value.type;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * Value.                                                       <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.Value.type()</code>                        <BR>
 * complies with its spec when a Value object represents        <BR>
 * an object of a primitive type.                               <BR>
 * <BR>
 * The cases for testing are as follows :               <BR>
 *                                                      <BR>
 * when a gebuggee executes the following :             <BR>
 *   static boolean bl = false;                         <BR>
 *   static byte    bt = 0;                             <BR>
 *   static char    ch = 0;                             <BR>
 *   static double  db = 0.0d;                          <BR>
 *   static float   fl = 0.0f;                          <BR>
 *   static int     in = 0;                             <BR>
 *   static long    ln = 0;                             <BR>
 *   static short   sh = 0;                             <BR>
 *                                                      <BR>
 * for each of the above primitive type variables,      <BR>
 * a coresponding Type object is created in a debugger  <BR>
 * by the statement like the following :                <BR>
 *                                                      <BR>
 * Type tbl = execClass.getValue(Field fsbl).type();    <BR>
 *                                                      <BR>
 * the following pair of statements don't throw         <BR>
 * ClassCastExceptions:                                 <BR>
 *                                                      <BR>
 *      PrimitiveType prtbl = (PrimitiveType) tbl;      <BR>
 *      BooleanType      bl = (BooleanType) prtbl;      <BR>
 *                                                      <BR>
 *      PrimitiveType prtbt = (PrimitiveType) tbt;      <BR>
 *      ByteType         bt = (ByteType) prtbt;         <BR>
 *              .                                       <BR>
 *              .                                       <BR>
 *      PrimitiveType prtsh = (PrimitiveType) tsh;      <BR>
 *      ShortType        sh = (ShortType) prtsh;        <BR>
 * <BR>
 */

public class type001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/Value/type/type001",
    sHeader2 = "--> type001: ",
    sHeader3 = "##> type001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new type001().runThis(argv, out);
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
        "nsk.jdi.Value.type.type001a";

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
        log2("type001a debuggee launched");
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

                case 0:                 // BooleanType
                        Type tbl = execClass.getValue(fsbl).type();
                        PrimitiveType prtbl = null;
                        try {
                            prtbl = (PrimitiveType) tbl;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: PrimitiveType prtbl = (PrimitiveType) tbl;");
                            break ;
                        }
                        try {
                            BooleanType bl = (BooleanType) prtbl;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: BooleanType blt = (BooleanType) prtbl;");
                        }
                        break;

                case 1:                 // ByteType
                        Type tbt = execClass.getValue(fsbt).type();
                        PrimitiveType prtbt = null;
                        try {
                            prtbt = (PrimitiveType) tbt;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: PrimitiveType ptbt = (PrimitiveType) tbt;");
                            break ;
                        }
                        try {
                            ByteType bt = (ByteType) prtbt;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: ByteType bt = (ByteType) ptbt;");
                        }
                        break;

                case 2:                 // CharType
                        Type tch = execClass.getValue(fsch).type();
                        PrimitiveType prtch = null;
                        try {
                            prtch = (PrimitiveType) tch;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: PrimitiveType prtch = (PrimitiveType) tch;");
                            break ;
                        }
                        try {
                            CharType ch = (CharType) prtch;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: CharType ch = (CharType) prtch;");
                        }
                        break;

                case 3:                 // DoubleType
                        Type tdb = execClass.getValue(fsdb).type();
                        PrimitiveType prtdb = null;
                        try {
                            prtdb = (PrimitiveType) tdb;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: PrimitiveType prtdb = (PrimitiveType) tdb;");
                            break ;
                        }
                        try {
                            DoubleType db = (DoubleType) prtdb;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: DoubleType db = (DoubleType) prtdb;");
                        }
                        break;

                case 4:                 // FloatType
                        Type tfl = execClass.getValue(fsfl).type();
                        PrimitiveType prtfl = null;
                        try {
                            prtfl = (PrimitiveType) tfl;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: PrimitiveType prtfl = (PrimitiveType) tfl;");
                            break ;
                        }
                        try {
                            FloatType fl = (FloatType) prtfl;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: FloatType fl = (FloatType) prtfl;");
                        }
                        break;

                case 5:                 // IntegerType
                        Type tin = execClass.getValue(fsin).type();
                        PrimitiveType prtin = null;
                        try {
                            prtin = (PrimitiveType) tin;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: PrimitiveType prtin = (PrimitiveType) tin;");
                            break ;
                        }
                        try {
                            IntegerType in = (IntegerType) prtin;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: IntegerType in = (IntegerType) prtin;");
                        }
                        break;

                case 6:                 // LongType
                        Type tln = execClass.getValue(fsln).type();
                        PrimitiveType prtln = null;
                        try {
                            prtln = (PrimitiveType) tln;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: PrimitiveType prtln = (PrimitiveType) tln;");
                            break ;
                        }
                        try {
                            LongType ln = (LongType) prtln;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: LongType ln = (LongType) prtln;");
                        }
                        break;

                case 7:                 // ShortType
                        Type tsh = execClass.getValue(fssh).type();
                        PrimitiveType prtsh = null;
                        try {
                            prtsh = (PrimitiveType) tsh;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: PrimitiveType prtsh = (PrimitiveType) tsh;");
                            break ;
                        }
                        try {
                            ShortType sh = (ShortType) prtsh;
                        } catch ( ClassCastException e ) {
                            expresult = 1;
                            log3("ERROR: ShortType sh = (ShortType) prtsh;");
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
            logHandler.complain("TEST FAILED");
        }
        return testExitCode;
    }
}
