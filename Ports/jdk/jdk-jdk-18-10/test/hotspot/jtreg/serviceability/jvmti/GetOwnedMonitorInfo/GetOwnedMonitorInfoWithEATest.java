/*
 * Copyright (c) 2019 SAP SE. All rights reserved.
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
 * @bug 8230677
 * @summary Test JVMTI's GetOwnedMonitorInfo with scalar replaced objects and eliminated locks on stack (optimizations based on escape analysis).
 * @comment Without RFE 8227745 escape analysis needs to be switched off to pass the test. For the implementation of RFE 8227745 it serves as a regression test.
 * @requires (vm.compMode != "Xcomp" & vm.compiler2.enabled)
 * @requires vm.jvmti
 * @library /test/lib
 * @compile GetOwnedMonitorInfoWithEATest.java
 * @run main/othervm/native
 *                  -agentlib:GetOwnedMonitorInfoWithEATest
 *                  -XX:+UnlockDiagnosticVMOptions
 *                  -Xms128m -Xmx128m
 *                  -XX:CompileCommand=dontinline,*::dontinline_*
 *                  -XX:+PrintCompilation
 *                  -XX:+PrintInlining
 *                  -XX:-TieredCompilation
 *                  -Xbatch
 *                  -XX:CICompilerCount=1
 *                  -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                  GetOwnedMonitorInfoWithEATest
 * @run main/othervm/native
 *                  -agentlib:GetOwnedMonitorInfoWithEATest
 *                  -XX:+UnlockDiagnosticVMOptions
 *                  -Xms128m -Xmx128m
 *                  -XX:CompileCommand=dontinline,*::dontinline_*
 *                  -XX:+PrintCompilation
 *                  -XX:+PrintInlining
 *                  -XX:-TieredCompilation
 *                  -Xbatch
 *                  -XX:CICompilerCount=1
 *                  -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -XX:-EliminateLocks -XX:+EliminateNestedLocks
 *                  GetOwnedMonitorInfoWithEATest
 * @run main/othervm/native
 *                  -agentlib:GetOwnedMonitorInfoWithEATest
 *                  -XX:+UnlockDiagnosticVMOptions
 *                  -Xms128m -Xmx128m
 *                  -XX:CompileCommand=dontinline,*::dontinline_*
 *                  -XX:+PrintCompilation
 *                  -XX:+PrintInlining
 *                  -XX:-TieredCompilation
 *                  -Xbatch
 *                  -XX:CICompilerCount=1
 *                  -XX:+DoEscapeAnalysis -XX:-EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                  GetOwnedMonitorInfoWithEATest
 * @run main/othervm/native
 *                  -agentlib:GetOwnedMonitorInfoWithEATest
 *                  -XX:+UnlockDiagnosticVMOptions
 *                  -Xms128m -Xmx128m
 *                  -XX:CompileCommand=dontinline,*::dontinline_*
 *                  -XX:+PrintCompilation
 *                  -XX:+PrintInlining
 *                  -XX:-TieredCompilation
 *                  -Xbatch
 *                  -XX:CICompilerCount=1
 *                  -XX:-DoEscapeAnalysis -XX:-EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                  GetOwnedMonitorInfoWithEATest
 */

import jdk.test.lib.Asserts;

public class GetOwnedMonitorInfoWithEATest {

    public static final int COMPILE_THRESHOLD = 20000;

    /**
     * Native wrapper arround JVMTI's GetOwnedMonitorInfo().
     * @param t The thread for which the owned monitors information should be retrieved.
     * @param ownedMonitors Array filled in by the call with the objects associated
     *        with the monitors owned by the given thread.
     * @param depths Per owned monitor the depth of the frame were it was locked.
     *        Filled in by the call
     * @return Number of monitors owned by the given thread.
     */
    public static native int getOwnedMonitorInfo(Thread t, Object[] ownedMonitors);

    public static void main(String[] args) throws Exception {
        new GetOwnedMonitorInfoWithEATest().runTest();
    }

    public void runTest() throws Exception {
        new TestCase_1().run();
        new TestCase_2().run();
    }

    public static abstract class TestCaseBase implements Runnable {

        public long checkSum;
        public boolean doLoop;
        public volatile long loopCount;
        public volatile boolean targetIsInLoop;

        public void run() {
            try {
                msgHL("Executing test case " + getClass().getName());
                warmUp();
                runTest();
            } catch (Exception e) {
                Asserts.fail("Unexpected Exception", e);
            }
        }

        public void warmUp() {
            int callCount = COMPILE_THRESHOLD + 1000;
            doLoop = true;
            while (callCount-- > 0) {
                dontinline_testMethod();
            }
        }

        public abstract void runTest() throws Exception;
        public abstract void dontinline_testMethod();

        public long dontinline_endlessLoop() {
            long cs = checkSum;
            while (doLoop && loopCount-- > 0) {
                targetIsInLoop = true;
                checkSum += checkSum % ++cs;
            }
            loopCount = 3;
            targetIsInLoop = false;
            return checkSum;
        }

        public void waitUntilTargetThreadHasEnteredEndlessLoop() throws Exception {
            while(!targetIsInLoop) {
                msg("Target has not yet entered the loop. Sleep 200ms.");
                try { Thread.sleep(200); } catch (InterruptedException e) { /*ignore */ }
            }
            msg("Target has entered the loop.");
        }

