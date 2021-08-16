/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.share;

import java.lang.reflect.Constructor;
import java.util.List;

import nsk.share.Consts;
import nsk.share.ArgumentParser;
import vm.share.ProcessUtils;
import vm.share.options.IgnoreUnknownArgumentsHandler;
import vm.share.options.OptionSupport;

/**
 * This class executes a test based on (a subclass of) either:
 * <ul>
 *   <li>{@link vm.mlvm.share.MlvmTest}
 *   <li>{@link java.lang.Runnable}
 * </ul>
 * using a number of launch() methods.
 *
 * Command-line parameters are parsed and set to the instance fields of the test marked with {@literal@}Option/Options annotations. See {@link vm.share.options} framework for details.
 *
 * Arguments for test constructor can be passed as well.
 *
 * Additionally the launch() methods:
 * <ul>
 *   <li>measures test run time
 *   <li> handles all test status passing methods (via boolean return value, via MlvmTest.markTestFailed() calls, exception thrown from overriden run() method)
 *   <li>optionally dumps heap after test execution if MlvmTest.setHeapDumpAfer(true) was called
 * </ul>
 *
 * @see vm.mlvm.share.MlvmTest
 * @see vm.share.options
 *
 */
public class MlvmTestExecutor {

    /**
     * The heap dump file name. If you call MlvmTest.setHeapDumpAfter(true), the heap is dumped into file
     * specified by this constant when test finishes
     */
    public static final String HEAP_DUMP_FILENAME = "heap.dump";

    /**
     * Launches MLVM test.
     * Please see documentation for {@link #launch(Class<?> testClass, Object[] constructorArgs)} method.
     *
     * This version of the method is a syntactic shugar to launch test in this way:
     *
     * <code>
     * public class MyTest extends MlvmTest {
     *     ...
     *     public static void main(String[] args) {
     *         MlvmTestExecutor.launch(new YourCustomArgumentParser(args));
     *     }
     * }
     * </code>
     *
     * @param args Command-line arguments, which are taken from supplied ArgumentParser (ArgumentParser is needed for compatibility with old tests)
                   and set to appropriate test instance fields using vm.share.options framework
     * @see #launch(Class<?> testClass, Object[] constructorArgs)
     */
    public static void launch(ArgumentParser argumentParser) {
        launch(argumentParser, null);
    }

    /**
     * Launches MLVM test.
     * Please see documentation for {@link #launch(Class<?> testClass, Object[] constructorArgs)} method.
     *
     * This version of the method is a syntactic shugar to launch test in this way:
     *
     * <code>
     * public class MyTest extends MlvmTest {
     *     ...
     *     public static void main(String[] args) {
     *         MlvmTestExecutor.launch(new YourCustomArgumentParser(args), new Object[] { constructor-arguments... });
     *     }
     * }
     * </code>
     *
     * @param args Command-line arguments, which are taken from supplied ArgumentParser (ArgumentParser is needed for compatibility with old tests)
                   and set to appropriate test instance fields using vm.share.options framework
     * @param constructorArgs Arguments, which are parsed to test constructor
     * @see #launch(Class<?> testClass, Object[] constructorArgs)
     */
    public static void launch(ArgumentParser argumentParser, Object[] constructorArgs) {
        Env.init(argumentParser);
        launch(constructorArgs);
    }

    /**
     * Launches MLVM test.
     * Please see documentation for {@link #launch(Class<?> testClass, Object[] constructorArgs)} method.
     *
     * This version of the method is a syntactic shugar to launch test in this way:
     *
     * <code>
     * public class MyTest extends MlvmTest {
     *     ...
     *     public static void main(String[] args) {
     *         MlvmTestExecutor.launch(args);
     *     }
     * }
     * </code>
     *
     * @param args Command-line arguments, which are parsed using internal ArgumentParser (for compatibility with old tests) and also set to appropriate test instance fields using vm.share.options framework
     * @see #launch(Class<?> testClass, Object[] constructorArgs)
     */
    public static void launch(String[] args) {
        launch(args, null);
    }

