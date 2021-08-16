/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
package optionsvalidation;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.AttachOperationFailedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jdk.test.lib.management.DynamicVMOption;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;
import jdk.test.lib.Platform;
import sun.tools.attach.HotSpotVirtualMachine;

import static optionsvalidation.JVMOptionsUtils.failedMessage;
import static optionsvalidation.JVMOptionsUtils.GCType;
import static optionsvalidation.JVMOptionsUtils.printOutputContent;
import static optionsvalidation.JVMOptionsUtils.VMType;

public abstract class JVMOption {

    private static final String UNLOCK_FLAG1 = "-XX:+UnlockDiagnosticVMOptions";
    private static final String UNLOCK_FLAG2 = "-XX:+UnlockExperimentalVMOptions";

    /**
     * Executor for JCMD
     */
    private final static CommandExecutor executor = new JMXExecutor();

    /**
     * Name of the tested parameter
     */
    protected String name;

    /**
     * Range is defined for option inside VM
     */
    protected boolean withRange;

    /**
     * Test valid min range value and additional small values
     */
    protected boolean testMinRange;

    /**
     * Test valid max range value and additional big values
     */
    protected boolean testMaxRange;

    private Set<Integer> allowedExitCodes;

    /**
     * Prepend string which added before testing option to the command line
     */
    private final List<String> prepend;
    private final StringBuilder prependString;

    protected JVMOption() {
        this.prepend = new ArrayList<>();
        prependString = new StringBuilder();
        allowedExitCodes = new HashSet<>();
        allowedExitCodes.add(0);
        allowedExitCodes.add(1);
        withRange = false;
        testMinRange = true;
        testMaxRange = true;
    }

    /**
     * Create JVM Option with given type and name.
     *
     * @param type type: "intx", "size_t", "uintx", "uint64_t" or "double"
     * @param name name of the option
     * @return created JVMOption
     */
    static JVMOption createVMOption(String type, String name) {
        JVMOption parameter;

        switch (type) {
            case "int":
            case "intx":
            case "size_t":
            case "uint":
            case "uintx":
            case "uint64_t":
                parameter = new IntJVMOption(name, type);
                break;
            case "double":
                parameter = new DoubleJVMOption(name);
                break;
            default:
                throw new Error("Expected only \"int\", \"intx\", \"size_t\", "
                        + "\"uint\", \"uintx\", \"uint64_t\", or \"double\" "
                        + "option types! Got " + type + " type!");
        }

        return parameter;
    }

    /**
     * Add passed options to the prepend options of the option. Prepend options
     * will be added before testing option to the command line.
     *
     * @param options array of prepend options
     */
    public final void addPrepend(String... options) {
        String toAdd;

        for (String option : options) {
            if (option.startsWith("-")) {
                toAdd = option;
            } else {
                /* Add "-" before parameter name */
                toAdd = "-" + option;

            }
            prepend.add(toAdd);
            prependString.append(toAdd).append(" ");
        }
    }

    /**
     * Get name of the option
     *
     * @return name of the option
     */
    public final String getName() {
        return name;
    }

    /**
     * Mark this option as option which range is defined inside VM
     */
    final void optionWithRange() {
        withRange = true;
    }

    /**
     * Exclude testing of min range value for this option
     */
    public final void excludeTestMinRange() {
        testMinRange = false;
    }

    /**
     * Exclude testing of max range value for this option
     */
    public final void excludeTestMaxRange() {
        testMaxRange = false;
    }

    public final void setAllowedExitCodes(Integer... allowedExitCodes) {
        this.allowedExitCodes.addAll(Arrays.asList(allowedExitCodes));
    }

    /**
     * Set new minimum option value
     *
     * @param min new minimum value
     */
    abstract void setMin(String min);

    /**
     * Get string with minimum value of the option
     *
     * @return string with minimum value of the option
     */
    abstract String getMin();

    /**
     * Set new maximum option value
     *
     * @param max new maximum value
     */
    abstract void setMax(String min);

    /**
     * Get string with maximum value of the option
     *
     * @return string with maximum value of the option
     */
    abstract String getMax();

    /**
     * Return list of strings with valid option values which used for testing
     * using jcmd, attach and etc.
     *
     * @return list of strings which contain valid values for option
     */
    protected abstract List<String> getValidValues();

    /**
     * Return list of strings with invalid option values which used for testing
     * using jcmd, attach and etc.
     *
     * @return list of strings which contain invalid values for option
     */
    protected abstract List<String> getInvalidValues();

    /**
     * Return expected error message for option with value "value" when it used
     * on command line with passed value
     *
     * @param value option value
     * @return expected error message
     */
    protected abstract String getErrorMessageCommandLine(String value);

