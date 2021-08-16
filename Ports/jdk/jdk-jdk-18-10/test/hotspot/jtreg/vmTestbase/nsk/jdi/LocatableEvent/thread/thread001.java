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

package nsk.jdi.LocatableEvent.thread;

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
 * LocatableEvent.                                              <BR>
 *                                                              <BR>
 * The test checks that results of the method                   <BR>
 * <code>com.sun.jdi.LocatableEvent.thread()</code>             <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * The cases for testing include all Locatable events.          <BR>
 * The test checks that                                         <BR>
 * for each type of LocatableEvent received in a debugger,      <BR>
 * a value returned by the method invoked on                    <BR>
 * a <type>Event object corresponds to a thread                 <BR>
 * which debuggee's counterpart event was generated in.         <BR>
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
 * In second phase to check Locatable events,                           <BR>
 * the debugger and the debuggee perform the following.                 <BR>
 * - The debugger resumes the debuggee and waits for the BreakpointEvent.<BR>
 * - The debuggee creates a number of threads, one for each check case  <BR>
 *   and invokes the methodForCommunication to be suspended and         <BR>
 *   to inform the debugger with the event.                             <BR>
 * - Upon getting the BreakpointEvent, the debugger                     <BR>
 *   gets ThreadReferences mirroring all tested threads in the debuggee,<BR>
 *   sets up Requests within them to get Events to check up on,         <BR>
 *   resumes the debuggee, waits for events, and upon getting them,     <BR>
 *   compares ThreadReferences put into Requests to ones from Events;   <BR>
 *   if any mismatch, the test FAILED.                                  <BR>
 * <BR>
 * Note. To inform each other of needed actions, the debugger and       <BR>
 *       and the debuggee use debuggeee's variable "instruction".       <BR>
 * <BR>
 * In third phase when at the end,                                      <BR>
 * the debuggee changes the value of the "instruction"                  <BR>
 * to inform the debugger of checks finished, and both end.             <BR>
 * <BR>
 */

