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

package nsk.jdi.ArrayType.componentType;

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
 * <code>com.sun.jdi.ArrayType.componentType()</code>           <BR>
 * complies with its spec when a type is one of ReferenceType(s).<BR>
 * <BR>
 * The test checks up that the component types of               <BR>
 * ReferenceType objects, corresponding in a debugger to        <BR>
 * the following in a debuggee:                                 <BR>
 *                                                                      <BR>
 *   a class type array      - "ClassForCheck_2 class3[]"  field,       <BR>
 *   an interface type array - "InterfaceForCheck iface[]" field,       <BR>
 *                                                                      <BR>
 * are ClassType and InterfaceType accordinly.                  <BR>
 * <BR>
 */

public class componenttype001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ArrayType/componentType/componenttype001",
    sHeader2 = "--> componenttype001: ",
    sHeader3 = "##> componenttype001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new componenttype001().runThis(argv, out);
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
        "nsk.jdi.ArrayType.componentType.componenttype001a";

    String mName = "nsk.jdi.ArrayType.componentType";

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
        log2("componenttype001a debuggee launched");
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

            List          listOfDebuggeeClasses = null;
            ReferenceType reftypeObj            = null;

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0 :                 // ClassType array

                        listOfDebuggeeClasses =
                            vm.classesByName(mName + ".ClassForCheck");
                        if (listOfDebuggeeClasses.size() != 1) {
                            expresult = 1;
                            log3("ERROR: for ClassType listOfDebuggeeClasses.size() != 1");
                            break ;
                        }
                        reftypeObj = (ReferenceType) listOfDebuggeeClasses.get(0);

                        Field classField = reftypeObj.fieldByName("class3");

                        try {
                            ClassType obj =
                                (ClassType) ( (ArrayType) classField.type() ).componentType();
                        } catch ( ClassNotLoadedException e ) {
                            log3("ERROR: (ClassType) ((ArrayType) classFieldType.type).componentType();");
                            expresult =1;
                            break ;
                        } catch ( ClassCastException e ) {
                            log3("ERROR: (ClassType) ((ArrayType) classFieldType.type).componentType();");
                            expresult =1;
                            break ;
                        }

                        break;


                case 1 :          // InterfaceType array

                        listOfDebuggeeClasses =
                            vm.classesByName(mName + ".ClassForCheck");
                        if (listOfDebuggeeClasses.size() != 1) {
                            expresult = 1;
                            log3("ERROR: for InterfaceType listOfDebuggeeClasses.size() != 1");
                            break ;
                        }
                        reftypeObj = (ReferenceType) listOfDebuggeeClasses.get(0);

                        Field ifaceField = reftypeObj.fieldByName("iface");

                        try {
                            InterfaceType obj =
                                (InterfaceType) ( (ArrayType) ifaceField.type() ).componentType();
                        } catch ( ClassNotLoadedException e ) {
                            log3("ERROR: (InterfaceType) ( (ArrayType) ifaceField.type() ).componentType();");
                            expresult =1;
                            break ;
                        } catch ( ClassCastException e ) {
                            log3("ERROR: (InterfaceType) ( (ArrayType) ifaceField.type() ).componentType();");
                            expresult =1;
                            break ;
                        }

                        break;

/*

                case 2 :         // PrimitiveType array  !!!!! throws ClassNotLoadedException

                        listOfDebuggeeClasses =
                            vm.classesByName(mName + ".ClassForCheck");
                        if (listOfDebuggeeClasses.size() != 1) {
                            expresult = 1;
                            log3("ERROR: for ArrayType listOfDebuggeeClasses.size() != 1");
                            break ;
                        }
                        reftypeObj = (ReferenceType) listOfDebuggeeClasses.get(0);

                        Field arrayField = reftypeObj.fieldByName("bl");

                        try {
                            ArrayType obj =
                                (ArrayType) ( (ArrayType) arrayField.type() ).componentType();
                        } catch ( ClassNotLoadedException e ) {
                            log3("ERROR: CNLE  (ArrayType) ( (ArrayType) arrayField.type() ).componentType();");
                            expresult =1;
                            break ;
                        } catch ( ClassCastException e ) {
                            log3("ERROR: CCE  (ArrayType) ( (ArrayType) arrayField.type() ).componentType();");
                            expresult =1;
                            break ;
                        }

                        break;
*/

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
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }
}