    /**
     * Testing writeable option using DynamicVMOption isValidValue and
     * isInvalidValue methods
     *
     * @return number of failed tests
     */
    public int testDynamic() {
        DynamicVMOption option = new DynamicVMOption(name);
        int failedTests = 0;
        String origValue;

        if (option.isWriteable()) {

            System.out.println("Testing " + name + " option dynamically by DynamicVMOption");

            origValue = option.getValue();

            for (String value : getValidValues()) {
                if (!option.isValidValue(value)) {
                    failedMessage(String.format("Option %s: Valid value \"%s\" is invalid", name, value));
                    failedTests++;
                }
            }

            for (String value : getInvalidValues()) {
                if (option.isValidValue(value)) {
                    failedMessage(String.format("Option %s: Invalid value \"%s\" is valid", name, value));
                    failedTests++;
                }
            }

            option.setValue(origValue);
        }

        return failedTests;
    }

    /**
     * Testing writeable option using Jcmd
     *
     * @return number of failed tests
     */
    public int testJcmd() {
        DynamicVMOption option = new DynamicVMOption(name);
        int failedTests = 0;
        OutputAnalyzer out;
        String origValue;

        if (option.isWriteable()) {

            System.out.println("Testing " + name + " option dynamically by jcmd");

            origValue = option.getValue();

            for (String value : getValidValues()) {
                out = executor.execute(String.format("VM.set_flag %s %s", name, value), true);

                if (out.getOutput().contains(name + " error")) {
                    failedMessage(String.format("Option %s: Can not change "
                            + "option to valid value \"%s\" via jcmd", name, value));
                    printOutputContent(out);
                    failedTests++;
                }
            }

            for (String value : getInvalidValues()) {
                out = executor.execute(String.format("VM.set_flag %s %s", name, value), true);

                if (!out.getOutput().contains(name + " error")) {
                    failedMessage(String.format("Option %s: Error not reported for "
                            + "option when it chagned to invalid value \"%s\" via jcmd", name, value));
                    printOutputContent(out);
                    failedTests++;
                }
            }

            option.setValue(origValue);
        }

        return failedTests;
    }

    private boolean setFlagAttach(HotSpotVirtualMachine vm, String flagName, String flagValue) throws Exception {
        boolean result;

        try {
            vm.setFlag(flagName, flagValue);
            result = true;
        } catch (AttachOperationFailedException e) {
            result = false;
        }

        return result;
    }

    /**
     * Testing writeable option using attach method
     *
     * @return number of failed tests
     * @throws Exception if an error occurred while attaching to the target JVM
     */
    public int testAttach() throws Exception {
        DynamicVMOption option = new DynamicVMOption(name);
        int failedTests = 0;
        String origValue;

        if (option.isWriteable()) {

            System.out.println("Testing " + name + " option dynamically via attach");

            origValue = option.getValue();

            HotSpotVirtualMachine vm = (HotSpotVirtualMachine) VirtualMachine.attach(String.valueOf(ProcessTools.getProcessId()));

            for (String value : getValidValues()) {
                if (!setFlagAttach(vm, name, value)) {
                    failedMessage(String.format("Option %s: Can not change option to valid value \"%s\" via attach", name, value));
                    failedTests++;
                }
            }

            for (String value : getInvalidValues()) {
                if (setFlagAttach(vm, name, value)) {
                    failedMessage(String.format("Option %s: Option changed to invalid value \"%s\" via attach", name, value));
                    failedTests++;
                }
            }

            vm.detach();

            option.setValue(origValue);
        }

        return failedTests;
    }

