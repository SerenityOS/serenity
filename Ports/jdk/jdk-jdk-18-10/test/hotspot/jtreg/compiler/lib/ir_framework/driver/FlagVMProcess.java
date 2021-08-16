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

package compiler.lib.ir_framework.driver;

import compiler.lib.ir_framework.TestFramework;
import compiler.lib.ir_framework.shared.TestFrameworkException;
import compiler.lib.ir_framework.flag.FlagVM;
import compiler.lib.ir_framework.shared.TestRunException;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class prepares, creates, and runs the "flag" VM with verification of proper termination. The flag VM determines
 * the flags required for the "test" VM. The flag VM writes these flags to a dedicated file which is then parsed by this
 * class after the termination of the flag VM.
 *
 * @see FlagVM
 */
public class FlagVMProcess {
    private static final boolean VERBOSE = Boolean.getBoolean("Verbose");

    private final List<String> cmds;
    private final List<String> testVMFlags;
    private boolean shouldVerifyIR;
    private String testVMFlagsFile;
    private OutputAnalyzer oa;

    public FlagVMProcess(Class<?> testClass, List<String> additionalFlags) {
        cmds = new ArrayList<>();
        testVMFlags = new ArrayList<>();
        prepareVMFlags(testClass, additionalFlags);
        start();
        parseTestVMFlags();
    }

    private void parseTestVMFlags() {
        String flags = readFlagsFromFile();
        if (VERBOSE) {
            System.out.println("Read data from " + testVMFlagsFile + ":");
            System.out.println(flags);
        }
        String patternString = "(.*DShouldDoIRVerification=(true|false).*)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(flags);
        TestFramework.check(matcher.find(), "Invalid flag encoding emitted by flag VM");
        // Maybe we run with flags that make IR verification impossible
        shouldVerifyIR = Boolean.parseBoolean(matcher.group(2));
        testVMFlags.addAll(Arrays.asList(matcher.group(1).split(FlagVM.TEST_VM_FLAGS_DELIMITER)));
    }

    private String readFlagsFromFile() {
        try (var br = Files.newBufferedReader(Paths.get(testVMFlagsFile))) {
            String flags = br.readLine();
            TestFramework.check(br.readLine() == null, testVMFlagsFile + " should only contain one line.");
            return flags;

        } catch (IOException e) {
            throw new TestFrameworkException("Error while reading from file " + testVMFlagsFile, e);
        }
    }

    /**
     * The flag VM needs White Box access to prepare all test VM flags. The flag VM will write the test VM flags to
     * a dedicated file which is afterwards parsed by the driver VM and added as flags to the test VM.
     */
    private void prepareVMFlags(Class<?> testClass, List<String> additionalFlags) {
        cmds.add("-Dtest.jdk=" + Utils.TEST_JDK);
        // Set java.library.path so JNI tests which rely on jtreg nativepath setting work
        cmds.add("-Djava.library.path=" + Utils.TEST_NATIVE_PATH);
        cmds.add("-cp");
        cmds.add(Utils.TEST_CLASS_PATH);
        cmds.add("-Xbootclasspath/a:.");
        cmds.add("-XX:+UnlockDiagnosticVMOptions");
        cmds.add("-XX:+WhiteBoxAPI");
        // TestFramework and scenario flags might have an influence on the later used test VM flags. Add them as well.
        cmds.addAll(additionalFlags);
        cmds.add(FlagVM.class.getCanonicalName());
        cmds.add(testClass.getCanonicalName());
    }

    private void start() {
        try {
            // Run "flag" VM with White Box access to determine the test VM flags and if IR verification should be done.
            oa = ProcessTools.executeTestJvm(cmds);
        } catch (Exception e) {
            throw new TestRunException("Failed to execute TestFramework flag VM", e);
        }
        testVMFlagsFile = FlagVM.TEST_VM_FLAGS_FILE_PREFIX + oa.pid()
                          + FlagVM.TEST_VM_FLAGS_FILE_POSTFIX;
        checkFlagVMExitCode();
    }

    private void checkFlagVMExitCode() {
        String flagVMOutput = oa.getOutput();
        int exitCode = oa.getExitValue();
        if (VERBOSE && exitCode == 0) {
            System.out.println("--- OUTPUT TestFramework flag VM ---");
            System.out.println(flagVMOutput);
        }

        if (exitCode != 0) {
            System.err.println("--- OUTPUT TestFramework flag VM ---");
            System.err.println(flagVMOutput);
            throw new RuntimeException("TestFramework flag VM exited with " + exitCode);
        }
    }

    public List<String> getTestVMFlags() {
        return testVMFlags;
    }

    public boolean shouldVerifyIR() {
        return shouldVerifyIR;
    }
}
