/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jdi;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.io.*;

import nsk.share.*;

/**
 * This class is a template for test debugger.
 * It bases on some existed tests for testing of
 * JDI requests and events.
 *
 * Requirements for a test debugger which extends
 * this template are as folllows:
 *    - the subclass must have 'main' and 'run' methods
 *      defined like:
 *           public static void main (String argv[]) {
 *               System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
 *           }
 *
 *           public static int run (String argv[], PrintStream out) {
 *               debuggeeName = <DEBUGGEE_NAME_AS_STRING>;
 *               return new filter001().runThis(argv, out);
 *           }
 *
 *    - the subclass must implement 'testRun' method
 *      which must contain checkings of all test's assertions.
 */
public abstract class TestDebuggerType1 {

    protected static int waitTime;
    protected static int testExitCode = Consts.TEST_PASSED;

    protected static Binder              binder;
    protected static Debugee             debuggee;
    protected static VirtualMachine      vm;
    protected static ArgumentHandler     argsHandler;
    protected static Log                 log;
    protected static String              debuggeeName; /* must be assigned in a subclass */
    protected static ReferenceType       debuggeeClass;

    protected static EventRequestManager eventRManager;
    protected static EventHandler        eventHandler;
    protected static BreakpointRequest   bpRequest  /* communication breakpoint */;
    protected static volatile int        bpCount;

    protected static void display(String message) {
        log.display("debugger> " + message);
    }

    protected static void complain(String message) {
        log.complain("debugger> " + message);
    }

    /**
     * Should be used in test debugger when assertion failed.
     */
    protected static void setFailedStatus(String message) {
        complain(message);
        testExitCode = Consts.TEST_FAILED;
    }

    protected int runThis (String argv[], PrintStream out) {

        argsHandler  = new ArgumentHandler(argv);
        log          = new Log(out, argsHandler);
        binder       = new Binder(argsHandler, log);
        waitTime     = argsHandler.getWaitTime() * 60000;

        try {
            debuggee = binder.bindToDebugee(debuggeeName);
            debuggee.redirectStdout(log, Debugee.DEBUGEE_STDERR_LOG_PREFIX);
            vm = debuggee.VM();
            eventRManager = vm.eventRequestManager();

            eventHandler = new EventHandler(debuggee, log);
            eventHandler.startListening();

            debuggeeClass = waitForClassPrepared(debuggeeName);

            /* A debuggee class must define 'methodForCommunication'
             * method and invoke it in points of synchronization
             * with a debugger.
             */
            setCommunicationBreakpoint(debuggeeClass,"methodForCommunication");

            display("TESTING BEGINS");

            // after waitForClassPrepared() main debuggee thread is suspended, resume it before test start
            display("RESUME DEBUGGEE VM");
            vm.resume();

            testRun();

            display("TESTING ENDS");

            display("Waiting for debuggee's exit...");
            eventHandler.waitForVMDisconnect();

            int status = EventHandler.getStatus();
            if (status != 0) {
                setFailedStatus("Event handler returned unexpected exit status: " +  status);
            } else {
                display("Event handler thread exited.");
            }

            debuggee.endDebugee();
            status = debuggee.getStatus();
            if (status != (Consts.TEST_PASSED + Consts.JCK_STATUS_BASE)) {
                setFailedStatus("Debuggee returned unexpected exit status: " +  status);
            } else {
                display("Debuggee PASSED.");
            }

        } catch (VMDisconnectedException e) {
            setFailedStatus("Unexpected VMDisconnectedException");
            e.printStackTrace(log.getOutStream());
            throw new Failure(e.getMessage());

        } catch (Exception e) {
            setFailedStatus("Unexpected exception : " + e.getMessage());
            e.printStackTrace(log.getOutStream());
            display("Forcing debuggee to exit...");
            vm.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        }

        return testExitCode;
    }

