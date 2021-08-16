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
 * The test checks that for each type of the following Events:  <BR>
 * AccessWatchpoint, ModificationWatchpoint, Breakpoint,        <BR>
 * Exception, MethodEntry, MethodExit, and Step,                <BR>
 *  - the method returns non-null object, and                   <BR>
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
 * In second phase the debugger and debuggee perform the following loop.<BR>
 * - The debugger creates new Request, resumes the debuggee, and        <BR>
 *   waits for corresponding Event.                                     <BR>
 * - The debuggee performes an action resulting in Event required.      <BR>
 * - Upon getting the Event, the debugger checks up on EventSet.        <BR>
 * <BR>
 * In third phase, at the end                                           <BR>
 * the debuggee changes the value of the "instruction"                  <BR>
 * to inform the debugger of checks finished, and both end.             <BR>
 * <BR>
 * Note. To inform each other of needed actions, the debugger and       <BR>
 *       and the debuggee use debuggee's variable "instruction".        <BR>
 * <BR>
 */

public class eventiterator002 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new eventiterator002().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.EventSet.eventIterator.eventiterator002a";

    private String testedClassName =
      "nsk.jdi.EventSet.eventIterator.eventiterator002aTestClass";

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

        ThreadReference mainThread = debuggee.threadByNameOrThrow("main");

        bpRequest = settingBreakpoint(mainThread,
                                      debuggeeClass,
                                      bPointMethod, lineForComm, "zero");
        bpRequest.enable();

    //------------------------------------------------------  testing section

        log1("     TESTING BEGINS");

            String accWatchpointName = "var1";
            String modWatchpointName = "var2";
            String bpLineName        = "breakpointLine";
            String bpMethodName      = "method";
            String awFieldName       = "awFieldName";
            String mwFieldName       = "mwFieldName";
            String excName           = "method";
            String menName           = "method";
            String mexName           = "method";

        EventRequest eRequest = null;

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

            log2("......ReferenceType testClass = (ReferenceType) (vm.classesByName(testedClassName)).get(0);");
            ReferenceType testClass = (ReferenceType) (vm.classesByName(testedClassName)).get(0);

            switch (i) {

              case 0:
                      eRequest = null;
                      if (vm.canWatchFieldAccess()) {
                          String awName = ( (StringReference) testClass.getValue(
                                            testClass.fieldByName(awFieldName))).value();
                          eRequest = settingAccessWatchpointRequest(mainThread,
                                            testClass, awName, "AccessWatchpointRequest");
                          eRequest.enable();
                      }
                      break;

              case 1:
                      eRequest = null;
                      if (vm.canWatchFieldModification() ) {
                          String mwName = ( (StringReference) testClass.getValue(
                                            testClass.fieldByName(mwFieldName))).value();
                          eRequest = settingModificationWatchpointRequest(mainThread,
                                            testClass, mwName, "ModificationWatchpointRequest");
                          eRequest.enable();
                      }
                      break;

              case 2:
                      eRequest = settingBreakpointRequest(mainThread, testClass,
                                        bpMethodName, bpLineName,
                                        EventRequest.SUSPEND_EVENT_THREAD, "BreakpointRequest");
                      eRequest.enable();
                      break;

              case 3:
                      eRequest = settingExceptionRequest(mainThread, testClass,
                                                                "ExceptionRequest");
                      eRequest.enable();
                      break;

              case 4:
                      eRequest = settingMethodEntryRequest(mainThread, testClass,
                                                                "MethodEntryRequest");
                      eRequest.enable();
                      break;

              case 5:
                      eRequest = settingMethodExitRequest(mainThread, testClass,
                                                                "MethodExitRequest");
                      eRequest.enable();
                      break;

              case 6:
                      eRequest = settingStepRequest(mainThread, "StepRequest");
                      eRequest.enable();
                      break;
            }

                                 //  see setting up Acceess&Modification requests above
            if (eRequest == null)
                continue;

            log2("......getting new Event and checking up on EventIterator");
            vm.resume();
            getEventSet();
            eRequest.disable();

            if (eventIterator == null) {
                testExitCode = FAILED;
                log3("ERROR: eventIterator == null");
            }
            if ( !(eventIterator instanceof Iterator) ) {
                testExitCode = FAILED;
                log3("ERROR: eventIterator is NOT instanceof Iterator");
            }

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

    // ============================== test's additional methods

    private AccessWatchpointRequest settingAccessWatchpointRequest (
                                                  ThreadReference thread,
                                                  ReferenceType   testedClass,
                                                  String          fieldName,
                                                  String          property        )
            throws JDITestRuntimeException {
        try {
            log2("......setting up AccessWatchpointRequest:");
            log2("       thread: " + thread + "; class: " + testedClass + "; fieldName: " + fieldName);
            Field field = testedClass.fieldByName(fieldName);

            AccessWatchpointRequest
            awr = eventRManager.createAccessWatchpointRequest(field);
            awr.putProperty("number", property);
            awr.addThreadFilter(thread);

            log2("      AccessWatchpointRequest has been set up");
            return awr;
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingAccessWatchpointRequest() : " + e);
            log3("       AccessWatchpointRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up AccessWatchpointRequest **");
        }
    }

    private ModificationWatchpointRequest settingModificationWatchpointRequest (
                                                  ThreadReference thread,
                                                  ReferenceType   testedClass,
                                                  String          fieldName,
                                                  String          property        )
            throws JDITestRuntimeException {
        try {
            log2("......setting up ModificationWatchpointRequest:");
            log2("       thread: " + thread + "; class: " + testedClass + "; fieldName: " + fieldName);
            Field field = testedClass.fieldByName(fieldName);

            ModificationWatchpointRequest
            mwr = eventRManager.createModificationWatchpointRequest(field);
            mwr.putProperty("number", property);
            mwr.addThreadFilter(thread);

            log2("      ModificationWatchpointRequest has been set up");
            return mwr;
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingModificationWatchpointRequest() : " + e);
            log3("       ModificationWatchpointRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up ModificationWatchpointRequest **");
        }
    }

    private BreakpointRequest settingBreakpointRequest ( ThreadReference thread,
                                                         ReferenceType   testedClass,
                                                         String          methodName,
                                                         String          bpLine,
                                                         int             suspendPolicy,
                                                         String          property        )
            throws JDITestRuntimeException {
        try {
            log2("......setting up a breakpoint:");
            log2("       thread: " + thread + "; class: " + testedClass + "; method: " + methodName + "; line: " + bpLine);

            int n = ( (IntegerValue) testedClass.getValue(testedClass.fieldByName(bpLine) ) ).value();
            Location loc = (Location) ((Method) testedClass.methodsByName(methodName).get(0)).allLineLocations().get(n);

            BreakpointRequest
            bpr = eventRManager.createBreakpointRequest(loc);
            bpr.putProperty("number", property);
            bpr.addThreadFilter(thread);
            bpr.setSuspendPolicy(suspendPolicy);

            log2("      a breakpoint has been set up");
            return bpr;
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingBreakpointRequest() : " + e);
            log3("       BreakpointRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up BreakpointRequest **");
        }
    }

    private MethodEntryRequest settingMethodEntryRequest ( ThreadReference thread,
                                                           ReferenceType   testedClass,
                                                           String          property       )
            throws JDITestRuntimeException {
        try {
            log2("......setting up MethodEntryRequest:");
            log2("       thread: " + thread + "; class: " + testedClass +  "; property: " + property);

            MethodEntryRequest
            menr = eventRManager.createMethodEntryRequest();
            menr.putProperty("number", property);
            menr.addThreadFilter(thread);
            menr.addClassFilter(testedClass);

            log2("      a MethodEntryRequest has been set up");
            return menr;
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingMethodEntryRequest() : " + e);
            log3("       MethodEntryRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up MethodEntryRequest **");
        }
    }

    private MethodExitRequest settingMethodExitRequest ( ThreadReference thread,
                                                         ReferenceType   testedClass,
                                                         String          property        )
            throws JDITestRuntimeException {
        try {
            log2("......setting up MethodExitRequest:");
            log2("       thread: " + thread + "; class: " + testedClass + "; property: " + property);

            MethodExitRequest
            mexr = eventRManager.createMethodExitRequest();
            mexr.putProperty("number", property);
            mexr.addThreadFilter(thread);
            mexr.addClassFilter(testedClass);

        log2("      MethodExitRequest has been set up");
        return mexr;
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingMethodExitRequest() : " + e);
            log3("       MethodExitRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up MethodExitRequest **");
        }
    }

    private StepRequest settingStepRequest ( ThreadReference thread,
                                             String          property        )
            throws JDITestRuntimeException {
        try {
            log2("......setting up StepRequest:");
            log2("       thread: " + thread + "; property: " + property);

            StepRequest
            str = eventRManager.createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_OVER);
            str.putProperty("number", property);
            str.addCountFilter(1);

            log2("      StepRequest has been set up");
            return str;
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingStepRequest() : " + e);
            log3("       StepRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up StepRequest **");
        }
    }

    private ExceptionRequest settingExceptionRequest ( ThreadReference thread,
                                                       ReferenceType   testedClass,
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

            log2("      ExceptionRequest has been set up");
            return excr;
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingExceptionRequest() : " + e);
            log3("       ExceptionRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up ExceptionRequest **");
        }
    }

}
