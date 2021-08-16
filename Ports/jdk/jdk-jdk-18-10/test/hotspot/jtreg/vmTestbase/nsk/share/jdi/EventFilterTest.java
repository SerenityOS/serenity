/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jdi.event.Event;
import nsk.share.TestBug;

import java.io.PrintStream;
import java.util.ArrayList;

/*
 * Debugger class for testing event filters, subclasses should parse command
 * line and call method JDIEventsDebugger.eventFilterTestTemplate with
 * required arguments.
 *
 * Class handles common for event filter tests parameters:
 * - debuggee class name (e.g. -debuggeeClassName nsk.share.jdi.MonitorEventsDebuggee)
 * - tested event type (e.g. -eventType MONITOR_CONTENTED_ENTER)
 */
public abstract class EventFilterTest extends JDIEventsDebugger {
    protected String debuggeeClassName;

    protected EventType eventType;

    protected String[] doInit(String[] args, PrintStream out) {
        args = super.doInit(args, out);

        ArrayList<String> standardArgs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-debuggeeClassName") && (i < args.length - 1)) {
                debuggeeClassName = args[i + 1];
                i++;
            } else if (args[i].equals("-eventType") && (i < args.length - 1)) {
                try {
                    eventType = EventType.valueOf(args[i + 1]);
                } catch (IllegalArgumentException e) {
                    throw new TestBug("Invalid event type : " + args[i + 1], e);
                }
                i++;
            } else
                standardArgs.add(args[i]);
        }

        if (eventType == null) {
            throw new TestBug("Test requires 'eventType' parameter");
        }
        if (debuggeeClassName == null) {
            throw new TestBug("Test requires 'debuggeeClassName' parameter");
        }
        return standardArgs.toArray(new String[]{});
    }

    abstract protected int getTestFiltersNumber();

    public final void doTest() {
        prepareDebuggee(new EventType[]{eventType});

        int filtersNumber = getTestFiltersNumber();
        if (filtersNumber <= 0) {
            throw new TestBug("Invalid filtersNumber: " + filtersNumber);
        }

        for (int i = 0; i < filtersNumber; i++) {
            int filterIndex = i;
            EventTestTemplates.runTestWithRerunPossibilty(this,
                    () -> eventFilterTestTemplate(eventType, filterIndex));
        }

        eventHandler.stopEventHandler();
        removeDefaultBreakpoint();
    }

    // can't control events from system libraries, so save events only from nsk packages
    protected boolean shouldSaveEvent(Event event) {
        return EventTestTemplates.isEventFromNSK(event, debuggee);
    }

    protected String debuggeeClassName() {
        return debuggeeClassName;
    }
}
