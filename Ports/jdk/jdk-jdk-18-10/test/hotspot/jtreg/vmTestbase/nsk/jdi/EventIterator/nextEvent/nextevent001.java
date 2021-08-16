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

package nsk.jdi.EventIterator.nextEvent;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * EventIterator.                                               <BR>
 *                                                              <BR>
 * The test checks that results of the method                   <BR>
 * <code>com.sun.jdi.EventIterator.nextEvent()</code>           <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * For each type of Events except for ClassUnloadEvent,                 <BR>
 * the cases to check are as follows:                                   <BR>
 * - the method returns Event object if there is an Event to return;    <BR>
 * - NoSuchElementException is thrown if no more events to return;      <BR>
 * - doesn't throw other Exceptions.                                    <BR>
 * <BR>
 * The test has three phases and works as follows.              <BR>
 * <BR>
 * In first phase,                                                      <BR>
 * upon launching debuggee's VM which will be suspended,                <BR>
 * a debugger waits for the VMStartEvent within a predefined            <BR>
 * time interval. If no the VMStartEvent received, the test is FAILED.  <BR>
 * Upon getting the VMStartEvent, it saves its EventSet into            <BR>
 * a special array and makes the request for debuggee's                 <BR>
 * ClassPrepareEvent with SUSPEND_EVENT_THREAD, resumes the VM,         <BR>
 * and waits for the event within the predefined time interval.         <BR>
 * If no the ClassPrepareEvent received, the test is FAILED.            <BR>
 * Upon getting the ClassPrepareEvent,                                  <BR>
 * the debugger saves its EventSet into the array and                   <BR>
 * sets up the breakpoint with SUSPEND_EVENT_THREAD                     <BR>
 * within debuggee's special methodForCommunication().                  <BR>
 * <BR>
 * In second phase the debugger and the debuggee perform the following. <BR>
 * - The debugger creates ThreadStartRequest and ThreadDeathRequest,    <BR>
 *   resumes the debuggee, and                                          <BR>
 *   waits for corresponding ThreadStartEvent and ThreadDeathEvent.     <BR>
 * - The debuggee creates new thread,  named "thread2",                 <BR>
 *   whose running creates the above events.                            <BR>
 * - Upon getting the events, the debugger saves their sets in the array,<BR>
 *   resumes the debuggee and waits for the BreakpointEvent.            <BR>
 * - The debuggee creates a number of threads, one for each             <BR>
 *   following event: AccessWatchpoint, ModificationWatchpoint,         <BR>
 *   MethodEntry, MethodExit, Step, Exception, and Breakpoint,          <BR>
 *   and invokes the methodForCommunication to be suspended and         <BR>
 *   to inform the debugger with the event.                             <BR>
 * - Upon getting the BreakpointForCommunication, the debugger          <BR>
 *   gets ThreadReferences mirroring all tested threads in the debuggee,<BR>
 *   sets up Requests within them to get EventSets to check up on,      <BR>
 *   resumes the debuggee, waits for events, and upon getting them,     <BR>
 *   saves its EventSets into the array.                                <BR>
 * <BR>
 * In third phase,at the end                                            <BR>
 * the debuggee changes the value of the "instruction"                  <BR>
 * to inform the debugger of checks finished, and ends.                 <BR>
 * The debugger waits for VMDeathEvent and VMDisconnectEvent, and       <BR>
 * upon getting them, saves their EventSets into the array.             <BR>
 * Finally, the debugger, using the array of EventSets,                 <BR>
 * checks up on their EventIterators.                                   <BR>
 * <BR>
 * Note. To inform each other of needed actions, the debugger and       <BR>
 *       and the debuggee use debuggee's variable "instruction".        <BR>
 * <BR>
 */

