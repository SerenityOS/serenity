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
 * @bug 8230956
 * @summary JVMTI agents can obtain references to not escaping objects using JVMTI Heap functions.
 *          Therefore optimizations based on escape analysis have to be reverted,
 *          i.e. scalar replaced objects need to be reallocated on the heap and objects with eliminated locking
 *          need to be relocked.
 * @requires ((vm.compMode == "Xmixed") & vm.compiler2.enabled & vm.jvmti)
 * @library /test/lib /test/hotspot/jtreg
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @compile IterateHeapWithEscapeAnalysisEnabled.java
 *
 * @comment BLOCK BEGIN EXCLUSIVE TESTCASES {
 *
 *          The following test cases are executed in fresh VMs because they require that the
 *          capability can_tag_objects is not taken until dontinline_testMethod is jit compiled and
 *          an activation of the compiled version is on stack of the target thread.
 *
 *          Without JDK-8227745 these test cases require that escape analysis is disabled at
 *          start-up because can_tag_objects can be taken lazily, potentially after loading an
 *          agent dynamically by means of the attach API. Disabling escape analysis and invalidating
 *          compiled methods does not help then because there may be compiled frames with ea-based
 *          optimizations on stack. Just like in this collection of test cases.
 *
 * @run main/othervm/native
 *                  -agentlib:IterateHeapWithEscapeAnalysisEnabled
 *                  -XX:+UnlockDiagnosticVMOptions
 *                  -Xms256m -Xmx256m
 *                  -XX:+PrintCompilation -XX:+PrintInlining
 *                  -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                  -Xbatch
 *                  -XX:CompileCommand=dontinline,*::dontinline_*
 *                  -XX:+DoEscapeAnalysis
 *                  IterateHeapWithEscapeAnalysisEnabled IterateOverReachableObjects
 * @run main/othervm/native
 *                  -agentlib:IterateHeapWithEscapeAnalysisEnabled
 *                  -XX:+UnlockDiagnosticVMOptions
 *                  -Xms256m -Xmx256m
 *                  -XX:+PrintCompilation -XX:+PrintInlining
 *                  -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                  -Xbatch
 *                  -XX:CompileCommand=dontinline,*::dontinline_*
 *                  -XX:+DoEscapeAnalysis
 *                  IterateHeapWithEscapeAnalysisEnabled IterateOverHeap
 * @run main/othervm/native
 *                  -agentlib:IterateHeapWithEscapeAnalysisEnabled
 *                  -XX:+UnlockDiagnosticVMOptions
 *                  -Xms256m -Xmx256m
 *                  -XX:+PrintCompilation -XX:+PrintInlining
 *                  -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                  -Xbatch
 *                  -XX:CompileCommand=dontinline,*::dontinline_*
 *                  -XX:+DoEscapeAnalysis
 *                  IterateHeapWithEscapeAnalysisEnabled IterateOverInstancesOfClass
 * @run main/othervm/native
 *                  -agentlib:IterateHeapWithEscapeAnalysisEnabled
 *                  -XX:+UnlockDiagnosticVMOptions
 *                  -Xms256m -Xmx256m
 *                  -XX:+PrintCompilation -XX:+PrintInlining
 *                  -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                  -Xbatch
 *                  -XX:CompileCommand=dontinline,*::dontinline_*
 *                  -XX:+DoEscapeAnalysis
 *                  IterateHeapWithEscapeAnalysisEnabled FollowReferences
 * @run main/othervm/native
 *                  -agentlib:IterateHeapWithEscapeAnalysisEnabled
 *                  -XX:+UnlockDiagnosticVMOptions
 *                  -Xms256m -Xmx256m
 *                  -XX:+PrintCompilation -XX:+PrintInlining
 *                  -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                  -Xbatch
 *                  -XX:CompileCommand=dontinline,*::dontinline_*
 *                  -XX:+DoEscapeAnalysis
 *                  IterateHeapWithEscapeAnalysisEnabled IterateThroughHeap
 *
 * @comment } BLOCK END EXCLUSIVE TESTCASES
 *
 * @comment BLOCK BEGIN NON EXCLUSIVE TESTCASES {
 *
 * @run main/othervm/native
 *                  -agentlib:IterateHeapWithEscapeAnalysisEnabled
 *                  -XX:+UnlockDiagnosticVMOptions
 *                  -Xms256m -Xmx256m
 *                  -XX:CompileCommand=dontinline,*::dontinline_*
 *                  -XX:+PrintCompilation
 *                  -XX:+PrintInlining
 *                  -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                  -Xbatch
 *                  -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                  IterateHeapWithEscapeAnalysisEnabled
 * @run main/othervm/native
 *                  -agentlib:IterateHeapWithEscapeAnalysisEnabled
 *                  -XX:+UnlockDiagnosticVMOptions
 *                  -Xms256m -Xmx256m
 *                  -XX:CompileCommand=dontinline,*::dontinline_*
 *                  -XX:+PrintCompilation
 *                  -XX:+PrintInlining
 *                  -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                  -Xbatch
 *                  -XX:+DoEscapeAnalysis -XX:-EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                  IterateHeapWithEscapeAnalysisEnabled
 * @run main/othervm/native
 *                  -agentlib:IterateHeapWithEscapeAnalysisEnabled
 *                  -XX:+UnlockDiagnosticVMOptions
 *                  -Xms256m -Xmx256m
 *                  -XX:CompileCommand=dontinline,*::dontinline_*
 *                  -XX:+PrintCompilation
 *                  -XX:+PrintInlining
 *                  -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                  -Xbatch
 *                  -XX:-DoEscapeAnalysis -XX:-EliminateAllocations -XX:+EliminateLocks -XX:+EliminateNestedLocks
 *                  IterateHeapWithEscapeAnalysisEnabled
 *
 * @comment } BLOCK END NON EXCLUSIVE TESTCASES
 */

