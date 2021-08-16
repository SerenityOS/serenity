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

package nsk.jdi.ArrayType.newInstance;

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
 * <code>com.sun.jdi.ArrayType.newInstance()</code>             <BR>
 * complies with its spec when componentType of an ArrayType    <BR>
 * object used as original for newInstance() is InterfeaceType. <BR>
 * The test works as follows.                                   <BR>
 *                                                              <BR>
 * For given debuggee's array whose components                  <BR>
 * are objects of a class type implementing the interface,      <BR>
 * a debugger forms corresponding ArrayType object              <BR>
 * to which it applies newInstance() method in order to get     <BR>
 * an ArrayReference object, newarray.                          <BR>
 * Then the debugger checks up that:                            <BR>
 * - newInstance() method's returned value != null;             <BR>
 * - length of newly created array is equal to 'arg' in         <BR>
 *   newInstance(int arg) method invocation;                    <BR>
 * - newarray.type().name().equals(array.componentType().name());<BR>
 * - all components of new array are nulls                      <BR>
 *   as its default values at the moment of the array creation. <BR>
 * <BR>
 */

public class newinstance003 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ArrayType/newInstance/newinstance003  ",
    sHeader2 = "--> newinstance003: ",
    sHeader3 = "##> newinstance003: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new newinstance003().runThis(argv, out);
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
        "nsk.jdi.ArrayType.newInstance.newinstance003a";

    String mName = "nsk.jdi.ArrayType.newInstance";

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
        log2("newinstance003a debuggee launched");
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

            // There are potentially other non-test Java threads allocating objects and triggering
            // GC's so we need to suspend the target VM to avoid the objects created in the test
            // from being accidentally GC'ed. However, we need the target VM temporary resumed
            // while reading its response. Below we resume the target VM (if required) and suspend
            // it only after pipe.readln() returns.

            // On the first iteration the target VM is not suspended yet.
            if (i > 0) {
                debuggee.resume();
            }
            line = pipe.readln();

            // Suspending target VM to prevent other non-test Java threads from triggering GCs.
            debuggee.suspend();

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

            List listOftestedClasses = vm.classesByName(mName + ".newinstance003aTestClass");
            if (listOftestedClasses.size() != 1) {
                log3("ERROR: listOftestedClasses.size() != 1");
                testExitCode = FAILED;
                continue;
            }
            ReferenceType testedClass =
                        (ReferenceType) listOftestedClasses.get(0);

            Field fieldArray = testedClass.fieldByName("iface");
            if (fieldArray == null) {
                log3("ERROR: fieldArray == null");
                testExitCode = FAILED;
                continue;
            }

            final int arraylength = 2;

            ArrayType ifaceArray =
                            (ArrayType) testedClass.getValue(fieldArray).type();

            ArrayReference newifaceArray = null;
            try {
                newifaceArray = ifaceArray.newInstance(arraylength);
            } catch ( Throwable e ) {
                log3 ("ERROR: Exception: " + e);
                testExitCode = FAILED;
                continue;
            }
            if (newifaceArray == null) {
                log3("ERROR: newifaceArray == null");
                testExitCode = FAILED;
                continue;
            }

            try {
                if ( newifaceArray.type().name().equals(ifaceArray.componentType().name()) ) {
                    log3("ERROR : types are not equal :\n" +
                         "newifaceArray.type().name()      =" + newifaceArray.type().name() + "\n" +
                         "ifaceArray.componentType().name()=" + ifaceArray.componentType().name() );
                    testExitCode = FAILED;
                    continue;
                }
            } catch ( ClassNotLoadedException e ) {
                log3("ERROR: ClassNotLoadedException for newifaceArray.type().name().equals(..");
                testExitCode = FAILED;
                continue;
            }

            if (newifaceArray.length() != arraylength) {
                log3("ERROR : newifaceArray.length() != arraylength   " + newifaceArray.length());
                testExitCode = FAILED;
                continue;
            }

            if (newifaceArray.getValue(0) != null ||
                           newifaceArray.getValue(1) != null) {
                log3("ERROR: newifaceArray.getValue() != null  ");
                testExitCode = FAILED;
                continue;
            }

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("      TESTING ENDS");

    //--------------------------------------------------   test summary section
    //-------------------------------------------------    standard end section
        debuggee.resume();
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
