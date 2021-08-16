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
 * The test checks that invocation with all threads resumption
 * and the single threaded one will be performed properly through
 * the JDI method<br>
 * <code>com.sun.jdi.ClassType.invokeMethod()</code>.
 * The following assertions are verified:
 * <li>by default, all threads in the target VM are resumed while
 * the method is being invoked
 * <li>when the invocation completes, all threads in the target VM
 * are suspended, regardless their state before the invocation
 * <li>only the specified thread will be resumed with the flag
 * <i>INVOKE_SINGLE_THREADED</i>
 * <li>upon completion of a single threaded invoke, the invoking
 * thread will be suspended once again.<p>
 *
 * A debuggee part of the test starts several threads. Then debugger
 * calls the JDI method without and with the flag <i>INVOKE_SINGLE_THREADED</i>
 * sequentially. During the invocations and after them the threads state
 * is expected to be as described above.
 */
public class invokemethod011 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ClassType.invokeMethod.invokemethod011t";

    // debuggee source line where it should be stopped
    static final int DEBUGGEE_STOPATLINE = 66;

    // debuggee thread names to be check
    static final int THRDS_NUM = 12;
    static final String DEBUGGEE_THRNAMES[] = {
        "invokemethod011tMainThr", "invokemethod011tThr1", "invokemethod011tThr2",
        "invokemethod011tThr3", "invokemethod011tThr4", "invokemethod011tThr5",
        "invokemethod011tThr6", "invokemethod011tThr7", "invokemethod011tThr8",
        "invokemethod011tThr9", "invokemethod011tThr10", "invokemethod011tThr11"
    };

    // tested debuggee method
    static final String DEBUGGEE_METHOD =
        "dummyMeth";

    // debuggee fields used to operate on an invoked method
    static final String DEBUGGEE_FIELDS[] = {
        "doExit", "isInvoked"
    };

    static final String COMMAND_READY = "ready";
    static final String COMMAND_GO = "go";
    static final String COMMAND_QUIT = "quit";

    static final int ATTEMPTS = 10;
    static final int DELAY = 1000; // in milliseconds

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
        return new invokemethod011().runIt(argv, out);
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
        debuggee.redirectStderr(log, "invokemethod011t.err> ");
        debuggee.resume();
        cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        ThreadReference thrRef[] = new ThreadReference[THRDS_NUM];
        for (int i=0; i<THRDS_NUM; i++) {
            if ((thrRef[i] =
                   debuggee.threadByName(DEBUGGEE_THRNAMES[i])) == null) {
                log.complain("TEST FAILURE: method Debugee.threadByName() returned null for debuggee thread "
                    + DEBUGGEE_THRNAMES[i]);
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
        }

        ClassType clsType = null;
        Field fldToExit = null;
        try {
            // debuggee main class
            ReferenceType rType = debuggee.classByName(DEBUGGEE_CLASS);
            clsType = (ClassType) rType;
            suspendAtBP(rType, DEBUGGEE_STOPATLINE);

            // debuggee field used to force an invoked method to exit
            fldToExit = rType.fieldByName(DEBUGGEE_FIELDS[0]);
            // debuggee field to check that a method has been really invoked
            Field fldToCheck = rType.fieldByName(DEBUGGEE_FIELDS[1]);
            // debuggee method to be invoked
            List<Method> methList = rType.methodsByName(DEBUGGEE_METHOD);

            if (methList.isEmpty()) {
                log.complain("TEST FAILURE: the expected debuggee method \""
                    + DEBUGGEE_METHOD
                    + "\" not found through the JDI method ReferenceType.methodsByName()");
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }

            Method meth = methList.get(0);
            LinkedList<Value> argList = new LinkedList<Value>();
            argList.add(vm.mirrorOf(9223372036854775807L));

            InvokingThread invThr = null;
            Wicket invThrWicket = null;
            BooleanValue val = null;

            // Check the tested assersion
            for (int i=1; i<=2; i++)
            {
                invThrWicket = new Wicket();

                switch(i) {
                case 1: // invocation with all threads resumption
                    invThr = new InvokingThread(clsType, thrRef[0], meth,
                                 argList, 0, invThrWicket);
                    break;
                case 2: // the single threaded invocation
                    invThr = new InvokingThread(clsType, thrRef[0], meth,
                                 argList, ClassType.INVOKE_SINGLE_THREADED, invThrWicket);
                    break;
                }
                invThr.setDaemon(true);
                invThr.start();

                // wait for invThr to be started
                log.display("Waiting for invThr thread to be started ...");
                invThrWicket.waitFor();

                log.display("Waiting for debuggee method invocation ...");
                int tryOns = 0;
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

                // check threads during method invocation
                checkThreads(thrRef, i);

                clsType.setValue(fldToExit, vm.mirrorOf(true));

                invThr.join(argHandler.getWaitTime()*60000);
                log.display("Thread \"" + invThr.getName() + "\" done");

                // check threads status after method invocation
                checkThreads(thrRef, 0);
            }

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
            log.display("\n\nVerifying threads status after method invocation:");
            break;
        case 1:
            log.display("\n\nVerifying invocation with all threads resumption:");
            break;
        case 2:
            log.display("\n\nVerifying the single threaded invocation (INVOKE_SINGLE_THREADED):");
            break;
        }

        // check an invoking debuggee thread
        if (thrRef[0].isSuspended()) {
            if (state == 0) { // after invocation
                log.display("CHECK PASSED: invoking debuggee thread " + thrRef[0]
                    + "\n\tis suspended again after invocation as expected");
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
            case 1: // invocation with all threads resumption
                if (thrRef[i].isSuspended()) {
                    log.complain("TEST FAILED: wrong invocation with all threads resumption: "
                        + "\n\tnon-invoking debuggee thread " + thrRef[i] + "\n\tis suspended");
                    tot_res = Consts.TEST_FAILED;
                } else {
                    log.display("CHECK PASSED: non-invoking debuggee thread " + thrRef[i]
                        + "\n\tis resumed as expected");
                }
                break;
            case 0: // threads status after method invocation
            case 2: // the single threaded invocation
                if (thrRef[i].isSuspended()) {
                    log.display("CHECK PASSED: non-invoking debuggee thread " + thrRef[i]
                        + "\n\tis suspended as expected");
                } else {
                    log.complain("TEST FAILED: wrong " +
                        new String((state==2)? "single threaded invocation (INVOKE_SINGLE_THREADED)" : " threads state after invocation")
                        + ":\n\tnon-invoking debuggee thread " + thrRef[i] + "\n\tis not suspended");
                    tot_res = Consts.TEST_FAILED;
                }
                break;
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
