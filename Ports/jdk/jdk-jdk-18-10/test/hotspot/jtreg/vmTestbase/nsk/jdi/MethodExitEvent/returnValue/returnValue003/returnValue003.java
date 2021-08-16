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
 *
 * @summary converted from VM Testbase nsk/jdi/MethodExitEvent/returnValue/returnValue003.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that method 'MethodExitEvent.returnValue()' returns the value that the method will return.
 *     Test generates MethodExitEvents for methods with following return types:
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
 *     Test checks case when MethodExitEvent is generated when method completes because of debugger calls method
 *     ThreadReference.forceEarlyReturn().
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.MethodExitEvent.returnValue.returnValue003.returnValue003
 *        nsk.jdi.MethodExitEvent.returnValue.returnValue003.returnValue003a
 * @run main/othervm
 *      nsk.jdi.MethodExitEvent.returnValue.returnValue003.returnValue003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.MethodExitEvent.returnValue.returnValue003;

import java.io.PrintStream;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.Consts;
import nsk.share.jdi.TestDebuggerType2;
import nsk.share.jpda.ForceEarlyReturnTestThread;

/*
 * Test checks that method 'MethodExitEvent.returnValue()' returns the value that the method will return.
 * Test checks case when MethodExitEvent is generated when method completes because of debugger calls method
 * ThreadReference.forceEarlyReturn().
 *
 * For generation MethodExitEvent for methods with different return types class ForceEarlyReturnTestThread is used.
 * Values intended to return through ThreadReference.ForceEarlyReturn are stored in ForceEarlyReturnTestThread's static
 * fields.
 *
 * This test thread executes methods with following return types:
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
 * Debugger sets breakpoints in all this methods and waits while BreakpointEvent occurs. When debuggee's
 * test thread stops at breakpoint debugger calls forceEarlyReturn() with corresponding value, creates MethodExitEvent
 * for test thread and resumes test thread. When MethodExitEvent is received debugger checks that
 * MethodExitEvent.returnValue() returns the same value which was passed to the ThreadReference,forceEarlyReturn().
 */
public class returnValue003 extends TestDebuggerType2 {
    // Data for testing forceEarlyReturn for single method
    // Contains following data:
    // - tested method's name
    // - breakpoint request for location in tested method
    // InvalidTypeException is thrown
    // - correct value to pass in forceEarlyReturn()
    class TestData {
        public TestData(ReferenceType referenceType, String methodName, int lineNumber, ThreadReference thread, Value returnValue) {
            breakpointRequest = debuggee.makeBreakpoint(referenceType, methodName, lineNumber);

            breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            breakpointRequest.addThreadFilter(thread);
            breakpointRequest.enable();

            this.returnValue = returnValue;
            this.methodName = methodName;
        }

        String methodName;

        BreakpointRequest breakpointRequest;

        Value returnValue;
    }

    protected boolean canRunTest() {
        return vm.canGetMethodReturnValues();
    }

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new returnValue003().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return returnValue003a.class.getName();
    }

    private TestExecutorThread testThread;

    // predefined in TestThread values to pass in forceEarlyReturn()
    private Value testValues[];

    class TestExecutorThread extends Thread {
        TestData testData[];

        private ThreadReference thread;

        public TestExecutorThread(ReferenceType testedThreadReferenceType, String threadName) {
            thread = debuggee.threadByName(threadName);

            testData = new TestData[ForceEarlyReturnTestThread.testedTypesNames.length];

            for (int i = 0; i < testData.length; i++) {
                testData[i] = new TestData(testedThreadReferenceType, ForceEarlyReturnTestThread.testedTypesNames[i] + "Method",
                        ForceEarlyReturnTestThread.breakpointLines[i], thread, testValues[i]);
            }
        }

        public void run() {
            for (int i = 0; i < testData.length; i++)
                test(testData[i]);

            log.display("Test executor thread exit");
        }

        public void test(TestData testData) {
            BreakpointEvent breakPointEvent = waitForBreakpoint(testData.breakpointRequest);

            try {
                log.display("Call forceEarlyReturn with value: " + testData.returnValue);
                breakPointEvent.thread().forceEarlyReturn(testData.returnValue);
            } catch (Exception e) {
                setSuccess(false);
                log.complain("Unexpected exception: " + e + ", method: " + testData.methodName);
                e.printStackTrace(System.out);
            }

            testMethodExitEvent(thread, testData.methodName, testData.returnValue);
        }
    }

    protected void testMethodExitEvent(ThreadReference thread, String methodName, Value expectedValue) {
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
            MethodExitEvent methodExitEvent = (MethodExitEvent) event;
            if (!methodExitEvent.method().name().equals(methodName)) {
                setSuccess(false);
                log.complain("Invalid MethodExitEvent: expected method - " + methodName + ", actually - "
                                + methodExitEvent.method().name());
            } else {
                Value returnValue = methodExitEvent.returnValue();

                if (!returnValue.equals(expectedValue)) {
                    setSuccess(false);
                    log.complain("Unexpected result of MethodExitEvent.returnValue(): " + returnValue + ", expected is " + expectedValue);
                } else {
                    log.display("Result of MethodExitEvent.returnValue():" + returnValue);
                }
            }
        }

        methodExitRequest.disable();

        thread.resume();
    }

    // initialize Values objects to pass in forceEarlyReturn()
    protected void initTestValues() {
        ReferenceType referenceType = debuggee.classByName(ForceEarlyReturnTestThread.class.getName());

        Value voidValue = createVoidValue();

        if (voidValue == null) {
            setSuccess(false);
            log.complain("Can't create void value");
            return;
        }

        testValues = new Value[ForceEarlyReturnTestThread.testedTypesNames.length + 1];

        testValues[0] = voidValue;

        for (int i = 1; i < ForceEarlyReturnTestThread.testedTypesNames.length; i++)
            testValues[i] = referenceType.getValue(referenceType.fieldByName("expected" + ForceEarlyReturnTestThread.testedTypesNames[i] + "Value"));
    }

    private void startTestThread() {
        ReferenceType referenceType = debuggee.classByName(ForceEarlyReturnTestThread.class.getName());

        testThread = new TestExecutorThread(referenceType, returnValue003a.testThreadName);
        testThread.start();
    }

    private void waitTestEnd() {
        try {
            testThread.join();
        } catch (InterruptedException e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }
    }

    public void doTest() {
        initTestValues();

        // start ForceEarlyReturnThread to let create breakpoints for this thread
        pipe.println(returnValue003a.COMMAND_START_AND_SUSPEND_TEST_THREAD);

        if (!isDebuggeeReady())
            return;

        startTestThread();

        pipe.println(returnValue003a.COMMAND_START_TEST_THREAD_EXECUTION);

        if (!isDebuggeeReady())
            return;

        waitTestEnd();

        pipe.println(returnValue003a.COMMAND_STOP_TEST_THREAD);

        if (!isDebuggeeReady())
            return;
    }
}
