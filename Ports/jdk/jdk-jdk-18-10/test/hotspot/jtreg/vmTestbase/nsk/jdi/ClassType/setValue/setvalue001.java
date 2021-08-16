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

package nsk.jdi.ClassType.setValue;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * ClassType.                                                   <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ClassType.setValue()</code>                <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The cases for testing include only the primitive types.              <BR>
 * After being started up,                                              <BR>
 * a debuggee creates a TestClass object containing fields of each of   <BR>
 * the primitive types, and informs a debugger of the object creation.  <BR>
 * Upon the receiption a message from the debuggee, the debugger        <BR>
 * performs the following for each of the primitive types:              <BR>
 * - gets corresponding <type>Value objects using the method            <BR>
 *        ReferenceType.getValue(Field field2),                         <BR>
 * - sets corresponding <type>Value objects using the method            <BR>
 *        ReferenceType.getValue(Field field1),                         <BR>
 * - gets corresponding <type>Value objects using the method            <BR>
 *        ReferenceType.getValue(Field field1),                         <BR>
 * - checks up that field1 holds new value.                             <BR>
 * <BR>
 */

public class setvalue001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ClassType/setValue/setvalue001 ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new setvalue001().runThis(argv, out);
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
        "nsk.jdi.ClassType.setValue.setvalue001a";

    String mName = "nsk.jdi.ClassType.setValue";

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

            String testedclassName = mName + ".setvalue001aTestClass";

            ReferenceType testedclass = null;

            log2("       getting: List classes = vm.classesByName(testedclassName); expected size == 1");
            List classes = vm.classesByName(testedclassName);
            int size = classes.size();
            if (size != 1) {
                log3("ERROR: classes.size() != 1 : " + size);
                testExitCode = FAILED;
                break ;
            }

            log2("      getting ReferenceType and ClassType objects for setvalue001aTestClass");
            testedclass = (ReferenceType) classes.get(0);

            ClassType tClass = (ClassType) testedclass;


            String bl1 = "bl1", bl2 = "bl2";
            String bt1 = "bt1", bt2 = "bt2";
            String ch1 = "ch1", ch2 = "ch2";
            String db1 = "db1", db2 = "db2";
            String fl1 = "fl1", fl2 = "fl2";
            String in1 = "in1", in2 = "in2";
            String ln1 = "ln1", ln2 = "ln2";
            String sh1 = "sh1", sh2 = "sh2";

            Field field1 = null;
            Field field2 = null;

            log2("      loop of testing all primitive types");
            for (int i3 = 0; i3 < 8; i3++) {

                try {

                    switch (i3) {

                    case 0:
                            log2("...... boolean values");
                            BooleanValue blv1 = null;
                            BooleanValue blv2 = null;

                            field1 = testedclass.fieldByName(bl1);
                            field2 = testedclass.fieldByName(bl2);
                            if (field1 == null || field2 == null) {
                                log3("ERROR: 'field1 == null || field2 == null'  for Boolean");
                                testExitCode = FAILED;
                                break;
                            }

                            blv2 = (BooleanValue) testedclass.getValue(field2);
                            tClass.setValue(field1, blv2);
                            blv1 = (BooleanValue) testedclass.getValue(field1);
                            if (blv1.value() != false) {
                                log3("ERROR: blv1 != false :  " + blv1.value() );
                                testExitCode = FAILED;
                            }

                            break;


                    case 1:
                            log2("...... byte values");
                            ByteValue btv1 = null;
                            ByteValue btv2 = null;

                            field1 = testedclass.fieldByName(bt1);
                            field2 = testedclass.fieldByName(bt2);
                            if (field1 == null || field2 == null) {
                                log3("ERROR: 'field1 == null || field2 == null'  for Byte");
                                testExitCode = FAILED;
                                break;
                            }

                            btv2 = (ByteValue) testedclass.getValue(field2);
                            tClass.setValue(field1, btv2);
                            btv1 = (ByteValue) testedclass.getValue(field1);
                            if (btv1.value() != 1) {
                                log3("ERROR: btv1 != 1 :  " + btv1.value() );
                                testExitCode = FAILED;
                            }

                            break;


                    case 2:
                            log2("...... char values");
                            CharValue chv1 = null;
                            CharValue chv2 = null;

                            field1 = testedclass.fieldByName(ch1);
                            field2 = testedclass.fieldByName(ch2);
                            if (field1 == null || field2 == null) {
                                log3("ERROR: 'field1 == null || field2 == null'  for Char");
                               testExitCode = FAILED;
                               break;
                            }

                            chv2 = (CharValue) testedclass.getValue(field2);
                            tClass.setValue(field1, chv2);
                            chv1 = (CharValue) testedclass.getValue(field1);
                            if (chv1.value() != 1) {
                                log3("ERROR: chv1 != 1 :  " + chv1.value() );
                                testExitCode = FAILED;
                            }

                            break;


                    case 3:
                            log2("...... double values");
                            DoubleValue dbv1 = null;
                            DoubleValue dbv2 = null;

                            field1 = testedclass.fieldByName(db1);
                            field2 = testedclass.fieldByName(db2);
                            if (field1 == null || field2 == null) {
                                log3("ERROR: 'field1 == null || field2 == null'  for Double");
                                testExitCode = FAILED;
                                break;
                            }

                            dbv2 = (DoubleValue) testedclass.getValue(field2);
//                            log2("1    : dbv2 = " + Double.doubleToRawLongBits(dbv2.value()) );
                            tClass.setValue(field1, dbv2);
                            dbv1 = (DoubleValue) testedclass.getValue(field1);
//                            log2("2    : dbv1 = " + Double.doubleToRawLongBits(dbv1.value()) );
                            if (dbv1.value() != 1111111111.0d) {
                                log3("ERROR: dbv1 != 1111111111.0d :  " + dbv1.value() );
                                testExitCode = FAILED;
                            }

                            break;


                    case 4:
                            log2("...... float values");
                            FloatValue flv1 = null;
                            FloatValue flv2 = null;

                            field1 = testedclass.fieldByName(fl1);
                            field2 = testedclass.fieldByName(fl2);
                            if (field1 == null || field2 == null) {
                                log3("ERROR: 'field1 == null || field2 == null'  for Float");
                                testExitCode = FAILED;
                                break;
                            }

                            flv2 = (FloatValue) testedclass.getValue(field2);
                            tClass.setValue(field1, flv2);
                            flv1 = (FloatValue) testedclass.getValue(field1);
                            if (flv1.value() != 1111111111.0f) {
                                log3("ERROR: flv1 != 1111111111.0f :  " + flv1.value() );
                                testExitCode = FAILED;
                            }

                            break;

                    case 5:
                            log2("...... int values");
                            IntegerValue inv1 = null;
                            IntegerValue inv2 = null;

                            field1 = testedclass.fieldByName(in1);
                            field2 = testedclass.fieldByName(in2);
                            if (field1 == null || field2 == null) {
                            log3("ERROR: 'field1 == null || field2 == null'  for Integer");
                                testExitCode = FAILED;
                                break;
                            }

                            inv2 = (IntegerValue) testedclass.getValue(field2);
                            tClass.setValue(field1, inv2);
                            inv1 = (IntegerValue) testedclass.getValue(field1);
                            if (inv1.value() != 1) {
                                log3("ERROR: inv1 != 1 :  " + inv1.value() );
                                testExitCode = FAILED;
                            }

                            break;


                    case 6:
                            log2("...... long values");
                            LongValue lnv1 = null;
                            LongValue lnv2 = null;

                            field1 = testedclass.fieldByName(ln1);
                            field2 = testedclass.fieldByName(ln2);
                            if (field1 == null || field2 == null) {
                                log3("ERROR: 'field1 == null || field2 == null'  for Long");
                                testExitCode = FAILED;
                                break;
                            }

                            lnv2 = (LongValue) testedclass.getValue(field2);
                            lnv1 = (LongValue) testedclass.getValue(field1);
                            tClass.setValue(field1, lnv2);
                            lnv1 = (LongValue) testedclass.getValue(field1);
                            if (lnv1.value() != 0x1234567890abcdefL) {
                                log3("ERROR: lnv1 != 0x1234567890abcdefL :  "
                                           + Long.toHexString(lnv1.value()) );
                                testExitCode = FAILED;
                            }

                            break;


                    case 7:
                            log2("...... short values");
                            ShortValue shv1 = null;
                            ShortValue shv2 = null;

                            field1 = testedclass.fieldByName(sh1);
                            field2 = testedclass.fieldByName(sh2);
                            if (field1 == null || field2 == null) {
                                log3("ERROR: 'field1 == null || field2 == null'  for Short");
                                testExitCode = FAILED;
                                break;
                            }

                            shv2 = (ShortValue) testedclass.getValue(field2);
                            tClass.setValue(field1, shv2);
                            shv1 = (ShortValue) testedclass.getValue(field1);
                            if (shv1.value() != 1) {
                                log3("ERROR: shv1 != 1 :  " + shv1.value() );
                                testExitCode = FAILED;
                            }

                            break;


                  default : log3("ERROR: TEST ERROR:  case: default:");
                            testExitCode = FAILED;
                            break;

                        }  // end of switch

                    } catch ( Exception e ) {
                        log3("ERROR:  unexpected Exception:  " + e);
                        testExitCode = FAILED;
                } // end of try

            } // end of for



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
