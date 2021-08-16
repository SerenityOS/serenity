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
import compiler.lib.ir_framework.shared.TestFormat;
import compiler.lib.ir_framework.shared.TestFrameworkException;
import compiler.lib.ir_framework.shared.TestRunException;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A custom run test allows the user to have full control over how the @Test method is invoked by specifying
 * a dedicated @Run method. See {@link Run} for more details and its precise definition.
 */
class CustomRunTest extends AbstractTest {
    private final Method runMethod;
    private final RunMode mode;
    private final Object runInvocationTarget;
    private final List<DeclaredTest> tests;
    private final RunInfo runInfo;

    public CustomRunTest(Method runMethod, Warmup warmUpAnno, Run runSpecification, List<DeclaredTest> tests, boolean skip) {
        // Make sure we can also call non-public or public methods in package private classes
        super(warmUpAnno != null ? warmUpAnno.value() : TestVM.WARMUP_ITERATIONS, skip);
        TestFormat.checkNoThrow(warmupIterations >= 0, "Cannot have negative value for @Warmup at " + runMethod);
        runMethod.setAccessible(true);
        this.runMethod = runMethod;
        this.runInvocationTarget = createInvocationTarget(runMethod);
        this.mode = runSpecification.mode();
        this.tests = tests;
        this.runInfo = new RunInfo(tests);
    }

    @Override
    public String toString() {
        String s = "Custom Run Test: @Run: " + runMethod.getName() + " - @Test";
        if (tests.size() == 1) {
            s += ": " + tests.get(0).getTestMethod().getName();
        } else {
            s += "s: {" + tests.stream().map(t -> t.getTestMethod().getName())
                               .collect(Collectors.joining(",")) + "}";
        }
        return s;
    }

    @Override
    String getName() {
        return runMethod.getName();
    }

    @Override
    public void run() {
        if (skip) {
            return;
        }
        switch (mode) {
            case STANDALONE -> {
                runInfo.setWarmUpFinished();
                invokeTest();
            }// Invoke once but do not apply anything else.
            case NORMAL -> super.run();
        }
    }

    @Override
    public void onWarmupFinished() {
        runInfo.setWarmUpFinished();
    }

    @Override
    protected void compileTest() {
        if (tests.size() == 1) {
            compileSingleTest();
        } else {
            compileMultipleTests();
        }
    }

    private void compileSingleTest() {
        DeclaredTest test = tests.get(0);
        if (shouldCompile(test)) {
            if (isWaitForCompilation(test)) {
                waitForCompilation(test);
            } else {
                compileMethod(test);
            }
        }
    }

    private void compileMultipleTests() {
        boolean anyWaitForCompilation = false;
        boolean anyCompileMethod = false;
        ExecutorService executor = Executors.newFixedThreadPool(tests.size());
        for (DeclaredTest test : tests) {
            if (shouldCompile(test)) {
                if (isWaitForCompilation(test)) {
                    anyWaitForCompilation = true;
                    executor.execute(() -> waitForCompilation(test));
                } else {
                    anyCompileMethod = true;
                    executor.execute(() -> compileMethod(test));
                }
            }
        }
        executor.shutdown();
        int timeout;
        if (anyCompileMethod && anyWaitForCompilation) {
            timeout = Math.max(WAIT_FOR_COMPILATION_TIMEOUT, TEST_COMPILATION_TIMEOUT) + 5000;
        } else if (anyWaitForCompilation) {
            timeout = WAIT_FOR_COMPILATION_TIMEOUT + 5000;
        } else {
            timeout = TEST_COMPILATION_TIMEOUT + 5000;
        }
        try {
            executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new TestRunException("Some compilations did not complete after " + timeout
                                       + "ms for @Run method " + runMethod);
        }
    }

    /**
     * Do not directly run the test but rather the run method that is responsible for invoking the actual test.
     */
    @Override
    protected void invokeTest() {
        try {
            if (runMethod.getParameterCount() == 1) {
                runMethod.invoke(runInvocationTarget, runInfo);
            } else {
                runMethod.invoke(runInvocationTarget);
            }
        } catch (Exception e) {
            throw new TestRunException("There was an error while invoking @Run method " + runMethod, e);
        }
    }

    @Override
    protected void checkCompilationLevel(DeclaredTest test) {
        CompLevel level = CompLevel.forValue(WhiteBox.getWhiteBox().getMethodCompilationLevel(test.getTestMethod()));
        if (level != test.getCompLevel()) {
            String message = "Compilation level should be " + test.getCompLevel().name() + " (requested) but was "
                             + level.name() + " for " + test.getTestMethod() + ".";
            switch (mode) {
                case STANDALONE -> throw new TestFrameworkException("Should not be called for STANDALONE method " + runMethod);
                case NORMAL -> message = message + System.lineSeparator() + "Check your @Run method " + runMethod
                                         + " to ensure that " + test.getTestMethod()
                                         + " is called at least once in each iteration.";
            }
            throw new TestRunException(message);
        }
    }
}
