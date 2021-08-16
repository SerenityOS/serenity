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

package nsk.jdi.ObjectReference.getValue;

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
 * <code>com.sun.jdi.ObjectReference.getValue()</code>          <BR>
 * complies with its specification.                             <BR>
 * <BR>
 * The cases for testing include fields of following ClassTypes:<BR>
 *   Class type, Interface type, Class Array type,  and         <BR>
 *   primitive Array type                                       <BR>
 * The test works as follows.                                   <BR>
 * Upon launch, a debuggee informs a debugger of creating       <BR>
 * a tested object.  The debugger gets mirrors of fields to test<BR>
 * and compares types of returned Value object to expected ones.<BR>
 */

public class getvalue003 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ObjectReference/getValue/getvalue003  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new getvalue003().runThis(argv, out);
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
        "nsk.jdi.ObjectReference.getValue.getvalue003a";

    private String testedClassName =
        "nsk.jdi.ObjectReference.getValue.getvalue003aTestClass";

    //String mName = "nsk.jdi.ObjectReference.getValue";

    //====================================================== test program
    //------------------------------------------------------ common section

    static ArgumentHandler      argsHandler;

    static int waitTime;

    static VirtualMachine  vm = null;

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

            List classes = null;

            log2("......getting: List classes = vm.classesByName(debuggeeName); expected size == 1");
            classes = vm.classesByName(debuggeeName);
            int size = classes.size();
            if (size != 1) {
                log3("ERROR: classes.size() != 1 : " + size);
                testExitCode = FAILED;
                break ;
            }


            log2("      getting ReferenceType and ClassType objects for debuggeeClass");
            ReferenceType debuggeeClass = (ReferenceType) classes.get(0);


            String objName            = "testObj";

            String tClassObjName      = "class2_0";
            String tClassArrayObjName = "cfc_0";
            String tIfaceObjName      = "iface_0";
            String tPrimArrayObjName  = "bl1";


            String className           = "nsk.jdi.ObjectReference.getValue.getvalue003aTestClass";

            String tClassTypeName      = "nsk.jdi.ObjectReference.getValue.getvalue003aClassForCheck_2";
            String tClassArrayTypeName = "nsk.jdi.ObjectReference.getValue.getvalue003aClassForCheck[]";
            String tIfaceTypeName      = "nsk.jdi.ObjectReference.getValue.getvalue003aClassForCheck_2";
            String tPrimArrayTypeName  = "boolean[][][][]";


            Field           field1  = null;
            Field           field2  = null;
            Value           val1    = null;
            Value           val2    = null;
            Type            type1   = null;
            String          str     = null;

            ObjectReference objRef  = null;

            ReferenceType   classRef  = null;


            log2("......getting the mirror of tested getvalue003aTestClass obj : ObjectReference objRef");

            field1  = debuggeeClass.fieldByName(objName);
            val1    = debuggeeClass.getValue(field1);
            objRef  = (ObjectReference) val1;

            log2("......getting the mirror of tested getvalue003aTestClass : ReferenceType classRef");

            classes   = vm.classesByName(testedClassName);
            classRef  = (ReferenceType) classes.get(0);


            log2("......checking up on class ClassType Field: getvalue003aClassForCheck_2 class2_0");

            field2    = classRef.fieldByName(tClassObjName);
            val2      = objRef.getValue(field2);
            type1     = val2.type();
            str       = type1.name();

            if (!str.equals(tClassTypeName)) {
                log3("ERROR: type of Value != nsk.jdi.ObjectReference.getValue.getvalue003aClassForCheck_2");
                log3("ERROR: type of Value == " + str);
                testExitCode = FAILED;
            }


            log2("......checking up on class ArrayType Field: getvalue003aClassForCheck[] cfc_0");

            field2    = classRef.fieldByName(tClassArrayObjName);
            val2      = objRef.getValue(field2);
            type1     = val2.type();
            str       = type1.name();

            if (!str.equals(tClassArrayTypeName)) {
                log3("ERROR: type of Value != nsk.jdi.ObjectReference.getValue.getvalue003aClassForCheck[]");
                log3("ERROR: type of Value == " + str);
                testExitCode = FAILED;
            }


            log2("......checking up on class InterfaceType Field: getvalue003aInterfaceForCheck iface_0");

            field2    = classRef.fieldByName(tIfaceObjName);
            val2      = objRef.getValue(field2);
            type1     = val2.type();
            str       = type1.name();

            if (!str.equals(tIfaceTypeName)) {
                log3("ERROR: type of Value != nsk.jdi.ObjectReference.getValue.getvalue003aClassForCheck_2");
                log3("ERROR: type of Value == " + str);
                testExitCode = FAILED;
            }


            log2("......checking up on class PrimitiveArrayType Field: boolean[][][][] bl1");

            field2    = classRef.fieldByName(tPrimArrayObjName);
            val2      = objRef.getValue(field2);
            type1     = val2.type();
            str       = type1.name();

            if (!str.equals(tPrimArrayTypeName)) {
                log3("ERROR: type of Value != boolean[][][][]");
                log3("ERROR: type of Value == " + str);
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
