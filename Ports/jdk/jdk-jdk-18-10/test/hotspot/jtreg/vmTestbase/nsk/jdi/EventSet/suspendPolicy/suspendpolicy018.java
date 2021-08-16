/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.EventSet.suspendPolicy;

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
 * <code>com.sun.jdi.EventSet.suspendPolicy()</code>            <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The test checks that for Event sets,                         <BR>
 * corresponding to VMStart, VMDeath, and VMDisconnect Events,  <BR>
 * the method returns                                           <BR>
 *      SUSPEND_ALL, SUSPEND_NONE, and SUSPEND_NONE.            <BR>
 * <BR>
 * The test works as follows.                                   <BR>
 * <BR>
 * Upon launching debuggee's VM which will be suspended,                <BR>
 * a debugger waits for the VMStartEvent within a predefined            <BR>
 * time interval. If no the VMStartEvent received, the test is FAILED.  <BR>
 * Upon getting the VMStartEvent,                                       <BR>
 * it checks up on the value returned by the method suspendPolicy(),    <BR>
 * resumes the VM, and waits for VMDeathEvent within                    <BR>
 * the predefined time interval.                                        <BR>
 * If no the event received, the test is FAILED.                        <BR>
 * Upon getting the VMDeathEvent, the debugger                          <BR>
 * checks up on the value returned by the method suspendPolicy() and    <BR>
 * waits for VMDisconnectEvent within the predefined time interval.     <BR>
 * If no the event received, the test is FAILED.                        <BR>
 * Upon getting the VMDisconnectEvent, the debugger                     <BR>
 * checks up on the value returned by the method suspendPolicy().       <BR>
 * <BR>
 */

public class suspendpolicy018 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/EventSet/suspendPolicy/suspendpolicy018 ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new suspendpolicy018().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
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
        "nsk.jdi.EventSet.suspendPolicy.suspendpolicy018a";

    private String testedClassName =
        "nsk.jdi.EventSet.suspendPolicy.TestClass";

    //====================================================== test program
    //------------------------------------------------------ common section

    static Debugee          debuggee;
    static ArgumentHandler  argsHandler;

    static int waitTime;

    static VirtualMachine      vm            = null;
    static EventRequestManager eventRManager = null;
    static EventQueue          eventQueue    = null;
    static EventSet            eventSet      = null;
    static EventIterator       eventIterator = null;

    static ReferenceType       debuggeeClass = null;

    static int  testExitCode = PASSED;


    class JDITestRuntimeException extends RuntimeException {
        JDITestRuntimeException(String str) {
            super("JDITestRuntimeException : " + str);
        }
    }

    int policyToCheck = 0;

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
                debuggee = binder.bindToDebugeeNoWait(debuggeeName + " -vbs");
            } else {
                debuggee = binder.bindToDebugeeNoWait(debuggeeName);
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
            log2("waiting for VMStartEvent");
            getEventSet();
            if (eventIterator.nextEvent() instanceof VMStartEvent) {
                log2("VMStartEvent received; test begins");
                int sp1 = eventSet.suspendPolicy();
                if (sp1 != EventRequest.SUSPEND_ALL) {
                    log3("ERROR: eventSet.suspendPolicy() != EventRequest.SUSPEND_ALL   but : " + sp1);
                    testExitCode = FAILED;
                }

                testRun();

                log2("waiting for VMDeathEvent");
                getEventSet();
                if ( !(eventIterator.nextEvent() instanceof VMDeathEvent) ) {
                    log3("ERROR: last event is not the VMDeathEvent");
                    return 1;
                }
                sp1 = eventSet.suspendPolicy();
                if (sp1 != EventRequest.SUSPEND_NONE) {
                    log3("ERROR: eventSet.suspendPolicy() != EventRequest.SUSPEND_NONE   but : " + sp1);
                    testExitCode = FAILED;
                }

                log2("waiting for VMDisconnectEvent");
                getEventSet();
                if ( !(eventIterator.nextEvent() instanceof VMDisconnectEvent) ) {
                    log3("ERROR: last event is not the VMDisconnectEvent");
                    return 1;
                }
                sp1 = eventSet.suspendPolicy();
                if (sp1 != EventRequest.SUSPEND_NONE) {
                    log3("ERROR: eventSet.suspendPolicy() != EventRequest.SUSPEND_NONE   but : " + sp1);
                    testExitCode = FAILED;
                }

                return 0;
            } else {
                log3("ERROR: first event is not the VMStartEvent");
                return 1;
            }
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

        log2("......getting ClassPrepareEvent for debuggee's class");
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

        vm.resume();

        log1("    TESTING ENDS");
        return;
    }

    private void getEventSet()
                 throws JDITestRuntimeException {
        try {
//            log2("       eventSet = eventQueue.remove(waitTime);");
            eventSet = eventQueue.remove(waitTime);
            if (eventSet == null) {
                throw new JDITestRuntimeException("** TIMEOUT while waiting for event **");
            }
//            log2("       eventIterator = eventSet.eventIterator;");
            eventIterator = eventSet.eventIterator();
        } catch ( Exception e ) {
            throw new JDITestRuntimeException("** EXCEPTION while waiting for event ** : " + e);
        }
    }

}
