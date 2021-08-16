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
 * implements the following test case:                  <br>
 *     Suite 2 - Breakpoints (multiple threads)         <br>
 *     TC1                                              <br>
 *     Description:    Line breakpoint & step           <br>
 *     Steps:          1.Set breakpoint on line 32      <br>
 *                     2.Debug Main                     <br>
 *                       X. Stops on line 32            <br>
 *                     3.Run | Step over                <br>
 *                       X. Steps to line 33            <br>
 *
 * When the test is starting debugee, debugger sets breakpoint at the 69th
 * line (method "foo"). After the breakpoint is reached, debugger creates
 * "step over" request and resumes debugee. For every hit event debugger
 * checks line number of one's location. It should be 69th line for Breakpoint
 * or 70th line for Step.
 *
 * In case, when at least one event doesn't arrive during waittime
 * interval or line number of event is wrong, test fails.
 */

public class tc01x001 {

    public final static String SGL_READY = "ready";
    public final static String SGL_PERFORM = "perform";
    public final static String SGL_QUIT = "quit";

    private final static String prefix = "nsk.jdi.BScenarios.multithrd.";
    private final static String debuggerName = prefix + "tc01x001";
    private final static String debugeeName = debuggerName + "a";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static int brkpEventCount = 0;
    private static int stepEventCount = 0;

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

        tc01x001 thisTest = new tc01x001();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        waitTime = argHandler.getWaitTime() * 60000;

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        try {
            thisTest.execTest();
        } catch (Throwable e) {
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } finally {
            debugee.resume();
            debugee.quit();
        }
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() throws Failure {

        debugeeClass = (ClassType)debugee.classByName(debugeeName);

        display("\nTEST BEGINS");
        display("===========");

        display("Tested class\t:" + debugeeClass.name());

        EventSet eventSet = null;
        EventIterator eventIterator = null;
        Event event;
        long totalTime = waitTime;
        long tmp, begin = System.currentTimeMillis(),
             delta = 0;
        boolean exit = false;

        debugee.setBreakpoint(debugeeClass,
                                tc01x001a.brkpMethodName,
                                tc01x001a.brkpLineNumber);
        debugee.resume();
        debugee.sendSignal(SGL_PERFORM);

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
//                    display(" event ===>>> " + event);

                    if (event instanceof BreakpointEvent) {
                        display(" event ===>>> " + event);
                        hitBreakpoint((BreakpointEvent )event);
                        debugee.resume();

                    } else if (event instanceof StepEvent) {
                        display(" event ===>>> " + event);
                        hitStepOver((StepEvent )event);
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
            exit = exit || (brkpEventCount == tc01x001a.threadCount &&
                                stepEventCount == tc01x001a.threadCount);
            tmp = System.currentTimeMillis();
            delta = tmp - begin;
            totalTime -= delta;
                begin = tmp;
        }

        if (brkpEventCount < tc01x001a.threadCount) {
            if (totalTime <= 0) {
                complain("out of wait time...");
            }
            complain("expecting " + tc01x001a.threadCount
                        + " MethodBreakpoint events, but "
                        + brkpEventCount + " events arrived.");
            exitStatus = Consts.TEST_FAILED;
        }

        display("=============");
        display("TEST FINISHES\n");
    }

    private void hitBreakpoint(BreakpointEvent event) {
        EventRequestManager evm = debugee.getEventRequestManager();
        ThreadReference thrd = event.thread();

        display("event info:");
        display("\tthread\t- " + event.thread().name());
        try {
            display("\tsource\t- " + event.location().sourceName());
        } catch (AbsentInformationException e) {
        }
        display("\tmethod\t- " + event.location().method().name());
        display("\tline\t- " + event.location().lineNumber());

        if (event.location().lineNumber() != tc01x001a.checkBrkpLine) {
            complain("BreakpointEvent stops on line " + event.location().lineNumber()
                        + ", expected line number is " + tc01x001a.checkBrkpLine);
            exitStatus = Consts.TEST_FAILED;
        } else {
            display("BreakpointEvent stops on the expected line "
                        + event.location().lineNumber());
        }

        display("");
        StepRequest step = evm.createStepRequest(thrd, StepRequest.STEP_LINE,
                                                            StepRequest.STEP_OVER);
        brkpEventCount++;
        step.enable();
    }

    private void hitStepOver(StepEvent event) {
        EventRequestManager evm = debugee.getEventRequestManager();

        display("event info:");
        display("\tthread\t- " + event.thread().name());
        try {
            display("\tsource\t- " + event.location().sourceName());
        } catch (AbsentInformationException e) {
        }
        display("\tmethod\t- " + event.location().method().name());
        display("\tline\t- " + event.location().lineNumber());

        if (event.location().lineNumber() != tc01x001a.checkStepLine) {
            complain("StepEvent steps to line " + event.location().lineNumber()
                        + ", expected line number is " + tc01x001a.checkStepLine);
            exitStatus = Consts.TEST_FAILED;
        } else {
            display("StepEvent steps to the expected line "
                        + event.location().lineNumber());
        }
        display("");

        evm.deleteEventRequest(event.request());
        stepEventCount++;
    }
}
