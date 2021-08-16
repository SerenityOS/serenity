/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ExceptionEvent._itself_;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.VMDisconnectedException;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * The test checks that caught exception events are properly reported
 * or not reported by the debugger. It calls
 * <code>com.sun.jdi.request.EventRequestManager.createExceptionRequest()</code>
 * method with two combinations of boolean parameter <code>notifyCaught</code>.
 * The debuggee part of the test raises three caught exceptions:
 * <li>own exevent001tException
 * <li>IllegalMonitorStateException
 * <li>NumberFormatException in another class
 */
public class exevent001 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final int TIMEOUT_DELTA = 1000; //milliseconds

    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ExceptionEvent._itself_.exevent001t";
    static final String DEBUGGEE_EXCEPTION1 = DEBUGGEE_CLASS + "Exception";
    static final String DEBUGGEE_EXCEPTION2 = "java.lang.IllegalMonitorStateException";
    static final String DEBUGGEE_EXCEPTION3 = "java.lang.NumberFormatException";

    static final String COMMAND_READY = "ready";
    static final String COMMAND_TEST1 = "test1";
    static final String COMMAND_TEST2 = "test2";
    static final String COMMAND_TEST3 = "test3";
    static final String COMMAND_QUIT = "quit";

    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private EventListener elThread;
    private EventRequestManager erManager;
    private VirtualMachine vm;
    private ReferenceType rType;
    private volatile ExceptionRequest eRequest;
    private volatile int t_case = 0; // testing debuggee's exception
    private volatile int exc1Count = 0;
    private volatile int exc2Count = 0;
    private volatile int exc3Count = 0;
    private volatile int totalRes = PASSED;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE); // JCK-style exit status.
    }

    public static int run(String argv[], PrintStream out) {
        return new exevent001().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "exevent001t.err> ");
        vm = debuggee.VM();
        erManager = vm.eventRequestManager();

        log.display("Resuming debuggee");
        debuggee.resume();
        String token = pipe.readln();

        if (!token.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + token);
            return quitDebuggee(FAILED);
        }

        if ((rType = debuggee.classByName(DEBUGGEE_CLASS)) == null) {
            log.complain("TEST BUG: cannot find class "
                + DEBUGGEE_CLASS);
            return quitDebuggee(FAILED);
        }

        log.display("Starting events listener");
        EventListener elThread = new EventListener();
        elThread.start();

/* Create a new ExceptionRequest with false notifyCaught parameter
   so caught exceptions should not be reported. */
        log.display("\nCreating ExceptionEvent request that not reports caught events");
        if ((eRequest = createExReq(false, false)) == null)
            return FAILED;

        if (runTestCase(1, COMMAND_TEST1) == FAILED)
            return quitDebuggee(FAILED);

        if (runTestCase(2, COMMAND_TEST2) == FAILED)
            return quitDebuggee(FAILED);

        if (runTestCase(3, COMMAND_TEST3) == FAILED)
            return quitDebuggee(FAILED);

        eRequest.disable();

// check results for caught exceptions
        if (exc1Count != 0) {
            log.complain("TEST FAILED: caught exception "
               + DEBUGGEE_EXCEPTION1 +
               " was reported by the debugger");
            totalRes = FAILED;
        }
        exc1Count = 0;
        if (exc2Count != 0) {
            log.complain("TEST FAILED: caught exception "
               + DEBUGGEE_EXCEPTION2 +
               " was reported by the debugger");
            totalRes = FAILED;
        }
        exc2Count = 0;
        if (exc3Count != 0) {
            log.complain("TEST FAILED: caught exception "
               + DEBUGGEE_EXCEPTION3 +
               " was reported by the debugger");
            totalRes = FAILED;
        }
        exc3Count = 0;

/* Create a new ExceptionRequest with true notifyCaught parameter
   so caught exceptions should be reported. */
        log.display("\nCreating ExceptionEvent request that reports caught events");
        if ((eRequest = createExReq(true, false)) == null)
            return FAILED;

        if (runTestCase(1, COMMAND_TEST1) == FAILED)
            return quitDebuggee(FAILED);

        if (runTestCase(2, COMMAND_TEST2) == FAILED)
            return quitDebuggee(FAILED);

        if (runTestCase(3, COMMAND_TEST3) == FAILED)
            return quitDebuggee(FAILED);

        eRequest.disable();