    /**
     * Launches MLVM test.
     * Please see documentation for {@link #launch(Class<?> testClass, Object[] constructorArgs)} method.
     *
     * This version of the method is a syntactic shugar to launch test in this way:
     *
     * <code>
     * public class MyTest extends MlvmTest {
     *     ...
     *     public static void main(String[] args) {
     *         MlvmTestExecutor.launch(args, new Object[] { constructor-arguments... });
     *     }
     * }
     * </code>
     *
     * @param args Command-line arguments, which are parsed using internal ArgumentParser (for compatibility with old tests) and also set to appropriate test instance fields using vm.share.options framework
     * @param constructorArgs Arguments, which are parsed to test constructor
     * @see #launch(Class<?> testClass, Object[] constructorArgs)
     */
    public static void launch(String[] args, Object[] constructorArgs) {
        Env.init(args);
        launch(constructorArgs);
    }

    /**
     * Launches MLVM test.
     * Please see documentation for {@link #launch(Class<?> testClass, Object[] constructorArgs)} method.
     *
     * This version of the method is a syntactic shugar to launch test in this way:
     *
     * <code>
     * public class MyTest extends MlvmTest {
     *     ...
     *     void aMethod() {
     *         ...
     *         MlvmTestExecutor.launch(new Object[] { constructor-arguments... });
     *     }
     * }
     * </code>
     *
     * @param constructorArgs Arguments, which are parsed to test constructor
     * @see #launch(Class<?> testClass, Object[] constructorArgs)
     */
    public static void launch(Object[] constructorArgs) {
        Class<?> testClass = getTestClassFromCallerStack();

        if (testClass == null) {
            throw new RuntimeException("TEST BUG: Can't find an instance of MlvmTest or Runnable in the stacktrace");
        }

        launch(testClass, constructorArgs);
    }

    private static Class<?> getTestClassFromCallerStack() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

            // Elements of the stack trace: 0=Thread.getStackTrace(), 1rd=this method, 2nd=launch() method
            // So we start searching from the most outer frame and finish searching at element 3
            for (int i = stackTrace.length - 1; i >= 3; --i) {
                StackTraceElement e = stackTrace[i];
                Class<?> klass = Class.forName(e.getClassName());
                if (MlvmTest.class.isAssignableFrom(klass)) {
                    return klass;
                }
            }

