/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.TimeLimitedRunner;
import jdk.test.lib.Utils;
import test.java.lang.invoke.lib.CodeCacheOverflowProcessor;
import test.java.lang.invoke.lib.Helper;

import java.lang.invoke.MethodHandle;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Lambda forms caching test case class. Contains all necessary test routines to
 * test lambda forms caching in method handles returned by methods of
 * MethodHandles class.
 *
 * @author kshefov
 */
public abstract class LambdaFormTestCase {

    private static final long TIMEOUT = Helper.IS_THOROUGH ?
            0L : (long) (Utils.adjustTimeout(Utils.DEFAULT_TEST_TIMEOUT) * 0.9);

    /**
     * Reflection link to {@code j.l.i.MethodHandle.internalForm} method. It is
     * used to get a lambda form from a method handle.
     */
    protected static final Method INTERNAL_FORM;
    protected static final Field KIND_FIELD;
    protected static final Field REF_FIELD;
    private static final List<GarbageCollectorMXBean> gcInfo;

    private static long gcCount() {
        return gcInfo.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
    }

    static {
        try {
            INTERNAL_FORM = MethodHandle.class.getDeclaredMethod("internalForm");
            INTERNAL_FORM.setAccessible(true);

            KIND_FIELD = Class.forName("java.lang.invoke.LambdaForm").getDeclaredField("kind");
            KIND_FIELD.setAccessible(true);

            REF_FIELD = Reference.class.getDeclaredField("referent");
            REF_FIELD.setAccessible(true);
        } catch (Exception ex) {
            throw new Error("Unexpected exception", ex);
        }

        gcInfo = ManagementFactory.getGarbageCollectorMXBeans();
        if (gcInfo.size() == 0) {
            throw new Error("No GarbageCollectorMXBeans found.");
        }
    }

    private final TestMethods testMethod;
    private long gcCountAtStart;

    private static class TestRun {

        final Function<TestMethods, LambdaFormTestCase> ctor;
        final Collection<TestMethods> testMethods;
        final long totalIterations;
        long doneIterations;
        long testCounter;
        long failCounter;
        boolean passed;

        TestRun(Function<TestMethods, LambdaFormTestCase> ctor,
                Collection<TestMethods> testMethods) {
            this.ctor = ctor;
            this.testMethods = testMethods;
            long testCaseNum = testMethods.size();
            long iterations = Math.max(1, Helper.TEST_LIMIT / testCaseNum);
            System.out.printf("Number of iterations according to -DtestLimit is %d (%d cases)%n",
                    iterations, iterations * testCaseNum);
            System.out.printf("Number of iterations is set to %d (%d cases)%n",
                    iterations, iterations * testCaseNum);
            System.out.flush();
            totalIterations = iterations;
            doneIterations = 0L;
            testCounter = 0L;
            failCounter = 0L;
            passed = true;
        }

        Boolean doIteration() {
            if (doneIterations >= totalIterations) {
                return false;
            }
            System.err.println(String.format("Iteration %d:", doneIterations));
            for (TestMethods testMethod : testMethods) {
                LambdaFormTestCase testCase = ctor.apply(testMethod);
                try {
                    System.err.printf("Tested LF caching feature"
                            + " with MethodHandles.%s method.%n",
                            testCase.getTestMethod().name);
                    Throwable t = CodeCacheOverflowProcessor
                            .runMHTest(testCase::doTest);
                    if (t != null) {
                        return false;
                    }
                    System.err.println("PASSED");
                } catch (OutOfMemoryError oome) {
                    // Don't swallow OOME so a heap dump can be created.
                    System.err.println("FAILED");
                    throw oome;
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.err.printf("FAILED. Caused by %s%n", t.getMessage());
                    passed = false;
                    failCounter++;
                }
                testCounter++;
            }
            doneIterations++;
            return true;
        }
    }

    /**
     * Test case constructor. Generates test cases with random method types for
     * given methods form {@code j.l.i.MethodHandles} class.
     *
     * @param testMethod A method from {@code j.l.i.MethodHandles} class which
     * returns a {@code j.l.i.MethodHandle}.
     */
    protected LambdaFormTestCase(TestMethods testMethod) {
        this.testMethod = testMethod;
        this.gcCountAtStart = gcCount();
    }

    public TestMethods getTestMethod() {
        return testMethod;
    }

    protected boolean noGCHappened() {
        return gcCount() == gcCountAtStart;
    }

    /**
     * Routine that executes a test case.
     */
    public abstract void doTest();

    /**
     * Runs a number of test cases defined by the size of testCases list.
     *
     * @param ctor constructor of LambdaFormCachingTest or its child classes
     * object.
     * @param testMethods list of test methods
     */
    public static void runTests(Function<TestMethods, LambdaFormTestCase> ctor,
                                Collection<TestMethods> testMethods) {
        LambdaFormTestCase.TestRun run
                = new LambdaFormTestCase.TestRun(ctor, testMethods);
        TimeLimitedRunner runner
                = new TimeLimitedRunner(TIMEOUT, 4.0d, run::doIteration);
        try {
            runner.call();
        } catch (Exception ex) {
            System.err.println("FAILED");
            throw new Error("Unexpected error!", ex);
        }
        if (!run.passed) {
            throw new Error(String.format("%d of %d test cases FAILED! %n"
                    + "Rerun the test with the same \"-Dseed=\" option as in the log file!",
                    run.failCounter, run.testCounter));
        } else {
            System.err.printf("All %d test cases PASSED!%n", run.testCounter);
        }
    }
}
