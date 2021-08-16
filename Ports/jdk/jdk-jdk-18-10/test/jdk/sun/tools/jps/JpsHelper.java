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

import static jdk.test.lib.Asserts.assertGreaterThan;
import static jdk.test.lib.Asserts.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

/**
 * The helper class for running jps utility and verifying output from it
 */
public final class JpsHelper {

    /**
     * Helper class for handling jps arguments
     */
    public enum JpsArg {
        q,
        l,
        m,
        v,
        V;

        /**
         * Generate all possible combinations of {@link JpsArg}
         * (31 argument combinations and no arguments case)
         */
        public static List<List<JpsArg>> generateCombinations() {
            final int argCount = JpsArg.values().length;
            // If there are more than 30 args this algorithm will overflow.
            Asserts.assertLessThan(argCount, 31, "Too many args");

            List<List<JpsArg>> combinations = new ArrayList<>();
            int combinationCount = (int) Math.pow(2, argCount);
            for (int currCombo = 0; currCombo < combinationCount; ++currCombo) {
                List<JpsArg> combination = new ArrayList<>();
                for (int position = 0; position < argCount; ++position) {
                    int bit = 1 << position;
                    if ((bit & currCombo) != 0) {
                        combination.add(JpsArg.values()[position]);
                    }
                }
                combinations.add(combination);
            }
            return combinations;
        }

        /**
         *  Return combination of {@link JpsArg} as a String array
         */
        public static String[] asCmdArray(List<JpsArg> jpsArgs) {
            List<String> list = new ArrayList<>();
            for (JpsArg jpsArg : jpsArgs) {
                list.add("-" + jpsArg.toString());
            }
            return list.toArray(new String[list.size()]);
        }

    }

    /**
     * VM flag to start test application with
     */
    public static final String VM_FLAG = "+DisableExplicitGC";

    private static File vmFlagsFile = null;
    /**
     * VM arguments to start test application with.
     * -XX:+UsePerfData is required for running the tests on embedded platforms.
     */
    private static String[] testVmArgs = {
      "-XX:+UsePerfData", "-Xmx512m", "-Xlog:gc",
      "-Dmultiline.prop=value1\nvalue2\r\nvalue3",
      null /* lazily initialized -XX:Flags */};
    private static File manifestFile = null;

    /**
     * Create a file containing VM_FLAG in the working directory
     */
    public static File getVmFlagsFile() throws IOException {
        if (vmFlagsFile == null) {
            vmFlagsFile = new File("vmflags");
            try (BufferedWriter output = new BufferedWriter(new FileWriter(vmFlagsFile))) {
                output.write(VM_FLAG);
            }
            vmFlagsFile.deleteOnExit();
        }
        return vmFlagsFile;
    }

    /**
     * Return a list of VM arguments
     */
    public static String[] getVmArgs() throws IOException {
        if (testVmArgs[testVmArgs.length - 1] == null) {
            testVmArgs[testVmArgs.length - 1] = "-XX:Flags=" + getVmFlagsFile().getAbsolutePath();
        }
        return testVmArgs;
    }

    /**
     * Start jps utility without any arguments
     */
    public static OutputAnalyzer jps() throws Exception {
        return jps(null, null);
    }

    /**
     * Start jps utility with tool arguments
     */
    public static OutputAnalyzer jps(String... toolArgs) throws Exception {
        return jps(null, Arrays.asList(toolArgs));
    }

