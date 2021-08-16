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

package nsk.jdi.LocalVariable.type;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * LocalVariable.                                               <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.LocalVariable.type()</code>                <BR>
 * complies with its spec when LocalVariable is one of          <BR>
 * ReferenceTypes.                                              <BR>
 * <BR>
 * The cases for testing are as follows.                <BR>
 *                                                      <BR>
 * When a gebuggee creates an object containing         <BR>
 * the following method:                                <BR>
 *  public  void testmethod (int param) {               <BR>
 *    ClassForCheck_2   class2 = new ClassForCheck_2(); <BR>
 *    InterfaceForCheck iface  = class2;                <BR>
 *    ClassForCheck     cfc[]  = { new ClassForCheck(), <BR>
 *                              new ClassForCheck() };  <BR>
 *    return;                                           <BR>
 *  }                                                   <BR>
 * a debugger checks up that the method                 <BR>
 * LocalVariable.type() applied to each of ReferenceType<BR>
 * method variable returns a Type object casted to      <BR>
 * a corresponding ReferenceType.                       <BR>
 * <BR>
 */

public class type002 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/LocalVariable/type/type002  ",
    sHeader2 = "--> type002: ",
    sHeader3 = "##> type002: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new type002().runThis(argv, out);
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
        "nsk.jdi.LocalVariable.type.type002a";

    String mName = "nsk.jdi.LocalVariable.type";

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
        log2("type002a debuggee launched");
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

            List listOfLoadedClasses = vm.classesByName(mName + ".type002aTestClass");

            if (listOfLoadedClasses.size() != 1) {
                testExitCode = FAILED;
                log3("ERROR: listOfLoadedClasses.size() != 1   " +
                     listOfLoadedClasses.size());
                break ;
            }

            List methods =
                ((ReferenceType) listOfLoadedClasses.get(0)).
                                 methodsByName("testmethod");

            Method testMethod = (Method) methods.get(0);


            String names[] = { "class2", "iface", "cfc" };

            int i2;
            int expresult = 0;

            for (i2 = 0; i2 < names.length; i2++) {

                log2("new check: #" + i2);

                List lVars = null;
                try {
                    lVars = testMethod.variablesByName(names[i2]);
                } catch ( AbsentInformationException e ) {
                    log3("ERROR: AbsentInformationException for " +
                         "lVars = testMethod.variablesByName(names[i2])" );
                    testExitCode = FAILED;
                    continue;
                }
                if (lVars.size() != 1) {
                    testExitCode = FAILED;
                    log3("ERROR: lVars.size() != 1 for i2=" + i2 + "  : " + lVars.size());
                    continue;
                }

                LocalVariable lVar = (LocalVariable) lVars.get(0);

                Type lVarType = null;
                try {
                    lVarType = lVar.type();
                } catch ( ClassNotLoadedException e) {
                    log3("ERROR : ClassNotLoadedExc for  lVarType = lVar.type();");
                    testExitCode = FAILED;
                    continue;
                }

                switch (i2) {

                case 0:                 // ClassType
                        try {
                            ClassType ct = (ClassType) lVarType;
                        } catch ( ClassCastException e ) {
                            testExitCode = FAILED;
                            log3("ERROR: ClassType ct = (ClassType) lVarType;");
                        }

                        break;

                case 1:                 // InterfaceType

                        try {
                            InterfaceType it = (InterfaceType) lVarType;
                        } catch ( ClassCastException e ) {
                            testExitCode = FAILED;
                            log3("ERROR: InterfaceType it = (InterfaceType) lVarType;");
                        }
                        break;

                case 2:                 // ArrayType
                        try {
                            ArrayType at = (ArrayType) lVarType;
                        } catch ( ClassCastException e ) {
                            testExitCode = FAILED;
                            log3("ERROR: ArrayType at = (ArrayType) lVarType;");
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
