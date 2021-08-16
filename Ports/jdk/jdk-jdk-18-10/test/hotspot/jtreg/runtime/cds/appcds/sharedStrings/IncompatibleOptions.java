/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

// NOTE: the test takes a long time for each VM option combination, so we split
// it into 3 @test parts, so that they can be executed in parallel. If you make a
// change, please ensure all @test blocks are in sync.


/*
 * @test
 * @summary Test options that are incompatible with use of shared strings
 *          Also test mismatch in oops encoding between dump time and run time
 * @requires vm.cds.archived.java.heap
 * @comment This test explicitly chooses the type of GC to be used by sub-processes. It may conflict with the GC type set
 * via the -vmoptions command line option of JTREG. vm.gc==null will help the test case to discard the explicitly passed
 * vm options.
 * @requires (vm.gc=="null")
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @build HelloString
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. IncompatibleOptions 0
 */


/*
 * @test
 * @requires vm.cds.archived.java.heap
 * @requires (vm.gc=="null")
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @build HelloString
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. IncompatibleOptions 1
 */

/*
 * @test
 * @requires vm.cds.archived.java.heap
 * @requires (vm.gc=="null")
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @build HelloString
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. IncompatibleOptions 2
 */


import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;

import sun.hotspot.code.Compiler;
import sun.hotspot.gc.GC;

public class IncompatibleOptions {
    static final String COOPS_DUMP_WARNING =
        "Cannot dump shared archive when UseCompressedOops or UseCompressedClassPointers is off";
    static final String GC_WARNING =
        "Archived java heap is not supported";
    static final String OBJ_ALIGNMENT_MISMATCH =
        "The shared archive file's ObjectAlignmentInBytes of .* does not equal the current ObjectAlignmentInBytes of";
    static final String COMPACT_STRING_MISMATCH =
        "The shared archive file's CompactStrings setting .* does not equal the current CompactStrings setting";
    static final String COMPRESSED_OOPS_NOT_CONSISTENT =
        "The saved state of UseCompressedOops and UseCompressedClassPointers is different from runtime, CDS will be disabled.";
    static String appJar;
    static String[] vmOptionsPrefix = {};

    public static void main(String[] args) throws Exception {
        String[] noargs = {};
        SharedStringsUtils.run(Integer.parseInt(args[0]), 3, noargs, IncompatibleOptions::test);
        // Add a new @test block if you get an assert ----^ about this number. See
        // SharedStringsUtils.java for details.
    }

    public static void test(String[] args_ignored) throws Exception {
        vmOptionsPrefix = SharedStringsUtils.getChildVMOptionsPrefix();
        appJar = JarBuilder.build("IncompatibleOptions", "HelloString");

        // Uncompressed OOPs
        testDump(1, "-XX:+UseG1GC", "-XX:-UseCompressedOops", null, false);
        if (GC.Z.isSupported()) {
            testDump(1, "-XX:+UseZGC", "-XX:-UseCompressedOops", null, false);
        }

        // incompatible GCs
        testDump(2, "-XX:+UseParallelGC", "", GC_WARNING, false);
        testDump(3, "-XX:+UseSerialGC", "", GC_WARNING, false);

        // Explicitly archive with compressed oops, run without.
        testDump(5, "-XX:+UseG1GC", "-XX:+UseCompressedOops", null, false);
        testExec(5, "-XX:+UseG1GC", "-XX:-UseCompressedOops",
                 COMPRESSED_OOPS_NOT_CONSISTENT, true);

        // NOTE: No warning is displayed, by design
        // Still run, to ensure no crash or exception
        testExec(6, "-XX:+UseParallelGC", "", "", false);
        testExec(7, "-XX:+UseSerialGC", "", "", false);

        // Test various oops encodings, by varying ObjectAlignmentInBytes and heap sizes
        testDump(9, "-XX:+UseG1GC", "-XX:ObjectAlignmentInBytes=8", null, false);
        testExec(9, "-XX:+UseG1GC", "-XX:ObjectAlignmentInBytes=16",
                 OBJ_ALIGNMENT_MISMATCH, true);

        // Implicitly archive with compressed oops, run without.
        // Max heap size for compressed oops is around 31G.
        // UseCompressedOops is turned on by default when heap
        // size is under 31G, but will be turned off when heap
        // size is greater than that.
        testDump(10, "-XX:+UseG1GC", "-Xmx1g", null, false);
        testExec(10, "-XX:+UseG1GC", "-Xmx32g", null, true);
        // Explicitly archive without compressed oops and run with.
        testDump(11, "-XX:+UseG1GC", "-XX:-UseCompressedOops", null, false);
        testExec(11, "-XX:+UseG1GC", "-XX:+UseCompressedOops", null, true);
        // Implicitly archive without compressed oops and run with.
        testDump(12, "-XX:+UseG1GC", "-Xmx32G", null, false);
        testExec(12, "-XX:+UseG1GC", "-Xmx1G", null, true);
        // CompactStrings must match between dump time and run time
        testDump(13, "-XX:+UseG1GC", "-XX:-CompactStrings", null, false);
        testExec(13, "-XX:+UseG1GC", "-XX:+CompactStrings",
                 COMPACT_STRING_MISMATCH, true);
        testDump(14, "-XX:+UseG1GC", "-XX:+CompactStrings", null, false);
        testExec(14, "-XX:+UseG1GC", "-XX:-CompactStrings",
                 COMPACT_STRING_MISMATCH, true);
    }

    static void testDump(int testCaseNr, String collectorOption, String extraOption,
        String expectedWarning, boolean expectedToFail) throws Exception {

        System.out.println("Testcase: " + testCaseNr);
        OutputAnalyzer output = TestCommon.dump(appJar, TestCommon.list("Hello"),
            TestCommon.concat(vmOptionsPrefix,
                "-XX:+UseCompressedOops",
                collectorOption,
                "-XX:SharedArchiveConfigFile=" + TestCommon.getSourceFile("SharedStringsBasic.txt"),
                "-Xlog:cds,cds+hashtables",
                extraOption));

        if (expectedWarning != null) {
            output.shouldContain(expectedWarning);
        }

        if (expectedToFail) {
            Asserts.assertNE(output.getExitValue(), 0,
            "JVM is expected to fail, but did not");
        }
    }

    static void testExec(int testCaseNr, String collectorOption, String extraOption,
        String expectedWarning, boolean expectedToFail) throws Exception {

        OutputAnalyzer output;
        System.out.println("Testcase: " + testCaseNr);

        // needed, otherwise system considers empty extra option as a
        // main class param, and fails with "Could not find or load main class"
        if (!extraOption.isEmpty()) {
            output = TestCommon.exec(appJar,
                TestCommon.concat(vmOptionsPrefix,
                    "-XX:+UseCompressedOops",
                    collectorOption, "-Xlog:cds", extraOption, "HelloString"));
        } else {
            output = TestCommon.exec(appJar,
                TestCommon.concat(vmOptionsPrefix,
                    "-XX:+UseCompressedOops",
                    collectorOption, "-Xlog:cds", "HelloString"));
        }

        if (expectedWarning != null) {
            output.shouldMatch(expectedWarning);
        }

        if (expectedToFail) {
            Asserts.assertNE(output.getExitValue(), 0);
        } else {
            SharedStringsUtils.checkExec(output);
        }
    }
}
