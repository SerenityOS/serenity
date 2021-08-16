/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @summary Write a lots of shared strings.
 * @requires vm.cds.archived.java.heap
 * @library /test/hotspot/jtreg/runtime/cds/appcds /test/lib
 * @build HelloString
 * @run driver/timeout=500 SharedStringsStress
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class SharedStringsStress {
    static String sharedArchiveConfigFile = CDSTestUtils.getOutputDir() + File.separator + "SharedStringsStress_gen.txt";

    public static void main(String[] args) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(sharedArchiveConfigFile)) {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(fos));
            out.println("VERSION: 1.0");
            out.println("@SECTION: String");
            out.println("31: shared_test_string_unique_14325");
            for (int i=0; i<200000; i++) {
                String s = "generated_string " + i;
                out.println(s.length() + ": " + s);
            }
            out.close();
        }

        SharedStringsUtils.run(args, SharedStringsStress::test);
    }

    public static void test(String[] args) throws Exception {
        String vmOptionsPrefix[] = SharedStringsUtils.getChildVMOptionsPrefix();
        String appJar = JarBuilder.build("SharedStringsStress", "HelloString");

        String test_cases[][] = {
            // default heap size
            {},

            // Test for handling of heap fragmentation. With sharedArchiveConfigFile, we will dump about
            // 18MB of shared objects on 64 bit VM (smaller on 32-bit).
            //
            // During dump time, an extra copy of these objects are allocated,
            // so we need about 36MB, plus a few MB for other system data. So 64MB total heap
            // should be enough.
            //
            // The VM should executed a full GC to maximize contiguous free space and
            // avoid fragmentation.
            {"-Xmx64m"},
        };

        for (String[] extra_opts: test_cases) {
            vmOptionsPrefix = TestCommon.concat(vmOptionsPrefix, extra_opts);

            OutputAnalyzer dumpOutput = TestCommon.dump(appJar, TestCommon.list("HelloString"),
                TestCommon.concat(vmOptionsPrefix,
                    "-XX:SharedArchiveConfigFile=" + sharedArchiveConfigFile,
                    "-Xlog:gc+region+cds",
                    "-Xlog:gc+region=trace"));
            TestCommon.checkDump(dumpOutput);
            OutputAnalyzer execOutput = TestCommon.exec(appJar,
                TestCommon.concat(vmOptionsPrefix, "HelloString"));
            TestCommon.checkExec(execOutput);
        }
    }
}
