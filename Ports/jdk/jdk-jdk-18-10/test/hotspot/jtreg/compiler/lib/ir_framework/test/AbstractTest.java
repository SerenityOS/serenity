/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.lib.ir_framework.test;

import compiler.lib.ir_framework.*;
import compiler.lib.ir_framework.shared.TestRun;
import compiler.lib.ir_framework.shared.TestRunException;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Abstract super class for base, checked and custom run tests.
 */
abstract class AbstractTest {
    protected static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    protected static final int TEST_COMPILATION_TIMEOUT = Integer.parseInt(System.getProperty("TestCompilationTimeout", "10000"));
    protected static final int WAIT_FOR_COMPILATION_TIMEOUT = Integer.parseInt(System.getProperty("WaitForCompilationTimeout", "10000"));
    protected static final boolean VERIFY_OOPS = (Boolean)WHITE_BOX.getVMFlag("VerifyOops");

    protected final int warmupIterations;
    protected final boolean skip;

    AbstractTest(int warmupIterations, boolean skip) {
        this.warmupIterations = warmupIterations;
        this.skip = skip;
    }

    protected boolean shouldCompile(DeclaredTest test) {
        return test.getCompLevel() != CompLevel.SKIP;
    }

    abstract String getName();

    /**
     * Should test be executed?
     */
    public boolean isSkipped() {
        return skip;
    }

    /**
     * See {@link CompLevel#WAIT_FOR_COMPILATION}.
     */
    protected static boolean isWaitForCompilation(DeclaredTest test) {
        return test.getCompLevel() == CompLevel.WAIT_FOR_COMPILATION;
    }

    protected static Object createInvocationTarget(Method method) {
        Class<?> clazz = method.getDeclaringClass();
        Object invocationTarget;
        if (Modifier.isStatic(method.getModifiers())) {
            invocationTarget = null;
        } else {
            try {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                invocationTarget = constructor.newInstance();
            } catch (Exception e) {
                throw new TestRunException("Could not create instance of " + clazz
                                           + ". Make sure there is a constructor without arguments.", e);
            }
        }
        return invocationTarget;
    }

    /**
     * Run the associated test.
     */
    public void run() {
        if (skip) {
            return;
        }
        onStart();
        for (int i = 0; i < warmupIterations; i++) {
            invokeTest();
        }
        onWarmupFinished();
        compileTest();
        // Always run the test as a last step of the test execution.
        invokeTest();
    }

    protected void onStart() {
        // Do nothing by default.
    }

    abstract protected void invokeTest();

    abstract protected void onWarmupFinished();

    abstract protected void compileTest();

    protected void compileMethod(DeclaredTest test) {
        final Method testMethod = test.getTestMethod();
        TestRun.check(WHITE_BOX.isMethodCompilable(testMethod, test.getCompLevel().getValue(), false),
                      "Method " + testMethod + " not compilable at level " + test.getCompLevel()
                      + ". Did you use compileonly without including all @Test methods?");
        TestRun.check(WHITE_BOX.isMethodCompilable(testMethod),
                      "Method " + testMethod + " not compilable at level " + test.getCompLevel()
                      + ". Did you use compileonly without including all @Test methods?");
        if (TestFramework.VERBOSE) {
            System.out.println("Compile method " + testMethod + " after warm-up...");
        }

        final boolean maybeCodeBufferOverflow = (TestVM.TEST_C1 && VERIFY_OOPS);
        final long started = System.currentTimeMillis();
        long elapsed = 0;
        int lastCompilationLevel = -10;
        enqueueMethodForCompilation(test);

        do {
            if (!WHITE_BOX.isMethodQueuedForCompilation(testMethod)) {
                if (elapsed > 0) {
                    if (TestVM.VERBOSE) {
                        System.out.println(testMethod + " is not in queue anymore due to compiling it simultaneously on " +
                                           "a different level. Enqueue again.");
                    }
                    enqueueMethodForCompilation(test);
                }
            }
            if (maybeCodeBufferOverflow && elapsed > 1000 && !WHITE_BOX.isMethodCompiled(testMethod, false)) {
                // Let's disable VerifyOops temporarily and retry.
                WHITE_BOX.setBooleanVMFlag("VerifyOops", false);
                WHITE_BOX.clearMethodState(testMethod);
                enqueueMethodForCompilation(test);
                WHITE_BOX.setBooleanVMFlag("VerifyOops", true);
            }

            lastCompilationLevel = WHITE_BOX.getMethodCompilationLevel(testMethod, false);
            if (lastCompilationLevel == test.getCompLevel().getValue()) {
                break;
            }
            elapsed = System.currentTimeMillis() - started;
        } while (elapsed < TEST_COMPILATION_TIMEOUT);
        TestRun.check(elapsed < TEST_COMPILATION_TIMEOUT,
                      "Could not compile " + testMethod + " at level " + test.getCompLevel() + " after "
                      + TEST_COMPILATION_TIMEOUT/1000 + "s. Last compilation level: " + lastCompilationLevel);
        checkCompilationLevel(test);
    }

    private void enqueueMethodForCompilation(DeclaredTest test) {
        TestVM.enqueueForCompilation(test.getTestMethod(), test.getCompLevel());
    }

    protected void checkCompilationLevel(DeclaredTest test) {
        CompLevel level = CompLevel.forValue(WHITE_BOX.getMethodCompilationLevel(test.getTestMethod()));
        TestRun.check(level == test.getCompLevel(),  "Compilation level should be " + test.getCompLevel().name()
                                                     + " (requested) but was " + level.name() + " for " + test.getTestMethod());
    }

    final protected void waitForCompilation(DeclaredTest test) {
        final Method testMethod = test.getTestMethod();
        final boolean maybeCodeBufferOverflow = (TestVM.TEST_C1 && VERIFY_OOPS);
        final long started = System.currentTimeMillis();
        boolean stateCleared = false;
        long elapsed;
        do {
            elapsed = System.currentTimeMillis() - started;
            int level = WHITE_BOX.getMethodCompilationLevel(testMethod);
            if (maybeCodeBufferOverflow && elapsed > 5000
                && (!WHITE_BOX.isMethodCompiled(testMethod, false) || level != test.getCompLevel().getValue())) {
                retryDisabledVerifyOops(testMethod, stateCleared);
                stateCleared = true;
            } else {
                invokeTest();
            }

            boolean isCompiled = WHITE_BOX.isMethodCompiled(testMethod, false);
            if (TestVM.VERBOSE) {
                System.out.println("Is " + testMethod + " compiled? " + isCompiled);
            }
            if (isCompiled || TestVM.XCOMP || TestVM.EXCLUDE_RANDOM) {
                // Don't wait for compilation if -Xcomp is enabled or if we are randomly excluding methods from compilation.
                return;
            }
        } while (elapsed < WAIT_FOR_COMPILATION_TIMEOUT);
        throw new TestRunException(testMethod + " not compiled after waiting for "
                                   + WAIT_FOR_COMPILATION_TIMEOUT/1000 + " s");
    }

    /**
     * If it takes too long, try to disable Verify Oops.
     */
    private void retryDisabledVerifyOops(Method testMethod, boolean stateCleared) {
        System.out.println("Temporarily disabling VerifyOops");
        try {
            WHITE_BOX.setBooleanVMFlag("VerifyOops", false);
            if (!stateCleared) {
                WHITE_BOX.clearMethodState(testMethod);
            }
            invokeTest();
        } finally {
            WHITE_BOX.setBooleanVMFlag("VerifyOops", true);
            System.out.println("Re-enabled VerifyOops");
        }
    }
}
