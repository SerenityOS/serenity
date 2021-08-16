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

package nsk.jdi.BScenarios.multithrd;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.util.*;
import java.io.*;

/**
 * This test is from the group of so-called Borland's scenarios and
 * implements the following test case:
 *     Suite 2 - Breakpoints (multiple threads)
 *     Test case:      TC4
 *     Description:    Exception breakpoint
 *     Steps:          1.Set method breakpoint on Main.foo()
 *                     2.Debug Main
 *                     3.Stops on line 27 in Main.java
 *
 * When the test is starting debugee, debugger creates <code>MethodEntryRequest</code>.
 * After <code>MethodEntryEvent</code> arrived, debugger checks method name and if
 * the one corresponds to specified name, it checks line number of the
 * event location. It should be 64th line, that is method <code>tc04x001a.foo</code>.
 *
 * In case, when at least one event doesn't arrive during waittime
 * interval or line number of event is wrong, test fails.
 */

public class tc04x001 {

    public final static String SGL_READY = "ready";
    public final static String SGL_LOAD = "load";
    public final static String SGL_PERFORM = "perform";
    public final static String SGL_QUIT = "quit";

    private final static String prefix = "nsk.jdi.BScenarios.multithrd.";
    private final static String debuggerName = prefix + "tc04x001";
    private final static String debugeeName = debuggerName + "a";
    private final static String methodName = "foo";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static int brkpEventCount = 0;

    private ClassType debugeeClass;

    private static void display(String msg) {
        log.display(msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE> " + msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        tc04x001 thisTest = new tc04x001();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        waitTime = argHandler.getWaitTime() * 60000;

        Binder binder = new Binder(argHandler, log);
        debugee = binder.bindToDebugee(debugeeName);

        try {
            thisTest.execTest();
        } catch (Throwable e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } finally {
            debugee.resume();
        }
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() throws Failure {

        display("\nTEST BEGINS");
        display("===========");

        EventSet eventSet = null;
        EventIterator eventIterator = null;
        Event event;
        long totalTime = waitTime;
        long tmp, begin = System.currentTimeMillis(),
             delta = 0;
        boolean exit = false;
        EventRequestManager evm = debugee.getEventRequestManager();
        MethodEntryRequest mthdReq = evm.createMethodEntryRequest();
        display("MethodEntryRequest created, expecting events "
                    + "from method \"" + methodName + "\"");
        display("---------------------------------------------"
                    + "-----------");
        mthdReq.addClassFilter(debugeeName);
        mthdReq.enable();

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

                    if (event instanceof MethodEntryEvent) {
                        display(" event ===>>> " + " MethodEntryEvent arrived");
                        hitMethodBreakpoint((MethodEntryEvent )event);
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
        if (brkpEventCount < tc04x001a.threadCount) {
            complain("expecting " + tc04x001a.threadCount
                        + " MethodBreakpoint events, but "
                        + brkpEventCount + " events arrived.");
            exitStatus = Consts.TEST_FAILED;
        }

        display("=============");
        display("TEST FINISHES\n");
    }

    private void hitMethodBreakpoint(MethodEntryEvent event) {
        ThreadReference thrd = event.thread();

        display("event info:");
        display("\tthread\t- " + event.thread().name());
        try {
            display("\tsource\t- " + event.location().sourceName());
        } catch (AbsentInformationException e) {
        }
        display("\tmethod\t- " + event.location().method().name());
        display("\tline\t- " + event.location().lineNumber());

        if (!event.method().name().equals(methodName)) {
            display("the event skipped, method - " + event.method().name() + "\n");
            return;
        }

        if (event.location().lineNumber() == tc04x001a.checkMethodBrkpLine) {
            display("!!!MethodBreakpoint stops on the expected line "
                        + event.location().lineNumber() + " in method "
                        + "\"" + event.method().name() + "\"!!!");
        } else {
            complain("MethodBreakpoint stops on line " + event.location().lineNumber()
                        + " in method " + event.method().name()
                        + ", expected line number is "
                        + tc04x001a.checkMethodBrkpLine);
            exitStatus = Consts.TEST_FAILED;
        }

        display("");

        brkpEventCount++;
    }
}
