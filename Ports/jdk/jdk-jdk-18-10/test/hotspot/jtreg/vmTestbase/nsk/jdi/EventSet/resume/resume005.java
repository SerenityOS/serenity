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

package nsk.jdi.EventSet.resume;

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
 * <code>com.sun.jdi.EventSet.resume()</code>                   <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * Test cases include all three possible suspensions, NONE,     <BR>
 * EVENT_THREAD, and ALL, for ExceptionEvent sets.              <BR>
 * <BR>
 * To check up on the method, a debugger,                       <BR>
 * upon getting new set for the EventSet,                       <BR>
 * suspends VM with the method VirtualMachine.suspend(),        <BR>
 * gets the List of debuggee's threads calling VM.allThreads(), <BR>
 * invokes the method EventSet.resume(), and                    <BR>
 * gets another List of debuggee's threads.                     <BR>
 * The debugger then compares values of                         <BR>
 * each thread's suspendCount from first and second Lists.      <BR>
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
 * In second phase to check the above,                                  <BR>
 * the debugger and the debuggee perform the following loop.            <BR>
 * - The debugger sets up a ExceptionRequest, resumes                   <BR>
 *   the debuggee, and waits for the ExceptionEvent.                    <BR>
 * - The debuggee invokes the special method which makes access to      <BR>
 *   a predefined variable to be resulting in the event.                <BR>
 * - Upon getting new event, the debugger                               <BR>
 *   performs the check corresponding to the event.                     <BR>
 * <BR>
 * Note. To inform each other of needed actions, the debugger and       <BR>
 *       and the debuggee use debuggee's variable "instruction".        <BR>
 * In third phase, at the end,                                          <BR>
 * the debuggee changes the value of the "instruction"                  <BR>
 * to inform the debugger of checks finished, and both end.             <BR>
 * <BR>
 */

