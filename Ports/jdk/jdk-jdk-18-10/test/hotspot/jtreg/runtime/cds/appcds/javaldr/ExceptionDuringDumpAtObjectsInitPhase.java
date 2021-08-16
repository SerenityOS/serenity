/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Out of memory When dumping the CDS archive
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @requires vm.cds.archived.java.heap
 * @requires vm.jvmti
 * @run driver ExceptionDuringDumpAtObjectsInitPhase
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;

public class ExceptionDuringDumpAtObjectsInitPhase {
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

        // 1. Test with exception
        System.out.println("1. Exception during dump");
        TestCommon.dump(appJar,
                        TestCommon.list(Hello.class.getName()),
                        "-XX:+UnlockDiagnosticVMOptions",
                        "-XX:+AllowArchivingWithJavaAgent",
                        "-javaagent:" + agentJar,
                        "-Xlog:cds,class+load",
                        "-Xmx32m",
                        "-Dtest.with.exception=true",
                        gcLog).shouldNotHaveExitValue(0)
                              .shouldContain("Preload Warning: Cannot find jdk/internal/math/FDBigInteger")
                              .shouldContain("VM exits due to exception, use -Xlog:cds,exceptions=trace for detail");

        // 2. Test with OOM
        System.out.println("2. OOM during dump");
        TestCommon.dump(appJar,
                        TestCommon.list(Hello.class.getName()),
                        "-XX:+UnlockDiagnosticVMOptions",
                        "-XX:+AllowArchivingWithJavaAgent",
                        "-javaagent:" + agentJar,
                        "-Dtest.with.oom=true",
                        "-Xlog:cds,class+load",
                        "-Xmx12M",
                        gcLog).shouldNotHaveExitValue(0)
                              .shouldContain("Out of memory. Please run with a larger Java heap, current MaxHeapSize");
    }
}
