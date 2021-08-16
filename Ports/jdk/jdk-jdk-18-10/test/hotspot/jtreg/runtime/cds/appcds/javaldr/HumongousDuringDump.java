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
 * @summary Test how CDS dumping handles the existence of humongous G1 regions.
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @requires vm.cds.archived.java.heap
 * @requires vm.jvmti
 * @run driver/timeout=240 HumongousDuringDump
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;

public class HumongousDuringDump {
    public static String appClasses[] = {
        Hello.class.getName(),
    };
    public static String agentClasses[] = {
        HumongousDuringDumpTransformer.class.getName(),
    };

    public static void main(String[] args) throws Throwable {
        String agentJar =
            ClassFileInstaller.writeJar("HumongousDuringDumpTransformer.jar",
                                        ClassFileInstaller.Manifest.fromSourceFile("HumongousDuringDumpTransformer.mf"),
                                        agentClasses);

        String appJar =
            ClassFileInstaller.writeJar("HumongousDuringDumpApp.jar", appClasses);

        String gcLog = Boolean.getBoolean("test.cds.verbose.gc") ?
            "-Xlog:gc*=info,gc+region=trace,gc+alloc+region=debug" : "-showversion";

        String extraArg = "-javaagent:" + agentJar;
        String extraOption = "-XX:+AllowArchivingWithJavaAgent";

        OutputAnalyzer out =
          TestCommon.testDump(appJar, TestCommon.list(Hello.class.getName()),
                              "-XX:+UnlockDiagnosticVMOptions", extraOption,
                              "-Xlog:gc+region+cds",
                              "-Xlog:gc+region=trace",
                              extraArg, "-Xmx64m", gcLog);
        out.shouldContain("(Unmovable) humongous regions have been found and may lead to fragmentation");
        out.shouldContain("All free regions should be at the top end of the heap, but we found holes.");
        out.shouldMatch("gc,region,cds. HeapRegion .* HUM. hole");
        String pattern = "gc,region,cds. HeapRegion .*hole";
        out.shouldMatch(pattern);
        out.shouldNotMatch(pattern + ".*unexpected");

        TestCommon.run(
                "-cp", appJar,
                "-verbose",
                "-Xmx64m",
                "-Xlog:cds=info",
                "-XX:+UnlockDiagnosticVMOptions", extraOption,
                gcLog,
                Hello.class.getName())
              .assertNormalExit();
    }
}

