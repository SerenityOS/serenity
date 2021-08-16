/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4425852
 * @author Robert Field
 *
 * @summary EventQueueDisconnectTest checks to see that
 * VMDisconnectedException is never thrown before VMDisconnectEvent.
 *
 * Failure mode for this test is throwing VMDisconnectedException
 * on vm.eventQueue().remove();
 * Does not use a scaffold since we don't want that hiding the exception.
 *
 * @run build VMConnection
 * @run compile -g EventQueueDisconnectTest.java
 * @run driver EventQueueDisconnectTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;


    /********** target program **********/

class EventQueueDisconnectTarg {
    public static void main(String args[]) {
        for (int i=0; i < 10; ++i) {
            Say(i);
        }
    }
    static void Say(int what) {
        System.out.println("Say " + what);
    }
}

    /********** test program **********/

public class EventQueueDisconnectTest {

    public static void main(String args[]) throws Exception {
        VMConnection connection = new VMConnection(
                                       "com.sun.jdi.CommandLineLaunch:",
                                       VirtualMachine.TRACE_NONE);
        connection.setConnectorArg("main", "EventQueueDisconnectTarg");
        String debuggeeVMOptions = VMConnection.getDebuggeeVMOptions();
        if (!debuggeeVMOptions.equals("")) {
            if (connection.connectorArg("options").length() > 0) {
                throw new IllegalArgumentException("VM options in two places");
            }
            connection.setConnectorArg("options", debuggeeVMOptions);
        }
        VirtualMachine vm = connection.open();
        EventRequestManager requestManager = vm.eventRequestManager();
        MethodEntryRequest req = requestManager.createMethodEntryRequest();
        req.addClassFilter("EventQueueDisconnectTarg");
        req.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        req.enable();

        // We need to have the BE stop when VMDeath comes
        VMDeathRequest ourVMDeathRequest = requestManager.createVMDeathRequest();
        ourVMDeathRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        ourVMDeathRequest.enable();

        vm.resume();
        while (true) {
            EventSet set = vm.eventQueue().remove();
            Event event = set.eventIterator().nextEvent();

            System.err.println("EventSet with: " + event.getClass());

            if (event instanceof VMDisconnectEvent) {
                System.err.println("Disconnecting successfully");
                break;
            }

            if (event instanceof VMDeathEvent) {
                System.err.println("Pausing after VM death");

                // sleep a few seconds
                try {
                    Thread.sleep(40 * 1000);
                } catch (InterruptedException exc) {
                    // ignore
                }
            }

            set.resume();
        }

        System.err.println("EventQueueDisconnectTest passed");
    }
}
