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

package nsk.jdi.Method.argumentTypes;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * Method.                                                      <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.Method.argumentTypes()</code>              <BR>
 * complies with its spec when a type is one of PrimitiveTypes. <BR>
 * <BR>
 * The cases for testing are as follows.                <BR>
 *                                                      <BR>
 * When a gebuggee creates an object of                 <BR>
 * the following class type:                            <BR>
 *    class argumenttypes001aTestClass {                                        <BR>
 *        public void primitiveargsmethod (             <BR>
 *            boolean bl,                               <BR>
 *            byte    bt,                               <BR>
 *            char    ch,                               <BR>
 *            double  db,                               <BR>
 *            float   fl,                               <BR>
 *            int     in,                               <BR>
 *            long    l,                                <BR>
 *            short   sh ) {                            <BR>
 *                  return ;                            <BR>
 *        }                                             <BR>
 * }                                                    <BR>
 * for all of the above primitive type arguments,       <BR>
 * a debugger forms their corresponding Type objects    <BR>
 * BooleanType, ByteType, CharType, etc.                <BR>
 * <BR>
 */

public class argumenttypes001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/Method/argumentTypes/argumenttypes001",
    sHeader2 = "--> argumenttypes001: ",
    sHeader3 = "##> argumenttypes001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new argumenttypes001().runThis(argv, out);
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
        "nsk.jdi.Method.argumentTypes.argumenttypes001a";

    String mName = "nsk.jdi.Method.argumentTypes";

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
        log2("argumenttypes001a debuggee launched");
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

            List listOfDebuggeeClasses = vm.classesByName(mName + ".argumenttypes001aTestClass");
                if (listOfDebuggeeClasses.size() != 1) {
                    testExitCode = FAILED;
                    log3("ERROR: listOfDebuggeeClasses.size() != 1");
                    break ;
                }

            List   methods  = null;
            Method m        = null;
            Method m1       = null;
            List   argTypes = null;


            methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                methodsByName("primitiveargsmethod");
            m = (Method) methods.get(0);
            try {
                argTypes = m.argumentTypes();
            } catch ( ClassNotLoadedException e ) {
                log3("ERROR: CNLE: argTypes = m.argumentTypes();");
                testExitCode = FAILED;
                break;
            }

            int i2;

            for (i2 = 0; ; i2++) {

                int expresult = 0;

                log2("new check: #" + i2);

                switch (i2) {

                case 0:                 // boolean arg
                        BooleanType blType = null;
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("bl");
                        m1 = (Method) methods.get(0);
                        try {
                            blType = (BooleanType) m1.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (BooleanType) m1.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (BooleanType) m1.returnType();");
                            expresult = 1;
                            break;
                        }

                        if (!argTypes.contains(blType)) {
                            log3("ERROR: !argTypes.contains(blType)");
                            expresult = 1;
                            break;
                        }
                        break;

                case 1:                 // byte arg
                        ByteType btType = null;
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("bt");
                        m1 = (Method) methods.get(0);
                        try {
                            btType = (ByteType) m1.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (ByteType) m1.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (ByteType) m1.returnType();");
                            expresult = 1;
                            break;
                        }

                        if (!argTypes.contains(btType)) {
                            log3("ERROR: !argTypes.contains(btType)");
                            expresult = 1;
                            break;
                        }
                        break;

                case 2:                 // char arg
                        CharType chType = null;
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("ch");
                        m1 = (Method) methods.get(0);
                        try {
                            chType = (CharType) m1.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (CharType) m1.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: CharType) m1.returnType();");
                            expresult = 1;
                            break;
                        }

                        if (!argTypes.contains(chType)) {
                            log3("ERROR: !argTypes.contains(chType)");
                            expresult = 1;
                            break;
                        }
                        break;

                case 3:                 // double arg
                        DoubleType dbType = null;
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("db");
                        m1 = (Method) methods.get(0);
                        try {
                            dbType = (DoubleType) m1.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (DoubleType) m1.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (DoubleType) m1.returnType();");
                            expresult = 1;
                            break;
                        }

                        if (!argTypes.contains(dbType)) {
                            log3("ERROR: !argTypes.contains(dbType)");
                            expresult = 1;
                            break;
                        }
                        break;

                case 4:                 // float arg
                        FloatType flType = null;
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("fl");
                        m1 = (Method) methods.get(0);
                        try {
                            flType = (FloatType) m1.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (FloatType) m1.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (FloatType) m1.returnType();");
                            expresult = 1;
                            break;
                        }

                        if (!argTypes.contains(flType)) {
                            log3("ERROR: !argTypes.contains(flType)");
                            expresult = 1;
                            break;
                        }
                        break;
                case 5:                 // int arg
                        IntegerType inType = null;
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("in");
                        m1 = (Method) methods.get(0);
                        try {
                            inType = (IntegerType) m1.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (IntegerType) m1.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (IntegerType) m1.returnType();");
                            expresult = 1;
                            break;
                        }

                        if (!argTypes.contains(inType)) {
                            log3("ERROR: !argTypes.contains(inType)");
                            expresult = 1;
                            break;
                        }
                        break;

                case 6:                 // long arg
                        LongType lnType = null;
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("ln");
                        m1 = (Method) methods.get(0);
                        try {
                            lnType = (LongType) m1.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (LongType) m1.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (LongType) m1.returnType();");
                            expresult = 1;
                            break;
                        }

                        if (!argTypes.contains(lnType)) {
                            log3("ERROR: !argTypes.contains(lnType)");
                            expresult = 1;
                            break;
                        }
                        break;

                case 7:                 // short arg
                        ShortType shType = null;
                        methods = ((ReferenceType) listOfDebuggeeClasses.get(0)).
                           methodsByName("sh");
                        m1 = (Method) methods.get(0);
                        try {
                            shType = (ShortType) m1.returnType();
                        } catch ( ClassCastException e1 ) {
                            log3("ERROR: CCE: (ShortType) m1.returnType();");
                            expresult = 1;
                            break;
                        } catch ( ClassNotLoadedException e2 ) {
                            log3("ERROR: CNLE: (ShortType) m1.returnType();");
                            expresult = 1;
                            break;
                        }

                        if (!argTypes.contains(shType)) {
                            log3("ERROR: !argTypes.contains(shType)");
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