public class thread001 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new thread001().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.LocatableEvent.thread.thread001a";

    private String testedClassName =
      "nsk.jdi.LocatableEvent.thread.TestClass";

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

        bpRequest = settingBreakpoint(debuggee.threadByNameOrThrow("main"),
                                      debuggeeClass,
                                      bPointMethod, lineForComm, "zero");
        bpRequest.enable();

    //------------------------------------------------------  testing section

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

            String accWatchpointName = "var1";
            String modWatchpointName = "var2";
            String bpLineName        = "breakpointLine";
            String bpMethodName      = "method";
            String awFieldName       = "awFieldName";
            String mwFieldName       = "mwFieldName";
            String excName           = "method";
            String menName           = "method";
            String mexName           = "method";

            String namesArray = "threadNames";

            String threadNames[] = {
                    "awThread"  ,
                    "mwThread"  ,
                    "bpThread"  ,
                    "excThread" ,
                    "menThread" ,
                    "mexThread" ,
                    "stThread"
               };


            EventRequest eRequests[] = new EventRequest[threadNames.length];
            int flags = 0;

            ThreadReference eventThreads[] = new ThreadReference[threadNames.length];


            List allThreads = vm.allThreads();

            log2("......getting: ArrayReference namesRef = (ArrayReference) debuggeeClass.getValue(debuggeeClass.fieldByName(namesArray));");
            ArrayReference namesRef = (ArrayReference)
                         debuggeeClass.getValue(debuggeeClass.fieldByName(namesArray));
            log2("       namesRef.length() == " + namesRef.length());

            log2("......getting and chcking up on debuggee threads' names");
            for (int n1 = 0; n1 < namesRef.length(); n1++) {

                log2("      String name = ((StringReference) namesRef.getValue(n1)).value();");
                String name = ((StringReference) namesRef.getValue(n1)).value();

                label0: {
                    for (int n2 = 0; n2 < threadNames.length; n2++) {

                        if (name.equals(threadNames[n2])) {
                            ListIterator li  = allThreads.listIterator();
                            for (; li.hasNext(); ) {
                                ThreadReference thread = (ThreadReference) li.next();
                                if (thread.name().equals(name)) {
                                    eventThreads[n1] =  thread;
                                    break;
                                }
                            }
                            break label0;
                        }
                    }
                    testExitCode = FAILED;
                    log3("ERROR: no thread found in the debuggee : " + name);
                }
            }
            if (testExitCode == FAILED)
                break;


            log2("......ReferenceType testClass = (ReferenceType) (vm.classesByName(testedClassName)).get(0);");
            ReferenceType testClass = (ReferenceType) (vm.classesByName(testedClassName)).get(0);

            log2("......setting up Requests");
            for ( int n3 = 0; n3 < namesRef.length(); n3++) {
                 switch (n3) {
                      case 0:
                             if (vm.canWatchFieldAccess()) {
                                 String awName = ( (StringReference) testClass.getValue(
                                             testClass.fieldByName(awFieldName))).value();
                                 eRequests[n3] = settingAccessWatchpoint(eventThreads[n3],
                                              testClass, awName, threadNames[n3]);
                                 eRequests[n3].enable();
                                 flags |= 1;
                             }
                             break;

                      case 1:
                             if (vm.canWatchFieldModification() ) {
                                 String mwName = ( (StringReference) testClass.getValue(
                                             testClass.fieldByName(mwFieldName))).value();
                                 eRequests[n3] = settingModificationWatchpoint(eventThreads[n3],
                                              testClass, mwName, threadNames[n3]);
                                 eRequests[n3].enable();
                                 flags |= 1<<1;
                             }
                             break;

                      case 2:
                             eRequests[n3] = settingBreakpoint(eventThreads[n3], testClass,
                                               bpMethodName, bpLineName, threadNames[n3]);
                             eRequests[n3].setSuspendPolicy( EventRequest.SUSPEND_NONE);
                             eRequests[n3].enable();
                             flags |= 1<<2;
                             break;

                      case 3:
                             eRequests[n3] = settingException(eventThreads[n3], debuggeeClass, //testClass,
                                                                threadNames[n3]);
                             eRequests[n3].enable();
                             flags |= 1<<3;
                             break;

                      case 4:
                             eRequests[n3] = settingMethodEntry(eventThreads[n3], testClass,
                                                                threadNames[n3]);
                             eRequests[n3].enable();
                             flags |= 1<<4;
                             break;

                      case 5:
                             eRequests[n3] = settingMethodExit(eventThreads[n3], testClass,
                                                                threadNames[n3]);
                             eRequests[n3].enable();
                             flags |= 1<<5;
                             break;

                      case 6:
                             eRequests[n3] = settingStep(eventThreads[n3], threadNames[n3]);
                             eRequests[n3].enable();
                             flags |= 1<<6;
                             break;

                      default:
                             throw new JDITestRuntimeException("** default case while prepareing requests**");
                }
            }

            log2(":::::::::vm.resume();");
            vm.resume();

            Event   event1    = null;
            String threadName = null;
            int    flagsCopy  = flags;
            String eName      = null;

            log2("......getting and checking up on Events");
            for (int n4 = 0; n4 < namesRef.length(); n4++) {
                int flag;
                int index;
                getEventSet();
                event1 = eventIterator.nextEvent();

                if (event1 instanceof AccessWatchpointEvent) {
                    eName = "AccessWatchpointEvent";
                    index = 0;
                } else if (event1 instanceof ModificationWatchpointEvent ) {
                    eName = "ModificationWatchpointEvent";
                    index = 1;
                } else if (event1 instanceof BreakpointEvent ) {
                    eName = "BreakpointEvent";
                    index = 2;
                } else if (event1 instanceof ExceptionEvent ) {
                    eName = "ExceptionEvent";
                    index = 3;
                } else if (event1 instanceof MethodEntryEvent ) {
                    eName = "MethodEntryEvent";
                    index = 4;
                } else if (event1 instanceof MethodExitEvent ) {
                    eName = "MethodExitEvent";
                    index = 5;
                } else if (event1 instanceof StepEvent ) {
                    eName = "StepEvent";
                    index = 6;
                } else {
                    log3("ERROR: else clause in detecting type of event1");
                    testExitCode = FAILED;
                    throw new JDITestRuntimeException("** unexpected event **");
                }
                log2("--------> got: " + eName);

                ThreadReference threadRef = ((LocatableEvent) event1).thread();

                label0: {
                    for (int n5 = 0; n5 < namesRef.length(); n5++) {
                        if (threadRef.equals(eventThreads[n5])) {
                            eventThreads[n5] = null;
                            threadName = threadNames[index];
                            break label0;
                        }
                    }
                    testExitCode = FAILED;
                    log3("ERROR: event's thread is not equal to any tested");
                    log3("        thread's name == " + threadRef.name());
                }

                flag = 1 << index;
                if ((flagsCopy & flag) == 0) {
                    log3("ERROR: event duplication: " + eName);
                    testExitCode = FAILED;
                } else {
                    flagsCopy ^= flag;
                    flags |= flag;
                }
            }

            breakpointForCommunication();

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

    // ============================== test's additional methods

    private AccessWatchpointRequest settingAccessWatchpoint (
                                                  ThreadReference thread,
                                                  ReferenceType testedClass,
                                                  String fieldName,
                                                  String property)
            throws JDITestRuntimeException {

        log2("......setting up AccessWatchpoint:");
        log2("       thread: " + thread + "; class: " + testedClass +
                             "; fieldName: " + fieldName);

        AccessWatchpointRequest awRequest = null;
        try {
            Field field = testedClass.fieldByName(fieldName);
            awRequest = eventRManager.createAccessWatchpointRequest(field);
            awRequest.putProperty("number", property);
            awRequest.addThreadFilter(thread);
            awRequest.setSuspendPolicy( EventRequest.SUSPEND_NONE);
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingAccessWatchpoint() : " + e);
            log3("       AN ACCESSWATCHPOINT HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up an AccessWatchpoint **");
        }

        log2("      an AccessWatchpoint has been set up");
        return awRequest;
    }

    private ModificationWatchpointRequest settingModificationWatchpoint (
                                                  ThreadReference thread,
                                                  ReferenceType testedClass,
                                                  String fieldName,
                                                  String property)
            throws JDITestRuntimeException {

        log2("......setting up ModificationWatchpoint:");
        log2("       thread: " + thread + "; class: " + testedClass +
                             "; fieldName: " + fieldName);

        ModificationWatchpointRequest mwRequest = null;
        try {
            Field field = testedClass.fieldByName(fieldName);
            mwRequest = eventRManager.createModificationWatchpointRequest(field);
            mwRequest.putProperty("number", property);
            mwRequest.addThreadFilter(thread);
            mwRequest.setSuspendPolicy( EventRequest.SUSPEND_NONE);
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingModificationWatchpoint() : " + e);
            log3("       AN ModificationWATCHPOINT HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up an AccessWatchpoint **");
        }

        log2("      a ModificationWatchpoint has been set up");
        return mwRequest;
    }

    private MethodEntryRequest settingMethodEntry ( ThreadReference thread,
                                                    ReferenceType testedClass,
                                                    String property)
            throws JDITestRuntimeException {

        log2("......setting up MethodEntry:");
        log2("       thread: " + thread + "; class: " + testedClass +
                             "; property: " + property);

        MethodEntryRequest menRequest = null;
        try {
            menRequest = eventRManager.createMethodEntryRequest();
            menRequest.putProperty("number", property);
            menRequest.addThreadFilter(thread);
            menRequest.addClassFilter(testedClass);
            menRequest.setSuspendPolicy( EventRequest.SUSPEND_NONE);
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingMethodEntry() : " + e);
            log3("       A MethodEntry HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up a MethodEntry **");
        }

        log2("      a MethodEntry has been set up");
        return menRequest;
    }

    private MethodExitRequest settingMethodExit ( ThreadReference thread,
                                                  ReferenceType testedClass,
                                                  String property)
            throws JDITestRuntimeException {

        log2("......setting up MethodExit:");
        log2("       thread: " + thread + "; class: " + testedClass +
                             "; property: " + property);

        MethodExitRequest mexRequest = null;
        try {
            mexRequest = eventRManager.createMethodExitRequest();
            mexRequest.putProperty("number", property);
            mexRequest.addThreadFilter(thread);
            mexRequest.addClassFilter(testedClass);
            mexRequest.setSuspendPolicy( EventRequest.SUSPEND_NONE);
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingMethodExit() : " + e);
            log3("       A MethodExit HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up a MethodExit **");
        }

        log2("      a MethodExit has been set up");
        return mexRequest;
    }

    private StepRequest settingStep ( ThreadReference thread, String property)
            throws JDITestRuntimeException {

        log2("......setting up Step:");
        log2("       thread: " + thread + "; property: " + property);

        StepRequest stRequest = null;
        try {
            stRequest = eventRManager.createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_OVER);
            stRequest.putProperty("number", property);
            stRequest.addCountFilter(1);
            stRequest.setSuspendPolicy( EventRequest.SUSPEND_NONE);
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingStep() : " + e);
            log3("       A Step HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up a Step **");
        }

        log2("      a Step has been set up");
        return stRequest;
    }


    private ExceptionRequest settingException ( ThreadReference thread,
                                                ReferenceType testedClass,
                                                String property)
            throws JDITestRuntimeException {

        log2("......setting up Exception:");
        log2("       thread: " + thread + "; class: " + testedClass +
                             "; property: " + property);

        ExceptionRequest excRequest = null;
        try {
            excRequest = eventRManager.createExceptionRequest(null, true, true);
            excRequest.putProperty("number", property);
            excRequest.addThreadFilter(thread);
            excRequest.addClassFilter(testedClass);
            excRequest.setSuspendPolicy( EventRequest.SUSPEND_NONE);
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingException() : " + e);
            log3("       A Exception HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up a Exception **");
        }

        log2("      a Exception has been set up");
        return excRequest;
    }

}
