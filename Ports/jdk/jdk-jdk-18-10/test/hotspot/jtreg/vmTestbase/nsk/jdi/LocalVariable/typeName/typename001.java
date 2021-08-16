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

package nsk.jdi.LocalVariable.typeName;

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
 * <code>com.sun.jdi.LocalVariable.typeName()</code>            <BR>
 * complies with its spec when LocalVariable is one of          <BR>
 * primitive types.                                             <BR>
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
 *      return;                                         <BR>
 *  }                                                   <BR>
 * a debugger checks up that the method                 <BR>
 * LocalVariable.typeName() applied to each of          <BR>
 * primitive type method variable returns a name of     <BR>
 * a corresponding primitive type.                      <BR>
 * <BR>
 */

public class typename001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/LocalVariable/typeName/typename001  ",
    sHeader2 = "--> typename001: ",
    sHeader3 = "##> typename001: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new typename001().runThis(argv, out);
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
        "nsk.jdi.LocalVariable.typeName.typename001a";

    String mName = "nsk.jdi.LocalVariable.typeName";

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
        log2("typename001a debuggee launched");
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

            List listOfLoadedClasses = vm.classesByName(mName + ".typename001aTestClass");

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
                               "fl1", "in1", "ln1", "sh1" };

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

                String lVarTypeName = null;
                lVarTypeName = lVar.typeName();

                switch (i2) {

                case 0:                 // BooleanType
                        if (!lVarTypeName.equals("boolean")) {
                            testExitCode = FAILED;
                            log3("ERROR: !lVarTypeName.equals('boolean')");
                        }

                        break;

                case 1:                 // ByteType

                        if (!lVarTypeName.equals("byte")) {
                            testExitCode = FAILED;
                            log3("ERROR: !lVarTypeName.equals('byte')");
                        }
                        break;

                case 2:                 // CharType
                        if (!lVarTypeName.equals("char")) {
                            testExitCode = FAILED;
                            log3("ERROR: !lVarTypeName.equals('char')");
                        }
                        break;

                case 3:                 // DoubleType
                        if (!lVarTypeName.equals("double")) {
                            testExitCode = FAILED;
                            log3("ERROR: !lVarTypeName.equals('double')");
                        }
                        break;

                case 4:                 // FloatType
                        if (!lVarTypeName.equals("float")) {
                            testExitCode = FAILED;
                            log3("ERROR: !lVarTypeName.equals('float')");
                        }
                        break;

                case 5:                 // IntegerType
                        if (!lVarTypeName.equals("int")) {
                            testExitCode = FAILED;
                            log3("ERROR: !lVarTypeName.equals('int')");
                        }
                        break;

                case 6:                 // LongType
                        if (!lVarTypeName.equals("long")) {
                            testExitCode = FAILED;
                            log3("ERROR: !lVarTypeName.equals('long')");
                        }
                        break;

                case 7:                 // ShortType
                        if (!lVarTypeName.equals("short")) {
                            testExitCode = FAILED;
                            log3("ERROR: !lVarTypeName.equals('short')");
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
