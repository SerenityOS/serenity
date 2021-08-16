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
 * complies with its spec.                                      <BR>
 * The test checks up on throwing IllegalArgumentException      <BR>
 *    for the method invocation setValue(null, Value value);    <BR>
 * <BR>
 * After being started up, a debuggee creates an value used in the test <BR>
 * and informs a debugger of the creation.                              <BR>
 * Upon the receiption a message from the debuggee, the debugger        <BR>
 * checks up that the call setValue.(null, ByteValue byteValue);        <BR>
 * throws IllegalArgumentException.                                     <BR>
 * <BR>
 */

public class setvalue005 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/ClassType/setValue/setvalue005 ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new setvalue005().runThis(argv, out);
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
        logHandler.display(sHeader3 + message);
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.ClassType.setValue.setvalue005a";

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

            Field field1 = null;
            Field field2 = null;

            String bt1 = "bt1";


            label1: {
                try {
                    field1 = dClass.fieldByName(bt1);
                    if (field1 == null) {
                        log3("ERROR:  field1 == null after: field1 = dClass.fieldByName('bt1');");
                        testExitCode = FAILED;
                        break label1;
                    }
                    ByteValue byteValue = (ByteValue) debuggeeclass.getValue(field1);
                    if (byteValue == null) {
                        log3("ERROR:  byteValue == null after: byteValue = (ByteValue) debuggeeclass.getValue(field1);");
                        testExitCode = FAILED;
                        break label1;
                    }

                    log2("......call: dClass.setValue(null, byteValue);  IllegalArgumentException expected");
                    dClass.setValue(null, byteValue);
                    log2("ERROR:  no any Exception ");
                    testExitCode = FAILED;
                } catch ( IllegalArgumentException e1 ) {
                    log2("     :  IllegalArgumentException ");
                } catch ( InvalidTypeException e2 ) {
                    log3("ERROR:  InvalidTypeException ");
                    testExitCode = FAILED;
                } catch ( ClassNotLoadedException e3 ) {
                    log3("ERROR:  ClassNotLoadedException ");
                    testExitCode = FAILED;
                } catch ( ObjectCollectedException e4 ) {
                    log3("ERROR:  ObjectCollectedException ");
                    testExitCode = FAILED;
                } catch ( VMMismatchException e5 ) {
                    log3("ERROR:  VMMismatchException  ");
                    testExitCode = FAILED;
                } catch ( Exception e6 ) {
                    log3("ERROR:  UNSPECIFIED EXCEPTION:  " + e6);
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
