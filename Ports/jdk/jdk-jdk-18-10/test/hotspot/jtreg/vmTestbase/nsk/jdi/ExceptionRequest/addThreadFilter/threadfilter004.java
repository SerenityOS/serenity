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

package nsk.jdi.ExceptionRequest.addThreadFilter;

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
 * ExceptionRequest.
 *
 * The test checks that results of the method
 * <code>com.sun.jdi.ExceptionRequest.addThreadFilter()</code>
 * complies with its spec.
 *
 * The test checks up on the following assertion:
 *     Restricts the events generated by this request
 *     to those in the given thread.
 * The cases to test include re-invocations of the method
 * addThreadFilter() on the same ExceptionRequest object.
 * There are two ExceptionRequests to check as follows:
 * (1) For ExceptionRequest2, both invocations are with different
 * ThreadReferences restricting one Exception event to two threads.
 * The test expects no Exception event will be received.
 * (2) For ExceptionRequest1, both invocations are with the same
 * ThreadReference restricting one Exception event to one thread.
 * The test expects this Exception event will be received.
 *
 * The test works as follows.
 * - The debugger resumes the debuggee and waits for the BreakpointEvent.
 * - The debuggee creates two threads, thread1 and thread2, and invokes
 *   the methodForCommunication to be suspended and
 *   to inform the debugger with the event.
 * - Upon getting the BreakpointEvent, the debugger
 *   - sets up ExceptionRequests 1&2 within the method
 *     in the class TestClass which will be calling by both threads,
 *   - restricts the ExceptionRequest1 only to thread1,
 *   - restricts the ExceptionRequest2 to both thread1 and thread2,
 *   - resumes debuggee's main thread, and
 *   - waits for the event.
 * - Debuggee's main thread starts both threads.
 * - Upon getting the event, the debugger performs the checks required.
 */

public class threadfilter004 extends TestDebuggerType1 {

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        debuggeeName = "nsk.jdi.ExceptionRequest.addThreadFilter.threadfilter004a";
        return new threadfilter004().runThis(argv, out);
    }

    private String testedClassName =
      "nsk.jdi.ExceptionRequest.addThreadFilter.threadfilter004aTestClass";

    protected void testRun() {

        EventRequest eventRequest1 = null;
        EventRequest eventRequest2 = null;

        ThreadReference thread1 = null;
        ThreadReference thread2 = null;

        String thread1Name = "thread1";
        String thread2Name = "thread2";

        String property1 = "ExceptionRequest1";
        String property2 = "ExceptionRequest2";

        ReferenceType testClassReference = null;

        for (int i = 0; ; i++) {

            if (!shouldRunAfterBreakpoint()) {
                vm.resume();
                break;
            }

            display(":::::: case: # " + i);

            switch (i) {

                case 0:
                testClassReference =
                     (ReferenceType) vm.classesByName(testedClassName).get(0);

                thread1 = (ThreadReference) debuggeeClass.getValue(
                                            debuggeeClass.fieldByName(thread1Name));
                thread2 = (ThreadReference) debuggeeClass.getValue(
                                            debuggeeClass.fieldByName(thread2Name));

                eventRequest1 = setting2ExceptionRequest (thread1,
                                       testClassReference,
                                       EventRequest.SUSPEND_ALL, property1);
                eventRequest2 = setting2ExceptionRequest (thread1,
                                       testClassReference,
                                       EventRequest.SUSPEND_ALL, property2);

                ((ExceptionRequest) eventRequest1).addThreadFilter(thread1);
                ((ExceptionRequest) eventRequest2).addThreadFilter(thread2);

                display("......waiting for ExceptionEvent in tested thread");
                Event newEvent = eventHandler.waitForRequestedEvent(new EventRequest[]{eventRequest1, eventRequest2}, waitTime, true);

                if ( !(newEvent instanceof ExceptionEvent)) {
                    setFailedStatus("ERROR: new event is not ExceptionEvent");
                } else {
                    String property = (String) newEvent.request().getProperty("number");
                    display("       got new ExceptionEvent with property 'number' == " + property);

                    if ( !property.equals(property1) ) {
                        setFailedStatus("ERROR: property is not : " + property1);
                    }

                    EventRequest newEventRequest = newEvent.request();
                    if (!newEventRequest.equals(eventRequest1) ) {
                        setFailedStatus("The ExceptionEvent occured not for eventRequest1");
                    }

                    ThreadReference thr = ((ExceptionEvent)newEvent).thread();
                    if (!thr.equals(thread1)) {
                        setFailedStatus("The ExceptionEvent occured in unexpected thread: " + thr);
                    }
                }
                vm.resume();
                break;

                default:
                throw new Failure("** default case 2 **");
            }
        }
        return;
    }


    private ExceptionRequest setting2ExceptionRequest ( ThreadReference thread,
                                                        ReferenceType   testedClass,
                                                        int             suspendPolicy,
                                                        String          property       ) {
        try {
            display("......setting up ExceptionRequest:");
            display("       thread: " + thread + "; class: " + testedClass + "; property: " + property);

            ExceptionRequest
            excr = eventRManager.createExceptionRequest(null, true, true);
            excr.putProperty("number", property);
            if (thread != null)
                excr.addThreadFilter(thread);
            excr.setSuspendPolicy(suspendPolicy);

            display("      ExceptionRequest has been set up");
            return excr;
        } catch ( Exception e ) {
            throw new Failure("** FAILURE to set up ExceptionRequest **");
        }
    }
}
