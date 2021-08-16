/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.AccessWatchpointEvent._itself_;

import com.sun.jdi.Field;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.AccessWatchpointRequest;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VMMismatchException;
import com.sun.jdi.event.*;

import java.util.Iterator;
import java.util.List;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method
 * <b>com.sun.jdi.request.AccessWatchpointEvent.createAccessWatchpointRequest()</b>
 * properly creates new disabled watchpoints which watch accesses to
 * the fields of different types.
 * A debugger part of the test checks that:
 * <li>all created watchpoint requests are disabled
 * <li>after the watchpoint requests activation, all events corresponding
 * to them, are properly generated.
 */
public class awevent001 {
    public static final int PASSED = 0;
    public static final int FAILED = 2;
    public static final int JCK_STATUS_BASE = 95;
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.AccessWatchpointEvent._itself_.awevent001t";
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";
    static final String COMMAND_BREAKPOINT = "breakpoint";
    static final String COMMAND_RUN1 = "run1";
    static final int FLDS_NUM = 9; // number of debuggee's fields
    static final String COMMAND_RUN[] = {
        "run1", "run2", "run3", "run4",
        "run5", "run6", "run7", "run8", "run9"
    };
    static final String DEBUGGEE_FLDS[][] = {
        {"byte", "byteFld"},
        {"short", "shortFld"},
        {"int", "intFld"},
        {"long", "longFld"},
        {"float", "floatFld"},
        {"double", "doubleFld"},
        {"char", "charFld"},
        {"boolean", "booleanFld"},
        {"java.lang.String", "strFld"}
    };
    private volatile int accFCount[] = {
        0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private ArgumentHandler argHandler;
    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private VirtualMachine vm;
    private EventListener elThread;
    private volatile AccessWatchpointRequest awpRequest[];
    private volatile int tot_res = PASSED;
    private volatile boolean breakPointReceived = false;

// for notification a main thread about received events
    private Object gotEvent = new Object();

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new awevent001().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        ReferenceType rType;
        List fields;
        String cmd;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "awevent001t.err> ");
        vm = debuggee.VM();
        EventRequestManager erManager = vm.eventRequestManager();
        debuggee.resume();
        cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: "
                + cmd);
            tot_res = FAILED;
            return quitDebuggee();
        }

        if ( !vm.canWatchFieldAccess() ) {
            log.display("  TEST CANCELLED due to:  vm.canWatchFieldAccess() == false");
            return quitDebuggee();
        }

// Create testing requests
        if ((rType = debuggee.classByName(DEBUGGEE_CLASS)) == null) {
            log.complain("TEST FAILURE: Method Debugee.classByName() returned null");
            tot_res = FAILED;
            return quitDebuggee();
        }
        try {
            fields = rType.allFields();
        } catch (Exception e) {
            log.complain("TEST FAILURE: allFields: caught " + e);
            tot_res = FAILED;
            return quitDebuggee();
        }
        if (createRequests(erManager, fields, rType) == FAILED) {
            tot_res = FAILED;
            return quitDebuggee();
        }