// check results for caught exceptions
        if (exc1Count == 0) {
            log.complain("TEST FAILED: caught exception "
               + DEBUGGEE_EXCEPTION1 +
               " was not reported by the debugger");
            totalRes = FAILED;
        }
        if (exc2Count == 0) {
            log.complain("TEST FAILED: caught exception "
               + DEBUGGEE_EXCEPTION2 +
               " was not reported by the debugger");
            totalRes = FAILED;
        }
        if (exc3Count == 0) {
            log.complain("TEST FAILED: caught exception "
               + DEBUGGEE_EXCEPTION3 +
               " was not reported by the debugger");
            totalRes = FAILED;
        }

        return quitDebuggee(totalRes);
    }

    private int runTestCase(int tNum, String command) {
        t_case = tNum;
        pipe.println(command);
        String token = pipe.readln();
        if (token == null || !token.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + token);
            return FAILED;
        }
        return PASSED;
    }

    private ExceptionRequest createExReq(boolean notifyC, boolean notifyUnc) {
        ExceptionRequest eRequest;
        try {
            eRequest = erManager.createExceptionRequest(null, notifyC, notifyUnc);
        } catch (Exception e) {
            log.complain("createExceptionRequest: " + e);
            return null;
        }
        eRequest.enable();
        return eRequest;
    }

    private int quitDebuggee(int stat) {
        if (eRequest != null) {
            eRequest.disable();
        }

        if (elThread != null) {
            elThread.isConnected = false;
            try {
                if (elThread.isAlive())
                    elThread.join();
            } catch (InterruptedException e) {
                log.complain("TEST INCOMPLETE: caught " + e);
                stat = FAILED;
            }
        }

        pipe.println(COMMAND_QUIT);

        int debStat = debuggee.endDebugee();
        if (debStat != (JCK_STATUS_BASE + PASSED)) {
            log.complain("TEST FAILED: debuggee's process finished with status: " + debStat);
            stat = FAILED;
        } else
            log.display("Debuggee's process finished with status: " + debStat);

        return stat;
    }

    class EventListener extends Thread {
        public volatile boolean isConnected = true;

        public void run() {
            try {
                do {
                    EventSet eventSet = vm.eventQueue().remove(TIMEOUT_DELTA);
                    if (eventSet != null) { // there is not a timeout
                        EventIterator it = eventSet.eventIterator();
                        while (it.hasNext()) {
                            Event event = it.nextEvent();
                            if (event instanceof VMDeathEvent) {
                                isConnected = false;
                            } else if (event instanceof VMDisconnectEvent) {
                                isConnected = false;
                            } else if (event instanceof ExceptionEvent) {
                                ExceptionEvent exEvent = (ExceptionEvent) event;
                                log.display("EventListener: following ExceptionEvent occured: \n\t"
                                    + exEvent);

                                if (eRequest.equals(event.request())) {
                                    switch (t_case) {
                                        case 1:
                                            if (exEvent.exception().referenceType().name().equals(
                                                    DEBUGGEE_EXCEPTION1)) {
                                                log.display(">> Exception event equals to expected "
                                                    + exEvent.exception().referenceType().name());
                                                exc1Count += 1;
                                            }
                                            break;
                                        case 2:
                                            if (exEvent.exception().referenceType().name().equals(
                                                    DEBUGGEE_EXCEPTION2)) {
                                                log.display(">> Exception event equals to expected "
                                                    + exEvent.exception().referenceType().name());
                                                exc2Count += 1;
                                            }
                                            break;
                                        case 3:
                                            if (exEvent.exception().referenceType().name().equals(
                                                    DEBUGGEE_EXCEPTION3)) {
                                                log.display(">> Exception event equals to expected "
                                                    + exEvent.exception().referenceType().name());
                                                exc3Count += 1;
                                            }
                                            break;
                                    }
                                }
                            } else {
                                log.display("EventListener: following JDI event occured: \n\t"
                                    + event);
                            }
                        }
                        if (isConnected) {
                            eventSet.resume();
                        }
                    }
                } while (isConnected);
            } catch (InterruptedException e) {
                totalRes = FAILED;
                log.complain("FAILURE in EventListener: caught unexpected "
                    + e);
            } catch (VMDisconnectedException e) {
                totalRes = FAILED;
                log.complain("FAILURE in EventListener: caught unexpected "
                    + e);
                e.printStackTrace();
            }
            log.display("EventListener: exiting");
        }
    }
}
