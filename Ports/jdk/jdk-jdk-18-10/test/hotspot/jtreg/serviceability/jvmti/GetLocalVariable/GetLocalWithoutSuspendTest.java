/*
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
 * @bug 8249293
 *
 * @summary Test if stack walk to get local variable in the JVMTI implementation is safe if the
 *          target thread is not suspended.
 *
 * @comment The main/target thread uses recursion to build a large stack, then
 *          calls a native method to notify the JVMTI agent thread to get a
 *          local variable deep in the stack. This prolongs the stack walk. The
 *          target thread's stack is walkable while in native. After sending the
 *          notification it waits a while to give the agent time to reach the
 *          stack walk, then it returns from native. This is when its stack
 *          becomes not walkable again.
 *
 * @library /test/lib
 * @compile GetLocalWithoutSuspendTest.java
 * @run main/othervm/native
 *                  -agentlib:GetLocalWithoutSuspendTest
 *                  -Xbatch
 *                  GetLocalWithoutSuspendTest
 */

public class GetLocalWithoutSuspendTest {

    public static final int M = 1 << 20;

    public static final int TEST_ITERATIONS = 200;

    /**
     * Native method to notify the agent thread to call GetLocalObject() on this thread.
     *
     * @param depth Depth of target frame for GetLocalObject() call. Should be
     *        large value to prolong the unsafe stack walk.
     * @param waitTime Time to wait after notify with
     *        walkable stack before returning an becoming unsafe again.
     * @return Dummy value.
     */
    public static native void notifyAgentToGetLocal(int depth, int waitTime);

    /**
     * Notify agent thread that we are shutting down and wait for it to terminate.
     */
    public static native void shutDown();

    /**
     * Provide agent thread with reference to target thread.
     * @param target The target thread
     */
    public static native void setTargetThread(Thread target);

    public static void main(String[] args) throws Exception {
        new GetLocalWithoutSuspendTest().runTest();
    }

    /**
     * Wait cycles in native, i.e. with walkable stack, after notifying agent
     * thread to do GetLocalObject() call.
     */
    public int waitCycles = 1;

    public void runTest() throws Exception {
        log("Set target thread for get local variable calls by agent.");
        setTargetThread(Thread.currentThread());

        log("Test how many frames fit on the stack by performing recursive calls until");
        log("StackOverflowError is thrown");
        int targetDepth = recursiveMethod(0, M);
        log("Testing with target depth: " + targetDepth);

        log("Begin Test.");
        long start = System.currentTimeMillis();
        for (int iterations = 0; iterations < TEST_ITERATIONS; iterations++) {
            long now = System.currentTimeMillis();
            log((now - start) + " ms  Iteration : " + iterations +
                "  waitTime : " + waitCycles);
            int newTargetDepth = recursiveMethod(0, targetDepth);
            if (newTargetDepth < targetDepth) {
                // A StackOverflowError can occur due to (re-)compilation. We
                // don't reach the native method notifyAgentToGetLocal() then
                // which is a prerequisite to trigger the problematic race
                // condition. So we reduce the targetDepth to avoid stack
                // overflow.
                log("StackOverflowError during test.");
                log("Old target depth: " + targetDepth);
                log("Retry with new target depth: " + newTargetDepth);
                targetDepth = newTargetDepth;
            }
            iterations++;
            // Double wait time, but limit to roughly 10^6 cycles.
            waitCycles = (waitCycles << 1) & (M - 1);
            waitCycles = waitCycles == 0 ? 1 : waitCycles;
        }

        // Notify agent thread that we are shutting down and wait for it to terminate.
        shutDown();

        log("Successfully finished test");
    }

    /**
     * Perform recursive calls until the target stack depth is reached or the stack overflows.
     * Call {@link #notifyAgentToGetLocal(int, int)} if the target depth is reached.
     *
     * @param depth Current recursion depth
     * @param targetStackDepth Target recursion depth
     * @return Depth at which the recursion was ended
     */
    public int recursiveMethod(int depth, int targetStackDepth) {
        int maxDepth = depth;
        try {
            if (depth == targetStackDepth) {
                notifyAgentToGetLocal(depth - 100, waitCycles);
            } else {
                maxDepth = recursiveMethod(depth + 1, targetStackDepth);
            }
        } catch (StackOverflowError e) {
            // Don't print message here, because this would likely trigger a new StackOverflowError
        }
        return maxDepth;
    }

    public static void log(String m) {
        System.out.println("### Java-Test: " + m);
    }
}
