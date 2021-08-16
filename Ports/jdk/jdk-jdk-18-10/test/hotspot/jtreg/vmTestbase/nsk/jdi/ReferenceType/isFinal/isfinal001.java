/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ReferenceType.isFinal;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * ReferenceType.                                               <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ReferenceType.isFinal()</code>             <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * Cases for testing include :                                  <BR>
 * - inner and outer ClassTypes, InterfaceTypes, and ArrayTypes;<BR>
 * - arrays of primitive values;                                <BR>
 * - primitive classes.                                         <BR>
 */

public class isfinal001 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new isfinal001().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return exitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.ReferenceType.isFinal.isfinal001a";

    private String testedClassName =
      "nsk.jdi.ReferenceType.isFinal.ClassToCheck";

    String mName = "nsk.jdi.ReferenceType.isFinal";

    //====================================================== test program

    private int runThis (String argv[], PrintStream out) {

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        waitTime        = argsHandler.getWaitTime() * 60000;

        try {
            log2("launching a debuggee :");
            log2("       " + debuggeeName);
            if (argsHandler.verbose()) {
                debuggee = binder.bindToDebugee(debuggeeName + " -vbs");
            } else {
                debuggee = binder.bindToDebugee(debuggeeName);
            }
            if (debuggee == null) {
                log3("ERROR: no debuggee launched");
                return FAILED;
            }
            log2("debuggee launched");
        } catch ( Exception e ) {
            log3("ERROR: Exception : " + e);
            log2("       test cancelled");
            return FAILED;
        }

        debuggee.redirectOutput(logHandler);

        vm = debuggee.VM();

        eventQueue = vm.eventQueue();
        if (eventQueue == null) {
            log3("ERROR: eventQueue == null : TEST ABORTED");
            vm.exit(PASS_BASE);
            return FAILED;
        }

        log2("invocation of the method runTest()");
        switch (runTest()) {

            case 0 :  log2("test phase has finished normally");
                      log2("   waiting for the debuggee to finish ...");
                      debuggee.waitFor();

                      log2("......getting the debuggee's exit status");
                      int status = debuggee.getStatus();
                      if (status != PASS_BASE) {
                          log3("ERROR: debuggee returned UNEXPECTED exit status: " +
                              status + " != PASS_BASE");
                          testExitCode = FAILED;
                      } else {
                          log2("......debuggee returned expected exit status: " +
                              status + " == PASS_BASE");
                      }
                      break;

            default : log3("ERROR: runTest() returned unexpected value");

            case 1 :  log3("test phase has not finished normally: debuggee is still alive");
                      log2("......forcing: vm.exit();");
                      testExitCode = FAILED;
                      try {
                          vm.exit(PASS_BASE);
                      } catch ( Exception e ) {
                          log3("ERROR: Exception : e");
                      }
                      break;

            case 2 :  log3("test cancelled due to VMDisconnectedException");
                      log2("......trying: vm.process().destroy();");
                      testExitCode = FAILED;
                      try {
                          Process vmProcess = vm.process();
                          if (vmProcess != null) {
                              vmProcess.destroy();
                          }
                      } catch ( Exception e ) {
                          log3("ERROR: Exception : e");
                      }
                      break;
            }

        return testExitCode;
    }


   /*
    * Return value: 0 - normal end of the test
    *               1 - ubnormal end of the test
    *               2 - VMDisconnectedException while test phase
    */

    private int runTest() {

        try {
            testRun();

            log2("waiting for VMDeathEvent");
            getEventSet();
            if (eventIterator.nextEvent() instanceof VMDeathEvent)
                return 0;

            log3("ERROR: last event is not the VMDeathEvent");
            return 1;
        } catch ( VMDisconnectedException e ) {
            log3("ERROR: VMDisconnectedException : " + e);
            return 2;
        } catch ( Exception e ) {
            log3("ERROR: Exception : " + e);
            return 1;
        }

    }

    private void testRun()
                 throws JDITestRuntimeException, Exception {

        eventRManager = vm.eventRequestManager();

        ClassPrepareRequest cpRequest = eventRManager.createClassPrepareRequest();
        cpRequest.setSuspendPolicy( EventRequest.SUSPEND_EVENT_THREAD);
        cpRequest.addClassFilter(debuggeeName);

        cpRequest.enable();
        vm.resume();
        getEventSet();
        cpRequest.disable();

        ClassPrepareEvent event = (ClassPrepareEvent) eventIterator.next();
        debuggeeClass = event.referenceType();

        if (!debuggeeClass.name().equals(debuggeeName))
           throw new JDITestRuntimeException("** Unexpected ClassName for ClassPrepareEvent **");

        log2("      received: ClassPrepareEvent for debuggeeClass");

        String bPointMethod = "methodForCommunication";
        String lineForComm  = "lineForComm";
        BreakpointRequest bpRequest;

        try {
            bpRequest = settingBreakpoint(debuggee.threadByNameOrThrow("main"),
                                          debuggeeClass,
                                          bPointMethod, lineForComm, "zero");
        } catch ( Exception e ) {
            throw e;
        }
        bpRequest.enable();

    //------------------------------------------------------  testing section

        log1("     TESTING BEGINS");

        for (int i = 0; ; i++) {

            vm.resume();
            breakpointForCommunication();

            int instruction = ((IntegerValue)
                               (debuggeeClass.getValue(debuggeeClass.fieldByName("instruction")))).value();

            if (instruction == 0) {
                vm.resume();
                break;
            }

            log1("  new check: # " + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            String names1[] = {

                      ".TestClass$InnerClass",        "f",
                      ".TestClass$InnerClass[]",      "t",

                      ".TestClass$FinalInnerClass",   "t",
                      ".TestClass$FinalInnerClass[]", "t",

                      ".TestClass$NestedIface",        "f",
                      ".TestClass$NestedIface[]",      "t",

                      ".OuterClass",        "f",
                      ".OuterClass[]",      "t",
                      ".OuterIface",        "f",
                      ".OuterIface[]",      "t",

                      ".FinalOuterClass",   "t",
                      ".FinalOuterClass[]", "t"

                      };

            String names2[] = {

                      "boolean[][][][]", "t",
                      "byte[][][][]",    "t",
                      "char[][][][]",    "t",
                      "double[][][][]",  "t",
                      "float[][][][]",   "t",
                      "int[][][][]",     "t",
                      "long[][][][]",    "t",
                      "short[][][][]",   "t"

                      };

            String names3[] = {

                      "java.lang.Boolean",   "t",
                      "java.lang.Byte",      "t",
                      "java.lang.Character", "t",
                      "java.lang.Double",    "t",
                      "java.lang.Float",     "t",
                      "java.lang.Integer",   "t",
                      "java.lang.Long",      "t",
                      "java.lang.Short",     "t"

                      };

            log2("----- Cases for testing: ReferenceTypes");
            for (int i1 = 0; i1 < names1.length; i1 += 2) {

                String typeForCheck = mName + names1[i1];
                log2("......typeForCheck: " + typeForCheck);

                log2("      getting: List classList = vm.classesByName(typeForCheck);");

                List classList = vm.classesByName(typeForCheck);
                if (classList.size() != 1) {
                    log3("ERROR: classList.size() != 1  : " + classList.size());
                    testExitCode = FAILED;
                    continue;
                }

                log2("      getting: ReferenceType referenceType  = (ReferenceType) classList.get(0);");
                ReferenceType referenceType  = (ReferenceType) classList.get(0);

                log2("      getting: boolean isFinal = referenceType.isfinal();");
                boolean isFinal = referenceType.isFinal();

                if ( names1[i1+1].equals("t") ) {
                    log2("      expected value of isFinal is 'true'");
                    if (!isFinal) {
                        log3("ERROR: isfinal != true for: " + typeForCheck);
                        testExitCode = FAILED;
                    }
                } else {
                    log2("      expected value of isFinal is 'false'");
                    if (isFinal) {
                        log3("ERROR: isfinal != false for: " + typeForCheck);
                        testExitCode = FAILED;
                    }
                }
            }

            log2("----- Cases for testing: primitive type arrays");
            for (int i1 = 0; i1 < names2.length; i1 += 2) {

                String typeForCheck = names2[i1];
                log2("......typeForCheck: " + typeForCheck);

                log2("      getting: List classList = vm.classesByName(typeForCheck);");
                List classList = vm.classesByName(typeForCheck);
                if (classList.size() != 1) {
                    log3("ERROR: classList.size() != 1  : " + classList.size());
                    testExitCode = FAILED;
                    continue;
                }

                log2("       getting: ReferenceType referenceType  = (ReferenceType) classList.get(0);");
                ReferenceType referenceType  = (ReferenceType) classList.get(0);

                log2("       getting: boolean isFinal = referenceType.isfinal();");
                boolean isFinal = referenceType.isFinal();

                log2("      expected value of isFinal is 'true'");
                if (!isFinal) {
                    log3("ERROR: isfinal != true for: " + typeForCheck);
                    testExitCode = FAILED;
                }

            }

            log2("----- Cases for testing: primitive classes");
            for (int i1 = 0; i1 < names3.length; i1 += 2) {

                String typeForCheck = names3[i1];
                log2("......typeForCheck: " + typeForCheck);

                log2("      getting: List classList = vm.classesByName(typeForCheck);");
                List classList = vm.classesByName(typeForCheck);
                if (classList.size() != 1) {
                    log3("ERROR: classList.size() != 1  : " + classList.size());
                    testExitCode = FAILED;
                    continue;
                }

                log2("      getting: ReferenceType referenceType  = (ReferenceType) classList.get(0);");
                ReferenceType referenceType  = (ReferenceType) classList.get(0);

                log2("      getting: boolean isFinal = referenceType.isfinal();");
                boolean isFinal = referenceType.isFinal();

                log2("      expected value of isFinal is 'true'");
                if (!isFinal) {
                    log3("ERROR: isfinal != true for: " + typeForCheck);
                    testExitCode = FAILED;
                }

            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

}