public class resume005 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new resume005().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.EventSet.resume.resume005a";

    private String testedClassName =
      "nsk.jdi.EventSet.resume.resume005aTestClass";

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
        ThreadReference   mainThread = debuggee.threadByNameOrThrow("main");
        bpRequest = settingBreakpoint(mainThread,
                                      debuggeeClass,
                                      bPointMethod, lineForComm, "zero");
        bpRequest.enable();

        vm.resume();

    //------------------------------------------------------  testing section

        log1("     TESTING BEGINS");

        EventRequest eventRequest1 = null;
        EventRequest eventRequest2 = null;
        EventRequest eventRequest3 = null;

        final int SUSPEND_NONE   = EventRequest.SUSPEND_NONE;
        final int SUSPEND_THREAD = EventRequest.SUSPEND_EVENT_THREAD;
        final int SUSPEND_ALL    = EventRequest.SUSPEND_ALL;

        String methodName = "method";
        String bpLineName = "breakpointLine";

        ReferenceType testClassReference = null;


        for (int i = 0; ; i++) {

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
                      testClassReference =
                           (ReferenceType) vm.classesByName(testedClassName).get(0);

                      eventRequest1 = settingExceptionRequest (mainThread,
                                             debuggeeClass, // testClassReference,
                                             SUSPEND_NONE, "ExceptionRequest1");
                      eventRequest1.enable();
                      break;

              case 1:
                      eventRequest2 = settingExceptionRequest (mainThread,
                                             debuggeeClass, // testClassReference,
                                             SUSPEND_THREAD, "ExceptionRequest2");
                      eventRequest2.enable();
                      break;

              case 2:
                      eventRequest3 = settingExceptionRequest (mainThread,
                                             debuggeeClass, // testClassReference,
                                             SUSPEND_ALL, "ExceptionRequest3");
                      eventRequest3.enable();
                      break;


              default:
                      throw new JDITestRuntimeException("** default case 2 **");
            }

            log2("......waiting for new ExceptionEvent : " + i);
            mainThread.resume();
            getEventSet();

            Event newEvent = eventIterator.nextEvent();
            if ( !(newEvent instanceof ExceptionEvent)) {
                 log3("ERROR: new event is not ExceptionEvent");
                 testExitCode = FAILED;
            } else {

                String property = (String) newEvent.request().getProperty("number");
                log2("       got new ExceptionEvent with propety 'number' == " + property);

                log2("......checking up on EventSet.resume()");
                log2("......--> vm.suspend();");
                vm.suspend();

                log2("        getting : Map<String, Integer> suspendsCounts1");

                Map<String, Integer> suspendsCounts1 = new HashMap<String, Integer>();
                for (ThreadReference threadReference : vm.allThreads()) {
                    suspendsCounts1.put(threadReference.name(), threadReference.suspendCount());
                }

                log2("        eventSet.resume;");
                eventSet.resume();

                log2("        getting : Map<String, Integer> suspendsCounts2");
                Map<String, Integer> suspendsCounts2 = new HashMap<String, Integer>();
                for (ThreadReference threadReference : vm.allThreads()) {
                    suspendsCounts2.put(threadReference.name(), threadReference.suspendCount());
                }

                log2(suspendsCounts1.toString());
                log2(suspendsCounts2.toString());

                log2("        getting : int policy = eventSet.suspendPolicy();");
                int policy = eventSet.suspendPolicy();

                switch (policy) {

                  case SUSPEND_NONE   :
                       log2("        case SUSPEND_NONE");
                       for (String threadName : suspendsCounts1.keySet()) {
                           log2("        checking " + threadName);
                           if (!suspendsCounts2.containsKey(threadName)) {
                               log3("ERROR: couldn't get ThreadReference for " + threadName);
                               testExitCode = FAILED;
                               break;
                           }
                           int count1 = suspendsCounts1.get(threadName);
                           int count2 = suspendsCounts2.get(threadName);
                           if (count1 != count2) {
                               log3("ERROR: suspendCounts don't match for : " + threadName);
                               log3("before resuming : " + count1);
                               log3("after  resuming : " + count2);
                               testExitCode = FAILED;
                               break;
                           }
                       }
                       eventRequest1.disable();
                       break;

                  case SUSPEND_THREAD :
                       log2("        case SUSPEND_THREAD");
                       for (String threadName : suspendsCounts1.keySet()) {
                           log2("checking " + threadName);
                           if (!suspendsCounts2.containsKey(threadName)) {
                               log3("ERROR: couldn't get ThreadReference for " + threadName);
                               testExitCode = FAILED;
                               break;
                           }
                           int count1 = suspendsCounts1.get(threadName);
                           int count2 = suspendsCounts2.get(threadName);
                           String eventThreadName = ((ExceptionEvent)newEvent).thread().name();
                           int expectedValue = count2 + (eventThreadName.equals(threadName) ? 1 : 0);
                           if (count1 != expectedValue) {
                               log3("ERROR: suspendCounts don't match for : " + threadName);
                               log3("before resuming : " + count1);
                               log3("after  resuming : " + count2);
                               testExitCode = FAILED;
                               break;
                           }
                       }
                       eventRequest2.disable();
                       break;

                  case SUSPEND_ALL    :

                        log2("        case SUSPEND_ALL");
                        for (String threadName : suspendsCounts1.keySet()) {
                            log2("checking " + threadName);
                            if (!event.request().equals(eventRequest3))
                                break;
                            if (!suspendsCounts2.containsKey(threadName)) {
                                log3("ERROR: couldn't get ThreadReference for " + threadName);
                                testExitCode = FAILED;
                                break;
                            }
                            int count1 = suspendsCounts1.get(threadName);
                            int count2 = suspendsCounts2.get(threadName);
                            if (count1 != count2 + 1) {
                                log3("ERROR: suspendCounts don't match for : " + threadName);
                                log3("before resuming : " + count1);
                                log3("after  resuming : " + count2);
                                testExitCode = FAILED;
                                break;
                            }
                        }
                        eventRequest3.disable();
                        break;

                  default: throw new JDITestRuntimeException("** default case 1 **");
                }
                informDebuggeeTestCase(i);
            }

            log2("......--> vm.resume()");
            vm.resume();
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

    // ============================== test's additional methods

    private ExceptionRequest settingExceptionRequest ( ThreadReference thread,
                                                       ReferenceType   testedClass,
                                                       int             suspendPolicy,
                                                       String          property       )
            throws JDITestRuntimeException {
        try {
            log2("......setting up ExceptionRequest:");
            log2("       thread: " + thread + "; class: " + testedClass + "; property: " + property);

            ExceptionRequest
            excr = eventRManager.createExceptionRequest(null, true, true);
            excr.putProperty("number", property);
            excr.addThreadFilter(thread);
            excr.addClassFilter(testedClass);
            excr.setSuspendPolicy(suspendPolicy);

            log2("      ExceptionRequest has been set up");
            return excr;
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingExceptionRequest() : " + e);
            log3("       ExceptionRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up ExceptionRequest **");
        }
    }
    /**
     * Inform debuggee which thread test the debugger has completed.
     * Used for synchronization, so the debuggee does not move too quickly.
     * @param testCase index of just completed test
     */
    void informDebuggeeTestCase(int testCase) {
        try {
            ((ClassType)debuggeeClass)
                .setValue(debuggeeClass.fieldByName("testCase"),
                          vm.mirrorOf(testCase));
        } catch (InvalidTypeException ite) {
            throw new Failure("** FAILURE setting testCase  **");
        } catch (ClassNotLoadedException cnle) {
            throw new Failure("** FAILURE notifying debuggee  **");
        } catch (VMDisconnectedException e) {
            throw new Failure("** FAILURE debuggee connection **");
        }
    }
}
