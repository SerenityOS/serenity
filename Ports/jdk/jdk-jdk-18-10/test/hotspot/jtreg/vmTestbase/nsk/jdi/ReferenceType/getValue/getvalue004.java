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

package nsk.jdi.ReferenceType.getValue;

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
 * <code>com.sun.jdi.ReferenceType.getValue()</code>
 * properly throws <i>IllegalArgumentException</i> - if the field
 * is not valid for this object's class.<p>
 *
 * Debuggee part of the test consists of the following classes:
 * <i>getvalue004t</i>, <i>getvalue004tDummySuperCls</i>, and
 * <i>getvalue004tDummyCls</i> which extends getvalue004tDummySuperCls.<br>
 * The exception is provoked to be thrown by getting a Value of:<br>
 * <li>field obtained from the getvalue004tDummyCls but applied to
 * reference type of the getvalue004t
 * <li>private field obtained from the getvalue004tDummySuperCls
 * but applied to reference type of the getvalue004t
 */
public class getvalue004 {
    static final String DEBUGGEE_CLASSES[] = {
        "nsk.jdi.ReferenceType.getValue.getvalue004t",
        "nsk.jdi.ReferenceType.getValue.getvalue004tDummyCls",
        "nsk.jdi.ReferenceType.getValue.getvalue004tDummySuperCls"
    };

    // debuggee source line where it should be stopped
    static final int DEBUGGEE_STOPATLINE = 58;

    static final int FLD_NUM = 16;
    // tested fields used to provoke the exception and
    // type reference numbers for obtaining/applying a Field
    static final String DEBUGGEE_FLDS[][] = {
        {"boolFld", "0", "1"},
        {"byteFld", "0", "1"},
        {"charFld", "0", "1"},
        {"doubleFld", "0", "1"},
        {"floatFld", "0", "1"},
        {"intFld", "0", "1"},
        {"longFld", "0", "1"},
        {"shortFld", "0", "1"},
        {"boolPrFld", "0", "2"},
        {"bytePrFld", "0", "2"},
        {"charPrFld", "0", "2"},
        {"doublePrFld", "0", "2"},
        {"floatPrFld", "0", "2"},
        {"intPrFld", "0", "2"},
        {"longPrFld", "0", "2"},
        {"shortPrFld", "0", "2"}
    };

    static final String COMMAND_READY = "ready";
    static final String COMMAND_GO = "go";
    static final String COMMAND_QUIT = "quit";

    static final int DELAY = 500; // in milliseconds

    private ArgumentHandler argHandler;
    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private VirtualMachine vm;
    private volatile int tot_res = Consts.TEST_PASSED;
    private BreakpointRequest BPreq;
    private volatile boolean gotEvent = false;

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new getvalue004().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASSES[0]);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "getvalue004t.err> ");
        debuggee.resume();
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        try {
            ReferenceType[] rType = new ReferenceType[3];
            // debuggee main & dummy classes
            rType[0] = debuggee.classByName(DEBUGGEE_CLASSES[0]);
            rType[1] = debuggee.classByName(DEBUGGEE_CLASSES[1]);
            rType[2] = debuggee.classByName(DEBUGGEE_CLASSES[2]);

            // suspend debuggee VM at breakpoint
            suspendAtBP(rType[0], DEBUGGEE_STOPATLINE);

            for (int i=0; i<FLD_NUM; i++) {
                // get reference type ##
                int appIx = Integer.parseInt(DEBUGGEE_FLDS[i][1]);
                int obtIx = Integer.parseInt(DEBUGGEE_FLDS[i][2]);

                Field fld = rType[obtIx].fieldByName(DEBUGGEE_FLDS[i][0]);

                // Check the tested assersion
                try {
                    log.display("\n" + (i+1)
                        + ") Trying to get a Value of static field \""
                        + fld.name() + " " + fld.signature()
                        + "\"\n\tobtained from reference type \""
                        + rType[obtIx]
                        + "\"\n\tbut using reference type \""
                        + rType[appIx] + "\" ...");

                    Value val = rType[appIx].getValue(fld);

                    log.complain("TEST FAILED: expected IllegalArgumentException was not thrown"
                        + "\n\twhen attempted to get a Value of static field \""
                        + fld.name() + " " + fld.signature()
                        + "\"\n\tobtained from reference type \""
                        + rType[obtIx]
                        + "\"\n\tbut using reference type \""
                        + rType[appIx] + "\"\n\tReturned value: "
                        + val);
                    tot_res = Consts.TEST_FAILED;
                } catch (IllegalArgumentException ee) {
                    log.display("CHECK PASSED: caught expected " + ee);
                } catch (Exception ue) {
                    ue.printStackTrace();
                    log.complain("TEST FAILED: caught unexpected "
                        + ue + "\n\tinstead of expected IllegalArgumentException"
                        + "\n\twhen attempted to get a Value of static field \""
                        + fld.name() + " " + fld.signature()
                        + "\"\n\tobtained from reference type \""
                        + rType[obtIx]
                        + "\"\n\tbut using reference type \""
                        + rType[appIx] + "\"");
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
                        EventSet eventSet = vm.eventQueue().remove(DELAY);
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
        log.display("\nFinal resumption of the debuggee VM");
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
