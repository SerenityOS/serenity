/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

/*
 * Class contains methods common for nsk/jdi/ThreadReference/forceEarlyReturn tests
 */
public class ForceEarlyReturnDebugger extends TestDebuggerType2 {
    protected boolean canRunTest() {
        if (!vm.canForceEarlyReturn()) {
            log.display("TEST CANCELED due to:  vm.canForceEarlyReturn() = false");
            return false;
        } else
            return super.canRunTest();
    }

    protected void testMethodExitEvent(ThreadReference thread, String methodName) {
        testMethodExitEvent(thread, methodName, true);
    }

    /*
     * Method for checking is after forceEarlyReturn MethodExitEvent is generated as it would be in a normal return
     * Before calling this method forceEarlyReturn() should be already called and tested thread should be suspended
     */
    protected void testMethodExitEvent(ThreadReference thread, String methodName, boolean resumeThreadAfterEvent) {
        MethodExitRequest methodExitRequest;
        methodExitRequest = debuggee.getEventRequestManager().createMethodExitRequest();
        methodExitRequest.addThreadFilter(thread);
        methodExitRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        methodExitRequest.enable();

        EventListenerThread listenerThread = new EventListenerThread(methodExitRequest);
        listenerThread.start();
        listenerThread.waitStartListen();

        thread.resume();

        Event event = listenerThread.getEvent();

        if (event == null) {
            setSuccess(false);
            log.complain("MethodExitEvent was not generated " + ", method: " + methodName);
        } else {
            if (!((MethodExitEvent) event).method().name().equals(methodName)) {
                setSuccess(false);
                log.complain("Invalid MethodExitEvent: expected method - " + methodName + ", actually - "
                        + ((MethodExitEvent) event).method().name());
            }
        }

        methodExitRequest.disable();
        vm.eventRequestManager().deleteEventRequest(methodExitRequest);

        if (resumeThreadAfterEvent)
            thread.resume();
    }
}
