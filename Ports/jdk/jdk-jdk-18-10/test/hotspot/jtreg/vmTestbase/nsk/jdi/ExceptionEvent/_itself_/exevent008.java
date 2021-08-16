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
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.VMDisconnectedException;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * The test checks that both caught and uncaught exception events
 * are properly reported by the debugger. It calls
 * <code>com.sun.jdi.request.EventRequestManager.createExceptionRequest()</code>
 * method with true values of boolean parameters <code>notifyCaught</code>
 * and <code>notifyUncaught</code>.
 * The debugee part of the test raises two exceptions:
 * <li>own caught exevent008tException
 * <li>uncaught NumberFormatException in another class
 */

public class exevent008 {
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int JCK_STATUS_BASE = 95;

    static final int TIMEOUT_DELTA = 1000; //milliseconds

    static final String DEBUGGEE_CLASS = "nsk.jdi.ExceptionEvent._itself_.exevent008t";
    static final String DEBUGGEE_EXCEPTION1 = DEBUGGEE_CLASS + "Exception";
//    static final String DEBUGGEE_EXCEPTION2 = "java.lang.NumberFormatException";
    static final String DEBUGGEE_EXCEPTION2 = "java.lang.reflect.InvocationTargetException";

    static final String COMMAND_READY = "ready";
    static final String COMMAND_TEST1 = "test1";
    static final String COMMAND_TEST2 = "test2";
    static final String COMMAND_QUIT = "quit";

    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private EventListener elThread;
    private VirtualMachine vm;
    private EventRequestManager erManager;
    private ExceptionRequest eRequest;
    private ReferenceType rType, rTypeEx;
    private String cmd;
    private int counter1 = 0, counter2 = 0;
    private volatile int tot_res = PASSED;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE); // JCK-style exit status.
    }

    public static int run(String argv[], PrintStream out) {
        return new exevent008().runIt(argv, out);
    }

    private int runIt(String argv[], PrintStream out) {

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "crexreq006t.err> ");
        vm = debuggee.VM();
        erManager = vm.eventRequestManager();

        log.display("Resuming debuggee");
        debuggee.resume();
        cmd = pipe.readln();

        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + cmd);
            tot_res = FAILED;
            return quitDebuggee();
        }
        if ((rType = debuggee.classByName(DEBUGGEE_CLASS)) == null) {
            log.complain("TEST FAILURE: Method Debugee.classByName() returned null for "
                + DEBUGGEE_CLASS);
            tot_res = FAILED;
            return quitDebuggee();
        }

/* Create a new ExceptionRequest with true notifyCaught and notifyUncaught
   parameters so all exceptions should be reported */
        log.display("Creating ExceptionEvent request");
        if ((eRequest = createExReq(true, true)) == null) {
            tot_res = FAILED;
            return quitDebuggee();
        }

        log.display("Starting events listener");
        EventListener elThread = new EventListener();
        elThread.start();

        log.display("Forcing debuggee to generate caught exception");
        counter1 = 0;
        pipe.println(COMMAND_TEST1);

        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + cmd);
            tot_res = FAILED;
            return quitDebuggee();
        }
        log.display("Exception caught");

        log.display("Forcing debuggee to generate uncaught exception");
        counter1 = 0;
        pipe.println(COMMAND_TEST2);

        log.display("Waiting for debuggee exits due to uncaught exception");
        debuggee.waitFor();
        log.display("Debuggee exited");

        log.display("Waiting for all events recieved");
        try {
            if (elThread.isAlive()) elThread.join();
        } catch (InterruptedException e) {
            log.complain("TEST INCOMPLETE: caught InterruptedException " + e);
            tot_res = FAILED;
            return quitDebuggee();
        }

        if (counter1 == 0) {
            log.complain("TEST FAILED: caught exception " + DEBUGGEE_EXCEPTION1 +
                     " was not reported by the debugger");
            tot_res = FAILED;
        }
        if (counter2 == 0) {
            log.complain("TEST FAILED: uncaught exception " + DEBUGGEE_EXCEPTION2 +
                     " was not reported by the debugger");
            tot_res = FAILED;
        }

        return tot_res;
    }

    private ExceptionRequest createExReq(boolean notifyC, boolean notifyUnc) {
        ExceptionRequest eRequest;
        try {
            eRequest = erManager.createExceptionRequest(null, notifyC, notifyUnc);
        } catch (Exception e) {
            log.complain("createExceptionRequest: " + e);
            return null;
        }
        eRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        eRequest.enable();
        return eRequest;
    }

    private int quitDebuggee() {
        if (eRequest != null) {
            eRequest.disable();
        }

        if (elThread != null) {
            elThread.isConnected = false;
            try {
                if (elThread.isAlive())
                    elThread.join();
            } catch (InterruptedException e) {
                log.complain("TEST INCOMPLETE: caught InterruptedException "
                    + e);
                tot_res = FAILED;
            }
        }

        pipe.println(COMMAND_QUIT);

        int debStat = debuggee.endDebugee();
        if (debStat != (JCK_STATUS_BASE + PASSED)) {
            log.complain("TEST FAILED: debuggee's process finished with status: "
                + debStat);
            tot_res = FAILED;
        } else
            log.display("Debuggee's process finished with status: "
                + debStat);

        return tot_res;
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
                                    + exEvent.toString());

                                if (eRequest.equals(event.request())) {
                                    if (exEvent.exception().referenceType().name().equals(DEBUGGEE_EXCEPTION1)) {
                                        log.display("CException event equals to expected for caught exception\n\t" +
                                                exEvent.exception().referenceType().name());
                                        counter1++;
                                        log.display("\t" + "counter1 = " + counter1);
                                    } else if (exEvent.exception().referenceType().name().equals(DEBUGGEE_EXCEPTION2)) {
                                        log.display("Exception event equals to expected for uncaught exception\n\t" +
                                                exEvent.exception().referenceType().name());
                                        counter2++;
                                        log.display("\t" + "counter2 = " + counter2);
                                    }
                                }
                            } else {
                                log.display("EventListener: following JDI event occured: \n\t"
                                    + event.toString());
                            }
                        }
                        if (isConnected) {
                            eventSet.resume();
                        }
                    }
                } while (isConnected);
            } catch (InterruptedException e) {
                tot_res = FAILED;
                log.complain("FAILURE in EventListener: caught unexpected "
                    + e);
            } catch (VMDisconnectedException e) {
                tot_res = FAILED;
                log.complain("FAILURE in EventListener: caught unexpected "
                    + e);
                e.printStackTrace();
            }
            log.display("EventListener: exiting");
        }
    }
}
