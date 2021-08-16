/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jdi;

import com.sun.jdi.IntegerValue;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import java.util.List;
import nsk.share.Log;

public class JDIBase {

    // Exit code constants
    public static final int PASSED = 0;
    public static final int FAILED = 2;
    public static final int PASS_BASE = 95;


    // Log helpers
    private final String sHeader1 = "\n=> " + this.getClass().getName().replace(".", "/") + " ";

    private static final String
            sHeader2 = "--> debugger: ",
            sHeader3 = "##> debugger: ";

    public final void log1(String message) {
        logHandler.display(sHeader1 + message);
    }

    public final void log2(String message) {
        logHandler.display(sHeader2 + message);
    }

    public final void log3(String message) {
        logHandler.complain(sHeader3 + message);
    }

    protected Log logHandler;

    // common variables used by tests
    protected Debugee debuggee;
    protected ArgumentHandler argsHandler;
    protected VirtualMachine vm;
    protected ReferenceType debuggeeClass;
    protected static int testExitCode = PASSED;
    protected long waitTime;

    // used by tests with breakpoint communication
    protected EventRequestManager eventRManager;
    protected EventQueue eventQueue;
    protected EventSet eventSet;
    protected EventIterator eventIterator;

    // additional fields initialized during breakpoint communication
    protected Location breakpLocation = null;
    protected BreakpointEvent bpEvent;

    protected final BreakpointRequest settingBreakpoint(ThreadReference thread,
                                                     ReferenceType testedClass,
                                                     String methodName,
                                                     String bpLine,
                                                     String property)
            throws JDITestRuntimeException {

        log2("......setting up a breakpoint:");
        log2("       thread: " + thread + "; class: " + testedClass +
                "; method: " + methodName + "; line: " + bpLine);

        List alllineLocations = null;
        Location lineLocation = null;
        BreakpointRequest breakpRequest = null;

        try {
            Method method = (Method) testedClass.methodsByName(methodName).get(0);

            alllineLocations = method.allLineLocations();

            int n =
                    ((IntegerValue) testedClass.getValue(testedClass.fieldByName(bpLine))).value();
            if (n > alllineLocations.size()) {
                log3("ERROR:  TEST_ERROR_IN_settingBreakpoint(): number is out of bound of method's lines");
            } else {
                lineLocation = (Location) alllineLocations.get(n);
                breakpLocation = lineLocation;
                try {
                    breakpRequest = eventRManager.createBreakpointRequest(lineLocation);
                    breakpRequest.putProperty("number", property);
                    breakpRequest.addThreadFilter(thread);
                    breakpRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                } catch (Exception e1) {
                    log3("ERROR: inner Exception within settingBreakpoint() : " + e1);
                    breakpRequest = null;
                }
            }
        } catch (Exception e2) {
            log3("ERROR: ATTENTION:  outer Exception within settingBreakpoint() : " + e2);
            breakpRequest = null;
        }

        if (breakpRequest == null) {
            log2("      A BREAKPOINT HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("**FAILURE to set up a breakpoint**");
        }

        log2("      a breakpoint has been set up");
        return breakpRequest;
    }

    protected final void getEventSet() throws JDITestRuntimeException {
        try {
            eventSet = eventQueue.remove(waitTime);
            if (eventSet == null) {
                throw new JDITestRuntimeException("** TIMEOUT while waiting for event **");
            }
            eventIterator = eventSet.eventIterator();
        } catch (Exception e) {
            throw new JDITestRuntimeException("** EXCEPTION while waiting for event ** : " + e);
        }
    }

    // Special version of getEventSet for ThreadStartEvent/ThreadDeathEvent.
    // When ThreadStartRequest and/or ThreadDeathRequest are enabled,
    // we can get the events from system threads unexpected for tests.
    // The method skips ThreadStartEvent/ThreadDeathEvent events
    // for all threads except the expected one.
    protected void getEventSetForThreadStartDeath(String threadName) throws JDITestRuntimeException {
        boolean gotDesiredEvent = false;
        while (!gotDesiredEvent) {
            getEventSet();
            Event event = eventIterator.nextEvent();
            if (event instanceof ThreadStartEvent evt) {
                if (evt.thread().name().equals(threadName)) {
                    gotDesiredEvent = true;
                } else {
                    log2("Got ThreadStartEvent for wrong thread: " + event);
                }
            } else if (event instanceof ThreadDeathEvent evt) {
                if (evt.thread().name().equals(threadName)) {
                    gotDesiredEvent = true;
                } else {
                    log2("Got ThreadDeathEvent for wrong thread: " + event);
                }
            } else {
                // not ThreadStartEvent nor ThreadDeathEvent
                gotDesiredEvent = true;
            }
        }
        // reset the iterator before return
        eventIterator = eventSet.eventIterator();
    }

    protected void breakpointForCommunication() throws JDITestRuntimeException {

        log2("breakpointForCommunication");
        getEventSet();

        Event event = eventIterator.nextEvent();
        if (event instanceof BreakpointEvent) {
            bpEvent = (BreakpointEvent) event;
            return;
        }

        throw new JDITestRuntimeException("** event '" + event + "' IS NOT a breakpoint **");
    }

}
