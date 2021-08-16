/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test SharedBaseAddress
 * @bug 8265705 8267351
 * @summary Test variety of values for SharedBaseAddress, making sure
 *          VM handles normal values as well as edge values w/o a crash.
 * @requires vm.cds
 * @library /test/lib
 * @run driver SharedBaseAddress 0
 */

/**
 * @test SharedBaseAddress
 * @bug 8265705 8267351
 * @summary Test variety of values for SharedBaseAddress, making sure
 *          VM handles normal values as well as edge values w/o a crash.
 * @requires vm.cds
 * @library /test/lib
 * @run driver SharedBaseAddress 1
 */

/**
 * @test SharedBaseAddress
 * @bug 8265705 8267351
 * @summary Test variety of values for SharedBaseAddress, making sure
 *          VM handles normal values as well as edge values w/o a crash.
 * @requires vm.cds
 * @requires vm.bits == 64
 * @library /test/lib
 * @run driver SharedBaseAddress 0 provoke
 */

/**
 * @test SharedBaseAddress
 * @bug 8265705 8267351
 * @summary Test variety of values for SharedBaseAddress, making sure
 *          VM handles normal values as well as edge values w/o a crash.
 * @requires vm.cds
 * @requires vm.bits == 64
 * @library /test/lib
 * @run driver SharedBaseAddress 1 provoke
 */

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;

public class SharedBaseAddress {

    // shared base address test table
    private static final String[] testTable = {
        "1g", "8g", "64g","512g", "4t",
        "32t", "128t", "0",
        "1", "64k", "64M",
        "0x800001000",        // Default base address + 1 page - probably valid but unaligned to metaspace alignment, see JDK 8247522
        "0xfffffffffff00000", // archive top wraps around 64-bit address space
        "0xfff80000",         // archive top wraps around 32-bit address space
        "0xffffffffffffffff", // archive bottom wraps around 64-bit address space -- due to align_up()
        "0xffffffff",         // archive bottom wraps around 32-bit address space -- due to align_up()
        "0x00007ffffff00000", // end of archive will go past the end of user space on linux/x64
        "0x500000000",        // (20g) below 32g at a 4g aligned address, but cannot be expressed with a logical
                              //    immediate on aarch64 (0x5_0000_0000) (see JDK-8265705)
        "0",                  // always let OS pick the base address at runtime (ASLR for CDS archive)
    };

    // failed pattern
    private static String failedPattern = "os::release_memory\\(0x[0-9a-fA-F]*,\\s[0-9]*\\)\\sfailed";

    public static void main(String[] args) throws Exception {
        int mid = testTable.length / 2;
        int start = args[0].equals("0") ? 0 : mid;
        int end   = args[0].equals("0") ? mid : testTable.length;
        boolean provoke = (args.length > 1 && args[1].equals("provoke"));

        // provoke == true: we want to increase the chance that mapping the generated archive at the designated base
        // succeeds, to test Klass pointer encoding at that weird location. We do this by sizing heap + class space
        // small, and by switching off compressed oops.

        // provoke == false:  we just go with default parameters. This is more of a test of
        // CDS' ability to recover if mapping at runtime fails.
        for (int i = start; i < end; i++) {
            String testEntry = testTable[i];
            String filename = "SharedBaseAddress-base" + testEntry + ".jsa";
            System.out.println("sharedBaseAddress = " + testEntry);
            CDSOptions opts = (new CDSOptions())
                        .setArchiveName(filename)
                        .addPrefix("-XX:SharedBaseAddress=" + testEntry)
                        .addPrefix("-Xlog:cds=debug")
                        .addPrefix("-Xlog:cds+reloc=debug")
                        .addPrefix("-Xlog:nmt=debug")
                        .addPrefix("-Xlog:os=debug")
                        .addPrefix("-Xlog:gc+metaspace")
                        .addPrefix("-XX:NativeMemoryTracking=detail");

            if (provoke) {
                opts.addPrefix("-Xmx128m")
                    .addPrefix("-XX:CompressedClassSpaceSize=32m")
                    .addPrefix("-XX:-UseCompressedOops");
            }
            if (Platform.isDebugBuild()) {
                // Make VM start faster in debug build with large heap.
                opts.addPrefix("-XX:-ZapUnusedHeapArea");
            }

            CDSTestUtils.createArchiveAndCheck(opts);
            OutputAnalyzer out = CDSTestUtils.runWithArchiveAndCheck(opts);
            if (testEntry.equals("0")) {
                out.shouldContain("Archive(s) were created with -XX:SharedBaseAddress=0. Always map at os-selected address.")
                   .shouldContain("Try to map archive(s) at an alternative address")
                   .shouldNotMatch(failedPattern);
            }
        }
    }
}
