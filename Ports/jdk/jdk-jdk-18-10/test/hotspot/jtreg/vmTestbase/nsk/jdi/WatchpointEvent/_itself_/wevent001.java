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

package nsk.jdi.WatchpointEvent._itself_;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type
 * WatchpointEvent.
 *
 * The test checks that results of methods in the interface
 * <code>com.sun.jdi.WatchpointEvent</code>
 * complies with its spec.
 *
 * The test checks up on the following assertion:
 *    The number of WatchpointEvents received is equal to
 *    the number of WatchpointRequestsset up by a debugger.
 * Tested are both Access and Modification WatchpointEvents.
 *
 * The test works as follows.
 * - The debugger resumes the debuggee and waits for the BreakpointEvent.
 * - The debuggee an object foo of CheckedClass and invokes
 *   the methodForCommunication to be suspended and
 *   to inform the debugger with the event.
 * - Upon getting the BreakpointEvent, the debugger:
 *   - set up enabled WatchpointRequests for all fields,
 *   - resumes the debuggee, and
 *   - waiting for all expected events to receive.
 */

public class wevent001 extends TestDebuggerType1 {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        debuggeeName = "nsk.jdi.WatchpointEvent._itself_.wevent001a";
        return new wevent001().runThis(argv, out);
    }

    private String testedClassName =
       "nsk.jdi.WatchpointEvent._itself_.CheckedClass";

    volatile int mwEventsCount = 0;
    volatile int awEventsCount = 0;
    volatile int requestsCount = 0;

    protected void testRun() {

        if ( !vm.canWatchFieldModification() ) {
            display("......vm.canWatchFieldModification == false :: test cancelled");
            vm.exit(Consts.JCK_STATUS_BASE);
            return;
        }
        if ( !vm.canWatchFieldAccess() ) {
            display("......vm.canWatchFieldAccess == false :: test cancelled");
            vm.exit(Consts.JCK_STATUS_BASE);
            return;
        }

        ReferenceType refType = null;
        List          fields  = null;
        ListIterator  li      = null;
        Field         field   = null;

        ModificationWatchpointRequest mwRequest = null;
        AccessWatchpointRequest       awRequest = null;

        for (int i = 0; ; i++) {

            if (!shouldRunAfterBreakpoint()) {
                vm.resume();
                break;
            }

            display(":::::: case: # " + i);

            switch (i) {
                case 0:
                refType = (ReferenceType) vm.classesByName(testedClassName).get(0);
                fields = refType.fields();
                li = fields.listIterator();

                display("......setting up WatchpointRequests");
                while (li.hasNext()) {
                    field = (Field) li.next();
                    if (!field.isSynthetic()) {
                        mwRequest = eventRManager.createModificationWatchpointRequest(field);
                        mwRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
                        mwRequest.enable();

                        awRequest = eventRManager.createAccessWatchpointRequest(field);
                        awRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
                        awRequest.enable();

                        requestsCount++;
                    }
                }
                display("       # Requests set up : " + requestsCount);

                eventHandler.addListener(
                     new EventHandler.EventListener() {
                         public boolean eventReceived(Event event) {
                            if (event instanceof ModificationWatchpointEvent) {
                                display("Received ModificationWatchpointEvent ");
                                mwEventsCount++;
                                return true;
                            } else if (event instanceof AccessWatchpointEvent) {
                                display("Received AccessWatchpointEvent ");
                                awEventsCount++;
                                return true;
                            }
                            if (gotAllRequestedEvents()) {
                                synchronized (eventHandler) {
                                    eventHandler.notifyAll();
                                }
                            }
                            return false;
                         }
                     }
                );

                display("......waiting for WatchpointEvents");
                vm.resume();

                long timeToFinish = System.currentTimeMillis() + waitTime;
                long timeLeft = waitTime;
                while (!gotAllRequestedEvents() && timeLeft > 0) {
                    try {
                        synchronized (eventHandler) {
                            eventHandler.wait(timeLeft);
                            timeLeft = timeToFinish - System.currentTimeMillis();
                        }
                    } catch (InterruptedException e) {
                        setFailedStatus("InterruptedException was thrown while waiting for the events.");
                        throw new Failure(e);
                    }
                }

                display("......checking up on numbers");
                if ( mwEventsCount != requestsCount ) {
                    setFailedStatus("ERROR: # ModificationWatchpointEvents != # Requests :: " +
                         mwEventsCount + " != " + requestsCount);
                }
                if ( awEventsCount != requestsCount ) {
                    setFailedStatus("ERROR: # AccessWatchpointEvents != # Requests :: " +
                         awEventsCount + " != " + requestsCount);
                }

                display("      # mwEventsCount == " + mwEventsCount);
                display("      # awEventsCount == " + awEventsCount);

                break;

                default:
                throw new Failure("** default case 1 **");
            }
        }
        return;
    }

    boolean gotAllRequestedEvents() {
        return (mwEventsCount >= requestsCount && awEventsCount >= requestsCount);
    }
}
