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

package nsk.jdi.BScenarios.singlethrd;

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
 * location. It should be 73th line, that is constructor of <code>tc03x003aClass1</code>
 * class. Every thread must generate <code>MethodEntryEvent</code>.
 *
 * In case, when at least one event doesn't arrive during waittime
 * interval or line number of event is wrong, test fails.
 */

public class tc03x003 {

    public final static String SGL_READY = "ready";
    public final static String SGL_LOAD = "load";
    public final static String SGL_PERFORM = "perform";
    public final static String SGL_QUIT = "quit";

    private final static String prefix = "nsk.jdi.BScenarios.singlethrd.";
    private final static String debuggerName = prefix + "tc03x003";
    private final static String debugeeName = debuggerName + "a";
    private final static String testedClassName = debugeeName + "Class1";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static int eventCount = 0;
    private final static int expectedEventCount = 3;

    private EventRequestManager evm = null;
    private MethodEntryRequest mthdReq = null;
    private volatile boolean exit = false;

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
        tc03x003 thisTest = new tc03x003();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        waitTime = argHandler.getWaitTime() * 60000;

        Binder binder = new Binder(argHandler, log);
        debugee = binder.bindToDebugee(debugeeName);
        IOPipe pipe = debugee.createIOPipe();

        try {
            thisTest.execTest();
        } catch (Throwable e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
            e.printStackTrace();
        } finally {
            debugee.endDebugee();
        }
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() throws Failure {
        evm = debugee.getEventRequestManager();

        ClassPrepareRequest crq = evm.createClassPrepareRequest();
        crq.addClassFilter(testedClassName);
        crq.enable();

        // separate thread to handle event
        Thread eventHandler = new Thread() {
            public void run() {
                EventQueue eventQueue = debugee.VM().eventQueue();
                while (!exit) {
                    EventSet eventSet = null;
                    try {
                        eventSet = eventQueue.remove(1000);
                    } catch (InterruptedException e) {
                        new Failure("Event handling thread interrupted:\n\t" + e);
                    }
                    if (eventSet == null) {
                        continue;
                    }
                    EventIterator eventIterator = eventSet.eventIterator();
                    while (eventIterator.hasNext()) {
                        Event event = eventIterator.nextEvent();

                        if (event instanceof ClassPrepareEvent) {
                            display(" event ===>>> " + event);
                            ClassPrepareEvent clsEvent = (ClassPrepareEvent)event;
                            hitEvent(clsEvent);

                            mthdReq = evm.createMethodEntryRequest();
                            ReferenceType testedClass = clsEvent.referenceType();
                            mthdReq.addClassFilter(testedClass);
                            mthdReq.enable();

                            debugee.resume();

                        } else if (event instanceof MethodEntryEvent) {
                            display(" event ===>>> " + event);
                            hitEvent((MethodEntryEvent )event);
                            debugee.resume();

                        } else if (event instanceof VMDeathEvent) {
                            exit = true;
                            break;
                        } else if (event instanceof VMDisconnectEvent) {
                            exit = true;
                            break;
                        } else {
                            throw new Failure("Unexpected event received:\n\t" + event);
                        } // if
                        exit = exit || (eventCount >= expectedEventCount);
                    } // while
                } // while
            } // run()
        }; // eventHandler

        display("Starting handling event");
        eventHandler.start();

        debugee.resume();
        debugee.receiveExpectedSignal(SGL_READY);

        display("\nTEST BEGINS");
        display("===========");
        debugee.sendSignal(SGL_LOAD);

        display("Waiting for all events received");
        try {
            eventHandler.join(waitTime);
        } catch (InterruptedException e) {
            throw new Failure("Main thread interrupted while waiting for eventHandler:\n\t"
                                + e);
        } finally {
            crq.disable();
            mthdReq.disable();
            exit = true;
            if (eventHandler.isAlive()) {
                display("Interrupting event handling thread");
                eventHandler.interrupt();
            }
        }

        if (eventCount < expectedEventCount) {
            complain("Expecting " + expectedEventCount
                        + " BreakpointEvents, but "
                        + eventCount + " events arrived.");
            exitStatus = Consts.TEST_FAILED;
        }

        display("=============");
        display("TEST FINISHES\n");
        debugee.sendSignal(SGL_QUIT);
    }

    private void hitEvent(MethodEntryEvent event) {
        ThreadReference thrd = event.thread();

        display("event info:");
        display("\tthread\t- " + event.thread().name());
        try {
            display("\tsource\t- " + event.location().sourceName());
        } catch (AbsentInformationException e) {
        }
        display("\tmethod\t- " + event.location().method().name());
        display("\tline\t- " + event.location().lineNumber());

        eventCount++;
        if (event.location().lineNumber() == tc03x003a.checkLastLine1 ||
                event.location().lineNumber() == tc03x003a.checkLastLine2) {
            display("MethodEntryEvent occurs stops on the expected line "
                        + event.location().lineNumber() + " in method "
                        + event.method().name());
        } else {
            complain("MewthodEntryEvent occurs on line " + event.location().lineNumber()
                        + " in method " + event.method().name()
                        + ", expected line numbers are "
                        + tc03x003a.checkLastLine1 + " or "
                        +  tc03x003a.checkLastLine2);
            exitStatus = Consts.TEST_FAILED;
        }

        display("");

    }

    private void hitEvent(ClassPrepareEvent event) {
        display("ClassPrepareEvent occurs\n");
        eventCount++;
    }
}
