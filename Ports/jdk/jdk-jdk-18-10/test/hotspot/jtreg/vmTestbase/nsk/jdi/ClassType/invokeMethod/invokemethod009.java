/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ClassType.invokeMethod;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method
 * <code>com.sun.jdi.ClassType.invokeMethod()</code>
 * properly throws <i>ClassNotLoadedException</i> - if any argument type
 * has not yet been loaded through the appropriate class loader.<p>
 *
 * The test works as follows. Debugger part of the test invokes
 * debuggee methods <i>dummyMeth</i>, <i>finDummyMeth</i> with arguments of
 * non-loaded reference types <i>DummyType</i>, <i>FinDummyType</i>.
 * The test makes sure that class has not been loaded by the debuggee
 * VM through the JDI method <code>VirtualMachine.classesByName()</code>
 * which should return list of loaded classes only.
 */
public class invokemethod009 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ClassType.invokeMethod.invokemethod009t";

    // name of debuggee main thread
    static final String DEBUGGEE_THRNAME = "invokemethod009tThr";

    // debuggee source line where it should be stopped
    static final int DEBUGGEE_STOPATLINE = 57;

    // tested debuggee methods, fields and reference types
    static final int METH_NUM = 2;
    static final String DEBUGGEE_METHODS[][] = {
            {"dummyMeth", "nsk.jdi.ClassType.invokeMethod.invokemethod009tDummyType",
                    "Lnsk/jdi/ClassType/invokeMethod/invokemethod009tDummyType;" },
            {"finDummyMeth", "nsk.jdi.ClassType.invokeMethod.invokemethod009tFinDummyType",
                    "Lnsk/jdi/ClassType/invokeMethod/invokemethod009tFinDummyType;"}
    };

    static final int TIMEOUT_DELTA = 1000; // in milliseconds

    static final String COMMAND_READY = "ready";
    static final String COMMAND_GO = "go";
    static final String COMMAND_QUIT = "quit";

    private ArgumentHandler argHandler;
    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private VirtualMachine vm;
    private ThreadReference thrRef = null;
    private BreakpointRequest BPreq;
    private volatile int tot_res = Consts.TEST_PASSED;
    private volatile boolean gotEvent = false;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new invokemethod009().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        String cmd;
        int num = 0;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "invokemethod009t.err> ");
        debuggee.resume();
        cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        if ((thrRef =
                debuggee.threadByName(DEBUGGEE_THRNAME)) == null) {
            log.complain("TEST FAILURE: Method Debugee.threadByName() returned null for debuggee thread "
                + DEBUGGEE_THRNAME);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }
        // debuggee main class
        ReferenceType rType = debuggee.classByName(DEBUGGEE_CLASS);