import compiler.whitebox.CompilerWhiteBoxTest;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

public class IterateHeapWithEscapeAnalysisEnabled {

    public static final WhiteBox WB = WhiteBox.getWhiteBox();

    public static final int COMPILE_THRESHOLD = CompilerWhiteBoxTest.THRESHOLD;

    public static native int jvmtiTagClass(Class<?> cls, long tag);

    // Methods to tag or count instances of a given class available in JVMTI
    public static enum TaggingAndCountingMethods {
        IterateOverReachableObjects,
        IterateOverHeap,
        IterateOverInstancesOfClass,
        FollowReferences,
        IterateThroughHeap
    }

    public static native int acquireCanTagObjectsCapability();
    public static native int registerMethod(TaggingAndCountingMethods m, String name);
    public static native void agentTearDown();

    /**
     * Count and tag instances of a given class.
     * @param cls Used by the method {@link TaggingAndCountingMethods#IterateOverInstancesOfClass} as class to count and tag instances of.
     *        Ignored by other counting methods.
     * @param clsTag Tag of the class to count and tag instances of. Used by all methods except
     *        {@link TaggingAndCountingMethods#IterateOverInstancesOfClass}
     * @param instanceTag The tag to be set for selected instances.
     * @param method JVMTI counting and tagging method to be used.
     * @return The number of instances or -1 if the call fails.
     */
    public static native int countAndTagInstancesOfClass(Class<?> cls, long clsTag, long instanceTag, TaggingAndCountingMethods method);

    /**
     * Get all objects tagged with the given tag.
     * @param tag The tag used to select objects.
     * @param result Selected objects are copied into this array.
     * @return -1 to indicated failure and 0 for success.
     */
    public static native int getObjectsWithTag(long tag, Object[] result);

    public static void main(String[] args) throws Exception {
        try {
            new IterateHeapWithEscapeAnalysisEnabled().runTestCases(args);
        } finally {
            agentTearDown();
        }
    }

