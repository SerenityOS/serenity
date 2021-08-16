/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary When dumping the CDS archive, try to cause garbage collection while classes are being loaded.
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @requires vm.cds
 * @requires vm.jvmti
 * @run driver GCDuringDump
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;

public class GCDuringDump {
    public static String appClasses[] = {
        Hello.class.getName(),
    };
    public static String agentClasses[] = {
        GCDuringDumpTransformer.class.getName(),
        GCDuringDumpTransformer.MyCleaner.class.getName(),
    };

    public static void main(String[] args) throws Throwable {
        String agentJar =
            ClassFileInstaller.writeJar("GCDuringDumpTransformer.jar",
                                        ClassFileInstaller.Manifest.fromSourceFile("GCDuringDumpTransformer.mf"),
                                        agentClasses);

        String appJar =
            ClassFileInstaller.writeJar("GCDuringDumpApp.jar", appClasses);

        String gcLog = Boolean.getBoolean("test.cds.verbose.gc") ?
            "-Xlog:gc*=info,gc+region=trace,gc+alloc+region=debug" : "-showversion";

        for (int i=0; i<3; i++) {
            // i = 0 -- run without agent = no extra GCs
            // i = 1 -- run with agent = cause extra GCs
            // i = 2 -- run with agent = cause extra GCs + use java.lang.ref.Cleaner

            String extraArg = (i == 0) ? "-showversion" : "-javaagent:" + agentJar;
            String extraOption = (i == 0) ? "-showversion" : "-XX:+AllowArchivingWithJavaAgent";
            String extraOption2 = (i != 2) ? "-showversion" : "-Dtest.with.cleaner=true";

            TestCommon.testDump(appJar, TestCommon.list(Hello.class.getName()),
                                "-XX:+UnlockDiagnosticVMOptions", extraOption, extraOption2,
                                "-Xlog:exceptions=trace",
                                extraArg, "-Xmx32m", gcLog);

            TestCommon.run(
                "-cp", appJar,
                "-Xmx32m",
                "-Xlog:cds=info",
                "-XX:+UnlockDiagnosticVMOptions", extraOption,
                gcLog,
                Hello.class.getName())
              .assertNormalExit();
        }
    }
}

