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

package nsk.jdi.StackFrame.getValues;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method:<br>
 * <code>com.sun.jdi.StackFrame.getValues()</code><br>
 * properly throws <i>IllegalArgumentException</i> - if
 * specified variable is invalid for this frame's method.<p>
 *
 * The test works as follows. The target VM executes two debuggee
 * threads: <i>getvalues003tMainThr</i> and <i>getvalues003tAuxThr</i>.
 * Debugger part tries to provoke the exception by getting values of
 * the local variables in stack frame obtained from the
 * <i>getvalue0s03tMainThr</i> thread, and one among them obtained
 * from the <i>getvalues003tAuxThr</i> thread.
 */
public class getvalues003 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.StackFrame.getValues.getvalues003t";

    // names of debuggee threads
    static final String DEBUGGEE_THRDNAMES[] = {
        "getvalues003tMainThr", "getvalues003tAuxThr"
    };

    // tested debuggee local vars
    static final int VAR_NUM = 9;
    static final String DEBUGGEE_VARS[] = {
        "getvalues003tFindMe", "shortVar", "intVar",
        "longVar", "floatVar", "doubleVar", "charVar",
        "booleanVar", "strVar"
    };

    // debuggee source line where it should be stopped
    static final int DEBUGGEE_STOPATLINE = 78;

    static final int DELAY = 500; // in milliseconds

    static final String COMMAND_READY = "ready";
    static final String COMMAND_GO = "go";
    static final String COMMAND_QUIT = "quit";

    private ArgumentHandler argHandler;
    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private VirtualMachine vm;
    private BreakpointRequest BPreq;
    private volatile int tot_res = Consts.TEST_PASSED;
    private volatile boolean gotEvent = false;
    private List<com.sun.jdi.LocalVariable> locVars;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new getvalues003().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "getvalues003t.err> ");
        debuggee.resume();
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        ThreadReference thrRef[] = new ThreadReference[2];
        for (int i=0; i<2; i++)
            if ((thrRef[i] =
                    debuggee.threadByName(DEBUGGEE_THRDNAMES[i])) == null) {
                log.complain("TEST FAILURE: method Debugee.threadByName() returned null for debuggee thread "
                    + DEBUGGEE_THRDNAMES[i]);
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }

        try {
            // debuggee main class
            ReferenceType rType = debuggee.classByName(DEBUGGEE_CLASS);

            suspendAtBP(rType, DEBUGGEE_STOPATLINE);

            // get a stack frame which belongs to the "getvalue003tMainThr" thread
            StackFrame stFrame = findFrame(thrRef[0], DEBUGGEE_VARS[0], true);

            // store a LocalVariable which belongs to the "getvalue003tAuxThr" thread
            StackFrame wrongStFrame = findFrame(thrRef[1], DEBUGGEE_VARS[0], false);

            StringBuffer varNames = new StringBuffer();
            Iterator varIter = locVars.iterator();
            while (varIter.hasNext()) {
                LocalVariable locv = (LocalVariable) varIter.next();
                varNames = varNames.append("\n\t\t" + locv.typeName()
                    + " " + locv.name() + " " + locv.signature());
            }

            for (int i=0; i<VAR_NUM; i++) {
                LocalVariable wrongLocVar =
                    wrongStFrame.visibleVariableByName(DEBUGGEE_VARS[i]);
                // visible variable with the given name is found
                if (wrongLocVar != null)
                    locVars.add(wrongLocVar);
                else throw new Failure("needed local variable "
                    + DEBUGGEE_VARS[i] + " not found");

                log.display("\n" + (i+1)
                    + ") Trying to get values of local variables:"
                    + varNames
                    + "\n\tgotten from thread \"" + thrRef[0]
                    + "\"\n\tand wrongly, of local variable\n\t\t"
                    + wrongLocVar.typeName() + " " + wrongLocVar.name()
                    + " " + wrongLocVar.signature()
                    + "\n\tgotten from thread \"" + thrRef[1]
                    + "\"\n\tusing stack frame \"" + stFrame
                    + "\"\n\tin the thread \"" + thrRef[0]
                    + "\" ...");

                // Check the tested assersion
                try {
                    Map valMap = stFrame.getValues(locVars);
                    log.complain("TEST FAILED: expected IllegalArgumentException was not thrown"
                        + "\n\twhen attempted to get values of local variables:"
                        + varNames
                        + "\"\n\tgotten from thread \"" + thrRef[0]
                        + "\"\n\tand wrongly, of local variable\n\t\t"
                        + wrongLocVar.typeName() + " " + wrongLocVar.name()
                        + " " + wrongLocVar.signature()
                        + "\n\tgotten from thread \"" + thrRef[1]
                        + "\"\n\tusing stack frame \"" + stFrame
                        + "\"\n\tin the thread \"" + thrRef[0] + "\"");
                    tot_res = Consts.TEST_FAILED;
                } catch(IllegalArgumentException ee) {
                    log.display("CHECK PASSED: caught expected " + ee);
                } catch(Exception ue) {
                    ue.printStackTrace();
                    log.complain("TEST FAILED: StackFrame.getValue(): caught unexpected "
                        + ue + "\n\tinstead of IllegalArgumentException"
                        + "\n\twhen attempted to get values of local variables:"
                        + varNames
                        + "\"\n\tgotten from thread \"" + thrRef[0]
                        + "\"\n\tand wrongly, of local variable\n\t\t"
                        + wrongLocVar.typeName() + " " + wrongLocVar.name()
                        + " " + wrongLocVar.signature()
                        + "\"\n\tgotten from thread \"" + thrRef[1]
                        + "\"\n\tusing stack frame \"" + stFrame
                        + "\"\n\tin the thread \"" + thrRef[0] + "\"");
                    tot_res = Consts.TEST_FAILED;
                }

                locVars.remove(wrongLocVar);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
        }

        return quitDebuggee();
    }

    private StackFrame findFrame(ThreadReference thrRef,
                String varName, boolean getLocVars) {
        try {
            List frames = thrRef.frames();
            Iterator iter = frames.iterator();
            while (iter.hasNext()) {
                StackFrame stackFr = (StackFrame) iter.next();
                try {
                    LocalVariable locVar =
                        stackFr.visibleVariableByName(varName);
                    // visible variable with the given name is found
                    if (locVar != null) {
                        if (getLocVars) // get list of local variables as well
                            locVars = stackFr.visibleVariables();

                        return stackFr;
                    }
                } catch(AbsentInformationException e) {
                    // this is not needed stack frame, ignoring
                } catch(NativeMethodException ne) {
                    // current method is native, also ignoring
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            tot_res = Consts.TEST_FAILED;
            throw new Failure("findFrame: caught unexpected exception: " + e);
        }
        throw new Failure("findFrame: needed debuggee stack frame not found");
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

        /**
         * This is a class containing a critical section which may lead to time
         * out of the test.
         */
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
        log.display("Final resumption of debuggee VM");
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