    public void runTestCases(String[] args) throws Exception {
        // register various instance tagging and counting methods with agent
        for (TaggingAndCountingMethods m : TaggingAndCountingMethods.values()) {
            msg("register instance count method " + m.name());
            int rc = registerMethod(m, m.name());
            Asserts.assertGreaterThanOrEqual(rc, 0, "method " + m.name() + " is unknown to agent");
        }

        if (args.length > 0) {
            // EXCLUSIVE TEST CASES
            // cant_tag_objects is acquired after warmup. Use given tagging/counting method.
            new TestCase01(true, 100, TaggingAndCountingMethods.valueOf(args[0])).run();
        } else {
            // NON-EXCLUSIVE TEST CASES
            // cant_tag_objects is acquired before test cases are run but still during live phase.
            msgHL("Acquire capability can_tag_objects before first test case.");
            int err = acquireCanTagObjectsCapability();
            Asserts.assertEQ(0, err, "acquireCanTagObjectsCapability FAILED");

            // run test cases
            for (TaggingAndCountingMethods m : TaggingAndCountingMethods.values()) {
                new TestCase01(false, 200, m).run();
            }
            new TestCase02a(200).run();
            new TestCase02b(300).run();
        }
    }

    static class ABBox {
        public int aVal;
        public int bVal;
        public TestCaseBase testCase;

        public ABBox() { /* empty */ }

        public ABBox(TestCaseBase testCase) {
            this.testCase = testCase;
        }

        /**
         * Increment {@link #aVal} and {@link #bVal} under lock. The method is supposed to
         * be inlined into the test method and locking is supposed to be eliminated. After
         * this object escaped to the JVMTI agent, the code with eliminated locking must
         * not be used anymore.
         */
        public synchronized void synchronizedSlowInc() {
            aVal++;
            testCase.waitingForCheck = true;
            dontinline_waitForCheck(testCase);
            testCase.waitingForCheck = false;
            bVal++;
        }

