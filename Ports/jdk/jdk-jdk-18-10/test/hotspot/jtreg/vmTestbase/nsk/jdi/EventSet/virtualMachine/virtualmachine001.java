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

package nsk.jdi.EventSet.virtualMachine;

import nsk.share.*;
import nsk.share.jdi.*;
import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;
import java.io.*;

/**
 * The debugger application of the test.
 */
public class virtualmachine001 {

    //------------------------------------------------------- immutable common fields

    final static String SIGNAL_READY = "ready";
    final static String SIGNAL_GO    = "go";
    final static String SIGNAL_QUIT  = "quit";

    private static int waitTime;
    private static int exitStatus;
    private static ArgumentHandler     argHandler;
    private static Log                 log;
    private static Debugee             debuggee;
    private static ReferenceType       debuggeeClass;

    //------------------------------------------------------- mutable common fields

    private final static String prefix = "nsk.jdi.EventSet.virtualMachine.";
    private final static String className = "virtualmachine001";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields

    private static VirtualMachine vm;

    //------------------------------------------------------- immutable common methods

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    private static void display(String msg) {
        log.display("debugger > " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE > " + msg);
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        waitTime = argHandler.getWaitTime() * 60000;

        debuggee = Debugee.prepareDebugee(argHandler, log, debuggeeName);

        debuggeeClass = debuggee.classByName(debuggeeName);
        if ( debuggeeClass == null ) {
            complain("Class '" + debuggeeName + "' not found.");
            exitStatus = Consts.TEST_FAILED;
        }

        execTest();

        debuggee.quit();

        return exitStatus;
    }

    //------------------------------------------------------ mutable common method

    private static void execTest() {

        BreakpointRequest brkp = debuggee.setBreakpoint(debuggeeClass,
                                                    virtualmachine001a.brkpMethodName,
                                                    virtualmachine001a.brkpLineNumber);
        debuggee.resume();

        // get expected reference for comparison.
        vm = debuggee.VM();

        debuggee.sendSignal(SIGNAL_GO);
        EventSet eventSet = null;

        // waiting the breakpoint event
        try {
            eventSet = waitEventSet(brkp, waitTime);
        } catch (InterruptedException e) {
            throw new Failure("unexpected InterruptedException while waiting for Breakpoint event");
        }
        if (eventSet == null)
            throw new Failure("Expected EventSet didn't arrive");

        if (!(eventSet.eventIterator().nextEvent() instanceof BreakpointEvent)) {
            debuggee.resume();
            throw new Failure("BreakpointEvent didn't arrive");
        }

        display("Checking virtualMachine() method for eventSet of Breakpoint event");
        checkVM (eventSet);

        display("Checking completed!");
        debuggee.resume();
    }

    //--------------------------------------------------------- test specific methods

    private static EventSet waitEventSet(EventRequest request, long timeout)
                            throws InterruptedException {
        Event event;
        long totalTime = timeout;
        long tmp, begin = System.currentTimeMillis(),
             delta = 0;
        boolean exit = false;
        EventIterator eventIterator = null;
        EventSet eventSet = null;

        while (totalTime > 0 && !exit) {
            if (eventIterator == null || !eventIterator.hasNext()) {
                eventSet = debuggee.VM().eventQueue().remove(totalTime);
                if (eventSet != null) {
                    eventIterator = eventSet.eventIterator();
                } else {
                    eventIterator = null;
                }
            }
            if (eventIterator != null) {
                while (eventIterator.hasNext()) {
                    event = eventIterator.nextEvent();
                    display(" event ===>>> " + event);
                    if (event.request() != null && event.request().equals(request)) {
                        return eventSet;
                    } else if (request == null && event instanceof VMStartEvent) {
                        return eventSet;
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
        return null;
    }

    private static void checkVM (EventSet eventSet) {
        VirtualMachine vm1 = eventSet.virtualMachine();
        if (vm1 == null) {
            complain("virtualMachine() returns null for event set");
            exitStatus = Consts.TEST_FAILED;
        } else if (vm1 != vm) {
            complain("virtualMachine() returns different VirtualMachine object");
            exitStatus = Consts.TEST_FAILED;
        }
    }
}
//--------------------------------------------------------- test specific classes
