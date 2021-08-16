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
 * <BR>
 * The test checks up that locks acquired by a popped frame,    <BR>
 * are released when it is popped.                              <BR>
 * This applies to synchronized methods being popped.           <BR>
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
 * In second phase to perform the above check,                          <BR>
 * the debugger and the debuggee perform the following.                 <BR>
 * - The debugger resumes the debuggee and waits for the BreakpointEvent.<BR>
 * - The debuggee creates two threads, thread2 and thread3, which will  <BR>
 * call a tested synchronized poppedMethod() from their run() methods.  <BR>
 * After getting the threads started, the debuggee invokes the          <BR>
 * methodForCommunication() to inform the debugger of the threads.      <BR>
 * - The debugger sets up Breakpoint and MethodEntry Requests           <BR>
 * within the poppedMethod(), first one for the thread2 and             <BR>
 * second one for the thread3, and resumes debuggee's main thread.      <BR>
 * - The main thread releases the thread2 and the thread3 in the order  <BR>
 * which makes the thread2 to be suspended at the breakpoint within     <BR>
 * the poppedMethod and the thread3 to be waiting for releasing the     <BR>
 * synchronized poppedMethod by the thread2. Then                       <BR>
 * the main thread informs the debugger via methodForCommunication().   <BR>
 * - After getting both events, from the thread2 and the main,          <BR>
 * the debugger invokes the popFrames() method and waits for            <BR>
 * the MethodEntry event in poppedMethod from the thread3.              <BR>
 * If no the event, the test FAILED.                                    <BR>
 */

