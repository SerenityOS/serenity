/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Pattern;
import nsk.share.TestFailure;
import nsk.share.log.Log;
import nsk.share.test.TestBase;
import vm.runtime.defmeth.AccessibilityFlagsTest;
import vm.runtime.defmeth.BasicTest;
import vm.runtime.defmeth.ConflictingDefaultsTest;
import vm.runtime.defmeth.DefaultVsAbstractTest;
import vm.runtime.defmeth.MethodResolutionTest;
import vm.runtime.defmeth.ObjectMethodOverridesTest;
import vm.runtime.defmeth.PrivateMethodsTest;
import vm.runtime.defmeth.StaticMethodsTest;
import vm.runtime.defmeth.SuperCallTest;
import vm.runtime.defmeth.shared.annotation.NotApplicableFor;
import vm.runtime.defmeth.shared.builder.TestBuilder;
import vm.runtime.defmeth.shared.builder.TestBuilderFactory;
import vm.share.options.Option;
import vm.share.options.OptionSupport;
import vm.share.options.Options;
import static java.lang.String.format;
import static jdk.internal.org.objectweb.asm.Opcodes.V17;
import static jdk.internal.org.objectweb.asm.Opcodes.V1_5;

import vm.runtime.defmeth.RedefineTest;

/**
 * Parent class for all default method tests.
 *
 * Contains common settings and code to run individual tests.
 *
 * Provides command-line interface to run arbitrary subset of
 * tests on default methods with some customizations.
 */
public abstract class DefMethTest extends TestBase {
    /** Classes that contain tests on default methods */
    static private final List<Class<? extends DefMethTest>> classes;

    // the number of tests has failed
    // note that if more than one sub-test has failed within a test,
    // it will be counted as 1 failure for that test
    private int numFailures;

    static {
        List<Class<? extends DefMethTest>> intlList = new ArrayList<>();

        intlList.add(AccessibilityFlagsTest.class);
        intlList.add(BasicTest.class);
        intlList.add(ConflictingDefaultsTest.class);
        intlList.add(DefaultVsAbstractTest.class);
        intlList.add(MethodResolutionTest.class);
        intlList.add(ObjectMethodOverridesTest.class);
        intlList.add(SuperCallTest.class);
        intlList.add(PrivateMethodsTest.class);
        intlList.add(StaticMethodsTest.class);
        intlList.add(RedefineTest.class);

        classes = Collections.unmodifiableList(intlList);
    }

    public static List<Class<? extends DefMethTest>> getTests() {
        return classes;
    }

    @Option(name="list", default_value="false", description="list tests w/o executing them")
    boolean listTests;

    @Option(name="filter", default_value="", description="filter executed tests")
    String filterString;

    @Option(name="silent", default_value="false", description="silent mode - don't print anything")
    boolean isSilent;

    @Option(name="failfast", default_value="false", description="fail the whole set of test on first failure")
    boolean failFast;

    @Option(name="testAllModes", default_value="false", description="run each test in all possible modes")
    boolean testAllModes;

    @Option(name="mode", description="invocation mode (direct, reflect, invoke)", default_value="direct")
    String mode;

    private Pattern filter; // Precompiled pattern for filterString

    public static final int MIN_MAJOR_VER = V1_5;
    public static final int MAX_MAJOR_VER = V17;

    /**
     * Used from individual tests to get TestBuilder instances,
     * which is aware of current testing configuration
     */
    @Options
    protected TestBuilderFactory factory = new TestBuilderFactory(this);

    private void init() {
        if (isSilent) {
            getLog().setInfoEnabled(false);
            getLog().setWarnEnabled(false);
            getLog().setDebugEnabled(false);
        }

        if (filterString != null && !"".equals(filterString)) {
            filter = Pattern.compile(".*" + filterString + ".*");
        } else {
            filter = Pattern.compile(".*"); // match everything
        }

        // Test-specific config
        configure();
    }

    @Override
    public final void run() {
        init();

        boolean success = runTest();
        if (!success) {
            getLog().info("TEST FAILED");
            setFailed(true);
        } else {
            getLog().info("TEST PASSED");
        }
    }

    protected void configure() {
        // Is overriden by specific tests to do test-specific setup
    }

    public Log getLog() {
        return log;
    }

    @Override
    public String toString() {
        return format("%s%s",
                getClass().getSimpleName(), factory);
    }

    /** Enumerate invocation modes to be tested */
    private ExecutionMode[] getInvocationModes() {
        if (factory.getExecutionMode() != null) {
            return new ExecutionMode[] { ExecutionMode.valueOf(factory.getExecutionMode()) };
        }

        if (testAllModes) {
            return ExecutionMode.values();
        }

        switch(mode) {
            case "direct":  return new ExecutionMode[] { ExecutionMode.DIRECT };
            case "reflect": return new ExecutionMode[] { ExecutionMode.REFLECTION };
            case "invoke_exact":   return new ExecutionMode[] { ExecutionMode.INVOKE_EXACT };
            case "invoke_generic": return new ExecutionMode[] { ExecutionMode.INVOKE_GENERIC };
            case "indy":    return new ExecutionMode[] { ExecutionMode.INDY };
            case "invoke":  return new ExecutionMode[] { ExecutionMode.INVOKE_WITH_ARGS,
                                                         ExecutionMode.INVOKE_EXACT,
                                                         ExecutionMode.INVOKE_GENERIC,
                                                         ExecutionMode.INDY };
            case "redefinition":
                throw new Error("redefinition is only a pseudo-mode");
            default:
                throw new Error("Unknown mode: " + mode);
        }
    }

