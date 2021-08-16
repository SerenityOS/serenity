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
 * @summary converted from VM Testbase nsk/jdi/MethodExitEvent/returnValue/returnValue004.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that method 'MethodExitEvent.returnValue()' returns the value that the method will return.
 *     Test checks case when MethodExitEvent are generated for native methods.
 *     Test generates MethodExitEvents for native methods with following return types:
 *         - void
 *         - all primitive types
 *         - array of objects
 *         - String
 *         - Thread
 *         - ThreadGroup
 *         - Class
 *         - ClassLoader
 *         - Object
 *         - wrappers for all primitive types
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.MethodExitEvent.returnValue.returnValue004.returnValue004
 *        nsk.jdi.MethodExitEvent.returnValue.returnValue004.returnValue004a
 * @run main/othervm/native
 *      nsk.jdi.MethodExitEvent.returnValue.returnValue004.returnValue004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.MethodExitEvent.returnValue.returnValue004;

import java.io.*;
import java.util.*;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.*;
import nsk.share.jdi.*;
import nsk.share.jpda.NativeMethodsTestThread;

/*
 * Test checks that method 'MethodExitEvent.returnValue()' returns the value that the method will return
 * (test checks case when events are generated for native methods).
 *
 * For generation MethodExitEvent for methods with different return types class NativeMethodsTestThread is used.
 * This test thread executes native methods with following return types:
 * - void
 * - all primitive types
 * - array of objects
 * - String
 * - Thread
 * - ThreadGroup
 * - Class
 * - ClassLoader
 * - Object
 * - wrappers for all primitive types
 *
 * Returned values are stored in NativeMethodsTestThread's static fields. Test receives MethodExitEvent, obtains value
 * of corresponding static field and compares this value with result of 'MethodExitEvent.returnValue()'.
 */
public class returnValue004 extends TestDebuggerType2 {

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new returnValue004().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return returnValue004a.class.getName();
    }

    protected boolean canRunTest() {
        return vm.canGetMethodReturnValues();
    }

    // event listener handles MethodExitEvents
    class EventListener extends EventHandler.EventListener {
        private int eventCounter;

        private int expectedEventNumber;

        volatile boolean allEventsWereReceived;

        private List<String> expectedTypes = new ArrayList<String>();

        public EventListener() {
            for (String typeName : NativeMethodsTestThread.testedTypesNames)
                expectedTypes.add(typeName);
            expectedEventNumber = expectedTypes.size();
        }

        public boolean eventReceived(Event event) {
            if (event instanceof MethodExitEvent) {

                /*
                 * NativeMethodsTestThread has several methods with different return types which are called
                 * in special manner: "<ReturnTypeName>Method", for example BooleanMethod. Return values for this methods
                 * are stored in NativeMethodsTestThread's static fields which are named "expected<ReturnTypeName>Value",
                 * for example "expectedBooleanValue".
                 * When MethodExitEvent is received EventListener finds method name for received MethodExitEvent and if
                 * method's name has form "<ReturnTypeName>Method" EventListener constructs static field name for corresponding
                 * type, obtains static field value and compares this value with result of MethodExitEvent.returnValue().
                 */
                MethodExitEvent methodExitEvent = (MethodExitEvent) event;
                String methodName = methodExitEvent.method().name();

                int index = methodName.indexOf("Method");

                if (index > 0) {

                    String typeName = methodName.substring(0, index);

                    if (expectedTypes.contains(typeName)) {
                        log.display("Received event for method: " + methodExitEvent.method());
                        expectedTypes.remove(typeName);

                        Value expectedReturnValue;

                        if (typeName.equals("Void")) {
                            expectedReturnValue = vm.mirrorOfVoid();
                        } else {
                            ReferenceType referenceType = debuggee.classByName(NativeMethodsTestThread.class.getName());
                            expectedReturnValue = referenceType.getValue(referenceType.fieldByName("expected" + typeName + "Value"));
                        }

                        Value returnValue = methodExitEvent.returnValue();

                        if (!returnValue.equals(expectedReturnValue)) {
                            setSuccess(false);
                            log.complain("Unexpected return value: " + returnValue + ", expected is " + expectedReturnValue);
                        } else {
                            log.display("Return value for method '" + methodName + "': " + returnValue);
                        }

                        eventCounter++;

                        if (eventCounter == expectedEventNumber) {
                            allEventsWereReceived = true;
                            log.display("All expected events were received");
                            methodExitEvent.request().disable();
                            testStopWicket.unlock();
                        }
                    }
                }

                methodExitEvent.thread().resume();

                return true;
            }

            return false;
        }
    }

    private Wicket testStopWicket = new Wicket();

    public void doTest() {
        // create request for MethodExitEvents generated by NativeMethodsTestThread
        MethodExitRequest request = debuggee.getEventRequestManager().createMethodExitRequest();
        ReferenceType referenceType = debuggee.classByName(NativeMethodsTestThread.class.getName());
        request.addClassFilter(referenceType);
        request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        request.enable();

        // start event handler
        EventHandler eventHandler = new EventHandler(debuggee, log);
        eventHandler.startListening();

        // add event listener which handles MethodExit events
        EventListener listener = new EventListener();
        eventHandler.addListener(listener);

        // start thread generating MethodExitEvent
        pipe.println(returnValue004a.COMMAND_START_TEST_THREAD);

        if (!isDebuggeeReady())
            return;

        // EventListener should notify main thread when all event are received
        testStopWicket.waitFor(argHandler.getWaitTime() * 60000);

        if (!listener.allEventsWereReceived) {
            setSuccess(false);
            log.complain("ERROR: not all events were received");
        }

        pipe.println(returnValue004a.COMMAND_STOP_TEST_THREAD);

        if (!isDebuggeeReady())
            return;

        eventHandler.stopEventHandler();
    }
}
