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
 * The test checks that virtual and non-virtual method invocation
 * will be performed properly through the JDI method
 * <code>com.sun.jdi.ObjectReference.invokeMethod()</code>.<br>
 * A debugger part of the test invokes several debuggee methods,
 * overriden in an object reference class, but obtained from
 * a reference type of its superclass. The debugger calls the
 * JDI method without and with the flag <i>ObjectReference.INVOKE_NONVIRTUAL</i>
 * sequentially. It is expected, that methods from the object reference
 * class instead of from the superclass will be invoked without
 * the flag <i>INVOKE_NONVIRTUAL</i> and vise versa otherwise.
 */
public class invokemethod008 {
    // debuggee classes
    static final String DEBUGGEE_CLASSES[] = {
        "nsk.jdi.ObjectReference.invokeMethod.invokemethod008t",
        "nsk.jdi.ObjectReference.invokeMethod.invokemethod008tDummySuperClass",
        "nsk.jdi.ObjectReference.invokeMethod.invokemethod008tDummySuperSuperClass"
    };

    // name of debuggee main thread
    static final String DEBUGGEE_THRNAME = "invokemethod008tThr";

    // debuggee source line where it should be stopped
    static final int DEBUGGEE_STOPATLINE = 69;

    // debuggee local var used to find needed stack frame
    static final String DEBUGGEE_LOCALVAR =
        "invokemethod008tdummyCls";

    // tested debuggee methods to be invoked, reference type numbers
    // and expected return values
    static final int METH_NUM = 4;
    static final String DEBUGGEE_METHODS[][] = {
        {"longProtMeth", "1", "9223372036854775806"},
        {"longMeth", "1", "9223372036854775806"},
        {"longProtMeth", "2", "9223372036854775805"},
        {"longMeth", "2", "9223372036854775805"}
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
        return new invokemethod008().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        String cmd;
        int num = 0;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASSES[0]);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "invokemethod008t.err> ");
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
        ReferenceType[] rType = new ReferenceType[3];
        // reference types of debuggee main & dummy classes
        rType[0] = debuggee.classByName(DEBUGGEE_CLASSES[0]);
        rType[1] = debuggee.classByName(DEBUGGEE_CLASSES[1]);
        rType[2] = debuggee.classByName(DEBUGGEE_CLASSES[2]);

// Check the tested assersion
        try {
            suspendAtBP(rType[0], DEBUGGEE_STOPATLINE);
            ObjectReference objRef = findObjRef(DEBUGGEE_LOCALVAR);

            for (int i=0; i<METH_NUM; i++) {
                // get reference type # from DEBUGGEE_METHODS[i][1]
                int typeIndex = Integer.parseInt(DEBUGGEE_METHODS[i][1]);

                // get expected return value from DEBUGGEE_METHODS[i][2]
                long expValue = Long.parseLong(DEBUGGEE_METHODS[i][2]);

                // get method to be invoked from DEBUGGEE_METHODS[i][0]
                List methList = rType[typeIndex].methodsByName(DEBUGGEE_METHODS[i][0]);
                if (methList.isEmpty()) {
                    log.complain("TEST FAILURE: the expected debuggee method \""
                        + DEBUGGEE_METHODS[i]
                        + "\" not found through the JDI method ReferenceType.methodsByName()");
                    tot_res = Consts.TEST_FAILED;
                    continue;
                }
                Method meth = (Method) methList.get(0);
                LinkedList<Value> argList = new LinkedList<Value>();
                argList.add(vm.mirrorOf(9223372036854775807L));

                try {
                    log.display("\n\n1) Trying to invoke the method \""
                        + meth.name() + " " + meth.signature() + " " + meth
                        + "\"\n\tgot from reference type \"" + rType[typeIndex]
                        + "\"\n\twith the arguments: " + argList
                        + " and without the flag ObjectReference.INVOKE_NONVIRTUAL"
                        + "\n\tusing the debuggee object reference \""
                        + objRef + "\" ...");

                    LongValue retVal = (LongValue)
                        objRef.invokeMethod(thrRef, meth, argList, 0);
                    if (retVal.value() == 9223372036854775807L) {
                        log.display("CHECK PASSED: the invoked method returns: " + retVal.value()
                            + " as expected");
                    }
                    else {
                        log.complain("TEST FAILED: wrong virtual invocation: the method \""
                            + meth.name() + " " + meth.signature()
                            + "\"\n\tgot from reference type \"" + rType[typeIndex]
                            + "\"\n\tinvoked with the arguments: " + argList
                            + " and without the flag ObjectReference.INVOKE_NONVIRTUAL"
                            + "\"\n\tusing the debuggee object reference \"" + objRef
                            + "\" returned: " + retVal.value()
                            + " instead of 9223372036854775807L as expected");
                        tot_res = Consts.TEST_FAILED;
                    }

                    log.display("\n2) Trying to invoke the method \""
                        + meth.name() + " " + meth.signature() + " " + meth
                        + "\"\n\tgot from reference type \"" + rType[typeIndex]
                        + "\"\n\twith the arguments: " + argList
                        + " and with the flag ObjectReference.INVOKE_NONVIRTUAL"
                        + "\n\tusing the debuggee object reference \""
                        + objRef + "\" ...");
                    LongValue retVal2 = (LongValue)
                        objRef.invokeMethod(thrRef, meth, argList,
                            ObjectReference.INVOKE_NONVIRTUAL);
                    if (retVal2.value() != expValue) {
                        log.complain("TEST FAILED: wrong non-virtual invocation: the method \""
                            + meth.name() + " " + meth.signature()
                            + "\"\n\tgot from reference type \"" + rType[typeIndex]
                            + "\"\n\tinvoked with the arguments: " + argList
                            + " and with the flag ObjectReference.INVOKE_NONVIRTUAL"
                            + "\"\n\tusing the debuggee object reference \"" + objRef
                            + "\",\n\treturns: " + retVal2.value()
                            + " instead of " + expValue + " as expected");
                        tot_res = Consts.TEST_FAILED;
                    }
                    else
                        log.display("CHECK PASSED: the method invoked with ObjectReference.INVOKE_NONVIRTUAL returns: "
                             + retVal2.value() + " as expected");
                } catch (Exception ee) {
                    ee.printStackTrace();
                    log.complain("TEST FAILED: ObjectReference.invokeMethod(): caught unexpected "
                        + ee + "\n\twhen attempted to invoke the method \""
                        + meth.name() + " " + meth.signature()
                        + "\"\n\tgot from reference type \"" + rType[typeIndex]
                        + "\"\n\twith the arguments: " + argList
                        + "\"\n\tusing the debuggee object reference \""
                        + objRef + "\"");
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

    private ObjectReference findObjRef(String varName) {
        try {
            List frames = thrRef.frames();
            Iterator iter = frames.iterator();
            while (iter.hasNext()) {
                StackFrame stackFr = (StackFrame) iter.next();
                try {
                    LocalVariable locVar = stackFr.visibleVariableByName(varName);
                    if (locVar == null) continue;

                    // dummy debuggee class
                    return (ObjectReference)stackFr.getValue(locVar);
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
