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

package nsk.jdi.ObjectReference.setValue;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * ObjectReference.                                             <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ObjectReference.setValue()</code>          <BR>
 * complies with its specification.                             <BR>
 * The cases for testing include values of class and instance   <BR>
 * variables of primitive types each.                           <BR>
 * No Exception is expected to be thrown.                       <BR>
 */

public class setvalue001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ObjectReference/setValue/setvalue001  ",
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
        "nsk.jdi.ObjectReference.setValue.setvalue001a";

    private String testedClassName =
        "nsk.jdi.ObjectReference.setValue.TestClass";

    //String mName = "nsk.jdi.ObjectReference.setValue";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine   vm  = null;

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

            String testObjName = "obj";

            ReferenceType   testedClass   = null;
            ReferenceType   debuggeeClass = null;
            ObjectReference objRef        = null;

            List classes = null;

            classes     = vm.classesByName(debuggeeName);
            debuggeeClass = (ReferenceType) classes.get(0);

            objRef = (ObjectReference)
                   debuggeeClass.getValue(debuggeeClass.fieldByName(testObjName));

            classes       = vm.classesByName(testedClassName);
            testedClass   = (ReferenceType) classes.get(0);


            Field fsbl1 = testedClass.fieldByName("bl1");
            Field fsbt1 = testedClass.fieldByName("bt1");
            Field fsch1 = testedClass.fieldByName("ch1");
            Field fsdb1 = testedClass.fieldByName("db1");
            Field fsfl1 = testedClass.fieldByName("fl1");
            Field fsin1 = testedClass.fieldByName("in1");
            Field fsln1 = testedClass.fieldByName("ln1");
            Field fssh1 = testedClass.fieldByName("sh1");

            Field fsbl2 = testedClass.fieldByName("bl2");
            Field fsbt2 = testedClass.fieldByName("bt2");
            Field fsch2 = testedClass.fieldByName("ch2");
            Field fsdb2 = testedClass.fieldByName("db2");
            Field fsfl2 = testedClass.fieldByName("fl2");
            Field fsin2 = testedClass.fieldByName("in2");
            Field fsln2 = testedClass.fieldByName("ln2");
            Field fssh2 = testedClass.fieldByName("sh2");


            log2("......loop of checks on each primitive type by performing statements like first ones:");
            log2("            BooleanValue blv1 = (BooleanValue) objRef.getValue(fsbl1);");
            log2("            BooleanValue blv2 = (BooleanValue) objRef.getValue(fsbl2);");
            log2("            boolean bl1 = blv1.value();");
            log2("            boolean bl2 = blv2.value();");
            log2("            objRef.setValue(fsbl1, blv2);");
            log2("            objRef.setValue(fsbl2, blv1);");
            log2("            blv1 = (BooleanValue) objRef.getValue(fsbl1);");
            log2("            if (blv1.value() != false) {");
            log2("            log3('ERROR: getValue(fsbl1) != false');");
            log2("            }");
            log2("            blv2 = (BooleanValue) objRef.getValue(fsbl2);");
            log2("            if (blv2.value() != true) {");
            log2("            log3('ERROR: getValue(fsbl2) != true');");
            log2("            }");

            for ( int i3 = 0; i3 < 8; i3++) {

                try {

                    switch (i3) {

                    case 0:
                            log2("      checking up on boolean");

                            BooleanValue blv1 = (BooleanValue) objRef.getValue(fsbl1);
                            BooleanValue blv2 = (BooleanValue) objRef.getValue(fsbl2);

                            boolean bl1 = blv1.value();
                            boolean bl2 = blv2.value();

                            objRef.setValue(fsbl1, blv2);
                            objRef.setValue(fsbl2, blv1);

                            blv1 = (BooleanValue) objRef.getValue(fsbl1);
                            if (blv1.value() != bl2) {
                                log3("ERROR: getValue(fsbl1) != bl2");
                                testExitCode = FAILED;
                            }
                            blv2 = (BooleanValue) objRef.getValue(fsbl2);
                            if (blv2.value() != bl1) {
                                log3("ERROR: getValue(fsbl2) != bl1");
                                testExitCode = FAILED;
                            }
                            break;

                    case 1:
                            log2("      checking up on byte");

                            ByteValue btv1 = (ByteValue) objRef.getValue(fsbt1);
                            ByteValue btv2 = (ByteValue) objRef.getValue(fsbt2);

                            byte bt1 = btv1.value();
                            byte bt2 = btv2.value();

                            objRef.setValue(fsbt1, btv2);
                            objRef.setValue(fsbt2, btv1);

                            btv1 = (ByteValue) objRef.getValue(fsbt1);
                            if (btv1.value() != bt2) {
                                log3("ERROR: getValue(fsbt1) != bt2");
                                testExitCode = FAILED;
                            }
                            btv2 = (ByteValue) objRef.getValue(fsbt2);
                            if (btv2.value() != bt1) {
                                log3("ERROR: getValue(fsbt2) != bt1");
                                testExitCode = FAILED;
                            }
                            break;

                    case 2:
                            log2("      checking up on char");

                            CharValue chv1 = (CharValue) objRef.getValue(fsch1);
                            CharValue chv2 = (CharValue) objRef.getValue(fsch2);

                            char ch1 = chv1.value();
                            char ch2 = chv2.value();

                            objRef.setValue(fsch1, chv2);
                            objRef.setValue(fsch2, chv1);

                            chv1 = (CharValue) objRef.getValue(fsch1);
                            if (chv1.value() != ch2) {
                                log3("ERROR: getValue(fsch1) != ch2");
                                testExitCode = FAILED;
                            }
                            chv2 = (CharValue) objRef.getValue(fsch2);
                            if (chv2.value() != ch1) {
                                log3("ERROR: getValue(fsch2) != ch1");
                                testExitCode = FAILED;
                            }
                            break;

                    case 3:
                            log2("      checking up on double");

                            DoubleValue dbv1 = (DoubleValue) objRef.getValue(fsdb1);
                            DoubleValue dbv2 = (DoubleValue) objRef.getValue(fsdb2);

                            double db1 = dbv1.value();
                            double db2 = dbv2.value();

                            objRef.setValue(fsdb1, dbv2);
                            objRef.setValue(fsdb2, dbv1);

                            dbv1 = (DoubleValue) objRef.getValue(fsdb1);
                            if (dbv1.value() != db2) {
                                log3("ERROR: getValue(fsdb1) != db2");
                                testExitCode = FAILED;
                            }
                            dbv2 = (DoubleValue) objRef.getValue(fsdb2);
                            if (dbv2.value() != db1) {
                                log3("ERROR: getValue(fsdb2) != db1");
                                testExitCode = FAILED;
                            }
                            break;

                    case 4:
                            log2("      checking up on float");

                            FloatValue flv1 = (FloatValue) objRef.getValue(fsfl1);
                            FloatValue flv2 = (FloatValue) objRef.getValue(fsfl2);

                            float fl1 = flv1.value();
                            float fl2 = flv2.value();

                            objRef.setValue(fsfl1, flv2);
                            objRef.setValue(fsfl2, flv1);

                            flv1 = (FloatValue) objRef.getValue(fsfl1);
                            if (flv1.value() != fl2) {
                                log3("ERROR: getValue(fsfl1) != fl2");
                                testExitCode = FAILED;
                            }
                            flv2 = (FloatValue) objRef.getValue(fsfl2);
                            if (flv2.value() != fl1) {
                                log3("ERROR: getValue(fsfl2) != fl1");
                                testExitCode = FAILED;
                            }
                            break;

                    case 5:
                            log2("      checking up on int");

                            IntegerValue inv1 = (IntegerValue) objRef.getValue(fsin1);
                            IntegerValue inv2 = (IntegerValue) objRef.getValue(fsin2);

                            int in1 = inv1.value();
                            int in2 = inv2.value();

                            objRef.setValue(fsin1, inv2);
                            objRef.setValue(fsin2, inv1);

                            inv1 = (IntegerValue) objRef.getValue(fsin1);
                            if (inv1.value() != in2) {
                                log3("ERROR: getValue(fsin1) != in2");
                                testExitCode = FAILED;
                            }
                            inv2 = (IntegerValue) objRef.getValue(fsin2);
                            if (inv2.value() != in1) {
                                log3("ERROR: getValue(fsin2) != in1");
                                testExitCode = FAILED;
                            }
                            break;

                    case 6:
                            log2("      checking up on long");

                            LongValue lnv1 = (LongValue) objRef.getValue(fsln1);
                            LongValue lnv2 = (LongValue) objRef.getValue(fsln2);

                            long ln1 = lnv1.value();
                            long ln2 = lnv2.value();

                            objRef.setValue(fsln1, lnv2);
                            objRef.setValue(fsln2, lnv1);

                            lnv1 = (LongValue) objRef.getValue(fsln1);
                            if (lnv1.value() != ln2) {
                                log3("ERROR: getValue(fsln1) != ln2");
                                testExitCode = FAILED;
                            }
                            lnv2 = (LongValue) objRef.getValue(fsln2);
                            if (lnv2.value() != ln1) {
                                log3("ERROR: getValue(fsln2) != ln1");
                                testExitCode = FAILED;
                            }
                            break;

                    case 7:
                            log2("      checking up on short");

                            ShortValue shv1 = (ShortValue) objRef.getValue(fssh1);
                            ShortValue shv2 = (ShortValue) objRef.getValue(fssh2);

                            short sh1 = shv1.value();
                            short sh2 = shv2.value();

                            objRef.setValue(fssh1, shv2);
                            objRef.setValue(fssh2, shv1);

                            shv1 = (ShortValue) objRef.getValue(fssh1);
                            if (shv1.value() != sh2) {
                                log3("ERROR: getValue(fssh1) != sh2");
                                testExitCode = FAILED;
                            }
                            shv2 = (ShortValue) objRef.getValue(fssh2);
                            if (shv2.value() != sh1) {
                                log3("ERROR: getValue(fssh2) != sh1");
                                testExitCode = FAILED;
                            }
                            break;


                  default : log3("ERROR: TEST ERROR:  case: default:");
                            testExitCode = FAILED;
                            break;

                    }  // end of switch

                } catch ( Exception e ) {
                    log3("ERROR:  unexpected exception:  " + e);
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
