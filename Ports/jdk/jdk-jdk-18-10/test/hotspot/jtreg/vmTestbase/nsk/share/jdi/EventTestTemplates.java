/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MonitorContendedEnterEvent;
import com.sun.jdi.event.MonitorContendedEnteredEvent;
import com.sun.jdi.event.MonitorWaitEvent;
import com.sun.jdi.event.MonitorWaitedEvent;

/*
 * Class contains debugger classes based on JDIEventsDebugger intended for JDI events testing
 */
public class EventTestTemplates {

    // how many times rerun test in case when tested events aren't generated
    static final int MAX_TEST_RUN_NUMBER = 10;

    /*
     * Method contains common code used by EventFilterTests and StressTestTemplate
     */
    static void runTestWithRerunPossibilty(JDIEventsDebugger debugger, Runnable testRunner) {
        for (int i = 0; i < MAX_TEST_RUN_NUMBER; i++) {
            testRunner.run();

            /*
             * If events weren't generated at all but test accepts missing events
             * try to rerun test (rerun test only if it didn't fail yet)
             */
            if (debugger.eventsNotGenerated() && debugger.getSuccess()) {
                if (i < MAX_TEST_RUN_NUMBER - 1) {
                    debugger.log.display("\nWARNING: tested events weren't generated at all, trying to rerun test (rerun attempt " + (i + 1) + ")\n");
                } else {
                    debugger.setSuccess(false);
                    debugger.log.complain("Tested events weren't generated after " + MAX_TEST_RUN_NUMBER + " runs, test FAILED");
                }
            } else {
                break;
            }
        }
    }

    static public boolean isEventFromNSK(Event event, Debugee debuggee) {
        try {
            if (event instanceof MonitorContendedEnterEvent) {
                return ((MonitorContendedEnterEvent) event).location() != null && ((MonitorContendedEnterEvent) event).monitor().type().name().startsWith("nsk.");
            }
            if (event instanceof MonitorContendedEnteredEvent) {
                return ((MonitorContendedEnteredEvent) event).location() != null && ((MonitorContendedEnteredEvent) event).monitor().type().name().startsWith("nsk.");
            }
            if (event instanceof MonitorWaitEvent) {
                return ((MonitorWaitEvent) event).monitor().type().name().startsWith("nsk.");
            }
            if (event instanceof MonitorWaitedEvent) {
                return ((MonitorWaitedEvent) event).monitor().type().name().startsWith("nsk.");
            }
        } catch (ObjectCollectedException ex) {
            // The monitor object the event refers to might be already collected. Ignore this exception.
            debuggee.getLog().display("Exception caught:" + ex);
            return false;
        }
        // don't filter other events
        return true;
    }

}
