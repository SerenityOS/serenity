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

package nsk.jdi.EventRequest.enable;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * EventRequest.                                                <BR>
 *                                                              <BR>
 * The test checks that results of the method                   <BR>
 * <code>com.sun.jdi.EventRequest.enable()</code>               <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The test checks up on the following assertion:               <BR>
 *    Throws: InvalidRequestStateException -                    <BR>
 *            if this request has been deleted.                 <BR>
 *            IllegalThreadStateException -                     <BR>
 *            if this is a StepRequest and                      <BR>
 *            the thread named in the request has died.         <BR>
 *  The cases to test include all sub-types of EventRequest.    <BR>
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
 * the debugger sets up the breakpoint with SUSPEND_ALL <BR>
 * within debuggee's special methodForCommunication().                  <BR>
 * <BR>
 * In second phase to check the assertion,                              <BR>
 * the debugger and the debuggee perform the following loop.            <BR>
 * - The debugger resumes the debuggee and waits for the BreakpointEvent.<BR>
 * - The debuggee prepares new check case (as if) and invokes           <BR>
 *   the methodForCommunication to be suspended and                     <BR>
 *   to inform the debugger with the event.                             <BR>
 * - Upon getting the BreakpointEvent,                                  <BR>
 *   the debugger performs the check case required.                     <BR>
 * <BR>
 * In third phase, at the end of the test, the debuggee changes         <BR>
 * the value of the "instruction" which the debugger and debuggee       <BR>
 * use to inform each other of needed actions,  and both end.           <BR>
 * <BR>
 */

public class enable002 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new enable002().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.EventRequest.enable.enable002a";

    private String testedClassName =
       "nsk.jdi.EventRequest.enable.enable002aTestClass11";


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


        EventRequest  eventRequest1 = null;

        String        fieldName1    = "var101";
        String        fieldName2    = "excObj";

        ThreadReference thread1     = null;
        String          threadName1 = "thread1";

        ReferenceType testClassReference = null;


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


            switch (i) {

              case 0:
                     thread1 = debuggee.threadByNameOrThrow(threadName1);

                     log2("......setting up StepRequest");
                     eventRequest1 = eventRManager.createStepRequest
                                     (thread1, StepRequest.STEP_MIN, StepRequest.STEP_INTO);
                     vm.resume();
                     breakpointForCommunication();
                     break;

              case 1:
                     testClassReference = (ReferenceType) vm.classesByName(testedClassName).get(0);

                     log2("......setting up AccessWatchpointRequest");
                     eventRequest1 = eventRManager.createAccessWatchpointRequest
                                    (testClassReference.fieldByName(fieldName1));
                     break;

              case 2:
                     log2(".....setting up ModificationWatchpointRequest");
                     eventRequest1 = eventRManager.createModificationWatchpointRequest
                                    (testClassReference.fieldByName(fieldName1));
                     break;

              case 3:
                     log2(".....setting up ClassPrepareRequest");
                     eventRequest1 = eventRManager.createClassPrepareRequest();
                     break;

              case 4:
                     log2(".....setting up ClassUnloadRequest");
                     eventRequest1 = eventRManager.createClassUnloadRequest();
                     break;

              case 5:
                     log2(".....setting up MethodEntryRequest");
                     eventRequest1 = eventRManager.createMethodEntryRequest();
                     break;

              case 6:
                     log2(".....setting up MethodExitRequest");
                     eventRequest1 = eventRManager.createMethodExitRequest();
                     break;

              case 7:
                     log2(".....setting up ThreadDeathRequest");
                     eventRequest1 = eventRManager.createThreadDeathRequest();
                     break;

              case 8:
                     log2(".....setting up ThreadStartRequest");
                     eventRequest1 = eventRManager.createThreadStartRequest();
                     break;

              case 9:
                     log2(".....setting up VMDeathRequest");
                     eventRequest1 = eventRManager.createVMDeathRequest();
                     break;

              case 10:
                     log2(".....setting up ExceptionRequest");
                     eventRequest1 = eventRManager.createExceptionRequest(
                                     (ReferenceType)
                                     (debuggeeClass.getValue(debuggeeClass.fieldByName(fieldName2))).type(),
                                     true, true );
                     break;

              case 11:
                     log2(".....setting up BreakpointRequest");
                     eventRequest1 = eventRManager.createBreakpointRequest(breakpLocation);
                     break;


              default:
                      throw new JDITestRuntimeException("** default case 2 **");
            }

            if (eventRequest1 instanceof StepRequest) {
                try {
                    log2("......eventRequest1.enable(); IllegalThreadStateException is expected");
                    eventRequest1.enable();
                    testExitCode = FAILED;
                    log3("ERROR:  NO  IllegalThreadStateException for StepRequest");
                } catch ( IllegalThreadStateException e ) {
                    log2("       IllegalThreadStateException");
                }
            }

            log2("......eventRManager.deleteEventRequest(eventRequest1);");
            eventRManager.deleteEventRequest(eventRequest1);
            try {
                log2("......eventRequest1.enable();  InvalidRequestStateException is expected");
                eventRequest1.enable();
                testExitCode = FAILED;
                log3("ERROR:  NO  InvalidRequestStateException");
            } catch ( InvalidRequestStateException e ) {
                    log2("       InvalidRequestStateException");
            }

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

}
