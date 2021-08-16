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

package nsk.jdi.EventRequestManager.deleteEventRequests;

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
 * <code>com.sun.jdi.EventRequestManager.deleteEventRequests()</code> <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The test checks up on the following assertion:               <BR>
 *     After invoking the tested method,                        <BR>
 *     the methods returning Lists of requests,                 <BR>
 *     such as EventRequestManager.accessWatchpointRequests(),  <BR>
 *     return a List containing no eventRequests                <BR>
 *     deleted by the tested method.                            <BR>
 * Testcases include all subclasses of EventRequest.            <BR>
 * <BR>
 * The test has three phases and works as follows.              <BR>
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

public class delevtreqs002 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new delevtreqs002().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.EventRequestManager.deleteEventRequests.delevtreqs002a";

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

        final int length1 = 10;
        final int length2 = 12;
        EventRequest requestArray[][] = new EventRequest[length1][length2];

        String fieldName1 = "testField1";
        String fieldName2 = "testField2";
        String fieldName3 = "testField3";

        List<EventRequest> requests   = null;
        ListIterator li         = null;

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


            log2("......creating EventRequests");
            for (int i1 = 0; i1 < length1; i1++) {

                ThreadReference threadRef = (ThreadReference)
                       ( (ArrayReference) debuggeeClass.getValue(debuggeeClass.fieldByName(fieldName3))
                       ).getValue(i1);

                requestArray[i1][0]  = eventRManager.createAccessWatchpointRequest(field1);
                requestArray[i1][1]  = eventRManager.createModificationWatchpointRequest(field1);
                requestArray[i1][2]  = eventRManager.createBreakpointRequest(breakpLocation);
                requestArray[i1][3]  = eventRManager.createClassPrepareRequest();
                requestArray[i1][4]  = eventRManager.createClassUnloadRequest();
                requestArray[i1][5]  = eventRManager.createExceptionRequest(refType, false, false);
                requestArray[i1][6]  = eventRManager.createMethodEntryRequest();
                requestArray[i1][7]  = eventRManager.createMethodExitRequest();
                requestArray[i1][8]  = eventRManager.createStepRequest(threadRef, StepRequest.STEP_MIN, StepRequest.STEP_OUT);
                requestArray[i1][9]  = eventRManager.createThreadDeathRequest();
                requestArray[i1][10] = eventRManager.createThreadStartRequest();
                requestArray[i1][11] = eventRManager.createVMDeathRequest();
            }
            for ( int ii1 = 0; ii1 < length1; ii1++) {
                for ( int ii2 = 0; ii2 < length2; ii2++) {
                    requestArray[ii1][ii2].putProperty("number", "request " + ii1 + " " + ii2);
                }
            }


            log2("......deleting Requests and checking up on Lists");
            for (int i2 = 0; i2 < length1; i2++) {

                EventRequest awRequest   = requestArray[i2][0];
                EventRequest mwRequest   = requestArray[i2][1];
                EventRequest bp1Request  = requestArray[i2][2];
                EventRequest cp1Request  = requestArray[i2][3];
                EventRequest cuRequest   = requestArray[i2][4];
                EventRequest exRequest   = requestArray[i2][5];
                EventRequest menRequest  = requestArray[i2][6];
                EventRequest mexRequest  = requestArray[i2][7];
                EventRequest stRequest   = requestArray[i2][8];
                EventRequest tdRequest   = requestArray[i2][9];
                EventRequest tsRequest   = requestArray[i2][10];
                EventRequest vmdRequest  = requestArray[i2][11];

                requests = new LinkedList<EventRequest>();

                try {
                    requests.add(awRequest);
                    requests.add(mwRequest);
                    requests.add(bp1Request);
                    requests.add(cp1Request);
                    requests.add(cuRequest);
                    requests.add(exRequest);
                    requests.add(menRequest);
                    requests.add(mexRequest);
                    requests.add(stRequest);
                    requests.add(tdRequest);
                    requests.add(tsRequest);
                    requests.add(vmdRequest);
                } catch ( UnsupportedOperationException e ) {
                    testExitCode = FAILED;
                    log3("ERROR: unexpected UnsupportedOperationException");
                    break;
                }

                eventRManager.deleteEventRequests(requests);

                for (EventRequest er : eventRManager.accessWatchpointRequests()) {
                    if ( er.equals(awRequest) ) {
                        testExitCode = FAILED;
                        log3("ERROR: deleted AccessWatchpointRequest is in the List :: " + er.getProperty("number") );
                    }
                }

                for (EventRequest er : eventRManager.modificationWatchpointRequests()) {
                    if ( er.equals(mwRequest) ) {
                        testExitCode = FAILED;
                        log3("ERROR: deleted ModificationWatchpointRequest is in the List :: " + er.getProperty("number") );
                    }
                }

                for (EventRequest er : eventRManager.breakpointRequests()) {
                    if ( er.equals(bp1Request) ) {
                        testExitCode = FAILED;
                        log3("ERROR: deleted BreakpointRequest is in the List :: " + er.getProperty("number") );
                    }
                }

                for (EventRequest er : eventRManager.classPrepareRequests()) {
                    if ( er.equals(cp1Request) ) {
                        testExitCode = FAILED;
                        log3("ERROR: deleted ClassPrepareRequest is in the List :: " + er.getProperty("number") );
                    }
                }

                for (EventRequest er : eventRManager.classUnloadRequests()) {
                    if ( er.equals(cuRequest) ) {
                        testExitCode = FAILED;
                        log3("ERROR: deleted ClassUnloadRequest is in the List :: " + er.getProperty("number") );
                    }
                }

                for (EventRequest er : eventRManager.exceptionRequests()) {
                    if ( er.equals(exRequest) ) {
                        testExitCode = FAILED;
                        log3("ERROR: deleted ExceptionRequest is in the List :: " + er.getProperty("number") );
                    }
                }

                for (EventRequest er : eventRManager.methodEntryRequests()) {
                    if ( er.equals(menRequest) ) {
                        testExitCode = FAILED;
                        log3("ERROR: deleted MethodEntryRequest is in the List :: " + er.getProperty("number") );
                    }
                }

                for (EventRequest er : eventRManager.methodExitRequests()) {
                    if ( er.equals(mexRequest) ) {
                        testExitCode = FAILED;
                        log3("ERROR: deleted MethodExitRequest is in the List :: " + er.getProperty("number") );
                    }
                }

                for (EventRequest er : eventRManager.stepRequests()) {
                    if ( er.equals(stRequest) ) {
                        testExitCode = FAILED;
                        log3("ERROR: deleted StepRequest is in the List :: " + er.getProperty("number") );
                    }
                }

                for (EventRequest er : eventRManager.threadDeathRequests()) {
                    if ( er.equals(tdRequest) ) {
                        testExitCode = FAILED;
                        log3("ERROR: deleted ThreadDeathRequest is in the List :: " + er.getProperty("number") );
                    }
                }

                for (EventRequest er : eventRManager.threadStartRequests()) {
                    if ( er.equals(tsRequest) ) {
                        testExitCode = FAILED;
                        log3("ERROR: deleted ThreadStartRequest is in the List :: " + er.getProperty("number") );
                    }
                }

                for (EventRequest er : eventRManager.vmDeathRequests()) {
                    if ( er.equals(vmdRequest) ) {
                        testExitCode = FAILED;
                        log3("ERROR: deleted VMDeathRequest is in the List :: " + er.getProperty("number") );
                    }
                }
            }
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

}
