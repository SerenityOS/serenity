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

package nsk.jdi.EventRequestManager.classPrepareRequests;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.event.*;

import java.util.List;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method
 * <code>com.sun.jdi.request.EventRequestManager.classPrepareRequests()</code>
 * properly returns all previously created ClassPrepareRequest objects when:
 * <li>event requests are disabled
 * <li>event requests are enabled.<br>
 * ClassPrepareRequest objects are distinguished by the different
 * <code>EventRequest</code>'s properties.<br>
 * EventHandler was added as workaround for the bug 4430096.
 * This prevents the target VM from potential hangup.
 */
public class clsprepreq001 {
    public static final int PASSED = 0;
    public static final int FAILED = 2;
    public static final int JCK_STATUS_BASE = 95;
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.EventRequestManager.classPrepareRequests.clsprepreq001t";
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final int CLSP_NUM = 7;
    static final String PROPS[][] = {
        {"first", "a quick"},
        {"second", "brown"},
        {"third", "fox"},
        {"fourth", "jumps"},
        {"fifth", "over"},
        {"sixth", "the lazy"},
        {"seventh", "dog"}
    };

    private ArgumentHandler argHandler;
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
        return new clsprepreq001().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        ClassPrepareRequest clpRequest[] = new ClassPrepareRequest[CLSP_NUM];
        String cmd;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "clsprepreq001t.err> ");
        vm = debuggee.VM();
        EventRequestManager erManager = vm.eventRequestManager();
        debuggee.resume();
        cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + cmd);
            tot_res = FAILED;
            return quitDebuggee();
        }

        for (int i=0; i<CLSP_NUM; i++) {
            log.display("Creating ClassPrepareRequest #" + i
                + " with the property ("
                + PROPS[i][0] + "," + PROPS[i][1] + ")...");
            clpRequest[i] = erManager.createClassPrepareRequest();
            clpRequest[i].putProperty(PROPS[i][0], PROPS[i][1]);
        }
        elThread = new EventListener();
        elThread.start();

// Check ClassPrepare requests when event requests are disabled
        log.display("\n1) Getting ClassPrepareRequest objects with disabled event requests...");
        checkRequests(erManager, 1);

// Check ClassPrepare requests when event requests are enabled
        for (int i=0; i<CLSP_NUM; i++) {
            clpRequest[i].enable();
        }
        log.display("\n2) Getting ClassPrepareRequest objects with enabled event requests...");
        checkRequests(erManager, 2);

// Finish the test
        for (int i=0; i<CLSP_NUM; i++) {
            clpRequest[i].disable();
        }
        return quitDebuggee();
    }

    private void checkRequests(EventRequestManager erManager, int t_case) {
        List reqL = erManager.classPrepareRequests();
        if (reqL.size() != CLSP_NUM) {
            log.complain("TEST CASE #" + t_case + " FAILED: found " + reqL.size()
                + " ClassPrepare requests\n\texpected: " + CLSP_NUM);
            tot_res = FAILED;
            return;
        }
        for (int i=0; i<CLSP_NUM; i++) {
            ClassPrepareRequest clpReq =
                (ClassPrepareRequest) reqL.get(i);
            boolean notFound = true;
            for (int j=0; j<CLSP_NUM; j++) {
                String propKey = (String) clpReq.getProperty(PROPS[j][0]);
                if (propKey != null) {
                    if (propKey.equals(PROPS[j][1])) {
                        log.display("Found expected ClassPrepare request with the property: ("
                            + PROPS[j][0] + "," + propKey + ")");
                    } else {
                        log.complain("TEST CASE #" + t_case
                            + " FAILED: found ClassPrepare request with unexpected property: ("
                            + PROPS[j][0] + "," + propKey + ")");
                        tot_res = FAILED;
                    }
                    notFound = false;
                    break;
                }
            }
            if (notFound) {
                log.complain("\nTEST CASE #" + t_case
                    + " FAILED: found unexpected ClassPrepare request");
                tot_res = FAILED;
            }
        }
    }

    private int quitDebuggee() {
        pipe.println(COMMAND_QUIT);

        if (elThread != null) {
            try {
                if (elThread.isAlive())
                    elThread.join();
            } catch (InterruptedException e) {
                log.complain("TEST INCOMPLETE: caught InterruptedException "
                             + e);
                tot_res = FAILED;
            }
        }

// Class containing a critical section which may lead to time out of the test
        class criticalSection extends Thread {
            public void run() {
                debuggee.waitFor();
            }
        }

        criticalSection critSect = new criticalSection();
        log.display("\nStarting potential timed out section: waiting "
            + argHandler.getWaitTime() + " minute(s) for debuggee's exit...");
        critSect.start();
        try {
            critSect.join(argHandler.getWaitTime()*60000);
            if (critSect.isAlive()) {
                log.complain("TEST FAILED: Timeout occured while waiting for debuggee's exit");
                return FAILED;
            }
        } catch (InterruptedException e) {
            log.complain("TEST INCOMPLETE: InterruptedException caught while waiting for debuggee's exit");
            return FAILED;
        }
        log.display("Potential timed out section passed\n");

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
        public void run() {
            try {
              eventlistening:
                do {
                    EventSet eventSet = vm.eventQueue().remove(1000);

                    if (eventSet != null) { // there is not a timeout
                        EventIterator it = eventSet.eventIterator();

                        while (it.hasNext()) {
                            Event event = it.nextEvent();

                            log.display("EventListener: following JDI event occured: " + event);

                            if (event instanceof VMDisconnectEvent) {
                                break eventlistening;
                            }
                        }

                        eventSet.resume();
                    }
                } while (true);
            } catch (InterruptedException e) {
                tot_res = FAILED;
                log.complain("FAILURE in EventListener: caught unexpected " + e);
            } catch (VMDisconnectedException e) {
                tot_res = FAILED;
                log.complain("FAILURE in EventListener: caught unexpected " + e);
                e.printStackTrace();
            }

            log.display("EventListener: exiting");
        }
    }
}
