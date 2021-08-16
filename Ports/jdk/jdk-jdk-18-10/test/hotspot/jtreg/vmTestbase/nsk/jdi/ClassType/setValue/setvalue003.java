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
 * complies with its spec in case when                          <BR>
 * no Exception is expected to be thrown.                       <BR>
 * The test makes checking up on ReferenceTypes only.           <BR>
 * <BR>
 * After being started up, a debuggee creates ReferenceType objects for <BR>
 * testing and informs a debugger of the creation.                      <BR>
 * Upon the receiption a message from the debuggee, the debugger        <BR>
 * gets ReferenceType debuggeeclass and ClassType dClass objects and    <BR>
 * checks up that for Fields field1 and field2 the following sequence   <BR>
 *       Value val1 = debuggeeclass.getValue(field1);   <BR>
 *       dClass.setValue(field2, val1);                 <BR>
 *       Value val2 = debuggeeclass.getValue(field2);   <BR>
 * results in:  val1.equals(val2)                       <BR>
 * for the following tested ReferenceType objects       <BR>
 *       ClassType                                      <BR>
 *       InterfaceType                                  <BR>
 *       ClassArray                                     <BR>
 *       InterfaceArray                                 <BR>
 *       PrimitiveArray                                 <BR>
 * <BR>
 */

public class setvalue003 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ClassType/setValue/setvalue003 ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new setvalue003().runThis(argv, out);
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
        "nsk.jdi.ClassType.setValue.setvalue003a";

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

            log2("       getting: List classes = vm.classesByName(debuggeeName); expected size == 1");
            List classes = vm.classesByName(debuggeeName);
            int size = classes.size();
            if (size != 1) {
                log3("ERROR: classes.size() != 1 : " + size);
                testExitCode = FAILED;
                break ;
            }

            log2("      getting ReferenceType and ClassType objects for debuggeeClass");
            ReferenceType debuggeeclass = (ReferenceType) classes.get(0);

            ClassType dClass = (ClassType) debuggeeclass;


            String names[][] = { { "class1", "class2" },
                                 { "class3", "class4" },
                                 { "iface1", "iface2" },
                                 { "iface3", "iface4" },
                                 { "array1", "array2" }  };

            Field field1 = null;
            Field field2 = null;

            log2("      checking up variables of ReferenceTypes");

            for (int i2 = 0; i2 < 5; i2++) {
                try {
                    field1 = debuggeeclass.fieldByName(names[i2][0]);
                    field2 = debuggeeclass.fieldByName(names[i2][1]);
                    log2("      : tested type: " + field1.typeName());
                    log2("      : tested variables: " + names[i2][0] + "  " + names[i2][1]);
                    if (field1 != null && field2 != null) {
                        Value val1 = debuggeeclass.getValue(field1);
                        dClass.setValue(field2, val1);
                        Value val2 = debuggeeclass.getValue(field2);
                        if ( !val1.equals(val2)) {
                            log3("ERROR: !val1.equals(val2)");
                            testExitCode = FAILED;
                        }
                    } else {
                        log3("ERROR: field1 != null && field2 != null");
                        testExitCode = FAILED;
                    }
                } catch ( Exception e ) {
                    log3("ERROR:  Exception :  " + e);
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
