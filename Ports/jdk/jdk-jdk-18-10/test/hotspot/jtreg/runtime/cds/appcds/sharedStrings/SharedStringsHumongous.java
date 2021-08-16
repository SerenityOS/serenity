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
 *
 */

/*
 * @test
 * @summary Use a shared string allocated in a humongous G1 region.
 * @comment -- the following implies that G1 is used (by command-line or by default)
 * @requires vm.cds.archived.java.heap
 *
 * @library /test/hotspot/jtreg/runtime/cds/appcds /test/lib
 * @build HelloString
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. SharedStringsHumongous
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import sun.hotspot.WhiteBox;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Asserts;

public class SharedStringsHumongous {
    static String sharedArchiveConfigFile = CDSTestUtils.getOutputDir() + File.separator + "SharedStringsHumongous_gen.txt";

    public static void main(String[] args) throws Exception {
        WhiteBox wb = WhiteBox.getWhiteBox();

        try (FileOutputStream fos = new FileOutputStream(sharedArchiveConfigFile)) {
            PrintWriter out = new PrintWriter(new OutputStreamWriter(fos));
            out.println("VERSION: 1.0");
            out.println("@SECTION: String");
            out.println("31: shared_test_string_unique_14325");
            int region_size = wb.g1RegionSize();
            char body[] = new char[region_size + region_size / 2];
            for (int i = 0; i < body.length; i++) {
              body[i] = 'x';
            }
            Asserts.assertTrue(wb.g1IsHumongous(body));
            String prefix = "generated_string (followed by " + body.length + " 'x') ";

            System.out.println("G1 region size: " + region_size);
            System.out.println("Using a humongous string: " + prefix);

            String s = prefix + new String(body);
            out.println(s.length() + ": " + s);
            out.close();
        }

        SharedStringsUtils.run(args, SharedStringsHumongous::test);
    }

    public static void test(String[] args) throws Exception {
        String vmOptionsPrefix[] = SharedStringsUtils.getChildVMOptionsPrefix();
        String appJar = JarBuilder.build("SharedStringsHumongous", "HelloString");

        OutputAnalyzer dumpOutput = TestCommon.dump(appJar, TestCommon.list("HelloString"),
                TestCommon.concat(vmOptionsPrefix,
                    "-XX:SharedArchiveConfigFile=" + sharedArchiveConfigFile,
                    "-Xlog:gc+region+cds",
                    "-Xlog:gc+region=trace"));
        TestCommon.checkDump(dumpOutput, "extra interned string ignored; size too large");
        // Extra strings that are humongous are not kelp alive, so they should be GC'ed
        // before dumping the string table. That means the heap should contain no
        // humongous regions.
        dumpOutput.shouldNotMatch("gc,region,cds. HeapRegion 0x[0-9a-f]* HUM");

        OutputAnalyzer execOutput = TestCommon.exec(appJar,
            TestCommon.concat(vmOptionsPrefix, "HelloString"));
        TestCommon.checkExec(execOutput);
    }
}
