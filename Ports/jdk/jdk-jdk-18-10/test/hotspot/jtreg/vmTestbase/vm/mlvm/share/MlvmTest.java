/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import nsk.share.ArgumentParser;
import nsk.share.Log;
import nsk.share.Log.TraceLevel;
import nsk.share.test.StressOptions;
import nsk.share.test.Stresser;
import vm.share.options.Option;
import vm.mlvm.share.ExceptionsOptionObjectFactory;

/**
 * The base class for MLVM tests.
 * Subclasses need to override {@link #run()} method to implement test logic.
 */
public abstract class MlvmTest {

    /**
     * MLVM tests are expected to implement this method to provide the logic.
     *
     * @return true if test passed, false if failed
     * @throws Throwable any subclass of Throwable to indicate test failure
     */
    public abstract boolean run() throws Throwable;

    /** Performs pre-run (prolog) actions in MlvmTest subclasses.
     * The default implementation does nothing.
     * Sublcasses may override this method to perform custom actions after test is initialized
     * (initialization order is described in MlvmTestExecutor class) but before {@link run()} method is invoked.
     * @throws Throwable in case of problem, which is interpreted as a test failure
     * @see MlvmTestExecutor
     */
    protected void initializeTest() throws Throwable {
    }

    /** Performs post-run (epilog) actions.
     * This method is executed after the {@link #run()} method.
     * Does nothing by default.
     * Subclasses may override this method when some finalization actions are required.
     * Test fails if this method throws exception.
     * @param result test execution status: true, if test passed, false otherwise
     * @throws Throwable may throw any subclass of Throwable to indicate test failure (regardless of run() method result)
     * @see MlvmTestExecutor
     */
    protected void finalizeTest(boolean result) throws Throwable {
    }

    /**
     * Resets the tests between runs.
     * You may override this method, especially if your test supports -stressRunsFactor option
     * @throws Throwable may throw any subclass of Throwable to indicate test failure (regardless of run() method result)
     * @see MlvmTestExecutor
     */
    protected void resetTest() throws Throwable {
        testMarkedFailed = false;
    }

    // Options for all MlvmTests
    @Option(name = "requireExceptions", default_value = "", factory = ExceptionsOptionObjectFactory.class,
            description = "Specifying this option turns test into negative one: "
                        + "the specified exception class names separated with commas have to be caught for the test to pass")
    private List<Class<? extends Throwable>> requiredExceptionClasses = new ArrayList<>();

    @Option(name = "runs", default_value = "1", description = "How many times the test should be re-run")
    private int runs = 1;

    // Some internal stuff
    private static MlvmTest instance;

    /**
     * Sets internal static variable to instance of the test.
     * Used in debugger/debuggee tests.
     * Not intended to work if there are several MlvmTests created.
     * @param inst Instance of the test
     */
    public static void setInstance(MlvmTest inst) {
        instance = inst;
    }

    /**
     * Returns internal static variable holding instance of the test, which was set using {@link #setInstance()}.
     * Used in debugger/debuggee tests.
     * Not intended to work if there are several MlvmTests created.
     * @return Instance of the test
     */
    public static MlvmTest getInstance() {
        return instance;
    }

    private static String name = "Test";

    /**
     * Sets internal static variable to the name of the test.
     * Debugger/debuggee MLVM tests use this feature to differentiate logging from debugger and debuggee
     * Not intended to work if there are several MlvmTests created
     * @param n Name of the test
     */
    public static void setName(String n) {
        name = n;
    }

    /**
     * Returns internal static variable holding the name of the test.
     * Debugger/debuggee MLVM tests use this feature to differentiate logging from debugger and debuggee
     * Not intended to work if there are several MlvmTests created
     * @return Name of the test
     */
    public static String getName() {
        return name;
    }

    /**
     * Sets number of test runs
     * @param r Number of test runs
     */
    public void setRunsNumber(int r) {
        runs = r;
    }

    /**
     * Return number of test runs
     * @return Number of test runs
     */
    public int getRunsNumber() {
        return runs;
    }

    // Sugar...
    /**
     * Provides Random Number Generator for the test. The tests should always use this generator
     * to guarantee repeatability, especially in multi-threaded usages
     * @return Random number generator for this thread, seeded with command-line option, if provided
     */
    public static Random getRNG() {
        return Env.getRNG();
    }

    /**
     * Returns logger, which is used in all MLVM framework. This guarantees correct ordering of messages
     * @return Logger object
     */
    public static Log getLog() {
        return Env.getLog();
    }

    /**
     * ArgumentParser is the old implementation of command-line parser (the new tests should use
     * vm.share.options framework). However it is maintained, because nsk JDI/SAJDI framework is built
     * on ArgumentParser.
     * @return ArgumentParser object created with command-line options (see {@link MlvmTestExecutor}
     *         for details)
     */
    public static ArgumentParser getArgumentParser() {
        return Env.getArgParser();
    }

    // ...and spice

