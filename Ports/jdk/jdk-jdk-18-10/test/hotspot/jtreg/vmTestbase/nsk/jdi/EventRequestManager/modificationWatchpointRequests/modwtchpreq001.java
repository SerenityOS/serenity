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

package nsk.jdi.EventRequestManager.modificationWatchpointRequests;

import com.sun.jdi.Field;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.request.ModificationWatchpointRequest;
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
 * <code>com.sun.jdi.request.EventRequestManager.modificationWatchpointRequests()</code>
 * properly returns all ModificationWatchpointRequest objects when:
 * <li>event requests are disabled
 * <li>event requests are enabled<br>
 * EventHandler was added as workaround for the bug 4430096.
 * This prevents the target VM from potential hangup.
 */
public class modwtchpreq001 {
    public static final int PASSED = 0;
    public static final int FAILED = 2;
    public static final int JCK_STATUS_BASE = 95;
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.EventRequestManager.modificationWatchpointRequests.modwtchpreq001t";
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final int FLDS_NUM = 16;
    static final String DEBUGGEE_FLDS[][] = {
        {"byte", "byteFld", "B"},
        {"short", "shortFld", "S"},
        {"int", "intFld", "I"},
        {"long", "longFld", "J"},
        {"float", "floatFld", "F"},
        {"double", "doubleFld", "D"},
        {"char", "charFld", "C"},
        {"boolean", "booleanFld", "Z"},
        {"java.lang.String", "strFld", "Ljava/lang/String;"},
        {"short", "sFld", "S"},
        {"byte", "prFld", "B"},
        {"float", "pubFld", "F"},
        {"double", "protFld", "D"},
        {"int", "tFld", "I"},
        {"long", "vFld", "J"},
        {"char", "fFld", "C"}
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
        return new modwtchpreq001().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        ReferenceType rType;
        List fields;
        ModificationWatchpointRequest mwpRequest[] =
            new ModificationWatchpointRequest[FLDS_NUM];
        String cmd;
        int i = 0;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "modwtchpreq001t.err> ");
        vm = debuggee.VM();
        EventRequestManager erManager = vm.eventRequestManager();
        debuggee.resume();
        cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + cmd);
            tot_res = FAILED;
            return quitDebuggee();
        }

        if ( !vm.canWatchFieldModification()  ) {
            log.display("  TEST CANCELLED due to:  vm.canWatchFieldModification() == false");
            return quitDebuggee();
        }

        if ((rType = debuggee.classByName(DEBUGGEE_CLASS)) == null) {
            log.complain("TEST FAILURE: Method Debugee.classByName() returned null");
            tot_res = FAILED;
            return quitDebuggee();
        }

        try {
            fields = rType.allFields();
        } catch (Exception e) {
            log.complain("TEST FAILURE: allFields: " + e);
            tot_res = FAILED;
            return quitDebuggee();
        }
        Iterator iter = fields.iterator();
        while (iter.hasNext()) {
            Field fld = (Field) iter.next();
            log.display("\nCreating ModificationWatchpointRequest for the field:\n\t"
                + fld.typeName() + " " + fld.name()
                + " type_signature=" + fld.signature());
            try {
                mwpRequest[i++] = erManager.createModificationWatchpointRequest(fld);
            } catch (Exception e) {
                log.complain("TEST FAILURE: createModificationWatchpointRequest: " + e);
                tot_res = FAILED;
                return quitDebuggee();
            }
        }
        elThread = new EventListener();
        elThread.start();

// Check ModificationWatchpoint requests when event requests are disabled
        log.display("\n1) Getting ModificationWatchpointRequest objects with disabled event requests...");
        checkRequests(erManager, 1);

// Check ModificationWatchpoint requests when event requests are enabled
        for (i=0; i<FLDS_NUM; i++) {
            mwpRequest[i].enable();
        }
        log.display("\n2) Getting ModificationWatchpointRequest objects with enabled event requests...");
        checkRequests(erManager, 2);

// Finish the test
        for (i=0; i<FLDS_NUM; i++) {
            mwpRequest[i].disable();
        }
        return quitDebuggee();
    }

    private void checkRequests(EventRequestManager erManager, int t_case) {
        List reqL = erManager.modificationWatchpointRequests();
        if (reqL.size() != FLDS_NUM) {
            log.complain("TEST CASE #" + t_case + " FAILED: found " + reqL.size()
                + " ModificationWatchpoint requests\n\texpected: " + FLDS_NUM);
            tot_res = FAILED;
            return;
        }
        for (int i=0; i<FLDS_NUM; i++) {
            ModificationWatchpointRequest mwpReq =
                (ModificationWatchpointRequest) reqL.get(i);
            Field fld = mwpReq.field();
            boolean notFound = true;
            for (int j=0; j<FLDS_NUM; j++) {
                if (fld.name().equals(DEBUGGEE_FLDS[j][1])) {
                    if (!fld.typeName().equals(DEBUGGEE_FLDS[j][0]) ||
                        !fld.signature().equals(DEBUGGEE_FLDS[j][2])) {
                        log.complain("\nTEST CASE #" + t_case
                            + " FAILED: found ModificationWatchpoint request for the field:\n\t"
                            + fld.typeName() + " " + fld.name()
                            + " type_signature=" + fld.signature()
                            + "\n\texpected field: " + DEBUGGEE_FLDS[j][0]
                            + " " + DEBUGGEE_FLDS[j][1] + " type_signature="
                            + DEBUGGEE_FLDS[j][2]);
                        tot_res = FAILED;
                    } else {
                        log.display("\nFound expected ModificationWatchpoint request for the field:\n\t"
                            + DEBUGGEE_FLDS[j][0] + " " + DEBUGGEE_FLDS[j][1]
                            + " type_signature=" + DEBUGGEE_FLDS[j][2]);
                    }
                    notFound = false;
                    break;
                }
            }
            if (notFound) {
                log.complain("\nTEST CASE #" + t_case
                    + " FAILED: found unexpected ModificationWatchpoint request for the field: "
                    + fld.typeName() + " " + fld.name()
                    + " type_signature=" + fld.signature());
                tot_res = FAILED;
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