// Check the tested assersion
        try {
            suspendAtBP(rType, DEBUGGEE_STOPATLINE);
            ClassType clsType = (ClassType) rType;

            for (int i=0; i<METH_NUM; i++) {
                List<Method> methList = rType.methodsByName(DEBUGGEE_METHODS[i][0]);
                if (methList.isEmpty()) {
                    log.complain("TEST FAILURE: the expected debuggee method \""
                        + DEBUGGEE_METHODS[i][0]
                        + "\" not found through the JDI method ReferenceType.methodsByName()");
                    tot_res = Consts.TEST_FAILED;
                    continue;
                }
                Method meth = methList.get(0);
                LinkedList<Value> argList = new LinkedList<Value>();
                argList.add(MockReferenceType.createObjectReference(clsType.virtualMachine(),
                        DEBUGGEE_METHODS[i][2], DEBUGGEE_METHODS[i][1]));

                try {
                    log.display("\n" + (i+1) + ") Trying to invoke the method \""
                        + meth.name() + " " + meth.signature()
                        + "\"\n\tgot from reference type \"" + rType
                        + "\"\n\tusing the debuggee class \""
                        + clsType + "\" ...");

                    clsType.invokeMethod(thrRef, meth, argList, 0);

                    // Make sure that the reference type is not loaded by the debuggee VM
                    if ((debuggee.classByName(DEBUGGEE_METHODS[i][1])) != null) {
                        log.display("Skipping the check: the tested reference type\n\t\""
                            + DEBUGGEE_METHODS[i][1]
                            + "\"\n\twas loaded by the debuggee VM, unable to test an assertion");
                        continue;
                    }
                    else {
                        log.complain("TEST FAILED: expected ClassNotLoadedException was not thrown"
                            + "\n\twhen attempted to invoke the method \""
                            + meth.name() + " " + meth.signature()
                            + "\"\n\tgot from reference type \"" + rType
                            + "\"\n\tusing the debuggee class \""
                            + clsType + "\"");
                        tot_res = Consts.TEST_FAILED;
                    }
                } catch (ClassNotLoadedException ce) {
                    log.display("CHECK PASSED: caught expected " + ce
                        + "\n\t" + ce.className());
                } catch (Exception ee) {
                    ee.printStackTrace();
                    log.complain("TEST FAILED: ClassType.invokeMethod(): caught unexpected "
                        + ee + "\n\tinstead of expected ClassNotLoadedException"
                        + "\n\twhen attempted to invoke the method \""
                        + meth.name() + " " + meth.signature()
                        + "\"\n\tgot from reference type \"" + rType
                        + "\"\n\tusing the debuggee class \""
                        + clsType + "\"");
                    tot_res = Consts.TEST_FAILED;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
        }

// Finish the test
        return quitDebuggee();
    }

    private BreakpointRequest setBP(ReferenceType refType, int bpLine) {
        EventRequestManager evReqMan =
            debuggee.getEventRequestManager();
        Location loc;

        try {
            List locations = refType.allLineLocations();
            Iterator iter = locations.iterator();
            while (iter.hasNext()) {
                loc = (Location) iter.next();
                if (loc.lineNumber() == bpLine) {
                    BreakpointRequest BPreq =
                        evReqMan.createBreakpointRequest(loc);
                    BPreq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                    log.display("created " + BPreq + "\n\tfor " + refType
                        + " ; line=" + bpLine);
                    return BPreq;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Failure("setBP: caught unexpected exception: " + e);
        }
        throw new Failure("setBP: location corresponding debuggee source line "
            + bpLine + " not found");
    }

    private void suspendAtBP(ReferenceType rType, int bpLine) {

// Class containing a critical section which may lead to time out of the test
        class CriticalSection extends Thread {
            public volatile boolean waitFor = true;

            public void run() {
                try {
                    do {
                        EventSet eventSet = vm.eventQueue().remove(TIMEOUT_DELTA);
                        if (eventSet != null) { // it is not a timeout
                            EventIterator it = eventSet.eventIterator();
                            while (it.hasNext()) {
                                Event event = it.nextEvent();
                                if (event instanceof VMDisconnectEvent) {
                                    log.complain("TEST FAILED: unexpected VMDisconnectEvent");
                                    break;
                                } else if (event instanceof VMDeathEvent) {
                                    log.complain("TEST FAILED: unexpected VMDeathEvent");
                                    break;
                                } else if (event instanceof BreakpointEvent) {
                                    if (event.request().equals(BPreq)) {
                                        log.display("expected Breakpoint event occured: "
                                            + event.toString());
                                        gotEvent = true;
                                        return;
                                    }
                                } else
                                    log.display("following JDI event occured: "
                                        + event.toString());
                            }
                        }
                    } while(waitFor);
                    log.complain("TEST FAILED: no expected Breakpoint event");
                    tot_res = Consts.TEST_FAILED;
                } catch (Exception e) {
                    e.printStackTrace();
                    tot_res = Consts.TEST_FAILED;
                    log.complain("TEST FAILED: caught unexpected exception: " + e);
                }
            }
        }
/////////////////////////////////////////////////////////////////////////////

        BPreq = setBP(rType, bpLine);
        BPreq.enable();
        CriticalSection critSect = new CriticalSection();
        log.display("\nStarting potential timed out section:\n\twaiting "
            + (argHandler.getWaitTime())
            + " minute(s) for JDI Breakpoint event ...\n");
        critSect.start();
        pipe.println(COMMAND_GO);
        try {
            critSect.join((argHandler.getWaitTime())*60000);
            if (critSect.isAlive()) {
                critSect.waitFor = false;
                throw new Failure("timeout occured while waiting for Breakpoint event");
            }
        } catch (InterruptedException e) {
            critSect.waitFor = false;
            throw new Failure("TEST INCOMPLETE: InterruptedException occured while waiting for Breakpoint event");
        } finally {
            BPreq.disable();
        }
        log.display("\nPotential timed out section successfully passed\n");
        if (gotEvent == false)
            throw new Failure("unable to suspend debuggee thread at breakpoint");
    }

    private int quitDebuggee() {
        vm.resume();
        pipe.println(COMMAND_QUIT);
        debuggee.waitFor();
        int debStat = debuggee.getStatus();
        if (debStat != (Consts.JCK_STATUS_BASE + Consts.TEST_PASSED)) {
            log.complain("TEST FAILED: debuggee process finished with status: "
                + debStat);
            tot_res = Consts.TEST_FAILED;
        } else
            log.display("\nDebuggee process finished with the status: "
                + debStat);

        return tot_res;
    }
}