    /* Makes the test "negative": one of the specified exception classes has to be thrown by the test to pass.
     * Test fails if exception has not been thrown.
     * Boolean value returned by {@link run()} method is ignored.
     * Calling {@link #markTestFailed()} causes test to fail anyway.
     * <p>
     * Invoke this method BEFORE run() method (e.g., in prolog) to instruct launcher
     * to anticipate the exception instead of the positive (normal) mode.
     * @param classes The list of exception classes
     *                Empty list or null indicates that test is positive.
     */
    @SafeVarargs
    public final void setRequiredExceptions(Class<? extends Throwable>... classes) {
        setRequiredExceptions(Arrays.asList(classes));
    }

    /* Makes the test "negative": one of the specified exception classes has to be thrown by the test to pass.
     * Test fails if exception has not been thrown.
     * Boolean value returned by {@link run()} method is ignored.
     * Calling {@link #markTestFailed()} causes test to fail anyway.
     * <p>
     * Invoke this method BEFORE run() method (e.g., in prolog) to instruct launcher
     * @param classes The list of exception classes.
     *                Empty list or null indicates that test is positive (in its standard form)
     */
    public final void setRequiredExceptions(List<Class<? extends Throwable>> classes) {
        if (requiredExceptionClasses.size() > 0) {
            Env.traceNormal("Expected exceptions specified in the test are overridden in command-line");
            return;
        }

        requiredExceptionClasses = classes;
    }

    /**
     * Returns the list of required exceptions
     * (please see {@link #setRequiredExceptions(Class<? extends Throwable>... classes)} method for details.
     * @return The list of exception classes. Empty list or null indicates that test is positive (in its standard form)
     */
    public final List<Class<? extends Throwable>> getRequiredExceptions() {
        return requiredExceptionClasses;
    }

    private boolean testMarkedFailed = false;

    /**
     * Marks the test as failed.
     * Regardless of run() method return value, the test is considered failed. Operation is not reversible.
     * Can be called from multiple threads
     */
    protected final void markTestFailed() {
        markTestFailed(null, null);
    }

    /**
     * Marks the test as failed, indicating falure reason.
     * Regardless of run() method return value, the test is considered failed. Operation is not reversible.
     * Can be called from multiple threads
     * @param msg A message to log (using Log.complain() method)
     */
    protected final void markTestFailed(String msg) {
        markTestFailedImpl(msg, null);
    }

    /**
     * Marks the test as failed, indicating falure reason and exception, which caused it.
     * Regardless of run() method return value, the test is considered failed. Operation is not reversible.
     * Can be called from multiple threads
     * @param msg A message to log (using Log.complain() method)
     * @param t An exception to log
     */
    protected final void markTestFailed(String msg, Throwable t) {
        markTestFailedImpl(msg, t);
    }

    private synchronized void markTestFailedImpl(String msg, Throwable t) {
        testMarkedFailed = true;

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        Env.complain(t, "%s marked failed at %s%s", getName(), stackTrace[3],
                     msg == null ? "" : ":\n" + msg);

    }

    /**
     * Checks if the test has marked failed.
     * @return true, if the test marked failed
     */
    protected final synchronized boolean isMarkedFailed() {
        return testMarkedFailed;
    }

    private static boolean dumpHeapAfter = false;

    /**
     * Checks if heap dump requestd after running the test.
     * @return true, if the test marked failed
     * @see MlvmTestExecutor for heap dumping details.
     */
    public static synchronized boolean getHeapDumpAfter() {
        return dumpHeapAfter;
    }

    /**
     * Sets or clears heap dumping request. Heap is dumped in MlvmTestExecutor after running the test.
     *
     * NB. heap dumping uses ProcessUtils libraries, so it should be added to library path in cfg-file:
     * {@code export LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:${COMMON_LIBS_LOCATION}/lib/${ARCH}/vm/share"}
     * @param enable true, if heap should be dumped, false if not
     * @see MlvmTestExecutor for heap dumping details.
     */
    public static synchronized void setHeapDumpAfter(boolean enable) {
        dumpHeapAfter = enable;
    }

    protected static Stresser createStresser() {
        Stresser s = new Stresser(getArgumentParser().getStressOptions());
        if (getLog().getTraceLevel() >= TraceLevel.TRACE_VERBOSE) {
            s.printStressInfo(getLog().getOutStream());
        }
        return s;
    }

    protected static StressOptions getStressOptions() {
        return getArgumentParser().getStressOptions();
    }

    // Launchers are left here for compatibility. Launching code has been moved to MlvmTestExecutor
    // TODO: A minor bug has to be filed to replace MlvmTest.launch() calls with MlvmTestExecutor.launch()

    protected static void launch(ArgumentParser argumentParser) {
        MlvmTestExecutor.launch(argumentParser);
    }

    protected static void launch(ArgumentParser argumentParser, Object[] constructorArgs) {
        MlvmTestExecutor.launch(argumentParser, constructorArgs);
    }

    protected static void launch(String[] args) {
        MlvmTestExecutor.launch(args, null);
    }

    protected static void launch(String[] args, Object[] constructorArgs) {
        MlvmTestExecutor.launch(args, constructorArgs);
    }

}
