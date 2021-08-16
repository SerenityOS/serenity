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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/forceEarlyReturn/forceEarlyReturn001.
 * VM Testbase keywords: [jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         The test checks that a result of the method com.sun.jdi.forceEarlyReturn(Value value)
 *         complies with its specification. The test checks:
 *                 - forceEarlyReturn throws InvalidTypeException exception if the value type does not match the method's return type
 *                 - the return value returned in debuggee VM after forceEarlyReturn is correct one
 *                 - no further instructions are executed in the called method, specifically, finally blocks are not executed
 *                 - MethodExitEvent is generated as it would be in a normal return
 *         Test scenario:
 *         Debuggee VM starts thread(class nsk.share.jpda.ForceEarlyReturnTestThread is used) which sequentially calls
 *         test methods with different return value's type:
 *                 - void
 *                 - all primitive types
 *                 - all wrappers of primitive types
 *                 - String
 *                 - Object
 *                 - array of java.lang.Object
 *                 - Thread
 *                 - ThreadGroup
 *                 - Class object
 *                 - ClassLoader
 *         Debugger VM set breakpoints in all this methods and waits while BreakpointEvent occurs. When debuggee's
 *         test thread stops at breakpoint debugger calls forceEarlyReturn() with following parameters:
 *                 - try pass incompatible value to forceEarlyReturn (expect InvalidTypeException)
 *                 - force thread return with value defined at test thread class
 *         Test thread in debuggee VM checks that value returned from test methods equals predefined value and no
 *         instructions was executed in called method after force return (finally blocks are not executed too).
 *         Debugger checks that MethodExitEvent is generated after forceEarlyReturn.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn001.forceEarlyReturn001
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn001.forceEarlyReturn001a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn001.forceEarlyReturn001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn001;

import java.io.PrintStream;
import java.util.ArrayList;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.Consts;
import nsk.share.jdi.ForceEarlyReturnDebugger;
import nsk.share.jpda.ForceEarlyReturnTestThread;

public class forceEarlyReturn001 extends ForceEarlyReturnDebugger {
    // Data for testing forceEarlyReturn for single method
    // Contains following data:
    // - tested method's name
    // - breakpoint request for location in tested method
    // - incompatible value to pass in forceEarlyReturn() to check that
    // InvalidTypeException is thrown
    // - correct value to pass in forceEarlyReturn()
    class TestData {
        public TestData(ReferenceType referenceType, String methodName, int lineNumber, ThreadReference thread,
                Value returnValue, Value wrongValue) {
            breakpointRequest = debuggee.makeBreakpoint(referenceType, methodName, lineNumber);

            breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            breakpointRequest.addThreadFilter(thread);
            breakpointRequest.enable();

            this.returnValue = returnValue;
            this.incompatibleValue = wrongValue;
            this.methodName = methodName;
        }

        String methodName;

        BreakpointRequest breakpointRequest;

        Value returnValue;

        Value incompatibleValue;
    }

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new forceEarlyReturn001().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return forceEarlyReturn001a.class.getName();
    }

    // initialize test and remove unsupported by nsk.share.jdi.ArgumentHandler
    // arguments
    protected String[] doInit(String args[], PrintStream out) {
        args = super.doInit(args, out);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-totalThreadsCount") && (i < args.length - 1)) {
                totalThreadsCount = Integer.parseInt(args[i + 1]);

                // if threads count is specified, test should take in account threads factor
                totalThreadsCount *= stressOptions.getThreadsFactor();

                i++;
            } else if (args[i].equals("-iterationsNumber") && (i < args.length - 1)) {
                baseIterationsNumber = Integer.parseInt(args[i + 1]);
                i++;
            } else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[] {});
    }

    private int totalThreadsCount = 1;

    private int baseIterationsNumber = 1;

    private TestExecutorThread testThread;

    // predefined in TestThread values to pass in forceEarlyReturn()
    private Value testValues[];

    private Value invalidValues[];

    class TestExecutorThread extends Thread {
        TestData testDataArray[];

        private ThreadReference thread;

        public TestExecutorThread(ReferenceType testedThreadReferenceType, String threadName) {
            thread = debuggee.threadByName(threadName);

            testDataArray = new TestData[ForceEarlyReturnTestThread.testedTypesNames.length];

            for (int i = 0; i < testDataArray.length; i++) {
                testDataArray[i] = new TestData(testedThreadReferenceType,
                        ForceEarlyReturnTestThread.testedTypesNames[i] + "Method",
                        ForceEarlyReturnTestThread.breakpointLines[i], thread, testValues[i], invalidValues[i]);
            }
        }

        public void run() {
            stresser.start(baseIterationsNumber);
            try {
                /*
                 * In this test exit loop condition should be checked before
                 * resuming test thread
                 */
                while (true) {
                    /*
                     * In this test single iteration is test forceEarlyReturn using
                     * all elements of testDataArray
                     */
                    stresser.iteration();

                    boolean stopExecution = false;

                    for (int i = 0; i < testDataArray.length; i++) {
                        testForceEarlyReturn(testDataArray[i]);

                        // check exit loop condition before resuming test thread
                        stopExecution = !stresser.continueExecution() || (stresser.getIterationsLeft() == 0);

                        if (stopExecution) {
                            log.display("Execution finished, stopping test threads");

                            /*
                             * When test completed debuggee should call for test thread
                             * ForceEstopExecution.stopExecution() before this thread is resumed
                             */
                            pipe.println(forceEarlyReturn001a.COMMAND_STOP_TEST_THREADS);

                            if (!isDebuggeeReady())
                                return;

                            thread.resume();

                            break;
                        } else {
                            thread.resume();
                        }
                    }

                    if (stopExecution)
                        break;
                }
            } finally {
                stresser.finish();
            }

            log.display("Waiting for test threads finishing");

            pipe.println(forceEarlyReturn001a.COMMAND_JOIN_TEST_THREADS);

            if (!isDebuggeeReady())
                return;
        }

        private void testForceEarlyReturn(TestData testData) {
            BreakpointEvent breakPointEvent = waitForBreakpoint(testData.breakpointRequest);

            try {
                log.display("Calling forceEarlyReturn with incompatible value: " + testData.incompatibleValue);

                thread.forceEarlyReturn(testData.incompatibleValue);
                setSuccess(false);
                log.complain("Expected InvalidTypeException was not thrown, method: " + testData.methodName);
            } catch (InvalidTypeException e) {
                // expected exception
            } catch (Exception e) {
                setSuccess(false);
                log.complain("Unexpected exception: " + e + ", method: " + testData.methodName);
                e.printStackTrace(System.out);
            }

            try {
                log.display("Calling forceEarlyReturn with value: " + testData.returnValue);
                breakPointEvent.thread().forceEarlyReturn(testData.returnValue);
            } catch (Exception e) {
                setSuccess(false);
                log.complain("Unexpected exception: " + e + ", method: " + testData.methodName);
                e.printStackTrace(System.out);
            }

            testMethodExitEvent(thread, testData.methodName, false);
        }
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
        invalidValues = new Value[ForceEarlyReturnTestThread.testedTypesNames.length + 1];

        testValues[0] = voidValue;

        for (int i = 1; i < ForceEarlyReturnTestThread.testedTypesNames.length; i++)
            testValues[i] = referenceType.getValue(referenceType.fieldByName("expected"
                    + ForceEarlyReturnTestThread.testedTypesNames[i] + "Value"));

        for (int i = 0; i < ForceEarlyReturnTestThread.testedTypesNames.length; i++) {
            invalidValues[i] = referenceType.getValue(referenceType.fieldByName("invalid"
                    + ForceEarlyReturnTestThread.testedTypesNames[i] + "Value"));
        }
    }

    private void startTestThread() {
        ReferenceType referenceType = debuggee.classByName(ForceEarlyReturnTestThread.class.getName());

        testThread = new TestExecutorThread(referenceType, forceEarlyReturn001a.baseTestThreadName + 0);

        testThread.start();
    }

    private void waitTestEnd() {
        try {
            testThread.join();
        } catch (InterruptedException e) {
            unexpectedException(e);
        }
    }

    public void doTest() {
        initTestValues();

        pipe.println(forceEarlyReturn001a.COMMAND_START_AND_SUSPEND_TEST_THREADS + ":" + totalThreadsCount + ":"
                + baseIterationsNumber * stressOptions.getIterationsFactor());

        if (!isDebuggeeReady())
            return;

        startTestThread();

        pipe.println(forceEarlyReturn001a.COMMAND_START_TEST_THREADS_EXECUTION);

        if (!isDebuggeeReady())
            return;

        waitTestEnd();

        // delete all created breakpoints
        for (TestData testData : testThread.testDataArray) {
            debuggee.getEventRequestManager().deleteEventRequest(testData.breakpointRequest);
        }
    }
}
