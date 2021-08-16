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
 * @summary Regression test for JDK-8098821
 * @bug 8098821
 * @requires vm.cds.archived.java.heap
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @run driver SysDictCrash
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class SysDictCrash {
    public static void main(String[] args) throws Exception {
        SharedStringsUtils.run(args, SysDictCrash::test);
    }

    public static void test(String[] args) throws Exception {
        String vmOptionsPrefix[] = SharedStringsUtils.getChildVMOptionsPrefix();

        // SharedBaseAddress=0 puts the archive at a very high address, which provokes the crash.
        boolean continueTest = true;

        CDSOptions opts = (new CDSOptions())
            .addPrefix(vmOptionsPrefix,
                       "-XX:+UseG1GC", "-XX:MaxRAMPercentage=12.5",
                       "-cp", ".",
                       "-XX:SharedBaseAddress=0",
                       "-showversion", "-Xlog:cds,cds+hashtables")
            .setArchiveName("./SysDictCrash.jsa");
        OutputAnalyzer output = CDSTestUtils.createArchive(opts);
        try {
            TestCommon.checkDump(output);
        } catch (java.lang.RuntimeException re) {
            if (!output.getStdout().contains("UseCompressedOops and UseCompressedClassPointers have been disabled due to")) {
                throw re;
            } else {
                System.out.println("Shared archive was not created due to UseCompressedOops and UseCompressedClassPointers have been disabled.");
                continueTest = false;
            }
        }

        if (!continueTest) {
            return;
        }

        opts = (new CDSOptions())
            .setArchiveName("./SysDictCrash.jsa") // prevents the assignment of a default archive name
            .setUseVersion(false) // the -version must be the last arg for this test to work
            .addSuffix(vmOptionsPrefix,
                       "-Xlog:cds",
                       "-XX:+UseG1GC", "-XX:MaxRAMPercentage=12.5",
                       "-XX:SharedArchiveFile=./SysDictCrash.jsa",
                       "-version");
        CDSTestUtils.run(opts)
                    .assertNormalExit();
    }
}
