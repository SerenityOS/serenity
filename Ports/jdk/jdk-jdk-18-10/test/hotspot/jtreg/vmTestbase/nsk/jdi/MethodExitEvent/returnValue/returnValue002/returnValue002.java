/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @modules java.base/jdk.internal.misc:+open
 *
 * @summary converted from VM Testbase nsk/jdi/MethodExitEvent/returnValue/returnValue002.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     Test checks that method 'MethodExitEvent.returnValue()' returns VoidValue when event is generated
 *     for static initializer, constructor and static method.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.MethodExitEvent.returnValue.returnValue002.returnValue002
 *        nsk.jdi.MethodExitEvent.returnValue.returnValue002.returnValue002a
 * @run main/othervm
 *      nsk.jdi.MethodExitEvent.returnValue.returnValue002.returnValue002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.MethodExitEvent.returnValue.returnValue002;

import java.io.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.*;
import nsk.share.jdi.*;

/*
 * Test checks that method 'MethodExitEvent.returnValue()' returns VoidValue for static initializer, class constructor
 * and static method.
 */
public class returnValue002 extends TestDebuggerType2 {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new returnValue002().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return returnValue002a.class.getName();
    }

    protected boolean canRunTest() {
        return vm.canGetMethodReturnValues();
    }

    class EventListener extends EventHandler.EventListener {

        private volatile boolean allEventsWereReceived;

        private boolean staticInitializerEventReceived;
        private boolean constructorEventReceived;
        private boolean staticMethodEventReceived;

        public boolean eventReceived(Event event) {
            if (event instanceof MethodExitEvent) {
                MethodExitEvent methodExitEvent = (MethodExitEvent) event;

                log.display("Received event for method: " + methodExitEvent.method());

                if (methodExitEvent.method().name().equals("<clinit>")) {
                    log.display("Static initializer event received");
                    staticInitializerEventReceived  = true;
                }

                if (methodExitEvent.method().name().equals("<init>")) {
                    log.display("Constructor event received");
                    constructorEventReceived  = true;
                }

                if (methodExitEvent.method().name().equals("staticMethod")) {
                    log.display("Static method event received");
                    staticMethodEventReceived  = true;
                }

                if (!methodExitEvent.returnValue().equals(vm.mirrorOfVoid())) {
                    setSuccess(false);
                    log.complain("Unexpected return value: " + methodExitEvent.returnValue() + ", expected is " + vm.mirrorOfVoid());
                }

                // expect events for static initializer, constructor, static method
                if (staticInitializerEventReceived && constructorEventReceived && staticMethodEventReceived) {
                    allEventsWereReceived = true;
                    log.display("All expected events were received");
                    methodExitEvent.request().disable();
                    testStopWicket.unlock();
                }

                methodExitEvent.thread().resume();

                return true;
            }

            return false;
        }
    }

    private Wicket testStopWicket = new Wicket();

    public void doTest() {
        // create MethodExitRequest for events generated for returnValue002a.TestClass methods
        MethodExitRequest request = debuggee.getEventRequestManager().createMethodExitRequest();
        request.addClassFilter(returnValue002a.TestClass.class.getName());
        request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        request.enable();

        // start event handler
        EventHandler eventHandler = new EventHandler(debuggee, log);
        eventHandler.startListening();

        // add event listener which handles MethodExitEvents
        EventListener listener = new EventListener();
        eventHandler.addListener(listener);

        pipe.println(returnValue002a.COMMAND_START_TEST_THREAD);

        if (!isDebuggeeReady())
            return;

        // EventListener should notify main thread when all event are received
        testStopWicket.waitFor(argHandler.getWaitTime() * 60000);

        if (!listener.allEventsWereReceived) {
            setSuccess(false);
            log.complain("ERROR: not all events were received, possible there is error in test logic");
        }

        pipe.println(returnValue002a.COMMAND_STOP_TEST_THREAD);

        if (!isDebuggeeReady())
            return;

        eventHandler.stopEventHandler();
    }
}
