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

package nsk.jdi.ObjectReference.invokeMethod;

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
 * <code>com.sun.jdi.ObjectReference.invokeMethod()</code>
 * properly throws <i>IllegalArgumentException</i> when
 * debugger part of the test attempts to invoke several
 * debuggee methods:
 * <li>which are not members of an object's class
 * <li>which are constructors or static intializers
 * <li>with the size mismatch of an argument list.
 */
public class invokemethod002 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ObjectReference.invokeMethod.invokemethod002t";

    // tested debuggee methods and type/object reference numbers
    static final int METH_NUM = 14;
    static final String DEBUGGEE_METHODS[][] = {
        {"<init>", "0", "0"},
        {"<clinit>", "0", "0"},
        {"<init>", "1", "0"},
        {"<clinit>", "1", "0"},
        {"byteMeth", "1", "0"},
        {"shortMeth", "1", "0"},
        {"intMeth", "1", "0"},
        {"longMeth", "1", "0"},
        {"floatMeth", "1", "0"},
        {"doubleMeth", "1", "0"},
        {"charMeth", "1", "0"},
        {"booleanMeth", "1", "0"},
        {"strMeth", "1", "0"},
        {"voidMeth", "1", "0"}
    };

    // name of debuggee main thread
    static final String DEBUGGEE_THRNAME = "invokemethod002tThr";

    // debuggee source line where it should be stopped
    static final int DEBUGGEE_STOPATLINE = 62;

    // debuggee local var used to find needed stack frame
    static final String DEBUGGEE_LOCALVAR = "invokemethod002tdummyCls";

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
    private ObjectReference[] objRef = new ObjectReference[2];
    private ReferenceType[] rType = new ReferenceType[2];
    private BreakpointRequest BPreq;
    private volatile int tot_res = Consts.TEST_PASSED;
    private volatile boolean gotEvent = false;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new invokemethod002().runIt(argv, out);
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
        debuggee.redirectStderr(log, "invokemethod002t.err> ");
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
        rType[0] = debuggee.classByName(DEBUGGEE_CLASS); // debuggee main class

// Check the tested assersion
        try {
            suspendAtBP(rType[0], DEBUGGEE_STOPATLINE);
            findObjRef(DEBUGGEE_LOCALVAR);
            rType[1] = objRef[1].referenceType(); // debuggee dummy class

            // provoke the IllegalArgumentException with the wrong methods
            provokeException(Collections.<Value>emptyList(), true);

            // provoke the IllegalArgumentException with the wrong argument list
            provokeException(
                Collections.<Value>singletonList(vm.mirrorOf(9223372036854775807L)),
                false);
        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
        }

// Finish the test
        return quitDebuggee();
    }

    private void provokeException(List<Value> argList, boolean use3rdIndex) {
        int objIndex;

        for (int i=0; i<METH_NUM; i++) {
            int typeIndex = Integer.parseInt(DEBUGGEE_METHODS[i][1]);
            if (use3rdIndex)
                objIndex = Integer.parseInt(DEBUGGEE_METHODS[i][2]);
            else
                objIndex = typeIndex;

            List methList = rType[typeIndex].methodsByName(DEBUGGEE_METHODS[i][0]);
            if (methList.isEmpty()) {
                log.complain("TEST FAILURE: the expected debuggee method \""
                    + DEBUGGEE_METHODS[i][0]
                    + "\" not found through the JDI method ReferenceType.methodsByName()");
                tot_res = Consts.TEST_FAILED;
                continue;
            }
            Method meth = (Method) methList.get(0);

            try {
                log.display("\nTrying to invoke the method \""
                    + meth.name() + " " + meth.signature()
                    + "\"\n\tgot from reference type \"" + rType[typeIndex]
                    + "\"\n\tusing the debuggee object reference \""
                    + objRef[objIndex] + "\" ...");

                objRef[objIndex].invokeMethod(thrRef, meth, argList, 0);

                log.complain("TEST FAILED: expected IllegalArgumentException was not thrown"
                    + "\n\twhen attempted to invoke the method \""
                    + meth.name() + " " + meth.signature()
                    + "\"\n\tgot from reference type \"" + rType[typeIndex]
                    + "\"\n\tusing the debuggee object reference \""
                    + objRef[objIndex] + "\"");
                tot_res = Consts.TEST_FAILED;
            } catch (IllegalArgumentException ia) {
                log.display("CHECK PASSED: caught expected " + ia);
            } catch (Exception ee) {
                ee.printStackTrace();
                log.complain("TEST FAILED: ObjectReference.invokeMethod(): caught unexpected "
                    + ee + "\n\tinstead of expected llegalArgumentException"
                    + "\n\twhen attempted to invoke the method \""
                    + meth.name() + " " + meth.signature()
                    + "\"\n\tgot from reference type \"" + rType[typeIndex]
                    + "\"\n\tusing the debuggee object reference \""
                    + objRef[objIndex] + "\"");
                tot_res = Consts.TEST_FAILED;
            }
        }
   }

    private void findObjRef(String varName) {
        try {
            List frames = thrRef.frames();
            Iterator iter = frames.iterator();
            while (iter.hasNext()) {
                StackFrame stackFr = (StackFrame) iter.next();
                try {
                    LocalVariable locVar = stackFr.visibleVariableByName(varName);

                    if (locVar == null) continue;
                    // main debuggee class
                    objRef[0] = stackFr.thisObject();
                    // dummy debuggee class
                    objRef[1] = (ObjectReference)
                        stackFr.getValue(locVar);
                    return;
                } catch(AbsentInformationException e) {
                    // this is not needed stack frame, ignoring
                } catch(NativeMethodException ne) {
                    // current method is native, also ignoring
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            tot_res = Consts.TEST_FAILED;
            throw new Failure("findObjRef: caught unexpected exception: " + e);
        }
        throw new Failure("findObjRef: needed debuggee stack frame not found");
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
