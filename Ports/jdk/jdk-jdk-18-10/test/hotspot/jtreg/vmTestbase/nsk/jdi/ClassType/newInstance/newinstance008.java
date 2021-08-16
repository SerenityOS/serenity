/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ClassType.newInstance;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 *
 * The test checks up the method com.sun.jdi.ClassType.newInstance()
 *
 * Test checks up the following assertion:
 *   If the target VM is disconnected during the invoke (for example,
 *   through VirtualMachine.dispose()) the method invocation continues.
 */

public class newinstance008 {

    private final static String prefix = "nsk.jdi.ClassType.newInstance.";
    private final static String debuggerName = prefix + "newinstance008";
    private final static String debugeeName = debuggerName + "a";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";
    public final static String SGNL_FINISH = "finish";
    public final static String SGNL_ABORT = "abort";

    public static int exitStatus;
    public static Log log;
    public static Debugee debugee;
    public static long waitTime;
    private static boolean isAborted = false;

    private ClassType testedClass;

    public static void display(String msg) {
        log.display(msg);
    }

    public static void complain(String msg) {
        log.complain("debugger FAILURE> " + msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        newinstance008 thisTest = new newinstance008();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        waitTime = argHandler.getWaitTime() * 60000;

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        try {
            thisTest.execTest();
        } catch (Failure e) {
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } finally {
            if (!isAborted) {
                debugee.resume();
                debugee.quit();
            }
        }

        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() throws Failure{

        debugee.VM().suspend();
        testedClass = (ClassType )debugee.classByName(debugeeName);

        display("\nTEST BEGINS");
        display("===========");

        InvokingThread invokeThrd = new InvokingThread();
        invokeThrd.start();

        // waiting READY signal from invoked constructor
        display("\nwaiting for signal \""
                        + SGNL_READY
                        + "\" from the invoked constructor");
        debugee.receiveExpectedSignal(SGNL_READY);

        display("\nConstructor has been invoked. Trying to dispose debugee's VM");

        AbortingThread abortingThrd = new AbortingThread();
        abortingThrd.start();

        debugee.VM().dispose();
        invokeThrd.waitVMDisconnect();

        try {
            invokeThrd.join();
        } catch(InterruptedException e) {
            throw new Failure("unexpected " + e);
        }

        if (isAborted) {
            complain("Debugee's VM disposing hung. Invoking was aborted!!!");
            exitStatus = Consts.TEST_FAILED;
        } else {
            display("\nVirtual machine has been disposed");
            display("sending signal \"" + SGNL_FINISH + "\" to the invoked constructor");
            debugee.sendSignal(SGNL_FINISH);
        }

        display("\n=============");
        display("TEST FINISHES\n");
    }

    private BreakpointEvent waitForBreakpoint(BreakpointRequest brkp) {
        Event event = null;
        BreakpointEvent brkpEvent;

        try {
            event = debugee.waitingEvent(brkp, waitTime);
        } catch (InterruptedException e) {
            throw new Failure("unexpected " + e);
        }

        if (!(event instanceof BreakpointEvent )) {
            debugee.resume();
            throw new Failure("BreakpointEvent didn't arrive");
        }

        brkpEvent = (BreakpointEvent )event;
        return brkpEvent;
    }

    private Method getConstructor(ReferenceType classType) {
        List methodList = classType.methods();
        Method method;
        for (int i = 0; i < methodList.size(); i++) {
            method = (Method )methodList.get(i);
            if (method.isConstructor()) {
                return method;
            }
        }
        return null;
    }

    class InvokingThread extends Thread {
        volatile boolean isNotified = false;

        public void run() {
            display("");
            BreakpointRequest brkp = debugee.setBreakpoint(testedClass,
                                                    newinstance008a.brkpMethodName,
                                                    newinstance008a.brkpLineNumber);
            debugee.resume();

            debugee.sendSignal("");

            BreakpointEvent brkpEvent = waitForBreakpoint(brkp);
            ThreadReference thread = brkpEvent.thread();

            Value retValue;
            List<? extends com.sun.jdi.Value> params = new Vector<com.sun.jdi.Value>();

            Method method = getConstructor(testedClass);
            display("\nInvoking debugee's constructor : " + method);

            try {
                retValue = testedClass.newInstance(thread, method, params, 0);
                if ( ((PrimitiveValue )retValue).intValue() == Consts.TEST_FAILED ) {
                    complain("VMDisconnectedException is not thrown");
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch(VMDisconnectedException e) {
                display("!!!expected VMDisconnectedException");
                notifyVMDisconnect();
            } catch(Exception e) {
                complain("Unexpected " + e);
                exitStatus = Consts.TEST_FAILED;
            }
            debugee.resume();
        }

        synchronized void notifyVMDisconnect() {
            isNotified = true;
            notify();
        }

        synchronized void waitVMDisconnect() {
            try {
                if (!isNotified) {
                    display("\nwaiting VMDisconnectedException");
                    wait(waitTime);
                }
            } catch (InterruptedException e) {
                throw new Failure("unexpected " + e);
            }
        }
    }

    class AbortingThread extends Thread {

        public void run() {
            try {
                sleep(waitTime);
            } catch (InterruptedException e) {
                throw new Failure("unexpected " + e);
            }

            display("\n!!!out of wait time!!!");
            display("sending signal \"" + SGNL_ABORT + "\" to the invoked method");
            isAborted = true;
            debugee.sendSignal(SGNL_ABORT);
            debugee.resume();
            debugee.quit();
        }
    }
}

class im008ListeningThread extends Thread {
    public static Object waitDisconnecting = new Object();
    public volatile static boolean isNotified = false;
    public void run() {
        newinstance008.display("");
        newinstance008.display("waiting for signal \""
                        + newinstance008.SGNL_READY
                        + "\" from the constructor");
        newinstance008.debugee.receiveExpectedSignal(newinstance008.SGNL_READY);

        newinstance008.display("");
        newinstance008.display("Virtual machine is disposed");
        newinstance008.debugee.VM().dispose();

        try {
            long startTime = System.currentTimeMillis();
            synchronized (waitDisconnecting) {
                if (!isNotified) {
                    newinstance008.display("");
                    newinstance008.display("waiting VMDisconnectedException");
                    waitDisconnecting.wait(newinstance008.waitTime);
                } else {
                    newinstance008.display("It doesn't wait VMDisconnectedException");
                }
            }
            if (System.currentTimeMillis() - startTime >= newinstance008.waitTime) {
                newinstance008.complain("VMDisconnectedException is not thrown");
                newinstance008.exitStatus = Consts.TEST_FAILED;
            }
        } catch (InterruptedException e) {
            newinstance008.complain("Unexpected InterruptedException");
            newinstance008.exitStatus = Consts.TEST_FAILED;
        }

        newinstance008.display("");
        newinstance008.display("sending signal \"" + newinstance008.SGNL_QUIT
                        + "\" to the constructor");
        newinstance008.debugee.quit();
    }
}
