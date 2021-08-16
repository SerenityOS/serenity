/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8133747 8218458
 * @summary Running with NMT detail should produce expected stack traces.
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @compile ../modules/CompilerUtils.java
 * @run driver CheckForProperDetailStackTrace
 */

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * We are checking for details that should be seen with NMT detail enabled.
 * In particular the stack traces from os::malloc call sites should have 4
 * (based on NMT detail stack depth setting) 'interesting' frames and skip
 * the higher-level allocation frames and frames specific to the NMT logic.
 * The actual stack trace is affected by the native compiler's inlining ability
 * and the type of build, so we need to check for a number of possible stacks.
 * This information does not change often enough that we are concerned about the
 * stability of this test - rather we prefer to detect changes in compiler behaviour
 * through this test and update it accordingly.
 */
public class CheckForProperDetailStackTrace {
    private static final String TEST_SRC = System.getProperty("test.src");
    private static final String TEST_CLASSES = System.getProperty("test.classes");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get(TEST_CLASSES, "mods");

    /* The stack trace we look for by default. Note that :: has been replaced by .*
       to make sure it matches even if the symbol is not unmangled.
    */
    private static String stackTraceDefault =
        ".*Hashtable.*new_entry.*\n" +
        ".*ModuleEntryTable.*new_entry.*\n" +
        ".*ModuleEntryTable.*locked_create_entry.*\n" +
        ".*Modules.*define_module.*\n";

    /* Alternate stacktrace that we check if the default fails, because
       new_entry may be inlined.
    */
    private static String stackTraceAlternate =
        ".*Hashtable.*new_entry.*\n" +
        ".*ModuleEntryTable.*locked_create_entry.*\n" +
        ".*Modules.*define_module.*\n" +
        ".*JVM_DefineModule.*\n";

    /* The stack trace we look for on AIX and Windows slowdebug builds.
       ALWAYSINLINE is only a hint and is ignored for AllocateHeap on the
       aforementioned platforms. When that happens allocate_new_entry is
       inlined instead.
    */
    private static String stackTraceAllocateHeap =
        ".*AllocateHeap.*\n" +
        ".*ModuleEntryTable.*new_entry.*\n" +
        ".*ModuleEntryTable.*locked_create_entry.*\n" +
        ".*Modules.*define_module.*\n";

    /* A symbol that should always be present in NMT detail output. */
    private static String expectedSymbol = "locked_create_entry";

    public static void main(String args[]) throws Exception {
        boolean compiled;
        // Compile module jdk.test declaration
        compiled = CompilerUtils.compile(
            SRC_DIR.resolve("jdk.test"),
            MODS_DIR.resolve("jdk.test"));
        if (!compiled) {
            throw new RuntimeException("Test failed to compile module jdk.test");
        }

        // If modules in the system image have been archived in CDS, they will not be
        // created again at run time. Explicitly use an external module to make sure
        // we have a runtime-defined ModuleEntry
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:NativeMemoryTracking=detail",
            "-XX:+PrintNMTStatistics",
            "-p", MODS_DIR.toString(),
            "-m", "jdk.test/test.Main");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        output.shouldHaveExitValue(0);

        // We should never see either of these frames because they are supposed to be skipped.
        output.shouldNotContain("NativeCallStack::NativeCallStack");
        output.shouldNotContain("os::get_native_stack");

        // AllocateHeap shouldn't be in the output because it is supposed to always be inlined.
        // We check for that here, but allow it for Aix and Windows slowdebug builds
        // because the compiler ends up not inlining AllocateHeap.
        Boolean okToHaveAllocateHeap = Platform.isSlowDebugBuild() &&
                                       (Platform.isAix() || Platform.isWindows());
        if (!okToHaveAllocateHeap) {
            output.shouldNotContain("AllocateHeap");
        }

        // See if we have any stack trace symbols in the output
        boolean hasSymbols = output.getStdout().contains(expectedSymbol) ||
                             output.getStderr().contains(expectedSymbol);
        if (!hasSymbols) {
            // It's ok for ARM not to have symbols, because it does not support NMT detail
            // when targeting thumb2. It's also ok for Windows not to have symbols, because
            // they are only available if the symbols file is included with the build.
            if (Platform.isWindows() || Platform.isARM()) {
                return; // we are done
            }
            output.reportDiagnosticSummary();
            throw new RuntimeException("Expected symbol missing from output: " + expectedSymbol);
        }

        // Make sure the expected NMT detail stack trace is found
        System.out.println("Looking for a stack matching:");
        if (okToHaveAllocateHeap) {
            System.out.print(stackTraceAllocateHeap);
            if (stackTraceMatches(stackTraceAllocateHeap, output)) {
                return;
            }
        } else {
            System.out.print(stackTraceDefault);
            if (!stackTraceMatches(stackTraceDefault, output)) {
                System.out.println("Looking for alternate stack matching:");
                System.out.print(stackTraceAlternate);
                if (stackTraceMatches(stackTraceAlternate, output)) {
                    return;
                }
            } else {
                return;
            }
        }
        // Failed to match so dump all the output
        output.reportDiagnosticSummary();
        throw new RuntimeException("Expected stack trace missing from output");
    }

    public static boolean stackTraceMatches(String stackTrace, OutputAnalyzer output) {
        Pattern p = Pattern.compile(stackTrace, Pattern.MULTILINE);
        Matcher stdoutMatcher = p.matcher(output.getStdout());
        Matcher stderrMatcher = p.matcher(output.getStderr());
        return (stdoutMatcher.find() || stderrMatcher.find());
    }
}
