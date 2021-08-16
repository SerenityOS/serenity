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
 * The test checks that invocation with all threads resumption
 * and the single threaded one will be performed properly through
 * the JDI method<br>
 * <code>com.sun.jdi.ObjectReference.invokeMethod()</code>.
 * The following assertions are verified:
 * <li>by default, all threads in the target VM are resumed while
 * the method is being invoked
 * <li>when the invocation completes, all threads in the target VM
 * are suspended, regardless their state before the invocation
 * <li>only the specified thread will be resumed with the
 * <i>INVOKE_SINGLE_THREADED</i>
 * <li>upon completion of a single threaded invoke, the invoking
 * thread will be suspended once again.<br>
 *
 * A debuggee part of the test starts several threads. Then debugger
 * calls the JDI method with and without the flag
 * <i>ObjectReference.INVOKE_SINGLE_THREADED</i> sequentially. During
 * the invocation and after it the threads state is expected to be as
 * described above.
 */
public class invokemethod011 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ObjectReference.invokeMethod.invokemethod011t";

    // debuggee source line where it should be stopped
    static final int DEBUGGEE_STOPATLINE = 71;

    // debuggee local var used to find needed stack frame
    static final String DEBUGGEE_LOCALVAR =
        "invokemethod011tdummyCls";

    // debuggee fields used to operate on an invoked method
    static final String DEBUGGEE_FIELDS[] = {
        "doExit", "isInvoked"
    };

    // debuggee thread names to be check
    static final int THRDS_NUM = 12;
    static final String DEBUGGEE_THRDS[] = {
        "invokemethod011tThr", "invokemethod011tThr1", "invokemethod011tThr2",
        "invokemethod011tThr3", "invokemethod011tThr4", "invokemethod011tThr5",
        "invokemethod011tThr6", "invokemethod011tThr7", "invokemethod011tThr8",
        "invokemethod011tThr9", "invokemethod011tThr10", "invokemethod011tThr11"
    };

    // debuggee method to be invoke
    static final String DEBUGGEE_METHOD =
        "longMeth";

    static final int DELAY = 400; // in milliseconds

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
        return new invokemethod011().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        String cmd;
        ObjectReference objRef = null;
        Field fldToExit = null;
        BooleanValue val = null;
        InvokingThread invThr = null;
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
                    debuggee.threadByName(DEBUGGEE_THRDS[i])) == null) {
                log.complain("TEST FAILURE: method Debugee.threadByName() returned null for debuggee thread "
                    + DEBUGGEE_THRDS[i]);
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
        }
        ReferenceType[] rType = new ReferenceType[2];
        // debuggee main class
        rType[0] = debuggee.classByName(DEBUGGEE_CLASS);

// Check the tested assersion
        try {
            suspendAtBP(rType[0], DEBUGGEE_STOPATLINE);
            objRef = findObjRef(thrRef[0], DEBUGGEE_LOCALVAR);
            // debuggee dummy class
            rType[1] = objRef.referenceType();
            // debuggee field used to force an invoked method to exit
            fldToExit = rType[1].fieldByName(DEBUGGEE_FIELDS[0]);
            // debuggee field to check that a method has been really invoked
            Field fldToCheck = rType[1].fieldByName(DEBUGGEE_FIELDS[1]);

            List<Method> methList = rType[1].methodsByName(DEBUGGEE_METHOD);
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

            for (int i=2; i>=1; i--) {
                switch(i) {
                case 1: // invocation with all threads resumption
                    invThr = new InvokingThread(objRef, thrRef[0],
                                 meth, argList, 0);
                    break;
                case 2: // the single threaded invocation
                    invThr = new InvokingThread(objRef, thrRef[0], meth,
                                 argList, ObjectReference.INVOKE_SINGLE_THREADED);
                    break;
                }
                invThr.setDaemon(true);
                invThr.start();

                log.display("Waiting for debuggee method invocation ...");
                int tryOns = 0;
                int attempts = (argHandler.getWaitTime() * 60000) / DELAY;

                do {
                    if (tryOns > attempts)
                        throw new Failure("unable to continue testing after "
                            + attempts
                            + " attempts: debuggee method is not invoked yet");

                    // reliable analogue of Thread.yield()
                    synchronized(this) {
                        this.wait(DELAY);
                    }
                    val = (BooleanValue)
                        objRef.getValue(fldToCheck);
                    tryOns++;
                } while (!val.value());

                // check threads during method invocation
                checkThreads(thrRef, i);

                objRef.setValue(fldToExit, vm.mirrorOf(true));

                invThr.join(argHandler.getWaitTime()*60000);
                log.display("Thread \"" + invThr.getName() + "\" done");

                // check threads status after method invocation
                checkThreads(thrRef, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // force an method to exit if it has been invoked
            if (objRef != null && fldToExit != null) {
                try {
                    objRef.setValue(fldToExit, vm.mirrorOf(true));
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
            log.display("\n\nVerifying threads status after the method invocation:");
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

    private ObjectReference findObjRef(ThreadReference thrRef, String varName) {
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
//                    BPreq.setSuspendPolicy(EventRequest.SUSPEND_ALL); // default
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
        ObjectReference objRef;
        ThreadReference thrRef;
        Method meth;
        LinkedList<Value> argList;
        int bitOpts;

        InvokingThread(ObjectReference objRef, ThreadReference thrRef, Method meth, LinkedList<Value> argList, int bitOpts) {
            this.objRef = objRef;
            this.thrRef = thrRef;
            this.meth = meth;
            this.argList = argList;
            this.bitOpts = bitOpts;
            super.setName("InvokingThread");
        }

        public void run() {
            try {
                log.display("\nInvokingThread: trying to invoke the method \""
                    + meth.name() + " " + meth.signature() + " " + meth
                    + "\"\n\twith the arguments: " + argList
                    + "\"\n\tand " +
                    new String((bitOpts==ObjectReference.INVOKE_SINGLE_THREADED)? "with" : "without")
                    + " the flag ObjectReference.INVOKE_SINGLE_THREADED\n\tusing the debuggee object reference \""
                    + objRef + "\" ...");

                LongValue retVal = (LongValue)
                    objRef.invokeMethod(thrRef, meth, argList, bitOpts);
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
