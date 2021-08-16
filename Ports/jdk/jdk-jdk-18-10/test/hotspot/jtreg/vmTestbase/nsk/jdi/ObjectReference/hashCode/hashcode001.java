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

package nsk.jdi.ObjectReference.hashCode;

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
 * <code>com.sun.jdi.ObjectReference.hashCode()</code>          <BR>
 * complies with its specification.                             <BR>
 * The case for testing incliudes two ObjectReference objects   <BR>
 * beloning to the same VM.                                     <BR>
 * <BR>
 * The test works as follows.                                   <BR>
 * Upon launch, a debuggee infroms a debugger of creating       <BR>
 * tested objects.                                              <BR>
 * The debuggee gets two ObjectReference objRef1 and objRef2    <BR>
 * mirroring the same object in the debuggee, and               <BR>
 * ObjectReference objRef3 mirroring another object.            <BR>
 * Then it checks up that                                       <BR>
 * - objRef1.hashCode() == objRef2.hashCode()                   <BR>
 * - objRef1.hashCode() != objRef3.hashCode()                   <BR>
 */

public class hashcode001 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ObjectReference/hashCode/hashcode001  ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new hashcode001().runThis(argv, out);
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
        "nsk.jdi.ObjectReference.hashCode.hashcode001a";

    private String testedClassName =
        "nsk.jdi.ObjectReference.getValues.TestClass";

    private String testedClass1Name =
        "nsk.jdi.ObjectReference.getValues.TestClass1";

    //String mName = "nsk.jdi.ObjectReference.hashCode";

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


            Field    field1  = null;
            Value    val1    = null;
            List     fields  = null;

            String   objName    = "testObj";
            String   obj1Name   = "testObj1";

            ObjectReference objRef1    = null;
            ObjectReference objRef2    = null;
            ObjectReference objRef3    = null;


            log2("......getting first mirror of tested TestClass obj : ObjectReference objRef1");

            field1  = debuggeeClass.fieldByName(objName);
            val1    = debuggeeClass.getValue(field1);
            objRef1  = (ObjectReference) val1;


            log2("......getting second mirror of tested TestClass obj : ObjectReference objRef2");

            objRef2  = (ObjectReference) val1;


            log2("......getting amirror of tested TestClass1 obj : ObjectReference objRef3");

            field1  = debuggeeClass.fieldByName(obj1Name);
            val1    = debuggeeClass.getValue(field1);
            objRef3  = (ObjectReference) val1;


            log2("......testing equality of hashCodes of objRef1 and objRef2");
            if (objRef1.hashCode() != objRef2.hashCode()) {
                log3("ERROR: objRef1.hashCode() is not equal to objRef2.hashCode()");
                testExitCode = FAILED;
            }

            log2("......testing inequality of hashCodes of objRef1 and objRef3");
            if (objRef1.hashCode() == objRef3.hashCode()) {
                log3("ERROR: objRef1.hashCode() is equal to objRef3.hashCode()");
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
