/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.SA.SATestUtils;
import jdk.test.lib.Utils;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @key randomness
 * @bug 8208091
 * @requires (os.family == "linux") & (vm.hasSA)
 * @library /test/lib
 * @run driver TestJhsdbJstackMixed
 */
public class TestJhsdbJstackMixed {

    private static final int MAX_ITERATIONS = 20;
    private static final String NATIVE_FUNCTION_NAME = "fib";
    private static final String LINE_MATCHER_STR = ".*" + NATIVE_FUNCTION_NAME
            + ".*";
    private static final Pattern LINE_PATTERN = Pattern
            .compile(LINE_MATCHER_STR);
    private static final String HEX_STR_PATTERN = "0x([a-fA-F0-9]+)";
    private static final String FIB_SPLIT_PATTERN = NATIVE_FUNCTION_NAME
            + "\\s+\\+";
    private static final Pattern HEX_PATTERN = Pattern.compile(HEX_STR_PATTERN);
    private static final int ADDRESS_ALIGNMENT_X86 = 4;

    /*
     * UnmappedAddressException will be thrown iff:
     * - The JNI code is being compiled with -fomit-frame-pointer AND
     * - The JNI code is currently executing at address A = pc() + offset
     *   where A % ADDRESS_SIZE == 0.
     *
     * In the below example we have: pc() == f6401546, offset == 56,
     * ADDRESS_SIZE == 4. Thus, A == F640159C which satisfies this condition.
     *
     * "NoFramePointerJNIFib" #11 prio=5 tid=0xa357bc00 nid=0x6de9 runnable [0xa365b000]
     *    java.lang.Thread.State: RUNNABLE
     *    JavaThread state: _thread_in_native
     * 0xf6401546 fib + 0x56
     */
    private static boolean isFibAndAlignedAddress(List<String> lines) {
        List<String> fibLines = findFibLines(lines);
        System.out.println("DEBUG: " + fibLines);
        // we're only interested in the first matched line.
        if (fibLines.size() >= 1) {
            String line = fibLines.get(0);
            return isMatchLine(line);
        }
        return false;
    }

    private static boolean isMatchLine(String line) {
        String[] tokens = line.split(FIB_SPLIT_PATTERN);
        if (tokens.length != 2) {
            return false; // NOT exactly two tokens, ignore.
        }
        String pcRaw = tokens[0].trim();
        String offsetRaw = tokens[1].trim();
        Matcher matcher = HEX_PATTERN.matcher(pcRaw);
        long pcVal = 3;
        boolean pcMatched = matcher.matches();
        if (pcMatched) {
            String pc = matcher.group(1);
            pcVal = Long.parseUnsignedLong(pc, 16);
        }
        matcher = HEX_PATTERN.matcher(offsetRaw);
        long offsetVal = 0;
        boolean offsetMatched = matcher.matches();
        if (offsetMatched) {
            String offset = matcher.group(1);
            offsetVal = Long.parseUnsignedLong(offset, 16);
        }
        if (offsetMatched && pcMatched
                && (pcVal + offsetVal) % ADDRESS_ALIGNMENT_X86 == 0) {
            return true;
        }
        return false;
    }

    private static List<String> findFibLines(List<String> lines) {
        boolean startReached = false;
        boolean endReached = false;
        List<String> interestingLines = new ArrayList<>();
        for (String line : lines) {
            if (line.contains(LingeredAppWithNativeMethod.THREAD_NAME)) {
                startReached = true;
            }
            if (startReached && line.contains("-------")) {
                endReached = true;
            }
            if (startReached && !endReached) {
                Matcher matcher = LINE_PATTERN.matcher(line);
                if (matcher.matches()) {
                    interestingLines.add(line);
                }
            }
        }
        return interestingLines;
    }

    private static void runJstackMixedInLoop(LingeredApp app) throws Exception {
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            JDKToolLauncher launcher = JDKToolLauncher
                    .createUsingTestJDK("jhsdb");
            launcher.addVMArgs(Utils.getTestJavaOpts());
            launcher.addToolArg("jstack");
            launcher.addToolArg("--mixed");
            launcher.addToolArg("--pid");
            launcher.addToolArg(Long.toString(app.getPid()));

            ProcessBuilder pb = SATestUtils.createProcessBuilder(launcher);
            Process jhsdb = pb.start();
            OutputAnalyzer out = new OutputAnalyzer(jhsdb);

            jhsdb.waitFor();

            System.out.println(out.getStdout());
            System.err.println(out.getStderr());

            out.shouldContain(LingeredAppWithNativeMethod.THREAD_NAME);
            if (isFibAndAlignedAddress(out.asLines())) {
                System.out.println("DEBUG: Test triggered interesting condition.");
                out.shouldNotContain("sun.jvm.hotspot.debugger.UnmappedAddressException:");
                System.out.println("DEBUG: Test PASSED.");
                return; // If we've reached here, all is well.
            }
            System.out.println("DEBUG: Iteration: " + (i + 1)
                                 + " - Test didn't trigger interesting condition.");
            out.shouldNotContain("sun.jvm.hotspot.debugger.UnmappedAddressException:");
        }
        System.out.println("DEBUG: Test didn't trigger interesting condition " +
                             "but no UnmappedAddressException was thrown. PASS!");
    }

    public static void main(String... args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        LingeredApp app = null;

        try {
            // Needed for LingeredApp to be able to resolve native library.
            String libPath = System.getProperty("java.library.path");
            String[] vmArgs = (libPath != null)
                ? Utils.prependTestJavaOpts("-Djava.library.path=" + libPath)
                : Utils.getTestJavaOpts();

            app = new LingeredAppWithNativeMethod();
            LingeredApp.startAppExactJvmOpts(app, vmArgs);
            System.out.println("Started LingeredApp with pid " + app.getPid());
            runJstackMixedInLoop(app);
            System.out.println("Test Completed");
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            LingeredApp.stopApp(app);
        }
    }
}
