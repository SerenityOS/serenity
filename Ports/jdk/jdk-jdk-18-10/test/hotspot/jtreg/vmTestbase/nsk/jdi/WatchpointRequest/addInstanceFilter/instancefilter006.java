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

package nsk.jdi.WatchpointRequest.addInstanceFilter;

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
 * WatchpointRequest.                                           <BR>
 *                                                              <BR>
 * The test checks that results of the method                   <BR>
 * <code>com.sun.jdi.WatchpointRequest.addInstanceFilter()</code><BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The test checks up on the following assertion:               <BR>
 *    Throws: UnsupportedOperationException -                   <BR>
 *       if the target virtual machine                          <BR>
 *       does not support this operation.                       <BR>
 * This request is ModificationWatchpointRequest.               <BR>
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
 * - The debuggee creates new threads, thread1, and invokes             <BR>
 *   the methodForCommunication to be suspended and                     <BR>
 *   to inform the debugger with the event.                             <BR>
 * - Upon getting the BreakpointEvent, the debugger creates             <BR>
 *   a ModificationWatchpointRequest and if addInstanceFilter()         <BR>
 *   is not supported, invokes the method on the request expecting      <BR>
 *   to catch UnsupportedOperationException.                            <BR>
 * <BR>
 * In third phase, at the end of the test, the debuggee changes         <BR>
 * the value of the "instruction" which the debugger and debuggee       <BR>
 * use to inform each other of needed actions,  and both end.           <BR>
 * <BR>
 */

public class instancefilter006 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new instancefilter006().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.WatchpointRequest.addInstanceFilter.instancefilter006a";

    private String testedClassName =
      "nsk.jdi.WatchpointRequest.addInstanceFilter.instancefilter006aTestClass";

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

        log1("     TESTING BEGINS");

        EventRequest eventRequest1 = null;

        ThreadReference thread1 = null;
        ThreadReference thread2 = null;

        String thread1Name = "thread1";
        String thread2Name = "thread2";

        String property1 = "ModificationWatchpointRequest1";

        String fieldName = "var1";

        ReferenceType testClassReference = null;

        String         arrayName = "objTC";
        ObjectReference instance = null;


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
                      testClassReference =
                           (ReferenceType) vm.classesByName(testedClassName).get(0);

                      thread1 = (ThreadReference) debuggeeClass.getValue(
                                                  debuggeeClass.fieldByName(thread1Name));

                      eventRequest1 = setting2ModificationWatchpointRequest (thread1,
                                             testClassReference, fieldName,
                                             EventRequest.SUSPEND_NONE, property1);

                      instance = (ObjectReference)
                                 ((ArrayReference) debuggeeClass.getValue(
                                 debuggeeClass.fieldByName(arrayName)) ).getValue(0);

                      if ( vm.canUseInstanceFilters() ) {
                          log2("......vm.canUseInstanceFilters == true :: test cancelled");
                      } else {

                          try {
                              log2("......eventRequest1.addInstanceFilter(instance);");
                              log2("        UnsupportedOperationException  expected");
                              ((WatchpointRequest) eventRequest1).addInstanceFilter(instance);
                              log3("ERROR: no UnsupportedOperationException ");
                              testExitCode = FAILED;
                          } catch ( InvalidRequestStateException e ) {
                              log2("        UnsupportedOperationException ");
                          }
                      }

                      break;

              default:
                      throw new JDITestRuntimeException("** default case 2 **");
            }

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

    // ============================== test's additional methods

    private ModificationWatchpointRequest setting2ModificationWatchpointRequest (
                                                  ThreadReference thread,
                                                  ReferenceType   testedClass,
                                                  String          fieldName,
                                                  int             suspendPolicy,
                                                  String          property        )
            throws JDITestRuntimeException {
        try {
            log2("......setting up ModificationWatchpointRequest:");
            log2("       thread: " + thread + "; class: " + testedClass + "; fieldName: " + fieldName);
            Field field = testedClass.fieldByName(fieldName);

            ModificationWatchpointRequest
            awr = eventRManager.createModificationWatchpointRequest(field);
            awr.putProperty("number", property);

            if (thread != null)
                awr.addThreadFilter(thread);
            awr.setSuspendPolicy(suspendPolicy);

            log2("      ModificationWatchpointRequest has been set up");
            return awr;
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingModificationWatchpointRequest() : " + e);
            log3("       ModificationWatchpointRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up ModificationWatchpointRequest **");
        }
    }

}
