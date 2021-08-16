/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8140520
 * @summary Setting small CompilerThreadStackSize, ThreadStackSize, and
 * VMThreadStackSize values should result in an error message that shows
 * the minimum stack size value for each thread type.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver TooSmallStackSize
 */

/*
 * The primary purpose of this test is to make sure we can run with a
 * stack smaller than the minimum without crashing. Also this test will
 * determine the minimum allowed stack size for the platform (as
 * provided by the JVM error message when a very small stack is used),
 * and then verify that the JVM can be launched with that stack size
 * without a crash or any error messages.
 *
 * Note: The '-Xss<size>' and '-XX:ThreadStackSize=<k-bytes>' options
 * both control Java thread stack size. This repo's version of the test
 * exercises the '-XX:ThreadStackSize' VM option. The jdk repo's version
 * of the test exercises the '-Xss' option.
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TooSmallStackSize {
    /* for debugging. Normally false. */
    static final boolean verbose = false;
    static final String CompilerThreadStackSizeString = "CompilerThreadStackSize";
    static final String ThreadStackSizeString = "Java thread stack size";
    static final String VMThreadStackSizeString = "VMThreadStackSize";

    /*
     * Returns the minimum stack size this platform will allowed based on the
     * contents of the error message the JVM outputs when too small of a
     * stack size was used.
     *
     * The testOutput argument must contain the result of having already run
     * the JVM with too small of a stack size.
     */
    static String getMinStackAllowed(String testOutput) {
        /*
         * The JVM output will contain in one of the lines:
         *   "The CompilerThreadStackSize specified is too small. Specify at least 100k"
         *   "The Java thread stack size specified is too small. Specify at least 100k"
         *   "The VMThreadStackSize specified is too small. Specify at least 100k"
         * Although the actual size will vary. We need to extract this size
         * string from the output and return it.
         */
        String matchStr = "Specify at least ";
        int match_idx = testOutput.indexOf(matchStr);
        if (match_idx >= 0) {
            int size_start_idx = match_idx + matchStr.length();
            int k_start_idx = testOutput.indexOf("k", size_start_idx);
            // don't include the 'k'; the caller will have to
            // add it back as needed.
            return testOutput.substring(size_start_idx, k_start_idx);
        }

        System.out.println("Expect='" + matchStr + "'");
        System.out.println("Actual: " + testOutput);
        System.out.println("FAILED: Could not get the stack size from the output");
        throw new RuntimeException("test fails");
    }

    /*
     * Run the JVM with the specified stack size.
     *
     * Returns the minimum allowed stack size gleaned from the error message,
     * if there is an error message. Otherwise returns the stack size passed in.
     */
    static String checkStack(String stackOption, String optionMesg, String stackSize) throws Exception {
        String min_stack_allowed;

        System.out.println("*** Testing " + stackOption + stackSize);

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            stackOption + stackSize,
            // Uncomment the following to get log output
            // that shows actual thread creation sizes.
            // "-Xlog:os+thread",
            "-version");

        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        if (verbose) {
            System.out.println("stdout: " + output.getStdout());
        }

        if (output.getExitValue() == 0) {
            // checkMinStackAllowed() is called with stackSize values
            // that should be the minimum that works. This method,
            // checkStack(), is called with stackSize values that
            // should be too small and result in error messages.
            // However, some platforms fix up a stackSize value that is
            // too small into something that works so we have to allow
            // for checkStack() calls that work.
            System.out.println("PASSED: got exit_code == 0 with " + stackOption + stackSize);
            min_stack_allowed = stackSize;
        } else {
            String expect = "The " + optionMesg + " specified is too small";
            if (verbose) {
                System.out.println("Expect='" + expect + "'");
            }
            output.shouldContain(expect);
            min_stack_allowed = getMinStackAllowed(output.getStdout());

            System.out.println("PASSED: got expected error message with " + stackOption + stackSize);
        }

        return min_stack_allowed;
    }

    /*
     * Run the JVM with the minimum allowed stack size. This should always succeed.
     */
    static void checkMinStackAllowed(String stackOption, String optionMesg, String stackSize) throws Exception {
        System.out.println("*** Testing " + stackOption + stackSize);

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            stackOption + stackSize,
            // Uncomment the following to get log output
            // that shows actual thread creation sizes.
            // "-Xlog:os+thread",
            "-version");

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);

        System.out.println("PASSED: VM launched with " + stackOption + stackSize);
    }

    public static void main(String... args) throws Exception {
        /*
         * The result of a 16k stack size should be a quick exit with a complaint
         * that the stack size is too small. However, for some win32 builds, the
         * stack is always at least 64k, and this also sometimes is the minimum
         * allowed size, so we won't see an error in this case.
         *
         * This test case will also produce a crash on some platforms if the fix
         * for 6762191 is not yet in place.
         */
        checkStack("-XX:ThreadStackSize=", ThreadStackSizeString, "16");

        /*
         * Try with a 64k stack size, which is the size that the launcher will
         * set to if you try setting to anything smaller. This should produce the same
         * result as setting to 16k if the fix for 6762191 is in place.
         */
        String min_stack_allowed = checkStack("-XX:ThreadStackSize=", ThreadStackSizeString, "64");

        /*
         * Try again with a the minimum stack size that was given in the error message
         */
        checkMinStackAllowed("-XX:ThreadStackSize=", ThreadStackSizeString, min_stack_allowed);

        /*
         * Now try with a stack size that is not page aligned.
         */
        checkMinStackAllowed("-XX:ThreadStackSize=", ThreadStackSizeString, "513");

        /*
         * Try with 0k which indicates that the default thread stack size from JVM will be used.
         */
        checkMinStackAllowed("-XX:ThreadStackSize=", ThreadStackSizeString, "0");

        /*
         * Now redo the same tests with the compiler thread stack size:
         */
        checkStack("-XX:CompilerThreadStackSize=", CompilerThreadStackSizeString, "16");
        min_stack_allowed = checkStack("-XX:CompilerThreadStackSize=", CompilerThreadStackSizeString, "64");
        checkMinStackAllowed("-XX:CompilerThreadStackSize=", CompilerThreadStackSizeString, min_stack_allowed);
        checkMinStackAllowed("-XX:CompilerThreadStackSize=", CompilerThreadStackSizeString, "513");
        checkMinStackAllowed("-XX:CompilerThreadStackSize=", CompilerThreadStackSizeString, "0");

        /*
         * Now redo the same tests with the VM thread stack size:
         */
        checkStack("-XX:VMThreadStackSize=", VMThreadStackSizeString, "16");
        min_stack_allowed = checkStack("-XX:VMThreadStackSize=", VMThreadStackSizeString, "64");
        checkMinStackAllowed("-XX:VMThreadStackSize=", VMThreadStackSizeString, min_stack_allowed);
        checkMinStackAllowed("-XX:VMThreadStackSize=", VMThreadStackSizeString, "513");
        checkMinStackAllowed("-XX:VMThreadStackSize=", VMThreadStackSizeString, "0");
    }
}
