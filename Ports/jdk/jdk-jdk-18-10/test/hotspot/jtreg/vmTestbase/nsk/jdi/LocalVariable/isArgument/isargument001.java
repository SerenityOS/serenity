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

package nsk.jdi.LocalVariable.isArgument;

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
 * <code>com.sun.jdi.LocalVariable.isArgument()</code>          <BR>
 * complies with its spec.                              <BR>
 * <BR>
 * The cases for testing are as follows.                <BR>
 *                                                      <BR>
 * When a gebuggee creates an object containing         <BR>
 * the following method:                                <BR>
 *   public  void testmethod (int param) {              <BR>
 *       boolean bl1 = false, bl2 = true;               <BR>
 *       byte    bt1 = 0,     bt2 = 1;                  <BR>
 *       char    ch1 = 0,     ch2 = 1;                  <BR>
 *       double  db1 = 0.0d,  db2 = 1.0d;               <BR>
 *       float   fl1 = 0.0f,  fl2 = 1.0f;               <BR>
 *       int     in1 = 0,     in2 = 1;                  <BR>
 *       long    ln1 = 0,     ln2 = 1;                  <BR>
 *       short   sh1 = 0,     sh2 = 1;                  <BR>
 *    ClassForCheck_2   class2 = new ClassForCheck_2(); <BR>
 *    InterfaceForCheck iface  = class2;                <BR>
 *    ClassForCheck     cfc[]  = { new ClassForCheck(), <BR>
 *                              new ClassForCheck() };  <BR>
 *      return;                                         <BR>
 *  }                                                   <BR>
 * a debugger checks up that the method                 <BR>
 * LocalVariable.isArgument() applied to each of        <BR>
 * method variable returns false for all the tested     <BR>
 * local variables and true for the argument "param".   <BR>
 * <BR>
 */

public class isargument001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/LocalVariable/isArgument/isargument001  ",
    sHeader2 = "--> isargument001: ",
    sHeader3 = "##> isargument001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new isargument001().runThis(argv, out);
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
        "nsk.jdi.LocalVariable.isArgument.isargument001a";

    String mName = "nsk.jdi.LocalVariable.isArgument";

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
        log2("isargument001a debuggee launched");
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

            List listOfLoadedClasses = vm.classesByName(mName + ".TestClass");

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


            String names[] = { "bl1", "bt1", "ch1", "db1",
                               "fl1", "in1", "ln1", "sh1",
                               "class2", "iface", "cfc", "param"  };

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

                switch (i2) {

                case 0:                 // BooleanType
                        if (lVar.isArgument()) {
                            testExitCode = FAILED;
                            log3("ERROR: lVar.isArgument for 'bl1'");
                        }
                        break;

                case 1:                 // ByteType
                        if (lVar.isArgument()) {
                            testExitCode = FAILED;
                            log3("ERROR: lVar.isArgument for 'bt1'");
                        }
                        break;

                case 2:                 // CharType
                        if (lVar.isArgument()) {
                            testExitCode = FAILED;
                            log3("ERROR: lVar.isArgument for 'ch1'");
                        }
                        break;

                case 3:                 // DoubleType
                        if (lVar.isArgument()) {
                            testExitCode = FAILED;
                            log3("ERROR: lVar.isArgument for 'db1'");
                        }
                        break;

                case 4:                 // FloatType
                        if (lVar.isArgument()) {
                            testExitCode = FAILED;
                            log3("ERROR: lVar.isArgument for 'fl1'");
                        }
                        break;

                case 5:                 // IntegerType
                        if (lVar.isArgument()) {
                            testExitCode = FAILED;
                            log3("ERROR: lVar.isArgument for 'in1'");
                        }
                        break;

                case 6:                 // LongType
                        if (lVar.isArgument()) {
                            testExitCode = FAILED;
                            log3("ERROR: lVar.isArgument for 'ln1'");
                        }
                        break;

                case 7:                 // ShortType
                        if (lVar.isArgument()) {
                            testExitCode = FAILED;
                            log3("ERROR: lVar.isArgument for 'sh1'");
                        }
                        break;


                case 8:                 // ClassType
                        if (lVar.isArgument()) {
                            testExitCode = FAILED;
                            log3("ERROR: lVar.isArgument for 'class2'");
                        }
                        break;

                case 9:                 // InterfaceType
                        if (lVar.isArgument()) {
                            testExitCode = FAILED;
                            log3("ERROR: lVar.isArgument for 'iface'");
                        }
                        break;

                case 10:                // ArrayType
                        if (lVar.isArgument()) {
                            testExitCode = FAILED;
                            log3("ERROR: lVar.isArgument for 'cfc'");
                        }
                        break;


                case 11:                // argument
                        if (!lVar.isArgument()) {
                            testExitCode = FAILED;
                            log3("ERROR: !lVar.isArgument for 'param'");
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
