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
package nsk.jdi.Scenarios.invokeMethod;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 * Debuggee's part contains a tested class (class <code>B</code>) and debugger exercises
 * method <code>runIt()</code> of this class by the following steps:                <br>
 * 1. On <code>ClassPrepareEvent</code> of class <code>B</code>,
 * <code>MethodExitRequest</code> is created and debugger waits
 * <code>MethodExitEvent</code> for <code><clinit></code> to be shure the static
 * members of class <code>B</code> have been initialized                            <br>
 * 2. After getting <code>MethodExitEvent</code> for <code><clinit></code>,         <br>
 *      - debugger creates <code>MethodEntryRequest</code>                          <br>
 *      - invokes the tested method (method <code>runIt</code>) by calling
 * <code>com.sun.jdi.ClassType.invokeMethod()</code>.                               <br>
 *    This invoking occurs in special thread of debugger's part so that
 *    debugger can process the events of the target VM.
 * 3. When getting <code>MethodEntryEvent</code> from the invoked method,
 *    debugger tries to pop current frame (with 0 index).
 *
 * The test passes when the above steps have been successfuly executed.
 */

public class popframes001 {

    public final static String UNEXPECTED_STRING = "***Unexpected exception ";

    private final static String prefix = "nsk.jdi.Scenarios.invokeMethod.";
    private final static String debuggerName = prefix + "popframes001";
    public final static String debugeeName = debuggerName + "a";
    public final static String testedClassName = debuggerName + "b";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;

    // We expect runIt() method to be called 2 times.
    // First time the method call frame will be popped.
    // When the metod is recalled it will complete normally.
    public final static int expectedEventCount = 2;
    private int eventCount = 0;
    private ClassType debugeeClass, testedClass;
    private EventRequestManager evm;


    private static void display(String msg) {
        log.display(msg);
    }

