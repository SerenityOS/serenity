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

package nsk.jdi.ReferenceType.getValues;

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
 * <code>com.sun.jdi.ReferenceType.getValues()</code>
 * properly throws <i>IllegalArgumentException</i> - if any field is
 * not valid for this object's class.<p>
 *
 * Debuggee part of the test consists of the following classes:
 * <i>getvalues002t</i>, <i>getvalues002tDummySuperCls</i>, and
 * <i>getvalues002tDummyCls</i> which extends getvalues002tDummySuperCls.<br>
 * The exception is provoked to be thrown by getting a Map of values
 * which correspond to a list containing some wrong fields such as:<br>
 * <li>fields obtained from the getvalues002tDummyCls but applied to
 * reference type of the getvalues002t
 * <li>some non-static fields from the getvalues002tDummyCls applied to
 * its reference type.
 */
public class getvalues002 {
    static final String DEBUGGEE_CLASSES[] = {
        "nsk.jdi.ReferenceType.getValues.getvalues002t",
        "nsk.jdi.ReferenceType.getValues.getvalues002tDummyCls",
        "nsk.jdi.ReferenceType.getValues.getvalues002tDummySuperCls"
    };

    // debuggee source line where it should be stopped
    static final int DEBUGGEE_STOPATLINE = 58;

    static final int LST_NUM = 2; // number of lists
    static final int FLD_NUM = 8; // number of fields in particular list
    // tested fields used to provoke the exception and
    // type reference numbers for applying/obtaining a Field
    static final String DEBUGGEE_FLDS[][][] = {
       {{"boolFld", "0", "1", "static"},
        {"byteFld", "0", "1", "static"},
        {"charFld", "0", "1", "static"},
        {"doubleFld", "0", "1", "static"},
        {"floatFld", "0", "1", "static"},
        {"intFld", "0", "1", "static"},
        {"longFld", "0", "1", "static"},
        {"shortFld", "0", "1", "static"}},

       {{"boolMiscFld", "1", "1", "static"},
        {"byteMiscFld", "1", "1", "static"},
        {"charMiscFld", "1", "1", "wrong non-static"},
        {"doubleMiscFld", "1", "1", "static"},
        {"floatMiscFld", "1", "1", "static"},
        {"intMiscFld", "1", "1", "static"},
        {"longMiscFld", "1", "1", "wrong non-static"},
        {"shortMiscFld", "1", "1", "static"}}
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
        return new getvalues002().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASSES[0]);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "getvalues002t.err> ");
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

            for (int l=0; l<LST_NUM; l++) {
                log.display("\n" + (l+1)
                    + ") Constructing a list of debuggee fields ...");
                ArrayList<Field> flds = new ArrayList<Field>();
                int appIx = 0;
                int obtIx = 0;


                for (int i=0; i<FLD_NUM; i++) {
                    // get reference type ##
                    appIx = Integer.parseInt(DEBUGGEE_FLDS[l][i][1]);
                    obtIx = Integer.parseInt(DEBUGGEE_FLDS[l][i][2]);

                    Field fld = rType[obtIx].fieldByName(DEBUGGEE_FLDS[l][i][0]);
                    log.display("\tAdding " + DEBUGGEE_FLDS[l][i][3]
                        + " field #" + (i+1) + ": \""
                        + fld.name() + " " + fld.signature()
                        + "\"\n\t\tobtained from reference type \""
                        + rType[obtIx]
                        + "\"\n\t\tand applied to reference type \""
                        + rType[appIx] + "\" ...");
                    flds.add(fld);
                }

                // Check the tested assersion
                try {
                    log.display("\tTrying to get a Map containing Value of each field in the list"
                        + "\n\t\tusing reference type \""
                        + rType[appIx] + "\" ...");

                    Map<Field, Value> valMap = rType[appIx].getValues(flds);

                    log.complain("TEST FAILED: expected IllegalArgumentException was not thrown"
                        + "\n\twhen attempted to get a Map containing Value of each field in the list"
                        + "\n\t\tusing reference type \""
                        + rType[appIx] + "\",\n\t\tand some of the fields are not valid");
                    tot_res = Consts.TEST_FAILED;
                } catch (IllegalArgumentException ee) {
                    log.display("CHECK PASSED: caught expected " + ee);
                } catch (Exception ue) {
                    ue.printStackTrace();
                    log.complain("TEST FAILED: caught unexpected "
                        + ue + "\n\tinstead of expected IllegalArgumentException"
                        + "\n\twhen attempted to get a Map containing Value of each field in the list"
                        + "\"\n\t\tusing reference type \""
                        + rType[appIx] + "\",\n\t\tand some of the fields are not valid");
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
