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

package nsk.jdi.EventQueue.remove_l;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;

import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * Test check up this method for the following values:
 * <code>Long.MIN_VALUE</code>, <code>-1</code> and <code>Long.MAX_VALUE</code>.
 * IllegalArgumentException is expected for negative values.
 */
public class remove_l005 {
    private final static String prefix = "nsk.jdi.EventQueue.remove_l.";
    private final static String debuggerName = prefix + "remove_l005";
    private final static String debugeeName = debuggerName + "a";

    private final static String brkpMethodName = "run";
    private final static int brkpLineNumber = 61;

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";
    public final static String SGNL_GO = "go";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;

    private static long waitTimes[] = {
                                            Long.MIN_VALUE,
                                            -1,
                                            Long.MAX_VALUE
    };

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

        remove_l005 thisTest = new remove_l005();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        thisTest.execTest();
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() {

        ReferenceType refType = debugee.classByName(debugeeName);
        BreakpointRequest brkpReq = debugee.setBreakpoint(refType, brkpMethodName, brkpLineNumber);

        display("\nTEST BEGINS");
        display("===========");

        Event event = null;
        EventSet eventSet = null;
        EventIterator eventIterator = null;

        debugee.sendSignal(SGNL_GO);

        for (int i = 0; i < waitTimes.length; i++) {
            try {
                display("invoking EventQueue.remove(" + waitTimes[i] + ")");
                eventSet = debugee.VM().eventQueue().remove(waitTimes[i]);
                if (waitTimes[i] < 0) {
                    complain("No exception was thrown for negative argument: " + waitTimes[i]);
                    exitStatus = Consts.TEST_FAILED;
                } else {
                    display("No exception was thrown for not-negative argument: " + waitTimes[i]);
                }
                if (eventSet != null) {
                    if (checkBrkpEvent(eventSet, brkpReq)) {
                        eventSet.resume();
                        debugee.sendSignal(SGNL_GO);
                    } else {
                        eventSet.resume();
                    }
                }
            } catch (IllegalArgumentException e1) {
                if (waitTimes[i] < 0) {
                    display("Expected IllegalArgumentException was thrown for negative argument: " + waitTimes[i]);
                } else {
                    complain("Unexpected IllegalArgumentException was thrown for not-negative argument: " + waitTimes[i]);
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch (Exception e) {
                complain("Unexpected exception was thrown for argument: " + waitTimes[i] + " :\n\t" + e );
                exitStatus = Consts.TEST_FAILED;
            }
            display("");
        }

        display("");
        display("=============");
        display("TEST FINISHES\n");

        brkpReq.disable();
        debugee.resume();
        debugee.quit();
    }

    private boolean checkBrkpEvent(EventSet eventSet, BreakpointRequest brkpReq) {
        EventIterator eventIterator = eventSet.eventIterator();
        while (eventIterator.hasNext()) {
            Event event = eventIterator.nextEvent();
            if (event instanceof BreakpointEvent) {
                EventRequest eventRequest = event.request();
                if (brkpReq.equals(eventRequest)) {
                    log.display("Got expected BreakpointEvent:\n\t" + event);
                    return true;
                }
            }
            log.display("Ignore unexpected event:\n\t" + event);
        }
        return false;
    }

}
