/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Relocate CDS archived regions to the top of the G1 heap
 * @bug 8214455
 * @requires vm.cds.archived.java.heap
 * @requires (sun.arch.data.model == "64" & os.maxMemory > 4g)
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build HeapFragmentationApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar HeapFragmentationApp.jar HeapFragmentationApp
 * @run driver HeapFragmentationTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class HeapFragmentationTest {
    public static void main(String[] args) throws Exception {
        String mainClass = "HeapFragmentationApp";
        String appJar = ClassFileInstaller.getJarPath(mainClass + ".jar");
        String appClasses[] = TestCommon.list(mainClass);

        // We run with a 1400m heap, so we should be able to allocate a 1000m buffer, regardless
        // of the heap size chosen at dump time.
        String dumpTimeHeapSize = "-Xmx800m";
        String runTimeHeapSize = "-Xmx1400m";
        String BUFF_SIZE = Integer.toString(1000 * 1024 * 1024);
        String successOutput = "array.length = " + BUFF_SIZE;

        // On Linux/x64, this would (usually, if not affected by ASLR) force the low end of
        // the heap to be at 0x600000000. Thus, the heap ranges would be
        //
        // dump time:  [0x600000000 ... 0x600000000 + 800m]
        //                                            [AHR] <- archived heap regions are dumped at here
        //
        // run  time:  [0x600000000 ...............  0x600000000 + 1400m]
        //                                                          [AHR] <- archived heap regions are moved to here
        //             |<----      max allocatable size      ----->|
        String heapBase = "-XX:HeapBaseMinAddress=0x600000000";

        TestCommon.dump(appJar, appClasses,
                        heapBase,
                        dumpTimeHeapSize, "-Xlog:cds=debug");

        String execArgs[] = TestCommon.concat("-cp", appJar,
                                              "-showversion",
                                              heapBase,
                                              "-Xlog:cds=debug",
                                              "-Xlog:gc+heap+exit",
                                              "-Xlog:gc,gc+heap,gc+ergo+heap",
                                              "-XX:+CrashOnOutOfMemoryError",
                                              "-XX:+IgnoreUnrecognizedVMOptions",
                                              "-XX:+G1ExitOnExpansionFailure");

        // First, make sure the test runs without CDS
        TestCommon.runWithoutCDS(TestCommon.concat(execArgs, runTimeHeapSize, mainClass, BUFF_SIZE))
            .assertNormalExit(successOutput);

        // Run with CDS. The archived heap regions should be relocated to avoid fragmentation.
        TestCommon.run(TestCommon.concat(execArgs, runTimeHeapSize, mainClass,  BUFF_SIZE))
            .assertNormalExit(successOutput);
    }
}
