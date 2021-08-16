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

package compiler.lib.ir_framework;

import compiler.lib.ir_framework.driver.*;
import compiler.lib.ir_framework.shared.*;
import compiler.lib.ir_framework.test.*;
import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import jdk.test.lib.helpers.ClassFileInstaller;
import sun.hotspot.WhiteBox;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents the main entry point to the test framework whose main purpose is to perform regex-based checks on
 * the C2 IR shape emitted by the VM flags {@code -XX:+PrintIdeal} and {@code -XX:+PrintOptoAssembly}. The framework can
 * also be used for other non-IR matching (and non-compiler) tests by providing easy to use annotations for commonly used
 * testing patterns and compiler control flags.
 * <p>
 * The framework offers various annotations to control how your test code should be invoked and being checked. There are
 * three kinds of tests depending on how much control is needed over the test invocation:
 * <b>Base tests</b> (see {@link Test}), <b>checked tests</b> (see {@link Check}), and <b>custom run tests</b>
 * (see {@link Run}). Each type of test needs to define a unique <i>test method</i> that specifies a {@link Test @Test}
 * annotation which represents the test code that is eventually executed by the test framework. More information about
 * the usage and how to write different tests can be found in {@link Test}, {@link Check}, and {@link Run}.
 * <p>
 * Each test method can specify an arbitrary number of IR rules. This is done by using {@link IR @IR} annotations which
 * can define regex strings that are matched on the output of {@code -XX:+PrintIdeal} and {@code -XX:+PrintOptoAssembly}.
 * The matching is done after the test method was (optionally) warmed up and compiled. More information about the usage
 * and how to write different IR rules can be found at {@link IR}.
 * <p>
 * This framework should be used with the following JTreg setup in your Test.java file in package <i>some.package</i>:
 * <pre>
 * {@literal @}library /test/lib
 * {@literal @}run driver some.package.Test
 * </pre>
 * Note that even though the framework uses the Whitebox API internally, it is not required to build and enabel it in the
 * JTreg test if the test itself is not utilizing any Whitebox features directly.
 * <p>
 * To specify additional flags, use {@link #runWithFlags(String...)}, {@link #addFlags(String...)}, or
 * {@link #addScenarios(Scenario...)} where the scenarios can also be used to run different flag combinations
 * (instead of specifying multiple JTreg {@code @run} entries).
 * <p>
 * After annotating your test code with the framework specific annotations, the framework needs to be invoked from the
 * {@code main()} method of your JTreg test. There are two ways to do so. The first way is by calling the various
 * {@code runXX()} methods of {@link TestFramework}. The second way, which gives more control, is to create a new
 * {@code TestFramework} builder object on which {@link #start()} needs to be eventually called to start the testing.
 * <p>
 * The framework is called from the <i>driver VM</i> in which the JTreg test is initially run by specifying {@code
 * @run driver} in the JTreg header. This strips all additionally specified JTreg VM and Javaoptions.
 * The framework creates a new <i>flag VM</i> with all these flags added again in order to figure out which flags are
 * required to run the tests specified in the test class (e.g. {@code -XX:+PrintIdeal} and {@code -XX:+PrintOptoAssembly}
 * for IR matching).
 * <p>
 * After the flag VM terminates, it starts a new <i>test VM</i> which performs the execution of the specified
 * tests in the test class as described in {@link Test}, {@link Check}, and {@link Run}.
 * <p>
 * In a last step, once the test VM has terminated without exceptions, IR matching is performed if there are any IR
 * rules and if no VM flags disable it (e.g. not running with {@code -Xint}, see {@link IR} for more details).
 * The IR regex matching is done on the output of {@code -XX:+PrintIdeal} and {@code -XX:+PrintOptoAssembly} by parsing
 * the hotspot_pid file of the test VM. Failing IR rules are reported by throwing a {@link IRViolationException}.
 *
 * @see Test
 * @see Check
 * @see Run
 * @see IR
 */
public class TestFramework {
    /**
     * JTreg can define additional VM (-Dtest.vm.opts) and Javaoptions (-Dtest.java.opts) flags. IR verification is only
     * performed when all these additional JTreg flags (does not include additionally added framework and scenario flags
     * by user code) are whitelisted.
     *
     * A flag is whitelisted if it is a property flag (starting with -D), -ea, -esa, or if the flag name contains any of
     * the entries of this list as a substring (partial match).
     */
    public static final Set<String> JTREG_WHITELIST_FLAGS = new HashSet<>(
            Arrays.asList(
                    // The following substrings are part of more than one VM flag
                    "RAM",
                    "Heap",
                    "Trace",
                    "Print",
                    "Verify",
                    "TLAB",
                    "UseNewCode",
                    "Xmn",
                    "Xms",
                    "Xmx",
                    "Xss",
                    // The following substrings are only part of one VM flag (=exact match)
                    "CreateCoredumpOnCrash",
                    "IgnoreUnrecognizedVMOptions",
                    "UnlockDiagnosticVMOptions",
                    "UnlockExperimentalVMOptions",
                    "BackgroundCompilation",
                    "Xbatch",
                    "TieredCompilation",
                    "Xmixed",
                    "server",
                    "Xlog",
                    "LogCompilation"
            )
    );

    public static final boolean VERBOSE = Boolean.getBoolean("Verbose");
    public static final boolean TESTLIST = !System.getProperty("Test", "").isEmpty();
    public static final boolean EXCLUDELIST = !System.getProperty("Exclude", "").isEmpty();
    private static final boolean REPORT_STDOUT = Boolean.getBoolean("ReportStdout");
    // Only used for internal testing and should not be used for normal user testing.
    private static final boolean SKIP_WHITEBOX_INSTALL = Boolean.getBoolean("SkipWhiteBoxInstall");

    private static final String RERUN_HINT = """
                                               #############################################################
                                                - To only run the failed tests use -DTest, -DExclude,
                                                  and/or -DScenarios.
                                                - To also get the standard output of the test VM run with
                                                  -DReportStdout=true or for even more fine-grained logging
                                                  use -DVerbose=true.
                                               #############################################################
                                             """ + System.lineSeparator();

    private boolean irVerificationPossible = Boolean.parseBoolean(System.getProperty("VerifyIR", "true"));
    private boolean shouldVerifyIR; // Should we perform IR matching?
    private static boolean toggleBool;

    private final Class<?> testClass;
    private Set<Class<?>> helperClasses;
    private List<Scenario> scenarios;
    private Set<Integer> scenarioIndices;
    private List<String> flags;
    private int defaultWarmup = -1;

    /*
     * Public interface methods
     */

    /**
     * Creates an instance acting as a builder to test the class from which this constructor was invoked from.
     * Use this constructor if you want to use multiple run options (flags, helper classes, scenarios).
     * Use the associated add methods ({@link #addFlags(String...)}, {@link #addScenarios(Scenario...)},
     * {@link #addHelperClasses(Class...)}) to set up everything and then start the testing by invoking {@link #start()}.
     */
    public TestFramework() {
        this(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
    }

    /**
     * Creates an instance acting as a builder to test {@code testClass}.
     * Use this constructor if you want to use multiple run options (flags, helper classes, scenarios).
     * Use the associated add methods ({@link #addFlags(String...)}, @link #addScenarios(Scenario...)},
     * {@link #addHelperClasses(Class...)}) to set up everything and then start the testing by invoking {@link #start()}.
     *
     * @param testClass the class to be tested by the framework.
     * @see #TestFramework()
     */
    public TestFramework(Class<?> testClass) {
        TestRun.check(testClass != null, "Test class cannot be null");
        this.testClass = testClass;
        if (VERBOSE) {
            System.out.println("Test class: " + testClass);
        }
    }

    /**
     * Tests the class from which this method was invoked from.
     */
    public static void run() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        run(walker.getCallerClass());
    }

    /**
     * Tests {@code testClass}.
     *
     * @param testClass the class to be tested by the framework.
     * @see #run()
     */
    public static void run(Class<?> testClass) {
        TestFramework framework = new TestFramework(testClass);
        framework.start();
    }

    /**
     * Tests the class from which this method was invoked from. The test VM is called with the specified {@code flags}.
     * <ul>
     *     <li><p>The {@code flags} override any set VM or Javaoptions flags by JTreg by default.<p>
     *            Use {@code -DPreferCommandLineFlags=true} if you want to prefer the JTreg VM and  Javaoptions flags over
     *            the specified {@code flags} of this method.</li>
     *     <li><p>If you want to run your entire JTreg test with additional flags, use this method.</li>
     *     <li><p>If you want to run your entire JTreg test with additional flags but for another test class then the one
     *            from which this method was called from, use {@link #addFlags(String...)}, use this method.</li>
     *     <li><p>If you want to run your JTreg test with multiple flag combinations, use
     *            {@link #addScenarios(Scenario...)}</li>
     * </ul>
     *
     * @param flags VM flags to be used for the test VM.
     */
    public static void runWithFlags(String... flags) {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        TestFramework framework = new TestFramework(walker.getCallerClass());
        framework.addFlags(flags);
        framework.start();
    }

    /**
     * Add VM flags to be used for the test VM. These flags override any VM or Javaoptions set by JTreg by default.<p>
     * Use {@code -DPreferCommandLineFlags=true} if you want to prefer the VM or Javaoptions over the scenario flags.
     *
     * <p>
     * The testing can be started by invoking {@link #start()}
     *
     * @param flags VM options to be applied to the test VM.
     * @return the same framework instance.
     */
    public TestFramework addFlags(String... flags) {
        TestRun.check(flags != null && Arrays.stream(flags).noneMatch(Objects::isNull), "A flag cannot be null");
        if (this.flags == null) {
            this.flags = new ArrayList<>();
        }
        this.flags.addAll(Arrays.asList(flags));
        return this;
    }

    /**
     * Add helper classes that can specify additional compile command annotations ({@link ForceCompile @ForceCompile},
     * {@link DontCompile @DontCompile}, {@link ForceInline @ForceInline}, {@link DontInline @DontInline}) to be applied
     * while testing {@code testClass} (also see description of {@link TestFramework}).
     *
     * <p>
     * Duplicates in {@code helperClasses} are ignored. If a class is used by the test class that does not specify any
     * compile command annotations, you do not need to include it with this method. If no helper class specifies any
     * compile commands, you do not need to call this method at all.
     *
     * <p>
     * The testing can be started by invoking {@link #start()}.
     *
     * @param helperClasses helper classes containing compile command annotations ({@link ForceCompile},
     *                      {@link DontCompile}, {@link ForceInline}, {@link DontInline}) to be applied
     *                      while testing {@code testClass} (also see description of {@link TestFramework}).
     * @return the same framework instance.
     */
    public TestFramework addHelperClasses(Class<?>... helperClasses) {
        TestRun.check(helperClasses != null && Arrays.stream(helperClasses).noneMatch(Objects::isNull),
                      "A Helper class cannot be null");
        if (this.helperClasses == null) {
            this.helperClasses = new HashSet<>();
        }

        this.helperClasses.addAll(Arrays.asList(helperClasses));
        return this;
    }

    /**
     * Add scenarios to be used for the test VM. A test VM is called for each scenario in {@code scenarios} by using the
     * specified VM flags in the scenario. The scenario flags override any flags set by {@link #addFlags(String...)}
     * and thus also override any VM or Javaoptions set by JTreg by default.<p>
     * Use {@code -DPreferCommandLineFlags=true} if you want to prefer the VM and Javaoptions over the scenario flags.
     *
     * <p>
     * The testing can be started by invoking {@link #start()}
     *
     * @param scenarios scenarios which specify specific flags for the test VM.
     * @return the same framework instance.
     */
    public TestFramework addScenarios(Scenario... scenarios) {
        TestFormat.check(scenarios != null && Arrays.stream(scenarios).noneMatch(Objects::isNull),
                         "A scenario cannot be null");
        if (this.scenarios == null) {
            this.scenarios = new ArrayList<>();
            this.scenarioIndices = new HashSet<>();
        }

        for (Scenario scenario : scenarios) {
            int scenarioIndex = scenario.getIndex();
            TestFormat.check(scenarioIndices.add(scenarioIndex),
                             "Cannot define two scenarios with the same index " + scenarioIndex);
            this.scenarios.add(scenario);
        }
        return this;
    }

    /**
     * Start the testing of the implicitly (by {@link #TestFramework()}) or explicitly (by {@link #TestFramework(Class)})
     * set test class.
     */
    public void start() {
        if (!SKIP_WHITEBOX_INSTALL) {
            installWhiteBox();
        }
        disableIRVerificationIfNotFeasible();

        if (scenarios == null) {
            try {
                start(null);
            } catch (TestVMException e) {
                System.err.println(System.lineSeparator() + e.getExceptionInfo() + RERUN_HINT);
                throw e;
            } catch (IRViolationException e) {
                System.out.println("Compilation(s) of failed match(es):");
                System.out.println(e.getCompilations());
                System.err.println(System.lineSeparator() + e.getExceptionInfo() + System.lineSeparator() + RERUN_HINT);
                throw e;
            }
        } else {
            startWithScenarios();
        }
    }

    /**
     * Set a new default warm-up (overriding the framework default of 2000 at
     * {@link TestVM#WARMUP_ITERATIONS}) to be applied for all tests that do not specify an explicit
     * warm-up with {@link Warmup @Warmup}.
     *
     * @param defaultWarmup a new non-negative default warm-up.
     * @return the same framework instance.
     */
    public TestFramework setDefaultWarmup(int defaultWarmup) {
        TestFormat.check(defaultWarmup >= 0, "Cannot specify a negative default warm-up");
        this.defaultWarmup = defaultWarmup;
        return this;
    }

    /**
     * Get the VM output of the test VM. Use {@code -DVerbose=true} to enable more debug information. If scenarios
     * were run, use {@link Scenario#getTestVMOutput()}.
     *
     * @return the last test VM output.
     */
    public static String getLastTestVMOutput() {
        return TestVMProcess.getLastTestVMOutput();
    }

    /*
     * The following methods are only intended to be called from actual @Test methods and not from the main() method of
     * a JTreg test. Calling these methods from main() results in a linking exception (Whitebox not yet loaded and enabled).
     */

    /**
     * Compile {@code m} at compilation level {@code compLevel}. {@code m} is first enqueued and might not be compiled,
     * yet, upon returning from this method.
     *
     * @param m the method to be compiled.
     * @param compLevel the (valid) compilation level at which the method should be compiled.
     * @throws TestRunException if compilation level is {@link CompLevel#SKIP} or {@link CompLevel#WAIT_FOR_COMPILATION}.
     */
    public static void compile(Method m, CompLevel compLevel) {
        TestVM.compile(m, compLevel);
    }

    /**
     * Deoptimize {@code m}.
     *
     * @param m the method to be deoptimized.
     */
    public static void deoptimize(Method m) {
        TestVM.deoptimize(m);
    }

    /**
     * Returns a boolean indicating if {@code m} is compiled at any level.
     *
     * @param m the method to be checked.
     * @return {@code true} if {@code m} is compiled at any level;
     *         {@code false} otherwise.
     */
    public static boolean isCompiled(Method m) {
        return TestVM.isCompiled(m);
    }

    /**
     * Returns a boolean indicating if {@code m} is compiled with C1.
     *
     * @param m the method to be checked.
     * @return {@code true} if {@code m} is compiled with C1;
     *         {@code false} otherwise.
     */
    public static boolean isC1Compiled(Method m) {
        return TestVM.isC1Compiled(m);
    }

    /**
     * Returns a boolean indicating if {@code m} is compiled with C2.
     *
     * @param m the method to be checked.
     * @return {@code true} if {@code m} is compiled with C2;
     *         {@code false} otherwise.
     */
    public static boolean isC2Compiled(Method m) {
        return TestVM.isC2Compiled(m);
    }

    /**
     * Returns a boolean indicating if {@code m} is compiled at the specified {@code compLevel}.
     *
     * @param m the method to be checked.
     * @param compLevel the compilation level.
     * @return {@code true} if {@code m} is compiled at {@code compLevel};
     *         {@code false} otherwise.
     */
    public static boolean isCompiledAtLevel(Method m, CompLevel compLevel) {
        return TestVM.isCompiledAtLevel(m, compLevel);
    }

    /**
     * Checks if {@code m} is compiled at any level.
     *
     * @param m the method to be checked.
     * @throws TestRunException if {@code m} is not compiled at any level.
     */
    public static void assertCompiled(Method m) {
        TestVM.assertCompiled(m);
    }

    /**
     * Checks if {@code m} is not compiled at any level.
     *
     * @param m the method to be checked.
     * @throws TestRunException if {@code m} is compiled at any level.
     */
    public static void assertNotCompiled(Method m) {
        TestVM.assertNotCompiled(m);
    }

    /**
     * Verifies that {@code m} is compiled with C1.
     *
     * @param m the method to be verified.
     * @throws TestRunException if {@code m} is not compiled with C1.
     */
    public static void assertCompiledByC1(Method m) {
        TestVM.assertCompiledByC1(m);
    }

    /**
     * Verifies that {@code m} is compiled with C2.
     *
     * @param m the method to be checked.
     * @throws TestRunException if {@code m} is not compiled with C2.
     */
    public static void assertCompiledByC2(Method m) {
        TestVM.assertCompiledByC2(m);
    }

    /**
     * Verifies that {@code m} is compiled at the specified {@code compLevel}.
     *
     * @param m the method to be checked.
     * @param compLevel the compilation level.
     * @throws TestRunException if {@code m} is not compiled at {@code compLevel}.
     */
    public static void assertCompiledAtLevel(Method m, CompLevel compLevel) {
        TestVM.assertCompiledAtLevel(m, compLevel);
    }

    /**
     * Verifies that {@code m} was deoptimized after being C1 compiled.
     *
     * @param m the method to be checked.
     * @throws TestRunException if {@code m} is was not deoptimized after being C1 compiled.
     */
    public static void assertDeoptimizedByC1(Method m) {
        TestVM.assertDeoptimizedByC1(m);
    }

    /**
     * Verifies that {@code m} was deoptimized after being C2 compiled.
     *
     * @param m the method to be checked.
     * @throws TestRunException if {@code m} is was not deoptimized after being C2 compiled.
     */
    public static void assertDeoptimizedByC2(Method m) {
        TestVM.assertDeoptimizedByC2(m);
    }

    /**
     * Returns a different boolean each time this method is invoked (switching between {@code false} and {@code true}).
     * The very first invocation returns {@code false}. Note that this method could be used by different tests and
     * thus the first invocation for a test could be {@code true} or {@code false} depending on how many times
     * other tests have already invoked this method.
     *
     * @return an inverted boolean of the result of the last invocation of this method.
     */
    public static boolean toggleBoolean() {
        toggleBool = !toggleBool;
        return toggleBool;
    }

    /*
     * End of public interface methods
     */

    /**
     * Used to move Whitebox class to the right folder in the JTreg test
     */
    private void installWhiteBox() {
        try {
            ClassFileInstaller.main(WhiteBox.class.getName());
        } catch (Exception e) {
            throw new Error("failed to install whitebox classes", e);
        }
    }

    /**
     * Disable IR verification completely in certain cases.
     */
    private void disableIRVerificationIfNotFeasible() {
        if (irVerificationPossible) {
            irVerificationPossible = Platform.isDebugBuild() && !Platform.isInt() && !Platform.isComp();
            if (!irVerificationPossible) {
                System.out.println("IR verification disabled due to not running a debug build (required for PrintIdeal" +
                                   "and PrintOptoAssembly), running with -Xint, or -Xcomp (use warm-up of 0 instead)");
                return;
            }

            irVerificationPossible = hasIRAnnotations();
            if (!irVerificationPossible) {
                System.out.println("IR verification disabled due to test " + testClass + " not specifying any @IR annotations");
                return;
            }

            // No IR verification is done if additional non-whitelisted JTreg VM or Javaoptions flag is specified.
            irVerificationPossible = onlyWhitelistedJTregVMAndJavaOptsFlags();
            if (!irVerificationPossible) {
                System.out.println("IR verification disabled due to using non-whitelisted JTreg VM or Javaoptions flag(s)."
                                   + System.lineSeparator());
            }
        }
    }

    /**
     * For scenarios: Run the tests with the scenario settings and collect all exceptions to be able to run all
     * scenarios without prematurely throwing an exception. Format violations, however, are wrong for all scenarios
     * and thus is reported immediately on the first scenario execution.
     */
    private void startWithScenarios() {
        Map<Scenario, Exception> exceptionMap = new TreeMap<>(Comparator.comparingInt(Scenario::getIndex));
        for (Scenario scenario : scenarios) {
            try {
                start(scenario);
            } catch (TestFormatException e) {
                // Test format violation is wrong for all the scenarios. Only report once.
                throw e;
            } catch (Exception e) {
                exceptionMap.put(scenario, e);
            }
        }
        if (!exceptionMap.isEmpty()) {
            reportScenarioFailures(exceptionMap);
        }
    }

    private void reportScenarioFailures(Map<Scenario, Exception> exceptionMap) {
        String failedScenarios = "The following scenarios have failed: #"
                                 + exceptionMap.keySet().stream()
                                               .map(s -> String.valueOf(s.getIndex()))
                                               .collect(Collectors.joining(", #"));
        StringBuilder builder = new StringBuilder(failedScenarios);
        builder.append(System.lineSeparator()).append(System.lineSeparator());
        for (Map.Entry<Scenario, Exception> entry : exceptionMap.entrySet()) {
            Exception e = entry.getValue();
            Scenario scenario = entry.getKey();
            String errorMsg = "";
            if (scenario != null) {
                errorMsg = getScenarioTitleAndFlags(scenario);
            }
            if (e instanceof IRViolationException irException) {
                // For IR violations, only show the actual violations and not the (uninteresting) stack trace.
                System.out.println((scenario != null ? "Scenario #" + scenario.getIndex() + " - " : "")
                                   + "Compilation(s) of failed matche(s):");
                System.out.println(irException.getCompilations());
                builder.append(errorMsg).append(System.lineSeparator()).append(irException.getExceptionInfo());
            } else if (e instanceof TestVMException testVMException) {
                builder.append(errorMsg).append(System.lineSeparator()).append(testVMException.getExceptionInfo());
            } else {
                // Print stack trace otherwise
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                builder.append(errors.toString());
            }
            builder.append(System.lineSeparator());
        }
        System.err.println(builder.toString());
        if (!VERBOSE && !REPORT_STDOUT && !TESTLIST && !EXCLUDELIST) {
            // Provide a hint to the user how to get additional output/debugging information.
            System.err.println(RERUN_HINT);
        }
        throw new TestRunException(failedScenarios + ". Please check stderr for more information.");
    }

    private static String getScenarioTitleAndFlags(Scenario scenario) {
        StringBuilder builder = new StringBuilder();
        String title = "Scenario #" + scenario.getIndex();
        builder.append(title).append(System.lineSeparator()).append("=".repeat(title.length()))
               .append(System.lineSeparator());
        builder.append("Scenario flags: [").append(String.join(", ", scenario.getFlags())).append("]")
               .append(System.lineSeparator());
        return builder.toString();
    }

    /**
     * Execute a separate "flag" VM with White Box access to determine all test VM flags. The flag VM sends an encoding of
     * all required flags for the test VM to the driver VM over a socket. Once the flag VM exits, this driver VM parses the
     * test VM flags, which also determine if IR matching should be done, and then starts the test VM to execute all tests.
     */
    private void start(Scenario scenario) {
        if (scenario != null && !scenario.isEnabled()) {
            System.out.println("Disabled scenario #" + scenario.getIndex() + "! This scenario is not present in set flag " +
                               "-DScenarios and is therefore not executed.");
            return;
        }
        shouldVerifyIR = irVerificationPossible;
        try {
            // Use TestFramework flags and scenario flags for new VMs.
            List<String> additionalFlags = new ArrayList<>();
            if (flags != null) {
                additionalFlags.addAll(flags);
            }
            if (scenario != null) {
                List<String> scenarioFlags = scenario.getFlags();
                String scenarioFlagsString = scenarioFlags.isEmpty() ? "" : " - [" + String.join(", ", scenarioFlags) + "]";
                System.out.println("Scenario #" + scenario.getIndex() + scenarioFlagsString + ":");
                additionalFlags.addAll(scenarioFlags);
            }
            String frameworkAndScenarioFlags = additionalFlags.isEmpty() ?
                    "" : " - [" + String.join(", ", additionalFlags) + "]";

            if (shouldVerifyIR) {
                // Only need to use flag VM if an IR verification is possibly done.
                System.out.println("Run Flag VM:");
                FlagVMProcess flagVMProcess = new FlagVMProcess(testClass, additionalFlags);
                shouldVerifyIR = flagVMProcess.shouldVerifyIR();
                if (shouldVerifyIR) {
                    // Add more flags for the test VM which are required to do IR verification.
                    additionalFlags.addAll(flagVMProcess.getTestVMFlags());
                } // else: Flag VM found a reason to not do IR verification.
            } else {
                System.out.println("Skip Flag VM due to not performing IR verification.");
            }

            System.out.println("Run Test VM" + frameworkAndScenarioFlags + ":");
            runTestVM(additionalFlags);
        } finally {
            if (scenario != null) {
                scenario.setTestVMOutput(TestVMProcess.getLastTestVMOutput());
            }
            System.out.println();
        }
    }

    private boolean hasIRAnnotations() {
        return Arrays.stream(testClass.getDeclaredMethods()).anyMatch(m -> m.getAnnotationsByType(IR.class) != null);
    }

    private boolean onlyWhitelistedJTregVMAndJavaOptsFlags() {
        List<String> flags = Arrays.stream(Utils.getTestJavaOpts())
                                   .map(s -> s.replaceFirst("-XX:[+|-]?|-(?=[^D|^e])", ""))
                                   .collect(Collectors.toList());
        for (String flag : flags) {
            // Property flags (prefix -D), -ea and -esa are whitelisted.
            if (!flag.startsWith("-D") && !flag.startsWith("-e") && JTREG_WHITELIST_FLAGS.stream().noneMatch(flag::contains)) {
                // Found VM flag that is not whitelisted
                return false;
            }
        }
        return true;
    }

    private void runTestVM(List<String> additionalFlags) {
        TestVMProcess testVMProcess = new TestVMProcess(additionalFlags, testClass, helperClasses, defaultWarmup);
        if (shouldVerifyIR) {
            try {
                new IRMatcher(testVMProcess.getHotspotPidFileName(), testVMProcess.getIrEncoding(), testClass);
            } catch (IRViolationException e) {
                e.addCommandLine(testVMProcess.getCommandLine());
                throw e;
            }
        } else {
            System.out.println("IR verification disabled either due to no @IR annotations, through explicitly setting " +
                               "-DVerify=false, due to not running a debug build, using a non-whitelisted JTreg VM or " +
                               "Javaopts flag like -Xint, or running the test VM with other VM flags added by user code " +
                               "that make the IR verification impossible (e.g. -XX:-UseCompile, " +
                               "-XX:TieredStopAtLevel=[1,2,3], etc.).");
        }
    }

    public static void check(boolean test, String failureMessage) {
        if (!test) {
            throw new TestFrameworkException(failureMessage);
        }
    }
}