    // Check whether the test is applicable to selected execution mode
    private boolean shouldExecute(Method m, ExecutionMode mode) {
        Class<? extends DefMethTest> test = this.getClass();

        int acc = m.getModifiers();
        if (!Modifier.isPublic(acc) || Modifier.isStatic(acc) ||
            m.getParameterTypes().length != 0 && !requiresTestBuilder(m)) {
            return false; // not a test
        }

        String testName = format("%s.%s", test.getName(), m.getName());
        if (!filter.matcher(testName).matches()) {
            return false; // test is filtered out
        }

        if (m.isAnnotationPresent(NotApplicableFor.class)) {
            for (ExecutionMode excludeFromMode : m.getAnnotation(NotApplicableFor.class).modes()) {
                if (mode == excludeFromMode) {
                    return false; // not applicable to current execution mode
                } else if (excludeFromMode == ExecutionMode.REDEFINITION &&
                          (factory.isRedefineClasses() || factory.isRetransformClasses())) {
                    return false; // Can't redefine some tests.
                }

            }
        }

        return true;
    }

    private boolean requiresTestBuilder(Method m) {
        Parameter[] params = m.getParameters();
        return params.length == 1 && (params[0].getType() == TestBuilder.class);
    }

    /** Information about the test being executed */
    public String shortTestName;

    public static class ComparableMethod implements Comparable<ComparableMethod> {
        final java.lang.reflect.Method m;
        ComparableMethod(java.lang.reflect.Method m) { this.m = m; }
        public int compareTo(ComparableMethod mo) {
           String name = m.getName();
           String mo_name = mo.m.getName();
           return name.compareTo(mo_name);
        }
    }

    /** helper method for subclass to report the number of test failures.
     *  It is more important for the reflection case as an exception thrown
     *  deep in the call stack may not be propagated to this level.
     *
     * @param failures
     */
    public void addFailureCount(int failures) {
        numFailures += failures;
    }

    /**
     * Execute all tests from current class and report status.
     *
     * The following execution customization is supported:
     *   - filter tests by name using regex
     *
     * @return any failures occurred?
     */
    public final boolean runTest() {
        ExecutionMode[] invocationModes = getInvocationModes();

        try {
            int totalTests = 0;

            Class<? extends DefMethTest> test = this.getClass();

            getLog().info(format("\n%s %s", test.getSimpleName(), factory.toString()));

            TreeSet<ComparableMethod> ts = new TreeSet<ComparableMethod>();
            for (java.lang.reflect.Method m : test.getDeclaredMethods()) {
                ts.add(new ComparableMethod(m));
            }

            for (ComparableMethod cm : ts) {
                java.lang.reflect.Method m = cm.m;
                for (ExecutionMode mode : invocationModes) {
                    shortTestName = format("%s.%s", test.getSimpleName(), m.getName());

                    if (!shouldExecute(m, mode)) {
                        continue; // skip the test due to current configuration
                    }

                    totalTests++;

                    getLog().info(shortTestName);

                    if (listTests) {
                        continue; // just print test info
                    }

                    // Iterate over all test modes
                    try {
                        factory.setExecutionMode(mode.name());
                        getLog().info(format("    %s: ", mode));
                        if (requiresTestBuilder(m)) {
                            TestBuilder b = factory.getBuilder();
                            m.invoke(this, b);
                            b.run();
                        } else {
                            m.invoke(this);
                        }
                    } catch (IllegalAccessException | IllegalArgumentException e) {
                        throw new TestFailure(e);
                    } catch (InvocationTargetException e) {
                        if (e.getCause() instanceof TestFailure) {
                            // Failure details were printed in GeneratedTest.run()/ReflectionTest.run()
                        } else {
                            if (Constants.PRINT_STACK_TRACE) {
                                e.printStackTrace();
                            }
                        }
                        addFailureCount(1);
                        if (failFast) {
                            throw new TestFailure(e.getCause());
                        }
                    }
                }
            }

            int passedTests = totalTests - numFailures;
            getLog().info(format("%d test run: %d passed, %d failed", totalTests, passedTests, numFailures));
            if (numFailures == 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception | Error e) {
            throw new RuntimeException(e);
        }
    }

    public static void runTest(Class<? extends DefMethTest> testClass,
                               Set<Integer> majorVerValues,
                               Set<Integer> flagsValues,
                               Set<Boolean> redefineValues,
                               Set<ExecutionMode> execModes) {
        for (int majorVer : majorVerValues) {
            for (int flags : flagsValues) {
                for (boolean redefine : redefineValues) {
                    for (ExecutionMode mode : execModes) {
                        try {
                            DefMethTest test = testClass.getDeclaredConstructor().newInstance();

                            OptionSupport.setup(test, new String[]{
                                        "-execMode", mode.toString(),
                                        "-ver", Integer.toString(majorVer),
                                        "-flags", Integer.toString(flags),
                                        "-redefine", Boolean.toString(redefine)
                                });

                            test.run();
                        } catch (ReflectiveOperationException e) {
                            throw new TestFailure(e);
                        }
                    }
                }
            }
        }
    }

    /** Command-line interface to initiate test run */
    public static void main(String[] args) {
        for (Class<? extends DefMethTest> clz : classes) {
            try {
                DefMethTest test = clz.newInstance();
                OptionSupport.setupAndRun(test, args);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new TestFailure(e);
            }
        }
    }
}