public class popframes003 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new popframes003().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.ThreadReference.popFrames.popframes003a";

    //====================================================== test program

    BreakpointRequest  bpRequest;
    MethodEntryRequest meRequest;

    BreakpointRequest  bpRequest2;
    MethodEntryRequest meRequest3;

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

        ThreadReference threadMainRef = debuggee.threadByNameOrThrow("main");
        try {
            bpRequest = settingBreakpoint(threadMainRef,
                                          debuggeeClass,
                                          bPointMethod, lineForComm, "zero");
        } catch ( Exception e ) {
            throw e;
        }
        bpRequest.enable();

    //------------------------------------------------------  testing section

        log1("     TESTING BEGINS");

        EventSet mainThreadEventSet = null;

        int flag = 0;


        vm.resume();
        breakpointForCommunication();

        log2("......setting MethodEntryRequest (meRequest) in ForCommunication.methodForCommunication");
        meRequest = settingMethodEntryRequest(threadMainRef,
                                              debuggeeName + "$ForCommunication",
                                              "zero");
        log2("meRequest.enable();");
        meRequest.enable();


        String thread2Name         = "thread2";
        ThreadReference thread2Ref = debuggee.threadByNameOrThrow(thread2Name);

        String thread3Name         = "thread3";
        ThreadReference thread3Ref = debuggee.threadByNameOrThrow(thread3Name);

        String poppedMethod    = "poppedMethod";
        String breakpointLine  = "breakpointLine";
        String testVariable    = "testVar1";

        log2("......setting BreakpointRequest (bpRequest2) in poppedMethod");
        List classes = vm.classesByName(debuggeeName + "$Popped");
        ReferenceType poppedClass = (ReferenceType) classes.get(0);
        bpRequest2 = settingBreakpoint(thread2Ref, poppedClass,
                                          poppedMethod, breakpointLine, "one");
        log2("......bpRequest2.enable();");
        bpRequest2.enable();

        log2("......setting MethodEntryRequest (meRequest3) in poppedMethod");
        meRequest3 = settingMethodEntryRequest(thread3Ref, debuggeeName + "$Popped",
                                          "two");
        log2("......meRequest3.enable();");
        meRequest3.enable();

        log2("......eventSet.resume();");
        eventSet.resume();

        log2(".....waiting for Breakpoint(thread2) and MethodEntry(main) Events");
        for ( ; ; ) {

            Event event1;

            if (flag == 3)
                break;

            getEventSet();
            event1 = eventIterator.nextEvent();

            if ( event1 instanceof MethodEntryEvent ) {
                mainThreadEventSet = eventSet;
                flag |= 1;
                log2("......event1 instanceof MethodEntryEvent");
            } else if ( event1 instanceof BreakpointEvent ) {
                flag |= 2;
                log2("......event1 instanceof BreakpointEvent");
            } else
                throw new JDITestRuntimeException("** event1 IS NOT Breakpoint or MethodEntry **");
        }

        log2("......bpRequest2.disable();");
        bpRequest2.disable();

        label0:
        {
//            log2("        THREAD_STATUS_RUNNING == " + ThreadReference.THREAD_STATUS_RUNNING );
//            log2("        thread2's status      == " + thread2Ref.status());

            if (thread2Ref.isSuspended())
                log2("         thread2Ref.isSuspended()");
            else {
                log3("ERROR:  !thread2Ref.isSuspended()");
                testExitCode = FAILED;
                break label0;
            }

            log2("......mainThreadEventSet.resume();");
            mainThreadEventSet.resume();

            log2("......up to WAITTIME loop of sleeping");
            log2("         to get thread3 waiting on monitor after calling poppedMethod");
            int thread3Status = 0;
            for ( int i1 = 0; i1 < waitTime; i1 += 10000) {
                if (thread3Ref.status() == ThreadReference.THREAD_STATUS_MONITOR) {
                    thread3Status = 1;
                    break;
                }
                Thread.sleep(10000);
            }
            if (thread3Status != 1) {
                log3("ERROR: thread3Ref.status() != THREAD_STATUS_MONITOR");
                testExitCode = FAILED;
                break label0;
            }

            log2("......StackFrame stackFrame = thread2Ref.frame(0);");
            StackFrame stackFrame = thread2Ref.frame(0);

            log2("......thread2Ref.popFrames(stackFrame);");
            try {
                thread2Ref.popFrames(stackFrame);
            } catch ( IncompatibleThreadStateException e ) {
                log3("ERROR: IncompatibleThreadStateException");
                testExitCode = FAILED;
                break label0;
            }

            log2(".....waiting for MethodEntry (main and thread3) Events");
            {
                flag = 0;

                for ( ; ; ) {

                    Event event1;

                    if (flag == 3)
                        break;

                    getEventSet();
                    event1 = eventIterator.nextEvent();

                    if ( !(event1 instanceof MethodEntryEvent) )
                        throw new JDITestRuntimeException("** event IS NOT MethodEntry **");

                    if ( event1.request().getProperty("number").equals("zero") ) {
                        flag |= 1;
                        log2("......MethodEntry in mainThread");
                    } else {
                        flag |= 2;
                        log2("......MethodEntry in thread3");
                    }
                }
            }

        }

        log2("......disabling requests and resuming vm");
        meRequest3.disable();
        vm.resume();

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        log1("    TESTING ENDS");
        return;
    }

   /*
    * private MethodEntryRequest settingMethodEntryRequest(ThreadReference, ReferenceType,
    *                                             String)
    *
    * It sets up a MethodEntryRequest in a given class for a given thread.
    *
    * Return value: MethodEntryRequest object  in case of success
    *
    * JDITestRuntimeException   in case of an Exception thrown within the method
    */

    private MethodEntryRequest settingMethodEntryRequest ( ThreadReference thread,
                                                         String className,
                                                         String property)
            throws JDITestRuntimeException {

        log2("......setting up a methodEntryRequest:");
        log2("       thread: " + thread + "; className: " + className +
                        "; property: " + property);

        List classList = vm.classesByName(className);
        ReferenceType classRef = (ReferenceType) classList.get(0);

        MethodEntryRequest methodEntryRequest = null;

        try {
             methodEntryRequest = eventRManager.createMethodEntryRequest();
             methodEntryRequest.putProperty("number", property);
             methodEntryRequest.addThreadFilter(thread);
             methodEntryRequest.addClassFilter(classRef);
             methodEntryRequest.setSuspendPolicy( EventRequest.SUSPEND_EVENT_THREAD);
        } catch ( Exception e ) {
             log3("ERROR: Exception within methodEntryRequest() : " + e);
             throw new JDITestRuntimeException("** FAILURE to set up a MethodEntryRequest **");
        }

        log2("      a methodEntryRequest has been set up");
        return methodEntryRequest;
    }


}
