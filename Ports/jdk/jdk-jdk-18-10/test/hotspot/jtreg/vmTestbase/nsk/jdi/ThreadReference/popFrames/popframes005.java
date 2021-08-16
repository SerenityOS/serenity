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
 * The test checks up on the following assertions:              <BR>
 * -  All StackFrame objects for this thread are invalidated.   <BR>
 * -  The frame previous to the parameter frame                 <BR>
 *    will become the current frame.                            <BR>
 * <BR>
 * The test has two phases and works as follows.                <BR>
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
 * In second phase to check the assetion,                               <BR>
 * the debugger and the debuggee perform the following.                 <BR>
 * - The debugger resumes the debuggee and waits for the BreakpointEvent<BR>
 * in the methodForCommunication().                                     <BR>
 * - The debuggee creates second thread, the thread2, which will        <BR>
 * call a tested poppedMethod() from its run() method.                  <BR>
 * The poppedMethod will call the special breakpointMethod,             <BR>
 * After creating the thread2, the debuggee invokes                     <BR>
 * the methodForCommunication to be suspended and                       <BR>
 * to inform the debugger with the event.                               <BR>
 * - Upon getting the BreakpointEvent in the methodForCommunication,    <BR>
 * the debugger sets up a breakpoint in the breakpointMethod,           <BR>
 * resumes the debuggee, waits for BreakpointEvent in the               <BR>
 * breakpointMethod and, and upon getting it,                           <BR>
 * (1) to check up on first assertion, gets thread2.frame(2),           <BR>
 * invokes popFrames() and checks up on equality of thread2.frame(2)    <BR>
 * and thread2.frame(0). Since thread2.frame(2) has to be invalid,      <BR>
 * the expected result is false; otherwise, the test FAILED.            <BR>
 * (2) to check up on second assertion, gets thread2.frame(1)           <BR>
 * which has to result in the IndexOutOfBoundsException because         <BR>
 * the current frame is the only one in FramesStack.                    <BR>
 * <BR>
 */

public class popframes005 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new popframes005().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.ThreadReference.popFrames.popframes005a";

    //====================================================== test program

    BreakpointRequest bpRequest;
    BreakpointRequest breakpointRequest2;


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

        if ( !vm.canPopFrames() ) {
            log2("......vm.canPopFrames() == false : test is cancelled");
            vm.resume();
            return;
        }

        String bPointMethod = "methodForCommunication";
        String lineForComm  = "lineForComm";

        log2("......setting BreakpointRequest (bpRequest) in main thread");
        bpRequest = settingBreakpoint(debuggee.threadByNameOrThrow("main"),
                                          debuggeeClass,
                                          bPointMethod, lineForComm, "zero");
        log2("bpRequest.enable();");
        bpRequest.enable();

    //------------------------------------------------------  testing section

        log1("     TESTING BEGINS");

        label0:
        {
            vm.resume();
            breakpointForCommunication();

            String thread2Name         = "thread2";
            ThreadReference thread2Ref = debuggee.threadByNameOrThrow(thread2Name);

            StackFrame stackFrame0 = null;
            StackFrame stackFrame1 = null;
            StackFrame stackFrame2 = null;

            String breakpointMethod = "breakpointMethod";
            String breakpointLine    = "breakpointLine";

            log2("......setting breakpoint in breakpointMethod");
            breakpointRequest2 = settingBreakpoint(thread2Ref, debuggeeClass,
                                          breakpointMethod, breakpointLine, "one");
            log2("......breakpointRequest2.enable();");
            breakpointRequest2.enable();

            {  // to get mainThread suspended; otherwise its end results in
               // no suspention for breakpointRequest2 (bug or not?), end of test,
               // and VMDisconnectedException
            thread2Ref.suspend();
            log2("......eventSet.resume();");
            eventSet.resume();
            breakpointForCommunication();
            log2("bpRequest.disable();");
            bpRequest.disable();
            thread2Ref.resume();
            }

            log2("......breakpointInMethod(breakpointRequest2);");
            breakpointInMethod("one");

            log2("......stackFrame2 = thread2Ref.frame(2);");
            stackFrame2 = thread2Ref.frame(2);

            log2("......stackFrame1 = thread2Ref.frame(1);");
            stackFrame1 = thread2Ref.frame(1);

            log2("......thread2Ref.popFrames(stackFrame1);");
            try {
                thread2Ref.popFrames(stackFrame1);
            } catch ( IncompatibleThreadStateException e ) {
                log3("ERROR: IncompatibleThreadStateException");
                testExitCode = FAILED;
                break label0;
            }
            log2("......checking up on equality");
            if ( thread2Ref.frame(0).equals(stackFrame2) ) {
                log3("ERROR: thread2Ref.frame(0).equals(stackFrame2)");
                testExitCode = FAILED;
            }

            log2("......stackFrame1 = thread2Ref.frame(1);");
            try {
                stackFrame1 = thread2Ref.frame(1);
                log3("ERROR: no IndexOutOfBoundsException");
                testExitCode = FAILED;
            } catch ( IndexOutOfBoundsException e ) {
                log2("      IndexOutOfBoundsException");
            }


            log2("......breakpointRequest2.disable();");
            breakpointRequest2.disable();

            vm.resume();

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }


    // ============================== test's additional methods

    private void breakpointInMethod(String number)
                 throws JDITestRuntimeException {

        Event event;

        getEventSet();
        event = eventIterator.nextEvent();

        if ( !(event instanceof BreakpointEvent) )
            throw new JDITestRuntimeException("** event IS NOT a breakpoint **");

        log2("---->: request().getProperty == " +
             event.request().getProperty("number"));

        if ( event.request().getProperty("number").equals(number) )
            return;

        throw new JDITestRuntimeException("** UNEXPECTED breakpoint **");
    }

}
