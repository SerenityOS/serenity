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
 * The test checks that for ThreadStartEvent and                <BR>
 * ThreadDeathEvent sets:                                       <BR>
 *  - the method returns non-null object;                       <BR>
 *  - object's class is subclass of class Iterator.             <BR>
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
 * In second phase the debugger and the debuggee perform the following. <BR>
 * - The debugger creates ThreadStartRequest,resumes the debuggee, and  <BR>
 *   waits for corresponding ThreadStartEvent.                          <BR>
 * - The debuggee starts new thread,  named "thread2",                  <BR>
 *   whose running creates the needed events.                           <BR>
 * - Upon getting the ThreadStartEvent, the debugger checks up on it,   <BR>
 *   creates ThreadDeathRequest, resumes the debuggee, and              <BR>
 *   waits for corresponding ThreadDeathEvent.                          <BR>
 *   Upon getting the ThreadDeathEvent, the debugger checks up on it.   <BR>
 * <BR>
 * In third phase, the debugger and the debuggee end.                   <BR>
 * <BR>
 */

public class eventiterator003 extends JDIBase {

    public static void main(String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run(String argv[], PrintStream out) {

        int exitCode = new eventiterator003().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName = "nsk.jdi.EventSet.eventIterator.eventiterator003a";

    private String testedClassName = "nsk.jdi.EventSet.eventIterator.TestClass";

    //====================================================== test program

    private int runThis(String argv[], PrintStream out) {

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        Binder binder = new Binder(argsHandler, logHandler);

        waitTime = argsHandler.getWaitTime() * 60000;

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
        } catch (Exception e) {
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

        case 0:
            log2("test phase has finished normally");
            log2("   waiting for the debuggee to finish ...");
            debuggee.waitFor();

            log2("......getting the debuggee's exit status");
            int status = debuggee.getStatus();
            if (status != PASS_BASE) {
                log3("ERROR: debuggee returned UNEXPECTED exit status: " + status + " != PASS_BASE");
                testExitCode = FAILED;
            } else {
                log2("......debuggee returned expected exit status: " + status + " == PASS_BASE");
            }
            break;

        default:
            log3("ERROR: runTest() returned unexpected value");

        case 1:
            log3("test phase has not finished normally: debuggee is still alive");
            log2("......forcing: vm.exit();");
            testExitCode = FAILED;
            try {
                vm.exit(PASS_BASE);
            } catch (Exception e) {
                log3("ERROR: Exception : e");
            }
            break;

        case 2:
            log3("test cancelled due to VMDisconnectedException");
            log2("......trying: vm.process().destroy();");
            testExitCode = FAILED;
            try {
                Process vmProcess = vm.process();
                if (vmProcess != null) {
                    vmProcess.destroy();
                }
            } catch (Exception e) {
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

            if (!(eventIterator.nextEvent() instanceof VMDeathEvent)) {
                log3("ERROR: last event is not the VMDeathEvent");
                return 1;
            }

            log2("waiting for VMDisconnectEvent");
            getEventSet();

            if (!(eventIterator.nextEvent() instanceof VMDisconnectEvent)) {
                log3("ERROR: last event is not the VMDisconnectEvent");
                return 1;
            }

            return 0;

        } catch (VMDisconnectedException e) {
            log3("ERROR: VMDisconnectedException : " + e);
            e.printStackTrace(logHandler.getOutStream());
            return 2;
        } catch (Exception e) {
            log3("ERROR: Exception : " + e);
            return 1;
        }

    }

    private void testRun() throws JDITestRuntimeException, Exception {

        eventRManager = vm.eventRequestManager();

        ClassPrepareRequest cpRequest = eventRManager.createClassPrepareRequest();
        cpRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
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
        String lineForComm = "lineForComm";
        BreakpointRequest bpRequest;

        ThreadReference mainThread = debuggee.threadByNameOrThrow("main");

        bpRequest = settingBreakpoint(mainThread, debuggeeClass, bPointMethod, lineForComm, "zero");
        bpRequest.enable();

        //------------------------------------------------------  testing section

        log1("     TESTING BEGINS");

        String thread2Name = "thread2";

        EventRequest eRequest = null;

        label0: {

            vm.resume();
            breakpointForCommunication();

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            ThreadReference tr = (ThreadReference) debuggeeClass.getValue(debuggeeClass.fieldByName(thread2Name));

            log2("......setting up ThreadStartRequest");
            eRequest = settingThreadStartRequest(tr, EventRequest.SUSPEND_EVENT_THREAD, "ThreadStartRequest");
            eRequest.enable();
            vm.resume();
            log2("......getting EventSet");
            getEventSet();
            eRequest.disable();

            String str = (String) eventIterator.nextEvent().request().getProperty("number");
            if (!str.equals("ThreadStartRequest")) {
                testExitCode = FAILED;
                log3("ERROR: new event doesn't corresponds to ThreadStartRequest required");
                break label0;
            }

            log2("......checking up on EventIterator");
            if (eventIterator == null) {
                testExitCode = FAILED;
                log3("ERROR: eventIterator == null");
            }
            if (!(eventIterator instanceof Iterator)) {
                testExitCode = FAILED;
                log3("ERROR: eventIterator is NOT instanceof Iterator");
            }

            log2("......setting up ThreadDeathRequest");
            // suspend policy 'SUSPEND_ALL' should be used for ThreadDeathRequest (see 6609499)
            eRequest = settingThreadDeathRequest(tr, EventRequest.SUSPEND_ALL, "ThreadDeathRequest");
            eRequest.enable();
            eventSet.resume();
            log2("......getting EventSet");
            getEventSet();
            eRequest.disable();

            str = (String) eventIterator.nextEvent().request().getProperty("number");
            if (!str.equals("ThreadDeathRequest")) {
                testExitCode = FAILED;
                log3("ERROR: new event doesn't corresponds to ThreadDeathRequest required");
                break label0;
            }

            log2("......checking up on EventIterator");
            if (eventIterator == null) {
                testExitCode = FAILED;
                log3("ERROR: eventIterator == null");
            }
            if (!(eventIterator instanceof Iterator)) {
                testExitCode = FAILED;
                log3("ERROR: eventIterator is NOT instanceof Iterator");
            }

            eventSet.resume();
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }

        log1("    TESTING ENDS");
        return;
    }

    // ============================== test's additional methods

    private ThreadStartRequest settingThreadStartRequest(ThreadReference thread, int suspendPolicy, String property)
            throws JDITestRuntimeException {
        try {
            ThreadStartRequest tsr = eventRManager.createThreadStartRequest();
            tsr.addThreadFilter(thread);
            tsr.addCountFilter(1);
            tsr.setSuspendPolicy(suspendPolicy);
            tsr.putProperty("number", property);
            return tsr;
        } catch (Exception e) {
            log3("ERROR: ATTENTION: Exception within settingThreadStartRequest() : " + e);
            log3("       ThreadStartRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up ThreadStartRequest **");
        }
    }

    private ThreadDeathRequest settingThreadDeathRequest(ThreadReference thread, int suspendPolicy, String property)
            throws JDITestRuntimeException {
        try {
            ThreadDeathRequest tdr = eventRManager.createThreadDeathRequest();
            tdr.addThreadFilter(thread);
            tdr.addCountFilter(1);
            tdr.setSuspendPolicy(suspendPolicy);
            tdr.putProperty("number", property);
            return tdr;
        } catch (Exception e) {
            log3("ERROR: ATTENTION: Exception within settingThreadDeathRequest() : " + e);
            log3("       ThreadDeathRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up ThreadDeathRequest **");
        }
    }

}
