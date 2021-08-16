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

package nsk.jdi.EventRequestManager.breakpointRequests;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotPreparedException;
import com.sun.jdi.ObjectCollectedException;
//import com.sun.jdi.InvalidLineNumberException;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.event.*;

import java.util.Iterator;
import java.util.List;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method
 * <code>com.sun.jdi.request.EventRequestManager.breakpointRequests()</code>
 * properly returns all previously created BreakpointRequest objects when:
 * <li>event requests are disabled
 * <li>event requests are enabled<br>
 * EventHandler was added as workaround for the bug 4430096.
 * This prevents the target VM from potential hangup.
 */
public class breakpreq001 {
    public static final int PASSED = 0;
    public static final int FAILED = 2;
    public static final int JCK_STATUS_BASE = 95;
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.EventRequestManager.breakpointRequests.breakpreq001t";
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final int BPS_NUM = 10;
    static final String DEBUGGEE_BPS[][] = {
        {"main", "void", "breakpreq001t.java"},
        {"byteMeth", "byte", "breakpreq001t.java"},
        {"shortMeth", "short", "breakpreq001t.java"},
        {"prMeth", "int", "breakpreq001t.java"},
        {"protMeth", "long", "breakpreq001t.java"},
        {"floatMeth", "float", "breakpreq001t.java"},
        {"doubleMeth", "double", "breakpreq001t.java"},
        {"charMeth", "char", "breakpreq001t.java"},
        {"boolMeth", "boolean", "breakpreq001t.java"},
        {"pubMeth", "java.lang.String", "breakpreq001t.java"}
    };
    static final int DEBUGGEE_LNS[] = {
        37, 54, 57, 60, 63, 66, 69, 72, 75, 78
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
        return new breakpreq001().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        ReferenceType rType;
        List loctns;
        BreakpointRequest bpRequest[] = new BreakpointRequest[BPS_NUM];
        String cmd;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "breakpreq001t.err> ");
        vm = debuggee.VM();
        EventRequestManager erManager = vm.eventRequestManager();
        debuggee.resume();
        cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + cmd);
            tot_res = FAILED;
            return quitDebuggee();
        }
        if ((rType = debuggee.classByName(DEBUGGEE_CLASS)) == null) {
            log.complain("TEST FAILURE: Method Debugee.classByName() returned null");
            tot_res = FAILED;
            return quitDebuggee();
        }

        for (int i=0; i<BPS_NUM; i++) {
            try {
                loctns = rType.locationsOfLine(DEBUGGEE_LNS[i]);
            } catch(AbsentInformationException e) {
                log.complain("TEST FAILURE: ReferenceType.locationsOfLine(): " + e);
                tot_res = FAILED;
                return quitDebuggee();
            } catch(ClassNotPreparedException e) {
                log.complain("TEST FAILURE: ReferenceType.locationsOfLine(): " + e);
                tot_res = FAILED;
                return quitDebuggee();
            } catch(ObjectCollectedException e) {
                log.complain("TEST FAILURE: ReferenceType.locationsOfLine(): " + e);
                tot_res = FAILED;
                return quitDebuggee();
/*            } catch(InvalidLineNumberException e) {
                log.complain("TEST FAILURE: ReferenceType.locationsOfLine(): " + e);
                tot_res = FAILED;
                return quitDebuggee();*/
            }
            if (loctns.size() == 0) {
                log.complain("TEST FAILED: ReferenceType.locationsOfLine() returned 0 JDI Locations\n"
                    + "\tfor the valid debuggee's line #" + DEBUGGEE_LNS[i]);
                tot_res = FAILED;
                return quitDebuggee();
            }
            Iterator iter = loctns.iterator();
            while (iter.hasNext()) {
                Location loc = (Location) iter.next();
                try {
                    log.display("Creating BreakpointRequest for the location:\n\t"
                        + loc.sourceName() + ":" + loc.lineNumber() + "\tmethod: "
                        + loc.method().returnTypeName()
                        + " " + loc.method().name());
                } catch(AbsentInformationException e) {
                    log.complain("TEST FAILURE: Location.sourceName(): " + e);
                    tot_res = FAILED;
                    return quitDebuggee();
                }
                try {
                    bpRequest[i] = erManager.createBreakpointRequest(loc);
                } catch (Exception e) {
                    log.complain("TEST FAILURE: EventRequestManager.createBreakpointRequest(): "
                        + e);
                    tot_res = FAILED;
                    return quitDebuggee();
                }
            }
        }
        elThread = new EventListener();
        elThread.start();

// Check Breakpoint requests when event requests are disabled
        log.display("\n1) Getting BreakpointRequest objects with disabled event requests...");
        checkRequests(erManager, 1);

// Check Breakpoint requests when event requests are enabled
        for (int i=0; i<BPS_NUM; i++) {
            bpRequest[i].enable();
        }
        log.display("\n2) Getting BreakpointRequest objects with enabled event requests...");
        checkRequests(erManager, 2);

// Finish the test
        for (int i=0; i<BPS_NUM; i++) {
            bpRequest[i].disable();
        }
        return quitDebuggee();
    }

    private void checkRequests(EventRequestManager erManager, int t_case) {
        List reqL = erManager.breakpointRequests();
        if (reqL.size() != BPS_NUM) {
            log.complain("\nTEST CASE #" + t_case + " FAILED: found " + reqL.size()
                + " Breakpoint requests\n\texpected: " + BPS_NUM);
            tot_res = FAILED;
            return;
        }
        for (int i=0; i<BPS_NUM; i++) {
            BreakpointRequest bpReq =
                (BreakpointRequest) reqL.get(i);
            Location loc = bpReq.location();
            boolean notFound = true;
            try {
                for (int j=0; j<BPS_NUM; j++) {
                    if (loc.lineNumber() == DEBUGGEE_LNS[j]) {
                        if (loc.method().name().equals(DEBUGGEE_BPS[j][0]) &&
                                loc.method().returnTypeName().equals(DEBUGGEE_BPS[j][1]) &&
                                loc.sourceName().equals(DEBUGGEE_BPS[j][2])) {
                            log.display("Found expected Breakpoint request for the location:\n\t"
                                + DEBUGGEE_BPS[j][2] + ":" + DEBUGGEE_LNS[j]
                                + "\tmethod: " + DEBUGGEE_BPS[j][1] + " " + DEBUGGEE_BPS[j][0]);
                        } else {
                            log.complain("\nTEST CASE #" + t_case
                                + " FAILED: found a Breakpoint request for the location:\n\t"
                                + loc.sourceName() + ":" + loc.lineNumber()
                                + "\tmethod: " + loc.method().returnTypeName() + " "
                                + loc.method().name()
                                + "\n\tmismatched with the expected location: "
                                + DEBUGGEE_BPS[j][2] + ":"
                                + DEBUGGEE_LNS[j] + "\tmethod: " + DEBUGGEE_BPS[j][1]
                                + " " + DEBUGGEE_BPS[j][0]);
                            tot_res = FAILED;
                        }
                        notFound = false;
                        break;
                    }
                }
                if (notFound) {
                    log.complain("\nTEST CASE #" + t_case
                        + " FAILED: found a Breakpoint request for the unexpected location:\n\t"
                        + loc.sourceName() + ":" + loc.lineNumber() + "\tmethod: "
                        + loc.method().returnTypeName() + " "
                        + loc.method().name());
                    tot_res = FAILED;
                }
            } catch(AbsentInformationException e) {
                log.complain("TEST FAILURE: checkRequests: Location.sourceName(): "
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
