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
 *     Test case:      TC3
 *     Description:    Exception breakpoint
 *     Steps:          1.Set caught exception breakpoint on class
 *                       javax.sound.midi.MidiUnavailableException
 *                     2.Debug Main
 *                       X. Stops on line 42 in Main.java
 *
 * When the test is starting debugee, debugger creates <code>ExceptionRequest</code>.
 * After <code>ExceptionEvent</code> arrived, debugger checks line number of one's
 * location. It should be 74th line, that is throwing <code>tc03x001aException</code>.
 * Every thread must generate <code>ExceptionEvent</code>.
 *
 * In case, when at least one event doesn't arrive during waittime
 * interval or line number of event is wrong, test fails.
 */

public class tc03x001 {

    public final static String SGL_READY = "ready";
    public final static String SGL_LOAD = "load";
    public final static String SGL_PERFORM = "perform";
    public final static String SGL_QUIT = "quit";

    private final static String prefix = "nsk.jdi.BScenarios.multithrd.";
    private final static String debuggerName = prefix + "tc03x001";
    private final static String debugeeName = debuggerName + "a";
    private final static String exceptionName = debugeeName + "Exception";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static long waitTime;
    private static int eventCount = 0;

    private EventRequestManager evm = null;
    private ExceptionRequest exReq = null;
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
        tc03x001 thisTest = new tc03x001();

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
        crq.addClassFilter(exceptionName);
        crq.enable();

        // event handling thread
        Thread eventHandler = new Thread() {
            public void run() {
                EventQueue eventQueue = debugee.VM().eventQueue();
                EventSet eventSet = null;
                while (!exit) {
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
                            exReq = evm.createExceptionRequest(
                                            ((ClassPrepareEvent )event).referenceType(),
                                            true, false);
                            exReq.enable();
                            debugee.resume();

                        } else if (event instanceof ExceptionEvent) {
                            hitEvent((ExceptionEvent )event);
                            debugee.resume();

                        } else if (event instanceof VMDeathEvent) {
                            exit = true;
                        } else if (event instanceof VMDisconnectEvent) {
                            exit = true;
                            break;
                        } else {
                            throw new Failure("Unexpected event:\n\t" + event);
                        } // if
                        exit = exit || (eventCount >= tc03x001a.threadCount);
                    } // while
                } // while
            } // run()
        }; // eventHadler

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
            exReq.disable();
            exit = true;
            if (eventHandler.isAlive()) {
                display("Interrupting event handling thread");
                eventHandler.interrupt();
            }
        }

        if (eventCount < tc03x001a.threadCount) {
            complain("expecting " + tc03x001a.threadCount
                        + " ExceptionEvents, but "
                        + eventCount + " events arrived.");
            exitStatus = Consts.TEST_FAILED;
        }
        display("=============");
        display("TEST FINISHES\n");
        debugee.sendSignal(SGL_QUIT);
    }

    private void hitEvent(ExceptionEvent event) {
        ThreadReference thrd = event.thread();

        display("event info:");
        display("\tthread\t- " + event.thread().name());
        try {
            display("\tsource\t- " + event.location().sourceName());
        } catch (AbsentInformationException e) {
        }
        display("\tmethod\t- " + event.location().method().name());
        display("\tline\t- " + event.location().lineNumber());

        if (event.location().lineNumber() == tc03x001a.checkExBrkpLine) {
            display("ExceptionEvent occurs on the expected line "
                        + event.location().lineNumber() + " in method "
                        + event.location().method().name());
        } else {
            complain("ExceptionEvent occurs stops on line " + event.location().lineNumber()
                        + " in method " + event.location().method().name()
                        + ", expected line number is "
                        + tc03x001a.checkExBrkpLine);
            exitStatus = Consts.TEST_FAILED;
        }

        display("");

        eventCount++;
    }
}
