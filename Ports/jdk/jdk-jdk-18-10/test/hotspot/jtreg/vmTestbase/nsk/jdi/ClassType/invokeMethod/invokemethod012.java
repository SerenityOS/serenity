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
 * The test checks that the single threaded invocation will be
 * performed properly through the JDI method<br>
 * <code>com.sun.jdi.ClassType.invokeMethod()</code>.
 * The following assertions are verified:
 * <li>only the specified thread will be resumed with the flag
 * <i>INVOKE_SINGLE_THREADED</i>
 * <li>upon completion of a single threaded invoke, the invoking
 * thread will be suspended once again; any threads started during
 * the single threaded invocation will not be suspended when the
 * invocation completes.<p>
 *
 * A debuggee part of the test starts several threads. The debugger
 * calls the JDI method with the flag <i>ClassType.INVOKE_SINGLE_THREADED</i>.
 * Then, during the invocation the debugger resumes some threads.
 * Upon completing the invocation, these resumed threads are checked
 * to be not suspended again.
 */
public class invokemethod012 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ClassType.invokeMethod.invokemethod012t";

    // debuggee source line where it should be stopped
    static final int DEBUGGEE_STOPATLINE = 68;

    // debuggee fields used to operate on an invoked method
    static final String DEBUGGEE_FIELDS[] = {
        "doExit", "isInvoked"
    };

    // debuggee threads to be check
    static final int THRDS_NUM = 12;
    static final String DEBUGGEE_THRDS[][] = {
        {"invokemethod012tMainThr", "no"},
        {"invokemethod012tThr1", "no"},
        {"invokemethod012tThr2", "resume"},
        {"invokemethod012tThr3", "resume"},
        {"invokemethod012tThr4", "no"},
        {"invokemethod012tThr5", "no"},
        {"invokemethod012tThr6", "no"},
        {"invokemethod012tThr7", "no"},
        {"invokemethod012tThr8", "resume"},
        {"invokemethod012tThr9", "no"},
        {"invokemethod012tThr10", "no"},
        {"invokemethod012tThr11", "resume"}
    };

    // debuggee method to be invoke
    static final String DEBUGGEE_METHOD =
        "dummyMeth";

    static final int ATTEMPTS = 10;
    static final int DELAY = 500; // in milliseconds

    static final String COMMAND_READY = "ready";
    static final String COMMAND_GO = "go";
    static final String COMMAND_QUIT = "quit";

    private ArgumentHandler argHandler;
    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private VirtualMachine vm;
    private volatile int tot_res = Consts.TEST_PASSED;
    private BreakpointRequest BPreq;
    private volatile boolean gotEvent = false;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new invokemethod012().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "invokemethod012t.err> ");
        debuggee.resume();
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        ThreadReference thrRef[] = new ThreadReference[THRDS_NUM];
        for (int i=0; i<THRDS_NUM; i++) {
            if ((thrRef[i] =
                    debuggee.threadByName(DEBUGGEE_THRDS[i][0])) == null) {
                log.complain("TEST FAILURE: method Debugee.threadByName() returned null for debuggee thread "
                    + DEBUGGEE_THRDS[i][0]);
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
        }

        ClassType clsType = null;
        Field fldToExit = null;
// Check the tested assersion
        try {
            // debuggee main class
            ReferenceType rType = debuggee.classByName(DEBUGGEE_CLASS);
            clsType = (ClassType) rType;
            suspendAtBP(rType, DEBUGGEE_STOPATLINE);

            // debuggee field used to force an invoked method to exit
            fldToExit = rType.fieldByName(DEBUGGEE_FIELDS[0]);
            // debuggee field to check that a method has been really invoked
            Field fldToCheck = rType.fieldByName(DEBUGGEE_FIELDS[1]);

            List methList = rType.methodsByName(DEBUGGEE_METHOD);
            if (methList.isEmpty()) {
                log.complain("TEST FAILURE: the expected debuggee method \""
                    + DEBUGGEE_METHOD
                    + "\" not found through the JDI method ReferenceType.methodsByName()");
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
            Method meth = (Method) methList.get(0);
            LinkedList<Value> argList = new LinkedList<Value>();
            argList.add(vm.mirrorOf(9223372036854775807L));

            Wicket invThrWicket = new Wicket();

            // the single threaded invocation
            InvokingThread invThr = new InvokingThread(clsType, thrRef[0], meth,
                         argList, ClassType.INVOKE_SINGLE_THREADED, invThrWicket);

            invThr.setDaemon(true);
            invThr.start();

            // wait for invThr to be started
            log.display("Waiting for invThr thread to be started ...");
            invThrWicket.waitFor();

            log.display("Waiting for debuggee method invocation ...");
            int tryOns = 0;
            BooleanValue val = null;
            do {
                if (tryOns > ATTEMPTS)
                    throw new Failure("unable to continue testing after "
                        + ATTEMPTS
                        + " attempts: debuggee method is not invoked yet");

                // reliable analogue of Thread.yield()
                synchronized(this) {
                    this.wait(DELAY);
                }
                val = (BooleanValue)
                    clsType.getValue(fldToCheck);
                tryOns++;
            } while (!val.value());

            // check threads during the method invocation
            checkThreads(thrRef, 1);

            // resume some threads during the method invocation
            resumeSomeThreads(thrRef);

            clsType.setValue(fldToExit, vm.mirrorOf(true));
            invThr.join(argHandler.getWaitTime()*60000);
            log.display("Thread \"" + invThr.getName() + "\" done");

            // check threads status after the method invocation
            checkThreads(thrRef, 0);
        } catch (Exception e) {
            e.printStackTrace();
            // force an method to exit if it has been invoked
            if (clsType != null && fldToExit != null) {
                try {
                    clsType.setValue(fldToExit, vm.mirrorOf(true));
                } catch(Exception ee) {
                    ee.printStackTrace();
                }
            }
            log.complain("TEST FAILURE: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
        }

// Finish the test
        return quitDebuggee();
    }

    private void checkThreads(ThreadReference thrRef[], int state) {
        switch (state) {
        case 0:
            log.display("\nVerifying threads status when the invocation completes:");
            break;
        case 1:
            log.display("\nVerifying the single threaded invocation:");
            break;
        }

        // check an invoking debuggee thread
        if (thrRef[0].isSuspended()) {
            if (state == 0) { // after invocation
                log.display("CHECK PASSED: invoking debuggee thread " + thrRef[0]
                    + "\n\tis suspended again after the invocation as expected");
            } else {
                log.complain("TEST FAILED: wrong invocation: "
                    + "\n\tinvoking debuggee thread " + thrRef[0] + "\n\tis still suspended");
                tot_res = Consts.TEST_FAILED;
            }
        } else {
            if (state == 0) { // after invocation
                log.complain("TEST FAILED: wrong invocation: "
                    + "\n\tinvoking debuggee thread " + thrRef[0]
                    + "\n\tis not suspended again after the invocation");
                tot_res = Consts.TEST_FAILED;
            } else {
                log.display("CHECK PASSED: invoking debuggee thread " + thrRef[0]
                    + "\n\tis resumed as expected");
            }
        }
        // check other debuggee threads
        for (int i=1; i<THRDS_NUM; i++) {
            switch (state) {
            case 0: // threads status after method invocation
                if (DEBUGGEE_THRDS[i][1].equals("resume")) { // there was a resumption
                    if (thrRef[i].isSuspended()) {
                        log.complain("TEST FAILED: wrong single threaded invocation (INVOKE_SINGLE_THREADED):"
                            + "\n\tnon-invoking debuggee thread "
                            + thrRef[i]
                            + "\n\tstarted during the invocation, is suspended when it completes");
                        tot_res = Consts.TEST_FAILED;
                    } else {
                        log.display("CHECK PASSED: non-invoking debuggee thread "
                            + thrRef[i]
                            + "\n\tstarted during the invocation, is not suspended when it completes as expected");
                    }
                } else { // there was no resumption
                    if (thrRef[i].isSuspended()) {
                        log.display("CHECK PASSED: non-invoking debuggee thread "
                            + thrRef[i]
                            + "\n\tis suspended when the invocation completes as expected");
                    } else {
                        log.complain("TEST FAILED: wrong single threaded invocation (INVOKE_SINGLE_THREADED):"
                            + "\n\tnon-invoking debuggee thread " + thrRef[i]
                            + "\n\tis not suspended when the invocation completes");
                        tot_res = Consts.TEST_FAILED;
                    }
                }
                break;
            case 1: // the single threaded invocation
                if (thrRef[i].isSuspended()) {
                    log.display("CHECK PASSED: non-invoking debuggee thread " + thrRef[i]
                        + "\n\tis suspended as expected");
                } else {
                    log.complain("TEST FAILED: wrong single threaded invocation (INVOKE_SINGLE_THREADED):"
                        + "\n\tnon-invoking debuggee thread "
                        + thrRef[i] + "\n\tis not suspended");
                    tot_res = Consts.TEST_FAILED;
                }
                break;
            }
        }
    }

    private void resumeSomeThreads(ThreadReference thrRef[])
            throws InterruptedException {
        // resume some dummy debuggee threads
        for (int i=1; i<THRDS_NUM; i++) {
            if (DEBUGGEE_THRDS[i][1].equals("resume")) { // do resume
                log.display("Resuming the debuggee thread "
                    + thrRef[i] + " ...");
                thrRef[i].resume();

                // wait for the thread resumption
                int tryOns = 0;
                do {
                    if (tryOns > ATTEMPTS)
                        throw new Failure("unable to continue testing after "
                            + ATTEMPTS
                            + " attempts: debuggee thread "
                            + thrRef[i] + " is not resumed yet");

                    // reliable analogue of Thread.yield()
                    synchronized(this) {
                        this.wait(DELAY);
                    }
                    tryOns++;
                } while (thrRef[i].isSuspended());
                log.display("The thread " + thrRef[i]
                    + "\n\tis resumed");
            }
        }
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

    /**
     * A separate thread class used in debuggee method invocation because
     * it is synchronous.
     */
    class InvokingThread extends Thread {
        ClassType clsRef;
        ThreadReference thrRef;
        Method meth;
        LinkedList<Value> argList;
        int bitOpts;
        Wicket wicket;

        InvokingThread(ClassType clsRef, ThreadReference thrRef, Method meth,
                LinkedList<Value> argList, int bitOpts, Wicket wicket) {
            this.clsRef = clsRef;
            this.thrRef = thrRef;
            this.meth = meth;
            this.argList = argList;
            this.bitOpts = bitOpts;
            this.wicket = wicket;
            super.setName("InvokingThread");
        }

        public void run() {
            wicket.unlock();

            try {
                log.display("\nInvokingThread: trying to invoke the method \""
                    + meth.name() + " " + meth.signature() + " " + meth
                    + "\"\n\twith the arguments: " + argList
                    + "\"\n\tand " +
                    new String((bitOpts==ClassType.INVOKE_SINGLE_THREADED)? "with" : "without")
                    + " the flag INVOKE_SINGLE_THREADED\n\tusing the debuggee class \""
                    + clsRef + "\" ...");

                LongValue retVal = (LongValue)
                    clsRef.invokeMethod(thrRef, meth, argList, bitOpts);
                log.display("InvokingThread: the method returned " + retVal);
            } catch (Exception e) {
                e.printStackTrace();
                tot_res = Consts.TEST_FAILED;
                log.complain("TEST FAILED: caught unexpected exception: " + e);
            }
            log.display("InvokingThread: exiting");
        }
    }
/////////////////////////////////////////////////////////////////////////////
}
