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

package nsk.jdi.ThreadReference.popFrames;

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
 * ThreadReference.                                             <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.ThreadReference.popFrames()</code>         <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The test checks up on the following assertion:               <BR>
 *  After this operation,                                       <BR>
 *  this thread will be suspended at the invoke instruction of  <BR>
 *  the target method that created frame.                       <BR>
 *  The frame's method can be reentered with a                  <BR>
 *  step into the instruction.                                  <BR>
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
 * Upon getting the ClassPrepareEvent,
 * the debugger sets up the breakpoint with SUSPEND_EVENT_THREAD        <BR>
 * within debuggee's special methodForCommunication().                  <BR>
 * <BR>
 * In second phase to check the assetion,                               <BR>
 * the debugger and the debuggee perform the following loop.            <BR>
 * - The debugger resumes the debuggee and waits for the BreakpointEvent.<BR>
 * - The debuggee prepares new check case and invokes                   <BR>
 *   the methodForCommunication to be suspended and                     <BR>
 *   to inform the debugger with the event.                             <BR>
 * - Upon getting the BreakpointEvent,                                  <BR>
 *   the debugger performs the check case required.                     <BR>
 * Note. To inform each other of needed actions, the debugger and       <BR>
 *       and the debuggee use debuggeee's variable "instruction".       <BR>
 * <BR>
 * In third phase when at the end,                                      <BR>
 * the debuggee changes the value of the "instruction"                  <BR>
 * to inform the debugger of checks finished, and both end.             <BR>
 * <BR>
 * In the second phase, the debuggee creates second thread which        <BR>
 * will call a tested poppedMethod() from its run() method.             <BR>
 * Being called, the poppedMethod() increments the value of             <BR>
 * the global variable testVar1.                                        <BR>
 * The debugger sets up a breakpoint within the poppedMethod() and,     <BR>
 * after getting this brakpoint event, invokes the popFrames() method,  <BR>
 * checks the value of testVar1 after first incerment,                  <BR>
 * and resumes debuggee's second thread to reenter the poppedMethod().  <BR>
 * After getting the breakpoint event second time,                      <BR>
 * the debugger just checks the value of testVar1 which                 <BR>
 * has to be incremented one more time.                                 <BR>
 */

public class popframes001 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new popframes001().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.ThreadReference.popFrames.popframes001a";

    //====================================================== test program

    BreakpointRequest breakpointRequest;

    //------------------------------------------------------ methods

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

        log2("......vm.resume();  resuming debuggee's main thread");
        vm.resume();

        for (int i = 0; ; i++) {

//            vm.resume();

            breakpointForCommunication();

            int instruction;

            log2("......instruction = ((IntegerValue)...; no Exception expected");
            try {
                instruction = ((IntegerValue)
                               (debuggeeClass.getValue(debuggeeClass.fieldByName("instruction")))).value();
            } catch ( Exception e ) {
                log3("ERROR: Exception when 'instruction = ((IntegerValue)...' : " + e);
                testExitCode = FAILED;
                return;
            }

            if (instruction == 0) {
                vm.resume();
                break;
            }

            log1("  new check: # " + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            if ( !vm.canPopFrames() ) {
                log2("......vm.canPopFrames() == false : test is cancelled");
                return;
            }

            String thread2Name         = "thread2";
            ThreadReference thread2Ref = debuggee.threadByNameOrThrow(thread2Name);


            String poppedMethod    = "poppedMethod";
            String breakpointLine  = "breakpointLine";
            String testVariable    = "testVar1";

            log2("......setting breakpoint in poppedMethod");
            try {
                breakpointRequest = settingBreakpoint(debuggee.threadByNameOrThrow(thread2Name),
                                          debuggeeClass,
                                          poppedMethod, breakpointLine, "one");
            } catch ( Exception e ) {
                throw e;
            }
            log2("......breakpointRequest.enable();");
            breakpointRequest.enable();

            log2("......eventSet.resume();   resuming debuggee's main thread");
            log2("                           to get new rendevous at the breakpointForCommunication");
            eventSet.resume();
            log2("......breakpointInMethod();");
            breakpointInMethod();

            log2("......getting value of 'testVar1'; 1 is expected");
            Value val = debuggeeClass.getValue(debuggeeClass.fieldByName(testVariable));
            int intVal = ((IntegerValue) val).value();
            if (intVal != 1) {
                log3("ERROR: intVal != 1");
                testExitCode = FAILED;
                break;
            }

            log2("......StackFrame stackFrame = thread2Ref.frame(0);");
            StackFrame stackFrame = thread2Ref.frame(0);

            log2("......thread2Ref.popFrames(stackFrame);");
            try {
                thread2Ref.popFrames(stackFrame);
            } catch ( IncompatibleThreadStateException e ) {
                log3("ERROR: IncompatibleThreadStateException");
                testExitCode = FAILED;
                break;
            }

            log2("......thread2Ref.resume();");
            thread2Ref.resume();
            log2("......breakpointInMethod();");
            breakpointInMethod();

            log2("......getting value of 'testVar1'; 2 is expected");
            val = debuggeeClass.getValue(debuggeeClass.fieldByName(testVariable));
            intVal = ((IntegerValue) val).value();
            if (intVal != 2) {
                log3("ERROR: intVal != 2");
                testExitCode = FAILED;
                break;
            }

            log2("......thread2Ref.resume();");
            thread2Ref.resume();

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

    // ============================== test's additional methods

    private void breakpointInMethod()
                 throws JDITestRuntimeException {

        log2("breakpointInMethod");
        getEventSet();
        Event event = eventIterator.nextEvent();

        if ( !(event instanceof BreakpointEvent) )
            throw new JDITestRuntimeException("** event IS NOT a breakpoint **");

        if ( !event.request().equals(breakpointRequest) )
            throw new JDITestRuntimeException("** unexpected breakpoint **");
    }
}
