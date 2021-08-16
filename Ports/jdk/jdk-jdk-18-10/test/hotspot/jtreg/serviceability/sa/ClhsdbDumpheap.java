/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import static jdk.test.lib.Asserts.assertTrue;
import static jdk.test.lib.Asserts.assertFalse;
import jdk.test.lib.hprof.HprofParser;
import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.hprof.parser.HprofReader;
import jtreg.SkippedException;

/**
 * @test
 * @bug 8240989
 * @summary Test clhsdb dumpheap command
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm/timeout=240 ClhsdbDumpheap
 */

public class ClhsdbDumpheap {
    // The default heap dump file name defined in JDK.
    private static final String HEAP_DUMP_FILENAME_DEFAULT = "heap.bin";
    private static final String HEAP_DUMP_GZIPED_FILENAME_DEFAULT = "heap.bin.gz";

    public static void printStackTraces(String file) {
        try {
            System.out.println("HprofReader.getStack() output:");
            String output = HprofReader.getStack(file, 0);
            if (!output.contains("LingeredApp.steadyState")) {
                throw new RuntimeException("'LingeredApp.steadyState' missing from stdout/stderr");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        }
    }

    private static void verifyDumpFile(File dump) throws Exception {
        assertTrue(dump.exists() && dump.isFile(), "Could not create dump file " + dump.getAbsolutePath());
        printStackTraces(dump.getAbsolutePath());
    }

    private static class SubTest {
        private String cmd;
        private String fileName;
        private String expectedOutput;
        boolean compression;
        boolean needVerify;

        public SubTest(String comm, String fName, boolean isComp, boolean verify, String expected) {
            cmd = comm;
            fileName = fName;
            expectedOutput = expected;
            compression = isComp;
            needVerify = verify;
        }

        public String getCmd() { return cmd; }
        public String getFileName() { return fileName; }
        public String getExpectedOutput() { return expectedOutput; }
        public boolean isCompression() { return compression; }
        public boolean needVerify() { return needVerify; }
    }

    private static void runTest(long appPid, SubTest subtest) throws Exception {
        ClhsdbLauncher test = new ClhsdbLauncher();
        String fileName = subtest.getFileName();
        String cmd = subtest.getCmd();
        String expectedOutput = subtest.getExpectedOutput();
        boolean compression = subtest.isCompression();
        /* The expected generated file, used to distinguish with fileName in case fileName is blank or null */
        String expectedFileName = fileName;
        if (fileName == null || fileName.length() == 0) {
            if (!compression) {
                expectedFileName = HEAP_DUMP_FILENAME_DEFAULT;
            } else {
                expectedFileName = HEAP_DUMP_GZIPED_FILENAME_DEFAULT;
            }
        }
        assertTrue (expectedFileName != null && expectedFileName.length() > 0,
                "Expected generated file name must have value");
        File file = new File(expectedFileName);
        if (file.exists()) {
            file.delete();
        }
        String command = cmd + fileName;
        List<String> cmds = List.of(command);
        Map<String, List<String>> expStrMap = new HashMap<>();
        expStrMap.put(command, List.of(expectedOutput));
        test.run(appPid, cmds, expStrMap, null);
        if (subtest.needVerify()) {
            verifyDumpFile(file);
        }
        file.delete();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ClhsdbDumpheap test");

        LingeredApp theApp = null;
        try {
            // Use file name different with JDK's default value "heap.bin".
            String heapDumpFileName = "heapdump.bin";
            String heapDumpFileNameGz = "heapdump.bin.gz";

            theApp = new LingeredApp();
            LingeredApp.startApp(theApp);
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            SubTest[] subtests = new SubTest[] {
                    new SubTest("dumpheap ", heapDumpFileName, false/*compression*/, true,/*verify*/
                            "heap written to " + heapDumpFileName),
                    new SubTest("dumpheap gz=1 ", heapDumpFileNameGz, true, true,
                            "heap written to " + heapDumpFileNameGz),
                    new SubTest("dumpheap gz=9 ", heapDumpFileNameGz, true, true,
                            "heap written to " + heapDumpFileNameGz),
                    new SubTest("dumpheap gz=0 ", heapDumpFileNameGz, true, false,
                            "Usage: dumpheap \\[gz=<1-9>\\] \\[filename\\]"),
                    new SubTest("dumpheap gz=100 ", heapDumpFileNameGz, true, false,
                            "Usage: dumpheap \\[gz=<1-9>\\] \\[filename\\]"),
                    new SubTest("dumpheap gz= ", heapDumpFileNameGz, true, false,
                            "Usage: dumpheap \\[gz=<1-9>\\] \\[filename\\]"),
                    new SubTest("dumpheap gz ", heapDumpFileNameGz, true, false,
                            "Usage: dumpheap \\[gz=<1-9>\\] \\[filename\\]"),
                    new SubTest("dumpheap", "", false, true,
                            "heap written to " + HEAP_DUMP_FILENAME_DEFAULT),
                    new SubTest("dumpheap gz=1", "", true, true,
                            "heap written to " + HEAP_DUMP_GZIPED_FILENAME_DEFAULT),
                    new SubTest("dumpheap gz=9", "", true, true,
                            "heap written to " + HEAP_DUMP_GZIPED_FILENAME_DEFAULT),
                    new SubTest("dumpheap gz=0", "", true, false,
                            "Usage: dumpheap \\[gz=<1-9>\\] \\[filename\\]"),
                    new SubTest("dumpheap gz=100", "", true, false,
                            "Usage: dumpheap \\[gz=<1-9>\\] \\[filename\\]"),
                    // Command "dumpheap gz=".
                    new SubTest("dumpheap ", "gz=", true, false,
                            "Usage: dumpheap \\[gz=<1-9>\\] \\[filename\\]"),
                    // Command "dumpheap gz".
                    new SubTest("dumpheap ", "gz", false, true, "heap written to gz"),
                    // Command "dump heap gz=1 gz=2".
                    new SubTest("dumpheap gz=1", "gz=2", true, false,
                            "Usage: dumpheap \\[gz=<1-9>\\] \\[filename\\]")
            };
            // Run subtests
            for (int i = 0; i < subtests.length;i++) {
                runTest(theApp.getPid(), subtests[i]);
            }
        } catch (SkippedException se) {
            throw se;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
        }
        System.out.println("Test PASSED");
    }
}
