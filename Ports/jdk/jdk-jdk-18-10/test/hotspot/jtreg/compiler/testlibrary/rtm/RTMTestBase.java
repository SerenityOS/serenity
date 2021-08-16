/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package compiler.testlibrary.rtm;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;
import jdk.test.lib.cli.CommandLineOptionTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auxiliary methods used for RTM testing.
 */
public class RTMTestBase {
    private static final String RTM_STATE_CHANGE_REASON = "rtm_state_change";
    /**
     * We don't parse compilation log as XML-document and use regular
     * expressions instead, because in some cases it could be
     * malformed.
     */
    private static final String FIRED_UNCOMMON_TRAP_PATTERN_TEMPLATE
            = "<uncommon_trap thread='[0-9]+' reason='%s'";
    private static final String INSTALLED_UNCOMMON_TRAP_PATTERN_TEMPLATE
            = "<uncommon_trap bci='[0-9]+' reason='%s'";

    /**
     * Executes RTM test in a new JVM started with {@code options} cli options.
     *
     * @param test test case to execute.
     * @param options additional options for VM
     * @throws Exception when something went wrong.
     */
    public static OutputAnalyzer executeRTMTest(CompilableTest test,
            String... options) throws Exception {
        ProcessBuilder processBuilder
                = ProcessTools.createJavaProcessBuilder(
                RTMTestBase.prepareTestOptions(test, options));
        OutputAnalyzer outputAnalyzer
                = new OutputAnalyzer(processBuilder.start());
        System.out.println(outputAnalyzer.getOutput());
        return outputAnalyzer;
    }

    /**
     * Executes test case and save compilation log to {@code logFileName}.
     *
     * @param logFileName a name of compilation log file
     * @param test a test case to execute case to execute
     * @param options additional options to VM
     * @return OutputAnalyzer for started test case
     * @throws Exception when something went wrong
     */
    public static OutputAnalyzer executeRTMTest(String logFileName,
            CompilableTest test, String... options) throws Exception {
        ProcessBuilder processBuilder
                = ProcessTools.createJavaProcessBuilder(
                RTMTestBase.prepareTestOptions(logFileName, test, options));
        OutputAnalyzer outputAnalyzer
                = new OutputAnalyzer(processBuilder.start());

        System.out.println(outputAnalyzer.getOutput());

        return outputAnalyzer;
    }

    /**
     * Finds count of uncommon traps with reason {@code reason} installed
     * during compilation.
     *
     * @param compilationLogFile a path to file with LogCompilation output.
     * @param reason reason of installed uncommon traps.
     * @return count of installed uncommon traps with reason {@code reason}.
     * @throws IOException
     */
    public static int installedUncommonTraps(String compilationLogFile,
            String reason)throws IOException {
        String pattern = String.format(
                RTMTestBase.INSTALLED_UNCOMMON_TRAP_PATTERN_TEMPLATE,
                reason);
        return RTMTestBase.findTraps(compilationLogFile, pattern);
    }

    /**
     * Finds count of uncommon traps with reason <i>rtm_state_change</i>
     * installed during compilation.
     *
     * @param compilationLogFile a path to file with LogCompilation output.
     * @return count of installed uncommon traps with reason
     *         <i>rtm_state_change</i>.
     * @throws IOException
     */
    public static int installedRTMStateChangeTraps(String compilationLogFile)
            throws IOException {
        return RTMTestBase.installedUncommonTraps(compilationLogFile,
                RTMTestBase.RTM_STATE_CHANGE_REASON);
    }

    /**
     * Finds count of fired uncommon traps with reason {@code reason}.
     *
     * @param compilationLogFile a path to file with LogCompilation output.
     * @param reason a reason of fired uncommon traps.
     * @return count of fired uncommon traps with reason {@code reason}.
     * @throws IOException
     */
    public static int firedUncommonTraps(String compilationLogFile,
            String reason) throws IOException {
        String pattern = String.format(
                RTMTestBase.FIRED_UNCOMMON_TRAP_PATTERN_TEMPLATE,
                reason);
        return RTMTestBase.findTraps(compilationLogFile, pattern);
    }

    /**
     * Finds count of fired uncommon traps with reason <i>rtm_state_change</i>.
     *
     * @param compilationLogFile a path to file with LogCompilation output.
     * @return count of fired uncommon traps with reason
     *         <i>rtm_state_change</i>.
     * @throws IOException
     */
    public static int firedRTMStateChangeTraps(String compilationLogFile)
            throws IOException {
        return RTMTestBase.firedUncommonTraps(compilationLogFile,
                RTMTestBase.RTM_STATE_CHANGE_REASON);
    }

