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

package nsk.jdi.BScenarios.multithrd;

import jdk.test.lib.Utils;
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
 * implements the following test case:                                  <br>
 *     Suite 2 - Breakpoints (multiple threads)                         <br>
 *     Test case:      TC2                                              <br>
 *     Description:    Class breakpoint                                 <br>
 *     Steps:          1.Add class breakpoint: singlethread.Class1      <br>
 *                     2.Debug Main                                     <br>
 *                       X. Stops on line 13 in Class1.java             <br>
 *
 * When the test is starting debugee, debugger creates <code>MethodEntryRequest</code>.
 * After <code>MethodEntryEvent</code> arrived, debugger checks line number of one's
 * location. It should be 73th line, that is constructor of <code>tc02x004aClass1</code>
 * class. Every thread must generate <code>MethodEntryEvent</code>.
 *
 * In case, when at least one event doesn't arrive during waittime
 * interval or line number of event is wrong, test fails.
 */

public class tc02x004 {

    public final static String SGL_READY = "ready";
    public final static String SGL_PERFORM = "perform";
    public final static String SGL_QUIT = "quit";

    private final static String prefix = "nsk.jdi.BScenarios.multithrd.";
    private final static String debuggerName = prefix + "tc02x004";
    private final static String debugeeName = debuggerName + "a";
    private final static String testedClassName = debugeeName + "Class1";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static int brkpEventCount = 0;
    MethodEntryRequest mthdReq;
    EventRequestManager evm;

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

        tc02x004 thisTest = new tc02x004();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        waitTime = Utils.adjustTimeout(argHandler.getWaitTime() * 60000);

        Binder binder = new Binder(argHandler, log);
        debugee = binder.bindToDebugee(debugeeName);
        debugee.redirectStderr(log.getOutStream());
        thisTest.evm = debugee.getEventRequestManager();

        try {
            thisTest.execTest();
        } catch (Throwable e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
            thisTest.evm.deleteEventRequest(thisTest.mthdReq);
        } finally {
            debugee.resume();
        }
        display("Test finished. exitStatus = " + exitStatus);

        debugee.endDebugee();
        return exitStatus;
    }

    private void execTest() throws Failure {

        display("\nTEST BEGINS");
        display("===========");
        debugee.resume();

        EventSet eventSet = null;
        EventIterator eventIterator = null;
        Event event;
        long totalTime = waitTime;
        long tmp, begin = System.currentTimeMillis(),
             delta = 0;
        boolean exit = false;

        mthdReq = evm.createMethodEntryRequest();
        mthdReq.addClassFilter(testedClassName);
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
                        display(" event ===>>> " + (brkpEventCount+1) + " MethodEntryEvent arrived");
                        hitClassBreakpoint((MethodEntryEvent )event);
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
            exit = exit || (brkpEventCount == tc02x004a.threadCount);
            tmp = System.currentTimeMillis();
            delta = tmp - begin;
            totalTime -= delta;
                begin = tmp;
        }

        if (totalTime <= 0) {
            complain("out of wait time...");
            exitStatus = Consts.TEST_FAILED;
        }
        if (brkpEventCount < tc02x004a.threadCount) {
            complain("expecting " + tc02x004a.threadCount
                        + " breakpoint events, but "
                        + brkpEventCount + " events arrived.");
            exitStatus = Consts.TEST_FAILED;
        }

        display("=============");
        display("TEST FINISHES\n");
    }

    private void hitClassBreakpoint(MethodEntryEvent event) {
        ThreadReference thrd = event.thread();

        display("event info:");
        display("\tthread\t- " + event.thread().name());
        try {
            display("\tsource\t- " + event.location().sourceName());
        } catch (AbsentInformationException e) {
        }
        display("\tmethod\t- " + event.location().method().name());
        display("\tline\t- " + event.location().lineNumber());

        display("thread:\t" + event.thread().name());
        try {
            display("source:\t" + event.location().sourceName());
        } catch (AbsentInformationException e) {
        }
        display("method:\t" + event.location().method().name());
        display("line:\t" + event.location().lineNumber());
        if (event.location().lineNumber() == tc02x004a.checkClassBrkpLine) {
            display("ClassBreakpoint stops on the expected line "
                        + event.location().lineNumber() + " in method "
                        + event.method().name());
        } else {
            complain("ClassBreakpoint stops on line " + event.location().lineNumber()
                        + " in method " + event.method().name()
                        + ", expected line number is "
                        + tc02x004a.checkClassBrkpLine);
            exitStatus = Consts.TEST_FAILED;
        }

        display("");

        brkpEventCount++;
    }
}
