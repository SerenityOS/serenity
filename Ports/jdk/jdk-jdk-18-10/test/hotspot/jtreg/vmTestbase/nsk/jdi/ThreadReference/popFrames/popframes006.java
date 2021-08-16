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

package nsk.jdi.ThreadReference.popFrames;

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
 * <code>com.sun.jdi.ThreadReference.popFrames()</code><br>
 * properly throws <i>IncompatibleThreadStateException</i> or
 * <i>InvalidStackFrameException</i>, if specified thread is resumed.
 *
 * <p>Note, the test is pending decision on the bug:<p>
 *
 * 4512840 JDI spec: for ThreadReference.popFrame() needs clarification<p>
 *
 * Actually, only one exception should be thrown in case of resumed
 * thread instead of two ones.
 */
public class popframes006 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ThreadReference.popFrames.popframes006t";

    // name of debuggee's main thread
    static final String DEBUGGEE_THRNAME = "popframes006tThr";
    // debuggee local var used to find needed stack frame
    static final String DEBUGGEE_LOCALVAR = "popframes006tFindMe";
    // debuggee field used to indicate that popping has been done
    static final String DEBUGGEE_FIELD = "wasPopped";

    // debuggee source line where it should be stopped
    static final int DEBUGGEE_STOPATLINE = 80;

    static final int ATTEMPTS = 5;
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
    private ObjectReference objRef;
    private volatile int tot_res = Consts.TEST_PASSED;
    private volatile boolean gotEvent = false;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new popframes006().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "popframes006t.err> ");
        debuggee.resume();
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        ThreadReference thrRef = null;
        if ((thrRef =
                debuggee.threadByName(DEBUGGEE_THRNAME)) == null) {
            log.complain("TEST FAILURE: method Debugee.threadByName() returned null for debuggee thread "
                + DEBUGGEE_THRNAME);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        Field doExit = null;
        try {
            // debuggee main class
            ReferenceType rType = debuggee.classByName(DEBUGGEE_CLASS);

            suspendAtBP(rType, DEBUGGEE_STOPATLINE);

            // debuggee field used to indicate that popping has been done
            doExit = rType.fieldByName(DEBUGGEE_FIELD);

            // debuggee stack frame to be popped
            StackFrame stFrame = findFrame(thrRef, DEBUGGEE_LOCALVAR);

            log.display("\nTrying to pop stack frame \"" + stFrame
                + "\"\n\tlocation \"" + stFrame.location()
                + "\"\n\tgot from thread reference \"" + thrRef
                + "\"\n\twith resumed debuggee thread ...");

                log.display("Resuming debuggee ...");
                vm.resume();

                // wait for the thread resumption
                int num = 0;
                while (thrRef.isSuspended()) {
                    if (num > ATTEMPTS)
                        throw new Failure("unable to continue testing after "
                            + ATTEMPTS
                            + " attempts: debuggee thread "
                            + thrRef + " is not resumed yet");

                    Thread.currentThread().sleep(DELAY);
                    num++;
                }
                log.display("Debugee is resumed");

// Check the tested assersion
            try {
                thrRef.popFrames(stFrame);
                log.complain("TEST FAILED: expected IncompatibleThreadStateException or InvalidStackFrameException was not thrown"
                    + "\n\twhen attempted to pop stack frame got from thread reference \""
                    + thrRef + "\"\n\twith resumed debuggee thread");
                tot_res = Consts.TEST_FAILED;
            // actually, it should be only one expected exception instead of two ones:
            // see the bug 4512840
            } catch(IncompatibleThreadStateException ee) {
                log.display("CHECK PASSED: caught expected " + ee);
            } catch(InvalidStackFrameException ee2) {
                log.display("CHECK PASSED: caught expected " + ee2);
            } catch(UnsupportedOperationException une) {
                if (vm.canPopFrames()) {
                    une.printStackTrace();
                    log.complain("TEST FAILED: caught exception: " + une
                        + "\n\tHowever, VirtualMachine.canPopFrames() shows, that the target VM"
                        + "\n\tdoes support popping frames of a threads stack: "
                        + vm.canPopFrames());
                    tot_res = Consts.TEST_FAILED;
                } else {
                    log.display("Warinig: unable to test an assertion: caught exception: " + une
                        + "\n\tand VirtualMachine.canPopFrames() shows, that the target VM"
                        + "\n\tdoes not support popping frames of a threads stack as well: "
                        + vm.canPopFrames());
                }

            } catch(Exception ue) {
                ue.printStackTrace();
                log.complain("TEST FAILED: ThreadReference.popFrames(): caught unexpected "
                    + ue + "\n\tinstead of IncompatibleThreadStateException or InvalidStackFrameException"
                    + "\n\twhen attempted to pop stack frame got from thread reference \""
                    + thrRef + "\"\n\twith resumed debuggee thread");
                tot_res = Consts.TEST_FAILED;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
        } finally {
// Finish the test
            // force an method to exit
            if (objRef != null && doExit != null) {
                try {
                    objRef.setValue(doExit, vm.mirrorOf(true));
                } catch(Exception sve) {
                    sve.printStackTrace();
                }
            }
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
                    if (locVar != null) {
                        objRef = (ObjectReference)
                            stackFr.getValue(locVar);
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
