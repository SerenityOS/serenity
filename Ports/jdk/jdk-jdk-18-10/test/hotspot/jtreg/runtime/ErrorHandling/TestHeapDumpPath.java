/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestHeapDumpPath
 * @summary Test verifies that -XX:HeapDumpPath= supports directory as a parameter.
 * @library /test/lib
 * @run driver TestHeapDumpPath
 */

import jdk.test.lib.Asserts;
import jdk.test.lib.hprof.HprofParser;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.File;

public class TestHeapDumpPath {

    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            try {
                Object[] oa = new Object[Integer.MAX_VALUE];
                throw new Error("OOME not triggered");
            } catch (OutOfMemoryError err) {
                return;
            }
        }

        testHeapDumpPath();
    }
    static void testHeapDumpPath() throws Exception {
        String heapdumpPath = "dumps";
        File dumpDirectory = new File(heapdumpPath);
        dumpDirectory.mkdir();
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:+HeapDumpOnOutOfMemoryError",
                "-Xmx64m", "-XX:HeapDumpPath=" + heapdumpPath, TestHeapDumpPath.class.getName(), "OOME");

        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.stdoutShouldNotBeEmpty();
        output.shouldContain("Dumping heap");

        Asserts.assertFalse(dumpDirectory.listFiles().length == 0,
                "There is no dump files found in " + dumpDirectory );

        Asserts.assertTrue(dumpDirectory.listFiles().length == 1,
                "There are unexpected files in " + dumpDirectory
                        + ": " + String.join(",", dumpDirectory.list()) +".");

        File dump = dumpDirectory.listFiles()[0];
        Asserts.assertTrue(dump.exists() && dump.isFile(),
                "Could not find dump file " + dump.getAbsolutePath());

        HprofParser.parse(dump);
        System.out.println("PASSED");
    }

}
