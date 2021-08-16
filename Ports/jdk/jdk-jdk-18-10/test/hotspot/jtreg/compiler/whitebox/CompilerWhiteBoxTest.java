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

package compiler.whitebox;

import jdk.test.lib.Platform;
import sun.hotspot.WhiteBox;
import sun.hotspot.code.NMethod;

import java.lang.reflect.Executable;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Abstract class for WhiteBox testing of JIT.
 * Depends on jdk.test.lib.Platform from testlibrary.
 *
 * @author igor.ignatyev@oracle.com
 */
public abstract class CompilerWhiteBoxTest {
    /** {@code CompLevel::CompLevel_none} -- Interpreter */
    public static final int COMP_LEVEL_NONE = 0;
    /** {@code CompLevel::CompLevel_any}, {@code CompLevel::CompLevel_all} */
    public static final int COMP_LEVEL_ANY = -1;
    /** {@code CompLevel::CompLevel_simple} -- C1 */
    public static final int COMP_LEVEL_SIMPLE = 1;
    /** {@code CompLevel::CompLevel_limited_profile} -- C1, invocation &amp; backedge counters */
    public static final int COMP_LEVEL_LIMITED_PROFILE = 2;
    /** {@code CompLevel::CompLevel_full_profile} -- C1, invocation &amp; backedge counters + mdo */
    public static final int COMP_LEVEL_FULL_PROFILE = 3;
    /** {@code CompLevel::CompLevel_full_optimization} -- C2 */
    public static final int COMP_LEVEL_FULL_OPTIMIZATION = 4;
    /** Maximal value for CompLevel */
    public static final int COMP_LEVEL_MAX = COMP_LEVEL_FULL_OPTIMIZATION;

    /** Instance of WhiteBox */
    protected static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    /** Value of {@code -XX:CompileThreshold} */
    protected static final int COMPILE_THRESHOLD
            = Integer.parseInt(getVMOption("CompileThreshold", "10000"));
    /** Value of {@code -XX:BackgroundCompilation} */
    protected static final boolean BACKGROUND_COMPILATION
            = Boolean.valueOf(getVMOption("BackgroundCompilation", "true"));
    protected static final boolean USE_COUNTER_DECAY
            = Boolean.valueOf(getVMOption("UseCounterDecay", "true"));
    /** Value of {@code -XX:TieredCompilation} */
    protected static final boolean TIERED_COMPILATION
            = Boolean.valueOf(getVMOption("TieredCompilation", "false"));
    /** Value of {@code -XX:TieredStopAtLevel} */
    protected static final int TIERED_STOP_AT_LEVEL
            = Integer.parseInt(getVMOption("TieredStopAtLevel", "0"));
    /** Flag for verbose output, true if {@code -Dverbose} specified */
    protected static final boolean IS_VERBOSE
            = System.getProperty("verbose") != null;
    /** invocation count to trigger compilation */
    public static final int THRESHOLD;
    /** invocation count to trigger OSR compilation */
    protected static final long BACKEDGE_THRESHOLD;

    static {
        BACKEDGE_THRESHOLD = THRESHOLD = 150000;
    }

    /**
     * Returns value of VM option.
     *
     * @param name option's name
     * @return value of option or {@code null}, if option doesn't exist
     * @throws NullPointerException if name is null
     */
    protected static String getVMOption(String name) {
        Objects.requireNonNull(name);
        return Objects.toString(WHITE_BOX.getVMFlag(name), null);
    }

    /**
     * Returns value of VM option or default value.
     *
     * @param name         option's name
     * @param defaultValue default value
     * @return value of option or {@code defaultValue}, if option doesn't exist
     * @throws NullPointerException if name is null
     * @see #getVMOption(String)
     */
    protected static String getVMOption(String name, String defaultValue) {
        String result = getVMOption(name);
        return result == null ? defaultValue : result;
    }

    /** copy of is_c1_compile(int) from utilities/globalDefinitions.hpp */
    protected static boolean isC1Compile(int compLevel) {
        return (compLevel > COMP_LEVEL_NONE)
                && (compLevel < COMP_LEVEL_FULL_OPTIMIZATION);
    }

    /** copy of is_c2_compile(int) from utilities/globalDefinitions.hpp */
    protected static boolean isC2Compile(int compLevel) {
        return compLevel == COMP_LEVEL_FULL_OPTIMIZATION;
    }

    protected static void main(
            Function<TestCase, CompilerWhiteBoxTest> constructor,
            String[] args) {
        if (args.length == 0) {
            for (TestCase test : SimpleTestCase.values()) {
                constructor.apply(test).runTest();
            }
        } else {
            for (String name : args) {
                constructor.apply(SimpleTestCase.valueOf(name)).runTest();
            }
        }
    }

    /** tested method */
    protected final Executable method;
    protected final TestCase testCase;

