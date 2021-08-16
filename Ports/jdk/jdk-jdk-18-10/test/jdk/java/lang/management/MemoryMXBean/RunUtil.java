/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Utility class for launching a test in a separate JVM.
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

public class RunUtil {

    // Used to mark that the test has passed successfully.
    public static final String successMessage = "Test passed.";

    public static void runTestClearGcOpts(String main, String... testOpts) throws Throwable {
        runTest(main, true, testOpts);
    }

    public static void runTestKeepGcOpts(String main, String... testOpts) throws Throwable {
        runTest(main, false, testOpts);
    }

    /**
     * Runs a test in a separate JVM.
     * command line like:
     * {test_jdk}/bin/java {defaultopts} -cp {test.class.path} {testopts} main
     *
     * {defaultopts} are the default java options set by the framework.
     * Default GC options in {defaultopts} may be removed.
     * This is used when the test specifies its own GC options.
     *
     * @param main Name of the main class.
     * @param clearGcOpts true if the default GC options should be removed.
     * @param testOpts java options specified by the test.
     */
    private static void runTest(String main, boolean clearGcOpts, String... testOpts)
                throws Throwable {
        List<String> opts = new ArrayList<>();
        opts.add(JDKToolFinder.getJDKTool("java"));
        opts.addAll(Arrays.asList(Utils.getTestJavaOpts()));
        opts.add("-cp");
        opts.add(System.getProperty("test.class.path", "test.class.path"));
        opts.add("-Xlog:gc*=debug");

        if (clearGcOpts) {
            opts = Utils.removeGcOpts(opts);
        }
        opts.addAll(Arrays.asList(testOpts));
        opts.add(main);

        OutputAnalyzer output = ProcessTools.executeProcess(opts.toArray(new String[0]));
        output.shouldHaveExitValue(0);
        if (output.getStdout().indexOf(successMessage) < 0) {
            throw new Exception("output missing '" + successMessage + "'");
        }
    }

}
