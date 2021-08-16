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

package nsk.jdi.EventRequestManager.stepRequests;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.StepRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.DuplicateRequestException;
import com.sun.jdi.ObjectCollectedException;
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
 * <code>com.sun.jdi.request.EventRequestManager.stepRequests()</code>
 * properly returns all StepRequest objects when:
 * <li>event requests are disabled
 * <li>event requests are enabled<br>
 * A debuggee part of the test creates several dummy user and daemon
 * threads with own names.<br>
 * EventHandler was added as workaround for the bug 4430096.
 * This prevents the target VM from potential hangup.
 */
public class stepreq001 {
    public static final int PASSED = 0;
    public static final int FAILED = 2;
    public static final int JCK_STATUS_BASE = 95;
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.EventRequestManager.stepRequests.stepreq001t";
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final int THRDS_NUM = 8;
    static final String DEBUGGEE_THRDS[] = {
        "main_thr", "thr1", "thr2", "thr3",
        "thr4", "thr5", "thr6", "thr7"
    };
    static final boolean DAEMON_THRDS[] = {
        false, true, false, true,
        true, false, true, false
    };
    static final int RESTRICTIONS[][] = {
        {StepRequest.STEP_MIN,  StepRequest.STEP_INTO},
        {StepRequest.STEP_LINE, StepRequest.STEP_OVER},
        {StepRequest.STEP_MIN,  StepRequest.STEP_OUT},
        {StepRequest.STEP_LINE, StepRequest.STEP_INTO},
        {StepRequest.STEP_LINE, StepRequest.STEP_INTO},
        {StepRequest.STEP_MIN,  StepRequest.STEP_OUT},
        {StepRequest.STEP_LINE, StepRequest.STEP_INTO},
        {StepRequest.STEP_MIN,  StepRequest.STEP_OUT}
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
        return new stepreq001().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        ThreadReference thR;
        List threads;
        StepRequest sRequest[] = new StepRequest[THRDS_NUM];
        String cmd;
        int i = 0;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "stepreq001t.err> ");
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

        try {
            threads = vm.allThreads();
        } catch (Exception e) {
            log.complain("TEST FAILURE: allThreads: " + e);
            tot_res = FAILED;
            return quitDebuggee();
        }

        Iterator iter = threads.iterator();
        while (iter.hasNext()) {
            thR = (ThreadReference) iter.next();
            for (int j=0; j<THRDS_NUM ; j++) {
                if (thR.name().equals(DEBUGGEE_THRDS[j])) {
                    log.display("\nCreating StepRequest for the debuggee's thread #"
                        + j + " \"" + thR.name() + "\"");
                    try {
                        sRequest[i++] = erManager.createStepRequest(thR,
                            RESTRICTIONS[j][0],RESTRICTIONS[j][1]);
                    } catch (DuplicateRequestException e) {
                        log.complain("TEST FAILURE: createStepRequest: caught " + e);
                        tot_res = FAILED;
                        return quitDebuggee();
                    } catch (ObjectCollectedException e) {
                        log.complain("TEST FAILURE: createStepRequest: caught " + e);
                        tot_res = FAILED;
                        return quitDebuggee();
                    } catch (VMMismatchException e) {
                        log.complain("TEST FAILURE: createStepRequest: caught " + e);
                        tot_res = FAILED;
                        return quitDebuggee();
                    }
                }
            }
        }
        elThread = new EventListener();
        elThread.start();

// Check Step requests when event requests are disabled
        log.display("\n1) Getting StepRequest objects with disabled event requests...");
        checkRequests(erManager, 1);

// Check Step requests when event requests are enabled
        for (i=0; i<THRDS_NUM; i++) {
            if (sRequest[i] != null) {
                sRequest[i].enable();
            } else {
                log.complain("TEST FAILED: StepRequest object #"
                    + i + " is null.\n\t"
                    + "It means that appropriate Step request has not been really created");
                tot_res = FAILED;
            }
        }
        log.display("\n2) Getting StepRequest objects with enabled event requests...");
        checkRequests(erManager, 2);

// Finish the test
        for (i=0; i<THRDS_NUM; i++) {
            if (sRequest[i] != null) {
                sRequest[i].disable();
            }
        }
        return quitDebuggee();
    }

    private void checkRequests(EventRequestManager erManager, int t_case) {
        List reqL = erManager.stepRequests();
        if (reqL.size() != THRDS_NUM) {
            log.complain("TEST CASE #" + t_case
                + " FAILED: found " + reqL.size()
                + " Step requests\n\texpected: " + THRDS_NUM);
            tot_res = FAILED;
            return;
        }
        for (int i=0; i<THRDS_NUM; i++) {
            StepRequest sReq =
                (StepRequest) reqL.get(i);
            ThreadReference thr = sReq.thread();
            boolean notFound = true;
            for (int j=0; j<THRDS_NUM; j++) {
                if (thr.name().equals(DEBUGGEE_THRDS[j])) {
                    if (sReq.size() != RESTRICTIONS[j][0] ||
                        sReq.depth() != RESTRICTIONS[j][1]) {
                        log.complain("\nTEST CASE #" + t_case
                            + " FAILED: found the following StepRequest object:\n\tthread_name=\""
                            + thr.name() + "\"; size=" + sReq.size()
                            + "; depth=" + sReq.depth()
                            + "\n\texpected object: thread_name=\""
                            + DEBUGGEE_THRDS[j] + "\"; size=" + RESTRICTIONS[j][0]
                            + "; depth=" + RESTRICTIONS[j][1]);
                        tot_res = FAILED;
                    } else {
                        log.display("\nFound expected StepRequest object:\n\tthread_name=\""
                            + thr.name() + "\"; size=" + sReq.size()
                            + "; depth=" + sReq.depth());
                    }
                    notFound = false;
                    break;
                }
            }
            if (notFound) {
                log.complain("\nTEST CASE #" + t_case
                    + " FAILED: found unexpected StepRequest object:\n\tthread_name=\""
                    + thr.name() + "\"; size=" + sReq.size()
                    + "; depth=" + sReq.depth());
                tot_res = FAILED;
            }
        }
    }

    private int quitDebuggee() {
        elThread.exiting = true;

        pipe.println(COMMAND_QUIT);
        debuggee.waitFor();

        elThread.isConnected = false;
        try {
            if (elThread.isAlive())
                elThread.join();
        } catch (InterruptedException e) {
            log.complain("TEST INCOMPLETE: caught InterruptedException "
                + e);
            tot_res = FAILED;
        }

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
        public volatile boolean exiting;

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
                                isConnected = false;
                                if (!exiting) {
                                    tot_res = FAILED;
                                    log.complain("TEST FAILED: unexpected VMDeathEvent");
                                }
                            } else if (event instanceof VMDisconnectEvent) {
                                isConnected = false;
                                if (!exiting) {
                                    tot_res = FAILED;
                                    log.complain("TEST FAILED: unexpected VMDisconnectEvent");
                                }
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
                if (!exiting) {
                    tot_res = FAILED;
                    log.complain("FAILURE in EventListener: caught unexpected "
                        + e);
                    e.printStackTrace();
                }
            }
            log.display("EventListener: exiting");
        }
    }
}