public class nextevent001 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new nextevent001().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.EventIterator.nextEvent.nextevent001a";

    private String testedClassName =
      "nsk.jdi.EventIterator.nextEvent.TestClass";

    //====================================================== test program

    //  Event #:
    //  0-6  : AccessWatchpoint, ModificationWatchpoint, Breakpoint, Exception,
    //         MethodEntry, MethodExit, Step
    //  7-8  : ClassPrepare, ClassUnload
    //  9-10 : ThreadDeath, ThreadStart
    // 11-13 : VMDeath, VMDisconnect, VMStart

    EventSet     eventSets[] = new EventSet [14];
    EventRequest eRequests[] = new EventRequest[14];

    int eventFlags[] = { 0,0,0,0, 0,0,0,0, 3,0,0,0, 1,1 };

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
//
            eventSets[13] = eventSet;
            if (eventIterator.nextEvent() instanceof VMStartEvent) {
                log2("VMStartEvent received; test begins");

                testRun();

                log2("waiting for VMDeathEvent");
                getEventSet();
                eventSets[11] = eventSet;
                if ( !(eventIterator.nextEvent() instanceof VMDeathEvent) ) {
                    log3("ERROR: last event is not the VMDeathEvent");
                    return 1;
                }

                log2("waiting for VMDisconnectEvent");
                getEventSet();
//
                eventSets[12] = eventSet;
                if ( !(eventIterator.nextEvent() instanceof VMDisconnectEvent) ) {
                    log3("ERROR: last event is not the VMDisconnectEvent");
                    return 1;
                }

                check();
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

        ClassPrepareRequest cpRequest = eventRManager.createClassPrepareRequest();
        cpRequest.setSuspendPolicy( EventRequest.SUSPEND_EVENT_THREAD);
        cpRequest.addClassFilter(debuggeeName);

        log2("......setting up ClassPrepareRequest");
        eRequests[7] = cpRequest;

        cpRequest.enable();
        vm.resume();

        getEventSet();
        eventSets[7] = eventSet;

        cpRequest.disable();

        ClassPrepareEvent event = (ClassPrepareEvent) eventIterator.next();
        debuggeeClass = event.referenceType();

        if (!debuggeeClass.name().equals(debuggeeName))
           throw new JDITestRuntimeException("** Unexpected ClassName for ClassPrepareEvent **");

        log2("      received: ClassPrepareEvent for debuggeeClass");

        log2("......setting up ClassPrepareEvent");

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

        {
            log2("......setting up ThreadStartRequest");
            ThreadStartRequest tsr = eventRManager.createThreadStartRequest();
            tsr.addCountFilter(1);
            tsr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            tsr.putProperty("number", "ThreadStartRequest");
            tsr.enable();

            eRequests[10] = tsr;

            log2("......setting up ThreadDeathRequest");
            ThreadDeathRequest tdr = eventRManager.createThreadDeathRequest();
            tdr.addCountFilter(1);
            tdr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            tsr.putProperty("number", "ThreadDeathRequest");
            tdr.enable();

            eRequests[9] = tdr;

            log2("......vm.resume();");
            vm.resume();

            log2("......waiting for ThreadStartEvent");
            getEventSet();
            eventSets[10] = eventSet;

            Event receivedEvent = eventIterator.nextEvent();
            if ( !(receivedEvent instanceof ThreadStartEvent) ) {
                testExitCode = FAILED;
                log3("ERROR: new event is not ThreadStartEvent: " + receivedEvent);
                return;
            }
            tsr.disable();

            log2("......vm.resume();");
            vm.resume();

            log2("......waiting for ThreadDeathEvent");
            getEventSet();
            eventSets[9] = eventSet;
            receivedEvent = eventIterator.nextEvent();
            if ( !(receivedEvent instanceof ThreadDeathEvent) ) {
                testExitCode = FAILED;
                log3("ERROR: new event is not ThreadDeathEvent: " + receivedEvent);
                return;
            }
            tdr.disable();
        }

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

            String bpLineName        = "breakpointLine";
            String bpMethodName      = "method";
            String awFieldName       = "awFieldName";
            String mwFieldName       = "mwFieldName";

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

            Event  event1     = null;
            int    flagsCopy  = flags;
            String eName      = null;
            int    index      = 0;

            log2("......getting and checking up on Events");
            for (int n4 = 0; n4 < namesRef.length(); n4++) {
                int flag;

                getEventSet();
                event1 = eventIterator.nextEvent();

                if (event1 instanceof AccessWatchpointEvent) {
                    index = 0;
                } else if (event1 instanceof ModificationWatchpointEvent ) {
                    index = 1;
                } else if (event1 instanceof BreakpointEvent ) {
                    index = 2;
                } else if (event1 instanceof ExceptionEvent ) {
                    index = 3;
                } else if (event1 instanceof MethodEntryEvent ) {
                    index = 4;
                } else if (event1 instanceof MethodExitEvent ) {
                    index = 5;
                } else if (event1 instanceof StepEvent ) {
                    index = 6;
                } else {
                    log3("ERROR: else clause in detecting type of event1");
                    testExitCode = FAILED;
                }

                flag = 1 << index;
                if ((flagsCopy & flag) == 0) {
                    log3("ERROR: event duplication: " + eName);
                    testExitCode = FAILED;
                } else {
                    flagsCopy ^= flag;
                    flags |= flag;
                }

                eventSets[index] = eventSet;
            }

            if (testExitCode == FAILED)
                break;

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

    private void checkingEventIterator(EventIterator eIterator) {

        log2("......checking up on eIterator.nextEvent()");

        try {
            log2("......first time: Event testedEvent = eIterator.nextEvent();");
            log2("        no Exception to be thrown is expected");
            Event testedEvent = eIterator.nextEvent();
        } catch (Exception e) {
                log3("ERROR: Exception while: testedEvent = eIterator.nextEvent(); :: " + e);
                testExitCode = FAILED;
        }
        try {
            log2("......second time: Event testedEvent = eIterator.nextEvent();");
            log2("        NoSuchElementException is expected");
            Event testedEvent = eIterator.nextEvent();
            log3("ERROR: no NoSuchElementException while: testedEvent = eIterator.nextEvent();");
            testExitCode = FAILED;
        } catch (NoSuchElementException e) {
            log2("        NoSuchElementException");
        } catch (Exception e) {
            log3("ERROR: unexpected Exception :: " + e);
            testExitCode = FAILED;
        }
    }

    private void check() {

        log2("......performing the check;");
        for (int k = 0; k < eventFlags.length; k++) {

            log2("......new check case ::  k == " + k);
            switch (eventFlags[k]) {

            case 0:
            case 1:
            case 2:
                   checkingEventIterator(eventSets[k].eventIterator());
                   break;

            case 3:
                   break;

            default:
                    log3("ERROR: unexpected default case");
                    testExitCode = FAILED;
                    throw new JDITestRuntimeException("** FAILURE within check() **");
            }
        }
    }

}
