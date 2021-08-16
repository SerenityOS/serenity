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
import nsk.share.Consts;
import nsk.share.TestBug;

import java.io.PrintStream;
import java.util.ArrayList;

/*
 * Class is intended for stress testing, expected command line parameters:
 * - debuggee class name (e.g.: -debuggeeClassName nsk.share.jdi.MonitorEventsDebuggee)
 * - one or more tested event types (e.g.: -eventTypes MONITOR_CONTENTED_ENTER:MONITOR_CONTENTED_ENTERED)
 * - number of events which should be generated during test execution (e.g.: -eventsNumber 500)
 * - number of threads which simultaneously generate events (e.g.: -threadsNumber 10)
 *
 * Class parses command line and calls method JDIEventsDebugger.stressTestTemplate.
 */
public class StressTestTemplate extends JDIEventsDebugger {
    protected String debuggeeClassName;
    protected EventType[] testedEventTypes;
    protected int eventsNumber = 1;
    protected int threadsNumber = 1;

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new StressTestTemplate().runIt(argv, out);
    }

    protected String[] doInit(String[] args, PrintStream out) {
        args = super.doInit(args, out);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-eventsNumber") && (i < args.length - 1)) {
                eventsNumber = Integer.parseInt(args[i + 1]);

                // for this stress test events number is equivalent of iterations
                // (don't support 0 value of IterationsFactor)
                if (stressOptions.getIterationsFactor() != 0) {
                    eventsNumber *= stressOptions.getIterationsFactor();
                }
                i++;
            } else if (args[i].equals("-threadsNumber") && (i < args.length - 1)) {
                threadsNumber = Integer.parseInt(args[i + 1]);

                // if 'threadsNumber' is specified test should take in account stress options
                threadsNumber *= stressOptions.getThreadsFactor();

                i++;
            } else if (args[i].equals("-debuggeeClassName") && (i < args.length - 1)) {
                debuggeeClassName = args[i + 1];
                i++;
            } else if (args[i].equals("-eventTypes") && (i < args.length - 1)) {
                String[] eventTypesNames = args[i + 1].split(":");
                testedEventTypes = new EventType[eventTypesNames.length];
                try {
                    for (int j = 0; j < eventTypesNames.length; j++) {
                        testedEventTypes[j] = EventType.valueOf(eventTypesNames[j]);
                    }
                } catch (IllegalArgumentException e) {
                    throw new TestBug("Invalid event type", e);
                }

                i++;
            } else {
                standardArgs.add(args[i]);
            }
        }

        if (testedEventTypes == null || testedEventTypes.length == 0) {
            throw new TestBug("Test requires 'eventTypes' parameter");
        }

        if (debuggeeClassName == null) {
            throw new TestBug("Test requires 'debuggeeClassName' parameter");
        }

        return standardArgs.toArray(new String[]{});
    }

    // can't control events from system libraries, so save events only from nsk packages
    protected boolean shouldSaveEvent(Event event) {
        return EventTestTemplates.isEventFromNSK(event, debuggee);
    }

    protected String debuggeeClassName() {
        return debuggeeClassName;
    }

    public void doTest() {
        prepareDebuggee(testedEventTypes);

        EventTestTemplates.runTestWithRerunPossibilty(this,
                () -> stressTestTemplate(testedEventTypes, eventsNumber, threadsNumber));

        eventHandler.stopEventHandler();
        removeDefaultBreakpoint();
    }
}
