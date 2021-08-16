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

/*
 * @test
 * @summary Basic test for shared strings
 * @requires vm.cds.archived.java.heap
 * @library /test/hotspot/jtreg/runtime/cds/appcds /test/lib
 * @build HelloString
 * @run driver SharedStringsBasic
 */
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;

// This test does not use SharedStringsUtils.dumpXXX()
// and SharedStringsUtils.runWithXXX() intentionally:
// - in order to demonstrate the basic use of the functionality
// - to provide sanity check and catch potential problems in the utils
public class SharedStringsBasic {
    public static void main(String[] args) throws Exception {
        SharedStringsUtils.run(args, SharedStringsBasic::test);
    }

    public static void test(String[] args) throws Exception {
        String vmOptionsPrefix[] = SharedStringsUtils.getChildVMOptionsPrefix();

        String appJar = JarBuilder.build("SharedStringsBasic", "HelloString");

        String sharedArchiveConfigFile =
            TestCommon.getSourceFile("SharedStringsBasic.txt").toString();

        CDSOptions opts = (new CDSOptions())
            .addPrefix(vmOptionsPrefix,
            "-cp", appJar,
            "-XX:SharedArchiveConfigFile=" + sharedArchiveConfigFile,
            "-Xlog:cds,cds+hashtables")
            .setArchiveName("./SharedStringsBasic.jsa");
        CDSTestUtils.createArchiveAndCheck(opts)
            .shouldContain("Shared string table stats");

        opts = (new CDSOptions())
            .setUseVersion(false)
            .setXShareMode("auto")
            .addSuffix(vmOptionsPrefix,
            "-cp", appJar,
            "-XX:SharedArchiveFile=./SharedStringsBasic.jsa",
            "-showversion",
            "HelloString");
        CDSTestUtils.run(opts)
                    .assertNormalExit();
    }
}
