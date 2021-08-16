/*
 * Copyright (c) 2021 SAP SE. All rights reserved.
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
 * @summary Test verifies that -XX:HeapDumpGzipLevel=0 works
 * @library /test/lib
 * @run driver/timeout=240 TestGZippedHeapDumpOnOutOfMemoryError run 0
 */

/*
 * @test
 * @summary Test verifies that -XX:HeapDumpGzipLevel=1 works
 * @library /test/lib
 * @run driver/timeout=240 TestGZippedHeapDumpOnOutOfMemoryError run 1
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.hprof.HprofParser;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.File;

public class TestGZippedHeapDumpOnOutOfMemoryError {

    static volatile Object[] oa;

    public static void main(String[] args) throws Exception {
        if (args.length == 2) {
            test(Integer.parseInt(args[1]));
            return;
        }

        try {
            oa = new Object[Integer.MAX_VALUE];
            throw new Error("OOME not triggered");
        } catch (OutOfMemoryError err) {
            // Ignore
        }
    }

    static void test(int level) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:HeapDumpGzipLevel=" + level,
            "-Xmx128M",
            TestGZippedHeapDumpOnOutOfMemoryError.class.getName());

        Process proc = pb.start();
        String heapdumpFilename = "java_pid" + proc.pid() + ".hprof" + (level > 0 ? ".gz" : "");
        OutputAnalyzer output = new OutputAnalyzer(proc);
        output.stdoutShouldNotBeEmpty();
        output.shouldContain("Dumping heap to " + heapdumpFilename);
        File dump = new File(heapdumpFilename);
        Asserts.assertTrue(dump.exists() && dump.isFile(),
                "Could not find dump file " + dump.getAbsolutePath());

        HprofParser.parse(new File(heapdumpFilename));
        System.out.println("PASSED");
    }

}