        public void terminateEndlessLoop() throws Exception {
            msg("Terminate endless loop");
            do {
                doLoop = false;
            } while(targetIsInLoop);
        }

        public void msg(String m) {
            System.out.println();
            System.out.println("### " + m);
            System.out.println();
        }

        public void msgHL(String m) {
            System.out.println();
            System.out.println("#####################################################");
            System.out.println("### " + m);
            System.out.println("###");
            System.out.println();
        }
    }

    /**
     * Starts target thread T and then queries monitor information for T using JVMTI's GetOwnedMonitorInfo().
     * With escape analysis enabled the jit compiled method {@link #dontinline_testMethod()} has
     * scalar replaced objects with eliminated (nested) locking in scope when the monitor
     * information is retrieved. Effectively the objects escape through the JVMTI call. This works
     * only with RFE 8227745. Without it escape analysis needs to be disabled.
     */
    public static class TestCase_1 extends TestCaseBase {

        public void runTest() throws Exception {
            loopCount = 1L << 62; // endless loop
            Thread t1 = new Thread(() -> dontinline_testMethod(), "Target Thread");
            t1.start();
            try {
                waitUntilTargetThreadHasEnteredEndlessLoop();
                int expectedMonitorCount = 1;
                int resultSize = expectedMonitorCount + 3;
                Object[] ownedMonitors = new Object[resultSize];
                msg("Get monitor info");
                int monitorCount = getOwnedMonitorInfo(t1, ownedMonitors);
                terminateEndlessLoop();
                t1.join();
                Asserts.assertGreaterThanOrEqual(monitorCount, 0, "getOwnedMonitorsFor() call failed");
                msg("Monitor info:");
                for (int i = 0; i < monitorCount; i++) {
                    System.out.println(i + ": cls=" + (ownedMonitors[i] != null ? ownedMonitors[i].getClass() : null));
                }
                Asserts.assertEQ(monitorCount, expectedMonitorCount, "unexpected monitor count returned by getOwnedMonitorsFor()");
                Asserts.assertNotNull(ownedMonitors[0]);
                Asserts.assertSame(ownedMonitors[0].getClass(), LockCls.class);
            } finally {
                terminateEndlessLoop();
                t1.join();
            }
        }

        public void dontinline_testMethod() {
            LockCls l1 = new LockCls();        // to be scalar replaced
            synchronized (l1) {
                inlinedTestMethodWithNestedLocking(l1);
            }
        }

        public void inlinedTestMethodWithNestedLocking(LockCls l1) {
            synchronized (l1) {              // nested
                dontinline_endlessLoop();
            }
        }
    }

    /**
     * Similar to {@link TestCase_1}. Additionally the target thread T has got eliminated locking
     * for a synchronized method of a different type {@linkplain LockCls2}.
     */
    public static class TestCase_2 extends TestCaseBase {

        public void runTest() throws Exception {
            loopCount = 1L << 62; // endless loop
            Thread t1 = new Thread(() -> dontinline_testMethod(), "Target Thread");
            t1.start();
            try {
                waitUntilTargetThreadHasEnteredEndlessLoop();
                int expectedMonitorCount = 2;
                int resultSize = expectedMonitorCount + 3;
                Object[] ownedMonitors = new Object[resultSize];
                msg("Get monitor info");
                int monitorCount = getOwnedMonitorInfo(t1, ownedMonitors);
                terminateEndlessLoop();
                t1.join();
                Asserts.assertGreaterThanOrEqual(monitorCount, 0, "getOwnedMonitorsFor() call failed");
                msg("Monitor info:");
                for (int i = 0; i < monitorCount; i++) {
                    System.out.println(i + ": cls=" + (ownedMonitors[i] != null ? ownedMonitors[i].getClass() : null));
                }
                Asserts.assertEQ(monitorCount, expectedMonitorCount, "unexpected monitor count returned by getOwnedMonitorsFor()");
                Asserts.assertNotNull(ownedMonitors[0]);
                Asserts.assertSame(ownedMonitors[0].getClass(), LockCls2.class);

                Asserts.assertNotNull(ownedMonitors[1]);
                Asserts.assertSame(ownedMonitors[1].getClass(), LockCls.class);
            } finally {
                terminateEndlessLoop();
                t1.join();
            }
        }

        public void dontinline_testMethod() {
            LockCls l1 = new LockCls();
            synchronized (l1) {
                inlinedTestMethodWithNestedLocking(l1);
            }
        }

        public void inlinedTestMethodWithNestedLocking(LockCls l1) {
            synchronized (l1) {
                dontinline_testMethod2();
            }
        }

        public void dontinline_testMethod2() {
            // Call synchronized method. Receiver of the call will be scalar replaced,
            // and locking will be eliminated. Here we use a different type.
            new LockCls2().inline_synchronized_testMethod(this);
        }
    }

    public static class LockCls {
    }

    public static class LockCls2 {
        public synchronized void inline_synchronized_testMethod(TestCaseBase testCase) {
            testCase.dontinline_endlessLoop();
        }
    }
}