    /**
     * Constructor.
     *
     * @param testCase object, that contains tested method and way to invoke it.
     */
    protected CompilerWhiteBoxTest(TestCase testCase) {
        Objects.requireNonNull(testCase);
        System.out.println("TEST CASE:" + testCase.name());
        method = testCase.getExecutable();
        this.testCase = testCase;
    }

    /**
     * Template method for testing. Prints tested method's info before
     * {@linkplain #test()} and after {@linkplain #test()} or on thrown
     * exception.
     *
     * @throws RuntimeException if method {@linkplain #test()} throws any
     *                          exception
     * @see #test()
     */
    protected final void runTest() {
        if (Platform.isInt()) {
            throw new Error("TESTBUG: test can not be run in interpreter");
        }
        System.out.println("at test's start:");
        printInfo();
        try {
            test();
        } catch (Exception e) {
            System.out.printf("on exception '%s':", e.getMessage());
            printInfo();
            e.printStackTrace();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
        System.out.println("at test's end:");
        printInfo();
    }

    /**
     * Checks, that {@linkplain #method} is not compiled at the given compilation
     * level or above.
     *
     * @param compLevel
     *
     * @throws RuntimeException if {@linkplain #method} is in compiler queue or
     *                          is compiled, or if {@linkplain #method} has zero
     *                          compilation level.
     */
    protected final void checkNotCompiled(int compLevel) {
        if (WHITE_BOX.isMethodQueuedForCompilation(method)) {
            throw new RuntimeException(method + " must not be in queue");
        }
        if (WHITE_BOX.getMethodCompilationLevel(method, false) >= compLevel) {
            throw new RuntimeException(method + " comp_level must be >= maxCompLevel");
        }
        if (WHITE_BOX.getMethodCompilationLevel(method, true) >= compLevel) {
            throw new RuntimeException(method + " osr_comp_level must be >= maxCompLevel");
        }
    }

    /**
     * Checks, that {@linkplain #method} is not compiled.
     *
     * @throws RuntimeException if {@linkplain #method} is in compiler queue or
     *                          is compiled, or if {@linkplain #method} has zero
     *                          compilation level.
     */
    protected final void checkNotCompiled() {
        waitBackgroundCompilation();
        checkNotCompiled(true);
        checkNotCompiled(false);
    }

    /**
     * Checks, that {@linkplain #method} is not (OSR-)compiled.
     *
     * @param isOsr Check for OSR compilation if true
     * @throws RuntimeException if {@linkplain #method} is in compiler queue or
     *                          is compiled, or if {@linkplain #method} has zero
     *                          compilation level.
     */
    protected final void checkNotCompiled(boolean isOsr) {
        if (WHITE_BOX.isMethodQueuedForCompilation(method)) {
            throw new RuntimeException(method + " must not be in queue");
        }
        if (WHITE_BOX.isMethodCompiled(method, isOsr)) {
            throw new RuntimeException(method + " must not be " +
                                       (isOsr ? "osr_" : "") + "compiled");
        }
        if (WHITE_BOX.getMethodCompilationLevel(method, isOsr) != 0) {
            throw new RuntimeException(method + (isOsr ? " osr_" : " ") +
                                       "comp_level must be == 0");
        }
    }

    /**
     * Checks, that {@linkplain #method} is compiled.
     *
     * @throws RuntimeException if {@linkplain #method} isn't in compiler queue
     *                          and isn't compiled, or if {@linkplain #method}
     *                          has nonzero compilation level
     */
    protected final void checkCompiled() {
        final long start = System.currentTimeMillis();
        waitBackgroundCompilation();
        if (WHITE_BOX.isMethodQueuedForCompilation(method)) {
            System.err.printf("Warning: %s is still in queue after %dms%n",
                    method, System.currentTimeMillis() - start);
            return;
        }
        if (!WHITE_BOX.isMethodCompiled(method, testCase.isOsr())) {
            throw new RuntimeException(method + " must be "
                    + (testCase.isOsr() ? "osr_" : "") + "compiled");
        }
        if (WHITE_BOX.getMethodCompilationLevel(method, testCase.isOsr())
                == 0) {
            throw new RuntimeException(method
                    + (testCase.isOsr() ? " osr_" : " ")
                    + "comp_level must be != 0");
        }
    }

    protected final void deoptimize() {
        WHITE_BOX.deoptimizeMethod(method, testCase.isOsr());
        if (testCase.isOsr()) {
            WHITE_BOX.deoptimizeMethod(method, false);
        }
    }

    protected final int getCompLevel() {
        NMethod nm = NMethod.get(method, testCase.isOsr());
        return nm == null ? COMP_LEVEL_NONE : nm.comp_level;
    }

    protected final boolean isCompilable() {
        return WHITE_BOX.isMethodCompilable(method, COMP_LEVEL_ANY,
                testCase.isOsr());
    }

    protected final boolean isCompilable(int compLevel) {
        return WHITE_BOX
                .isMethodCompilable(method, compLevel, testCase.isOsr());
    }

    protected final void makeNotCompilable() {
        WHITE_BOX.makeMethodNotCompilable(method, COMP_LEVEL_ANY,
                testCase.isOsr());
    }

    protected final void makeNotCompilable(int compLevel) {
        WHITE_BOX.makeMethodNotCompilable(method, compLevel, testCase.isOsr());
    }

    /**
     * Waits for completion of background compilation of {@linkplain #method}.
     */
    protected final void waitBackgroundCompilation() {
        waitBackgroundCompilation(method);
    }

    /**
     * Waits for completion of background compilation of the given executable.
     *
     * @param executable Executable
     */
    public static final void waitBackgroundCompilation(Executable executable) {
        if (!BACKGROUND_COMPILATION) {
            return;
        }
        final Object obj = new Object();
        for (int i = 0; i < 100
                && WHITE_BOX.isMethodQueuedForCompilation(executable); ++i) {
            synchronized (obj) {
                try {
                    obj.wait(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Prints information about {@linkplain #method}.
     */
    protected final void printInfo() {
        System.out.printf("%n%s:%n", method);
        System.out.printf("\tcompilable:\t%b%n",
                WHITE_BOX.isMethodCompilable(method, COMP_LEVEL_ANY, false));
        boolean isCompiled = WHITE_BOX.isMethodCompiled(method, false);
        System.out.printf("\tcompiled:\t%b%n", isCompiled);
        if (isCompiled) {
            System.out.printf("\tcompile_id:\t%d%n",
                    NMethod.get(method, false).compile_id);
        }
        System.out.printf("\tcomp_level:\t%d%n",
                WHITE_BOX.getMethodCompilationLevel(method, false));
        System.out.printf("\tosr_compilable:\t%b%n",
                WHITE_BOX.isMethodCompilable(method, COMP_LEVEL_ANY, true));
        isCompiled = WHITE_BOX.isMethodCompiled(method, true);
        System.out.printf("\tosr_compiled:\t%b%n", isCompiled);
        if (isCompiled) {
            System.out.printf("\tosr_compile_id:\t%d%n",
                    NMethod.get(method, true).compile_id);
        }
        System.out.printf("\tosr_comp_level:\t%d%n",
                WHITE_BOX.getMethodCompilationLevel(method, true));
        System.out.printf("\tin_queue:\t%b%n",
                WHITE_BOX.isMethodQueuedForCompilation(method));
        System.out.printf("compile_queues_size:\t%d%n%n",
                WHITE_BOX.getCompileQueuesSize());
    }

    /**
     * Executes testing.
     */
    protected abstract void test() throws Exception;

    /**
     * Tries to trigger compilation of {@linkplain #method} by call
     * {@linkplain TestCase#getCallable()} enough times.
     *
     * @return accumulated result
     * @see #compile(int)
     */
    protected final int compile() throws Exception {
        if (USE_COUNTER_DECAY) {
            throw new Exception("Tests using compile method must turn off counter decay for reliability");
        }
        if (testCase.isOsr()) {
            return compile(1);
        } else {
            return compile(THRESHOLD);
        }
    }

    /**
     * Tries to trigger compilation of {@linkplain #method} by call
     * {@linkplain TestCase#getCallable()} specified times.
     *
     * @param count invocation count
     * @return accumulated result
     */
    protected final int compile(int count) {
        int result = 0;
        Integer tmp;
        for (int i = 0; i < count; ++i) {
            try {
                tmp = testCase.getCallable().call();
            } catch (Exception e) {
                tmp = null;
            }
            result += tmp == null ? 0 : tmp;
        }
        if (IS_VERBOSE) {
            System.out.println("method was invoked " + count + " times");
        }
        return result;
    }

    /**
     * Utility interface provides tested method and object to invoke it.
     */
    public interface TestCase {
        /** the name of test case */
        String name();

        /** tested method */
        Executable getExecutable();

        /** object to invoke {@linkplain #getExecutable()} */
        Callable<Integer> getCallable();

        /** flag for OSR test case */
        boolean isOsr();
    }

    /**
     * @return {@code true} if the current test case is OSR and the mode is
     *          Xcomp, otherwise {@code false}
     */
    protected boolean skipXcompOSR() {
        boolean result = testCase.isOsr() && Platform.isComp();
        if (result && IS_VERBOSE) {
            System.err.printf("Warning: %s is not applicable in %s%n",
                    testCase.name(), Platform.vmInfo);
        }
        return result;
    }

    /**
     * Skip the test for the specified value of Tiered Compilation
     * @param value of TieredCompilation the test should not run with
     * @return {@code true} if the test should be skipped,
     *         {@code false} otherwise
     */
    public static boolean skipOnTieredCompilation(boolean value) {
        if (value == CompilerWhiteBoxTest.TIERED_COMPILATION) {
            System.err.println("Test isn't applicable w/ "
                    + (value ? "enabled" : "disabled")
                    + "TieredCompilation. Skip test.");
            return true;
        }
        return false;
    }
}