    /**
     * Finds count of uncommon traps that matches regular
     * expression in {@code re}.
     *
     * @param compilationLogFile a path to file with LogCompilation output.
     * @param re regular expression to match uncommon traps.
     * @throws IOException
     */
    private static int findTraps(String compilationLogFile, String re)
            throws IOException {
        String compilationLog = RTMTestBase.fileAsString(compilationLogFile);
        Pattern pattern = Pattern.compile(re);
        Matcher matcher = pattern.matcher(compilationLog);
        int traps = 0;
        while (matcher.find()) {
            traps++;
        }
        return traps;
    }

    /**
     * Returns file's content as a string.
     *
     * @param path a path to file to operate on.
     * @return string with content of file.
     * @throws IOException
     */
    private static String fileAsString(String path) throws IOException {
        byte[] fileAsBytes = Files.readAllBytes(Paths.get(path));
        return new String(fileAsBytes);
    }

    /**
     * Prepares VM options for test execution.
     * This method get test java options, filter out all RTM-related options,
     * adds CompileCommand=compileonly,method_name options for each method
     * from {@code methodToCompile} and finally appends all {@code vmOpts}.
     *
     * @param test test case whose methods that should be compiled.
     *             If {@code null} then no additional <i>compileonly</i>
     *             commands will be added to VM options.
     * @param vmOpts additional options to pass to VM.
     * @return Array with VM options.
     */
    private static String[] prepareTestOptions(CompilableTest test,
            String... vmOpts) {
        return RTMTestBase.prepareFilteredTestOptions(test, null, vmOpts);
    }

    /**
     * Prepares VM options for test execution.
     * This method get test java options, filter out all RTM-related options
     * and all options that matches regexps in {@code additionalFilters},
     * adds CompileCommand=compileonly,method_name options for each method
     * from {@code methodToCompile} and finally appends all {@code vmOpts}.
     *
     * @param test test case whose methods that should be compiled.
     *             If {@code null} then no additional <i>compileonly</i>
     *             commands will be added to VM options.
     * @param additionalFilters array with regular expression that will be
     *                          used to filter out test java options.
     *                          If {@code null} then no additional filters
     *                          will be used.
     * @param vmOpts additional options to pass to VM.
     * @return array with VM options.
     */
    private static String[] prepareFilteredTestOptions(CompilableTest test,
            String[] additionalFilters, String... vmOpts) {
        List<String> finalVMOpts = new LinkedList<>();
        String[] filters;

        if (additionalFilters != null) {
            filters = Arrays.copyOf(additionalFilters,
                    additionalFilters.length + 1);
        } else {
            filters = new String[1];
        }

        filters[filters.length - 1] = "RTM";
        String[] filteredVMOpts = Utils.getFilteredTestJavaOpts(filters);
        Collections.addAll(finalVMOpts, filteredVMOpts);
        Collections.addAll(finalVMOpts, "-Xcomp", "-server",
                "-XX:-TieredCompilation", "-XX:+UseRTMLocking",
                CommandLineOptionTest.UNLOCK_DIAGNOSTIC_VM_OPTIONS,
                CommandLineOptionTest.UNLOCK_EXPERIMENTAL_VM_OPTIONS,
                "-Xbootclasspath/a:.", "-XX:+WhiteBoxAPI",
                "--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED");

        if (test != null) {
            for (String method : test.getMethodsToCompileNames()) {
                finalVMOpts.add("-XX:CompileCommand=compileonly," + method);
            }
        }
        Collections.addAll(finalVMOpts, vmOpts);
        return finalVMOpts.toArray(new String[finalVMOpts.size()]);
    }

    /**
     * Adds additional options for VM required for successful execution of test.
     *
     * @param logFileName a name of compilation log file
     * @param test a test case to execute
     * @param options additional options to VM
     * @return an array with VM options
     */
    private static String[] prepareTestOptions(String logFileName,
            CompilableTest test, String... options) {
        String[] preparedOptions = RTMTestBase.prepareFilteredTestOptions(
                test,
                new String[] {
                        "LogCompilation",
                        "LogFile"
                });
        List<String> updatedOptions = new LinkedList<>();
        Collections.addAll(updatedOptions, preparedOptions);
        Collections.addAll(updatedOptions,
                "-XX:+LogCompilation",
                "-XX:LogFile=" + logFileName);
        Collections.addAll(updatedOptions, options);

        return updatedOptions.toArray(new String[updatedOptions.size()]);
    }
}
