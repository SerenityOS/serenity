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
 * @summary Test automatic relocation of archive heap regions dur to heap size changes.
 * @requires vm.cds.archived.java.heap
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @compile ../test-classes/Hello.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=160 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. DifferentHeapSizes
 */

import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;
import jdk.test.lib.cds.CDSTestUtils;

public class DifferentHeapSizes {
    static final String DEDUP = "-XX:+UseStringDeduplication"; // This increases code coverage.

    static class Scenario {
        int dumpSize;   // in MB
        int runSizes[]; // in MB
        Scenario(int ds, int... rs) {
            dumpSize = ds;
            runSizes = rs;
        }
    }

    static Scenario[] scenarios = {
        //           dump -Xmx ,         run -Xmx
        new Scenario(        32,         32, 64, 512, 2048, 4097, 16374, 31000),
        new Scenario(       128,         32, 64, 512, 2048, 4097, 16374, 31000, 40000),
        new Scenario(      2048,         32, 512, 2600, 4097, 8500, 31000,      40000),
        new Scenario(     17000,         32, 512, 2048, 4097, 8500, 31000,      40000),
        new Scenario(     31000,         32, 512, 2048, 4097, 8500, 17000,      40000)
    };

    public static void main(String[] args) throws Exception {
        JarBuilder.getOrCreateHelloJar();
        String appJar = TestCommon.getTestJar("hello.jar");
        String appClasses[] = TestCommon.list("Hello");

        for (Scenario s : scenarios) {
            String dumpXmx = "-Xmx" + s.dumpSize + "m";
            OutputAnalyzer output = TestCommon.dump(appJar, appClasses, dumpXmx);

            for (int runSize : s.runSizes) {
                String runXmx = "-Xmx" + runSize + "m";
                CDSTestUtils.Result result = TestCommon.run("-cp", appJar, "-showversion",
                        "-Xlog:cds", runXmx, DEDUP, "Hello");
                if (runSize < 32768) {
                    result
                        .assertNormalExit("Hello World")
                        .assertNormalExit(out -> {
                            out.shouldNotContain(CDSTestUtils.MSG_RANGE_NOT_WITHIN_HEAP);
                            out.shouldNotContain(CDSTestUtils.MSG_RANGE_ALREADT_IN_USE);
                        });
                } else {
                    result
                        .assertAbnormalExit("Unable to use shared archive.",
                                            "The saved state of UseCompressedOops and UseCompressedClassPointers is different from runtime, CDS will be disabled.");
                }
            }
        }

        // Test various settings of -XX:HeapBaseMinAddress that would trigger
        // "CDS heap data need to be relocated because the desired range ... is outside of the heap"
        long default_base = WhiteBox.getWhiteBox().getSizeTVMFlag("HeapBaseMinAddress").longValue();
        long M = 1024 * 1024;
        long bases[] = new long[] {
            /* dump xmx */   /* run xmx */   /* dump base */             /* run base */
            128 * M,         128 * M,        default_base,               default_base + 256L * 1024 * 1024,
            128 * M,         16376 * M,      0x0000000119200000L,        -1,
        };

        for (int i = 0; i < bases.length; i += 4) {
            String dump_xmx  = getXmx(bases[i+0]);
            String run_xmx   = getXmx(bases[i+1]);
            String dump_base = getHeapBaseMinAddress(bases[i+2]);
            String run_base  = getHeapBaseMinAddress(bases[i+3]);

            TestCommon.dump(appJar, appClasses, dump_xmx, dump_base);
            TestCommon.run("-cp", appJar, "-showversion", "-Xlog:cds", run_xmx, run_base, DEDUP, "Hello")
                .assertNormalExit("Hello World")
                .assertNormalExit(out -> {
                        out.shouldNotContain(CDSTestUtils.MSG_RANGE_NOT_WITHIN_HEAP);
                        out.shouldNotContain(CDSTestUtils.MSG_RANGE_ALREADT_IN_USE);
                    });
        }
    }

    static String getXmx(long value) {
        if (value < 0) {
            return "-showversion"; // This is a harmless command line arg
        } else {
            return "-Xmx" + (value / 1024 / 1024) + "m";
        }
    }
    static String getHeapBaseMinAddress(long value) {
        if (value < 0) {
            return "-showversion"; // This is a harmless command line arg
        } else {
            return "-XX:HeapBaseMinAddress=0x" + Long.toHexString(value);
        }
    }
}