    /**
     * Start jps utility with VM args and tool arguments
     */
    public static OutputAnalyzer jps(List<String> vmArgs, List<String> toolArgs) throws Exception {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jps");
        launcher.addVMArgs(Utils.getFilteredTestJavaOpts("-XX:+UsePerfData"));
        launcher.addVMArg("-XX:+UsePerfData");
        if (vmArgs != null) {
            for (String vmArg : vmArgs) {
                launcher.addVMArg(vmArg);
            }
        }
        if (toolArgs != null) {
            for (String toolArg : toolArgs) {
                launcher.addToolArg(toolArg);
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder(launcher.getCommand());
        System.out.println(Arrays.toString(processBuilder.command().toArray()).replace(",", ""));
        OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);
        System.out.println(output.getOutput());

        return output;
    }

    /**
     * Verify jps stdout contains only pids and programs' name information.
     * jps stderr may contain VM warning messages which will be ignored.
     *
     * The output can look like:
     * 35536 Jps
     * 35417 Main
     * 31103 org.eclipse.equinox.launcher_1.3.0.v20120522-1813.jar
     */
    public static void verifyJpsOutput(OutputAnalyzer output, String regex) {
        output.shouldHaveExitValue(0);
        output.stdoutShouldMatchByLine(regex);
        output.stderrShouldNotMatch("[E|e]xception");
        output.stderrShouldNotMatch("[E|e]rror");
    }

    /**
     * Compare jps output with a content in a file line by line
     */
    public static void verifyOutputAgainstFile(OutputAnalyzer output) throws IOException {
        String testSrc = System.getProperty("test.src", "?");
        Path path = Paths.get(testSrc, "usage.out");
        List<String> fileOutput = Files.readAllLines(path);
        List<String> outputAsLines = output.asLines();
        assertTrue(outputAsLines.containsAll(fileOutput),
                "The ouput should contain all content of " + path.toAbsolutePath());
    }

    public static void runJpsVariants(Long pid, String processName, String fullProcessName, String argument) throws Exception {
        System.out.printf("INFO: user.dir:  '%s''\n", System.getProperty("user.dir"));
        List<List<JpsHelper.JpsArg>> combinations = JpsHelper.JpsArg.generateCombinations();
        for (List<JpsHelper.JpsArg> combination : combinations) {
            OutputAnalyzer output = JpsHelper.jps(JpsHelper.JpsArg.asCmdArray(combination));
            output.shouldHaveExitValue(0);

            boolean isQuiet = false;
            boolean isFull = false;
            String pattern;
            for (JpsHelper.JpsArg jpsArg : combination) {
                switch (jpsArg) {
                case q:
                    // If '-q' is specified output should contain only a list of local VM identifiers:
                    // 30673
                    isQuiet = true;
                    JpsHelper.verifyJpsOutput(output, "^\\d+$");
                    output.shouldContain(Long.toString(pid));
                    break;
                case l:
                    // If '-l' is specified output should contain the full package name for the application's main class
                    // or the full path name to the application's JAR file:
                    // 30673 /tmp/jtreg/jtreg-workdir/scratch/LingeredAppForJps.jar ...
                    isFull = true;
                    pattern = "^" + pid + "\\s+" + replaceSpecialChars(fullProcessName) + ".*";
                    output.shouldMatch(pattern);
                    break;
                case m:
                    // If '-m' is specified output should contain the arguments passed to the main method:
                    // 30673 LingeredAppForJps lockfilename ...
                    pattern = "^" + pid + ".*" + replaceSpecialChars(argument) + ".*";
                    output.shouldMatch(pattern);
                    break;
                case v:
                    // If '-v' is specified output should contain VM arguments:
                    // 30673 LingeredAppForJps -Xmx512m -XX:+UseParallelGC -XX:Flags=/tmp/jtreg/jtreg-workdir/scratch/vmflags ...
                    for (String vmArg : JpsHelper.getVmArgs()) {
                        pattern = "^" + pid + ".*" + replaceSpecialChars(vmArg) + ".*";
                        output.shouldMatch(pattern);
                    }
                    break;
                case V:
                    // If '-V' is specified output should contain VM flags:
                    // 30673 LingeredAppForJps +DisableExplicitGC ...
                    pattern = "^" + pid + ".*" + replaceSpecialChars(JpsHelper.VM_FLAG) + ".*";
                    output.shouldMatch(pattern);
                    break;
                }

                if (isQuiet) {
                    break;
                }
            }

            if (!isQuiet) {
                // Verify output line by line.
                // Output should only contain lines with pids after the first line with pid.
                JpsHelper.verifyJpsOutput(output, "^\\d+\\s+.*");
                if (!isFull) {
                    pattern = "^" + pid + "\\s+" + replaceSpecialChars(processName);
                    if (combination.isEmpty()) {
                        // If no arguments are specified output should only contain
                        // pid and process name
                        pattern += "$";
                    } else {
                        pattern += ".*";
                    }
                    output.shouldMatch(pattern);
                }
            }
        }
    }

    private static String replaceSpecialChars(String str) {
        String tmp = str.replace("\\", "\\\\");
        tmp = tmp.replace("+", "\\+");
        tmp = tmp.replace(".", "\\.");
        tmp = tmp.replace("\n", "\\\\n");
        tmp = tmp.replace("\r", "\\\\r");
        return tmp;
    }
}