// Create a thread listening JDI events
        elThread = new EventListener();
        elThread.setPriority(Thread.NORM_PRIORITY + 2);
        synchronized(gotEvent) {
            elThread.start();

// Check that all created AccessWatchpointRequests are disabled
            log.display("\na) Getting disabled requested AccessWatchpointEvents...");
            if (checkEvents(false) != PASSED) {
                return FAILED;
            }

// Stop at the breakpoint so we can verify that there were no AccessWatchPointEvent
            if (sendCommand(COMMAND_BREAKPOINT, true) != PASSED) {
                return FAILED;
            }
            if (!breakPointReceived) {
                log.display("TEST FAILED: Did not receive the breakpoint event.");
                return FAILED;
            }

// Check that no AccessWatchpoint events occurred
            for (int i=0; i<FLDS_NUM; i++) {
                if (accFCount[i] != 0) {
                    log.complain("TEST FAILED: got AccessWatchpointEvent for the following field:\n\t"
                        + DEBUGGEE_FLDS[i][0] + " " + DEBUGGEE_FLDS[i][1]
                        + "\n\tbut a corresponding request, created by EventRequestManager.createAccessWatchpointRequest(), should by disabled");
                    tot_res = FAILED;
                } else
                    log.display("\nTEST PASSED: no event for the disabled AccessWatchpointRequest #"
                        + i);
            }

// Check that all events corresponding requested AccessWatchpointRequest
// are properly generated
            log.display("\nb) Getting enabled requested AccessWatchpointEvents...");
            for (int i=0; i<FLDS_NUM; i++) {
                if (awpRequest[i] != null) {
                    awpRequest[i].enable();
                } else {
                    log.complain("TEST FAILED: AccessWatchpointRequest object #"
                        + i + " is null.\n\t"
                        + "It means that appropriate AccessWatchpoint request has not been really created");
                    tot_res = FAILED;
                }
            }
            checkEvents(true);

// Finish the test
            for (int i=0; i<FLDS_NUM; i++) {
                if (awpRequest[i] != null) {
                    awpRequest[i].disable();
                }
            }
        }
        return quitDebuggee();
    }

    private int createRequests(EventRequestManager erManager,
            List fields,
            ReferenceType debuggeeType) {
        Field fld = null;
        int i = 0;

        awpRequest =
            new AccessWatchpointRequest[FLDS_NUM];
        for (i=0; i<FLDS_NUM; i++) {
            boolean notFound = true;
            Iterator iter = fields.iterator();
            while (iter.hasNext()) {
                fld = (Field) iter.next();
                if (fld.name().equals(DEBUGGEE_FLDS[i][1]) &&
                    fld.typeName().equals(DEBUGGEE_FLDS[i][0])) {
                    log.display("\nCreating AccessWatchpointRequest #"
                        + i + " for the debuggee's field:\n\t"
                        + fld.typeName() + " " + fld.name());
                    try {
                        awpRequest[i] =
                            erManager.createAccessWatchpointRequest(fld);
                        awpRequest[i].setSuspendPolicy(AccessWatchpointRequest.SUSPEND_NONE);
                    } catch (NullPointerException e) {
                        log.complain("TEST FAILED: createAccessWatchpointRequest: caught "
                            + e);
                        tot_res = FAILED;
                        return FAILED;
                    } catch(VMMismatchException e) {
                        log.complain("TEST FAILED: createAccessWatchpointRequest: caught "
                            + e);
                        tot_res = FAILED;
                        return FAILED;
                    }
                    notFound = false;
                    break;
                }
            }
            if (notFound) {
                log.complain("TEST FAILED: found unexpected debuggee's field:\n\t"
                    + fld.typeName() + " " + fld.name());
                tot_res = FAILED;
                return FAILED;
            }
        }
        log.display("\nCreating BreakpointRequest for the debuggee's method: breakpoint");
        try {
            Method bpMethod = debuggeeType.methodsByName("breakpoint").get(0);
            BreakpointRequest bpr = erManager.createBreakpointRequest(bpMethod.location());
            bpr.enable();
        } catch(Exception e) {
            log.complain("TEST FAILED: createBreakpointRequest: caught "
                + e);
            tot_res = FAILED;
            return FAILED;
        }
        return PASSED;
    }

    private int sendCommand(String cmd, boolean waitForEvent) {
        String token = null;

        log.display("\nSending the command \""
            + cmd + "\" to a debuggee");
        pipe.println(cmd);

// wait for a requested event
        if (waitForEvent) {
            try {
                gotEvent.wait(argHandler.getWaitTime() * 60 * 1000);
            } catch (InterruptedException e) {
                log.complain("TEST FAILURE: waiting for a requested Event"
                    + ": caught " + e);
                e.printStackTrace();
                tot_res = FAILED;
                return quitDebuggee();
            }
            log.display("Notification about the Event"
                + " received,\n\tor time has elapsed");
        }

        if ((token = pipe.readln()) == null) {
            log.complain("TEST FAILURE: debuggee's reply is empty, probably due to the VM crash");
            tot_res = FAILED;
            return quitDebuggee();
        }
        if (!token.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + token);
            tot_res = FAILED;
            return quitDebuggee();
        }
        else log.display("Debuggee's reply received: "
            + token);
        return PASSED;
    }

    private int checkEvents(boolean shouldBe) {
        for (int i=0; i<FLDS_NUM; i++) {
            accFCount[i] = 0;
            if (sendCommand(COMMAND_RUN[i], shouldBe) == FAILED)
                return FAILED;
            if (shouldBe) {
                if (accFCount[i] != 0) {
                    log.display("Got expected AccessWatchpointEvent for the following debuggee's field:\n\t"
                        + DEBUGGEE_FLDS[i][0] + " " + DEBUGGEE_FLDS[i][1]);
                } else {
                    log.complain("TEST FAILED: no AccessWatchpointEvent for the following debuggee's field:\n\t"
                        + DEBUGGEE_FLDS[i][0] + " " + DEBUGGEE_FLDS[i][1]);
                    tot_res = FAILED;
                }
            }
        }
        return PASSED;
    }

    private int quitDebuggee() {
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
        debuggee.waitFor();
        int debStat = debuggee.getStatus();
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
                    EventSet eventSet = vm.eventQueue().remove(10);
                    if (eventSet != null) { // there is not a timeout
                        EventIterator it = eventSet.eventIterator();
                        while (it.hasNext()) {
                            Event event = it.nextEvent();
                            if (event instanceof VMDeathEvent) {
                                tot_res = FAILED;
                                isConnected = false;
                                log.complain("TEST FAILED: caught unexpected VMDeathEvent");
                            } else if (event instanceof VMDisconnectEvent) {
                                tot_res = FAILED;
                                isConnected = false;
                                log.complain("TEST FAILED: caught unexpected VMDisconnectEvent");
                            } else {
                                log.display("EventListener: following JDI event occured: "
                                    + event.toString());

                                if (event instanceof AccessWatchpointEvent) {
                                    AccessWatchpointEvent awpEvent =
                                        (AccessWatchpointEvent) event;
                                    Field fld = awpEvent.field();
                                    boolean notFound = true;
                                    for (int i=0; i<FLDS_NUM; i++) {
                                        if (awpRequest[i].equals(event.request())) {
                                            log.display("EventListener: AccessWatchpointEvent for the debuggee's field #"
                                                + i + ":\n\t" + fld.typeName()
                                                + " " + fld.name());
                                            accFCount[i] += 1;
                                            notFound = false;
                                            log.display("EventListener: notifying about the received event #"
                                                + i);
                                            synchronized(gotEvent) {
                                                gotEvent.notify(); // notify the main thread
                                            }
                                            break;
                                        }
                                    }
                                    if (notFound) {
                                        log.complain("TEST FAILED: found in the received AccessWatchpointEvent\n\tunexpected debuggee's field "
                                            + fld.typeName() + " " + fld.name());
                                        tot_res = FAILED;
                                    }
                                }
                                else if(event instanceof BreakpointEvent) {
                                    synchronized(gotEvent) {
                                        breakPointReceived = true;
                                        gotEvent.notify(); // notify the main thread
                                    }
                                }
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
