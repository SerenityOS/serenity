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
 * The test checks that uncaught exception event is properly reported
 * by the debugger. It calls
 * <code>com.sun.jdi.request.EventRequestManager.createExceptionRequest()</code>
 * method with true value of boolean parameter <code>notifyUncaught</code>.
 * The debuggee part of the test raises own uncaught exevent002tException.
 */

public class exevent002 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final int TIMEOUT_DELTA = 1000; //milliseconds

    static final String DEBUGGEE_CLASS = "nsk.jdi.ExceptionEvent._itself_.exevent002t";
    static final String DEBUGGEE_EXCEPTION = DEBUGGEE_CLASS + "Exception";
    static final String COMMAND_READY = "ready";
    static final String COMMAND_RUN = "run";
    static final String COMMAND_QUIT = "quit";

    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private EventListener elThread;
    private VirtualMachine vm;
    private static EventRequestManager erManager;
    private ExceptionRequest eRequest;
    private static ReferenceType rType, rTypeEx;
    private int counter = 0;
    private String cmd;
    private volatile int totalRes = PASSED;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE); // JCK-style exit status.
    }

    public static int run(String argv[], PrintStream out) {
        return new exevent002().runIt(argv, out);
    }

    private int runIt(String argv[], PrintStream out) {

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "exevent002t.err> ");
        vm = debuggee.VM();
        erManager = vm.eventRequestManager();

        log.display("Resuming debuggee");
        debuggee.resume();
        cmd = pipe.readln();

        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + cmd);
            return quitDebuggee(FAILED);
        }
        if ((rType = debuggee.classByName(DEBUGGEE_CLASS)) == null) {
            log.complain("TEST FAILURE: Method Debugee.classByName() returned null for "
                + DEBUGGEE_CLASS);
            return quitDebuggee(FAILED);
        }
        if ((rTypeEx = debuggee.classByName(DEBUGGEE_EXCEPTION)) == null) {
            log.complain("TEST FAILURE: Method Debugee.classByName() returned null for "
                + DEBUGGEE_EXCEPTION);
            return quitDebuggee(FAILED);
        }

/* Create a new ExceptionRequest with true notifyUncaught parameter
   so uncaught exceptions should be reported. */
        log.display("Creating ExceptionEvent request");
        if ((eRequest = createExReq(false, true)) == null) {
            return quitDebuggee(FAILED);
        }

        log.display("Starting events listener");
        EventListener elThread = new EventListener();
        elThread.start();

        log.display("Forcing debuggee to generate uncaught exception");
        counter = 0;
        pipe.println(COMMAND_RUN);

        log.display("Waiting for debuggee exits due to uncaught exception");
        debuggee.waitFor();

        log.display("Waiting for all events recieved");
        try {
            if (elThread.isAlive()) elThread.join();
        } catch (InterruptedException e) {
            log.complain("TEST INCOMPLETE: caught InterruptedException " + e);
            return quitDebuggee(FAILED);
        }

        if (counter == 0) {
            log.complain("TEST FAILED: uncaught exception " + rTypeEx.name() +
                     " was not reported by the debugger");
            return FAILED;
        }
        return PASSED;
    }

    private ExceptionRequest createExReq(boolean notifyC, boolean notifyUnc) {
        ExceptionRequest eRequest;
        try {
            eRequest = erManager.createExceptionRequest(rTypeEx, notifyC, notifyUnc);
        } catch (Exception e) {
            log.complain("createExceptionRequest: " + e);
            return null;
        }
        eRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        eRequest.enable();
        return eRequest;
    }

    private int quitDebuggee(int stat) {
        if (eRequest != null) {
            eRequest.disable();
        }

        pipe.println(COMMAND_QUIT);

        if (elThread != null) {
            elThread.isConnected = false;
            try {
                if (elThread.isAlive())
                    elThread.join();
            } catch (InterruptedException e) {
                log.complain("TEST INCOMPLETE: caught InterruptedException "
                    + e);
                stat = FAILED;
            }
        }

        int debStat = debuggee.endDebugee();
        if (debStat != (JCK_STATUS_BASE + PASSED)) {
            log.complain("TEST FAILED: debuggee's process finished with status: "
                + debStat);
            stat = FAILED;
        } else
            log.display("Debuggee's process finished with status: "
                + debStat);

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
                                    if (exEvent.exception().referenceType().equals(rTypeEx)) {
                                        log.display("Exception event equals to expected \n\t" +
                                                exEvent.exception().referenceType().name());
                                        counter++;
                                        log.display("\t" + "counter = " + counter);
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
