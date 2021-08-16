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
 * an object of a ReferenceType.                                <BR>
 * <BR>
 * The cases for testing are as follows :               <BR>
 *                                                      <BR>
 * when a gebuggee created the following objects:       <BR>
 *   static ClassForCheck_2   classObj =                <BR>
 *       new ClassForCheck_2();                         <BR>
 *   static InterfaceForCheck ifaceObj  = classObj;     <BR>
 *   static ClassForCheck[]     cfcObj  =               <BR>
 *       { new ClassForCheck(), new ClassForCheck() };  <BR>
 *                                                      <BR>
 * for each of the above refrence type static fields,   <BR>
 * a coresponding Type object named                     <BR>
 *    classFieldType,                                   <BR>
 *    ifaceFieldType, and                               <BR>
 *    arrayFieldType                                    <BR>
 * are created in a debugger, which then checks up that <BR>
 * the following statements                             <BR>
 * don't throw ClassCastExceptions:                     <BR>
 *    ClassType ct = (ClassType) classFieldType;        <BR>
 *    ClassType ct = (ClassType) ifaceFieldType;        <BR>
 *    ArrayType at = (ArrayType) arrayFieldType;        <BR>
 * whereas the statement                                <BR>
 *   InterfaceType it = (InterfaceType) ifaceFieldType; <BR>
 * does throw the exception.                            <BR>
 * <BR>
 */

public class type003 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/Value/type/type003",
    sHeader2 = "--> type003: ",
    sHeader3 = "##> type003: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new type003().runThis(argv, out);
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
        "nsk.jdi.Value.type.type003a";

    String mName = "nsk.jdi.Value.type";

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
        log2("type003a debuggee launched");
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

            log1("new checkready: #" + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            List listOfLoadedClasses = vm.classesByName(mName + ".TestClass");

            if (listOfLoadedClasses.size() != 1) {
                testExitCode = FAILED;
                log3("ERROR: listOfLoadedClasses.size() != 1   " +
                     listOfLoadedClasses.size());
                break ;
            }

            ReferenceType testedClass = (ReferenceType) listOfLoadedClasses.get(0);

            int i2;
            int expresult = 0;

            for (i2 = 0; ; i2++) {

                log2("new check: #" + i2);

                switch (i2) {

                case 0:                 // ClassType

                        String classFieldName = "classObj";
                        Field classField =
                              testedClass.fieldByName(classFieldName);

                        Value classFieldValue = testedClass.getValue(classField);
                        Type classFieldType = classFieldValue.type();

                        try {
                            ClassType ct = (ClassType) classFieldType;
                            log2("     : ClassType ct = (ClassType) classFieldType;");
                        } catch ( ClassCastException e ) {
                            testExitCode = FAILED;
                            log3("ERROR: ClassType ct = (ClassType) classFieldType;");
                        }

                        break;

                case 1:                 // InterfaceType

                        String ifaceFieldName = "ifaceObj";
                        Field ifaceField =
                              testedClass.fieldByName(ifaceFieldName);

                        Value ifaceFieldValue = testedClass.getValue(ifaceField);
                        Type ifaceFieldType = ifaceFieldValue.type();

                        try {
                            InterfaceType it = (InterfaceType) ifaceFieldType;
                            testExitCode = FAILED;
                            log3("ERROR: InterfaceType it = (InterfaceType) ifaceFieldType;");
                        } catch ( ClassCastException e ) {
                            log2("     : ClassCastException for: InterfaceType it = (InterfaceType) ifaceFieldType;");
                        }
                        try {
                            ClassType ct = (ClassType) ifaceFieldType;
                            log2("     : ClassType ct = (ClassType) ifaceFieldType;");
                        } catch ( ClassCastException e ) {
                            testExitCode = FAILED;
                            log3("ERROR: ClassCastException for: ClassType ct = (ClassType) ifaceFieldType;");
                        }
                        break;

                case 2:                 // ArrayType

                        String arrayFieldName = "cfcObj";
                        Field arrayField =
                              testedClass.fieldByName(arrayFieldName);

                        Value arrayFieldValue = testedClass.getValue(arrayField);
                        Type arrayFieldType = arrayFieldValue.type();

                        try {
                            ArrayType at = (ArrayType) arrayFieldType;
                            log2("     : ArrayType at = (ArrayType) arrayFieldType;");
                        } catch ( ClassCastException e ) {
                            testExitCode = FAILED;
                            log3("ERROR: ArrayType at = (ArrayType) arrayFieldType;");
                        }

                        break;


                default: expresult = 2;
                         break ;
                }

                if (expresult == 2) {
                    log2("      test cases finished");
                    break ;
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
