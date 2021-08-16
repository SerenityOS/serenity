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

package nsk.jdi.EventRequestManager.exceptionRequests;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.event.*;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ObjectCollectedException;

import java.util.List;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method
 * <code>com.sun.jdi.request.EventRequestManager.exceptionRequests()</code>
 * properly returns all previously created ExceptionRequest objects for the
 * different kinds of methods in two debuggee's classes when:
 * <li>event requests are disabled
 * <li>event requests are enabled<br>
 * The debuggee loads several dummy classes which ReferenceType
 * objects are obtained by the debugger for exercising
 * the <code>exceptionRequests()</code>.<br>
 * EventHandler was added as workaround for the bug 4430096.
 * This prevents the target VM from potential hangup.
 */
public class excreq001 {
    public static final int PASSED = 0;
    public static final int FAILED = 2;
    public static final int JCK_STATUS_BASE = 95;
    static final String PACK =
        "nsk.jdi.EventRequestManager.exceptionRequests";
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final int EXC_NUM = 4;
// list of debuggee's loaded classes
    static final String DEBUGGEE_CLS[][] = {
        {PACK + ".excreq001t", "excreq001t.java"},
        {PACK + ".excreq001a", "excreq001a.java"},
        {PACK + ".excreq001b", "excreq001b.java"},
        {PACK + ".excreq001c", "excreq001c.java"},
    };
// caught/uncaught exception notifications
    static final boolean DEBUGGEE_NTFS[][] = {
        {true, true},
        {true, false},
        {false, false},
        {false, true}
    };

    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private VirtualMachine vm;
    private EventListener elThread;
    private volatile int tot_res = PASSED;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new excreq001().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        ReferenceType rType;
        ExceptionRequest excRequest[] = new ExceptionRequest[EXC_NUM];
        String cmd;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLS[0][0]);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "excreq001t.err> ");
        vm = debuggee.VM();
        EventRequestManager erManager = vm.eventRequestManager();
        debuggee.resume();
        cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + cmd);
            tot_res = FAILED;
            return quitDebuggee();
        }

        for (int i=0; i<EXC_NUM; i++) {
            if ((rType = debuggee.classByName(DEBUGGEE_CLS[i][0])) == null) {
                log.complain("TEST BUG: cannot find " + DEBUGGEE_CLS[i][0]);
                tot_res = FAILED;
                return quitDebuggee();
            }
            try {
                log.display("\nCreating ExceptionRequest for the ReferenceType:\n\tname="
                    + rType.name() + "\n\tsource=" + rType.sourceName()
                    + "\n\tnotifications of caught/uncaught exception: ("
                    + DEBUGGEE_NTFS[i][0] + "," + DEBUGGEE_NTFS[i][1] + ")");
            } catch(AbsentInformationException e) {
                log.complain("TEST FAILURE: ReferenceType.sourceName(): " + e);
                tot_res = FAILED;
                return quitDebuggee();
            } catch(ObjectCollectedException e) {
                log.complain("TEST FAILURE: caught " + e);
                tot_res = FAILED;
                return quitDebuggee();
            }
            try {
                excRequest[i] = erManager.createExceptionRequest(rType,
                    DEBUGGEE_NTFS[i][0],DEBUGGEE_NTFS[i][1]);
            } catch (Exception e) {
                log.complain("TEST FAILURE: EventRequestManager.createExceptionRequest(): "
                    + e);
                tot_res = FAILED;
                return quitDebuggee();
            }
        }
        elThread = new EventListener();
        elThread.start();

// Check Exception requests when event requests are disabled
        log.display("\n1) Getting ExceptionRequest objects with disabled event requests...");
        checkRequests(erManager, 1);

// Check Exception requests when event requests are enabled
        for (int i=0; i<EXC_NUM; i++) {
            excRequest[i].enable();
        }
        log.display("\n2) Getting ExceptionRequest objects with enabled event requests...");
        checkRequests(erManager, 2);

// Finish the test
        for (int i=0; i<EXC_NUM; i++) {
            excRequest[i].disable();
        }
        return quitDebuggee();
    }

    private void checkRequests(EventRequestManager erManager, int t_case) {
        List reqL = erManager.exceptionRequests();
        if (reqL.size() != EXC_NUM) {
            log.complain("\nTEST CASE #" + t_case + " FAILED: found " + reqL.size()
                + " ExceptionRequest objects\n\texpected: " + EXC_NUM);
            tot_res = FAILED;
            return;
        }
        for (int i=0; i<EXC_NUM; i++) {
            ExceptionRequest excReq =
                (ExceptionRequest) reqL.get(i);
            ReferenceType refT = excReq.exception();
            boolean notFound = true;
            try {
                for (int j=0; j<EXC_NUM; j++) {
                    if (refT.name().equals(DEBUGGEE_CLS[j][0])) {
                        if (refT.sourceName().equals(DEBUGGEE_CLS[j][1]) &&
                                excReq.notifyCaught() == DEBUGGEE_NTFS[j][0] &&
                                excReq.notifyUncaught() == DEBUGGEE_NTFS[j][1]) {
                            log.display("\nFound an expected ExceptionRequest object for the ReferenceType:\n\tname="
                                + DEBUGGEE_CLS[j][0] + "\n\tsource=" + DEBUGGEE_CLS[j][1]
                                + "\n\tnotifications of caught/uncaught exceptions: (" + DEBUGGEE_NTFS[j][0] + ","
                                + DEBUGGEE_NTFS[j][1] + ")");
                        } else {
                            log.complain("\nTEST CASE #" + t_case
                                + " FAILED: found an ExceptionRequest object for the ReferenceType:\n\tname="
                                + refT.name() + "\n\tsource=" + refT.sourceName()
                                + "\n\tnotifications of caught/uncaught exception: ("
                                + excReq.notifyCaught() + "," + excReq.notifyUncaught() + ")"
                                + "\n\tmismatched with the expected ReferenceType:\n\tname="
                                + DEBUGGEE_CLS[j][0] + "\n\tsource=" + DEBUGGEE_CLS[j][1]
                                + "\tnotifications of caught/uncaught exception: (" + DEBUGGEE_NTFS[j][0] + ","
                                + DEBUGGEE_NTFS[j][1] + ")");
                             tot_res = FAILED;
                        }
                        notFound = false;
                        break;
                    }
                }
                if (notFound) {
                    log.complain("\nTEST CASE #" + t_case
                        + " FAILED: found an ExceptionRequest object for the unexpected ReferenceType:\n\tname="
                        + refT.name() + "\n\tsource=" + refT.sourceName()
                        + "\n\tnotifications of caught/uncaught exception: ("
                        + excReq.notifyCaught() + "," + excReq.notifyUncaught() + ")");
                    tot_res = FAILED;
                }
            } catch(AbsentInformationException e) {
                log.complain("TEST FAILURE: checkRequests: caught: "
                    + e);
                tot_res = FAILED;
                return;
            }
        }
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
                    EventSet eventSet = vm.eventQueue().remove(1000);
                    if (eventSet != null) { // there is not a timeout
                        EventIterator it = eventSet.eventIterator();
                        while (it.hasNext()) {
                            Event event = it.nextEvent();
                            if (event instanceof VMDeathEvent) {
                                tot_res = FAILED;
                                isConnected = false;
                                log.complain("TEST FAILED: unexpected VMDeathEvent");
                            } else if (event instanceof VMDisconnectEvent) {
                                tot_res = FAILED;
                                isConnected = false;
                                log.complain("TEST FAILED: unexpected VMDisconnectEvent");
                            } else
                                log.display("EventListener: following JDI event occured: "
                                    + event.toString());
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
