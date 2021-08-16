/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Scanner;

import jdk.test.lib.Asserts;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.SA.SATestUtils;

/**
 * @test
 * @bug 6313383
 * @requires vm.hasSA
 * @summary Regression test for hprof export issue due to large heaps (>2G)
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management/sun.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @build JMapHProfLargeHeapProc
 * @run driver JMapHProfLargeHeapTest
 */

public class JMapHProfLargeHeapTest {
    private static final String HEAP_DUMP_FILE_NAME = "heap.bin";
    private static final String HPROF_HEADER_1_0_2 = "JAVA PROFILE 1.0.2";
    private static final long M = 1024L;
    private static final long G = 1024L * M;

    public static void main(String[] args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.

        // All heap dumps should create 1.0.2 file format
        testHProfFileFormat("-Xmx1g", 22 * M, HPROF_HEADER_1_0_2);

        /**
         * This test was deliberately commented out since the test system lacks
         * support to handle the requirements for this kind of heap size in a
         * good way. If or when it becomes possible to run this kind of tests in
         * the test environment the test should be enabled again.
         * */
        // Large heap 2,2 gigabytes, should create 1.0.2 file format
        // testHProfFileFormat("-Xmx4g", 2 * G + 2 * M, HPROF_HEADER_1_0_2);
    }

    private static void testHProfFileFormat(String vmArgs, long heapSize,
            String expectedFormat) throws Exception, IOException,
            InterruptedException, FileNotFoundException {
        ProcessBuilder procBuilder = ProcessTools.createJavaProcessBuilder(
                "--add-exports=java.management/sun.management=ALL-UNNAMED", vmArgs, "JMapHProfLargeHeapProc", String.valueOf(heapSize));
        procBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process largeHeapProc = procBuilder.start();

        try (Scanner largeHeapScanner = new Scanner(
                largeHeapProc.getInputStream());) {
            String pidstring = null;
            if (!largeHeapScanner.hasNext()) {
                throw new RuntimeException ("Test failed: could not open largeHeapScanner.");
            }
            while ((pidstring = largeHeapScanner.findInLine("PID\\[[0-9].*\\]")) == null) {
                Thread.sleep(500);
            }
            int pid = Integer.parseInt(pidstring.substring(4,
                    pidstring.length() - 1));
            System.out.println("Extracted pid: " + pid);

            JDKToolLauncher jMapLauncher = JDKToolLauncher
                    .createUsingTestJDK("jhsdb");
            jMapLauncher.addVMArgs(Utils.getTestJavaOpts());
            jMapLauncher.addToolArg("jmap");
            jMapLauncher.addToolArg("--binaryheap");
            jMapLauncher.addToolArg("--pid");
            jMapLauncher.addToolArg(String.valueOf(pid));

            ProcessBuilder jMapProcessBuilder = SATestUtils.createProcessBuilder(jMapLauncher);
            System.out.println("jmap command: "
                    + Arrays.toString(jMapLauncher.getCommand()));

            Process jMapProcess = jMapProcessBuilder.start();
            OutputAnalyzer analyzer = new OutputAnalyzer(jMapProcess);
            analyzer.shouldHaveExitValue(0);
            analyzer.shouldContain(HEAP_DUMP_FILE_NAME);

            largeHeapProc.getOutputStream().write('\n');

            File dumpFile = new File(HEAP_DUMP_FILE_NAME);
            Asserts.assertTrue(dumpFile.exists(), "Heap dump file not found.");

            try (Reader reader = new BufferedReader(new FileReader(dumpFile))) {
                CharBuffer buf = CharBuffer.allocate(expectedFormat.length());
                reader.read(buf);
                buf.clear();
                Asserts.assertEQ(buf.toString(), expectedFormat,
                        "Wrong file format. Expected '" + expectedFormat
                                + "', but found '" + buf.toString() + "'");
            }

            System.out.println("Success!");

        } finally {
            largeHeapProc.destroyForcibly();
        }
    }
}
