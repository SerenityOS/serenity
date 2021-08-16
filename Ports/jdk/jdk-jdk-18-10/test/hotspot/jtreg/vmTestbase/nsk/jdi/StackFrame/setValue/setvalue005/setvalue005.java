/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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


/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jdi/StackFrame/setValue/setvalue005.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that the JDI method:
 *         com.sun.jdi.StackFrame.setValue()
 *     does not throw ClassNotLoadedException when a debugger part of
 *     the test attempts to set null value for a debuggee local variable
 *     which type has not yet been loaded through the appropriate class loader.
 *     The test works as follows. The debuggee part has two local
 *     variables: "dummyVar" of non-loaded type "DummyType" and
 *     "finDummyVar" of non-loaded type "FinDummyType".
 *     Debugger part tries to provoke the exception by setting values of
 *     these variables. The test makes sure that class has not been loaded
 *     by the debuggee VM through the JDI method "VirtualMachine.classesByName()"
 *     which should return list of loaded classes only.
 * COMMENTS
 *     The test was fixed due to the following bug:
 *     4735268 TEST_BUG: some jdi tests expect ClassNotLoadedException for
 *             null values
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StackFrame.setValue.setvalue005.setvalue005
 *        nsk.jdi.StackFrame.setValue.setvalue005.setvalue005t
 *
 * @comment make sure setvalue005t is compiled with full debug info
 * @clean nsk.jdi.StackFrame.setValue.setvalue005.setvalue005t
 * @compile -g:lines,source,vars setvalue005t.java
 *
 * @run main/othervm
 *      nsk.jdi.StackFrame.setValue.setvalue005.setvalue005
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.StackFrame.setValue.setvalue005;

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
 * <code>com.sun.jdi.StackFrame.setValue()</code><br>
 * does not throw <i>ClassNotLoadedException</i> when a debugger part
 * of the test attempts to set null value for a debuggee local variable
 * which type has not yet been loaded through the appropriate class
 * loader.<p>
 *
 * The test works as follows. The debuggee part has two local
 * variables: <i>dummyVar</i> of non-loaded type <i>DummyType</i> and
 * <i>finDummyVar</i> of non-loaded type <i>FinDummyType</i>.
 * Debugger part tries to provoke the exception by setting values of
 * these variables. The test makes sure that class has not been loaded
 * by the debuggee VM through the JDI method <i>VirtualMachine.classesByName()</i>
 * which should return list of loaded classes only.
 */
public class setvalue005 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.StackFrame.setValue.setvalue005.setvalue005t";

    // names of debuggee threads
    static final String DEBUGGEE_THRDNAME =
        "setvalue005tMainThr";

    // tested debuggee local vars and theirs types
    static final int VAR_NUM = 3;
    static final String DEBUGGEE_VARS[][] = {
        {"setvalue005tFindMe", "IGNORE MY TYPE"},
        {"dummyVar", "nsk.jdi.StackFrame.setValue.setvalue005.DummyType"},
        {"finDummyVar", "nsk.jdi.StackFrame.setValue.setvalue005.FinDummyType"}
    };

    // debuggee source line where it should be stopped
    static final int DEBUGGEE_STOPATLINE = 67;

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

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new setvalue005().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "setvalue005t.err> ");
        debuggee.resume();
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        ThreadReference thrRef = null;
        if ((thrRef =
                debuggee.threadByName(DEBUGGEE_THRDNAME)) == null) {
            log.complain("TEST FAILURE: method Debugee.threadByName() returned null for debuggee thread "
                + DEBUGGEE_THRDNAME);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        try {
            // debuggee main class
            ReferenceType rType = debuggee.classByName(DEBUGGEE_CLASS);

            suspendAtBP(rType, DEBUGGEE_STOPATLINE);

            // find a stack frame which belongs to the "setvalue005tMainThr" thread
            StackFrame stFrame = findFrame(thrRef, DEBUGGEE_VARS[0][0]);

            for (int i=1; i<VAR_NUM; i++) {
                LocalVariable locVar =
                    stFrame.visibleVariableByName(DEBUGGEE_VARS[i][0]);
                // visible variable with the given name is not found
                if (locVar == null)
                    throw new Failure("needed local variable "
                        + DEBUGGEE_VARS[i][0] + " not found");

                // Make sure that the reference type is not loaded by the debuggee VM
                if ((debuggee.classByName(DEBUGGEE_VARS[i][1])) != null) {
                    log.display("Skipping the check: the tested reference type\n\t\t\""
                        + DEBUGGEE_VARS[i][1]
                        + "\"\n\twas loaded by the debuggee VM, unable to test an assertion");
                    continue;
                }

                log.display("\n" + i
                    + ") Trying to set null value for the local variable: "
                    + locVar.typeName() + " " + locVar.name()
                    + " " + locVar.signature()
                    + "\n\tusing stack frame \"" + stFrame
                    + "\"\n\tin the thread \"" + thrRef
                    + "\" ...");

                // Check the tested assersion
                try {
                    stFrame.setValue(locVar, null);

                    log.display("CHECK PASSED: ClassNotLoadedException was not thrown as expected"
                        + "\n\twhen attempted to set null value for the local variable: "
                        + locVar.typeName() + " " + locVar.name()
                        + " " + locVar.signature()
                        + "\n\tusing stack frame \"" + stFrame
                        + "\"\n\tin the thread \"" + thrRef + "\"");
                } catch(ClassNotLoadedException ee) {
                    log.complain("TEST FAILED: " + ee + " was thrown"
                        + "\n\twhen attempted to set null value for the local variable: "
                        + locVar.typeName() + " " + locVar.name()
                        + " " + locVar.signature()
                        + "\n\tusing stack frame \"" + stFrame
                        + "\"\n\tin the thread \"" + thrRef + "\"");
                    tot_res = Consts.TEST_FAILED;
                } catch(Exception ue) {
                    ue.printStackTrace();
                    log.complain("TEST FAILED: caught unexpected "
                        + ue + "\n\twhen attempted to set null value for the local variable: "
                        + locVar.typeName() + " " + locVar.name()
                        + " " + locVar.signature()
                        + "\n\tusing stack frame \"" + stFrame
                        + "\"\n\tin the thread \"" + thrRef + "\"");
                    tot_res = Consts.TEST_FAILED;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
        }

        return quitDebuggee();
    }

    private StackFrame findFrame(ThreadReference thrRef, String varName) {
        try {
            List frames = thrRef.frames();
            Iterator iter = frames.iterator();
            while (iter.hasNext()) {
                StackFrame stackFr = (StackFrame) iter.next();
                try {
                    LocalVariable locVar =
                        stackFr.visibleVariableByName(varName);
                    // visible variable with the given name is found
                    if (locVar != null)
                        return stackFr;
                } catch(AbsentInformationException e) {
                    // this is not needed stack frame, ignoring
                } catch(NativeMethodException ne) {
                    // current method is native, also ignoring
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            tot_res = Consts.TEST_FAILED;
            throw new Failure("findFrame: caught unexpected exception: "
                + e);
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
        log.display("\nFinal resumption of debuggee VM");
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
