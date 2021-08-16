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

package nsk.jdi.EventSet.eventIterator;

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
 * EventSet.                                                    <BR>
 *                                                              <BR>
 * The test checks that results of the method                   <BR>
 * <code>com.sun.jdi.EventSet.eventIterator()</code>            <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The test checks that for ClassPrepareEvent set               <BR>
 *  - the method returns a non-null object, and                 <BR>
 *  - object's class is the subclass of class Iterator.         <BR>
 * <BR>
 * The test works as follows.                                           <BR>
 * <BR>
 * Upon launching debuggee's VM which will be suspended,                <BR>
 * a debugger waits for the VMStartEvent within a predefined            <BR>
 * time interval. If no the VMStartEvent received, the test is FAILED.  <BR>
 * Upon getting the VMStartEvent,                                       <BR>
 * the debugger sets up two requests, both with SUSPEND_EVENT_THREAD,   <BR>
 * one for debuggee's ClassPrepareEvent,                                <BR>
 * another one for a special TestClass,  resumes the VM,                <BR>
 * and waits for the events within the predefined time interval.        <BR>
 * If no the events received, the test is FAILED.                       <BR>
 * Upon getting the debuggeeClass's ClassPrepareEvent,                  <BR>
 * the debugger sets up the breakpoint with SUSPEND_EVENT_THREAD        <BR>
 * within debuggee's special methodForCommunication().                  <BR>
 * Upon getting TestClass's ClassPrepareEvent,                          <BR>
 * the debugger performs the check.                                     <BR>
 * <BR>
 * At the end the debuggee changes the value of the "instruction"       <BR>
 * to inform the debugger of checks finished, and both ends.            <BR>
 * <BR>
 * Note. To inform each other of needed actions, the debugger and       <BR>
 *       and the debuggee use debuggee's variable "instruction".        <BR>
 * <BR>
 */

public class eventiterator001 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new eventiterator001().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.EventSet.eventIterator.eventiterator001a";

    private String testedClassName =
      "nsk.jdi.EventSet.eventIterator.eventiterator001aTestClass";

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

            if ( !(eventIterator.nextEvent() instanceof VMDeathEvent) ) {
                log3("ERROR: last event is not the VMDeathEvent");
                return 1;
            }

            log2("waiting for VMDisconnectEvent");
            getEventSet();

            if ( !(eventIterator.nextEvent() instanceof VMDisconnectEvent) ) {
                log3("ERROR: last event is not the VMDisconnectEvent");
                return 1;
            }

            return 0;

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
        cpRequest.putProperty("number", "debuggeeClass");
        cpRequest.enable();

        ClassPrepareRequest tcRequest = settingClassPrepareRequest(testedClassName,
                                                    EventRequest.SUSPEND_EVENT_THREAD,
                                                    "TestClassPrepareRequest");
        tcRequest.enable();

        int nn1 = 0;
        int nn2 = 0;
        for (int nn = 0; nn < 2; nn++) {

            vm.resume();
            getEventSet();

            ClassPrepareEvent event = (ClassPrepareEvent) eventIterator.next();

            log2("String property = (String) event.request().getProperty('number');");
            String property = (String) event.request().getProperty("number");

            if (property.equals("debuggeeClass")) {

                log2("      received: ClassPrepareEvent for debuggeeClass");

                nn1++;
                if (nn1 > 1)
                    throw new JDITestRuntimeException("** 2-nd event for debuggeeClass **");

                cpRequest.disable();
                debuggeeClass = event.referenceType();

                String          bPointMethod = "methodForCommunication";
                String          lineForComm  = "lineForComm";
                ThreadReference mainThread   = debuggee.threadByNameOrThrow("main");

                BreakpointRequest bpRequest = settingBreakpoint(mainThread,
                                              debuggeeClass,
                                              bPointMethod, lineForComm, "zero");
                bpRequest.enable();

            } else if (property.equals("TestClassPrepareRequest")) {
                nn2++;
                if (nn2 > 1)
                    throw new JDITestRuntimeException("** 2-nd event for TestClass **");

                if (eventIterator == null) {
                    testExitCode = FAILED;
                    log3("ERROR: eventIterator == null");
                }
                if ( !(eventIterator instanceof Iterator) ) {
                    testExitCode = FAILED;
                    log3("ERROR: eventIterator is NOT instanceof Iterator");
                }
            } else {
                log3("ERROR: unexpected Event :  property == " + property);
                throw new JDITestRuntimeException("** UNEXPECTED Event **");
            }
        }

    //------------------------------------------------------  testing section

        log1("     TESTING BEGINS");

        vm.resume();
        breakpointForCommunication();

        int instruction = ((IntegerValue)
                           (debuggeeClass.getValue(debuggeeClass.fieldByName("instruction")))).value();

        if (instruction != 0) {
            throw new JDITestRuntimeException("** instruction != 0 **");
        }
        vm.resume();
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        log1("    TESTING ENDS");
        return;
    }

    // ============================== test's additional methods

    private ClassPrepareRequest settingClassPrepareRequest ( String  testedClass,
                                                             int     suspendPolicy,
                                                             String  property       )
            throws JDITestRuntimeException {
        try {
            log2("......setting up ClassPrepareRequest:");
            log2("       class: " + testedClass + "; property: " + property);

            ClassPrepareRequest
            cpr = eventRManager.createClassPrepareRequest();
            cpr.putProperty("number", property);
            cpr.addClassFilter(testedClass);
            cpr.setSuspendPolicy(suspendPolicy);

            log2("      ClassPrepareRequest has been set up");
            return cpr;
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingClassPrepareRequest() : " + e);
            log3("       ClassPreparenRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up ClassPrepareRequest **");
        }
    }
}
