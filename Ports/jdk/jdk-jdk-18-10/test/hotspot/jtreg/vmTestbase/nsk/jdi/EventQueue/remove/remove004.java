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

package nsk.jdi.EventQueue.remove;

import jdk.test.lib.Utils;
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
 * EventQueue.                                                  <BR>
 *                                                              <BR>
 * The test checks up that results of the method                <BR>
 * <code>com.sun.jdi.EventQueue.remove()</code>                 <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * Since method's assertion is                                  <BR>
 *     "Waits forever for the next available event."            <BR>
 * in the condition of a limited time for testing,              <BR>
 * the test case limits time for waiting for the next event     <BR>
 * to the standard value of WAITTIME set up in the testbase_nsk.<BR>
 * <BR>
 * The test works as follows.                                   <BR>
 * Upon launching debuggee's VM which will be suspended,        <BR>
 * a debugger waits for the VMStartEvent within a predefined    <BR>
 * time interval. If no the VMStartEvent received, the test is FAILED.<BR>
 * Upon getting the VMStartEvent, it makes the request          <BR>
 * for debuggee's ClassPrepareEvent with SUSPEND_EVENT_THREAD,  <BR>
 * resumes the VM, and waits for the event within the predefined<BR>
 * time interval. If no the ClassPrepareEvent received, the test is FAILED.<BR>
 * Upon getting the ClassPrepareEvent,                          <BR>
 * the debugger sets up the breakpoint with SUSPEND_EVENT_THREAD,<BR>
 * the debugger resumes the debuggee and waits for the BreakpointEvent.<BR>
 * The debuggee prepares new check and invokes the methodForCommunication<BR>
 * to be suspended and to inform the debugger with the event.           <BR>
 * Upon getting the BreakpointEvent, the debugger performs the check.   <BR>
 * At the end, the debuggee changes the value of the "instruction"      <BR>
 * to inform the debugger of checks finished, and both end.             <BR>
 * <BR>
 * The check includes two steps.                                        <BR>
 * In first one, second thread waits for any incoming event from the    <BR>
 * debugger which is sleeping for some time; hence,                     <BR>
 * no events are expected to be received at the debugger end.           <BR>
 * In second, second thread is interrupted, and the debugger waits for  <BR>
 * a breakpoint event after the debuggee finishes sleeping.             <BR>
 */

public class remove004 extends JDIBase {

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new remove004().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.EventQueue.remove.remove004a";

    //====================================================== test program

    static Value trueValue;

    private int runThis (String argv[], PrintStream out) {

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        waitTime        = Utils.adjustTimeout(argsHandler.getWaitTime() * 1000);

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

        trueValue = debuggeeClass.getValue(debuggeeClass.fieldByName("BOOLEAN_TRUE_VALUE"));

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
        vm.resume();

        for (int i = 0; ; i++) {

            breakpointForCommunication();

            int instruction = ((IntegerValue)
                               (debuggeeClass.getValue(debuggeeClass.fieldByName("instruction")))).value();

            if (instruction == 0) {
                vm.resume();
                break;
            }

            log1("  new check: # " + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            vm.resume();

            Threadremove004 thread2 = new Threadremove004("thread2");
            log2("       thread2 is created");

            synchronized (lockingObject) {
                synchronized (waitnotifyObj) {
                    log2("       before: thread2.start()");
                    thread2.start();

                    try {
                        log2("       before:   waitnotifyObj.wait();");
                        waitnotifyObj.wait();
                    } catch ( Exception e) {
                        log3("ERROR:  Exception : " + e );
                        testExitCode = FAILED;
                        throw new JDITestRuntimeException("** Exception while waiting: notify() **");
                    }
                }
            }
            log2("mainThread is out of: synchronized (lockingObject)");

            Object waitObj = new Object();

            synchronized (waitObj) {
                waitObj.wait(waitTime);
                if (!thread2.isAlive()) {
                    log3("ERROR:  thread2 is not alive");
                    testExitCode = FAILED;
                    break;
                }

                thread2.interrupt();

                for (int i2 = 0; i2 < waitTime; ) {
                    waitObj.wait(1000);
                    if (!thread2.isAlive()) {
                        break;
                    }
                    i2 += 1000;
                }
                if (thread2.isAlive()) {
                    log3("ERROR: thread2 is still alive");
                    testExitCode = FAILED;
                    break;
                }
            }

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

    // ============================== test's additional methods


    public static Object waitnotifyObj = new Object();
    public static Object lockingObject = new Object();

    class Threadremove004 extends Thread {

        public Threadremove004(String threadName) {
            super(threadName);
        }

        public void run() {
            log2("-----t2: method 'run' enter");
            synchronized (waitnotifyObj)   {
                log2("-----t2: entered into block:  synchronized (waitnotifyObj)");
                waitnotifyObj.notify();
            }
            log2("-----t2: exited from block:  synchronized (waitnotifyObj)");
            synchronized (lockingObject)  {
                log2("-----t2: entered into block:  synchronized (lockingObject)");
            }
            log2("-----t2: exited from block:  synchronized (lockingObject)");

            try {
                log2("-----t2:  eventSet = eventQueue.remove(); expects: InterruptedException");
                eventSet = eventQueue.remove();
                throw new JDITestRuntimeException("** return from eventQueue.remove(); **");
            } catch (InterruptedException e1) {
                log2("-----t2:            InterruptedException");
                // Signal to debuggee to stop sleeping
                try {
                    ((ClassType) debuggeeClass).setValue(debuggeeClass.fieldByName("stopSleeping"),
                            trueValue);
                } catch (InvalidTypeException | ClassNotLoadedException e) {
                    log3("ERROR: -----t2: Exception : " + e);
                    testExitCode = FAILED;
                }
            } catch (Exception e) {
                log3("ERROR: -----t2: Exception : " + e);
                testExitCode = FAILED;
            }

            log2("-----t2: method 'run' exit");
            return;
        }
    }
}