            return null;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to get Class object by its name either due to a different ClassLoader (a test bug) or JVM error", e);
        }
    }

    /**
     * Launches MLVM test. The method is the principal MLVM test launcher. This method in conjunction with {@link #runMlvmTest} method:
     * <ol>
     *   <li>instantiates test class (optionally passing arguments to constructor)
     *   <li>parses command-line arguments using vm.share.options framework and updates the appropriate test fields
     *   <li>launches the tests
     *   <li>handles all test status passing methods (see below)
     *   <li>performs System.exit() with VM-testbase standard exit code 95 (success) or 97 (failure).
     * </ol>
     *
     * <p>The following tests status passing mechanism are available to test writer:
     * <ol>
     *   <li>Return a boolean value (true if test passed, false otherwise. Good for simple tests)
     *   <li>Assume that test has failed by calling {@link MlvmTest#markTestFailed()} method (suitable for complex logic and multithreaded tests)
     *   <li>by throwing exception from test {@link MlvmTest#run()} method
     * </ol>

     * <p>Additionally the launcher:
     * <ul>
     *   <li>measures test run time and logs it
     *   <li>optionally dumps heap after test execution if {@link MlvmTest#setHeapDumpAfer(true)} was called
     *   <li>enables verbose logging on error
     * </ul>
     *
     * @param testClass a class to instantiate. Has to be subclass of vm.mlvm.share.MlvmTest or java.lang.Runnable
     * @param constructorArgs Arguments to pass to test constructor. Can be null.
     * @see #runMlvmTest(Class<?> testClass, Object[] constructorArgs)
     */
    public static void launch(Class<?> testClass, Object[] constructorArgs) {
        Env.getLog().enableVerboseOnError(true);

        long startTime = System.currentTimeMillis();
        boolean passed;
        try {
            Env.traceDebug(MlvmTest.getName() + " class: " + testClass);
            passed = runMlvmTest(testClass, constructorArgs);
        } catch (Throwable e) {
            Env.complain(e, MlvmTest.getName() + " caught an exception: ");
            passed = false;
        }

        long finishTime = System.currentTimeMillis();
        Env.traceNormal("The test took " + (finishTime - startTime) + " ms");

        optionallyDumpHeap();

        final String testNameUC = MlvmTest.getName().toUpperCase();
        if (passed) {
            Env.display(testNameUC + " PASSED");
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_PASSED);
        } else {
            Env.display(testNameUC + " FAILED");
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }
    }

    private static void optionallyDumpHeap() {
        try {
            if (MlvmTest.getHeapDumpAfter()) {
                ProcessUtils.dumpHeapWithHotspotDiagnosticMXBean(HEAP_DUMP_FILENAME);
            }
        } catch (Exception e) {
            Env.traceNormal(e, "Error dumping heap: ");
        }
    }

    /**
     * Launches MLVM test (syntatic shugar method).
     * Calls {@link #runMlvmTest(Class<?> testClass, Object[] constructorArgs)} method providing no arguments to pass to the test constructor.
     *
     * The method throws unchecked exception when there is an error in test logic or handling.
     * Exceptions thrown from MlvmTest.run() method or test constructor are wrapped into java.lang.RuntimeException or java.lang.Error
     *
     * @param testClass a class to instantiate. Has to be subclass of vm.mlvm.share.MlvmTest or java.lang.Runnable
     * @return true if test passed, false otherwise
     * @see #runMlvmTest(Class<?> testClass, Object[] constructorArgs)
     */
    public static boolean runMlvmTest(Class<?> testClass) {
        return runMlvmTest(testClass, null);
    }

    /**
     * Launches MLVM test. In details, it:
     * <ol>
     *   <li>instantiates test class (optionally passing arguments to constructor)
     *   <li>parses command-line arguments using vm.share.options framework and updates the appropriate test fields
     *   <li>launches the tests
     *   <li>handles all test status passing methods (see below)
     * </ol>
     *
     * <p>Unlike {@link #launch()} methods, it does NOT:
     * <ul>
     *   <li>performs System.exit() with VM-testbase standard exit code 95 (success) or 97 (failure).
     *   <li>measures test run time and logs it
     *   <li>optionally dumps heap after test execution if {@link MlvmTest#setHeapDumpAfer(true)} was called
     *   <li>enables verbose logging on error
     * </ul>
     * Please see {@link #launch(Class<?> testClass, Object[] constructorArgs)} for additional details.
     *
     * The method throws unchecked exception when there is an error in test logic or handling.
     * Exceptions thrown from MlvmTest.run() method or test constructor are wrapped into java.lang.RuntimeException or java.lang.Error
     *
     * @param testClass a class to instantiate. Has to be subclass of vm.mlvm.share.MlvmTest or java.lang.Runnable
     * @param constructorArgs Arguments to pass to test constructor. Can be null.
     * @return true if test passed, false otherwise
     * @see #launch(Class<?> testClass, Object[] constructorArgs)
     */
    public static boolean runMlvmTest(Class<?> testClass, Object[] constructorArgs) {
        boolean passed;
        Throwable exception = null;

        try {
            MlvmTest instance = constructMlvmTest(testClass, constructorArgs);
            setupMlvmTest(instance);

            instance.initializeTest();

            try {
                passed = runMlvmTestInstance(instance);
            } catch(Throwable e) {
                exception = e;
                passed = false;
            }

            try {
                instance.finalizeTest(passed);
            } catch (Throwable e) {
                Env.complain(e, "TEST BUG: exception thrown in finalizeTest");
                if (exception == null) {
                    exception = e;
                }
                passed = false;
            }

        } catch (Throwable e) {
            Env.complain(e, "TEST BUG: exception thrown when instantiating/preparing test for run");
            exception = e;
            passed = false; // never really needed, but let's keep it
        }

        if (exception != null) {
            Env.throwAsUncheckedException(exception); // always throws
        }

        return passed;
    }

    private static void setupMlvmTest(MlvmTest instance) {
        MlvmTest.setInstance(instance);
        OptionSupport.setup(instance, Env.getArgParser().getRawArguments(), new IgnoreUnknownArgumentsHandler());
    }

    private static boolean runMlvmTestInstance(MlvmTest instance) throws Throwable {
        List<Class<? extends Throwable>> expectedExceptions = instance.getRequiredExceptions();
        int runsCount = instance.getRunsNumber() * instance.getStressOptions().getRunsFactor();
        if (runsCount < 1) {
            throw new RuntimeException("Runs number obtained via command-line options is less than 1");
        }

        int failedRuns = 0;

        try {
            for (int run = 1; run <= runsCount; ++run) {
                if (runsCount > 1) {
                    Env.traceNormal("TEST RUN " + run + "/" + runsCount + "; Failed " + failedRuns + " runs");
                }

                if (run > 1) {
                    instance.resetTest();
                }

                boolean instancePassed;
                if (expectedExceptions.size() == 0) {
                    instancePassed = instance.run();
                } else {
                    try {
                        instance.run();
                        Env.complain("Expected exceptions: " + expectedExceptions + ", but caught none");
                        instancePassed = false;
                    } catch (Throwable e) {
                        if (checkExpectedException(expectedExceptions, e)) {
                            instancePassed = true;
                        } else {
                            Env.complain(e, "Expected exceptions: " + expectedExceptions + ", but caught: ");
                            instancePassed = false;
                        }
                    }
                }

                if (instance.isMarkedFailed()) {
                    instancePassed = false;
                }

                if (!instancePassed) {
                    ++failedRuns;
                }
            }
        } finally {
             if (failedRuns > 0) {
                 Env.complain("Failed runs: " + failedRuns + " of " + runsCount);
             }
        }

        return failedRuns == 0;
    }

    private static Object constructTest(Class<?> testClass, Object[] constructorArgs) throws Throwable {
        if (constructorArgs == null || constructorArgs.length == 0) {
            return testClass.newInstance();
        }

        for (Constructor<?> c : testClass.getConstructors()) {
            Class<?> paramClasses[] = c.getParameterTypes();
            if (!parametersAreAssignableFrom(paramClasses, constructorArgs)) {
                continue;
            }

            return c.newInstance(constructorArgs);
        }

        throw new RuntimeException("Test bug: in class " + testClass.getName() + " no appropriate constructor found for arguments " + constructorArgs);
    }

    private static MlvmTest constructMlvmTest(Class<?> testClass, Object[] constructorArgs) throws Throwable {
        Object testObj = constructTest(testClass, constructorArgs);

        MlvmTest instance;
        if (testObj instanceof MlvmTest) {
            instance = (MlvmTest) testObj;
        } else if (testObj instanceof Runnable) {
            instance = new RunnableWrapper((Runnable) testObj);
        } else {
            // You can add wrapping of other types of tests here into MlvmTest if you need
            throw new RuntimeException("TEST BUG: Test class should be subclass of MlvmTest or Runnable. Its type is "
                                     + testObj.getClass().getName());
        }

        return instance;
    }

    private static boolean parametersAreAssignableFrom(Class<?>[] paramClasses, Object[] constructorArgs) {
        if (paramClasses.length != constructorArgs.length) {
            return false;
        }

        for (int i = 0; i < paramClasses.length; ++i) {
            if (!paramClasses[i].isAssignableFrom(constructorArgs[i].getClass())) {
                return false;
            }
        }

        return true;
    }

    private static boolean checkExpectedException(List<Class<? extends Throwable>> expectedExceptions, Throwable caught) throws Throwable {
        for (Class<? extends Throwable> expected : expectedExceptions) {
            if (expected.isAssignableFrom(caught.getClass())) {
                Env.traceNormal("Caught anticipated exception " + caught.getClass().getName() + ". Cause: " + caught.getCause());
                return true;
            }
        }

        return false;
    }

    private static class RunnableWrapper extends MlvmTest {
        private Runnable runnable;

        public RunnableWrapper(Runnable r) {
            runnable = r;
        }

        @Override
        public boolean run() throws Throwable {
            runnable.run();
            return true;
        }
    }
}
