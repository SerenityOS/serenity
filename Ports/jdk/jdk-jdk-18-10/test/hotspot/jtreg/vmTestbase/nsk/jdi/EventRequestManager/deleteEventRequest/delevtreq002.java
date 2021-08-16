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

package nsk.jdi.EventRequestManager.deleteEventRequest;

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
 * EventRequestManager.                                         <BR>
 *                                                              <BR>
 * The test checks that results of the method                   <BR>
 * <code>com.sun.jdi.EventRequestManager.deleteEventRequest()</code> <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The test checks up on the following assertion:                       <BR>
 *    Once the eventRequest is deleted, no operations                   <BR>
 *    (for example, EventRequest.setEnabled(boolean)) are permitted -   <BR>
 *    attempts to do so will generally cause                            <BR>
 *    an InvalidRequestStateException.                                  <BR>
 * Note: only operations specified to throw InvalidRequestStateException <BR>
 * are not permitted.                                                   <BR>
 * Testcases unclude all 12 subclasses of EventRequest created by       <BR>
 * EventRequestManager.                                                 <BR>
 * <BR>
 * The test has three phases and works as follows.                      <BR>
 * <BR>
 * In first phase,                                                      <BR>
 * upon launching debuggee's VM which will be suspended,                <BR>
 * a debugger waits for the VMStartEvent within a predefined            <BR>
 * time interval. If no the VMStartEvent received, the test is FAILED.  <BR>
 * Upon getting the VMStartEvent, it makes the request for debuggee's   <BR>
 * ClassPrepareEvent with SUSPEND_EVENT_THREAD, resumes the VM,         <BR>
 * and waits for the event within the predefined time interval.         <BR>
 * If no the ClassPrepareEvent received, the test is FAILED.            <BR>
 * Upon getting the ClassPrepareEvent,                                  <BR>
 * the debugger sets up the breakpoint with SUSPEND_EVENT_THREAD        <BR>
 * within debuggee's special methodForCommunication().                  <BR>
 * <BR>
 * In second phase to check the assertion,                              <BR>
 * the debugger and the debuggee perform the following.                 <BR>
 * - The debugger resumes the debuggee and waits for the BreakpointEvent.<BR>
 * - The debuggee prepares testcases and invokes                        <BR>
 *   the methodForCommunication to be suspended and                     <BR>
 *   to inform the debugger with the event.                             <BR>
 * - Upon getting the BreakpointEvent,                                  <BR>
 *   the debugger performs the checks required.                         <BR>
 * <BR>
 * In third phase, at the end of the test, the debuggee changes         <BR>
 * the value of the "instruction" which the debugger and debuggee       <BR>
 * use to inform each other of needed actions,  and both end.           <BR>
 * <BR>
 */

public class delevtreq002 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new delevtreq002().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.EventRequestManager.deleteEventRequest.delevtreq002a";

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
                          log3("ERROR: Exception : " + e);
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
                          log3("ERROR: Exception : " + e);
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

        ThreadReference   mainThread = debuggee.threadByNameOrThrow("main");

        BreakpointRequest bpRequest = settingBreakpoint(mainThread,
                                             debuggeeClass,
                                            bPointMethod, lineForComm, "zero");
        bpRequest.enable();

    //------------------------------------------------------  testing section

        EventRequest requestArray[] = new EventRequest[12];

        String fieldName1 = "testField1";
        String fieldName2 = "testField2";
        String fieldName3 = "testField3";


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

            log1(":::::: case: # " + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part


            Field field1 = debuggeeClass.fieldByName(fieldName1);

            ReferenceType refType = (ReferenceType)
                                debuggeeClass.getValue(debuggeeClass.fieldByName(fieldName2)).type();

            ThreadReference threadRef = (ThreadReference)
                                debuggeeClass.getValue(debuggeeClass.fieldByName(fieldName3));

            String requests[] = { "AccessWatchpoint", "ModificationWatchpoint",
                                  "Breakpoint",       "ClassPrepare",
                                  "ClassUnload",      "Exception",
                                  "MethodEntry",      "MethodExit",
                                  "Step",             "ThreadDeath",
                                  "ThreadStart",      "VMDeath"        };

            requestArray[0]  = eventRManager.createAccessWatchpointRequest(field1);
            requestArray[1]  = eventRManager.createModificationWatchpointRequest(field1);
            requestArray[2]  = eventRManager.createBreakpointRequest(breakpLocation);
            requestArray[3]  = eventRManager.createClassPrepareRequest();
            requestArray[4]  = eventRManager.createClassUnloadRequest();
            requestArray[5]  = eventRManager.createExceptionRequest(refType, false, false);
            requestArray[6]  = eventRManager.createMethodEntryRequest();
            requestArray[7]  = eventRManager.createMethodExitRequest();
            requestArray[8]  = eventRManager.createStepRequest(threadRef, StepRequest.STEP_MIN, StepRequest.STEP_OUT);
            requestArray[9]  = eventRManager.createThreadDeathRequest();
            requestArray[10] = eventRManager.createThreadStartRequest();
            requestArray[11] = eventRManager.createVMDeathRequest();

            for (int i1 = 0; i1 < requestArray.length; i1++) {

                log2("......eventRManager.deleteEventRequest(requestArray[i1]); :: " + requests[i1]);
                eventRManager.deleteEventRequest(requestArray[i1]);

                try {
                    requestArray[i1].addCountFilter(1);
                    testExitCode = FAILED;
                    log3("ERROR: NO InvalidRequestStateException for addCountFilter(1);");
                } catch ( InvalidRequestStateException e ) {
                }
                try {
                    requestArray[i1].disable();
                    testExitCode = FAILED;
                    log3("ERROR: NO InvalidRequestStateException for disable();");
                } catch ( InvalidRequestStateException e ) {
                }
                try {
                    requestArray[i1].enable();
                    testExitCode = FAILED;
                    log3("ERROR: NO InvalidRequestStateException for enable();");
                } catch ( InvalidRequestStateException e ) {
                }
                try {
                    requestArray[i1].getProperty("number");
                } catch ( InvalidRequestStateException e ) {
                    testExitCode = FAILED;
                    log3("ERROR: InvalidRequestStateException for getProperty('number');");
                }
                try {
                    requestArray[i1].isEnabled();
                } catch ( InvalidRequestStateException e ) {
                    testExitCode = FAILED;
                    log3("ERROR: InvalidRequestStateException for isEnabled();");
                }
                try {
                    requestArray[i1].putProperty("number", "request" + i1);
                } catch ( InvalidRequestStateException e ) {
                    testExitCode = FAILED;
                    log3("ERROR: InvalidRequestStateException for putProperty('number', 'request' + i1);");
                }
                try {
                    requestArray[i1].setEnabled(true);
                    testExitCode = FAILED;
                    log3("ERROR: NO InvalidRequestStateException for setEnabled(true);");
                } catch ( InvalidRequestStateException e ) {
                }
                try {
                    requestArray[i1].setSuspendPolicy(EventRequest.SUSPEND_NONE);
                    testExitCode = FAILED;
                    log3("ERROR: NO InvalidRequestStateException for setSuspendPolicy(EventRequest.SUSPEND_NONE);");
                } catch ( InvalidRequestStateException e ) {
                }
                try {
                    requestArray[i1].suspendPolicy();
                } catch ( InvalidRequestStateException e ) {
                    testExitCode = FAILED;
                    log3("ERROR: InvalidRequestStateException for suspendPolicy();");
                }
            }

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

}