    /**
     * This method which be implemented in all test debuggers
     * and must contain checking of test assertions.
     */
    protected abstract void testRun();

    /**
     * Waits until debuggee class is prepared.
     *
     */
    private ReferenceType waitForClassPrepared(String className) {
        ClassPrepareRequest cpRequest = eventRManager.createClassPrepareRequest();
        cpRequest.setSuspendPolicy( EventRequest.SUSPEND_EVENT_THREAD);
        cpRequest.addClassFilter(className);

        ClassPrepareEvent event = (ClassPrepareEvent)eventHandler.waitForRequestedEvent(
                                       new EventRequest[]{cpRequest}, waitTime, true);
        ReferenceType refType = event.referenceType();
        if (!refType.name().equals(className))
            throw new Failure("Unexpected class name for received ClassPrepareEvent: " + refType.name() +
                              "\n\texpected name: " + debuggeeName);
        display("Received ClassPrepareEvent for debuggee class: " + refType.name());
        return refType;
    };

    /**
     * Sets up breakpoint request which serves for synchronization
     * between debugger and debuggee.
     *
     */
    private void setCommunicationBreakpoint(ReferenceType refType, String methodName) {
        Method method = debuggee.methodByName(refType, methodName);
        Location location = null;
        try {
            location = method.allLineLocations().get(0);
        } catch (AbsentInformationException e) {
            throw new Failure(e);
        }
        bpRequest = debuggee.makeBreakpoint(location);

        bpRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        bpRequest.putProperty("number", "zero");
        bpRequest.enable();

        eventHandler.addListener(
             new EventHandler.EventListener() {
                 public boolean eventReceived(Event event) {
                    if (event instanceof BreakpointEvent && bpRequest.equals(event.request())) {
                        synchronized(eventHandler) {
                            display("Received communication breakpoint event.");
                            bpCount++;
                            eventHandler.notifyAll();
                        }
                        return true;
                    }
                    return false;
                 }
             }
        );
    }

    /**
     * Waits for synchronization breakpoint event and checks
     * up if there are more test case to check.
     *
     * Note: debuggee VM shouldn't be suspended when this method
     * is called
     *
     * @return true if there are more test case to check,
     *          false otherwise or debuggee is disconnected.
     */
    protected boolean shouldRunAfterBreakpoint() {
        display("shouldRunAfterBreakpoint: entered");
        boolean shouldRun = true;

        long timeToFinish = System.currentTimeMillis() + waitTime;
        long timeLeft = waitTime;
        synchronized(eventHandler) {
            while (!EventHandler.isDisconnected() && bpCount <= 0 && timeLeft > 0) {
                display("shouldRunAfterBreakpoint: waiting for breakpoint event during 1 sec.");
                try {
                    eventHandler.wait(1000);
                } catch (InterruptedException e) {
                    throw new Failure(e);
                }
                timeLeft = timeToFinish - System.currentTimeMillis();
            }
        }
        if (timeLeft <= 0 && bpCount <= 0) {
            setFailedStatus("shouldRunAfterBreakpoint: had not received breakpoint event during waitTime.");
            shouldRun = false;

        } else if (bpCount > 0) {
            display("shouldRunAfterBreakpoint: received breakpoint event.");
            bpCount--;
        }

        if (!EventHandler.isDisconnected()) {
            try {
                int instruction = ((IntegerValue)
                                   (debuggeeClass.getValue(debuggeeClass.fieldByName("instruction")))).value();

                if (instruction == 0) {
                    display("shouldRunAfterBreakpoint: received instruction from debuggee to finish.");
                    shouldRun = false;
                }
            } catch (VMDisconnectedException e) {
                shouldRun = false;
            }
        } else {
            shouldRun = false;
        }

        if (shouldRun) {
            display("shouldRunAfterBreakpoint: exited with true.");
        } else {
            display("shouldRunAfterBreakpoint: exited with false.");
        }
        return shouldRun;
    }
}
