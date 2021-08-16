/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @comment the test uses -XX:ArchiveRelocationMode=1 to force relocation.
 * @requires vm.cds
 * @summary Testing relocation of CDS archive (during both dump time and run time)
 * @comment JDK-8231610 Relocate the CDS archive if it cannot be mapped to the requested address
 * @bug 8231610
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds/test-classes
 * @build Hello
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar hello.jar Hello
 * @run driver ArchiveRelocationTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;
import jtreg.SkippedException;

public class ArchiveRelocationTest {
    public static void main(String... args) throws Exception {
        try {
            test(false);
            test(true);
        } catch (SkippedException s) {
            s.printStackTrace();
            throw new RuntimeException("Archive mapping should always succeed after JDK-8231610 (did the machine run out of memory?)");
        }
    }

    static int caseCount = 0;

    // run_reloc  - force relocation of archive during run time?
    // Note: relocation always happens during dumping.
    static void test(boolean run_reloc) throws Exception {
        caseCount += 1;
        System.out.println("============================================================");
        System.out.println("case = " + caseCount + ", run_reloc = " + run_reloc);
        System.out.println("============================================================");

        String appJar = ClassFileInstaller.getJarPath("hello.jar");
        String mainClass = "Hello";
        String forceRelocation = "-XX:ArchiveRelocationMode=1";
        String runRelocArg  = run_reloc  ? forceRelocation : "-showversion";
        String logArg = "-Xlog:cds=debug,cds+reloc=debug";
        String unlockArg = "-XX:+UnlockDiagnosticVMOptions";
        String nmtArg = "-XX:NativeMemoryTracking=detail";

        OutputAnalyzer out = TestCommon.dump(appJar,
                                             TestCommon.list(mainClass),
                                             unlockArg, logArg, nmtArg);
        out.shouldContain("Relocating archive from");

        TestCommon.run("-cp", appJar, unlockArg, runRelocArg, logArg,  mainClass)
            .assertNormalExit(output -> {
                    if (run_reloc) {
                        output.shouldContain("Try to map archive(s) at an alternative address");
                    }
                });
    }
}