    /**
     * Run java with passed parameter and check the result depending on the
     * 'valid' parameter
     *
     * @param param tested parameter passed to the JVM
     * @param valid indicates whether the JVM should fail or not
     * @return true - if test passed
     * @throws Exception if java process can not be started
     */
    private boolean runJavaWithParam(String optionValue, boolean valid) throws Exception {
        int exitCode = 0;
        boolean result = true;
        String errorMessage = null;
        String explicitGC = null;
        List<String> runJava = new ArrayList<>();
        OutputAnalyzer out = null;

        if (VMType != null) {
            runJava.add(VMType);
        }

        // Run with a small heap to avoid excessive execution time
        long max = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        if (max > 1024) {
            runJava.add("-Xmx1024m");
        }

        if (Platform.isDebugBuild()) {
            // Avoid excessive execution time.
            runJava.add("-XX:-ZapUnusedHeapArea");
        }

        if (GCType != null &&
            !(prepend.contains("-XX:+UseSerialGC") ||
              prepend.contains("-XX:+UseParallelGC") ||
              prepend.contains("-XX:+UseG1GC"))) {
            explicitGC = GCType;
        }

        if (explicitGC != null) {
            runJava.add(explicitGC);
        }

        runJava.add(UNLOCK_FLAG1);
        runJava.add(UNLOCK_FLAG2);

        runJava.addAll(prepend);
        runJava.add(optionValue);
        runJava.add(JVMStartup.class.getName());

        out = new OutputAnalyzer(ProcessTools.createJavaProcessBuilder(runJava).start());

        exitCode = out.getExitValue();
        String exitCodeString = null;
        if (exitCode != 0) {
            exitCodeString = exitCode + " [0x" + Integer.toHexString(exitCode).toUpperCase() + "]";
        }

        if (out.getOutput().contains("A fatal error has been detected by the Java Runtime Environment")) {
            /* Always consider "fatal error" in output as fail */
            errorMessage = "JVM output reports a fatal error. JVM exited with code " + exitCodeString + "!";
        } else if (out.getStderr().contains("Ignoring option " + name)) {
            // Watch for newly obsoleted, but not yet removed, flags
            System.out.println("SKIPPED: Ignoring test result for obsolete flag " + name);
        } else if (valid == true) {
            if (!allowedExitCodes.contains(exitCode)) {
                errorMessage = "JVM exited with unexpected error code = " + exitCodeString;
            } else if ((exitCode != 0) && (out.getOutput().isEmpty() == true)) {
                errorMessage = "JVM exited with error(exitcode == " + exitCodeString + "), but with empty stdout and stderr. " +
                       "Description of error is needed!";
            } else if (out.getOutput().contains("is outside the allowed range")) {
                errorMessage = "JVM output contains \"is outside the allowed range\"";
            }
        } else {
            // valid == false
            String value = optionValue.substring(optionValue.lastIndexOf("=") + 1);
            String errorMessageCommandLineValue = getErrorMessageCommandLine(value);
            if (exitCode == 0) {
                errorMessage = "JVM successfully exit";
            } else if (exitCode != 1) {
                errorMessage = "JVM exited with code " + exitCodeString + " which does not equal to 1";
            } else if (!out.getOutput().contains(errorMessageCommandLineValue)) {
                errorMessage = "JVM output does not contain expected output \"" + errorMessageCommandLineValue + "\"";
            }
        }

        if (errorMessage != null) {
            String fullOptionString = String.format("%s %s %s %s",
                    VMType == null ? "" : VMType, explicitGC == null ? "" : explicitGC, prependString.toString(), optionValue).trim().replaceAll("  +", " ");
            failedMessage(name, fullOptionString, valid, errorMessage);
            printOutputContent(out);
            result = false;
        }

        System.out.println("");

        return result;
    }

    /**
     * Construct option string with passed value
     *
     * @param value parameter value
     * @return string containing option with passed value
     */
    private String constructOption(String value) {
        return "-XX:" + name + "=" + value;
    }

    /**
     * Return list of strings which contain options with valid values which can
     * be used for testing on command line
     *
     * @return list of strings which contain options with valid values
     */
    private List<String> getValidCommandLineOptions() {
        List<String> validParameters = new ArrayList<>();

        for (String value : getValidValues()) {
            validParameters.add(constructOption(value));
        }

        return validParameters;
    }

    /**
     * Return list of strings which contain options with invalid values which
     * can be used for testing on command line
     *
     * @return list of strings which contain options with invalid values
     */
    private List<String> getInvalidCommandLineOptions() {
        List<String> invalidParameters = new ArrayList<>();

        for (String value : getInvalidValues()) {
            invalidParameters.add(constructOption(value));
        }

        return invalidParameters;
    }

    /**
     * Perform test of the parameter. Call java with valid option values and
     * with invalid option values.
     *
     * @return number of failed tests
     * @throws Exception if java process can not be started
     */
    public int testCommandLine() throws Exception {
        ProcessBuilder pb;
        int failed = 0;
        List<String> optionValuesList;

        optionValuesList = getValidCommandLineOptions();

        if (optionValuesList.isEmpty() != true) {
            System.out.println("Testing valid " + name + " values.");
            for (String optionValid : optionValuesList) {
                if (runJavaWithParam(optionValid, true) == false) {
                    failed++;
                }
            }
        }

        optionValuesList = getInvalidCommandLineOptions();

        if (optionValuesList.isEmpty() != true) {
            System.out.println("Testing invalid " + name + " values.");

            for (String optionInvalid : optionValuesList) {
                if (runJavaWithParam(optionInvalid, false) == false) {
                    failed++;
                }
            }
        }

        /* return number of failed tests for this option */
        return failed;
    }

}