    private static void complain(String msg) {
        log.complain(msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        popframes001 thisTest = new popframes001();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        waitTime = argHandler.getWaitTime() * 60000;

        Binder binder = new Binder(argHandler, log);
        debugee = binder.bindToDebugee(debugeeName);
        debugee.redirectOutput(log);

        try {
            thisTest.execTest();
        } catch (Throwable e) {
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } finally {
            debugee.endDebugee();
        }
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() throws Failure {

        if (!debugee.VM().canPopFrames()) {
            display("\n>>>canPopFrames() is false<<< test canceled.\n");
            return;
        }

        display("\nTEST BEGINS");
        display("===========");

        EventSet eventSet = null;
        EventIterator eventIterator = null;
        Event event;
        long totalTime = waitTime;
        long tmp, begin = System.currentTimeMillis(),
             delta = 0;
        boolean exit = false;

        evm = debugee.getEventRequestManager();

        ClassPrepareRequest req = evm.createClassPrepareRequest();
        req.addClassFilter(testedClassName);
        req.enable();
        display("\nresuming...");
        debugee.resume();

        while (totalTime > 0 && !exit) {
            if (eventIterator == null || !eventIterator.hasNext()) {
                try {
                    eventSet = debugee.VM().eventQueue().remove(totalTime);
                } catch (InterruptedException e) {
                    new Failure(e);
                }
                if (eventSet != null) {
                    eventIterator = eventSet.eventIterator();
                } else {
                    eventIterator = null;
                }
            }
            if (eventIterator != null) {
                while (eventIterator.hasNext()) {
                    event = eventIterator.nextEvent();
//                    display("\nevent ===>>> " + event);

                    if (event instanceof ClassPrepareEvent) {
                        display("\nevent ===>>> " + event);
                        testedClass = (ClassType )debugee.classByName(testedClassName);
                        debugeeClass = (ClassType )debugee.classByName(debugeeName);

                        display("\ncreating MethodExitRequest for the \""
                                    + testedClassName
                                    + "\" class");

                        MethodExitRequest mreq = evm.createMethodExitRequest();
                        mreq.addClassFilter(testedClassName);
                        mreq.enable();

                        display("\nresuming...");
                        debugee.resume();

                    } else if (event instanceof MethodExitEvent) {
                        display("\nevent ===>>> " + event);
                        hitMethodExitEvent((MethodExitEvent )event);

                    } else if (event instanceof MethodEntryEvent) {
                        display("\nevent ===>>> " + event);
                        hitMethodEntryEvent((MethodEntryEvent )event);
                        display("\nresuming...");
                        debugee.resume();

                    } else if (event instanceof VMDeathEvent) {
                        exit = true;
                        break;
                    } else if (event instanceof VMDisconnectEvent) {
                        exit = true;
                        break;
                    } // if
                } // while
            } // if
            tmp = System.currentTimeMillis();
            delta = tmp - begin;
            totalTime -= delta;
                begin = tmp;
        }

        if (totalTime <= 0) {
            complain("out of wait time...");
            exitStatus = Consts.TEST_FAILED;
        }
        if (eventCount != expectedEventCount) {
            complain("expecting " + expectedEventCount
                        + " events, but "
                        + eventCount + " events arrived.");
            exitStatus = Consts.TEST_FAILED;
        }

        display("=============");
        display("TEST FINISHES\n");
    }

    private void hitMethodExitEvent(MethodExitEvent event) {
        Method mthd = event.method();
        display("MethodExitEvent:: method name   :\t" + mthd.name());
        display("MethodExitEvent:: is it <clinit>:\t"
                    + mthd.isStaticInitializer());
        if (mthd.isStaticInitializer()) {
            display("\nMethodExitEvent:: creating MethodEntryRequest for the \""
                    + testedClassName
                    + "\" class");

            evm.createExceptionRequest(testedClass, false, true).enable();

            MethodEntryRequest req = evm.createMethodEntryRequest();
            req.addClassFilter(testedClassName);
            req.enable();
            MethodInvoker invoker = new MethodInvoker(testedClass,
                                                    popframes001b.methodNameCaller,
                                                    event.thread());
            invoker.start();

        } else {
            display("MethodExitEvent:: no actions for this method");
            display("\nresuming...");
            debugee.resume();
        }
    }

    // Set a flag that the frame has been popped.
    // Otherwise we would get an eternal loop calling and popping runIt().
    private boolean isFramePopped = false;

    private void hitMethodEntryEvent(MethodEntryEvent event) {
        String methodName = event.method().name();
        display("MethodEntryEvent:: method name:\t" + methodName);
        if (popframes001b.methodName.compareTo(methodName) == 0) {
            if (!isFramePopped) {
                displayActiveFrames(event.thread());
                popFrames(event.thread());
                displayActiveFrames(event.thread());
                isFramePopped = true;
            }
            eventCount++;
        }
    }

    private void displayActiveFrames(ThreadReference thread) {
        int i = 0;
        display("\nActive frames:");
        try {
            for (i = 0; i < thread.frameCount(); i++) {
                display("\t" + i + ". " + thread.frame(i).location()
                            + "\tmethod: " + thread.frame(i).location().method().name());
            }
        } catch(IncompatibleThreadStateException e) {
            display("\t" + i + ". ??? " + e);
        }
    }

    private void popFrames(ThreadReference thread) {
        try {
            StackFrame frame = thread.frame(0);
            display("\nresetting frame of " + frame.location().method());
            thread.popFrames(frame);
            display("frame has been reset");
        } catch (IncompatibleThreadStateException e) {
            throw new Failure(UNEXPECTED_STRING + e);
        }
    }

    class MethodInvoker extends Thread {
        private ClassType clsType;
        private String methodName;
        private ThreadReference thread;

        public MethodInvoker(ClassType clsType, String methodName,
                                    ThreadReference thread) {
            display("\ninvokingMethodThread:: thread created");
            this.clsType = clsType;
            this.methodName = methodName;
            this.thread = thread;
        }

        public void run() {
            Method mthd = debugee.methodByName(clsType, methodName);
            display("invokingMethodThread:: invoking method\t:\"" + mthd.name() + "\"");
            display("invokingMethodThread:: -------------------------------");

            try {
                clsType.invokeMethod(thread, mthd, new Vector<Value>(), 0);
            } catch (Throwable e) {
                complain("invokingMethodThread:: " + UNEXPECTED_STRING + e);
                exitStatus = Consts.TEST_FAILED;
                e.printStackTrace();
            }

            display("setting <finishIt> field");
            Field fld = debugeeClass.fieldByName("finishIt");
            try {
                debugeeClass.setValue(fld,debugee.VM().mirrorOf(true));
            } catch (Exception e) {
                throw new Failure(UNEXPECTED_STRING + e);
            }

            debugee.resume();
        }
    }
}
