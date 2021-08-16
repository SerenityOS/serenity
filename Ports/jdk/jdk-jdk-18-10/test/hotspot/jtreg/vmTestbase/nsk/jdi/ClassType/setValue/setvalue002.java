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
 * The test checks up on throwing IllegalArgumentException and  <BR>
 * InvalidTypeException.                                        <BR>
 * <BR>
 * The cases for testing include the primitive types and the array type.<BR>
 * After being started up,                                              <BR>
 * a debuggee creates a TestClass object containing fields of each of   <BR>
 * the primitive types and the array type,                              <BR>
 * and informs a debugger of the object creation.                       <BR>
 * Upon the receiption a message from the debuggee, the debugger        <BR>
 * gets the mirror of the object, ClassType tClass,                     <BR>
 * and performs the following checks:                                   <BR>
 * - invoking the method                                                <BR>
 *     tClass.setValue(Field field1, BooleanValue blv2)                 <BR>
 *   with non-static field1 throws  IllegalArgumentException;           <BR>                                    <BR>
 * - invoking the method                                                <BR>
 *     tClass.setValue(Field field1, ByteValue btv2)                    <BR>
 *   with final field1 throws  IllegalArgumentException;                <BR>
 * - invoking the method                                                <BR>
 *     tClass.setValue(Field field1, IntegerValue inv2)                 <BR>
 *   with field1 mirroring an static field within another debuggee's    <BR>
 *   class throws  IllegalArgumentException;                            <BR>
 * - invoking the method                                                <BR>
 *     tClass.setValue(Field field1, ByteValue btv2)                    <BR>
 *   with field1 mirroring not a primitive but an array type            <BR>
 *   throws  InvalidTypeException.                                      <BR>
 * <BR>
 */

public class setvalue002 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ClassType/setValue/setvalue002  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new setvalue002().runThis(argv, out);
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
        "nsk.jdi.ClassType.setValue.setvalue002a";

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


            String testedclassName = mName + ".setvalue002aTestClass";

            ReferenceType testedclass = null;

            log2("       getting: List classes = vm.classesByName(testedclassName); expected size == 1");
            List classes = vm.classesByName(testedclassName);
            int size = classes.size();
            if (size != 1) {
                log3("ERROR: classes.size() != 1 : " + size);
                testExitCode = FAILED;
                break ;
            }

            log2("      getting ReferenceType and ClassType objects for setvalue002aTestClass");
            testedclass = (ReferenceType) classes.get(0);

            ClassType tClass = (ClassType) testedclass;


            ReferenceType debuggeeclass = null;

            log2("       getting: List classes = vm.classesByName(debuggeeName); expected size == 1");
            classes = vm.classesByName(debuggeeName);
            size = classes.size();
            if (size != 1) {
                log3("ERROR: classes.size() != 1 : " + size);
                testExitCode = FAILED;
                break ;
            }

            log2("      getting ReferenceType and ClassType objects for debuggeeClass");
            debuggeeclass = (ReferenceType) classes.get(0);

            ClassType dClass = (ClassType) debuggeeclass;




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



            log2("...... checking up: not static field; IllegalArgumentException is expected");
            try {
                BooleanValue blv1 = null;
                BooleanValue blv2 = null;

                field1 = testedclass.fieldByName(bl1);
                field2 = testedclass.fieldByName(bl2);
                if (field1 == null || field2 == null) {
                    log3("ERROR: field1 == null || field2 == null");
                    testExitCode = FAILED;
                } else {
                    blv2 = (BooleanValue) testedclass.getValue(field2);
                    tClass.setValue(field1, blv2);
                    log3("ERROR: no IllegalArgumentException");
                    testExitCode = FAILED;
                }
            } catch ( IllegalArgumentException e1 ) {
                log2("         IllegalArgumentException");
            } catch ( Exception e2 ) {
                log3("ERROR: unexpected Exception: " + e2);
                testExitCode = FAILED;
            }

            log2("...... checking up: final field; IllegalArgumentException is expected");
            try {
                ByteValue btv1 = null;
                ByteValue btv2 = null;

                field1 = testedclass.fieldByName(bt1);
                field2 = testedclass.fieldByName(bt2);
                if (field1 == null || field2 == null) {
                    log3("ERROR: field1 == null || field2 == null");
                    testExitCode = FAILED;
                } else {
                    btv2 = (ByteValue) testedclass.getValue(field2);
                    tClass.setValue(field1, btv2);
                    log3("ERROR: no IllegalArgumentException");
                    testExitCode = FAILED;
                }
            } catch ( IllegalArgumentException e1 ) {
                log2("         IllegalArgumentException");
            } catch ( Exception e2 ) {
                log3("ERROR: unexpected Exception: " + e2);
                testExitCode = FAILED;
            }


            log2("...... checking up: field doesn't exist in this class; IllegalArgumentException is expected");
            try {
                IntegerValue inv1 = null;
                IntegerValue inv2 = null;

                field1 = debuggeeclass.fieldByName(in1);
                field2 = testedclass.fieldByName(in2);
                if (field1 == null || field2 == null) {
                    log3("ERROR: field1 == null || field2 == null");
                    testExitCode = FAILED;
                } else {
                    inv2 = (IntegerValue) testedclass.getValue(field2);
                    tClass.setValue(field1, inv2);
                    log3("ERROR: no IllegalArgumentException");
                    testExitCode = FAILED;
                }
            } catch ( IllegalArgumentException e1 ) {
                log2("         IllegalArgumentException");
            } catch ( Exception e2 ) {
                log3("ERROR: unexpected Exception: " + e2);
                testExitCode = FAILED;
            }


            log2("...... checking up: no match the field's declared type; InvalidTypeException is expected");
            try {
                ByteValue btv2 = null;
                String bta = "bta";

                field1 = testedclass.fieldByName(bta);
                field2 = testedclass.fieldByName(bt2);
                if (field1 == null || field2 == null) {
                    log3("ERROR: field1 == null || field2 == null");
                    testExitCode = FAILED;
                } else {
                    btv2 = (ByteValue) testedclass.getValue(field2);
                    tClass.setValue(field1, btv2);
                    log3("ERROR: no InvalidTypeException");
                    testExitCode = FAILED;
                }
            } catch ( InvalidTypeException e1 ) {
                log2("         InvalidTypeException");
            } catch ( Exception e2 ) {
                log3("ERROR: unexpected Exception: " + e2);
                testExitCode = FAILED;
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
