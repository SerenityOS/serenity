/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.VirtualMachine.resume;

import nsk.share.*;
import nsk.share.jdi.*;
import com.sun.jdi.*;
import java.io.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

public class resume001 extends TestDebuggerType2 {

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new resume001().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return resume001a.class.getName();
    }

    class EventListener extends EventHandler.EventListener {
        Object lock = new Object();

        volatile int breakpointCounter;

        volatile int expectedBreakpointNumber;

        EventListener(int expectedBreakpointNumber) {
            this.expectedBreakpointNumber = expectedBreakpointNumber;
        }

        void waitForBreakpoints() {
            synchronized (lock) {
                while (breakpointCounter < expectedBreakpointNumber) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        unexpectedException(e);
                    }
                }
            }
        }

        public boolean eventReceived(Event event) {
            if (event instanceof BreakpointEvent) {
                log.display("BreakpointEvent was received: " + event);

                breakpointCounter++;

                if (breakpointCounter == expectedBreakpointNumber) {
                    synchronized (lock) {
                        lock.notify();
                    }
                }

                if (breakpointCounter > expectedBreakpointNumber) {
                    setSuccess(false);
                    System.out.println("Extra breakpoint event was received: " + event);
                }

                return true;
            }

            return false;
        }
    }

    protected void doTest() {
        ReferenceType testThreadClass = debuggee.classByName(resume001a.class.getName());

        BreakpointRequest breakpointRequest = null;

        try {
            for (Location location : testThreadClass.allLineLocations()) {
                if (location.lineNumber() == resume001a.BREAKPOINT_LINE_NUMBER) {
                    breakpointRequest = debuggee.getEventRequestManager().createBreakpointRequest(location);
                    break;
                }
            }
        } catch (AbsentInformationException e) {
            unexpectedException(e);
            return;
        }

        if (breakpointRequest == null) {
            setSuccess(false);
            log.complain("Location for line " + resume001a.BREAKPOINT_LINE_NUMBER + " wasn't found in class " + testThreadClass.name());
            return;
        }

        breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        breakpointRequest.enable();

        log.display("BreakpointRequest was created: " + breakpointRequest);

        EventHandler eventHandler = new EventHandler(debuggee, log);
        eventHandler.startListening();

        try {
            /*
             * Wait for BreakpointEvents from debuggee main thread and from each
             * additional test thread
             */
            final int suspendedThreadsNumber = resume001a.TEST_THREAD_NUMBER + 1;

            EventListener listener = new EventListener(suspendedThreadsNumber);
            eventHandler.addListener(listener);

            pipe.println(resume001a.COMMAND_STOP_ALL_THREADS_AT_BREAKPOINT);

            listener.waitForBreakpoints();

            log.display("Resume debuggee VM");

            debuggee.VM().resume();

            /*
             * Wait for replay for previous command (resume001a.COMMAND_STOP_ALL_THREADS_AT_BREAKPOINT)
             */
            if (!isDebuggeeReady())
                return;

            pipe.println(resume001a.COMMAND_JOIN_TEST_THREADS);

            if (!isDebuggeeReady())
                return;

            ReferenceType debuggeeClass = debuggee.classByName(debuggeeClassNameWithoutArgs());

            Field counterField = debuggeeClass.fieldByName(resume001a.COUNTER_FIELD_NAME);
            if (counterField == null) {
                setSuccess(false);
                log.complain("Field " + resume001a.COUNTER_FIELD_NAME + " wasn't found in class " + debuggeeClassNameWithoutArgs());
                return;
            }

            IntegerValue value = (IntegerValue) debuggeeClass.getValue(counterField);

            /*
             * Each suspended thread should increment counter after resuming
             */
            if (value.value() != suspendedThreadsNumber) {
                setSuccess(false);
                log.complain("Unexpected value of field " + resume001a.COUNTER_FIELD_NAME +
                        ": " + value.intValue() + ", expected value is " + suspendedThreadsNumber);
            }
        } finally {
            eventHandler.stopEventHandler();
        }
    }
}