        public static void dontinline_waitForCheck(TestCaseBase testCase) {
            if (testCase.warmUpDone) {
                while(!testCase.checkingNow) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) { /*ign*/ }
                }
            }
        }

        /**
         * This method and incrementing {@link #aVal} and {@link #bVal} are synchronized.
         * So {@link #aVal} and {@link #bVal} should always be equal. Unless the optimized version
         * of {@link #synchronizedSlowInc()} without locking is still used after this object
         * escaped to the JVMTI agent.
         * @return
         */
        public synchronized boolean check() {
            return aVal == bVal;
        }
    }

    public static abstract class TestCaseBase implements Runnable {
        public final long classTag;
        public long instanceTag;

        public final Class<?> taggedClass;

        public long checkSum;
        public long loopCount;
        public volatile boolean doLoop;
        public volatile boolean targetIsInLoop;

        public volatile boolean waitingForCheck;
        public volatile boolean checkingNow;

        public boolean warmUpDone;

        public TestCaseBase(long classTag, Class<?> taggedClass) {
            this.classTag = classTag;
            this.taggedClass = taggedClass;
        }

        public void setUp() {
            // Tag the class of instances to be scalar replaced
            msg("tagging " + taggedClass.getName() + " with tag " +  classTag);
            int err = jvmtiTagClass(taggedClass, classTag);
            Asserts.assertEQ(0, err, "jvmtiTagClass FAILED");
        }

        // to be overridden by test cases
        abstract public void dontinline_testMethod();

        public void warmUp() {
            msg("WarmUp: START");
            int callCount = COMPILE_THRESHOLD + 1000;
            doLoop = true;
            while (callCount-- > 0) {
                dontinline_testMethod();
            }
            warmUpDone = true;
            msg("WarmUp: DONE");
        }

        public Object dontinline_endlessLoop(Object argEscape) {
            long cs = checkSum;
            while (loopCount-- > 0 && doLoop) {
                targetIsInLoop = true;
                checkSum += checkSum % ++cs;
            }
            loopCount = 3;
            targetIsInLoop = false;
            return argEscape;
        }

        public void waitUntilTargetThreadHasEnteredEndlessLoop() {
            while(!targetIsInLoop) {
                msg("Target has not yet entered the loop. Sleep 100ms.");
                try { Thread.sleep(100); } catch (InterruptedException e) { /*ignore */ }
            }
            msg("Target has entered the loop.");
        }

        public void terminateEndlessLoop() throws Exception {
            msg("Terminate endless loop");
            doLoop = false;
        }
    }

    /**
     * Use JVMTI heap functions associated with the elements of {@link TaggingAndCountingMethods} to
     * get a reference to an object allocated in {@link TestCase01#dontinline_testMethod()}. The
     * allocation can be eliminated / scalar replaced.  The test case can be run in two modes: (1)
     * the capability can_tag_objects which is required to use the JVMTI heap functions is taken
     * before the test case (2) the capability is taken after {@link TestCase01#dontinline_testMethod()}
     * is compiled and the target thread has an activation of it on stack.
     */
    public static class TestCase01 extends TestCaseBase {

        public volatile int testMethod_result;
        public boolean acquireCanTagObjectsCapabilityAfterWarmup;
        public TaggingAndCountingMethods taggingMethod;

        public TestCase01(boolean acquireCanTagObjectsCapabilityAfterWarmup, long classTag, TaggingAndCountingMethods taggingMethod) {
            super(classTag, ABBox.class);
            instanceTag = classTag + 1;
            this.acquireCanTagObjectsCapabilityAfterWarmup = acquireCanTagObjectsCapabilityAfterWarmup;
            this.taggingMethod = taggingMethod;
        }

        @Override
        public void setUp() {
            if (!acquireCanTagObjectsCapabilityAfterWarmup) {
                super.setUp();
            }
        }

        public void setUpAfterWarmUp() {
            if (acquireCanTagObjectsCapabilityAfterWarmup) {
                msg("Acquire capability can_tag_objects " + (warmUpDone ? "after" : "before") + " warmup.");
                int err = acquireCanTagObjectsCapability();
                Asserts.assertEQ(0, err, "acquireCanTagObjectsCapability FAILED");
                super.setUp();
            }
        }

        public void run() {
            try {
                msgHL(getClass().getName() + ": test if object that may be scalar replaced is found using " + taggingMethod);
                msg("The capability can_tag_object is acquired " + (acquireCanTagObjectsCapabilityAfterWarmup ? "AFTER" : "BEFORE")
                        + " warmup.");
                setUp();
                warmUp();
                WB.deflateIdleMonitors();
                WB.fullGC(); // get rid of dead instances from previous test cases
                runTest(taggingMethod);
            } catch (Exception e) {
                Asserts.fail("Unexpected Exception", e);
            }
        }

        public void runTest(TaggingAndCountingMethods m) throws Exception {
            loopCount = 1L << 62; // endless loop
            doLoop = true;
            testMethod_result = 0;
            Thread t1 = new Thread(() -> dontinline_testMethod(), "Target Thread (" + getClass().getName() + ")");
            try {
                t1.start();
                try {
                    waitUntilTargetThreadHasEnteredEndlessLoop();
                    setUpAfterWarmUp();
                    msg("count and tag instances of " + taggedClass.getName() + " with tag " + instanceTag + " using JVMTI " + m.name());
                    int count = countAndTagInstancesOfClass(taggedClass, classTag, instanceTag, m);
                    msg("Done. Count is " + count);
                    Asserts.assertGreaterThanOrEqual(count, 0, "countAndTagInstancesOfClass FAILED");
                    Asserts.assertEQ(count, 1, "unexpected number of instances");

                    ABBox[] result = new ABBox[1];
                    msg("get instances tagged with " + instanceTag + ". The instances escape thereby.");
                    int err = getObjectsWithTag(instanceTag, result);
                    msg("Done.");
                    Asserts.assertEQ(0, err, "getObjectsWithTag FAILED");

                    msg("change the now escaped instance' bVal");
                    ABBox abBox = result[0];
                    abBox.bVal = 3;
                    terminateEndlessLoop();

                    msg("wait until target thread has set testMethod_result");
                    while (testMethod_result == 0) {
                        Thread.sleep(50);
                    }
                    msg("check if the modification of bVal is reflected in testMethod_result.");
                    Asserts.assertEQ(7, testMethod_result, " testMethod_result has wrong value");
                    msg("ok.");
                } finally {
                    terminateEndlessLoop();
                }
            } finally {
                t1.join();
            }
        }

        @Override
        public void dontinline_testMethod() {
            ABBox ab = new ABBox();     // can be scalar replaced
            ab.aVal = 4;
            ab.bVal = 2;
            dontinline_endlessLoop(null);   // JVMTI agent acquires reference to ab and changes bVal
            testMethod_result = ab.aVal + ab.bVal;
        }
    }

    /**
     * {@link #dontinline_testMethod()} creates an ArgEscape instance of {@link TestCaseBase#taggedClass} on stack.
     * The jvmti agent tags all instances of this class using one of the {@link TaggingAndCountingMethods}. Then it gets the tagged
     * instances using <code>GetObjectsWithTags()</code>. This is where the ArgEscape globally escapes.
     * It happens at a location without eliminated locking but there is
     * eliminated locking following, so the compiled frame must be deoptimized. This is checked by letting the agent call the
     * synchronized method {@link ABBox#check()} on the escaped instance.
     */
    public static class TestCase02a extends TestCaseBase {

        public long instanceTag;

        public TestCase02a(long classTag) {
            super(classTag, ABBox.class);
            instanceTag = classTag + 1;
        }

        public void run() {
            try {
                msgHL(getClass().getName() + ": test if owning frame is deoptimized if ArgEscape escapes globally");
                setUp();
                warmUp();
                for (TaggingAndCountingMethods m : TaggingAndCountingMethods.values()) {
                    msgHL(getClass().getName() + ": Tag and Get of ArgEscapes using " + m.name());
                    waitingForCheck = false;
                    checkingNow = false;
                    WB.deflateIdleMonitors();
                    WB.fullGC(); // get rid of dead instances from previous test cases
                    runTest(m);
                }
            } catch (Exception e) {
                Asserts.fail("Unexpected Exception", e);
            }
        }

        public void runTest(TaggingAndCountingMethods m) throws Exception {
            loopCount = 1L << 62; // endless loop
            doLoop = true;
            Thread t1 = new Thread(() -> dontinline_testMethod(), "Target Thread (" + getClass().getName() + ")");
            try {
                t1.start();
                try {
                    waitUntilTargetThreadHasEnteredEndlessLoop();
                    msg("count and tag instances of " + taggedClass.getName() + " with tag " + instanceTag + " using JVMTI " + m.name());
                    int count = countAndTagInstancesOfClass(taggedClass, classTag, instanceTag, m);
                    msg("Done. Count is " + count);
                    Asserts.assertGreaterThanOrEqual(count, 0, "countAndTagInstancesOfClass FAILED");
                    Asserts.assertEQ(count, 1, "unexpected number of instances");
                } finally {
                    terminateEndlessLoop();
                }

                ABBox[] result = new ABBox[1];
                msg("get instances tagged with " + instanceTag);
                int err = getObjectsWithTag(instanceTag, result);
                msg("Done.");
                Asserts.assertEQ(0, err, "getObjectsWithTag FAILED");

                ABBox abBoxArgEscape = result[0];
                while (!waitingForCheck) {
                    Thread.yield();
                }
                msg("Check abBoxArgEscape's state is consistent");
                checkingNow = true;
                Asserts.assertTrue(abBoxArgEscape.check(), "Detected inconsistent state. abBoxArgEscape.aVal != abBoxArgEscape.bVal");
                msg("Ok.");
            } finally {
                checkingNow = true;
                t1.join();
            }
        }

        @Override
        public void dontinline_testMethod() {
            ABBox ab = new ABBox(this);
            dontinline_endlessLoop(ab);
            ab.synchronizedSlowInc();
        }
    }

    /**
     * Like {@link TestCase02a}, with the exception that at the location in {@link #dontinline_testMethod()} where the
     * ArgEscape escapes it is not referenced by a local variable.
     */
    public static class TestCase02b extends TestCaseBase {

        public long instanceTag;

        public TestCase02b(long classTag) {
            super(classTag, ABBox.class);
            instanceTag = classTag + 1;
        }

        public void run() {
            try {
                msgHL(getClass().getName() + ": test if owning frame is deoptimized if ArgEscape escapes globally");
                setUp();
                warmUp();
                for (TaggingAndCountingMethods m : TaggingAndCountingMethods.values()) {
                    msgHL(getClass().getName() + ": Tag and Get of ArgEscapes using " + m.name());
                    waitingForCheck = false;
                    checkingNow = false;
                    WB.deflateIdleMonitors();
                    WB.fullGC(); // get rid of dead instances from previous test cases
                    runTest(m);
                }
            } catch (Exception e) {
                Asserts.fail("Unexpected Exception", e);
            }
        }

        public void runTest(TaggingAndCountingMethods m) throws Exception {
            loopCount = 1L << 62; // endless loop
            doLoop = true;
            Thread t1 = new Thread(() -> dontinline_testMethod(), "Target Thread (" + getClass().getName() + ")");
            try {
                t1.start();
                try {
                    waitUntilTargetThreadHasEnteredEndlessLoop();
                    msg("count and tag instances of " + taggedClass.getName() + " with tag " + instanceTag + " using JVMTI " + m.name());
                    int count = countAndTagInstancesOfClass(taggedClass, classTag, instanceTag, m);
                    msg("Done. Count is " + count);
                    Asserts.assertGreaterThanOrEqual(count, 0, "countAndTagInstancesOfClass FAILED");
                    Asserts.assertEQ(count, 1, "unexpected number of instances");
                } finally {
                    terminateEndlessLoop();
                }

                ABBox[] result = new ABBox[1];
                msg("get instances tagged with " + instanceTag);
                int err = getObjectsWithTag(instanceTag, result);
                msg("Done.");
                Asserts.assertEQ(0, err, "getObjectsWithTag FAILED");

                ABBox abBoxArgEscape = result[0];
                while (!waitingForCheck) {
                    Thread.yield();
                }
                msg("Check abBoxArgEscape's state is consistent");
                checkingNow = true;
                Asserts.assertTrue(abBoxArgEscape.check(), "Detected inconsistent state. abBoxArgEscape.aVal != abBoxArgEscape.bVal");
                msg("Ok.");
            } finally {
                checkingNow = true;
                t1.join();
            }
        }

        @Override
        public void dontinline_testMethod() {
            // The new instance is an ArgEscape instance and escapes to the JVMTI agent
            // while the target thread is in the call to dontinline_endlessLoop(). At this
            // location there is no local variable that references the ArgEscape.
            ((ABBox) dontinline_endlessLoop(new ABBox(this))).synchronizedSlowInc();;
        }
    }

    public static void msg(String m) {
        System.out.println();
        System.out.println("### " + m);
        System.out.println();
    }

    public static void msgHL(String m) {
        System.out.println(); System.out.println(); System.out.println();
        System.out.println("#####################################################");
        System.out.println("### " + m);
        System.out.println("###");
        System.out.println();
    }
}
